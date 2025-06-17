package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.actions.bots.registry.BotDescriptor;
import nurgling.actions.bots.registry.BotRegistry;

import java.awt.image.BufferedImage;
import java.util.*;

public class NBotsMenu extends Widget
{
    final static String dir_path = "nurgling/bots/icons/";
    public NBotsMenu() {
        List<BotDescriptor.BotType> menuOrder = List.of(
                BotDescriptor.BotType.RESOURCES,
                BotDescriptor.BotType.PRODUCTIONS,
                BotDescriptor.BotType.BATTLE,
                BotDescriptor.BotType.FARMING,
                BotDescriptor.BotType.UTILS,
                BotDescriptor.BotType.BUILD,
                BotDescriptor.BotType.TOOLS
        );

        Map<BotDescriptor.BotType, NLayout> layouts = new LinkedHashMap<>();

        Map<BotDescriptor.BotType, String> layoutNames = Map.of(
                BotDescriptor.BotType.RESOURCES,   "resources",
                BotDescriptor.BotType.PRODUCTIONS, "productions",
                BotDescriptor.BotType.BATTLE,      "battle",
                BotDescriptor.BotType.FARMING,     "farming",
                BotDescriptor.BotType.UTILS,       "utils",
                BotDescriptor.BotType.BUILD,       "build",
                BotDescriptor.BotType.TOOLS,       "tools"
        );

        for (BotDescriptor.BotType type : menuOrder) {
            String layoutName = layoutNames.getOrDefault(type, type.name().toLowerCase());
            layouts.put(type, new NLayout(layoutName));
        }

        for (BotDescriptor bot : BotRegistry.allowedInBotMenu()) {
            BotDescriptor.BotType groupType = (bot.type == BotDescriptor.BotType.LIVESTOCK)
                    ? BotDescriptor.BotType.FARMING
                    : bot.type;
            NLayout layout = layouts.get(groupType);
            if (layout == null) continue;
            layout.elements.add(
                    new NButton(
                            bot.id,
                            bot.instantiate(Map.of()),
                            bot.disStacks
                    )
            );
        }

        for (NLayout layout : layouts.values()) {
            if (!layout.elements.isEmpty())
                addLayout(layout);
        }

        showLayouts();
        pack();
    }

    NButton dragging = null;
    @Override
    public void draw(GOut g, boolean strict) {
        super.draw(g, strict);
        if(dragging != null) {
            BufferedImage ds = dragging.btn.up;
            Coord dssz = new Coord(ds.getWidth(),ds.getHeight());
            ui.drawafter(new UI.AfterDraw() {
                public void draw(GOut g) {
                    g.reclip(ui.mc.sub(dssz.div(2)), dssz);
                    g.image(new TexI(ds), ui.mc );
                }
            });
        }
    }
    private NButton bhit(Coord c) {
        for(NLayout lay : layouts)
        {
            for(NButton b : lay.elements)
            {
                if(b.btn.visible())
                {
                    if(c.x <= b.btn.c.x + b.btn.sz.x && c.y <= b.btn.c.y + b.btn.sz.y && c.x >= b.btn.c.x && c.y >= b.btn.c.y)
                        return b;
                }
            }
        }
        return(null);
    }

    @Override
    public void mousemove(MouseMoveEvent ev) {
        if((dragging == null) && (pressed != null)) {
            NButton h = bhit(ev.c);
            if(h != pressed) {
                dragging = pressed;
                if(dragging.btn.d!=null)
                {
                    dragging.btn.d.remove();
                    dragging.btn.d=null;
                }
            }
        }
        super.mousemove(ev);
    }


    private NButton pressed = null;
    private UI.Grab grab = null;

    @Override
    public boolean mouseup(MouseUpEvent ev) {
        NButton h = bhit(ev.c);
        if((ev.b == 1) && (grab != null)) {
            if(dragging != null) {
                DropTarget.dropthing(ui.root, ui.mc, dragging);
                pressed = null;
                dragging = null;
            } else if(pressed != null) {
                if(pressed == h) {
                    pressed.btn.click();
                }
                pressed = null;
            }
            grab.remove();
            grab = null;
        }
        return super.mouseup(ev);
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        NButton h = bhit(ev.c);
        if((ev.b == 1) && (h != null)) {
            pressed = h;
            grab = ui.grabmouse(this);
        }
        boolean res = super.mousedown(ev);
        if(pressed!=null)
        {
            if(pressed.btn.d!=null)
            {
                pressed.btn.d.remove();
                pressed.btn.d=null;
            }
        }
        return res;
    }

    void addLayout(NLayout lay){
        int count = 0;
        for(NButton btn: lay.elements)
        {
            add(btn.btn, new Coord(0, (btn.btn.sz.y + UI.scale(2)) * count++));
            btn.btn.hide();
        }
        add(lay.btn);
        lay.btn.hide();
        layouts.add(lay);
    }

    public void showLayouts(){
        for(NLayout lay : layouts)
        {
            lay.hideElements();
        }
        int w = 0;
        int h = 0;
        for (NLayout lay : layouts)
        {
            lay.btn.move(new Coord(w * UI.scale(34), h * UI.scale(34)));
            lay.btn.show();
            if (h > 8)
            {
                w += 1;
                h = 0;
            }
            else
            {
                h += 1;
            }
        }
        if(parent!=null)
            parent.resize(new Coord((w + 1) * UI.scale(34), layouts.size() * UI.scale(34)).add(NDraggableWidget.delta));
    }

    public void hideLayouts(){
        for (NLayout lay : layouts)
        {
            lay.btn.hide();
        }
    }

    ArrayList<NLayout> layouts = new ArrayList<>();

    public NButton find(String path) {
        for (NLayout lay : layouts)
        {
            for(NButton element: lay.elements)
            {
                if(element.path!=null && element.path.equals(path))
                    return element;
            }
        }
        return null;
    }

    public class NButton
    {
        public final IButton btn;
        public String path;
        public boolean disStacks;
        NButton(String path, Action action) {
            this.path = path;
            Resource res = Resource.remote().load(dir_path + path + "/u").get();
            btn = new IButton(Resource.loadsimg(dir_path + path + "/u"), Resource.loadsimg(dir_path + path + "/d"), Resource.loadsimg(dir_path + path + "/h")) {
                TexI rtip;

                @Override
                public Object tooltip(Coord c, Widget prev) {
                    if (rtip == null && res.layers(Resource.Tooltip.class) != null) {
                        List<ItemInfo> info = new ArrayList<>();
                        int count = 0;
                        for (Resource.Tooltip tt : res.layers(Resource.Tooltip.class)) {
                            if(count == 0)
                            {
                                info.add(new ItemInfo.Tip(null) {
                                    @Override
                                    public BufferedImage tipimg() {
                                        return Text.render(tt.t).img;
                                    }
                                });
                            }
                            else {
                                info.add(new ItemInfo.Pagina(null, tt.t));
                            }
                            count++;
                        }
                        BufferedImage img = ItemInfo.longtip(info);
                        if(img!=null)
                            rtip = new TexI(img);
                    }
                    return (rtip);
                }

                @Override
                public boolean mouseup(MouseUpEvent ev) {
                    if((ev.b == 1) && (grab != null)) {
                        if(dragging != null) {
                            DropTarget.dropthing(ui.root, ui.mc, dragging);
                            pressed = null;
                            dragging = null;
                        } else if(pressed != null) {
                            pressed = null;
                        }
                        grab.remove();
                        grab = null;
                    }
                    return super.mouseup(ev);
                }
            }
                    .action(
                            new Runnable() {
                                @Override
                                public void run() {
                                    start(path, action);
                                }
                            });

        }

        NButton(String path, Action action, Boolean disStacks)
        {
            this(path, action);
            this.disStacks = disStacks;
        }

        private NButton()
        {
            btn = new IButton(Resource.loadsimg(dir_path + "back" + "/u"), Resource.loadsimg(dir_path +  "back" + "/d"), Resource.loadsimg(dir_path +  "back" + "/h")){
                @Override
                public void click() {
                    super.click();
                    showLayouts();
                }

                @Override
                public boolean mouseup(MouseUpEvent ev) {
                    if((ev.b == 1) && (grab != null)) {
                        if(dragging != null) {
                            pressed = null;
                            dragging = null;
                        } else if(pressed != null) {
                            pressed = null;
                        }
                        grab.remove();
                        grab = null;
                    }
                    return super.mouseup(ev);
                }
            };
       }

        void start(String path, Action action)
        {

            Thread t;
            t = new Thread(new Runnable()
            {
                ArrayList<Thread> supports = new ArrayList<>();
                @Override
                public void run()
                {
                    try
                    {
                        showLayouts();
                        NGameUI gui = NUtils.getGameUI();
                        if(gui!=null) {
                            for (Action sup : action.getSupp()) {
                                Thread st;
                                supports.add(st = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            sup.run(gui);
                                        } catch (InterruptedException e) {
                                        }
                                    }
                                }));
                                st.start();
                            }
                            action.run(gui);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        NUtils.getGameUI().msg(path + ":" + "STOPPED");
                    }
                    finally
                    {
                        if(action instanceof ActionWithFinal)
                        {
                            ((ActionWithFinal)action).endAction();
                        }
                        for(Thread st: supports)
                        {
                            st.interrupt();
                        }
                    }
                }
            }, path);
            if(disStacks)
                NUtils.getGameUI().biw.addObserve(t, true);
            else
                NUtils.getGameUI().biw.addObserve(t);
            t.start();
        }


    };

    class NLayout
    {
        public final IButton btn;

        ArrayList<NButton> elements = new ArrayList<>();

        public NLayout(String path)
        {
            this.btn = new IButton(Resource.loadsimg(dir_path + path + "/u"),Resource.loadsimg(dir_path + path + "/d"),Resource.loadsimg(dir_path + path + "/h")).action(new Runnable()
            {
                @Override
                public void run()
                {
                    hideLayouts();
                    showElements();
                }
            });
            elements.add(new NButton());
        }

        void hideElements()
        {
            for (NButton element : elements)
            {
                element.btn.hide();
            }
        }

        void showElements()
        {
            int w = 0;
            int h = 0;
            for (NButton element : elements)
            {
                element.btn.move(new Coord(w * UI.scale(34), h * UI.scale(34)));
                if (h > 7)
                {
                    w += 1;
                    h = 0;
                }
                else
                {
                    h += 1;
                }
                element.btn.show();
            }
            parent.resize(new Coord((w + 1) * UI.scale(34), (w > 0 ? 9 : h + 1) * UI.scale(34)).add(NDraggableWidget.delta));
        }
    };
}
