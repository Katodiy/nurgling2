package nurgling;

import haven.*;
import haven.Window;
import haven.res.ui.tt.slot.Slotted;
import haven.res.ui.tt.stackn.Stack;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.*;
import nurgling.tools.*;
import nurgling.widgets.NPopupWidget;
import nurgling.widgets.NSearchWidget;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.List;

import static haven.TexI.glcm;

public class NInventory extends Inventory
{
    public NSearchWidget searchwdg;
    public NPopupWidget toggles;
    public ICheckBox bundle;
    public MenuGrid.PagButton pagBundle = null;
    boolean showPopup = false;
    BufferedImage numbers = null;
    short[][] oldinv = null;
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

    public Coord getFreeCoord(Coord sz) throws InterruptedException
    {
        GetFreePlace gfp = new GetFreePlace(this, sz);
        NUtils.getUI().core.addTask(gfp);
        return gfp.result();
    }
    public Coord getFreeCoord(GItem item) throws InterruptedException
    {
        GetFreePlace gfp = new GetFreePlace(this, item);
        NUtils.getUI().core.addTask(gfp);
        return gfp.result();
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

    public WItem getItem(NAlias name) throws InterruptedException
    {
        GetItem gi = new GetItem(this, name);
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

    public int getTotalAmountItems(NAlias name) throws InterruptedException
    {
        GetTotalAmountItems gi = new GetTotalAmountItems(this, name);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public ArrayList<WItem> getItems(NAlias name) throws InterruptedException
    {
        GetItems gi = new GetItems(this, name);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public ArrayList<WItem> getItems(NAlias name, double th) throws InterruptedException
    {
        GetItems gi = new GetItems(this, name, (float)th);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }

    public ArrayList<WItem> getItemsOfType(NAlias name) throws InterruptedException
    {
        GetItems gi = new GetItems(this, name);
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
        if(searchwdg!=null && searchwdg.history!=null) {
            searchwdg.history.move(new Coord(c.x  + ((Window)parent).ca().ul.x + UI.scale(7), c.y + parent.sz.y- UI.scale(37)));
        }
        super.mousemove(c);
    }

    @Override
    public void tick(double dt) {
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

        parent.pack();
        toggles = NUtils.getGameUI().add(new NPopupWidget(new Coord(UI.scale(50), UI.scale(80)), NPopupWidget.Type.RIGHT));


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
        movePopup(parent.c);
        toggles.pack();
    }

    private short[][] containerMatrix()
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

    public int calcNumberFreeCoord(Coord target_size)
    {
        int count = 0;
        short[][] inventory = containerMatrix();
        if(inventory == null)
            return -1;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if (inventory[i][j] == 0) {
                    if (i + target_size.x - 1 < isz.y && j + target_size.y - 1 < isz.x) {
                        boolean isFree = true;
                        for (int k = i; k < i + target_size.x; k++) {
                            for (int n = j; n < j + target_size.y; n++) {
                                if (inventory[k][n]!=0) {
                                    isFree = false;
                                    break;
                                }
                            }
                        }
                        if (isFree) {
                            count += 1;
                            for (int k = i; k < i + target_size.x; k++) {
                                for (int n = j; n < j + target_size.y; n++) {
                                    inventory[k][n] = 1;
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    public Coord findFreeCoord(WItem wItem)
    {
        return findFreeCoord(wItem.item.spr.sz().div(UI.scale(32)));
    }


    public Coord findFreeCoord(Coord target_size)
    {
        short[][] inventory = containerMatrix();
        if(inventory == null)
            return null;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if (inventory[i][j] == 0) {
                    if (i + target_size.x - 1 < isz.y && j + target_size.y - 1 < isz.x) {
                        boolean isFree = true;
                        for (int k = i; k < i + target_size.x; k++) {
                            for (int n = j; n < j + target_size.y; n++) {
                                if (inventory[k][n]!=0) {
                                    isFree = false;
                                    break;
                                }
                            }
                        }
                        if (isFree) {
                            return new Coord(j,i);
                        }
                    }
                }
            }
        }
        return null;
    }

    public int calcFreeSpace()
    {
        int freespace = 0;
        short[][] inventory = containerMatrix();
        if(inventory == null)
            return -1;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if (inventory[i][j] == 0)
                    freespace++;
            }
        }
        return freespace;
    }

    public int calcTotalSpace()
    {
        int totalSpace = 0;
        short[][] inventory = containerMatrix();
        if(inventory == null)
            return -1;
        for (int i = 0; i < isz.y; i++) {
            for (int j = 0; j < isz.x; j++) {
                if (inventory[i][j] != 2)
                    totalSpace++;
            }
        }
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


    public void activateItem(NAlias name) throws InterruptedException {
        WItem it = getItem(name);
        it.item.wdgmsg("iact", Coord.z, 1);
    }

    public <C extends ItemInfo> ArrayList<WItem> getItems(Class<C> c) throws InterruptedException
    {
        GetItemsWithInfo gi = new GetItemsWithInfo(this, c);
        NUtils.getUI().core.addTask(gi);
        return gi.getResult();
    }
}
