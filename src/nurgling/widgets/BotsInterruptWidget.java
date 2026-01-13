package nurgling.widgets;

import haven.*;
import nurgling.NInventory;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.actions.AutoDrink;
import nurgling.areas.NContext;
import nurgling.NConfig;
import nurgling.NCore;
import haven.res.ui.croster.Entry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;

public class BotsInterruptWidget extends Widget {
    boolean oldStackState = false;

    // Stack trace writing for autorunner debugging
    private static String autorunnerStackTraceFile = null;
    private static long lastStackTraceWrite = 0;
    private static final long STACK_TRACE_WRITE_INTERVAL = 2000; // 2 seconds


    public class Gear extends Widget
    {
        final Thread t;

        public IButton cancelb;

        public Gear(Thread t) {
            super();
            assert t!=null;
            this.t = t;
            cancelb = add(new IButton(NStyle.canceli[0].back,NStyle.canceli[1].back,NStyle.canceli[2].back){
                @Override
                public void click() {
                    removeObserve(t);
                }
            },new Coord(NStyle.gear[0].sz().x / 2 -NStyle.canceli[0].sz().x/2 , NStyle.gear[0].sz().y / 2 -NStyle.canceli[0].sz().y/2).add(UI.scale(1,-1)));
            sz = NStyle.gear[0].sz();
        }

        @Override
        public void tick(double dt) {
            super.tick(dt);
            StackTraceElement el = null;
            for(StackTraceElement e : t.getStackTrace())
            {
                if(e.toString().contains("actions."))
                {
                    el = e;
                    break;
                }
            };
            if(el!=null)
                cancelb.settip("Bot: " + t.getName() + " Op: " + el.toString());
        }

        @Override
        public void draw(GOut g) {
            int id = (int) (NUtils.getTickId() / 5) % 12;

            g.image(NStyle.gear[id], new Coord(sz.x / 2 - NStyle.gear[0].sz().x / 2, sz.y / 2 - NStyle.gear[0].sz().y / 2));
            super.draw(g);
        }
    }

    private void initializeStackTraceFile() {
        // Check if running under autorunner and stackTraceFile is configured
        if (NConfig.isBotMod() && NConfig.botmod != null && NConfig.botmod.stackTraceFile != null) {
            autorunnerStackTraceFile = NConfig.botmod.stackTraceFile;
            System.out.println("Autorunner mode detected: Stack trace file = " + autorunnerStackTraceFile);
        }
    }

    public BotsInterruptWidget() {
        sz = NStyle.gear[0].sz().mul(4.5);
        initializeStackTraceFile();
    }

    public void addObserve(Thread t)
    {
        if(obs.isEmpty())
        {
            NContext.waitBot.set(true);
        }

        if(obs.size()>=6)
            NUtils.getGameUI().error("Too many running bots!");
        else {
            obs.add(add(new Gear(t)));
        }
        repack();
    }

    public void addObserve(Thread t, boolean disStack)
    {
        if(disStack)
        {
            if(stackObs.isEmpty())
            {
                 if(((NInventory) NUtils.getGameUI().maininv).bundle.a) {
                     oldStackState = true;
                     NUtils.stackSwitch(false);
                 }
            }
            if(oldStackState)
                stackObs.add(t);
        }
        addObserve(t);
    }


    void repack()
    {
        double r = NStyle.gear[0].sz().x*1.5;
        double phi = -Math.PI/3;
        for(Gear g: obs)
        {
            g.move(new Coord((int)Math.round(r*Math.cos(phi))-NStyle.gear[0].sz().x/2 + sz.x/2, (int)Math.round(r*Math.sin(phi))-NStyle.gear[0].sz().y/2+ sz.y/2 + UI.scale(50)));
            phi+= Math.PI/3;
        }
    }


    public void removeObserve(Thread t)
    {
        t.interrupt();
        // Clear kill list highlight when bot stops
        Entry.killList.clear();
        synchronized (obs)
        {
            for(Gear g: obs)
            {
                if(g.t == t) {
                    if(stackObs.contains(g.t))
                    {
                        stackObs.remove(g.t);
                        if(stackObs.isEmpty() && oldStackState)
                        {
                            NUtils.stackSwitch(true);
                        }

                    }
                    g.remove();
                    obs.remove(g);
                    break;
                }
            }
        }
        repack();
        if(obs.isEmpty())
            NContext.waitBot.set(false);
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);

        // Only write stack traces if running under autorunner
        if (autorunnerStackTraceFile != null &&
            System.currentTimeMillis() - lastStackTraceWrite > STACK_TRACE_WRITE_INTERVAL) {
            writeCurrentStackTrace();
            lastStackTraceWrite = System.currentTimeMillis();
        }
        synchronized (obs)
        {
            for(Gear g: obs)
            {
                if(g.t.isInterrupted() || !g.t.isAlive())
                {
                    // Clear kill list highlight when bot stops
                    Entry.killList.clear();
                    if(stackObs.contains(g.t))
                    {
                        stackObs.remove(g.t);
                        if(stackObs.isEmpty() && oldStackState)
                        {
                            NUtils.stackSwitch(true);
                        }

                    }
                    g.remove();
                    obs.remove(g);
                    if(obs.isEmpty())
                        NContext.waitBot.set(false);
                    break;
                }
            }
        }
        repack();
    }

    private void writeCurrentStackTrace() {
        try {
            // Create JSON with current stack trace information
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"timestamp\": \"").append(Instant.now().toString()).append("\",\n");

            // Find current bot action (same logic as gear tooltip)
            String currentAction = null;
            String botName = null;

            synchronized (obs) {
                if (!obs.isEmpty()) {
                    Gear firstGear = obs.iterator().next();
                    botName = firstGear.t.getName();

                    for (StackTraceElement el : firstGear.t.getStackTrace()) {
                        if (el.toString().contains("actions.")) {
                            currentAction = el.toString();
                            break;
                        }
                    }
                }
            }

            json.append("  \"botName\": \"").append(botName != null ? botName : "Unknown").append("\",\n");
            json.append("  \"currentAction\": \"").append(currentAction != null ? currentAction.replace("\"", "\\\"") : "No action found").append("\",\n");
            json.append("  \"activeBotsCount\": ").append(obs.size()).append("\n");
            json.append("}");

            // Write atomically to temp file then rename
            Path tempFile = Paths.get(autorunnerStackTraceFile + ".tmp");
            Path finalFile = Paths.get(autorunnerStackTraceFile);

            Files.write(tempFile, json.toString().getBytes(), StandardOpenOption.CREATE);
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            // File I/O operations failed - log but don't crash
            System.err.println("Failed to write stack trace: " + e.getMessage());
        } catch (SecurityException e) {
            // File permission issue - log but don't crash
            System.err.println("Permission denied writing stack trace: " + e.getMessage());
        }
    }

    final ArrayList<Gear> obs = new ArrayList<>();
    final ArrayList<Thread> stackObs = new ArrayList<>();

//    @Override
//    public void draw(GOut g) {
//        Coord pcc = null;
//        Gob pl = NUtils.player();
//        if(pl != null && pl.placed.getc() !=null && NUtils.getGameUI().map.screenxf(pl.placed.getc()) != null) {
//            pcc = NUtils.getGameUI().map.screenxf(pl.placed.getc()).round2();
//        }
//        if(pcc!=null) {
//            Coord p1 = pcc.sub(sz.div(2));
//            for(Gear gear: obs)
//            {
//                gear.d
//            }
//
//            super.draw(g.reclip2(p1,p1.add(sz)));
//        }
//    }
}
