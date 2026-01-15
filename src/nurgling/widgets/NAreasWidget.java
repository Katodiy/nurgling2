package nurgling.widgets;

import haven.*;
import haven.Frame;
import haven.Label;
import haven.Window;
import haven.render.*;
import nurgling.*;
import nurgling.actions.bots.*;
import nurgling.areas.*;
import nurgling.navigation.ChunkNavManager;
import nurgling.navigation.ChunkPath;
import nurgling.overlays.map.*;
import nurgling.tools.*;
import org.json.*;

import javax.swing.*;
import javax.swing.colorchooser.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import static nurgling.widgets.Specialisation.findSpecialisation;
import static nurgling.i18n.L10n.get;
import nurgling.i18n.L10n;

public class NAreasWidget extends Window
{
    public IngredientContainer in_items;
    public IngredientContainer out_items;
    CurrentSpecialisationList csl;
    public AreaList al;
    public boolean createMode = false;
    public String currentPath = "";
    public String searchQuery = "";
    final static Tex folderIcon = new TexI(Resource.loadsimg("nurgling/hud/folder/d"));
    final static Tex openfolderIcon = new TexI(Resource.loadsimg("nurgling/hud/folder/u"));
    NCatSelection catSelection;
    class Folder
    {
        public String name;
        public String rootPath;

        public Folder(String name, String path) {
            this.name = name;
            this.rootPath = path;
        }
    }

    public NAreasWidget()
    {
        super(UI.scale(new Coord(700,500)), get("area.title"));

        IButton createNewFolder;
        prev = add(createNewFolder = new IButton(NStyle.addfolder[0].back,NStyle.addfolder[1].back,NStyle.addfolder[2].back){
            @Override
            public void click()
            {
                super.click();
                NEditFolderName.createFolder(currentPath);
            }
        },new Coord(0,UI.scale(5)));
        createNewFolder.settip(get("area.btn.create_folder"));

        IButton create;
        add(create = new IButton(NStyle.addarea[0].back,NStyle.addarea[1].back,NStyle.addarea[2].back){
            @Override
            public void click()
            {
                super.click();
                NUtils.getGameUI().msg(get("area.msg.select_area"));
                new Thread(new NAreaSelector(NAreaSelector.Mode.CREATE)).start();
            }
        },prev.pos("ur").adds(UI.scale(5,0)));
        create.settip(get("area.btn.create_area"));

        IButton showCat;
        add(showCat = new IButton(NStyle.catmenu[0].back,NStyle.catmenu[1].back,NStyle.catmenu[2].back){
            @Override
            public void click()
            {
                super.click();
                if(al.sel!=null) {
//                    if(catSelection == null) {
                        ui.gui.add(catSelection = new NCatSelection(), NAreasWidget.this.c.add(0, NAreasWidget.this.sz.y));
//                    }
                    catSelection.visible = true;
                }
            }
        },create.pos("ur").adds(UI.scale(5,0)));
        showCat.settip(get("area.btn.show_categories"));

        IButton importbt;
        add(importbt = new IButton(NStyle.importb[0].back,NStyle.importb[1].back,NStyle.importb[2].back){
            @Override
            public void click()
            {
                super.click();
                java.awt.EventQueue.invokeLater(() -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileNameExtensionFilter("Areas setting file", "json"));
                    if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                        return;
                    if(fc.getSelectedFile()!=null)
                    {
                        // Show import strategy dialog
                        NImportStrategyDialog.showDialog(fc.getSelectedFile());
                    }
                });
            }
        },showCat.pos("ur").adds(UI.scale(25,0)));
        importbt.settip(get("area.btn.import"));

        IButton exportbt;
        add(exportbt = new IButton(NStyle.exportb[0].back,NStyle.exportb[1].back,NStyle.exportb[2].back){
            @Override
            public void click()
            {
                super.click();
                java.awt.EventQueue.invokeLater(() -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileNameExtensionFilter("Areas setting file", "json"));
                    if(fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                        return;
                    NUtils.getUI().core.config.writeAreas(fc.getSelectedFile().getAbsolutePath()+".json");
                });
            }
        },importbt.pos("ur").adds(UI.scale(5,0)));
        exportbt.settip(get("area.btn.export"));

//        // Export to Database button
//        haven.Button exportDbBtn;
//        add(exportDbBtn = new haven.Button(UI.scale(80), "Export to DB") {
//            @Override
//            public void click() {
//                super.click();
//                exportAreasToDatabase();
//            }
//        }, exportbt.pos("ur").adds(UI.scale(10, 0)));
//        exportDbBtn.settip("Export all areas to database for sharing");

        TextEntry searchField;
        prev = add(searchField = new TextEntry(UI.scale(580), "") {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                boolean result = super.keydown(ev);
                searchQuery = text().toLowerCase();
                updateFilteredList();
                return result;
            }
        }, createNewFolder.pos("bl").adds(0, 10));
        searchField.settip(get("area.search.placeholder"));

        prev = add(al = new AreaList(UI.scale(new Coord(400,170))), searchField.pos("bl").adds(0, 25));
        Widget lab = add(new Label(get("area.label.specialisation"),NStyle.areastitle), prev.pos("bl").add(UI.scale(0,5)));

        add(csl = new CurrentSpecialisationList(UI.scale(164,190)),lab.pos("bl").add(UI.scale(0,5)));
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

        prev = add(Frame.with(in_items = new IngredientContainer("in"),true), prev.pos("ur").add(UI.scale(5,-5)));
        add(new Label(get("area.label.take"),NStyle.areastitle),prev.pos("ul").sub(UI.scale(-5,20)));
        add(new IngredientContainer.RuleButton(in_items ),prev.pos("ur").sub(UI.scale(30,20)));
        prev = add(Frame.with(out_items = new IngredientContainer("out"),true), prev.pos("ur").adds(UI.scale(5, 0)));
        add(new Label(get("area.label.put"),NStyle.areastitle),prev.pos("ul").sub(UI.scale(-5,20)));
        add(new IngredientContainer.RuleButton(out_items ),prev.pos("ur").sub(UI.scale(30,20)));
        pack();
    }

    public void removeArea(int id)
    {
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            NOverlay nol = NUtils.getGameUI().map.nols.get(id);
            nol.remove();
            NUtils.getGameUI().map.nols.remove(id);
        }
        showPath(currentPath);
    }

    @Override
    public void show()
    {
        showPath(currentPath);
        super.show();
    }

    public void changePath(String newpath, String path) {
        for(NArea area : ((NMapView)NUtils.getGameUI().map).glob.map.areas.values())
        {
            if(area.path.startsWith(path))
            {
                area.path = area.path.replace(path,newpath);
            }
        }
    }

    public void showPath(String path) {
        showPath(path, -1);
    }
    
    public void showPath(String path, int selectAreaId) {
        synchronized (items) {
            items.clear();
            currentPath = path;
            HashMap<String, Folder> folders = new HashMap<>();
            ArrayList<AreaItem> areas = new ArrayList<>();
            for (NArea area : ((NMapView) NUtils.getGameUI().map).glob.map.areas.values()) {
                if (area.path.equals(path)) {
                    areas.add(new AreaItem(area.name, area));
                } else if (area.path.startsWith(path)) {
                    String cand = area.path.substring(path.length());
                    if (cand.startsWith("/")) {
                        String[] parts = cand.split("/");
                        if (parts.length > 1) {
                            String fname = parts[1];
                            folders.put(fname, new Folder(fname, path));
                        }
                    }
                }
            }


            if (!currentPath.isEmpty()) {
                if (currentPath.contains("/")) {
                    String subPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
                    items.add(new AreaItem(subPath));
                } else {
                    items.add(new AreaItem(""));
                }

            }

            for (Folder folder : folders.values()) {
                if (folder.rootPath.equals(path)) {
                    items.add(new AreaItem(folder.name, true));
                }
            }
            items.addAll(areas);
        }
            if(!items.isEmpty()) {
                // Try to select the specified area, otherwise select last item
                AreaItem selectedItem = null;
                if(selectAreaId >= 0) {
                    for(AreaItem item : items) {
                        if(item.area != null && item.area.id == selectAreaId) {
                            selectedItem = item;
                            break;
                        }
                    }
                }
                if(selectedItem == null) {
                    selectedItem = items.get(items.size() - 1);
                }
                al.sel = selectedItem;
                if (al.sel.area != null) {
                    select(al.sel.area.id);
                }
                else
                {
                    select();
                }
            }

    }
    
    public void selectAreaById(int areaId) {
        for(AreaItem item : items) {
            if(item.area != null && item.area.id == areaId) {
                al.sel = item;
                select(areaId);
                return;
            }
        }
    }

    private void updateFilteredList() {
        if (searchQuery.isEmpty()) {
            showPath(currentPath);
            return;
        }

        synchronized (items) {
            items.clear();
            ArrayList<AreaItem> filteredAreas = new ArrayList<>();
            
            for (NArea area : ((NMapView) NUtils.getGameUI().map).glob.map.areas.values()) {
                if (matchesSearch(area)) {
                    filteredAreas.add(new AreaItem(area.name, area));
                }
            }
            
            items.addAll(filteredAreas);
        }
        
        if (!items.isEmpty()) {
            al.sel = items.get(0);
            if (al.sel.area != null) {
                select(al.sel.area.id);
            }
        } else {
            select();
        }
    }

    private boolean matchesSearch(NArea area) {
        String query = searchQuery.toLowerCase();
        
        if (area.name.toLowerCase().contains(query)) {
            return true;
        }
        
        for (NArea.Specialisation spec : area.spec) {
            if (spec.name.toLowerCase().contains(query)) {
                return true;
            }
            if (spec.subtype != null && spec.subtype.toLowerCase().contains(query)) {
                return true;
            }
            // Search by pretty name
            Specialisation.SpecialisationItem specItem = findSpecialisation(spec.name);
            if (specItem != null && specItem.prettyName.toLowerCase().contains(query)) {
                return true;
            }
        }
        
        for (int i = 0; i < area.jin.length(); i++) {
            String itemName = (String) ((JSONObject) area.jin.get(i)).get("name");
            if (itemName.toLowerCase().contains(query)) {
                return true;
            }
        }
        
        for (int i = 0; i < area.jout.length(); i++) {
            String itemName = (String) ((JSONObject) area.jout.get(i)).get("name");
            if (itemName.toLowerCase().contains(query)) {
                return true;
            }
        }
        
        return false;
    }

    public class AreaItem extends Widget{
        Label text;
        IButton remove;
        CheckBox hide;

        public NArea area;

        boolean isDir = false;
        private String rootPath = null;
        final ArrayList<String> opt;
        @Override
        public void resize(Coord sz) {
            if(remove!=null) {
                remove.move(new Coord(sz.x - NStyle.removei[0].sz().x - UI.scale(5), remove.c.y));
            }
            super.resize(sz);
        }

        public AreaItem(String text, NArea area){
            this.text = add(new Label(text));
            this.area = area;
            this.settip(text);
            hide = add(new CheckBox(""){
                @Override
                public void changed(boolean val) {
                    ((NMapView)NUtils.getGameUI().map).disableArea(AreaItem.this.text.text(), area.path, val);
                    al.sel = AreaItem.this;
                    super.changed(val);
                }
            },new Coord(al.sz.x - 2*NStyle.removei[0].sz().x-UI.scale(2), 0).sub(UI.scale(5),0 ));
            hide.a = area.hide;
            hide.settip(get("area.btn.disable"));
            remove = add(new IButton(NStyle.removei[0].back,NStyle.removei[1].back,NStyle.removei[2].back){
                @Override
                public void click() {
                    ((NMapView)NUtils.getGameUI().map).removeArea(AreaItem.this.text.text());
                    NConfig.needAreasUpdate();
                }
            },new Coord(al.sz.x - NStyle.removei[0].sz().x, 0).sub(UI.scale(5),UI.scale(1) ));
            remove.settip(get("area.btn.remove"));
            opt = new ArrayList<String>(){
                {
                    add(L10n.get("area.menu.navigate"));
                    add(L10n.get("area.menu.select_space"));
                    add(L10n.get("area.menu.set_color"));
                    add(L10n.get("area.menu.edit_name"));
                    add(L10n.get("area.menu.scan"));
                }
            };

            pack();
        }

        public AreaItem(String text, boolean isDir){
            this.text = add(new Label(text));
            this.area = null;
            this.isDir = isDir;
            remove = add(new IButton(NStyle.removei[0].back,NStyle.removei[1].back,NStyle.removei[2].back){
                @Override
                public void click() {
                }
            },new Coord(al.sz.x - NStyle.removei[0].sz().x, 0).sub(UI.scale(5),UI.scale(1) ));
            opt = new ArrayList<String>(){
                {
                    add(L10n.get("area.menu.edit_folder"));
                    add(L10n.get("area.menu.remove_content"));
                }
            };
            pack();
        }

        public AreaItem(String rootPath) {
            this.text = add(new Label(".."));
            this.area = null;
            this.isDir = false;
            this.rootPath = rootPath;
            remove = add(new IButton(NStyle.removei[0].back,NStyle.removei[1].back,NStyle.removei[2].back){
                @Override
                public void click() {
                }
            },new Coord(al.sz.x - NStyle.removei[0].sz().x, 0).sub(UI.scale(5),UI.scale(1) ));
            opt = new ArrayList<>();
            pack();
        }

        @Override
        public void draw(GOut g) {
            if (rootPath!=null) {
                g.image(openfolderIcon, Coord.z, UI.scale(16,16));
                g.text(text.text(), new Coord(UI.scale(21), 0)); // Text next to icon
            }else if (area == null) {
                g.image(folderIcon, Coord.z, UI.scale(16,16));
                g.text(text.text(), new Coord(UI.scale(21), 0)); // Text next to icon
            } else {
                super.draw(g);
            }
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 3)
            {
                opts(c);
                return true;
            }
            else if (ev.b == 1) {
                if (!isDir)
                    if(area != null)
                    {
                        NAreasWidget.this.select(area.id);
                    }
                    else
                    {
                        NAreasWidget.this.showPath(rootPath);
                    }

                else
                    showPath(currentPath + "/" + text.text());
            }
            return super.mousedown(ev);
        }


        NFlowerMenu menu;

        public void opts( Coord c ) {
            if(menu == null) {
                menu = new NFlowerMenu(opt.toArray(new String[0])) {
                    @Override
                    public boolean mousedown(MouseDownEvent ev) {
                        if(super.mousedown(ev))
                            nchoose(null);
                        return true;
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
                            if (option.name.equals(get("area.menu.navigate")))
                            {
                                Thread t = new Thread(() -> {
                                        ChunkNavManager chunkNav = ((NMapView)NUtils.getGameUI().map).getChunkNavManager();
                                        if (chunkNav != null && chunkNav.isInitialized())
                                        {
                                            ChunkPath path = chunkNav.planToArea(area);
                                            if (path != null)
                                            {
                                                try
                                                {
                                                    chunkNav.navigateToArea(area, NUtils.getGameUI());
                                            } catch (InterruptedException ignored)
                                            {
                                            }
                                        }
                                    }
                                }, "AreaNavigator");
                                t.start();
                                NUtils.getGameUI().biw.addObserve(t);
                            }
                            else if (option.name.equals(get("area.menu.select_space")))
                            {
                                ((NMapView)NUtils.getGameUI().map).changeArea(area.id);
                            }
                            else if (option.name.equals(get("area.menu.set_color")))
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
                                        final int areaId = area.id;
                                        JDialog chooser = JColorChooser.createDialog(null, "SelectColor", true, colorChooser, new AbstractAction() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                NArea theArea = NUtils.getArea(areaId);
                                                if(theArea != null) {
                                                    theArea.color = colorChooser.getColor();
                                                    theArea.lastLocalChange = System.currentTimeMillis();
                                                    if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
                                                    {
                                                        NOverlay nol = NUtils.getGameUI().map.nols.get(areaId);
                                                        if(nol != null) {
                                                            nol.remove();
                                                            NUtils.getGameUI().map.nols.remove(areaId);
                                                        }
                                                    }
                                                    NConfig.needAreasUpdate();
                                                    // Retain selection on this area
                                                    NUtils.getGameUI().areas.showPath(NUtils.getGameUI().areas.currentPath, areaId);
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
                            else if (option.name.equals(get("area.menu.edit_name")))
                            {
                                NEditAreaName.changeName(area, AreaItem.this);
                            }
                            else if (option.name.equals(get("area.menu.scan")))
                            {
                                Scaner.startScan(area);
                            }
                            else if (option.name.equals(get("area.menu.edit_folder")))
                            {
                                NEditFolderName.changeName(currentPath, AreaItem.this.text.text());
                            }
                            else if (option.name.equals(get("area.menu.remove_content")))
                            {
                                ArrayList<Integer> forRemove = new ArrayList<>();
                                for (NArea area : ((NMapView) NUtils.getGameUI().map).glob.map.areas.values()) {
                                    if(area.path.startsWith(currentPath + "/" + text.text())) {
                                        forRemove.add(area.id);
                                    }
                                }
                                synchronized (((NMapView) NUtils.getGameUI().map).glob.map.areas)
                                {
                                    for(Integer key:forRemove)
                                    {
                                        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
                                        {
                                            NOverlay nol = NUtils.getGameUI().map.nols.get(key);
                                            nol.remove();
                                            NUtils.getGameUI().map.nols.remove(key);
                                            Gob dummy = ((NMapView) NUtils.getGameUI().map).dummys.get(((NMapView) NUtils.getGameUI().map).glob.map.areas.get(key).gid);
                                            if(dummy!=null) {
                                                NUtils.getGameUI().map.glob.oc.remove(dummy);
                                                ((NMapView) NUtils.getGameUI().map).dummys.remove(dummy.id);
                                            }
                                            ((NMapView) NUtils.getGameUI().map).glob.map.areas.remove(key);
                                            
                                            // Delete from database if enabled
                                            if ((Boolean) nurgling.NConfig.get(nurgling.NConfig.Key.ndbenable) &&
                                                nurgling.NCore.databaseManager != null && 
                                                nurgling.NCore.databaseManager.isReady()) {
                                                String profile = NUtils.getGameUI().getGenus();
                                                if (profile == null || profile.isEmpty()) {
                                                    profile = "global";
                                                }
                                                nurgling.NCore.databaseManager.getAreaService().deleteAreaAsync(key, profile);
                                            }
                                        }
                                    }
                                    NConfig.needAreasUpdate();
                                    NAreasWidget.this.showPath(NAreasWidget.this.currentPath);
                                }
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

        @Override
        public void draw(GOut g, boolean strict) {
            super.draw(g, strict);
        }
    }

    public void select(int id)
    {
        in_items.load(id);
        out_items.load(id);
        loadSpec(id);
    }

    public void select()
    {
        in_items.items.clear();
        out_items.items.clear();
        specItems.clear();
    }

    public void set(int id)
    {
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
//    private ConcurrentHashMap<Integer, AreaItem> areas = new ConcurrentHashMap<>();

    private final ArrayList<AreaItem> items = new ArrayList<>();

    public class AreaList extends SListBox<AreaItem, Widget> {
        AreaList(Coord sz) {
            super(sz, UI.scale(15));
        }

        public List<AreaItem> items() {
            synchronized (items) {
                return items;
            }
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(UI.scale(170)-UI.scale(6), sz.y));
        }

        protected Widget makeitem(AreaItem item, int idx, Coord sz) {
            return(new ItemWidget<AreaItem>(this, sz.add(UI.scale(0,5)), item) {
                {
                    //item.resize(new Coord(searchF.sz.x - removei[0].sz().x  + UI.scale(4), item.sz.y));
                    add(item);
                }

                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    boolean psel = sel == item;
                    super.mousedown(ev);
                    if(!psel) {
                        String value = item.text.text();
                    }
                    return super.mousedown(ev);
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

        @Override
        public void change(AreaItem item) {
            if(item != null && !item.isDir && item.area==null && item.rootPath != null) {
                showPath(item.rootPath);
            }
            else
                super.change(item);
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
            super(sz, UI.scale(24));
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

                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    return super.mousedown(ev);
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
        IButton rankPresetBtn = null;
        NFlowerMenu menu;
        NFlowerMenu rankMenu;
        TexI icon;
        
        // Check if specialisation is for animals
        private boolean isAnimalSpec(String name) {
            return name.equals("cows") || name.equals("goats") || name.equals("sheeps") || 
                   name.equals("pigs") || name.equals("horses") || name.equals("deer");
        }
        
        // Get list of presets for animal type
        private java.util.HashSet<String> getPresetNames(String specName) {
            switch(specName) {
                case "cows": return nurgling.conf.CowsHerd.getKeySet();
                case "goats": return nurgling.conf.GoatsHerd.getKeySet();
                case "sheeps": return nurgling.conf.SheepsHerd.getKeySet();
                case "pigs": return nurgling.conf.PigsHerd.getKeySet();
                case "horses": return nurgling.conf.HorseHerd.getKeySet();
                case "deer": return nurgling.conf.TeimDeerHerd.getKeySet();
                default: return new java.util.HashSet<>();
            }
        }
        
        // Get current preset for area and animal type
        private String getCurrentPreset(NArea area, String specName) {
            if(area == null) return null;
            return NConfig.getAreaRankPreset(area.id, specName);
        }
        
        // Set preset for area and animal type
        private void setPreset(NArea area, String specName, String presetName) {
            if(area == null) return;
            NConfig.setAreaRankPreset(area.id, specName, presetName);
        }
        
        public SpecialisationItem(NArea.Specialisation item)
        {
            this.item = item;
            Specialisation.SpecialisationItem specialisationItem = findSpecialisation(item.name);
            if(item.subtype == null) {
                this.text = add(new Label(specialisationItem == null ? "???" + item.name + "???":specialisationItem.prettyName), new Coord(UI.scale(30,4)));
            }
            else
            {
                this.text = add(new Label((specialisationItem == null ? "???" + item.name + "???":specialisationItem.prettyName) + "(" + item.subtype + ")"), new Coord(UI.scale(30,4)));
            }
            if(specialisationItem != null) {
                icon = new TexI(specialisationItem.image);
            }
            
            int btnX = 135;
            
            if(SpecialisationData.data.get(item.name)!=null)
            {
                add(spec = new IButton("nurgling/hud/buttons/settingsnf/","u","d","h"){
                    @Override
                    public void click() {
                        super.click();
                        menu = new NFlowerMenu(SpecialisationData.data.get(item.name)) {

                            @Override
                            public boolean mousedown(MouseDownEvent ev) {
                                if(super.mousedown(ev))
                                    nchoose(null);
                                return true;
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
                                    Specialisation.SpecialisationItem specItem = findSpecialisation(item.name);
                                    String prettyName = specItem != null ? specItem.prettyName : item.name;
                                    SpecialisationItem.this.text.settext(prettyName + "(" + option.name + ")");
                                    item.subtype = option.name;
                                    
                                    // Auto-rename area if its name matches the specialisation prettyName
                                    if(al.sel != null && al.sel.area != null) {
                                        NArea area = al.sel.area;
                                        if(area.name.equals(prettyName)) {
                                            String newName = prettyName + "(" + option.name + ")";
                                            ((NMapView)NUtils.getGameUI().map).changeAreaName(area.id, newName);
                                            al.sel.text.settext(newName);
                                            al.sel.settip(newName);
                                            // Update area label on map
                                            Gob dummy = ((NMapView) NUtils.getGameUI().map).dummys.get(area.gid);
                                            if(dummy != null) {
                                                Gob.Overlay ol = dummy.findol(nurgling.overlays.NAreaLabel.class);
                                                if(ol != null && ol.spr instanceof nurgling.overlays.NAreaLabel) {
                                                    nurgling.overlays.NAreaLabel tl = (nurgling.overlays.NAreaLabel) ol.spr;
                                                    tl.update();
                                                }
                                            }
                                        }
                                    }
                                    
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
                },UI.scale(new Coord(btnX,4)));
                btnX += 18;
            }
            
            // Add rank preset selection button for animal specialisations
            if(isAnimalSpec(item.name))
            {
                add(rankPresetBtn = new IButton("nurgling/hud/buttons/settingsnf/","u","d","h"){
                    @Override
                    public void click() {
                        super.click();
                        java.util.HashSet<String> presets = getPresetNames(item.name);
                        if(presets.isEmpty()) {
                            NUtils.getGameUI().msg(get("area.msg.no_presets", item.name));
                            return;
                        }
                        
                        // Add reset option
                        String noneOption = get("area.preset.none");
                        String[] options = new String[presets.size() + 1];
                        options[0] = noneOption;
                        int i = 1;
                        for(String preset : presets) {
                            options[i++] = preset;
                        }
                        
                        rankMenu = new NFlowerMenu(options) {
                            @Override
                            public boolean mousedown(MouseDownEvent ev) {
                                if(super.mousedown(ev))
                                    nchoose(null);
                                return true;
                            }

                            public void destroy() {
                                rankMenu = null;
                                super.destroy();
                            }

                            @Override
                            public void nchoose(NPetal option) {
                                if(option != null && al.sel != null && al.sel.area != null) {
                                    String presetName = option.name.equals(noneOption) ? null : option.name;
                                    setPreset(al.sel.area, item.name, presetName);
                                    NConfig.needAreasUpdate();
                                    if(presetName != null) {
                                        NUtils.getGameUI().msg(get("area.msg.preset_set", presetName));
                                    } else {
                                        NUtils.getGameUI().msg(get("area.msg.preset_cleared"));
                                    }
                                }
                                uimsg("cancel");
                            }
                        };
                        Widget par = parent;
                        Coord pos = c.add(UI.scale(32,43));
                        while(par!=null && !(par instanceof GameUI)) {
                            pos = pos.add(par.c);
                            par = par.parent;
                        }
                        ui.root.add(rankMenu, pos);
                    }
                    
                    @Override
                    public Object tooltip(Coord c, Widget prev) {
                        if(al.sel != null && al.sel.area != null) {
                            String current = getCurrentPreset(al.sel.area, item.name);
                            return get("area.tooltip.rank_preset", current != null ? current : get("area.tooltip.default"));
                        }
                        return get("area.tooltip.select_preset");
                    }
                },UI.scale(new Coord(btnX, 4)));
            }
            pack();
            sz.y = UI.scale(24);
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
            g.image(icon,Coord.z,UI.scale(24,24));
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
    }

    @Override
    public boolean show(boolean show) {
        if(show)
        {
            showPath(currentPath);
            ((NMapView)NUtils.getGameUI().map).initDummys();
        }
        return super.show(show);
    }

    /**
     * Export all areas to database for sharing with other clients.
     * Reads from the old areas file and imports into database.
     */
    private void exportAreasToDatabase() {
        if (nurgling.NCore.databaseManager == null) {
            NUtils.getGameUI().msg("Database is not connected");
            return;
        }

        if (!nurgling.NCore.databaseManager.isReady()) {
            NUtils.getGameUI().msg("Database is not ready");
            return;
        }

        // Get current profile/genus
        String profile = "global";
        if (NUtils.getGameUI() != null) {
            String genus = NUtils.getGameUI().getGenus();
            if (genus != null && !genus.isEmpty()) {
                profile = genus;
            }
        }

        final String finalProfile = profile;

        // First try to load areas from the old file
        java.util.Map<Integer, NArea> areasToExport = new java.util.HashMap<>();
        
        // Read from file
        String areasPath = NUtils.getUI().core.config.getAreasPath();
        java.io.File areasFile = new java.io.File(areasPath);
        
        if (areasFile.exists()) {
            try {
                StringBuilder contentBuilder = new StringBuilder();
                java.nio.file.Files.lines(java.nio.file.Paths.get(areasPath), java.nio.charset.StandardCharsets.UTF_8)
                    .forEach(s -> contentBuilder.append(s).append("\n"));
                
                String content = contentBuilder.toString().trim();
                if (!content.isEmpty() && content.startsWith("{")) {
                    org.json.JSONObject main = new org.json.JSONObject(content);
                    org.json.JSONArray array = main.getJSONArray("areas");
                    for (int i = 0; i < array.length(); i++) {
                        NArea area = new NArea(array.getJSONObject(i));
                        areasToExport.put(area.id, area);
                    }
                }
            } catch (Exception e) {
                NUtils.getGameUI().error("Failed to read areas file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // If no areas from file, use current cache
        if (areasToExport.isEmpty()) {
            areasToExport = NUtils.getGameUI().map.glob.map.areas;
        }

        if (areasToExport == null || areasToExport.isEmpty()) {
            NUtils.getGameUI().msg("No areas to export");
            return;
        }

        final java.util.Map<Integer, NArea> finalAreas = areasToExport;
        NUtils.getGameUI().msg("Exporting " + finalAreas.size() + " areas to database...");

        // Export asynchronously
        nurgling.NCore.databaseManager.getAreaService().exportAreasToDatabaseAsync(finalAreas, finalProfile)
            .thenAccept(count -> {
                NUtils.getGameUI().msg("Exported " + count + " areas to database");
            })
            .exceptionally(e -> {
                NUtils.getGameUI().error("Failed to export areas: " + e.getMessage());
                e.printStackTrace();
                return null;
            });
    }
}
