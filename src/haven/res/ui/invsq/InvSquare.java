package haven.res.ui.invsq;/* Preprocessed source code */
import haven.*;

/* >wdg: haven.res.ui.invsq.InvSquare */
@haven.FromResource(name = "ui/invsq", version = 4)
public class InvSquare extends Img {
    public InvSquare() {
	super(Inventory.invsq);
    }

    public static Widget mkwidget(UI ui, Object... args) {
	InvSquare ret = new InvSquare();
	if(args.length > 0)
	    ret.hit = (Integer)args[0] != 0;
	return(ret);
    }

    public boolean checkhit(Coord c) {
	return(true);
    }
}
