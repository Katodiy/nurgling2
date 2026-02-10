package nurgling.widgets;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.bots.EquipmentBot;
import nurgling.equipment.EquipmentPreset;
import nurgling.equipment.EquipmentPresetIcons;

public class NEquipmentPresetButton extends IButton {
    private final EquipmentPreset preset;

    public NEquipmentPresetButton(EquipmentPreset preset) {
        super(
            EquipmentPresetIcons.loadPresetIconUp(preset),
            EquipmentPresetIcons.loadPresetIconDown(preset),
            EquipmentPresetIcons.loadPresetIconHover(preset)
        );
        this.preset = preset;
        setupButton();
    }

    private void setupButton() {
        this.action(() -> executePreset());
    }

    private void executePreset() {
        if (preset != null) {
            Thread t = new Thread(() -> {
                try {
                    EquipmentBot bot = new EquipmentBot(preset);
                    bot.run(NUtils.getGameUI());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "EquipmentBot-" + preset.getName());

            NUtils.getGameUI().biw.addObserve(t);
            t.start();
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        if (preset != null) {
            return "Equip: " + preset.getName();
        }
        return super.tooltip(c, prev);
    }

    public EquipmentPreset getPreset() {
        return preset;
    }

    public String getDisplayName() {
        return preset != null ? preset.getName() : "Unknown Preset";
    }
}
