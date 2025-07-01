/* Preprocessed source code */
/* $use: ui/obj/buddy */

package haven.res.gfx.hud.mmap.plo;

import haven.*;
import haven.res.ui.obj.buddy.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.*;

@haven.FromResource(name = "gfx/hud/mmap/plo", version = 12)
public class Factory implements GobIcon.Icon.Factory {
    public GobIcon.Icon create(OwnerContext owner, Resource res, Message sdt) {
	if(!sdt.eom() && (sdt.uint8() != 0))
	    return(new DeadPlayer(owner, res));
	return(new Player(owner, res));
    }

    public Collection<GobIcon.Icon> enumerate(OwnerContext owner, Resource res, Message sdt) {
	Collection<GobIcon.Icon> ret = new ArrayList<>();
	for(int i = 0; i < BuddyWnd.gc.length; i++)
	    ret.add(new Player(owner, res, i));
	ret.add(new DeadPlayer(owner, res));
	return(ret);
    }
}
