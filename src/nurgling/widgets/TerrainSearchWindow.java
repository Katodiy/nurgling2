package nurgling.widgets;

import haven.*;
import nurgling.NUtils;

import java.awt.Color;
import java.util.*;

public class TerrainSearchWindow extends Window {
    private static final int WINDOW_WIDTH = UI.scale(450);
    private static final int WINDOW_HEIGHT = UI.scale(500);
    
    private TextEntry terrainSearchField;
    private CategoryList categoryList;
    private PresetList presetList;
    private TerrainCategory selectedCategory = null;
    
    public TerrainSearchWindow() {
        super(new Coord(WINDOW_WIDTH, WINDOW_HEIGHT), "Tile Highlight");
        
        int y = 0;
        int margin = UI.scale(5);
        
        // Terrain search label and field
        add(new Label("Terrain search:"), margin, y);
        y += UI.scale(20);
        terrainSearchField = add(new TextEntry(WINDOW_WIDTH - margin * 2, "") {
            @Override
            public void changed() {
                super.changed();
                applyTerrainSearch();
            }
            
            @Override
            public boolean keydown(KeyDownEvent ev) {
                if(ev.code == java.awt.event.KeyEvent.VK_ENTER) {
                    applyTerrainSearch();
                    return true;
                }
                return super.keydown(ev);
            }
        }, margin, y);
        y += terrainSearchField.sz.y + UI.scale(10);
        
        // Categories label
        add(new Label("Categories:"), margin, y);
        y += UI.scale(20);
        
        // Create split view: categories on left, presets on right
        int listWidth = (WINDOW_WIDTH - margin * 3) / 2;
        int itemHeight = UI.scale(20);
        int visibleItems = 15;  // Number of visible items in list
        int listHeight = visibleItems * itemHeight;
        
        categoryList = add(new CategoryList(new Coord(listWidth, listHeight), itemHeight), margin, y);
        presetList = add(new PresetList(new Coord(listWidth, listHeight), itemHeight), margin * 2 + listWidth, y);
        
        y += listHeight + UI.scale(10);
        
        // Select All button for current category
        add(new Button(UI.scale(90), "Select All") {
            @Override
            public void click() {
                selectAllInCategory();
            }
        }, margin, y);
        
        // Clear search button
        add(new Button(UI.scale(80), "Clear") {
            @Override
            public void click() {
                clearSearch();
            }
        }, WINDOW_WIDTH - UI.scale(90), y);
        
        pack();
    }
    
    private void applyTerrainSearch() {
        String pattern = terrainSearchField.text().trim();
        
        // Parse patterns separated by |
        java.util.Set<String> tilesToHighlight = new java.util.HashSet<>();
        if(!pattern.isEmpty()) {
            String[] patterns = pattern.split("\\|");
            for(String p : patterns) {
                String trimmed = p.trim();
                if(!trimmed.isEmpty()) {
                    // Find matching tile resource names
                    tilesToHighlight.addAll(findMatchingTiles(trimmed));
                }
            }
        }
        
        // Update TileHighlight
        TileHighlight.setHighlighted(tilesToHighlight);
        
        // Force map redraw
        if(NUtils.getGameUI() != null) {
            if(NUtils.getGameUI().mmap instanceof NMiniMap) {
                ((NMiniMap)NUtils.getGameUI().mmap).invalidateDisplayCache();
            }
        }
    }
    
    /**
     * Find all tile resource names that match the search pattern
     */
    private java.util.Set<String> findMatchingTiles(String pattern) {
        java.util.Set<String> result = new java.util.HashSet<>();
        String lowerPattern = pattern.toLowerCase();
        
        // Search through all categories
        for(TerrainCategory cat : TerrainCategory.ALL_CATEGORIES) {
            for(TerrainPreset preset : cat.presets) {
                // Check for exact match or contains match
                if(preset.searchPattern.toLowerCase().equals(lowerPattern) || 
                   preset.searchPattern.toLowerCase().contains(lowerPattern)) {
                    // Convert search pattern to full resource name
                    result.addAll(presetToResourceNames(preset.searchPattern));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Convert preset search pattern to full tile resource names
     */
    private java.util.Set<String> presetToResourceNames(String searchPattern) {
        java.util.Set<String> result = new java.util.HashSet<>();
        String lower = searchPattern.toLowerCase();
        
        // Map common search patterns to resource paths
        // Ores - gfx/tiles/rocks/*
        if(lower.equals("cassiterite")) result.add("gfx/tiles/rocks/cassiterite");
        else if(lower.equals("chalcopyrite")) result.add("gfx/tiles/rocks/chalcopyrite");
        else if(lower.equals("malachite")) result.add("gfx/tiles/rocks/malachite");
        else if(lower.equals("heavyearth")) result.add("gfx/tiles/rocks/ilmenite");
        else if(lower.equals("ironochre")) result.add("gfx/tiles/rocks/limonite");
        else if(lower.equals("bloodstone")) result.add("gfx/tiles/rocks/hematite");
        else if(lower.equals("blackore")) result.add("gfx/tiles/rocks/magnetite");
        else if(lower.equals("cinnabar")) result.add("gfx/tiles/rocks/cinnabar");
        else if(lower.equals("galena")) result.add("gfx/tiles/rocks/galena");
        else if(lower.equals("silvershine")) result.add("gfx/tiles/rocks/argentite");
        else if(lower.equals("hornsilver")) result.add("gfx/tiles/rocks/hornsilver");
        else if(lower.equals("wineglance")) result.add("gfx/tiles/rocks/cuprite");
        else if(lower.equals("leadglance")) result.add("gfx/tiles/rocks/leadglance");
        else if(lower.equals("leafore")) result.add("gfx/tiles/rocks/petzite");
        else if(lower.equals("schrifterz")) result.add("gfx/tiles/rocks/sylvanite");
        else if(lower.equals("direvein")) result.add("gfx/tiles/rocks/nagyagite");
        else if(lower.equals("blackcoal")) result.add("gfx/tiles/rocks/blackcoal");
        // Rocks - gfx/tiles/rocks/*
        else if(lower.equals("alabaster")) result.add("gfx/tiles/rocks/alabaster");
        else if(lower.equals("apatite")) result.add("gfx/tiles/rocks/apatite");
        else if(lower.equals("arkose")) result.add("gfx/tiles/rocks/arkose");
        else if(lower.equals("basalt")) result.add("gfx/tiles/rocks/basalt");
        else if(lower.equals("breccia")) result.add("gfx/tiles/rocks/breccia");
        else if(lower.equals("chert")) result.add("gfx/tiles/rocks/chert");
        else if(lower.equals("diabase")) result.add("gfx/tiles/rocks/diabase");
        else if(lower.equals("diorite")) result.add("gfx/tiles/rocks/diorite");
        else if(lower.equals("dolomite")) result.add("gfx/tiles/rocks/dolomite");
        else if(lower.equals("eclogite")) result.add("gfx/tiles/rocks/eclogite");
        else if(lower.equals("feldspar")) result.add("gfx/tiles/rocks/feldspar");
        else if(lower.equals("flint")) result.add("gfx/tiles/rocks/flint");
        else if(lower.equals("fluorospar")) result.add("gfx/tiles/rocks/fluorospar");
        else if(lower.equals("gabbro")) result.add("gfx/tiles/rocks/gabbro");
        else if(lower.equals("gneiss")) result.add("gfx/tiles/rocks/gneiss");
        else if(lower.equals("granite")) result.add("gfx/tiles/rocks/granite");
        else if(lower.equals("graywacke")) result.add("gfx/tiles/rocks/graywacke");
        else if(lower.equals("greenschist")) result.add("gfx/tiles/rocks/greenschist");
        else if(lower.equals("hornblende")) result.add("gfx/tiles/rocks/hornblende");
        else if(lower.equals("jasper")) result.add("gfx/tiles/rocks/jasper");
        else if(lower.equals("korund")) result.add("gfx/tiles/rocks/corund");
        else if(lower.equals("kyanite")) result.add("gfx/tiles/rocks/kyanite");
        else if(lower.equals("limestone")) result.add("gfx/tiles/rocks/limestone");
        else if(lower.equals("marble")) result.add("gfx/tiles/rocks/marble");
        else if(lower.equals("mica")) result.add("gfx/tiles/rocks/mica");
        else if(lower.equals("microlite")) result.add("gfx/tiles/rocks/microlite");
        else if(lower.equals("olivine")) result.add("gfx/tiles/rocks/olivine");
        else if(lower.equals("orthoclase")) result.add("gfx/tiles/rocks/orthoclase");
        else if(lower.equals("pegmatite")) result.add("gfx/tiles/rocks/pegmatite");
        else if(lower.equals("porphyry")) result.add("gfx/tiles/rocks/porphyry");
        else if(lower.equals("pumice")) result.add("gfx/tiles/rocks/pumice");
        else if(lower.equals("quartz")) result.add("gfx/tiles/rocks/quartz");
        else if(lower.equals("quarryartz")) result.add("gfx/tiles/rocks/quartz");
        else if(lower.equals("rhyolite")) result.add("gfx/tiles/rocks/rhyolite");
        else if(lower.equals("rocksalt")) result.add("gfx/tiles/rocks/halite");
        else if(lower.equals("sandstone")) result.add("gfx/tiles/rocks/sandstone");
        else if(lower.equals("schist")) result.add("gfx/tiles/rocks/schist");
        else if(lower.equals("serpentine")) result.add("gfx/tiles/rocks/serpentine");
        else if(lower.equals("slate")) result.add("gfx/tiles/rocks/slate");
        else if(lower.equals("soapstone")) result.add("gfx/tiles/rocks/soapstone");
        else if(lower.equals("sodalite")) result.add("gfx/tiles/rocks/sodalite");
        else if(lower.equals("sunstone")) result.add("gfx/tiles/rocks/sunstone");
        else if(lower.equals("zincspar")) result.add("gfx/tiles/rocks/zincspar");
        // Ground tiles - gfx/tiles/*
        else {
            // Try as generic ground tile
            result.add("gfx/tiles/" + lower);
        }
        
        return result;
    }
    
    private void clearSearch() {
        // Clear all enabled presets
        for(TerrainCategory cat : TerrainCategory.ALL_CATEGORIES) {
            for(TerrainPreset preset : cat.presets) {
                preset.enabled = false;
            }
        }
        // Clear search field
        terrainSearchField.settext("");
        // Clear TileHighlight
        TileHighlight.clear();
        // Force map redraw
        if(NUtils.getGameUI() != null) {
            if(NUtils.getGameUI().mmap instanceof NMiniMap) {
                ((NMiniMap)NUtils.getGameUI().mmap).invalidateDisplayCache();
            }
        }
        // Refresh preset list if visible
        if(selectedCategory != null) {
            presetList.updatePresets(selectedCategory);
        }
    }

    @Override
    public void resize(Coord sz)
    {
        super.resize(sz);
    }

    private void selectAllInCategory() {
        if(selectedCategory != null) {
            for(TerrainPreset preset : selectedCategory.presets) {
                preset.enabled = true;
            }
            presetList.applyPresetSearch();
        }
    }
    
    // Category list widget with cached rendering
    private class CategoryList extends SListBox<TerrainCategory, Widget> {
        public CategoryList(Coord sz, int itemh) {
            super(sz, itemh);
        }
        
        @Override
        protected List<TerrainCategory> items() {
            return TerrainCategory.ALL_CATEGORIES;
        }
        
        @Override
        protected Widget makeitem(TerrainCategory cat, int idx, Coord sz) {
            return new ItemWidget<TerrainCategory>(this, sz, cat) {
                private final Text.Line text = Text.render(cat.name);
                
                @Override
                public void draw(GOut g) {
                    g.image(text.tex(), new Coord(UI.scale(5), (sz.y - text.sz().y) / 2));
                }
                
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if(ev.b == 1) {
                        CategoryList.this.change(cat);
                        return true;
                    }
                    return super.mousedown(ev);
                }
            };
        }
        
        public void change(TerrainCategory item) {
            super.change(item);
            selectedCategory = item;
            presetList.updatePresets(item);
        }
    }
    
    // Preset list widget with cached rendering
    private class PresetList extends SListBox<TerrainPreset, Widget> {
        private List<TerrainPreset> currentPresets = new ArrayList<>();
        
        public PresetList(Coord sz, int itemh) {
            super(sz, itemh);
        }
        
        public void updatePresets(TerrainCategory category) {
            currentPresets = category != null ? category.presets : new ArrayList<>();
            reset();
            sel = null;
        }
        
        @Override
        protected List<TerrainPreset> items() {
            return currentPresets;
        }
        
        @Override
        protected Widget makeitem(TerrainPreset preset, int idx, Coord sz) {
            return new ItemWidget<TerrainPreset>(this, sz, preset) {
                private final Text.Line text = Text.render(preset.displayName);
                private final Tex checkboxBorder;
                private final Coord checkboxSize = new Coord(UI.scale(14), UI.scale(14));
                private final Coord checkboxPos = new Coord(UI.scale(3), UI.scale(3));
                
                // Cache checkbox border texture
                {
                    java.awt.image.BufferedImage borderImg = TexI.mkbuf(checkboxSize);
                    java.awt.Graphics2D graphics = borderImg.createGraphics();
                    graphics.setColor(Color.BLACK);
                    graphics.drawRect(0, 0, checkboxSize.x - 1, checkboxSize.y - 1);
                    graphics.dispose();
                    checkboxBorder = new TexI(borderImg);
                }
                
                @Override
                public void draw(GOut g) {
                    // Draw checkbox fill
                    g.chcolor(preset.enabled ? Color.GREEN : Color.GRAY);
                    g.frect(checkboxPos, checkboxSize);
                    g.chcolor();
                    
                    // Draw checkbox border (cached)
                    g.image(checkboxBorder, checkboxPos);
                    
                    // Draw name (cached texture)
                    g.image(text.tex(), new Coord(UI.scale(22), (sz.y - text.sz().y) / 2));
                }
                
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if(ev.b == 1) {
                        preset.enabled = !preset.enabled;
                        applyPresetSearch();
                        return true;
                    }
                    return super.mousedown(ev);
                }
            };
        }
        
        private void applyPresetSearch() {
            // Collect all enabled patterns (search patterns, not resource names)
            java.util.Set<String> enabledPatterns = new java.util.LinkedHashSet<>();
            for(TerrainCategory cat : TerrainCategory.ALL_CATEGORIES) {
                for(TerrainPreset preset : cat.presets) {
                    if(preset.enabled) {
                        enabledPatterns.add(preset.searchPattern);
                    }
                }
            }
            
            // Update text field with unique patterns only
            terrainSearchField.settext(String.join("|", enabledPatterns));
            
            // Apply the search
            applyTerrainSearch();
        }
    }
    
    // Terrain category definition
    public static class TerrainCategory {
        public final String name;
        public final List<TerrainPreset> presets;
        
        public TerrainCategory(String name, TerrainPreset... presets) {
            this.name = name;
            this.presets = Arrays.asList(presets);
        }
        
        // All available categories
        public static final List<TerrainCategory> ALL_CATEGORIES = createCategories();
        
        private static List<TerrainCategory> createCategories() {
            // Define all individual categories first
            TerrainCategory natural = new TerrainCategory("Natural",
                new TerrainPreset("Grass", "grass"),
                new TerrainPreset("Beach", "beach"),
                new TerrainPreset("Beech Grove", "beechgrove"),
                new TerrainPreset("Black Wood", "blackwood"),
                new TerrainPreset("Blue Sod", "bluesod"),
                new TerrainPreset("Bog", "bog"),
                new TerrainPreset("Bounty Acre", "bountyacre"),
                new TerrainPreset("Cloud Range", "cloudrange"),
                new TerrainPreset("Deep Tangle", "deeptangle"),
                new TerrainPreset("Dry Flat", "dryflat"),
                new TerrainPreset("Dry Weald", "dryweald"),
                new TerrainPreset("Fen", "fen"),
                new TerrainPreset("Flower Meadow", "flowermeadow"),
                new TerrainPreset("Greenbrake", "greenbrake"),
                new TerrainPreset("Greensward", "greensward"),
                new TerrainPreset("Grove", "grove"),
                new TerrainPreset("Hard Steppe", "hardsteppe"),
                new TerrainPreset("Heath", "heath"),
                new TerrainPreset("Highground", "highground"),
                new TerrainPreset("Leaf", "leaf"),
                new TerrainPreset("Leaf Patch", "leafpatch"),
                new TerrainPreset("Lichen Wold", "lichenwold"),
                new TerrainPreset("Lush Field", "lushfield"),
                new TerrainPreset("Moor", "moor"),
                new TerrainPreset("Moss Brush", "mossbrush"),
                new TerrainPreset("Oak Wilds", "oakwilds"),
                new TerrainPreset("Ox Pasture", "oxpasture"),
                new TerrainPreset("Peat Moss", "peatmoss"),
                new TerrainPreset("Pine Barren", "pinebarren"),
                new TerrainPreset("Red Plain", "redplain"),
                new TerrainPreset("Root Bosk", "rootbosk"),
                new TerrainPreset("Scrub Veld", "scrubveld"),
                new TerrainPreset("Shady Copse", "shadycopse"),
                new TerrainPreset("Skargard", "skargard"),
                new TerrainPreset("Sombre Bramble", "sombrebramble"),
                new TerrainPreset("Sour Timber", "sourtimber"),
                new TerrainPreset("Swamp", "swamp"),
                new TerrainPreset("Timber Land", "timberland"),
                new TerrainPreset("Wald", "wald"),
                new TerrainPreset("Wild Moor", "wildmoor"),
                new TerrainPreset("Wild Turf", "wildturf")
            );
            
            TerrainCategory ore = new TerrainCategory("Ore",
                new TerrainPreset("Cassiterite", "cassiterite"),
                new TerrainPreset("Chalcopyrite", "chalcopyrite"),
                new TerrainPreset("Malachite", "malachite"),
                new TerrainPreset("Heavy Earth (Ilmenite)", "heavyearth"),
                new TerrainPreset("Iron Ochre (Limonite)", "ironochre"),
                new TerrainPreset("Bloodstone (Hematite)", "bloodstone"),
                new TerrainPreset("Black Ore (Magnetite)", "blackore"),
                new TerrainPreset("Cinnabar", "cinnabar"),
                new TerrainPreset("Galena", "galena"),
                new TerrainPreset("Silvershine (Argentite)", "silvershine"),
                new TerrainPreset("Horn Silver", "hornsilver"),
                new TerrainPreset("Wine Glance (Cuprite)", "wineglance"),
                new TerrainPreset("Lead Glance", "leadglance"),
                new TerrainPreset("Leaf Ore", "leafore"),
                new TerrainPreset("Schrifterz", "schrifterz"),
                new TerrainPreset("Direvein", "direvein"),
                new TerrainPreset("Black Coal", "blackcoal")
            );
            
            TerrainCategory rocks = new TerrainCategory("Rocks",
                new TerrainPreset("Alabaster", "alabaster"),
                new TerrainPreset("Apatite", "apatite"),
                new TerrainPreset("Arkose", "arkose"),
                new TerrainPreset("Basalt", "basalt"),
                new TerrainPreset("Breccia", "breccia"),
                new TerrainPreset("Chert", "chert"),
                new TerrainPreset("Diabase", "diabase"),
                new TerrainPreset("Diorite", "diorite"),
                new TerrainPreset("Dolomite", "dolomite"),
                new TerrainPreset("Eclogite", "eclogite"),
                new TerrainPreset("Feldspar", "feldspar"),
                new TerrainPreset("Flint", "flint"),
                new TerrainPreset("Fluorospar", "fluorospar"),
                new TerrainPreset("Gabbro", "gabbro"),
                new TerrainPreset("Gneiss", "gneiss"),
                new TerrainPreset("Granite", "granite"),
                new TerrainPreset("Graywacke", "graywacke"),
                new TerrainPreset("Greenschist", "greenschist"),
                new TerrainPreset("Hornblende", "hornblende"),
                new TerrainPreset("Jasper", "jasper"),
                new TerrainPreset("Korund", "korund"),
                new TerrainPreset("Kyanite", "kyanite"),
                new TerrainPreset("Limestone", "limestone"),
                new TerrainPreset("Marble", "marble"),
                new TerrainPreset("Mica", "mica"),
                new TerrainPreset("Microlite", "microlite"),
                new TerrainPreset("Olivine", "olivine"),
                new TerrainPreset("Orthoclase", "orthoclase"),
                new TerrainPreset("Pegmatite", "pegmatite"),
                new TerrainPreset("Porphyry", "porphyry"),
                new TerrainPreset("Pumice", "pumice"),
                new TerrainPreset("Quarryartz", "quarryartz"),
                new TerrainPreset("Quartz", "quartz"),
                new TerrainPreset("Rhyolite", "rhyolite"),
                new TerrainPreset("Rock Salt", "rocksalt"),
                new TerrainPreset("Sandstone", "sandstone"),
                new TerrainPreset("Schist", "schist"),
                new TerrainPreset("Serpentine", "serpentine"),
                new TerrainPreset("Slate", "slate"),
                new TerrainPreset("Soapstone", "soapstone"),
                new TerrainPreset("Sodalite", "sodalite"),
                new TerrainPreset("Sunstone", "sunstone"),
                new TerrainPreset("Zincspar", "zincspar")
            );
            
            TerrainCategory paving = new TerrainCategory("Paving",
                new TerrainPreset("Teal Brick", "brick"),
                new TerrainPreset("Red Brick", "brickred"),
                new TerrainPreset("Yellow Brick", "brickyellow"),
                new TerrainPreset("Green Brick", "brickgreen"),
                new TerrainPreset("Gray Brick", "brickblack"),
                new TerrainPreset("Pink Brick", "brickpink"),
                new TerrainPreset("White Brick", "brickwhite"),
                new TerrainPreset("Blue Brick", "brickblue"),
                new TerrainPreset("Coade Stone Brick", "brickcoade"),
                new TerrainPreset("Bronze Metal", "metalbronze"),
                new TerrainPreset("Cast Iron Metal", "metalcastiron"),
                new TerrainPreset("Copper Metal", "metalcopper"),
                new TerrainPreset("Gold Metal", "metalgold"),
                new TerrainPreset("Lead Metal", "metallead"),
                new TerrainPreset("Meteoric Iron Metal", "metalmetiron"),
                new TerrainPreset("Rose Gold Metal", "metalrosegold"),
                new TerrainPreset("Silver Metal", "metalsilver"),
                new TerrainPreset("Steel Metal", "metalsteel"),
                new TerrainPreset("Tin Metal", "metaltin"),
                new TerrainPreset("Wrought Iron Metal", "metalwroughtiron"),
                new TerrainPreset("Cobblestone", "cobble"),
                new TerrainPreset("Flagstone", "flagstone"),
                new TerrainPreset("Wooden", "plank"),
                new TerrainPreset("Dirt", "dirt"),
                new TerrainPreset("Plowed Dirt", "ploweddirt")
            );
            
            TerrainCategory water = new TerrainCategory("Water",
                new TerrainPreset("Shallow Water", "shallowwater"),
                new TerrainPreset("Deep Water", "deepwater"),
                new TerrainPreset("Shallow Ocean", "shallowocean"),
                new TerrainPreset("Deep Ocean", "deepocean"),
                new TerrainPreset("High Seas", "highseas"),
                new TerrainPreset("Rock Beach", "rockbeach"),
                new TerrainPreset("Sand Cliff", "sandcliff")
            );
            
            TerrainCategory specialStones = new TerrainCategory("Special Stones",
                new TerrainPreset("Bat Rock", "batrock"),
                new TerrainPreset("Cat Gold", "catgold"),
                new TerrainPreset("Dross", "dross"),
                new TerrainPreset("Lava Rock", "lavarock"),
                new TerrainPreset("Obsidian", "obsidian"),
                new TerrainPreset("Meteorite", "meteorite"),
                new TerrainPreset("Rock Crystal", "rockcrystal"),
                new TerrainPreset("Conchite", "conchite"),
                new TerrainPreset("Slag", "slag")
            );
            
            TerrainCategory mountain = new TerrainCategory("Mountain & Cave",
                new TerrainPreset("Mountain", "mountain"),
                new TerrainPreset("Mountain Snow", "mountainsnow"),
                new TerrainPreset("Cave", "cave"),
                new TerrainPreset("Wild Cavern", "wildcavern"),
                new TerrainPreset("Lava", "lava"),
                new TerrainPreset("Ashland", "ashland")
            );
            
            // Collect all presets from all categories for "All" category
            List<TerrainPreset> allPresets = new ArrayList<>();
            allPresets.addAll(natural.presets);
            allPresets.addAll(ore.presets);
            allPresets.addAll(rocks.presets);
            allPresets.addAll(paving.presets);
            allPresets.addAll(water.presets);
            allPresets.addAll(specialStones.presets);
            allPresets.addAll(mountain.presets);
            
            TerrainCategory all = new TerrainCategory("All", allPresets.toArray(new TerrainPreset[0]));
            
            return Arrays.asList(
                all,
                natural,
                ore,
                rocks,
                paving,
                water,
                specialStones,
                mountain
            );
        }
    }
    
    // Terrain preset definition
    public static class TerrainPreset {
        public final String displayName;
        public final String searchPattern;
        public boolean enabled = false;
        
        public TerrainPreset(String displayName, String searchPattern) {
            this.displayName = displayName;
            this.searchPattern = searchPattern;
        }
    }
    
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
