package nurgling;

import haven.*;
import mapv4.NMappingClient;
import nurgling.actions.AutoDrink;
import nurgling.tasks.*;

import java.util.*;
import java.util.concurrent.*;

public class NCore extends Widget
{
    public boolean debug = false;
    boolean isinspect = false;
    public NMappingClient mappingClient;
    public AutoDrink autoDrink = null;
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
        actions.gob = null;
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
}
