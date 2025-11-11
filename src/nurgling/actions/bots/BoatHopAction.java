package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tasks.*;
import nurgling.tools.*;

import static haven.OCache.posres;

/**
 * Shared action for boat hopping in a specified direction.
 * Performs: place boat -> board new boat -> lift old boat
 */
public class BoatHopAction implements Action {
    private static final double PLACEMENT_DISTANCE = 33.0;
    private static final NAlias ROWBOAT_ALIAS = new NAlias("rowboat");

    private final double direction;
    private final String directionName;

    public BoatHopAction(double direction, String directionName) {
        this.direction = direction;
        this.directionName = directionName;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== Starting Single BoatHop (" + directionName + " Direction) ===");

        // Step 1: Find the lifted boat (already being carried)
        System.out.println("Finding lifted boat...");
        Gob liftedBoat = Finder.findLiftedbyPlayer();
        if (liftedBoat == null) {
            System.out.println("No lifted boat found, stopping...");
            return Results.ERROR("No lifted boat found - need to start with a lifted boat");
        }
        long liftedBoatId = liftedBoat.id;
        System.out.println("Found lifted boat: " + liftedBoatId);

        // Step 2: Find the boat we're currently standing on
        System.out.println("Finding current boat...");
        Gob currentBoat = findBoatAtPlayerPosition();
        if (currentBoat == null) {
            System.out.println("No current boat found, stopping...");
            return Results.ERROR("No current boat found - need to be standing on a boat");
        }
        System.out.println("Found current boat: " + currentBoat.id);

        // Step 3: Calculate placement position in specified direction
        Gob player = NUtils.player();
        if (player == null) {
            System.out.println("ERROR: Player not found");
            return Results.ERROR("Player not found");
        }
        Coord2d playerPos = player.rc;

        double dx = Math.cos(direction) * PLACEMENT_DISTANCE;
        double dy = Math.sin(direction) * PLACEMENT_DISTANCE;
        Coord2d placementPos = playerPos.add(dx, dy);

        // Step 4: Right-click in direction to place the lifted boat
        System.out.println("Placing boat " + directionName + " at: " + placementPos);
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
            System.out.println("ERROR: Lost placed boat");
            return Results.ERROR("Lost placed boat after placement");
        }

        System.out.println("Boarding placed boat: " + placedBoat.id);
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

        // Step 6: Lift the old boat (re-find it after boarding)
        final long currentBoatId = currentBoat.id;

        System.out.println("Looking for boat to lift: " + currentBoatId);

        // Re-find the boat (it might have moved after we boarded the new one)
        Gob boatToLift = Finder.findGob(currentBoatId);
        if (boatToLift == null) {
            System.out.println("Boat to lift vanished: " + currentBoatId);
            return Results.ERROR("Boat to lift vanished");
        }

        System.out.println("Found boat to lift at: " + boatToLift.rc);

        // Activate carry mode
        gui.ui.rcvr.rcvmsg(NUtils.getUI().getMenuGridId(), "act", "carry");

        // Brief wait for carry mode to activate
        NUtils.getUI().core.addTask(new NTask() {
            int ticks = 0;
            @Override
            public boolean check() {
                return ticks++ >= 2;
            }
        });

        // Click on the boat to lift it (use fresh position)
        gui.map.wdgmsg("click", Coord.z, boatToLift.rc.floor(posres), 1, 0, 0,
                      (int) currentBoatId, boatToLift.rc.floor(posres), 0, -1);

        // Wait for lift to complete with position-based verification
        final long boatIdFinal = currentBoatId;
        NUtils.getUI().core.addTask(new NTask() {
            int checkCount = 0;
            @Override
            public boolean check() {
                checkCount++;

                // Check if boat is both close to player AND has following attribute
                Gob boat = Finder.findGob(boatIdFinal);
                Gob player = NUtils.player();
                if (boat != null && player != null) {
                    double distance = boat.rc.dist(player.rc);
                    haven.Following fl = boat.getattr(haven.Following.class);
                    boolean hasFollowing = (fl != null && fl.tgt == NUtils.playerID());

                    if (distance < 5.0 && hasFollowing) {
                        System.out.println("Boat lifted - distance: " + String.format("%.1f", distance) + ", following: true");
                        return true; // Lift succeeded
                    }
                }

                // Timeout after 15 ticks
                return checkCount >= 15;
            }
        });

        // Verify lift succeeded using both position AND following attribute
        Gob liftedCheck = Finder.findGob(currentBoatId);
        Gob currentPlayer = NUtils.player();
        boolean liftSucceeded = false;
        if (liftedCheck != null && currentPlayer != null) {
            double distance = liftedCheck.rc.dist(currentPlayer.rc);
            haven.Following fl = liftedCheck.getattr(haven.Following.class);
            boolean hasFollowing = (fl != null && fl.tgt == NUtils.playerID());

            if (distance < 5.0 && hasFollowing) {
                liftSucceeded = true;
                System.out.println("Lift verified - distance: " + String.format("%.1f", distance) + ", following: true - " + directionName + " hop complete!");
            } else {
                System.out.println("Lift incomplete - distance: " + String.format("%.1f", distance) + ", following: " + hasFollowing);
            }
        } else {
            System.out.println("Boat or player not found after lift attempt");
        }

        if (!liftSucceeded) {
            System.out.println("Lift failed");
            return Results.ERROR("Lift failed");
        }

        System.out.println("=== Single BoatHop (" + directionName + ") Complete ===");
        return Results.SUCCESS();
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