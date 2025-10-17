package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.overlays.NCustomBauble;
import nurgling.overlays.TrellisGhostPreview;
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

            Gob player = null;
            SelectAreaWithRotation buildarea = null;

            try {
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

                // Properly cancel the placement cursor - we'll restart it later
                if (NUtils.getGameUI().map.placing != null) {
                    plob.delattr(ResDrawable.class);
                    NUtils.getGameUI().map.placing.cancel();
                    NUtils.getGameUI().map.placing = null;
                }

                // First select build area with ghost previews
                NUtils.getGameUI().msg("Please, select build area and choose direction");
                buildarea = new SelectAreaWithRotation(Resource.loadsimg("baubles/buildArea"), trellisHitBox);
                buildarea.run(NUtils.getGameUI());

                Pair<Coord2d, Coord2d> area = buildarea.getRCArea();
                int orientation = buildarea.orientation;
                // Rotate for EW orientations: 2, 3, and 5
                boolean needRotate = (orientation == 2 || orientation == 3 || orientation == 5);

                // Now select resource areas
                NUtils.getGameUI().msg("Please, select area for blocks");
                SelectArea blockarea = new SelectArea(Resource.loadsimg("baubles/blockIng"));
                blockarea.run(NUtils.getGameUI());
                command.ingredients.add(new Build.Ingredient(new Coord(1,2), blockarea.getRCArea(), new NAlias("Block"), 3));

                NUtils.getGameUI().msg("Please, select area for strings");
                SelectArea stringarea = new SelectArea(Resource.loadsimg("baubles/stringsIng"));
                stringarea.run(NUtils.getGameUI());
                command.ingredients.add(new Build.Ingredient(new Coord(1,1), stringarea.getRCArea(), new NAlias("Flax Fibres", "Hemp Fibres", "Spindly Taproot", "Cattail Fibres", "Stinging Nettle", "Hide Strap", "Straw Twine", "Bark Cordage"), 1));

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

                pos = findFreePlaceWithLimit(area, needRotate ? plob.ngob.hitBox.rotate() : plob.ngob.hitBox, tileCount, orientation);

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
        } finally {
            // Always clean up ghost previews when bot finishes or is interrupted
            player = NUtils.player();
            if (player != null) {
                // Remove ghost preview overlay
                Gob.Overlay ghostOverlay = player.findol(TrellisGhostPreview.class);
                if (ghostOverlay != null) {
                    ghostOverlay.remove();
                }

                // Remove custom bauble overlay
                Gob.Overlay baubleOverlay = player.findol(NCustomBauble.class);
                if (baubleOverlay != null) {
                    baubleOverlay.remove();
                }
            }

            // Clean up area selection mode
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
                ((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.set(false);
            }
        }
    }

    private Coord2d findFreePlaceWithLimit(Pair<Coord2d, Coord2d> area, NHitBox hitBox, HashMap<Coord, Integer> tileCount, int orientation) {
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

            // Calculate which tiles are in the area
            Coord tileBegin = area.a.floor(MCache.tilesz);
            Coord tileEnd = area.b.sub(1, 1).floor(MCache.tilesz);

            // Width and length of hitbox (for tight packing)
            // Add small spacing (0.1 units) to avoid collision detection with already-placed trellises
            double hitboxWidth = hitBox.end.x - hitBox.begin.x + 0.1;
            double hitboxLength = hitBox.end.y - hitBox.begin.y + 0.1;

            // Calculate total width/length occupied by 3 trellises for centering
            double totalNSWidth = hitboxWidth * 3;
            double totalEWLength = hitboxLength * 3;

            // Iterate tile by tile to ensure proper alignment
            for (int tx = tileBegin.x; tx <= tileEnd.x; tx++) {
                for (int ty = tileBegin.y; ty <= tileEnd.y; ty++) {
                    Coord tile = new Coord(tx, ty);

                    // How many trellises already placed in this tile?
                    int currentCount = tileCount.getOrDefault(tile, 0);

                    // Skip tiles that already have enough trellises
                    if(currentCount >= TRELLIS_PER_TILE) {
                        continue;
                    }

                    // Calculate the bounds for this specific tile
                    Coord2d tileStart = tile.mul(MCache.tilesz);

                    // Calculate position for next trellis based on orientation and current count
                    // Place them touching each other
                    Coord2d testPos;

                    if (orientation == 0) {
                        // NS-East: pack from right edge, centered on that edge, vertical orientation
                        double centerOffsetY = (MCache.tilesz.y - hitboxLength) / 2.0;
                        testPos = tileStart.add(
                            MCache.tilesz.x - hitBox.end.x - (currentCount * hitboxWidth),
                            -hitBox.begin.y + centerOffsetY
                        );
                    } else if (orientation == 1) {
                        // NS-West: pack from left edge, centered on that edge, vertical orientation
                        double centerOffsetY = (MCache.tilesz.y - hitboxLength) / 2.0;
                        testPos = tileStart.add(
                            -hitBox.begin.x + (currentCount * hitboxWidth),
                            -hitBox.begin.y + centerOffsetY
                        );
                    } else if (orientation == 2) {
                        // EW-North: pack from top edge, centered on that edge, horizontal orientation
                        double centerOffsetX = (MCache.tilesz.x - hitboxWidth) / 2.0;
                        testPos = tileStart.add(
                            -hitBox.begin.x + centerOffsetX,
                            -hitBox.begin.y + (currentCount * hitboxLength)
                        );
                    } else if (orientation == 3) {
                        // EW-South: pack from bottom edge, centered on that edge, horizontal orientation
                        double centerOffsetX = (MCache.tilesz.x - hitboxWidth) / 2.0;
                        testPos = tileStart.add(
                            -hitBox.begin.x + centerOffsetX,
                            MCache.tilesz.y - hitBox.end.y - (currentCount * hitboxLength)
                        );
                    } else if (orientation == 4) {
                        // NS-Center: all 3 trellises centered in the tile, vertical orientation
                        double startX = (MCache.tilesz.x - totalNSWidth) / 2.0;
                        double centerOffsetY = (MCache.tilesz.y - hitboxLength) / 2.0;
                        testPos = tileStart.add(
                            -hitBox.begin.x + startX + (currentCount * hitboxWidth),
                            -hitBox.begin.y + centerOffsetY
                        );
                    } else {
                        // EW-Center (orientation == 5): all 3 trellises centered in the tile, horizontal orientation
                        double centerOffsetX = (MCache.tilesz.x - hitboxWidth) / 2.0;
                        double startY = (MCache.tilesz.y - totalEWLength) / 2.0;
                        testPos = tileStart.add(
                            -hitBox.begin.x + centerOffsetX,
                            -hitBox.begin.y + startY + (currentCount * hitboxLength)
                        );
                    }

                    // Check collisions with existing objects (not with our own trellises)
                    NHitBoxD testGobBox = new NHitBoxD(hitBox.begin, hitBox.end, testPos, 0);
                    boolean passed = true;
                    for (NHitBoxD significantHitbox : significantGobs) {
                        if(significantHitbox.intersects(testGobBox, false)) {
                            passed = false;
                            break;
                        }
                    }

                    if(passed) {
                        // Calculate center position of the hitbox at testPos
                        double centerX = testPos.x + (hitBox.end.x + hitBox.begin.x) / 2.0;
                        double centerY = testPos.y + (hitBox.end.y + hitBox.begin.y) / 2.0;
                        return new Coord2d(centerX, centerY);
                    }
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
