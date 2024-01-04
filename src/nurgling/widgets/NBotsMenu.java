package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.actions.bots.CowsAction;
import nurgling.actions.bots.GoatsAction;
import nurgling.actions.bots.TurnipsFarmer;
import nurgling.actions.test.*;

import java.util.*;

public class NBotsMenu extends Widget
{
    final static String dir_path = "nurgling/bots/icons/";
    public NBotsMenu()
    {
        if(NUtils.getUI().core.debug)
        {
            NLayout tests = new NLayout("resources");
            tests.elements.add(new NButton("chop", new TurnipsFarmer()));
            tests.elements.add(new NButton("chop", new TESTLiftDrop()));
            tests.elements.add(new NButton("chop", new GoatsAction()));
            tests.elements.add(new NButton("chop", new TESTorestockpiletransfernoclose()));
            tests.elements.add(new NButton("chop", new TESTblockstockpiletransferpacks()));
            tests.elements.add(new NButton("chop", new TESTbranchinvtransferpacks()));
            tests.elements.add(new NButton("chop", new TESTtakehanddporop()));
            tests.elements.add(new NButton("chop", new TESTskinstockpiletransfer()));
            tests.elements.add(new NButton("chop", new TESTselectfloweraction()));
            tests.elements.add(new NButton("chop", new TESTpf()));
//            tests.elements.add(new NButton("chop", new TESTfindallchest()));
            tests.elements.add(new NButton("chop", new TESTAvalaible()));
            addLayout(tests);
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

        void start(String path, Action action)
        {
            new Thread(new Runnable()
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
                }
            }, path).start();
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
