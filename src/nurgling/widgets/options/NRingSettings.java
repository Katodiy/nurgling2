package nurgling.widgets.options;

import haven.Button;
import haven.Label;
import haven.*;
import nurgling.NConfig;
import nurgling.NStyle;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NRingSettings extends Widget {

    public NRingSettings() {
        prev = add(new Label("Animal rings settings:"));

        pack();
    }

}