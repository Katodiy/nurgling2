package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A meter widget that displays the amount of water and tea the player is carrying.
 * Scans belt, hands, feet, and inventory for drink containers.
 */
public class DrinkMeter extends Widget {
    // Use stamina meter frame as base (from nurgling resources)
    private static final Tex FRAME = Resource.loadtex("nurgling/hud/meter/water");
    private static final Color BG = new Color(40, 51, 46);
    private static final Color TEA = new Color(147, 110, 68);
    private static final Color WATER = new Color(82, 116, 188);

    // Pattern to parse content strings like "6.55l of Water" or "3 l of Tea"
    private static final Pattern CONTENT_PATTERN = Pattern.compile("([\\d.]+)\\s*l\\s+of\\s+(.+)", Pattern.CASE_INSENSITIVE);

    // Container capacities in liters
    private static final float BUCKET_CAPACITY = 10.0f;
    private static final float WATERSKIN_CAPACITY = 3.0f;
    private static final float GLASSJUG_CAPACITY = 5.0f;
    private static final float WATERFLASK_CAPACITY = 2.0f;
    private static final float KUKSA_CAPACITY = 0.8f;

    // Drink container names
    private static final NAlias DRINK_CONTAINERS = new NAlias("Waterskin", "Glass Jug", "Waterflask", "Kuksa");

    private float water = 0f;
    private float tea = 0f;
    private float max = 0f;
    private double wait = 0;

    /**
     * Returns the total amount of drinkable liquid (water + tea) in liters.
     * @return total water and tea amount in liters
     */
    public float getTotalDrinkable() {
        return water + tea;
    }

    /**
     * Returns the current water amount in liters.
     * @return water amount in liters
     */
    public float getWater() {
        return water;
    }

    /**
     * Returns the current tea amount in liters.
     * @return tea amount in liters
     */
    public float getTea() {
        return tea;
    }

    // Text displayed on the meter bar
    private Tex text = null;
    private String lastTextValue = null;

    // Tooltip caching
    private String cachedTipText = null;
    private Tex cachedTip = null;

    public DrinkMeter() {
        super(IMeter.fsz);  // UI.scale(190, 48)
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        wait -= dt;
        if (wait <= 0) {
            wait = process() ? 0.25 : 1.0;
        }
    }

    private boolean process() {
        if (ui == null || ui.gui == null) return false;
        NGameUI gui = (NGameUI) ui.gui;

        // Reset counters
        water = tea = max = 0f;

        NEquipory equip = NUtils.getEquipment();
        if (equip == null) return false;

        // 1. Scan Belt for drink containers
        WItem belt = equip.quickslots[NEquipory.Slots.BELT.idx];
        if (belt != null && belt.item != null && belt.item.contents != null) {
            scanContainerContents(belt.item.contents);
        }

        // 2. Scan Hands for Buckets and drink containers
        processEquipSlot(equip, NEquipory.Slots.HAND_LEFT.idx);
        processEquipSlot(equip, NEquipory.Slots.HAND_RIGHT.idx);

        // 3. Scan Feet for Waterskins/Glass Jugs
        processEquipSlot(equip, NEquipory.Slots.LFOOT.idx);
        processEquipSlot(equip, NEquipory.Slots.RFOOT.idx);

        // 4. Scan main inventory for drink containers
        NInventory inv = gui.getInventory();
        if (inv != null) {
            scanInventoryForContainers(inv);
        }

        // Update text display
        updateText();

        return true;
    }

    /**
     * Updates the text displayed on the meter bar
     */
    private void updateText() {
        String newText;
        float total = water + tea;

        if (max <= 0) {
            newText = "";
        } else {
            // Format: "1.5 / 15 l" similar to health meter style
            newText = String.format("%.1f / %.1f l", total, max);
        }

        // Only recreate texture if text changed
        if (!newText.equals(lastTextValue)) {
            lastTextValue = newText;
            if (text != null) {
                text.dispose();
                text = null;
            }
            if (!newText.isEmpty()) {
                text = NStyle.meter.render(newText).tex();
            }
        }
    }

    /**
     * Scans a container (like belt contents) for drink containers
     */
    private void scanContainerContents(Widget contents) {
        if (contents == null) return;

        for (Widget w = contents.child; w != null; w = w.next) {
            if (w instanceof WItem) {
                WItem witem = (WItem) w;
                if (witem.item instanceof NGItem) {
                    NGItem ngitem = (NGItem) witem.item;
                    String name = ngitem.name();
                    if (name != null && NParser.checkName(name, DRINK_CONTAINERS)) {
                        float capacity = getContainerCapacity(name);
                        processContainer(ngitem, capacity);
                    }
                }
            }
        }
    }

    /**
     * Scans main inventory for drink containers
     */
    private void scanInventoryForContainers(NInventory inv) {
        for (Widget w = inv.child; w != null; w = w.next) {
            if (w instanceof WItem) {
                WItem witem = (WItem) w;
                if (witem.item instanceof NGItem) {
                    NGItem ngitem = (NGItem) witem.item;
                    String name = ngitem.name();
                    if (name != null && NParser.checkName(name, DRINK_CONTAINERS)) {
                        float capacity = getContainerCapacity(name);
                        processContainer(ngitem, capacity);
                    }
                }
            }
        }
    }

    /**
     * Processes an equipment slot for drink containers or buckets
     */
    private void processEquipSlot(NEquipory equip, int slotIdx) {
        WItem item = equip.quickslots[slotIdx];
        if (item == null || !(item.item instanceof NGItem)) return;

        NGItem ngitem = (NGItem) item.item;
        String name = ngitem.name();
        if (name == null) return;

        // Check for bucket
        if (NParser.checkName(name, "Bucket")) {
            processContainer(ngitem, BUCKET_CAPACITY);
            return;
        }

        // Check for drink containers
        if (NParser.checkName(name, DRINK_CONTAINERS)) {
            float capacity = getContainerCapacity(name);
            processContainer(ngitem, capacity);
        }
    }

    /**
     * Gets the capacity of a container based on its name
     */
    private float getContainerCapacity(String name) {
        if (name == null) return 0f;

        if (NParser.checkName(name, "Bucket")) return BUCKET_CAPACITY;
        if (NParser.checkName(name, "Waterskin")) return WATERSKIN_CAPACITY;
        if (NParser.checkName(name, "Glass Jug")) return GLASSJUG_CAPACITY;
        if (NParser.checkName(name, "Waterflask")) return WATERFLASK_CAPACITY;
        if (NParser.checkName(name, "Kuksa")) return KUKSA_CAPACITY;

        return 0f;
    }

    /**
     * Processes a container item, extracting water/tea content
     */
    private void processContainer(NGItem ngitem, float capacity) {
        if (capacity <= 0) return;

        boolean hasWaterOrTea = false;
        boolean hasOtherLiquid = false;

        for (NGItem.NContent content : ngitem.content()) {
            String contentName = content.name();
            if (contentName == null) continue;

            Matcher m = CONTENT_PATTERN.matcher(contentName);
            if (m.find()) {
                try {
                    float amount = Float.parseFloat(m.group(1));
                    String type = m.group(2).trim();

                    if (type.contains("Water")) {
                        water += amount;
                        hasWaterOrTea = true;
                    } else if (type.contains("Tea")) {
                        tea += amount;
                        hasWaterOrTea = true;
                    } else {
                        // Other liquid (milk, honey, etc.)
                        hasOtherLiquid = true;
                    }
                } catch (NumberFormatException ignored) {
                    // Skip malformed content
                }
            }
        }

        // Only add to max capacity if the container is empty or contains water/tea
        // Don't count containers with other liquids
        if (!hasOtherLiquid || hasWaterOrTea || ngitem.content().isEmpty()) {
            max += capacity;
        }
    }

    @Override
    public void draw(GOut g) {
        Coord isz = IMeter.msz;  // UI.scale(130, 20)
        Coord off = IMeter.off;  // UI.scale(24, 4)

        // Draw background
        g.chcolor(BG);
        g.frect(off, isz);

        if (max > 0) {
            // Draw tea layer (behind water, so tea + water shows full amount)
            if (tea > 0) {
                g.chcolor(TEA);
                float a = Math.min((tea + water) / max, 1.0f);
                g.frect(off, new Coord(Math.round(isz.x * a), isz.y));
            }
            // Draw water layer (in front)
            if (water > 0) {
                g.chcolor(WATER);
                float a = Math.min(water / max, 1.0f);
                g.frect(off, new Coord(Math.round(isz.x * a), isz.y));
            }
        }
        g.chcolor();

        // Draw frame
        g.image(FRAME, Coord.z);

        // Draw text centered on the meter bar
        if (text != null) {
            Coord textPos = new Coord(
                off.x + isz.x / 2 - text.sz().x / 2,
                off.y + isz.y / 2 - text.sz().y / 2
            );
            g.image(text, textPos);
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        if (max <= 0) return null;

        StringBuilder tt = new StringBuilder();
        if (water > 0) {
            tt.append(String.format("%.2f l of Water\n", water));
        }
        if (tea > 0) {
            tt.append(String.format("%.2f l of Tea\n", tea));
        }
        tt.append(String.format("Capacity: %.2f litres", max));

        String tipText = tt.toString();
        if (!tipText.equals(cachedTipText)) {
            cachedTipText = tipText;
            if (cachedTip != null) {
                cachedTip.dispose();
            }
            cachedTip = RichText.render(tipText, 0).tex();
        }

        return cachedTip;
    }

    @Override
    public void dispose() {
        if (cachedTip != null) {
            cachedTip.dispose();
            cachedTip = null;
        }
        if (text != null) {
            text.dispose();
            text = null;
        }
        super.dispose();
    }
}
