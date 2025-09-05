package nurgling.scenarios;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.bots.ScenarioRunner;

public class ScenarioDisplayButton extends IButton {
    private final Scenario scenario;

    public ScenarioDisplayButton(Scenario scenario) {
        super(
            ScenarioIcons.loadScenarioIconUp(scenario),
            ScenarioIcons.loadScenarioIconDown(scenario), 
            ScenarioIcons.loadScenarioIconHover(scenario)
        );
        this.scenario = scenario;
        setupButton();
    }
    
    private void setupButton() {
        this.action(() -> executeScenario());
    }
    
    private void executeScenario() {
        if (scenario != null) {
            // Run scenario in background thread like other bots do
            Thread t = new Thread(() -> {
                try {
                    ScenarioRunner runner = new ScenarioRunner(scenario);
                    runner.run(NUtils.getGameUI());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    NUtils.getGameUI().error("Scenario execution failed: " + e.getMessage());
                }
            }, "ScenarioRunner-" + scenario.getName());
            
            // Add to bot observer system like other actions
            NUtils.getGameUI().biw.addObserve(t);
            t.start();
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        if (scenario != null) {
            return scenario.getName() + " (Click to run)";
        }
        return super.tooltip(c, prev);
    }
    
    public Scenario getScenario() {
        return scenario;
    }
    
    public String getDisplayName() {
        return scenario != null ? scenario.getName() : "Unknown Scenario";
    }
}