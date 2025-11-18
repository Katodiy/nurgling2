package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Scrollbar;
import haven.Window;
import nurgling.*;
import org.json.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class BlueprintWidget extends Window
{
    private int gridWidth = 20;
    private int gridHeight = 20;
    private GridPanel gridPanel;
    private TreeListPanel treeListPanel;
    private TreeItem selectedTree = null;
    private TreeItem draggingTree = null;
    private GridScrollArea gridScrollArea;
    private static String currentBlueprintName = "Blueprint 1";
    private static Map<String, BlueprintData> blueprints = new HashMap<>();
    private static boolean blueprintsLoaded = false;
    private Dropbox<String> blueprintSelector;
    private TextEntry widthEntry;
    private TextEntry heightEntry;
    private static final Map<String, BufferedImage> iconCache = new HashMap<>();
    
    private static class BlueprintData {
        int width;
        int height;
        Map<Coord, String> trees = new HashMap<>();
        
        BlueprintData(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    
    private static class TreeDef {
        String resName;
        String displayName;
        
        TreeDef(String resName, String displayName) {
            this.resName = resName;
            this.displayName = displayName;
        }
    }
    
    private static final List<TreeDef> AVAILABLE_TREES = Arrays.asList(
        new TreeDef("gfx/terobjs/mm/trees/acacia", "Acacia"),
        new TreeDef("gfx/terobjs/mm/trees/alder", "Alder"),
        new TreeDef("gfx/terobjs/mm/trees/almondtree", "Almond Tree"),
        new TreeDef("gfx/terobjs/mm/trees/appletree", "Apple Tree"),
        new TreeDef("gfx/terobjs/mm/trees/appletreegreen", "Green Apple Tree"),
        new TreeDef("gfx/terobjs/mm/trees/ash", "Ash"),
        new TreeDef("gfx/terobjs/mm/trees/aspen", "Aspen"),
        new TreeDef("gfx/terobjs/mm/trees/baywillow", "Bay Willow"),
        new TreeDef("gfx/terobjs/mm/trees/beech", "Beech"),
        new TreeDef("gfx/terobjs/mm/trees/birch", "Birch"),
        new TreeDef("gfx/terobjs/mm/trees/birdcherrytree", "Bird Cherry Tree"),
        new TreeDef("gfx/terobjs/mm/trees/blackpine", "Black Pine"),
        new TreeDef("gfx/terobjs/mm/trees/blackpoplar", "Black Poplar"),
        new TreeDef("gfx/terobjs/mm/trees/buckthorn", "Buckthorn"),
        new TreeDef("gfx/terobjs/mm/trees/carobtree", "Carob Tree"),
        new TreeDef("gfx/terobjs/mm/trees/cedar", "Cedar"),
        new TreeDef("gfx/terobjs/mm/trees/charredtree", "Charred Tree"),
        new TreeDef("gfx/terobjs/mm/trees/chastetree", "Chaste Tree"),
        new TreeDef("gfx/terobjs/mm/trees/checkertree", "Checker Tree"),
        new TreeDef("gfx/terobjs/mm/trees/cherry", "Cherry Tree"),
        new TreeDef("gfx/terobjs/mm/trees/chestnuttree", "Chestnut Tree"),
        new TreeDef("gfx/terobjs/mm/trees/conkertree", "Conker Tree"),
        new TreeDef("gfx/terobjs/mm/trees/corkoak", "Corkoak"),
        new TreeDef("gfx/terobjs/mm/trees/crabappletree", "Crabapple Tree"),
        new TreeDef("gfx/terobjs/mm/trees/cypress", "Cypress"),
        new TreeDef("gfx/terobjs/mm/trees/dogwood", "Dogwood"),
        new TreeDef("gfx/terobjs/mm/trees/dwarfpine", "Dwarf Pine"),
        new TreeDef("gfx/terobjs/mm/trees/elm", "Elm"),
        new TreeDef("gfx/terobjs/mm/trees/figtree", "Fig Tree"),
        new TreeDef("gfx/terobjs/mm/trees/fir", "Fir"),
        new TreeDef("gfx/terobjs/mm/trees/gloomcap", "Gloomcap"),
        new TreeDef("gfx/terobjs/mm/trees/gnomeshat", "Gnome's Hat"),
        new TreeDef("gfx/terobjs/mm/trees/goldenchain", "Goldenchain"),
        new TreeDef("gfx/terobjs/mm/trees/grayalder", "Gray Alder"),
        new TreeDef("gfx/terobjs/mm/trees/hazel", "Hazel"),
        new TreeDef("gfx/terobjs/mm/trees/hornbeam", "Hornbeam"),
        new TreeDef("gfx/terobjs/mm/trees/juniper", "Juniper"),
        new TreeDef("gfx/terobjs/mm/trees/kingsoak", "King's Oak"),
        new TreeDef("gfx/terobjs/mm/trees/larch", "Larch"),
        new TreeDef("gfx/terobjs/mm/trees/laurel", "Laurel"),
        new TreeDef("gfx/terobjs/mm/trees/lemontree", "Lemon Tree"),
        new TreeDef("gfx/terobjs/mm/trees/linden", "Linden"),
        new TreeDef("gfx/terobjs/mm/trees/lotetree", "Lote Tree"),
        new TreeDef("gfx/terobjs/mm/trees/maple", "Maple"),
        new TreeDef("gfx/terobjs/mm/trees/mayflower", "Mayflower"),
        new TreeDef("gfx/terobjs/mm/trees/medlartree", "Medlar Tree"),
        new TreeDef("gfx/terobjs/mm/trees/moundtree", "Mound Tree"),
        new TreeDef("gfx/terobjs/mm/trees/mulberry", "Mulberry Tree"),
        new TreeDef("gfx/terobjs/mm/trees/oak", "Oak"),
        new TreeDef("gfx/terobjs/mm/trees/olivetree", "Olive Tree"),
        new TreeDef("gfx/terobjs/mm/trees/orangetree", "Orange Tree"),
        new TreeDef("gfx/terobjs/mm/trees/osier", "Osier"),
        new TreeDef("gfx/terobjs/mm/trees/peartree", "Pear Tree"),
        new TreeDef("gfx/terobjs/mm/trees/persimmontree", "Persimmon Tree"),
        new TreeDef("gfx/terobjs/mm/trees/pine", "Pine"),
        new TreeDef("gfx/terobjs/mm/trees/planetree", "Plane Tree"),
        new TreeDef("gfx/terobjs/mm/trees/plumtree", "Plum Tree"),
        new TreeDef("gfx/terobjs/mm/trees/poplar", "Poplar"),
        new TreeDef("gfx/terobjs/mm/trees/quincetree", "Quince Tree"),
        new TreeDef("gfx/terobjs/mm/trees/rowan", "Rowan"),
        new TreeDef("gfx/terobjs/mm/trees/sallow", "Sallow"),
        new TreeDef("gfx/terobjs/mm/trees/silverfir", "Silverfir"),
        new TreeDef("gfx/terobjs/mm/trees/sorbtree", "Sorb Tree"),
        new TreeDef("gfx/terobjs/mm/trees/spruce", "Spruce"),
        new TreeDef("gfx/terobjs/mm/trees/stonepine", "Stone Pine"),
        new TreeDef("gfx/terobjs/mm/trees/strawberrytree", "Strawberry Tree"),
        new TreeDef("gfx/terobjs/mm/trees/sweetgum", "Sweetgum"),
        new TreeDef("gfx/terobjs/mm/trees/sycamore", "Sycamore"),
        new TreeDef("gfx/terobjs/mm/trees/tamarisk", "Tamarisk"),
        new TreeDef("gfx/terobjs/mm/trees/terebinth", "Terebinth"),
        new TreeDef("gfx/terobjs/mm/trees/towercap", "Towercap"),
        new TreeDef("gfx/terobjs/mm/trees/treeheath", "Tree Heath"),
        new TreeDef("gfx/terobjs/mm/trees/trombonechantrelle", "Trombone Chantrelle"),
        new TreeDef("gfx/terobjs/mm/trees/walnuttree", "Walnut Tree"),
        new TreeDef("gfx/terobjs/mm/trees/wartybirch", "Warty Birch"),
        new TreeDef("gfx/terobjs/mm/trees/whitebeam", "Whitebeam"),
        new TreeDef("gfx/terobjs/mm/trees/willow", "Willow"),
        new TreeDef("gfx/terobjs/mm/trees/wychelm", "Wych Elm"),
        new TreeDef("gfx/terobjs/mm/trees/yew", "Yew"),
        new TreeDef("gfx/terobjs/mm/trees/zelkova", "Zelkova")
    );

    public BlueprintWidget() {
        super(UI.scale(new Coord(800, 600)), "Blueprint Manager");
        
        // Load blueprints from file only once
        if (!blueprintsLoaded) {
            loadBlueprintsFromFile();
            if (blueprints.isEmpty()) {
                blueprints.put(currentBlueprintName, new BlueprintData(gridWidth, gridHeight));
            }
            blueprintsLoaded = true;
        }
        
        Widget prev;
        
        // Blueprint selector
        Widget firstRow = add(new Label("Blueprint:"), new Coord(UI.scale(10), UI.scale(10)));
        int baseY = firstRow.c.y;
        
        prev = add(blueprintSelector = new Dropbox<String>(UI.scale(150), 5, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return new ArrayList<>(blueprints.keySet()).get(i);
            }
            
            @Override
            protected int listitems() {
                return blueprints.size();
            }
            
            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                loadBlueprint(item);
            }
        }, new Coord(firstRow.c.x + firstRow.sz.x + UI.scale(10), baseY));
        blueprintSelector.change(currentBlueprintName);
        
        IButton addBlueprint;
        prev = add(addBlueprint = new IButton(NStyle.add[0].back, NStyle.add[1].back, NStyle.add[2].back) {
            @Override
            public void click() {
                super.click();
                createNewBlueprintDialog();
            }
        }, new Coord(prev.c.x + prev.sz.x + UI.scale(5), baseY));
        addBlueprint.settip("Create new blueprint");
        
        // Grid size inputs
        prev = add(new Label("Grid:"), new Coord(prev.c.x + prev.sz.x + UI.scale(15), baseY));
        
        prev = add(widthEntry = new TextEntry(UI.scale(50), String.valueOf(gridWidth)) {
            public void changed() {
                updateGridSize(this, null);
            }
        }, new Coord(prev.c.x + prev.sz.x + UI.scale(5), baseY));
        
        prev = add(new Label("x"), new Coord(prev.c.x + prev.sz.x + UI.scale(5), baseY));
        
        prev = add(heightEntry = new TextEntry(UI.scale(50), String.valueOf(gridHeight)) {
            public void changed() {
                updateGridSize(null, this);
            }
        }, new Coord(prev.c.x + prev.sz.x + UI.scale(5), baseY));
        
        // Export/Import buttons
        IButton save;
        prev = add(save = new IButton(NStyle.exportb[0].back, NStyle.exportb[1].back, NStyle.exportb[2].back) {
            @Override
            public void click() {
                super.click();
                saveBlueprintDialog();
            }
        }, new Coord(prev.c.x + prev.sz.x + UI.scale(15), baseY));
        save.settip("Export blueprint");
        
        IButton load;
        prev = add(load = new IButton(NStyle.importb[0].back, NStyle.importb[1].back, NStyle.importb[2].back) {
            @Override
            public void click() {
                super.click();
                loadBlueprintDialog();
            }
        }, new Coord(prev.c.x + prev.sz.x + UI.scale(5), baseY));
        load.settip("Import blueprint");
        
        int contentY = UI.scale(45);
        
        prev = add(new Label("Available Trees:", NStyle.areastitle), 
                   new Coord(UI.scale(10), contentY));
        
        treeListPanel = new TreeListPanel(UI.scale(new Coord(180, 500)));
        prev = add(treeListPanel, prev.pos("bl").adds(0, UI.scale(5)));
        
        gridScrollArea = add(new GridScrollArea(UI.scale(new Coord(550, 500))),
                             new Coord(prev.pos("ur").x + UI.scale(10), contentY + UI.scale(25)));
        gridPanel = new GridPanel(gridWidth, gridHeight);
        gridScrollArea.setGrid(gridPanel);
        
        // Load trees from current blueprint after gridPanel is created
        if (blueprints.containsKey(currentBlueprintName)) {
            BlueprintData data = blueprints.get(currentBlueprintName);
            gridPanel.trees.putAll(data.trees);
        }
        
        pack();
    }
    
    private void updateGridSize(TextEntry widthEntry, TextEntry heightEntry) {
        try {
            if (widthEntry != null) {
                int newWidth = Integer.parseInt(widthEntry.text());
                if (newWidth > 0 && newWidth <= 50) {
                    gridWidth = newWidth;
                    getCurrentBlueprint().width = newWidth;
                    recreateGrid();
                    saveBlueprintsToFile();
                }
            }
            if (heightEntry != null) {
                int newHeight = Integer.parseInt(heightEntry.text());
                if (newHeight > 0 && newHeight <= 50) {
                    gridHeight = newHeight;
                    getCurrentBlueprint().height = newHeight;
                    recreateGrid();
                    saveBlueprintsToFile();
                }
            }
        } catch (NumberFormatException e) {
            // Ignore invalid input while typing
        }
    }
    
    private BlueprintData getCurrentBlueprint() {
        return blueprints.get(currentBlueprintName);
    }
    
    private void saveCurrentBlueprint() {
        if (gridPanel == null)
            return;
        BlueprintData data = getCurrentBlueprint();
        data.trees.clear();
        data.trees.putAll(gridPanel.trees);
    }
    
    private void loadBlueprint(String name) {
        if (name == null || !blueprints.containsKey(name))
            return;
        
        // Save current blueprint before switching
        String oldName = currentBlueprintName;
        if (!oldName.equals(name)) {
            saveCurrentBlueprint();
        }
        
        currentBlueprintName = name;
        
        BlueprintData data = getCurrentBlueprint();
        gridWidth = data.width;
        gridHeight = data.height;
        
        // Update text entries
        if (widthEntry != null)
            widthEntry.settext(String.valueOf(gridWidth));
        if (heightEntry != null)
            heightEntry.settext(String.valueOf(gridHeight));
        
        if (gridPanel != null) {
            recreateGrid();
            gridPanel.trees.putAll(data.trees);
        }
    }
    
    private void createNewBlueprintDialog() {
        Window dialog = new Window(UI.scale(new Coord(300, 100)), "New Blueprint") {
            {
                Widget prev;
                prev = add(new Label("Name:"), new Coord(UI.scale(10), UI.scale(10)));
                TextEntry nameEntry;
                prev = add(nameEntry = new TextEntry(UI.scale(200), "Blueprint " + (blueprints.size() + 1)), 
                          prev.pos("ur").adds(UI.scale(10), 0));
                
                add(new haven.Button(UI.scale(80), "OK") {
                    public void click() {
                        String name = nameEntry.text().trim();
                        if (!name.isEmpty() && !blueprints.containsKey(name)) {
                            saveCurrentBlueprint();
                            blueprints.put(name, new BlueprintData(20, 20));
                            currentBlueprintName = name;
                            blueprintSelector.change(name);
                            parent.reqdestroy();
                        } else if (blueprints.containsKey(name)) {
                            NUtils.getGameUI().msg("Blueprint name already exists");
                        }
                    }
                }, new Coord(UI.scale(60), UI.scale(60)));
                
                add(new haven.Button(UI.scale(80), "Cancel") {
                    public void click() {
                        parent.reqdestroy();
                    }
                }, new Coord(UI.scale(160), UI.scale(60)));
                
                pack();
            }
            
            @Override
            public void wdgmsg(String msg, Object... args) {
                if (msg.equals("close")) {
                    reqdestroy();
                } else {
                    super.wdgmsg(msg, args);
                }
            }
        };
        
        ui.root.add(dialog, new Coord(ui.root.sz.x / 2 - dialog.sz.x / 2, ui.root.sz.y / 2 - dialog.sz.y / 2));
    }
    
    private void recreateGrid() {
        if (gridPanel != null) {
            gridPanel.destroy();
        }
        gridPanel = new GridPanel(gridWidth, gridHeight);
        gridScrollArea.setGrid(gridPanel);
    }
    
    private void saveBlueprintDialog() {
        java.awt.EventQueue.invokeLater(() -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Tree Garden Blueprint", "json"));
            if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
                return;
            
            File file = fc.getSelectedFile();
            if (!file.getName().endsWith(".json")) {
                file = new File(file.getAbsolutePath() + ".json");
            }
            
            saveBlueprint(file);
        });
    }
    
    private void saveBlueprint(File file) {
        try {
            JSONObject blueprint = new JSONObject();
            blueprint.put("width", gridWidth);
            blueprint.put("height", gridHeight);
            
            JSONArray trees = new JSONArray();
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    String tree = gridPanel.getTree(x, y);
                    if (tree != null) {
                        JSONObject treeObj = new JSONObject();
                        treeObj.put("x", x);
                        treeObj.put("y", y);
                        treeObj.put("type", tree);
                        trees.put(treeObj);
                    }
                }
            }
            blueprint.put("trees", trees);
            
            Files.write(file.toPath(), blueprint.toString(2).getBytes());
            NUtils.getGameUI().msg("Blueprint saved successfully");
        } catch (Exception e) {
            NUtils.getGameUI().msg("Failed to save blueprint: " + e.getMessage());
        }
    }
    
    private void loadBlueprintDialog() {
        java.awt.EventQueue.invokeLater(() -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Tree Garden Blueprint", "json"));
            if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                return;
            
            loadBlueprint(fc.getSelectedFile());
        });
    }
    
    private void loadBlueprint(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject blueprint = new JSONObject(content);
            
            gridWidth = blueprint.getInt("width");
            gridHeight = blueprint.getInt("height");
            
            recreateGrid();
            
            JSONArray trees = blueprint.getJSONArray("trees");
            for (int i = 0; i < trees.length(); i++) {
                JSONObject treeObj = trees.getJSONObject(i);
                int x = treeObj.getInt("x");
                int y = treeObj.getInt("y");
                String type = treeObj.getString("type");
                gridPanel.setTree(x, y, type);
            }
            
            NUtils.getGameUI().msg("Blueprint loaded successfully");
        } catch (Exception e) {
            NUtils.getGameUI().msg("Failed to load blueprint: " + e.getMessage());
        }
    }
    
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            saveCurrentBlueprint();
            saveBlueprintsToFile();
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
    
    private void saveBlueprintsToFile() {
        try {
            JSONObject root = new JSONObject();
            root.put("current", currentBlueprintName);
            
            JSONArray blueprintArray = new JSONArray();
            for (Map.Entry<String, BlueprintData> entry : blueprints.entrySet()) {
                JSONObject bp = new JSONObject();
                bp.put("name", entry.getKey());
                bp.put("width", entry.getValue().width);
                bp.put("height", entry.getValue().height);
                
                JSONArray treesArray = new JSONArray();
                for (Map.Entry<Coord, String> treeEntry : entry.getValue().trees.entrySet()) {
                    JSONObject tree = new JSONObject();
                    tree.put("x", treeEntry.getKey().x);
                    tree.put("y", treeEntry.getKey().y);
                    tree.put("type", treeEntry.getValue());
                    treesArray.put(tree);
                }
                bp.put("trees", treesArray);
                blueprintArray.put(bp);
            }
            root.put("blueprints", blueprintArray);
            
            File configFile = new File("tree_garden_blueprints.json");
            Files.write(configFile.toPath(), root.toString(2).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadBlueprintsFromFile() {
        try {
            File configFile = new File("tree_garden_blueprints.json");
            if (!configFile.exists())
                return;
            
            String content = new String(Files.readAllBytes(configFile.toPath()));
            JSONObject root = new JSONObject(content);
            
            if (root.has("current")) {
                currentBlueprintName = root.getString("current");
            }
            
            JSONArray blueprintArray = root.getJSONArray("blueprints");
            for (int i = 0; i < blueprintArray.length(); i++) {
                JSONObject bp = blueprintArray.getJSONObject(i);
                String name = bp.getString("name");
                int width = bp.getInt("width");
                int height = bp.getInt("height");
                
                BlueprintData data = new BlueprintData(width, height);
                
                if (bp.has("trees")) {
                    JSONArray treesArray = bp.getJSONArray("trees");
                    for (int j = 0; j < treesArray.length(); j++) {
                        JSONObject tree = treesArray.getJSONObject(j);
                        int x = tree.getInt("x");
                        int y = tree.getInt("y");
                        String type = tree.getString("type");
                        data.trees.put(new Coord(x, y), type);
                    }
                }
                
                blueprints.put(name, data);
            }
            
            // Set grid dimensions from current blueprint
            if (blueprints.containsKey(currentBlueprintName)) {
                BlueprintData current = blueprints.get(currentBlueprintName);
                gridWidth = current.width;
                gridHeight = current.height;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void draw(GOut g, boolean strict) {
        super.draw(g, strict);
        if (draggingTree != null) {
            ui.drawafter(new UI.AfterDraw() {
                public void draw(GOut g) {
                    BufferedImage icon = loadTreeIcon(draggingTree.resName);
                    if (icon != null) {
                        Coord hsz = new Coord(icon.getWidth() / 2, icon.getHeight() / 2);
                        g.image(icon, ui.mc.sub(hsz));
                    } else {
                        g.chcolor(new Color(34, 139, 34, 180));
                        g.frect(ui.mc.sub(UI.scale(16), UI.scale(16)), UI.scale(new Coord(32, 32)));
                        g.chcolor();
                    }
                }
            });
        }
    }
    
    private String getTreeAbbreviation(String treeName) {
        if (treeName.length() <= 3) return treeName;
        String[] parts = treeName.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return treeName.substring(0, Math.min(2, treeName.length())).toUpperCase();
    }
    
    private static BufferedImage loadTreeIcon(String resName) {
        if (iconCache.containsKey(resName)) {
            return iconCache.get(resName);
        }
        
        try {
            Resource res = Resource.remote().loadwait(resName);
            Resource.Image img = res.layer(Resource.imgc);
            if (img != null) {
                BufferedImage icon = PUtils.convolvedown(img.img, new Coord(UI.scale(32), UI.scale(32)), CharWnd.iconfilter);
                iconCache.put(resName, icon);
                return icon;
            }
        } catch (Exception e) {
            // Failed to load, return null
        }
        return null;
    }
    
    private class TreeListPanel extends SListBox<TreeItem, Widget> implements DTarget {
        private final List<TreeItem> items = new ArrayList<>();
        private TreeItem pressed = null;
        private UI.Grab grab = null;
        
        TreeListPanel(Coord sz) {
            super(sz, UI.scale(35));
            for (TreeDef treeDef : AVAILABLE_TREES) {
                items.add(new TreeItem(treeDef.resName, treeDef.displayName));
            }
        }
        
        @Override
        protected List<TreeItem> items() {
            return items;
        }
        
        @Override
        protected Widget makeitem(TreeItem item, int idx, Coord sz) {
            return new ItemWidget<TreeItem>(this, sz, item) {
                {
                    BufferedImage icon = loadTreeIcon(item.resName);
                    if (icon != null) {
                        add(new GobIcon(icon), new Coord(UI.scale(2), (sz.y - UI.scale(32)) / 2));
                    }
                    add(new Label(item.displayName), new Coord(UI.scale(36), (sz.y - UI.scale(15)) / 2));
                }
                
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    boolean result = super.mousedown(ev);
                    if (ev.b == 1) {
                        selectedTree = item;
                        pressed = item;
                        grab = ui.grabmouse(TreeListPanel.this);
                    }
                    return result;
                }
            };
        }
        
        @Override
        public void mousemove(MouseMoveEvent ev) {
            if ((draggingTree == null) && (pressed != null)) {
                draggingTree = pressed;
            }
            super.mousemove(ev);
        }
        
        @Override
        public boolean mouseup(MouseUpEvent ev) {
            if ((ev.b == 1) && (grab != null)) {
                if (draggingTree != null) {
                    DropTarget.dropthing(ui.root, ui.mc, draggingTree);
                    draggingTree = null;
                }
                pressed = null;
                grab.remove();
                grab = null;
            }
            return super.mouseup(ev);
        }
        
        @Override
        public void draw(GOut g) {
            Color bg = new Color(30, 40, 40, 160);
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }
        
        @Override
        public boolean drop(Coord cc, Coord ul) {
            return false;
        }
        
        @Override
        public boolean iteminteract(Coord cc, Coord ul) {
            return false;
        }
    }
    
    private class TreeItem {
        String resName;
        String displayName;
        
        TreeItem(String resName, String displayName) {
            this.resName = resName;
            this.displayName = displayName;
        }
    }
    
    private static class GobIcon extends Widget {
        private BufferedImage img;
        
        GobIcon(BufferedImage img) {
            super(new Coord(img.getWidth(), img.getHeight()));
            this.img = img;
        }
        
        @Override
        public void draw(GOut g) {
            g.image(img, Coord.z);
        }
    }
    
    private class GridScrollArea extends Widget {
        private final GridViewport viewport;
        private final HScrollbar hbar;
        private final Scrollbar vbar;
        private final int sbsz = UI.scale(16);
        
        GridScrollArea(Coord sz) {
            super(sz);
            Coord viewsz = sz.sub(sbsz, sbsz);
            
            // Add viewport that contains the grid
            viewport = add(new GridViewport(viewsz), Coord.z);
            
            // Add scrollbars
            hbar = add(new HScrollbar(viewsz.x, 0, 100) {
                @Override
                public void changed() {
                    viewport.updateOffset(new Coord(val, viewport.off.y));
                }
            }, new Coord(0, viewsz.y));
            
            vbar = add(new Scrollbar(viewsz.y, 0, 100) {
                @Override
                public void changed() {
                    viewport.updateOffset(new Coord(viewport.off.x, val));
                }
            }, new Coord(viewsz.x, 0));
        }
        
        void setGrid(GridPanel grid) {
            viewport.setGrid(grid);
            update();
        }
        
        void update() {
            Coord csz = viewport.contentSize();
            Coord viewsz = viewport.sz;
            hbar.max = Math.max(0, csz.x - viewsz.x);
            vbar.max = Math.max(0, csz.y - viewsz.y);
            hbar.val = Math.min(hbar.val, hbar.max);
            vbar.val = Math.min(vbar.val, vbar.max);
            viewport.updateOffset(new Coord(hbar.val, vbar.val));
        }
        
        @Override
        public boolean mousewheel(MouseWheelEvent ev) {
            vbar.ch(ev.a * UI.scale(15));
            return true;
        }
    }
    
    private static class GridViewport extends Widget {
        private GridPanel grid;
        Coord off = Coord.z;
        
        GridViewport(Coord sz) {
            super(sz);
        }
        
        void setGrid(GridPanel grid) {
            if (this.grid != null) {
                this.grid.destroy();
            }
            this.grid = add(grid, Coord.z);
            updateOffset(off);
        }
        
        void updateOffset(Coord off) {
            this.off = off;
            if (grid != null) {
                grid.c = off.inv();
            }
        }
        
        Coord contentSize() {
            return grid != null ? grid.sz : Coord.z;
        }
        
        @Override
        public void draw(GOut g) {
            // Draw background
            g.chcolor(new Color(0, 0, 0, 100));
            g.frect(Coord.z, sz);
            g.chcolor();
            
            // Draw grid with clipping
            if (grid != null && grid.visible) {
                Coord cc = grid.c;
                // Only draw if any part is visible
                if ((cc.x + grid.sz.x > 0) && (cc.y + grid.sz.y > 0) &&
                    (cc.x < sz.x) && (cc.y < sz.y)) {
                    grid.draw(g.reclip(cc, grid.sz));
                }
            }
        }
    }
    
    private static class HScrollbar extends Widget {
        public int val, min, max;
        private UI.Grab drag = null;
        
        public HScrollbar(int w, int min, int max) {
            super(new Coord(w, Scrollbar.width));
            this.min = min;
            this.max = max;
            this.val = min;
        }
        
        public boolean vis() {
            return max > min;
        }
        
        @Override
        public void draw(GOut g) {
            if (vis()) {
                // Rotate textures 90 degrees for horizontal scrollbar
                Coord chainSz = Scrollbar.schain.sz();
                Coord flapSz = Scrollbar.sflarp.sz();
                int cy = (flapSz.x / 2) - (chainSz.x / 2);
                int ew = sz.x + Scrollbar.chcut, cw = chainSz.y;
                int n = Math.max((ew + cw - 1) / cw, 2);
                for (int i = 0; i < n; i++) {
                    Coord pos = Coord.of(((ew - cw) * i) / (n - 1), cy);
                    g.rotimage(Scrollbar.schain, pos.add(chainSz.swapXY().div(2)), chainSz.div(2), Math.PI / 2);
                }
                double a = (double) val / (double) (max - min);
                int fx = (int) ((sz.x - flapSz.y) * a);
                g.rotimage(Scrollbar.sflarp, new Coord(fx + flapSz.y / 2, flapSz.x / 2), flapSz.div(2), Math.PI / 2);
            }
        }
        
        private void update(Coord c) {
            Coord flapSz = Scrollbar.sflarp.sz();
            double a = (double) (c.x - (flapSz.y / 2)) / (double) (sz.x - flapSz.y);
            if (a < 0) a = 0;
            if (a > 1) a = 1;
            int val = (int) Math.round(a * (max - min)) + min;
            if (val != this.val) {
                this.val = val;
                changed();
            }
        }
        
        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b != 1) return super.mousedown(ev);
            if (!vis()) return false;
            drag = ui.grabmouse(this);
            update(ev.c);
            return true;
        }
        
        @Override
        public void mousemove(MouseMoveEvent ev) {
            super.mousemove(ev);
            if (drag != null) update(ev.c);
        }
        
        @Override
        public boolean mouseup(MouseUpEvent ev) {
            if (ev.b != 1) return super.mouseup(ev);
            if (drag == null) return false;
            drag.remove();
            drag = null;
            return true;
        }
        
        public void changed() {}
        
        public void ch(int a) {
            int val = this.val + a;
            if (val > max) val = max;
            if (val < min) val = min;
            if (this.val != val) {
                this.val = val;
                changed();
            }
        }
    }
    
    
    private class GridPanel extends Widget implements DropTarget {
        private final int cols;
        private final int rows;
        private final int cellSize = UI.scale(40);
        private final Map<Coord, String> trees = new HashMap<>();
        
        GridPanel(int cols, int rows) {
            this.cols = cols;
            this.rows = rows;
            
            int totalWidth = cols * cellSize;
            int totalHeight = rows * cellSize;
            
            this.sz = new Coord(totalWidth, totalHeight);
        }
        
        void clear() {
            trees.clear();
        }
        
        String getTree(int x, int y) {
            return trees.get(new Coord(x, y));
        }
        
        void setTree(int x, int y, String tree) {
            trees.put(new Coord(x, y), tree);
        }
        
        @Override
        public void draw(GOut g) {
            // Calculate viewport boundaries in grid coordinate space
            // g.ul and g.br are absolute screen coordinates
            // g.tx is the translation offset for this widget
            int viewportX1 = Math.max(0, g.ul.x - g.tx.x);
            int viewportY1 = Math.max(0, g.ul.y - g.tx.y);
            int viewportX2 = Math.min(sz.x, g.br.x - g.tx.x);
            int viewportY2 = Math.min(sz.y, g.br.y - g.tx.y);
            
            // Don't draw if nothing is visible
            if (viewportX2 <= viewportX1 || viewportY2 <= viewportY1) {
                return;
            }
            
            // Calculate which grid lines to draw
            int startX = viewportX1 / cellSize;
            int endX = (viewportX2 + cellSize - 1) / cellSize;
            int startY = viewportY1 / cellSize;
            int endY = (viewportY2 + cellSize - 1) / cellSize;
            
            // Clamp to actual grid size
            startX = Math.max(0, startX);
            endX = Math.min(cols, endX);
            startY = Math.max(0, startY);
            endY = Math.min(rows, endY);
            
            g.chcolor(Color.WHITE);
            
            // Draw vertical lines - only if they're within viewport
            for (int x = startX; x <= endX; x++) {
                int px = x * cellSize;
                // Skip if line is outside viewport horizontally
                if (px < viewportX1 || px > viewportX2) continue;
                
                int y1 = Math.max(viewportY1, 0);
                int y2 = Math.min(viewportY2, rows * cellSize);
                if (y2 > y1) {
                    g.line(new Coord(px, y1), new Coord(px, y2), 1);
                }
            }
            
            // Draw horizontal lines - only if they're within viewport
            for (int y = startY; y <= endY; y++) {
                int py = y * cellSize;
                // Skip if line is outside viewport vertically
                if (py < viewportY1 || py > viewportY2) continue;
                
                int x1 = Math.max(viewportX1, 0);
                int x2 = Math.min(viewportX2, cols * cellSize);
                if (x2 > x1) {
                    g.line(new Coord(x1, py), new Coord(x2, py), 1);
                }
            }
            
            for (Map.Entry<Coord, String> entry : trees.entrySet()) {
                Coord cell = entry.getKey();
                String treeResName = entry.getValue();
                
                // Skip if cell is outside visible area
                if (cell.x < startX || cell.x >= endX || cell.y < startY || cell.y >= endY) {
                    continue;
                }
                
                int px = cell.x * cellSize;
                int py = cell.y * cellSize;
                
                BufferedImage icon = loadTreeIcon(treeResName);
                if (icon != null) {
                    int iconW = icon.getWidth();
                    int iconH = icon.getHeight();
                    g.image(icon, new Coord(px + (cellSize - iconW) / 2, py + (cellSize - iconH) / 2));
                } else {
                    g.chcolor(new Color(34, 139, 34, 100));
                    g.frect(new Coord(px + 1, py + 1), new Coord(cellSize - 2, cellSize - 2));
                }
            }
            
            g.chcolor();
        }
        
        private String getTreeAbbreviation(String treeName) {
            if (treeName.length() <= 3) return treeName;
            String[] parts = treeName.split(" ");
            if (parts.length >= 2) {
                return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
            }
            return treeName.substring(0, Math.min(2, treeName.length())).toUpperCase();
        }
        
        @Override
        public boolean mousedown(MouseDownEvent ev) {
            boolean changed = false;
            if (ev.b == 1) {
                int cellX = ev.c.x / cellSize;
                int cellY = ev.c.y / cellSize;
                
                if (cellX >= 0 && cellX < cols && cellY >= 0 && cellY < rows) {
                    Coord cell = new Coord(cellX, cellY);
                    
                    if (selectedTree != null) {
                        if (trees.containsKey(cell)) {
                            trees.remove(cell);
                            changed = true;
                        } else {
                            trees.put(cell, selectedTree.resName);
                            changed = true;
                        }
                    }
                }
            } else if (ev.b == 3) {
                int cellX = ev.c.x / cellSize;
                int cellY = ev.c.y / cellSize;
                
                if (cellX >= 0 && cellX < cols && cellY >= 0 && cellY < rows) {
                    Coord cell = new Coord(cellX, cellY);
                    if (trees.remove(cell) != null) {
                        changed = true;
                    }
                }
            }
            
            if (changed) {
                saveCurrentBlueprint();
                saveBlueprintsToFile();
            }
            
            return super.mousedown(ev);
        }
        
        @Override
        public boolean dropthing(Coord cc, Object thing) {
            if (thing instanceof TreeItem) {
                TreeItem treeItem = (TreeItem) thing;
                int cellX = cc.x / cellSize;
                int cellY = cc.y / cellSize;
                
                if (cellX >= 0 && cellX < cols && cellY >= 0 && cellY < rows) {
                    Coord cell = new Coord(cellX, cellY);
                    if (!trees.containsKey(cell)) {
                        trees.put(cell, treeItem.resName);
                        selectedTree = treeItem;
                        saveCurrentBlueprint();
                        saveBlueprintsToFile();
                    }
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public Object tooltip(Coord c, Widget prev) {
            int cellX = c.x / cellSize;
            int cellY = c.y / cellSize;
            
            if (cellX >= 0 && cellX < cols && cellY >= 0 && cellY < rows) {
                Coord cell = new Coord(cellX, cellY);
                String treeResName = trees.get(cell);
                
                if (treeResName != null) {
                    // Find tree display name from resName
                    for (TreeDef treeDef : AVAILABLE_TREES) {
                        if (treeDef.resName.equals(treeResName)) {
                            return Text.render(treeDef.displayName).tex();
                        }
                    }
                    // Fallback to resource name if not found
                    return Text.render(treeResName).tex();
                }
            }
            
            return super.tooltip(c, prev);
        }
    }
}
