package nurgling.widgets;

import haven.*;
import nurgling.actions.bots.ScenarioRunner;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;
import nurgling.scenarios.*;
import haven.Label;
import haven.Window;
import nurgling.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScenarioWidget extends Window {
    private ScenarioManager manager;
    private final int margin = UI.scale(10);

    // Panels
    private final Widget listPanel;
    private Widget editorPanel = null;

    // Scenario list controls
    private final SListBox<Scenario, Widget> scenarioList;
    private final Button addScenarioButton;

    // Editor controls
    private final TextEntry scenarioNameEntry;
    private SListBox<BotStep, Widget> stepList;
    // Add step buttons etc.
    private final Button saveButton, cancelButton;

    private StepSettingsPanel stepSettingsPanel;

    private BotStep selectedStep = null;

    private Scenario editingScenario = null;

    public ScenarioWidget() {
//        super(UI.scale(new Coord(420, 520)), "Scenarios");
        super(new Coord(UI.scale(370) + UI.scale(10)*2, 520), "Scenarios");
        int btnWidth = UI.scale(120);
        int btnSpacing = UI.scale(20);
        int contentWidth = UI.scale(500); // Match your other controls
        int btnHeight = UI.scale(28);
        int margin = UI.scale(10);

        int totalBtnWidth = btnWidth * 2 + btnSpacing;
        int btnStartX = (contentWidth - totalBtnWidth) / 2;

        // --- List Panel ---
        listPanel = add(new Widget(new Coord(contentWidth, sz.y)), new Coord(margin, 0));
        listPanel.add(new Label("Scenarios:"), new Coord(margin, margin));

        scenarioList = listPanel.add(new SListBox<Scenario, Widget>(UI.scale(new Coord(470, 302)), UI.scale(32)) {
            @Override
            protected List<Scenario> items() {
                return manager != null ? new ArrayList<>(manager.getScenarios().values()) : Collections.emptyList();
            }
            @Override
            protected Widget makeitem(Scenario item, int idx, Coord sz) {
                Widget w = new Widget(sz);
                Label label = new Label(item.getName());

                int btnWidth = UI.scale(60);
                int btnSpacing = UI.scale(8);
                int rightPadding = UI.scale(10);

                // We now have 3 buttons: Run, Edit, Delete
                int runBtnX = sz.x - rightPadding - btnWidth * 3 - btnSpacing * 2;
                int editBtnX = sz.x - rightPadding - btnWidth * 2 - btnSpacing;
                int deleteBtnX = sz.x - rightPadding - btnWidth;

                int labelAreaWidth = runBtnX - margin;
                int labelX = margin + (labelAreaWidth - label.sz.x) / 2;

                w.add(label, new Coord(labelX, (sz.y - label.sz.y) / 2));

                // RUN button (new)
                w.add(new Button(btnWidth, "Run", () -> runScenario(item)), new Coord(runBtnX, (sz.y - UI.scale(28)) / 2));

                // EDIT button
                w.add(new Button(btnWidth, "Edit", () -> editScenario(item)), new Coord(editBtnX, (sz.y - UI.scale(28)) / 2));

                // DELETE button
                w.add(new Button(btnWidth, "Delete", () -> deleteScenario(item)), new Coord(deleteBtnX, (sz.y - UI.scale(28)) / 2));
                return w;
            }

        }, new Coord(margin, margin + UI.scale(32)));

        addScenarioButton = listPanel.add(
                new Button(btnWidth, "Add Scenario", this::addScenario),
                new Coord((contentWidth - btnWidth) / 2, margin + UI.scale(370))
        );

        // --- Editor Panel ---
        editorPanel = add(new Widget(new Coord(contentWidth, sz.y)), new Coord(margin, 0));
        editorPanel.hide();

        int y = margin;

        editorPanel.add(new Label("Edit Scenario:"), new Coord(margin, y));
        y += UI.scale(22);

        scenarioNameEntry = editorPanel.add(new TextEntry((contentWidth - margin * 2) - 10, ""), new Coord(margin, y));
        y += UI.scale(36);

        int listWidth = contentWidth - margin * 2 - UI.scale(180) - UI.scale(12); // leave space for right panel
        int settingsPanelWidth = UI.scale(180);

        stepList = editorPanel.add(new SListBox<BotStep, Widget>(new Coord(listWidth, UI.scale(270)), UI.scale(32)) {
            @Override
            protected List<BotStep> items() {
                return editingScenario != null ? editingScenario.getSteps() : Collections.emptyList();
            }
            @Override
            public void change(BotStep item) {
                super.change(item);
                selectedStep = item;
                stepSettingsPanel.setStep(selectedStep);
            }
            @Override
            protected Widget makeitem(BotStep step, int idx, Coord sz) {
                return new ItemWidget<BotStep>(this, sz, step) {{
                    String botName = step.getBotKey();
                    BotDescriptor desc = BotRegistry.getDescriptor(step.getBotKey());
                    if (desc != null)
                        botName = desc.displayName;

                    boolean hasSettings = desc != null && !desc.factory.requiredSettings().isEmpty();
                    String marker = hasSettings ? " ✪" : "";
                    Label label = new Label(botName + marker);

                    int labelX = UI.scale(10);
                    int labelY = (sz.y - label.sz.y) / 2;
                    add(label, new Coord(labelX, labelY));

                    int removeBtnX = sz.x - UI.scale(80);
                    add(new Button(UI.scale(70), "Remove", () -> {
                        if (editingScenario != null) {
                            editingScenario.getSteps().remove(step);
                            stepList.update();
                            if (selectedStep == step) {
                                selectedStep = null;
                                updateStepSettingsPanel();
                            }
                        }
                    }), new Coord(removeBtnX, (sz.y - UI.scale(28)) / 2));
                }
                    @Override
                    public void draw(GOut g) {
                        // Highlight if selected
                        if (list.sel == this.item) {
                            g.chcolor(50, 80, 120, 120);
                            g.frect(Coord.z, sz);
                            g.chcolor();
                        }
                        super.draw(g);
                    }

                    @Override
                    public boolean mousedown(MouseDownEvent ev) {
                        if (ev.b == 1) {
                            list.change(this.item); // select on left click
                            return true;
                        }
                        return super.mousedown(ev);
                    }
                };
            }

        }, new Coord(margin, y));
        stepSettingsPanel = editorPanel.add(
                new StepSettingsPanel(new Coord(settingsPanelWidth, UI.scale(270)), null),
                new Coord(margin + listWidth + UI.scale(12), y)
        );

        y += UI.scale(270) + UI.scale(10);

        int bottomY = editorPanel.sz.y - margin - btnHeight;

        Button addStepButton = editorPanel.add(
                new Button(btnWidth, "Add Step", this::showBotSelectDialog),
                new Coord((contentWidth - btnWidth) / 2, bottomY - btnHeight - UI.scale(8)) // Centered above Save/Cancel
        );

        saveButton = editorPanel.add(
                new Button(btnWidth, "Save", this::saveScenario),
                new Coord(btnStartX, bottomY)
        );

        cancelButton = editorPanel.add(
                new Button(btnWidth, "Cancel", this::cancelEdit),
                new Coord(btnStartX + btnWidth + btnSpacing, bottomY)
        );

        editorPanel.pack();
        pack();
    }

    @Override
    public void show() {
        super.show();
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            this.manager = ((NMapView) NUtils.getGameUI().map).scenarioManager;
            scenarioList.update();
        }
        showListPanel();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
                NUtils.getGameUI().map.glob.oc.paths.pflines = null;
            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private void showListPanel() {
        listPanel.show();
        editorPanel.hide();
        scenarioList.update();
    }

    private void showEditorPanel() {
        listPanel.hide();
        editorPanel.show();
        scenarioNameEntry.settext(editingScenario != null ? editingScenario.getName() : "");
        stepList.update();
        List<BotStep> steps = editingScenario != null ? editingScenario.getSteps() : Collections.emptyList();
        if (!steps.isEmpty()) {
            stepList.change(steps.get(0));
        } else {
            selectedStep = null;
            stepSettingsPanel.setStep(null);
        }
    }

    private void editScenario(Scenario s) {
        editingScenario = cloneScenario(s);
        showEditorPanel();
    }

    private void addScenario() {
        editingScenario = new Scenario(getNextScenarioId(), "New Scenario");
        showEditorPanel();
    }

    private void deleteScenario(Scenario s) {
        if (manager != null) {
            manager.deleteScenario(s.getId());
            manager.writeScenarios(null);
            scenarioList.update();
        }
    }

    private void saveScenario() {
        if (manager != null && editingScenario != null) {
            editingScenario.setName(scenarioNameEntry.text());
            manager.addOrUpdateScenario(editingScenario);
            manager.writeScenarios(null);
        }
        editingScenario = null;
        showListPanel();
    }

    private void cancelEdit() {
        editingScenario = null;
        showListPanel();
    }

    private int getNextScenarioId() {
        return manager.getScenarios().keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }

    private Scenario cloneScenario(Scenario s) {
        return new Scenario(s.toJson());
    }

    private void showBotSelectDialog() {
        ui.root.add(new ScenarioBotSelectionDialog(bot -> {
            if (editingScenario != null && bot != null) {
                editingScenario.addStep(new BotStep(bot.key));
                stepList.update();
            }
        }), this.c.add(50, 50)); // You can adjust position as you wish
    }

    private void runScenario(Scenario scenario) {
        if (scenario == null || NUtils.getGameUI() == null)
            return;

        // This is your bot runner. Adjust import if needed.
        ScenarioRunner runner = new ScenarioRunner(scenario);

        // You probably want to run this in a new thread, as with other bots:
        new Thread(() -> {
            try {
                runner.run(NUtils.getGameUI());
            } catch (InterruptedException e) {
                NUtils.getGameUI().msg("ScenarioRunner: Interrupted!");
            }
        }, "ScenarioRunnerThread").start();
    }

    private void updateStepSettingsPanel() {
        stepSettingsPanel.setStep(selectedStep);
    }

    private void clearChildren(Widget parent) {
        Widget child = parent.child;
        while (child != null) {
            Widget next = child.next;
            child.destroy();
            child = next;
        }
    }

}

