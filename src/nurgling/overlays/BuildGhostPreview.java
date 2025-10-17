package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.pf.NHitBoxD;

import java.awt.Color;
import java.util.*;

/**
 * Generic ghost preview overlay showing where buildings will be placed
 * Simpler than TrellisGhostPreview - just shows all valid positions in the area
 */
public class BuildGhostPreview extends Sprite {
    private static final Pipe.Op ghostColor = Pipe.Op.compose(
            new BaseColor(new Color(0, 255, 0, 255)),  // Bright green
            new States.Facecull(States.Facecull.Mode.NONE),
            new States.LineWidth(3)
    );

    private Pair<Coord2d, Coord2d> area;
    private NHitBox buildingHitBox;
    private Model ghostModel;
    private List<Location> ghostLocations = new ArrayList<>();
    private List<RenderTree.Slot> slots = new ArrayList<>();

    public BuildGhostPreview(Owner owner, Pair<Coord2d, Coord2d> area, NHitBox hitBox) {
        super(owner, null);
        this.area = area;
        this.buildingHitBox = hitBox;
        if (area != null && hitBox != null) {
            calculateGhostPositions();
        }
    }

    /**
     * Calculate all valid building positions using the same logic as Finder.getFreePlace()
     */
    private void calculateGhostPositions() {
        ghostLocations.clear();

        if (buildingHitBox == null || area == null) {
            return;
        }

        // Create the box model once (centered at origin)
        ghostModel = createGhostBoxModel(buildingHitBox);

        // Find all obstacles in the area (same as Finder.getFreePlace)
        ArrayList<NHitBoxD> obstacles = findObstacles();

        // Calculate building dimensions for grid spacing
        int buildingWidth = (int)Math.ceil(buildingHitBox.end.x - buildingHitBox.begin.x);
        int buildingDepth = (int)Math.ceil(buildingHitBox.end.y - buildingHitBox.begin.y);

        // Add some padding to avoid tight packing
        int stepX = Math.max(buildingWidth + 1, 1);
        int stepY = Math.max(buildingDepth + 1, 1);

        Coord inchMax = area.b.sub(area.a).floor();
        Coord margin = buildingHitBox.end.sub(buildingHitBox.begin).floor(2, 2);

        // Grid-based placement instead of pixel-by-pixel
        for (int i = margin.x; i <= inchMax.x - margin.x; i += stepX) {
            for (int j = margin.y; j <= inchMax.y - margin.y; j += stepY) {
                Coord2d testPos = area.a.add(i, j);
                NHitBoxD testBox = new NHitBoxD(buildingHitBox.begin, buildingHitBox.end, testPos, 0);

                // Check collisions
                boolean passed = true;
                for (NHitBoxD obstacle : obstacles) {
                    if (obstacle.intersects(testBox, false)) {
                        passed = false;
                        break;
                    }
                }

                if (passed) {
                    // Calculate center position of the hitbox at testPos
                    float centerX = (float)(testBox.rc.x);
                    float centerY = (float)(testBox.rc.y);

                    // Store location for this ghost position
                    Location loc = Location.xlate(new Coord3f(centerX, -centerY, 0));
                    ghostLocations.add(loc);
                }
            }
        }
    }

    /**
     * Create a simple wireframe box model for a ghost building
     * Box is centered at origin and will be positioned via Location transforms
     */
    private Model createGhostBoxModel(NHitBox hitBox) {
        // Calculate half-dimensions to center the box at origin
        float halfWidth = (float)((hitBox.end.x - hitBox.begin.x) / 2.0);
        float halfDepth = (float)((hitBox.end.y - hitBox.begin.y) / 2.0);
        float h = 16f; // Height of ghost box

        // Create vertices for the box centered at origin (8 corners)
        java.nio.FloatBuffer posb = Utils.wfbuf(8 * 3);

        // Bottom vertices (0-3) - centered at origin in XY plane
        posb.put(-halfWidth).put(-halfDepth).put(0f);
        posb.put(halfWidth).put(-halfDepth).put(0f);
        posb.put(halfWidth).put(halfDepth).put(0f);
        posb.put(-halfWidth).put(halfDepth).put(0f);

        // Top vertices (4-7)
        posb.put(-halfWidth).put(-halfDepth).put(h);
        posb.put(halfWidth).put(-halfDepth).put(h);
        posb.put(halfWidth).put(halfDepth).put(h);
        posb.put(-halfWidth).put(halfDepth).put(h);

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

    /**
     * Find obstacles in area (same logic as Finder.getFreePlace)
     */
    private ArrayList<NHitBoxD> findObstacles() {
        ArrayList<NHitBoxD> obstacles = new ArrayList<>();
        NHitBoxD areaBox = new NHitBoxD(area.a, area.b);

        try {
            synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
                for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                    if (!(gob instanceof OCache.Virtual || gob.attr.isEmpty() ||
                          gob.getClass().getName().contains("GlobEffector"))) {
                        if (gob.ngob.hitBox != null && gob.getattr(Following.class) == null &&
                            gob.id != NUtils.player().id) {
                            NHitBoxD gobBox = new NHitBoxD(gob);
                            if (gobBox.intersects(areaBox, true)) {
                                obstacles.add(gobBox);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle exceptions finding obstacles
        }

        return obstacles;
    }

    @Override
    public void added(RenderTree.Slot slot) {
        slots.add(slot);

        // Set initial state similar to NBoxOverlay
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
}
