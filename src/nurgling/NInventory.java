package nurgling;

import haven.*;
import haven.res.ui.tt.slot.Slotted;
import haven.res.ui.tt.stackn.Stack;
import nurgling.iteminfo.NFoodInfo;
import nurgling.widgets.NPopupWidget;
import nurgling.widgets.NSearchWidget;

public class NInventory extends Inventory
{
    public NSearchWidget searchwdg;
    public NPopupWidget toggles;
    public ICheckBox bundle;
    public MenuGrid.Pagina pagBundle = null;
    boolean showPopup = false;

    public NInventory(Coord sz)
    {
        super(sz);
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
        super.tick(dt);
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
                pagBundle.button().use(new MenuGrid.Interaction(1, 0));
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
}
