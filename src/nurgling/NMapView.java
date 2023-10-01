package nurgling;

import haven.*;
import static haven.MCache.tilesz;

import java.awt.image.*;
import java.util.*;

public class NMapView extends MapView
{
    public NMapView(Coord sz, Glob glob, Coord2d cc, long plgob)
    {
        super(sz, glob, cc, plgob);
    }

    final HashMap<String, String> ttip = new HashMap<>();

    public Object tooltip(Coord c, Widget prev) {
        if (!ttip.isEmpty() && NUtils.getGameUI().ui.core.isInspectMode()) {

            Collection<BufferedImage> imgs = new LinkedList<BufferedImage>();
            if (ttip.get("gob") != null) {
                BufferedImage gob = RichText.render(String.format("$col[128,128,255]{%s}:", "Gob"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("gob"), 0).img);
            }
            if (ttip.get("rc") != null) {
                BufferedImage gob = RichText.render(String.format("$col[128,128,128]{%s}:", "Coord"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("rc"), 0).img);
            }
            if (ttip.get("id") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,255]{%s}:", "id"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("id"), 0).img);
            }
            if (ttip.get("tile") != null) {
                BufferedImage tile = RichText.render(String.format("$col[128,128,255]{%s}:", "Tile"), 0).img;
                imgs.add(tile);
                imgs.add(RichText.render(ttip.get("tile"), 0).img);
            }
            if (ttip.get("tags") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,128]{%s}:", "Tags"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("tags"), 0).img);
            }
            if (ttip.get("status") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,128]{%s}:", "Status"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("status"), 0).img);
            }
            if (ttip.get("HitBox") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,255]{%s}:", "HitBox"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("HitBox"), 0).img);
            }
            if (ttip.get("marker") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,83,83]{%s}:", "Marker"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("marker"), 0).img);
            }
            if (ttip.get("cont") != null) {
                BufferedImage gob = RichText.render(String.format("$col[83,255,83]{%s}:", "Container"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("cont"), 0).img);
            }
            if (ttip.get("ols") != null) {
                BufferedImage gob = RichText.render(String.format("$col[83,255,155]{%s}:", "Overlays"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("ols"), 0).img);
            }
            if (ttip.get("poses") != null) {
                BufferedImage gob = RichText.render(String.format("$col[255,128,128]{%s}:", "Poses"), 0).img;
                imgs.add(gob);
                imgs.add(RichText.render(ttip.get("poses"), 0).img);
            }
            return new TexI((ItemInfo.catimgs(0, imgs.toArray(new BufferedImage[0]))));
        }
        return (super.tooltip(c, prev));
    }

    void inspect(Coord c) {
        new Hittest(c) {
            @Override
            protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                ttip.clear();
                if (inf != null) {
                    Gob gob = Gob.from(inf.ci);
                    if (gob != null) {
                        ttip.put("gob", gob.ngob.name);
                        if(gob.ngob.hitBox!=null)
                            ttip.put("HitBox", gob.ngob.hitBox.toString());
                        ttip.put("rc" , gob.rc.toString());
                        if(!gob.ols.isEmpty()) {
                            StringBuilder ols = new StringBuilder();
                            boolean isPrinted = false;
                            for (Gob.Overlay ol : gob.ols) {
                                if (ol.spr != null) {
                                    isPrinted = true;
                                    String res = ol.spr.getClass().toString();
                                    if(!res.contains("$"))
                                        ols.append(res + " ");
                                }
                            }
                            if(isPrinted)
                                ttip.put("ols", ols.toString());
                        }


                        ttip.put("id", String.valueOf(gob.id));

                        if (gob.ngob.getModelAttribute()!=-1) {
                            ttip.put("marker", String.valueOf(gob.ngob.getModelAttribute()));
                        }

//                        if(gob.getattr(Drawable.class)!=null && gob.getattr(Drawable.class) instanceof Composite && ((Composite)gob.getattr(Drawable.class)).oldposes!=null)
//                        {
//                            StringBuilder poses = new StringBuilder();
//                            Iterator<ResData> pose = ((Composite)gob.getattr(Drawable.class)).oldposes.iterator();
//                            while (pose.hasNext()) {
//                                poses.append(pose.next().res.get().name);
//                                if (pose.hasNext())
//                                    poses.append(", ");
//                            }
//                            ttip.put("poses", poses.toString());
//                        }

                    }
                }
                MCache mCache = ui.sess.glob.map;
                int tile = mCache.gettile(mc.div(tilesz).floor());
                Resource res = mCache.tilesetr(tile);
                if (res != null) {
                    ttip.put("tile", res.name);
                }
            }

            @Override
            protected void nohit(Coord pc) {
                ttip.clear();
            }
        }.run();
    }

}
