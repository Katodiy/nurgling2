package nurgling.widgets;

import haven.*;
import haven.render.*;
import haven.res.ui.tt.q.quality.Quality;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.tools.DirectionalVector;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static haven.MCache.tilesz;
import static java.lang.Math.*;

public class NProspecting extends Window {
    private static final Queue<Dowse> EFFECTS = new ConcurrentLinkedQueue<>();
    public static final Queue<NProspecting> WINDOWS = new ConcurrentLinkedQueue<>();
    public static final Queue<Quality> QUALITIES = new ConcurrentLinkedQueue<>();
    private static final Pattern detect = Pattern.compile("There appears to be (.*) directly below.");
    private RenderTree.Slot slot;
    private Coord2d pc;
    private String detected;
    private final Button mark;

    public NProspecting(Coord sz, String cap) {
        super(sz, cap);
        mark = add(new Button(UI.scale(100), "Mark", false), UI.scale(105, 25));
        mark.action(this::mark);
        mark.hide();
    }

    @Override
    public void pack() {
        mark.c.y = children(Button.class).stream().filter(button -> button != mark).findFirst().orElse(mark).c.y;
        super.pack();
    }

    @Override
    public void destroy() {
        synchronized (WINDOWS) {WINDOWS.remove(this);}
        if(slot != null) {slot.remove();}
        super.destroy();
    }

    @Override
    protected void attach(UI ui) {
        super.attach(ui);
        synchronized (WINDOWS) {WINDOWS.add(this);}
        Gob player = ui.gui.map.player();
        pc = player == null ? null : player.rc;
        attachEffect();
    }

    private void mark() {
        if(detected == null) {return;}
        if(pc == null) {
            Gob p = ui.gui.map.player();
            if(p != null) {pc = p.rc;}
        }
//        if(pc != null) {
//            ui.gui.mapfile.addMarker(pc.floor(tilesz), String.format("%s (below)", NUtils.prettyResName(detected)));
//        }
    }

    private void fx(Dowse fx) {
        slot = ui.gui.map.drawadd(fx);
    }

    private static void attachEffect() {
        synchronized (WINDOWS) {
            if(!WINDOWS.isEmpty() && !EFFECTS.isEmpty()) {
                WINDOWS.remove().fx(EFFECTS.remove());
            }
        }
    }

    public static void overlay(Gob gob, Gob.Overlay overlay) {
        if(!QUALITIES.isEmpty()) {

            double a1 = getFieldValueDouble(overlay.spr, "a1");
            double a2 = getFieldValueDouble(overlay.spr, "a2");

            EFFECTS.add(new Dowse(gob, a1, a2, QUALITIES.remove()));
            attachEffect();

            // Add directional vectors for cone edges
            addConeVectors(gob, a1, a2);
        }
    }

    /**
     * Adds directional vectors for the edges of a dowsing/tracking cone to the minimap.
     * @param gob The player gob (origin of the cone)
     * @param a1 First angle (left edge)
     * @param a2 Second angle (right edge)
     */
    public static void addConeVectors(Gob gob, double a1, double a2) {
        try {
            NGameUI gui = NUtils.getGameUI();
            if (gui == null || gui.map == null || !(gui.map instanceof NMapView)) {
                return;
            }
            if (gui.mmap == null || gui.mmap.sessloc == null) {
                return;
            }

            NMapView mapView = (NMapView) gui.map;
            MiniMap.Location sessloc = gui.mmap.sessloc;

            // Player position in world coordinates
            Coord2d playerWorld = gob.rc;

            // Convert player world position to tile coordinates
            Coord playerTileCoords = playerWorld.div(MCache.tilesz).floor().add(sessloc.tc);

            // Calculate far points along each edge of the cone
            // Use a long distance so the vectors extend far on the map
            double vectorLength = 5000; // In world units

            // Edge 1: angle a1
            // Note: The game uses a coordinate system where Y is inverted for rendering
            // cos(a) gives X direction, sin(a) gives Y direction (but we need to check the sign)
            Coord2d edge1World = new Coord2d(
                playerWorld.x + cos(a1) * vectorLength,
                playerWorld.y - sin(a1) * vectorLength  // Negate sin because of coordinate system
            );
            Coord edge1TileCoords = edge1World.div(MCache.tilesz).floor().add(sessloc.tc);

            // Edge 2: angle a2
            Coord2d edge2World = new Coord2d(
                playerWorld.x + cos(a2) * vectorLength,
                playerWorld.y - sin(a2) * vectorLength  // Negate sin because of coordinate system
            );
            Coord edge2TileCoords = edge2World.div(MCache.tilesz).floor().add(sessloc.tc);

            // Get color for this pair (same color for both edges)
            java.awt.Color pairColor = DirectionalVector.getNextColor();

            // Add the vectors with the same color
            mapView.directionalVectors.add(new DirectionalVector(
                playerTileCoords, edge1TileCoords, "Dowse Edge 1", -1, pairColor
            ));
            mapView.directionalVectors.add(new DirectionalVector(
                playerTileCoords, edge2TileCoords, "Dowse Edge 2", -1, pairColor
            ));

            // Defer window creation to UI thread to avoid deadlock
            gui.ui.loader.defer(() -> TrackingVectorWindow.showWindow(), null);

        } catch (Exception e) {
            // Silently ignore errors
        }
    }

    public static double getFieldValueDouble(Object obj, String name) {
        double v = 0;
        try {
            Field f = getField(obj, name);
            v = f.getDouble(obj);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return v;
    }

    private static Field getField(Object obj, String name) throws NoSuchFieldException {
        Class cls = obj.getClass();
        while (true) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
                if(cls == null){
                    throw e;
                }
            }

        }
    }

    public static void item(WItem item) {
        if(item != null) {
            for(ItemInfo itemInfo: item.item.info)
            {
                if(itemInfo instanceof Quality)
                {
                    QUALITIES.add((Quality)itemInfo);
                }
            }
        }
    }

    public void text(String text) {
        Matcher matcher = detect.matcher(text);
        if(matcher.matches()) {
            detected = matcher.group(1);
        } else if(mark != null) {
            mark.hide();
        }
    }

    public static class Dowse extends Sprite {
        private static final VertexArray.Layout fmt = new VertexArray.Layout(
                new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 16),
                new VertexArray.Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.UNORM8), 0, 12, 16)
        );
        private static final Pipe.Op state = Pipe.Op.compose(VertexColor.instance, Rendered.last, States.Depthtest.none, States.maskdepth, Rendered.postpfx);

        private final Coord3f c;
        private final double a1;
        private final double a2;
        private final double r;
        private final Model model;

        protected Dowse(Gob gob, double a1, double a2, Quality q) {
            super(gob, null);
            this.c = new Coord3f((float) gob.rc.x, (float) -gob.rc.y, 0.1f);
            this.a1 = a1;
            this.a2 = a2;
            if(q == null) {
                r = 100;
            } else {
                r = 110 * (q.q - 10);
            }
            model = new Model(Model.Mode.TRIANGLE_FAN, new VertexArray(fmt, new VertexArray.Buffer(v2(), DataBuffer.Usage.STREAM)), null);
        }

        private ByteBuffer v2() {
            ByteBuffer buf = ByteBuffer.allocate(128);
            buf.order(ByteOrder.nativeOrder());
            byte alpha = (byte) 80;

            buf.putFloat(c.x).putFloat(c.y).putFloat(c.z);
            buf.put((byte) 255).put((byte) 0).put((byte) 0).put(alpha);
            for (double ca = a1; ca < a2; ca += PI * 0x0.04p0) {
                buf = Utils.growbuf(buf, 16);
                buf.putFloat(c.x + (float) (cos(ca) * r)).putFloat(c.y + (float) (sin(ca) * r)).putFloat(c.z);
                buf.put((byte) 255).put((byte) 0).put((byte) 0).put(alpha);
            }
            buf = Utils.growbuf(buf, 16);
            buf.putFloat(c.x + (float) (cos(a2) * r)).putFloat(c.y + (float) (sin(a2) * r)).putFloat(c.z);
            buf.put((byte) 255).put((byte) 0).put((byte) 0).put(alpha);
            ((Buffer) buf).flip();
            return (buf);
        }


        public void added(RenderTree.Slot slot) {
            slot.ostate(state);
            slot.add(model);
        }
    }
}
