package nurgling.widgets;

import haven.*;
import nurgling.NInventory;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.actions.AutoDrink;

import java.util.ArrayList;

public class BotsInterruptWidget extends Widget {
    boolean oldStackState = false;
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



    public BotsInterruptWidget() {
        sz = NStyle.gear[0].sz().mul(4.5);
    }

    public void addObserve(Thread t)
    {
        if(obs.isEmpty())
        {
            AutoDrink.waitBot.set(true);
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
            AutoDrink.waitBot.set(false);
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        synchronized (obs)
        {
            for(Gear g: obs)
            {
                if(g.t.isInterrupted() || !g.t.isAlive())
                {
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
                        AutoDrink.waitBot.set(false);
                    break;
                }
            }
        }
        repack();
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
