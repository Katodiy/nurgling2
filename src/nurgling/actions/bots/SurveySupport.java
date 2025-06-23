package nurgling.actions.bots;

import haven.*;
import haven.res.ui.relcnt.RelCont;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.RestoreResources;
import nurgling.actions.Results;
import nurgling.tasks.GetCurs;
import nurgling.tasks.NTask;
import nurgling.tasks.WaitPose;
import nurgling.tasks.WaitWindow;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class SurveySupport implements Action
{
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        Gob target = Finder.findGob(new NAlias("survobj"));
        if (target == null) {
            return Results.ERROR("Survey object not found");
        }


        String leftover = null;

        new PathFinder(target.rc).run(gui);
        while (true) {

            if (!new RestoreResources(target.rc).run(gui).IsSuccess()) {
                return Results.FAIL();
            }
            target = Finder.findGob(target.id);
            NUtils.rclickGob(target);
            NUtils.addTask(new WaitWindow("Land survey"));

            Window wnd = NUtils.getGameUI().getWindow("Land survey");
            if (wnd != null) {
                for (Widget child : wnd.children()) {
                    if (child instanceof Label) {
                        Label cand = (Label) child;
                        NUtils.addTask(new NTask() {
                            @Override
                            public boolean check() {
                                return !cand.text().equals("...");
                            }
                        });
                        if (cand.text().contains("Units of soil left") || cand.text().contains("Units of soil req") ) {
                            if (leftover == null || !leftover.equals(cand.text())) {
                                leftover = cand.text();
                            } else {
                                return Results.SUCCESS();
                            }
                        }
                    }

                    if (child instanceof Button) {
                        Button button = (Button) child;
                        if (button.text.text.equals("Dig")) {
                            button.click();
                        }
                    }
                }
                Gob player = NUtils.player();
                if (player == null) {
                    return Results.FAIL();
                }
                NUtils.addTask(new NTask() {
                    int counter = 0;

                    @Override
                    public boolean check() {
                        if (player.pose().contains("idle"))
                            counter++;
                        else
                            counter = 0;
                        return counter >= 360 || NUtils.getStamina() < 0.25 || NUtils.getEnergy() < 0.3;
                    }
                });
            }
        }


    }
}
