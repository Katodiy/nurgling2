package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.navigation.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.*;

import static nurgling.navigation.ChunkNavConfig.*;

/**
 * In-game visualization of ChunkNav data.
 * Shows world overview with all chunks, portal connections, and paths.
 * Click on a chunk in the overview to see its detail view.
 */
public class ChunkNavVisualizerWindow extends Window {

    // Layout constants
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 600;
    private static final int SETTINGS_WIDTH = 150;
    private static final int CANVAS_HEIGHT = 500;

    // Rendering constants
    private static final int PIXELS_PER_CHUNK = 50;  // In world overview
    private static final int DETAIL_SIZE = 400;      // Detail view size

    // Colors
    private static final Color COLOR_WALKABLE = new Color(51, 153, 51);
    private static final Color COLOR_BLOCKED = new Color(153, 51, 51);
    private static final Color COLOR_UNOBSERVED = new Color(77, 77, 77);
    private static final Color COLOR_SELECTED = new Color(255, 255, 0);
    private static final Color COLOR_BACKGROUND = new Color(26, 26, 26);
    private static final Color COLOR_PORTAL = new Color(0, 255, 255);
    private static final Color COLOR_CONNECTION = new Color(100, 150, 255, 200);
    private static final Color COLOR_PATH = new Color(255, 255, 0);
    private static final Color COLOR_PATH_START = new Color(0, 255, 0);
    private static final Color COLOR_PATH_END = new Color(255, 0, 0);
    private static final Color COLOR_GRID = new Color(100, 100, 100);

    // UI State
    private boolean showPortals = true;
    private boolean showConnections = true;
    private boolean showPath = false;
    private boolean showGrid = true;
    private boolean showCellLines = false;

    // Pan/Zoom state
    private float zoom = 1.0f;
    private float panX = 0;
    private float panY = 0;
    private boolean dragging = false;
    private Coord dragStart = null;
    private float dragStartPanX = 0;
    private float dragStartPanY = 0;

    // Data
    private List<ChunkNavData> chunks = new ArrayList<>();
    private Map<Long, Coord> positions = new HashMap<>();  // gridId -> (x, y) in grid coords
    private Map<String, int[]> layerBounds = new HashMap<>();  // layer -> [minY, maxY] in grid coords
    private Coord worldBoundsMin = Coord.z;
    private Coord worldBoundsMax = Coord.z;
    private int selectedChunkIdx = -1;
    private ChunkPath lastPath = null;

    // Textures
    private Tex worldTex = null;
    private Tex detailTex = null;
    private int worldTexWidth = 0;
    private int worldTexHeight = 0;

    // Widgets
    private final Label statusLabel;
    private final Label selectedLabel;
    private final WorldCanvas worldCanvas;
    private final DetailCanvas detailCanvas;
    private final CheckBox portalsCb;
    private final CheckBox connectionsCb;
    private final CheckBox pathCb;
    private final CheckBox gridCb;
    private final CheckBox cellLinesCb;
    private final Button deleteChunkBtn;
    private final Button deleteAllBtn;

    public ChunkNavVisualizerWindow() {
        super(new Coord(UI.scale(WINDOW_WIDTH), UI.scale(WINDOW_HEIGHT)), "ChunkNav Visualizer");

        int y = UI.scale(5);

        // Settings panel (left side)
        Widget prev = add(new Label("DISPLAY"), new Coord(UI.scale(10), y));
        y += UI.scale(20);

        portalsCb = add(new CheckBox("Portals") {
            @Override
            public void changed(boolean val) {
                showPortals = val;
                regenerateTextures();
            }
        }, new Coord(UI.scale(10), y));
        portalsCb.a = showPortals;
        y += UI.scale(20);

        connectionsCb = add(new CheckBox("Connections") {
            @Override
            public void changed(boolean val) {
                showConnections = val;
                regenerateTextures();
            }
        }, new Coord(UI.scale(10), y));
        connectionsCb.a = showConnections;
        y += UI.scale(20);

        pathCb = add(new CheckBox("Path") {
            @Override
            public void changed(boolean val) {
                showPath = val;
                regenerateTextures();
            }
        }, new Coord(UI.scale(10), y));
        pathCb.a = showPath;
        y += UI.scale(20);

        gridCb = add(new CheckBox("Chunk borders") {
            @Override
            public void changed(boolean val) {
                showGrid = val;
                regenerateTextures();
            }
        }, new Coord(UI.scale(10), y));
        gridCb.a = showGrid;
        y += UI.scale(20);

        cellLinesCb = add(new CheckBox("Cell lines") {
            @Override
            public void changed(boolean val) {
                showCellLines = val;
                regenerateDetailTexture();
            }
        }, new Coord(UI.scale(10), y));
        cellLinesCb.a = showCellLines;
        y += UI.scale(30);

        add(new Label("SELECTED"), new Coord(UI.scale(10), y));
        y += UI.scale(18);
        selectedLabel = add(new Label("None"), new Coord(UI.scale(10), y));
        y += UI.scale(30);

        add(new Button(UI.scale(SETTINGS_WIDTH - 20), "Reload") {
            @Override
            public void click() {
                super.click();
                reloadData();
            }
        }, new Coord(UI.scale(10), y));
        y += UI.scale(30);

        deleteChunkBtn = add(new Button(UI.scale(SETTINGS_WIDTH - 20), "Delete Chunk") {
            @Override
            public void click() {
                super.click();
                deleteSelectedChunk();
            }
        }, new Coord(UI.scale(10), y));
        deleteChunkBtn.disable(true);  // Disabled until a chunk is selected
        y += UI.scale(30);

        deleteAllBtn = add(new Button(UI.scale(SETTINGS_WIDTH - 20), "Delete All") {
            @Override
            public void click() {
                super.click();
                deleteAllChunks();
            }
        }, new Coord(UI.scale(10), y));
        y += UI.scale(40);

        statusLabel = add(new Label("Ready"), new Coord(UI.scale(10), y));

        // World canvas (center)
        int canvasX = UI.scale(SETTINGS_WIDTH + 10);
        int canvasWidth = UI.scale((WINDOW_WIDTH - SETTINGS_WIDTH - DETAIL_SIZE - 30) / 2 + 150);
        worldCanvas = add(new WorldCanvas(new Coord(canvasWidth, UI.scale(CANVAS_HEIGHT))),
                new Coord(canvasX, UI.scale(5)));
        add(new Label("World Map (LMB select, RMB pan, scroll zoom)"), new Coord(canvasX, UI.scale(CANVAS_HEIGHT + 10)));

        // Detail canvas (right)
        int detailX = canvasX + canvasWidth + UI.scale(10);
        detailCanvas = add(new DetailCanvas(new Coord(UI.scale(DETAIL_SIZE), UI.scale(CANVAS_HEIGHT))),
                new Coord(detailX, UI.scale(5)));
        add(new Label("Chunk Detail"), new Coord(detailX, UI.scale(CANVAS_HEIGHT + 10)));

        pack();

        // Load initial data
        reloadData();
    }

    private ChunkNavManager getChunkNavManager() {
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui != null && gui.map != null && gui.map instanceof NMapView) {
                return ((NMapView) gui.map).getChunkNavManager();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private void reloadData() {
        ChunkNavManager manager = getChunkNavManager();
        if (manager == null || !manager.isInitialized()) {
            statusLabel.settext("ChunkNav not initialized");
            return;
        }

        // Get all chunks
        chunks = new ArrayList<>(manager.getGraph().getAllChunks());
        statusLabel.settext("Loaded " + chunks.size() + " chunks");

        // Get last planned path
        lastPath = manager.getLastPlannedPath();

        // Build positions from neighbor relationships
        buildPositions();

        // Generate textures
        regenerateTextures();
    }

    /**
     * Build grid positions by traversing neighbor relationships.
     * Groups chunks by layer and stacks layers vertically.
     * Similar to Python's build_grid_positions_by_layer().
     */
    private void buildPositions() {
        positions.clear();
        layerBounds.clear();
        if (chunks.isEmpty()) {
            worldBoundsMin = Coord.z;
            worldBoundsMax = Coord.z;
            return;
        }

        // Group chunks by layer
        Map<String, List<ChunkNavData>> chunksByLayer = new LinkedHashMap<>();
        for (ChunkNavData chunk : chunks) {
            String layer = chunk.layer != null ? chunk.layer : "outside";
            chunksByLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(chunk);
        }

        // Define layer order
        List<String> layerOrder = new ArrayList<>();
        layerOrder.add("outside");
        layerOrder.add("inside");
        layerOrder.add("cellar");
        // Add mine layers in order
        List<String> mineLayers = new ArrayList<>();
        for (String layer : chunksByLayer.keySet()) {
            if (layer.startsWith("mine")) {
                mineLayers.add(layer);
            }
        }
        mineLayers.sort((a, b) -> {
            try {
                int numA = Integer.parseInt(a.substring(4));
                int numB = Integer.parseInt(b.substring(4));
                return Integer.compare(numA, numB);
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });
        layerOrder.addAll(mineLayers);
        // Add any remaining layers
        for (String layer : chunksByLayer.keySet()) {
            if (!layerOrder.contains(layer)) {
                layerOrder.add(layer);
            }
        }

        int currentYOffset = 0;
        int layerGap = 5;

        for (String layer : layerOrder) {
            List<ChunkNavData> layerChunks = chunksByLayer.get(layer);
            if (layerChunks == null || layerChunks.isEmpty()) continue;

            // Build positions for this layer
            Map<Long, Coord> layerPositions = buildLayerPositions(layerChunks);
            if (layerPositions.isEmpty()) continue;

            // Find bounds of this layer
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            for (Coord pos : layerPositions.values()) {
                minX = Math.min(minX, pos.x);
                maxX = Math.max(maxX, pos.x);
                minY = Math.min(minY, pos.y);
                maxY = Math.max(maxY, pos.y);
            }

            // Normalize and offset positions for this layer
            int layerHeight = maxY - minY + 1;
            for (Map.Entry<Long, Coord> entry : layerPositions.entrySet()) {
                Coord pos = entry.getValue();
                int newX = pos.x - minX;  // Normalize X to start at 0
                int newY = pos.y - minY + currentYOffset;  // Normalize Y and add offset
                positions.put(entry.getKey(), new Coord(newX, newY));
            }

            // Store layer bounds
            layerBounds.put(layer, new int[]{currentYOffset, currentYOffset + layerHeight - 1});

            // Next layer starts below this one
            currentYOffset += layerHeight + layerGap;
        }

        // Calculate global bounds
        if (!positions.isEmpty()) {
            int minGx = Integer.MAX_VALUE, minGy = Integer.MAX_VALUE;
            int maxGx = Integer.MIN_VALUE, maxGy = Integer.MIN_VALUE;
            for (Coord pos : positions.values()) {
                minGx = Math.min(minGx, pos.x);
                minGy = Math.min(minGy, pos.y);
                maxGx = Math.max(maxGx, pos.x);
                maxGy = Math.max(maxGy, pos.y);
            }
            worldBoundsMin = new Coord(minGx, minGy);
            worldBoundsMax = new Coord(maxGx, maxGy);
        }

        // Reset pan/zoom on reload
        zoom = 1.0f;
        panX = 0;
        panY = 0;
    }

    /**
     * Build positions for chunks in a single layer using BFS.
     */
    private Map<Long, Coord> buildLayerPositions(List<ChunkNavData> layerChunks) {
        Map<Long, Coord> layerPositions = new HashMap<>();
        if (layerChunks.isEmpty()) return layerPositions;

        // Build lookup by gridId
        Map<Long, ChunkNavData> chunksById = new HashMap<>();
        for (ChunkNavData chunk : layerChunks) {
            chunksById.put(chunk.gridId, chunk);
        }

        Set<Long> positioned = new HashSet<>();
        int componentOffsetX = 0;

        // Process all chunks, handling disconnected components
        while (positioned.size() < layerChunks.size()) {
            // Find an unpositioned chunk
            ChunkNavData start = null;
            for (ChunkNavData chunk : layerChunks) {
                if (!positioned.contains(chunk.gridId)) {
                    start = chunk;
                    break;
                }
            }
            if (start == null) break;

            layerPositions.put(start.gridId, new Coord(componentOffsetX, 0));
            positioned.add(start.gridId);

            // BFS to assign positions
            Queue<Long> queue = new LinkedList<>();
            queue.add(start.gridId);
            int minX = componentOffsetX, maxX = componentOffsetX;

            while (!queue.isEmpty()) {
                long currentId = queue.poll();
                ChunkNavData current = chunksById.get(currentId);
                if (current == null) continue;

                Coord pos = layerPositions.get(currentId);
                if (pos == null) continue;

                minX = Math.min(minX, pos.x);
                maxX = Math.max(maxX, pos.x);

                // Check neighbors (only within this layer)
                long[] neighbors = {current.neighborNorth, current.neighborSouth,
                        current.neighborEast, current.neighborWest};
                int[][] offsets = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};

                for (int i = 0; i < 4; i++) {
                    long neighborId = neighbors[i];
                    if (neighborId != -1 && chunksById.containsKey(neighborId) && !positioned.contains(neighborId)) {
                        layerPositions.put(neighborId, pos.add(offsets[i][0], offsets[i][1]));
                        positioned.add(neighborId);
                        queue.add(neighborId);
                    }
                }
            }

            // Next component starts after this one with a gap
            componentOffsetX = maxX + 3;
        }

        return layerPositions;
    }

    private void regenerateTextures() {
        regenerateWorldTexture();
        regenerateDetailTexture();
    }

    private void regenerateWorldTexture() {
        if (chunks.isEmpty() || positions.isEmpty()) {
            worldTex = null;
            return;
        }

        int gridWidth = worldBoundsMax.x - worldBoundsMin.x + 1;
        int gridHeight = worldBoundsMax.y - worldBoundsMin.y + 1;

        worldTexWidth = gridWidth * PIXELS_PER_CHUNK;
        worldTexHeight = gridHeight * PIXELS_PER_CHUNK;

        // Limit texture size
        int maxDim = 2048;
        float scale = 1.0f;
        if (worldTexWidth > maxDim || worldTexHeight > maxDim) {
            scale = Math.min((float) maxDim / worldTexWidth, (float) maxDim / worldTexHeight);
            worldTexWidth = (int) (worldTexWidth * scale);
            worldTexHeight = (int) (worldTexHeight * scale);
        }
        int actualChunkSize = (int) (PIXELS_PER_CHUNK * scale);

        WritableRaster buf = PUtils.imgraster(new Coord(worldTexWidth, worldTexHeight));

        // Fill background
        for (int y = 0; y < worldTexHeight; y++) {
            for (int x = 0; x < worldTexWidth; x++) {
                setPixel(buf, x, y, COLOR_BACKGROUND);
            }
        }

        // Draw each chunk
        for (int idx = 0; idx < chunks.size(); idx++) {
            ChunkNavData chunk = chunks.get(idx);
            Coord pos = positions.get(chunk.gridId);
            if (pos == null) continue;

            int px = (pos.x - worldBoundsMin.x) * actualChunkSize;
            int py = (pos.y - worldBoundsMin.y) * actualChunkSize;

            // Draw scaled chunk
            drawScaledChunk(buf, chunk, px, py, actualChunkSize);

            // Draw grid lines
            if (showGrid) {
                for (int i = 0; i < actualChunkSize; i++) {
                    if (py < worldTexHeight && px + i < worldTexWidth) {
                        setPixel(buf, px + i, py, COLOR_GRID);
                    }
                    if (py + i < worldTexHeight && px < worldTexWidth) {
                        setPixel(buf, px, py + i, COLOR_GRID);
                    }
                }
            }

            // Draw selection
            if (idx == selectedChunkIdx) {
                int borderWidth = Math.max(2, actualChunkSize / 15);
                drawSelectionBorder(buf, px, py, actualChunkSize, borderWidth);
            }
        }

        // Draw portals
        if (showPortals) {
            int markerSize = Math.max(1, actualChunkSize / 35);
            for (ChunkNavData chunk : chunks) {
                Coord pos = positions.get(chunk.gridId);
                if (pos == null) continue;

                int chunkPx = (pos.x - worldBoundsMin.x) * actualChunkSize;
                int chunkPy = (pos.y - worldBoundsMin.y) * actualChunkSize;

                for (ChunkPortal portal : chunk.portals) {
                    int localX = portal.localCoord.x;
                    int localY = portal.localCoord.y;
                    int px = chunkPx + (localX * actualChunkSize / CHUNK_SIZE);
                    int py = chunkPy + (localY * actualChunkSize / CHUNK_SIZE);

                    drawMarker(buf, px, py, markerSize, COLOR_PORTAL);
                }
            }
        }

        // Draw connections
        if (showConnections) {
            for (ChunkNavData chunk : chunks) {
                Coord pos = positions.get(chunk.gridId);
                if (pos == null) continue;

                int chunkPx = (pos.x - worldBoundsMin.x) * actualChunkSize;
                int chunkPy = (pos.y - worldBoundsMin.y) * actualChunkSize;

                for (ChunkPortal portal : chunk.portals) {
                    if (portal.connectsToGridId == -1) continue;
                    Coord targetPos = positions.get(portal.connectsToGridId);
                    if (targetPos == null) continue;

                    int srcX = chunkPx + (portal.localCoord.x * actualChunkSize / CHUNK_SIZE);
                    int srcY = chunkPy + (portal.localCoord.y * actualChunkSize / CHUNK_SIZE);

                    int tgtX, tgtY;
                    if (portal.exitLocalCoord != null) {
                        tgtX = (targetPos.x - worldBoundsMin.x) * actualChunkSize +
                                (portal.exitLocalCoord.x * actualChunkSize / CHUNK_SIZE);
                        tgtY = (targetPos.y - worldBoundsMin.y) * actualChunkSize +
                                (portal.exitLocalCoord.y * actualChunkSize / CHUNK_SIZE);
                    } else {
                        tgtX = (targetPos.x - worldBoundsMin.x) * actualChunkSize + actualChunkSize / 2;
                        tgtY = (targetPos.y - worldBoundsMin.y) * actualChunkSize + actualChunkSize / 2;
                    }

                    drawArrow(buf, srcX, srcY, tgtX, tgtY, COLOR_CONNECTION);
                }
            }
        }

        // Draw path
        if (showPath && lastPath != null && !lastPath.segments.isEmpty()) {
            Map<Long, Coord> gridToPos = new HashMap<>();
            for (ChunkNavData chunk : chunks) {
                gridToPos.put(chunk.gridId, positions.get(chunk.gridId));
            }

            List<Coord> pathPoints = new ArrayList<>();
            for (ChunkPath.PathSegment seg : lastPath.segments) {
                Coord pos = gridToPos.get(seg.gridId);
                if (pos == null) continue;

                int chunkPx = (pos.x - worldBoundsMin.x) * actualChunkSize;
                int chunkPy = (pos.y - worldBoundsMin.y) * actualChunkSize;

                for (ChunkPath.TileStep step : seg.steps) {
                    int px = chunkPx + (step.localCoord.x * actualChunkSize / CHUNK_SIZE);
                    int py = chunkPy + (step.localCoord.y * actualChunkSize / CHUNK_SIZE);
                    pathPoints.add(new Coord(px, py));
                }
            }

            // Draw path lines
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Coord p1 = pathPoints.get(i);
                Coord p2 = pathPoints.get(i + 1);
                drawLine(buf, p1.x, p1.y, p2.x, p2.y, COLOR_PATH, 2);
            }

            // Draw start and end markers
            if (!pathPoints.isEmpty()) {
                int markerSize = Math.max(3, actualChunkSize / 15);
                Coord start = pathPoints.get(0);
                Coord end = pathPoints.get(pathPoints.size() - 1);
                drawMarker(buf, start.x, start.y, markerSize, COLOR_PATH_START);
                drawMarker(buf, end.x, end.y, markerSize, COLOR_PATH_END);
            }
        }

        worldTex = new TexI(PUtils.rasterimg(buf));
    }

    private void regenerateDetailTexture() {
        if (selectedChunkIdx < 0 || selectedChunkIdx >= chunks.size()) {
            detailTex = null;
            return;
        }

        ChunkNavData chunk = chunks.get(selectedChunkIdx);
        int texSize = DETAIL_SIZE;

        WritableRaster buf = PUtils.imgraster(new Coord(texSize, texSize));

        // Draw walkability at cell level
        float cellSize = (float) texSize / CELLS_PER_EDGE;
        for (int cy = 0; cy < CELLS_PER_EDGE; cy++) {
            for (int cx = 0; cx < CELLS_PER_EDGE; cx++) {
                boolean obs = chunk.observed[cx][cy];
                byte walk = chunk.walkability[cx][cy];

                Color color;
                if (!obs) {
                    color = COLOR_UNOBSERVED;
                } else if (walk == 0) {
                    color = COLOR_WALKABLE;
                } else {
                    color = COLOR_BLOCKED;
                }

                int px = (int) (cx * cellSize);
                int py = (int) (cy * cellSize);
                int pw = (int) ((cx + 1) * cellSize) - px;
                int ph = (int) ((cy + 1) * cellSize) - py;

                for (int y = py; y < py + ph && y < texSize; y++) {
                    for (int x = px; x < px + pw && x < texSize; x++) {
                        setPixel(buf, x, y, color);
                    }
                }
            }
        }

        // Draw cell lines
        if (showCellLines && cellSize > 1) {
            Color gridColor = new Color(60, 60, 60);
            for (int i = 0; i <= CELLS_PER_EDGE; i++) {
                int pos = (int) (i * cellSize);
                if (pos < texSize) {
                    for (int j = 0; j < texSize; j++) {
                        setPixel(buf, pos, j, gridColor);
                        setPixel(buf, j, pos, gridColor);
                    }
                }
            }
        }

        // Draw portals
        if (showPortals) {
            int markerSize = Math.max(4, (int) (cellSize * 2));
            for (ChunkPortal portal : chunk.portals) {
                // Portal coords are tile coords (0-99), convert to cell coords (*2)
                int cellX = portal.localCoord.x * 2;
                int cellY = portal.localCoord.y * 2;
                int px = (int) (cellX * cellSize + cellSize / 2);
                int py = (int) (cellY * cellSize + cellSize / 2);

                drawMarker(buf, px, py, markerSize, COLOR_PORTAL);
            }
        }

        // Draw path segments in this chunk
        if (showPath && lastPath != null) {
            long chunkGridId = chunk.gridId;
            float tileSize = (float) texSize / CHUNK_SIZE;

            for (ChunkPath.PathSegment seg : lastPath.segments) {
                if (seg.gridId != chunkGridId) continue;

                List<Coord> points = new ArrayList<>();
                for (ChunkPath.TileStep step : seg.steps) {
                    int px = (int) (step.localCoord.x * tileSize + tileSize / 2);
                    int py = (int) (step.localCoord.y * tileSize + tileSize / 2);
                    points.add(new Coord(px, py));
                }

                // Draw path lines
                int lineWidth = Math.max(2, (int) cellSize);
                for (int i = 0; i < points.size() - 1; i++) {
                    Coord p1 = points.get(i);
                    Coord p2 = points.get(i + 1);
                    drawLine(buf, p1.x, p1.y, p2.x, p2.y, COLOR_PATH, lineWidth);
                }

                // Mark segment start and end
                if (!points.isEmpty()) {
                    int markerR = Math.max(4, (int) (cellSize * 2));
                    Coord start = points.get(0);
                    Coord end = points.get(points.size() - 1);
                    drawMarker(buf, start.x, start.y, markerR, COLOR_PATH_START);
                    drawMarker(buf, end.x, end.y, markerR, new Color(255, 165, 0)); // Orange
                }
            }
        }

        detailTex = new TexI(PUtils.rasterimg(buf));
    }

    private void drawScaledChunk(WritableRaster buf, ChunkNavData chunk, int px, int py, int chunkSize) {
        float stepX = (float) CELLS_PER_EDGE / chunkSize;
        float stepY = (float) CELLS_PER_EDGE / chunkSize;

        for (int y = 0; y < chunkSize; y++) {
            for (int x = 0; x < chunkSize; x++) {
                int imgX = px + x;
                int imgY = py + y;
                if (imgX >= worldTexWidth || imgY >= worldTexHeight) continue;

                int srcX = (int) (x * stepX);
                int srcY = (int) (y * stepY);
                if (srcX >= CELLS_PER_EDGE) srcX = CELLS_PER_EDGE - 1;
                if (srcY >= CELLS_PER_EDGE) srcY = CELLS_PER_EDGE - 1;

                boolean obs = chunk.observed[srcX][srcY];
                byte walk = chunk.walkability[srcX][srcY];

                Color color;
                if (!obs) {
                    color = COLOR_UNOBSERVED;
                } else if (walk == 0) {
                    color = COLOR_WALKABLE;
                } else {
                    color = COLOR_BLOCKED;
                }

                setPixel(buf, imgX, imgY, color);
            }
        }
    }

    private void drawSelectionBorder(WritableRaster buf, int px, int py, int size, int borderWidth) {
        for (int i = 0; i < size; i++) {
            for (int b = 0; b < borderWidth; b++) {
                // Top
                if (py + b < worldTexHeight && px + i < worldTexWidth) {
                    setPixel(buf, px + i, py + b, COLOR_SELECTED);
                }
                // Bottom
                if (py + size - 1 - b < worldTexHeight && px + i < worldTexWidth) {
                    setPixel(buf, px + i, py + size - 1 - b, COLOR_SELECTED);
                }
                // Left
                if (py + i < worldTexHeight && px + b < worldTexWidth) {
                    setPixel(buf, px + b, py + i, COLOR_SELECTED);
                }
                // Right
                if (py + i < worldTexHeight && px + size - 1 - b < worldTexWidth) {
                    setPixel(buf, px + size - 1 - b, py + i, COLOR_SELECTED);
                }
            }
        }
    }

    private void drawMarker(WritableRaster buf, int cx, int cy, int radius, Color color) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                if (x >= 0 && x < buf.getWidth() && y >= 0 && y < buf.getHeight()) {
                    setPixel(buf, x, y, color);
                }
            }
        }
    }

    private void drawLine(WritableRaster buf, int x0, int y0, int x1, int y1, Color color, int thickness) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0, y = y0;

        while (true) {
            for (int ty = -thickness / 2; ty <= thickness / 2; ty++) {
                for (int tx = -thickness / 2; tx <= thickness / 2; tx++) {
                    int px = x + tx;
                    int py = y + ty;
                    if (px >= 0 && px < buf.getWidth() && py >= 0 && py < buf.getHeight()) {
                        setPixel(buf, px, py, color);
                    }
                }
            }

            if (x == x1 && y == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private void drawArrow(WritableRaster buf, int x0, int y0, int x1, int y1, Color color) {
        drawLine(buf, x0, y0, x1, y1, color, 1);

        // Arrow head
        double angle = Math.atan2(y1 - y0, x1 - x0);
        int arrowLen = 5;
        double arrowAngle = Math.PI / 7;

        int ax1 = (int) (x1 - arrowLen * Math.cos(angle - arrowAngle));
        int ay1 = (int) (y1 - arrowLen * Math.sin(angle - arrowAngle));
        int ax2 = (int) (x1 - arrowLen * Math.cos(angle + arrowAngle));
        int ay2 = (int) (y1 - arrowLen * Math.sin(angle + arrowAngle));

        drawLine(buf, x1, y1, ax1, ay1, color, 1);
        drawLine(buf, x1, y1, ax2, ay2, color, 1);
    }

    private void setPixel(WritableRaster buf, int x, int y, Color color) {
        if (x < 0 || x >= buf.getWidth() || y < 0 || y >= buf.getHeight()) return;
        buf.setSample(x, y, 0, color.getRed());
        buf.setSample(x, y, 1, color.getGreen());
        buf.setSample(x, y, 2, color.getBlue());
        buf.setSample(x, y, 3, color.getAlpha());
    }

    /**
     * Select chunk at texture coordinates.
     */
    private void selectChunkAtTexture(int texX, int texY) {
        if (worldTex == null || worldTexWidth == 0 || worldTexHeight == 0) return;

        // Calculate chunk size in texture
        int gridWidth = worldBoundsMax.x - worldBoundsMin.x + 1;
        int gridHeight = worldBoundsMax.y - worldBoundsMin.y + 1;
        if (gridWidth == 0 || gridHeight == 0) return;

        int chunkSizeX = worldTexWidth / gridWidth;
        int chunkSizeY = worldTexHeight / gridHeight;
        if (chunkSizeX == 0 || chunkSizeY == 0) return;

        // Find grid position
        int gridX = texX / chunkSizeX + worldBoundsMin.x;
        int gridY = texY / chunkSizeY + worldBoundsMin.y;

        // Find chunk at this position
        for (int i = 0; i < chunks.size(); i++) {
            ChunkNavData chunk = chunks.get(i);
            Coord pos = positions.get(chunk.gridId);
            if (pos != null && pos.x == gridX && pos.y == gridY) {
                selectedChunkIdx = i;
                updateSelectedLabel();
                regenerateTextures();
                return;
            }
        }
    }

    private void updateSelectedLabel() {
        if (selectedChunkIdx >= 0 && selectedChunkIdx < chunks.size()) {
            ChunkNavData chunk = chunks.get(selectedChunkIdx);
            int portalCount = chunk.portals.size();
            int observedCount = 0;
            for (int x = 0; x < CELLS_PER_EDGE; x++) {
                for (int y = 0; y < CELLS_PER_EDGE; y++) {
                    if (chunk.observed[x][y]) observedCount++;
                }
            }
            int obsPct = observedCount * 100 / (CELLS_PER_EDGE * CELLS_PER_EDGE);
            String layer = chunk.layer != null ? chunk.layer : "outside";
            selectedLabel.settext(String.format("%s\nID: %d\nPortals: %d\nObs: %d%%",
                    layer, chunk.gridId, portalCount, obsPct));
            deleteChunkBtn.disable(false);  // Enable delete button
        } else {
            selectedLabel.settext("None");
            deleteChunkBtn.disable(true);   // Disable delete button
        }
    }

    /**
     * Delete the currently selected chunk from memory and disk.
     */
    private void deleteSelectedChunk() {
        if (selectedChunkIdx < 0 || selectedChunkIdx >= chunks.size()) {
            return;
        }

        ChunkNavData chunk = chunks.get(selectedChunkIdx);
        long gridId = chunk.gridId;

        ChunkNavManager manager = getChunkNavManager();
        if (manager == null) {
            statusLabel.settext("Error: ChunkNav not available");
            return;
        }

        // Perform deletion
        boolean success = manager.deleteChunk(gridId);
        if (success) {
            statusLabel.settext("Deleted chunk " + gridId);
            // Clear selection and reload data
            selectedChunkIdx = -1;
            reloadData();
        } else {
            statusLabel.settext("Failed to delete chunk");
        }
    }

    /**
     * Delete ALL chunks from memory and disk.
     */
    private void deleteAllChunks() {
        ChunkNavManager manager = getChunkNavManager();
        if (manager == null) {
            statusLabel.settext("Error: ChunkNav not available");
            return;
        }

        int count = manager.deleteAllChunks();
        statusLabel.settext("Deleted " + count + " chunks");

        // Clear selection and reload data
        selectedChunkIdx = -1;
        reloadData();
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
            reqdestroy();
        } else {
            super.wdgmsg(msg, args);
        }
    }

    /**
     * Canvas for world overview display with pan/zoom.
     */
    private class WorldCanvas extends Widget {
        private UI.Grab grab = null;

        public WorldCanvas(Coord sz) {
            super(sz);
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(COLOR_BACKGROUND);
            g.frect(Coord.z, sz);
            g.chcolor();

            if (worldTex != null) {
                // Calculate base scaling to fit texture in canvas
                float baseScaleX = (float) sz.x / worldTexWidth;
                float baseScaleY = (float) sz.y / worldTexHeight;
                float baseScale = Math.min(baseScaleX, baseScaleY);

                // Apply zoom
                float scale = baseScale * zoom;

                int drawW = (int) (worldTexWidth * scale);
                int drawH = (int) (worldTexHeight * scale);

                // Apply pan (centered initially)
                int drawX = (int) ((sz.x - drawW) / 2 + panX);
                int drawY = (int) ((sz.y - drawH) / 2 + panY);

                g.image(worldTex, new Coord(drawX, drawY), new Coord(drawW, drawH));

                // Draw zoom level indicator
                g.chcolor(Color.WHITE);
                g.atext(String.format("Zoom: %.0f%%", zoom * 100), new Coord(5, sz.y - 5), 0, 1);
                g.chcolor();
            } else {
                g.chcolor(Color.WHITE);
                g.atext("No data - click Reload", sz.div(2), 0.5, 0.5);
                g.chcolor();
            }
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 1) {
                // Left click - select chunk
                Coord texCoord = canvasToTexture(ev.c);
                if (texCoord != null) {
                    selectChunkAtTexture(texCoord.x, texCoord.y);
                }
                return true;
            } else if (ev.b == 3) {
                // Right click - start drag for pan
                dragging = true;
                dragStart = ev.c;
                dragStartPanX = panX;
                dragStartPanY = panY;
                grab = ui.grabmouse(this);
                return true;
            }
            return super.mousedown(ev);
        }

        @Override
        public boolean mouseup(MouseUpEvent ev) {
            if (ev.b == 3 && dragging) {
                dragging = false;
                if (grab != null) {
                    grab.remove();
                    grab = null;
                }
                return true;
            }
            return super.mouseup(ev);
        }

        @Override
        public void mousemove(MouseMoveEvent ev) {
            if (dragging && dragStart != null) {
                panX = dragStartPanX + (ev.c.x - dragStart.x);
                panY = dragStartPanY + (ev.c.y - dragStart.y);
            }
            super.mousemove(ev);
        }

        @Override
        public boolean mousewheel(MouseWheelEvent ev) {
            // Zoom centered on mouse position
            float oldZoom = zoom;
            if (ev.a < 0) {
                zoom = Math.min(10.0f, zoom * 1.2f);
            } else {
                zoom = Math.max(0.1f, zoom / 1.2f);
            }

            // Adjust pan to zoom toward mouse position
            if (worldTex != null) {
                float zoomFactor = zoom / oldZoom;
                float cx = ev.c.x - sz.x / 2f;
                float cy = ev.c.y - sz.y / 2f;
                panX = (panX - cx) * zoomFactor + cx;
                panY = (panY - cy) * zoomFactor + cy;
            }

            return true;
        }

        /**
         * Convert canvas coordinates to texture coordinates.
         * Returns null if outside the drawn area.
         */
        private Coord canvasToTexture(Coord canvasCoord) {
            if (worldTex == null || worldTexWidth == 0 || worldTexHeight == 0) return null;

            float baseScaleX = (float) sz.x / worldTexWidth;
            float baseScaleY = (float) sz.y / worldTexHeight;
            float baseScale = Math.min(baseScaleX, baseScaleY);
            float scale = baseScale * zoom;

            int drawW = (int) (worldTexWidth * scale);
            int drawH = (int) (worldTexHeight * scale);
            int drawX = (int) ((sz.x - drawW) / 2 + panX);
            int drawY = (int) ((sz.y - drawH) / 2 + panY);

            int relX = canvasCoord.x - drawX;
            int relY = canvasCoord.y - drawY;

            if (relX < 0 || relX >= drawW || relY < 0 || relY >= drawH) return null;

            int texX = (int) (relX / scale);
            int texY = (int) (relY / scale);

            return new Coord(texX, texY);
        }
    }

    /**
     * Canvas for chunk detail display.
     */
    private class DetailCanvas extends Widget {
        public DetailCanvas(Coord sz) {
            super(sz);
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(COLOR_BACKGROUND);
            g.frect(Coord.z, sz);
            g.chcolor();

            if (detailTex != null) {
                // Draw square texture maintaining aspect ratio
                int drawSize = Math.min(sz.x, sz.y);
                int drawX = (sz.x - drawSize) / 2;
                int drawY = (sz.y - drawSize) / 2;
                g.image(detailTex, new Coord(drawX, drawY), new Coord(drawSize, drawSize));
            } else {
                g.chcolor(Color.WHITE);
                g.atext("Select a chunk", sz.div(2), 0.5, 0.5);
                g.chcolor();
            }
        }
    }
}
