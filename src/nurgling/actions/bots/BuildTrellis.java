package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.pf.NHitBoxD;
import nurgling.tasks.*;
import nurgling.tools.*;

import java.util.*;

import static haven.OCache.posres;

public class BuildTrellis implements Action {

    private static final int TRELLIS_PER_TILE = 3;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Build.Command command = new Build.Command();
        command.name = "Trellis";

        NUtils.getGameUI().msg("Please, select area for blocks");
        SelectArea blockarea = new SelectArea(Resource.loadsimg("baubles/blockIng"));
        blockarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,2), blockarea.getRCArea(), new NAlias("Block"), 3));

        NUtils.getGameUI().msg("Please, select area for strings");
        SelectArea stringarea = new SelectArea(Resource.loadsimg("baubles/stringsIng"));
        stringarea.run(NUtils.getGameUI());
        command.ingredients.add(new Build.Ingredient(new Coord(1,1), stringarea.getRCArea(), new NAlias("Flax Fibres", "Hemp Fibres", "Spindly Taproot", "Cattail Fibres", "Stinging Nettle", "Hide Strap", "Straw Twine", "Bark Cordage"), 1));

        // Activate build menu to get the trellis hitbox
        for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
            if (pag.button() != null && pag.button().name().equals(command.name)) {
                pag.button().use(new MenuGrid.Interaction(1, 0));
                break;
            }
        }

        if(NUtils.getGameUI().map.placing == null) {
            for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
                if (pag.button() != null && pag.button().name().equals(command.name)) {
                    pag.button().use(new MenuGrid.Interaction(1, 0));
                    break;
                }
            }
        }

        NUtils.addTask(new WaitPlob());
        MapView.Plob plob = NUtils.getGameUI().map.placing.get();
        NHitBox trellisHitBox = plob.ngob.hitBox;

        // Cancel the placement for now - we'll restart it later
        NUtils.getGameUI().map.placing = null;

        // Now select build area with ghost previews
        NUtils.getGameUI().msg("Please, select build area and choose direction");
        SelectAreaWithRotation buildarea = new SelectAreaWithRotation(Resource.loadsimg("baubles/buildArea"), trellisHitBox);
        buildarea.run(NUtils.getGameUI());

        Pair<Coord2d, Coord2d> area = buildarea.getRCArea();
        boolean needRotate = buildarea.getRotation();

        for(Build.Ingredient ingredient: command.ingredients) {
            if (ingredient.area != null) {
                for (Gob sm : Finder.findGobs(ingredient.area, new NAlias(new ArrayList<>(Context.contcaps.keySet())))) {
                    Container cand = new Container(sm, Context.contcaps.get(sm.ngob.name));
                    cand.initattr(Container.Space.class);
                    ingredient.containers.add(cand);
                }
            }
        }

        HashMap<Coord, Integer> tileCount = new HashMap<>();
        Coord2d pos = Coord2d.z;

        do {
            boolean isExist = false;
            ArrayList<Build.Ingredient> curings = new ArrayList<>();
            for(Build.Ingredient ingredient: command.ingredients) {
                int size = NUtils.getGameUI().getInventory().getItems(ingredient.name).size();
                if(size > 0) {
                    isExist = true;
                }
                Build.Ingredient copy = new Build.Ingredient(ingredient.coord, ingredient.area, ingredient.name, ingredient.count - size, ingredient.specialWay);
                copy.containers = ingredient.containers;
                copy.left = Math.max(0, size - copy.count);
                curings.add(copy);
            }

            if(!isExist) {
                if(!refillIng(gui, curings))
                    return Results.ERROR("NO ITEMS");
            }

            for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
                if (pag.button() != null && pag.button().name().equals(command.name)) {
                    pag.button().use(new MenuGrid.Interaction(1, 0));
                    break;
                }
            }

            if(NUtils.getGameUI().map.placing == null) {
                for (MenuGrid.Pagina pag : NUtils.getGameUI().menu.paginae) {
                    if (pag.button() != null && pag.button().name().equals(command.name)) {
                        pag.button().use(new MenuGrid.Interaction(1, 0));
                        break;
                    }
                }
            }

            NUtils.addTask(new WaitPlob());
            plob = NUtils.getGameUI().map.placing.get();
            plob.a = needRotate ? Math.PI / 2 : 0;

            pos = findFreePlaceWithLimit(area, needRotate ? plob.ngob.hitBox.rotate() : plob.ngob.hitBox, tileCount);

            if(pos == null) {
                break;
            }

            PathFinder pf = new PathFinder(NGob.getDummy(pos, plob.a, plob.ngob.hitBox), true);
            pf.isHardMode = true;
            pf.run(gui);

            gui.map.wdgmsg("place", pos.floor(posres), (int) Math.round((needRotate ? Math.PI / 2 : 0) * 32768 / Math.PI), 1, 0);
            NUtils.addTask(new WaitConstructionObject(pos));
            NUtils.addTask(new WaitWindow(command.name));

            Gob gob;
            do {
                if(needRefill(curings)) {
                    if(!refillIng(gui, curings))
                        return Results.ERROR("NO ITEMS");
                    gob = Finder.findGob(pos);
                    if(gob == null)
                        return Results.ERROR("Something went wrong, no gob");
                    new PathFinder(gob).run(gui);
                    NUtils.rclickGob(gob);
                    NUtils.addTask(new WaitWindow(command.name));
                }

                NUtils.startBuild(NUtils.getGameUI().getWindow(command.name));

                NUtils.addTask(new NTask() {
                    int count = 0;
                    @Override
                    public boolean check() {
                        return NUtils.getGameUI().prog != null || count++ > 100;
                    }
                });

                WaitBuildState wbs = new WaitBuildState();
                NUtils.addTask(wbs);
                if(wbs.getState() == WaitBuildState.State.TIMEFORDRINK) {
                    if(!(new Drink(0.9, false).run(gui)).IsSuccess())
                        return Results.ERROR("Drink is not found");
                } else if (wbs.getState() == WaitBuildState.State.DANGER) {
                    return Results.ERROR("Low energy");
                }
            } while ((gob = Finder.findGob(pos)) != null && NParser.checkName(gob.ngob.name, "gfx/terobjs/consobj"));

            Coord2d finalPos = pos;
            final Gob[] targetGob = {null};
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    return (targetGob[0] = Finder.findGob(finalPos)) != null;
                }
            });

            if(targetGob[0] != null) {
                Coord tile = targetGob[0].rc.floor(MCache.tilesz);
                tileCount.put(tile, tileCount.getOrDefault(tile, 0) + 1);

                Coord2d shift = targetGob[0].rc.sub(NUtils.player().rc).norm().mul(4);
                new GoTo(NUtils.player().rc.sub(shift)).run(gui);
            }

            for(Build.Ingredient ingredient: curings) {
                NUtils.addTask(new WaitItems(NUtils.getGameUI().getInventory(), ingredient.name, ingredient.left));
            }

        } while (pos != null);

        return Results.SUCCESS();
    }

    private Coord2d findFreePlaceWithLimit(Pair<Coord2d, Coord2d> area, NHitBox hitBox, HashMap<Coord, Integer> tileCount) {
        ArrayList<NHitBoxD> significantGobs = new ArrayList<>();
        NHitBoxD chekerOfArea = new NHitBoxD(area.a, area.b);

        NHitBoxD temporalGobBox = new NHitBoxD(hitBox.begin, hitBox.end, Coord2d.of(0), 0);
        if(chekerOfArea.c[2].sub(chekerOfArea.c[0]).x < temporalGobBox.getCircumscribedBR().sub(temporalGobBox.getCircumscribedUL()).x ||
                chekerOfArea.c[2].sub(chekerOfArea.c[0]).y < temporalGobBox.getCircumscribedBR().sub(temporalGobBox.getCircumscribedUL()).y)
            return null;

        synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                if (!(gob instanceof OCache.Virtual || gob.attr.isEmpty() || gob.getClass().getName().contains("GlobEffector")))
                    if(gob.ngob.hitBox != null && gob.getattr(Following.class) == null && gob.id != NUtils.player().id) {
                        NHitBoxD gobBox = new NHitBoxD(gob);
                        if (gobBox.intersects(chekerOfArea, true))
                            significantGobs.add(gobBox);
                    }
            }
        }

        Coord inchMax = area.b.sub(area.a).floor();
        Coord margin = hitBox.end.sub(hitBox.begin).floor(2, 2);

        for (int i = margin.x; i <= inchMax.x - margin.x; i++) {
            for (int j = margin.y; j <= inchMax.y - margin.y; j++) {
                Coord2d testPos = area.a.add(i, j);
                Coord tile = testPos.floor(MCache.tilesz);

                if(tileCount.getOrDefault(tile, 0) >= TRELLIS_PER_TILE) {
                    continue;
                }

                boolean passed = true;
                NHitBoxD testGobBox = new NHitBoxD(hitBox.begin, hitBox.end, testPos, 0);
                for (NHitBoxD significantHitbox : significantGobs) {
                    if(significantHitbox.intersects(testGobBox, false))
                        passed = false;
                }

                if(passed)
                    return Coord2d.of(testGobBox.rc.x, testGobBox.rc.y);
            }
        }
        return null;
    }

    private boolean refillIng(NGameUI gui, ArrayList<Build.Ingredient> curings) throws InterruptedException {
        for(Build.Ingredient ingredient: curings) {
            if(ingredient.specialWay == null) {
                if(!ingredient.containers.isEmpty()) {
                    for (Container container : ingredient.containers) {
                        Container.Space space = container.getattr(Container.Space.class);
                        if(space.isReady()) {
                            int aval = (int) space.getRes().get(Container.Space.MAXSPACE) - (int) space.getRes().get(Container.Space.FREESPACE);
                            if (aval != 0) {
                                new PathFinder(Finder.findGob(container.gobid)).run(gui);
                                new OpenTargetContainer(container).run(gui);
                                TakeAvailableItemsFromContainer tifc = new TakeAvailableItemsFromContainer(container, ingredient.name, ingredient.count);
                                tifc.run(gui);
                                ingredient.count = ingredient.count - tifc.getCount();
                            }
                        } else {
                            new PathFinder(Finder.findGob(container.gobid)).run(gui);
                            new OpenTargetContainer(container).run(gui);
                            TakeAvailableItemsFromContainer tifc = new TakeAvailableItemsFromContainer(container, ingredient.name, ingredient.count);
                            tifc.run(gui);
                            ingredient.count = ingredient.count - tifc.getCount();
                        }

                        if (ingredient.count == 0)
                            break;
                    }
                } else {
                    while (ingredient.count != 0 && NUtils.getGameUI().getInventory().getNumberFreeCoord(ingredient.coord) != 0) {
                        ArrayList<Gob> piles = Finder.findGobs(ingredient.area, new NAlias("stockpile"));
                        if (piles.isEmpty()) {
                            if(NUtils.getGameUI().getInventory().getItems(ingredient.name).size() != ingredient.count)
                                return false;
                        }
                        piles.sort(NUtils.d_comp);
                        if(piles.isEmpty())
                            return false;
                        Gob pile = piles.get(0);
                        new PathFinder(pile).run(NUtils.getGameUI());
                        new OpenTargetContainer("Stockpile", pile).run(NUtils.getGameUI());
                        TakeItemsFromPile tifp;
                        (tifp = new TakeItemsFromPile(pile, NUtils.getGameUI().getStockpile(), Math.min(ingredient.count, NUtils.getGameUI().getInventory().getNumberFreeCoord(ingredient.coord)))).run(gui);
                        new CloseTargetWindow(NUtils.getGameUI().getWindow("Stockpile")).run(gui);
                        ingredient.count = ingredient.count - tifp.getResult();
                    }
                }
            }
        }
        return !needRefill(curings);
    }

    boolean needRefill(ArrayList<Build.Ingredient> curings) throws InterruptedException {
        boolean needRefill = false;
        for(Build.Ingredient ingredient: curings) {
            if(ingredient.count > 0) {
                int size = NUtils.getGameUI().getInventory().getItems(ingredient.name).size();
                if (size > 0) {
                    return false;
                } else {
                    needRefill = true;
                }
            }
        }
        return needRefill;
    }
}
