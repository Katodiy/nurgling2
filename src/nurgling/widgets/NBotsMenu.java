package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.actions.bots.*;
import nurgling.actions.test.*;

import java.util.*;

public class NBotsMenu extends Widget
{
    final static String dir_path = "nurgling/bots/icons/";
    public NBotsMenu()
    {
        if(NUtils.getUI().core.debug)
        {
            NLayout resources = new NLayout("resources");
            resources.elements.add(new NButton("choper", new Chopper()));
            resources.elements.add(new NButton("log", new TransferLog()));
            addLayout(resources);
            NLayout productions = new NLayout("productions");
            productions.elements.add(new NButton("smelter", new SmelterAction()));
            addLayout(productions);

            NLayout farming = new NLayout("farming");
            farming.elements.add(new NButton("turnip", new TurnipsFarmer()));
            farming.elements.add(new NButton("hemp", new HempFarmer()));
            farming.elements.add(new NButton("flax", new FlaxFarmer()));
            farming.elements.add(new NButton("goats", new GoatsAction()));
            farming.elements.add(new NButton("sheeps", new SheepsAction()));
            farming.elements.add(new NButton("pigs", new PigsAction()));
            farming.elements.add(new NButton("cows", new nurgling.actions.bots.CowsAction()));
            addLayout(farming);
            NLayout utils = new NLayout("utils");
            utils.elements.add(new NButton("shieldsword", new EquipShieldSword()));
            utils.elements.add(new NButton("filwater", new FillWaterskins()));
            addLayout(utils);
            NLayout tests = new NLayout("tools");
            tests.elements.add(new NButton("test1", new TESTLiftDrop()));
            tests.elements.add(new NButton("test4", new TESTbranchinvtransferpacks()));
            tests.elements.add(new NButton("test5", new TESTtakehanddporop()));
            tests.elements.add(new NButton("test7", new TESTselectfloweraction()));
            tests.elements.add(new NButton("test8", new TESTpf()));
//            tests.elements.add(new NButton("chop", new TESTfindallchest()));
            tests.elements.add(new NButton("test9", new NomadWalker()));
            tests.elements.add(new NButton("calibrator", new NomadCalibration()));
            addLayout(tests);
            NLayout nords = new NLayout("toolsNords");
            nords.elements.add(new NButton("nomadCalibrator", new NomadCalibration() ));
            nords.elements.add(new NButton("nomadWalker", new NomadWalker() ));
            nords.elements.add(new NButton("nomadSailor", new NomadSailor() ));
            nords.elements.add(new NButton("test1", new NomadOisterer() ));
            addLayout(nords);

        }
        showLayouts();
        pack();
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

    class NButton
    {
        public final IButton btn;

        NButton(String path, Action action)
        {
            btn = new IButton(Resource.loadsimg(dir_path + path + "/u"), Resource.loadsimg(dir_path + path + "/d"), Resource.loadsimg(dir_path + path + "/h")).action(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            start(path, action);
                        }
                    });
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
            (t = new Thread(new Runnable()
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
                        NUtils.getGameUI().msg(path + ":" +"STOPPED");
                    }
                    finally
                    {
                        if(action instanceof ActionWithFinal)
                        {
                            ((ActionWithFinal)action).endAction();
                        }
                    }
                }
            }, path)).start();
            NUtils.getGameUI().biw.addObserve(t);
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
                if (h > 8)
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
