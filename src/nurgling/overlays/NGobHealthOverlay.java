package nurgling.overlays;

import haven.*;

import java.awt.*;
import java.awt.image.*;

public class NGobHealthOverlay extends NObjectTexLabel
{
    static final TexI lvl25 = new TexI(Resource.loadimg("marks/brokenh"));
    static final TexI lvl50 = new TexI(Resource.loadimg("marks/brokenm"));
    static final TexI lvl75 = new TexI(Resource.loadimg("marks/brokens"));

    static TexI lvl25t(){
        if(lvl25t==null)
        {
            synchronized (lvl25)
            {
                lvl25t = init(0.25f,lvl25);
            }
        }
        return lvl25t;
    }

    static TexI lvl50t(){
        if(lvl50t==null)
        {
            synchronized (lvl50)
            {
                lvl50t = init(0.50f,lvl50);
            }
        }
        return lvl50t;
    }

    static TexI lvl75t(){
        if(lvl75t==null)
        {
            synchronized (lvl75)
            {
                lvl75t = init(0.75f,lvl75);
            }
        }
        return lvl75t;
    }

    static TexI lvl25t = null;
    static TexI lvl50t = null;
    static TexI lvl75t = null;

    public static final Font bsans  = new Font("Sans", Font.BOLD, 10);
    private static final Text.Furnace active_title = new PUtils.BlurFurn(new Text.Foundry(bsans, 15, Color.WHITE).aa(true), 2, 1, new Color(36, 25, 25));
    static TexI init(float lvl ,TexI img)
    {
        String value = String.format("%.0f",lvl*100)+"%";
        BufferedImage retlabel = active_title.render(value).img;
        BufferedImage ret = TexI.mkbuf(new Coord(UI.scale(1)+img.sz().x+retlabel.getWidth(), Math.max(img.sz().y,retlabel.getHeight())));
        Graphics g = ret.getGraphics();
        g.drawImage(img.back, 0, ret.getHeight()/2-img.sz().y/2, null);
        g.drawImage(retlabel,UI.scale(1)+img.sz().x,ret.getHeight()/2-retlabel.getHeight()/2,null);
        g.dispose();
        return new TexI(ret);
    }

    Gob gob;
    public NGobHealthOverlay(Owner owner)
    {
        super(owner);
        gob = (Gob) owner;
    }

    @Override
    public boolean tick(double dt)
    {
        GobHealth gh = gob.getattr(GobHealth.class);
        if(gh.hp<1)
        {
            if(gh.hp<=0.25f)
            {
                img = lvl25;
                label = lvl25t();
            }
            else if(gh.hp<=0.5f)
            {
                img = lvl50;
                label = lvl50t();
            }
            else
            {
                img = lvl75;
                label = lvl75t();
            }
        }
        else
        {
            img = null;
            label = null;
        }
        return super.tick(dt);
    }
}
