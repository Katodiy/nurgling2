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

            // Wait for the boat to no longer be following (placed) - with shorter timeout
            System.out.println("[" + hopCount + "] Waiting for placement...");
            final long boatIdToCheck = liftedBoatId;
            final int hopCountForPlacement = hopCount;
            NUtils.getUI().core.addTask(new NTask() {
                @Override
                public boolean check() {
                    Gob boat = Finder.findGob(boatIdToCheck);
                    if (boat == null) {
                        System.out.println("[" + hopCountForPlacement + "] Boat vanished during placement check");
                        return true; // Exit wait if boat disappeared
                    }
                    boolean notFollowing = boat.getattr(haven.Following.class) == null;
                    if (notFollowing) {
                        System.out.println("[" + hopCountForPlacement + "] Boat no longer following");
                    }
                    return notFollowing;
                }
                {
                    maxCounter = 50; // Shorter timeout
                }
            });
            System.out.println("[" + hopCount + "] Placement wait complete");

            // Re-fetch the boat to check current state
            Gob boatCheck = Finder.findGob(liftedBoatId);
            if (boatCheck != null && boatCheck.getattr(haven.Following.class) != null) {
                System.out.println("[" + hopCount + "] TIMEOUT: Boat placement failed, retrying...");
                continue; // Skip to next iteration instead of erroring out
            }

            // Step 5: Find and right-click the placed boat to board it
            System.out.println("[" + hopCount + "] Finding placed boat...");
            Gob placedBoat = Finder.findGob(liftedBoatId);
            if (placedBoat == null) {
                System.out.println("[" + hopCount + "] ERROR: Lost placed boat, retrying...");
                continue; // Skip to next iteration
            }
            System.out.println("[" + hopCount + "] Found placed boat, boarding...");

            // Wait a moment for the boat to settle before boarding
            NUtils.getUI().core.addTask(new NTask() {
                int ticks = 0;
                @Override
                public boolean check() {
                    return ticks++ > 8;
                }
            });

            NUtils.rclickGob(placedBoat);

            // Wait for boarding (increased wait time)
            System.out.println("[" + hopCount + "] Waiting for boarding...");
            NUtils.getUI().core.addTask(new NTask() {
                int ticks = 0;
                @Override
                public boolean check() {
                    return ticks++ > 5;
                }
            });
            System.out.println("[" + hopCount + "] Boarding complete");

            // Step 6: Lift the old boat (now behind us) - with retries
            final long currentBoatId = currentBoat.id;
            boolean liftSucceeded = false;

            for (int liftAttempt = 1; liftAttempt <= 3 && !liftSucceeded; liftAttempt++) {
                System.out.println("[" + hopCount + "] Lifting old boat (attempt " + liftAttempt + "): " + currentBoatId);

                // Activate carry mode
                gui.ui.rcvr.rcvmsg(NUtils.getUI().getMenuGridId(), "act", "carry");

                // Find the boat again (it might have moved slightly)
                Gob boatToLift = Finder.findGob(currentBoatId);
                if (boatToLift == null) {
                    System.out.println("[" + hopCount + "] ERROR: Boat vanished, retrying entire hop...");
                    break;
                }

                // Click on the boat to lift it
                gui.map.wdgmsg("click", Coord.z, boatToLift.rc.floor(posres), 1, 0, 0,
                              (int) currentBoatId, boatToLift.rc.floor(posres), 0, -1);

                // Wait a moment for the lift action to register
                NUtils.getUI().core.addTask(new NTask() {
                    int ticks = 0;
                    @Override
                    public boolean check() {
                        return ticks++ > 2;
                    }
                });

                // Wait for lift to complete - with explicit timeout
                System.out.println("[" + hopCount + "] Waiting for lift...");
                final int hopCountFinal = hopCount;
                final int attemptNum = liftAttempt;
                NUtils.getUI().core.addTask(new NTask() {
                    int checkCount = 0;
                    @Override
                    public boolean check() {
                        checkCount++;
                        if (checkCount % 10 == 1) {
                            System.out.println("[" + hopCountFinal + "] Lift attempt " + attemptNum + " check " + checkCount);
                        }

                        // Explicit timeout after 50 iterations
                        if (checkCount > 50) {
                            System.out.println("[" + hopCountFinal + "] Lift attempt " + attemptNum + " timeout");
                            return true;
                        }

                        Gob boat = Finder.findGob(currentBoatId);
                        if (boat == null) {
                            System.out.println("[" + hopCountFinal + "] Boat vanished during lift");
                            return true;
                        }

                        haven.Following fl = boat.getattr(haven.Following.class);
                        boolean isLifted = fl != null && fl.tgt == NUtils.playerID();
                        if (isLifted) {
                            System.out.println("[" + hopCountFinal + "] Boat now being carried!");
                        }
                        return isLifted;
                    }
                    {
                        maxCounter = 50;
                    }
                });
                System.out.println("[" + hopCount + "] Lift wait complete");

                // Check if lift succeeded
                Gob liftedCheck = Finder.findGob(currentBoatId);
                if (liftedCheck != null) {
                    haven.Following fl = liftedCheck.getattr(haven.Following.class);
                    if (fl != null && fl.tgt == NUtils.playerID()) {
                        liftSucceeded = true;
                        System.out.println("[" + hopCount + "] Lift succeeded!");
                    }
                }

                if (!liftSucceeded && liftAttempt < 3) {
                    System.out.println("[" + hopCount + "] Lift failed, will retry...");
                }
            }

            if (!liftSucceeded) {
                System.out.println("[" + hopCount + "] ERROR: Failed to lift boat after 3 attempts, retrying hop...");
                continue; // Retry the entire hop
            }
            System.out.println("[" + hopCount + "] Hop complete!");

            // Small delay between hops to let the game settle
            NUtils.getUI().core.addTask(new NTask() {
                int ticks = 0;
                @Override
                public boolean check() {
                    return ticks++ > 2;
                }
            });

            // Continue loop - now we're on the new boat with the old one lifted
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

        // Copy gobs to list first (quickly, inside synchronized block) to avoid holding lock during expensive checks
        java.util.ArrayList<Gob> gobsToCheck = new java.util.ArrayList<>();
        synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                gobsToCheck.add(gob);
            }
        }
        System.out.println("  > Copied " + gobsToCheck.size() + " gobs to check");

        int gobCount = 0;
        int boatCount = 0;

        // Now iterate outside the lock
        for (Gob gob : gobsToCheck) {
            gobCount++;

            // Skip invalid gobs (negative IDs are virtual/placeholder gobs)
            if (gob.id < 0) {
                continue;
            }

            // Skip the lifted boat - we want the boat we're standing ON, not carrying
            if (liftedBoat != null && gob.id == liftedBoat.id) {
                continue;
            }

            // Skip gobs without resources (might be loading or invalid)
            if (gob.rc == null) {
                continue;
            }

            // Check if it's a rowboat (this can throw InterruptedException)
            boolean isBoat = false;
            try {
                isBoat = NParser.isIt(gob, ROWBOAT_ALIAS);
            } catch (InterruptedException e) {
                System.out.println("  > Interrupted while checking gob " + gob.id + ", rethrowing");
                throw e;
            } catch (Exception e) {
                // Skip gobs that cause errors during checking (might be invalid/loading)
                System.out.println("  > Error checking gob " + gob.id + ": " + e.getMessage() + ", skipping");
                continue;
            }

            if (isBoat) {
                boatCount++;
                // Check if player is close to it (standing on it)
                double distance = gob.rc.dist(playerPos);
                System.out.println("  > Found boat " + gob.id + " at distance " + distance);
                if (distance < 2.0) {
                    System.out.println("  > Boat is close enough, returning it!");
                    return gob;
                }
            }
        }

        System.out.println("  > Checked " + gobCount + " gobs, found " + boatCount + " boats, none close enough");
        return null;
    }
}