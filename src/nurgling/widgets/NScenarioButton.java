package nurgling.widgets;

import haven.*;
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
}
