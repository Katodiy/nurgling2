/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Color;
import java.util.*;
import haven.render.*;
import haven.render.sl.*;
import nurgling.NConfig;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

public class Outlines implements RenderTree.Node {
    private boolean symmetric;

    // Nurgling: Outline thickness parameters - configurable via NConfig.Key.thinOutlines
    public static boolean isThinOutlines() {
        return Boolean.TRUE.equals(NConfig.get(NConfig.Key.thinOutlines));
    }
    public static double getDepthThreshold() {
        return isThinOutlines() ? 0.00005 : 0.0002;
    }
    public static double getDepthSmoothLow() {
        return isThinOutlines() ? 12.0 : 5.0;
    }
    public static double getDepthSmoothHigh() {
        return isThinOutlines() ? 15.0 : 6.0;
    }
    public static double getNormalScale() {
        return isThinOutlines() ? 0.5 : 1.0;
    }
    public static double getFinalSmoothLow() {
        return isThinOutlines() ? 0.7 : 0.4;
    }
    public static double getFinalSmoothHigh() {
        return isThinOutlines() ? 0.85 : 0.6;
    }
    // Sub-pixel sample offsets for spatial AA (rotated grid pattern)
    public static double[][] getAAOffsets() {
        return new double[][] {
            {-0.125, -0.375},
            { 0.375, -0.125},
            {-0.375,  0.125},
            { 0.125,  0.375}
        };
    }

    private final static Uniform snrm = new Uniform(SAMPLER2D, p -> ((Draw)p.get(RUtils.adhoc)).nrm, RUtils.adhoc);
    private final static Uniform sdep = new Uniform(SAMPLER2D, p -> ((Draw)p.get(RUtils.adhoc)).depth, RUtils.adhoc);
    private final static Uniform msnrm = new Uniform(SAMPLER2DMS, p -> ((Draw)p.get(RUtils.adhoc)).nrm, RUtils.adhoc);
    private final static Uniform msdep = new Uniform(SAMPLER2DMS, p -> ((Draw)p.get(RUtils.adhoc)).depth, RUtils.adhoc);
    private final static ShaderMacro[] shaders = new ShaderMacro[4];

    private static class Draw extends RUtils.AdHoc {
	final Texture2D.Sampler2D nrm, depth;

	Draw(ShaderMacro code, Texture2D.Sampler2D nrm, Texture2D.Sampler2D depth) {
	    super(code);
	    this.nrm = nrm;
	    this.depth = depth;
	}
    }

    private static ShaderMacro shader(final boolean symmetric, final boolean ms) {
	return(new ShaderMacro() {
		Color color = Color.BLACK;
		Coord[] points = {
		    new Coord(-1,  0),
		    new Coord( 1,  0),
		    new Coord( 0, -1),
		    new Coord( 0,  1),
		};

		Expression sample(boolean nrm, Expression c, Expression s, Coord o) {
		    if(ms) {
			Expression ctc = ivec2(floor(mul(c, FrameConfig.u_screensize.ref())));
			if(!o.equals(Coord.z))
			    ctc = add(ctc, ivec2(o));
			return(texelFetch((nrm?msnrm:msdep).ref(), ctc, s));
		    } else {
			Expression ctc = c;
			if(!o.equals(Coord.z))
			    ctc = add(c, mul(vec2(o), FrameConfig.u_pixelpitch.ref()));
			return(texture2D((nrm?snrm:sdep).ref(), ctc));
		    }
		}

		Function ofac = new Function.Def(FLOAT) {{
		    Expression sample = param(PDir.IN, INT).ref();
		    Expression tc = Tex2D.rtexcoord.ref();
		    LValue ret = code.local(FLOAT, l(0.0)).ref();
		    Expression lnrm = code.local(VEC3, pick(sample(true, tc, sample, Coord.z), "rgb")).ref();
		    Expression ldep = code.local(FLOAT, pick(sample(false, tc, sample, Coord.z), "r")).ref();
		    /* XXX: Current depth detection doesn't work well
		     * with frustum projections, perhaps because of
		     * the lack of precision in the depth buffer
		     * (though I'm not sure I buy that explanation
		     * yet). */
		    LValue dh = code.local(FLOAT, l(getDepthThreshold())).ref(), dl = code.local(FLOAT, l(-getDepthThreshold())).ref();
		    for(int i = 0; i < points.length; i++) {
			Expression cdep = pick(sample(false, tc, sample, points[i]), "r");
			cdep = sub(ldep, cdep);
			cdep = code.local(FLOAT, cdep).ref();
			code.add(stmt(ass(dh, max(dh, cdep))));
			code.add(stmt(ass(dl, min(dl, cdep))));
		    }
		    if(symmetric)
			code.add(aadd(ret, smoothstep(l(getDepthSmoothLow()), l(getDepthSmoothHigh()), max(div(dh, neg(dl)), div(dl, neg(dh))))));
		    else
			code.add(aadd(ret, smoothstep(l(getDepthSmoothLow()), l(getDepthSmoothHigh()), div(dh, neg(dl)))));
		    for(int i = 0; i < points.length; i++) {
			Expression cnrm = pick(sample(true, tc, sample, points[i]), "rgb");
			if(symmetric) {
			    code.add(aadd(ret, mul(sub(l(1.0), abs(dot(lnrm, cnrm))), l(getNormalScale()))));
			} else {
			    cnrm = code.local(VEC3, cnrm).ref();
			    code.add(new If(gt(pick(cross(lnrm, cnrm), "z"), l(0.0)),
					    stmt(aadd(ret, mul(sub(l(1.0), abs(dot(lnrm, cnrm))), l(getNormalScale()))))));
			}
		    }
		    code.add(new Return(smoothstep(l(getFinalSmoothLow()), l(getFinalSmoothHigh()), min(ret, l(1.0)))));
		}};

		Function msfac = new Function.Def(FLOAT) {{
		    LValue ret = code.local(FLOAT, l(0.0)).ref();
		    LValue i = code.local(INT, null).ref();
		    code.add(new For(ass(i, l(0)), lt(i, FrameConfig.u_numsamples.ref()), linc(i),
				     stmt(aadd(ret, ofac.call(i)))));
		    code.add(new Return(div(ret, FrameConfig.u_numsamples.ref())));
		}};

		// Nurgling: Spatial AA function - samples at sub-pixel offsets for smoother edges
		Function ofacAA = new Function.Def(FLOAT) {{
		    Expression sampleIdx = param(PDir.IN, INT).ref();
		    Expression tcBase = Tex2D.rtexcoord.ref();
		    double[][] aaOffsets = getAAOffsets();
		    if (aaOffsets != null && aaOffsets.length > 0) {
			LValue total = code.local(FLOAT, l(0.0)).ref();
			for (double[] offset : aaOffsets) {
			    // Offset texture coordinate by sub-pixel amount
			    Expression tcOffset = add(tcBase, mul(vec2(l(offset[0]), l(offset[1])), FrameConfig.u_pixelpitch.ref()));
			    // Sample edge detection at offset position
			    LValue ret = code.local(FLOAT, l(0.0)).ref();
			    Expression lnrm = code.local(VEC3, pick(texture2D(snrm.ref(), tcOffset), "rgb")).ref();
			    Expression ldep = code.local(FLOAT, pick(texture2D(sdep.ref(), tcOffset), "r")).ref();
			    LValue dh = code.local(FLOAT, l(getDepthThreshold())).ref();
			    LValue dl = code.local(FLOAT, l(-getDepthThreshold())).ref();
			    for (Coord pt : points) {
				Expression sampleTc = add(tcOffset, mul(vec2(pt), FrameConfig.u_pixelpitch.ref()));
				Expression cdep = pick(texture2D(sdep.ref(), sampleTc), "r");
				cdep = sub(ldep, cdep);
				cdep = code.local(FLOAT, cdep).ref();
				code.add(stmt(ass(dh, max(dh, cdep))));
				code.add(stmt(ass(dl, min(dl, cdep))));
			    }
			    if (symmetric)
				code.add(aadd(ret, smoothstep(l(getDepthSmoothLow()), l(getDepthSmoothHigh()), max(div(dh, neg(dl)), div(dl, neg(dh))))));
			    else
				code.add(aadd(ret, smoothstep(l(getDepthSmoothLow()), l(getDepthSmoothHigh()), div(dh, neg(dl)))));
			    for (Coord pt : points) {
				Expression sampleTc = add(tcOffset, mul(vec2(pt), FrameConfig.u_pixelpitch.ref()));
				Expression cnrm = pick(texture2D(snrm.ref(), sampleTc), "rgb");
				if (symmetric) {
				    code.add(aadd(ret, mul(sub(l(1.0), abs(dot(lnrm, cnrm))), l(getNormalScale()))));
				} else {
				    cnrm = code.local(VEC3, cnrm).ref();
				    code.add(new If(gt(pick(cross(lnrm, cnrm), "z"), l(0.0)),
						    stmt(aadd(ret, mul(sub(l(1.0), abs(dot(lnrm, cnrm))), l(getNormalScale()))))));
				}
			    }
			    code.add(aadd(total, smoothstep(l(getFinalSmoothLow()), l(getFinalSmoothHigh()), min(ret, l(1.0)))));
			}
			code.add(new Return(div(total, l((double)aaOffsets.length))));
		    } else {
			// Fallback to regular sampling
			code.add(new Return(ofac.call(sampleIdx)));
		    }
		}};

		public void modify(ProgramContext prog) {
		    FragColor.fragcol(prog.fctx).mod(in -> {
			    Expression of;
			    if (ms) {
				of = msfac.call();
			    } else if (getAAOffsets() != null) {
				of = ofacAA.call(l(-1));
			    } else {
				of = ofac.call(l(-1));
			    }
			    return(vec4(col3(color), mix(l(0.0), l(1.0), of)));
			}, 0);
		}
	    });
    }

    static {
	/* XXX: It would be good to have some kind of more convenient
	 * shader internation. */
	shaders[0] = shader(false, false);
	shaders[1] = shader(false, true);
	shaders[2] = shader(true,  false);
	shaders[3] = shader(true,  true);
    }

    public Outlines(boolean symmetric) {
	this.symmetric = symmetric;
    }

    public void added(RenderTree.Slot slot) {
	RenderedNormals.get(slot.state());
	slot.add(new Rendered.ScreenQuad(false), p -> {
		FrameConfig fb = p.get(FrameConfig.slot);
		DepthBuffer<?> dbuf = p.get(DepthBuffer.slot);
		RenderedNormals nbuf = p.get(RenderedNormals.slot);
		boolean ms = fb.samples > 1;
		p.prep(Rendered.postfx);
		p.put(RenderedNormals.slot, null);
		p.put(DepthBuffer.slot, null);
		p.prep(new Draw(shaders[(symmetric?2:0) | (ms?1:0)],
				new Texture2D.Sampler2D((Texture2D)nbuf.img.tex),
				new Texture2D.Sampler2D((Texture2D)((Texture.Image)dbuf.image).tex)));
	    });
    }

    public void removed(RenderTree.Slot slot) {
	RenderedNormals.put(slot.state());
    }
}
