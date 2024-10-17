package nurgling.tools;

import haven.Gob;
import haven.Indir;
import haven.Resource;
import nurgling.NUtils;
import nurgling.areas.NArea;

import java.util.*;

public class Context {
    public static HashMap<String, String> contcaps = new HashMap<>();
    static {
        contcaps.put("gfx/terobjs/chest", "Chest");
        contcaps.put("gfx/terobjs/crate", "Crate");
        contcaps.put("gfx/terobjs/cupboard", "Cupboard");
        contcaps.put("gfx/terobjs/shed", "Shed");
        contcaps.put("gfx/terobjs/largechest", "Large Chest");
        contcaps.put("gfx/terobjs/metalcabinet", "Metal Cabinet");
        contcaps.put("gfx/terobjs/strawbasket", "Straw Basket");
        contcaps.put("gfx/terobjs/bonechest", "Bone Chest");
        contcaps.put("gfx/terobjs/coffer", "Coffer");
        contcaps.put("gfx/terobjs/leatherbasket", "Leather Basket");
        contcaps.put("gfx/terobjs/woodbox", "Woodbox");
        contcaps.put("gfx/terobjs/linencrate", "Linen Crate");
        contcaps.put("gfx/terobjs/stonecasket", "Stone Casket");
        contcaps.put("gfx/terobjs/birchbasket", "Birch Basket");
        contcaps.put("gfx/terobjs/wbasket", "Basket");
        contcaps.put("gfx/terobjs/exquisitechest", "Exquisite Chest");
        contcaps.put("gfx/terobjs/furn/table-stone", "Table");
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

    static HashMap<String, String> equip_map;
    static {
        equip_map = new HashMap<>();
        equip_map.put("gfx/invobjs/small/fryingpan", "Frying Pan");
    }

    static HashMap<String, Workstation> workstation_map;
    static {
        workstation_map = new HashMap<>();
        workstation_map.put("paginae/bld/meatgrinder",new Workstation("gfx/terobjs/meatgrinder", "gfx/borka/idle"));
        workstation_map.put("paginae/bld/loom",new Workstation("gfx/terobjs/loom", "gfx/borka/loomsit"));
        workstation_map.put("paginae/bld/ropewalk",new Workstation("gfx/terobjs/ropewalk", "gfx/borka/idle"));
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

    public interface Output
    {
        NArea getArea();

        int getTh();
    }

    public interface Input
    {

    }


    public static class InputContainer extends Container implements Input
    {
        public InputContainer(Gob gob, String name)
        {
            this.gob = gob;
            this.cap = name;
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

        public OutputPile(Gob gob, NArea area, int th)
        {
            super(gob);
            this.area = area;
            this.th = th;
        }

        @Override
        public NArea getArea() {
            return area;
        }

        NArea area = null;

        @Override
        public int getTh()
        {
            return th;
        }

        Integer th = 1;
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
        if(area == null)
            return outputs;
        NArea.Ingredient ingredient = area.getOutput(item);
        switch (ingredient.type)
        {
            case BARTER:
                outputs.add(new OutputBarter( Finder.findGob(area, new NAlias("gfx/terobjs/barterstand")),
                        Finder.findGob(area, new NAlias("gfx/terobjs/chest")),area, ingredient.th));
                break;
            case CONTAINER:
            {
                for(Gob gob: Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()),new ArrayList<>())))
                {
                    OutputContainer container = new OutputContainer(gob,area, ingredient.th);
                    container.initattr(Container.Space.class);
                    outputs.add(container);
                }
                for(Gob gob: Finder.findGobs(area, new NAlias ("stockpile")))
                {
                    outputs.add(new OutputPile(gob, area, ingredient.th));
                }
                if(outputs.isEmpty())
                {
                    outputs.add(new OutputPile(null, area, ingredient.th));
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
                for(Gob gob: Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()),new ArrayList<>())))
                {
                        inputs.add(new InputContainer(gob, contcaps.get(gob.ngob.name)));
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


    public static class OutputBarter extends Barter implements Output
    {
        public OutputBarter(Gob barter, Gob chest, NArea area, int th)
        {
            super(barter, chest);
            this.area = area;
            this.th = th;
        }

        @Override
        public NArea getArea() {
            return area;
        }

        NArea area = null;
        @Override
        public int getTh()
        {
            return th;
        }

        Integer th = 1;
    }

    public ArrayList<Output> getOutputs(String name, int th) {
        if(output.get(name)!=null)
        {
            for(Integer val: output.get(name).keySet())
            {
                if(th<=val)
                    return output.get(name).get(val);
            }
        }
        return null;
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


    public static class OutputContainer extends Container implements Output
    {

        public OutputContainer(Gob gob, NArea area, int th)
        {
            this.gob = gob;
            this.cap = contcaps.get(gob.ngob.name);
            this.area = area;
            this.th = th;
        }

        @Override
        public NArea getArea() {
            return area;
        }

        NArea area = null;

        @Override
        public int getTh()
        {
            return th;
        }

        Integer th = 1;
    }

    public ArrayList<Input> getInputs(String name)
    {
        ArrayList<Input> in = input.get(name);
        ArrayList<Input> for_remove =  new ArrayList<>();
        if(in!=null) {
            for (Input i : in) {
                if (i instanceof Pile) {
                    if (Finder.findGob(((Pile) i).pile.id) == null)
                        for_remove.add(i);
                }
            }

            in.removeAll(for_remove);
        }
        return in;
    }

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

    public boolean addOutput(String name, int th, Output out)
    {
        output.computeIfAbsent(name, k -> new TreeMap<>());
        output.get(name).computeIfAbsent(th, k -> new ArrayList<>());
        output.get(name).get(th).add(out);
        return true;
    }

    public boolean addOutput(String name, ArrayList<Output> outputs)
    {
        for(Output out: outputs)
        {
            if(!addOutput(name, out.getTh(), out))
                return false;
        }
        return true;
    }

    public ArrayList<Container> icontainers = new ArrayList<>();
    HashMap<String,ArrayList<Input>> input = new HashMap<>();
    HashMap<String, SortedMap<Integer,ArrayList<Output>>> output = new HashMap<>();
}
