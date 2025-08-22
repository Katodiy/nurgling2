package nurgling.overlays;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import nurgling.NConfig;
import nurgling.NUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

public class NSpeedometerOverlay extends Sprite implements RenderTree.Node, PView.Render2D {
    private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("0.0");
    private static final Font SPEED_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Color SPEED_COLOR = Color.WHITE;
    private static final Color OUTLINE_COLOR = Color.BLACK;
    
    protected Coord3f pos;
    private TexI speedLabel = null;
    private String lastSpeedText = "";
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 100; // Update every 100ms
    
    // Visibility flags
    private boolean shouldShow = false;
    private boolean configEnabled = true;
    
    public NSpeedometerOverlay(Owner owner) {
        super(owner, null);
        pos = new Coord3f(0, 0, 18); // Position well above the gob (above head level)
    }
    
    @Override
    public boolean tick(double dt) {
        // Don't remove overlay for temporary UI issues
        if (NUtils.getGameUI() == null) {
            return false; // Keep overlay, UI might come back
        }
        
        // Check if the gob still exists
        Gob gob = (Gob) owner;
        if (gob == null) {
            return true; // Only remove if owner gob is truly null
        }
        
        // Check configuration setting
        configEnabled = (Boolean) NConfig.get(NConfig.Key.showSpeedometer);
        
        // Check if gob is moving
        shouldShow = configEnabled && (gob.getattr(Moving.class) != null) && (gob.getv() > 0.1);
        
        // Update speed label periodically only if we should show
        if (shouldShow) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
                updateSpeedLabel(gob);
                lastUpdateTime = currentTime;
            }
        }
        
        return false;
    }
    
    private void updateSpeedLabel(Gob gob) {
        double speed = gob.getv();
        String speedText = SPEED_FORMAT.format(speed);
        
        // Only update texture if speed changed
        if (!speedText.equals(lastSpeedText)) {
            lastSpeedText = speedText;
            speedLabel = createSpeedTexture(speedText);
        }
    }
    
    private TexI createSpeedTexture(String speedText) {
        // Create a simple text image
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(SPEED_FONT);
        int width = fm.stringWidth(speedText) + 4; // Add padding
        int height = fm.getHeight() + 2;
        
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw outline (black text slightly offset)
        g2d.setFont(SPEED_FONT);
        g2d.setColor(OUTLINE_COLOR);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x != 0 || y != 0) {
                    g2d.drawString(speedText, 2 + x, fm.getAscent() + 1 + y);
                }
            }
        }
        
        // Draw main text
        g2d.setColor(SPEED_COLOR);
        g2d.drawString(speedText, 2, fm.getAscent() + 1);
        
        g2d.dispose();
        return new TexI(img);
    }
    
    @Override
    public void draw(GOut g, Pipe state) {
        if (!shouldShow || speedLabel == null) {
            return;
        }
        
        Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
        g.aimage(speedLabel, sc, 0.5, 0.5);
    }
}