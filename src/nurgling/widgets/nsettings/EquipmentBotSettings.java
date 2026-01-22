package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.bots.EquipmentBot;
import nurgling.equipment.EquipmentPreset;
import nurgling.equipment.EquipmentPresetManager;
import nurgling.widgets.CustomIcon;
import nurgling.widgets.CustomIconManager;
import nurgling.widgets.NEquipmentPresetButton;
import nurgling.widgets.SavedIconsWindow;

import nurgling.i18n.L10n;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static haven.Inventory.invsq;
import static haven.Equipory.ebgs;
import static haven.Equipory.etts;

public class EquipmentBotSettings extends Panel implements DTarget {

    private EquipmentPresetManager manager;
    private final int margin = UI.scale(10);

    private final Widget listPanel;
    private Widget editorPanel = null;

    private final SListBox<EquipmentPreset, Widget> presetList;
    private final TextEntry presetNameEntry;
    private IButton iconPreview;
    private Button iconSelectBtn;
    private Button iconClearBtn;

    private EquipmentPreset editingPreset = null;

    // Slot configuration for editor
    private final Map<Integer, String> slotConfig = new HashMap<>();
    private final Map<Integer, Tex> slotTextures = new HashMap<>();

    // Custom horizontal layout: 2 rows of slots instead of 2 columns
    // Row 1: slots 0-11, Row 2: slots 12-22
    private static final int SLOTS_PER_ROW = 12;
    private static final Coord[] slotCoords;
    static {
        int numSlots = haven.Equipory.ecoords.length;
        slotCoords = new Coord[numSlots];
        int slotW = Inventory.sqsz.x;
        int slotH = Inventory.sqsz.y;
        int gap = UI.scale(2);
        for (int i = 0; i < numSlots; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;
            slotCoords[i] = new Coord(col * (slotW + gap), row * (slotH + gap));
        }
    }

    // Grid offset for drawing equipment slots in editor
    private static final Coord gridOffset = UI.scale(new Coord(10, 156));
    private static final Coord slotSize = Inventory.sqsz;

    public EquipmentBotSettings() {
        super("");

        int btnWidth = UI.scale(120);
        int btnHeight = UI.scale(28);
        int titleY = UI.scale(40);

        int contentWidth = sz.x - margin * 2;
        int contentHeight = sz.y - titleY;
        int slistHeight = UI.scale(400);

        // List Panel
        listPanel = add(new Widget(new Coord(contentWidth, contentHeight)), new Coord(margin, margin));
        listPanel.add(new Label(L10n.get("equipment.title")), new Coord(0, 0));
        listPanel.add(new Label(L10n.get("equipment.drag_hint")), new Coord(0, UI.scale(16)));

        int slistWidth = contentWidth - margin * 2;
        SListBox<EquipmentPreset, Widget> presetListBox = new SListBox<EquipmentPreset, Widget>(new Coord(slistWidth, slistHeight), UI.scale(40)) {
            private NEquipmentPresetButton drag = null;
            private UI.Grab grab = null;

            @Override
            protected List<EquipmentPreset> items() {
                return manager != null ? new ArrayList<>(manager.getPresets().values()) : Collections.emptyList();
            }

            @Override
            public void draw(GOut g, boolean strict) {
                super.draw(g, strict);
                if (drag != null) {
                    BufferedImage ds = drag.up;
                    Coord dssz = new Coord(ds.getWidth(), ds.getHeight());
                    ui.drawafter(new UI.AfterDraw() {
                        public void draw(GOut g) {
                            g.reclip(ui.mc.sub(dssz.div(2)), dssz);
                            g.image(new TexI(ds), ui.mc);
                        }
                    });
                }
            }

            public void drag(NEquipmentPresetButton btn) {
                if (grab == null)
                    grab = ui.grabmouse(this);
                drag = btn;
            }

            @Override
            public boolean mouseup(MouseUpEvent ev) {
                if ((grab != null) && (ev.b == 1)) {
                    grab.remove();
                    grab = null;
                    if (drag != null) {
                        DropTarget.dropthing(ui.root, ev.c.add(rootpos()), drag);
                        drag = null;
                    }
                    return true;
                }
                return super.mouseup(ev);
            }

            @Override
            protected Widget makeitem(EquipmentPreset item, int idx, Coord sz) {
                return new PresetItemWidget(this, sz, item);
            }
        };
        presetList = listPanel.add(presetListBox, new Coord(margin, margin + UI.scale(36)));

        int bottomY = contentHeight - margin - btnHeight;

        listPanel.add(
            new Button(btnWidth, L10n.get("equipment.add"), this::addPreset),
            new Coord((contentWidth - btnWidth) / 2, bottomY - btnHeight - UI.scale(8))
        );

        // Editor Panel
        editorPanel = add(new Widget(new Coord(contentWidth, contentHeight)), new Coord(margin, margin));
        editorPanel.hide();

        int y = margin;
        editorPanel.add(new Label(L10n.get("equipment.edit")), new Coord(0, 0));
        y += UI.scale(22);

        presetNameEntry = editorPanel.add(new TextEntry(contentWidth - margin * 2 - 10, ""), new Coord(margin, y));
        y += UI.scale(36);

        // Custom icon selection
        editorPanel.add(new Label("Icon:"), new Coord(margin, y));
        y += UI.scale(18);

        // Icon preview (32x32)
        BufferedImage defaultIcon = createDefaultIconImage();
        iconPreview = editorPanel.add(new IButton(defaultIcon, defaultIcon, defaultIcon), new Coord(margin, y));

        // Select icon button
        iconSelectBtn = editorPanel.add(new Button(UI.scale(80), "Select") {
            @Override
            public void click() {
                openIconSelectWindow();
            }
        }, new Coord(margin + UI.scale(40), y));

        // Clear icon button
        iconClearBtn = editorPanel.add(new Button(UI.scale(60), "Clear") {
            @Override
            public void click() {
                if (editingPreset != null) {
                    editingPreset.setCustomIconId(null);
                    updateIconPreview();
                }
            }
        }, new Coord(margin + UI.scale(128), y));

        y += UI.scale(36);

        editorPanel.add(new Label(L10n.get("equipment.drag_items")), new Coord(margin, y));
        y += UI.scale(18);
        editorPanel.add(new Label(L10n.get("equipment.rightclick_hint")), new Coord(margin, y));

        // Equipment slots are drawn in draw() method

        // Buttons at bottom
        int btnY = bottomY - btnHeight - UI.scale(8);
        editorPanel.add(new Button(btnWidth, L10n.get("equipment.equip_now"), this::equipNow), new Coord(margin, btnY));

        editorPanel.pack();
        pack();

        showListPanel();
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);

        // Draw equipment slots only in editor panel
        if (editorPanel != null && editorPanel.visible()) {
            // Adjust position for editor panel offset
            Coord panelPos = editorPanel.c;

            for (int i = 0; i < slotCoords.length; i++) {
                Coord pos = panelPos.add(gridOffset).add(slotCoords[i]);

                g.image(invsq, pos);

                if (ebgs[i] != null) {
                    g.image(ebgs[i], pos);
                }

                if (slotConfig.containsKey(i)) {
                    Tex itemTex = slotTextures.get(i);
                    if (itemTex != null) {
                        Coord texSz = itemTex.sz();
                        Coord targetSz = slotSize.sub(2, 2);

                        double scale = Math.min(
                            (double) targetSz.x / texSz.x,
                            (double) targetSz.y / texSz.y
                        );

                        if (scale < 1.0) {
                            Coord scaledSz = new Coord((int)(texSz.x * scale), (int)(texSz.y * scale));
                            Coord drawPos = pos.add(1, 1).add(targetSz.sub(scaledSz).div(2));
                            g.image(itemTex, drawPos, scaledSz);
                        } else {
                            Coord drawPos = pos.add(slotSize.div(2)).sub(texSz.div(2));
                            g.image(itemTex, drawPos);
                        }
                    } else {
                        g.chcolor(100, 150, 100, 180);
                        g.frect(pos.add(2, 2), slotSize.sub(4, 4));
                        g.chcolor();
                    }
                }
            }
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        if (editorPanel != null && editorPanel.visible()) {
            int slot = getSlotAt(c);
            if (slot >= 0 && slot < slotCoords.length) {
                String slotName = (etts[slot] != null) ? etts[slot].text : "Slot " + slot;

                if (slotConfig.containsKey(slot)) {
                    String resName = slotConfig.get(slot);
                    String itemName = resName;
                    if (resName.contains("/")) {
                        itemName = resName.substring(resName.lastIndexOf("/") + 1);
                    }
                    return slotName + ": " + itemName;
                }
                return slotName + " " + L10n.get("equipment.empty");
            }
        }
        return super.tooltip(c, prev);
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if (editorPanel != null && editorPanel.visible() && ev.b == 3) {
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
        if (editorPanel != null && editorPanel.visible()) {
            int slot = getSlotAt(cc);
            if (slot >= 0) {
                WItem handItem = NUtils.getGameUI().vhand;
                if (handItem != null && handItem.item != null) {
                    Resource res = handItem.item.getres();
                    if (res != null) {
                        String resName = res.name;
                        slotConfig.put(slot, resName);

                        Resource.Image img = res.layer(Resource.imgc);
                        if (img != null) {
                            slotTextures.put(slot, new TexI(img.scaled()));
                        }
                        return true;
                    }
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
        if (editorPanel == null || !editorPanel.visible()) {
            return -1;
        }
        Coord panelPos = editorPanel.c;
        for (int i = 0; i < slotCoords.length; i++) {
            Coord pos = panelPos.add(gridOffset).add(slotCoords[i]);
            if (c.isect(pos, slotSize)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void load() {
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            this.manager = NUtils.getUI().core.equipmentPresetManager;
            presetList.update();
        }
        showListPanel();
    }

    @Override
    public void save() {
        savePreset();
    }

    @Override
    public void show() {
        super.show();
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            this.manager = NUtils.getUI().core.equipmentPresetManager;
            presetList.update();
        }
        showListPanel();
    }

    private void showListPanel() {
        listPanel.show();
        editorPanel.hide();
        presetList.update();
    }

    private void showEditorPanel() {
        listPanel.hide();
        editorPanel.show();
        presetNameEntry.settext(editingPreset != null ? editingPreset.getName() : "");
        updateIconPreview();

        // Load slot config from editing preset
        slotConfig.clear();
        slotTextures.clear();
        if (editingPreset != null) {
            for (Map.Entry<Integer, String> entry : editingPreset.getSlotConfig().entrySet()) {
                slotConfig.put(entry.getKey(), entry.getValue());
                try {
                    Resource res = Resource.remote().loadwait(entry.getValue());
                    if (res != null) {
                        Resource.Image img = res.layer(Resource.imgc);
                        if (img != null) {
                            slotTextures.put(entry.getKey(), new TexI(img.scaled()));
                        }
                    }
                } catch (Exception e) {
                    // Resource not available
                }
            }
        }
    }

    private void editPreset(EquipmentPreset preset) {
        editingPreset = new EquipmentPreset(preset.toJson());
        showEditorPanel();
    }

    private void addPreset() {
        if (manager == null) {
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
                this.manager = NUtils.getUI().core.equipmentPresetManager;
            } else {
                return;
            }
        }
        editingPreset = new EquipmentPreset("New Preset");
        showEditorPanel();
    }

    private void deletePreset(EquipmentPreset preset) {
        if (manager != null) {
            manager.deletePreset(preset.getId());
            manager.writePresets(null);
            presetList.update();
        }
    }

    private void savePreset() {
        if (manager != null && editingPreset != null) {
            editingPreset.setName(presetNameEntry.text());
            editingPreset.setSlotConfig(new HashMap<>(slotConfig));
            manager.addOrUpdatePreset(editingPreset);
            manager.writePresets(null);
        }
        editingPreset = null;
        showListPanel();
    }

    private void equipNow() {
        if (slotConfig.isEmpty()) {
            NUtils.getGameUI().msg(L10n.get("equipment.no_config"));
            return;
        }

        // Create a temporary preset with current config
        EquipmentPreset tempPreset = new EquipmentPreset("Temp");
        tempPreset.setSlotConfig(new HashMap<>(slotConfig));

        Thread t = new Thread(() -> {
            try {
                new EquipmentBot(tempPreset).run(NUtils.getGameUI());
            } catch (InterruptedException e) {
                NUtils.getGameUI().msg(L10n.get("equipment.stopped"));
            }
        }, "EquipmentBot");

        NUtils.getGameUI().biw.addObserve(t);
        t.start();
    }

    private class PresetItemWidget extends Widget {
        private final Object parentList;
        private final EquipmentPreset preset;
        private NEquipmentPresetButton presetBtn;
        private Coord dp;

        PresetItemWidget(Object parentList, Coord sz, EquipmentPreset preset) {
            super(sz);
            this.parentList = parentList;
            this.preset = preset;

            Label label = new Label(preset.getName());

            int btnW = UI.scale(60);
            int btnS = UI.scale(8);
            int rightPad = UI.scale(10);
            int presetBtnSize = UI.scale(32);
            int presetBtnSpacing = UI.scale(12);

            // Add draggable preset button at the far left
            presetBtn = new NEquipmentPresetButton(preset);
            add(presetBtn, new Coord(margin, (sz.y - presetBtnSize) / 2));

            int editBtnX = sz.x - rightPad - btnW * 2 - btnS;
            int deleteBtnX = sz.x - rightPad - btnW;

            int labelX = margin + presetBtnSize + presetBtnSpacing;

            add(label, new Coord(labelX, (sz.y - label.sz.y) / 2));
            int itemBtnHeight = UI.scale(28);
            add(new Button(btnW, L10n.get("common.edit"), () -> editPreset(preset)), new Coord(editBtnX, (sz.y - itemBtnHeight) / 2));
            add(new Button(btnW, L10n.get("common.delete"), () -> deletePreset(preset)), new Coord(deleteBtnX, (sz.y - itemBtnHeight) / 2));
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            Coord btnPos = presetBtn.c;
            Coord btnSz = presetBtn.sz;
            if (ev.c.isect(btnPos, btnSz)) {
                if (ev.b == 1) {
                    dp = ev.c;
                    return true;
                }
            }
            return super.mousedown(ev);
        }

        @Override
        public void mousemove(MouseMoveEvent ev) {
            if ((dp != null) && (ev.c.dist(dp) > 5)) {
                dp = null;
                try {
                    java.lang.reflect.Method dragMethod = parentList.getClass().getMethod("drag", NEquipmentPresetButton.class);
                    dragMethod.invoke(parentList, presetBtn);
                } catch (Exception e) {
                    // Fallback
                }
            }
            super.mousemove(ev);
        }

        @Override
        public boolean mouseup(MouseUpEvent ev) {
            if (dp != null) {
                Coord btnPos = presetBtn.c;
                Coord btnSz = presetBtn.sz;
                if (ev.c.isect(btnPos, btnSz) && ev.c.dist(dp) <= 5) {
                    dp = null;
                    presetBtn.click();
                    return true;
                } else {
                    dp = null;
                    return true;
                }
            }
            return super.mouseup(ev);
        }
    }

    private void openIconSelectWindow() {
        SavedIconsWindow window = new SavedIconsWindow(icon -> {
            if (editingPreset != null && icon != null) {
                editingPreset.setCustomIconId(icon.getId());
                updateIconPreview();
            }
        });
        ui.root.add(window, ui.root.sz.div(2).sub(window.sz.div(2)));
    }

    private void updateIconPreview() {
        String iconId = editingPreset != null ? editingPreset.getCustomIconId() : null;
        BufferedImage img;

        if (iconId != null) {
            CustomIcon customIcon = CustomIconManager.getInstance().getIcon(iconId);
            if (customIcon != null) {
                img = customIcon.getImage(0);
            } else {
                img = createDefaultIconImage();
            }
        } else {
            img = createDefaultIconImage();
        }

        if (iconPreview != null) {
            // IButton fields are final, so we need to recreate it
            Coord pos = iconPreview.c;
            iconPreview.reqdestroy();
            iconPreview = editorPanel.add(new IButton(img, img, img), pos);
        }
    }

    private BufferedImage createDefaultIconImage() {
        int size = UI.scale(32);
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(60, 80, 60));
        g.fillRoundRect(0, 0, size, size, 4, 4);
        g.setColor(new java.awt.Color(100, 120, 100));
        g.drawRoundRect(0, 0, size - 1, size - 1, 4, 4);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, UI.scale(10)));
        String text = "None";
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (size - textWidth) / 2, size / 2 + UI.scale(4));
        g.dispose();
        return img;
    }
}
