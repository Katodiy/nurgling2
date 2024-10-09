package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.conf.*;

public class NDraggableWidget extends Widget
{
    protected final String name;
    private UI.Grab dm;
    private Coord doff;
    public Coord target_c;
    protected ICheckBox btnLock;
    protected ICheckBox btnVis;
    private boolean isFlipped = false;
    protected ICheckBox btnFlip;
    public static final IBox box = Window.wbox;

    public final static Coord off = new Coord(UI.scale(10,10));
    public final static Coord delta = new Coord(UI.scale(35,20));
    public Widget content = null;

    public NDraggableWidget(Widget content, String name, Coord sz)
    {
        this(name,sz);
        this.content = add(content);
        this.content.visible = btnVis.a;
        content.resize(this.sz.sub(delta));
        content.move(off);
    }

    public NDraggableWidget(String name, Coord sz)
    {
        this.sz = sz;
        this.name = name;
        add(btnLock = new ICheckBox(NStyle.locki[0], NStyle.locki[1], NStyle.locki[2], NStyle.locki[3])
        {
            @Override
            public void changed(boolean val)
            {
                super.changed(val);
                if(NDraggableWidget.this.parent instanceof GameUI)
                {
                    NDragProp prop = new NDragProp(NDraggableWidget.this.c, val, btnVis.a, name);
                    prop.flip = btnFlip.a;
                    NDragProp.set(name, prop);
                }
            }
        }, new Coord(sz.x - NStyle.locki[0].sz().x - NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y / 2));

        add(btnVis = new ICheckBox(NStyle.visi[0], NStyle.visi[1], NStyle.visi[2], NStyle.visi[3])
        {
            @Override
            public void changed(boolean val)
            {
                super.changed(val);
                content.visible = val;
                if(NDraggableWidget.this.parent instanceof GameUI)
                {
                    NDragProp prop = new NDragProp(NDraggableWidget.this.c, btnLock.a, val, name);
                    prop.flip = btnFlip.a;
                    NDragProp.set(name, prop);
                }
            }
        }, new Coord(sz.x - NStyle.locki[0].sz().x - NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y + off.y));

        add(btnFlip = new ICheckBox(NStyle.flipi[0], NStyle.flipi[1], NStyle.flipi[2], NStyle.flipi[3])
        {
            @Override
            public void changed(boolean val)
            {
                super.changed(val);
                if(content!=null)
                {
                    flipContent();
                }
                if(NDraggableWidget.this.parent instanceof GameUI)
                {
                    NDragProp prop = new NDragProp(NDraggableWidget.this.c, btnLock.a, btnVis.a, name);
                    prop.flip = val;
                    NDragProp.set(name, prop);
                }
            }
        }, new Coord(NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y/2));

        btnVis.hide();
        btnLock.hide();
        btnFlip.hide();
//        this.sz = sz.add(new Coord(NStyle.locki[0].sz().x, 0));
        NDragProp prop = NDragProp.get(name);
        if (prop.c != Coord.z)
        {
            this.c = new Coord(prop.c);
            this.target_c = prop.c;
            this.btnLock.a = prop.locked;
            this.btnVis.a = prop.vis;
            this.btnFlip.a = prop.flip;
        }
        else
        {
            this.target_c = new Coord(Coord.z);
            this.btnVis.a = true;
        }
    }



    @Override
    public void resize(Coord sz)
    {
        super.resize(sz);
        btnLock.move(new Coord(sz.x - NStyle.locki[0].sz().x - NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y / 2));
        btnVis.move(new Coord(sz.x - NStyle.locki[0].sz().x - NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y + off.y));
        if(isFlipped)
            btnFlip.move(new Coord(NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y/2));
        if(content!=null)
        {
            content.resize(sz.sub(delta));
            content.move(off);
        }
    }

    public static final Tex bg = Resource.loadtex("nurgling/hud/wnd/bg");
    private static final Tex ctl = Resource.loadtex("nurgling/hud/box/tl");

    @Override
    public void draw(GOut g)
    {
        if (ui.core.mode == NCore.Mode.DRAG)
        {
            drawBg(g,sz);
            box.draw(g, Coord.z, sz);
        }
        super.draw(g);
    }

    public static void drawBg(GOut g, Coord sz) {
        int x_pos = ctl.sz().x;
        int y_pos = ctl.sz().y;
        for (int x = ctl.sz().x / 2; x + bg.sz().x < sz.x - ctl.sz().x / 2; x += bg.sz().x)
        {
            for (int y = ctl.sz().y / 2; y + bg.sz().y < sz.y - ctl.sz().y / 2; y += bg.sz().y)
            {
                g.image(bg, new Coord(x, y));
                y_pos = Math.max(y_pos, y + bg.sz().y);
                x_pos = Math.max(x_pos, x + bg.sz().x);
            }
        }
        for (int x = ctl.sz().x / 2; x + bg.sz().x < sz.x - ctl.sz().x / 2; x += bg.sz().x)
        {
            g.image(bg, new Coord(x, y_pos), new Coord(bg.sz().x, sz.y - y_pos - ctl.sz().y / 2));
            x_pos = Math.max(x_pos, x + bg.sz().x);
        }
        for (int y = ctl.sz().y / 2; y + bg.sz().y < sz.y - ctl.sz().y / 2; y += bg.sz().y)
        {
            g.image(bg, new Coord(x_pos, y), new Coord(sz.x - x_pos - ctl.sz().x / 2, bg.sz().y));
            y_pos = Math.max(y_pos, y + bg.sz().y);
        }
        if (x_pos < sz.x - ctl.sz().x / 2 && y_pos < sz.y - ctl.sz().y / 2)
        {
            g.image(bg, new Coord(x_pos, y_pos), new Coord(sz.x - x_pos - ctl.sz().x / 2, sz.y - y_pos - ctl.sz().y / 2));
        }
    }

    @Override
    public boolean mousedown(Coord c, int button)
    {
        if (ui.core.mode == NCore.Mode.DRAG)
        {
            if (    !btnLock.mousedown(c.add(xlate(btnLock.c, true).inv()), button) &&
                    !btnVis.mousedown(c.add(xlate(btnVis.c, true).inv()), button) &&
                    !btnFlip.mousedown(c.add(xlate(btnFlip.c, true).inv()), button))
            {
                if (c.isect(Coord.z, sz))
                    if (ui.mousegrab.isEmpty())
                    {
                        if (!btnLock.a)
                        {
                            if (button == 1)
                            {
                                dm = ui.grabmouse(this);
                                doff = c;
                            }
                        }
                    }
                    else
                    {
                        if (button == 1)
                        {
                            dm = ui.grabmouse(this);
                            doff = c;
                        }
                        parent.setfocus(this);
                    }
            }
            return true;
        }
        else
        {
            return super.mousedown(c, button);
        }
    }

    @Override
    public boolean mouseup(Coord c, int button)
    {
        if (dm != null && ui.core.mode == NCore.Mode.DRAG)
        {
            NDragProp res = new NDragProp(NDraggableWidget.this.c, btnLock.a, name);
            res.flip = btnFlip.a;
            NDragProp.set(name, res);
            target_c.x = this.c.x;
            target_c.y = this.c.y;
            dm.remove();
            dm = null;
            return true;
        }
        else
        {
            return super.mouseup(c, button);
        }
    }

    @Override
    public void mousemove(Coord c)
    {
        if (ui.core.mode == NCore.Mode.DRAG)
        {

            if (dm != null)
            {
                Coord prepc = this.c.add(c.add(doff.inv()));
                this.c = prepc.div(UI.scale(8)).mul(UI.scale(8)).sub(UI.scale(4),UI.scale(4));
            }
            else
            {
                if (c.isect(Coord.z, sz))
                {
                    btnLock.mousemove(c.add(xlate(btnLock.c, true).inv()));
                    btnVis.mousemove(c.add(xlate(btnVis.c, true).inv()));
                    if(isFlipped)
                        btnFlip.mousemove(c.add(xlate(btnFlip.c, true).inv()));
                }
            }
        }
        else
        {
            super.mousemove(c);
        }
    }


    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        if (ui.core.mode == NCore.Mode.DRAG)
        {
            btnLock.show();
            btnVis.show();
            if( isFlipped )
                btnFlip.show();
        }
        else
        {
            if (btnLock.visible())
            {
                btnLock.hide();
                btnVis.hide();
                btnFlip.hide();
            }
        }

        if(NUtils.getGameUI()!=null && NUtils.getGameUI().sz!=Coord.z && dm == null)
        {
            if (c.x + sz.x > NUtils.getGameUI().sz.x - GameUI.margin.x)
                c.x = NUtils.getGameUI().sz.x - sz.x;
            else
                c.x = target_c.x;
            if (c.y + sz.y > NUtils.getGameUI().sz.y - GameUI.margin.y)
                c.y = NUtils.getGameUI().sz.y - sz.y;
            else
                c.y = target_c.y;
        }
    }

    public String getName()
    {
        return name;
    }

    public void flipContent()
    {
        content.flip(btnFlip.a);
        resize(content.sz.add(delta));
    }

    public void setFlipped(boolean val)
    {
        isFlipped = val;
        flipContent();
    }
}
