package nurgling;

import haven.*;
import nurgling.actions.AutoDrink;
import nurgling.actions.bots.*;
import nurgling.areas.NContext;
import nurgling.widgets.NProspecting;

import java.util.*;

public class NFlowerMenu extends FlowerMenu
{
    public static final Tex bl = Resource.loadtex("nurgling/hud/flower/left");
    public static final Tex bm = Resource.loadtex("nurgling/hud/flower/mid");
    public static final Tex br = Resource.loadtex("nurgling/hud/flower/right");

    public static final Tex bhl = Resource.loadtex("nurgling/hud/flower/hleft");
    public static final Tex bhm = Resource.loadtex("nurgling/hud/flower/hmid");
    public static final Tex bhr = Resource.loadtex("nurgling/hud/flower/hright");

    public NPetal[] nopts;

    int len = 0;
    public boolean shiftMode = false;
    public NFlowerMenu(String[] opts)
    {
        super();
        shiftMode = ((NMapView)NUtils.getGameUI().map).shiftPressed;
        nopts = new NPetal[opts.length];
        int y = 0;

        for(int i = 0; i < opts.length; i++)
        {
            add(nopts[i] = new NPetal(opts[i], i + 1), new Coord(0,y));
            nopts[i].num = i;
            y+=bl.sz().y + UI.scale(2);
            len = Math.max(nopts[i].sz.x,len);
        }
        for(int i = 0; i < opts.length; i++)
        {
            nopts[i].resize(len, bl.sz().y);
        }
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if(!shiftMode && (Boolean) NConfig.get(NConfig.Key.asenable) && !NContext.waitBot.get()) {
            if ((Boolean) NConfig.get(NConfig.Key.singlePetal) && nopts.length == 1 && (NUtils.getUI().core.getLastActions()==null || NUtils.getUI().core.getLastActions().item == null)) {
                nchoose(nopts[0]);
            } else {
                ArrayList<String> autoPetal = NUtils.getPetals();
                for (NPetal opt : nopts) {
                    if (autoPetal.contains(opt.name)) {
                        nchoose(opt);
                        break;
                    }
                }
            }
        }
    }

    public NFlowerMenu(ArrayList<String> opts)
    {
        this(opts.toArray(new String[0]));
    }

    public void nchoose(NPetal option)
    {
        if (option == null)
        {
            wdgmsg("cl", -1);
            NUtils.getUI().core.setLastAction();
        }
        else
        {
            // Handle custom "Save Tree Location" option
            if(option.name.equals("Save Tree Location")) {
                NCore.LastActions actions = NUtils.getUI().core.getLastActions();
                if(actions != null && actions.gob != null) {
                    NGameUI gui = (NGameUI) NUtils.getGameUI();
                    if(gui != null && gui.treeLocationService != null) {
                        gui.treeLocationService.saveTreeLocation(actions.gob);
                    }
                }
                wdgmsg("cl", -1); // Close menu without sending to server
                NUtils.getUI().core.setLastAction();
                return;
            }

            wdgmsg("cl", option.num, ui.modflags());
            NCore.LastActions actions = NUtils.getUI().core.getLastActions();
            if(actions!=null) {
                if (actions.item != null) {
                    NUtils.getUI().core.setLastAction(option.name, actions.item);
                } else if (actions.gob != null) {
                    NUtils.getUI().core.setLastAction(option.name, actions.gob);
                }
            }
        }
        if(!NUtils.getUI().core.isBotmod() && (Boolean)NConfig.get(NConfig.Key.autoFlower))
        {
            if (option != null && NUtils.getUI().core.getLastActions()!=null)
            {
                if (NUtils.getUI().core.getLastActions().item != null && NUtils.getUI().core.getLastActions().item.parent instanceof NInventory && ((NGItem)NUtils.getUI().core.getLastActions().item.item).name()!=null) {
                    if (!option.name.equals("Split") || ((NGItem)NUtils.getUI().core.getLastActions().item.item).name().startsWith("Block") || ((NGItem)NUtils.getUI().core.getLastActions().item.item).name().startsWith("Head of")) {
                        AutoChooser.enable((NInventory) NUtils.getUI().core.getLastActions().item.parent,((NGItem)NUtils.getUI().core.getLastActions().item.item).name(), option.name);
                    }
                }
            }
        }
        if(option != null && NUtils.getUI().core.getLastActions()!=null && NUtils.getUI().core.getLastActions().item!=null && option.name.contains("Prospect")) {
            NProspecting.item(NUtils.getUI().core.getLastActions().item);
        }
        NUtils.getUI().core.resetLastAction();
    }

    public boolean hasOpt(String action) {
        for(NPetal petal: nopts)
        {
            if(petal.name.equals(action))
            {
                return true;
            }
        }
        return false;
    }

    public class NPetal extends Widget {
        public String name;
        public int num;
        private Text text;
        private Text textnum;

        public NPetal(String name, int num) {
            super(Coord.z);
            this.name = name;
            this.num = num;
            text = NStyle.flower.render(name);
            textnum = NStyle.flower.render(String.valueOf(num));
            resize(text.sz().x + bl.sz().x + br.sz().x + UI.scale(30), ph);
        }

        public void draw(GOut g)
        {
            g.image((isHighligted) ? bhl : bl, new Coord(0, 0));

            Coord pos = new Coord(0, 0);
            for (pos.x = bl.sz().x; pos.x + bm.sz().x <= len - br.sz().x; pos.x += bm.sz().x)
            {
                g.image((isHighligted) ? bhm : bm, pos);
            }
            g.image((isHighligted) ? bhm : bm, pos, new Coord(sz.x - pos.x - br.sz().x, br.sz().y));
            g.image(textnum.tex(), new Coord(bl.sz().x/2 - textnum.tex().sz().x/2 - UI.scale(1), br.sz().y / 2 - textnum.tex().sz().y / 2));
            g.image(text.tex(), new Coord(br.sz().x + bl.sz().x + UI.scale(10), br.sz().y / 2 - text.tex().sz().y / 2));
            g.image((isHighligted) ? bhr : br, new Coord(len - br.sz().x, 0));
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            nchoose(this);
            return(true);
        }

        @Override
        public void mousemove(MouseMoveEvent ev)
        {
            isHighligted = ev.c.isect(Coord.z, sz);
            super.mousemove(ev);
        }

        boolean isHighligted = false;
    }

    protected void added()
    {
        if (c.equals(-1, -1))
            c = parent.ui.lcc;
        mg = ui.grabmouse(this);
        kg = ui.grabkeys(this);
    }

    public void uimsg(String msg, Object... args)
    {

        if (msg.equals("cancel") || msg.equals("act"))
        {
            ui.destroy(NFlowerMenu.this);
        }
    }


    @Override
    public void destroy() {
        mg.remove();
        kg.remove();
        super.destroy();
    }

    public boolean keydown(KeyDownEvent ev) {
        char key = ev.c;
        if((key >= '0') && (key <= '9')) {
            int opt = (key == '0')?10:(key - '1');
            if(opt < nopts.length) {
                nchoose(nopts[opt]);
                kg.remove();
            }
            return(true);
        } else if(key_esc.match(ev)) {
            nchoose(null);
            kg.remove();
            return(true);
        }
        return(false);
    }

    public boolean chooseOpt(String value)
    {
        for(NPetal petal: nopts)
        {
            if(petal.name.equals(value))
            {
                nchoose(petal);
                return true;
            }
        }
        wdgmsg("cl", -1);
        return false;
    }
}
