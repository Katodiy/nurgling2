package nurgling.actions;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tasks.HandIsFree;
import nurgling.tasks.WaitTargetSize;
import nurgling.tools.Container;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.HashMap;

public class WaterToContainers implements Action
{

    ArrayList<Container> conts;
    Coord targetCoord = new Coord(1, 1);

    public WaterToContainers(ArrayList<Container> conts) {
        this.conts = conts;
    }



    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        HashMap<String, Integer> neededWater = new HashMap<>();
        for (Container cont : conts) {
            Container.WaterLvl watLvl = cont.getattr(Container.WaterLvl.class);

            neededWater.put(Container.WaterLvl.WATERLVL, watLvl.neededWater());
        }
        for (Container cont : conts) {
            Container.WaterLvl watLvl = cont.getattr(Container.WaterLvl.class);
            if (watLvl.neededWater() != 0) {

                new UseWorkStationNC(cont.gob).run(gui);
                new OpenTargetContainer(cont).run(gui);
                watLvl = cont.getattr(Container.WaterLvl.class);

                int watah = watLvl.neededWater();
                gui.msg(String.valueOf(watah));

                Gob cistern = Finder.findGob(NArea.findSpec(Specialisation.SpecName.water_refiller.toString()), new NAlias("cistern"));
                Gob barrel = Finder.findGob(NArea.findSpec(Specialisation.SpecName.water_refiller.toString()), new NAlias("barrel"));
                //gui.msg("Watered the container!");
                //найти зону с водой
                //найти в зоне с водой бочку
                //заглянуть в бочку
                //если меньше чем watah - открыть цистерну и проверить достаточно ли там
                //если достаточно то ПКМ по цистерне с пасфайндером
                //потом пасфайндер к cont
                //пкм по cont
                //пасфайндер обратно к зоне воды
                //поставить бочку на место

//                int aftersize = gui.getInventory().getItems().size() - fuelLvl.neededFuel();
//                for (int i = 0; i < fueled; i++) {
//                    NUtils.takeItemToHand(items.get(i));
//                    NUtils.activateItem(cont.gob);
//                    NUtils.getUI().core.addTask(new HandIsFree(NUtils.getGameUI().getInventory()));
//                }
//                NUtils.getUI().core.addTask(new WaitTargetSize(NUtils.getGameUI().getInventory(), aftersize));
//                new CloseTargetContainer(cont).run(gui);
            }
        }
        return Results.SUCCESS();
    }
}
