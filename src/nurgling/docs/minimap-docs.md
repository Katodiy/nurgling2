# Nurgling2 Minimap System Documentation

## Overview
The minimap in Nurgling2 renders discovered terrain information from the Haven & Hearth game world. It starts as a black/empty map and progressively reveals terrain as the player explores.

## Core Components

### 1. Data Structure (MCache.java:315)
The `MCache.Grid` class represents a 100x100 tile area:
- `tiles[]` - Array storing tile type IDs for each coordinate
- `z[]` - Height/elevation information for terrain
- `ols` - Overlay data for additional terrain features
- `id` - Unique identifier for the grid

### 2. Rendering Pipeline

#### Initial State
- Map appears black/empty because no Grid objects exist
- No terrain data has been received from server

#### Progressive Discovery
1. **Data Reception**: Server sends tile information as player explores
2. **Grid Population**: `MCache.Grid` objects created and populated with:
   - Tile type IDs (`gettile(coord)`)
   - Height data (`getz(coord)`)
   - Overlay information
3. **Visual Rendering**: Tile IDs converted to visual representation

### 3. Rendering Process

#### Primary Rendering (MapFile.java:460, MinimapImageGenerator.java:35)
```java
public static BufferedImage drawmap(MCache map, MCache.Grid grid)
```

**Steps:**
1. **Tile Lookup**: `tileimg(grid.gettile(c), texes, map)` retrieves texture for each tile
2. **Color Extraction**: Gets RGB color from tile texture
3. **Biome Rendering**: Two modes available:
   - **Normal**: Uses texture coordinates for varied appearance
   - **Uniform**: Uses single color from pixel (0,0) for solid biome colors
4. **Edge Detection**: Adds black borders between different terrain types
5. **Ridge Processing**: Special handling for broken ridges (darkening effect)

#### Display Updates (MiniMap.java:527)
The `redisplay()` method:
- Tracks current location and zoom level
- Creates `DisplayGrid[]` for visible area
- Loads grid data as player moves
- Triggers re-rendering when new areas discovered

## Persistent Storage System

### Storage Location
- **Windows**: `%APPDATA%\Haven and Hearth\data\`
- **Other OS**: `~/.haven/data/`

### File Structure
The MapFile system maintains several file types:

#### 1. Index File (`map/index`)
- List of known segments
- Marker information
- Overall map metadata

#### 2. Grid Info Files (`map/gi-{gridId}`)
Stores metadata for each discovered grid:
- Grid ID
- Segment ID it belongs to  
- Coordinates within segment

#### 3. Grid Data Files (`map/grid-{gridId}`)
Contains the actual terrain data:

**Tile Information** (MapFile.java:570-583):
- `TileInfo[]` array mapping tile IDs to resources
  - Resource name (e.g., "gfx/tiles/grass", "gfx/tiles/forest")
  - Version number
  - Rendering priority
- `int[] tiles` - 100x100 array of tile type IDs

**Height Data** (MapFile.java:628-650):
- `float[] zmap` - Elevation information

**Overlay Data** (MapFile.java:652-684):
- Biome overlays (cave entrances, water bodies, etc.)
- Stored as boolean arrays per coordinate

#### 4. Segment Files (`map/seg-{segmentId}`)
Higher-level organization of related grids

### Loading Process (GameUI.java:593)
When game restarts:
1. `MapFile.load()` reads persistent map data
2. `BackCache` system loads grid info and segments on-demand
3. Previously discovered terrain immediately visible
4. Real-time updates continue as player explores new areas

## Key Classes and Methods

### Core Classes
- `MiniMap` - Main minimap widget and rendering coordinator
- `MCache.Grid` - Represents 100x100 tile area with terrain data
- `MapFile` - Persistent storage and loading system
- `MinimapImageGenerator` - Converts grid data to visual representation
- `DisplayGrid` - Cached rendering of grid sections

### Important Methods
- `MiniMap.redisplay()` - Updates visible area and triggers rendering
- `MapFile.Grid.load()` - Loads grid data from disk
- `MinimapImageGenerator.drawmap()` - Converts tile data to BufferedImage
- `MCache.Grid.gettile()` - Gets tile type at coordinate
- `DataGrid.render()` - Primary rendering method with biome color support

### Data Flow
1. **Exploration**: Player moves → Server sends tile data → `MCache.Grid` populated
2. **Rendering**: Tile IDs → Texture lookup → Color extraction → Visual display
3. **Persistence**: Grid data → Serialized to disk → Available on restart
4. **Loading**: Disk files → `BackCache` → Immediate display of known terrain

## Configuration
- **Uniform Biome Colors**: `NConfig.Key.uniformBiomeColors` - Use solid colors vs. textured appearance
- **Auto Mapper**: Integration with external mapping services
- **Zoom Levels**: Multiple zoom levels supported with cached rendering

## Performance Considerations
- **BackCache**: LRU cache for grid data (100 grids, 5 segments)
- **Lazy Loading**: Grid data loaded on-demand as areas are viewed
- **Deferred Rendering**: Texture generation happens asynchronously
- **Cut System**: Large grids divided into 25x25 cuts for efficient rendering

This system ensures that discovered terrain persists across game sessions while efficiently handling real-time updates as new areas are explored.