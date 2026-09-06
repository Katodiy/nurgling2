package nurgling.widgets;


import haven.*;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import nurgling.widgets.nsettings.*;
import nurgling.widgets.nsettings.search.*;
import nurgling.widgets.options.*;

import java.awt.Color;
import java.util.*;

public class NSettingsWindow extends Widget {

    private static TexI rbtn = new TexI(Resource.loadsimg("nurgling/hud/buttons/right/u"));
    private static TexI dbtn = new TexI(Resource.loadsimg("nurgling/hud/buttons/down/u"));

    /* Sidebar geometry. The tree used to own the whole 200x580 column; it now starts below
     * the search field and its one-line result counter, and still ends where it always did. */
    private static final int SIDE_X = 10, SIDE_Y = 10, SIDE_W = 200, SIDE_BOTTOM = 590;
    private static final int INFO_H = UI.scale(13);
    private static final int RESULT_H = UI.scale(30);
    private static final int MAX_RESULTS = 40;
    private static final double HIGHLIGHT_TIME = 3.0;

    private static final Text.Foundry crumbfnd = new Text.Foundry(Text.dfont, 9).aa(true);
    private static final Color crumbcol = new Color(158, 145, 120);
    private static final Color sectioncol = new Color(190, 210, 235);
    private static final Color panelcol = new Color(235, 210, 150);

    private final SettingsList list;
    private final SearchField searchField;
    private final ResultList results;
    private final int infoY;

    private List<SettingEntry> index = null;
    private List<SettingEntry> hits = Collections.emptyList();
    private String query = "";
    private String infotext = null;
    private Text infotex = null;
    private SettingEntry highlight = null;
    private double highlightEnd = 0;
    public World world;
    public Navigation navigation;
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

        searchField = add(new SearchField(UI.scale(SIDE_W)), UI.scale(SIDE_X, SIDE_Y));
        infoY = UI.scale(SIDE_Y) + searchField.sz.y + UI.scale(2);
        int listY = infoY + INFO_H;
        Coord listSz = new Coord(UI.scale(SIDE_W), UI.scale(SIDE_BOTTOM) - listY);
        list = add(new SettingsList(listSz), new Coord(UI.scale(SIDE_X), listY));
        /* Both lists occupy the same rectangle; exactly one is visible at a time. */
        results = add(new ResultList(listSz), new Coord(UI.scale(SIDE_X), listY));
        results.hide();

        saveBtn = add(new Button(UI.scale(100), L10n.get("nsettings.btn.save")) {
            public void click() {
                if(currentPanel != null) {
                    currentPanel.save();
                }
            }
        }, UI.scale(680, 560));

        cancelBtn = add(new Button(UI.scale(100), L10n.get("nsettings.btn.cancel")) {
            public void click() {
                if(currentPanel != null) {
                    currentPanel.load();
                }
            }
        }, UI.scale(580, 560));

        // Add Back button only if back action is provided
        if(backAction != null) {
            backBtn = add(new Button(UI.scale(100), L10n.get("nsettings.btn.back")) {
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
        SettingsCategory general = new SettingsCategory(L10n.get("nsettings.cat.general"), new Panel(L10n.get("nsettings.cat.general")), container);
        general.addChild(new SettingsItem(L10n.get("nsettings.item.fonts"), new Fonts(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.item_overlays"), new ItemOverlaySettings(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.navigation"), navigation = new Navigation(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.map_settings"), new MapSettings(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.qol"), qol = new QoL(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.database"), new DatabaseSettings(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.auto_mapper"), new AutoMapper(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.auto_selection"), as = new AutoSelection(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.quick_actions"), qa = new QuickActions(), container));
        general.addChild(new SettingsItem(L10n.get("nsettings.item.discord"), new DiscordSettings(), container));

        SettingsCategory gameenvironment = new SettingsCategory(L10n.get("nsettings.cat.game_environment"), new Panel(L10n.get("nsettings.cat.game_environment")), container);
        gameenvironment.addChild(new SettingsItem(L10n.get("nsettings.item.world"), world = new World(), container));
        gameenvironment.addChild(new SettingsItem(L10n.get("nsettings.item.object_hiding"), new ObjectHiding(), container));
        gameenvironment.addChild(new SettingsItem(L10n.get("nsettings.item.animal_rings"), new NRingSettings(), container));
        gameenvironment.addChild(new SettingsItem(L10n.get("nsettings.item.critter_circles"), new nurgling.widgets.options.NCritterCircleSettings(), container));
        gameenvironment.addChild(new SettingsItem("Combat HUD", new CombatSettings(), container));

        SettingsCategory scenarios = new SettingsCategory(L10n.get("nsettings.cat.autorunner"), new Panel(L10n.get("nsettings.cat.autorunner")), container);
        scenarios.addChild(new SettingsItem(L10n.get("nsettings.item.scenarios"), new ScenarioPanel(), container));
        scenarios.addChild(new SettingsItem(L10n.get("nsettings.item.craft_presets"), new CraftPresetsPanel(), container));

        SettingsCategory bots = new SettingsCategory(L10n.get("nsettings.cat.bots"), new Panel(L10n.get("nsettings.cat.bots")), container);
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.feed_clover"), new FeedClover(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.auto_drop"), new Dropper(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.eating_bot"), new Eater(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.farming"), new FarmingSettingsPanel(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.cheese_orders"), new CheeseOrdersPanel(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.pickling"), new PicklingSettings(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.parasite"), new ParasiteSettings(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.equipment"), new EquipmentBotSettings(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.starvation"), new StarvationAlertSettings(), container));
        bots.addChild(new SettingsItem(L10n.get("nsettings.item.autologout"), new AutoLogoutSettings(), container));
        bots.addChild(new SettingsItem("Icon Generator", new IconGeneratorPanel(), container));

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

    /* ---------------------------------------------------------------- search */

    /** Put the keyboard in the search field. Called when this page is opened. */
    public void focusSearch() {
        if((searchField != null) && (parent != null))
            setfocus(searchField);
    }

    /**
     * Drop the cached search index.
     *
     * <p>The index is harvested from the panels' widgets, so anything that rebuilds those widgets
     * — a language change, say — leaves it describing strings that are no longer on screen.
     */
    public void reindex() {
        index = null;
        highlight = null;
    }

    private List<SettingEntry> index() {
        if(index == null)
            index = buildIndex();
        return(index);
    }

    /*
     * Built on the first keystroke rather than in the constructor, by which point every panel has
     * finished constructing itself and the tree is complete.
     */
    private List<SettingEntry> buildIndex() {
        List<SettingEntry> out = new ArrayList<>();
        for(SettingsItem cat : list.categories) {
            out.add(new SettingEntry(SettingEntry.Kind.PANEL, cat.getName(), null, null, null,
                                     null, null, null, null, () -> openItem(cat)));
            for(SettingsItem item : cat.getChildren()) {
                Runnable open = () -> openItem(item);
                out.add(new SettingEntry(SettingEntry.Kind.PANEL, item.getName(), cat.getName(),
                                         null, null, null, null, null, null, open));
                if(item.panel instanceof Panel)
                    out.addAll(SettingsIndexer.index((Panel)item.panel, cat.getName(), item.getName(), open));
            }
        }
        return(out);
    }

    private void search(String q) {
        q = (q == null) ? "" : q.trim();
        if(q.equals(query))
            return;
        query = q;
        if(q.isEmpty()) {
            hits = Collections.emptyList();
            results.change(null);
            results.hide();
            list.show();
            return;
        }
        hits = SettingsIndexer.search(index(), q, MAX_RESULTS);
        results.change(hits.isEmpty() ? null : hits.get(0));
        results.reset();
        results.scrollval(0);
        list.hide();
        results.show();
    }

    private void moveSel(int d) {
        if(hits.isEmpty())
            return;
        int p = hits.indexOf(results.sel);
        if(p < 0)
            p = (d > 0) ? 0 : (hits.size() - 1);
        else
            p = Math.max(0, Math.min(hits.size() - 1, p + d));
        results.change(hits.get(p));
        results.display(p);
    }

    private void jumpToSelection() {
        SettingEntry e = results.sel;
        if((e == null) && !hits.isEmpty())
            e = hits.get(0);
        if(e != null) {
            results.change(e);
            jump(e);
        }
    }

    private void jump(SettingEntry e) {
        if(e.open != null)
            e.open.run();
        if(e.reveal != null)
            e.reveal.run();
        highlight = null;
        if(e.target == null)
            return;
        scrollto(e);
        highlight = e;
        highlightEnd = Utils.rtime() + HIGHLIGHT_TIME;
    }

    private void scrollto(SettingEntry e) {
        Scrollport sp = e.scroll;
        if((sp == null) || !e.target.hasparent(sp.cont))
            return;
        /* The bar's range is only recomputed when the content changes, and a panel that grew
         * since its last update would otherwise clamp every scroll to zero. */
        sp.cont.update();
        /* parentpos already accounts for the current scroll, so this is where the setting sits
         * in the viewport right now. Land it a third of the way down rather than flush with the
         * top edge, so the section heading above it stays visible. */
        Coord tp = e.target.parentpos(sp.cont);
        sp.bar.ch(tp.y - (sp.cont.sz.y / 3));
    }

    private void openItem(SettingsItem item) {
        /* Typed as the base class because `expanded` is private to it, and private members
         * are not inherited by SettingsCategory. */
        for(SettingsItem cat : list.categories) {
            if((cat == item) || cat.getChildren().contains(item))
                cat.expanded = true;
        }
        list.change(item);
        list.display(item);
        /* Re-showing the panel you are already on would call load() and quietly discard edits
         * that have not been saved yet, so jumping within a panel leaves it alone. */
        if(item.panel != currentPanel)
            showSettings(item);
    }

    @Override
    public void draw(GOut g) {
        super.draw(g);
        drawinfo(g);
        drawhighlight(g);
    }

    private void drawinfo(GOut g) {
        String s;
        if(query.isEmpty())
            s = L10n.get("nsettings.search.hint");
        else if(hits.isEmpty())
            s = L10n.get("nsettings.search.none");
        else
            s = L10n.get("nsettings.search.count", hits.size(), index().size());
        if(!s.equals(infotext)) {
            if(infotex != null)
                infotex.dispose();
            infotex = fit(crumbfnd, s, crumbcol, UI.scale(SIDE_W));
            infotext = s;
        }
        g.image(infotex.tex(), new Coord(UI.scale(SIDE_X + 3), infoY));
    }

    /*
     * Drawn after the panels rather than as a widget inside one, so that finding a setting cannot
     * disturb the layout or the event routing of the panel it lives in.
     */
    private void drawhighlight(GOut g) {
        SettingEntry e = highlight;
        if(e == null)
            return;
        if((e.target == null) || (Utils.rtime() > highlightEnd)
           || (currentPanel == null) || !e.target.hasparent(currentPanel)) {
            highlight = null;
            return;
        }
        Coord ul = e.target.parentpos(this).sub(UI.scale(2), UI.scale(2));
        Coord br = ul.add(e.target.sz).add(UI.scale(4), UI.scale(4));
        if(e.scroll != null) {
            /* Clip to the viewport, so a target the user has scrolled away from does not paint
             * a stray frame over the rest of the panel. */
            Coord cul = e.scroll.cont.parentpos(this);
            Coord cbr = cul.add(e.scroll.cont.sz);
            if((br.y < cul.y) || (ul.y > cbr.y))
                return;
            ul = ul.max(cul);
            br = br.min(cbr);
        }
        double pulse = Math.abs(Math.sin(Utils.rtime() * 4.0));
        g.chcolor(255, 207, 92, (int)(110 + (140 * pulse)));
        g.rect2(ul, br);
        g.rect2(ul.add(1, 1), br.sub(1, 1));
        g.chcolor();
    }

    private static Text fit(Text.Foundry fnd, String text, Color col, int w) {
        if(fnd.strsize(text).x <= w)
            return(fnd.render(text, col));
        String s = text;
        while((s.length() > 1) && (fnd.strsize(s + "...").x > w))
            s = s.substring(0, s.length() - 1);
        return(fnd.render(s + "...", col));
    }

    @Override
    public void dispose() {
        super.dispose();
        if(infotex != null) {
            infotex.dispose();
            infotex = null;
        }
    }

    private static Color kindcol(SettingEntry.Kind kind) {
        switch(kind) {
        case SECTION: return(sectioncol);
        case PANEL: return(panelcol);
        default: return(Color.WHITE);
        }
    }

    private static String crumbtext(SettingEntry e) {
        if(e.kind == SettingEntry.Kind.PANEL)
            return((e.category == null) ? L10n.get("nsettings.search.category") : e.category);
        String c = e.crumb();
        return((e.section == null) ? c : (c + " \u203A " + e.section));
    }

    private class SearchField extends TextEntry {
        SearchField(int w) {
            super(w, "");
        }

        @Override
        protected void changed() {
            super.changed();
            /* Fires once from the superclass constructor, before the result list exists. */
            if(results != null)
                search(text());
        }

        @Override
        public boolean keydown(KeyDownEvent ev) {
            /* Only claim the navigation keys while there is a result list to navigate; with
             * the tree showing they should behave as they always did. */
            if(!hits.isEmpty()) {
                if(ev.code == java.awt.event.KeyEvent.VK_DOWN) {
                    moveSel(1);
                    return(true);
                }
                if(ev.code == java.awt.event.KeyEvent.VK_UP) {
                    moveSel(-1);
                    return(true);
                }
                if(ev.code == java.awt.event.KeyEvent.VK_ENTER) {
                    jumpToSelection();
                    return(true);
                }
            }
            if(ev.c == 27) {
                /* First escape clears the search; a second one falls through to whatever
                 * normally closes the window. */
                if(!text().isEmpty()) {
                    settext("");
                    search("");
                    return(true);
                }
                return(false);
            }
            return(super.keydown(ev));
        }
    }

    private class ResultList extends SListBox<SettingEntry, ResultItem> {
        ResultList(Coord sz) {
            super(sz, RESULT_H);
        }

        @Override
        protected List<? extends SettingEntry> items() {
            return(hits);
        }

        @Override
        protected ResultItem makeitem(SettingEntry item, int idx, Coord sz) {
            return(new ResultItem(this, sz, item));
        }
    }

    private class ResultItem extends SListWidget.ItemWidget<SettingEntry> {
        private final Text name, crumb;

        ResultItem(SListWidget<SettingEntry, ?> list, Coord sz, SettingEntry item) {
            super(list, sz, item);
            int w = sz.x - UI.scale(6);
            this.name = fit(Text.std, item.label, kindcol(item.kind), w);
            this.crumb = fit(crumbfnd, crumbtext(item), crumbcol, w);
        }

        @Override
        public void draw(GOut g) {
            g.image(name.tex(), new Coord(UI.scale(3), UI.scale(2)));
            g.image(crumb.tex(), new Coord(UI.scale(3), UI.scale(2) + name.sz().y - UI.scale(1)));
        }

        @Override
        protected boolean clicked(MouseDownEvent ev) {
            super.clicked(ev);
            jump(item);
            return(true);
        }

        @Override
        public void dispose() {
            super.dispose();
            name.dispose();
            crumb.dispose();
        }
    }
}