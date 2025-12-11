/* Preprocessed source code */
package haven.res.ui.tt.ingred;

import haven.*;
import java.util.*;
import java.awt.image.BufferedImage;

/* >tt: Ingredient */
@haven.FromResource(name = "ui/tt/ingred", version = 27)
public class Ingredient extends ItemInfo.Tip {
    public final String name;
    public final Double val;
    public final String resName;  // Resource name for unique identification (e.g., "gfx/invobjs/meat-lynx")

    public Ingredient(Owner owner, String name, Double val, String resName) {
	super(owner);
	this.name = name;
	this.val = val;
	this.resName = resName;
    }

    public Ingredient(Owner owner, String name, Double val) {
	this(owner, name, val, null);
    }

    public Ingredient(Owner owner, String name) {
	this(owner, name, null, null);
    }

    public static Ingredient mkinfo(Owner owner, Object... args) {
	int a = 1;
	String name;
	String resName = null;
	if(args[a] instanceof String) {
	    name = (String)args[a++];
	} else if(args[1] instanceof Integer) {
	    Indir<Resource> res = owner.context(Resource.Resolver.class).getres((Integer)args[a++]);
	    Message sdt = Message.nil;
	    if((args.length > a) && (args[a] instanceof byte[]))
		sdt = new MessageBuf((byte[])args[a++]);
	    ItemSpec spec = new ItemSpec(owner, new ResData(res, sdt), null);
	    name = spec.name();
	    // Capture the resource name for unique identification
	    try {
		resName = res.get().name;
	    } catch (Exception e) {
		// If we can't get the resource name, leave it null
	    }
	} else {
	    throw(new IllegalArgumentException());
	}
	Double val = null;
	if(args.length > a)
	    val = (args[a] == null)?null:((Number)args[a]).doubleValue();
	return(new Ingredient(owner, name, val, resName));
    }

    public static class Line extends Tip {
	final List<Ingredient> all = new ArrayList<Ingredient>();

	Line(Owner owner) {super(owner);}

	public BufferedImage tipimg() {
	    StringBuilder buf = new StringBuilder();
	    Collections.sort(all, (a, b) -> a.name.compareTo(b.name));
	    buf.append("Made with ");
	    buf.append(all.get(0).descr());
	    if(all.size() > 2) {
		for(int i = 1; i < all.size() - 1; i++) {
		    buf.append(", ");
		    buf.append(all.get(i).descr());
		}
	    }
	    if(all.size() > 1) {
		buf.append(" and ");
		buf.append(all.get(all.size() - 1).descr());
	    }
	    return(RichText.render(buf.toString(), UI.scale(250)).img);
	}
    }
    public static final Layout.TipID<Line> id = Line::new;

    public void prepare(Layout l) {
	l.intern(id).all.add(this);
    }

    public String descr() {
	if(val == null)
	    return(name);
	return(String.format("%s (%d%%)", name, (int)Math.floor(val * 100.0)));
    }
}
