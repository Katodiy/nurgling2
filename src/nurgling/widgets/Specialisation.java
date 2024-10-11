package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Window;
import nurgling.*;
import nurgling.areas.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

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
        swill,
        trough,
        crop,
        seed,
        cows,
        sheeps,
        pigs,
        goats,
        deadkritter,
        water_refiller,
        ore,
        fuel,
        ovens,
        gardenpot, barrel,
        leafs, htable
    }

    private static ArrayList<SpecialisationItem> specialisation = new ArrayList<>();

    static {
        specialisation.add(new SpecialisationItem(SpecName.smelter.toString()));
        specialisation.add(new SpecialisationItem(SpecName.kiln.toString()));
        specialisation.add(new SpecialisationItem(SpecName.water.toString()));
        specialisation.add(new SpecialisationItem(SpecName.swill.toString()));
        specialisation.add(new SpecialisationItem(SpecName.trough.toString()));
        specialisation.add(new SpecialisationItem(SpecName.crop.toString()));
        specialisation.add(new SpecialisationItem(SpecName.seed.toString()));
        specialisation.add(new SpecialisationItem(SpecName.cows.toString()));
        specialisation.add(new SpecialisationItem(SpecName.goats.toString()));
        specialisation.add(new SpecialisationItem(SpecName.sheeps.toString()));
        specialisation.add(new SpecialisationItem(SpecName.deadkritter.toString()));
        specialisation.add(new SpecialisationItem(SpecName.pigs.toString()));
        specialisation.add(new SpecialisationItem(SpecName.water_refiller.toString()));
        specialisation.add(new SpecialisationItem(SpecName.ore.toString()));
        specialisation.add(new SpecialisationItem(SpecName.fuel.toString()));
        specialisation.add(new SpecialisationItem(SpecName.barrel.toString()));
        specialisation.add(new SpecialisationItem(SpecName.ovens.toString()));
        specialisation.add(new SpecialisationItem(SpecName.gardenpot.toString()));
        specialisation.add(new SpecialisationItem(SpecName.leafs.toString()));
        specialisation.add(new SpecialisationItem(SpecName.htable.toString()));
    }

    public class SpecialisationList extends SListBox<SpecialisationItem, Widget> {
        SpecialisationList(Coord sz) {
            super(sz, UI.scale(15));
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

                public boolean mousedown(Coord c, int button) {
                    super.mousedown(c, button);

                    String value = item.text.text();
                    boolean isFound = false;
                    for(NArea.Specialisation s: area.spec)
                    {
                        if(s.name.equals(item.text.text()))
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
        Label text;


        public SpecialisationItem(String text)
        {
            this.text = add(new Label(text));
            pack();
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
