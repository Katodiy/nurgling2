/* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.ModSprite.*;
import java.util.*;
import java.util.function.Consumer;

@haven.FromResource(name = "lib/vmat", version = 39)
public class VarSprite extends ModSprite {
	public final Optional<Gob> gob = (owner instanceof Gob) ? Optional.of((Gob)owner) : owner.ocontext(Gob.class);
	private Mapping cmats;
    public VarSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res, sdt);
	// Add custom material override for ModSprite system
	imod(new MaterialOverride());
    }

    public Mapping mats() {
	return(gob.map(gob -> gob.getattr(Mapping.class)).orElse(Mapping.empty));
    }

//    public void iparts(int mask, Collection<RenderTree.Node> rbuf, Collection<Runnable> tbuf, Collection<Consumer<Render>> gbuf) {
//	Mapping mats = (this.cmats == null) ? Mapping.empty : cmats;
//	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
//	    String sid = mr.rdat.get("vm");
//	    int mid = (sid == null) ? -1 : Integer.parseInt(sid);
//	    if(((mr.mat != null) || (mid >= 0)) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
//		rbuf.add(animwrap(new Wrapping(mr.m, mats.mergemat(mr.mat.get(), mid), mid), tbuf, gbuf));
//	}
//	Owner rec = null;
//	for(RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
//	    if((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
//		if(rec == null)
//		    rec = new RecOwner();
//		RenderTree.Node r = lr.l.make(rec);
//		if(r instanceof Pipe.Op.Wrapping)
//		    r = animwrap((Pipe.Op.Wrapping)r, tbuf, gbuf);
//		rbuf.add(r);
//	    }
//	}
//    }

    public boolean tick(double dt) {
	Mapping bmats = mats(), mats;

	if(gob.isEmpty()) {
		mats = bmats;
	} else {
		Mapping customMats = gob.get().ngob.mats(bmats);
		if(customMats == null) {
			mats = bmats;
		} else {
			mats = customMats;
		}
	}

	Mapping pmats = this.cmats;
	if(mats != pmats) {
	    try {
		this.cmats = mats;
		update();
	    } catch(Loading l) {
		this.cmats = pmats;
	    }
	}
	return(super.tick(dt));
    }

    /**
     * Custom ModSprite Mod that overrides mesh materials with our custom materials.
     * This replaces the old iparts() method and integrates with the new ModSprite architecture.
     */
    private class MaterialOverride implements Mod {
        @Override
        public void operate(Cons cons) {
            // Apply custom materials to mesh parts if we have custom materials
            if (cmats != null && cmats instanceof Materials) {
                Materials customMats = (Materials) cmats;

                // Override materials for each mesh part
                for (Part part : cons.parts) {
                    if (part.obj instanceof FastMesh) {
                        FastMesh mesh = (FastMesh) part.obj;

                        // Try to find the mesh resource to get its material slot ID
                        for (FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
                            if (mr.m == mesh) {
                                String sid = mr.rdat.get("vm");
                                int mid = (sid == null) ? -1 : Integer.parseInt(sid);

                                // Apply custom material if available for this slot
                                if (mid >= 0 && customMats.mats.containsKey(mid)) {
                                    Material customMat = customMats.mats.get(mid);
                                    if (customMat != null) {
                                        // Replace the material in the part
                                        part.state.clear(); // Clear existing material state
                                        part.state.add(customMat); // Add our custom material
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public int order() {
            return 5000; // Run after mesh creation but before final rendering
        }
    }
}
