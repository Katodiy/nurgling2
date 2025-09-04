package nurgling.actions;

import nurgling.NGameUI;
import nurgling.actions.bots.ScenarioRunner;
import nurgling.scenarios.Scenario;

public class ScenarioAction implements Action {
    private final Scenario scenario;
    private final boolean disStacks;
    
    public ScenarioAction(Scenario scenario) {
        this(scenario, false);
    }
    
    public ScenarioAction(Scenario scenario, boolean disStacks) {
        this.scenario = scenario;
        this.disStacks = disStacks;
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        ScenarioRunner runner = new ScenarioRunner(scenario);
        return runner.run(gui);
    }
    
    public String getDisplayName() {
        return scenario.getName();
    }
    
    public Scenario getScenario() {
        return scenario;
    }
    
    public boolean getDisStacks() {
        return disStacks;
    }
}