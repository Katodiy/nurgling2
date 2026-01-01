#!/usr/bin/env python3
"""
ChunkNav Visualizer - Creates a visual representation of the chunknav navigation data.

Usage:
    python chunknav_visualizer.py [path_to_chunknav.json] [--layer LAYER] [--path] [-w WORLD]

Options:
    --layer LAYER    Filter by layer (surface, inside, cellar, mine, etc.)
                     Default: surface
    --path           Show the last calculated path (from chunknav_path.json)
    -w, --world      Select world profile (1 or 2)
                     1 = b7c199a4557503a8
                     2 = c646473983afec09

If no path is provided, it will look in the default location:
    %APPDATA%/Haven and Hearth/profiles/*/chunknav.nurgling.json
"""

import json
import sys
import os
from pathlib import Path

# Try to import visualization libraries
try:
    import matplotlib.pyplot as plt
    import matplotlib.patches as patches
    from matplotlib.collections import PatchCollection
    import numpy as np
    HAS_MATPLOTLIB = True
except ImportError:
    HAS_MATPLOTLIB = False
    print("Warning: matplotlib not installed. Install with: pip install matplotlib")
    print("Falling back to ASCII visualization.\n")


# Constants matching ChunkNavConfig.java
CHUNK_SIZE = 100  # Tiles per chunk (unchanged)
# CELLS_PER_EDGE is now determined per-chunk based on data version:
# Version 1: 100x100 (tile-level)
# Version 2: 200x200 (half-tile level)


def get_cells_per_edge(chunk):
    """Determine grid size from chunk data version or array length."""
    version = chunk.get('version', 1)
    if version >= 2:
        return 200
    # Fallback: infer from walkability array length
    walkability = chunk.get('walkability', [])
    if walkability:
        # sqrt of array length gives cells per edge
        import math
        size = int(math.sqrt(len(walkability)))
        if size * size == len(walkability):
            return size
    return 100  # Default to v1


def find_chunknav_files(world=None):
    """Find chunknav.nurgling.json files in default locations.

    Args:
        world: Optional world number (1 or 2) to select specific profile
    """
    files = []

    # Known world profiles
    WORLD_PROFILES = {
        1: "b7c199a4557503a8",
        2: "c646473983afec09",
    }

    # If world is specified, use that specific profile only - no fallback
    if world and world in WORLD_PROFILES:
        profile_id = WORLD_PROFILES[world]
        hardcoded_paths = [
            Path(rf"C:\Users\imbecil\AppData\Roaming\Haven and Hearth\profiles\{profile_id}\chunknav.nurgling.json"),
            Path(f"/mnt/c/Users/imbecil/AppData/Roaming/Haven and Hearth/profiles/{profile_id}/chunknav.nurgling.json"),
        ]
        for p in hardcoded_paths:
            if p.exists():
                files.append(p)
                return files
        # World specified but file not found - return empty, don't fall back to other worlds
        print(f"Error: World {world} profile ({profile_id}) chunknav file not found")
        return files

    # No world specified - scan all profiles
    # Fallback: Windows AppData location
    appdata = os.environ.get('APPDATA', '')
    if appdata:
        hh_path = Path(appdata) / "Haven and Hearth" / "profiles"
        if hh_path.exists():
            for profile_dir in hh_path.iterdir():
                if profile_dir.is_dir():
                    chunknav_file = profile_dir / "chunknav.nurgling.json"
                    if chunknav_file.exists():
                        files.append(chunknav_file)

    # Also check WSL path
    wsl_path = Path("/mnt/c/Users")
    if wsl_path.exists():
        for user_dir in wsl_path.iterdir():
            hh_path = user_dir / "AppData" / "Roaming" / "Haven and Hearth" / "profiles"
            if hh_path.exists():
                for profile_dir in hh_path.iterdir():
                    if profile_dir.is_dir():
                        chunknav_file = profile_dir / "chunknav.nurgling.json"
                        if chunknav_file.exists() and chunknav_file not in files:
                            files.append(chunknav_file)

    return files


def load_chunknav_data(filepath):
    """Load and parse the chunknav JSON file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        return json.load(f)


def load_path_data(chunknav_filepath):
    """Load the path visualization data if it exists."""
    path_file = Path(chunknav_filepath).parent / "chunknav_path.json"
    if path_file.exists():
        with open(path_file, 'r', encoding='utf-8') as f:
            return json.load(f)
    return None


def parse_walkability(walk_array, cells_per_edge):
    """Parse flat walkability array into grid."""
    grid = []
    idx = 0
    for x in range(cells_per_edge):
        row = []
        for y in range(cells_per_edge):
            row.append(walk_array[idx])
            idx += 1
        grid.append(row)
    return grid


def parse_observed(obs_array, cells_per_edge):
    """Parse flat observed array into grid."""
    if not obs_array:
        return [[True] * cells_per_edge for _ in range(cells_per_edge)]
    grid = []
    idx = 0
    for x in range(cells_per_edge):
        row = []
        for y in range(cells_per_edge):
            row.append(obs_array[idx] != 0)
            idx += 1
        grid.append(row)
    return grid


def get_available_layers(data):
    """Get all unique layers from the chunk data."""
    layers = set()
    for chunk in data.get('chunks', []):
        layer = chunk.get('layer', 'surface')
        layers.add(layer)
    return sorted(layers)


def filter_chunks_by_layer(chunks, layer_filter):
    """Filter chunks to only include those in the specified layer."""
    if layer_filter is None or layer_filter == 'all':
        return chunks
    return [c for c in chunks if c.get('layer', 'surface') == layer_filter]


def build_grid_positions_from_neighbors(chunks):
    """
    Build grid positions by traversing neighbor relationships.
    Returns a dict of gridId -> (x, y) positions.
    This creates a consistent coordinate system from the neighbor graph.
    Handles disconnected components by placing them offset from each other.
    """
    if not chunks:
        return {}

    # Build lookup by gridId
    chunks_by_id = {c['gridId']: c for c in chunks}
    all_chunk_ids = set(chunks_by_id.keys())

    positions = {}
    component_offset_x = 0  # Offset for disconnected components

    # Process all chunks, handling disconnected components
    while len(positions) < len(chunks):
        # Find an unpositioned chunk to start a new component
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

            # Check all neighbors
            neighbors = [
                ('neighborNorth', 0, -1),
                ('neighborSouth', 0, 1),
                ('neighborEast', 1, 0),
                ('neighborWest', -1, 0),
            ]

            for key, dx, dy in neighbors:
                neighbor_id = chunk.get(key, -1)
                # Only process neighbors that are in our chunk set and not yet positioned
                if neighbor_id != -1 and neighbor_id in chunks_by_id and neighbor_id not in positions:
                    positions[neighbor_id] = (cx + dx, cy + dy)
                    queue.append(neighbor_id)

        # Next component starts after this one with a gap
        component_offset_x = component_max_x + 3

    return positions


def build_grid_positions_by_layer(chunks):
    """
    Build grid positions for all chunks, organizing by layer.
    Each layer gets its own coordinate space, stacked vertically.
    Returns a dict of gridId -> (x, y) positions with layers offset.
    Also returns layer_bounds dict with {layer: (min_y, max_y)} for drawing labels.
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

    # Define layer order (surface at top, then inside, cellar, then mine levels)
    layer_order = ['surface', 'inside', 'cellar']
    # Add mine levels in order (mine1, mine2, mine3, ...)
    mine_layers = sorted([l for l in chunks_by_layer.keys() if l.startswith('mine')],
                         key=lambda x: int(x[4:]) if x[4:].isdigit() else 0)
    layer_order.extend(mine_layers)
    # Add any other layers not in the predefined order
    for layer in chunks_by_layer.keys():
        if layer not in layer_order:
            layer_order.append(layer)

    all_positions = {}
    layer_bounds = {}
    current_y_offset = 0
    layer_gap = 5  # Gap between layers in grid units

    for layer in layer_order:
        if layer not in chunks_by_layer:
            continue

        layer_chunks = chunks_by_layer[layer]
        # Build positions for this layer independently
        layer_positions = build_grid_positions_from_neighbors(layer_chunks)

        if not layer_positions:
            continue

        # Find bounds of this layer
        ys = [pos[1] for pos in layer_positions.values()]
        min_y = min(ys)
        max_y = max(ys)
        layer_height = max_y - min_y + 1

        # Offset all positions for this layer
        for grid_id, (x, y) in layer_positions.items():
            # Shift y so layer starts at current_y_offset, with min_y normalized to 0
            adjusted_y = current_y_offset + (y - min_y)
            all_positions[grid_id] = (x, adjusted_y)

        # Record layer bounds for labeling
        layer_bounds[layer] = (current_y_offset, current_y_offset + layer_height - 1)

        # Move offset for next layer
        current_y_offset += layer_height + layer_gap

    return all_positions, layer_bounds


def get_chunk_grid_coord(chunk, neighbor_positions=None):
    """Get grid coordinates for a chunk."""
    # Use neighbor-based positions if available
    if neighbor_positions and chunk['gridId'] in neighbor_positions:
        return neighbor_positions[chunk['gridId']]
    # Fall back to session-based worldTileOrigin (only works within same session)
    if 'worldTileOriginX' in chunk and 'worldTileOriginY' in chunk:
        return (chunk['worldTileOriginX'] // CHUNK_SIZE, chunk['worldTileOriginY'] // CHUNK_SIZE)
    return None


def visualize_ascii(data, layer_filter='surface'):
    """Create ASCII visualization of the chunk map."""
    all_chunks = data.get('chunks', [])
    chunks = filter_chunks_by_layer(all_chunks, layer_filter)

    if not chunks:
        print(f"No chunks found for layer '{layer_filter}'.")
        return

    available_layers = get_available_layers(data)
    print(f"\n=== ChunkNav Data Summary ===")
    print(f"World: {data.get('genus', 'unknown')}")
    print(f"Total chunks: {len(all_chunks)} (showing {len(chunks)} in layer '{layer_filter}')")
    print(f"Available layers: {', '.join(available_layers)}")
    print(f"Last saved: {data.get('lastSaved', 'unknown')}")
    print()

    # Check how many chunks have neighbor data vs session-based coords
    has_neighbors = sum(1 for c in chunks if any(c.get(k, -1) != -1 for k in ['neighborNorth', 'neighborSouth', 'neighborEast', 'neighborWest']))
    has_session = sum(1 for c in chunks if 'worldTileOriginX' in c and 'worldTileOriginY' in c)
    print(f"Coordinates: {has_neighbors} with neighbor relationships, {has_session} with session-based worldTileOrigin")

    # Build positions from neighbor relationships
    neighbor_positions = build_grid_positions_from_neighbors(chunks)
    print(f"Positioned {len(neighbor_positions)} chunks via neighbor graph")

    # Find bounds
    min_x = min_y = float('inf')
    max_x = max_y = float('-inf')

    chunk_map = {}
    for chunk in chunks:
        coord = get_chunk_grid_coord(chunk, neighbor_positions)
        if coord:
            x, y = coord
            min_x = min(min_x, x)
            min_y = min(min_y, y)
            max_x = max(max_x, x)
            max_y = max(max_y, y)
            chunk_map[(x, y)] = chunk

    if not chunk_map:
        print("No chunks with world coordinates found.")
        return

    print(f"Grid bounds: ({min_x}, {min_y}) to ({max_x}, {max_y})")
    print()

    # Print chunk grid
    print("=== Chunk Grid ===")
    print("Legend: # = chunk with portals, O = chunk without portals, . = empty")
    print()

    for y in range(int(min_y), int(max_y) + 1):
        row = ""
        for x in range(int(min_x), int(max_x) + 1):
            if (x, y) in chunk_map:
                chunk = chunk_map[(x, y)]
                portals = chunk.get('portals', [])
                if portals:
                    row += "# "
                else:
                    row += "O "
            else:
                row += ". "
        print(row)

    print()

    # Print detailed chunk info
    print("=== Chunk Details ===")
    for chunk in chunks:
        grid_id = chunk.get('gridId', 'unknown')
        portals = chunk.get('portals', [])
        connected = chunk.get('connectedChunks', [])

        # Calculate walkability stats
        walkability = chunk.get('walkability', [])
        observed = chunk.get('observed', [])

        if walkability:
            total_cells = len(walkability)
            walkable = sum(1 for w in walkability if w == 0)
            partial = sum(1 for w in walkability if w == 1)
            blocked = sum(1 for w in walkability if w == 2)

            obs_count = sum(1 for o in observed if o) if observed else total_cells

            print(f"\nChunk {grid_id}:")
            if 'worldTileOriginX' in chunk:
                print(f"  World origin: ({chunk['worldTileOriginX']}, {chunk['worldTileOriginY']})")
            print(f"  Walkability: {walkable} walkable, {partial} partial, {blocked} blocked")
            print(f"  Observed: {obs_count}/{total_cells} cells ({100*obs_count//total_cells}%)")
            print(f"  Connected to: {len(connected)} chunks")

            if portals:
                print(f"  Portals ({len(portals)}):")
                for portal in portals:
                    portal_name = portal.get('gobName', 'unknown').split('/')[-1]
                    connects_to = portal.get('connectsToGridId', -1)
                    local_x = portal.get('localX', 0)
                    local_y = portal.get('localY', 0)
                    print(f"    - {portal_name} at ({local_x}, {local_y}) -> grid {connects_to}")


def draw_path_on_detail(ax, path_data, chunk):
    """Draw the path segments that pass through this specific chunk on the detail view."""
    if not path_data or not chunk:
        return

    chunk_grid_id = chunk.get('gridId')

    # Find segments that belong to this chunk
    segments = path_data.get('segments', [])
    for seg in segments:
        seg_grid_id = seg.get('gridId')

        # Match by gridId only - worldTileOrigin is session-based and unreliable
        if seg_grid_id != chunk_grid_id:
            continue

        steps = seg.get('steps', [])
        if steps:
            # Extract local coordinates for this segment
            path_x = [step.get('localX', 0) for step in steps]
            path_y = [step.get('localY', 0) for step in steps]

            # Draw the path line
            ax.plot(path_x, path_y, color='cyan', linewidth=3, alpha=0.9, zorder=10)

            # Mark start and end of segment
            ax.plot(path_x[0], path_y[0], marker='o', markersize=10, color='lime',
                    markeredgecolor='black', markeredgewidth=2, zorder=11)
            ax.plot(path_x[-1], path_y[-1], marker='o', markersize=10, color='orange',
                    markeredgecolor='black', markeredgewidth=2, zorder=11)

    # Also draw waypoints that are in this chunk
    waypoints = path_data.get('waypoints', [])
    for wp in waypoints:
        wp_grid_id = wp.get('gridId')
        if wp_grid_id == chunk_grid_id:
            local_x = wp.get('localX', 50)
            local_y = wp.get('localY', 50)
            wp_type = wp.get('type', 'WALK')

            if wp_type == 'PORTAL_ENTRY':
                ax.plot(local_x, local_y, marker='D', markersize=14, color='magenta',
                        markeredgecolor='white', markeredgewidth=2, zorder=12)
            elif wp_type == 'DESTINATION':
                ax.plot(local_x, local_y, marker='*', markersize=20, color='red',
                        markeredgecolor='white', markeredgewidth=2, zorder=12)


def draw_path_on_overview(ax, path_data, chunk_map):
    """Draw the calculated path on the overview map."""
    if not path_data:
        return

    # Build grid_id to chunk origin mapping (uses display positions from chunk_map)
    grid_to_origin = {}
    for (cx, cy), chunk in chunk_map.items():
        grid_to_origin[chunk.get('gridId')] = (cx, cy)

    # Draw path segments (tile-level detail)
    segments = path_data.get('segments', [])
    all_path_x = []
    all_path_y = []

    for seg in segments:
        grid_id = seg.get('gridId')

        # Use display position from chunk_map if available
        if grid_id in grid_to_origin:
            origin_x, origin_y = grid_to_origin[grid_id]
        else:
            # Fall back to session-based worldTileOrigin from path data
            origin_x = seg.get('worldTileOriginX', 0)
            origin_y = seg.get('worldTileOriginY', 0)

        steps = seg.get('steps', [])

        for step in steps:
            world_x = origin_x + step.get('localX', 0)
            world_y = origin_y + step.get('localY', 0)
            all_path_x.append(world_x)
            all_path_y.append(world_y)

    # Draw path line
    if all_path_x:
        ax.plot(all_path_x, all_path_y, color='cyan', linewidth=2, alpha=0.9, zorder=10)

        # Mark start and end
        ax.plot(all_path_x[0], all_path_y[0], marker='*', markersize=15, color='lime',
                markeredgecolor='black', markeredgewidth=1, zorder=11)
        ax.plot(all_path_x[-1], all_path_y[-1], marker='*', markersize=15, color='red',
                markeredgecolor='black', markeredgewidth=1, zorder=11)

    # Draw waypoints
    waypoints = path_data.get('waypoints', [])
    for wp in waypoints:
        grid_id = wp.get('gridId')
        local_x = wp.get('localX', 50)
        local_y = wp.get('localY', 50)
        wp_type = wp.get('type', 'WALK')

        # Find chunk origin for this grid
        origin = grid_to_origin.get(grid_id)
        if origin:
            world_x = origin[0] + local_x
            world_y = origin[1] + local_y

            # Different markers for different waypoint types
            if wp_type == 'PORTAL_ENTRY':
                ax.plot(world_x, world_y, marker='D', markersize=12, color='magenta',
                        markeredgecolor='white', markeredgewidth=2, zorder=12)
            elif wp_type == 'DESTINATION':
                ax.plot(world_x, world_y, marker='X', markersize=15, color='red',
                        markeredgecolor='white', markeredgewidth=2, zorder=12)

    # Add path info text
    target_area = path_data.get('targetArea', 'unknown')
    total_cost = path_data.get('totalCost', 0)
    num_segments = len(segments)
    total_steps = sum(len(seg.get('steps', [])) for seg in segments)

    info_text = f"Path to: {target_area}\nCost: {total_cost:.1f} | Segments: {num_segments} | Steps: {total_steps}"
    ax.text(0.02, 0.98, info_text, transform=ax.transAxes, fontsize=9,
            verticalalignment='top', bbox=dict(boxstyle='round', facecolor='black', alpha=0.7),
            color='white')


def visualize_matplotlib(data, output_file=None, detail_chunk_index=0, layer_filter='surface', path_data=None):
    """Create matplotlib visualization of the chunk map with interactive chunk selection."""
    all_chunks = data.get('chunks', [])
    chunks = filter_chunks_by_layer(all_chunks, layer_filter)
    available_layers = get_available_layers(data)

    if not chunks:
        print(f"No chunks found for layer '{layer_filter}'.")
        print(f"Available layers: {', '.join(available_layers)}")
        return

    # Build positions from neighbor relationships
    # For 'all' layers, use layer-aware positioning that stacks layers vertically
    layer_bounds = {}
    if layer_filter == 'all':
        neighbor_positions, layer_bounds = build_grid_positions_by_layer(chunks)
        print(f"Layer layout: {', '.join(f'{layer}(y={bounds[0]}-{bounds[1]})' for layer, bounds in layer_bounds.items())}")
    else:
        neighbor_positions = build_grid_positions_from_neighbors(chunks)

    # Find bounds and build chunk map
    min_x = min_y = float('inf')
    max_x = max_y = float('-inf')

    chunk_map = {}
    chunk_by_origin = {}  # For click lookup
    for i, chunk in enumerate(chunks):
        coord = get_chunk_grid_coord(chunk, neighbor_positions)
        if coord is None:
            continue

        # Scale grid coords to tile coords for visualization
        x = coord[0] * CHUNK_SIZE
        y = coord[1] * CHUNK_SIZE

        min_x = min(min_x, x)
        min_y = min(min_y, y)
        max_x = max(max_x, x + CHUNK_SIZE)
        max_y = max(max_y, y + CHUNK_SIZE)
        chunk_map[(x, y)] = chunk
        chunk_by_origin[(x, y)] = i

    if not chunk_map:
        print("No chunks with world coordinates found.")
        return

    coord_type = "neighbor-based" if neighbor_positions else "session-based"
    print(f"Using {coord_type} coordinates for visualization ({len(neighbor_positions)} positioned via neighbor graph)")

    # Create figure with two subplots
    fig, axes = plt.subplots(1, 2, figsize=(20, 10))
    ax1, ax2 = axes

    # Store state for interactive updates
    state = {
        'current_index': detail_chunk_index,
        'highlight_rect': None
    }

    def draw_overview():
        """Draw the overview map on ax1 with full walkability detail for each chunk."""
        ax1.clear()
        ax1.set_title(f"ChunkNav World Map - {len(chunks)} chunks in layer '{layer_filter}'\nWorld: {data.get('genus', 'unknown')[:16]} | Layers: {', '.join(available_layers)}")
        ax1.set_xlabel("World X (tiles)")
        ax1.set_ylabel("World Y (tiles)")

        # Draw each chunk's walkability as an image
        for (cx, cy), chunk in chunk_map.items():
            chunk_idx = chunk_by_origin.get((cx, cy), -1)
            is_selected = (chunk_idx == state['current_index'])

            walkability = chunk.get('walkability', [])
            observed = chunk.get('observed', [])
            cells_per_edge = get_cells_per_edge(chunk)

            if walkability:
                walk_grid = parse_walkability(walkability, cells_per_edge)
                obs_grid = parse_observed(observed, cells_per_edge) if observed else [[True]*cells_per_edge for _ in range(cells_per_edge)]

                # Create image data for this chunk
                # Use origin='upper' so Y=0 is at top (matching game coordinates where Y increases southward)
                img_data = np.zeros((cells_per_edge, cells_per_edge, 3))
                for row in range(cells_per_edge):
                    for col in range(cells_per_edge):
                        # row corresponds to Y (local), col corresponds to X (local)
                        w = walk_grid[col][row]
                        o = obs_grid[col][row]

                        if not o:
                            img_data[row, col] = [0.3, 0.3, 0.3]  # Unobserved - dark gray
                        elif w == 0:
                            img_data[row, col] = [0.2, 0.6, 0.2]  # Walkable - green
                        else:
                            img_data[row, col] = [0.6, 0.2, 0.2]  # Blocked - red

                # Draw chunk image at world position
                # With inverted Y axis (north up), use origin='lower' and normal extent
                ax1.imshow(img_data, origin='lower', extent=[cx, cx + CHUNK_SIZE, cy, cy + CHUNK_SIZE],
                          aspect='equal', zorder=1)
            else:
                # No walkability data - draw gray rectangle
                rect = patches.Rectangle(
                    (cx, cy), CHUNK_SIZE, CHUNK_SIZE,
                    linewidth=1, edgecolor='black', facecolor='lightgray', alpha=0.7, zorder=1
                )
                ax1.add_patch(rect)

            # Draw chunk border (highlighted if selected)
            linewidth = 3 if is_selected else 0.5
            edgecolor = 'yellow' if is_selected else 'white'
            border = patches.Rectangle(
                (cx, cy), CHUNK_SIZE, CHUNK_SIZE,
                linewidth=linewidth, edgecolor=edgecolor, facecolor='none', zorder=5
            )
            ax1.add_patch(border)

            # Mark portals
            portals = chunk.get('portals', [])
            for portal in portals:
                px = cx + portal.get('localX', 50)
                py = cy + portal.get('localY', 50)
                portal_type = portal.get('type', 'DOOR')

                if portal_type == 'CELLAR':
                    marker = 's'
                    pcolor = 'blue'
                elif portal_type == 'DOOR':
                    marker = 'o'
                    pcolor = 'purple'
                elif 'STAIRS' in portal_type:
                    marker = '^'
                    pcolor = 'orange'
                else:
                    marker = 'x'
                    pcolor = 'red'

                ax1.plot(px, py, marker=marker, markersize=6, color=pcolor,
                        markeredgecolor='white', markeredgewidth=0.5, zorder=6)

        # Draw portal connections
        for (cx, cy), chunk in chunk_map.items():
            portals = chunk.get('portals', [])
            for portal in portals:
                connects_to = portal.get('connectsToGridId', -1)
                if connects_to != -1:
                    for (tcx, tcy), target_chunk in chunk_map.items():
                        if target_chunk.get('gridId') == connects_to:
                            px = cx + portal.get('localX', 50)
                            py = cy + portal.get('localY', 50)
                            ax1.annotate('', xy=(tcx + CHUNK_SIZE/2, tcy + CHUNK_SIZE/2),
                                        xytext=(px, py),
                                        arrowprops=dict(arrowstyle='->', color='blue', alpha=0.5))
                            break

        # Draw layer separators and labels when showing all layers
        if layer_bounds:
            layer_colors = {
                'surface': '#4CAF50',  # Green
                'inside': '#9C27B0',   # Purple
                'cellar': '#2196F3',   # Blue
            }
            # Generate colors for mine levels (orange shades, darker for deeper levels)
            for layer in layer_bounds.keys():
                if layer.startswith('mine'):
                    try:
                        level = int(layer[4:])
                        # Darker orange for deeper levels
                        r = max(0.5, 1.0 - level * 0.1)
                        g = max(0.2, 0.6 - level * 0.1)
                        b = 0
                        layer_colors[layer] = (r, g, b)
                    except ValueError:
                        layer_colors[layer] = '#FF9800'  # Default orange

            for layer, (layer_min_y, layer_max_y) in layer_bounds.items():
                # Convert grid coords to tile coords
                y_start = layer_min_y * CHUNK_SIZE
                y_end = (layer_max_y + 1) * CHUNK_SIZE

                # Draw separator line above each layer (except first)
                if layer_min_y > 0:
                    sep_y = y_start - (2.5 * CHUNK_SIZE)  # Middle of the gap
                    ax1.axhline(y=sep_y, color='white', linestyle='--', linewidth=1, alpha=0.5, zorder=10)

                # Draw layer label on the left side
                label_color = layer_colors.get(layer, 'white')
                ax1.text(min_x - 50, (y_start + y_end) / 2, f"[{layer.upper()}]",
                        fontsize=12, fontweight='bold', color=label_color,
                        verticalalignment='center', horizontalalignment='right',
                        bbox=dict(boxstyle='round,pad=0.3', facecolor='black', alpha=0.7),
                        zorder=10)

        # Draw path if available
        if path_data:
            draw_path_on_overview(ax1, path_data, chunk_map)

        # Extend left margin for layer labels when showing all layers
        left_margin = 120 if layer_bounds else 10
        ax1.set_xlim(min_x - left_margin, max_x + 10)
        ax1.set_ylim(max_y + 10, min_y - 10)  # Invert Y axis so north is up
        ax1.set_aspect('equal')
        ax1.set_facecolor('#1a1a1a')  # Dark background for unexplored areas

        # Add legend outside the plot (below)
        legend_elements = [
            patches.Patch(facecolor=(0.2, 0.6, 0.2), edgecolor='black', label='Walkable'),
            patches.Patch(facecolor=(0.6, 0.2, 0.2), edgecolor='black', label='Blocked'),
            patches.Patch(facecolor=(0.3, 0.3, 0.3), edgecolor='black', label='Unobserved'),
            patches.Patch(facecolor='none', edgecolor='yellow', linewidth=3, label='Selected'),
            plt.Line2D([0], [0], marker='o', color='w', markerfacecolor='purple', markersize=8, label='Door'),
            plt.Line2D([0], [0], marker='s', color='w', markerfacecolor='blue', markersize=8, label='Cellar'),
            plt.Line2D([0], [0], marker='^', color='w', markerfacecolor='orange', markersize=8, label='Stairs'),
        ]
        if path_data:
            legend_elements.append(plt.Line2D([0], [0], color='cyan', linewidth=3, label='Path'))
            legend_elements.append(plt.Line2D([0], [0], marker='*', color='w', markerfacecolor='lime', markersize=12, label='Start'))
            legend_elements.append(plt.Line2D([0], [0], marker='*', color='w', markerfacecolor='red', markersize=12, label='End'))
        ax1.legend(handles=legend_elements, loc='upper center', bbox_to_anchor=(0.5, -0.05),
                   ncol=5, fontsize=7, framealpha=0.9)

    def draw_detail():
        """Draw the detail view on ax2."""
        ax2.clear()

        detail_chunk = None
        if chunks and 0 <= state['current_index'] < len(chunks):
            detail_chunk = chunks[state['current_index']]
        elif chunks:
            detail_chunk = chunks[0]

        if detail_chunk:
            walkability = detail_chunk.get('walkability', [])
            observed = detail_chunk.get('observed', [])
            cells_per_edge = get_cells_per_edge(detail_chunk)

            if walkability:
                walk_grid = parse_walkability(walkability, cells_per_edge)
                obs_grid = parse_observed(observed, cells_per_edge) if observed else [[True]*cells_per_edge for _ in range(cells_per_edge)]

                # Create image data (cells_per_edge x cells_per_edge)
                # Use origin='upper' so Y=0 is at top (matching game coordinates)
                img_data = np.zeros((cells_per_edge, cells_per_edge, 3))
                for row in range(cells_per_edge):
                    for col in range(cells_per_edge):
                        # row = Y coordinate, col = X coordinate
                        w = walk_grid[col][row]
                        o = obs_grid[col][row]

                        if not o:
                            img_data[row, col] = [0.5, 0.5, 0.5]  # Unobserved - gray
                        elif w == 0:
                            img_data[row, col] = [0.2, 0.8, 0.2]  # Walkable - green
                        else:
                            img_data[row, col] = [0.8, 0.2, 0.2]  # Blocked - red

                ax2.imshow(img_data, origin='upper', extent=[0, CHUNK_SIZE, CHUNK_SIZE, 0])

                # Mark portals
                # Note: localX/localY in the JSON correspond to tile coords within the chunk
                # The image is displayed with origin='lower' and extent=[0, CHUNK_SIZE, 0, CHUNK_SIZE]
                # so we plot portals at (localX, localY) directly
                portals = detail_chunk.get('portals', [])
                for portal in portals:
                    px = portal.get('localX', 50)
                    py = portal.get('localY', 50)
                    portal_name = portal.get('gobName', 'unknown').split('/')[-1]
                    connects_to = portal.get('connectsToGridId', -1)

                    # Color based on connection status
                    if connects_to != -1:
                        marker_color = 'cyan'  # Has connection
                    else:
                        marker_color = 'white'  # No connection

                    ax2.plot(px, py, 'o', markersize=10, color=marker_color,
                            markeredgecolor='black', markeredgewidth=2)

                    # Show portal name and connection info
                    if connects_to != -1:
                        label = f"{portal_name}\n-> {str(connects_to)[:8]}..."
                    else:
                        label = f"{portal_name}\n(no link)"
                    ax2.annotate(label, (px, py), textcoords="offset points",
                                xytext=(5, 5), fontsize=7, color='white',
                                bbox=dict(boxstyle='round', facecolor='black', alpha=0.7))

                # Draw path on this chunk if available
                if path_data:
                    draw_path_on_detail(ax2, path_data, detail_chunk)

                grid_id = detail_chunk.get('gridId', 'unknown')
                origin_x = detail_chunk.get('worldTileOriginX', '?')
                origin_y = detail_chunk.get('worldTileOriginY', '?')
                ax2.set_title(f"Chunk {state['current_index']}: origin=({origin_x}, {origin_y})\nGrid {str(grid_id)[:16]}...")
            else:
                ax2.text(0.5, 0.5, "No walkability data", ha='center', va='center', transform=ax2.transAxes)

        ax2.set_xlabel("Local X (tiles)")
        ax2.set_ylabel("Local Y (tiles)")
        ax2.grid(True, alpha=0.3)

        # Add legend for detail view (below the plot)
        legend_elements2 = [
            patches.Patch(facecolor=(0.2, 0.8, 0.2), label='Walkable'),
            patches.Patch(facecolor=(0.8, 0.2, 0.2), label='Blocked'),
            patches.Patch(facecolor=(0.5, 0.5, 0.5), label='Unobserved'),
        ]
        if path_data:
            legend_elements2.append(plt.Line2D([0], [0], color='cyan', linewidth=3, label='Path'))
        ax2.legend(handles=legend_elements2, loc='upper center', bbox_to_anchor=(0.5, -0.05),
                   ncol=4, fontsize=8, framealpha=0.9)

    def on_click(event):
        """Handle click events on the overview map."""
        if event.inaxes != ax1:
            return

        click_x, click_y = event.xdata, event.ydata
        if click_x is None or click_y is None:
            return

        # Find which chunk was clicked
        for (cx, cy), chunk in chunk_map.items():
            if cx <= click_x < cx + CHUNK_SIZE and cy <= click_y < cy + CHUNK_SIZE:
                chunk_idx = chunk_by_origin.get((cx, cy), -1)
                if chunk_idx >= 0 and chunk_idx != state['current_index']:
                    state['current_index'] = chunk_idx
                    draw_overview()
                    draw_detail()
                    fig.canvas.draw_idle()
                break

    # Initial draw
    draw_overview()
    draw_detail()
    plt.tight_layout()
    plt.subplots_adjust(bottom=0.1)  # Make room for legends below plots

    if output_file:
        plt.savefig(output_file, dpi=150, bbox_inches='tight')
        print(f"Saved visualization to: {output_file}")
    else:
        # Connect click handler for interactive mode
        fig.canvas.mpl_connect('button_press_event', on_click)
        print("\nClick on any chunk in the left panel to view its details on the right.")
        plt.show()


def main():
    import argparse
    parser = argparse.ArgumentParser(description='Visualize ChunkNav navigation data')
    parser.add_argument('filepath', nargs='?', help='Path to chunknav.nurgling.json')
    parser.add_argument('--world', '-w', type=int, choices=[1, 2],
                        help='Select world profile (1=b7c199a4557503a8, 2=c646473983afec09)')
    parser.add_argument('--layer', '-l', default='surface',
                        help='Filter by layer (surface, inside, cellar, mine, all). Default: surface')
    parser.add_argument('--output', '-o', help='Output image file (PNG)')
    parser.add_argument('--path', '-p', action='store_true',
                        help='Show the last calculated path (from chunknav_path.json)')
    args = parser.parse_args()

    # Determine input file
    if args.filepath:
        filepath = Path(args.filepath)
        if not filepath.exists():
            print(f"Error: File not found: {filepath}")
            sys.exit(1)
    else:
        # Find files automatically
        files = find_chunknav_files(world=args.world)
        if not files:
            print("No chunknav.nurgling.json files found.")
            print("Usage: python chunknav_visualizer.py [path_to_chunknav.json] [--layer LAYER]")
            sys.exit(1)

        if len(files) == 1:
            filepath = files[0]
        else:
            print("Found multiple chunknav files:")
            for i, f in enumerate(files):
                print(f"  {i+1}. {f}")
            choice = input("Select file number: ")
            try:
                filepath = files[int(choice) - 1]
            except (ValueError, IndexError):
                print("Invalid selection.")
                sys.exit(1)

    print(f"Loading: {filepath}")
    data = load_chunknav_data(filepath)

    # Load path data if requested
    path_data = None
    if args.path:
        path_data = load_path_data(filepath)
        if path_data:
            print(f"Loaded path to: {path_data.get('targetArea', 'unknown')}")
            print(f"  Segments: {len(path_data.get('segments', []))}")
            print(f"  Total steps: {sum(len(s.get('steps', [])) for s in path_data.get('segments', []))}")
        else:
            print("No path data found (chunknav_path.json)")

    # Show available layers
    available_layers = get_available_layers(data)
    print(f"Available layers: {', '.join(available_layers)}")
    print(f"Filtering by layer: {args.layer}")

    # Always show ASCII summary
    visualize_ascii(data, layer_filter=args.layer)

    # Show matplotlib if available
    if HAS_MATPLOTLIB:
        visualize_matplotlib(data, output_file=args.output, layer_filter=args.layer, path_data=path_data)


if __name__ == '__main__':
    main()
