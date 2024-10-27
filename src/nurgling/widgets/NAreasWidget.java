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

public class NAreasWidget extends Window
{
    public IngredientContainer in_items;
    public IngredientContainer out_items;
    CurrentSpecialisationList csl;
    public AreaList al;
    public boolean createMode = false;
    public String currentPath = "";
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
        super(UI.scale(new Coord(700,500)), "Areas Settings");

        IButton createNewFolder;
        prev = add(createNewFolder = new IButton(NStyle.addfolder[0].back,NStyle.addfolder[1].back,NStyle.addfolder[2].back){
            @Override
            public void click()
            {
                super.click();
                NEditFolderName.createFolder(currentPath);
            }
        },new Coord(0,UI.scale(5)));
        createNewFolder.settip("Create new folder");

        IButton create;
        add(create = new IButton(NStyle.addarea[0].back,NStyle.addarea[1].back,NStyle.addarea[2].back){
            @Override
            public void click()
            {
                super.click();
                NUtils.getGameUI().msg("Please, select area");
                new Thread(new NAreaSelector(NAreaSelector.Mode.CREATE)).start();
            }
        },prev.pos("ur").adds(UI.scale(5,0)));
        create.settip("Create new area");

        IButton showCat;
        add(showCat = new IButton(NStyle.catmenu[0].back,NStyle.catmenu[1].back,NStyle.catmenu[2].back){
            @Override
            public void click()
            {
                super.click();
                if(al.sel!=null) {
                    if(catSelection == null) {
                        ui.gui.add(catSelection = new NCatSelection(), NAreasWidget.this.c.add(0, NAreasWidget.this.sz.y));
                    }
                    catSelection.visible = true;
                }
            }
        },create.pos("ur").adds(UI.scale(5,0)));
        showCat.settip("Show all categories");

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
                        NUtils.getUI().core.config.mergeAreas(fc.getSelectedFile());
                    }
                    NAreasWidget.this.hide();
                    NAreasWidget.this.show();
                    NConfig.needAreasUpdate();
                });
            }
        },showCat.pos("ur").adds(UI.scale(25,0)));
        importbt.settip("Import");

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
        exportbt.settip("Export");

        prev = add(al = new AreaList(UI.scale(new Coord(400,170))), prev.pos("bl").adds(0, 10));
        Widget lab = add(new Label("Specialisation",NStyle.areastitle), prev.pos("bl").add(UI.scale(0,5)));

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
        add(new Label("Take:",NStyle.areastitle),prev.pos("ul").sub(UI.scale(-5,20)));
        prev = add(Frame.with(out_items = new IngredientContainer("out"),true), prev.pos("ur").adds(UI.scale(5, 0)));
        add(new Label("Put:",NStyle.areastitle),prev.pos("ul").sub(UI.scale(-5,20)));
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
        synchronized (items) {
            items.clear();
            currentPath = path;
            HashMap<String, Folder> folders = new HashMap<>();
            ArrayList<AreaItem> areas = new ArrayList<>();
            for (NArea area : ((NMapView) NUtils.getGameUI().map).glob.map.areas.values()) {
                if (area.path.equals(path)) {
                    areas.add(new AreaItem(area.name, area));
                }
                else if(area.path.startsWith(path))
                {
                    String cand = area.path.substring(path.length());
                    String fname = cand.split("/")[1];
                    folders.put(fname, new Folder(fname,path));
                }
            }


            if(!currentPath.isEmpty())
            {
                if(currentPath.contains("/"))
                {
                    String subPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
                    items.add(new AreaItem(subPath));
                }
                else
                {
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

    }

    public class AreaItem extends Widget{
        Label text;
        IButton remove;

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
            remove = add(new IButton(NStyle.removei[0].back,NStyle.removei[1].back,NStyle.removei[2].back){
                @Override
                public void click() {
                    ((NMapView)NUtils.getGameUI().map).removeArea(AreaItem.this.text.text());
                    NConfig.needAreasUpdate();
                }
            },new Coord(al.sz.x - NStyle.removei[0].sz().x, 0).sub(UI.scale(5),UI.scale(1) ));
            remove.settip(Resource.remote().loadwait("nurgling/hud/buttons/removeItem/u").flayer(Resource.tooltip).t);
            opt = new ArrayList<String>(){
                {
                    add("Select area space");
                    add("Set color");
                    add("Edit name");
                    add("Scan");
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
                    add("Edit folder name");
                    add("Remove with content");
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
                g.text(text.text(), new Coord(UI.scale(21), 0)); // Текст рядом с иконкой
            }else if (area == null) {
                g.image(folderIcon, Coord.z, UI.scale(16,16));
                g.text(text.text(), new Coord(UI.scale(21), 0)); // Текст рядом с иконкой
            } else {
                super.draw(g);
            }
        }

        @Override
        public boolean mousedown(Coord c, int button)
        {
            if (button == 3)
            {
                opts(c);
                return true;
            }
            else if (button == 1) {
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
            return super.mousedown(c, button);

        }



        NFlowerMenu menu;

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
                            else if (option.name.equals("Edit folder name"))
                            {
                                NEditFolderName.changeName(currentPath, AreaItem.this.text.text());
                            }
                            else if (option.name.equals("Remove with content"))
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

    private void select(int id)
    {
        in_items.load(id);
        out_items.load(id);
        loadSpec(id);
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

        protected List<AreaItem> items() {
            synchronized (items) {
                return items;
            }
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(UI.scale(170)-UI.scale(6), sz.y));
        }

        protected Widget makeitem(AreaItem item, int idx, Coord sz) {
            return(new ItemWidget<AreaItem>(this, sz, item) {
                {
                    //item.resize(new Coord(searchF.sz.x - removei[0].sz().x  + UI.scale(4), item.sz.y));
                    add(item);
                }

                public boolean mousedown(Coord c, int button) {
                    boolean psel = sel == item;
                    super.mousedown(c, button);
                    if(!psel) {
                        String value = item.text.text();
                    }
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
        TexI icon;
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
                },UI.scale(new Coord(135,4)));
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
}
