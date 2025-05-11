/* Preprocessed source code */
package haven.res.ui.tt.attrmod;

import haven.*;
import static haven.PUtils.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.BufferedImage;

@Resource.PublishedCode(name = "attrmod")
@haven.FromResource(name = "ui/tt/attrmod", version = 12)
public class pmattr extends resattr {
    public final int dec;

    public pmattr(Resource res, Object... args) {
	super(res);
	dec = (args.length > 0) ? Utils.iv(args[0]) : 1;
    }

    public String format(double val) {
	String bval = (Math.abs(val) >= 10000) ?
	    String.format("%s\u00d7", Utils.odformat2(Math.abs(val) * 0.001, dec)) :
	    String.format("%s%%", Utils.odformat2(Math.abs(val) * 0.1, dec));
	return(String.format("%s{%s%s}",
			     RichText.Parser.col2a((val < 0) ? debuff : buff),
			     (val < 0) ? "-" : "+", bval));
    }
}
