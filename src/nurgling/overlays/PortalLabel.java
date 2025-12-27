package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import nurgling.NUtils;
import nurgling.navigation.ChunkNavData;
import nurgling.navigation.ChunkPortal;

/**
 * Simple overlay to visualize ChunkNav portals in-game as colored dots.
 * Green = has connection, Red = no connection yet.
 */
public class PortalLabel extends Sprite implements RenderTree.Node, PView.Render2D {
    public final Tex tex = Resource.loadtex("nurgling/hud/point");
    public final ChunkPortal portal;
    public final ChunkNavData chunk;
    Coord sc;

    public PortalLabel(Owner owner, ChunkNavData chunk, ChunkPortal portal) {
        super(owner, null);
        this.chunk = chunk;
        this.portal = portal;
    }

    @Override
    public void draw(GOut g, Pipe state) {
        sc = Homo3D.obj2view(Coord3f.o, state, Area.sized(Coord.z, g.sz())).round2();
        if (sc == null)
            return;

        Coord c = tex.sz().inv();
        c.x = c.x / 2;

        // Color based on whether portal has a connection
        if (portal.connectsToGridId != -1) {
            g.chcolor(128, 255, 128, 255); // Green = has connection
        } else {
            g.chcolor(255, 128, 128, 255); // Red = no connection
        }
        g.image(tex, sc.add(c));
        g.chcolor();
    }
}
