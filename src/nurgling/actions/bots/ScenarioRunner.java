package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.scenarios.*;
import nurgling.actions.bots.registry.BotRegistry;

public class ScenarioRunner implements Action {
    private final Scenario scenario;

    public ScenarioRunner(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        for (BotStep step : scenario.getSteps()) {
            // Get the actual bot action by key
            Action bot = BotRegistry.createBot(step.getBotKey());
            if (bot == null) {
                gui.msg("ScenarioRunner: Unknown bot key: " + step.getBotKey());
                return Results.FAIL();
            }
            Results result = bot.run(gui);
            if (!result.IsSuccess()) {
                gui.msg("ScenarioRunner: Bot failed: " + step.getBotKey());
                return result;
            }
        }
        return Results.SUCCESS();
    }
}
