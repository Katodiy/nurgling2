package nurgling.widgets.nsettings;

import haven.*;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.ActionWithFinal;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;
import nurgling.scenarios.*;
import nurgling.widgets.CustomIcon;
import nurgling.widgets.CustomIconManager;
import nurgling.widgets.NScenarioButton;
import nurgling.widgets.SavedIconsWindow;
import nurgling.widgets.ScenarioBotSelectionDialog;
import nurgling.widgets.StepSettingsPanel;

import nurgling.i18n.L10n;

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
    private IButton iconPreview;
    private Button iconSelectBtn;
    private Button iconClearBtn;

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
        listPanel.add(new Label(L10n.get("scenarios.title")), new Coord(0, 0));

        int slistWidth = contentWidth - margin * 2;
        SListBox<Scenario, Widget> scenarioListBox = new SListBox<Scenario, Widget>(new Coord(slistWidth, slistHeight), UI.scale(40)) {
            private NScenarioButton drag = null;
            private UI.Grab grab = null;
            
            @Override
            protected List<Scenario> items() {
                return manager != null ? new ArrayList<>(manager.getScenarios().values()) : Collections.emptyList();
            }
            
            @Override
            public void draw(GOut g, boolean strict) {
                super.draw(g, strict);
                if(drag != null) {
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
            
            public void drag(NScenarioButton btn) {
                if(grab == null)
                    grab = ui.grabmouse(this);
                drag = btn;
            }
            
            @Override
            public boolean mouseup(MouseUpEvent ev) {
                if((grab != null) && (ev.b == 1)) {
                    grab.remove();
                    grab = null;
                    if(drag != null) {
                        DropTarget.dropthing(ui.root, ev.c.add(rootpos()), drag);
                        drag = null;
                    }
                    return true;
                }
                return super.mouseup(ev);
            }
            
            @Override
            protected Widget makeitem(Scenario item, int idx, Coord sz) {
                return new ScenarioItemWidget(this, sz, item);
            }
        };
        scenarioList = listPanel.add(scenarioListBox,
                new Coord(margin, margin + UI.scale(32))
        );

        int bottomY = contentHeight - margin - btnHeight;

        listPanel.add(
                new Button(btnWidth, L10n.get("scenarios.add"), this::addScenario),
                new Coord((contentWidth - btnWidth) / 2, bottomY - btnHeight - UI.scale(8))
        );

        editorPanel = add(new Widget(new Coord(contentWidth, contentHeight)), new Coord(margin, margin));
        editorPanel.hide();

        int y = margin;
        editorPanel.add(new Label(L10n.get("scenarios.edit")), new Coord(0, 0));
        y += UI.scale(22);

        scenarioNameEntry = editorPanel.add(new TextEntry(contentWidth - margin * 2 - 10, ""), new Coord(margin, y));
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
                if (editingScenario != null) {
                    editingScenario.setCustomIconId(null);
                    updateIconPreview();
                }
            }
        }, new Coord(margin + UI.scale(128), y));

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
                                botId = desc.getDisplayName();
                                try {
                                    BufferedImage iconImg = Resource.loadsimg(desc.getUpIconPath());
                                    if (iconImg != null)
                                        iconTex = new TexI(iconImg);
                                } catch (Exception e) {
                                    iconTex = null;
                                }
                            }

                            // Mark ✪ for bots that have settings
                            boolean hasSettings = desc != null && ("goto_area".equals(desc.id) || "forager".equals(desc.id));
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
                            int upBtnX = sz.x - UI.scale(90);
                            add(new IButton(NStyle.upSquareArrow[0].back, NStyle.upSquareArrow[1].back, NStyle.upSquareArrow[2].back) {
                                @Override
                                public void click() {
                                    moveStep(step, -1);
                                }

                            }, new Coord(upBtnX, (sz.y - UI.scale(22)) / 2));

                            // Move Down button
                            int downBtnX = sz.x - UI.scale(60);

                            add(new IButton(NStyle.downSquareArrow[0].back, NStyle.downSquareArrow[1].back, NStyle.downSquareArrow[2].back) {
                                @Override
                                public void click() {
                                    moveStep(step, 1);
                                }

                            }, new Coord(downBtnX, (sz.y - UI.scale(22)) / 2));

                            int removeBtnX = sz.x - UI.scale(30);

                            add(new IButton(NStyle.crossSquare[0].back, NStyle.crossSquare[1].back, NStyle.crossSquare[2].back) {
                                @Override
                                public void click() {
                                    if (editingScenario != null) {
                                    editingScenario.getSteps().remove(step);
                                    stepList.update();
                                    if (selectedStep == step) {
                                        selectedStep = null;
                                        updateStepSettingsPanel();
                                    }
                                }
                                }

                            }, new Coord(removeBtnX, (sz.y - UI.scale(22)) / 2));
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
                new Button(btnWidth, L10n.get("scenarios.add_step"), this::showBotSelectDialog),
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
        updateIconPreview();
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
        if (manager == null) {
            // Try to initialize manager
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
                this.manager = NUtils.getUI().core.scenarioManager;
            } else {
                // Can't create scenario without manager
                return;
            }
        }
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
        if (manager == null) {
            return 1;
        }
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
    
    private class ScenarioItemWidget extends Widget {
        private final Object parentList;
        private final Scenario scenario;
        private NScenarioButton scenarioBtn;
        private Coord dp;
        
        ScenarioItemWidget(Object parentList, Coord sz, Scenario scenario) {
            super(sz);
            this.parentList = parentList;
            this.scenario = scenario;
            
            Label label = new Label(scenario.getName());

            int btnW = UI.scale(60);
            int btnS = UI.scale(8);
            int rightPad = UI.scale(10);
            int scenarioBtnSize = UI.scale(32); // Actual size of scenario button icons
            int scenarioBtnSpacing = UI.scale(12); // More horizontal spacing between button and text

            // Add draggable scenario button at the far left with more margin
            scenarioBtn = new NScenarioButton(scenario);
            add(scenarioBtn, new Coord(margin, (sz.y - scenarioBtnSize) / 2));

            int runBtnX = sz.x - rightPad - btnW * 3 - btnS * 2;
            int editBtnX = sz.x - rightPad - btnW * 2 - btnS;
            int deleteBtnX = sz.x - rightPad - btnW;

            // Adjust label position to account for scenario button with more spacing
            int labelX = margin + scenarioBtnSize + scenarioBtnSpacing;

            add(label, new Coord(labelX, (sz.y - label.sz.y) / 2));
            int itemBtnHeight = UI.scale(28);
            add(new Button(btnW, L10n.get("scenarios.btn_edit"), () -> editScenario(scenario)), new Coord(editBtnX, (sz.y - itemBtnHeight) / 2));
            add(new Button(btnW, L10n.get("scenarios.btn_delete"), () -> deleteScenario(scenario)), new Coord(deleteBtnX, (sz.y - itemBtnHeight) / 2));
        }
        
        @Override
        public boolean mousedown(MouseDownEvent ev) {
            // Check if click is within scenario button area
            Coord btnPos = scenarioBtn.c;
            Coord btnSz = scenarioBtn.sz;
            if(ev.c.isect(btnPos, btnSz)) {
                if(ev.b == 1) {
                    dp = ev.c;
                    return true;
                }
            }
            return super.mousedown(ev);
        }
        
        @Override
        public void mousemove(MouseMoveEvent ev) {
            if((dp != null) && (ev.c.dist(dp) > 5)) {
                dp = null;
                // Use reflection to call the drag method
                try {
                    java.lang.reflect.Method dragMethod = parentList.getClass().getMethod("drag", NScenarioButton.class);
                    dragMethod.invoke(parentList, scenarioBtn);
                } catch (Exception e) {
                    // Fallback - shouldn't happen
                }
            }
            super.mousemove(ev);
        }
        
        @Override
        public boolean mouseup(MouseUpEvent ev) {
            if(dp != null) {
                // Check if this was a click (no drag) within the button area
                Coord btnPos = scenarioBtn.c;
                Coord btnSz = scenarioBtn.sz;
                if(ev.c.isect(btnPos, btnSz) && ev.c.dist(dp) <= 5) {
                    // This was a click, not a drag - forward to the button
                    dp = null;
                    scenarioBtn.click();
                    return true;
                } else {
                    // This was a drag or outside button area
                    dp = null;
                    return true;
                }
            }
            return super.mouseup(ev);
        }
    }

    private void openIconSelectWindow() {
        SavedIconsWindow window = new SavedIconsWindow(icon -> {
            if (editingScenario != null && icon != null) {
                editingScenario.setCustomIconId(icon.getId());
                updateIconPreview();
            }
        });
        ui.root.add(window, ui.root.sz.div(2).sub(window.sz.div(2)));
    }

    private void updateIconPreview() {
        String iconId = editingScenario != null ? editingScenario.getCustomIconId() : null;
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
        g.setColor(new java.awt.Color(60, 60, 80));
        g.fillRoundRect(0, 0, size, size, 4, 4);
        g.setColor(new java.awt.Color(100, 100, 120));
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
