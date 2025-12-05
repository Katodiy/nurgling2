package nurgling.widgets;

import haven.*;
import nurgling.*;

import static haven.Inventory.invsq;

/**
 * Proxy widget to display belt inventory in a horizontal row on screen
 */
public class NBeltProxy extends Widget implements DTarget {
    private Inventory beltInventory = null;
    private static final int MAX_SLOTS = 25; // Maximum belt size (5x5)
    
    public NBeltProxy() {
        super(invsz(new Coord(MAX_SLOTS, 1)));
    }
    
    public void setBeltInventory(Inventory inv) {
        this.beltInventory = inv;
        if (inv != null) {
            // Resize based on actual belt size
            int totalSlots = inv.isz.x * inv.isz.y;
            resize(invsz(new Coord(totalSlots, 1)));
        }
    }
    
    public Inventory getBeltInventory() {
        // Try to find belt inventory window
        if (beltInventory == null || beltInventory.parent == null) {
            beltInventory = findBeltInventory();
            if(beltInventory!=null)
            {
                int totalSlots = beltInventory.isz.x * beltInventory.isz.y;
                resize(invsz(new Coord(totalSlots, 1)));
            }
        }
        return beltInventory;
    }
    
    private Inventory findBeltInventory() {
        // Search for window with "Belt" in caption
        GameUI gui = NUtils.getGameUI();
        if (gui != null) {
            for (Widget w = gui.lchild; w != null; w = w.prev) {
                if (w instanceof Window) {
                    Window wnd = (Window) w;
                    if (wnd.cap != null && wnd.cap.toLowerCase().contains("belt")) {
                        // Found belt window, find inventory inside
                        for (Widget child = wnd.child; child != null; child = child.next) {
                            if (child instanceof Inventory) {
                                return (Inventory) child;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void resize(Coord sz)
    {
        NDraggableWidget drg = (NDraggableWidget)parent;
        drg.sz = sz.add(NDraggableWidget.delta);
        drg.btnLock.move(new Coord(drg.sz.x - NStyle.locki[0].sz().x - NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y / 2));
        drg.btnVis.move(new Coord(drg.sz.x - NStyle.locki[0].sz().x - NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y + NDraggableWidget.off.y));
        super.resize(sz);
    }

    private Coord slotCoord(int slotIndex) {
        return new Coord(slotIndex, 0);
    }
    
    private int coordToSlot(Coord c) {
        Coord slot = sqroff(c);
        return slot.x;
    }
    
    @Override
    public void draw(GOut g) {
        Inventory belt = getBeltInventory();
        if (belt != null) {
            int totalSlots = belt.isz.x * belt.isz.y;
            
            // Draw slots
            for (int i = 0; i < totalSlots; i++) {
                Coord slotPos = sqoff(slotCoord(i));
                g.image(invsq, slotPos);
            }
            
            // Draw items
            for (Widget w = belt.child; w != null; w = w.next) {
                if (w instanceof WItem) {
                    WItem witem = (WItem) w;
                    // Calculate original grid position
                    Coord gridPos = witem.c.sub(1, 1).div(Inventory.sqsz);
                    // Convert to horizontal index
                    int slotIndex = gridPos.y * belt.isz.x + gridPos.x;
                    Coord drawPos = sqoff(slotCoord(slotIndex)).add(1, 1);
                    
                    // Draw the item at horizontal position
                    witem.draw(g.reclipl(drawPos, invsq.sz()));
                }
            }
        }
    }
    
    @Override
    public boolean mousedown(MouseDownEvent ev) {
        Inventory belt = getBeltInventory();
        if (belt != null) {
            int slot = coordToSlot(ev.c);
            if (slot >= 0 && slot < belt.isz.x * belt.isz.y) {
                // Find item at this slot
                WItem item = getItemAtSlot(belt, slot);
                if (item != null) {
                    // Calculate offset within the slot and create adjusted event
                    Coord slotPos = sqoff(slotCoord(slot)).add(1, 1);
                    Coord itemOffset = ev.c.sub(slotPos);
                    MouseDownEvent adjustedEv = new MouseDownEvent(ev, itemOffset);
                    item.mousedown(adjustedEv);
                    return true;
                }
            }
        }
        return super.mousedown(ev);
    }
    
    private WItem getItemAtSlot(Inventory inv, int slotIndex) {
        int gridX = slotIndex % inv.isz.x;
        int gridY = slotIndex / inv.isz.x;
        Coord gridPos = new Coord(gridX, gridY);
        
        for (Widget w = inv.child; w != null; w = w.next) {
            if (w instanceof WItem) {
                WItem witem = (WItem) w;
                Coord itemGridPos = witem.c.sub(1, 1).div(Inventory.sqsz);
                if (itemGridPos.equals(gridPos)) {
                    return witem;
                }
            }
        }
        return null;
    }
    
    @Override
    public Object tooltip(Coord c, Widget prev) {
        Inventory belt = getBeltInventory();
        if (belt != null) {
            int slot = coordToSlot(c);
            WItem item = getItemAtSlot(belt, slot);
            if (item != null) {
                return item.tooltip(c, (prev == this) ? item : prev);
            }
        }
        return super.tooltip(c, prev);
    }
    
    @Override
    public boolean drop(Coord cc, Coord ul) {
        Inventory belt = getBeltInventory();
        if (belt != null) {
            int slot = coordToSlot(cc);
            int gridX = slot % belt.isz.x;
            int gridY = slot / belt.isz.x;
            belt.wdgmsg("drop", new Coord(gridX, gridY));
            return true;
        }
        return false;
    }
    
    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        Inventory belt = getBeltInventory();
        if (belt != null) {
            int slot = coordToSlot(cc);
            WItem item = getItemAtSlot(belt, slot);
            if (item != null) {
                return item.iteminteract(cc, ul);
            }
        }
        return false;
    }
    
    public static Coord sqoff(Coord c) {
        return c.mul(invsq.sz());
    }
    
    public static Coord sqroff(Coord c) {
        return c.div(invsq.sz());
    }
    
    public static Coord invsz(Coord sz) {
        return Inventory.sqsz.add(2, 1).mul(sz);
    }
}
