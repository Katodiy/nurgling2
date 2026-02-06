/* Preprocessed source code */
package haven.res.ui.croster;

import haven.*;
import haven.render.Homo3D;
import haven.render.Pipe;
import haven.render.RenderTree;
import haven.res.gfx.hud.rosters.cow.Ochs;
import haven.res.gfx.hud.rosters.goat.Goat;
import haven.res.gfx.hud.rosters.horse.Horse;
import haven.res.gfx.hud.rosters.pig.Pig;
import haven.res.gfx.hud.rosters.sheep.Sheep;
import haven.res.gfx.hud.rosters.teimdeer.Teimdeer;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.overlays.NCattleMarkRing;
import nurgling.tools.NParser;

import java.awt.*;

@haven.FromResource(name = "ui/croster", version = 77)
public class CattleId extends GAttrib implements RenderTree.Node, PView.Render2D {
    public final UID id;

    public CattleId(Gob gob, UID id) {
	super(gob);
	this.id = id;
    }

    public static void parse(Gob gob, Message dat) {
	UID id = UID.of(dat.int64());
	gob.setattr(new CattleId(gob, id));
    }

    private int rmseq = 0, entryseq = 0;
    private RosterWindow wnd = null;
    private CattleRoster<?> roster = null;
    private Entry entry = null;
    public Entry entry() {
	if((entry == null) || ((roster != null) && (roster.entryseq != entryseq))) {
	    if(rmseq != RosterWindow.rmseq) {
		synchronized(RosterWindow.rosters) {
		    RosterWindow wnd = RosterWindow.rosters.get(gob.glob);
		    if(wnd != null) {
			for(CattleRoster<?> ch : wnd.children(CattleRoster.class)) {
			    if(ch.entries.get(this.id) != null) {
				this.wnd = wnd;
				this.roster = ch;
				this.rmseq = RosterWindow.rmseq;
				break;
			    }
			}
		    }
		}
	    }
	    if(roster != null) {
		this.entry = roster.entries.get(this.id);
		// Set areaId based on gob position if not set
		if(this.entry != null && this.entry.areaId < 0 && gob.rc != null) {
		    NArea area = NUtils.getAreaByPosition(gob.rc);
		    if(area != null) {
			this.entry.areaId = area.id;
		    }
		}
	    }
	}
	return(entry);
    }

    /**
     * Resource name prefix (gfx/kritter/...) for each roster animal type. Used to match gob to selected tab.
     */
    public static String kritterPrefixFor(Class<? extends Entry> rosterType) {
        if (rosterType == Pig.class) return "gfx/kritter/pig/";
        if (rosterType == Ochs.class) return "gfx/kritter/cattle/";
        if (rosterType == Horse.class) return "gfx/kritter/horse/";
        if (rosterType == Sheep.class) return "gfx/kritter/sheep/";
        if (rosterType == Goat.class) return "gfx/kritter/goat/";
        if (rosterType == Teimdeer.class) return "gfx/kritter/reindeer/";
        return null;
    }

    /**
     * True if gob is an animal of the given roster type (by resource name). Works for any gob (with or without CattleId).
     */
    public static boolean gobMatchesRosterType(Gob gob, CattleRoster<?> roster) {
        String prefix = kritterPrefixFor(roster.getGenType());
        if (prefix == null) return false;
        return (gob.ngob != null && gob.ngob.name != null && gob.ngob.name.startsWith(prefix));
    }

    /**
     * True if this gob should show the "not in roster" ring: matches selected tab type and is not in that roster's entries.
     */
    public static boolean shouldShowNotInRosterRing(Gob gob, CattleRoster<?> visibleRoster) {
        if (visibleRoster == null) return false;
        if (!gobMatchesRosterType(gob, visibleRoster)) return false;
        if (gob.pose() != null && NParser.checkName(gob.pose(), "knock"))
            return false;
        CattleId cid = gob.getattr(CattleId.class);
		// no CattleId - not memorized
        if (cid == null) return true;
        return !visibleRoster.entries.containsKey(cid.id);
    }

    private String lnm;
    private int lgrp;
    private Tex rnm;
    public void draw(GOut g, Pipe state) {
	Coord sc = Homo3D.obj2view(new Coord3f(0, 0, 25), state, Area.sized(g.sz())).round2();
	if(sc.isect(Coord.z, g.sz())) {
	    Entry entry = entry();
	    int grp = (entry != null) ? entry.grp : 0;
	    String name = (entry != null) ? entry.name : null;
	    if((name != null) && ((rnm == null) || !name.equals(lnm) || (grp != lgrp))) {
		Color col = BuddyWnd.gc[grp];
		rnm = new TexI(Utils.outline2(Text.render(name, col).img, Utils.contrast(col)));
		lnm = name;
		lgrp = grp;
	    }
	    if((rnm != null) && (wnd != null) && wnd.visible) {
		Coord nmc = sc.sub(rnm.sz().x / 2, -rnm.sz().y);
		g.image(rnm, nmc);
		if((entry != null) && entry.mark.a)
		    g.image(CheckBox.smark, nmc.sub(CheckBox.smark.sz().x, 0));
	    }
	}
	updateMarkRing();
    }

    private void updateMarkRing() {
	Entry entry = entry();
	Gob.Overlay markOverlay = gob.findol(NCattleMarkRing.class);
	
	if((entry != null) && entry.mark.a) {
	    if(markOverlay == null) {
		gob.addcustomol(new NCattleMarkRing(gob));
	    }
	} else {
	    if(markOverlay != null) {
		markOverlay.remove();
	    }
	}
    }
}
