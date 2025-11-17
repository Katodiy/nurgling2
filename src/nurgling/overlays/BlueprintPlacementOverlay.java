package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.NUtils;

import java.awt.Color;
import java.util.*;

public class BlueprintPlacementOverlay extends Sprite implements RenderTree.Node, PView.Render2D {
    private final Map<Coord, String> blueprintData;
    private Coord2d basePosition = Coord2d.z;
    private static final Tex treeTex = Resource.loadtex("nurgling/hud/point");

    public BlueprintPlacementOverlay(Owner owner, Map<Coord, String> blueprintData) {
        super(owner, null);
        this.blueprintData = new HashMap<>(blueprintData);
    }

    @Override
    public void draw(GOut g, Pipe state) {
        try {
            for (Map.Entry<Coord, String> entry : blueprintData.entrySet()) {
                Coord gridPos = entry.getKey();
                
                Coord2d treeWorldPos = basePosition.add(
                    gridPos.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                    gridPos.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
                );
                
                Coord3f treePos3d = new Coord3f((float)treeWorldPos.x, -(float)treeWorldPos.y, 0f);
                Coord sc = Homo3D.obj2view(treePos3d, state, Area.sized(Coord.z, g.sz())).round2();
                
                if (sc != null) {
                    g.chcolor(0, 255, 0, 180);
                    Coord offset = treeTex.sz().div(2);
                    g.image(treeTex, sc.sub(offset));
                }
            }
            g.chcolor();
        } catch (Exception e) {
            // Silent fail for rendering errors
        }
    }

    @Override
    public boolean tick(double dt) {
        return false;
    }

    public void updatePosition(Coord2d newPos) {
        this.basePosition = newPos;
    }

    public Coord2d getPosition() {
        return basePosition;
    }

    public Map<Coord, String> getBlueprintData() {
        return new HashMap<>(blueprintData);
    }

    public int getTreeCount() {
        return blueprintData.size();
    }
}
