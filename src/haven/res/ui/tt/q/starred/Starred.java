/* Preprocessed source code */
/* $use: ui/tt/q/qbuff */
/* $use: ui/tt/q/quality */

package haven.res.ui.tt.q.starred;

import haven.*;
import haven.res.ui.tt.q.qbuff.*;
import haven.res.ui.tt.q.quality.*;

/* >tt: Starred */
@haven.FromResource(name = "ui/tt/q/starred", version = 3)
public class Starred extends ItemInfo.Tip implements QBuff.Modifier {
    public Starred(Owner owner) {
	super(owner);
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new Starred(owner));
    }

    public void prepare(Layout l) {
	l.intern(QBuff.lid).mods.add(this);
    }

    public void prepare(QBuff.QList ql) {
	for(QBuff q : ql.ql) {
	    if(q instanceof Quality) {
		q.icon = Resource.classres(Starred.class).layer(Resource.imgc).scaled();
	    }
	}
    }

    public Tip shortvar() {
	return(new Tip(owner) {
		public void prepare(Layout l) {
		    l.intern(QBuff.sid).mods.add(Starred.this);
		}
	    });
    }
}
