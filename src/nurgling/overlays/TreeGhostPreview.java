package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NUtils;

import java.awt.Color;
import java.util.*;

/**
 * Ghost preview overlay showing where trees will be planted
 * Shows green wireframe boxes at each tree position
 */
public class TreeGhostPreview extends Sprite {
    private static final Pipe.Op ghostColor = Pipe.Op.compose(
            new BaseColor(new Color(0, 255, 0, 255)),  // Bright green
            new States.Facecull(States.Facecull.Mode.NONE),
            new States.LineWidth(3)
    );

    private final ArrayList<Coord2d> treePositions;
    private Model ghostModel;
    private List<Location> ghostLocations = new ArrayList<>();
    private List<RenderTree.Slot> slots = new ArrayList<>();

    public TreeGhostPreview(Owner owner, ArrayList<Coord2d> positions) {
        super(owner, null);
        this.treePositions = new ArrayList<>(positions);
        calculateGhostPositions();
    }

    /**
     * Calculate ghost locations for each tree position
     */
    private void calculateGhostPositions() {
        ghostLocations.clear();

        if (treePositions.isEmpty()) {
            return;
        }

        // Create the box model once (simple tree-sized box)
        ghostModel = createTreeGhostBoxModel();

        // Create location for each tree position
        for (Coord2d treePos : treePositions) {
            float centerX = (float) treePos.x;
            float centerY = (float) treePos.y;

            // Get terrain height at this position
            float terrainZ = getTerrainHeight();

            // Store location for this ghost position
            Location loc = Location.xlate(new Coord3f(centerX, -centerY, terrainZ));
            ghostLocations.add(loc);
        }
    }

    /**
     * Create a simple wireframe box model for a ghost tree
     * Box represents a 1x1 tile with some height for visibility
     */
    private Model createTreeGhostBoxModel() {
        // Tree box dimensions (1 tile = 11x11 units typically)
        float halfTile = (float)(MCache.tilesz.x / 2.0);
        float l = -halfTile;  // Left edge
        float r = halfTile;   // Right edge
        float u = -halfTile;  // Top edge (negative Y)
        float b = halfTile;   // Bottom edge (negative Y)
        float h = 15f;        // Height of ghost box

        // Create vertices for the box (8 corners)
        java.nio.FloatBuffer posb = Utils.wfbuf(8 * 3);

        // Bottom vertices (0-3) - ground level
        posb.put(l).put(u).put(0f);
        posb.put(r).put(u).put(0f);
        posb.put(r).put(b).put(0f);
        posb.put(l).put(b).put(0f);

        // Top vertices (4-7) - elevated
        posb.put(l).put(u).put(h);
        posb.put(r).put(u).put(h);
        posb.put(r).put(b).put(h);
        posb.put(l).put(b).put(h);

        VertexBuf.VertexData posa = new VertexBuf.VertexData(Utils.bufcp(posb));
        VertexBuf vbuf = new VertexBuf(posa);

        // Create wireframe using LINES mode - just the edges
        java.nio.ShortBuffer idx = Utils.wsbuf(24);
        short[] indices = {
            0,1, 1,2, 2,3, 3,0,  // Bottom edges
            4,5, 5,6, 6,7, 7,4,  // Top edges
            0,4, 1,5, 2,6, 3,7   // Vertical edges
        };
        for (short i : indices) {
            idx.put(i);
        }

        return new Model(Model.Mode.LINES, vbuf.data(),
            new Model.Indices(24, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
                DataBuffer.Filler.of(idx.array())));
    }

    @Override
    public void added(RenderTree.Slot slot) {
        slots.add(slot);

        // Set initial state similar to TrellisGhostPreview
        slot.ostate(Pipe.Op.compose(
            Rendered.postpfx,
            new States.Facecull(States.Facecull.Mode.NONE),
            p -> p.put(Homo3D.loc, null)
        ));

        // Add all ghost boxes to this slot (reuse single model, different locations)
        if (ghostModel != null) {
            for (Location loc : ghostLocations) {
                slot.add(ghostModel, Pipe.Op.compose(
                    ghostColor,
                    loc
                ));
            }
        }
    }

    @Override
    public void removed(RenderTree.Slot slot) {
        slots.remove(slot);
    }

    @Override
    public boolean tick(double dt) {
        return false; // Don't auto-remove
    }

    /**
     * Gets the number of trees in this preview
     */
    public int getTreeCount() {
        return treePositions.size();
    }

    /**
     * Gets a copy of the tree positions
     */
    public ArrayList<Coord2d> getPositions() {
        return new ArrayList<>(treePositions);
    }

    /**
     * Get terrain height at a position
     */
    private float getTerrainHeight() {
        try {
            Gob player = NUtils.player();
            if (player != null) {
                Coord3f playerPos = player.getc();
                if (playerPos != null) {
                    // Use player's Z coordinate as base reference
                    return playerPos.z - 1;
                }
            }
        } catch (NullPointerException e) {
            // Fallback to 0 if player or position unavailable
        }
        return 0f;
    }
}