/* Preprocessed source code */
package nurgling.overlays;

import haven.*;
import haven.render.*;
import haven.render.Model.Indices;
import nurgling.NGob;
import nurgling.NUtils;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BooleanSupplier;

/* >spr: BPRad */
public class NTexMarker extends Sprite implements RenderTree.Node, PView.Render2D {

	int z = 15;
	TexI img;
	private final BooleanSupplier condition; // Функциональный интерфейс для условия

	public NTexMarker(Owner owner, TexI img, BooleanSupplier condition) {
		super(owner, null);
		this.img = img;
		this.condition = condition;
	}

	public NTexMarker(Owner owner, TexI img) {
		this(owner, img, () -> true);
	}

	@Override
	public boolean tick(double dt) {
		boolean result = super.tick(dt);
		return result || (condition != null && condition.getAsBoolean());
	}

	@Override
	public void gtick(Render g) {
		super.gtick(g);
	}

	@Override
	public void draw(GOut g, Pipe state) {
		Coord3f markerPos = new Coord3f(0, 0, z + NUtils.getDeltaZ());
		Coord sc = Homo3D.obj2view(markerPos, state, Area.sized(g.sz())).round2();
		g.aimage(img, sc, 0.5, 0.5,UI.scale(48,48));
	}
}