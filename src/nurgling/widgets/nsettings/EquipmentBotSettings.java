package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.widgets.NEquipory;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static haven.Inventory.invsq;
import static haven.Equipory.ecoords;
import static haven.Equipory.ebgs;
import static haven.Equipory.etts;

public class EquipmentBotSettings extends Panel implements DTarget {

    // Slot configuration: slot index -> resource name
    private final Map<Integer, String> slotConfig = new HashMap<>();

    // Cached textures for configured items (scaled to fit slot)
    private final Map<Integer, Tex> slotTextures = new HashMap<>();

    // Grid offset for drawing
    private static final Coord gridOffset = UI.scale(new Coord(10, 70));

    // Slot size (same as inventory slot)
    private static final Coord slotSize = Inventory.sqsz;

    public EquipmentBotSettings() {
        super("Equipment Bot Settings");

        // Buttons positioned below the equipment slots (slots go to row 10, ~11 rows total)
        int y = UI.scale(480);

        add(new Label("Drag items from inventory onto slots to configure."), UI.scale(10, 36));
        add(new Label("Right-click a slot to clear it."), UI.scale(10, 52));

        // Add clear all button
        add(new Button(UI.scale(100), "Clear All") {
            @Override
            public void click() {
                slotConfig.clear();
                slotTextures.clear();
            }
        }, new Coord(UI.scale(10), y));

        // Add equip now button
        add(new Button(UI.scale(100), "Equip Now") {
            @Override
            public void click() {
                equipConfiguredItems();
            }
        }, new Coord(UI.scale(120), y));
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);

        // Draw all equipment slots using the same layout as Equipory
        for (int i = 0; i < ecoords.length; i++) {
            Coord pos = gridOffset.add(ecoords[i]);

            // Draw slot background (inventory square)
            g.image(invsq, pos);

            // Draw slot type indicator (the little icon showing what type of slot)
            if (ebgs[i] != null) {
                g.image(ebgs[i], pos);
            }

            // Draw configured item if any
            if (slotConfig.containsKey(i)) {
                Tex itemTex = slotTextures.get(i);
                if (itemTex != null) {
                    // Scale and center the item texture to fit in slot
                    Coord texSz = itemTex.sz();
                    Coord targetSz = slotSize.sub(2, 2); // Leave 1px border

                    // Calculate scale factor to fit in slot
                    double scale = Math.min(
                        (double) targetSz.x / texSz.x,
                        (double) targetSz.y / texSz.y
                    );

                    if (scale < 1.0) {
                        // Need to scale down
                        Coord scaledSz = new Coord((int)(texSz.x * scale), (int)(texSz.y * scale));
                        Coord drawPos = pos.add(1, 1).add(targetSz.sub(scaledSz).div(2));
                        g.image(itemTex, drawPos, scaledSz);
                    } else {
                        // No scaling needed, just center
                        Coord drawPos = pos.add(slotSize.div(2)).sub(texSz.div(2));
                        g.image(itemTex, drawPos);
                    }
                } else {
                    // Draw placeholder if texture not loaded
                    g.chcolor(100, 150, 100, 180);
                    g.frect(pos.add(2, 2), slotSize.sub(4, 4));
                    g.chcolor();
                }
            }
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        int slot = getSlotAt(c);
        if (slot >= 0 && slot < ecoords.length) {
            // Use the actual slot tooltip from Equipory
            String slotName = (etts[slot] != null) ? etts[slot].text : "Slot " + slot;

            if (slotConfig.containsKey(slot)) {
                String resName = slotConfig.get(slot);
                // Extract just the item name from resource path
                String itemName = resName;
                if (resName.contains("/")) {
                    itemName = resName.substring(resName.lastIndexOf("/") + 1);
                }
                return slotName + ": " + itemName;
            }
            return slotName + " (empty)";
        }
        return super.tooltip(c, prev);
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if (ev.b == 3) { // Right-click to clear
            int slot = getSlotAt(ev.c);
            if (slot >= 0) {
                slotConfig.remove(slot);
                slotTextures.remove(slot);
                return true;
            }
        }
        return super.mousedown(ev);
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
        int slot = getSlotAt(cc);
        if (slot >= 0) {
            WItem handItem = NUtils.getGameUI().vhand;
            if (handItem != null && handItem.item != null) {
                Resource res = handItem.item.getres();
                if (res != null) {
                    String resName = res.name;
                    slotConfig.put(slot, resName);

                    // Cache the item texture
                    Resource.Image img = res.layer(Resource.imgc);
                    if (img != null) {
                        slotTextures.put(slot, new TexI(img.scaled()));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        return drop(cc, ul);
    }

    private int getSlotAt(Coord c) {
        for (int i = 0; i < ecoords.length; i++) {
            Coord pos = gridOffset.add(ecoords[i]);
            if (c.isect(pos, slotSize)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load() {
        slotConfig.clear();
        slotTextures.clear();

        Object configObj = NConfig.get(NConfig.Key.equipmentBotConfig);
        if (configObj instanceof Map) {
            Map<String, Object> config = (Map<String, Object>) configObj;
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                try {
                    int slot = Integer.parseInt(entry.getKey());
                    String resName = (String) entry.getValue();
                    slotConfig.put(slot, resName);

                    // Try to load texture
                    try {
                        Resource res = Resource.remote().loadwait(resName);
                        if (res != null) {
                            Resource.Image img = res.layer(Resource.imgc);
                            if (img != null) {
                                slotTextures.put(slot, new TexI(img.scaled()));
                            }
                        }
                    } catch (Exception e) {
                        // Resource not available, will show placeholder
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
    }

    @Override
    public void save() {
        Map<String, String> config = new HashMap<>();
        for (Map.Entry<Integer, String> entry : slotConfig.entrySet()) {
            config.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        NConfig.set(NConfig.Key.equipmentBotConfig, config);
        NConfig.needUpdate();
    }

    private void equipConfiguredItems() {
        if (slotConfig.isEmpty()) {
            NUtils.getGameUI().msg("No equipment configured!", Color.YELLOW);
            return;
        }

        // Run the equipment bot
        Thread t = new Thread(() -> {
            try {
                new nurgling.actions.bots.EquipmentBot().run(NUtils.getGameUI());
            } catch (InterruptedException e) {
                NUtils.getGameUI().msg("Equipment bot stopped.");
            }
        }, "EquipmentBot");

        NUtils.getGameUI().biw.addObserve(t);
        t.start();
    }
}
