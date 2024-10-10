package nurgling.actions;

import haven.*;
import nurgling.NGameUI;
import nurgling.NGob;
import nurgling.NUtils;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.ArrayList;
import java.util.HashSet;

import static haven.OCache.posres;

public class Build implements Action{
    Command cmd;
    Pair<Coord2d, Coord2d> area;
    boolean needRotate = false;

    public static class Command
    {
        public String name;

        public ArrayList<Ingredient> ingredients = new ArrayList<Ingredient>();
    }
    public static class Ingredient{

        public Coord coord;
        public Pair<Coord2d, Coord2d> area;
        public NAlias name;
        public int count;
        public int left = 0;
        public Action specialWay = null;
        public ArrayList<Container> containers = new ArrayList<>();

        public Ingredient(Coord coord, Pair<Coord2d, Coord2d> area, NAlias name, int count) {
            this.coord = coord;
            this.area = area;
            this.name = name;
            this.count = count;
        }

        public Ingredient(Coord coord, Pair<Coord2d, Coord2d> area, NAlias name, int count, Action specialWay) {
            this.coord = coord;
            this.area = area;
            this.name = name;
            this.count = count;
            this.specialWay = specialWay;
        }
    }

    public Build(Command cmd, Pair<Coord2d, Coord2d> area) {
        this.cmd = cmd;
        this.area = area;
        needRotate = (Math.abs(area.b.x-area.a.x)<Math.abs(area.b.y-area.a.y));
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {


        for(Ingredient ingredient: cmd.ingredients) {
            if (ingredient.area != null) {
                for (Gob sm : Finder.findGobs(ingredient.area, new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
                    Container cand = new Container();
                    cand.gob = sm;
                    cand.cap = Context.contcaps.get(cand.gob.ngob.name);
                    cand.initattr(Container.Space.class);
                    ingredient.containers.add(cand);
                }
            }
        }

        Coord2d pos = Coord2d.z;
        do
        {
            boolean isExist = false;
            ArrayList<Ingredient> curings = new ArrayList<Ingredient>();
            for(Ingredient ingredient: cmd.ingredients)
            {
                int size = NUtils.getGameUI().getInventory().getItems(ingredient.name).size();
                if(size>0)
                {
                    isExist = true;
                }
                Ingredient copy = new Ingredient(ingredient.coord, ingredient.area, ingredient.name, ingredient.count - size , ingredient.specialWay);
                copy.containers = ingredient.containers;
                copy.left = Math.max(0,size- copy.count);
                curings.add(copy);
            }
            if(!isExist) {
                if(!refillIng(gui, curings))
                    return Results.ERROR("NO ITEMS");
            }

            for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
                if (pag.button() != null && pag.button().name().equals(cmd.name)) {
                    pag.button().use(new MenuGrid.Interaction(1, 0));
                    break;
                }
            }

            if(NUtils.getGameUI().map.placing==null)
            {
                for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
                    if (pag.button() != null && pag.button().name().equals(cmd.name)) {
                        pag.button().use(new MenuGrid.Interaction(1, 0));
                        break;
                    }
                }
            }
            NUtils.addTask(new WaitPlob());
            MapView.Plob plob = NUtils.getGameUI().map.placing.get();
            plob.a = needRotate ? Math.PI / 2 : 0;
            pos = Finder.getFreePlace(area, needRotate?plob.ngob.hitBox.rotate():plob.ngob.hitBox);

            PathFinder pf = new PathFinder(NGob.getDummy(pos, plob.a, plob.ngob.hitBox), true);
            pf.isHardMode = true;
            pf.run(gui);

            gui.map.wdgmsg("place", pos.floor(posres), (int) Math.round((needRotate ? Math.PI / 2 : 0) * 32768 / Math.PI), 1, 0);
            NUtils.addTask(new WaitConstructionObject(pos));
            NUtils.addTask(new WaitWindow(cmd.name));
            Gob gob;
            do {
                if(needRefill(curings))
                {
                    if(!refillIng(gui, curings))
                        return Results.ERROR("NO ITEMS");
                    gob = Finder.findGob(pos);
                    if(gob==null)
                        return Results.ERROR("Something went wrong, no gob");
                    new PathFinder(gob).run(gui);
                    NUtils.rclickGob(gob);
                    NUtils.addTask(new WaitWindow(cmd.name));
                }

                NUtils.startBuild(NUtils.getGameUI().getWindow(cmd.name));
                WaitBuildState wbs = new WaitBuildState();
                NUtils.addTask(wbs);
                if(wbs.getState()== WaitBuildState.State.TIMEFORDRINK)
                {
                    new Drink(0.9).run(gui);
                }
                else if (wbs.getState()== WaitBuildState.State.DANGER)
                {
                    return Results.ERROR("Low energy");
                }
            }while ((gob = Finder.findGob(pos))!=null && NParser.checkName(gob.ngob.name, "gfx/terobjs/consobj"));

            for(Ingredient ingredient: curings)
            {
                NUtils.addTask(new WaitItems(NUtils.getGameUI().getInventory(),ingredient.name,ingredient.left));
            }

            pos = Finder.getFreePlace(area, needRotate?plob.ngob.hitBox.rotate():plob.ngob.hitBox);
        }
        while (pos!=null);
        return Results.SUCCESS();
    }

    private boolean refillIng(NGameUI gui, ArrayList<Ingredient> curings) throws InterruptedException {
        for(Ingredient ingredient: curings)
        {
            if(ingredient.specialWay==null)
            {
                if(!ingredient.containers.isEmpty()) {
                    for (Container container : ingredient.containers) {
                        Container.Space space = container.getattr(Container.Space.class);
                        if(space.isReady())
                        {
                            int aval = (int) space.getRes().get(Container.Space.MAXSPACE) - (int) space.getRes().get(Container.Space.FREESPACE);
                            if (aval != (int) space.getRes().get(Container.Space.FREESPACE)) {
                                new PathFinder(container.gob).run(gui);
                                new OpenTargetContainer(container).run(gui);
                                TakeAvailableItemsFromContainer tifc = new TakeAvailableItemsFromContainer(container,ingredient.name, ingredient.count);
                                tifc.run(gui);
                                ingredient.count = ingredient.count - tifc.getCount();
                            }
                        }
                        else
                        {
                            new PathFinder(container.gob).run(gui);
                            new OpenTargetContainer(container).run(gui);
                            TakeAvailableItemsFromContainer tifc = new TakeAvailableItemsFromContainer(container,ingredient.name, ingredient.count);
                            tifc.run(gui);
                            ingredient.count = ingredient.count - tifc.getCount();
                        }

                        if (ingredient.count == 0)
                            break;
                    }
                }
                else
                {
                    while ( ingredient.count!= 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(ingredient.coord)!=0) {
                        ArrayList<Gob> piles = Finder.findGobs(ingredient.area, new NAlias("stockpile"));
                        if (piles.isEmpty()) {
                            if(NUtils.getGameUI().getInventory().getItems(ingredient.name).size()!=ingredient.count)
                                return false;
                        }
                        piles.sort(NUtils.d_comp);

                        Gob pile = piles.get(0);
                        new PathFinder(pile).run(NUtils.getGameUI());
                        new OpenTargetContainer("Stockpile", pile).run(NUtils.getGameUI());
                        TakeItemsFromPile tifp;
                        (tifp = new TakeItemsFromPile(pile, NUtils.getGameUI().getStockpile(), Math.min(ingredient.count, NUtils.getGameUI().getInventory().getFreeSpace()))).run(gui);
                        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                        ingredient.count = ingredient.count - tifp.getResult();
                    }
                }
            }
        }
        return !needRefill(curings);
    }

    boolean needRefill(ArrayList<Ingredient> curings) throws InterruptedException {
        boolean needRefill = false;
        for(Ingredient ingredient: curings)
        {
            if(ingredient.count>0) {
                int size = NUtils.getGameUI().getInventory().getItems(ingredient.name).size();
                if (size > 0) {
                    return false;
                }
                else
                {
                    needRefill = true;
                }
            }
        }
        return needRefill;
    }
}
