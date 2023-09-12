package nurgling;

import haven.*;

public class NCore extends Widget
{

    public enum Mode
    {
        IDLE,
        DRAG
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
    }
}
