package nurgling.actions;

import nurgling.NGameUI;
import nurgling.tools.Context;
import nurgling.tools.NAlias;

import java.util.concurrent.atomic.AtomicInteger;

public class TransferItems implements Action
{
    final Context cnt;
    String item;
    int count;


    public TransferItems(Context context, String item, int count)
    {
        this.cnt = context;
        this.item = item;
        this.count = count;
    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        AtomicInteger left = new AtomicInteger(count);
        if(cnt.getOutputs(item)!=null) {
            for (Context.Output output : cnt.getOutputs(item)) {
                if (output instanceof Context.Pile) {
                    if (((Context.OutputPile) output).getArea() != null)
                        return new TransferToPiles(((Context.OutputPile) output).getArea().getRCArea(), new NAlias(item)).run(gui);
                }
                if (output instanceof Context.Container) {
                    if (((Context.OutputContainer) output).getArea() != null)
                        return new TransferToContainer(cnt, (Context.OutputContainer) output, new NAlias(item) ).run(gui);
                }
                if (output instanceof Context.Barter) {
//                if(((Context.Container)output).getArea()!=null)
//                    return new TransferToPiles(((Context.OutputPile)output).getArea().getRCArea(),new NAlias(item)).run(gui);
                }

                if (left.get() == 0)
                    return Results.SUCCESS();
            }
        }
        return Results.SUCCESS();
    }

}
