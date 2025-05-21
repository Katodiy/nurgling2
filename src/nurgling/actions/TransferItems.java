package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.tools.Context;
import nurgling.tools.Container;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TransferItems implements Action
{
    final Context cnt;
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

    public TransferItems(Context context, HashSet<String> items)
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
            TreeMap<Integer, NArea> areas = NArea.findOuts(new NAlias(item));
            if(!areas.isEmpty()) {
                ArrayList<Integer> ths = new ArrayList<>(areas.keySet());
                ListIterator<Integer> listIter = ths.listIterator(areas.size());
                while (listIter.hasPrevious()) {
                    int th = listIter.previous();
                    NArea area = areas.get(th);
                    for (Context.Output out : cnt.GetOutput(item, area))
                        cnt.addOutput(item, th, out);

                    if (cnt.getOutputs(item, th) != null) {
                        for (Context.Output output : cnt.getOutputs(item, th)) {
                            if (output instanceof Context.Pile) {
                                if (((Context.OutputPile) output).getArea() != null)
                                    new TransferToPiles(((Context.OutputPile) output).getArea(), new NAlias(item), th).run(gui);
                            }
                            if (output instanceof Container) {
                                if (((Context.OutputContainer) output).getArea() != null)
                                    if(((Container.Space)((Context.OutputContainer) output).getattr(Container.Space.class))==null || !((Container.Space)((Context.OutputContainer) output).getattr(Container.Space.class)).isReady() || ((Container.Space)((Context.OutputContainer) output).getattr(Container.Space.class)).getFreeSpace()!=0) {
                                        new TransferToContainer(cnt, (Context.OutputContainer) output, new NAlias(item), th).run(gui);
                                    }
                            }
                            if (output instanceof Context.Barter) {
                                if (((Context.OutputBarter) output).getArea() != null)
                                    new TransferToBarter(((Context.OutputBarter) output), new NAlias(item), th).run(gui);
                            }
                            if (output instanceof Context.Barrel) {
                                if (((Context.OutputBarrel) output).getArea() != null)
                                    new TransferToBarrel(((Context.OutputBarrel) output).barrel, new NAlias(item)).run(gui);
                            }
                        }
                    }
                }
            }
            else
            {
                if (cnt.getOutputs(item, 1) != null) {
                    for (Context.Output output : cnt.getOutputs(item, 1)) {
                        if (output instanceof Context.Pile) {
                            if (((Context.OutputPile) output).getArea() != null)
                                new TransferToPiles(((Context.OutputPile) output).getArea(), new NAlias(item), 1).run(gui);
                        }
                        if (output instanceof Container) {
                            if (((Context.OutputContainer) output).getArea() != null)
                                new TransferToContainer(cnt, (Context.OutputContainer) output, new NAlias(item), 1).run(gui);
                        }
                        if (output instanceof Context.Barter) {
                            if (((Context.OutputBarter) output).getArea() != null)
                                new TransferToBarter(((Context.OutputBarter) output), new NAlias(item), 1).run(gui);
                        }
                    }
                }
            }

        }




        return Results.SUCCESS();
    }



}
