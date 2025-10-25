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
 */
public class NRockTileHighlightOverlay extends NOverlay {

    public static final int ROCK_TILE_OVERLAY = -2;

    private Set<String> selectedTileResources = new HashSet<>();
    private boolean needsUpdate = false;
    private boolean isEnabled = true;

    // Cache for Icon Settings to detect changes
    private Map<GobIcon.Setting.ID, Boolean> lastIconStates = new HashMap<>();

    public NRockTileHighlightOverlay() {
        super(ROCK_TILE_OVERLAY);
        bc = new Color(255, 200, 100, 100); // Orange-ish color for rock highlights
    }

    /**
     * Updates which rock tile resources should be highlighted based on Icon Settings.
     */
    public void updateSelectedRocks() {
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
                        currentStates.put(setting.id, setting.show);
                    }
                }
            }

            // Check if anything changed
            boolean changed = !currentStates.equals(lastIconStates);
            lastIconStates = currentStates;

            if (changed) {
                // Convert selected gob resources to tile resources
                selectedTileResources = RockResourceMapper.getTileResourcesToHighlight(newSelectedGobResources);
                needsUpdate = true;
                requpdate2 = true;  // Trigger map re-render
            }
        } catch (Exception e) {
            // Silently ignore errors to avoid breaking the game
        }
    }

    /**
     * Checks if a tile should be highlighted based on its resource name.
     */
    private boolean shouldHighlightTile(Coord gc) {
        if (!isEnabled || selectedTileResources.isEmpty()) {
            return false;
        }

        try {
            MCache map = NUtils.getGameUI().map.glob.map;
            int tileId = map.gettile(gc);

            if (tileId < 0 || tileId >= map.nsets.length) {
                return false;
            }

            Resource.Spec tileSpec = map.nsets[tileId];
            if (tileSpec == null) {
                return false;
            }

            String tileResourceName = tileSpec.name;

            if (selectedTileResources.contains(tileResourceName)) {
                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Check if feature is enabled in config
        boolean configEnabled = (Boolean) NConfig.get(NConfig.Key.highlightRockTiles);
        if (isEnabled != configEnabled) {
            setEnabled(configEnabled);
        }

        // Periodically check if icon settings changed
        if (isEnabled) {
            updateSelectedRocks();
        }
    }

    // Directions for checking neighbors (N, E, S, W)
    private static final Coord[] NEIGHBOR_OFFSETS = {
        new Coord(0, -1),  // North
        new Coord(1, 0),   // East
        new Coord(0, 1),   // South
        new Coord(-1, 0)   // West
    };

    // Corner coords for wall quads
    private static final Coord[] WALL_CORNERS = {
        new Coord(0, 0),
        new Coord(1, 0),
        new Coord(1, 1),
        new Coord(0, 1)
    };

    /**
     * Checks if a tile at the given coordinate should have a wall on the given edge.
     * A wall exists if the neighbor is not a rock tile or is out of bounds.
     */
    private boolean shouldHaveWall(MCache map, Coord gc, int direction) {
        if (!shouldHighlightTile(gc)) {
            return false;
        }

        Coord neighbor = gc.add(NEIGHBOR_OFFSETS[direction]);

        // If neighbor is highlighted rock, no wall needed
        if (shouldHighlightTile(neighbor)) {
            return false;
        }

        // Wall needed if neighbor is different type
        return true;
    }

    @Override
    public RenderTree.Node makenol(MapMesh mm, Long grid_id, Coord grid_ul) {
        if (selectedTileResources.isEmpty() || !isEnabled) {
            return null;
        }

        MCache map = NUtils.getGameUI().map.glob.map;
        MapMesh.MapSurface ms = mm.data(MapMesh.gnd);

        if (ms == null) {
            return null;
        }

        // Wall height
        final float wallHeight = 16.0f;

        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Short> indices = new ArrayList<>();
        short vertexCount = 0;

        // Scan each tile in the mesh
        for (int ty = 0; ty < mm.sz.y; ty++) {
            for (int tx = 0; tx < mm.sz.x; tx++) {
                Coord lc = new Coord(tx, ty);
                Coord gc = lc.add(mm.ul);

                // Check each direction for walls
                for (int dir = 0; dir < 4; dir++) {
                    if (shouldHaveWall(map, gc, dir)) {
                        // Create a vertical wall quad
                        // Get the two corners for this wall edge
                        Coord c1 = lc.add(WALL_CORNERS[(dir + 1) % 4]);
                        Coord c2 = lc.add(WALL_CORNERS[dir]);

                        // Get surface vertices at these corners
                        Surface.Vertex sv1 = ms.fortile(c1);
                        Surface.Vertex sv2 = ms.fortile(c2);

                        // Bottom-left vertex
                        vertices.add(sv1.x);
                        vertices.add(sv1.y);
                        vertices.add(sv1.z);

                        // Bottom-right vertex
                        vertices.add(sv2.x);
                        vertices.add(sv2.y);
                        vertices.add(sv2.z);

                        // Top-right vertex
                        vertices.add(sv2.x);
                        vertices.add(sv2.y);
                        vertices.add(sv2.z + wallHeight);

                        // Top-left vertex
                        vertices.add(sv1.x);
                        vertices.add(sv1.y);
                        vertices.add(sv1.z + wallHeight);

                        // Create two triangles for the quad
                        // Triangle 1: bottom-left, bottom-right, top-right
                        indices.add(vertexCount);
                        indices.add((short)(vertexCount + 1));
                        indices.add((short)(vertexCount + 2));

                        // Triangle 2: bottom-left, top-right, top-left
                        indices.add(vertexCount);
                        indices.add((short)(vertexCount + 2));
                        indices.add((short)(vertexCount + 3));

                        vertexCount += 4;
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

        // Create model
        haven.render.Model mod = new haven.render.Model(
            haven.render.Model.Mode.TRIANGLES,
            vbuf.data(),
            new haven.render.Model.Indices(indices.size(), NumberFormat.UINT16,
                DataBuffer.Usage.STATIC, DataBuffer.Filler.of(idxb.array()))
        );

        // Apply color to the walls
        Pipe.Op colorOp = new BaseColor(new java.awt.Color(
            bc.getRed(),
            bc.getGreen(),
            bc.getBlue(),
            bc.getAlpha()
        ));

        return new MapMesh.ShallowWrap(mod,
            Pipe.Op.compose(new MapMesh.NOLOrder(id), colorOp));
    }

    @Override
    public RenderTree.Node makenolol(MapMesh mm, Long grid_id, Coord grid_ul) {
        // No outline needed for wall highlighting
        return null;
    }

    @Override
    public boolean requpdate() {
        boolean result = needsUpdate;
        needsUpdate = false;
        if (result) {
            requpdate2 = false;  // Reset after update processed
        }
        return result;
    }

    /**
     * Sets whether this overlay is enabled.
     */
    public void setEnabled(boolean enabled) {
        if (this.isEnabled != enabled) {
            this.isEnabled = enabled;
            needsUpdate = true;
        }
    }

    /**
     * Returns whether this overlay is enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }
}
