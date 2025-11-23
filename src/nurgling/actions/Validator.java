package nurgling.actions;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;

public class Validator implements Action{


    public Validator(ArrayList<NArea.Specialisation> req, ArrayList<NArea.Specialisation> opt)
    {
        this(req);
        this.opt = opt;
    }

    public Validator(ArrayList<NArea.Specialisation> req)
    {
        this.req = req;
    }

    ArrayList<NArea.Specialisation> req;
    ArrayList<NArea.Specialisation> opt;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if(NUtils.getEnergy()<0.22)
        {
            return Results.ERROR("WARNING: LOW ENERGY");
        }
        NArea test;
        for(NArea.Specialisation s: req)
        {
            if(s.subtype!=null)
            {
                if((NContext.findSpecGlobal(s.name,s.subtype))==null)
                {
                    return Results.ERROR("Area " + Specialisation.findSpecialisation(s.name).prettyName + " ( " + s.subtype + " ) required, but not found!");
                }
            }
            else
            {

                if((NContext.findSpecGlobal(s.name))==null)
                {
                    return Results.ERROR("Area " + Specialisation.findSpecialisation(s.name).prettyName + " required, but not found!");
                }
            }
        }

        for(NArea.Specialisation s: opt)
        {
            if(s.subtype!=null) {
                if ((NContext.findSpecGlobal(s.name,s.subtype))==null) {
                    NUtils.getGameUI().msg("Optional area " + Specialisation.findSpecialisation(s.name).prettyName + " ( " + s.subtype + " ) not found.");
                }
            }
            else
            {
                if((NContext.findSpecGlobal(s.name))==null)
                {
                    NUtils.getGameUI().msg("Optional area " + Specialisation.findSpecialisation(s.name).prettyName +" not found.");
                }
            }
        }
        return Results.SUCCESS();
    }
}
