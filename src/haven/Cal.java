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

import static java.lang.Math.PI;
import nurgling.widgets.*;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class Cal extends Widget {
    public static final double hbr = UI.scale(20.0);
    static final Tex bg = Resource.loadtex("nurgling/hud/calendar/glass");
    static final Tex[] dlnd = new Tex[4];
    static final Tex[] nlnd = new Tex[4];
    static final Resource.Image dsky = Resource.loadrimg("gfx/hud/calendar/daysky");
    static final Resource.Image nsky = Resource.loadrimg("gfx/hud/calendar/nightsky");
    static final Resource.Anim sun = Resource.local().loadwait("gfx/hud/calendar/sun").layer(Resource.animc);
    static final Resource.Anim moon = Resource.local().loadwait("gfx/hud/calendar/moon").layer(Resource.animc);

	static final HashMap<String,TexI> events = new HashMap<>();
	static {
	events.put("dawn",new TexI(Resource.loadimg("nurgling/hud/cal/dawn")));
	events.put("mantle",new TexI(Resource.loadimg("nurgling/hud/cal/mantle")));
	events.put("wolf",new TexI(Resource.loadimg("nurgling/hud/cal/wolf")));
	events.put("rain",new TexI(Resource.loadimg("nurgling/hud/cal/rain")));
	}

	ArrayList<String> eventNames = new ArrayList<>();
	String weather = null;

    static {
	for(int i = 0; i < dlnd.length; i++) {
	    dlnd[i] = Resource.loadtex(String.format("gfx/hud/calendar/dayscape-%d", i));
	    nlnd[i] = Resource.loadtex(String.format("gfx/hud/calendar/nightscape-%d", i));
	}
    }

    public Cal() {
	super(bg.sz().mul(2));
    }

    public void draw(GOut g) {
	Astronomy a = ui.sess.glob.ast;
	long now = System.currentTimeMillis();
	Coord center = sz.div(2).sub(bg.sz().div(2));
	g.image(a.night ? nsky : dsky, center);
	int mp = (int)Math.round(a.mp * (double)moon.f.length) % moon.f.length;
	Resource.Image moon = Cal.moon.f[mp][0];
	Resource.Image sun = Cal.sun.f[(int)((now / Cal.sun.d) % Cal.sun.f.length)][0];
	Coord mc = Coord.sc((a.dt + 0.25) * 2 * PI, hbr).add(sz.div(2)).sub(moon.ssz.div(2));
	Coord sc = Coord.sc((a.dt + 0.75) * 2 * PI, hbr).add(sz.div(2)).sub(sun.ssz.div(2));
	g.chcolor(a.mc);
	g.image(moon, mc);
	g.chcolor();
	g.image(sun, sc);

	g.image((a.night ? nlnd : dlnd)[a.is], center);
	g.image(bg, center);

	Coord2d dir = UI.scale(new Coord2d(50,-15));
	int count = 0;
	for(String key : eventNames) {
		g.aimage(events.get(key), dir.floor().add(sz.div(2)),0.5,0.5);
		count +=1;
		if(count>1)
		{
			count = 0;
			dir.y +=30;
		}
		dir = UI.scale(new Coord2d(50+(30*count),dir.y));
	}
    }

    public boolean checkhit(Coord c) {
	return(Utils.checkhit(dsky.scaled(), c.sub(dsky.o)));
    }

    private static String ord(int i) {
	if(((i % 100) / 10) != 1) {
	    if((i % 10) == 1)
		return(i + "st");
	    else if((i % 10) == 2)
		return(i + "nd");
	    else if((i % 10) == 3)
		return(i + "rd");
	}
	return(i + "th");
    }

	@Override
	public void tick(double dt) {
		super.tick(dt);
		eventNames.clear();
		Astronomy a = ui.sess.glob.ast;
		int mp = (int)Math.round(a.mp * (double)moon.f.length) % moon.f.length;
		if(Astronomy.phase[mp].equals("Full Moon"))
			eventNames.add("wolf");
		int curinmin = a.hh*60+a.mm;
		if(curinmin>=285&&curinmin<=435)
		{
			eventNames.add("mantle");
			eventNames.add("dawn");
		}
		if(weather!=null && (weather.contains("rain") || weather.contains("wet")))
			eventNames.add("rain");

	}

	private String tip = null;
	public Object tooltip(Coord c, Widget prev) {
        Astronomy a = ui.sess.glob.ast;
        int mp = (int)Math.round(a.mp * (double)moon.f.length) % moon.f.length;
        String season = String.format("Season: %s, day %d of %d", a.season(), a.scday + 1, a.season().length);
        int day = (int) Math.floor(a.md) + 1, month = (int) Math.floor(a.ym) + 1, year = (int) Math.floor(a.years) + 1;
        String tt = String.format("%02d-%02d-%02d, %02d:%02d\n%s\nMoon: %s", day, month, year, a.hh, a.mm, season, Astronomy.phase[mp]);
        if(!tt.equals(tip)) {
            tip = tt;
            tooltip = RichText.render(tt, UI.scale(250));
        }
        return tooltip;
	}

	public void setWeather(String resnm) {
		weather = resnm;
	}
}
