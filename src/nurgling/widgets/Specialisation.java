package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Window;
import nurgling.*;
import nurgling.areas.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Specialisation extends Window
{

    private NArea area = null;

    public Specialisation()
    {
        super(UI.scale(200,500), "Specialisation");
        add(new SpecialisationList(UI.scale(200,500)));
    }
    public enum SpecName
    {
        smelter,
        kiln,
        water,
        boiler,
        swill,
        trough,
        crop,
        seed,
        cows,
        sheeps,
        pigs,
        goats,
        deadkritter,
        ore,
        fuel,
        ovens,
        gardenpot,
        barrel,
        leafs,
        htable,
        rawhides,
        dframe,
        horses,
        ttub,
        tanning,
        logs,
        smokshed,
        tarkiln,
        boneforash,
        blockforash,
        readyHides,
        crucibles,
        chicken,
        incubator,
        bed,
        eat,
        safe,
        sorting,
        fforge,
        anvil,
        rabbit,
        rabbitIncubator,
        dreamcatcher,
        meatgrinder,
        loom,
        ropewalk,
        crucible,
        pow,
        cauldron,
        potterswheel,
        barrelworkarea,
        deer
    }

    private static ArrayList<SpecialisationItem> specialisation = new ArrayList<>();

    static {
        specialisation.add(new SpecialisationItem(SpecName.smelter.toString(),"Smelters",Resource.loadsimg("nurgling/categories/smelter")));
        specialisation.add(new SpecialisationItem(SpecName.kiln.toString(),"Kilns",Resource.loadsimg("nurgling/categories/kiln")));
        specialisation.add(new SpecialisationItem(SpecName.water.toString(),"Source of water",Resource.loadsimg("nurgling/categories/water")));
        specialisation.add(new SpecialisationItem(SpecName.boiler.toString(),"Cauldron",Resource.loadsimg("nurgling/categories/boiler")));
        specialisation.add(new SpecialisationItem(SpecName.swill.toString(),"Swill",Resource.loadsimg("nurgling/categories/swill")));
        specialisation.add(new SpecialisationItem(SpecName.trough.toString(),"Trough for swill",Resource.loadsimg("nurgling/categories/trough")));
        specialisation.add(new SpecialisationItem(SpecName.crop.toString(),"Crop",Resource.loadsimg("nurgling/categories/crop")));
        specialisation.add(new SpecialisationItem(SpecName.seed.toString(),"Seeds of crop",Resource.loadsimg("nurgling/categories/seed")));
        specialisation.add(new SpecialisationItem(SpecName.cows.toString(),"Cows",Resource.loadsimg("nurgling/categories/cows")));
        specialisation.add(new SpecialisationItem(SpecName.goats.toString(),"Goats",Resource.loadsimg("nurgling/categories/goats")));
        specialisation.add(new SpecialisationItem(SpecName.sheeps.toString(),"Sheep",Resource.loadsimg("nurgling/categories/sheeps")));
        specialisation.add(new SpecialisationItem(SpecName.deadkritter.toString(),"Animal carcasses",Resource.loadsimg("nurgling/categories/deadkritter")));
        specialisation.add(new SpecialisationItem(SpecName.pigs.toString(),"Pigs",Resource.loadsimg("nurgling/categories/pigs")));
        specialisation.add(new SpecialisationItem(SpecName.horses.toString(),"Horses",Resource.loadsimg("nurgling/categories/horses")));
        specialisation.add(new SpecialisationItem(SpecName.ore.toString(),"Piles of ore",Resource.loadsimg("nurgling/categories/ores")));
        specialisation.add(new SpecialisationItem(SpecName.fuel.toString(),"Fuel",Resource.loadsimg("nurgling/categories/fuel")));
        specialisation.add(new SpecialisationItem(SpecName.barrel.toString(),"Barrel",Resource.loadsimg("nurgling/categories/barrel")));
        specialisation.add(new SpecialisationItem(SpecName.ovens.toString(),"Ovens",Resource.loadsimg("nurgling/categories/ovens")));
        specialisation.add(new SpecialisationItem(SpecName.crucibles.toString(),"Steelbox",Resource.loadsimg("nurgling/categories/stell")));
        specialisation.add(new SpecialisationItem(SpecName.gardenpot.toString(),"Ready Garden pots",Resource.loadsimg("nurgling/categories/gardenpot")));
        specialisation.add(new SpecialisationItem(SpecName.leafs.toString(),"Piles of leaf",Resource.loadsimg("nurgling/categories/leafs")));
        specialisation.add(new SpecialisationItem(SpecName.htable.toString(),"Herbalist tables",Resource.loadsimg("nurgling/categories/htable")));
        specialisation.add(new SpecialisationItem(SpecName.dframe.toString(),"Drying frames",Resource.loadsimg("nurgling/categories/dframe")));
        specialisation.add(new SpecialisationItem(SpecName.rawhides.toString(),"Piles of raw hides",Resource.loadsimg("nurgling/categories/rawhide")));
        specialisation.add(new SpecialisationItem(SpecName.readyHides.toString(),"Piles of ready hides",Resource.loadsimg("nurgling/categories/readyhides")));
        specialisation.add(new SpecialisationItem(SpecName.ttub.toString(),"Tanning tubs",Resource.loadsimg("nurgling/categories/ttub")));
        specialisation.add(new SpecialisationItem(SpecName.tanning.toString(),"Source of tanning fluid",Resource.loadsimg("nurgling/categories/tanning")));
        specialisation.add(new SpecialisationItem(SpecName.smokshed.toString(),"Smoked sheds",Resource.loadsimg("nurgling/categories/smokshed")));
        specialisation.add(new SpecialisationItem(SpecName.tarkiln.toString(),"Tarkilns",Resource.loadsimg("nurgling/categories/tarkiln")));
        specialisation.add(new SpecialisationItem(SpecName.boneforash.toString(),"Bones for Ash",Resource.loadsimg("nurgling/categories/boneash")));
        specialisation.add(new SpecialisationItem(SpecName.blockforash.toString(),"Block for Ash",Resource.loadsimg("nurgling/categories/block")));
        specialisation.add(new SpecialisationItem(SpecName.chicken.toString(),"Chicken",Resource.loadsimg("nurgling/categories/chicken")));
        specialisation.add(new SpecialisationItem(SpecName.rabbit.toString(),"Rabbit",Resource.loadsimg("nurgling/categories/rabbit_buck")));
        specialisation.add(new SpecialisationItem(SpecName.incubator.toString(),"Chick Incubator",Resource.loadsimg("nurgling/categories/cincub")));
        specialisation.add(new SpecialisationItem(SpecName.bed.toString(),"Bed",Resource.loadsimg("nurgling/categories/bed")));
        specialisation.add(new SpecialisationItem(SpecName.eat.toString(),"Eating area",Resource.loadsimg("nurgling/categories/eat")));
        specialisation.add(new SpecialisationItem(SpecName.rabbitIncubator.toString(),"Rabbit Incubator",Resource.loadsimg("nurgling/categories/bunny")));
        specialisation.add(new SpecialisationItem(SpecName.safe.toString(),"Safe area",Resource.loadsimg("nurgling/categories/safety")));
        specialisation.add(new SpecialisationItem(SpecName.sorting.toString(),"Sorting area",Resource.loadsimg("nurgling/categories/sorting")));
        specialisation.add(new SpecialisationItem(SpecName.fforge.toString(),"Finery Forge",Resource.loadsimg("nurgling/categories/fineryforge")));
        specialisation.add(new SpecialisationItem(SpecName.anvil.toString(),"Anvil",Resource.loadsimg("nurgling/categories/anvil")));
        specialisation.add(new SpecialisationItem(SpecName.dreamcatcher.toString(),"Dream Catcher",Resource.loadsimg("nurgling/categories/anvil")));
        specialisation.add(new SpecialisationItem(SpecName.meatgrinder.toString(),"Meat Grinder",Resource.loadsimg("nurgling/categories/anvil")));
        specialisation.add(new SpecialisationItem(SpecName.loom.toString(),"Loom",Resource.loadsimg("nurgling/categories/anvil")));
        specialisation.add(new SpecialisationItem(SpecName.ropewalk.toString(),"Rope Walk",Resource.loadsimg("nurgling/categories/anvil")));
        specialisation.add(new SpecialisationItem(SpecName.crucible.toString(),"Crucible",Resource.remote().loadwait("paginae/bld/crucible").flayer(Resource.imgc).img));
        specialisation.add(new SpecialisationItem(SpecName.pow.toString(),"Fire Place",Resource.loadsimg("nurgling/categories/anvil")));
        specialisation.add(new SpecialisationItem(SpecName.potterswheel.toString(),"Potters Wheel",Resource.loadsimg("nurgling/categories/anvil")));
        specialisation.add(new SpecialisationItem(SpecName.barrelworkarea.toString(),"Craft area with barrels",Resource.loadsimg("nurgling/categories/anvil")));
        specialisation.add(new SpecialisationItem(SpecName.deer.toString(),"Deer",Resource.loadsimg("nurgling/categories/horses")));

        specialisation.sort(new Comparator<SpecialisationItem>() {
            @Override
            public int compare(SpecialisationItem o1, SpecialisationItem o2) {
                return o1.prettyName.compareTo(o2.prettyName);
            }
        });
    }

    public static SpecialisationItem findSpecialisation(String name)
    {
        for(SpecialisationItem specialisationItem : specialisation)
            if(specialisationItem.name.contains(name))
                return specialisationItem;
        return null;
    }

    public class SpecialisationList extends SListBox<SpecialisationItem, Widget> {
        SpecialisationList(Coord sz) {
            super(sz, UI.scale(24));
        }

        @Override
        public void change(SpecialisationItem item)
        {
            super.change(item);
        }

        protected List<SpecialisationItem> items() {return new ArrayList<>(specialisation);}

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(sz.x, sz.y));
        }

        protected Widget makeitem(SpecialisationItem item, int idx, Coord sz) {
            return(new ItemWidget<SpecialisationItem>(this, sz, item) {
                {
                    //item.resize(new Coord(searchF.sz.x - removei[0].sz().x  + UI.scale(4), item.sz.y));
                    add(item);
                }

                public boolean mousedown(MouseDownEvent ev) {
                    super.mousedown(ev);

                    String value = item.name;
                    boolean isFound = false;
                    for(NArea.Specialisation s: area.spec)
                    {
                        if(s.name.equals(item.name))
                            isFound = true;
                    }
                    if(!isFound)
                    {
                        area.spec.add(new NArea.Specialisation(value));
                        NConfig.needAreasUpdate();
                        NUtils.getGameUI().areas.loadSpec(area.id);
                        Specialisation.this.hide();
                    }
                    else
                    {
                        NUtils.getGameUI().error("Specialisation already selected.");
                    }
                    return(true);
                }
            });
        }

        @Override
        public void wdgmsg(String msg, Object... args)
        {
            super.wdgmsg(msg, args);
        }

        Color bg = new Color(30,40,40,160);

        @Override
        public void draw(GOut g)
        {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }


    }

    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if(msg.equals("close"))
        {
            hide();
        }
        else
        {
            super.wdgmsg(msg, args);
        }
    }

    public static class SpecialisationItem extends Widget
    {
        public Label text;
        public String name;
        public String prettyName;
        public BufferedImage image;
        private TexI tex;
        public SpecialisationItem(String text, String prettyName, BufferedImage image)
        {
            this.text = add(new Label(prettyName), UI.scale(30, 4));
            this.name = text;
            this.prettyName = prettyName;
            this.image = image;
            tex = new TexI(image);
            pack();
            sz.y = UI.scale(24);
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
            g.image(tex,Coord.z,UI.scale(24,24));
        }
    }

    public static void selectSpecialisation(NArea area)
    {
        NUtils.getGameUI().spec.show();
        NUtils.getGameUI().setfocus(NUtils.getGameUI().spec);
        NUtils.getGameUI().spec.raise();
        NUtils.getGameUI().spec.area = area;
    }
}
