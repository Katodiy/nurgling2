package nurgling.overlays.map;

import haven.*;
import haven.render.*;
import nurgling.*;
import nurgling.tools.RockResourceMapper;

import java.awt.*;
import java.util.*;

/**
 * Overlay that highlights rock tiles in mines when corresponding bumbling icons are selected.
 * This allows users to see both surface rocks (bumblings) and underground rock tiles
 * when they enable a rock type in Icon Settings.
 *
 * Optimized to avoid FPS drops by:
 * - Pre-caching which tiles to highlight instead of checking every tile every frame
 * - Only updating when icon selection changes or config toggles
 * - Using static color instead of animated pulsing
 */
public class NRockTileHighlightOverlay extends NOverlay {

    public static final int ROCK_TILE_OVERLAY = -2;

    // Cache of tile resource names that should be highlighted
    private Set<String> selectedTileResources = new HashSet<>();

    // Tracks when we need to rebuild the mesh
    private boolean needsRebuild = false;

    // Tracks if feature is enabled
    private boolean isEnabled = false;

    // Cache for Icon Settings to detect changes
    private Map<GobIcon.Setting.ID, Boolean> lastIconStates = new HashMap<>();

    // Bright red color for border
    private static final Color HIGHLIGHT_COLOR = new Color(255, 0, 0, 255); // Bright red

    public NRockTileHighlightOverlay() {
        super(ROCK_TILE_OVERLAY);
        bc = HIGHLIGHT_COLOR;

        // Initialize enabled state from config
        try {
            Boolean enabled = (Boolean) NConfig.get(NConfig.Key.highlightRockTiles);
            isEnabled = (enabled != null && enabled);
        } catch (Exception e) {
            isEnabled = false;
        }
    }

    /**
     * Updates which rock tile resources should be highlighted based on Icon Settings.
     * Only called when we detect icon settings may have changed.
     */
    private void updateSelectedRocks() {
        if (NUtils.getGameUI() == null || NUtils.getGameUI().ui == null) {
            return;
        }

        try {
            GobIcon.Settings iconConf = NUtils.getGameUI().iconconf;
            if (iconConf == null) {
                return;
            }

            Set<String> newSelectedGobResources = new HashSet<>();
            Map<GobIcon.Setting.ID, Boolean> currentStates = new HashMap<>();

            // Collect all selected icons from the settings
            synchronized (iconConf.settings) {
                for (GobIcon.Setting setting : iconConf.settings.values()) {
                    if (setting.show && setting.icon != null && setting.icon.res != null) {
                        String resName = setting.icon.res.name;
                        newSelectedGobResources.add(resName);
                        currentStates.put(setting.id, true);
                    }
                }
            }

            // Check if anything changed
            if (!currentStates.equals(lastIconStates)) {
                lastIconStates = currentStates;

                // Convert selected gob resources to tile resources
                selectedTileResources = RockResourceMapper.getTileResourcesToHighlight(newSelectedGobResources);
                needsRebuild = true;
            }
        } catch (Exception e) {
            // Silently ignore errors to avoid breaking the game
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Check if feature is enabled in config
        try {
            Boolean configEnabled = (Boolean) NConfig.get(NConfig.Key.highlightRockTiles);
            boolean newEnabled = (configEnabled != null && configEnabled);

            if (isEnabled != newEnabled) {
                isEnabled = newEnabled;
                needsRebuild = true;
            }
        } catch (Exception e) {
            // Ignore
        }

        // Only check icon settings if enabled (avoids unnecessary work)
        if (isEnabled) {
            updateSelectedRocks();
        }

        // Signal map to rebuild if needed
        requpdate2 = requpdate();
    }

    @Override
    public boolean requpdate() {
        if (needsRebuild) {
            needsRebuild = false;
            return true;
        }
        return false;
    }

    @Override
    public RenderTree.Node makenol(MapMesh mm, Long grid_id, Coord grid_ul) {
        // No fill - only render outline
        return null;
    }

    @Override
    public RenderTree.Node makenolol(MapMesh mm, Long grid_id, Coord grid_ul) {
        // Don't render if disabled or no rocks selected
        if (!isEnabled || selectedTileResources.isEmpty()) {
            return null;
        }

        MCache map = mm.map;
        MapMesh.MapSurface ms = mm.data(MapMesh.gnd);

        if (ms == null) {
            return null;
        }

        // Check if short walls are enabled to determine wall height
        boolean shortWalls = false;
        float wallHeight = 16.0f; // Default to full height
        try {
            Boolean sw = (Boolean) NConfig.get(NConfig.Key.shortWalls);
            if (sw != null && sw) {
                shortWalls = true;
                wallHeight = 4.0f; // SHORT_H from NCaveTile
            }
        } catch (Exception e) {
            // Use default
        }

        // Build boolean grid (same as makenol)
        boolean[][] highlightGrid = new boolean[mm.sz.x][mm.sz.y];

        for (int ty = 0; ty < mm.sz.y; ty++) {
            for (int tx = 0; tx < mm.sz.x; tx++) {
                Coord gc = new Coord(tx, ty).add(mm.ul);

                try {
                    int tileId = map.gettile(gc);

                    if (tileId >= 0 && tileId < map.nsets.length) {
                        Resource.Spec tileSpec = map.nsets[tileId];

                        if (tileSpec != null && selectedTileResources.contains(tileSpec.name)) {
                            highlightGrid[tx][ty] = true;
                        }
                    }
                } catch (Exception e) {
                    // Skip
                }
            }
        }

        // Create custom outline geometry for expanded tiles
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Short> indices = new ArrayList<>();
        short vertexCount = 0;

        // Same expansion as fill overlay
        final float EXPAND = 0.15f;

        for (int ty = 0; ty < mm.sz.y; ty++) {
            for (int tx = 0; tx < mm.sz.x; tx++) {
                if (highlightGrid[tx][ty]) {
                    Coord lc = new Coord(tx, ty);

                    // Get the 4 corner vertices
                    Surface.Vertex v0 = ms.fortile(lc.add(0, 0));
                    Surface.Vertex v1 = ms.fortile(lc.add(1, 0));
                    Surface.Vertex v2 = ms.fortile(lc.add(1, 1));
                    Surface.Vertex v3 = ms.fortile(lc.add(0, 1));

                    // Calculate tile center
                    float cx = (v0.x + v1.x + v2.x + v3.x) / 4.0f;
                    float cy = (v0.y + v1.y + v2.y + v3.y) / 4.0f;
                    float cz = (v0.z + v1.z + v2.z + v3.z) / 4.0f;

                    // Create expanded vertices at the TOP of the wall (ground + wallHeight)
                    float[] ev0 = {cx + (v0.x - cx),
                                   cy + (v0.y - cy),
                                   cz + (v0.z - cz) + wallHeight + 0.1f};
                    float[] ev1 = {cx + (v1.x - cx),
                                   cy + (v1.y - cy),
                                   cz + (v1.z - cz) + wallHeight + 0.1f};
                    float[] ev2 = {cx + (v2.x - cx),
                                   cy + (v2.y - cy),
                                   cz + (v2.z - cz) + wallHeight + 0.1f};
                    float[] ev3 = {cx + (v3.x - cx),
                                   cy + (v3.y - cy),
                                   cz + (v3.z - cz) + wallHeight + 0.1f};

                    // Check which edges are boundaries (no adjacent highlighted tile)
                    boolean[] edges = new boolean[4]; // bottom, right, top, left
                    edges[0] = (ty == 0 || !highlightGrid[tx][ty - 1]); // Bottom
                    edges[1] = (tx == mm.sz.x - 1 || !highlightGrid[tx + 1][ty]); // Right
                    edges[2] = (ty == mm.sz.y - 1 || !highlightGrid[tx][ty + 1]); // Top
                    edges[3] = (tx == 0 || !highlightGrid[tx - 1][ty]); // Left

                    // Add line segments for boundary edges
                    if (edges[0]) { // Bottom edge: v0 -> v1
                        vertices.add(ev0[0]); vertices.add(ev0[1]); vertices.add(ev0[2]);
                        vertices.add(ev1[0]); vertices.add(ev1[1]); vertices.add(ev1[2]);
                        indices.add(vertexCount); indices.add((short)(vertexCount + 1));
                        vertexCount += 2;
                    }
                    if (edges[1]) { // Right edge: v1 -> v2
                        vertices.add(ev1[0]); vertices.add(ev1[1]); vertices.add(ev1[2]);
                        vertices.add(ev2[0]); vertices.add(ev2[1]); vertices.add(ev2[2]);
                        indices.add(vertexCount); indices.add((short)(vertexCount + 1));
                        vertexCount += 2;
                    }
                    if (edges[2]) { // Top edge: v2 -> v3
                        vertices.add(ev2[0]); vertices.add(ev2[1]); vertices.add(ev2[2]);
                        vertices.add(ev3[0]); vertices.add(ev3[1]); vertices.add(ev3[2]);
                        indices.add(vertexCount); indices.add((short)(vertexCount + 1));
                        vertexCount += 2;
                    }
                    if (edges[3]) { // Left edge: v3 -> v0
                        vertices.add(ev3[0]); vertices.add(ev3[1]); vertices.add(ev3[2]);
                        vertices.add(ev0[0]); vertices.add(ev0[1]); vertices.add(ev0[2]);
                        indices.add(vertexCount); indices.add((short)(vertexCount + 1));
                        vertexCount += 2;
                    }
                }
            }
        }

        if (indices.isEmpty()) {
            return null;
        }

        // Convert ArrayLists to buffers
        java.nio.FloatBuffer posb = Utils.wfbuf(vertices.size());
        for (Float v : vertices) {
            posb.put(v);
        }

        java.nio.ShortBuffer idxb = Utils.wsbuf(indices.size());
        for (Short i : indices) {
            idxb.put(i);
        }

        // Create vertex buffer
        VertexBuf.VertexData posa = new VertexBuf.VertexData(Utils.bufcp(posb));
        VertexBuf vbuf = new VertexBuf(posa);

        // Create outline mesh
        haven.render.Model mod = new haven.render.Model(
            haven.render.Model.Mode.LINES,
            vbuf.data(),
            new haven.render.Model.Indices(indices.size(), NumberFormat.UINT16,
                DataBuffer.Usage.STATIC, DataBuffer.Filler.of(idxb.array()))
        );

        return new MapMesh.ShallowWrap(mod,
            Pipe.Op.compose(new MapMesh.NOLOrder(id), new States.LineWidth(3)));
    }

    @Override
    public void added(RenderTree.Slot slot) {
        this.slot = slot;
        // Only add outline with red color (no fill)
        slot.add(outl, new BaseColor(255, 0, 0, 255)); // Bright red outline
    }

    /**
     * Sets whether this overlay is enabled.
     */
    public void setEnabled(boolean enabled) {
        if (this.isEnabled != enabled) {
            this.isEnabled = enabled;
            needsRebuild = true;
        }
    }

    /**
     * Returns whether this overlay is enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Forces a rebuild of the overlay mesh.
     * Useful when settings change that affect the overlay's appearance (like wall height).
     */
    public void forceRebuild() {
        needsRebuild = true;
    }
}
