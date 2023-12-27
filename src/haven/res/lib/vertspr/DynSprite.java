/* Preprocessed source code */
package haven.res.lib.vertspr;

import java.util.*;
import haven.*;
import haven.render.*;
import haven.render.VertexArray.Layout;

@haven.FromResource(name = "lib/vertspr", version = 2)
public abstract class DynSprite extends Sprite implements Rendered {
    public static final Layout vf_pn  = new Layout(new Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 24),
						   new Layout.Input(Homo3D.normal, new VectorFormat(3, NumberFormat.FLOAT32), 0, 12, 24));
    public static final Layout vf_pnc = new Layout(new Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 40),
						   new Layout.Input(Homo3D.normal, new VectorFormat(3, NumberFormat.FLOAT32), 0, 12, 40),
						   new Layout.Input(VertexColor.color, new VectorFormat(4, NumberFormat.FLOAT32), 0, 24, 40));
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    public Model model = null;
    public VertexArray va = null;
    public Model.Indices indb = null;
    public Pipe.Op state = null;

    public DynSprite(Owner owner, Resource res) {
	super(owner, res);
    }

    public abstract Layout fmt();
    public abstract Model.Mode mode();

    public void draw(Pipe state, Render out) {
	if(model != null)
	    out.draw(state, model);
    }

    public void added(RenderTree.Slot slot) {
	if(state != null)
	    slot.ostate(state);
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }

    public void update(Render out, DataBuffer.Filler<? super VertexArray.Buffer> data, int dlen, DataBuffer.Filler<? super Model.Indices> ind, int ilen, int nv) {
	if((va != null) && ((va.bufs[0].size() < dlen) || (va.bufs[0].size() > dlen * 4))) {
	    va.dispose();
	    va = null;
	}
	if(va == null)
	    va = new VertexArray(fmt(), new VertexArray.Buffer(Math.max(Tex.nextp2(dlen), 256), DataBuffer.Usage.STREAM, null)).shared();
	if((indb != null) && ((ind == null) || (indb.size() < ilen * 2) || (indb.size() > ilen * 8))) {
	    indb.dispose();
	    indb = null;
	}
	if((indb == null) && (ind != null) && (ilen > 0))
	    indb = new Model.Indices(Math.max(Tex.nextp2(ilen * 2), 64), NumberFormat.UINT16, DataBuffer.Usage.STREAM, null).shared();
	if(data instanceof DataBuffer.PartFiller)
	    out.update(va.bufs[0], (DataBuffer.PartFiller<? super VertexArray.Buffer>)data, 0, dlen);
	else
	    out.update(va.bufs[0], data);
	if((ind != null) && (ilen > 0)) {
	    if(ind instanceof DataBuffer.PartFiller)
		out.update(indb, (DataBuffer.PartFiller<? super Model.Indices>)ind, 0, ilen * 2);
	    else
		out.update(indb, ind);
	}
	if(model != null) {
	    int isz = (model.ind == null) ? 0 : model.ind.size();
	    if((model.va != va) || (model.n != nv) || (model.ind != indb)) {
		model.dispose();
		model = null;
	    }
	}
	if(model == null) {
	    model = new Model(mode(), va, indb, 0, nv);
	    for(RenderTree.Slot slot : this.slots)
		slot.update();
	}
    }

    public void update(Render out, DataBuffer.Filler<? super VertexArray.Buffer> data, int dlen) {
	update(out, data, dlen, null, 0, dlen / fmt().inputs[0].stride);
    }

    public void update(Render out, byte[] data, short[] ind, int nv) {
	DataBuffer.Filler<DataBuffer> ifill = (ind == null) ? null : DataBuffer.Filler.of(ind);
	int ilen = (ind == null) ? 0 : ind.length;
	update(out, DataBuffer.Filler.of(data), data.length, ifill, ilen, nv);
    }
    public void update(Render out, byte[] data) {
	update(out, data, null, data.length / fmt().inputs[0].stride);
    }
    public void update(Render out, byte[] data, short[] ind) {
	update(out, data, ind, ind.length);
    }
    public void ostate(Pipe.Op... states) {
	Pipe.Op st = this.state = Pipe.Op.compose(states);
	for(RenderTree.Slot slot : this.slots)
	    slot.ostate(st);
    }

    public void dispose() {
	if(model != null) {
	    model.dispose();
	    model = null;
	}
	if(indb != null) {
	    indb.dispose();
	    indb = null;
	}
	if(va != null) {
	    va.dispose();
	    va = null;
	}
	super.dispose();
    }
}
