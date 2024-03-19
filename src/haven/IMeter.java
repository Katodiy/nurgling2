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

import nurgling.*;

import java.awt.Color;
import java.util.*;

public class IMeter extends LayerMeter {
	public String name;
	Tex text = null;
    public static final Coord off = UI.scale(24, 4);
    public static final Coord fsz = UI.scale(190, 48);
    public static final Coord ssz = UI.scale(145, 48);
    public static final Coord msz = UI.scale(130, 20);
    public final Indir<Resource> bg;

    @RName("im")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Indir<Resource> bg = ui.sess.getresv(args[0]);
        String resnm = ui.sess.rescache.get((Integer)args[0]).resnm;
        if(resnm!=null)
        {
            String key = resnm.substring(resnm.lastIndexOf("/")+1);
            switch (key)
            {
                case "hp":
                    bg = Resource.remote().load("nurgling/hud/meter/hp");
                    break;
                case "stam":
                    bg = Resource.remote().load("nurgling/hud/meter/stam");
                    break;
                case "nrj":
                    bg = Resource.remote().load("nurgling/hud/meter/nrj");
                    break;
                case "mount":
                    bg = Resource.remote().load("nurgling/hud/meter/mount");
                    break;
                case "boat":
                    bg = Resource.remote().load("nurgling/hud/meter/boat");
                    break;
                case "häst":
                    bg = Resource.remote().load("nurgling/hud/meter/hast");
                    break;
            }
        }

	    List<Meter> meters = decmeters(args, 1);
		IMeter result = new IMeter(bg, meters);
		result.name = resnm;
		if(resnm!= null && result.name.endsWith("st"))
			result.name = "gfx/hud/meter/hast";
	    return(result);
	}
    }

    public IMeter(Indir<Resource> bg, List<Meter> meters) {
	super(fsz);
	this.bg = bg;
	set(meters);
    }

    public void draw(GOut g) {
	try {
	    Tex bg = this.bg.get().flayer(Resource.imgc).tex();
	    g.chcolor(0, 0, 0, 255);
	    g.frect(off, msz);
	    g.chcolor();
	    for(Meter m : meters) {
		int w = msz.x;
		w = (int)Math.ceil(w * m.a);
		g.chcolor(m.c);
		g.frect(off, new Coord(w, msz.y));
	    }
	    g.chcolor();
	    g.image(bg, Coord.z);
		if(text!=null)
		{
			g.image(text,new Coord(off.x + msz.x/2 -text.sz().x/2,off.y + msz.y/2 -text.sz().y/2));
		}
	} catch(Loading l) {
	}
    }

	@Override
	public void uimsg(String msg, Object... args)
	{
		if(msg == "tip") {
			String val = (String) args[0];
			if(val!=null)
			{
				String key = val.substring(0, val.indexOf(":"));
				switch (key)
				{
					case "Stamina":
					case "Satiety":
					case "Pony Power":
					case "Seaworthiness":
						text = NStyle.meter.render(val.substring(val.indexOf(":")+1)).tex();
						break;
					case "Health":
						text = NStyle.meter.render(val.substring(val.indexOf(":")+1).replace("/", " / ")).tex();
						break;
					case "Energy":
						text = NStyle.meter.render(val.substring(val.indexOf(":")+1, val.lastIndexOf("%")+1)).tex();
						break;

				}
			}
		}
		super.uimsg(msg, args);
	}
}
