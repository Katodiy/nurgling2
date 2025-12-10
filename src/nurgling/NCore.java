package nurgling;

import haven.*;
import haven.res.ui.tt.ingred.Ingredient;
import haven.resutil.FoodInfo;
import mapv4.NMappingClient;
import monitoring.ContainerWatcher;
import monitoring.ItemWatcher;
import monitoring.NGlobalSearchItems;
import nurgling.actions.AutoDrink;
import nurgling.actions.AutoSaveTableware;
import nurgling.iteminfo.NFoodInfo;
import nurgling.scenarios.ScenarioManager;
import nurgling.tasks.*;
import nurgling.tools.NSearchItem;

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

    public DBPoolManager poolManager = null;
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

    @Override
    public void tick(double dt)
    {
        if((Boolean) NConfig.get(NConfig.Key.ndbenable) && poolManager == null)
        {
            poolManager = new DBPoolManager(1);
        }

        if(!(Boolean) NConfig.get(NConfig.Key.ndbenable) && poolManager != null)
        {
            poolManager.shutdown();
            poolManager = null;
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
        if (config.isRoutesUpdated())
        {
            config.writeRoutes(null);
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
                    if(task.check())
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
        if(!task.check())
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
        if(poolManager!=null)
        {
            poolManager.shutdown();
            poolManager = null;
        }
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


    public static class NGItemWriter implements Runnable {
        private final NGItem item;
        private final DBPoolManager poolManager;

        public NGItemWriter(NGItem item, DBPoolManager poolManager) {
            this.item = item;
            this.poolManager = poolManager;
        }

        private String getInsertRecipeSQL() {
            if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                return "INSERT INTO recipes (recipe_hash, item_name, resource_name, hunger, energy) VALUES (?, ?, ?, ?, ?) ON CONFLICT(recipe_hash) DO NOTHING";
            } else { // SQLite
                return "INSERT OR IGNORE INTO recipes (recipe_hash, item_name, resource_name, hunger, energy) VALUES (?, ?, ?, ?, ?)";
            }
        }

        private String getInsertIngredientSQL() {
            if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                return "INSERT INTO ingredients (recipe_hash, name, percentage) VALUES (?, ?, ?) ON CONFLICT(recipe_hash, name) DO NOTHING";
            } else { // SQLite
                return "INSERT OR IGNORE INTO ingredients (recipe_hash, name, percentage) VALUES (?, ?, ?)";
            }
        }

        private String getInsertFepsSQL() {
            if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                return "INSERT INTO feps (recipe_hash, name, value, weight) VALUES (?, ?, ?, ?) ON CONFLICT(recipe_hash, name) DO NOTHING";
            } else { // SQLite
                return "INSERT OR IGNORE INTO feps (recipe_hash, name, value, weight) VALUES (?, ?, ?, ?)";
            }
        }

        @Override
        public void run() {
            if (!(Boolean) NConfig.get(NConfig.Key.postgres) && !(Boolean) NConfig.get(NConfig.Key.sqlite)) {
                return;
            }

            java.sql.Connection conn = null;
            try {
                conn = poolManager.getConnection();
                if (conn == null) {
                    System.err.println("NGItemWriter: Unable to get database connection");
                    return;
                }

                PreparedStatement recipeStatement = conn.prepareStatement(getInsertRecipeSQL());
                PreparedStatement ingredientStatement = conn.prepareStatement(getInsertIngredientSQL());
                PreparedStatement fepsStatement = conn.prepareStatement(getInsertFepsSQL());

                NFoodInfo fi = item.getInfo(NFoodInfo.class);
                String hunger = Utils.odformat2(2 * fi.glut / (1 + Math.sqrt(item.quality / 10)) * 1000, 2);
                StringBuilder hashInput = new StringBuilder();
                hashInput.append(item.name).append((int) (100 * fi.energy()));

                for (ItemInfo info : item.info) {
                    if (info instanceof Ingredient) {
                        Ingredient ing = ((Ingredient) info);
                        // Use resName if available (for unique identification of meats, fish, etc.)
                        // Fall back to name if resName is null
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

                String recipeHash = NUtils.calculateSHA256(hashInput.toString());

                recipeStatement.setString(1, recipeHash);
                recipeStatement.setString(2, item.name());
                recipeStatement.setString(3, item.getres().name);
                recipeStatement.setDouble(4, Double.parseDouble(hunger));
                recipeStatement.setInt(5, (int) (fi.energy() * 100));
                recipeStatement.execute();

                for (ItemInfo info : item.info) {
                    if (info instanceof Ingredient) {
                        Ingredient ing = (Ingredient) info;
                        ingredientStatement.setString(1, recipeHash);
                        ingredientStatement.setString(2, ing.name);
                        ingredientStatement.setDouble(3, ing.val != null ? ing.val * 100 : 0);
                        ingredientStatement.executeUpdate();
                    }
                }

                double multiplier = Math.sqrt(item.quality / 10.0);
                for (FoodInfo.Event ef : fi.evs) {
                    fepsStatement.setString(1, recipeHash);
                    fepsStatement.setString(2, ef.ev.nm);
                    fepsStatement.setDouble(3, Double.parseDouble(Utils.odformat2(ef.a / multiplier, 2)));
                    fepsStatement.setDouble(4, Double.parseDouble(Utils.odformat2(ef.a / fi.fepSum, 2)));
                    fepsStatement.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                // Ignore unique constraint violations
                if ((Boolean) NConfig.get(NConfig.Key.postgres)) {
                    if (e.getSQLState() == null || !e.getSQLState().equals("23505")) {
                        e.printStackTrace();
                    }
                } else if ((Boolean) NConfig.get(NConfig.Key.sqlite)) {
                    if (!e.getMessage().contains("UNIQUE constraint")) {
                        e.printStackTrace();
                    }
                }
            } finally {
                if (conn != null) {
                    poolManager.returnConnection(conn);
                }
            }
        }
    }

    public void writeNGItem(NGItem item) {
        if (poolManager == null || !poolManager.isConnectionReady()) {
            return;
        }
        NGItemWriter ngItemWriter = new NGItemWriter(item, poolManager);
        poolManager.submitTask(ngItemWriter);
    }

    public void writeContainerInfo(Gob gob) {
        if (gob != null && poolManager != null && poolManager.isConnectionReady()) {
            ContainerWatcher cw = new ContainerWatcher(gob, poolManager);
            poolManager.submitTask(cw);
        }
    }

    public void writeItemInfoForContainer(ArrayList<ItemWatcher.ItemInfo> iis) {
        if (poolManager == null || !poolManager.isConnectionReady()) {
            return;
        }
        ItemWatcher itemWatcher = new ItemWatcher(iis, poolManager);
        poolManager.submitTask(itemWatcher);
    }

    final ArrayList<String> targetGobs = new ArrayList<>();

    public void searchContainer(NSearchItem item) {
        if (poolManager == null || !poolManager.isConnectionReady()) {
            return;
        }
        NGlobalSearchItems gsi = new NGlobalSearchItems(item, poolManager);
        poolManager.submitTask(gsi);
    }
}
