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
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class NInventory extends Inventory
{
    public NSearchWidget searchwdg;
    public NPopupWidget toggles;
    public NPopupWidget rightTogglesExpanded;
    public NPopupWidget rightTogglesCompact;
    public ICheckBox checkBoxForRight;
    public Scrollport itemListContainer;
    public Widget itemListContent;
    public Scrollport compactListContainer;
    public Widget compactListContent;
    public Dropbox<String> sortTypeDropbox;
    public Dropbox<String> orderDropbox;
    public ICheckBox bundle;
    public MenuGrid.PagButton pagBundle = null;
    boolean showPopup = false;
    boolean showRightPanel = false;
    RightPanelMode rightPanelMode = RightPanelMode.EXPANDED;
    boolean compactNameAscending = true;
    boolean compactQuantityAscending = false;
    String compactLastSortType = "quantity"; // Track which was clicked last
    short[][] oldinv = null;
    public Gob parentGob = null;
    long lastUpdate = 0;
    
    // Track pending item deletions for database sync
    public static class PendingDeletion {
        public final String itemHash;
        public final long deleteAtTick;
        public final String containerHash;
        
        public PendingDeletion(String itemHash, String containerHash, long deleteAtTick) {
            this.itemHash = itemHash;
            this.containerHash = containerHash;
            this.deleteAtTick = deleteAtTick;
        }
    }
    public final java.util.List<PendingDeletion> pendingDeletions = new java.util.ArrayList<>();
    private long pendingSearchRefreshTick = 0;
    
    // Pre-cached slot number textures for performance
    private static final int MAX_SLOT_NUMBERS = 200;
    private static final TexI[] cachedSlotNumbers = new TexI[MAX_SLOT_NUMBERS + 1];
    
    static {
        // Pre-render all slot numbers once at startup
        for (int i = 1; i <= MAX_SLOT_NUMBERS; i++) {
            cachedSlotNumbers[i] = new TexI(NStyle.slotnums.render(String.valueOf(i)).img);
        }
    }

    public NInventory(Coord sz)
    {
        super(sz);
    }

    @Override
    protected void added() {
        super.added();
        // Add Plan button for Study Desk after the widget is added to its parent
        nurgling.widgets.StudyDeskInventoryExtension.addPlanButtonIfStudyDesk(this);
    }

    public enum QualityType {
        High, Low
    }
    
    public enum RightPanelMode {
        COMPACT, EXPANDED
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        if((Boolean)NConfig.get(NConfig.Key.showInventoryNums) && oldinv != null) {
            drawSlotNumbers(g);
        }
    }
    
    // Optimized direct rendering without creating intermediate BufferedImage
    private void drawSlotNumbers(GOut g) {
        int counter = 1;
        Coord coord = new Coord(0, 0);
        for (coord.y = 0; coord.y < isz.y; coord.y++) {
            for (coord.x = 0; coord.x < isz.x; coord.x++) {
                // Check bounds to prevent ArrayIndexOutOfBoundsException
                if (coord.y >= oldinv.length || coord.x >= oldinv[coord.y].length) {
                    break;
                }
                if (oldinv[coord.y][coord.x] == 0 && counter <= MAX_SLOT_NUMBERS) {
                    TexI numTex = cachedSlotNumbers[counter];
                    Coord pos = coord.mul(sqsz).add(sqsz.div(2));
                    Coord sz = numTex.sz();
                    pos = pos.add((int)((double)sz.x * -0.5), (int)((double)sz.y * -0.5));
                    g.image(numTex, pos);
                }
                if (oldinv[coord.y][coord.x] != 2)
                    counter++;
            }
        }
    }


    // Simplified version - just updates inventory state, rendering happens in draw()
    void updateInventoryState(short[][] inventory) {
        oldinv = inventory.clone();
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
        if(searchwdg != null) {
            searchwdg.resize(new Coord(sz.x , 0));
            searchwdg.move(new Coord(0,sz.y + UI.scale(5)));
        }
        moveCheckbox();
        parent.pack();
        movePopup(parent.c);
        moveCheckboxAfterPack();
    }

    public void movePopup(Coord c) {
        if(toggles !=null)
        {
            toggles.move(new Coord(c.x - toggles.sz.x + toggles.atl.x +UI.scale(10),c.y + UI.scale(35)));
        }
        // Update both right panels
        updateRightPanelPositions(c);
        if(searchwdg!=null && searchwdg.history!=null) {
            searchwdg.history.move(new Coord(c.x  + ((Window)parent).ca().ul.x + UI.scale(7), c.y + parent.sz.y- UI.scale(37)));
        }
    }

    public void moveCheckbox() {
        if(checkBoxForRight != null) {
            // Since the button is positioned relative to sz.x, it should automatically 
            // adjust when the inventory resizes. Only reposition if needed.
            checkBoxForRight.c = new Coord(sz.x - UI.scale(40), 0);
        }
    }

    public void moveCheckboxAfterPack() {
        if(checkBoxForRight != null) {
            // Since the button is positioned relative to sz.x, it should automatically
            // adjust when the inventory resizes. Only reposition if needed.
            checkBoxForRight.c = new Coord(sz.x + UI.scale(4), UI.scale(27));
        }
    }
    
    
    private void updateRightPanelPositions(Coord c) {
        int invH = this.sz.y;
        int insetY = UI.scale(8); // Default inset
        int desiredInner = Math.round(invH * 1.2f);
        int outerH = desiredInner;
        int panelW = UI.scale(250);
        int compactPanelW = UI.scale(100); // Smaller width for compact mode
        int compactOuterH = Math.round(invH * 1.2f); // Taller than expanded but still smaller than inventory
        
        if (rightTogglesExpanded != null) {
            rightTogglesExpanded.move(new Coord(
                    c.x + parent.sz.x - rightTogglesExpanded.atl.x - UI.scale(6),
                    c.y + UI.scale(20)
            ));
            rightTogglesExpanded.resize(panelW, outerH);
            
            if (itemListContainer != null) {
                // Resize expanded list container
                int insetX = rightTogglesExpanded.atl.x;
                insetY = rightTogglesExpanded.atl.y;
                int contentLeft = insetX;
                int contentRight = rightTogglesExpanded.sz.x - insetX;
                int contentBottom = rightTogglesExpanded.sz.y - insetY;
                int listTopY = itemListContainer.c.y;
                int sidePad = UI.scale(12);
                int bottomPad = UI.scale(8);
                
                int listWidth = Math.max(0, (contentRight - contentLeft) - sidePad * 2);
                int listHeight = Math.max(0, contentBottom - listTopY - bottomPad);
                
                itemListContainer.resize(new Coord(listWidth, listHeight));
                rebuildItemList();
            }
        }
        
        if (rightTogglesCompact != null) {
            rightTogglesCompact.move(new Coord(
                    c.x + parent.sz.x - rightTogglesCompact.atl.x - UI.scale(6),
                    c.y + UI.scale(20)
            ));
            rightTogglesCompact.resize(compactPanelW, compactOuterH);
            
            if (compactListContainer != null) {
                // Resize compact list container  
                int insetX = rightTogglesCompact.atl.x;
                insetY = rightTogglesCompact.atl.y;
                int contentLeft = insetX;
                int contentRight = rightTogglesCompact.sz.x - insetX;
                int contentBottom = rightTogglesCompact.sz.y - insetY;
                int listTopY = compactListContainer.c.y;
                int sidePad = UI.scale(4);
                int bottomPad = UI.scale(8);
                
                int listWidth = Math.max(0, (contentRight - contentLeft) - sidePad * 2);
                int listHeight = Math.max(0, contentBottom - listTopY - bottomPad);
                
                compactListContainer.resize(new Coord(listWidth, listHeight));
                rebuildCompactList();
            }
        }
    }
    
    private void updateRightPanelVisibility() {
        if (rightTogglesExpanded != null) {
            if (showRightPanel && rightPanelMode == RightPanelMode.EXPANDED) {
                rightTogglesExpanded.show();
            } else {
                rightTogglesExpanded.hide();
            }
        }
        
        if (rightTogglesCompact != null) {
            if (showRightPanel && rightPanelMode == RightPanelMode.COMPACT) {
                rightTogglesCompact.show();
                rebuildCompactList();
            } else {
                rightTogglesCompact.hide();
            }
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
        
        // Process pending deletions - items deleted while inventory stays open
        if (!pendingDeletions.isEmpty() && (Boolean) NConfig.get(NConfig.Key.ndbenable) && 
            ui != null && ui.core != null && ui.core.databaseManager != null && ui.core.databaseManager.isReady()) {
            long currentTick = NUtils.getTickId();
            java.util.Iterator<PendingDeletion> it = pendingDeletions.iterator();
            int deletedCount = 0;
            while (it.hasNext()) {
                PendingDeletion pd = it.next();
                if (currentTick >= pd.deleteAtTick) {
                    // Delete item from database async
                    ui.core.databaseManager.getStorageItemService().deleteStorageItemAsync(pd.itemHash);
                    it.remove();
                    deletedCount++;
                }
            }
            // Schedule search refresh after a short delay to allow DB operations to complete
            if (deletedCount > 0) {
                pendingSearchRefreshTick = currentTick + 5; // Refresh search in 5 ticks
            }
        }
        
        // Handle pending search refresh
        if (pendingSearchRefreshTick > 0 && NUtils.getTickId() >= pendingSearchRefreshTick) {
            pendingSearchRefreshTick = 0;
            if (NUtils.getGameUI() != null && NUtils.getGameUI().itemsForSearch != null) {
                NUtils.getGameUI().itemsForSearch.refreshSearch();
            }
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
                    }
                } else {
                    isDiffrent = true;
                }
            if (isDiffrent)
                updateInventoryState(newInv);
        }
        else
            oldinv = null;
        if(toggles !=null)
            toggles.visible = parent.visible && showPopup;
        if(rightTogglesExpanded != null) {
            rightTogglesExpanded.visible = parent.visible && showRightPanel && (rightPanelMode == RightPanelMode.EXPANDED);
            if (showRightPanel && rightPanelMode == RightPanelMode.EXPANDED) {
                // Update expanded panel contents periodically
                if (NUtils.getTickId() % 10 == 0) { // Update every 10 ticks
                    updateRightPanelItems();
                }
            }
        }
        if(rightTogglesCompact != null) {
            rightTogglesCompact.visible = parent.visible && showRightPanel && (rightPanelMode == RightPanelMode.COMPACT);
            if (showRightPanel && rightPanelMode == RightPanelMode.COMPACT) {
                // Update compact panel contents periodically
                if (NUtils.getTickId() % 20 == 0) { // Update every 20 ticks (less frequent)
                    rebuildCompactList();
                }
            }
        }
    }

    private static final TexI[] collapsei = new TexI[]{
            new TexI(Resource.loadsimg("nurgling/hud/buttons/itogglec/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/itogglec/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/itogglec/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/itogglec/dh"))};

    // Mirrored versions for right-side toggle
    private static final TexI[] collapseiRight = createMirroredTextures(collapsei);
    
    // Helper method to create horizontally mirrored textures
    private static TexI[] createMirroredTextures(TexI[] original) {
        TexI[] mirrored = new TexI[original.length];
        for (int i = 0; i < original.length; i++) {
            BufferedImage img = original[i].back;

            // Use ARGB format to ensure compatibility
            int imageType = img.getType();
            if (imageType == 0) {
                imageType = BufferedImage.TYPE_INT_ARGB;
            }

            BufferedImage flippedImg = new BufferedImage(img.getWidth(), img.getHeight(), imageType);

            // Create mirrored image using Graphics2D for better handling
            java.awt.Graphics2D g2d = flippedImg.createGraphics();
            g2d.drawImage(img, img.getWidth(), 0, 0, img.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
            g2d.dispose();

            mirrored[i] = new TexI(flippedImg);
        }
        return mirrored;
    }

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


        checkBoxForRight = new ICheckBox(collapseiRight[0], collapseiRight[1], collapseiRight[2], collapseiRight[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                showRightPanel = val;
                NConfig.set(NConfig.Key.inventoryRightPanelShow, val);
                updateRightPanelVisibility();
            }
        };

        parent.pack();

        // Right panel toggle button - using mirrored textures
        parent.add(checkBoxForRight, new Coord(sz.x + UI.scale(4), UI.scale(27)));

        toggles = NUtils.getGameUI().add(new NPopupWidget(new Coord(UI.scale(50), UI.scale(80)), NPopupWidget.Type.RIGHT));
        
        // Create expanded panel
        int panelW = UI.scale(250);
        rightTogglesExpanded = NUtils.getGameUI().add(
                new NPopupWidget(new Coord(panelW, UI.scale(100)), NPopupWidget.Type.LEFT)
        );
        int insetY = rightTogglesExpanded.atl.y;
        int desiredInner = Math.round(this.sz.y * 0.80f);
        int outerH = desiredInner + insetY * 2;
        rightTogglesExpanded.resize(panelW, outerH);
        
        // Create compact panel (taller than expanded)
        int compactPanelW = UI.scale(120);
        int compactOuterH = Math.round(this.sz.y * 0.90f) + rightTogglesExpanded.atl.y * 2;
        rightTogglesCompact = NUtils.getGameUI().add(
                new NPopupWidget(new Coord(compactPanelW, compactOuterH), NPopupWidget.Type.LEFT)
        );

        Widget pw = toggles.add(new ICheckBox(gildingi[0], gildingi[1], gildingi[2], gildingi[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                Slotted.show = val;
            }
        }, toggles.atl);
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/gilding/u").flayer(Resource.tooltip).text());
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
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/var/u").flayer(Resource.tooltip).text());
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
        rpw.settip(Resource.remote().loadwait("nurgling/hud/buttons/numbering/u").flayer(Resource.tooltip).text());
        ((ICheckBox)rpw).a = (Boolean)NConfig.get(NConfig.Key.showInventoryNums);

        pw = toggles.add(new ICheckBox(stacki[0], stacki[1], stacki[2], stacki[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                Stack.show = val;
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        ((ICheckBox)pw).a = Stack.show;
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/stack/u").flayer(Resource.tooltip).text());

        bundle = toggles.add(new ICheckBox(bundlei[0], bundlei[1], bundlei[2], bundlei[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                pagBundle.use(new MenuGrid.Interaction(1, 0));
            }
        }, pw.pos("ur").add(UI.scale(new Coord(5, 0))));
        bundle.settip(Resource.remote().loadwait("nurgling/hud/buttons/bundle/u").flayer(Resource.tooltip).text());

        pw = toggles.add(new ICheckBox(autoflower[0], autoflower[1], autoflower[2], autoflower[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                NConfig.set(NConfig.Key.autoFlower, val);
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/autoflower/u").flayer(Resource.tooltip).text());
        ((ICheckBox)pw).a = (Boolean)NConfig.get(NConfig.Key.autoFlower);
        pw = toggles.add(new ICheckBox(autosplittor[0], autosplittor[1], autosplittor[2], autosplittor[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                NConfig.set(NConfig.Key.autoSplitter, val);
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/autosplittor/u").flayer(Resource.tooltip).text());
        ((ICheckBox)pw).a = (Boolean)NConfig.get(NConfig.Key.autoSplitter);

        pw = toggles.add(new ICheckBox(dropperi[0], dropperi[1], dropperi[2], dropperi[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                NConfig.set(NConfig.Key.autoDropper, val);
            }
        }, pw.pos("bl").add(UI.scale(new Coord(0, 5))));
        pw.settip(Resource.remote().loadwait("nurgling/hud/buttons/dropper/u").flayer(Resource.tooltip).text());
        ((ICheckBox)pw).a = (Boolean)NConfig.get(NConfig.Key.autoDropper);

        toggles.pack();

        // Setup both right panels
        setupExpandedPanel();
        setupCompactPanel();

        // Load settings from NConfig
        Boolean showPanelConfig = (Boolean) NConfig.get(NConfig.Key.inventoryRightPanelShow);
        showRightPanel = showPanelConfig != null ? showPanelConfig : false;
        
        String panelModeStr = (String) NConfig.get(NConfig.Key.inventoryRightPanelMode);
        if ("COMPACT".equals(panelModeStr)) {
            rightPanelMode = RightPanelMode.COMPACT;
        } else {
            rightPanelMode = RightPanelMode.EXPANDED;
        }
        
        checkBoxForRight.a = showRightPanel;
        updateRightPanelVisibility();

        movePopup(parent.c);
        toggles.pack();
    }

    private void setupExpandedPanel() {
        int panelMargin = UI.scale(8);
        Coord headerPos = rightTogglesExpanded.atl.add(new Coord(panelMargin, panelMargin));

        // View toggle button in top-right of expanded panel  
        ICheckBox viewToggle = new ICheckBox(
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lsearch/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lsearch/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lsearch/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lsearch/dh"))
        ) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                rightPanelMode = RightPanelMode.COMPACT;
                NConfig.set(NConfig.Key.inventoryRightPanelMode, "COMPACT");
                updateRightPanelVisibility();
            }
        };
        viewToggle.a = false; // Start in expanded mode
        viewToggle.settip("Switch to compact view");
        rightTogglesExpanded.add(viewToggle, new Coord(rightTogglesExpanded.sz.x - UI.scale(50), headerPos.y));

        // Position for dropdowns - below header
        Coord dropdownPos = headerPos.add(new Coord(8, 0));
        
        // Sort type dropdown (smaller, cleaner)
        sortTypeDropbox = new Dropbox<String>(UI.scale(85), 4, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                String[] options = {"Count", "Name", "Resource", "Quality"};
                return options[i];
            }
            
            @Override
            protected int listitems() { return 4; }
            
            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, new Coord(3, 2));
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                applySorting();
            }
        };
        sortTypeDropbox.change("Count");
        rightTogglesExpanded.add(sortTypeDropbox, dropdownPos);
        
        // Order dropdown (smaller, right aligned)
        orderDropbox = new Dropbox<String>(UI.scale(60), 2, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                String[] options = {"Asc", "Desc"};
                return options[i];
            }
            
            @Override
            protected int listitems() { return 2; }
            
            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, new Coord(3, 2));
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                applySorting();
            }
        };
        orderDropbox.change("Asc");  // Default to descending
        rightTogglesExpanded.add(orderDropbox, dropdownPos.add(new Coord(UI.scale(90), 0)));

        
        // Create Scrollport for item list - following CheeseOrdersPanel pattern
        Coord listPos = dropdownPos.add(new Coord(0, UI.scale(25)));
        int listWidth = UI.scale(220);
        int listHeight = UI.scale(150);
        
        itemListContainer = rightTogglesExpanded.add(new Scrollport(new Coord(listWidth, listHeight)), listPos);
        itemListContent = new Widget(new Coord(listWidth, UI.scale(50))) {
            @Override
            public void pack() {
                // Auto-resize based on children
                resize(contentsz());
            }
        };
        itemListContainer.cont.add(itemListContent, Coord.z);
        
        // Initial population of items
        rebuildItemList();
    }
    
    private void setupCompactPanel() {
        int panelMargin = UI.scale(4);
        Coord headerPos = rightTogglesCompact.atl.add(new Coord(panelMargin, panelMargin));
        
        // View toggle button in top-right of compact panel
        ICheckBox viewToggle = new ICheckBox(
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lsearch/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lsearch/d")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lsearch/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/lsearch/dh"))
        ) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                rightPanelMode = RightPanelMode.EXPANDED;
                NConfig.set(NConfig.Key.inventoryRightPanelMode, "EXPANDED");
                updateRightPanelVisibility();
            }
        };
        viewToggle.a = false; // Will switch to expanded mode
        viewToggle.settip("Switch to expanded view");
        rightTogglesCompact.add(viewToggle, new Coord(rightTogglesCompact.sz.x - UI.scale(40), headerPos.y));
        
        // Sorting buttons for compact mode
        // Name sort button (above icons area)
        ICheckBox nameSortButton = new ICheckBox(
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v2/UP/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v2/DOWN/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v2/UP/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v2/DOWN/h"))
        ) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                compactNameAscending = !val; // false = ascending, true = descending
                compactLastSortType = "name"; // Mark name as last clicked
                rebuildCompactList();
            }
        };
        nameSortButton.a = false; // Start with ascending (up arrow)
        nameSortButton.settip("Sort by name (ascending/descending)");
        rightTogglesCompact.add(nameSortButton, new Coord(headerPos.x + UI.scale(3), headerPos.y));
        
        // Quantity sort button (above quantities area)  
        ICheckBox quantitySortButton = new ICheckBox(
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v2/UP/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v2/DOWN/u")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v2/UP/h")),
            new TexI(Resource.loadsimg("nurgling/hud/buttons/arrows/v2/DOWN/h"))
        ) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                compactQuantityAscending = !val; // false = ascending, true = descending
                compactLastSortType = "quantity"; // Mark quantity as last clicked
                rebuildCompactList();
            }
        };
        quantitySortButton.a = true; // Start with descending (down arrow) for quantities
        quantitySortButton.settip("Sort by quantity (ascending/descending)");
        rightTogglesCompact.add(quantitySortButton, new Coord(headerPos.x + UI.scale(20), headerPos.y));

        // Create compact Scrollport for item list (below the sorting buttons)
        Coord listPos = headerPos.add(new Coord(0, UI.scale(25)));
        int listWidth = UI.scale(90);
        int listHeight = UI.scale(180);
        
        compactListContainer = rightTogglesCompact.add(new Scrollport(new Coord(listWidth, listHeight)), listPos);
        compactListContent = new Widget(new Coord(listWidth, UI.scale(50))) {
            @Override
            public void pack() {
                resize(contentsz());
            }
        };
        compactListContainer.cont.add(compactListContent, Coord.z);
        
        // Initial population
        rebuildCompactList();
    }

    private void applySorting() {
        // Trigger re-population of items with current sort settings
        rebuildItemList();
    }

    private void updateRightPanelItems() {
        rebuildItemList();
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
                // Get proper stack count using Amount info like GetTotalAmountItems does
                int stackSize = 1;
                try {
                    GItem.Amount amount = item.getInfo(GItem.Amount.class);
                    if (amount != null && amount.itemnum() > 0) {
                        stackSize = amount.itemnum();
                    }
                } catch (Exception e) {
                    stackSize = 1;
                }

                totalQuantity += stackSize;

                // Calculate quality - try to get stack quality first, then fallback to item quality
                double itemQuality = 0;
                if(stackSize > 1) {
                    // Try to get stack quality info for stacked items
                    Stack stackInfo = item.getInfo(Stack.class);
                    if (stackInfo != null && stackInfo.quality > 0) {
                        itemQuality = stackInfo.quality;
                    } else if (item.quality != null && item.quality > 0) {
                        // Fallback to individual item quality if no stack quality
                        itemQuality = item.quality;
                    }
                } else {
                    // Fallback to individual item quality on any error
                    try {
                        if (item.quality != null && item.quality > 0) {
                            itemQuality = item.quality;
                        }
                    } catch (Exception e2) {
                        // Ignore and continue with 0 quality
                        itemQuality = 0;
                    }
                }

                
                if (itemQuality > 0) {
                    // Weight quality by stack size for accurate average
                    totalQuality += itemQuality * stackSize;
                    qualityCount += stackSize;
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
    
    private void rebuildItemList() {
        if (itemListContent == null) return;
        
        // Clear existing widgets from content
        for (Widget child : new ArrayList<>(itemListContent.children())) {
            child.destroy();
        }
        
        // Get current inventory items and group by name
        Map<String, ItemGroup> itemGroupMap = new HashMap<>();
        
        // Access parent inventory's children
        for (Widget widget = this.child; widget != null; widget = widget.next) {
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
        List<ItemGroup> itemGroups = new ArrayList<>(itemGroupMap.values());
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
                return -result; // Descending
            }
        });
        
        // Create widgets for expanded mode (original list layout)
        int y = 0;
        int contentWidth = itemListContainer.cont.sz.x;
        int itemHeight = UI.scale(20);
        
        for (ItemGroup group : itemGroups) {
            Widget itemWidget = createItemWidget(group, new Coord(contentWidth, itemHeight));
            itemListContent.add(itemWidget, new Coord(0, y));
            y += itemHeight + UI.scale(1);
        }
        
        // Let the content widget auto-resize and update scrollbar
        itemListContent.pack();
        itemListContainer.cont.update();
    }
    
    private void rebuildCompactList() {
        if (compactListContent == null) return;
        
        // Clear existing widgets from content
        for (Widget child : new ArrayList<>(compactListContent.children())) {
            child.destroy();
        }
        
        // Get current inventory items and group by name (same logic as expanded)
        Map<String, ItemGroup> itemGroupMap = new HashMap<>();
        
        for (Widget widget = this.child; widget != null; widget = widget.next) {
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
        
        // Sort items - whatever was clicked last becomes the primary sort
        List<ItemGroup> itemGroups = new ArrayList<>(itemGroupMap.values());
        itemGroups.sort((a, b) -> {
            if ("name".equals(compactLastSortType)) {
                // Name is primary sort
                int nameResult = a.name.compareTo(b.name);
                if (!compactNameAscending) nameResult = -nameResult;
                
                // Secondary sort by quantity for ties
                if (nameResult == 0) {
                    int quantityResult = Integer.compare(a.totalQuantity, b.totalQuantity);
                    return compactQuantityAscending ? quantityResult : -quantityResult;
                }
                return nameResult;
            } else {
                // Quantity is primary sort (default)
                int quantityResult = Integer.compare(a.totalQuantity, b.totalQuantity);
                if (!compactQuantityAscending) quantityResult = -quantityResult;
                
                // Secondary sort by name for ties
                if (quantityResult == 0) {
                    int nameResult = a.name.compareTo(b.name);
                    return compactNameAscending ? nameResult : -nameResult;
                }
                return quantityResult;
            }
        });
        
        // Create compact list layout - one line per item
        int y = 0;
        int contentWidth = compactListContainer.cont.sz.x;
        int itemHeight = UI.scale(18); // Single line height
        
        for (ItemGroup group : itemGroups) {
            Widget compactWidget = createCompactItemWidget(group, new Coord(contentWidth, itemHeight));
            compactListContent.add(compactWidget, new Coord(0, y));
            y += itemHeight + UI.scale(1);
        }
        
        // Let the content widget auto-resize and update scrollbar
        compactListContent.pack();
        compactListContainer.cont.update();
    }
    
    private Widget createItemWidget(ItemGroup group, Coord sz) {
        return new Widget(sz) {
            @Override
            public void draw(GOut g) {
                int iconSize = UI.scale(19);
                int margin = UI.scale(1);
                int textY = UI.scale(2);
                
                // Draw item icon
                NGItem representativeItem = group.getRepresentativeItem();
                if (representativeItem != null) {
                    Coord iconPos = new Coord(margin, margin);
                    
                    try {
                        Resource.Image img = representativeItem.getres().layer(Resource.imgc);
                        if (img != null) {
                            g.image(img.tex(), iconPos, new Coord(iconSize, iconSize));
                        } else {
                            // Fallback: draw colored placeholder
                            g.chcolor(100, 150, 100, 200);
                            g.frect(iconPos, new Coord(iconSize, iconSize));
                            g.chcolor();
                        }
                    } catch (Exception e) {
                        // Fallback: draw colored placeholder
                        g.chcolor(100, 150, 100, 200);
                        g.frect(iconPos, new Coord(iconSize, iconSize));
                        g.chcolor();
                    }
                }
                
                // Calculate text positions
                int textStartX = margin + iconSize + UI.scale(4);
                int quantityWidth = UI.scale(25);
                int nameStartX = textStartX + quantityWidth;
                int qualityX = sz.x - UI.scale(35);
                
                // Draw quantity with "x" prefix
                String quantityText = "x" + group.totalQuantity;
                g.text(quantityText, new Coord(textStartX, textY));
                
                // Draw item name
                g.text(group.name, new Coord(nameStartX, textY));
                
                // Draw quality if available
                if (group.averageQuality > 0) {
                    // Draw blue quality dot
                    try {
                        Tex qualityIcon = new TexI(Resource.remote().loadwait("ui/tt/q/quality").layer(Resource.imgc, 0).scaled());
                        int dotSize = UI.scale(12);
                        int dotX = qualityX - dotSize - UI.scale(2);
                        g.image(qualityIcon, new Coord(dotX, textY), new Coord(dotSize, dotSize));
                    } catch (Exception e) {
                        // Ignore if icon fails to load
                    }
                    
                    // Draw quality value
                    String qualityText = String.format("%.1f", group.averageQuality);
                    g.text(qualityText, new Coord(qualityX, textY));
                }
            }
        };
    }
    
    private Widget createCompactItemWidget(ItemGroup group, Coord sz) {
        return new Widget(sz) {
            @Override
            public void draw(GOut g) {
                int iconSize = UI.scale(16);
                int margin = UI.scale(1);
                int textY = UI.scale(2);
                
                // Draw item icon
                NGItem representativeItem = group.getRepresentativeItem();
                if (representativeItem != null) {
                    Coord iconPos = new Coord(margin, margin);
                    
                    try {
                        Resource.Image img = representativeItem.getres().layer(Resource.imgc);
                        if (img != null) {
                            g.image(img.tex(), iconPos, new Coord(iconSize, iconSize));
                        } else {
                            // Fallback: draw colored placeholder
                            g.chcolor(100, 150, 100, 200);
                            g.frect(iconPos, new Coord(iconSize, iconSize));
                            g.chcolor();
                        }
                    } catch (Exception e) {
                        // Fallback: draw colored placeholder
                        g.chcolor(100, 150, 100, 200);
                        g.frect(iconPos, new Coord(iconSize, iconSize));
                        g.chcolor();
                    }
                }
                
                // Draw just the quantity next to the icon
                int textStartX = margin + iconSize + UI.scale(4);
                String quantityText = "x" + group.totalQuantity;
                g.text(quantityText, new Coord(textStartX, textY));
            }
            
            @Override
            public Object tooltip(Coord c, Widget prev) {
                // Show item name as tooltip when hovering over the icon area
                int iconSize = UI.scale(16);
                int margin = UI.scale(1);
                Coord iconArea = new Coord(margin + iconSize, iconSize + margin * 2);
                
                if (c.isect(new Coord(margin, margin), iconArea)) {
                    return group.name;
                }
                return super.tooltip(c, prev);
            }
        };
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
                if (item.item.spr != null)
                {
                    Coord size = item.item.spr.sz().div(UI.scale(32));
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
        else if (msg.equals("drop-same")) {
            process(getSame((NGItem) args[0], (Boolean) args[1]), "drop");
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
            // Only collect direct children of inventory, don't expand stacks
            // (expanding stacks would break them apart during transfer)
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

    /**
     * Gets the effective quality of an item, considering stack quality for stacked items
     */
    private static double getEffectiveQuality(NGItem item) {
        // First try to get stack quality (for stacked items)
        Stack stackInfo = item.getInfo(Stack.class);
        if (stackInfo != null && stackInfo.quality > 0) {
            return stackInfo.quality;
        }
        // Fall back to individual item quality
        if (item.quality != null && item.quality > 0) {
            return item.quality;
        }
        return -1; // No quality available
    }
    
    public static final Comparator<NGItem> ITEM_COMPARATOR_ASC = new Comparator<NGItem>() {
        @Override
        public int compare(NGItem o1, NGItem o2) {
            double q1 = getEffectiveQuality(o1);
            double q2 = getEffectiveQuality(o2);
            // Items with no quality (-1) go to the end
            if (q1 < 0 && q2 < 0) return 0;
            if (q1 < 0) return 1;  // no quality goes to the end
            if (q2 < 0) return -1; // no quality goes to the end
            return Double.compare(q1, q2);
        }
    };
    public static final Comparator<NGItem> ITEM_COMPARATOR_DESC = new Comparator<NGItem>() {
        @Override
        public int compare(NGItem o1, NGItem o2) {
            double q1 = getEffectiveQuality(o1);
            double q2 = getEffectiveQuality(o2);
            // Items with no quality (-1) go to the end
            if (q1 < 0 && q2 < 0) return 0;
            if (q1 < 0) return 1;  // no quality goes to the end
            if (q2 < 0) return -1; // no quality goes to the end
            return Double.compare(q2, q1);
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
        // Clear pending deletions - the ItemWatcher will handle bulk update on close
        pendingDeletions.clear();
        
        if(parentGob!=null && parentGob.ngob.hash!=null)
        {
            if((Boolean)NConfig.get(NConfig.Key.ndbenable)) {
                ui.core.writeItemInfoForContainer(iis);
            }
        }

        // Close Study Desk Planner if this is a study desk inventory
        if (nurgling.widgets.StudyDeskInventoryExtension.isStudyDeskInventory(this)) {
            NGameUI gameUI = NUtils.getGameUI();
            if (gameUI != null && gameUI.studyDeskPlanner != null && gameUI.studyDeskPlanner.visible()) {
                gameUI.studyDeskPlanner.hide();
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
