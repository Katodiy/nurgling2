package nurgling.actions.bots.cheese;

import haven.Coord;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

/**
 * Central location for all cheese-related constants
 * Eliminates 38+ magic value occurrences across cheese classes
 */
public class CheeseConstants {
    
    // ============== ITEM CONSTANTS ==============
    
    /** Cheese tray item name - used 23+ times across cheese classes */
    public static final String CHEESE_TRAY_NAME = "Cheese Tray";
    
    /** Cheese tray alias for inventory queries - eliminates repeated NAlias creation */
    public static final NAlias CHEESE_TRAY_ALIAS = new NAlias(CHEESE_TRAY_NAME);
    
    /** Empty cheese tray name for storage operations */
    public static final String EMPTY_CHEESE_TRAY_NAME = "Empty Cheese Tray";
    
    // ============== SIZE CONSTANTS ==============
    
    /** Cheese tray size in inventory (1x2) - used 8+ times across cheese classes */
    public static final Coord CHEESE_TRAY_SIZE = new Coord(1, 2);
    
    /** Single slot size for cheese pieces and small items */
    public static final Coord SINGLE_SLOT_SIZE = new Coord(1, 1);
    
    /** Inventory slots needed for slicing (tray + up to 5 cheese pieces) */
    public static final int SLICING_INVENTORY_REQUIREMENT = 7;

    /** Total space needed before transferring a tray for slicing (slicing requirement + tray size) */
    public static final int SLICING_TOTAL_SPACE_REQUIREMENT = SLICING_INVENTORY_REQUIREMENT + 2;
    
    // ============== AREA CONSTANTS ==============
    
    /** Cheese racks specialization name - used 7+ times across cheese classes */
    public static final Specialisation.SpecName CHEESE_RACKS_SPEC = Specialisation.SpecName.cheeseRacks;
    
    /** Cheese rack game object resource name */
    public static final String CHEESE_RACK_RESOURCE = "gfx/terobjs/cheeserack";
    
    /** Rack container type name */
    public static final String RACK_CONTAINER_TYPE = "Rack";
    
    // ============== WORKFLOW CONSTANTS ==============
    
    /** Minimum curds needed to fill a cheese tray */
    public static final int CURDS_PER_TRAY = 4;
    
    /** Reserved inventory space for curds when fetching trays */
    public static final int RESERVED_CURD_SLOTS = 2; // 2 tray-sized slots for 4 curds (1x1 each)
}