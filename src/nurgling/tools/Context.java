package nurgling.tools;

import haven.*;
import nurgling.NUtils;
import nurgling.areas.*;

import java.util.*;

public class Context
{

    public static NAlias containers = new NAlias(
        "gfx/terobjs/chest",
        "gfx/terobjs/crate",
        "gfx/terobjs/cupboard",
        "gfx/terobjs/shed");

    public static HashMultiMap<String, String> contcaps = new HashMultiMap<>();
    static {
        contcaps.put("gfx/terobjs/chest", "Chest");
        contcaps.put("gfx/terobjs/crate", "Crate");
        contcaps.put("gfx/terobjs/cupboard", "Cupboard");
        contcaps.put("gfx/terobjs/shed", "Shed");
        contcaps.put("gfx/terobjs/smelter", "Ore Smelter");
        contcaps.put("gfx/terobjs/smelter", "Smith's Smelter");
        contcaps.put("gfx/terobjs/primsmelter", "Furnace");
    }


    public ArrayList<Input> getInputs(String name)
    {
        ArrayList<Input> in =  input.get(name);
        ArrayList<Input> for_remove =  new ArrayList<>();
        for(Input i : in)
        {
            if(i instanceof Pile)
            {
                if(Finder.findGob(((Pile) i).pile.id)==null)
                    for_remove.add(i);
            }
        }
        in.removeAll(for_remove);
        return in;
    }

    static HashMap<String, String> equip_map;
    static {
        equip_map = new HashMap<>();
    }

    static HashMap<String, Workstation> workstation_map;
    static {
        workstation_map = new HashMap<>();
        workstation_map.put("paginae/bld/meatgrinder",new Workstation("gfx/terobjs/meatgrinder", "gfx/borka/idle"));
        workstation_map.put("paginae/bld/loom",new Workstation("gfx/terobjs/loom", "gfx/borka/loomsit"));
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

    public ArrayList<Output> getOutputs(String name) {
        return output.get(name);
    }

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

    interface Updater{
        boolean isEqual(Container cont);
    }

    public static class Container
    {
        boolean isFree = false;

        int freeSpace;

        int maxSpace;

        Collection<String> names;

        String cap;

        Gob gob;

        public Container(Gob gob, Collection<String> names) {
            this.gob = gob;
            this.names = names;
        }
    }

    public static class InputContainer extends Container implements Input
    {
        public InputContainer(Gob gob, Collection<String> name)
        {
            super(gob, name);
        }
    }

    public static class InputBarter extends Barter implements Input
    {
        public InputBarter(Gob barter, Gob chest)
        {
            super(barter, chest);
        }
    }

    public static class InputPile extends Pile implements Input
    {
        public InputPile(Gob gob)
        {
            super(gob);
        }
    }

    public static class OutputPile extends Pile implements Output
    {
        public OutputPile(Gob gob)
        {
            super(gob);
        }

        public OutputPile(Gob gob, NArea area)
        {
            super(gob);
            this.area = area;
        }

        @Override
        public NArea getArea() {
            return area;
        }

        NArea area = null;
    }

    public static class Pile {
        public Gob pile;
        public Pile(Gob gob)
        {
            this.pile = gob;
        }
    }

    public static ArrayList<Output> GetOutput(String item, NArea area)  throws InterruptedException
    {
        ArrayList<Output> outputs = new ArrayList<>();
        NArea.Ingredient ingredient = area.getOutput(item);
        switch (ingredient.type)
        {
            case BARTER:
//                inputs.add(new InputBarter( Finder.findGob(area, new NAlias("gfx/terobjs/barterstand")),
//                        Finder.findGob(area, new NAlias("gfx/terobjs/chest"))));
                break;
            case CONTAINER:
            {
                for(Gob gob: Finder.findGobs(area, containers))
                {
//                    inputs.add(new InputContainer(gob));
                }
                for(Gob gob: Finder.findGobs(area, new NAlias ("stockpile")))
                {
                    outputs.add(new OutputPile(gob, area));
                }

            }
        }
        return outputs;
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
            case CONTAINER:
            {
                for(Gob gob: Finder.findGobs(area, containers))
                {
                        inputs.add(new InputContainer(gob, contcaps.getall(gob.ngob.name)));
                }
                for(Gob gob: Finder.findGobs(area, new NAlias ("stockpile")))
                {
                        inputs.add(new InputPile(gob));
                }

            }
        }
        inputs.sort(new Comparator<Input>() {
            @Override
            public int compare(Input o1, Input o2) {
                if (o1 instanceof InputPile && o2 instanceof InputPile)
                    return NUtils.d_comp.compare(((InputPile)o1).pile,((InputPile)o2).pile);
                return 0;
            }
        });
        return inputs;
    }

    public interface Output
    {
        NArea getArea();
    }

    public class OutputBarter extends Barter implements Output
    {
        public OutputBarter(Gob barter, Gob chest)
        {
            super(barter, chest);

        }

        @Override
        public NArea getArea() {
            return area;
        }

        NArea area = null;
    }

    HashMap<String,ArrayList<Input>> input = new HashMap<>();
    HashMap<String,ArrayList<Output>> output = new HashMap<>();

    public ArrayList<Container> getContainersInWork() {
        return containersInWork;
    }

    final ArrayList<Container> containersInWork = new ArrayList<>();

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

    public boolean addOutput(String name, Output out)
    {
        output.computeIfAbsent(name, k -> new ArrayList<>());
        output.get(name).add(out);
        return true;
    }

    public void addConstContainers(ArrayList<Container> containers)
    {
        synchronized (containersInWork)
        {
             containersInWork.addAll(containers);
        }
    }

    public boolean addOutput(String name, ArrayList<Output> outputs)
    {
        for(Output out: outputs)
        {
            if(!addOutput(name, out))
                return false;
        }
        return true;
    }
}
