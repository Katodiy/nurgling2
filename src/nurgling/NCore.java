package nurgling;

import haven.*;
import nurgling.tasks.*;

import java.util.*;
import java.util.concurrent.*;

public class NCore extends Widget
{
    boolean debug = true;
    boolean isinspect = false;
    public boolean isInspectMode()
    {
        if(debug)
        {
            return isinspect;
        }
        return false;
    }

    public enum Mode
    {
        IDLE,
        DRAG
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
