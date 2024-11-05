package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.actions.bots.*;
import nurgling.actions.test.*;

import java.awt.image.BufferedImage;
import java.util.*;

public class NBotsMenu extends Widget
{
    final static String dir_path = "nurgling/bots/icons/";
    public NBotsMenu() {
        NLayout resources = new NLayout("resources");
        resources.elements.add(new NButton("choper", new Chopper()));
        resources.elements.add(new NButton("chipper", new Chipper(), true));
        resources.elements.add(new NButton("pblocks", new PrepareBlocks()));
        resources.elements.add(new NButton("pboards", new PrepareBoards()));
        resources.elements.add(new NButton("log", new TransferLog()));
        resources.elements.add(new NButton("clay", new ClayDigger(), true));
        resources.elements.add(new NButton("bark", new CollectBark(), true));
        resources.elements.add(new NButton("bough", new CollectBough(), true));
        resources.elements.add(new NButton("leaf", new CollectLeaf(), true));
        resources.elements.add(new NButton("fisher", new Fishing(), true));
        resources.elements.add(new NButton("plower", new Plower(), true));
        addLayout(resources);

        NLayout productions = new NLayout("productions");
        productions.elements.add(new NButton("smelter", new SmelterAction(), true));
        productions.elements.add(new NButton("backer", new BackerAction(), true));
        productions.elements.add(new NButton("ugardenpot", new UnGardentPotAction(), true));
        productions.elements.add(new NButton("butcher", new Butcher(), true));
        productions.elements.add(new NButton("hides", new DFrameHidesAction(), true));
        productions.elements.add(new NButton("fishroast", new FriedFish(), true));
        productions.elements.add(new NButton("leather", new LeatherAction(), true));
        productions.elements.add(new NButton("smoking", new Smoking(), true));
        productions.elements.add(new NButton("tarkiln", new TarkilnAction(), true));
        productions.elements.add(new NButton("tabaco", new TabacoAction(), true));
        productions.elements.add(new NButton("brick", new BricksAction(), true));
        productions.elements.add(new NButton("wrap", new WrapAction(), true));
        addLayout(productions);
        NLayout battle = new NLayout("battle");
        battle.elements.add(new NButton("reagro", new Reagro()));
        battle.elements.add(new NButton("attacknearcurs", new AggroNearCurs()));
        battle.elements.add(new NButton("attacknear", new AggroNearest()));
        battle.elements.add(new NButton("attacknearborka", new AggroNearestBorka()));
        battle.elements.add(new NButton("attackall", new AttackAll()));
        addLayout(battle);
        NLayout farming = new NLayout("farming");
        farming.elements.add(new NButton("turnip", new TurnipsFarmer()));
        farming.elements.add(new NButton("hemp", new HempFarmer(), true));
        farming.elements.add(new NButton("flax", new FlaxFarmer(), true));
        farming.elements.add(new NButton("goats", new GoatsAction()));
        farming.elements.add(new NButton("sheeps", new SheepsAction()));
        farming.elements.add(new NButton("pigs", new PigsAction()));
        farming.elements.add(new NButton("cows", new nurgling.actions.bots.CowsAction()));
        addLayout(farming);
        NLayout utils = new NLayout("utils");
        utils.elements.add(new NButton("shieldsword", new EquipShieldSword()));
        utils.elements.add(new NButton("filwater", new FillWaterskins(false)));
        utils.elements.add(new NButton("unbox", new FreeContainersInArea(), true));
        utils.elements.add(new NButton("water_cheker", new CheckWater()));
        utils.elements.add(new NButton("clay_cheker", new CheckClay(), true));
        utils.elements.add(new NButton("clover", new FeedClover(), true));
        utils.elements.add(new NButton("collectalltopile", new CollectSameItemsFromEarth(), true));
        utils.elements.add(new NButton("worldexplorer", new WorldExplorer(), true));
        addLayout(utils);
        NLayout build = new NLayout("build");
        build.elements.add(new NButton("dframe", new BuildDryingFrame(), true));
        build.elements.add(new NButton("cellar", new BuildCellar()));
        build.elements.add(new NButton("ttub", new BuildTtub()));
        build.elements.add(new NButton("cupboard", new BuildCupboard()));
        build.elements.add(new NButton("cheese_rack", new BuildCheeseRack()));
        build.elements.add(new NButton("kiln", new BuildKiln()));
        build.elements.add(new NButton("barrel", new BuildBarrel()));
        build.elements.add(new NButton("chest", new BuildChest()));
        build.elements.add(new NButton("tarkilnb", new BuildTarKiln()));
        build.elements.add(new NButton("smoke_shed", new BuildSmokeShed()));
        addLayout(build);
        if (NUtils.getUI().core.debug) {
            NLayout tests = new NLayout("tools");
            tests.elements.add(new NButton("test1", new TESTMapv4()));
            tests.elements.add(new NButton("test2", new TESTFillCauldron()));
            tests.elements.add(new NButton("test4", new TESTbranchinvtransferpacks()));
            tests.elements.add(new NButton("test5", new TESTfreeStockpilesAndTransfer()));
            tests.elements.add(new NButton("test7", new TESTselectfloweraction()));
            tests.elements.add(new NButton("test8", new TESTpf()));
//            tests.elements.add(new NButton("chop", new TESTfindallchest()));
            tests.elements.add(new NButton("test9", new TESTAvalaible()));
            addLayout(tests);
        }
        showLayouts();
        pack();
    }
    NButton dragging = null;
    @Override
    public void draw(GOut g, boolean strict) {
        super.draw(g, strict);
        if(dragging != null) {
            BufferedImage ds = dragging.btn.up;
            Coord dssz = new Coord(ds.getWidth(),ds.getHeight());
            ui.drawafter(new UI.AfterDraw() {
                public void draw(GOut g) {
                    g.reclip(ui.mc.sub(dssz.div(2)), dssz);
                    g.image(new TexI(ds), ui.mc );
                }
            });
        }
    }
    private NButton bhit(Coord c) {
        for(NLayout lay : layouts)
        {
            for(NButton b : lay.elements)
            {
                if(b.btn.visible())
                {
                    if(c.x <= b.btn.c.x + b.btn.sz.x && c.y <= b.btn.c.y + b.btn.sz.y && c.x >= b.btn.c.x && c.y >= b.btn.c.y)
                        return b;
                }
            }
        }
        return(null);
    }
    public void mousemove(Coord c) {
        if((dragging == null) && (pressed != null)) {
            NButton h = bhit(c);
            if(h != pressed) {
                dragging = pressed;
                if(dragging.btn.d!=null)
                {
                    dragging.btn.d.remove();
                    dragging.btn.d=null;
                }
            }
        }
        super.mousemove(c);
    }

    private NButton pressed = null;
    private UI.Grab grab = null;
    public boolean mouseup(Coord c, int button) {
        NButton h = bhit(c);
        if((button == 1) && (grab != null)) {
            if(dragging != null) {
                ui.dropthing(ui.root, ui.mc, dragging);
                pressed = null;
                dragging = null;
            } else if(pressed != null) {
                if(pressed == h) {
                    pressed.btn.click();
                }
                pressed = null;
            }
            grab.remove();
            grab = null;
        }
        return(super.mouseup(c,button));
    }

    public boolean mousedown(Coord c, int button) {
        NButton h = bhit(c);
        if((button == 1) && (h != null)) {
            pressed = h;
            grab = ui.grabmouse(this);
        }
        boolean res = super.mousedown(c,button);
        if(pressed!=null)
        {
            if(pressed.btn.d!=null)
            {
                pressed.btn.d.remove();
                pressed.btn.d=null;
            }
        }
        return res;
    }

    void addLayout(NLayout lay){
        int count = 0;
        for(NButton btn: lay.elements)
        {
            add(btn.btn, new Coord(0, (btn.btn.sz.y + UI.scale(2)) * count++));
            btn.btn.hide();
        }
        add(lay.btn);
        lay.btn.hide();
        layouts.add(lay);
    }

    public void showLayouts(){
        for(NLayout lay : layouts)
        {
            lay.hideElements();
        }
        int w = 0;
        int h = 0;
        for (NLayout lay : layouts)
        {
            lay.btn.move(new Coord(w * UI.scale(34), h * UI.scale(34)));
            lay.btn.show();
            if (h > 8)
            {
                w += 1;
                h = 0;
            }
            else
            {
                h += 1;
            }
        }
        if(parent!=null)
            parent.resize(new Coord((w + 1) * UI.scale(34), layouts.size() * UI.scale(34)).add(NDraggableWidget.delta));
    }

    public void hideLayouts(){
        for (NLayout lay : layouts)
        {
            lay.btn.hide();
        }
    }

    ArrayList<NLayout> layouts = new ArrayList<>();

    public NButton find(String path) {
        for (NLayout lay : layouts)
        {
            for(NButton element: lay.elements)
            {
                if(element.path!=null && element.path.equals(path))
                    return element;
            }
        }
        return null;
    }

    public class NButton
    {
        public final IButton btn;
        public String path;
        public boolean disStacks;
        NButton(String path, Action action) {
            this.path = path;
            Resource res = Resource.remote().load(dir_path + path + "/u").get();
            btn = new IButton(Resource.loadsimg(dir_path + path + "/u"), Resource.loadsimg(dir_path + path + "/d"), Resource.loadsimg(dir_path + path + "/h")) {
                TexI rtip;

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    if (rtip == null && res.layers(Resource.Tooltip.class) != null) {
                        List<ItemInfo> info = new ArrayList<>();
                        int count = 0;
                        for (Resource.Tooltip tt : res.layers(Resource.Tooltip.class)) {
                            if(count == 0)
                            {
                                info.add(new ItemInfo.Tip(null) {
                                    @Override
                                    public BufferedImage tipimg() {
                                        return Text.render(tt.t).img;
                                    }
                                });
                            }
                            else {
                                info.add(new ItemInfo.Pagina(null, tt.t));
                            }
                            count++;
                        }
                        BufferedImage img = ItemInfo.longtip(info);
                        if(img!=null)
                            rtip = new TexI(img);
                    }
                    return (rtip);
                }
            }
                    .action(
                            new Runnable() {
                                @Override
                                public void run() {
                                    start(path, action);
                                }
                            });

        }

        NButton(String path, Action action, Boolean disStacks)
        {
            this(path, action);
            this.disStacks = disStacks;
        }

        private NButton()
        {
            btn = new IButton(Resource.loadsimg(dir_path + "back" + "/u"), Resource.loadsimg(dir_path +  "back" + "/d"), Resource.loadsimg(dir_path +  "back" + "/h")){
                @Override
                public void click() {
                    super.click();
                    showLayouts();
                }
            };
       }





        void start(String path, Action action)
        {
            Thread t;
            t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        showLayouts();
                        action.run(NUtils.getGameUI());
                    }
                    catch (InterruptedException e)
                    {
                        NUtils.getGameUI().msg(path + ":" + "STOPPED");
                    }
                    finally
                    {
                        if(action instanceof ActionWithFinal)
                        {
                            ((ActionWithFinal)action).endAction();
                        }
                    }
                }
            }, path);
            if(disStacks)
                NUtils.getGameUI().biw.addObserve(t, true);
            else
                NUtils.getGameUI().biw.addObserve(t);
            t.start();
        }


    };

    class NLayout
    {
        public final IButton btn;

        ArrayList<NButton> elements = new ArrayList<>();

        public NLayout(String path)
        {
            this.btn = new IButton(Resource.loadsimg(dir_path + path + "/u"),Resource.loadsimg(dir_path + path + "/d"),Resource.loadsimg(dir_path + path + "/h")).action(new Runnable()
            {
                @Override
                public void run()
                {
                    hideLayouts();
                    showElements();
                }
            });
            elements.add(new NButton());
        }

        void hideElements()
        {
            for (NButton element : elements)
            {
                element.btn.hide();
            }
        }

        void showElements()
        {
            int w = 0;
            int h = 0;
            for (NButton element : elements)
            {
                element.btn.move(new Coord(w * UI.scale(34), h * UI.scale(34)));
                if (h > 7)
                {
                    w += 1;
                    h = 0;
                }
                else
                {
                    h += 1;
                }
                element.btn.show();
            }
            parent.resize(new Coord((w + 1) * UI.scale(34), (w > 0 ? 9 : h + 1) * UI.scale(34)).add(NDraggableWidget.delta));
        }
    };
}
