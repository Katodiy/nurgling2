package nurgling.widgets;

import haven.*;

public class NZergwnd extends GameUI.Hidewnd
{
    Tabs tabs = new Tabs(Coord.z, Coord.z, this);
    public final NZergwnd.TButton kin;
    public final NZergwnd.TButton pol;
    final NZergwnd.TButton pol2;

    class TButton extends IButton {
        Tabs.Tab tab = null;
        final Tex inv;

        TButton(String nm, boolean g) {
            super("gfx/hud/buttons/" + nm, "u", "d", null);
            if(g)
                inv = Resource.loadtex("gfx/hud/buttons/" + nm + "g");
            else
                inv = null;
        }

        public void draw(GOut g) {
            if((tab == null) && (inv != null))
                g.image(inv, Coord.z);
            else
                super.draw(g);
        }

        public void click() {
            if(tab != null) {
                tabs.showtab(tab);
                repack();
            }
        }
    }

    public NZergwnd() {
        super(Coord.z, "Kith & Kin2", true);
        kin = add(new NZergwnd.TButton("kin", false));
        kin.tooltip = Text.render("Kin");
        pol = add(new NZergwnd.TButton("pol", true));
        pol2 = add(new NZergwnd.TButton("rlm", true));
    }

    private void repack() {
        tabs.indpack();
        kin.c = new Coord(0, tabs.curtab.contentsz().y + UI.scale(20));
        pol.c = new Coord(kin.c.x + kin.sz.x + UI.scale(10), kin.c.y);
        pol2.c = new Coord(pol.c.x + pol.sz.x + UI.scale(10), pol.c.y);
        this.pack();
    }

    public Tabs.Tab ntab(Widget ch, NZergwnd.TButton btn) {
        Tabs.Tab tab = add(tabs.new Tab() {
            public void cresize(Widget ch) {
                repack();
            }
        }, tabs.c);
        tab.add(ch, Coord.z);
        btn.tab = tab;
        repack();
        return(tab);
    }

    public void dtab(NZergwnd.TButton btn) {
        btn.tab.destroy();
        btn.tab = null;
        repack();
    }

    public void addpol(Polity p) {
        /* This isn't very nice. :( */
        NZergwnd.TButton btn = p.cap.equals("Village")?pol:pol2;
        ntab(p, btn);
        btn.tooltip = Text.render(p.cap);
    }
}