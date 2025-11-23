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
    private static final Color OUTLINE_COLOR = Color.BLACK;
    
    // Speed comparison colors
    private static final Color PLAYER_COLOR = Color.WHITE;
    private static final Color SLOWER_COLOR = Color.GREEN;
    private static final Color FASTER_COLOR = Color.RED;
    
    protected Coord3f pos;
    private TexI speedLabel = null;
    private String lastSpeedText = "";
    private double lastPlayerSpeed = -1; // Track player speed for color updates
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 100; // Update every 100ms
    
    // Visibility flags
    private boolean shouldShow = false;
    private boolean configEnabled = true;
    
    // Speedometer icon texture
    public final Tex speedometerIcon = Resource.loadtex("nurgling/hud/speedometer");
    
    public NSpeedometerOverlay(Owner owner) {
        super(owner, null);
        pos = new Coord3f(0, 0, -5); // Position well above the gob (above head level)
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

        // Check if this is a mounted player (don't show speedometer on mounted players)
        boolean isMountedPlayer = false;
        String pose = gob.pose();
        if (pose != null && pose.contains("gfx/borka/")) {
            // This is a player, check if they're mounted
            Following following = gob.getattr(Following.class);
            isMountedPlayer = (following != null);
        }

        // Check if gob is moving and is a player or kritter (but not a mounted player)
        shouldShow = configEnabled &&
                    !isMountedPlayer &&
                    (gob.getattr(Moving.class) != null) &&
                    (gob.getv() > 0.1) &&
                    isPlayerOrKritter(gob);
        
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
        
        // Get current player speed for comparison
        Gob player = NUtils.player();
        double currentPlayerSpeed = (player != null) ? player.getv() : 0.0;
        
        // Update texture if speed changed OR player speed changed (affects color)
        if (!speedText.equals(lastSpeedText) || Math.abs(currentPlayerSpeed - lastPlayerSpeed) > 0.01) {
            lastSpeedText = speedText;
            lastPlayerSpeed = currentPlayerSpeed;
            Color speedColor = getSpeedColor(gob, speed);
            speedLabel = createSpeedTexture(speedText, speedColor);
        }
    }
    
    private Color getSpeedColor(Gob currentGob, double currentSpeed) {
        // Check if this is the player
        Gob player = NUtils.player();
        if (player != null && currentGob.id == player.id) {
            return PLAYER_COLOR; // White for player
        }
        
        // Compare with player speed
        if (player != null) {
            double playerSpeed = player.getv();
            
            if (currentSpeed < playerSpeed) {
                // Slower than player - shade from green to white
                double ratio = Math.min(currentSpeed / playerSpeed, 1.0);
                return blendColors(SLOWER_COLOR, PLAYER_COLOR, ratio);
            } else if (currentSpeed > playerSpeed) {
                // Faster than player - shade from white to red
                double ratio = Math.min((currentSpeed - playerSpeed) / playerSpeed, 1.0);
                return blendColors(PLAYER_COLOR, FASTER_COLOR, ratio);
            } else {
                // Same speed as player
                return PLAYER_COLOR;
            }
        }
        
        // Default to white if no player reference
        return PLAYER_COLOR;
    }
    
    private Color blendColors(Color color1, Color color2, double ratio) {
        // Clamp ratio between 0 and 1
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        
        int red = (int) (color1.getRed() * (1 - ratio) + color2.getRed() * ratio);
        int green = (int) (color1.getGreen() * (1 - ratio) + color2.getGreen() * ratio);
        int blue = (int) (color1.getBlue() * (1 - ratio) + color2.getBlue() * ratio);
        
        return new Color(red, green, blue);
    }
    
    private TexI createSpeedTexture(String speedText, Color speedColor) {
        // Get font metrics and icon dimensions
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(SPEED_FONT);
        int iconWidth = speedometerIcon.sz().x;
        int iconHeight = speedometerIcon.sz().y;
        int textWidth = fm.stringWidth(speedText);
        int textHeight = fm.getHeight();
        
        // Calculate combined dimensions with small gap between icon and text
        int gap = 3;
        int totalWidth = iconWidth + gap + textWidth + 4; // Add padding
        int totalHeight = Math.max(iconHeight, textHeight) + 2;
        
        BufferedImage img = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw speedometer icon
        int iconY = (totalHeight - iconHeight) / 2;
        g2d.drawImage(((TexI)speedometerIcon).back, 2, iconY, null);
        
        // Calculate text position (vertically centered with icon)
        int textX = 2 + iconWidth + gap;
        int textY = (totalHeight - textHeight) / 2 + fm.getAscent();
        
        // Draw text outline (black text slightly offset)
        g2d.setFont(SPEED_FONT);
        g2d.setColor(OUTLINE_COLOR);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x != 0 || y != 0) {
                    g2d.drawString(speedText, textX + x, textY + y);
                }
            }
        }
        
        // Draw main text with dynamic color
        g2d.setColor(speedColor);
        g2d.drawString(speedText, textX, textY);
        
        g2d.dispose();
        return new TexI(img);
    }
    
    @Override
    public void draw(GOut g, Pipe state) {
        if (!shouldShow || speedLabel == null) {
            return;
        }
        
        Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
        if(sc == null)
            return;
            
        // Draw combined speedometer icon and text (centered)
        g.aimage(speedLabel, sc, 0.5, 0.5);
    }

    /**
     * Checks if the gob is a player or kritter (creature) that should display speedometer.
     * Only players and creatures should show speedometer, not vehicles or other objects.
     */
    private boolean isPlayerOrKritter(Gob gob) {
        try {
            // Check if it's any player using pose pattern (proven method from NKinRing)
            String pose = gob.pose();
            if (pose != null && pose.contains("gfx/borka/")) {
                return true; // This is a player
            }

            // Check if it's a creature (kritter) using resource name pattern
            if (gob.ngob != null && gob.ngob.name != null) {
                return gob.ngob.name.contains("kritter");
            }

            return false;
        } catch (Exception e) {
            // If any error occurs, default to not showing speedometer
            return false;
        }
    }
}