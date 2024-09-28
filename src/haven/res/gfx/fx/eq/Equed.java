/* Preprocessed source code */
package haven.res.gfx.fx.eq;

import haven.*;
import haven.render.*;

/*
  >spr: Equed
  >rlink: Equed
*/
@haven.FromResource(name = "gfx/fx/eq", version = 18)
public class Equed extends Sprite {
    private final Sprite espr;
    private final RenderTree.Node eqd;
    
    public Equed(Owner owner, Resource res, Sprite espr, Skeleton.BoneOffset bo) {
	super(owner, res);
	this.espr = espr;
	this.eqd = RUtils.StateTickNode.from(this.espr, bo.from(owner.fcontext(EquipTarget.class, false)));
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

    public static Equed mkrlink(Owner owner, Resource res, Object... args) {
	String epn = (String)args[0];
	String fl = (String)args[1];
	Resource eres = res.pool.load((String)args[2], (Integer)args[3]).get();
	Resource epres;
	if(fl.indexOf('l') >= 0)
	    epres = eres;
	else if(fl.indexOf("c") >= 0)
	    epres = ctxres(owner);
	else if(fl.indexOf("o") >= 0)
	    epres = res;
	else
	    epres = res;
	Sprite espr;
	if((args.length > 4) && (args[4] instanceof byte[])) {
	    espr = Sprite.create(owner, eres, new MessageBuf((byte[])args[4]));
	} else if((args.length > 4) && (args[4] instanceof Object[])) {
	    RenderTree.Node n = eres.getcode(RenderLink.ArgLink.class, true).create(owner, res, (Object[])args[4]);
	    if(!(n instanceof Sprite))
		throw(new Sprite.ResourceException("Sublink returned non-sprite node " + String.valueOf(n), eres));
	    espr = (Sprite)n;
	} else {
	    espr = Sprite.create(owner, eres, Message.nil);
	}
	Skeleton.BoneOffset bo = epres.layer(Skeleton.BoneOffset.class, epn);
	if(bo == null)
	    throw(new RuntimeException("No such bone-offset in " + epres.name + ": " + epn));
	return(new Equed(owner, res, espr, bo));
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
