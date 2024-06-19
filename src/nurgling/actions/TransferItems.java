package nurgling.actions;

import haven.WItem;
import nurgling.NGItem;
import nurgling.NGameUI;
import nurgling.areas.NArea;
import nurgling.tools.Context;
import nurgling.tools.Container;
import nurgling.tools.NAlias;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TransferItems implements Action
{
    final Context cnt;
    String item;


    public TransferItems(Context context, String item)
    {
        this.cnt = context;
        this.item = item;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {

        TreeMap<Integer, NArea> areas = NArea.findOuts(item);
        ArrayList<Integer> ths = new ArrayList<>(areas.keySet());
        ListIterator<Integer> listIter = ths.listIterator(areas.size());
        while (listIter.hasPrevious()) {
            int th = listIter.previous();
            NArea area = areas.get(th);
            for(Context.Output out: Context.GetOutput(item, area))
                cnt.addOutput(item,th,out);

            if(cnt.getOutputs(item, th)!=null) {
                for (Context.Output output : cnt.getOutputs(item, th)) {
                    if (output instanceof Context.Pile) {
                        if (((Context.OutputPile) output).getArea() != null)
                            new TransferToPiles(((Context.OutputPile) output).getArea().getRCArea(), new NAlias(item)).run(gui);
                    }
                    if (output instanceof Container) {
                        if (((Context.OutputContainer) output).getArea() != null)
                            new TransferToContainer(cnt, (Context.OutputContainer) output, new NAlias(item) , th).run(gui);
                    }
                    if (output instanceof Context.Barter) {
                        if(((Context.OutputBarter)output).getArea()!=null)
                            new TransferToBarter(((Context.OutputBarter)output),new NAlias(item)).run(gui);
                    }
                }
            }
        }




        return Results.SUCCESS();
    }



}
