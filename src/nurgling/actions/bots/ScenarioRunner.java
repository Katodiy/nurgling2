package nurgling.actions.bots;

import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.scenarios.*;
import nurgling.actions.bots.registry.BotRegistry;

public class ScenarioRunner implements Action {
    private final Scenario scenario;

    public ScenarioRunner(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        System.out.println("[ScenarioRunner] Starting scenario with " + scenario.getSteps().size() + " steps");
        for (BotStep step : scenario.getSteps()) {
            System.out.println("[ScenarioRunner] Running step: " + step.getId());
            BotDescriptor desc = BotRegistry.byId(step.getId());
            Action bot = (desc != null) ? desc.instantiate(step.getSettings()) : null;
            if (bot == null) {
                System.out.println("[ScenarioRunner] Unknown bot key: " + step.getId());
                gui.msg("ScenarioRunner: Unknown bot key: " + step.getId());
                return Results.FAIL();
            }
            System.out.println("[ScenarioRunner] Executing bot: " + bot.getClass().getSimpleName());
            Results result = bot.run(gui);
            System.out.println("[ScenarioRunner] Step " + step.getId() + " result: " + (result.IsSuccess() ? "SUCCESS" : "FAIL/ERROR"));
            if (!result.IsSuccess()) {
                gui.msg("ScenarioRunner: Bot failed: " + step.getId());
                return result;
            }
        }
        System.out.println("[ScenarioRunner] Scenario completed successfully");
        return Results.SUCCESS();
    }
}
