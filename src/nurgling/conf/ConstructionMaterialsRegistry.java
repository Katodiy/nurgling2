package nurgling.conf;

import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.*;

/**
 * Registry for construction materials and their zone subtypes.
 * Used to automatically find construction material zones for building bots.
 * 
 * When a building bot needs materials (like blocks), it can use this registry
 * to find the appropriate buildMaterials zone instead of asking the user.
 * 
 * Zone specialization: buildMaterials
 * Subtypes: Block, Board, Stone, String, Nugget, MetalBar, Clay, Brick, Thatch
 */
public class ConstructionMaterialsRegistry {

    /**
     * Material types that can be stored in construction material zones
     */
    public enum MaterialType {
        BLOCK("Block"),
        BOARD("Board"),
        STONE("Stone"),
        STRING("String"),       // Flax Fibres, Hemp Fibres, etc.
        NUGGET("Nugget"),
        METAL_BAR("Metal Bar"),
        CLAY("Clay"),
        BRICK("Brick"),
        THATCH("Thatch"),       // Straw, Dried Bush, etc.
        BRANCH("Branch"),
        BOUGH("Bough"),
        LOG("Log");

        private final String subtype;

        MaterialType(String subtype) {
            this.subtype = subtype;
        }

        public String getSubtype() {
            return subtype;
        }
    }

    /**
     * Maps item name aliases to material types
     */
    private static final Map<MaterialType, NAlias> MATERIAL_ALIASES = new HashMap<>();

    static {
        // Blocks
        MATERIAL_ALIASES.put(MaterialType.BLOCK, new NAlias("Block"));
        
        // Boards
        MATERIAL_ALIASES.put(MaterialType.BOARD, new NAlias("Board"));
        
        // Stone
        MATERIAL_ALIASES.put(MaterialType.STONE, new NAlias("Stone"));
        
        // String materials (fibres, twines, etc.)
        MATERIAL_ALIASES.put(MaterialType.STRING, new NAlias(
            "Flax Fibres", "Hemp Fibres", "Spindly Taproot", "Cattail Fibres", 
            "Stinging Nettle", "Grass Twine", "Hide Strap", "Straw Twine", 
            "Bark Cordage", "Toadflax"
        ));
        
        // Nuggets
        MATERIAL_ALIASES.put(MaterialType.NUGGET, new NAlias("Nugget"));
        
        // Metal Bars
        MATERIAL_ALIASES.put(MaterialType.METAL_BAR, new NAlias("Bar"));
        
        // Clay
        MATERIAL_ALIASES.put(MaterialType.CLAY, new NAlias("Clay"));
        
        // Brick
        MATERIAL_ALIASES.put(MaterialType.BRICK, new NAlias("Brick"));
        
        // Thatch materials (all valid thatching materials)
        MATERIAL_ALIASES.put(MaterialType.THATCH, new NAlias(
            "Straw", "Reeds", "Glimmermoss", "Tarsticks", "Brown Kelp", "Dried Bush"
        ));
        
        // Branch
        MATERIAL_ALIASES.put(MaterialType.BRANCH, new NAlias("Branch"));
        
        // Bough
        MATERIAL_ALIASES.put(MaterialType.BOUGH, new NAlias("Bough"));
        
        // Log
        MATERIAL_ALIASES.put(MaterialType.LOG, new NAlias("Log"));
    }

    /**
     * Get the material type for a given item alias.
     * First tries exact match, then word-based matching.
     */
    public static MaterialType getMaterialType(NAlias itemAlias) {
        // First pass: exact match (case-insensitive)
        for (Map.Entry<MaterialType, NAlias> entry : MATERIAL_ALIASES.entrySet()) {
            for (String key : itemAlias.getKeys()) {
                for (String materialKey : entry.getValue().getKeys()) {
                    if (key.equalsIgnoreCase(materialKey)) {
                        return entry.getKey();
                    }
                }
            }
        }
        
        // Second pass: check if item key starts with material key (for things like "Straw" matching items "Straw")
        // But avoid "Straw" matching "Straw Twine" - only match if it's a word boundary
        for (Map.Entry<MaterialType, NAlias> entry : MATERIAL_ALIASES.entrySet()) {
            for (String key : itemAlias.getKeys()) {
                for (String materialKey : entry.getValue().getKeys()) {
                    String keyLower = key.toLowerCase();
                    String matLower = materialKey.toLowerCase();
                    // Match if key equals materialKey or if materialKey is a complete word at start of key
                    if (keyLower.equals(matLower) || 
                        (keyLower.startsWith(matLower) && keyLower.length() > matLower.length() && 
                         !Character.isLetterOrDigit(keyLower.charAt(matLower.length())))) {
                        return entry.getKey();
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Get the zone subtype for a given item alias
     */
    public static String getZoneSubtype(NAlias itemAlias) {
        MaterialType type = getMaterialType(itemAlias);
        return type != null ? type.getSubtype() : null;
    }

    /**
     * Find a construction materials zone for the given material type
     */
    public static NArea findMaterialZone(MaterialType materialType) {
        NArea area = NContext.findSpec(
            Specialisation.SpecName.buildMaterials.toString(),
            materialType.getSubtype()
        );
        if (area == null) {
            area = NContext.findSpecGlobal(
                Specialisation.SpecName.buildMaterials.toString(),
                materialType.getSubtype()
            );
        }
        return area;
    }

    /**
     * Find a construction materials zone for the given item alias
     */
    public static NArea findMaterialZone(NAlias itemAlias) {
        MaterialType type = getMaterialType(itemAlias);
        if (type != null) {
            return findMaterialZone(type);
        }
        return null;
    }

    /**
     * Check if a construction materials zone exists for the given material type
     */
    public static boolean hasMaterialZone(MaterialType materialType) {
        return findMaterialZone(materialType) != null;
    }

    /**
     * Check if a construction materials zone exists for the given item alias
     */
    public static boolean hasMaterialZone(NAlias itemAlias) {
        return findMaterialZone(itemAlias) != null;
    }

    /**
     * Get the NAlias for a material type
     */
    public static NAlias getMaterialAlias(MaterialType materialType) {
        return MATERIAL_ALIASES.get(materialType);
    }

    /**
     * Get all available material types
     */
    public static Collection<MaterialType> getAllMaterialTypes() {
        return Arrays.asList(MaterialType.values());
    }

    /**
     * Get all zone subtypes as a list of strings
     */
    public static List<String> getAllSubtypes() {
        List<String> subtypes = new ArrayList<>();
        for (MaterialType type : MaterialType.values()) {
            subtypes.add(type.getSubtype());
        }
        return subtypes;
    }
}

