package mapv4;

import haven.*;
import haven.resutil.Ridges;
import nurgling.NUtils;
import nurgling.tasks.CheckGrid;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author APXEOLOG (Artyom Melnikov), at 28.01.2019
 */
public class MinimapImageGenerator {

    private static BufferedImage tileimg(int t, BufferedImage[] texes, MCache map) {
        BufferedImage img = texes[t];
        if (img == null) {
            Resource r = map.tilesetr(t);
            if (r == null)
                return (null);
            Resource.Image ir = r.layer(Resource.imgc);
            if (ir == null)
                return (null);
            img = ir.img;
            texes[t] = img;
        }
        return (img);
    }

    public static BufferedImage drawmap(MCache map, MCache.Grid grid) throws InterruptedException {
        BufferedImage[] texes = new BufferedImage[256];
        BufferedImage buf = TexI.mkbuf(MCache.cmaps);
        Coord c = new Coord();

        for (c.y = 0; c.y < MCache.cmaps.y; c.y++) {
            for (c.x = 0; c.x < MCache.cmaps.x; c.x++) {
                BufferedImage tex = tileimg(grid.gettile(c), texes, map);
                int rgb = 0;
                if (tex != null)
                    rgb = tex.getRGB(Utils.floormod(c.x, tex.getWidth()),
                            Utils.floormod(c.y, tex.getHeight()));
                buf.setRGB(c.x, c.y, rgb);
            }
        }

        for (c.y = 1; c.y < MCache.cmaps.y - 1; c.y++) {
            for (c.x = 1; c.x < MCache.cmaps.x - 1; c.x++) {
                int t = grid.gettile(c);
                Tiler tl = map.tiler(t);
                if (tl instanceof Ridges.RidgeTile) {
                    Coord rc = grid.ul.add(c);
                    Coord current = rc.div(MCache.cmaps);
                    CheckGrid cg = new CheckGrid(current);
                    NUtils.addTask(cg);
                    if(cg.status())
                        throw new InterruptedException();
                    if (Ridges.brokenp(map, rc)) {
                        for (int y = c.y - 1; y <= c.y + 1; y++) {
                            for (int x = c.x - 1; x <= c.x + 1; x++) {
                                Color cc = new Color(buf.getRGB(x, y));
                                buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y)) ? 1 : 0.1).getRGB());
                            }
                        }
                    }
                }
            }
        }

        for (c.y = 1; c.y < MCache.cmaps.y - 1; c.y++) {
            for (c.x = 1; c.x < MCache.cmaps.x - 1; c.x++) {
                int t = grid.gettile(c);
                Coord r = c.add(grid.ul);

                for(Coord test: tested) {
                    Coord rc = r.add(test);
                    Coord current = rc.div(MCache.cmaps);
                    CheckGrid cg = new CheckGrid(current);
                    NUtils.addTask(cg);
                    if (cg.status())
                        throw new InterruptedException();
                }
                NUtils.addTask(new CheckGrid(r.add(-1, 0).div(MCache.cmaps)));
                NUtils.addTask(new CheckGrid(r.add(1, 0).div(MCache.cmaps)));
                NUtils.addTask(new CheckGrid(r.add(0, -1).div(MCache.cmaps)));
                NUtils.addTask(new CheckGrid(r.add(0, 1).div(MCache.cmaps)));
                if ((map.gettile(r.add(-1, 0)) > t) ||
                        (map.gettile(r.add(1, 0)) > t) ||
                        (map.gettile(r.add(0, -1)) > t) ||
                        (map.gettile(r.add(0, 1)) > t)) {
                    buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
                }
            }
        }
        return buf;
    }

    static private ArrayList<Coord> tested = new ArrayList<>(Arrays.asList(new Coord[]{new Coord(0,1),new Coord(1,0),new Coord(0,-1),new Coord(-1,0)}));
}