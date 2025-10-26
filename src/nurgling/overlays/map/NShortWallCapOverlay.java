package nurgling.overlays.map;

import haven.*;
import haven.render.*;
import haven.resutil.CaveTile;
import nurgling.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Overlay that renders textured horizontal caps on top of short mine walls.
 * When shortWalls config is enabled, this adds a visible horizontal surface
 * on top of cave walls at the reduced height (4 units).
 *
 * Performance optimizations:
 * - Material lookups cached per tile ID
 * - CaveTile checks cached per mesh
 * - Only exterior walls rendered (no interior/bottom faces)
 * - Geometry batched by material
 */
public class NShortWallCapOverlay extends NOverlay {

    public static final int SHORT_WALL_CAP_OVERLAY = -3;
    private static final float CAP_HEIGHT = CaveTile.SHORT_H; // 4 units - short wall height

    // Corner coords for tile
    private static final Coord[] TILE_CORNERS = {
        new Coord(0, 0),
        new Coord(1, 0),
        new Coord(1, 1),
        new Coord(0, 1)
    };

    // Edge directions: N, E, S, W
    private static final Coord[] EDGE_DIRS = {
        new Coord(0, -1), // North
        new Coord(1, 0),  // East
        new Coord(0, 1),  // South
        new Coord(-1, 0)  // West
    };

    // Material cache: tileId -> Material (for ground texture)
    private static final Map<Integer, Material> materialCache = new ConcurrentHashMap<>();

    // CaveTile cache: mesh key -> Map<Coord, Boolean>
    private final Map<String, Map<Coord, Boolean>> caveTileCache = new HashMap<>();

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
            // Clear caches
            caveTileCache.clear();
            // Trigger map regeneration when setting changes
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null &&
                NUtils.getGameUI().map.glob != null && NUtils.getGameUI().map.glob.map != null) {
                NUtils.getGameUI().map.glob.map.trimall();
            }
        }
    }

    /**
     * Check if a tile is a cave tile (cached per mesh)
     */
    private boolean isCaveTile(MCache map, Coord gc, String meshKey) {
        Map<Coord, Boolean> meshCache = caveTileCache.get(meshKey);
        if (meshCache == null) {
            meshCache = new HashMap<>();
            caveTileCache.put(meshKey, meshCache);
        }

        Boolean cached = meshCache.get(gc);
        if (cached != null) {
            return cached;
        }

        boolean isCave = false;
        try {
            int tileId = map.gettile(gc);
            if (tileId >= 0 && tileId < map.nsets.length) {
                Resource.Spec tileSpec = map.nsets[tileId];
                if (tileSpec != null) {
                    // Skip "gfx/tiles/cave" tiles - they don't need caps
                    if ("gfx/tiles/cave".equals(tileSpec.name)) {
                        isCave = false;
                    } else {
                        // Check if tile's tiler is a CaveTile
                        Tileset ts = map.tileset(tileId);
                        if (ts != null) {
                            Tiler tiler = ts.tfac().create(tileId, ts);
                            isCave = (tiler instanceof CaveTile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Default to false on error
        }

        meshCache.put(gc, isCave);
        return isCave;
    }

    /**
     * Get cached material for a tile ID
     */
    private Material getMaterialForTile(MCache map, int tileId) {
        Material cached = materialCache.get(tileId);
        if (cached != null) {
            return cached;
        }

        Material material = null;
        try {
            Tileset ts = map.tileset(tileId);
            if (ts != null && ts.getres() != null) {
                Resource res = ts.getres();
                // Look for material layer in the resource
                for (Resource.Layer layer : res.layers(Material.Res.class)) {
                    Material.Res mres = (Material.Res) layer;
                    material = mres.get();
                    break; // Use first material found
                }
            }

            // Fallback: get wall texture from CaveTile
            if (material == null) {
                Tiler tiler = ts != null ? ts.tfac().create(tileId, ts) : null;
                if (tiler instanceof CaveTile) {
                    material = ((CaveTile) tiler).wtex;
                }
            }
        } catch (Exception e) {
            // Return null on error
        }

        if (material != null) {
            materialCache.put(tileId, material);
        }
        return material;
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

        // Create unique key for this mesh (for caching)
        String meshKey = grid_id != null ? grid_id.toString() : (mm.ul.x + "_" + mm.ul.y);

        // Group tiles by their material texture
        Map<Material, java.util.List<TileData>> tilesByMaterial = new HashMap<>();

        // First pass: identify cave tiles and group by material
        for (int ty = 0; ty < mm.sz.y; ty++) {
            for (int tx = 0; tx < mm.sz.x; tx++) {
                Coord lc = new Coord(tx, ty);
                Coord gc = lc.add(mm.ul);

                // Check if this is a cave tile (using cache)
                if (!isCaveTile(map, gc, meshKey)) {
                    continue;
                }

                // Get material for this tile (using cache)
                Material tileTexture = null;
                try {
                    int tileId = map.gettile(gc);
                    tileTexture = getMaterialForTile(map, tileId);
                } catch (Exception e) {
                    // Skip on error
                    continue;
                }

                if (tileTexture == null) {
                    continue; // Skip if no material
                }

                // Determine which edges are exterior (adjacent to non-cave tiles)
                boolean[] exteriorEdges = new boolean[4];
                for (int i = 0; i < 4; i++) {
                    Coord neighbor = gc.add(EDGE_DIRS[i]);
                    exteriorEdges[i] = !isCaveTile(map, neighbor, meshKey);
                }

                // Store tile data
                TileData td = new TileData(lc, exteriorEdges);
                if (!tilesByMaterial.containsKey(tileTexture)) {
                    tilesByMaterial.put(tileTexture, new ArrayList<>());
                }
                tilesByMaterial.get(tileTexture).add(td);
            }
        }

        // Second pass: render geometry grouped by material
        java.util.List<RenderTree.Node> nodes = new ArrayList<>();

        for (Map.Entry<Material, java.util.List<TileData>> entry : tilesByMaterial.entrySet()) {
            Material capTexture = entry.getKey();
            java.util.List<TileData> tiles = entry.getValue();

            ArrayList<Float> vertices = new ArrayList<>();
            ArrayList<Float> texCoords = new ArrayList<>();
            ArrayList<Short> indices = new ArrayList<>();
            short vertexCount = 0;

            // Process each tile with this material
            for (TileData td : tiles) {
                Coord lc = td.coord;
                boolean[] exteriorEdges = td.exteriorEdges;

                // Get the 4 corner vertices for this tile
                Surface.Vertex[] corners = new Surface.Vertex[4];
                for (int i = 0; i < 4; i++) {
                    corners[i] = ms.fortile(lc.add(TILE_CORNERS[i]));
                }

                // Always render top face (horizontal cap)
                addQuad(vertices, texCoords, indices, vertexCount,
                       corners[0].x, corners[0].y, corners[0].z + CAP_HEIGHT, 0, 0,
                       corners[3].x, corners[3].y, corners[3].z + CAP_HEIGHT, 0, 1,
                       corners[2].x, corners[2].y, corners[2].z + CAP_HEIGHT, 1, 1,
                       corners[1].x, corners[1].y, corners[1].z + CAP_HEIGHT, 1, 0);
                vertexCount += 4;

                // Only render exterior walls (where adjacent tile is not a cave tile)

                // North wall (index 0)
                if (exteriorEdges[0]) {
                    addQuad(vertices, texCoords, indices, vertexCount,
                           corners[0].x, corners[0].y, corners[0].z, 0, 0,
                           corners[1].x, corners[1].y, corners[1].z, 1, 0,
                           corners[1].x, corners[1].y, corners[1].z + CAP_HEIGHT, 1, 1,
                           corners[0].x, corners[0].y, corners[0].z + CAP_HEIGHT, 0, 1);
                    vertexCount += 4;
                }

                // East wall (index 1)
                if (exteriorEdges[1]) {
                    addQuad(vertices, texCoords, indices, vertexCount,
                           corners[1].x, corners[1].y, corners[1].z, 0, 0,
                           corners[2].x, corners[2].y, corners[2].z, 1, 0,
                           corners[2].x, corners[2].y, corners[2].z + CAP_HEIGHT, 1, 1,
                           corners[1].x, corners[1].y, corners[1].z + CAP_HEIGHT, 0, 1);
                    vertexCount += 4;
                }

                // South wall (index 2)
                if (exteriorEdges[2]) {
                    addQuad(vertices, texCoords, indices, vertexCount,
                           corners[2].x, corners[2].y, corners[2].z, 0, 0,
                           corners[3].x, corners[3].y, corners[3].z, 1, 0,
                           corners[3].x, corners[3].y, corners[3].z + CAP_HEIGHT, 1, 1,
                           corners[2].x, corners[2].y, corners[2].z + CAP_HEIGHT, 0, 1);
                    vertexCount += 4;
                }

                // West wall (index 3)
                if (exteriorEdges[3]) {
                    addQuad(vertices, texCoords, indices, vertexCount,
                           corners[3].x, corners[3].y, corners[3].z, 0, 0,
                           corners[0].x, corners[0].y, corners[0].z, 1, 0,
                           corners[0].x, corners[0].y, corners[0].z + CAP_HEIGHT, 1, 1,
                           corners[3].x, corners[3].y, corners[3].z + CAP_HEIGHT, 0, 1);
                    vertexCount += 4;
                }
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

            // Apply this material's texture
            Pipe.Op renderOp = Pipe.Op.compose(
                capTexture,
                new States.Facecull(States.Facecull.Mode.NONE)  // Render both sides
            );

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

    /**
     * Helper class to store tile rendering data
     */
    private static class TileData {
        final Coord coord;
        final boolean[] exteriorEdges; // N, E, S, W

        TileData(Coord coord, boolean[] exteriorEdges) {
            this.coord = coord;
            this.exteriorEdges = exteriorEdges;
        }
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
