package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.tools.EncyclopediaManager;
import nurgling.widgets.EncyclopediaWindow;

public class EncyclopediaPanel extends Panel {
    private EncyclopediaWindow encyclopediaWindow;
    
    public EncyclopediaPanel() {
        super("");
        
        int margin = UI.scale(10);
        Widget prev;
        
        // Title
        prev = add(new Label("Encyclopedia"), new Coord(margin, margin));
        
        // Simple open button
        prev = add(new Button(UI.scale(150), "Open Encyclopedia") {
            @Override
            public void click() {
                super.click();
                openEncyclopedia();
            }
        }, prev.pos("bl").adds(0, 15));
        
        // Info text
        prev = add(new Label("Browse documentation for the Nurgling2 client."), 
                  prev.pos("bl").adds(0, 15));
        
        pack();
    }
    
    @Override
    public void load() {
        // No configuration needed
    }
    
    @Override
    public void save() {
        // No configuration to save
    }
    
    private void openEncyclopedia() {
        if (encyclopediaWindow != null) {
            encyclopediaWindow.destroy();
        }
        
        encyclopediaWindow = new EncyclopediaWindow();
        ui.gui.add(encyclopediaWindow, ui.gui.sz.div(2).sub(encyclopediaWindow.sz.div(2)));
        encyclopediaWindow.show();
    }
}