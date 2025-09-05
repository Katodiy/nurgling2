package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.ScenarioAction;
import nurgling.scenarios.Scenario;
import nurgling.scenarios.ScenarioIcons;

import java.awt.image.BufferedImage;

public class NScenarioButton extends IButton {
    private final Scenario scenario;
    private final ScenarioAction action;
    
    public NScenarioButton(Scenario scenario) {
        super(
            ScenarioIcons.loadScenarioIconUp(scenario),
            ScenarioIcons.loadScenarioIconDown(scenario),
            ScenarioIcons.loadScenarioIconHover(scenario)
        );
        this.scenario = scenario;
        this.action = new ScenarioAction(scenario);
        // No click action - handled by parent widget
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
