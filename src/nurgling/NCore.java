package nurgling;

import haven.*;

import java.util.*;
import java.util.concurrent.*;

public class NCore extends Widget
{

    public enum Mode
    {
        IDLE,
        DRAG
    }

    public Mode mode = Mode.IDLE;
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

    private final LinkedList<Task> for_remove = new LinkedList<>();
    private final ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();

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
            for(final Task task: tasks)
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

    public abstract static class Task
    {
        public abstract boolean check();
    };

    public void addTask(final Task task) throws InterruptedException
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
