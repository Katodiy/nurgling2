package nurgling.tools;

import haven.*;
import nurgling.NGItem;
import nurgling.NInventory;
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

    public void updateContainer(String cap, NInventory inventory, Container container) throws InterruptedException {
        containerUpdater.update(cap,inventory,container);
    }


    public ArrayList<Input> getInputs(String name)
    {
        ArrayList<Input> in =  input.get(name);
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

    public Set<String> getOutputItems() {
        return output.keySet();
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

    public interface Updater{
        void update(String cap, NInventory inv, Container cont) throws InterruptedException;
    }

    public static class Container
    {
        public boolean isFree = false;

        public int freeSpace;

        public int maxSpace;

        public ArrayList<String> names;

        public String cap = null;

        public Gob gob = null;

        public HashMap<String, Integer> itemInfo = new HashMap<>();

        public Container(Gob gob, ArrayList<String> names) {
            this.gob = gob;
            this.names = names;
        }
    }


    public static class OutputContainer extends Container implements Output
    {

        public OutputContainer(Gob gob, NArea area)
        {
            super(gob,  new ArrayList<>(Context.contcaps.getall(gob.ngob.name)));
            this.area = area;
        }

        @Override
        public NArea getArea() {
            return area;
        }

        NArea area = null;
    }


    public static class InputContainer extends Container implements Input
    {
        public InputContainer(Gob gob, ArrayList<String> name)
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
        if(area == null)
            return outputs;
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
                    outputs.add(new OutputContainer(gob,area));
                }
                for(Gob gob: Finder.findGobs(area, new NAlias ("stockpile")))
                {
                    outputs.add(new OutputPile(gob, area));
                }
                if(outputs.isEmpty())
                {
                    outputs.add(new OutputPile(null, area));
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
                        inputs.add(new InputContainer(gob, new ArrayList<>(contcaps.getall(gob.ngob.name))));
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

    public void fillForInventory(NInventory inv, HashMap<String, Integer> itemInfo) throws InterruptedException {
        for(WItem item: inv.getItems())
        {
            String name = ((NGItem)item.item).name();
            if(output.get(name)== null)
                addOutput(name , Context.GetOutput(name, NArea.findOut(((NGItem)item.item).name())));
            itemInfo.put(name, inv.getItems((((NGItem) item.item).name())).size());
        }
    }

    Context.Updater containerUpdater = new Context.Updater() {
        @Override
        public void update(String cap, NInventory inv, Context.Container cont) throws InterruptedException {
            cont.cap = cap;
            cont.freeSpace = inv.getFreeSpace();
            cont.maxSpace = inv.getTotalSpace();
            cont.isFree = cont.freeSpace == cont.maxSpace;
        }
    };
    public void setCurrentUpdater(Context.Updater updater)
    {
        containerUpdater = updater;
    }
}
