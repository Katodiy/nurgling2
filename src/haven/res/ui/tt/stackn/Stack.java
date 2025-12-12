package haven.res.ui.tt.stackn;/* Preprocessed source code */
import haven.*;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.conf.FontSettings;
import nurgling.conf.ItemQualityOverlaySettings;
import nurgling.conf.ItemQualityOverlaySettings.Corner;

import java.awt.*;
import java.awt.image.BufferedImage;

/* >tt: Stack */
@FromResource(name = "ui/tt/stackn", version = 4)
public class Stack extends ItemInfo.Name implements GItem.OverlayInfo<Tex> {
	public static boolean show = true;
	
	// Cached settings for performance
	private static ItemQualityOverlaySettings cachedSettings = null;
	private static long lastSettingsCheck = 0;
	private static final long SETTINGS_CHECK_INTERVAL = 200;
	private static long settingsVersion = 0;
	private static boolean forceRefresh = false;
	
	public Stack(Owner owner, String str) {
		super(owner, str);
	}

	public double quality = 0;
	private double lastQuality = -1;
	private long lastSettingsVersion = -1;
	private Tex cachedOverlay = null;
	
	public static void invalidateCache() {
		forceRefresh = true;
		settingsVersion++;
	}
	
	private static ItemQualityOverlaySettings getSettings() {
		long now = System.currentTimeMillis();
		if (forceRefresh || cachedSettings == null || now - lastSettingsCheck > SETTINGS_CHECK_INTERVAL) {
			ItemQualityOverlaySettings newSettings = (ItemQualityOverlaySettings) NConfig.get(NConfig.Key.stackQualityOverlay);
			if (newSettings == null) {
				newSettings = new ItemQualityOverlaySettings();
				newSettings.corner = Corner.TOP_LEFT;
			}
			if (cachedSettings != newSettings || forceRefresh) {
				cachedSettings = newSettings;
				settingsVersion++;
				forceRefresh = false;
			}
			lastSettingsCheck = now;
		}
		return cachedSettings;
	}

	public Tex overlay() {
		int count = 0;
		double q = 0;
		NGItem item = (NGItem)owner;
		if(item.contents != null) {
			for (Widget ch : item.contents.children()) {
				if (ch instanceof NGItem) {
					if(((NGItem)ch).quality == null)
						return null;
					q+=((NGItem)ch).quality;
					count++;
				}
			}
		}
		if(count>0) {
			q = q / count;
			quality = q;
			
			// Check if we need to rebuild the overlay
			long currentVersion = settingsVersion;
			if (cachedOverlay != null && Double.compare(lastQuality, q) == 0 && lastSettingsVersion == currentVersion) {
				return cachedOverlay;
			}
			
			ItemQualityOverlaySettings settings = getSettings();
			BufferedImage text = renderQualityText(q, settings);
			
			if (settings.showBackground) {
				BufferedImage bi = new BufferedImage(text.getWidth(), text.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = bi.createGraphics();
				graphics.setColor(settings.backgroundColor);
				graphics.fillRect(0, 0, bi.getWidth(), bi.getHeight());
				graphics.drawImage(text, 0, 0, null);
				graphics.dispose();
				cachedOverlay = new TexI(bi);
			} else {
				cachedOverlay = new TexI(text);
			}
			
			lastQuality = q;
			lastSettingsVersion = currentVersion;
			return cachedOverlay;
		}
		return null;
	}
	
	private BufferedImage renderQualityText(double quality, ItemQualityOverlaySettings settings) {
		FontSettings fontSettings = (FontSettings) NConfig.get(NConfig.Key.fonts);
		Font font;
		if (fontSettings != null) {
			font = fontSettings.getFont(settings.fontFamily);
			if (font == null) {
				font = new Font("SansSerif", Font.BOLD, UI.scale(settings.fontSize));
			} else {
				font = font.deriveFont(Font.BOLD, UI.scale((float) settings.fontSize));
			}
		} else {
			font = new Font("SansSerif", Font.BOLD, UI.scale(settings.fontSize));
		}
		
		String qualityText;
		if (settings.showDecimal) {
			qualityText = String.format("%.1f", quality);
		} else {
			qualityText = Integer.toString((int) Math.round(quality));
		}
		
		Color textColor = settings.getColorForQuality(quality);
		Text.Foundry fnd = new Text.Foundry(font, textColor).aa(true);
		BufferedImage textImg = fnd.render(qualityText, textColor).img;
		
		if (settings.showOutline) {
			return outlineWithWidth(textImg, settings.outlineColor, settings.outlineWidth);
		} else {
			return textImg;
		}
	}
	
	private BufferedImage outlineWithWidth(BufferedImage img, Color outlineColor, int width) {
		if (width <= 0) {
			return img;
		}
		
		int w = img.getWidth();
		int h = img.getHeight();
		int padding = width;
		
		BufferedImage result = new BufferedImage(w + padding * 2, h + padding * 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = result.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Create a colored version of the image for outline
		BufferedImage coloredImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D cg = coloredImg.createGraphics();
		cg.drawImage(img, 0, 0, null);
		cg.setComposite(AlphaComposite.SrcIn);
		cg.setColor(outlineColor);
		cg.fillRect(0, 0, w, h);
		cg.dispose();
		
		// Draw outline in all directions
		for (int dx = -width; dx <= width; dx++) {
			for (int dy = -width; dy <= width; dy++) {
				if (dx != 0 || dy != 0) {
					g.drawImage(coloredImg, padding + dx, padding + dy, null);
				}
			}
		}
		
		// Draw original image on top
		g.drawImage(img, padding, padding, null);
		g.dispose();
		
		return result;
	}

	public void drawoverlay(GOut g, Tex ol) {
		if(show && ol!=null) {
			ItemQualityOverlaySettings settings = getSettings();
			int pad = settings.showOutline ? settings.outlineWidth : 0;
			Coord pos;
			
			switch (settings.corner) {
				case TOP_LEFT:
					pos = new Coord(-pad, -pad);
					g.aimage(ol, pos, 0, 0);
					break;
				case TOP_RIGHT:
					pos = new Coord(g.sz().x + pad, -pad);
					g.aimage(ol, pos, 1, 0);
					break;
				case BOTTOM_LEFT:
					pos = new Coord(-pad, g.sz().y + pad);
					g.aimage(ol, pos, 0, 1);
					break;
				case BOTTOM_RIGHT:
				default:
					pos = new Coord(g.sz().x + pad, g.sz().y + pad);
					g.aimage(ol, pos, 1, 1);
					break;
			}
		}
	}

	@Override
	public boolean tick(double dt) {
		double q = 0;
		int count = 0;
		NGItem item = (NGItem)owner;
		if(item.contents != null) {
			for (Widget ch : item.contents.children()) {
				if (ch instanceof NGItem) {
					if(((NGItem)ch).quality == null)
						break;
					q+=((NGItem)ch).quality;
					count++;
				}
			}
		}
		
		// Also check if settings changed
		long currentVersion = settingsVersion;
		if (lastSettingsVersion != currentVersion) {
			cachedOverlay = null;
			return false; // Force redraw
		}

		return Double.compare(quality,q/count) == 0;
	}
}
