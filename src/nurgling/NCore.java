package nurgling;

import haven.*;
import haven.res.ui.tt.ingred.Ingredient;
import haven.resutil.FoodInfo;
import mapv4.NMappingClient;
import monitoring.ContainerWatcher;
import monitoring.ItemWatcher;
import monitoring.NGlobalSearchItems;
import nurgling.actions.AutoDrink;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tasks.*;
import nurgling.tools.NSearchItem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
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

    public class BotmodSettings
    {
        public String user;
        public String pass;
        public String character;
        public String bot;
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
        config = new NConfig();
        config.read();
        mode = (Boolean) NConfig.get(NConfig.Key.show_drag_menu) ? Mode.DRAG : Mode.IDLE;
        mappingClient = new NMappingClient();

    }

    @Override
    public void tick(double dt)
    {
        if((Boolean) NConfig.get(NConfig.Key.ndbenable) && poolManager == null)
        {
            try {
                poolManager = new DBPoolManager(1);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        if(!(Boolean) NConfig.get(NConfig.Key.ndbenable) && poolManager != null)
        {
            poolManager.shutdown();
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
        super.tick(dt);
        if (config.isUpdated())
        {
            config.write();
        }
        if( config.isAreasUpdated())
        {
            config.writeAreas(null);
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
        NGItem item;
        java.sql.Connection connection;

        public NGItemWriter(NGItem item) {
            this.item = item;
        }

        final private static String insertRecipeSQL = "INSERT INTO recipes (recipe_hash, item_name, resource_name, hunger, energy) VALUES (?, ?, ?, ?, ?)";
        final private static String insertIngredientSQL = "INSERT INTO ingredients (recipe_hash, name, percentage) VALUES (?, ?, ?)";
        final private static String insertFepsSQL = "INSERT INTO feps (recipe_hash, name, value) VALUES (?, ?, ?)";

        @Override
        public void run() {
            try {


                PreparedStatement recipeStatement = connection.prepareStatement(insertRecipeSQL);
                PreparedStatement ingredientStatement = connection.prepareStatement(insertIngredientSQL);
                PreparedStatement fepsStatement = connection.prepareStatement(insertFepsSQL);

                NFoodInfo fi = item.getInfo(NFoodInfo.class);
                String hunger = Utils.odformat2(2 * fi.glut / (1 + Math.sqrt(item.quality / 10)) * 100, 2);
                StringBuilder hashInput = new StringBuilder();
                hashInput.append(item.getres().name).append((int) (100 * fi.energy()));

                for (ItemInfo info : item.info) {
                    if (info instanceof Ingredient) {
                        Ingredient ing = ((Ingredient) info);
                        hashInput.append(ing.name).append(ing.val * 100);
                    }
                }

                String recipeHash = NUtils.calculateSHA256(hashInput.toString());

                recipeStatement.setString(1, recipeHash);
                recipeStatement.setString(2, item.name());
                recipeStatement.setString(3, item.getres().name);
                recipeStatement.setDouble(4, Double.parseDouble(hunger));
                recipeStatement.setInt(5, (int) (fi.energy() * 100));

                recipeStatement.execute();

                // Вставляем ингредиенты
                for (ItemInfo info : item.info) {
                    if (info instanceof Ingredient) {
                        ingredientStatement.setString(1, recipeHash);
                        ingredientStatement.setString(2, ((Ingredient) info).name);
                        ingredientStatement.setDouble(3, ((Ingredient) info).val * 100);
                        ingredientStatement.executeUpdate();
                    }
                }

                // Вставляем эффекты (FEPS)
                for (FoodInfo.Event ef : fi.evs) {
                    fepsStatement.setString(1, recipeHash);
                    fepsStatement.setString(2, ef.ev.nm);
                    fepsStatement.setDouble(3, ef.a / fi.cons);
                    fepsStatement.executeUpdate();
                }

                // Фиксируем транзакцию
                connection.commit();

            } catch (SQLException e) {
                try {
                    // В случае ошибки откатываем транзакцию
                    if (connection != null) {
                        connection.rollback();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                if (!e.getSQLState().equals("23505")) {  // Код ошибки для нарушения уникальности
                    e.printStackTrace();
                }
            }
        }
    }

    public void writeNGItem(NGItem item) {
        NGItemWriter ngItemWriter = new NGItemWriter(item);
        ngItemWriter.connection = poolManager.connection;
        poolManager.submitTask(ngItemWriter);
    }

    public void writeContainerInfo(NInventory.ParentGob gob)
    {
        if(gob.gob!=null) {
            ContainerWatcher cw = new ContainerWatcher(gob);
            cw.connection = poolManager.connection;
            poolManager.submitTask(cw);
        }
    }

    public void writeItemInfoForContainer(ArrayList<ItemWatcher.ItemInfo> iis) {

        ItemWatcher itemWatcher = new ItemWatcher(iis);
        itemWatcher.connection = poolManager.connection;
        poolManager.submitTask(itemWatcher);

    }

    final ArrayList<String> targetGobs = new ArrayList<>();

    public void searchContainer(NSearchItem item) {

        NGlobalSearchItems gsi = new NGlobalSearchItems(item);
        gsi.connection = poolManager.connection;
        poolManager.submitTask(gsi);

    }
}
