package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Results;
import nurgling.actions.Action;
import haven.Gob;

/**
 * Boat hopper that continuously hops in the player's facing direction.
 */
public class BoatHopperContinuous implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("=== Starting Continuous BoatHop (Player Direction) ===");

        // Get player's current facing direction
        Gob player = NUtils.player();
        if (player == null) {
            System.out.println("ERROR: Player not found");
            return Results.ERROR("Player not found");
        }

        double playerDirection = player.a;
        String directionName = getDirectionName(playerDirection);

        System.out.println("Player facing direction: " + String.format("%.2f", playerDirection) + " radians (" + directionName + ")");

        int hopCount = 0;
        while (true) {
            hopCount++;
            System.out.println("=== Starting Hop " + hopCount + " (" + directionName + ") ===");

            // Use the shared BoatHopAction with player's direction
            Results result = new BoatHopAction(playerDirection, directionName).run(gui);

            System.out.println("Hop " + hopCount + " completed successfully!");

            // Small delay between hops for stability - no delay needed as BoatHopAction has internal timing
        }
    }

    /**
     * Converts angle in radians to a readable direction name
     */
    private String getDirectionName(double angle) {
        // Normalize angle to 0-2π range
        angle = angle % (2 * Math.PI);
        if (angle < 0) angle += 2 * Math.PI;

        // Convert to degrees for easier understanding
        double degrees = Math.toDegrees(angle);

        if (degrees >= 315 || degrees < 45) {
            return "East (" + String.format("%.0f", degrees) + "°)";
        } else if (degrees >= 45 && degrees < 135) {
            return "South (" + String.format("%.0f", degrees) + "°)";
        } else if (degrees >= 135 && degrees < 225) {
            return "West (" + String.format("%.0f", degrees) + "°)";
        } else {
            return "North (" + String.format("%.0f", degrees) + "°)";
        }
    }
}