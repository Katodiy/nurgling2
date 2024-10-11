package nurgling.widgets.bots;

import haven.*;
import haven.Frame;
import haven.Label;
import haven.res.lib.itemtex.ItemTex;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.widgets.NMakewindow;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static haven.Inventory.invsq;

public class UsingTools extends Widget {
    public static class Tools {
        public static ArrayList<Tool> axes = new ArrayList<>();
        public static ArrayList<Tool> pickaxe = new ArrayList<>();
        public static ArrayList<Tool> shovels = new ArrayList<>();
        public static ArrayList<Tool> saw= new ArrayList<>();

        static {
            axes.add(new Tool("gfx/invobjs/woodsmansaxe", "Woodsman's Axe", "gfx/invobjs/small/woodsmansaxe"));
            axes.add(new Tool("gfx/invobjs/stoneaxe", "Stone Axe"));
            axes.add(new Tool("gfx/invobjs/butcherscleaver", "Butcher's cleaver"));
            axes.add(new Tool("gfx/invobjs/axe-m", "Metal Axe"));
            axes.add(new Tool("gfx/invobjs/tinkersthrowingaxe", "Tinker's Axe"));
            axes.add(new Tool("gfx/invobjs/b12axe", "Battle Axe of the Twelfth Bay", "gfx/invobjs/small/b12axe"));
            shovels.add(new Tool("gfx/invobjs/small/shovel-w", "Wooden Shovel"));
            shovels.add(new Tool("gfx/invobjs/small/shovel-m", "Metal Shovel"));
            shovels.add(new Tool("gfx/invobjs/small/shovel-t", "Tinker's Shovel"));
            saw.add(new Tool("gfx/invobjs/small/bonesaw", "Bonesaw"));
            saw.add(new Tool("gfx/invobjs/small/saw-m", "Metal Saw"));

            pickaxe.add(new Tool("gfx/invobjs/woodsmansaxe", "Woodsman's Axe", "gfx/invobjs/small/woodsmansaxe"));
            pickaxe.add(new Tool("gfx/invobjs/stoneaxe", "Stone Axe"));
            pickaxe.add(new Tool("gfx/invobjs/butcherscleaver", "Butcher's cleaver"));
            pickaxe.add(new Tool("gfx/invobjs/axe-m", "Metal Axe"));
            pickaxe.add(new Tool("gfx/invobjs/tinkersthrowingaxe", "Tinker's Axe"));
            pickaxe.add(new Tool("gfx/invobjs/b12axe", "Battle Axe of the Twelfth Bay", "gfx/invobjs/small/b12axe"));
            pickaxe.add(new Tool("gfx/invobjs/pickaxe", "Pickaxe", "gfx/invobjs/small/pickaxe"));
        }
    }

    public static final TexI frame = new TexI(Resource.loadimg("nurgling/hud/iconframe"));
    public UsingTools(ArrayList<Tool> tools, boolean showLabel) {
        this.tools = tools;
        prev = add(l = new Label("Tool:"));
        if(!showLabel)
            l.hide();
        sz = prev.sz.add(0,Inventory.sqsz.y).add(UI.scale(0,5));
        sz.x = Math.max(Inventory.sqsz.x,sz.x);
    }

    public UsingTools(ArrayList<Tool> tools) {
        this(tools,true);
    }

    final ArrayList<Tool> tools;
    Label l;
    @Override
    public void draw(GOut g) {
        super.draw(g);
        g.image(frame, new Coord(0,l.sz.y+UI.scale(5)), UI.scale(32, 32));
        if(s!=null)
            g.image(s.img, new Coord(0,l.sz.y+UI.scale(5)), UI.scale(32, 32));
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if(c.isect(new Coord(0,l.sz.y+UI.scale(5)),new Coord(sz)))
        {
            avTools = NUtils.getUI().root.add(new AvTools(tools));
            Coord pos = new Coord(0,l.sz.y+UI.scale(5));
            Widget par = this;
            while (par!=null && par!=NUtils.getGameUI()) {
                pos = pos.add(par.c);
                par = par.parent;
            }
            avTools.move(pos.add(UI.scale(22,62)));
            return true;
        }
        else
        {
            return super.mousedown(c, button);
        }
    }

    public static class Tool{
        TexI img;
        public String name;
        String path;

        public Tool(String path, String name)
        {
            this.img = new TexI(Resource.remote().loadwait(path).layer(Resource.imgc).scaled());
            this.path = path;
            this.name = name;
        }

        public Tool(String path, String name, String apath)
        {
            this.img = new TexI(Resource.remote().loadwait(apath).layer(Resource.imgc).scaled());
            this.name = name;
            this.path = path;
        }
    }

    AvTools avTools = null;
    Tool s = null;

    public class AvTools extends Widget
    {
        Color bg = new Color(30,40,40,160);
        ArrayList<Tool> data;
        haven.Frame fr;
        final Coord avtoff = UI.scale(8,8);
        final Coord avtend = UI.scale(15,15);
        public AvTools(ArrayList<Tool> data)
        {
            super(new Coord(Math.max((Inventory.sqsz.x+ UI.scale(1))*((data.size()/6>=1)?6:0),(Inventory.sqsz.x+UI.scale(1))*(data.size()%6))- UI.scale(2),(Inventory.sqsz.x+UI.scale(1))*(data.size()/6+(data.size()%6!=0?1:0))).add(UI.scale(20,18)));
            this.data = data;
            add(fr = new Frame(sz.sub(avtend),true));
        }

        @Override
        public void draw(GOut g)
        {
            g.chcolor(bg);
            g.frect(UI.scale(4,4), fr.inner());
            Coord pos = new Coord(avtoff);
            Coord shift = new Coord(0,0);
            for(Tool ing: data)
            {
                GOut sg = g.reclip(pos, invsq.sz());
                sg.image(ing.img, Coord.z, UI.scale(Inventory.sqsz));
                if(shift.x<5)
                {
                    pos = pos.add(Inventory.sqsz.x + UI.scale(1), 0);
                    shift.x+=1;
                }
                else
                {
                    pos.x = UI.scale(8);
                    pos = pos.add(0, Inventory.sqsz.y + UI.scale(1));
                }
            }
            super.draw(g);
        }
        UI.Grab mg;
        @Override
        protected void added()
        {
            mg = NUtils.getUI().grabmouse(this);
        }

        @Override
        public void remove()
        {
            mg.remove();
            super.remove();
        }

        @Override
        public boolean mousedown(Coord c, int button)
        {
            Coord pos = new Coord(avtoff);
            if(!c.isect(pos, sz.sub(avtend)))
            {
                destroy();
                avTools = null;
                return false;
            }
            else
            {
                Coord shift = new Coord(0,0);
                for(Tool ing: data)
                {
                    if(c.isect(pos, invsq.sz()))
                    {
                        s = ing;
                        destroy();
                        avTools = null;
                        return true;
                    }
                    if(shift.x<5)
                    {
                        pos = pos.add(Inventory.sqsz.x + UI.scale(1), 0);
                        shift.x+=1;
                    }
                    else
                    {
                        pos.x = UI.scale(8);
                        pos = pos.add(0, Inventory.sqsz.y + UI.scale(1));
                    }
                }
                return true;
            }
        }
    }
}
