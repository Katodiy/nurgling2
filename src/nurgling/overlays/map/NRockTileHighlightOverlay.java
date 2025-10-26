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
        bc = new Color(255, 200, 100, 80); // Orange-ish semi-transparent color
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

    // Corner coords for tile box
    private static final Coord[] TILE_CORNERS = {
        new Coord(0, 0),
        new Coord(1, 0),
        new Coord(1, 1),
        new Coord(0, 1)
    };

    /**
     * Helper method to add a quad (4 vertices forming 2 triangles) to the mesh
     */
    private void addQuad(ArrayList<Float> vertices, ArrayList<Short> indices, short baseVertex,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float x4, float y4, float z4) {
        // Add 4 vertices
        vertices.add(x1); vertices.add(y1); vertices.add(z1);
        vertices.add(x2); vertices.add(y2); vertices.add(z2);
        vertices.add(x3); vertices.add(y3); vertices.add(z3);
        vertices.add(x4); vertices.add(y4); vertices.add(z4);

        // Add 2 triangles (6 indices)
        indices.add(baseVertex);
        indices.add((short)(baseVertex + 1));
        indices.add((short)(baseVertex + 2));

        indices.add(baseVertex);
        indices.add((short)(baseVertex + 2));
        indices.add((short)(baseVertex + 3));
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

        final float boxHeight = 18.0f; // Height of the colored box

        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Short> indices = new ArrayList<>();
        short vertexCount = 0;

        // Scan each tile in the mesh
        for (int ty = 0; ty < mm.sz.y; ty++) {
            for (int tx = 0; tx < mm.sz.x; tx++) {
                Coord lc = new Coord(tx, ty);
                Coord gc = lc.add(mm.ul);

                // Check if this tile should be highlighted
                if (shouldHighlightTile(gc)) {
                    // Get the 4 corner vertices for this tile
                    Surface.Vertex[] corners = new Surface.Vertex[4];
                    for (int i = 0; i < 4; i++) {
                        corners[i] = ms.fortile(lc.add(TILE_CORNERS[i]));
                    }

                    // Create a box with 6 faces around this tile
                    // Bottom face (floor)
                    addQuad(vertices, indices, vertexCount,
                           corners[0].x, corners[0].y, corners[0].z,
                           corners[1].x, corners[1].y, corners[1].z,
                           corners[2].x, corners[2].y, corners[2].z,
                           corners[3].x, corners[3].y, corners[3].z);
                    vertexCount += 4;

                    // Top face (ceiling)
                    addQuad(vertices, indices, vertexCount,
                           corners[0].x, corners[0].y, corners[0].z + boxHeight,
                           corners[3].x, corners[3].y, corners[3].z + boxHeight,
                           corners[2].x, corners[2].y, corners[2].z + boxHeight,
                           corners[1].x, corners[1].y, corners[1].z + boxHeight);
                    vertexCount += 4;

                    // North wall (front)
                    addQuad(vertices, indices, vertexCount,
                           corners[0].x, corners[0].y, corners[0].z,
                           corners[1].x, corners[1].y, corners[1].z,
                           corners[1].x, corners[1].y, corners[1].z + boxHeight,
                           corners[0].x, corners[0].y, corners[0].z + boxHeight);
                    vertexCount += 4;

                    // East wall (right)
                    addQuad(vertices, indices, vertexCount,
                           corners[1].x, corners[1].y, corners[1].z,
                           corners[2].x, corners[2].y, corners[2].z,
                           corners[2].x, corners[2].y, corners[2].z + boxHeight,
                           corners[1].x, corners[1].y, corners[1].z + boxHeight);
                    vertexCount += 4;

                    // South wall (back)
                    addQuad(vertices, indices, vertexCount,
                           corners[2].x, corners[2].y, corners[2].z,
                           corners[3].x, corners[3].y, corners[3].z,
                           corners[3].x, corners[3].y, corners[3].z + boxHeight,
                           corners[2].x, corners[2].y, corners[2].z + boxHeight);
                    vertexCount += 4;

                    // West wall (left)
                    addQuad(vertices, indices, vertexCount,
                           corners[3].x, corners[3].y, corners[3].z,
                           corners[0].x, corners[0].y, corners[0].z,
                           corners[0].x, corners[0].y, corners[0].z + boxHeight,
                           corners[3].x, corners[3].y, corners[3].z + boxHeight);
                    vertexCount += 4;
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

        // Apply semi-transparent color with disabled backface culling for visibility
        Pipe.Op colorOp = Pipe.Op.compose(
            new BaseColor(new java.awt.Color(bc.getRed(), bc.getGreen(), bc.getBlue(), bc.getAlpha())),
            new States.Facecull(States.Facecull.Mode.NONE)  // Render both sides
        );

        return new MapMesh.ShallowWrap(mod,
            Pipe.Op.compose(new MapMesh.NOLOrder(id), colorOp));
    }

    @Override
    public void added(RenderTree.Slot slot) {
        this.slot = slot;
        // Add the base grid so makenol() gets called, but we won't use area-based rendering
        slot.add(base, new States.Depthtest(States.Depthtest.Test.TRUE));
        // Don't call super.added() to avoid NOverlay's BaseColor null pointer
    }

    @Override
    public RenderTree.Node makenolol(MapMesh mm, Long grid_id, Coord grid_ul) {
        // No outline needed
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
