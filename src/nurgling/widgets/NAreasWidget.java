package nurgling.widgets;

import haven.*;
import haven.Frame;
import haven.Label;
import haven.Window;
import haven.render.*;
import nurgling.*;
import nurgling.actions.bots.*;
import nurgling.areas.*;
import nurgling.overlays.map.*;
import nurgling.tools.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import javax.swing.*;
import javax.swing.colorchooser.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class NAreasWidget extends Window
{
    SearchableDropbox<String> groupBy;
    List<String> folderItems = new ArrayList<>();
    TextEntry folderSearch;
    public IngredientContainer in_items;
    public IngredientContainer out_items;
    CurrentSpecialisationList csl;
    public AreaList al;
    public boolean createMode = false;
    public NAreasWidget()
    {
        super(UI.scale(new Coord(700,500)), "Areas Settings");
        IButton create;
        prev = add(create = new IButton(NStyle.addarea[0].back,NStyle.addarea[1].back,NStyle.addarea[2].back){
            @Override
            public void click()
            {
                super.click();
                NUtils.getGameUI().msg("Please, select area");
                String selectedDir = null;
                if (groupBy.sel != null && !groupBy.sel.equals("All Folders") && !groupBy.sel.equals("DefaultFolder")) {
                    selectedDir = groupBy.sel;
                }
                new Thread(new NAreaSelector(NAreaSelector.Mode.CREATE, selectedDir)).start();
            }
        },new Coord(180,UI.scale(5)));

        initAreas();
        updateFolderItems();
//        List<String> folderItems = new ArrayList<>();
//        folderItems.add("All Folders"); // Option to display all areas
//        Set<String> dirs = new HashSet<>();
//        for (AreaItem areaItem : areas.values()) {
//            if (areaItem.area.dir != null && !areaItem.area.dir.isEmpty()) {
//                dirs.add(areaItem.area.dir);
//            } else {
//                dirs.add("DefaultFolder");
//            }
//        }
//        folderItems.addAll(dirs);
        prev = add(groupBy = new SearchableDropbox<String>(UI.scale(160), UI.scale(10), UI.scale(20)) {
            @Override
            protected String listitem(int i) {
                return folderItems.get(i);
            }

            @Override
            protected int listitems() {
                return folderItems.size();
            }

            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                this.sel = item;
                al.updateList();
            }
        }, new Coord(UI.scale(15), UI.scale(5)));
        groupBy.sel = "All Folders"; // Set default selection

        create.settip("Create new area");
        prev = add(al = new AreaList(UI.scale(new Coord(400,270))), prev.pos("bl").adds(0, 10));
        Widget lab = add(new Label("Specialisation",NStyle.areastitle), prev.pos("bl").add(UI.scale(0,5)));

        add(csl = new CurrentSpecialisationList(UI.scale(164,90)),lab.pos("bl").add(UI.scale(0,5)));
        add(new IButton(NStyle.add[0].back,NStyle.add[1].back,NStyle.add[2].back){
            @Override
            public void click()
            {
                super.click();
                if(al.sel!=null)
                    Specialisation.selectSpecialisation(al.sel.area);
            }
        },prev.pos("br").sub(UI.scale(40,-5)));

        add(new IButton(NStyle.remove[0].back,NStyle.remove[1].back,NStyle.remove[2].back){
            @Override
            public void click()
            {
                super.click();
                if(al.sel!=null && csl.sel!=null)
                {
                    for(NArea.Specialisation s: al.sel.area.spec)
                    {
                        if(csl.sel.item!=null && s.name.equals(csl.sel.item.name)) {
                            al.sel.area.spec.remove(s);
                            break;
                        }
                    }
                    for(SpecialisationItem item : specItems)
                    {
                        if(csl.sel.item!=null && item.item.name.equals(csl.sel.item.name))
                        {
                            specItems.remove(item);
                            break;
                        }
                    }
                    NConfig.needAreasUpdate();
                }
            }
        },prev.pos("br").sub(UI.scale(17,-5)));

        prev = add(Frame.with(in_items = new IngredientContainer("in"),true), prev.pos("ur").add(UI.scale(5,15)));
        add(new Label("Take:",NStyle.areastitle),prev.pos("ul").sub(UI.scale(-5,20)));
        prev = add(Frame.with(out_items = new IngredientContainer("out"),true), prev.pos("ur").adds(UI.scale(5, 0)));
        add(new Label("Put:",NStyle.areastitle),prev.pos("ul").sub(UI.scale(-5,20)));
        pack();
    }
    // Method to initialize 'areas'
    private void initAreas() {
        if(areas.isEmpty() && NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            Map<Integer, NArea> gameAreas = NUtils.getGameUI().map.glob.map.areas;
            if (!gameAreas.isEmpty()) {
                for (NArea area : gameAreas.values()) {
                    addArea(area.id, area.name, area);
                }
            }
        }
    }
    // Method to update 'folderItems'
    private void updateFolderItems() {
        folderItems.clear();
        folderItems.add("All Folders"); // Option to display all areas
        Set<String> dirs = new HashSet<>();
        for (AreaItem areaItem : areas.values()) {
            if (areaItem.area.dir != null && !areaItem.area.dir.isEmpty()) {
                dirs.add(areaItem.area.dir);
            } else {
                dirs.add("DefaultFolder");
            }
        }
        folderItems.addAll(dirs);
    }
    @Override
    public void destroy() {
        if (groupBy != null) {
            groupBy.destroyDroplist();
        }
        super.destroy();
    }
    public class GroupBy extends SDropBox<String, Widget> {
        public String sel = "All Folders"; // Устанавливаем начальное значение

        public GroupBy(int w) {
            super(w, UI.scale(160), UI.scale(20));
        }

        protected List<String> items() {
            List<String> items = new ArrayList<>();
            items.add("All Folders"); // Опция для отображения всех зон

            // Получаем уникальные значения dir из списка зон
            Set<String> dirs = new HashSet<>();
            for (AreaItem areaItem : areas.values()) {
                if (areaItem.area.dir != null && !areaItem.area.dir.isEmpty()) {
                    dirs.add(areaItem.area.dir);
                } else {
                    dirs.add("DefaultFolder");
                }
            }

            items.addAll(dirs);
            return items;
        }

        protected Widget makeitem(String item, int idx, Coord sz) {
            return SListWidget.TextItem.of(sz, Text.std, () -> item);
        }

        public void change(String item) {
            super.change(item);
            this.sel = item; // Сохраняем выбранный элемент
            // Обновляем список зон при изменении выбранной папки
            al.updateList();
        }

    }


    public void removeArea(int id)
    {
        areas.remove(id);
        updateFolderItems();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            NOverlay nol = NUtils.getGameUI().map.nols.get(id);
            nol.remove();
            NUtils.getGameUI().map.nols.remove(id);
        }
    }

    @Override
    public void show()
    {
        if(areas.isEmpty() && !NUtils.getGameUI().map.glob.map.areas.isEmpty())
        {
            for (NArea area : NUtils.getGameUI().map.glob.map.areas.values())
                addArea(area.id, area.name, area);
        }
        super.show();
    }

    public class AreaItem extends Widget{
        Label text;
        IButton remove;

        public NArea area;

        @Override
        public void resize(Coord sz) {
            remove.move(new Coord(sz.x - NStyle.removei[0].sz().x - UI.scale(5),  remove.c.y));
            super.resize(sz);
        }

        public AreaItem(String text, NArea area){
            this.text = add(new Label(text));
            this.area = area;
            remove = add(new IButton(NStyle.removei[0].back,NStyle.removei[1].back,NStyle.removei[2].back){
                @Override
                public void click() {
                    ((NMapView)NUtils.getGameUI().map).removeArea(AreaItem.this.text.text());
                    NConfig.needAreasUpdate();
                }
            },new Coord(al.sz.x - NStyle.removei[0].sz().x, 0).sub(UI.scale(5),UI.scale(1) ));
            remove.settip(Resource.remote().loadwait("nurgling/hud/buttons/removeItem/u").flayer(Resource.tooltip).t);

            pack();
        }



        @Override
        public boolean mousedown(Coord c, int button)
        {
            if (button == 3)
            {
                opts(c);
                return true;
            }
            else if (button == 1)
            {
                NAreasWidget.this.select(area.id);
            }
            return super.mousedown(c, button);

        }

        final ArrayList<String> opt = new ArrayList<String>(){
            {
                add("Select area space");
                add("Set color");
                add("Edit name");
                add("Scan");
                add("Change folder");
            }
        };

        NFlowerMenu menu;
        private void changeFolder() {
            // Получаем список существующих папок
            Set<String> dirs = new HashSet<>();
            for (AreaItem areaItem : NAreasWidget.this.areas.values()) {
                if (areaItem.area.dir != null && !areaItem.area.dir.isEmpty()) {
                    dirs.add(areaItem.area.dir);
                } else {
                    dirs.add("DefaultFolder");
                }
            }
            // Убираем текущую папку из списка
            dirs.remove(area.dir != null && !area.dir.isEmpty() ? area.dir : "DefaultFolder");
            List<String> folderList = new ArrayList<>(dirs);
            folderList.add(0, "DefaultFolder");
            folderList.add("New folder...");

            // Показываем окно NChangeAreaFolder
            NChangeAreaFolder changeFolderWindow = new NChangeAreaFolder(NAreasWidget.this, area, this, folderList);
            ui.root.add(changeFolderWindow, NUtils.getGameUI().sz.div(2).sub(changeFolderWindow.sz.div(2)));
        }
        public void opts( Coord c ) {
            if(menu == null) {
                menu = new NFlowerMenu(opt.toArray(new String[0])) {
                    public boolean mousedown(Coord c, int button) {
                        if(super.mousedown(c, button))
                            nchoose(null);
                        return(true);
                    }

                    public void destroy() {
                        menu = null;
                        super.destroy();
                    }

                    @Override
                    public void nchoose(NPetal option)
                    {
                        if(option!=null)
                        {
                            if (option.name.equals("Select area space"))
                            {
                                ((NMapView)NUtils.getGameUI().map).changeArea(AreaItem.this.text.text());
                            }
                            else if (option.name.equals("Set color"))
                            {
                                JColorChooser colorChooser = new JColorChooser();
                                final AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
                                for (final AbstractColorChooserPanel accp : panels) {
                                    if (!accp.getDisplayName().equals("RGB")) {
                                        colorChooser.removeChooserPanel(accp);
                                    }
                                }
                                colorChooser.setPreviewPanel(new JPanel());

                                colorChooser.setColor(area.color);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                        float old = NUtils.getUI().gprefs.bghz.val;
                                        NUtils.getUI().gprefs.bghz.val = NUtils.getUI().gprefs.hz.val;
                                        JDialog chooser = JColorChooser.createDialog(null, "SelectColor", true, colorChooser, new AbstractAction() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                area.color = colorChooser.getColor();
                                                if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
                                                {
                                                    NOverlay nol = NUtils.getGameUI().map.nols.get(area.id);
                                                    nol.remove();
                                                    NUtils.getGameUI().map.nols.remove(area.id);
                                                }
                                            }
                                        }, new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {

                                            }
                                        });
                                        chooser.setVisible(true);
                                        NUtils.getUI().gprefs.bghz.val= old;
                                    }
                                }).start();
                            }
                            else if (option.name.equals("Edit name"))
                            {
                                NEditAreaName.changeName(area, AreaItem.this);
                            }
                            else if (option.name.equals("Scan"))
                            {
                                Scaner.startScan(area);
                            }
                            else if (option.name.equals("Change folder"))
                            {
                                // Новый код для смены папки
                                changeFolder();
                            }
                        }
                        uimsg("cancel");
                    }

                };
            }
            Widget par = parent;
            Coord pos = c;
            while(par!=null && !(par instanceof GameUI))
            {
                pos = pos.add(par.c);
                par = par.parent;
            }
            ui.root.add(menu, pos.add(UI.scale(25,38)));
        }


    }


    private void select(int id)
    {
        in_items.load(id);
        out_items.load(id);
        loadSpec(id);
    }

    public void set(int id)
    {
        al.change(areas.get(id));
        select(id);
    }

    public void loadSpec(int id)
    {
        if(NUtils.getArea(id)!=null) {
            specItems.clear();
            for (NArea.Specialisation spec : NUtils.getArea(id).spec) {
                specItems.add(new SpecialisationItem(spec));
            }
        }
    }
    private ConcurrentHashMap<Integer, AreaItem> areas = new ConcurrentHashMap<>();

    public void addArea(int id, String val, NArea area)
    {
        areas.put(id, new AreaItem(val, area));
        updateFolderItems();
    }

    public class AreaList extends SListBox<AreaItem, Widget> {
        private String currentFolder = null; // Текущая папка, если null — корень
        final Tex folderIcon = new TexI(Resource.loadsimg("nurgling/data/folder/u"));

        AreaList(Coord sz) {
            super(sz, UI.scale(15));
        }

        // Метод для обновления текущего расположения (папки)
        public void setCurrentFolder(String folder) {
            currentFolder = folder;
            updateList(); // Обновляем список при изменении папки
        }

        protected List<AreaItem> items() {
            List<AreaItem> list = new ArrayList<>();

            // Если находимся в папке, добавляем опцию выхода
            if (currentFolder != null) {
                list.add(new AreaItem(".. (Back to root)", null) {
                    @Override
                    public boolean mousedown(Coord c, int button) {
                        // При клике возвращаемся в корень
                        setCurrentFolder(null);
                        return true;
                    }
                });
            }

            // Отображаем папки, если находимся в корне
            if (currentFolder == null) {
                Set<String> dirs = new HashSet<>();
                for (AreaItem areaItem : areas.values()) {
                    if (areaItem.area.dir != null && !areaItem.area.dir.isEmpty()) {
                        dirs.add(areaItem.area.dir); // Добавляем уникальные папки
                    }
                }

                for (String dir : dirs) {
                    list.add(new AreaItem(dir, null) {
                        @Override
                        public boolean mousedown(Coord c, int button) {
                            // Переход внутрь папки
                            setCurrentFolder(dir);
                            return true;
                        }
                    });
                }
            }

            // Отображаем зоны, если мы находимся в корне или внутри папки
            for (AreaItem areaItem : areas.values()) {
                if (currentFolder == null) {
                    // Папка пуста — отображаем зоны, которые не находятся в папке
                    if (areaItem.area.dir == null || areaItem.area.dir.isEmpty()) {
                        list.add(areaItem);
                    }
                } else {
                    // Отображаем зоны только внутри текущей папки
                    if (currentFolder.equals(areaItem.area.dir)) {
                        list.add(areaItem);
                    }
                }
            }

            return list;
        }

        @Override
        protected Widget makeitem(AreaItem item, int idx, Coord sz) {
            return new ItemWidget<AreaItem>(this, sz, item) {
                {
                    add(item);
                }

                @Override
                public void draw(GOut g) {
                    if (item.area == null) {
                        // Если это папка, рисуем иконку папки
                        g.image(folderIcon, Coord.z);
                        g.text(item.text.text(), new Coord(folderIcon.sz().x + 5, 0)); // Текст рядом с иконкой
                    } else {
                        // Если это зона, рисуем только текст
                        super.draw(g);
                    }
                }

                @Override
                public boolean mousedown(Coord c, int button) {
                    if (item.area != null) {
                        // Если это зона, делаем выбор
                        NAreasWidget.this.select(item.area.id);
                    }
                    return super.mousedown(c, button);
                }
            };
        }

        // Добавляем метод для обновления списка зон
        public void updateList() {
            super.reset();
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(UI.scale(170) - UI.scale(6), sz.y));
        }

        @Override
        public void wdgmsg(String msg, Object... args) {
            super.wdgmsg(msg, args);
        }

        Color bg = new Color(30, 40, 40, 160);

        @Override
        public void draw(GOut g) {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }
    }


    List<SpecialisationItem> specItems = new ArrayList<>();
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args)
    {
        if(msg.equals("close"))
            hide();
        else
        {
            super.wdgmsg(sender, msg, args);
        }
    }

    public class CurrentSpecialisationList extends SListBox<SpecialisationItem, Widget> {
        CurrentSpecialisationList(Coord sz) {
            super(sz, UI.scale(15));
        }

        @Override
        public void change(SpecialisationItem item)
        {
            super.change(item);
        }

        protected List<SpecialisationItem> items() {return specItems;}

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(sz.x, sz.y));
        }

        protected Widget makeitem(SpecialisationItem item, int idx, Coord sz) {
            return(new ItemWidget<SpecialisationItem>(this, sz, item) {
                {
                    add(item);
                }

                public boolean mousedown(Coord c, int button) {
                    super.mousedown(c, button);
                    return(true);
                }
            });
        }

        @Override
        public void wdgmsg(String msg, Object... args)
        {
            super.wdgmsg(msg, args);
        }

        Color bg = new Color(30,40,40,160);

        @Override
        public void draw(GOut g)
        {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }


    }



    public class SpecialisationItem extends Widget
    {
        Label text;
        NArea.Specialisation item;
        IButton spec = null;
        NFlowerMenu menu;

        public SpecialisationItem(NArea.Specialisation item)
        {
            this.item = item;
            if(item.subtype == null) {
                this.text = add(new Label(item.name));
            }
            else
            {
                this.text = add(new Label(item.name + "(" + item.subtype + ")"));
            }
            if(SpecialisationData.data.get(item.name)!=null)
            {
                add(spec = new IButton("nurgling/hud/buttons/settingsnf/","u","d","h"){
                    @Override
                    public void click() {
                        super.click();
                        menu = new NFlowerMenu(SpecialisationData.data.get(item.name)) {
                            public boolean mousedown(Coord c, int button) {
                                if(super.mousedown(c, button))
                                    nchoose(null);
                                return(true);
                            }

                            public void destroy() {
                                menu = null;
                                super.destroy();
                            }

                            @Override
                            public void nchoose(NPetal option)
                            {
                                if(option!=null)
                                {
                                    SpecialisationItem.this.text.settext(item.name + "(" + option.name + ")");
                                    item.subtype = option.name;
                                    NConfig.needAreasUpdate();
                                }
                                uimsg("cancel");
                            }

                        };
                        Widget par = parent;
                        Coord pos = c.add(UI.scale(32,43));
                        while(par!=null && !(par instanceof GameUI))
                        {
                            pos = pos.add(par.c);
                            par = par.parent;
                        }
                        ui.root.add(menu, pos);
                    }
                },UI.scale(new Coord(135,0)));
            }
            pack();
        }
    }

    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        if(al.sel == null)
        {
            NAreasWidget.this.in_items.load(-1);
            NAreasWidget.this.out_items.load(-1);
        }
    }

    @Override
    public void hide() {
        super.hide();
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null && !createMode)
            ((NMapView)NUtils.getGameUI().map).destroyDummys();
        if (groupBy != null) {
            groupBy.destroyDroplist();
        }
    }

    @Override
    public boolean show(boolean show) {
        if(show)
        {
            ((NMapView)NUtils.getGameUI().map).initDummys();
        }
        return super.show(show);
    }
}