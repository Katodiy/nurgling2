/* Preprocessed source code */
package haven.res.ui.tt.ameter;

import haven.*;
import java.awt.Color;

/* >tt: AMeter */
@haven.FromResource(name = "ui/tt/ameter", version = 3)
public class AMeter extends Buff.AMeterTip {
    public final double m;

    public AMeter(Owner owner, double m) {
	super(owner);
	this.m = m;
    }

    public static AMeter mkinfo(Owner owner, Object... args) {
	double m;
	if(args[1] instanceof Integer)
	    m = ((Integer)args[1]) / 100.0;
	else
	    m = ((Number)args[1]).doubleValue();
	return(new AMeter(owner, m));
    }

    public double ameter() {
	return(m);
    }
}
