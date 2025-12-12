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

import java.awt.image.BufferedImage;
import nurgling.NUI;

public class Img extends Widget {
    protected Tex img;
    private BufferedImage rimg;
    public boolean hit = false, opaque = false;
    
    // Marker for character selection screen icons
    public enum CharselType { NONE, BACKGROUND, VERIFY, SUB }
    public CharselType charselType = CharselType.NONE;
	
    // Character selection screen replacement textures
    private static final String CHARSEL_BG_RES = "gfx/ccscr";
    private static final String CHARSEL_VERIFY_RES = "gfx/ccver";
    private static final String CHARSEL_SUB_RES = "gfx/ccsub";
    private static Tex charselBg = null;
    private static Tex verifyTex = null;
    private static Tex subTex = null;
    
    private static Tex getCharselBg() {
	if(charselBg == null)
	    charselBg = Resource.loadtex("nurgling/hud/world161");
	return charselBg;
    }
    
    private static Tex getVerifyTex() {
	if(verifyTex == null)
	    verifyTex = Resource.loadtex("nurgling/hud/verify");
	return verifyTex;
    }
    
    private static Tex getSubTex() {
	if(subTex == null)
	    subTex = Resource.loadtex("nurgling/hud/sub");
	return subTex;
    }

    @RName("img")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Indir<Resource> res;
	    String resName = null;
	    int a = 0;
	    if(args[a] instanceof String) {
		String nm = (String)args[a++];
		int ver = (args.length > a) ? Utils.iv(args[a++]) : -1;
		res = new Resource.Spec(Resource.remote(), nm, ver);
	    } else {
		res = ui.sess.getresv(args[a++]);
	    }
	    resName = res.get().name;
	    Tex tex;
	    CharselType charselType = CharselType.NONE;
	    // Replace character selection screen resources
	    if(resName != null && resName.contains(CHARSEL_BG_RES)) {
		tex = getCharselBg();
		charselType = CharselType.BACKGROUND;
	    } else if(resName != null && resName.contains(CHARSEL_VERIFY_RES)) {
		tex = getVerifyTex();
		charselType = CharselType.VERIFY;
	    } else if(resName != null && resName.contains(CHARSEL_SUB_RES)) {
		tex = getSubTex();
		charselType = CharselType.SUB;
	    } else {
		tex = res.get().flayer(Resource.imgc).tex();
	    }
	    Img ret = new Img(tex);
	    ret.charselType = charselType;
	    if(args.length > a) {
		int fl = Utils.iv(args[a++]);
		ret.hit = (fl & 1) != 0;
		ret.opaque = (fl & 2) != 0;
	    }
	    return(ret);
	}
    }

    public void setimg(Tex img) {
	this.img = img;
	resize(img.sz());
	if(img instanceof TexI)
	    rimg = ((TexI)img).back;
	else
	    rimg = null;
    }

    public void draw(GOut g) {
	g.image(img, Coord.z);
    }

    public Img(Tex img) {
	super(img.sz());
	setimg(img);
    }

    public void uimsg(String name, Object... args) {
	if(name == "ch") {
	    Indir<Resource> res;
	    if(args[0] instanceof String) {
		String nm = (String)args[0];
		int ver = (args.length > 1) ? Utils.iv(args[1]) : -1;
		res = new Resource.Spec(Resource.remote(), nm, ver);
	    } else {
		res = ui.sess.getresv(args[0]);
	    }
	    setimg(res.get().flayer(Resource.imgc).tex());
	} else if(name == "cl") {
	    hit = Utils.bv(args[0]);
	} else if(name == "tip") {
	    // Handle tip message and check for verification/subscription
	    super.uimsg(name, args);
	    checkVerificationStatus();
	} else {
	    super.uimsg(name, args);
	}
    }
    
    public boolean checkhit(Coord c) {
	if(!c.isect(Coord.z, sz))
	    return(false);
	if(opaque || (rimg == null) || (rimg.getRaster().getNumBands() < 4))
	    return(true);
	return(rimg.getRaster().getSample(c.x, c.y, 3) >= 128);
    }

    public boolean mousedown(MouseDownEvent ev) {
	if(hit && checkhit(ev.c)) {
	    wdgmsg("click", ev.c, ev.b, ui.modflags());
	    return(true);
	}
	return(super.mousedown(ev));
    }

    /**
     * Check if this Img's tooltip indicates verification or subscription status
     * and update NUI.sessInfo accordingly.
     */
    private void checkVerificationStatus() {
	if(ui == null || !(ui instanceof NUI)) return;
	NUI nui = (NUI) ui;
	
	// Initialize sessInfo if not yet created but session is available
	if(nui.sessInfo == null) {
	    nui.initSessInfo();
	}
	if(nui.sessInfo == null) return;
	
	if(tooltip instanceof Widget.KeyboundTip) {
	    String tooltipText = ((Widget.KeyboundTip) tooltip).base;
	    if(tooltipText != null) {
		if(!nui.sessInfo.isVerified && tooltipText.contains("Verif")) {
		    nui.sessInfo.setVerified(true);
		}
		if(!nui.sessInfo.isSubscribed && tooltipText.contains("Subsc")) {
		    nui.sessInfo.setSubscribed(true);
		}
	    }
	}
    }
}
