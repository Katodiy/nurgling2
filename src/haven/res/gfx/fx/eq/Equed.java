/* Preprocessed source code */
package haven.res.gfx.fx.eq;

import haven.*;
import haven.render.*;
import java.util.function.*;

/*
  >spr: Equed
  >rlink: Equed
*/
@haven.FromResource(name = "gfx/fx/eq", version = 21)
public class Equed extends Sprite {
    private final Sprite espr;
    private final RenderTree.Node eqd;
    
    public Equed(Owner owner, Resource res, Sprite espr, Skeleton.BoneOffset bo) {
	super(owner, res);
	this.espr = espr;
	this.eqd = RUtils.StateTickNode.of(this.espr, bo.from(owner.fcontext(EquipTarget.class, false)));
    }

    public static Resource ctxres(Owner owner) {
	Gob gob = owner.context(Gob.class);
	if(gob == null)
	    throw(new RuntimeException("no context resource for owner " + owner));
	Drawable d = gob.getattr(Drawable.class);
	if(d == null)
	    throw(new RuntimeException("no drawable on object " +gob));
	return(d.getres());
    }

    public static Equed mksprite(Owner owner, Resource res, Message sdt) {
	Indir<Resource> eres = owner.context(Resource.Resolver.class).getres(sdt.uint16());
	int fl = sdt.uint8();
	String epn = sdt.string();
	Resource epres = ((fl & 1) == 0) ? (ctxres(owner)) : eres.get();
	Message sub = Message.nil;
	if((fl & 2) != 0)
	    sub = new MessageBuf(sdt.bytes(sdt.uint8()));
	Skeleton.BoneOffset bo = epres.layer(Skeleton.BoneOffset.class, epn);
	if(bo == null)
	    throw(new RuntimeException("No such bone-offset in " + epres.name + ": " + epn));
	return(new Equed(owner, res, Sprite.create(owner, eres.get(), sub), bo));
    }

    public static RenderLink mkrlink(Resource res, Object... args) {
	KeywordArgs p = new KeywordArgs(args, res.pool, "eq", "from", "@res", "args");
	String epn = (String)p.get("eq");
	String epf = (String)p.get("from", "");
	Resource eres = (Resource)((Indir)p.get("res")).get();
	Function<Owner, Skeleton.BoneOffset> ep;
	switch((String)p.get("from", "")) {
	case "l": case "linked": {
	    Skeleton.BoneOffset bo = eres.flayer(Skeleton.BoneOffset.class, epn);
	    ep = owner -> bo;
	    break;
	}
	case "c": case "object": {
	    ep = owner -> ctxres(owner).flayer(Skeleton.BoneOffset.class,epn);
	    break;
	}
	case "o": case "this": default: {
	    Skeleton.BoneOffset bo = res.flayer(Skeleton.BoneOffset.class, epn);
	    ep = owner -> bo;
	    break;
	}
	}
	Sprite.Mill<?> mill;
	Object targs = p.get("args", null);
	if((targs instanceof byte[])) {
	    mill = owner -> Sprite.create(owner, eres, new MessageBuf((byte[])targs));
	} else if((targs instanceof Object[])) {
	    RenderLink rl = eres.getcode(RenderLink.ArgLink.class, true).parse(res, (Object[])targs);
	    mill = owner -> {
		RenderTree.Node n = rl.make(owner);
		if(!(n instanceof Sprite))
		    throw(new Sprite.ResourceException("Sublink returned non-sprite node " + String.valueOf(n), eres));
		return((Sprite)n);
	    };
	} else {
	    mill = owner -> Sprite.create(owner, eres, Message.nil);
	}
	return(owner -> new Equed(owner, res, mill.create(owner), ep.apply(owner)));
    }
    
    public void added(RenderTree.Slot slot) {
	slot.add(eqd);
    }
    
    public boolean tick(double dt) {
	espr.tick(dt);
	return(false);
    }

    public void age() {
	espr.age();
    }
}
