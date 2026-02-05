#!/usr/bin/env python3
"""
ChunkNav Visualizer - Dear PyGui Edition

A GPU-accelerated visualization tool for chunknav navigation data.
All configuration is done through the UI - no command line flags needed.

Usage:
    python chunknav_visualizer_dpg.py

Requirements:
    pip install dearpygui numpy
"""

import json
import math
import os
import struct
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import dearpygui.dearpygui as dpg
import numpy as np

# =============================================================================
# Constants (matching ChunkNavConfig.java)
# =============================================================================

CHUNK_SIZE = 100  # Tiles per chunk
CELLS_PER_EDGE = 200  # Binary format uses 200x200 (half-tile level)
TOTAL_CELLS = CELLS_PER_EDGE * CELLS_PER_EDGE  # 40,000

# Binary format constants
MAGIC = 0x434E4156  # "CNAV" in ASCII
HEADER_SIZE = 64

# Layer byte mapping
LAYER_OUTSIDE = 0
LAYER_INSIDE = 1
LAYER_CELLAR = 2

LAYER_NAMES = {
    LAYER_OUTSIDE: 'surface',
    LAYER_INSIDE: 'inside',
    LAYER_CELLAR: 'cellar',
}

# Portal type mapping (must match ChunkPortal.PortalType enum order)
PORTAL_TYPES = ['DOOR', 'GATE', 'STAIRS_UP', 'STAIRS_DOWN', 'CELLAR',
                'MINE_ENTRANCE', 'MINEHOLE', 'LADDER', 'TELEPORT', 'UNKNOWN']

# Known world profiles
WORLD_PROFILES = {
    1: "b7c199a4557503a8",
    2: "c646473983afec09",
}

# Colors (RGBA 0-255)
COLOR_WALKABLE = (51, 153, 51, 255)      # Green
COLOR_BLOCKED = (153, 51, 51, 255)       # Red
COLOR_UNOBSERVED = (77, 77, 77, 255)     # Dark gray
COLOR_SELECTED = (255, 255, 0, 255)      # Yellow
COLOR_BACKGROUND = (26, 26, 26, 255)     # Near black

# Portal colors - all portals use the same bright color to stand out
PORTAL_COLOR = (0, 255, 255, 255)  # Cyan (bright, pops)
PORTAL_COLORS = {
    'DOOR': PORTAL_COLOR,
    'GATE': PORTAL_COLOR,
    'CELLAR': PORTAL_COLOR,
    'STAIRS_UP': PORTAL_COLOR,
    'STAIRS_DOWN': PORTAL_COLOR,
    'MINE_ENTRANCE': PORTAL_COLOR,
    'MINEHOLE': PORTAL_COLOR,
    'LADDER': PORTAL_COLOR,
    'TELEPORT': PORTAL_COLOR,
    'UNKNOWN': PORTAL_COLOR,
}


# =============================================================================
# Application State
# =============================================================================

@dataclass
class AppState:
    """Global application state."""
    # Profile data
    profiles: list = field(default_factory=list)  # [(chunknav_dir, profile_id, chunk_count), ...]
    current_profile_idx: int = 0

    # Chunk data
    chunks: list = field(default_factory=list)           # All loaded chunks
    filtered_chunks: list = field(default_factory=list)  # After layer filter
    positions: dict = field(default_factory=dict)        # gridId -> (x, y)
    layer_bounds: dict = field(default_factory=dict)     # layer -> (min_y, max_y)
    available_layers: list = field(default_factory=list) # ['surface', 'inside', ...]
    path_data: Optional[dict] = None                     # From chunknav_path.json

    # UI State
    current_layer: str = 'surface'
    selected_chunk_idx: int = -1
    show_walkability: bool = True
    show_portals: bool = True
    show_path: bool = False
    show_grid: bool = True        # Grid lines on world map (chunk borders)
    show_cell_lines: bool = False  # Cell lines on detail view
    show_connections: bool = True  # Portal connection lines on world map

    # View state
    zoom_level: float = 1.0
    pan_offset_x: float = 0.0
    pan_offset_y: float = 0.0
    detail_zoom_level: float = 2.0  # Start at 2x for detail view (400x400 display)

    # Cached textures
    chunk_textures: dict = field(default_factory=dict)  # gridId -> texture_id
    world_bounds: tuple = (0, 0, 0, 0)  # (min_gx, min_gy, max_gx, max_gy) in grid coords
    chunk_pixel_size: int = 100  # Pixels per chunk in the world texture (higher = better quality)

    # Texture tag counters (to avoid tag collisions)
    world_texture_counter: int = 0
    detail_texture_counter: int = 0
    current_world_tag: str = ""
    current_detail_tag: str = ""
    world_texture_width: int = 0
    world_texture_height: int = 0
    world_texture_dirty: bool = True  # Flag to track if texture needs regeneration


# Global state instance
state = AppState()


# =============================================================================
# Binary Loading Functions (from existing chunknav_visualizer.py)
# =============================================================================

def unpack_walkability(data: bytes) -> list:
    """Unpack 2-bit walkability values from bytes to flat array.

    Each byte contains 4 walkability values (2 bits each).
    Returns list of 40,000 values (0=walkable, 1=partial, 2=blocked).
    """
    result = []
    for byte in data:
        result.append((byte >> 6) & 0x03)
        result.append((byte >> 4) & 0x03)
        result.append((byte >> 2) & 0x03)
        result.append(byte & 0x03)
    return result


def unpack_observed(data: bytes) -> list:
    """Unpack 1-bit observed values from bytes to flat array.

    Each byte contains 8 observed bits.
    Returns list of 40,000 boolean values.
    """
    result = []
    for byte in data:
        for bit in range(7, -1, -1):
            result.append((byte >> bit) & 0x01)
    return result


def unpack_edges(data: bytes) -> dict:
    """Unpack 1-bit edge values from bytes.

    Each byte contains 8 edge walkability bits.
    Returns dict with north/south/east/west edge arrays (200 booleans each).
    """
    bits = []
    for byte in data:
        for bit in range(7, -1, -1):
            bits.append((byte >> bit) & 0x01)

    edges = {
        'north': bits[0:200],
        'south': bits[200:400],
        'east': bits[400:600],
        'west': bits[600:800]
    }
    return edges


def read_length_prefixed_string(f) -> str:
    """Read a length-prefixed UTF-8 string (2-byte big-endian length prefix)."""
    length_bytes = f.read(2)
    if len(length_bytes) < 2:
        raise EOFError("Unexpected end of file reading string length")
    length = struct.unpack('>H', length_bytes)[0]
    if length == 0:
        return ""
    string_bytes = f.read(length)
    if len(string_bytes) < length:
        raise EOFError("Unexpected end of file reading string")
    return string_bytes.decode('utf-8')


def load_chunk_binary(filepath: Path) -> dict:
    """Load a single chunk from binary format.

    Binary format (ChunkNavBinaryFormat.java):
    - Magic (4 bytes): 0x434E4156 ("CNAV")
    - Header (60 bytes): version, flags, gridId, timestamps, layer byte, neighbors, padding
    - Walkability grid (10,000 bytes): 40,000 cells at 2 bits each
    - Observed grid (5,000 bytes): 40,000 cells at 1 bit each
    - Edge arrays (100 bytes): 800 booleans at 1 bit each
    - Portals (variable): count + portal data
    - Connected chunks (variable): count + gridIds
    - Reachable areas (variable): count + areaIds

    Returns dict matching the JSON structure for compatibility.
    """
    with open(filepath, 'rb') as f:
        # Read and verify magic number
        magic = struct.unpack('>I', f.read(4))[0]
        if magic != MAGIC:
            raise ValueError(f"Invalid chunk file: bad magic number (got {hex(magic)}, expected {hex(MAGIC)})")

        # Read header
        version = struct.unpack('>H', f.read(2))[0]
        flags = struct.unpack('>H', f.read(2))[0]
        grid_id = struct.unpack('>q', f.read(8))[0]
        last_updated = struct.unpack('>q', f.read(8))[0]
        confidence = struct.unpack('>f', f.read(4))[0]
        layer_byte = struct.unpack('>B', f.read(1))[0]
        neighbor_north = struct.unpack('>q', f.read(8))[0]
        neighbor_south = struct.unpack('>q', f.read(8))[0]
        neighbor_east = struct.unpack('>q', f.read(8))[0]
        neighbor_west = struct.unpack('>q', f.read(8))[0]

        # Skip 3 padding bytes to complete 64-byte header
        f.read(3)

        # Convert layer byte to string
        layer = LAYER_NAMES.get(layer_byte, 'surface')

        # Read grids
        walkability_bytes = f.read(10000)  # 40,000 cells / 4 cells per byte
        observed_bytes = f.read(5000)      # 40,000 cells / 8 cells per byte
        edge_bytes = f.read(100)           # 800 edge points / 8 per byte

        walkability = unpack_walkability(walkability_bytes)
        observed = unpack_observed(observed_bytes)
        edges = unpack_edges(edge_bytes)

        # Read portals
        portal_count = struct.unpack('>H', f.read(2))[0]
        portals = []
        for _ in range(portal_count):
            gob_name = read_length_prefixed_string(f)
            gob_hash = read_length_prefixed_string(f)
            portal_type_byte = struct.unpack('>B', f.read(1))[0]
            local_x = struct.unpack('>H', f.read(2))[0]
            local_y = struct.unpack('>H', f.read(2))[0]
            connects_to = struct.unpack('>q', f.read(8))[0]
            exit_x = struct.unpack('>h', f.read(2))[0]  # signed short
            exit_y = struct.unpack('>h', f.read(2))[0]  # signed short
            last_traversed = struct.unpack('>q', f.read(8))[0]

            portal_type = PORTAL_TYPES[portal_type_byte] if portal_type_byte < len(PORTAL_TYPES) else 'UNKNOWN'

            portals.append({
                'gobName': gob_name,
                'gobHash': gob_hash,
                'type': portal_type,
                'localX': local_x,
                'localY': local_y,
                'connectsToGridId': connects_to,
                'exitLocalX': exit_x if exit_x >= 0 else None,
                'exitLocalY': exit_y if exit_y >= 0 else None,
                'lastTraversed': last_traversed
            })

        # Read connected chunks
        connected_count = struct.unpack('>H', f.read(2))[0]
        connected_chunks = []
        for _ in range(connected_count):
            connected_chunks.append(struct.unpack('>q', f.read(8))[0])

        # Read reachable areas
        area_count = struct.unpack('>H', f.read(2))[0]
        reachable_areas = []
        for _ in range(area_count):
            reachable_areas.append(struct.unpack('>i', f.read(4))[0])

        # Build chunk dict
        chunk = {
            'version': version,
            'gridId': grid_id,
            'lastUpdated': last_updated,
            'confidence': confidence,
            'layer': layer,
            'neighborNorth': neighbor_north,
            'neighborSouth': neighbor_south,
            'neighborEast': neighbor_east,
            'neighborWest': neighbor_west,
            'walkability': walkability,
            'observed': observed,
            'edges': edges,
            'portals': portals,
            'connectedChunks': connected_chunks,
            'reachableAreaIds': reachable_areas,
        }

        return chunk


def load_all_chunks_binary(chunknav_dir: Path) -> dict:
    """Load all .chunk files from a directory.

    Returns dict with 'chunks' list matching the JSON structure.
    """
    chunks = []
    chunk_dir = Path(chunknav_dir)

    if not chunk_dir.exists() or not chunk_dir.is_dir():
        return {'chunks': [], 'genus': 'unknown'}

    chunk_files = list(chunk_dir.glob('*.chunk'))
    errors = 0

    for chunk_file in chunk_files:
        try:
            chunk = load_chunk_binary(chunk_file)
            chunks.append(chunk)
        except ValueError as e:
            errors += 1
            if errors <= 3:
                print(f"Warning: Failed to load {chunk_file.name}: {e}")
        except Exception as e:
            errors += 1
            if errors <= 3:
                print(f"Warning: Failed to load {chunk_file.name}: {e}")

    if errors > 3:
        print(f"... and {errors - 3} more errors")

    # Extract genus from parent directory path
    genus = chunk_dir.parent.name if chunk_dir.parent else 'unknown'

    return {
        'chunks': chunks,
        'genus': genus,
        'format': 'binary',
        'chunkCount': len(chunks)
    }


def load_path_data(chunknav_path: Path) -> Optional[dict]:
    """Load the path visualization data if it exists."""
    path = Path(chunknav_path)

    # If it's a directory, look for path file in parent
    if path.is_dir():
        path_file = path.parent / "chunknav_path.json"
    else:
        path_file = path.parent / "chunknav_path.json"

    if path_file.exists():
        with open(path_file, 'r', encoding='utf-8') as f:
            return json.load(f)
    return None


# =============================================================================
# Profile Discovery
# =============================================================================

def discover_profiles() -> list:
    """Find chunknav directories in default locations.

    Returns list of (chunknav_dir, profile_id, chunk_count) tuples.
    """
    dirs = []

    def scan_profiles(profiles_path: Path):
        if not profiles_path.exists():
            print(f"  Path does not exist: {profiles_path}")
            return
        print(f"  Scanning: {profiles_path}")
        for profile_dir in profiles_path.iterdir():
            if profile_dir.is_dir():
                chunknav_dir = profile_dir / "chunknav"
                if chunknav_dir.exists() and chunknav_dir.is_dir():
                    chunk_files = list(chunknav_dir.glob('*.chunk'))
                    if chunk_files:
                        print(f"    Found: {profile_dir.name} ({len(chunk_files)} chunks)")
                        dirs.append((chunknav_dir, profile_dir.name, len(chunk_files)))
                    else:
                        print(f"    Skipping {profile_dir.name}: no .chunk files")

    print("Discovering profiles...")

    # Windows AppData location
    appdata = os.environ.get('APPDATA', '')
    if appdata:
        hh_path = Path(appdata) / "Haven and Hearth" / "profiles"
        scan_profiles(hh_path)
    else:
        print("  APPDATA not set")

    # Direct Windows path (fallback)
    direct_path = Path(r"C:\Users\imbecil\AppData\Roaming\Haven and Hearth\profiles")
    if direct_path.exists() and str(direct_path) not in [str(d[0].parent) for d in dirs]:
        scan_profiles(direct_path)

    # Remove duplicates
    seen = set()
    unique_dirs = []
    for chunknav_dir, profile_id, count in dirs:
        key = str(chunknav_dir.resolve()) if chunknav_dir.exists() else str(chunknav_dir)
        if key not in seen:
            seen.add(key)
            unique_dirs.append((chunknav_dir, profile_id, count))
        else:
            print(f"    Duplicate skipped: {profile_id}")

    print(f"  Total unique profiles: {len(unique_dirs)}")
    return unique_dirs


# =============================================================================
# Data Processing Functions
# =============================================================================

def get_available_layers(data: dict) -> list:
    """Get all unique layers from the chunk data."""
    layers = set()
    for chunk in data.get('chunks', []):
        layer = chunk.get('layer', 'surface')
        layers.add(layer)
    return sorted(layers)


def filter_chunks_by_layer(chunks: list, layer_filter: str) -> list:
    """Filter chunks to only include those in the specified layer."""
    if layer_filter is None or layer_filter == 'all':
        return chunks
    return [c for c in chunks if c.get('layer', 'surface') == layer_filter]


def build_grid_positions_from_neighbors(chunks: list) -> dict:
    """Build grid positions by traversing neighbor relationships.

    Returns a dict of gridId -> (x, y) positions.
    Creates a consistent coordinate system from the neighbor graph.
    Handles disconnected components by placing them offset from each other.
    """
    if not chunks:
        return {}

    # Build lookup by gridId
    chunks_by_id = {c['gridId']: c for c in chunks}
    all_chunk_ids = set(chunks_by_id.keys())

    positions = {}
    component_offset_x = 0

    # Process all chunks, handling disconnected components
    while len(positions) < len(chunks):
        unpositioned = [cid for cid in all_chunk_ids if cid not in positions]
        if not unpositioned:
            break

        start_id = unpositioned[0]
        positions[start_id] = (component_offset_x, 0)

        # BFS to assign positions based on neighbor relationships
        queue = [start_id]
        component_min_x = component_offset_x
        component_max_x = component_offset_x

        while queue:
            current_id = queue.pop(0)
            if current_id not in chunks_by_id:
                continue

            chunk = chunks_by_id[current_id]
            cx, cy = positions[current_id]
            component_min_x = min(component_min_x, cx)
            component_max_x = max(component_max_x, cx)

            neighbors = [
                ('neighborNorth', 0, -1),
                ('neighborSouth', 0, 1),
                ('neighborEast', 1, 0),
                ('neighborWest', -1, 0),
            ]

            for key, dx, dy in neighbors:
                neighbor_id = chunk.get(key, -1)
                if neighbor_id != -1 and neighbor_id in chunks_by_id and neighbor_id not in positions:
                    positions[neighbor_id] = (cx + dx, cy + dy)
                    queue.append(neighbor_id)

        # Next component starts after this one with a gap
        component_offset_x = component_max_x + 3

    return positions


def build_grid_positions_by_layer(chunks: list) -> tuple:
    """Build grid positions for all chunks, organizing by layer.

    Each layer gets its own coordinate space, stacked vertically.
    Returns (positions_dict, layer_bounds_dict).
    """
    if not chunks:
        return {}, {}

    # Group chunks by layer
    chunks_by_layer = {}
    for chunk in chunks:
        layer = chunk.get('layer', 'surface')
        if layer not in chunks_by_layer:
            chunks_by_layer[layer] = []
        chunks_by_layer[layer].append(chunk)

    # Define layer order
    layer_order = ['surface', 'inside', 'cellar']
    mine_layers = sorted([l for l in chunks_by_layer.keys() if l.startswith('mine')],
                         key=lambda x: int(x[4:]) if x[4:].isdigit() else 0)
    layer_order.extend(mine_layers)
    for layer in chunks_by_layer.keys():
        if layer not in layer_order:
            layer_order.append(layer)

    all_positions = {}
    layer_bounds = {}
    current_y_offset = 0
    layer_gap = 5

    for layer in layer_order:
        if layer not in chunks_by_layer:
            continue

        layer_chunks = chunks_by_layer[layer]
        layer_positions = build_grid_positions_from_neighbors(layer_chunks)

        if not layer_positions:
            continue

        ys = [pos[1] for pos in layer_positions.values()]
        min_y = min(ys)
        max_y = max(ys)
        layer_height = max_y - min_y + 1

        for grid_id, (x, y) in layer_positions.items():
            adjusted_y = current_y_offset + (y - min_y)
            all_positions[grid_id] = (x, adjusted_y)

        layer_bounds[layer] = (current_y_offset, current_y_offset + layer_height - 1)
        current_y_offset += layer_height + layer_gap

    return all_positions, layer_bounds


# =============================================================================
# Texture Generation (numpy-vectorized for performance)
# =============================================================================

def draw_line(img: np.ndarray, x0: int, y0: int, x1: int, y1: int, color: tuple, thickness: int = 1):
    """Draw a line on an image using Bresenham's algorithm.

    Args:
        img: RGBA numpy array (height, width, 4)
        x0, y0: Start point
        x1, y1: End point
        color: RGBA tuple
        thickness: Line thickness in pixels
    """
    height, width = img.shape[:2]

    dx = abs(x1 - x0)
    dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy

    x, y = x0, y0

    while True:
        # Draw point with thickness
        for ty in range(-thickness//2, thickness//2 + 1):
            for tx in range(-thickness//2, thickness//2 + 1):
                px, py = x + tx, y + ty
                if 0 <= px < width and 0 <= py < height:
                    img[py, px] = color

        if x == x1 and y == y1:
            break

        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x += sx
        if e2 < dx:
            err += dx
            y += sy


def draw_arrow(img: np.ndarray, x0: int, y0: int, x1: int, y1: int, color: tuple, thickness: int = 1):
    """Draw an arrow from (x0, y0) to (x1, y1).

    Args:
        img: RGBA numpy array (height, width, 4)
        x0, y0: Start point
        x1, y1: End point (arrow head)
        color: RGBA tuple
        thickness: Line thickness in pixels
    """
    # Draw the main line
    draw_line(img, x0, y0, x1, y1, color, thickness)

    # Calculate arrow head
    angle = math.atan2(y1 - y0, x1 - x0)
    arrow_length = 5
    arrow_angle = math.pi / 7  # ~25 degrees (narrower head)

    # Arrow head points
    ax1 = int(x1 - arrow_length * math.cos(angle - arrow_angle))
    ay1 = int(y1 - arrow_length * math.sin(angle - arrow_angle))
    ax2 = int(x1 - arrow_length * math.cos(angle + arrow_angle))
    ay2 = int(y1 - arrow_length * math.sin(angle + arrow_angle))

    draw_line(img, x1, y1, ax1, ay1, color, thickness)
    draw_line(img, x1, y1, ax2, ay2, color, thickness)


def generate_chunk_texture(chunk: dict) -> np.ndarray:
    """Generate RGBA texture for a single chunk.

    The flat walkability array is stored as walk_array[x * 200 + y] = value at (x, y).
    For an image, we need img[row, col] = img[y, x] to display correctly.
    So we need to transpose: img[y, x] = walk[x, y]

    Returns (CELLS_PER_EDGE, CELLS_PER_EDGE, 4) uint8 array.
    """
    walkability = chunk.get('walkability', [])
    observed = chunk.get('observed', [])

    if not walkability:
        # Return gray texture for chunks without data
        return np.full((CELLS_PER_EDGE, CELLS_PER_EDGE, 4), COLOR_UNOBSERVED, dtype=np.uint8)

    # Convert to numpy arrays and reshape
    # After reshape: walk[x, y] = walkability at position (x, y)
    walk = np.array(walkability, dtype=np.uint8).reshape(CELLS_PER_EDGE, CELLS_PER_EDGE)
    obs = np.array(observed, dtype=bool).reshape(CELLS_PER_EDGE, CELLS_PER_EDGE) if observed else np.ones((CELLS_PER_EDGE, CELLS_PER_EDGE), dtype=bool)

    # Transpose so that img[y, x] corresponds to walk[x, y]
    walk = walk.T
    obs = obs.T

    # Create RGBA image - img[row, col] = img[y, x]
    rgba = np.zeros((CELLS_PER_EDGE, CELLS_PER_EDGE, 4), dtype=np.uint8)

    # Vectorized color assignment
    # Unobserved cells
    rgba[~obs] = COLOR_UNOBSERVED

    # Observed and walkable (walkability == 0)
    walkable_mask = obs & (walk == 0)
    rgba[walkable_mask] = COLOR_WALKABLE

    # Observed and blocked (walkability != 0)
    blocked_mask = obs & (walk != 0)
    rgba[blocked_mask] = COLOR_BLOCKED

    return rgba


def generate_world_texture(chunks: list, positions: dict, selected_idx: int = -1,
                           show_grid: bool = False, show_connections: bool = False,
                           show_path: bool = False, path_data: dict = None) -> tuple:
    """Generate a composite texture of all chunks.

    Returns (rgba_array, bounds, scale) where bounds is (min_x, min_y, max_x, max_y) in grid coords.
    """
    if not chunks or not positions:
        return np.zeros((100, 100, 4), dtype=np.uint8), (0, 0, 1, 1), 1.0

    # Calculate world bounds in grid coords
    xs = [pos[0] for pos in positions.values()]
    ys = [pos[1] for pos in positions.values()]
    min_gx, max_gx = min(xs), max(xs)
    min_gy, max_gy = min(ys), max(ys)

    grid_width = max_gx - min_gx + 1
    grid_height = max_gy - min_gy + 1

    # Use a fixed pixel size per chunk for the overview
    # Higher value = better quality but more memory
    PIXELS_PER_CHUNK = 100

    # Limit texture size to prevent memory issues
    max_dim = 4096
    tex_width = grid_width * PIXELS_PER_CHUNK
    tex_height = grid_height * PIXELS_PER_CHUNK

    scale = 1.0
    if tex_width > max_dim or tex_height > max_dim:
        scale = min(max_dim / tex_width, max_dim / tex_height)
        tex_width = int(tex_width * scale)
        tex_height = int(tex_height * scale)

    actual_chunk_size = int(PIXELS_PER_CHUNK * scale)

    world = np.full((tex_height, tex_width, 4), COLOR_BACKGROUND, dtype=np.uint8)

    # Draw each chunk
    for idx, chunk in enumerate(chunks):
        grid_id = chunk.get('gridId')
        if grid_id not in positions:
            continue

        gx, gy = positions[grid_id]
        px = int((gx - min_gx) * actual_chunk_size)
        py = int((gy - min_gy) * actual_chunk_size)

        # Generate chunk texture (200x200)
        chunk_tex = generate_chunk_texture(chunk)

        # Scale down to actual_chunk_size x actual_chunk_size
        # Use simple nearest-neighbor downsampling
        step_y = CELLS_PER_EDGE / actual_chunk_size
        step_x = CELLS_PER_EDGE / actual_chunk_size

        scaled_tex = np.zeros((actual_chunk_size, actual_chunk_size, 4), dtype=np.uint8)
        for sy in range(actual_chunk_size):
            for sx in range(actual_chunk_size):
                src_y = int(sy * step_y)
                src_x = int(sx * step_x)
                if src_y < CELLS_PER_EDGE and src_x < CELLS_PER_EDGE:
                    scaled_tex[sy, sx] = chunk_tex[src_y, src_x]

        # Blit to world texture
        if py + actual_chunk_size <= tex_height and px + actual_chunk_size <= tex_width:
            world[py:py+actual_chunk_size, px:px+actual_chunk_size] = scaled_tex

        # Draw grid lines (chunk borders)
        if show_grid and py + actual_chunk_size <= tex_height and px + actual_chunk_size <= tex_width:
            grid_color = (100, 100, 100, 255)  # Gray grid lines
            # Top edge
            world[py, px:px+actual_chunk_size] = grid_color
            # Left edge
            world[py:py+actual_chunk_size, px] = grid_color

        # Draw selection highlight (on top of grid)
        if idx == selected_idx:
            border_width = max(2, int(3 * scale))
            # Top
            world[py:py+border_width, px:px+actual_chunk_size] = COLOR_SELECTED
            # Bottom
            world[py+actual_chunk_size-border_width:py+actual_chunk_size, px:px+actual_chunk_size] = COLOR_SELECTED
            # Left
            world[py:py+actual_chunk_size, px:px+border_width] = COLOR_SELECTED
            # Right
            world[py:py+actual_chunk_size, px+actual_chunk_size-border_width:px+actual_chunk_size] = COLOR_SELECTED

    # Draw portal markers on world map
    portal_marker_size = max(1, actual_chunk_size // 35)  # Scale with chunk size (smaller)
    for chunk in chunks:
        grid_id = chunk.get('gridId')
        if grid_id not in positions:
            continue

        gx, gy = positions[grid_id]

        for portal in chunk.get('portals', []):
            local_x = portal.get('localX', 50)
            local_y = portal.get('localY', 50)

            # Convert to pixel coordinates
            px = int((gx - min_gx) * actual_chunk_size + (local_x / CHUNK_SIZE) * actual_chunk_size)
            py = int((gy - min_gy) * actual_chunk_size + (local_y / CHUNK_SIZE) * actual_chunk_size)

            portal_type = portal.get('type', 'UNKNOWN')
            color = PORTAL_COLORS.get(portal_type, PORTAL_COLORS['UNKNOWN'])

            # Draw portal marker
            for dy in range(-portal_marker_size, portal_marker_size + 1):
                for dx in range(-portal_marker_size, portal_marker_size + 1):
                    img_y, img_x = py + dy, px + dx
                    if 0 <= img_y < tex_height and 0 <= img_x < tex_width:
                        world[img_y, img_x] = color

    # Draw portal connections (arrows from portal to destination portal)
    if show_connections:
        connection_color = (100, 150, 255, 255)  # Light blue

        for chunk in chunks:
            grid_id = chunk.get('gridId')
            if grid_id not in positions:
                continue

            src_gx, src_gy = positions[grid_id]

            for portal in chunk.get('portals', []):
                connects_to = portal.get('connectsToGridId', -1)
                if connects_to == -1 or connects_to not in positions:
                    continue

                # Source: portal position within the chunk
                # Portal localX/localY are in tile coords (0-99), scale to chunk pixel position
                local_x = portal.get('localX', 50)
                local_y = portal.get('localY', 50)

                # Convert to pixel coordinates in the world texture
                src_px = int((src_gx - min_gx) * actual_chunk_size + (local_x / CHUNK_SIZE) * actual_chunk_size)
                src_py = int((src_gy - min_gy) * actual_chunk_size + (local_y / CHUNK_SIZE) * actual_chunk_size)

                # Target: use exit coordinates if available, otherwise center of chunk
                tgt_gx, tgt_gy = positions[connects_to]
                exit_x = portal.get('exitLocalX')
                exit_y = portal.get('exitLocalY')

                if exit_x is not None and exit_y is not None:
                    # Point to the destination portal
                    tgt_px = int((tgt_gx - min_gx) * actual_chunk_size + (exit_x / CHUNK_SIZE) * actual_chunk_size)
                    tgt_py = int((tgt_gy - min_gy) * actual_chunk_size + (exit_y / CHUNK_SIZE) * actual_chunk_size)
                else:
                    # Fallback to center of chunk if no exit coords
                    tgt_px = int((tgt_gx - min_gx) * actual_chunk_size + actual_chunk_size // 2)
                    tgt_py = int((tgt_gy - min_gy) * actual_chunk_size + actual_chunk_size // 2)

                # Draw arrow from source portal to destination portal
                draw_arrow(world, src_px, src_py, tgt_px, tgt_py, connection_color, thickness=1)

    # Draw path if enabled and path data exists
    if show_path and path_data:
        path_color = (255, 255, 0, 255)  # Yellow path
        start_color = (0, 255, 0, 255)   # Lime green start
        end_color = (255, 0, 0, 255)     # Red end
        waypoint_color = (255, 0, 255, 255)  # Magenta waypoints

        # Build gridId -> position mapping
        grid_to_pos = {c.get('gridId'): positions.get(c.get('gridId')) for c in chunks}

        # Collect all path points
        segments = path_data.get('segments', [])
        all_points = []

        for seg in segments:
            grid_id = seg.get('gridId')
            pos = grid_to_pos.get(grid_id)
            if not pos:
                continue

            gx, gy = pos
            steps = seg.get('steps', [])

            for step in steps:
                # localX/localY are tile coords (0-99), convert to pixel coords
                local_x = step.get('localX', 0)
                local_y = step.get('localY', 0)
                px = int((gx - min_gx) * actual_chunk_size + (local_x / CHUNK_SIZE) * actual_chunk_size)
                py = int((gy - min_gy) * actual_chunk_size + (local_y / CHUNK_SIZE) * actual_chunk_size)
                all_points.append((px, py))

        # Draw path lines
        if len(all_points) >= 2:
            for i in range(len(all_points) - 1):
                x0, y0 = all_points[i]
                x1, y1 = all_points[i + 1]
                draw_line(world, x0, y0, x1, y1, path_color, thickness=2)

            # Mark start point (lime green)
            sx, sy = all_points[0]
            marker_size = max(3, actual_chunk_size // 15)
            for dy in range(-marker_size, marker_size + 1):
                for dx in range(-marker_size, marker_size + 1):
                    if dx*dx + dy*dy <= marker_size*marker_size:  # Circle
                        img_y, img_x = sy + dy, sx + dx
                        if 0 <= img_y < tex_height and 0 <= img_x < tex_width:
                            world[img_y, img_x] = start_color

            # Mark end point (red)
            ex, ey = all_points[-1]
            for dy in range(-marker_size, marker_size + 1):
                for dx in range(-marker_size, marker_size + 1):
                    if dx*dx + dy*dy <= marker_size*marker_size:  # Circle
                        img_y, img_x = ey + dy, ex + dx
                        if 0 <= img_y < tex_height and 0 <= img_x < tex_width:
                            world[img_y, img_x] = end_color

        # Draw waypoints
        waypoints = path_data.get('waypoints', [])
        for wp in waypoints:
            grid_id = wp.get('gridId')
            pos = grid_to_pos.get(grid_id)
            if not pos:
                continue

            gx, gy = pos
            local_x = wp.get('localX', 50)
            local_y = wp.get('localY', 50)
            wp_type = wp.get('type', 'WALK')

            px = int((gx - min_gx) * actual_chunk_size + (local_x / CHUNK_SIZE) * actual_chunk_size)
            py = int((gy - min_gy) * actual_chunk_size + (local_y / CHUNK_SIZE) * actual_chunk_size)

            # Draw waypoint marker (diamond shape for portal entries)
            if wp_type in ('PORTAL_ENTRY', 'DESTINATION'):
                wp_size = max(4, actual_chunk_size // 12)
                for d in range(wp_size + 1):
                    for dx in range(-d, d + 1):
                        dy1, dy2 = wp_size - d, -(wp_size - d)
                        for dy in [dy1, dy2]:
                            img_y, img_x = py + dy, px + dx
                            if 0 <= img_y < tex_height and 0 <= img_x < tex_width:
                                world[img_y, img_x] = waypoint_color

    return world, (min_gx, min_gy, max_gx, max_gy), actual_chunk_size


# =============================================================================
# UI Creation
# =============================================================================

def create_ui():
    """Create the Dear PyGui UI layout."""
    dpg.create_context()

    # Create texture registry (textures will be added dynamically)
    dpg.add_texture_registry(tag="texture_registry")

    with dpg.window(label="ChunkNav Visualizer", tag="main_window"):
        with dpg.group(horizontal=True):
            # Left sidebar - Settings
            with dpg.child_window(width=200, tag="settings_panel"):
                dpg.add_text("SETTINGS", color=(200, 200, 200))
                dpg.add_separator()

                dpg.add_text("Profile:")
                dpg.add_combo([], tag="profile_combo", callback=on_profile_change, width=-1)

                dpg.add_spacer(height=10)
                dpg.add_text("Layer:")
                dpg.add_combo(['surface'], tag="layer_combo", callback=on_layer_change,
                             default_value='surface', width=-1)

                dpg.add_spacer(height=15)
                dpg.add_separator()
                dpg.add_text("DISPLAY", color=(200, 200, 200))
                dpg.add_separator()

                dpg.add_checkbox(label="Walkability", default_value=True,
                                tag="show_walkability", callback=on_display_toggle)
                dpg.add_checkbox(label="Portals", default_value=True,
                                tag="show_portals", callback=on_display_toggle)
                dpg.add_checkbox(label="Connections", default_value=True,
                                tag="show_connections", callback=on_display_toggle)
                dpg.add_checkbox(label="Path", default_value=False,
                                tag="show_path", callback=on_display_toggle)
                dpg.add_checkbox(label="Chunk borders", default_value=True,
                                tag="show_grid", callback=on_display_toggle)
                dpg.add_checkbox(label="Cell lines", default_value=False,
                                tag="show_cell_lines", callback=on_display_toggle)

                dpg.add_spacer(height=15)
                dpg.add_separator()
                dpg.add_text("SELECTED CHUNK", color=(200, 200, 200))
                dpg.add_separator()

                dpg.add_text("Grid ID: -", tag="selected_grid_id")
                dpg.add_text("Layer: -", tag="selected_layer")
                dpg.add_text("Portals: -", tag="selected_portals")
                dpg.add_text("Observed: -", tag="selected_observed")

                dpg.add_spacer(height=15)
                dpg.add_separator()

                dpg.add_button(label="Reload Data", callback=on_reload, width=-1)
                dpg.add_button(label="Export PNG", callback=on_export, width=-1)

            # Right side - Map and Detail views SIDE BY SIDE (equal width, responsive)
            with dpg.table(header_row=False, resizable=True, policy=dpg.mvTable_SizingStretchProp,
                          borders_innerV=True, tag="main_table"):
                dpg.add_table_column(init_width_or_weight=1.0)
                dpg.add_table_column(init_width_or_weight=1.0)

                with dpg.table_row():
                    # World map panel
                    with dpg.group():
                        with dpg.group(horizontal=True):
                            dpg.add_text("WORLD MAP")
                            dpg.add_button(label="+", callback=on_zoom_in, tag="world_zoom_in", width=30)
                            dpg.add_button(label="-", callback=on_zoom_out, tag="world_zoom_out", width=30)
                            dpg.add_button(label="Fit", callback=on_zoom_fit, width=40)

                        with dpg.child_window(width=-1, height=-30, tag="map_container", border=True,
                                              horizontal_scrollbar=True):
                            dpg.add_text("Loading...", tag="world_placeholder")

                    # Detail view panel
                    with dpg.group():
                        with dpg.group(horizontal=True):
                            dpg.add_text("CHUNK DETAIL")
                            dpg.add_button(label="+", callback=on_detail_zoom_in, tag="detail_zoom_in", width=30)
                            dpg.add_button(label="-", callback=on_detail_zoom_out, tag="detail_zoom_out", width=30)
                            dpg.add_button(label="Fit", callback=on_detail_zoom_fit, width=40)

                        with dpg.child_window(width=-1, height=-30, tag="detail_container", border=True,
                                              horizontal_scrollbar=True):
                            dpg.add_text("Select a chunk", tag="detail_placeholder")

        # Status bar
        dpg.add_separator()
        dpg.add_text("Status: Ready | Chunks: 0 | Layer: surface | Zoom: 100%", tag="status_bar")

    # Register click handler for map
    with dpg.item_handler_registry(tag="map_click_handler"):
        dpg.add_item_clicked_handler(callback=on_map_clicked)

    dpg.create_viewport(title="ChunkNav Visualizer", width=1200, height=900)
    dpg.setup_dearpygui()
    dpg.set_primary_window("main_window", True)


# =============================================================================
# Event Handlers
# =============================================================================

def on_profile_change(sender, app_data):
    """Handle profile selection change."""
    global state
    profile_name = app_data

    # Find profile index
    for idx, (path, name, count) in enumerate(state.profiles):
        if f"{name} ({count} chunks)" == profile_name or name == profile_name:
            state.current_profile_idx = idx
            load_current_profile()
            break


def on_layer_change(sender, app_data):
    """Handle layer selection change."""
    global state
    state.current_layer = app_data
    apply_layer_filter()
    update_world_texture()
    update_status_bar()


def on_display_toggle(sender, app_data):
    """Handle display option toggles."""
    global state
    tag = dpg.get_item_alias(sender) or sender

    if tag == "show_walkability":
        state.show_walkability = app_data
    elif tag == "show_portals":
        state.show_portals = app_data
        update_world_texture()
        update_detail_texture()
        return
    elif tag == "show_path":
        state.show_path = app_data
        update_world_texture()
        update_detail_texture()
        return
    elif tag == "show_grid":
        state.show_grid = app_data
    elif tag == "show_connections":
        state.show_connections = app_data
    elif tag == "show_cell_lines":
        state.show_cell_lines = app_data
        update_detail_texture()
        return

    update_world_texture()


def on_reload(sender=None, app_data=None):
    """Reload data from disk."""
    load_current_profile()


def on_export(sender=None, app_data=None):
    """Export current view to PNG."""
    # TODO: Implement file dialog and PNG export
    print("Export not yet implemented")


def on_zoom_in(sender=None, app_data=None):
    """Zoom in the map."""
    global state
    state.zoom_level = min(5.0, state.zoom_level * 1.5)
    update_world_image_size()
    update_status_bar()


def on_zoom_out(sender=None, app_data=None):
    """Zoom out the map."""
    global state
    state.zoom_level = max(0.2, state.zoom_level / 1.5)
    update_world_image_size()
    update_status_bar()


def on_zoom_fit(sender=None, app_data=None):
    """Reset zoom to fit."""
    global state
    state.zoom_level = 1.0
    update_world_image_size()
    update_status_bar()


def on_detail_zoom_in(sender=None, app_data=None):
    """Zoom in the detail view."""
    global state
    state.detail_zoom_level = min(5.0, state.detail_zoom_level * 1.5)
    update_detail_texture()


def on_detail_zoom_out(sender=None, app_data=None):
    """Zoom out the detail view."""
    global state
    state.detail_zoom_level = max(0.5, state.detail_zoom_level / 1.5)
    update_detail_texture()


def on_detail_zoom_fit(sender=None, app_data=None):
    """Reset detail zoom to default."""
    global state
    state.detail_zoom_level = 2.0
    update_detail_texture()


def on_map_clicked(sender, app_data):
    """Handle click on the map image to select a chunk."""
    global state

    # Get mouse position (global screen coordinates)
    mouse_pos = dpg.get_mouse_pos(local=False)

    if not dpg.does_item_exist("world_image"):
        return

    try:
        # Get the absolute screen position of the image using rect_min
        # This gives the top-left corner in screen coordinates
        img_rect_min = dpg.get_item_rect_min("world_image")

        # Calculate position relative to the image
        rel_x = mouse_pos[0] - img_rect_min[0]
        rel_y = mouse_pos[1] - img_rect_min[1]

        # Account for zoom (convert from display coordinates to texture coordinates)
        tex_x = rel_x / state.zoom_level
        tex_y = rel_y / state.zoom_level

        # Check bounds
        if tex_x < 0 or tex_y < 0:
            return
        if tex_x >= state.world_texture_width or tex_y >= state.world_texture_height:
            return

        # Find chunk at this position
        select_chunk_at(tex_x, tex_y)
    except Exception as e:
        print(f"Click handling error: {e}")


# =============================================================================
# Helper Functions
# =============================================================================

def load_current_profile():
    """Load chunks from the currently selected profile."""
    global state

    if not state.profiles:
        return

    chunknav_dir, profile_id, _ = state.profiles[state.current_profile_idx]
    print(f"Loading profile: {profile_id}")

    # Load chunks
    data = load_all_chunks_binary(chunknav_dir)
    state.chunks = data.get('chunks', [])

    # Load path data
    state.path_data = load_path_data(chunknav_dir)
    if state.path_data:
        print(f"Loaded path to: {state.path_data.get('targetArea', 'unknown')}")

    # Get available layers
    state.available_layers = get_available_layers(data)
    if state.available_layers:
        dpg.configure_item("layer_combo", items=['all'] + state.available_layers)
        if state.current_layer not in state.available_layers and state.current_layer != 'all':
            state.current_layer = state.available_layers[0]
            dpg.set_value("layer_combo", state.current_layer)

    # Apply filter and build positions
    apply_layer_filter()

    # Update textures
    update_world_texture()
    update_detail_texture()
    update_status_bar()


def apply_layer_filter():
    """Apply current layer filter and rebuild positions."""
    global state

    state.filtered_chunks = filter_chunks_by_layer(state.chunks, state.current_layer)

    if state.current_layer == 'all':
        state.positions, state.layer_bounds = build_grid_positions_by_layer(state.chunks)
    else:
        state.positions = build_grid_positions_from_neighbors(state.filtered_chunks)
        state.layer_bounds = {}

    # Reset selection if it's out of bounds
    if state.selected_chunk_idx >= len(state.filtered_chunks):
        state.selected_chunk_idx = -1


def select_chunk_at(pixel_x: float, pixel_y: float):
    """Select the chunk at the given pixel coordinates (in texture space, before zoom)."""
    global state

    if not state.filtered_chunks or not state.positions:
        return

    # Get bounds
    min_gx, min_gy, max_gx, max_gy = state.world_bounds
    chunk_size = state.chunk_pixel_size

    # Convert pixel position to grid position
    grid_x = int(pixel_x / chunk_size) + min_gx
    grid_y = int(pixel_y / chunk_size) + min_gy

    # Find chunk at this grid position
    for idx, chunk in enumerate(state.filtered_chunks):
        grid_id = chunk.get('gridId')
        if grid_id not in state.positions:
            continue

        gx, gy = state.positions[grid_id]
        if gx == grid_x and gy == grid_y:
            state.selected_chunk_idx = idx
            update_selected_chunk_info()
            update_world_texture()
            update_detail_texture()
            return

    print(f"No chunk found at grid ({grid_x}, {grid_y})")


def update_selected_chunk_info():
    """Update the selected chunk info in the sidebar."""
    global state

    if state.selected_chunk_idx < 0 or state.selected_chunk_idx >= len(state.filtered_chunks):
        dpg.set_value("selected_grid_id", "Grid ID: -")
        dpg.set_value("selected_layer", "Layer: -")
        dpg.set_value("selected_portals", "Portals: -")
        dpg.set_value("selected_observed", "Observed: -")
        return

    chunk = state.filtered_chunks[state.selected_chunk_idx]
    grid_id = chunk.get('gridId', 0)
    layer = chunk.get('layer', 'surface')
    portals = chunk.get('portals', [])
    observed = chunk.get('observed', [])

    obs_count = sum(1 for o in observed if o) if observed else 0
    obs_pct = int(100 * obs_count / TOTAL_CELLS) if observed else 100

    dpg.set_value("selected_grid_id", f"Grid ID: {grid_id}")
    dpg.set_value("selected_layer", f"Layer: {layer}")
    dpg.set_value("selected_portals", f"Portals: {len(portals)}")
    dpg.set_value("selected_observed", f"Observed: {obs_pct}%")


def update_world_texture():
    """Regenerate and update the world map texture."""
    global state

    if not state.filtered_chunks:
        return

    # Generate world texture
    world_rgba, bounds, chunk_pixel_size = generate_world_texture(
        state.filtered_chunks, state.positions, state.selected_chunk_idx,
        state.show_grid, state.show_connections, state.show_path, state.path_data
    )
    state.world_bounds = bounds
    state.chunk_pixel_size = chunk_pixel_size

    # Convert to float32 for DPG
    height, width = world_rgba.shape[:2]
    state.world_texture_width = width
    state.world_texture_height = height
    texture_data = (world_rgba.astype(np.float32) / 255.0).flatten()

    # Delete old texture if exists
    if state.current_world_tag and dpg.does_item_exist(state.current_world_tag):
        dpg.delete_item(state.current_world_tag)

    # Create new texture with unique tag
    state.world_texture_counter += 1
    new_tag = f"world_tex_{state.world_texture_counter}"
    state.current_world_tag = new_tag

    dpg.add_raw_texture(width, height, texture_data,
                       format=dpg.mvFormat_Float_rgba, tag=new_tag,
                       parent="texture_registry")

    # Remove placeholder and create/update image
    if dpg.does_item_exist("world_placeholder"):
        dpg.delete_item("world_placeholder")

    # Calculate display size with zoom
    display_width = int(width * state.zoom_level)
    display_height = int(height * state.zoom_level)

    if dpg.does_item_exist("world_image"):
        dpg.configure_item("world_image", texture_tag=new_tag,
                          width=display_width, height=display_height)
    else:
        dpg.add_image(new_tag, tag="world_image", parent="map_container",
                     width=display_width, height=display_height)

    # Bind click handler to the image
    if dpg.does_item_exist("map_click_handler"):
        dpg.bind_item_handler_registry("world_image", "map_click_handler")

    # Mark texture as clean
    state.world_texture_dirty = False


def update_world_image_size():
    """Update only the display size of the world image (fast, no regeneration)."""
    global state

    if not dpg.does_item_exist("world_image"):
        return

    # Calculate display size with zoom
    display_width = int(state.world_texture_width * state.zoom_level)
    display_height = int(state.world_texture_height * state.zoom_level)

    dpg.configure_item("world_image", width=display_width, height=display_height)


def update_detail_texture():
    """Update the detail view texture for selected chunk.

    Generates a high-resolution texture at display size with:
    - Each cell rendered as multiple pixels based on zoom level
    - Grid lines between cells when enabled
    - Portal markers at correct positions
    """
    global state

    # Calculate texture size based on zoom (render at actual display resolution)
    pixels_per_cell = max(1, int(state.detail_zoom_level))
    tex_size = CELLS_PER_EDGE * pixels_per_cell

    if state.selected_chunk_idx < 0 or state.selected_chunk_idx >= len(state.filtered_chunks):
        # Show empty texture
        detail_rgba = np.full((tex_size, tex_size, 4), COLOR_BACKGROUND, dtype=np.uint8)
    else:
        chunk = state.filtered_chunks[state.selected_chunk_idx]
        # Get base 200x200 chunk texture
        chunk_rgba = generate_chunk_texture(chunk)

        # Create high-res texture by scaling up each cell
        detail_rgba = np.zeros((tex_size, tex_size, 4), dtype=np.uint8)

        for cy in range(CELLS_PER_EDGE):
            for cx in range(CELLS_PER_EDGE):
                # Get color for this cell
                color = chunk_rgba[cy, cx]
                # Fill the corresponding pixels in the high-res texture
                py = cy * pixels_per_cell
                px = cx * pixels_per_cell
                detail_rgba[py:py+pixels_per_cell, px:px+pixels_per_cell] = color

        # Draw grid lines between cells (if enabled and zoom > 1)
        if state.show_cell_lines and pixels_per_cell > 1:
            grid_color = (60, 60, 60, 255)  # Dark gray grid lines
            for i in range(CELLS_PER_EDGE + 1):
                pos = i * pixels_per_cell
                if pos < tex_size:
                    # Horizontal line
                    detail_rgba[pos, :] = grid_color
                    # Vertical line
                    detail_rgba[:, pos] = grid_color

        # Draw portals on detail view
        # Portal localX/localY are in TILE coordinates (0-99), not cell coordinates
        # Need to scale by 2 to convert to cell coordinates (0-199)
        if state.show_portals:
            for portal in chunk.get('portals', []):
                tile_x = portal.get('localX', 0)
                tile_y = portal.get('localY', 0)
                # Convert tile coords to cell coords, then to pixel coords
                cell_x = tile_x * 2
                cell_y = tile_y * 2
                pixel_x = cell_x * pixels_per_cell + pixels_per_cell // 2
                pixel_y = cell_y * pixels_per_cell + pixels_per_cell // 2

                portal_type = portal.get('type', 'UNKNOWN')
                color = PORTAL_COLORS.get(portal_type, PORTAL_COLORS['UNKNOWN'])

                # Draw a small marker (scales with zoom)
                marker_size = max(4, pixels_per_cell * 2)
                half = marker_size // 2
                for dy in range(-half, half + 1):
                    for dx in range(-half, half + 1):
                        img_row = pixel_y + dy
                        img_col = pixel_x + dx
                        if 0 <= img_row < tex_size and 0 <= img_col < tex_size:
                            detail_rgba[img_row, img_col] = color

        # Draw path segments that pass through this chunk
        if state.show_path and state.path_data:
            chunk_grid_id = chunk.get('gridId')
            path_color = (255, 255, 0, 255)  # Yellow
            start_color = (0, 255, 0, 255)   # Lime green
            end_color = (255, 165, 0, 255)   # Orange
            waypoint_color = (255, 0, 255, 255)  # Magenta

            segments = state.path_data.get('segments', [])
            for seg in segments:
                if seg.get('gridId') != chunk_grid_id:
                    continue

                steps = seg.get('steps', [])
                if len(steps) >= 2:
                    # Convert steps to pixel coordinates
                    # localX/localY are tile coords (0-99), need to convert to cell coords (*2) then to pixels
                    points = []
                    for step in steps:
                        tile_x = step.get('localX', 0)
                        tile_y = step.get('localY', 0)
                        cell_x = tile_x * 2
                        cell_y = tile_y * 2
                        pixel_x = cell_x * pixels_per_cell + pixels_per_cell // 2
                        pixel_y = cell_y * pixels_per_cell + pixels_per_cell // 2
                        points.append((pixel_x, pixel_y))

                    # Draw path lines
                    for i in range(len(points) - 1):
                        x0, y0 = points[i]
                        x1, y1 = points[i + 1]
                        draw_line(detail_rgba, x0, y0, x1, y1, path_color, thickness=max(2, pixels_per_cell))

                    # Mark start of segment (lime green circle)
                    sx, sy = points[0]
                    marker_r = max(4, pixels_per_cell * 2)
                    for dy in range(-marker_r, marker_r + 1):
                        for dx in range(-marker_r, marker_r + 1):
                            if dx*dx + dy*dy <= marker_r*marker_r:
                                img_y, img_x = sy + dy, sx + dx
                                if 0 <= img_y < tex_size and 0 <= img_x < tex_size:
                                    detail_rgba[img_y, img_x] = start_color

                    # Mark end of segment (orange circle)
                    ex, ey = points[-1]
                    for dy in range(-marker_r, marker_r + 1):
                        for dx in range(-marker_r, marker_r + 1):
                            if dx*dx + dy*dy <= marker_r*marker_r:
                                img_y, img_x = ey + dy, ex + dx
                                if 0 <= img_y < tex_size and 0 <= img_x < tex_size:
                                    detail_rgba[img_y, img_x] = end_color

            # Draw waypoints in this chunk
            waypoints = state.path_data.get('waypoints', [])
            for wp in waypoints:
                if wp.get('gridId') != chunk_grid_id:
                    continue

                tile_x = wp.get('localX', 50)
                tile_y = wp.get('localY', 50)
                wp_type = wp.get('type', 'WALK')

                cell_x = tile_x * 2
                cell_y = tile_y * 2
                pixel_x = cell_x * pixels_per_cell + pixels_per_cell // 2
                pixel_y = cell_y * pixels_per_cell + pixels_per_cell // 2

                if wp_type in ('PORTAL_ENTRY', 'DESTINATION'):
                    # Draw diamond marker
                    wp_size = max(6, pixels_per_cell * 3)
                    for d in range(wp_size + 1):
                        for dx in range(-d, d + 1):
                            dy1, dy2 = wp_size - d, -(wp_size - d)
                            for dy in [dy1, dy2]:
                                img_y, img_x = pixel_y + dy, pixel_x + dx
                                if 0 <= img_y < tex_size and 0 <= img_x < tex_size:
                                    detail_rgba[img_y, img_x] = waypoint_color

    # Convert to float32 for DPG
    texture_data = (detail_rgba.astype(np.float32) / 255.0).flatten()

    # Delete old texture if exists
    if state.current_detail_tag and dpg.does_item_exist(state.current_detail_tag):
        dpg.delete_item(state.current_detail_tag)

    # Create new texture with unique tag
    state.detail_texture_counter += 1
    new_tag = f"detail_tex_{state.detail_texture_counter}"
    state.current_detail_tag = new_tag

    dpg.add_raw_texture(tex_size, tex_size, texture_data,
                       format=dpg.mvFormat_Float_rgba, tag=new_tag,
                       parent="texture_registry")

    # Remove placeholder and create/update image
    if dpg.does_item_exist("detail_placeholder"):
        dpg.delete_item("detail_placeholder")

    if dpg.does_item_exist("detail_image"):
        dpg.configure_item("detail_image", texture_tag=new_tag,
                          width=tex_size, height=tex_size)
    else:
        dpg.add_image(new_tag, tag="detail_image", parent="detail_container",
                     width=tex_size, height=tex_size)


def update_status_bar():
    """Update the status bar text."""
    global state

    chunk_count = len(state.filtered_chunks)
    total_count = len(state.chunks)
    layer = state.current_layer
    zoom_pct = int(state.zoom_level * 100)

    status = f"Status: Ready | Chunks: {chunk_count}/{total_count} | Layer: {layer} | Zoom: {zoom_pct}%"
    dpg.set_value("status_bar", status)


def initialize():
    """Initialize the application."""
    global state

    # Discover profiles
    state.profiles = discover_profiles()
    print(f"Found {len(state.profiles)} profile(s) with chunks")

    if state.profiles:
        # Update profile combo
        profile_names = [f"{name} ({count} chunks)" for _, name, count in state.profiles]
        dpg.configure_item("profile_combo", items=profile_names, default_value=profile_names[0])

        # Load first profile
        load_current_profile()
    else:
        dpg.set_value("status_bar", "Status: No profiles found. Check Haven & Hearth data directory.")


# =============================================================================
# Main Entry Point
# =============================================================================

def main():
    """Main entry point."""
    create_ui()
    initialize()

    dpg.show_viewport()
    dpg.start_dearpygui()
    dpg.destroy_context()


if __name__ == '__main__':
    main()
