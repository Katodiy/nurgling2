package nurgling.overlays;


import haven.*;
import haven.render.*;
import nurgling.NUtils;

/**
 * Renders a textured ring around a gob based on GobIcon settings
 */
public class NGobIconRing extends Sprite implements RenderTree.Node
{
    static final VertexArray.Layout pfmt = new VertexArray.Layout(
            new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 20),
            new VertexArray.Layout.Input(Tex2D.texc, new VectorFormat(2, NumberFormat.FLOAT32), 0, 12, 20));

    final Model emod;
    Gob gob;
    ColorTex texture;
    float radius;
    private boolean lastRingState = true; // Assume true on creation

    /**
     * Creates a ring overlay for a gob
     * @param owner The gob to attach the ring to
     * @param radius The radius of the ring in game units
     */
    public NGobIconRing(Owner owner, float radius)
    {
        super(owner, null);
        this.gob = (Gob) owner;
        this.radius = radius;
        
        // Load the ring texture from marks/notifyrings
        texture = new TexI(Resource.loadimg("marks/notifyrings")).st();

        // Create vertex data for a quad that will display the ring texture
        // The quad is positioned at z=5f (slightly above ground) to avoid z-fighting
        float[] data = {
                radius, radius, 5f, 1, 1,
                -radius, radius, 5f, 0, 1,
                -radius, -radius, 5f, 0, 0,
                radius, -radius, 5f, 1, 0,
        };
        
        VertexArray va = new VertexArray(pfmt,
                new VertexArray.Buffer((4) * pfmt.inputs[0].stride, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(data)));
        this.emod = new Model(Model.Mode.TRIANGLE_FAN, va, null);
    }

    public void added(RenderTree.Slot slot)
    {
        // Render setup with blending for transparency
        Pipe.Op rmat = Pipe.Op.compose(
                new Rendered.Order.Default(-100), 
                new States.Depthtest(States.Depthtest.Test.LE), 
                States.maskdepth,
                FragColor.blend(new BlendMode(
                        BlendMode.Function.ADD, BlendMode.Factor.SRC_ALPHA, BlendMode.Factor.INV_SRC_ALPHA,
                        BlendMode.Function.ADD, BlendMode.Factor.ONE, BlendMode.Factor.INV_SRC_ALPHA)), 
                texture, 
                Rendered.postpfx);
        slot.add(emod, rmat);
    }

    @Override
    public boolean tick(double dt)
    {
        // Only check every ~10 ticks to reduce overhead
        // This means rings will update within ~0.3 seconds of setting change
        if (Math.random() > 0.1) {
            return false;
        }
        
        // Remove ring if GameUI is not available
        if (NUtils.getGameUI() == null)
            return true;

        // Check if ring is still enabled in icon settings
        GobIcon icon = gob.getattr(GobIcon.class);
        if (icon == null)
            return true;

        try {
            GobIcon.Settings settings = NUtils.getGameUI().iconconf;
            if (settings != null && icon.icon() != null) {
                GobIcon.Setting setting = settings.get(icon.icon());
                if (setting == null || !setting.ring) {
                    return true; // Remove if ring disabled
                }
            }
        } catch (Exception e) {
            // If we can't check settings, keep the ring
        }

        return false; // Keep the ring
    }

    /**
     * Helper method to check if a gob should have a ring based on icon settings
     */
    public static boolean shouldShowRing(Gob gob) {
        if (NUtils.getGameUI() == null) {
            return false;
        }

        GobIcon icon = gob.getattr(GobIcon.class);
        if (icon == null || icon.icon() == null) {
            return false;
        }

        try {
            nurgling.NGameUI gui = NUtils.getGameUI();
            GobIcon.Settings settings = gui.iconconf;
            if (settings == null) {
                return false;
            }

            GobIcon.Setting setting = settings.get(icon.icon());
            if (setting == null) {
                return false;
            }
            
            // Check setting.ring first
            if (setting.ring) {
                return true;
            }
            
            // If setting.ring is false, check local config as fallback
            // This handles the case where settings haven't been applied yet
            if (gui.iconRingConfig != null) {
                String iconResName = icon.icon().res.name;
                boolean localRing = gui.iconRingConfig.getRing(iconResName);
                
                // Apply local setting to iconconf for next time
                if (localRing && !setting.ring) {
                    setting.ring = true;
                }
                
                return localRing;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a ring with custom size based on gob's hitbox
     */
    public static NGobIconRing createAutoSize(Gob gob) {
        double len = MCache.tilesz.x * 2;
        if (gob.ngob != null && gob.ngob.hitBox != null) {
            len = Math.max(gob.ngob.hitBox.end.dist(gob.ngob.hitBox.begin), len);
        }
        return new NGobIconRing(gob, (float) len);
    }
}
