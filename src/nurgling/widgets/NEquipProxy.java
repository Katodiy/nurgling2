package nurgling.widgets;


import haven.*;

import java.awt.*;

import static haven.Equipory.ebgs;
import static haven.Equipory.etts;
import static haven.Inventory.invsq;
import nurgling.*;


public class NEquipProxy extends Widget implements DTarget {
    NEquipory.Slots []slots;
    public NEquipProxy(NEquipory.Slots...slots) {
        super(new Coord(UI.scale(2)+NInventory.sqsz.x*3, NInventory.sqsz.y+UI.scale(2) ));
        setSlots(slots);
    }

    public void setSlots(NEquipory.Slots...slots) {
        this.slots = slots;
        sz = invsz(new Coord(slots.length, 1));
    }

    private NEquipory.Slots slot(Coord c) {
        int slot = sqroff(c).x;
        if(slot < 0) {slot = 0;}
        if(slot >= slots.length) {slot = slots.length - 1;}
        return slots[slot];
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        Equipory e = NUtils.getEquipment();
        if(e != null) {
            WItem w = NUtils.getEquipment().quickslots[slot(ev.c).idx];
            if(w != null) {
                w.mousedown(ev);
                return true;
            }
        }
        return super.mousedown(ev);
    }


    @Override
    public void draw(GOut g) {
        Equipory equipory = NUtils.getEquipment();
        if(equipory != null) {
            int k = 0;
            Coord c0 = new Coord(0, 0);
            for (NEquipory.Slots slot : slots) {
                c0.x = k;
                Coord c1 = sqoff(c0);
                g.image(invsq, c1);
                WItem w = NUtils.getEquipment().quickslots[slot.idx];
                if(w != null) {
                    w.draw(g.reclipl(c1, invsq.sz()));
                } else if(ebgs[slot.idx] != null) {
                    g.image(ebgs[slot.idx], c1);
                }
                k++;
            }
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        Equipory e = NUtils.getEquipment();
        if(e != null) {
            NEquipory.Slots slot = slot(c);
            WItem w = NUtils.getEquipment().quickslots[slot.idx];
            if(w != null) {
                return w.tooltip(c, (prev == this) ? w : prev);
            } else {
                return etts[slot.idx];
            }
        }
        return super.tooltip(c, prev);
    }

    @Override
    public boolean mousehover(MouseHoverEvent ev, boolean hovering) {
        boolean ret =  super.mousehover(ev, hovering);
        NEquipory.Slots slot = slot(ev.c);
        if(NUtils.getEquipment()!=null) {
            WItem w = NUtils.getEquipment().quickslots[slot.idx];
            if (w!=null && hovering && (w.item.contents != null)) {
                w.item.hovering(this);
                return (true);
            }
        }
        return(ret);
    }

    @Override
    public boolean drop(Coord cc, Coord ul){
        Equipory e = NUtils.getEquipment();
        if(e != null) {
            e.wdgmsg("drop", slot(cc).idx);
            return true;
        }
        return false;
    }


    @Override
    public boolean iteminteract(Coord cc, Coord ul){
        Equipory e = NUtils.getEquipment();
        if(e != null) {
            WItem w = NUtils.getEquipment().quickslots[slot(cc).idx];
            if(w != null) {
                return w.iteminteract( cc, ul);
            }
        }
        return false;
    }


    public static Coord sqoff(Coord c){
        return c.mul(invsq.sz());
    }

    public static Coord sqroff(Coord c){
        return c.div(invsq.sz());
    }

    public static Coord invsz(Coord sz) {
        return invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1));
    }
}
