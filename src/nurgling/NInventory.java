package nurgling;

import haven.*;
import haven.Window;
import haven.res.ui.stackinv.ItemStack;
import haven.res.ui.tt.slot.Slotted;
import haven.res.ui.tt.stackn.Stack;
import monitoring.ItemWatcher;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.NPopupWidget;
import nurgling.widgets.NSearchWidget;

import java.util.Map;
import java.util.HashMap;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.List;

public class NInventory extends Inventory
{
    public NSearchWidget searchwdg;
    public NPopupWidget toggles;
    public NPopupWidget rightToggles;
    public ItemSListBox itemListBox;
    public Dropbox<String> sortTypeDropbox;
    public Dropbox<String> orderDropbox;
    public ICheckBox bundle;
    public MenuGrid.PagButton pagBundle = null;
    boolean showPopup = false;
    boolean showRightPopup = false;
    BufferedImage numbers = null;
    short[][] oldinv = null;
    public Gob parentGob = null;
    long lastUpdate = 0;

    public NInventory(Coord sz)
    {
        super(sz);
    }

    public enum QualityType {
        High, Low
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        if(numbers!=null) {
            g.image(numbers,Coord.z);
        }
    }


    void generateNumberMatrix(short[][] inventory)
    {
        oldinv = inventory.clone();
        TexI[][] numberMatrix = new TexI[isz.y][isz.x];
        int counter = 1;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if (inventory[i][j] == 0)
                {
                    numberMatrix[i][j] = new TexI(NStyle.slotnums.render(String.valueOf(counter)).img);
                }
                else
                {
                    numberMatrix[i][j] = null;
                }
                if(inventory[i][j] != 2)
                    counter++;
            }
        }
        WritableRaster buf = Raster.createInterleavedRaster(java.awt.image.DataBuffer.TYPE_BYTE, isz.x*sqsz.y, isz.y*sqsz.x, 4, null);
        BufferedImage tgt = new BufferedImage(new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, java.awt.image.DataBuffer.TYPE_BYTE), buf, false, null);
        Graphics2D g = tgt.createGraphics();
        Coord coord = new Coord(0,0);
        for (coord.y = 0; coord.y < isz.y; coord.y++) {
            for (coord.x = 0; coord.x < isz.x; coord.x++) {
                if(numberMatrix[coord.y][coord.x]!=null) {
                    Coord pos = coord.mul(sqsz).add(sqsz.div(2));
                    TexI img = numberMatrix[coord.y][coord.x];
                    Coord sz = img.sz();
                    pos = pos.add((int)((double)sz.x * -0.5), (int)((double)sz.y * -0.5));
                    g.drawImage(img.back, pos.x,pos.y,null);
                }
            }
        }
        g.dispose();
        numbers = tgt;
    }

    @Override
    public void addchild(Widget child, Object... args) {
        super.addchild(child, args);
    }

    public int getNumberFreeCoord(Coord coord) throws InterruptedException
    {
        GetNumberFreeCoord gnfc = new GetNumberFreeCoord(this, coord);
        NUtils.getUI().core.addTask(gnfc);
        return gnfc.result();
    }


    public int getNumberFreeCoord(GItem item) throws InterruptedException
    {
        GetNumberFreeCoord gnfc = new GetNumberFreeCoord(this, item);
        NUtils.getUI().core.addTask(gnfc);
        return gnfc.result();
    }

    public int getNumberFreeCoord(WItem item) throws InterruptedException
    {
        return getNumberFreeCoord(item.item);
    }

    public Coord getFreeCoord(WItem item) throws InterruptedException
    {
        GetFreePlace gfp = new GetFreePlace(this, item.item);
        NUtils.getUI().core.addTask(gfp);
        return gfp.result();
    }

    public int getFreeSpace() throws InterruptedException
    {
        GetFreeSpace gfs = new GetFreeSpace(this);
        NUtils.getUI().core.addTask(gfs);
        return gfs.result();
    }

    public int getTotalSpace() throws InterruptedException
    {
        GetTotalSpace gts = new GetTotalSpace(this);
        NUtils.getUI().core.addTask(gts);
        return gts.result();
    }

    public int getTotalAmountItems(NAlias name) throws InterruptedException
    {
        GetTotalAmountItems gi = new GetTotalAmountItems(this, name);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public WItem getItem(NAlias name) throws InterruptedException
    {
        GetItem gi = new GetItem(this, name);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public WItem getItem(NAlias name, Class<? extends ItemInfo> prop) throws InterruptedException
    {
        GetItem gi = new GetItem(this, name, prop);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public WItem getItem(NAlias name, Float q) throws InterruptedException
    {
        GetItem gi = new GetItem(this, name, q);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public WItem getItem(String name) throws InterruptedException
    {
        return getItem(new NAlias(name));
    }

    public WItem getItem(NAlias name, QualityType type) throws InterruptedException {
        ArrayList<WItem> items = getItems(name, type);
        if (items.isEmpty()) {
            return null;
        }
        return items.get(0);
    }

    public ArrayList<WItem> getItems(NAlias name, QualityType type) throws InterruptedException {
        GetItems gi = new GetItems(this, name, type);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }


    public ArrayList<WItem> getItems() throws InterruptedException
    {
        GetItems gi = new GetItems(this);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public ArrayList<WItem> getItems(NAlias name) throws InterruptedException
    {
        GetItems gi = new GetItems(this, name);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public ArrayList<WItem> getWItems(NAlias name) throws InterruptedException
    {
        GetWItems gi = new GetWItems(this, name);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }



    public ArrayList<WItem> getItems(NAlias name, double th) throws InterruptedException
    {
        GetItems gi = new GetItems(this, name, (float)th);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public ArrayList<WItem> getItems(String name) throws InterruptedException
    {
        return getItems(new NAlias(name));
    }

    public ArrayList<WItem> getItems(String name, double th) throws InterruptedException
    {
        return getItems(new NAlias(name), th);
    }

    public ArrayList<WItem> getItems(GItem target) throws InterruptedException
    {
        GetItems gi = new GetItems(this, target);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public void activateItem(NAlias name) throws InterruptedException {
        WItem it = getItem(name);
        it.item.wdgmsg("iact", Coord.z, 1);
    }

    public void activateItem(WItem item) throws InterruptedException {
        item.item.wdgmsg("iact", Coord.z, 1);
    }

    public void dropOn(Coord dc, String name) throws InterruptedException
    {
        if (NUtils.getGameUI().vhand != null)
        {
            wdgmsg("drop", dc);
            NUtils.getUI().core.addTask(new DropOn(this, dc, name));
        }
    }

    public void dropOn(Coord dc, NAlias name) throws InterruptedException
    {
        if (NUtils.getGameUI().vhand != null)
        {
            wdgmsg("drop", dc);
            NUtils.getUI().core.addTask(new DropOn(this, dc, name));
        }
    }

    public void dropOn(Coord dc) throws InterruptedException
    {
        if (NUtils.getGameUI().vhand != null)
        {
            wdgmsg("drop", dc);
            NUtils.getUI().core.addTask(new DropOn(this, dc, ((NGItem)NUtils.getGameUI().vhand.item).name()));
        }
    }

    @Override
    public void resize(Coord sz) {
        super.resize(new Coord(sz));
        searchwdg.resize(new Coord(sz.x , 0));
        searchwdg.move(new Coord(0,sz.y + UI.scale(5)));
        parent.pack();
        movePopup(parent.c);
    }

    public void movePopup(Coord c) {
        if(toggles !=null)
        {
            toggles.move(new Coord(c.x - toggles.sz.x + toggles.atl.x +UI.scale(10),c.y + UI.scale(35)));
        }
        if(rightToggles != null)
        {
            rightToggles.move(new Coord(c.x + parent.sz.x - rightToggles.atl.x - UI.scale(10), c.y + UI.scale(35)));
        }
        if(searchwdg!=null && searchwdg.history!=null) {
            searchwdg.history.move(new Coord(c.x  + ((Window)parent).ca().ul.x + UI.scale(7), c.y + parent.sz.y- UI.scale(37)));
        }
    }

    @Override
    public void tick(double dt) {
        if(lastUpdate==0)
        {
            lastUpdate = NUtils.getTickId();
        }
        if(!iis.isEmpty() && NUtils.getTickId() - lastUpdate > 20)
        {
            iis.clear();
        }
        if(NUtils.getGameUI() == null)
            return;
        super.tick(dt);
        if((Boolean)NConfig.get(NConfig.Key.showInventoryNums)) {
            short[][] newInv = containerMatrix();
            boolean isDiffrent = false;
            if (newInv != null)
                if (oldinv != null) {
                    if (newInv.length != oldinv.length)
                        isDiffrent = true;
                    else {
                        for (int i = 0; i < newInv.length; i++) {
                            if (newInv[i].length != oldinv[i].length) {
                                isDiffrent = true;
                                break;
                            }
                            for (int j = 0; j < newInv[i].length; j++) {
                                if (newInv[i][j] != oldinv[i][j]) {
                                    isDiffrent = true;
                                    break;
                                }
                            }
                        }
                        if(!isDiffrent && numbers == null)
                            isDiffrent = true;
                    }
                } else {
                    isDiffrent = true;
                }
            if (isDiffrent)
                generateNumberMatrix(newInv);
        }
        else
            numbers = null;
        if(toggles !=null)
            toggles.visible = parent.visible && showPopup;
        if(rightToggles != null) {
            rightToggles.visible = parent.visible && showRightPopup;
            if (showRightPopup) {
                // Update right panel contents periodically
                if (NUtils.getTickId() % 10 == 0) { // Update every 10 ticks
                    updateRightPanelItems();
                }
            }
        }
    }

    private static final TexI[] collapsei = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/itogglec/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/itogglec/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/itogglec/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/itogglec/dh"))};

    private static final TexI[] gildingi = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/gilding/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/gilding/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/gilding/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/gilding/dh"))};

    private static final TexI[] vari = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/var/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/var/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/var/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/var/dh"))};

    private static final TexI[] stacki = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/stack/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/stack/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/stack/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/stack/dh"))};

    private static final TexI[] autoflower = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/autoflower/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/autoflower/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/autoflower/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/autoflower/dh"))};

    private static final TexI[] autosplittor = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/autosplittor/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/autosplittor/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/autosplittor/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/autosplittor/dh"))};

    private static final TexI[] bundlei = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/bundle/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/bundle/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/bundle/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/bundle/dh"))};

    private static final TexI[] numberi = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/numbering/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/numbering/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/numbering/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/numbering/dh"))};

    private static final TexI[] dropperi = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/dropper/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/dropper/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/dropper/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/dropper/dh"))};

    public void installMainInv() {
        searchwdg = new NSearchWidget(new Coord(sz));
        searchwdg.resize(sz);
        parent.add(searchwdg, (new Coord(0, sz.y + UI.scale(10))));
        parent.add(new ICheckBox(collapsei[0], collapsei[1], collapsei[2], collapsei[3]) {
                       @Override
                       public void changed(boolean val) {
                           super.changed(val);
                           showPopup = val;
                       }
                   }
                , new Coord(-gildingi[0].sz().x + UI.scale(2), UI.scale(27)));

        // Right panel toggle button
        parent.add(new ICheckBox(collapsei[0], collapsei[1], collapsei[2], collapsei[3]) {
                       @Override
                       public void changed(boolean val) {
                           super.changed(val);
                           showRightPopup = val;
                       }
                   }
                , new Coord(sz.x - UI.scale(2), UI.scale(27)));

        parent.pack();
        toggles = NUtils.getGameUI().add(new NPopupWidget(new Coord(UI.scale(50), UI.scale(80)), NPopupWidget.Type.RIGHT));
        rightToggles = NUtils.getGameUI().add(new NPopupWidget(new Coord(UI.scale(180), UI.scale(230)), NPopupWidget.Type.LEFT));

        Widget pw = toggles.add(new ICheckBox(gildingi[0], gildingi[1], gildingi[2], gildingi[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                Slotted.show = val;
            }
        }, toggles.atl);
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/gilding/u").flayer(Resource.tooltip).t);
        ((ICheckBox)pw).a = Slotted.show;
        pw = toggles.add(new ICheckBox(vari[0], vari[1], vari[2], vari[3]) {
            @Override
            public void changed(boolean val)
            {
                super.changed(val);
                NFoodInfo.show = val;
                NConfig.set(NConfig.Key.showVarity, val);
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/var/u").flayer(Resource.tooltip).t);
        NFoodInfo.show = (Boolean)NConfig.get(NConfig.Key.showVarity);
        ((ICheckBox)pw).a = NFoodInfo.show;

        ICheckBox rpw = toggles.add(new ICheckBox(numberi[0], numberi[1], numberi[2], numberi[3]) {
            @Override
            public void changed(boolean val)
            {
                super.changed(val);
                NConfig.set(NConfig.Key.showInventoryNums, val);
            }
        }, pw.pos("ur").add(UI.scale(new Coord(5, 0))));
        rpw.settip(Resource.remote().loadwait("nurgling/hud/buttons/numbering/u").flayer(Resource.tooltip).t);
        ((ICheckBox)rpw).a = (Boolean)NConfig.get(NConfig.Key.showInventoryNums);

        pw = toggles.add(new ICheckBox(stacki[0], stacki[1], stacki[2], stacki[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                Stack.show = val;
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        ((ICheckBox)pw).a = Stack.show;
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/stack/u").flayer(Resource.tooltip).t);

        bundle = toggles.add(new ICheckBox(bundlei[0], bundlei[1], bundlei[2], bundlei[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                pagBundle.use(new MenuGrid.Interaction(1, 0));
            }
        }, pw.pos("ur").add(UI.scale(new Coord(5, 0))));
        bundle.settip(Resource.remote().loadwait("nurgling/hud/buttons/bundle/u").flayer(Resource.tooltip).t);

        pw = toggles.add(new ICheckBox(autoflower[0], autoflower[1], autoflower[2], autoflower[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                NConfig.set(NConfig.Key.autoFlower, val);
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/autoflower/u").flayer(Resource.tooltip).t);
        ((ICheckBox)pw).a = (Boolean)NConfig.get(NConfig.Key.autoFlower);
        pw = toggles.add(new ICheckBox(autosplittor[0], autosplittor[1], autosplittor[2], autosplittor[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                NConfig.set(NConfig.Key.autoSplitter, val);
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/autosplittor/u").flayer(Resource.tooltip).t);

        pw = toggles.add(new ICheckBox(dropperi[0], dropperi[1], dropperi[2], dropperi[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                NConfig.set(NConfig.Key.autoDropper, val);
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/dropper/u").flayer(Resource.tooltip).t);

        toggles.pack();

        // Setup right panel sorting controls
        setupRightPanel();

        movePopup(parent.c);
        toggles.pack();
        rightToggles.pack();
    }

    private void setupRightPanel() {
        Coord currentPos = rightToggles.atl.add(new Coord(50, 1));
        
        // Sort type dropdown (left side)
        sortTypeDropbox = new Dropbox<String>(UI.scale(150), 4, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                String[] options = {"Count", "Name", "Resource", "Quality"};
                return options[i];
            }
            
            @Override
            protected int listitems() { return 4; }
            
            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, new Coord(2, 1));
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                applySorting();
            }
        };
        // Set default selection
        sortTypeDropbox.change("Count");
        rightToggles.add(sortTypeDropbox, currentPos.add(new Coord(10, 0)));
        
        // Order dropdown (right side)  
        orderDropbox = new Dropbox<String>(UI.scale(150), 2, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                String[] options = {"Asc", "Desc"};
                return options[i];
            }
            
            @Override
            protected int listitems() { return 2; }
            
            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, new Coord(2, 1));
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                applySorting();
            }
        };
        // Set default selection
        orderDropbox.change("Desc");
        rightToggles.add(orderDropbox, new Coord(currentPos.x + UI.scale(100), currentPos.y));
        
        // Create SListBox for item list - position it below the dropdowns
        itemListBox = rightToggles.add(new ItemSListBox(new Coord(UI.scale(180), UI.scale(250))),
                                      new Coord(currentPos.x, UI.scale(45)));
        
        // Initial population of items
        if (itemListBox != null) {
            itemListBox.updateItems();
        }
    }

    private void applySorting() {
        // Trigger re-population of items with current sort settings
        if (itemListBox != null) {
            itemListBox.updateItems();
        }
    }

    private void applyItemQualitySort(String itemName, boolean ascending) {
        // Implement item-specific quality sorting
        // This would sort items of a specific type by quality
    }

    private void updateRightPanelItems() {
        if (itemListBox != null) {
            itemListBox.updateItems();
        }
    }
    
    // Helper class to group items by name
    private static class ItemGroup {
        String name;
        int totalQuantity = 0;
        double averageQuality = 0;
        java.util.List<NGItem> items = new ArrayList<>();
        
        ItemGroup(String name) {
            this.name = name;
        }
        
        void addItem(NGItem item) {
            items.add(item);
            recalculate();
        }
        
        void recalculate() {
            // Recalculate total quantity and quality
            totalQuantity = 0;
            double totalQuality = 0;
            int qualityCount = 0;
            
            for (NGItem item : items) {
                // Try to get stack size, default to 1
                int stackSize = 1;
                try {
                    // Check if item has a stack size property
                    if (item.num >= 0) {
                        stackSize = item.num;
                    }
                } catch (Exception e) {
                    stackSize = 1;
                }
                
                totalQuantity += stackSize;
                
                // Calculate quality
                if (item.quality != null && item.quality > 0) {
                    totalQuality += item.quality;
                    qualityCount++;
                }
            }
            
            if (qualityCount > 0) {
                averageQuality = totalQuality / qualityCount;
            } else {
                averageQuality = 0;
            }
        }
        
        NGItem getRepresentativeItem() {
            return items.isEmpty() ? null : items.get(0);
        }
    }
    
    // SListBox implementation for inventory items
    private class ItemSListBox extends SListBox<ItemGroup, Widget> {
        private java.util.List<ItemGroup> itemGroups = new ArrayList<>();
        
        public ItemSListBox(Coord sz) {
            super(sz, UI.scale(34), 0);  // Increased to accommodate 32px icons
        }
        
        protected java.util.List<ItemGroup> items() {
            return itemGroups;
        }
        
        protected Widget makeitem(ItemGroup group, int idx, Coord sz) {
            return new Widget(sz) {
                @Override
                public void draw(GOut g) {
                    int iconSize = UI.scale(24);
                    
                    // Draw item icon or placeholder
                    NGItem representativeItem = group.getRepresentativeItem();
                        try {
                            GSprite spr = representativeItem.spr();
                            if (spr != null) {
                                // Get the sprite's texture and draw it scaled
                                Coord iconPos = new Coord(2, 1);
                                Resource.Image img = representativeItem.getres().layer(Resource.imgc);
                                if (img != null) {
                                    g.image(img.tex(), iconPos, new Coord(iconSize, iconSize));
                                } else {
                                    // Fallback: draw gray placeholder
                                    g.chcolor(128, 128, 128, 180);
                                    g.frect(iconPos, new Coord(iconSize, iconSize));
                                    g.chcolor();
                                }
                            } else {
                                // Draw gray placeholder if no sprite
                                g.chcolor(128, 128, 128, 180);
                                g.frect(new Coord(2, 1), new Coord(iconSize, iconSize));
                                g.chcolor();
                            }
                        } catch (Exception e) {
                            // Draw gray placeholder if sprite fails
                            g.chcolor(128, 128, 128, 180);
                            g.frect(new Coord(2, 1), new Coord(iconSize, iconSize));
                            g.chcolor();
                        }
                    
                    // Draw quantity and name
                    String displayText = String.format("%d %s", group.totalQuantity, group.name);
                    g.text(displayText, new Coord(iconSize + 5, 2));
                    
                    // Draw average quality if available
                    if (group.averageQuality > 0) {
                        String qualityText = String.format("%.1f", group.averageQuality);
                        g.text(qualityText, new Coord(sz.x - 35, 2));
                    }
                }
            };
        }
        
        public void updateItems() {
            // Get current inventory items and group by name  
            Map<String, ItemGroup> itemGroupMap = new HashMap<>();
            
            // Access parent inventory's children
            for (Widget widget = NInventory.this.child; widget != null; widget = widget.next) {
                if (widget instanceof WItem) {
                    WItem wItem = (WItem) widget;
                    if (wItem.item instanceof NGItem) {
                        NGItem nitem = (NGItem) wItem.item;
                        String itemName = nitem.name();
                        
                        if (itemName != null) {
                            ItemGroup group = itemGroupMap.get(itemName);
                            if (group == null) {
                                group = new ItemGroup(itemName);
                                itemGroupMap.put(itemName, group);
                            }
                            group.addItem(nitem);
                        }
                    }
                }
            }
            
            // Sort the items based on current dropdown selections
            itemGroups = new ArrayList<>(itemGroupMap.values());
            itemGroups.sort((a, b) -> {
                int result = 0;
                
                if (sortTypeDropbox != null && sortTypeDropbox.sel != null) {
                    switch (sortTypeDropbox.sel) {
                        case "Count":
                            result = Integer.compare(a.totalQuantity, b.totalQuantity);
                            break;
                        case "Name":
                            result = a.name.compareTo(b.name);
                            break;
                        case "Resource":
                            result = a.name.compareTo(b.name); // Same as name for now
                            break;
                        case "Quality":
                            result = Double.compare(a.averageQuality, b.averageQuality);
                            break;
                        default:
                            result = 0;
                    }
                }
                
                // Apply ascending/descending order
                if (orderDropbox != null && "Asc".equals(orderDropbox.sel)) {
                    return result;
                } else {
                    return -result;
                }
            });
            
            // Tell SListBox that items have changed
            reset();
        }
        
        @Override
        public void tick(double dt) {
            // Periodically update items to keep list current
            updateItems();
            super.tick(dt);
        }
    }

    public short[][] containerMatrix()
    {
        short[][] ret = new short[isz.y][isz.x];
        for (int x = 0; x < isz.x; x++)
        {
            for (int y = 0; y < isz.y; y++)
            {
                if (sqmask == null || !sqmask[y * isz.x + x])
                {
                    ret[y][x] = 0; // Пустая ячейка
                }
                else
                {
                    ret[y][x] = 2; // Заблокированная ячейка
                }
            }
        }
        for (Widget widget = child; widget != null; widget = widget.next)
        {
            if (widget instanceof WItem)
            {
                WItem item = (WItem) widget;
                if (((NGItem) item.item).spr != null)
                {
                    Coord size = ((NGItem) item.item).spr.sz().div(UI.scale(32));
                    int xSize = size.x;
                    int ySize = size.y;
                    int xLoc = item.c.div(Inventory.sqsz).x;
                    int yLoc = item.c.div(Inventory.sqsz).y;

                    for (int j = 0; j < ySize; j++)
                    {
                        for (int i = 0; i < xSize; i++)
                        {
                            if (yLoc + j < isz.y && xLoc + i < isz.x)
                            {
                                ret[yLoc + j][xLoc + i] = 1;
                            }
                        }
                    }
                }
                else
                    return null;
            }
        }
        return ret;
    }

    public int calcNumberFreeCoord(Coord target_size) {
        if (target_size.x < 1 || target_size.y < 1)
            return 0;

        int count = 0;
        short[][] inventory = containerMatrix();
        if (inventory == null)
            return -1;
        for (int i = 0; i <= inventory.length - target_size.x; i++)
            for (int j = 0; j <= inventory[i].length - target_size.y; j++) {
                boolean isFree = true;
                for (int k = i; k < i + target_size.x; k++)
                    for (int n = j; n < j + target_size.y; n++)
                        if (inventory[k][n] != 0) {
                            isFree = false;
                            break;
                        }

                if (isFree) {
                    count++;
                    for (int k = i; k < i + target_size.x; k++)
                        for (int n = j; n < j + target_size.y; n++)
                            inventory[k][n] = 1;
                }
            }

        return count;
    }

    public Coord findFreeCoord(WItem wItem)
    {
        Coord sz = wItem.item.spr.sz().div(UI.scale(32));
        return findFreeCoord(new Coord(sz.y,sz.x));
    }


    public Coord findFreeCoord(Coord target_size) {
        short[][] inventory = containerMatrix();
        if ((inventory == null) || (target_size.y < 1) || (target_size.x < 1))
            return null;
        for (int i = 0; i <= isz.y - target_size.y; i++)
            for (int j = 0; j <= isz.x - target_size.x; j++)
                if (inventory[i][j] == 0) {
                    boolean isFree = true;
                    for (int k = i; k < i + target_size.x; k++)
                        for (int n = j; n < j + target_size.y; n++)
                            if (n >= isz.x || k >= isz.y || inventory[k][n] != 0) {
                                isFree = false;
                                break;
                            }
                    if (isFree)
                        return new Coord(j, i);
                }
        return null;
    }

    public int calcFreeSpace()
    {
        int freespace = 0;
        short[][] inventory = containerMatrix();
        if(inventory == null)
            return -1;
        for (int i = 0; i < isz.y; i++)
            for (int j = 0; j < isz.x; j++)
                if (inventory[i][j] == 0)
                    freespace++;
        return freespace;
    }

    public int calcTotalSpace()
    {
        int totalSpace = 0;
        short[][] inventory = containerMatrix();
        if(inventory == null)
            return -1;
        for (int i = 0; i < isz.y; i++)
            for (int j = 0; j < isz.x; j++)
                if (inventory[i][j] != 2)
                    totalSpace++;
        return totalSpace;
    }

    public boolean isSlotFree(Coord pos)
    {
        short[][] inventory = containerMatrix();
        return inventory!=null && inventory[pos.y][pos.x] == 0;
    }

    public boolean isItemInSlot(Coord pos , NAlias name)
    {
        for (Widget widget = child; widget != null; widget = widget.next)
        {
            if (widget instanceof WItem)
            {
                WItem item = (WItem) widget;
                if(item.c.div(Inventory.sqsz).equals(pos))
                    return ((NGItem)item.item).name() != null && NParser.checkName(((NGItem)item.item).name(), name);
            }
        }
        return false;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("transfer-same")) {
            process(getSame((NGItem) args[0], (Boolean) args[1]), "transfer");
        }
        else
        {
            super.wdgmsg(sender, msg, args);
        }
    }

    private void process(List<NGItem> items, String action) {
        for (GItem item : items) {
            item.wdgmsg(action, Coord.z);
        }
    }

    private List<NGItem> getSame(NGItem item, Boolean ascending)
    {
        List<NGItem> items = new ArrayList<>();
        if (item != null && item.name() != null)
        {

            for (Widget wdg = lchild; wdg != null; wdg = wdg.prev)
            {
                if (wdg.visible && wdg instanceof NWItem)
                {
                    NWItem wItem = (NWItem) wdg;
                    if (item.isSearched)
                    {
                        if (((NGItem) wItem.item).isSearched)
                            items.add((NGItem) wItem.item);
                    }
                    else
                    {

                        if (NParser.checkName(item.name(), ((NGItem) wItem.item).name()))
                        {
                            items.add((NGItem) wItem.item);
                        }
                    }
                }
            }
            items.sort(ascending ? ITEM_COMPARATOR_ASC : ITEM_COMPARATOR_DESC);
        }
        return items;
    }

    public static final Comparator<NGItem> ITEM_COMPARATOR_ASC = new Comparator<NGItem>() {
        @Override
        public int compare(NGItem o1, NGItem o2) {

            if(o1.quality!=null && o2.quality!=null)
                return Double.compare(o1.quality, o2.quality);
            else
                return 1;
        }
    };
    public static final Comparator<NGItem> ITEM_COMPARATOR_DESC = new Comparator<NGItem>() {
        @Override
        public int compare(NGItem o1, NGItem o2) {
            return ITEM_COMPARATOR_ASC.compare(o2, o1);
        }
    };


    public <C extends ItemInfo> ArrayList<WItem> getItems(Class<C> c) throws InterruptedException
    {
        GetItemsWithInfo gi = new GetItemsWithInfo(this, c);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public ArrayList<ItemWatcher.ItemInfo> iis = new ArrayList<>();

    @Override
    public void reqdestroy() {
        if(parentGob!=null && parentGob.ngob.hash!=null)
        {
            if((Boolean)NConfig.get(NConfig.Key.ndbenable)) {
                ui.core.writeItemInfoForContainer(iis);
            }
        }
        super.reqdestroy();
    }

    public ItemStack findNotFullStack(String name) throws InterruptedException {
        GetNotFullStack gi = new GetNotFullStack(this, new NAlias(name));
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public WItem findNotStack(String name) throws InterruptedException {
        GetNotStack gi = new GetNotStack(this, new NAlias(name));
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

}
