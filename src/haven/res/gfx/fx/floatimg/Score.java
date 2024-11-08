/* Preprocessed source code */
package haven.res.gfx.fx.floatimg;

import haven.*;
import haven.render.*;
import nurgling.overlays.NDMGOverlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import static haven.PUtils.*;

@haven.FromResource(name = "gfx/fx/floatimg", version = 6)
public class Score implements Sprite.Factory {
    private static int dup(int nibble) {
	return((nibble << 4) | nibble);
    }

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
	int num = sdt.int32();
	int fl = sdt.uint8();
	int col = sdt.uint16();
	NDMGOverlay.IsDMG(col,num,(Gob)owner);
	String buf;
	int t = (fl & 6) >> 1;
	if(t == 3) {
	    float val = Float.intBitsToFloat(num);
	    String sign = ((fl & 1) != 0) ? "+" : "";
	    if(val < 0) {
		val = -val;
		sign = "-";
	    }
	    if(val >= 0.95) {
		buf = Integer.toString(Math.round(val));
	    } else if(val >= 0.05) {
		buf = String.format("%.1f", val);
	    } else if(val >= 0.005) {
		buf = String.format("%.2f", val);
	    } else {
		buf = "0";
	    }
	    buf = sign + buf;
	} else {
	    String sign = ((fl & 1) != 0) ? "+" : "";
	    if(num < 0) {
		num = -num;
		sign = "-";
	    }
	    if(t == 1) {
		buf = Integer.toString(num / 10) + "." + Integer.toString(num % 10);
	    } else if(t == 2) {
		buf = String.format("%02d:%02d", num / 60, num % 60);
	    } else {
		buf = Integer.toString(num);
	    }
	    buf = sign + buf;
	}
	Color dcol = new Color(dup((col & 0xf000) >> 12),
			       dup((col & 0x0f00) >> 8),
			       dup((col & 0x00f0) >> 4),
			       dup((col & 0x000f) >> 0));
	return(new FloatText(owner, res, buf, dcol));
    }
}
