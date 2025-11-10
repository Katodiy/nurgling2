package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tasks.*;
import nurgling.tools.*;

import static haven.OCache.posres;

public class BoatHopper implements Action {
    private static final double PLACEMENT_DISTANCE = 33.0;
    private static final NAlias ROWBOAT_ALIAS = new NAlias("rowboat");
    private double travelAngle = 0; // Stores initial direction

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        int hopCount = 0;

        while (true) {
            hopCount++;
            System.out.println("[" + hopCount + "] Starting hop");

            // Step 1: Find the lifted boat (already being carried)
            System.out.println("[" + hopCount + "] Finding lifted boat...");
            Gob liftedBoat = Finder.findLiftedbyPlayer();
            if (liftedBoat == null) {
                System.out.println("[" + hopCount + "] No lifted boat found, stopping...");
                return Results.ERROR("No lifted boat found - need to start with a lifted boat");
            }
            long liftedBoatId = liftedBoat.id;
            System.out.println("[" + hopCount + "] Found lifted boat: " + liftedBoatId);

            // Step 2: Find the boat we're currently standing on
            System.out.println("[" + hopCount + "] Finding current boat...");
            Gob currentBoat = findBoatAtPlayerPosition();
            if (currentBoat == null) {
                System.out.println("[" + hopCount + "] No current boat found, retrying...");
                continue; // Skip to next iteration
            }
            System.out.println("[" + hopCount + "] Found current boat: " + currentBoat.id);

            // Step 3: Calculate placement position
            Gob player = NUtils.player();
            if (player == null) {
                System.out.println("[" + hopCount + "] ERROR: Player not found, retrying...");
                continue;
            }
            Coord2d playerPos = player.rc;
            double playerAngle = player.a;

            // Store initial travel angle on first hop, reuse on subsequent hops
            if (hopCount == 1) {
                travelAngle = player.a;
                playerAngle = travelAngle;
            } else {
                playerAngle = travelAngle;
            }

            double dx = Math.cos(playerAngle) * PLACEMENT_DISTANCE;
            double dy = Math.sin(playerAngle) * PLACEMENT_DISTANCE;
            Coord2d placementPos = playerPos.add(dx, dy);

            // Step 4: Right-click ahead to place the lifted boat
            System.out.println("[" + hopCount + "] Placing boat at: " + placementPos);
            gui.map.wdgmsg("click", Coord.z, placementPos.floor(posres), 3, 0, 0);

            // Wait for placement to complete
            NUtils.getUI().core.addTask(new NTask() {
                int ticks = 0;
                @Override
                public boolean check() {
                    return ticks++ >= 8;
                }
            });

            // Step 5: Find and board the placed boat immediately
            Gob placedBoat = Finder.findGob(liftedBoatId);
            if (placedBoat == null) {
                System.out.println("[" + hopCount + "] ERROR: Lost placed boat, retrying...");
                continue; // Skip to next iteration
            }

            // Board the boat
            NUtils.rclickGob(placedBoat);

            // Wait for boarding to complete
            NUtils.getUI().core.addTask(new NTask() {
                int ticks = 0;
                @Override
                public boolean check() {
                    return ticks++ >= 6;
                }
            });

            // Step 6: Fast lift the old boat
            final long currentBoatId = currentBoat.id;

            // Single lift attempt with aggressive timing
            gui.ui.rcvr.rcvmsg(NUtils.getUI().getMenuGridId(), "act", "carry");

            // Click on the boat to lift it (use cached boat object)
            gui.map.wdgmsg("click", Coord.z, currentBoat.rc.floor(posres), 1, 0, 0,
                          (int) currentBoatId, currentBoat.rc.floor(posres), 0, -1);

            // Wait for lift to complete
            NUtils.getUI().core.addTask(new NTask() {
                int ticks = 0;
                @Override
                public boolean check() {
                    return ticks++ >= 8;
                }
            });

            System.out.println("[" + hopCount + "] Hop complete!");

            // Continue loop immediately - no delay between hops
            // Direction is maintained via travelAngle, no need to toggle
        }
    }

    /**
     * Finds the boat at the player's current position (not the lifted one!)
     */
    private Gob findBoatAtPlayerPosition() throws InterruptedException {
        System.out.println("  > Entering findBoatAtPlayerPosition");
        Gob player = NUtils.player();
        if (player == null) {
            System.out.println("  > Player is null!");
            return null;
        }

        Coord2d playerPos = player.rc;
        System.out.println("  > Player position: " + playerPos);

        Gob liftedBoat = Finder.findLiftedbyPlayer();
        System.out.println("  > Lifted boat ID: " + (liftedBoat != null ? liftedBoat.id : "none"));

        // Find boats near player position (within 2 tiles)
        System.out.println("  > Searching for nearby boats...");

        // Fast boat search - no copying, minimal checks
        synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                // Quick filters first
                if (gob == null || gob.id < 0 || gob.rc == null) continue;
                if (liftedBoat != null && gob.id == liftedBoat.id) continue;

                // Distance check before expensive parsing
                double distance = gob.rc.dist(playerPos);
                if (distance >= 2.0) continue;

                // Only check if it's a boat if it's close
                try {
                    if (NParser.isIt(gob, ROWBOAT_ALIAS)) {
                        return gob;
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    continue;
                }
            }
        }

        return null;
    }
}