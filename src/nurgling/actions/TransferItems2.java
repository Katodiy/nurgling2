package nurgling.actions;

import haven.WItem;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NContext;
import nurgling.tools.Container;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

public class TransferItems2 implements Action
{
    final NContext cnt;
    HashSet<String> items;

    static HashSet<String> orderList = new HashSet<>();
    static {
        orderList.add("Moose Antlers");
        orderList.add("Flipper Bones");
        orderList.add("Red Deer Antlers");
        orderList.add("Wolf's Claws");
        orderList.add("Bear Tooth");
        orderList.add("Lynx Claws");
        orderList.add("Boar Tusk");
        orderList.add("Billygoat Horn");
        orderList.add("Bog Turtle Shell");
        orderList.add("Boreworm Beak");
        orderList.add("Cachalot Tooth");
        orderList.add("Roe Deer Antlers");
        orderList.add("Wildgoat Horn");
        orderList.add("Mole's Pawbone");
        orderList.add("Orca Tooth");
        orderList.add("Adder Skeleton");
        orderList.add("Ant Chitin");
        orderList.add("Bee Chitin");
        orderList.add("Mammoth Tusk");
        orderList.add("Cave Louse Chitin");
        orderList.add("Crabshell");
        orderList.add("Trollbone");
        orderList.add("Walrus Tusk");
        orderList.add("Troll Tusks");
        orderList.add("Whale Bone Material");
        orderList.add("Wishbone");
    }

    public TransferItems2(NContext context, HashSet<String> items)
    {
        this.cnt = context;
        this.items = items;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        ArrayList<String> before = new ArrayList<>();
        ArrayList<String> after = new ArrayList<>();
        for (String item : items)
        {
            if(orderList.contains(item))
            {
                before.add(item);
            }
            else
            {
                after.add(item);
            }
        }
        ArrayList<String> resitems = new ArrayList<>();
        resitems.addAll(before);
        resitems.addAll(after);
        for(String item : resitems) {
            TreeMap<Double,String> areas = cnt.getOutAreas(item);
            for (Double key : areas.descendingKeySet()) {
                ArrayList<WItem> items = NUtils.getGameUI().getInventory().getItems(new NAlias(item), key);
                if(!items.isEmpty())
                {
                    for (NContext.ObjectStorage output : cnt.getOutStorages(item,key))
                    {
                        if (output instanceof NContext.Pile) {
                            new TransferToPiles(cnt.getRCArea(areas.get(key)), new NAlias(item), key.intValue()).run(gui);
                        }
                        if (output instanceof Container) {
                            new TransferToContainer((Container) output, new NAlias(item), key.intValue()).run(gui);
                        }
                        if(output instanceof NContext.Barrel)
                        {
                            new TransferToBarrel(Finder.findGob(((NContext.Barrel) output).barrel),new NAlias(item)).run(gui);
                        }
//                        if (output instanceof NContext.Barter) {
//                            if (((NContext.Barter) output).getArea() != null)
//                                new TransferToBarter(((NContext.Barter) output), new NAlias(item), key.intValue()).run(gui);
//                        }
                    }
                }
            }
        }

        return Results.SUCCESS();
    }

}
