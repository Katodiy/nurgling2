package nurgling.widgets;

import haven.*;
import nurgling.*;

import java.io.File;

public class NImportStrategyDialog extends Window
{
    public enum ImportStrategy {
        FULL_REPLACE,    // Delete all old, add new
        DUPLICATE,       // Old behavior - rename duplicates
        OVERWRITE        // Replace areas with same name
    }
    
    private File selectedFile;
    
    public NImportStrategyDialog()
    {
        super(UI.scale(new Coord(300, 120)), "Import Strategy");
        
        int btnWidth = UI.scale(90);
        int btnHeight = UI.scale(25);
        int spacing = UI.scale(10);
        
        // Label
        prev = add(new Label("Choose import strategy:"), UI.scale(10, 10));
        
        // Full Replace button
        prev = add(new Button(UI.scale(280), "Full Replace (delete old, add new)")
        {
            @Override
            public void click()
            {
                super.click();
                executeImport(ImportStrategy.FULL_REPLACE);
            }
        }, prev.pos("bl").adds(0, 15));
        
        // Duplicate button  
        prev = add(new Button(UI.scale(280), "Duplicate (rename conflicts)")
        {
            @Override
            public void click()
            {
                super.click();
                executeImport(ImportStrategy.DUPLICATE);
            }
        }, prev.pos("bl").adds(0, 5));
        
        // Overwrite button
        prev = add(new Button(UI.scale(280), "Overwrite (replace same names)")
        {
            @Override
            public void click()
            {
                super.click();
                executeImport(ImportStrategy.OVERWRITE);
            }
        }, prev.pos("bl").adds(0, 5));
        
        pack();
    }
    
    private void executeImport(ImportStrategy strategy) {
        if(selectedFile != null) {
            switch(strategy) {
                case FULL_REPLACE:
                    NUtils.getUI().core.config.replaceAreas(selectedFile);
                    break;
                case DUPLICATE:
                    NUtils.getUI().core.config.mergeAreas(selectedFile);
                    break;
                case OVERWRITE:
                    NUtils.getUI().core.config.overwriteAreas(selectedFile);
                    break;
            }
            NConfig.needAreasUpdate();
            if(NUtils.getGameUI().areas != null) {
                NUtils.getGameUI().areas.hide();
                NUtils.getGameUI().areas.show();
            }
        }
        hide();
    }

    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if(msg.equals("close"))
        {
            hide();
        }
        else
        {
            super.wdgmsg(msg, args);
        }
    }

    public static void showDialog(File file)
    {
        NUtils.getGameUI().importDialog.selectedFile = file;
        NUtils.getGameUI().importDialog.show();
        NUtils.getGameUI().importDialog.raise();
        // Position relative to areas widget if it exists and is visible
        if(NUtils.getGameUI().areas != null && NUtils.getGameUI().areas.visible()) {
            NUtils.getGameUI().importDialog.c = NUtils.getGameUI().areas.c.add(
                (NUtils.getGameUI().areas.sz.x - NUtils.getGameUI().importDialog.sz.x) / 2,
                (NUtils.getGameUI().areas.sz.y - NUtils.getGameUI().importDialog.sz.y) / 2
            );
        }
    }
}

