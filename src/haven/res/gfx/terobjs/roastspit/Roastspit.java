/* Preprocessed source code */
package haven.res.gfx.terobjs.roastspit;

import haven.*;
import haven.render.*;
import java.util.*;
import java.util.function.*;

/* >spr: Roastspit */
@haven.FromResource(name = "gfx/terobjs/roastspit", version = 56)
public class Roastspit extends ModSprite {
    private Equed equed;

    public Roastspit(Owner owner, Resource res, Message sdt) {
	super(owner, res, sdt);
    }

    protected void decdata(Message sdt) {
	flags = sdt.eom() ? 0 : sdt.uint8();
	int eqid = sdt.eom() ? 65535 : sdt.uint16();
	Resource eqres = (eqid == 65535) ? null : owner.context(Resource.Resolver.class).getres(eqid).get();
	equed = (eqres == null) ? null : new Equed(eqres);
    }

    public class Equed implements Mod {
	public final Resource res;
	private RenderTree.Node equed = null;

	public Equed(Resource res) {
	    this.res = res;
	}

	public void operate(Cons cons) {
	    if(equed == null) {
		Sprite eqs = Sprite.create(owner, res, Message.nil);
		Skeleton.BoneOffset bo = res.layer(Skeleton.BoneOffset.class, "s");
		equed = RUtils.StateTickNode.of(eqs, bo.from(Roastspit.this));
	    }
	    cons.add(new Part(equed));
	}

	public int order() {
	    return(2000);
	}
    }

    protected void modifiers(Cons cons) {
	super.modifiers(cons);
	if(equed != null)
	    cons.add(equed);
    }

	public String getContent() {
		return equed != null ? equed.toString() : null;
	}
}
