package nurgling.actions.bots;

import haven.Coord;
import haven.Resource;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Build;
import nurgling.areas.NArea;
import nurgling.areas.NContext;
import nurgling.conf.ConstructionMaterialsRegistry;
import nurgling.tools.NAlias;

import java.awt.image.BufferedImage;

/**
 * Helper class for construction bots to get materials from construction materials zones
 * or fallback to user selection if zones are not defined.
 * Uses NArea for all areas to support global navigation.
 */
public class BuildMaterialHelper {

    private final NContext context;
    private final NGameUI gui;

    public BuildMaterialHelper(NContext context, NGameUI gui) {
        this.gui = gui;
        this.context = context;
    }

    public BuildMaterialHelper(NGameUI gui) {
        this.gui = gui;
        this.context = new NContext(gui);
    }

    /**
     * Get ingredient from construction materials zone or ask user to select area.
     * Uses NArea for global navigation support - if the zone is in a different location,
     * the bot will navigate there using global pathfinding.
     * 
     * @param itemSize Size of item in inventory
     * @param itemAlias Alias of the item to find
     * @param count Number of items needed
     * @param selectIcon Icon to show if user needs to select area
     * @param selectMessage Message to show if user needs to select area
     * @return Build.Ingredient with NArea set
     */
    public Build.Ingredient getIngredient(
            Coord itemSize,
            NAlias itemAlias,
            int count,
            BufferedImage selectIcon,
            String selectMessage
    ) throws InterruptedException {
        // Try to get from construction materials zone first
        NArea zone = context.getBuildMaterialArea(itemAlias);
        if (zone != null) {
            String materialName = itemAlias.getKeys().isEmpty() ? "material" : itemAlias.getKeys().get(0);
            NUtils.getGameUI().msg("Using construction materials area for " + materialName);
            return new Build.Ingredient(itemSize, zone, itemAlias, count);
        }
        
        // Fallback to user selection - create temp NArea
        NUtils.getGameUI().msg(selectMessage);
        String areaId = context.createArea(selectMessage, selectIcon);
        NArea selectedArea = context.getAreaById(areaId);
        return new Build.Ingredient(itemSize, selectedArea, itemAlias, count);
    }

    /**
     * Get ingredient from construction materials zone or ask user to select area.
     * Uses resource path for icon.
     */
    public Build.Ingredient getIngredient(
            Coord itemSize,
            NAlias itemAlias,
            int count,
            String iconResourcePath,
            String selectMessage
    ) throws InterruptedException {
        return getIngredient(itemSize, itemAlias, count, Resource.loadsimg(iconResourcePath), selectMessage);
    }

    /**
     * Check if a construction materials zone exists for the given item.
     */
    public boolean hasZone(NAlias itemAlias) {
        return context.hasBuildMaterialArea(itemAlias);
    }

    /**
     * Check if a construction materials zone exists for the given material type.
     */
    public boolean hasZone(ConstructionMaterialsRegistry.MaterialType materialType) {
        return context.hasBuildMaterialArea(materialType);
    }

    /**
     * Get the NContext used by this helper.
     */
    public NContext getContext() {
        return context;
    }

    // Convenience methods for common materials

    /**
     * Get blocks ingredient (auto-zone or user selection)
     */
    public Build.Ingredient getBlocks(int count) throws InterruptedException {
        return getIngredient(
            new Coord(1, 2),
            new NAlias("Block"),
            count,
            "baubles/blockIng",
            "Please, select area for blocks"
        );
    }

    /**
     * Get boards ingredient (auto-zone or user selection)
     */
    public Build.Ingredient getBoards(int count) throws InterruptedException {
        return getIngredient(
            new Coord(4, 1),
            new NAlias("Board"),
            count,
            "baubles/boardIng",
            "Please, select area for boards"
        );
    }

    /**
     * Get string/fibre ingredient (auto-zone or user selection)
     */
    public Build.Ingredient getStrings(int count) throws InterruptedException {
        NAlias stringAlias = new NAlias(
            "Flax Fibres", "Hemp Fibres", "Spindly Taproot", "Cattail Fibres",
            "Stinging Nettle", "Grass Twine", "Hide Strap", "Straw Twine",
            "Bark Cordage", "Toadflax"
        );
        return getIngredient(
            new Coord(1, 1),
            stringAlias,
            count,
            "baubles/stringsIng",
            "Please, select area for strings"
        );
    }

    /**
     * Get stone ingredient (auto-zone or user selection)
     */
    public Build.Ingredient getStone(int count) throws InterruptedException {
        return getIngredient(
            new Coord(1, 1),
            new NAlias("Stone"),
            count,
            "baubles/chipperPiles",
            "Please, select area for stone"
        );
    }

    /**
     * Get nuggets ingredient (auto-zone or user selection)
     */
    public Build.Ingredient getNuggets(int count) throws InterruptedException {
        return getIngredient(
            new Coord(1, 1),
            new NAlias("Nugget"),
            count,
            "baubles/nugget",
            "Please, select area for nuggets"
        );
    }

    /**
     * Get bricks ingredient (auto-zone or user selection)
     */
    public Build.Ingredient getBricks(int count) throws InterruptedException {
        return getIngredient(
            new Coord(1, 1),
            new NAlias("Brick"),
            count,
            "baubles/bricks",
            "Please, select area for bricks"
        );
    }

    /**
     * Get metal bars ingredient (auto-zone or user selection)
     */
    public Build.Ingredient getMetalBars(int count) throws InterruptedException {
        return getIngredient(
            new Coord(1, 1),
            new NAlias("Bar"),
            count,
            "baubles/nugget",
            "Please, select area for metal bars"
        );
    }

    /**
     * Get plant fibre ingredient (auto-zone or user selection)
     * This includes Finer Plant Fibre, Plant Fibre, Prepared Tree Bast
     */
    public Build.Ingredient getFibres(int count) throws InterruptedException {
        NAlias fibreAlias = new NAlias(
            "Finer Plant Fibre", "Plant Fibre", "Prepared Tree Bast"
        );
        return getIngredient(
            new Coord(1, 1),
            fibreAlias,
            count,
            "baubles/stringsIng",
            "Please, select area for plant fibre"
        );
    }

    /**
     * Get finer plant fibre specifically (auto-zone or user selection)
     */
    public Build.Ingredient getFinerPlantFibre(int count) throws InterruptedException {
        return getIngredient(
            new Coord(1, 1),
            new NAlias("Finer Plant Fibre"),
            count,
            "baubles/stringsIng",
            "Please, select area for finer plant fibre"
        );
    }
}

