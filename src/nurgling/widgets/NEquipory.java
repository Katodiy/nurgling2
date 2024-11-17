package nurgling.widgets;

import haven.*;
import haven.res.ui.tt.gast.Gast;
import haven.res.ui.tt.wear.Wear;
import nurgling.*;
import nurgling.tasks.GetItem;
import nurgling.tasks.WaitItemSpr;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class NEquipory extends Equipory
{
    public static Text.Furnace fnd = new PUtils.BlurFurn(new Text.Foundry(Text.sans.deriveFont(java.awt.Font.BOLD), 12).aa(true), UI.scale(1), UI.scale(1), Color.BLACK);
    final TexI eye = new TexI(Resource.loadsimg("nurgling/hud/eye"));
    final TexI armor = new TexI(Resource.loadsimg("nurgling/hud/armor"));
    int percExp = -1;
    int hardArmor = -1;
    int softArmor = -1;
    public NEquipory(long gobid)
    {
        super(gobid);
    }

    BufferedImage percExpText = null;
    BufferedImage hardSoft = null;

    public enum Slots {
        HEAD(0),       //00: Headgear
        ACCESSORY(1),  //01: Main Accessory
        SHIRT(2),      //02: Shirt
        ARMOR_BODY(3), //03: Torso Armor
        GLOVES(4),     //04: Gloves
        BELT(5),       //05: Belt
        HAND_LEFT(6),  //06: Left Hand
        HAND_RIGHT(7), //07: Right Hand
        RING_LEFT(8),  //08: Left Hand Ring
        RING_RIGHT(9), //09: Right Hand Ring
        ROBE(10),      //10: Cloaks & Robes
        BACK(11),      //11: Backpack
        PANTS(12),     //12: Pants
        ARMOR_LEG(13), //13: Armor
        CAPE(14),      //14: Cape
        BOOTS(15),     //15: Shoes
        STORE_HAT(16), //16: Hat from store
        EYES(17),      //17: Eyes
        MOUTH(18),     //18: Mouth
        LFOOT(19),     //19: Left Foot
        RFOOT(20);     //20: Right Foot

        public final int idx;
        Slots(int idx) {
            this.idx = idx;
        }
    }

    public WItem[] quickslots = new NWItem[ecoords.length];

    private void updatePercExpText() {
        int perceptionValue = 0;
        int explorationValue = 0;
        GameUI gui = getparent(GameUI.class);
        if (gui != null && gui.chrwdg != null) {
            if (gui.chrwdg.battr != null) {
                for (BAttrWnd.Attr attr : gui.chrwdg.battr.attrs) {
                    if (attr.nm.equals("prc")) {
                        perceptionValue = attr.attr.comp;
                        break;
                    }
                }
            }
            if (gui.chrwdg.sattr != null) {
                for (SAttrWnd.SAttr attr : gui.chrwdg.sattr.attrs) {
                    if (attr.nm.equals("explore")) {
                        explorationValue = attr.attr.comp;
                        break;
                    }
                }
            }

            int percExp = perceptionValue * explorationValue;
            if(this.percExp!=percExp) {
                this.percExp = percExp;
                percExpText = fnd.render(String.valueOf(percExp)).img;
            }
        }
    }

    public void updateTotalArmor() {
        int hardArmor = 0;
        int softArmor = 0;

        for (Slots slot : Slots.values()) {
            WItem item = quickslots[slot.idx];
            if (item != null) {
                NGItem gitem = (NGItem) item.item;
                Wear wear = gitem.getInfo(Wear.class);
                if(wear!=null) {
                    if(wear.d!=wear.m) {
                        hardArmor += gitem.hardArmor;
                        softArmor += gitem.softArmor;
                    }
                }
            }
        }
        if(hardArmor!=this.hardArmor || softArmor!=this.softArmor) {
            this.hardArmor = hardArmor;
            this.softArmor = softArmor;
            hardSoft = fnd.render(String.format("%d/%d",hardArmor,softArmor)).img;
        }
    }


    @Override
    public void addchild (
            Widget child,
            Object... args
    ) {
        if ( child instanceof NGItem ) {
            add ( child );
            NGItem g = ( NGItem ) child;
            WItem[] v = new NWItem[args.length];
            for ( int i = 0 ; i < args.length ; i++ ) {
                int ep = ( Integer ) args[i];
                v[i] = quickslots[ep] = add ( new NWItem(g), ecoords[ep].add ( 1, 1 ) );
            }
            wmap.put ( g, Arrays.asList ( v.clone () ) );
        }
        else {
            super.addchild ( child, args );
        }
    }

    @Override
    public void cdestroy ( Widget w ) {
        if ( w instanceof GItem ) {
            GItem i = ( GItem ) w;
            for ( WItem v : wmap.remove ( i ) ) {
                ui.destroy ( v );
                for ( int qsi = 0 ; qsi < ecoords.length ; qsi++ ) {
                    if ( quickslots[qsi] == v ) {
                        /// Снимаемый предмет удаляется из массива
                        quickslots[qsi] = null;
                        break;
                    }
                }
            }
        }
    }


    @Override
    public void tick(double dt) {
        super.tick(dt);
        updatePercExpText();
        updateTotalArmor();
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        Coord textCoord = new Coord(sz.x - percExpText.getWidth() - UI.scale(65), UI.scale(3));
        if (percExpText != null) {

            g.image(eye, textCoord, UI.scale(20,20));
            g.image(percExpText, textCoord.add(UI.scale(21, -1)));
        }
        if(hardSoft!=null) {
            textCoord = textCoord.add(UI.scale(0, 18));
            g.image(armor, textCoord, UI.scale(20, 20));
            g.image(hardSoft, textCoord.add(UI.scale(21, -1)));
        }
    }

    public WItem findItem(int id) throws InterruptedException {
        if (quickslots[id] != null) {
            NUtils.getUI().core.addTask(new WaitItemSpr(quickslots[id]));
            return quickslots[id];
        }
        return null;
    }

    public WItem findItem(String name) throws InterruptedException {
        for(int i = 0; i < ecoords.length;i++) {
            if (quickslots[i] != null) {
                if (((NGItem) quickslots[i].item).name().endsWith(name)) {
                    return quickslots[i];
                }
            }
        }
        return null;
    }

    public WItem findBucket (String content) throws InterruptedException {
        if (quickslots[Slots.HAND_RIGHT.idx] != null) {
            NUtils.getUI().core.addTask(new WaitItemSpr(quickslots[Slots.HAND_RIGHT.idx]));
            if (NParser.checkName("Bucket", ((NGItem) quickslots[Slots.HAND_RIGHT.idx].item).name())) {
                if (((NGItem) quickslots[Slots.HAND_RIGHT.idx].item).content().isEmpty() || NParser.checkName(((NGItem) quickslots[Slots.HAND_RIGHT.idx].item).content().get(0).name(), content))
                    return quickslots[Slots.HAND_RIGHT.idx];
            }
        }
        if (quickslots[Slots.HAND_LEFT.idx] != null) {
            if (NParser.checkName("Bucket", ((NGItem) quickslots[Slots.HAND_LEFT.idx].item).name())) {
                if (((NGItem) quickslots[Slots.HAND_LEFT.idx].item).content().isEmpty() || NParser.checkName(((NGItem) quickslots[Slots.HAND_LEFT.idx].item).content().get(0).name(), content))
                    return quickslots[Slots.HAND_LEFT.idx];
            }
        }
        return null;
    }
}
