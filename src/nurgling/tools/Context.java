package nurgling.tools;

import haven.*;
import nurgling.areas.*;

import java.util.*;

public class Context
{

    public ArrayList<Input> getInputs(String name)
    {
        return input.get(name);
    }

    static HashMap<String, String> equip_map;
    static {
        equip_map = new HashMap<>();
    }

    static HashMap<String, Workstation> workstation_map;
    static {
        workstation_map = new HashMap<>();
        workstation_map.put("paginae/bld/meatgrinder",new Workstation("gfx/terobjs/meatgrinder", "gfx/borka/idle"));
    }
    public void addTools(List<Indir<Resource>> tools)
    {
        for (Indir<Resource> res : tools)
        {
            String equip_cand = equip_map.get(res.get().name);
            if(equip_cand!=null)
            {
                equip = equip_cand;
            }
            Workstation workstation_cand = workstation_map.get(res.get().name);
            if(workstation_cand!=null)
            {
                workstation = workstation_cand;
            }
        }
    }

    public String equip = null;
    public Workstation workstation = null;

    public static class Workstation
    {
        public String station;
        public String pose;

        public Workstation(String station, String pose)
        {
            this.station = station;
            this.pose = pose;
        }
    }
    public static class Barter
    {
        public Gob barter;
        public Gob chest;

        public Barter(Gob barter, Gob chest)
        {
            this.barter = barter;
            this.chest = chest;
        }
    }

    public interface Input
    {

    }

    public static class InputBarter extends Barter implements Input
    {
        public InputBarter(Gob barter, Gob chest)
        {
            super(barter, chest);
        }
    }

    public static ArrayList<Input> GetInput(String item, NArea area ) throws InterruptedException
    {
        ArrayList<Input> inputs = new ArrayList<>();
        NArea.Ingredient ingredient = area.getInput(item);
        switch (ingredient.type)
        {
            case BARTER:
                inputs.add(new InputBarter( Finder.findGob(area, new NAlias("gfx/terobjs/barterstand")),
                                            Finder.findGob(area, new NAlias("gfx/terobjs/chest"))));
                break;
        }
        return inputs;
    }

    public interface Output
    {

    }

    public class OutputBarter extends Barter implements Output
    {
        public OutputBarter(Gob barter, Gob chest)
        {
            super(barter, chest);
        }
    }

    HashMap<String,ArrayList<Input>> input = new HashMap<>();
    HashMap<String,ArrayList<Output>> output = new HashMap<>();

    public boolean addInput(String name, Input in)
    {
        input.computeIfAbsent(name, k -> new ArrayList<>());
        input.get(name).add(in);
        return true;
    }

    public boolean addInput(String name, ArrayList<Input> inputs)
    {
        for(Input in: inputs)
        {
            if(!addInput(name, in))
                return false;
        }
        return true;
    }

    public boolean addOutput(Output output)
    {
        return true;
    }
}
