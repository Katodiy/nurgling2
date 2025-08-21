package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.tools.EncyclopediaManager;
import nurgling.widgets.EncyclopediaWindow;

public class EncyclopediaPanel extends Panel {
    private TextEntry pathEntry;
    private Button openButton;
    private Button reloadButton;
    private Label statusLabel;
    private EncyclopediaWindow encyclopediaWindow;
    
    public EncyclopediaPanel() {
        super("");
        
        int margin = UI.scale(10);
        Widget prev;
        
        // Title
        prev = add(new Label("Encyclopedia Settings"), new Coord(margin, margin));
        
        // Path setting
        prev = add(new Label("Documents Path:"), prev.pos("bl").adds(0, 15));
        pathEntry = add(new TextEntry(UI.scale(300), ""), prev.pos("bl").adds(0, 5));
        
        // Buttons
        Button browseButton = add(new Button(UI.scale(80), "Browse") {
            @Override
            public void click() {
                super.click();
                // TODO: Add file browser dialog
                ui.gui.msg("File browser not yet implemented. Edit path manually.", java.awt.Color.YELLOW);
            }
        }, pathEntry.pos("ur").adds(5, 0));
        
        prev = openButton = add(new Button(UI.scale(120), "Open Encyclopedia") {
            @Override
            public void click() {
                super.click();
                openEncyclopedia();
            }
        }, pathEntry.pos("bl").adds(0, 10));
        
        reloadButton = add(new Button(UI.scale(100), "Reload Docs") {
            @Override
            public void click() {
                super.click();
                reloadDocuments();
            }
        }, prev.pos("ur").adds(10, 0));
        
        // Status
        statusLabel = add(new Label("Status: Not loaded"), prev.pos("bl").adds(0, 15));
        
        // Info text
        prev = add(new Label("Place .md files in the documents directory to add them to the encyclopedia."), 
                  statusLabel.pos("bl").adds(0, 10));
        prev = add(new Label("Supports markdown: headers, bold, italic, links, and images."), 
                  prev.pos("bl").adds(0, 5));
        
        pack();
    }
    
    @Override
    public void load() {
        String path = (String) NConfig.get(NConfig.Key.encyclopediaPath);
        if (path != null) {
            pathEntry.settext(path);
        }
        updateStatus();
    }
    
    @Override
    public void save() {
        String path = pathEntry.text().trim();
        if (!path.isEmpty()) {
            NConfig.set(NConfig.Key.encyclopediaPath, path);
            NConfig.needUpdate();
        }
    }
    
    private void openEncyclopedia() {
        if (encyclopediaWindow != null) {
            encyclopediaWindow.destroy();
        }
        
        // Save current path before opening
        save();
        
        encyclopediaWindow = new EncyclopediaWindow();
        ui.gui.add(encyclopediaWindow, ui.gui.sz.div(2).sub(encyclopediaWindow.sz.div(2)));
        encyclopediaWindow.show();
        updateStatus();
    }
    
    private void reloadDocuments() {
        // Save current path before reloading
        save();
        
        EncyclopediaManager.getInstance().reload();
        updateStatus();
        ui.gui.msg("Encyclopedia documents reloaded", java.awt.Color.GREEN);
    }
    
    private void updateStatus() {
        EncyclopediaManager manager = EncyclopediaManager.getInstance();
        int docCount = manager.getAllDocumentKeys().size();
        
        if (docCount == 0) {
            statusLabel.settext("Status: No documents found");
        } else {
            statusLabel.settext("Status: " + docCount + " documents loaded");
        }
    }
}