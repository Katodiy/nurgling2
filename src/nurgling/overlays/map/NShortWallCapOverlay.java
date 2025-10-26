package nurgling.overlays.map;

import haven.*;
import haven.render.*;
import haven.resutil.CaveTile;
import nurgling.*;

import java.util.*;

/**
 * Overlay that renders textured horizontal caps on top of short mine walls.
 * When shortWalls config is enabled, this adds a visible horizontal surface
 * on top of cave walls at the reduced height (4 units).
 */
public class NShortWallCapOverlay extends NOverlay {

    public static final int SHORT_WALL_CAP_OVERLAY = -3;
    private static final float CAP_HEIGHT = CaveTile.SHORT_H; // 4 units - short wall height
    // How thick the cap is

    // Corner coords for tile
    private static final Coord[] TILE_CORNERS = {
        new Coord(0, 0),
        new Coord(1, 0),
        new Coord(1, 1),
        new Coord(0, 1)
    };

    private boolean lastShortWallsState = false;

    public NShortWallCapOverlay() {
        super(SHORT_WALL_CAP_OVERLAY);
        // Initialize with current state
        try {
            Boolean sw = (Boolean) NConfig.get(NConfig.Key.shortWalls);
            lastShortWallsState = (sw != null && sw);
        } catch (Exception e) {
            // Use default
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Check if shortWalls setting changed
        boolean currentState = false;
        try {
            Boolean sw = (Boolean) NConfig.get(NConfig.Key.shortWalls);
            currentState = (sw != null && sw);
        } catch (Exception e) {
            // Use default
        }

        if (currentState != lastShortWallsState) {
            lastShortWallsState = currentState;
            // Trigger map regeneration when setting changes
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null &&
                NUtils.getGameUI().map.glob != null && NUtils.getGameUI().map.glob.map != null) {
                NUtils.getGameUI().map.glob.map.trimall();
            }
        }
    }

    /**
     * Check if a tile is a cave tile and return the CaveTile instance
     */
    private CaveTile getCaveTile(MCache map, Coord gc) {
        try {
            int tileId = map.gettile(gc);
            if (tileId < 0 || tileId >= map.nsets.length) {
                return null;
            }

            Resource.Spec tileSpec = map.nsets[tileId];
            if (tileSpec == null) {
                return null;
            }

            // Check if tile's tiler is a CaveTile
            Tileset ts = map.tileset(tileId);
            if (ts != null) {
                Tiler tiler = ts.tfac().create(tileId, ts);
                if (tiler instanceof CaveTile) {
                    return (CaveTile) tiler;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Helper to add a quad with texture coordinates
     */
    private void addQuad(ArrayList<Float> vertices, ArrayList<Float> texCoords, ArrayList<Short> indices, short baseVertex,
                        float x1, float y1, float z1, float u1, float v1,
                        float x2, float y2, float z2, float u2, float v2,
                        float x3, float y3, float z3, float u3, float v3,
                        float x4, float y4, float z4, float u4, float v4) {
        // Add 4 vertices
        vertices.add(x1); vertices.add(y1); vertices.add(z1);
        vertices.add(x2); vertices.add(y2); vertices.add(z2);
        vertices.add(x3); vertices.add(y3); vertices.add(z3);
        vertices.add(x4); vertices.add(y4); vertices.add(z4);

        // Add 4 texture coordinates
        texCoords.add(u1); texCoords.add(v1);
        texCoords.add(u2); texCoords.add(v2);
        texCoords.add(u3); texCoords.add(v3);
        texCoords.add(u4); texCoords.add(v4);

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
        // Check if short walls are enabled
        boolean shortWalls;
        try {
            Boolean sw = (Boolean) NConfig.get(NConfig.Key.shortWalls);
            shortWalls = (sw != null && sw);
        } catch (Exception e) {
            return null;
        }

        if (!shortWalls) {
            return null;
        }

        MCache map = NUtils.getGameUI().map.glob.map;
        MapMesh.MapSurface ms = mm.data(MapMesh.gnd);

        if (ms == null) {
            return null;
        }

        // We'll need to group tiles by their material texture
        // Map from Material to list of tile coordinates that use that material
        Map<Material, java.util.List<Coord>> tilesByMaterial = new HashMap<>();

        // Scan each tile in the mesh and group by material
        for (int ty = 0; ty < mm.sz.y; ty++) {
            for (int tx = 0; tx < mm.sz.x; tx++) {
                Coord lc = new Coord(tx, ty);
                Coord gc = lc.add(mm.ul);

                // Check if this is a cave tile
                CaveTile caveTile = getCaveTile(map, gc);
                if (caveTile == null) {
                    continue;
                }

                // Skip tiles with resource "gfx/tiles/cave" - these don't need caps
                try {
                    int tileId = map.gettile(gc);
                    if (tileId >= 0 && tileId < map.nsets.length) {
                        Resource.Spec tileSpec = map.nsets[tileId];
                        if (tileSpec != null && "gfx/tiles/cave".equals(tileSpec.name)) {
                            continue; // Skip this tile
                        }
                    }
                } catch (Exception e) {
                    // If we can't check, proceed with rendering
                }

                // Get ground/floor texture for THIS specific tile
                Material tileTexture = null;
                try {
                    int tileId = map.gettile(gc);
                    Tileset ts = map.tileset(tileId);
                    if (ts != null && ts.getres() != null) {
                        // Try to get ground material from the tile resource
                        Resource res = ts.getres();
                        // Look for material layer in the resource
                        for (Resource.Layer layer : res.layers(Material.Res.class)) {
                            Material.Res mres = (Material.Res) layer;
                            tileTexture = mres.get();
                            break; // Use first material found
                        }
                    }
                } catch (Exception e) {
                    // Fall back to wall texture if we can't get ground texture
                    tileTexture = caveTile.wtex;
                }

                // Final fallback to wall texture
                if (tileTexture == null) {
                    tileTexture = caveTile.wtex;
                }

                // Group this tile with others using the same material
                if (!tilesByMaterial.containsKey(tileTexture)) {
                    tilesByMaterial.put(tileTexture, new ArrayList<>());
                }
                tilesByMaterial.get(tileTexture).add(lc);
            }
        }

        // Now render all tiles grouped by material
        // This minimizes the number of separate render nodes we create
        java.util.List<RenderTree.Node> nodes = new ArrayList<>();

        for (Map.Entry<Material, java.util.List<Coord>> entry : tilesByMaterial.entrySet()) {
            Material capTexture = entry.getKey();
            java.util.List<Coord> tiles = entry.getValue();

            ArrayList<Float> vertices = new ArrayList<>();
            ArrayList<Float> texCoords = new ArrayList<>();
            ArrayList<Short> indices = new ArrayList<>();
            short vertexCount = 0;

            // Process each tile with this material
            for (Coord lc : tiles) {

                // Get the 4 corner vertices for this tile
                Surface.Vertex[] corners = new Surface.Vertex[4];
                for (int i = 0; i < 4; i++) {
                    corners[i] = ms.fortile(lc.add(TILE_CORNERS[i]));
                }

                // Create a box with all 6 faces (full 11x11 tile size)
                // The box sits at ground level and extends upward by CAP_HEIGHT

                // Bottom face (at ground level)
                addQuad(vertices, texCoords, indices, vertexCount,
                       corners[0].x, corners[0].y, corners[0].z, 0, 0,
                       corners[1].x, corners[1].y, corners[1].z, 1, 0,
                       corners[2].x, corners[2].y, corners[2].z, 1, 1,
                       corners[3].x, corners[3].y, corners[3].z, 0, 1);
                vertexCount += 4;

                // Top face (CAP_HEIGHT units above ground)
                addQuad(vertices, texCoords, indices, vertexCount,
                       corners[0].x, corners[0].y, corners[0].z + CAP_HEIGHT, 0, 0,
                       corners[3].x, corners[3].y, corners[3].z + CAP_HEIGHT, 0, 1,
                       corners[2].x, corners[2].y, corners[2].z + CAP_HEIGHT, 1, 1,
                       corners[1].x, corners[1].y, corners[1].z + CAP_HEIGHT, 1, 0);
                vertexCount += 4;

                // North wall (front) - vertical texture mapping
                addQuad(vertices, texCoords, indices, vertexCount,
                       corners[0].x, corners[0].y, corners[0].z, 0, 0,
                       corners[1].x, corners[1].y, corners[1].z, 1, 0,
                       corners[1].x, corners[1].y, corners[1].z + CAP_HEIGHT, 1, 1,
                       corners[0].x, corners[0].y, corners[0].z + CAP_HEIGHT, 0, 1);
                vertexCount += 4;

                // East wall (right) - vertical texture mapping
                addQuad(vertices, texCoords, indices, vertexCount,
                       corners[1].x, corners[1].y, corners[1].z, 0, 0,
                       corners[2].x, corners[2].y, corners[2].z, 1, 0,
                       corners[2].x, corners[2].y, corners[2].z + CAP_HEIGHT, 1, 1,
                       corners[1].x, corners[1].y, corners[1].z + CAP_HEIGHT, 0, 1);
                vertexCount += 4;

                // South wall (back) - vertical texture mapping
                addQuad(vertices, texCoords, indices, vertexCount,
                       corners[2].x, corners[2].y, corners[2].z, 0, 0,
                       corners[3].x, corners[3].y, corners[3].z, 1, 0,
                       corners[3].x, corners[3].y, corners[3].z + CAP_HEIGHT, 1, 1,
                       corners[2].x, corners[2].y, corners[2].z + CAP_HEIGHT, 0, 1);
                vertexCount += 4;

                // West wall (left) - vertical texture mapping
                addQuad(vertices, texCoords, indices, vertexCount,
                       corners[3].x, corners[3].y, corners[3].z, 0, 0,
                       corners[0].x, corners[0].y, corners[0].z, 1, 0,
                       corners[0].x, corners[0].y, corners[0].z + CAP_HEIGHT, 1, 1,
                       corners[3].x, corners[3].y, corners[3].z + CAP_HEIGHT, 0, 1);
                vertexCount += 4;
            }

            if (indices.isEmpty()) {
                continue; // Skip this material if no geometry
            }

            // Convert ArrayLists to buffers
            java.nio.FloatBuffer posb = Utils.wfbuf(vertices.size());
            for (Float v : vertices) {
                posb.put(v);
            }

            java.nio.FloatBuffer texb = Utils.wfbuf(texCoords.size());
            for (Float t : texCoords) {
                texb.put(t);
            }

            java.nio.ShortBuffer idxb = Utils.wsbuf(indices.size());
            for (Short i : indices) {
                idxb.put(i);
            }

            // Create vertex buffer with position and texture coordinate data
            VertexBuf.VertexData posa = new VertexBuf.VertexData(Utils.bufcp(posb));
            VertexBuf.TexelData texa = new VertexBuf.TexelData(Utils.bufcp(texb));
            VertexBuf vbuf = new VertexBuf(posa, texa);

            // Create model
            haven.render.Model mod = new haven.render.Model(
                haven.render.Model.Mode.TRIANGLES,
                vbuf.data(),
                new haven.render.Model.Indices(indices.size(), NumberFormat.UINT16,
                    DataBuffer.Usage.STATIC, DataBuffer.Filler.of(idxb.array()))
            );

            // Apply this material's texture (depth testing handled in added() method)
            Pipe.Op renderOp;
            if (capTexture != null) {
                renderOp = Pipe.Op.compose(
                    capTexture,
                    new States.Facecull(States.Facecull.Mode.NONE)  // Render both sides
                );
            } else {
                renderOp = Pipe.Op.compose(
                    new BaseColor(new java.awt.Color(150, 150, 150, 180)),  // Fallback gray color
                    new States.Facecull(States.Facecull.Mode.NONE)
                );
            }

            nodes.add(new MapMesh.ShallowWrap(mod,
                Pipe.Op.compose(new MapMesh.NOLOrder(id), renderOp)));
        }

        // If no nodes were created, return null
        if (nodes.isEmpty()) {
            return null;
        }

        // If only one node, return it directly
        if (nodes.size() == 1) {
            return nodes.get(0);
        }

        // Return a composite node containing all material groups
        return new RenderTree.Node() {
            public void added(RenderTree.Slot slot) {
                for (RenderTree.Node node : nodes) {
                    slot.add(node);
                }
            }
        };
    }

    @Override
    public void added(RenderTree.Slot slot) {
        this.slot = slot;
        // Add the base grid so makenol() gets called
        // Use LE depth test so character and other objects can properly occlude the boxes
        slot.add(base, new States.Depthtest(States.Depthtest.Test.LE));
        // Don't call super.added() to avoid NOverlay's BaseColor null pointer
    }

    @Override
    public RenderTree.Node makenolol(MapMesh mm, Long grid_id, Coord grid_ul) {
        return null;
    }

    @Override
    public boolean requpdate() {
        return false;
    }
}
