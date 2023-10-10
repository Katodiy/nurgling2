package nurgling;

import haven.*;
import nurgling.tasks.*;

import java.util.*;
import java.util.concurrent.*;

public class NCore extends Widget
{
    public boolean debug = true;
    boolean isinspect = false;
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

    private LastActions actions = new LastActions();

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
    }
    public void resetLastAction()
    {
        actions = new LastActions();
    }


    public Mode mode = Mode.IDLE;
    private boolean botmod = false;
    public boolean enablegrid = true;

    public NPFMap pfMap = new NPFMap(MCache.tilesz2.x/4.d);
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

    NConfig config;

    public NCore()
    {
        config = new NConfig();
        config.read();

        add(pfMap);
    }

    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        if (config.isUpdated())
        {
            config.write();
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
                task.wait();
            }
        }
    }
}
