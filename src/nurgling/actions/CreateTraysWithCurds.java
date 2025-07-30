package nurgling.actions;

import haven.GItem;
import haven.WItem;
import nurgling.*;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;

import java.util.HashSet;

public class CreateTraysWithCurds implements Action {
    private final String curdType = "Cow's Curd";
    private final int count = 4;
    private final String cheeseTrayType = "Cheese Tray";

//    public CreateTraysWithCurds(String curdType, int count) {
//        this.curdType = curdType;
//        this.count = count;
//    }

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NContext context = new NContext(gui);
        int created = 0;

        context.addInItem(curdType, null);
        context.addInItem(cheeseTrayType, null);

        while (created < count) {
            if (gui.getInventory().getItem(new NAlias(cheeseTrayType)) == null) {
                Results traysRes = new TakeItems2(context, cheeseTrayType, 1).run(gui);
                if (!traysRes.isSuccess) {
                    return Results.ERROR("No Cheese Trays available");
                }
            }

            int need = Math.min(4, count - created);

            Results curdsRes = new TakeItems2(context, curdType, need).run(gui);
            int curdsInInv = gui.getInventory().getItems(new NAlias(curdType)).size();
            if (curdsInInv == 0) {
                return Results.ERROR("No more curds available");
            }

            int toUse = Math.min(need, curdsInInv);

            WItem tray = gui.getInventory().getItem(new NAlias(cheeseTrayType));
            for (int i = 0; i < toUse; i++) {
//                new UseItemOnItem(new NAlias(curdType), tray).run(gui);
//                NUtils.waitEvent(() -> gui.getInventory().getItems(new NAlias(curdType)).size() == (curdsInInv - i - 1), 10);
            }

            created += toUse;

            new TransferFilledTraysToOutput(context).run(gui);

            if (toUse == 0) break;
        }

        return Results.SUCCESS();
    }

    static class TransferFilledTraysToOutput implements Action {
        private final NContext context;

        TransferFilledTraysToOutput(NContext context) {
            this.context = context;
        }

        @Override
        public Results run(NGameUI gui) throws InterruptedException {

            HashSet<String> traySet = new HashSet<>();
            traySet.add("Cheese Tray");

            return new TransferItems2(context, traySet).run(gui);
        }
    }
}
