package nurgling;

import haven.*;
import haven.res.lib.itemtex.ItemTex;
import haven.res.ui.tt.ingred.Ingredient;
import haven.resutil.FoodInfo;
import mapv4.NMappingClient;
import monitoring.ContainerWatcher;
import monitoring.ItemWatcher;
import monitoring.NGlobalSearchItems;
import nurgling.actions.AutoDrink;
import nurgling.actions.AutoSaveTableware;
import nurgling.iteminfo.NFoodInfo;
import nurgling.equipment.EquipmentPresetManager;
import nurgling.scenarios.ScenarioManager;
import nurgling.tasks.*;
import nurgling.tools.NSearchItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class NCore extends Widget
{
    public boolean debug = false;
    boolean isinspect = false;
    public NMappingClient mappingClient;
    public AutoDrink autoDrink = null;
    public AutoSaveTableware autoSaveTableware = null;
    public ScenarioManager scenarioManager = new ScenarioManager();
    public EquipmentPresetManager equipmentPresetManager = new EquipmentPresetManager();

    public static volatile nurgling.db.DatabaseManager databaseManager = null;
    public boolean isInspectMode()
    {
        if(debug)
        {
            return isinspect;
        }
        return false;
    }

    public void enableBotMod()
    {
        botmod = true;
    }

    public void disableBotMod()
    {
        botmod = false;
    }

    public void resetTasks()
    {
        synchronized (tasks)
        {
            if(tasks.size()>0)
            {
                for (final NTask task : tasks)
                {
                    task.notify();
                }
                tasks.clear();
            }
        }
    }

    public void setLastAction() {
        actions = null;
    }


    public enum Mode
    {
        IDLE,
        DRAG
    }

    public class LastActions
    {
        public String petal = null;
        public WItem item = null;
        public Gob gob = null;
    }

    private LastActions actions = null;

    public LastActions getLastActions()
    {
        return actions;
    }

    public void setLastAction(String petal, WItem item)
    {
        actions = new LastActions();
        actions.petal = petal;
        actions.item = item;
    }

    public void setLastAction(String petal, Gob gob)
    {
        actions = new LastActions();
        actions.petal = petal;
        actions.gob = gob;
    }

    public void setLastAction(Gob gob)
    {
        actions = new LastActions();
        actions.gob = gob;
    }

    public void setLastAction(WItem item)
    {
        actions = new LastActions();
        actions.item = item;
        actions.gob = null;
    }
    public void resetLastAction()
    {
        actions = null;
    }


    public Mode mode = Mode.DRAG;
    private boolean botmod = false;
    public boolean enablegrid = true;

    public static class BotmodSettings
    {
        public String user;
        public String pass;
        public String character;
        public Integer scenarioId;
        public String stackTraceFile; // Path to stack trace file for autorunner debugging

        public BotmodSettings(String user, String password, String character, Integer scenarioId) {
            this.user = user;
            this.pass = password;
            this.character = character;
            this.scenarioId = scenarioId;
        }
    }

    private BotmodSettings bms;

    private final LinkedList<NTask> for_remove = new LinkedList<>();
    private final ConcurrentLinkedQueue<NTask> tasks = new ConcurrentLinkedQueue<>();
    
    /**
     * Get list of active task names for debug display
     */
    public String[] getActiveTaskNames() {
        synchronized (tasks) {
            if (tasks.isEmpty()) {
                return new String[0];
            }
            return tasks.stream()
                .map(t -> {
                    String name = t.getClass().getName();
                    // Shorten package names
                    name = name.replace("nurgling.actions.", "");
                    name = name.replace("nurgling.tasks.", "");
                    // For anonymous classes, show parent class
                    if (name.contains("$")) {
                        int dollarIdx = name.indexOf('$');
                        String parent = name.substring(0, dollarIdx);
                        String suffix = name.substring(dollarIdx);
                        // Get just class name from parent
                        int lastDot = parent.lastIndexOf('.');
                        if (lastDot > 0) {
                            parent = parent.substring(lastDot + 1);
                        }
                        name = parent + suffix;
                    }
                    return name;
                })
                .toArray(String[]::new);
        }
    }
    
    /**
     * Get count of active tasks
     */
    public int getActiveTaskCount() {
        return tasks.size();
    }

    public BotmodSettings getBotMod()
    {
        return bms;
    }

    public boolean isBotmod()
    {
        return botmod;
    }

    public NConfig config;

    public NCore()
    {
        config = MainFrame.config;
        mode = (Boolean) NConfig.get(NConfig.Key.show_drag_menu) ? Mode.DRAG : Mode.IDLE;
        mappingClient = new NMappingClient();

    }

    /**
     * Updates the config instance to use profile-aware configuration
     * This should be called when the genus becomes available
     */
    public void updateConfigForProfile(String genus) {
        if (genus != null && !genus.isEmpty()) {
            config = nurgling.profiles.ConfigFactory.getConfig(genus);
        }
    }

    private static final Object dbLock = new Object();

    @Override
    public void tick(double dt)
    {
        if((Boolean) NConfig.get(NConfig.Key.ndbenable) && databaseManager == null)
        {
            synchronized (dbLock) {
                if (databaseManager == null) {  // Double-check inside lock
                    databaseManager = new nurgling.db.DatabaseManager(1);
                    // Start area and route sync after database is initialized
                    startAreaSync();
                }
            }
        }

        if(!(Boolean) NConfig.get(NConfig.Key.ndbenable) && databaseManager != null)
        {
            synchronized (dbLock) {
                if (databaseManager != null) {
                    stopAreaSync();
                    databaseManager.shutdown();
                    databaseManager = null;
                }
            }
        }

        if(autoDrink == null && (Boolean)NConfig.get(NConfig.Key.autoDrink))
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        (autoDrink = new AutoDrink()).run(NUtils.getGameUI());
                    } catch (InterruptedException ignored) {
                    }
                }
            }).start();
        }
        else
        {
            if(autoDrink != null && !(Boolean)NConfig.get(NConfig.Key.autoDrink))
            {
                AutoDrink.stop.set(true);
            }
        }

        if(autoSaveTableware == null && (Boolean)NConfig.get(NConfig.Key.autoSaveTableware))
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        (autoSaveTableware = new AutoSaveTableware()).run(NUtils.getGameUI());
                    } catch (InterruptedException ignored) {
                    }
                }
            }).start();
        }
        else
        {
            if(autoSaveTableware != null && !(Boolean)NConfig.get(NConfig.Key.autoSaveTableware))
            {
                AutoSaveTableware.stop.set(true);
            }
        }
        super.tick(dt);
        
        // Save global config (UI settings, credentials, etc.)
        if (NConfig.current != null && NConfig.current.isUpdated())
        {
            NConfig.current.write();
        }
        
        // Save profile-specific config and data
        if (config.isUpdated())
        {
            config.write();
        }
        if (config.isAreasUpdated())
        {
            config.writeAreas(null);
        }
        if (config.isExploredUpdated())
        {
            config.writeExploredArea(null);
        }
        if (config.isScenariosUpdated())
        {
            config.writeScenarios(null);
        }
        synchronized (tasks)
        {
            for(final NTask task: tasks)
            {
                try
                {
                    if(task.baseCheck())
                    {
                        synchronized (task)
                        {
                            task.notify();
                        }
                        for_remove.add(task);
                    }
                }
                catch (Loading e)
                {
                    NUtils.getGameUI().error(task.toString());
                }
            }
            tasks.removeAll(for_remove);
            for_remove.clear();
        }
        mappingClient.tick(dt);
    }



    public void addTask(final NTask task) throws InterruptedException
    {
        if(!task.baseCheck())
        {
            synchronized (tasks)
            {
                tasks.add(task);
            }
            synchronized (task)
            {
                try {
                    task.wait();
                    if(task.criticalExit)
                    {
                        ui.gui.error("Incorrect final of task " + task.getClass().toString());
                    }
                }
                catch (InterruptedException e)
                {
                    synchronized (tasks)
                    {
                        tasks.remove(task);
                        throw e;
                    }
                }

            }
        }
        if(task.criticalExit)
        {
            new InterruptedException();
        }
    }

    @Override
    public void dispose() {
        mappingClient.done.set(true);
        // Don't shutdown databaseManager here - it's static and should persist across UI/session changes
        // It will be shutdown only when the application exits or database is disabled
        super.dispose();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        synchronized (tasks) {
            for (NTask task : tasks) {
                res.append(task.toString() + "|");
            }
        }
        return res.toString();
    }


    // In-memory cache of recently sent recipe hashes to avoid duplicate DB writes
    private static final Set<String> sentRecipeHashes = ConcurrentHashMap.newKeySet();
    private static final int MAX_RECIPE_CACHE_SIZE = 5000;
    
    // Quick cache for early filtering (name + energy) - checked BEFORE creating task
    private static final Set<String> recipeQuickCache = ConcurrentHashMap.newKeySet();
    private static final int MAX_QUICK_CACHE_SIZE = 2000;
    
    // Pending recipe tasks counter for debug
    private static final java.util.concurrent.atomic.AtomicInteger pendingRecipeTasks = new java.util.concurrent.atomic.AtomicInteger(0);
    
    /**
     * Get current recipe cache size for debug display
     */
    public static int getRecipeCacheSize() {
        return sentRecipeHashes.size();
    }
    
    /**
     * Get pending recipe tasks count for debug
     */
    public static int getPendingRecipeTasks() {
        return pendingRecipeTasks.get();
    }
    
    /**
     * Check if recipe hash is already in cache (call from main thread before creating task)
     */
    public static boolean isRecipeInCache(String recipeHash) {
        return sentRecipeHashes.contains(recipeHash);
    }
    
    /**
     * Add recipe hash to cache
     */
    public static void addRecipeToCache(String recipeHash) {
        if (sentRecipeHashes.size() >= MAX_RECIPE_CACHE_SIZE) {
            // Simple eviction: clear half of the cache when full
            int toRemove = MAX_RECIPE_CACHE_SIZE / 2;
            java.util.Iterator<String> it = sentRecipeHashes.iterator();
            while (it.hasNext() && toRemove > 0) {
                it.next();
                it.remove();
                toRemove--;
            }
        }
        sentRecipeHashes.add(recipeHash);
    }
    
    /**
     * Check if recipe is in quick cache (early filtering before creating task)
     */
    public static boolean isRecipeQuickCached(String quickKey) {
        return recipeQuickCache.contains(quickKey);
    }
    
    /**
     * Add to quick cache
     */
    public static void addRecipeQuickCache(String quickKey) {
        if (recipeQuickCache.size() >= MAX_QUICK_CACHE_SIZE) {
            // Simple eviction
            int toRemove = MAX_QUICK_CACHE_SIZE / 2;
            java.util.Iterator<String> it = recipeQuickCache.iterator();
            while (it.hasNext() && toRemove > 0) {
                it.next();
                it.remove();
                toRemove--;
            }
        }
        recipeQuickCache.add(quickKey);
    }
    
    /**
     * Get quick cache size for debug
     */
    public static int getRecipeQuickCacheSize() {
        return recipeQuickCache.size();
    }
    
    public static class NGItemWriter implements Runnable {
        private final NGItem item;
        private final nurgling.db.DatabaseManager databaseManager;

        public NGItemWriter(NGItem item, nurgling.db.DatabaseManager databaseManager) {
            this.item = item;
            this.databaseManager = databaseManager;
        }

        @Override
        public void run() {
            try {
                // Extract food information
                NFoodInfo fi = item.getInfo(NFoodInfo.class);
                if (fi == null) {
                    return; // Not a food item
                }

                // Calculate hunger value
                String hunger = Utils.odformat2(2 * fi.glut / (1 + Math.sqrt(item.quality / 10)) * 1000, 2);

                // Get composite resource name from item sprite
                String resourceName = getCompositeResourceName();

                // Build recipe hash
                String recipeHash = buildRecipeHash(fi, resourceName);
                
                // Check if we already sent this recipe (in-memory cache)
                if (sentRecipeHashes.contains(recipeHash)) {
                    nurgling.db.DatabaseManager.incrementSkippedRecipe();
                    return; // Already sent, skip DB write
                }

                // Extract ingredients (including smoking wood)
                java.util.Map<String, nurgling.cookbook.Recipe.IngredientInfo> ingredients = extractIngredients();

                // Extract food effects (FEPs)
                java.util.Map<String, nurgling.cookbook.Recipe.Fep> feps = extractFeps(fi);

                // Create recipe object
                nurgling.cookbook.Recipe recipe = new nurgling.cookbook.Recipe(
                    recipeHash,
                    item.name(),
                    resourceName,
                    Double.parseDouble(hunger),
                    (int) (fi.energy() * 100),
                    ingredients,
                    feps
                );

                // Add to cache before saving (prevents duplicates during async save)
                if (sentRecipeHashes.size() >= MAX_RECIPE_CACHE_SIZE) {
                    // Simple eviction: clear half of the cache when full
                    int toRemove = MAX_RECIPE_CACHE_SIZE / 2;
                    Iterator<String> it = sentRecipeHashes.iterator();
                    while (it.hasNext() && toRemove > 0) {
                        it.next();
                        it.remove();
                        toRemove--;
                    }
                }
                sentRecipeHashes.add(recipeHash);

                // Save recipe using service (handles duplicates gracefully)
                databaseManager.getRecipeService().saveRecipeAsync(recipe)
                    .exceptionally(ex -> {
                        System.err.println("Failed to save recipe: " + ex.getMessage());
                        return null;
                    });

            } catch (Exception e) {
                // Log error but don't crash - recipe import should be resilient
                System.err.println("Failed to save recipe for item: " + item.name());
                e.printStackTrace();
            } finally {
                // Always decrement pending counter
                pendingRecipeTasks.decrementAndGet();
            }
        }

        private String getCompositeResourceName() {
            String resourceName = item.getres().name;
            try {
                GSprite spr = item.spr;
                if (spr != null) {
                    JSONObject saved = ItemTex.save(spr);
                    if (saved != null) {
                        if (saved.has("layer")) {
                            JSONArray layers = saved.getJSONArray("layer");
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < layers.length(); i++) {
                                if (i > 0) sb.append("+");
                                sb.append(layers.getString(i));
                            }
                            resourceName = sb.toString();
                        } else if (saved.has("static")) {
                            resourceName = saved.getString("static");
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to base resource name
            }
            return resourceName;
        }

        private String buildRecipeHash(NFoodInfo fi, String resourceName) {
            StringBuilder hashInput = new StringBuilder();
            hashInput.append(item.name).append((int) (100 * fi.energy()));
            hashInput.append(resourceName);

            for (ItemInfo info : item.info) {
                if (info instanceof Ingredient) {
                    Ingredient ing = (Ingredient) info;
                    // Use resName if available, otherwise fall back to name
                    if (ing.resName != null) {
                        hashInput.append(ing.resName);
                    } else {
                        hashInput.append(ing.name);
                    }
                    if (ing.val != null) {
                        hashInput.append(ing.val * 100);
                    }
                }
            }

            // Add smoking wood info to hash so different smoking materials create different recipes
            // Format matches regular ingredients: resName/name + (val * 100), where val=1.0 for smoking (100%)
            try {
                for (ItemInfo info : item.info) {
                    if (info.getClass().getName().contains("Smoke")) {
                        try {
                            // Try to get resource name first, then fall back to name
                            String woodIdentifier = null;
                            try {
                                java.lang.reflect.Field resNameField = info.getClass().getDeclaredField("resName");
                                resNameField.setAccessible(true);
                                woodIdentifier = (String) resNameField.get(info);
                            } catch (NoSuchFieldException e) {
                                // resName field doesn't exist
                            }
                            if (woodIdentifier == null || woodIdentifier.isEmpty()) {
                                java.lang.reflect.Field nameField = info.getClass().getDeclaredField("name");
                                nameField.setAccessible(true);
                                woodIdentifier = (String) nameField.get(info);
                            }
                            if (woodIdentifier != null && !woodIdentifier.isEmpty()) {
                                hashInput.append(woodIdentifier);
                                hashInput.append(1.0 * 100); // Smoking wood is always 100%
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            // Could not extract smoking wood info
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignore errors in smoking wood extraction
            }

            return NUtils.calculateSHA256(hashInput.toString());
        }

        private java.util.Map<String, nurgling.cookbook.Recipe.IngredientInfo> extractIngredients() {
            java.util.Map<String, nurgling.cookbook.Recipe.IngredientInfo> ingredients = new java.util.HashMap<>();

            // Extract regular ingredients
            for (ItemInfo info : item.info) {
                if (info instanceof Ingredient) {
                    Ingredient ing = (Ingredient) info;
                    double percentage = ing.val != null ? ing.val * 100 : 0;
                    // Use pretty name as key, store resName in IngredientInfo for resource lookup
                    ingredients.put(ing.name, new nurgling.cookbook.Recipe.IngredientInfo(percentage, ing.resName));
                }
            }
            
            // Extract smoking wood information from Smoke ItemInfo
            try {
                for (ItemInfo info : item.info) {
                    // Check if this is the Smoke info (dynamically loaded from resources)
                    if (info.getClass().getName().contains("Smoke")) {
                        // Try to get wood information via reflection
                        try {
                            java.lang.reflect.Field nameField = info.getClass().getDeclaredField("name");
                            nameField.setAccessible(true);
                            String woodName = (String) nameField.get(info);
                            
                            // Try to get resource name
                            String woodResName = null;
                            try {
                                java.lang.reflect.Field resNameField = info.getClass().getDeclaredField("resName");
                                resNameField.setAccessible(true);
                                woodResName = (String) resNameField.get(info);
                            } catch (NoSuchFieldException e) {
                                // resName field doesn't exist, use name only
                            }
                            
                            if (woodName != null && !woodName.isEmpty()) {
                                // Add wood as ingredient with 100% (smoking wood is always 100%)
                                ingredients.put(woodName, new nurgling.cookbook.Recipe.IngredientInfo(100.0, woodResName));
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            System.err.println("Could not extract smoking wood info: " + e.getMessage());
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignore errors in smoking wood extraction - item might not be smoked
            }

            return ingredients;
        }

        private java.util.Map<String, nurgling.cookbook.Recipe.Fep> extractFeps(NFoodInfo fi) {
            java.util.Map<String, nurgling.cookbook.Recipe.Fep> feps = new java.util.HashMap<>();
            double multiplier = Math.sqrt(item.quality / 10.0);

            for (FoodInfo.Event ef : fi.evs) {
                double value = Double.parseDouble(Utils.odformat2(ef.a / multiplier, 2));
                double weight = Double.parseDouble(Utils.odformat2(ef.a / fi.fepSum, 2));
                feps.put(ef.ev.nm, new nurgling.cookbook.Recipe.Fep(value, weight));
            }

            return feps;
        }
    }

    public void writeNGItem(NGItem item) {
        if (databaseManager == null) {
            return;
        }
        
        pendingRecipeTasks.incrementAndGet();
        NGItemWriter ngItemWriter = new NGItemWriter(item, databaseManager);
        databaseManager.submitTask(ngItemWriter);
    }

    public void writeContainerInfo(Gob gob) {
        if (gob != null && databaseManager != null && databaseManager.isReady()) {
            ContainerWatcher cw = new ContainerWatcher(gob, databaseManager);
            databaseManager.submitTask(cw);
        }
    }

    /**
     * Clear all items for a container when it's opened (to refresh data)
     * Note: Don't notify search here - data is being cleared, search will update when container closes
     */
    public void clearContainerItems(Gob gob) {
        if (gob == null || databaseManager == null || !databaseManager.isReady()) {
            return;
        }
        databaseManager.submitTask(() -> {
            try {
                // Wait for hash to be available
                int waitCount = 0;
                while (gob.ngob.hash == null && waitCount < 100) {
                    Thread.sleep(10);
                    waitCount++;
                }
                if (gob.ngob.hash != null) {
                    databaseManager.getStorageItemService().deleteStorageItemsByContainer(gob.ngob.hash);
                    // Don't notify search here - container is being browsed, data will be saved when closed
                }
            } catch (Exception e) {
                // Silently ignore errors during container item clearing
            }
        });
    }

    public void writeItemInfoForContainer(ArrayList<ItemWatcher.ItemInfo> iis, String containerHash) {
        if (databaseManager == null || !databaseManager.isReady()) {
            return;
        }
        ItemWatcher itemWatcher = new ItemWatcher(iis, databaseManager, containerHash);
        databaseManager.submitTask(itemWatcher);
    }

    final ArrayList<String> targetGobs = new ArrayList<>();

    public void searchContainer(NSearchItem item) {
        if (databaseManager == null || !databaseManager.isReady()) {
            return;
        }
        NGlobalSearchItems gsi = new NGlobalSearchItems(item, databaseManager);
        databaseManager.submitTask(gsi);
    }

    private static volatile boolean areaSyncStarted = false;
    private static volatile boolean routeSyncStarted = false;

    /**
     * Start periodic area sync from database
     */
    private void startAreaSync() {
        if (areaSyncStarted || databaseManager == null || !databaseManager.isReady()) {
            return;
        }

        // Get current profile
        String profile = "global";
        try {
            if (NUtils.getGameUI() != null) {
                String genus = NUtils.getGameUI().getGenus();
                if (genus != null && !genus.isEmpty()) {
                    profile = genus;
                }
            }
        } catch (Exception e) {
            // Use default profile
        }

        final String syncProfile = profile;

        // Start sync with 4 second interval
        databaseManager.getAreaService().startSync(syncProfile, 4,
            new nurgling.db.service.AreaService.AreaSyncCallback() {
                @Override
                public void onAreasUpdated(java.util.List<nurgling.areas.NArea> updatedAreas) {
                    // Update areas in map cache
                    if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null &&
                        NUtils.getGameUI().map.glob != null && NUtils.getGameUI().map.glob.map != null) {
                        long now = System.currentTimeMillis();
                        int skipped = 0;
                        int updated = 0;
                        boolean needsWidgetRefresh = false;
                        for (nurgling.areas.NArea newArea : updatedAreas) {
                            // Check if this area was deleted locally - don't restore it
                            boolean isLocallyDeleted = ((NMapView)NUtils.getGameUI().map).isLocallyDeleted(newArea.id);
                            if (isLocallyDeleted) {
                                System.out.println("Area sync: Skipping locally deleted area " + newArea.id + " (" + newArea.name + ")");
                                skipped++;
                                continue;
                            }

                            // Check if this area was modified locally recently
                            // Window = debounce(3s) + save time(2s) + buffer(5s) = 10s
                            nurgling.areas.NArea localArea = NUtils.getGameUI().map.glob.map.areas.get(newArea.id);
                            if (localArea != null && (now - localArea.lastLocalChange) < 10000) {
                                // Skip - local changes are still pending save
                                skipped++;
                                continue;
                            }

                            // Also skip if local version >= DB version (we just saved it)
                            if (localArea != null && localArea.version >= newArea.version) {
                                skipped++;
                                continue;
                            }

                            if (localArea != null) {
                                // Update existing area object (preserves references in labels/lists)
                                localArea.updateFrom(newArea);
                            } else {
                                // New area - add it
                                NUtils.getGameUI().map.glob.map.areas.put(newArea.id, newArea);
                            }
                            needsWidgetRefresh = true;
                            
                            // Force overlay to redraw
                            try {
                                nurgling.overlays.map.NOverlay overlay = NUtils.getGameUI().map.nols.get(newArea.id);
                                if (overlay != null) {
                                    overlay.requpdate2 = true;
                                }
                            } catch (Exception e) {
                                // Ignore overlay refresh errors
                            }
                            updated++;
                        }
                        
                        // Refresh area labels and widget
                        if (needsWidgetRefresh) {
                            refreshAreaLabelsAndWidget();
                        }
                        
                        if (updated > 0) {
                            System.out.println("Updated " + updated + " areas from database" + (skipped > 0 ? " (skipped " + skipped + " with pending local changes)" : ""));
                        }
                    }
                }

                @Override
                public void onAreaDeleted(int areaId) {
                    // Remove area from map cache
                    if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null &&
                        NUtils.getGameUI().map.glob != null && NUtils.getGameUI().map.glob.map != null) {
                        NUtils.getGameUI().map.glob.map.areas.remove(areaId);
                        refreshAreaLabelsAndWidget();
                        System.out.println("Deleted area " + areaId + " from database sync");
                    }
                }

                @Override
                public void onFullSync(java.util.Map<Integer, nurgling.areas.NArea> allAreas) {
                    // Replace all areas in map cache, but filter out locally deleted areas
                    if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null &&
                        NUtils.getGameUI().map.glob != null && NUtils.getGameUI().map.glob.map != null) {
                        NUtils.getGameUI().map.glob.map.areas.clear();
                        for (java.util.Map.Entry<Integer, nurgling.areas.NArea> entry : allAreas.entrySet()) {
                            // Skip areas that were deleted locally
                            boolean isLocallyDeleted = ((NMapView)NUtils.getGameUI().map).isLocallyDeleted(entry.getKey());
                            if (isLocallyDeleted) {
                                System.out.println("Full sync: Skipping locally deleted area " + entry.getKey() + " (" + entry.getValue().name + ")");
                                continue;
                            }
                            NUtils.getGameUI().map.glob.map.areas.put(entry.getKey(), entry.getValue());
                        }
                        refreshAreaLabelsAndWidget();
                        System.out.println("Full sync: loaded " + allAreas.size() + " areas from database");
                    }
                }
            });

        areaSyncStarted = true;
        System.out.println("Area sync started for profile: " + syncProfile);
    }

    /**
     * Refresh area labels on map and NAreasWidget after sync update
     */
    private static void refreshAreaLabelsAndWidget() {
        try {
            if (NUtils.getGameUI() == null || NUtils.getGameUI().map == null) return;
            
            nurgling.NMapView map = (nurgling.NMapView) NUtils.getGameUI().map;
            
            // Refresh area overlays on map
            if (map.nols != null) {
                for (nurgling.overlays.map.NOverlay overlay : map.nols.values()) {
                    if (overlay != null) {
                        overlay.requpdate2 = true;
                    }
                }
            }
            
            // Update area labels (NAreaLabel sprites on dummy gobs)
            if (map.dummys != null) {
                for (haven.Gob dummy : map.dummys.values()) {
                    if (dummy != null) {
                        for (haven.Gob.Overlay ol : dummy.ols) {
                            if (ol != null && ol.spr instanceof nurgling.overlays.NAreaLabel) {
                                ((nurgling.overlays.NAreaLabel) ol.spr).update();
                            }
                        }
                    }
                }
            }
            
            // Refresh NAreasWidget if open
            if (NUtils.getGameUI().areas != null && NUtils.getGameUI().areas.al != null) {
                // Trigger list refresh by re-showing current path
                NUtils.getGameUI().areas.showPath(NUtils.getGameUI().areas.currentPath);
            }
        } catch (Exception e) {
            // Ignore refresh errors
        }
    }

    /**
     * Stop periodic area sync
     */
    private void stopAreaSync() {
        if (databaseManager != null && databaseManager.getAreaService() != null) {
            databaseManager.getAreaService().stopSync();
        }
        areaSyncStarted = false;
    }

}
