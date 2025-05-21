package nurgling.actions.bots;

import haven.Gob;
import haven.WItem;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.tasks.FindNInventory;
import nurgling.tasks.WaitForBurnout;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NEquipory;
import nurgling.widgets.Specialisation;
import space.dynomake.libretranslate.Language;
import space.dynomake.libretranslate.Translator;

import java.util.ArrayList;
import java.util.Optional;

public class TestAction implements Action {
    String cap = "Cauldron";
    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        WItem bucket = NUtils.getEquipment().findBucket("Pickling Brine");
        if(bucket != null){
            String a = ((NGItem) bucket.item).content().get(0).name();
            gui.msg("Bucker found : " + a);
        }
        NUtils.getEquipment();
        NInventory i = gui.getInventory();
        ArrayList<WItem> banki = i.getItems("Pickling jar");
        gui.msg("banki size: " + banki.size());
        for(WItem banka : banki){
            ArrayList<NGItem.NContent> items = ((NGItem) banka.item).content();
            if(!items.isEmpty()){
                gui.msg("banka liquid content:" + items.getFirst().name());
            }
        }
        NGItem ngi = ((NGItem) banki.getFirst().item);

        NInventory banka_inv = (NInventory)ngi.contents;
        ArrayList<WItem> banka_items = banka_inv.getItems();
        gui.msg("Banka inv size: " + banka_items.size());
        for (int j = 0; j < banka_items.size(); j++) {

            Optional<Double> drying = ((NWItem) banka_items.get(j)).getDryingProgress();

            String progress = drying
                    .map(val -> "progress: " + (int)(val * 100) + "%")
                    .orElse("progress: unknown");

            gui.msg("Percent of " + ((NGItem) banka_items.get(j).item).name() + " : " + progress);
        }

//
        return Results.SUCCESS();
    }
}
