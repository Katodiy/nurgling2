package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.ActionWithFinal;
import nurgling.actions.bots.ScenarioRunner;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;
import nurgling.scenarios.*;
import nurgling.widgets.ScenarioBotSelectionDialog;
import nurgling.widgets.StepSettingsPanel;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScenarioPanel extends Panel {
    private ScenarioManager manager;
    private final int margin = UI.scale(10);

    private final Widget listPanel;
    private Widget editorPanel = null;

    private final SListBox<Scenario, Widget> scenarioList;

    private final TextEntry scenarioNameEntry;
    private SListBox<BotStep, Widget> stepList;

    private StepSettingsPanel stepSettingsPanel;

    private BotStep selectedStep = null;
    private Scenario editingScenario = null;

    private ScenarioBotSelectionDialog stepDialog = null;

    public ScenarioPanel() {
        super("");

        int btnWidth = UI.scale(120);
        int btnHeight = UI.scale(28);
        int titleY = UI.scale(40);

        int contentWidth = sz.x - margin * 2;
        int contentHeight = sz.y - titleY;
        int slistHeight = UI.scale(400);
        int editorListHeight = UI.scale(380);

        listPanel = add(new Widget(new Coord(contentWidth, contentHeight)), new Coord(margin, margin));
        listPanel.add(new Label("Scenarios:"), new Coord(0, 0));

        int slistWidth = contentWidth - margin * 2;
        scenarioList = listPanel.add(
                new SListBox<Scenario, Widget>(new Coord(slistWidth, slistHeight), UI.scale(32)) {
                    @Override
                    protected List<Scenario> items() {
                        return manager != null ? new ArrayList<>(manager.getScenarios().values()) : Collections.emptyList();
                    }
                    @Override
                    protected Widget makeitem(Scenario item, int idx, Coord sz) {
                        Widget w = new Widget(sz);
                        Label label = new Label(item.getName());

                        int btnW = UI.scale(60);
                        int btnS = UI.scale(8);
                        int rightPad = UI.scale(10);

                        int runBtnX = sz.x - rightPad - btnW * 3 - btnS * 2;
                        int editBtnX = sz.x - rightPad - btnW * 2 - btnS;
                        int deleteBtnX = sz.x - rightPad - btnW;

                        int labelAreaWidth = runBtnX - margin;
                        int labelX = margin + (labelAreaWidth - label.sz.x) / 2;

                        w.add(label, new Coord(labelX, (sz.y - label.sz.y) / 2));
                        w.add(new Button(btnW, "Run", () -> runScenario(item)), new Coord(runBtnX, (sz.y - btnHeight) / 2));
                        w.add(new Button(btnW, "Edit", () -> editScenario(item)), new Coord(editBtnX, (sz.y - btnHeight) / 2));
                        w.add(new Button(btnW, "Delete", () -> deleteScenario(item)), new Coord(deleteBtnX, (sz.y - btnHeight) / 2));
                        return w;
                    }
                },
                new Coord(margin, margin + UI.scale(32))
        );

        int bottomY = contentHeight - margin - btnHeight;

        listPanel.add(
                new Button(btnWidth, "Add Scenario", this::addScenario),
                new Coord((contentWidth - btnWidth) / 2, bottomY - btnHeight - UI.scale(8))
        );

        editorPanel = add(new Widget(new Coord(contentWidth, contentHeight)), new Coord(margin, margin));
        editorPanel.hide();

        int y = margin;
        editorPanel.add(new Label("Edit Scenario:"), new Coord(0, 0));
        y += UI.scale(22);

        scenarioNameEntry = editorPanel.add(new TextEntry(contentWidth - margin * 2 - 10, ""), new Coord(margin, y));
        y += UI.scale(36);

        int colSpacing = UI.scale(12);
        int stepPanelWidth = (contentWidth - margin * 2 - colSpacing) * 2 / 3;
        int settingsPanelWidth = (contentWidth - margin * 2 - colSpacing) - stepPanelWidth;

        stepList = editorPanel.add(
                new SListBox<BotStep, Widget>(new Coord(stepPanelWidth, editorListHeight), UI.scale(32)) {
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
                            String botId = step.getId();
                            BotDescriptor desc = BotRegistry.byId(botId);
                            Tex iconTex = null;
                            if (desc != null) {
                                botId = desc.displayName;
                                try {
                                    BufferedImage iconImg = Resource.loadsimg(desc.getUpIconPath());
                                    if (iconImg != null)
                                        iconTex = new TexI(iconImg);
                                } catch (Exception e) {
                                    iconTex = null;
                                }
                            }

                            // For now, only mark ✪ for goto_area, since that's the only setting-driven bot
                            boolean hasSettings = desc != null && "goto_area".equals(desc.id);
                            String marker = hasSettings ? " ✪" : "";
                            Label label = new Label(botId + marker);

                            int iconMargin = UI.scale(4);
                            int iconSize = UI.scale(24);
                            int labelX = iconTex != null ? iconSize + iconMargin * 2 : UI.scale(10);
                            int labelY = (sz.y - label.sz.y) / 2;

                            if (iconTex != null) {
                                Tex finalIconTex = iconTex;
                                add(new Widget(new Coord(iconSize, iconSize)) {
                                    @Override
                                    public void draw(GOut g) {
                                        g.image(finalIconTex, Coord.z, new Coord(iconSize, iconSize));
                                    }
                                }, new Coord(iconMargin, (sz.y - iconSize) / 2));
                            }

                            add(label, new Coord(labelX, labelY));

                            // Move Up button
                            int upBtnX = sz.x - UI.scale(110);
                            add(new Button(UI.scale(30), "↑", () -> {
                                moveStep(step, -1);
                            }), new Coord(upBtnX, (sz.y - UI.scale(28)) / 2));

                            // Move Down button
                            int downBtnX = sz.x - UI.scale(140);
                            add(new Button(UI.scale(30), "↓", () -> {
                                moveStep(step, 1);
                            }), new Coord(downBtnX, (sz.y - UI.scale(28)) / 2));

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
                                if (list.sel == this.item) {
                                    g.chcolor(50, 80, 120, 120);
                                    g.frect(Coord.z, sz);
                                    g.chcolor();
                                }
                                super.draw(g);
                            }
                            @Override
                            public boolean mousedown(MouseDownEvent ev) {
                                if (super.mousedown(ev)) return true;
                                if (ev.b == 1) {
                                    list.change(this.item);
                                    return true;
                                }
                                return false;
                            }
                        };
                    }
                },
                new Coord(margin, y)
        );

        stepSettingsPanel = editorPanel.add(
                new StepSettingsPanel(new Coord(settingsPanelWidth, editorListHeight), null),
                new Coord(margin + stepPanelWidth + colSpacing, y)
        );

        y += UI.scale(270) + UI.scale(10);

        editorPanel.add(
                new Button(btnWidth, "Add Step", this::showBotSelectDialog),
                new Coord((contentWidth - btnWidth) / 2, bottomY - btnHeight - UI.scale(8))
        );

        editorPanel.pack();
        pack();

        showListPanel();
    }

    @Override
    public void load() {
        if (stepDialog != null) {
            stepDialog.reqdestroy();
            stepDialog = null;
        }

        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            this.manager = NUtils.getUI().core.scenarioManager;
            scenarioList.update();
        }
        showListPanel();
    }

    @Override
    public void save() {
        saveScenario();
    }

    @Override
    public void show() {
        super.show();
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            this.manager = NUtils.getUI().core.scenarioManager;
            scenarioList.update();
        }
        showListPanel();
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
        editingScenario = new Scenario(s.toJson());
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
        if (stepDialog != null) {
            stepDialog.reqdestroy();
            stepDialog = null;
        }

        if (manager != null && editingScenario != null) {
            editingScenario.setName(scenarioNameEntry.text());
            manager.addOrUpdateScenario(editingScenario);
            manager.writeScenarios(null);
        }
        editingScenario = null;
        showListPanel();
    }

    private int getNextScenarioId() {
        return manager.getScenarios().keySet().stream().max(Integer::compareTo).orElse(0) + 1;
    }

    private void showBotSelectDialog() {
        if (stepDialog != null) {
            stepDialog.reqdestroy();
        }

        stepDialog = new ScenarioBotSelectionDialog(bot -> {
            if (editingScenario != null && bot != null) {
                editingScenario.addStep(new BotStep(bot.id));
                stepList.update();
            }
            stepDialog = null;
        });
        ui.root.add(stepDialog, this.c.add(50, 50));
    }

    private void runScenario(Scenario scenario) {
        if (scenario == null || NUtils.getGameUI() == null)
            return;
        ScenarioRunner runner = new ScenarioRunner(scenario);
        start("scenario_runner", runner);
    }

    private void updateStepSettingsPanel() {
        stepSettingsPanel.setStep(selectedStep);
    }

    private void moveStep(BotStep step, int direction) {
        if (editingScenario == null) return;
        List<BotStep> steps = editingScenario.getSteps();
        int idx = steps.indexOf(step);
        int newIdx = idx + direction;
        if (idx < 0 || newIdx < 0 || newIdx >= steps.size()) return;

        // Swap the steps
        Collections.swap(steps, idx, newIdx);
        stepList.update();

        // Maintain selection/focus
        selectedStep = step;
        updateStepSettingsPanel();
    }

    void start(String path, Action action)
    {
        Thread t;
        t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    action.run(NUtils.getGameUI());
                }
                catch (InterruptedException e)
                {
                    NUtils.getGameUI().msg(path + ":" + "STOPPED");
                }
                finally
                {
                    if(action instanceof ActionWithFinal)
                    {
                        ((ActionWithFinal)action).endAction();
                    }
                }
            }
        }, path);

        NUtils.getGameUI().biw.addObserve(t);

        t.start();
    }
}
