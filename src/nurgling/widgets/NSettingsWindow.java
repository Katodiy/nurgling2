package nurgling.widgets;


import haven.*;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.widgets.nsettings.*;
import nurgling.widgets.options.*;

import java.util.*;

public class NSettingsWindow extends Widget {

    private static TexI rbtn = new TexI(Resource.loadsimg("nurgling/hud/buttons/right/u"));
    private static TexI dbtn = new TexI(Resource.loadsimg("nurgling/hud/buttons/down/u"));
    private final SettingsList list;
    public World world;
    Widget container;
    public Panel currentPanel = null;
    private Button saveBtn, cancelBtn, backBtn;
    public QuickActions qa;
    public AutoSelection as;
    public QoL qol;
    private Runnable backAction;

    public NSettingsWindow() {
        this(null);
    }

    public NSettingsWindow(Runnable backAction) {
        this.backAction = backAction;
        sz = UI.scale(800, 600);
        container = add(new Widget(Coord.z));
        list = add(new SettingsList(UI.scale(200, 580)), UI.scale(10, 10));

        saveBtn = add(new Button(UI.scale(100), "Save") {
            public void click() {
                if(currentPanel != null) {
                    currentPanel.save();
                }
            }
        }, UI.scale(680, 560));

        cancelBtn = add(new Button(UI.scale(100), "Cancel") {
            public void click() {
                if(currentPanel != null) {
                    currentPanel.load();
                }
            }
        }, UI.scale(580, 560));

        // Add Back button only if back action is provided
        if(backAction != null) {
            backBtn = add(new Button(UI.scale(100), "Back") {
                public void click() {
                    backAction.run();
                }
                
                public boolean keydown(KeyDownEvent ev) {
                    if(ev.c == 27) { // ESC key
                        backAction.run();
                        return true;
                    }
                    return super.keydown(ev);
                }
            }, UI.scale(480, 560));
        }

        fillSettings();
        container.resize(UI.scale(800, 600));
    }


    private void fillSettings() {
        SettingsCategory general = new SettingsCategory("General", new Panel("General"), container);
        general.addChild(new SettingsItem("Fonts", new Fonts(), container));
        general.addChild(new SettingsItem("Quality of life", qol = new QoL(), container));
        general.addChild(new SettingsItem("Database", new DatabaseSettings(), container));
        general.addChild(new SettingsItem("Auto Mapper", new AutoMapper(), container));
        general.addChild(new SettingsItem("Auto Selection", as = new AutoSelection(), container));
        general.addChild(new SettingsItem("Quick Actions", qa = new QuickActions(), container));

        SettingsCategory gameenvironment = new SettingsCategory("Game environment", new Panel("Game environment"), container);
        gameenvironment.addChild(new SettingsItem("World",world = new World(), container));
        gameenvironment.addChild(new SettingsItem("Animal rings", new NRingSettings(), container));

        SettingsCategory scenarios = new SettingsCategory("Autorunner", new Panel("Autorunner scenarios"), container);
        scenarios.addChild(new SettingsItem("Scenarios", new ScenarioPanel(), container));

        SettingsCategory bots = new SettingsCategory("Bots", new Panel("Bots"), container);
        bots.addChild(new SettingsItem("Feed Clover", new FeedClover(), container));
        bots.addChild(new SettingsItem("Auto Drop settings", new Dropper(), container));
        bots.addChild(new SettingsItem("Eating bot", new Eater(), container));
        bots.addChild(new SettingsItem("Farming Settings", new FarmingSettingsPanel(), container));
        bots.addChild(new SettingsItem("Cheese orders", new CheeseOrdersPanel(), container));

        list.addCategory(general);
        list.addCategory(gameenvironment);
        list.addCategory(scenarios);
        list.addCategory(bots);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
            if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
                ((NMapView) NUtils.getGameUI().map).destroyRouteDummys();
                NUtils.getGameUI().map.glob.oc.paths.pflines = null;
            }
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    private class SettingsList extends SListBox<SettingsItem, SettingsListItem> {
        public SettingsList(Coord sz) {
            super(sz, UI.scale(24));
        }

        @Override
        protected List<? extends SettingsItem> items() {
            List<SettingsItem> allItems = new ArrayList<>();
            for (SettingsItem item : categories) {
                allItems.add(item);
                if(item.expanded)
                    allItems.addAll(item.getChildren());
            }
            return allItems;
        }

        @Override
        protected SettingsListItem makeitem(SettingsItem item, int idx, Coord sz) {
            return new SettingsListItem(this, sz, item);
        }

        private final List<SettingsCategory> categories = new ArrayList<>();

        public void addCategory(SettingsCategory category) {
            categories.add(category);
            update();
        }

        public void update() {
            super.update();
        }
    }

    private class SettingsListItem extends SListWidget.ItemWidget<SettingsItem> {
        private final Text text;


        public SettingsListItem(SListWidget<SettingsItem, ?> list, Coord sz, SettingsItem item) {
            super(list, sz, item);

            int indent = item.getLevel() * UI.scale(15);

            this.text = Text.render(item.getName());

            if (!item.getChildren().isEmpty()) {
                add(new Button(UI.scale(20), "+"), indent, 0).action(() -> {
                    item.expanded = !item.expanded;
                    ((SettingsList)list).update();
                });
            }
        }

        @Override
        public void draw(GOut g) {
            if(!item.getChildren().isEmpty()) {
                g.image(item.expanded ? dbtn : rbtn, Coord.of(UI.scale(5), (sz.y - text.sz().y) / 2));
            }
            int indent = item.getLevel() * UI.scale(5);
            g.image(text.tex(), Coord.of(indent + UI.scale(25), (sz.y - text.sz().y) / 2));
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (super.mousedown(ev)) {
                NSettingsWindow.this.showSettings(item);
                return true;
            }
            list.change(item);
            return true;
        }
    }

    private static class SettingsItem {
        public Widget panel;
        private boolean expanded = false;
        private final String name;
        private final List<SettingsItem> children = new ArrayList<>();
        private SettingsItem parent;

        public SettingsItem(String name, Widget panel, Widget container) {
            this.name = name;
            this.panel = panel;
            container.add(panel, UI.scale(210,0));
            panel.hide();
        }

        public String getName() { return name; }
        public List<SettingsItem> getChildren() { return children; }

        public void addChild(SettingsItem child) {
            child.parent = this;
            children.add(child);
        }

        public int getLevel() {
            return parent == null ? 0 : parent.getLevel() + 1;
        }
    }

    private static class SettingsCategory extends SettingsItem {
        public SettingsCategory(String name, Widget panel, Widget container) {
            super(name, panel, container);
        }
    }

    private void showSettings(SettingsItem item) {
        if(currentPanel != null)
            currentPanel.hide();
        currentPanel = (Panel)item.panel;
        currentPanel.show();
        currentPanel.load();
    }


}