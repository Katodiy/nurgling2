package nurgling.actions.bots;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.actions.Action;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.tools.Context;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Collection;

public class SmelterAction implements Action {

    public static class Smelter extends Context.Container
    {
        public Smelter(Gob gob, Collection<String> names) {
            super(gob, names);
        }

        public static void update(Smelter smelter)
        {
            if(smelter==null)
            {
                
            }
        }
    }
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        NArea smelter = NArea.findSpec(Specialisation.SpecName.smelter.toString());
        Context context = new Context();
        ArrayList<Context.Container> smelterCont = new ArrayList<>();

        for(Gob sm : Finder.findGobs(smelter,new NAlias("gfx/terobjs/smelter")))
        {
            smelterCont.add(new Context.Container(sm, Context.contcaps.getall(sm.ngob.name)));
        }
        context.addConstContainers(smelterCont);

        for(Gob sm : Finder.findGobs(smelter,new NAlias("gfx/terobjs/primsmelter")))
        {
            smelterCont.add(new Context.Container(sm, Context.contcaps.getall(sm.ngob.name)));
        }

//        new OpenTargetContainer()

        return null;
    }
}
