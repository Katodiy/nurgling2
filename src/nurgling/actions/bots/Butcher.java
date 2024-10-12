package nurgling.actions.bots;

import haven.Coord;
import haven.FlowerMenu;
import haven.Gob;
import nurgling.NFlowerMenu;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.*;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Butcher implements Action {

    static HashMap<String,Req> options = new HashMap<>();
    static ArrayList<String> order = new ArrayList<>();

    static {
        order.add("Skin");
        order.add("Scale");
        order.add("Crack");
        order.add("Clean");
        order.add("Butcher");
        order.add("Collect bones");
        options.put("Skin", new Req(new Coord(2,2),1));
        options.put("Clean", new Req(new Coord(1,1),1));
        options.put("Butcher", new Req(new Coord(1,1),1));
        options.put("Collect bones", new Req(new Coord(2,2),1));
        options.put("Crack", new Req(new Coord(2,2),1));
    }

    static class Req{
        public Req(Coord size, int num) {
            this.size = size;
            this.num = num;
        }

        public Coord size;
        public int num;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea.Specialisation kritter_corpse = new NArea.Specialisation(Specialisation.SpecName.deadkritter.toString());
        NArea area = NArea.findSpec(kritter_corpse);
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(kritter_corpse);

        ArrayList<NArea.Specialisation> opt = new ArrayList<>();

        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            ArrayList<Gob> gobs = getGobs(area);
            Context context = new Context();
            while (!gobs.isEmpty())
            {
                gobs.sort(NUtils.d_comp);
                for(Gob gob: gobs)
                {

                    while (Finder.findGob(gob.id)!=null) {
                        NUtils.rclickGob(gob);
                        NFlowerMenu fm = NUtils.getFlowerMenu();
                        String optForSelect = null;
                        for(String option: order)
                        {
                            for(NFlowerMenu.NPetal petal : fm.nopts)
                            {
                                if(petal.name.equals(option))
                                {
                                    optForSelect = option;
                                    fm.wdgmsg("cl", -1);
                                    NUtils.getUI().core.addTask(new NFlowerMenuIsClosed());
                                    break;
                                }
                            }
                            if(optForSelect != null)
                                break;
                        }
                        boolean optFound = optForSelect != null;
                        while(optFound) {

                            if (NUtils.getGameUI().getInventory().getNumberFreeCoord(options.get(optForSelect).size) < options.get(optForSelect).num) {
                                new FreeInventory(context).run(gui);
                            }
                            if (NUtils.getGameUI().getInventory().getNumberFreeCoord(options.get(optForSelect).size) < options.get(optForSelect).num) {
                                return Results.ERROR("No free coord found for: " + optForSelect + "|" + options.get(optForSelect).size.toString() + "| target size: " + options.get(optForSelect).num);
                            }
                            new PathFinder(gob).run(gui);

                            if(new SelectFlowerAction(optForSelect,gob).run(gui).IsSuccess()) {

                                if (!optForSelect.equals("Collect bones")) {
                                    NUtils.getUI().core.addTask(new WaitPose(NUtils.player(), "gfx/borka/butcher"));
                                    WaitButcherState wbs = new WaitButcherState(options.get(optForSelect).size);
                                    NUtils.addTask(wbs);
                                    if (wbs.getState() == WaitButcherState.State.READY)
                                    {
                                        optFound = false;
                                    }
                                }
                                else
                                {
                                    NUtils.addTask(new NoGob(gob.id));
                                    if(gui.vhand!=null)
                                    {
                                        NUtils.drop(gui.vhand);
                                        NUtils.addTask(new WaitFreeHand());
                                        new FreeInventory(context).run(gui);
                                    }
                                    optFound = false;
                                }
                            }
                        }
                    }
                }
                gobs = getGobs(area);
            }
        }

        return Results.SUCCESS();
    }

    private static ArrayList<Gob> getGobs(NArea area) throws InterruptedException {
        ArrayList<Gob> result = new ArrayList<>();
        ArrayList<Gob> gobs = Finder.findGobs(area.getRCArea(),new NAlias("kritter"));
        for(Gob gob: gobs)
        {
            if(gob.pose()!=null && !gob.pose().contains("dead") && PathFinder.isAvailable(gob))
            {
                result.add(gob);
            }
        }
        return result;
    }
}
