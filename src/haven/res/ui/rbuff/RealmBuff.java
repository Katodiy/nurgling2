package haven.res.ui.rbuff;/* Preprocessed source code */
/* $use: ui/tt/attrmod */

import haven.*;
import java.util.*;
import java.awt.image.BufferedImage;

/* >wdg: RealmBuff */
@haven.FromResource(name = "ui/rbuff", version = 21)
public class RealmBuff extends Buff implements ItemInfo.ResOwner {
    public final Indir<Resource> res;
    public Object[] rawinfo;

    public RealmBuff(Indir<Resource> res) {
	super(res);
	this.res = res;
    }

    public static Widget mkwidget(UI ui, Object... args) {
	Indir<Resource> res = ui.sess.getres((Integer)args[0]);
	return(new RealmBuff(res));
    }

    public Resource resource() {return(res.get());}

    public static final ClassResolver<UI> uictx = new ClassResolver<UI>()
	.add(Glob.class, ui -> ui.sess.glob)
	.add(Session.class, ui -> ui.sess);
    public <C> C context(Class<C> cl) {
	return(uictx.context(cl, ui));
    }

    private List<ItemInfo> info = Collections.emptyList();
    public List<ItemInfo> info() {
	if(info == null)
	    info = ItemInfo.buildinfo(this, rawinfo);
	return(info);
    }

    private Tex rtip;
    public Object tooltip(Coord c, Widget prev) {
	try {
	    if(rtip == null) {
		List<ItemInfo> info = info();
		BufferedImage img = ItemInfo.longtip(info);
		Resource.Pagina pag = res.get().layer(Resource.pagina);
		if(pag != null)
		    img = ItemInfo.catimgs(0, img, RichText.render("\n" + pag.text, UI.scale(200)).img);
		rtip = new TexI(img);
	    }
	    return(rtip);
	} catch(Loading l) {
	    return("...");
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "tt") {
	    rawinfo = args;
	    info = null;
	    rtip = null;
	} else {
	    super.uimsg(msg, args);
	}
    }
}
