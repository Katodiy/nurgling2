package nurgling.actions.bots;

import haven.*;
import haven.res.ui.relcnt.RelCont;
import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.PathFinder;
import nurgling.actions.RestoreResources;
import nurgling.actions.Results;
import nurgling.areas.NContext;
import nurgling.routes.RoutePoint;
import nurgling.tasks.*;
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
        long targetid = target.id;
        new PathFinder(target.rc).run(gui);
        RoutePoint rp = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI());



        String leftover = null;


        while (true) {

            if (!new RestoreResources().run(gui).IsSuccess()) {
                return Results.FAIL();
            }
            if(rp!=null && rp.toCoord2d(NUtils.getGameUI().map.glob.map)==null)
                new RoutePointNavigator(rp).run(gui);
            target = Finder.findGob(targetid);
            if(target == null)
                return Results.ERROR("Survey object not found");
            new PathFinder(target.rc).run(gui);
            NUtils.rclickGob(target);
            NUtils.addTask(new WaitWindow("Land survey"));

            Window wnd = NUtils.getGameUI().getWindow("Land survey");
            if (wnd != null) {
                boolean needRemove = false;
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
                                needRemove = true;
                            }
                        }
                    }

                    if (child instanceof Button) {
                        Button button = (Button) child;
                        if (button.text.text.equals("Dig")) {
                            button.click();
                        }
                        if (needRemove)
                            if (button.text.text.equals("Remove")) {
                                button.click();
                                NUtils.addTask(new WindowIsClosed(wnd));
                                target = Finder.findGob(new NAlias("survobj"));
                                if (target == null) {
                                    return Results.ERROR("Survey object not found");
                                }
                                targetid = target.id;
                                new PathFinder(target.rc).run(gui);
                                rp = ((NMapView) NUtils.getGameUI().map).routeGraphManager.getGraph().findNearestPointToPlayer(NUtils.getGameUI());
                                break;
                            }
                    }
                }
                if(needRemove)
                    continue;
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
