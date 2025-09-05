package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.bots.ScenarioRunner;
import nurgling.scenarios.Scenario;
import nurgling.scenarios.ScenarioIcons;

public class NScenarioButton extends IButton {
    private final Scenario scenario;

    public NScenarioButton(Scenario scenario) {
        super(
            ScenarioIcons.loadScenarioIconUp(scenario),
            ScenarioIcons.loadScenarioIconDown(scenario),
            ScenarioIcons.loadScenarioIconHover(scenario)
        );
        this.scenario = scenario;
        setupButton();
    }
    
    private void setupButton() {
        // Set up the click action for direct scenario execution
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
            return scenario.getName();
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
