package haven.res.ui.gobcp;/* Preprocessed source code */
import haven.*;
import haven.render.*;
import java.util.*;

/* >spr: haven.res.ui.gobcp.Gobcopy */
@haven.FromResource(name = "ui/gobcp", version = 4)
public class Gobcopy extends Sprite {
    public final long id;
    private final OCache oc;
    private final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    public Gob gob;
    
    public Gobcopy(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	id = sdt.uint32();
	oc = owner.context(Glob.class).oc;
	gob = oc.getgob(id);
    }

    private void parts(RenderTree.Slot slot) {
	if(gob != null)
	    slot.add(gob);
    }

    public void added(RenderTree.Slot slot) {
	parts(slot);
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }

    public boolean tick(double dt) {
	Gob ngob = oc.getgob(id), pgob = this.gob;
	if(ngob != pgob) {
	    gob = ngob;
	    try {
		RUtils.readd(slots, this::parts, () -> gob = pgob);
	    } catch(Loading l) {
	    }
	}
	return(false);
    }
}
