package nurgling.areas;

import haven.*;
import nurgling.*;
import nurgling.actions.bots.SelectArea;
import nurgling.navigation.AreaNavigationHelper;
import nurgling.navigation.ChunkNavManager;
import nurgling.navigation.ChunkPath;
import nurgling.tools.*;
import nurgling.tools.Container;
import nurgling.widgets.Specialisation;
import nurgling.conf.ConstructionMaterialsRegistry;
import org.json.*;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NContext {

    public final static AtomicBoolean waitBot = new AtomicBoolean(false);
    private HashMap<String, String> inAreas = new HashMap<>();
    private HashMap<String, TreeMap<Double,String>> outAreas = new HashMap<>();
    private HashMap<String, String> barrels = new HashMap<>();
    private HashMap<String, BarrelStorage> barrelstorage = new HashMap<>();
    private HashMap<NArea.Specialisation, String> specArea = new HashMap<>();
    private HashMap<String, NArea> areas = new HashMap<>();
    private HashMap<String, ObjectStorage> containers = new HashMap<>();
    
    // Barrel tracking for BarrelWorkArea (when no workstation is used)
    private HashMap<String, String> bwaPlacedBarrelHashes = new HashMap<>();
    private HashMap<String, NGlobalCoord> bwaOriginalBarrelCoords = new HashMap<>();

    public boolean bwaused = false;
    int counter = 0;
    private NGameUI gui;

    private NGlobalCoord lastcoord;

    public static HashMap<String, String> contcaps = new HashMap<>();
    static {
        contcaps.put("gfx/terobjs/chest", "Chest");
        contcaps.put("gfx/terobjs/crate", "Crate");
        contcaps.put("gfx/terobjs/kiln", "Kiln");
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
        contcaps.put("gfx/terobjs/thatchbasket", "Basket");
        contcaps.put("gfx/terobjs/map/stonekist", "Stonekist");
        contcaps.put("gfx/terobjs/exquisitechest", "Exquisite Chest");
        contcaps.put("gfx/terobjs/furn/table-stone", "Table");
        contcaps.put("gfx/terobjs/furn/table-rustic", "Table");
        contcaps.put("gfx/terobjs/furn/table-elegant", "Table");
        contcaps.put("gfx/terobjs/furn/cottagetable", "Table");
        contcaps.put("gfx/terobjs/map/jotunclam", "Jotun Clam");
        contcaps.put("gfx/terobjs/studydesk", "Study Desk");
        contcaps.put("gfx/terobjs/htable", "Herbalist Table");
        contcaps.put("gfx/terobjs/dng/ratchest", "Chest");
    }

    public static HashMap<String, String> customTool = new HashMap<>();
    static {
        customTool.put("Clay Jar", "paginae/bld/potterswheel");
        customTool.put("Garden Pot", "paginae/bld/potterswheel");
        customTool.put("Pot", "paginae/bld/potterswheel");
        customTool.put("Treeplanter's Pot", "paginae/bld/potterswheel");
        customTool.put("Urn", "paginae/bld/potterswheel");
        customTool.put("Teapot", "paginae/bld/potterswheel");
        customTool.put("Mug", "paginae/bld/potterswheel");
        customTool.put("Stoneware Vase", "paginae/bld/potterswheel");
    }

    static HashMap<String, String> equip_map;
    static {
        equip_map = new HashMap<>();
        equip_map.put("gfx/invobjs/small/fryingpan", "Frying Pan");
        equip_map.put("gfx/invobjs/small/glassrod", "Glass Blowing Rod");
        equip_map.put("gfx/invobjs/smithshammer", "Smithy's Hammer");
    }

    static HashMap<String, NContext.Workstation> workstation_map;
    static {
        workstation_map = new HashMap<>();
        workstation_map.put("paginae/bld/meatgrinder",new NContext.Workstation("gfx/terobjs/meatgrinder", "gfx/borka/idle"));
        workstation_map.put("paginae/bld/churn",new NContext.Workstation("gfx/terobjs/churn", "gfx/borka/churnan-idle"));
        workstation_map.put("paginae/bld/loom",new NContext.Workstation("gfx/terobjs/loom", "gfx/borka/loomsit"));
        workstation_map.put("paginae/bld/ropewalk",new NContext.Workstation("gfx/terobjs/ropewalk", "gfx/borka/idle"));
        workstation_map.put("paginae/bld/crucible",new NContext.Workstation("gfx/terobjs/crucible", null));
        workstation_map.put("gfx/invobjs/fire",new NContext.Workstation("gfx/terobjs/pow", null));
        workstation_map.put("gfx/invobjs/cauldron",new NContext.Workstation("gfx/terobjs/cauldron", null));
        workstation_map.put("paginae/bld/potterswheel",new NContext.Workstation("gfx/terobjs/potterswheel", "gfx/borka/pwheelidle"));
        workstation_map.put("paginae/bld/swheel",new NContext.Workstation("gfx/terobjs/swheel", "gfx/borka/swheelan-idle"));
        workstation_map.put("paginae/bld/anvil",new NContext.Workstation("gfx/terobjs/anvil", null));
    }

    public static HashMap<String, Specialisation.SpecName> workstation_spec_map;
    static {
        workstation_spec_map = new HashMap<>();
        workstation_spec_map.put("gfx/terobjs/meatgrinder", Specialisation.SpecName.meatgrinder);
        workstation_spec_map.put("gfx/terobjs/churn", Specialisation.SpecName.churn);
        workstation_spec_map.put("gfx/terobjs/loom",Specialisation.SpecName.loom);
        workstation_spec_map.put("gfx/terobjs/swheel",Specialisation.SpecName.sswheel);
        workstation_spec_map.put("gfx/terobjs/ropewalk",Specialisation.SpecName.ropewalk);
        workstation_spec_map.put("gfx/terobjs/crucible",Specialisation.SpecName.crucible);
        workstation_spec_map.put("gfx/terobjs/pow",Specialisation.SpecName.pow);
        workstation_spec_map.put("gfx/terobjs/cauldron",Specialisation.SpecName.boiler);
        workstation_spec_map.put("gfx/terobjs/potterswheel",Specialisation.SpecName.potterswheel);
        workstation_spec_map.put("gfx/terobjs/anvil",Specialisation.SpecName.anvil);
    }

    public static HashSet<String> doubleOutputItems;
    static {
        doubleOutputItems = new HashSet<>();
        doubleOutputItems.add("Silk Filament");
    }

    public static int getOutputMultiplier(String itemName) {
        if (doubleOutputItems.contains(itemName)) {
            ///TODO FIX SKILL
            return 2;
        }
        return 1;
    }


    public static class Barter implements ObjectStorage
    {
        public long barter;
        public long chest;

        public Barter(Gob barter, Gob chest)
        {
            this.barter = barter.id;
            this.chest = chest.id;
        }
    }

    public static class Barrel implements ObjectStorage
    {
        public long barrel;

        public Barrel(Gob barrel)
        {
            this.barrel = barrel.id;
        }
    }



    public interface ObjectStorage
    {
        default double getTh(){
            return 1;
        }
    }

    public static class Pile implements ObjectStorage{
        public Gob pile;
        public Pile(Gob gob)
        {
            this.pile = gob;
        }
    }


    public void addCustomTool(String resName) {
        String cust = customTool.get(resName);
        if(cust != null) {
            NContext.Workstation workstation_cand = workstation_map.get(cust);
            if(workstation_cand!=null)
            {
                workstation = workstation_cand;
            }
        }
    }



    public TreeMap<Double,String> getOutAreas(String item) {
        return outAreas.get(item);
    }

    public NArea getSpecArea(NContext.Workstation workstation) throws InterruptedException {
        String specName = workstation_spec_map.get(workstation.station).toString();
        NUtils.getGameUI().msg("getSpecArea: Looking for spec '" + specName + "' for station '" + workstation.station + "'");
        
        if(!areas.containsKey(specName)) {
            NUtils.getGameUI().msg("getSpecArea: Area not in cache, searching...");
            NArea area = findSpec(specName);
            NUtils.getGameUI().msg("getSpecArea: findSpec returned " + (area != null ? "area" : "null"));
            if (area == null) {
                area = findSpecGlobal(specName);
                NUtils.getGameUI().msg("getSpecArea: findSpecGlobal returned " + (area != null ? "area" : "null"));
            }
            if (area != null) {
                areas.put(specName, area);
            }
            else
            {
                NUtils.getGameUI().msg("getSpecArea: No area found, returning null");
                return null;
            }
        }
        navigateToAreaIfNeeded(specName);
        return areas.get(specName);
    }

    public static class BarrelStorage
    {
        public NGlobalCoord coord;
        public String olname;

        public BarrelStorage(NGlobalCoord coord, String olname) {
            this.coord = coord;
            this.olname = olname;
        }
    }

    public BarrelStorage getBarrelStorage(String item)
    {
        return barrelstorage.get(item);
    }

    public Gob getBarrelInArea(String item) throws InterruptedException {
        if(!barrels.containsKey(item)) {
            NArea area = findIn(item);
            if (area == null) {
                area = findInGlobal(item);
            }
            if (area != null) {
                areas.put(String.valueOf(area.id), area);
                barrels.put(item, String.valueOf(area.id));
            }
            if(area == null)
                return null;
        }
        String areaid = barrels.get(item);
        navigateToAreaIfNeeded(areaid);
        for(Gob gob: Finder.findGobs(areas.get(areaid), new NAlias("barrel")))
        {
            if(NUtils.barrelHasContent(gob))
            {
                barrelstorage.put(item,new BarrelStorage(new NGlobalCoord(gob.rc), NUtils.getContentsOfBarrel(gob)));
                return gob;
            }
        }
        return null;

    }


    public Gob getBarrelInWorkArea(String item) throws InterruptedException {
        String storedHash = getPlacedBarrelHash(item);
        
        // First try to find barrel by stored hash (most reliable after placement)
        if (storedHash != null) {
            ArrayList<Gob> allBarrels = nurgling.tools.Finder.findGobs(new nurgling.tools.NAlias("barrel"));
            for (Gob gob : allBarrels) {
                if (storedHash.equals(gob.ngob.hash)) {
                    return gob;
                }
            }
            
            // Hash stored but barrel not found in cache
            // Check if we're already near workstation - if so, just search by proximity without navigation
            boolean alreadyNearWorkstation = false;
            if (workstation != null && workstation.selected != -1) {
                Gob ws = nurgling.tools.Finder.findGob(workstation.selected);
                if (ws != null) {
                    double distToWs = NUtils.player().rc.dist(ws.rc);
                    if (distToWs < 30) {
                        alreadyNearWorkstation = true;
                        NUtils.getGameUI().msg("getBarrelInWorkArea: Already near workstation (dist=" + 
                                String.format("%.2f", distToWs) + "), searching nearby barrels...");
                        
                        // Search by proximity without navigation - verify content
                        String expectedContent = barrelstorage.containsKey(item) ? barrelstorage.get(item).olname : null;
                        for (Gob gob : allBarrels) {
                            if (gob.rc.dist(ws.rc) < 30) {
                                // Check if barrel has correct content or is empty
                                boolean isEmpty = !NUtils.barrelHasContent(gob);
                                String barrelContent = NUtils.getContentsOfBarrel(gob);
                                boolean contentMatches = expectedContent != null && 
                                        barrelContent != null && 
                                        barrelContent.equalsIgnoreCase(expectedContent);
                                
                                if (isEmpty || contentMatches) {
                                    NUtils.getGameUI().msg("getBarrelInWorkArea: Found barrel near workstation (dist=" + 
                                            String.format("%.2f", gob.rc.dist(ws.rc)) + ", empty=" + isEmpty + 
                                            ", content=" + barrelContent + "), updating hash");
                                    storeBarrelInfo(item, gob.ngob.hash, getOriginalBarrelCoord(item));
                                    return gob;
                                }
                            }
                        }
                    }
                }
            }
            
            // Not near workstation - navigate to reload objects from cache
            if (!alreadyNearWorkstation) {
                NUtils.getGameUI().msg("getBarrelInWorkArea: Barrel with hash not in cache, navigating to reload...");
                
                NArea area;
                if(workstation==null)
                    area = getSpecArea(Specialisation.SpecName.barrelworkarea);
                else
                    area = getSpecArea(workstation);
                
                if (area != null) {
                    haven.Pair<haven.Coord2d, haven.Coord2d> rcArea = area.getRCArea();
                    if (rcArea != null) {
                        haven.Coord2d center = rcArea.b.sub(rcArea.a).div(2).add(rcArea.a);
                        new nurgling.actions.PathFinder(center).run(NUtils.getGameUI());
                        
                        Thread.sleep(500);
                        
                        allBarrels = nurgling.tools.Finder.findGobs(new nurgling.tools.NAlias("barrel"));
                        for (Gob gob : allBarrels) {
                            if (storedHash.equals(gob.ngob.hash)) {
                                NUtils.getGameUI().msg("getBarrelInWorkArea: Found barrel after navigation, hash=" + storedHash.substring(0, 16) + "...");
                                return gob;
                            }
                        }
                        
                        // Search by proximity after navigation - verify content
                        if (workstation != null && workstation.selected != -1) {
                            Gob ws = nurgling.tools.Finder.findGob(workstation.selected);
                            if (ws != null) {
                                String expectedContent = barrelstorage.containsKey(item) ? barrelstorage.get(item).olname : null;
                                for (Gob gob : allBarrels) {
                                    if (gob.rc.dist(ws.rc) < 30) {
                                        // Check if barrel has correct content or is empty
                                        boolean isEmpty = !NUtils.barrelHasContent(gob);
                                        String barrelContent = NUtils.getContentsOfBarrel(gob);
                                        boolean contentMatches = expectedContent != null && 
                                                barrelContent != null && 
                                                barrelContent.equalsIgnoreCase(expectedContent);
                                        
                                        if (isEmpty || contentMatches) {
                                            NUtils.getGameUI().msg("getBarrelInWorkArea: Found barrel near workstation (dist=" + 
                                                    String.format("%.2f", gob.rc.dist(ws.rc)) + ", empty=" + isEmpty + 
                                                    ", content=" + barrelContent + "), updating hash");
                                            storeBarrelInfo(item, gob.ngob.hash, getOriginalBarrelCoord(item));
                                            return gob;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Fallback: search in area by content
        NArea area;
        if(workstation==null)
            area = getSpecArea(Specialisation.SpecName.barrelworkarea);
        else
            area = getSpecArea(workstation);
        if(area==null)
            return null;
        if(barrelstorage.containsKey(item))
        {
            for (Gob gob : nurgling.tools.Finder.findGobs(area, new nurgling.tools.NAlias("barrel")))
            {
                String content = NUtils.getContentsOfBarrel(gob);
                if (content != null && (content.equalsIgnoreCase(barrelstorage.get(item).olname)))
                {
                    return gob;
                }
            }
        }
        return null;

    }
    
    /**
     * Get placed barrel hash for an item (checks both workstation and BWA storage)
     */
    public String getPlacedBarrelHash(String item) {
        if (workstation != null) {
            return workstation.getPlacedBarrelHash(item);
        } else {
            return bwaPlacedBarrelHashes.get(item);
        }
    }
    
    /**
     * Get original barrel coord for an item (checks both workstation and BWA storage)
     */
    public NGlobalCoord getOriginalBarrelCoord(String item) {
        if (workstation != null) {
            return workstation.getOriginalBarrelCoord(item);
        } else {
            return bwaOriginalBarrelCoords.get(item);
        }
    }
    
    /**
     * Store barrel tracking info for an item
     */
    public void storeBarrelInfo(String item, String hash, NGlobalCoord originalCoord) {
        if (workstation != null) {
            workstation.storeBarrelInfo(item, hash, originalCoord);
        } else {
            bwaPlacedBarrelHashes.put(item, hash);
            bwaOriginalBarrelCoords.put(item, originalCoord);
        }
    }
    
    /**
     * Clear barrel tracking info for a specific item
     */
    public void clearBarrelInfo(String item) {
        if (workstation != null) {
            workstation.clearBarrelInfo(item);
        } else {
            bwaPlacedBarrelHashes.remove(item);
            bwaOriginalBarrelCoords.remove(item);
        }
    }
    
    /**
     * Clear all barrel tracking info
     */
    public void clearAllBarrelInfo() {
        if (workstation != null) {
            workstation.clearAllBarrelInfo();
        }
        bwaPlacedBarrelHashes.clear();
        bwaOriginalBarrelCoords.clear();
    }

    public void navigateToBarrelArea(String item) throws InterruptedException {
        String areaid = barrels.get(item);
        navigateToAreaIfNeeded(areaid);
    }
    
    /**
     * Get the barrel storage area for a specific item.
     * Returns null if no barrel area is registered for this item.
     */
    public NArea getBarrelAreaForItem(String item) {
        String areaid = barrels.get(item);
        if (areaid != null) {
            return areas.get(areaid);
        }
        return null;
    }

    public NArea getSpecArea(Specialisation.SpecName name) throws InterruptedException {
        if(!areas.containsKey(name.toString())) {
            NArea area = findSpec(name.toString());
            if (area == null) {
                area = findSpecGlobal(name.toString());
            }
            if (area != null) {
                areas.put(String.valueOf(name.toString()), area);
            }
            else
            {
                return null;
            }
        }
        navigateToAreaIfNeeded(name.toString());
        return areas.get(name.toString());
    }

    public NArea getSpecArea(Specialisation.SpecName name, String sub) throws InterruptedException {
        String key = name.toString() + (sub != null ? "_" + sub : "");
        if(!areas.containsKey(key)) {
            NArea area = findSpec(name.toString(),sub);
            if (area == null) {
                area = findSpecGlobal(name.toString(),sub);
            }
            if (area != null) {
                areas.put(key, area);
            }
            else
            {
                return null;
            }
        }
        navigateToAreaIfNeeded(key);
        return areas.get(key);
    }

    /**
     * Find construction materials zone for a specific material type WITHOUT navigating.
     * Only finds and caches the area, navigation happens later when needed.
     * @param materialType The type of material (BLOCK, BOARD, STONE, etc.)
     * @return The area containing the material, or null if not found
     */
    public NArea getBuildMaterialArea(ConstructionMaterialsRegistry.MaterialType materialType) throws InterruptedException {
        return findSpecAreaNoNavigate(Specialisation.SpecName.buildMaterials, materialType.getSubtype());
    }

    /**
     * Find construction materials zone for a specific item alias WITHOUT navigating.
     * Only finds and caches the area, navigation happens later when needed.
     * @param itemAlias The alias of the item (e.g., "Block", "Board", "Flax Fibres")
     * @return The area containing the material, or null if not found
     */
    public NArea getBuildMaterialArea(NAlias itemAlias) throws InterruptedException {
        ConstructionMaterialsRegistry.MaterialType materialType = 
            ConstructionMaterialsRegistry.getMaterialType(itemAlias);
        if (materialType != null) {
            return getBuildMaterialArea(materialType);
        }
        return null;
    }
    
    /**
     * Find a specialization area WITHOUT navigating to it.
     * Use this when you just need to check if a zone exists or get a reference.
     * @param name Specialization name
     * @param sub Subtype (can be null)
     * @return The area or null if not found
     */
    public NArea findSpecAreaNoNavigate(Specialisation.SpecName name, String sub) throws InterruptedException {
        String key = name.toString() + (sub != null ? "_" + sub : "");
        if (!areas.containsKey(key)) {
            NArea area = findSpec(name.toString(), sub);
            if (area == null) {
                area = findSpecGlobal(name.toString(), sub);
            }
            if (area != null) {
                areas.put(key, area);
            } else {
                return null;
            }
        }
        // NO navigation - just return the cached area
        return areas.get(key);
    }

    /**
     * Check if a construction materials zone exists for the given material type.
     * Does not navigate to the zone.
     * @param materialType The type of material
     * @return true if a zone exists, false otherwise
     */
    public boolean hasBuildMaterialArea(ConstructionMaterialsRegistry.MaterialType materialType) {
        return ConstructionMaterialsRegistry.hasMaterialZone(materialType);
    }

    /**
     * Check if a construction materials zone exists for the given item alias.
     * Does not navigate to the zone.
     * @param itemAlias The alias of the item
     * @return true if a zone exists, false otherwise
     */
    public boolean hasBuildMaterialArea(NAlias itemAlias) {
        return ConstructionMaterialsRegistry.hasMaterialZone(itemAlias);
    }

    /**
     * Get an area by its ID and navigate to it using global pathfinding.
     * Similar to getSpecArea but takes an area ID instead of spec name.
     */
    public NArea getAreaById(int areaId) throws InterruptedException {
        String key = "area_" + areaId;
        if (!areas.containsKey(key)) {
            NArea area = NUtils.getGameUI().map.glob.map.areas.get(areaId);
            if (area != null) {
                areas.put(key, area);
            } else {
                return null;
            }
        }
        navigateToAreaIfNeeded(key);
        return areas.get(key);
    }

    public NArea getAreaById(String key) throws InterruptedException {
        if (!areas.containsKey(key)) {
            NArea area = NUtils.getGameUI().map.glob.map.areas.get(key);
            if (area != null) {
                areas.put(key, area);
            } else {
                return null;
            }
        }
        navigateToAreaIfNeeded(key);
        return areas.get(key);
    }

    public ArrayList<ObjectStorage> getSpecStorages(Specialisation.SpecName name) throws InterruptedException {
        return getSpecStorages(name, null);
    }

    public ArrayList<ObjectStorage> getSpecStorages(Specialisation.SpecName name, String subtype) throws InterruptedException {

        ArrayList<ObjectStorage> inputs = new ArrayList<>();
        NArea area;
        if (subtype != null && !subtype.isEmpty()) {
            area = getSpecArea(name, subtype);
        } else {
            area = getSpecArea(name);
        }

        if(area == null) {
            return null;
        }

        navigateToAreaIfNeeded(String.valueOf(name));

        for (Gob gob : Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()), new ArrayList<>()))) {
            String hash = gob.ngob.hash;
            if(containers.containsKey(hash))
            {
                inputs.add(containers.get(hash));
            }
            else {
                Container ic = new Container(gob, contcaps.get(gob.ngob.name),area);
                ic.initattr(Container.Space.class);
                containers.put(gob.ngob.hash, ic);
                inputs.add(ic);
            }
        }

        for (Gob gob : Finder.findGobs(area, new NAlias("stockpile"))) {
            inputs.add(new Pile(gob));
        }
        if (inputs.isEmpty()) {
            inputs.add(new Pile(null));
        }

        inputs.sort(new Comparator<ObjectStorage>() {
            @Override
            public int compare(ObjectStorage o1, ObjectStorage o2) {
                if (o1 instanceof Pile && o2 instanceof Pile)
                    return NUtils.d_comp.compare(((Pile) o1).pile, ((Pile) o2).pile);
                return 0;
            }
        });
        return inputs;
    }

    public ArrayList<ObjectStorage> getInStorages(String item) throws InterruptedException {

        ArrayList<ObjectStorage> inputs = new ArrayList<>();
        String id = inAreas.get(item);

        if(id!=null) {
            navigateToAreaIfNeeded(inAreas.get(item));

            NArea area = areas.get(id);
            if(area != null) {
                NArea.Ingredient ingredient = area.getInput(item);
                if(ingredient == null) {
                } else {
                    switch (ingredient.type) {
                    case BARTER:
                        inputs.add(new Barter(Finder.findGob(area, new NAlias("gfx/terobjs/barterstand")),
                                Finder.findGob(area, new NAlias("gfx/terobjs/chest"))));
                        break;
                    case CONTAINER: {
                        for (Gob gob : Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()), new ArrayList<>()))) {
                            String hash = gob.ngob.hash;
                            if(containers.containsKey(hash))
                            {
                                inputs.add(containers.get(hash));
                            }
                            else {
                                Container ic = new Container(gob, contcaps.get(gob.ngob.name),area);
                                containers.put(gob.ngob.hash, ic);
                                inputs.add(ic);
                            }
                        }
                        for (Gob gob : Finder.findGobs(area, new NAlias("stockpile"))) {
                            inputs.add(new Pile(gob));
                        }

                    }
                    case BARREL: {
                        for (Gob gob : Finder.findGobs(area, new NAlias("barrel"))) {
                            inputs.add(new Barrel(gob));
                        }
                    }
                    }
                }
            }
            inputs.sort(new Comparator<ObjectStorage>() {
                @Override
                public int compare(ObjectStorage o1, ObjectStorage o2) {
                    if (o1 instanceof Pile && o2 instanceof Pile)
                        return NUtils.d_comp.compare(((Pile) o1).pile, ((Pile) o2).pile);
                    return 0;
                }
            });
        }
        return inputs;
    }

    public ArrayList<ObjectStorage> getOutStorages(String item, double q)  throws InterruptedException
    {
        ArrayList<ObjectStorage> outputs = new ArrayList<>();
        TreeMap<Double,String> thmap =  outAreas.get(item);
        String id = null;
        for(Double key: thmap.descendingKeySet())
        {
            if(q>=key)
            {
                id = thmap.get(key);
                break;
            }
        }
        if(id!=null) {
            navigateToAreaIfNeeded(id);

            NArea area = areas.get(id);
            NArea.Ingredient ingredient = area.getOutput(item);
            if (ingredient != null) {
                switch (ingredient.type) {
                    case BARTER:
                        outputs.add(new Barter(Finder.findGob(area, new NAlias("gfx/terobjs/barterstand")),
                                Finder.findGob(area, new NAlias("gfx/terobjs/chest"))));
                        break;
                    case CONTAINER: {

                        for (Gob gob : Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()), new ArrayList<>()))) {
                            String hash = gob.ngob.hash;
                            if(containers.containsKey(hash))
                            {
                                outputs.add(containers.get(hash));
                            }
                            else {
                                Container ic = new Container(gob, contcaps.get(gob.ngob.name),area);
                                ic.initattr(Container.Space.class);
                                containers.put(gob.ngob.hash, ic);
                                outputs.add(ic);
                            }
                        }
                        for (Gob gob : Finder.findGobs(area, new NAlias("stockpile"))) {
                            outputs.add(new Pile(gob));
                        }
                        if (outputs.isEmpty()) {
                            outputs.add(new Pile(null));
                        }
                        break;
                    }
                    case BARREL: {
                        for (Gob gob : Finder.findGobs(area, new NAlias("barrel"))) {
                            outputs.add(new Barrel(gob));
                        }
                    }
                }
            }
            else
            {
                for (Gob gob : Finder.findGobs(area, new NAlias(new ArrayList<String>(contcaps.keySet()), new ArrayList<>()))) {
                    String hash = gob.ngob.hash;
                    if(containers.containsKey(hash))
                    {
                        outputs.add(containers.get(hash));
                    }
                    else {
                        Container ic = new Container(gob, contcaps.get(gob.ngob.name),area);
                        ic.initattr(Container.Space.class);
                        containers.put(gob.ngob.hash, ic);
                        outputs.add(ic);
                    }
                }
                for (Gob gob : Finder.findGobs(area, new NAlias("stockpile"))) {
                    outputs.add(new Pile(gob));
                }
                if (outputs.isEmpty()) {
                    outputs.add(new Pile(null));
                }
            }
        }
        return outputs;
    }

    public static class Workstation
    {
        public String station;
        public String pose;
        public long selected = -1;

        public NGlobalCoord targetPoint = null;
        
        // Map of placed barrels: item name -> barrel hash (supports multiple barrels)
        public HashMap<String, String> placedBarrelHashes = new HashMap<>();
        // Map of original barrel coordinates: item name -> original position
        public HashMap<String, NGlobalCoord> originalBarrelCoords = new HashMap<>();

        public Workstation(String station, String pose)
        {
            this.station = station;
            this.pose = pose;
        }
        
        /**
         * Store barrel tracking info for a specific item
         */
        public void storeBarrelInfo(String item, String hash, NGlobalCoord originalCoord) {
            placedBarrelHashes.put(item, hash);
            originalBarrelCoords.put(item, originalCoord);
        }
        
        /**
         * Get placed barrel hash for a specific item
         */
        public String getPlacedBarrelHash(String item) {
            return placedBarrelHashes.get(item);
        }
        
        /**
         * Get original barrel coord for a specific item
         */
        public NGlobalCoord getOriginalBarrelCoord(String item) {
            return originalBarrelCoords.get(item);
        }
        
        /**
         * Clear barrel tracking info for a specific item
         */
        public void clearBarrelInfo(String item) {
            placedBarrelHashes.remove(item);
            originalBarrelCoords.remove(item);
        }
        
        /**
         * Reset all barrel tracking info
         */
        public void clearAllBarrelInfo() {
            placedBarrelHashes.clear();
            originalBarrelCoords.clear();
            this.targetPoint = null;
        }
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
            NContext.Workstation workstation_cand = workstation_map.get(res.get().name);
            if(workstation_cand!=null)
            {
                workstation = workstation_cand;
            }
        }
    }

    public String equip = null;
    public NContext.Workstation workstation = null;
    public NContext(NGameUI gui)
    {
        this.gui = gui;
    }

    public void setLastPos(Coord2d pos)
    {
        lastcoord = new NGlobalCoord(pos);
    }

    public ArrayList<Gob> getGobs(String areaId, NAlias pattern) throws InterruptedException {
        navigateToAreaIfNeeded(areaId);
        return Finder.findGobs(areas.get(areaId), pattern);
    }

    public Gob getGob(String areaId, NAlias pattern) throws InterruptedException {
        navigateToAreaIfNeeded(areaId);
        return Finder.findGob(areas.get(areaId), pattern);
    }

    public Gob getGob(String areaId, long id) throws InterruptedException {
        navigateToAreaIfNeeded(areaId);
        return Finder.findGob(id);
    }

    public void navigateToAreaIfNeeded(String areaId) throws InterruptedException {
        NArea area = areas.get(areaId);
        if(area == null) {
            return;
        }
        NUtils.navigateToArea(area);
    }

    public String createArea(String msg, BufferedImage bauble) throws InterruptedException {
        return createArea(msg, bauble,null);
    }

    public String createArea(String msg, BufferedImage bauble, BufferedImage custom) throws InterruptedException {
        SelectArea insa;
        NUtils.getGameUI().msg(msg);
        if(custom==null)
            (insa = new SelectArea(bauble)).run(gui);
        else
            (insa = new SelectArea(bauble,custom)).run(gui);
        String id = "temp"+counter++;
        NArea tempArea = new NArea(id);
        tempArea.space = insa.result;
        tempArea.lastLocalChange = System.currentTimeMillis();
        tempArea.grids_id.clear();
        tempArea.grids_id.addAll(tempArea.space.space.keySet());
        areas.put(id, tempArea);
        return id;
    }

    public String createAreaWithGhost(nurgling.tasks.SelectAreaWithLiveGhosts sa) throws InterruptedException {
        if(sa == null)
            return null;
        String id = "temp"+counter++;
        NArea tempArea = new NArea(id);
        tempArea.space = sa.getAreaSpace();
        tempArea.lastLocalChange = System.currentTimeMillis();
        tempArea.grids_id.clear();
        tempArea.grids_id.addAll(tempArea.space.space.keySet());
        areas.put(id, tempArea);
        return id;
    }

    public String createPlayerLastPos()
    {
        String id = "temp"+counter++;
        NArea tempArea = new NArea(id);
        Coord2d plc = NUtils.player().rc;
        tempArea.space = new NArea.Space(plc.sub(MCache.tilehsz).floor(MCache.tilesz),plc.add(MCache.tilehsz).floor(MCache.tilesz));
        tempArea.lastLocalChange = System.currentTimeMillis();
        tempArea.grids_id.clear();
        tempArea.grids_id.addAll(tempArea.space.space.keySet());
        areas.put(id, tempArea);
        return id;
    }

    public boolean isInBarrel(String item) {
        NArea area = findIn(item);
        if (area == null) {
            area = findInGlobal(item);
        }
        if(area!=null)
        {
            return area.getInput(item).type == NArea.Ingredient.Type.BARREL;
        }
        return false;
    }

    public void addInItem(String name, BufferedImage loadsimg) throws InterruptedException {
        NArea area = findIn(name);
        if (area == null) {
            area = findInGlobal(name);
        }
        if(area!=null)
        {
            areas.put(String.valueOf(area.id),area);
            inAreas.put(name, String.valueOf(area.id));
        }
        if (loadsimg!=null && area == null) {
            inAreas.put(name, createArea("Please select area with:" + name, Resource.loadsimg("baubles/custom"), loadsimg));
        }
    }

    public boolean addOutItem(String name, BufferedImage loadsimg, double th) throws InterruptedException {
        if(!outAreas.containsKey(name))
        {
            outAreas.put(name,new TreeMap<>());
        }
        else
        {
            for(Double key :outAreas.get(name).descendingKeySet())
            {
                if(th>key) {
                    return true;
                }
            }
        }
        NArea area = findOutGlobal(name, th, gui);
        if(area!=null)
        {
            areas.put(String.valueOf(area.id),area);
            outAreas.get(name).put(Math.abs((double)area.getOutput(name).th), String.valueOf(area.id));
        }
        if (loadsimg!=null && area == null) {
            outAreas.get(name).put(Math.abs(th), createArea("Please select area for:" + name, Resource.loadsimg("baubles/custom"), loadsimg));
        }
        else
        {
            if(area == null)
                return false;
        }
        return true;
    }

    
    public static NArea findIn(String name) {
        double dist = 10000;
        Gob player = NUtils.player();
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>0 && player!=null) {
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containIn(name)) {
                        NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                        Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                        if(test.getRCArea()!=null) {
                            double testdist;
                            if ((testdist = (testrc.a.dist(player.rc) + testrc.b.dist(player.rc))) < dist) {
                                res = test;
                                dist = testdist;
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static NArea findIn(NAlias name) {
        double dist = 10000;
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>0) {
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containIn(name)) {
                        NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                        Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                        if(test.getRCArea()!=null) {
                            double testdist;
                            if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                                res = test;
                                dist = testdist;
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static ArrayList<NArea> findAllIn(NAlias name) {
        ArrayList<NArea> results = new ArrayList<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>0) {
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containIn(name)) {
                        NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                        if(test.getRCArea()!=null) {
                            results.add(test);
                        }
                    }
                }
            }
        }
        return results;
    }

    public Coord2d getLastPosCoord(String areaId) throws InterruptedException {
        navigateToAreaIfNeeded(areaId);

        return lastcoord.getCurrentCoord();
    }

    public Pair<Coord2d, Coord2d> getRCArea(String marea) throws InterruptedException {
        NArea area = areas.get(marea);
        if(!area.isVisible())
        {
            navigateToAreaIfNeeded(marea);
        }
        return area.getRCArea();
    }




    private static class TestedArea {
        NArea area;
        double th;

        public TestedArea(NArea area, double th) {
            this.area = area;
            this.th = th;
        }
    }

    static Comparator<TestedArea> ta_comp = new Comparator<TestedArea>(){
        @Override
        public int compare(TestedArea o1, TestedArea o2) {
            return Double.compare(o1.th, o2.th);
        }
    };

    public static NArea findOut(NAlias name, double th) {
        double dist = 10000;
        NArea res = null;

        ArrayList<TestedArea> areas = new ArrayList<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0) {
                    NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                    if (cand.isVisible() && cand.containOut(name.getDefault(), th) && cand.getRCArea()!=null) {
                        areas.add(new TestedArea(cand, cand.getOutput(name.getDefault()).th));
                    }
                }
            }
        }

        areas.sort(ta_comp);

        double tth = 1;
        for (TestedArea area : areas) {
            if(area.th<=th) {
                res = area.area;
                tth = area.th;
            }
        }

        ArrayList<NArea> targets = new ArrayList<>();
        for(TestedArea area :areas) {
            if(area.th ==tth)
                targets.add(area.area);
        }

        if(targets.size()>1) {
            for (NArea test: targets) {
                Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                if(testrc == null)
                    continue;
                double testdist;
                if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                    res = test;
                    dist = testdist;
                }
            }
        }
        return res;
    }

    public static NArea findOut(String name, double th) {
        double dist = 10000;
        NArea res = null;

        ArrayList<TestedArea> areas = new ArrayList<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0) {
                    NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                    if (cand.isVisible() && cand.containOut(name, th) && cand.getRCArea()!=null) {
                        areas.add(new TestedArea(cand, cand.getOutput(name).th));
                    }
                }
            }
        }

        areas.sort(ta_comp);

        double tth = 1;
        for (TestedArea area : areas) {
            if(area.th<=th) {
                res = area.area;
                tth = area.th;
            }
        }

        ArrayList<NArea> targets = new ArrayList<>();
        for(TestedArea area :areas) {
            if(area.th == tth)
                targets.add(area.area);
        }

        if(targets.size()>1) {
            for (NArea test: targets) {
                Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                if(testrc == null)
                    continue;
                double testdist;
                if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                    res = test;
                    dist = testdist;
                }
            }
        }
        return res;
    }

    /**
     * Calculate distance to an area using ChunkNav if available, falling back to RouteGraph.
     * Returns Double.MAX_VALUE if area is unreachable.
     */
    private static double getDistanceToArea(NArea area, NGameUI gui) {
        if (gui == null || gui.map == null) {
            return Double.MAX_VALUE;
        }

        // Try ChunkNav first - plan paths to all 4 corners and pick shortest
        if (gui.map instanceof NMapView) {
            ChunkNavManager chunkNav = ((NMapView) gui.map).getChunkNavManager();
            if (chunkNav != null && chunkNav.isInitialized()) {
                try {
                    // Use AreaNavigationHelper to find shortest path to any of the 4 corners
                    ChunkPath path = AreaNavigationHelper.findShortestPathToAreaCorners(area, chunkNav);
                    if (path != null) {
                        // ChunkNav has a path - use its cost
                        return path.totalCost;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Double.MAX_VALUE;
                }
            }
        }

        return Double.MAX_VALUE;
    }

    /**
     * Calculate distance to an area by areaId using ChunkNav.
     * Returns Double.MAX_VALUE if area is unreachable.
     */
    public double getDistanceToAreaById(String areaId, NGameUI gui) {
        NArea area = areas.get(areaId);
        if (area == null) return Double.MAX_VALUE;
        return getDistanceToArea(area, gui);
    }

    public static NArea findInGlobal(String name) {
        return findInGlobal(new NAlias(name));
    }

    public static NArea findInGlobal(NAlias name) {
        double dist = Double.MAX_VALUE;
        NArea res = null;
        NGameUI gui = NUtils.getGameUI();
        if (gui != null && gui.map != null) {
            Set<Integer> nids = gui.map.nols.keySet();
            for (Integer id : nids) {
                if (id > 0) {
                    NArea cand = gui.map.glob.map.areas.get(id);
                    if (cand != null && cand.containIn(name)) {
                        double candDist = getDistanceToArea(cand, gui);
                        if (candDist < dist) {
                            res = cand;
                            dist = candDist;
                        }
                    }
                }
            }
        }
        return res;
    }

    public static NArea findSpecGlobal(String name, String sub) {
        double dist = Double.MAX_VALUE;
        NArea target = null;
        NGameUI gui = NUtils.getGameUI();
        if (gui != null && gui.map != null) {
            Set<Integer> nids = gui.map.nols.keySet();
            for (Integer id : nids) {
                if (id > 0) {
                    NArea area = gui.map.glob.map.areas.get(id);
                    if (area == null) continue;
                    for (NArea.Specialisation s : area.spec) {
                        if (s.name.equals(name) && ((sub == null || sub.isEmpty()) || (s.subtype != null && s.subtype.equalsIgnoreCase(sub)))) {
                            double candDist = getDistanceToArea(area, gui);
                            if (candDist < dist) {
                                target = area;
                                dist = candDist;
                            }
                            break; // Found matching spec, no need to check other specs for same area
                        }
                    }
                }
            }
        }
        return target;
    }

    public static NArea findSpecGlobal(NArea.Specialisation spec) {
        return findSpecGlobal(spec.name, spec.subtype);
    }

    public static NArea findSpecGlobal(String name) {
        return findSpecGlobal(name, null);
    }

    public static NArea findOutGlobal(String name, double th, NGameUI gui) {
        NArea res = null;
        ArrayList<TestedArea> areas = new ArrayList<>();
        if (gui != null && gui.map != null) {
            Set<Integer> nids = gui.map.nols.keySet();
            for (Integer id : nids) {
                if (id > 0) {
                    NArea cand = gui.map.glob.map.areas.get(id);
                    if (cand != null && cand.containOut(name)) {
                        // Check reachability using ChunkNav or RouteGraph
                        double dist = getDistanceToArea(cand, gui);
                        if (dist < Double.MAX_VALUE) {
                            areas.add(new TestedArea(cand, cand.getOutput(name).th));
                        }
                    }
                }
            }
        }

        areas.sort(ta_comp);

        double tth = 1;
        for (TestedArea area : areas) {
            if (area.th <= th) {
                res = area.area;
                tth = area.th;
            }
        }

        // If multiple areas have the same threshold, pick the closest one
        ArrayList<NArea> targets = new ArrayList<>();
        for (TestedArea area : areas) {
            if (area.th == tth) {
                targets.add(area.area);
            }
        }

        if (targets.size() > 1) {
            double bestDist = Double.MAX_VALUE;
            for (NArea test : targets) {
                double dist = getDistanceToArea(test, gui);
                if (dist < bestDist) {
                    res = test;
                    bestDist = dist;
                }
            }
        }
        return res;
    }

    public static NArea findAreaById(int areaId) {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            Map<Integer, NArea> areas = NUtils.getGameUI().map.glob.map.areas;
            return areas.get(areaId);
        }
        return null;
    }

    public static TreeMap<Integer,NArea> findOuts(NAlias name) {
        TreeMap<Integer,NArea> areas = new TreeMap<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0)
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containOut(name.getDefault())) {
                        NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                        if(cand.getRCArea()!=null) {
                            for (int i = 0; i < cand.jout.length(); i++) {
                                if (NParser.checkName((String) ((JSONObject) cand.jout.get(i)).get("name"), name)) {
                                    Integer th = (((JSONObject) cand.jout.get(i)).has("th")) ? ((Integer) ((JSONObject) cand.jout.get(i)).get("th")) : 1;
                                    areas.put(th, cand);
                                }
                            }
                        }
                    }
            }
        }
        return areas;
    }

    public static TreeMap<Integer,NArea> findOutsGlobal(String name) {
        TreeMap<Integer,NArea> areas = new TreeMap<>();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if (id > 0)
                    if (NUtils.getGameUI().map.glob.map.areas.get(id).containOut(name)) {
                        NArea cand = NUtils.getGameUI().map.glob.map.areas.get(id);
                        if(!cand.hide) {
                            for (int i = 0; i < cand.jout.length(); i++) {
                                if (NParser.checkName((String) ((JSONObject) cand.jout.get(i)).get("name"), name)) {
                                    Integer th = (((JSONObject) cand.jout.get(i)).has("th")) ? ((Integer) ((JSONObject) cand.jout.get(i)).get("th")) : 1;
                                    areas.put(th, cand);
                                }
                            }
                        }
                    }
            }
        }
        return areas;
    }

    public static NArea findSpec(NArea.Specialisation spec) {
        if(spec.subtype==null)
            return findSpec(spec.name);
        else
            return findSpec(spec.name, spec.subtype);
    }

    public static NArea findSpec(String name) {
        double dist = 10000;
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>=0) {
                    for (NArea.Specialisation s : NUtils.getGameUI().map.glob.map.areas.get(id).spec) {
                        if (s.name.equals(name)) {
                            NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                            if(test.isVisible()) {
                                Pair<Coord2d, Coord2d> testrc = test.getRCArea();
                                if(testrc != null) {
                                    double testdist;
                                    if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                                        res = test;
                                        dist = testdist;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static NArea findSpec(String name, String sub) {
        double dist = 10000;
        NArea res = null;
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null) {
            Set<Integer> nids = NUtils.getGameUI().map.nols.keySet();
            for(Integer id : nids) {
                if(id>=0) {
                    for (NArea.Specialisation s : NUtils.getGameUI().map.glob.map.areas.get(id).spec) {
                        if (s.name.equals(name) && s.subtype != null && s.subtype.toLowerCase().equals(sub.toLowerCase())) {
                            NArea test = NUtils.getGameUI().map.glob.map.areas.get(id);
                            if(test.isVisible()) {
                                Pair<Coord2d,Coord2d> testrc = test.getRCArea();
                                if(testrc!=null) {
                                    double testdist;
                                    if ((testdist = (testrc.a.dist(NUtils.player().rc) + testrc.b.dist(NUtils.player().rc))) < dist) {
                                        res = test;
                                        dist = testdist;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     * Find all areas with a specific specialization and optional subtype.
     * Uses ChunkNav distance when available, falling back to route graph.
     * Returns list of areas sorted by distance from player.
     */
    public static ArrayList<NArea> findAllSpec(String name, String subtype) {
        // Map to store areas with their distances
        Map<NArea, Double> areaDistances = new HashMap<>();
        NGameUI gui = NUtils.getGameUI();

        if (gui != null && gui.map != null) {
            Set<Integer> nids = gui.map.nols.keySet();
            for (Integer id : nids) {
                if (id > 0) {
                    NArea area = gui.map.glob.map.areas.get(id);
                    if (area != null) {
                        for (NArea.Specialisation s : area.spec) {
                            boolean nameMatch = s.name.equals(name);
                            boolean subtypeMatch = (subtype == null || subtype.isEmpty()) ||
                                (s.subtype != null && s.subtype.equalsIgnoreCase(subtype));
                            if (nameMatch && subtypeMatch) {
                                // Check if area is reachable using ChunkNav or RouteGraph
                                double dist = getDistanceToArea(area, gui);
                                if (dist < Double.MAX_VALUE) {
                                    areaDistances.put(area, dist);
                                }
                                break; // Don't check other specs for same area
                            }
                        }
                    }
                }
            }
        }

        // Sort by distance
        ArrayList<NArea> results = new ArrayList<>(areaDistances.keySet());
        results.sort((a, b) -> Double.compare(areaDistances.get(a), areaDistances.get(b)));
        return results;
    }

    /**
     * Find all areas with a specific specialization (any subtype).
     */
    public static ArrayList<NArea> findAllSpec(String name) {
        return findAllSpec(name, null);
    }

    /**
     * Find swill delivery areas (areas with swill or trough specialization).
     * Returns areas prioritized by distance from player.
     */
    public static List<NArea> findSwillDeliveryAreas() {
        List<NArea> areas = new ArrayList<>();
//
//        // Find areas with swill specialization
//        NArea swillArea = findSpec("swill");
//        if (swillArea != null) {
//            areas.add(swillArea);
//        }

        // Find areas with trough specialization
        NArea troughArea = findSpec("trough");
        if (troughArea != null && !areas.contains(troughArea)) {
            areas.add(troughArea);
        }

        return areas;
    }
}