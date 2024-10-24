package nurgling.actions;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NArea;

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
        NArea test;
        for(NArea.Specialisation s: req)
        {
            if(s.subtype!=null)
            {
                if((test = NArea.findSpec(s.name,s.subtype))==null || test.getRCArea() == null)
                {
                    return Results.ERROR("Area " + s.name + " ( " + s.subtype + " ) required, but not found!");
                }
            }
            else
            {

                if((test = NArea.findSpec(s.name))==null || test.getRCArea() == null)
                {
                    return Results.ERROR("Area " + s.name + " required, but not found!");
                }
            }
        }

        for(NArea.Specialisation s: opt)
        {
            if(s.subtype!=null) {
                if ((test = NArea.findSpec(s.name,s.subtype))==null || test.getRCArea() == null) {
                    NUtils.getGameUI().msg("Optional area " + s.name + " ( " + s.subtype + " ) not found.");
                }
            }
            else
            {
                if((test = NArea.findSpec(s.name))==null || test.getRCArea() == null)
                {
                    NUtils.getGameUI().msg("Optional area " + s.name +" not found.");
                }
            }
        }
        return Results.SUCCESS();
    }
}
