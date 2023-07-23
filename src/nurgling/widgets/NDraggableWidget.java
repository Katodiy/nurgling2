package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.conf.*;

public class NDraggableWidget extends Widget
{
    protected final String name;
    private UI.Grab dm;
    private Coord doff;
    protected ICheckBox btnLock;
    public static final IBox box = Window.wbox;

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
                    Coord2d kff = new Coord2d(c.x / (double) GameUI.getInstance().sz.x, c.y / (double) GameUI.getInstance().sz.y);
                    NDragProp.set(name, new NDragProp( NDraggableWidget.this.c , val, name, kff));
                }
                else
                {
                    NDragProp.set(name, new NDragProp(NDraggableWidget.this.c, val, name));
                }
            }
        }, new Coord(sz.x - NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y / 2));
        btnLock.hide();
        this.sz = sz.add(new Coord(NStyle.locki[0].sz().x, 0));
        NDragProp prop = NDragProp.get(name);
        if (prop.c != Coord.z)
        {
            if(GameUI.getInstance()!=null && GameUI.getInstance().sz!=Coord.z)
            {
                this.c = GameUI.getInstance().sz.mul(prop.pos_koef).round();
            }
            else
            {
                this.c = prop.c;
            }
            this.btnLock.a = prop.locked;
        }
    }

    @Override
    public void resize(Coord sz)
    {
        Coord nsz = sz.add(new Coord(NStyle.locki[0].sz().x, 0));
        super.resize(nsz);
        btnLock.move(new Coord(nsz.x - NStyle.locki[0].sz().x - NStyle.locki[0].sz().x / 2, NStyle.locki[0].sz().y / 2));
    }

    public static final Tex bg = Resource.loadtex("nurgling/hud/wnd/bg");
    private static final Tex ctl = Resource.loadtex("nurgling/hud/box/tl");

    @Override
    public void draw(GOut g)
    {
        if (ui.core.mode == NCore.Mode.DRAG)
        {
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
            box.draw(g, Coord.z, sz);
        }
        super.draw(g);
    }

    @Override
    public boolean mousedown(Coord c, int button)
    {
        if (ui.core.mode == NCore.Mode.DRAG)
        {
            Coord cc = xlate(btnLock.c, true);
            if (!btnLock.mousedown(c.add(cc.inv()), button))
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
            dm.remove();
            dm = null;
            if(NDraggableWidget.this.parent instanceof GameUI)
            {
                Coord2d kff = new Coord2d( (NDraggableWidget.this.c.x / (double) GameUI.getInstance().sz.x),  (NDraggableWidget.this.c.y / (double) GameUI.getInstance().sz.y));
                NDragProp.set(name, new NDragProp( NDraggableWidget.this.c , btnLock.a, name, kff));
            }
            else
            {
                NDragProp.set(name, new NDragProp(NDraggableWidget.this.c, btnLock.a, name));
            }
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
                    Coord cc = xlate(btnLock.c, true);
                    btnLock.mousemove(c.add(cc.inv()));
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
        }
        else
        {
            if (btnLock.visible())
                btnLock.hide();
        }
    }

    public String getName()
    {
        return name;
    }
}
