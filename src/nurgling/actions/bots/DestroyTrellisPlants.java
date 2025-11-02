package nurgling.actions.bots;

import haven.Coord;
import haven.Coord2d;
import haven.Gob;
import haven.Pair;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.tasks.NTask;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static haven.MCache.tilesz;
import static haven.MCache.tilehsz;

public class DestroyTrellisPlants implements Action {

    // All trellis plant types
    private static final NAlias TRELLIS_PLANTS = new NAlias(
        "plants/wine",      // Grape
        "plants/hops",      // Hops
        "plants/pepper",    // Peppercorn
        "plants/pea",       // Pea
        "plants/cucumber"   // Cucumber
    );

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Prompt user to select area using SelectArea action
        SelectArea selectArea;
        NUtils.getGameUI().msg("Please select area to destroy trellis plants");
        (selectArea = new SelectArea(Resource.loadsimg("baubles/outputArea"))).run(gui);

        Pair<Coord2d, Coord2d> rcArea = selectArea.getRCArea();
        if (rcArea == null) {
            return Results.ERROR("No area selected!");
        }

        // Find all trellis plants in selected area
        ArrayList<Gob> plantsToDestroy = Finder.findGobs(rcArea, TRELLIS_PLANTS);

        if (plantsToDestroy.isEmpty()) {
            NUtils.getGameUI().msg("No trellis plants found in selected area");
            return Results.SUCCESS(); // Nothing to destroy
        }

        // Group plants by tile for optimized pathfinding
        Map<Coord, ArrayList<Gob>> plantsByTile = groupGobsByTile(plantsToDestroy);

        // Process each tile
        for (Map.Entry<Coord, ArrayList<Gob>> entry : plantsByTile.entrySet()) {
            Coord tile = entry.getKey();
            ArrayList<Gob> plantsOnTile = entry.getValue();

            // Get first plant for pathfinding reference
            Gob firstPlant = plantsOnTile.get(0);

            // Calculate pathfinder endpoint at edge of tile
            // Check all 4 sides of the tile and use first reachable one
            Coord2d tileBase = tile.mul(tilesz);

            // Try all 4 edges of the tile
            Coord2d[] tileEdges = new Coord2d[] {
                tileBase.add(tilehsz.x, 0),                    // North edge
                tileBase.add(tilehsz.x, tilesz.y),             // South edge
                tileBase.add(0, tilehsz.y),                    // West edge
                tileBase.add(tilesz.x, tilehsz.y)              // East edge
            };

            Coord2d pathfinderEndpoint = null;
            for (Coord2d edge : tileEdges) {
                if (PathFinder.isAvailable(edge)) {
                    pathfinderEndpoint = edge;
                    break;
                }
            }

            // Navigate to reachable tile edge, or fallback to first plant
            PathFinder pf;
            if (pathfinderEndpoint != null) {
                pf = new PathFinder(pathfinderEndpoint);
            } else {
                pf = new PathFinder(firstPlant);
            }
            pf.run(gui);

            // Destroy ALL plants on this tile
            for (Gob plant : plantsOnTile) {
                // Loop destroy until plant actually disappears
                while (Finder.findGob(plant.id) != null) {
                    // Restore resources before each destroy attempt
                    new RestoreResources().run(gui);

                    // Activate destroy from menu grid and click plant
                    NUtils.destroy(plant);

                    // Wait for either:
                    // 1. Resources low (energy/stamina < 25%) OR
                    // 2. Plant gob disappears
                    NUtils.addTask(new NTask() {
                        @Override
                        public boolean check() {
                            return NUtils.getEnergy() < 0.25 ||
                                   NUtils.getStamina() < 0.25 ||
                                   Finder.findGob(plant.id) == null;
                        }
                    });
                }
            }
        }

        return Results.SUCCESS();
    }

    private Map<Coord, ArrayList<Gob>> groupGobsByTile(ArrayList<Gob> gobs) {
        Map<Coord, ArrayList<Gob>> gobsByTile = new LinkedHashMap<>();

        for (Gob gob : gobs) {
            Coord tile = gob.rc.floor(tilesz);
            gobsByTile.computeIfAbsent(tile, k -> new ArrayList<>()).add(gob);
        }

        return gobsByTile;
    }
}
