package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NHitBox;
import nurgling.NUtils;
import nurgling.pf.NHitBoxD;

import java.awt.Color;
import java.util.*;

/**
 * Ghost preview overlay showing where trellises will be placed
 */
public class TrellisGhostPreview extends Sprite {
    private static final int TRELLIS_PER_TILE = 3;
    private static final Pipe.Op ghostColor = Pipe.Op.compose(
            new BaseColor(new Color(0, 255, 0, 255)),  // Bright green
            new States.Facecull(States.Facecull.Mode.NONE),
            new States.LineWidth(3)
    );

    private Pair<Coord2d, Coord2d> area;
    private int orientation; // 0=NS-East, 1=NS-West, 2=EW-North, 3=EW-South
    private NHitBox trellisHitBox;
    private Model ghostModel;
    private List<Location> ghostLocations = new ArrayList<>();
    private List<RenderTree.Slot> slots = new ArrayList<>();

    public TrellisGhostPreview(Owner owner, Pair<Coord2d, Coord2d> area, int orientation, NHitBox hitBox) {
        super(owner, null);
        this.area = area;
        this.orientation = orientation;
        this.trellisHitBox = hitBox;
        if (area != null) {
            calculateGhostPositions();
        }
    }

    /**
     * Calculate all trellis positions (matching BuildTrellis logic)
     */
    private void calculateGhostPositions() {
        ghostLocations.clear();

        if (trellisHitBox == null || area == null) {
            return;
        }

        // Determine if we need to rotate hitbox (EW orientations 2 and 3)
        boolean needRotate = (orientation >= 2);
        NHitBox rotatedHitBox = needRotate ? trellisHitBox.rotate() : trellisHitBox;

        // Create the box model once (centered at origin)
        ghostModel = createGhostBoxModel(rotatedHitBox);

        HashMap<Coord, Integer> tileCount = new HashMap<>();

        // Find all valid positions
        ArrayList<NHitBoxD> obstacles = findObstacles(area, rotatedHitBox);

        // Calculate which tiles are in the area
        Coord tileBegin = area.a.floor(MCache.tilesz);
        Coord tileEnd = area.b.sub(1, 1).floor(MCache.tilesz);

        int validPositions = 0;

        // Iterate tile by tile to ensure proper alignment
        for (int tx = tileBegin.x; tx <= tileEnd.x; tx++) {
            for (int ty = tileBegin.y; ty <= tileEnd.y; ty++) {
                Coord tile = new Coord(tx, ty);

                // Calculate the bounds for this specific tile
                Coord2d tileStart = tile.mul(MCache.tilesz);
                Coord2d tileEndPos = tileStart.add(MCache.tilesz.x, MCache.tilesz.y);

                // Clamp to the selected area
                Coord2d searchStart = new Coord2d(
                    Math.max(tileStart.x, area.a.x),
                    Math.max(tileStart.y, area.a.y)
                );
                Coord2d searchEnd = new Coord2d(
                    Math.min(tileEndPos.x, area.b.x),
                    Math.min(tileEndPos.y, area.b.y)
                );

                Coord searchRange = searchEnd.sub(searchStart).floor();
                Coord margin = rotatedHitBox.end.sub(rotatedHitBox.begin).floor(2, 2);

                // Search within this tile only, using small step for tight packing
                int step = 2;
                int tileCount_local = 0;

                // Determine search order based on orientation to pack against specific edge
                // 0=NS-East (pack to right), 1=NS-West (pack to left)
                // 2=EW-North (pack to top), 3=EW-South (pack to bottom)
                boolean reverseX = (orientation == 0); // NS-East: start from right
                boolean reverseY = (orientation == 3); // EW-South: start from bottom

                for (int ii = 0; ii <= searchRange.x - margin.x * 2 && tileCount_local < TRELLIS_PER_TILE; ii += step) {
                    int i = reverseX ? (searchRange.x - margin.x - ii) : (margin.x + ii);
                    for (int jj = 0; jj <= searchRange.y - margin.y * 2 && tileCount_local < TRELLIS_PER_TILE; jj += step) {
                        int j = reverseY ? (searchRange.y - margin.y - jj) : (margin.y + jj);
                        Coord2d testPos = searchStart.add(i, j);

                        // Check collisions
                        NHitBoxD testBox = new NHitBoxD(rotatedHitBox.begin, rotatedHitBox.end, testPos, 0);
                        boolean passed = true;
                        for (NHitBoxD obstacle : obstacles) {
                            if (obstacle.intersects(testBox, false)) {
                                passed = false;
                                break;
                            }
                        }

                        if (passed) {
                            // Calculate center position of the hitbox at testPos
                            float centerX = (float)(testPos.x + (rotatedHitBox.end.x + rotatedHitBox.begin.x) / 2.0);
                            float centerY = (float)(testPos.y + (rotatedHitBox.end.y + rotatedHitBox.begin.y) / 2.0);

                            // Store location for this ghost position
                            Location loc = Location.xlate(new Coord3f(centerX, -centerY, 0));
                            ghostLocations.add(loc);
                            tileCount_local++;
                            validPositions++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a simple wireframe box model for a ghost trellis
     * Box is centered at origin and will be positioned via Location transforms
     */
    private Model createGhostBoxModel(NHitBox hitBox) {
        // Use hitbox dimensions directly
        float l = (float) hitBox.begin.x;
        float u = (float) hitBox.begin.y;
        float r = (float) hitBox.end.x;
        float b = (float) hitBox.end.y;
        float h = 16f; // Height of ghost box

        // Create vertices for the box (8 corners)
        java.nio.FloatBuffer posb = Utils.wfbuf(8 * 3);

        // Bottom vertices (0-3)
        posb.put(l).put(-u).put(0f);
        posb.put(r).put(-u).put(0f);
        posb.put(r).put(-b).put(0f);
        posb.put(l).put(-b).put(0f);

        // Top vertices (4-7)
        posb.put(l).put(-u).put(h);
        posb.put(r).put(-u).put(h);
        posb.put(r).put(-b).put(h);
        posb.put(l).put(-b).put(h);

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
     * Find obstacles in area (same logic as BuildTrellis)
     */
    private ArrayList<NHitBoxD> findObstacles(Pair<Coord2d, Coord2d> area, NHitBox hitBox) {
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

    /**
     * Update all existing slots with new models
     */
    private void updateSlots() {
        // Clear and re-add to existing slots
        for (RenderTree.Slot slot : slots) {
            // Can't easily remove, so we'll rely on sprite removal/re-add
        }
    }

    @Override
    public boolean tick(double dt) {
        return false; // Don't auto-remove
    }

    /**
     * Check if the preview needs to be updated (and therefore recreated)
     */
    public boolean needsUpdate(Pair<Coord2d, Coord2d> newArea, int newOrientation) {
        if (newArea != null && !newArea.equals(this.area)) {
            return true;
        }
        if (this.orientation != newOrientation) {
            return true;
        }
        return false;
    }

    /**
     * Update preview when area or orientation changes
     */
    public void update(Pair<Coord2d, Coord2d> newArea, int newOrientation) {
        boolean changed = false;
        if (newArea != null && !newArea.equals(this.area)) {
            this.area = newArea;
            changed = true;
        }
        if (this.orientation != newOrientation) {
            this.orientation = newOrientation;
            changed = true;
        }
        if (changed && area != null) {
            calculateGhostPositions();
        }
    }
}
