/* Preprocessed source code */
/* $use: lib/mapres */

package haven.res.lib.itemtex;

import haven.*;
import static haven.Resource.imgc;
import haven.render.*;
import haven.res.lib.layspr.*;
import haven.res.lib.mapres.ResourceMap;
import org.json.*;

import java.util.*;
import java.awt.image.BufferedImage;

@haven.FromResource(name = "lib/itemtex", version = 2)
public class ItemTex {
    public static class Icon implements GSprite.Owner, Resource.Resolver {
	public final Resource res;
	final Resource.Resolver pool;

	Icon(Resource res, Resource.Resolver pool) {
	    this.res = res;
	    this.pool = pool;
	}

	public Indir<Resource> getres(int id) {
	    return(null);
	}

	static final ClassResolver<Icon> rsv = new ClassResolver<Icon>()
	    .add(Resource.Resolver.class, ico -> ico.pool);
	public <C> C context(Class<C> cl) {return(rsv.context(cl, this));}
	public Resource getres() {return(res);}
	public Random mkrandoom() {return(new Random());}
    }
    
    public static GSprite mkspr(OwnerContext owner, Message sdt) {
	int resid = sdt.uint16();
	Message isdt = Message.nil;
	if((resid & 0x8000) != 0) {
	    resid &= ~0x8000;
	    isdt = new MessageBuf(sdt.bytes(sdt.uint8()));
	}
	Resource ires = owner.context(Resource.Resolver.class).getres(resid).get();
	GSprite.Owner ctx = new Icon(ires, new ResourceMap(owner.context(Resource.Resolver.class), sdt));
	return(GSprite.create(ctx, ires, isdt));
    }

    public static BufferedImage sprimg(GSprite spr) {
	if(spr instanceof GSprite.ImageSprite)
	    return(((GSprite.ImageSprite)spr).image());
	return(spr.owner.getres().layer(imgc).img);
    }

    public static final Map<MessageBuf, BufferedImage> made = new CacheMap<>();
    public static BufferedImage create(OwnerContext owner, Message osdt) {
	MessageBuf copy = new MessageBuf(osdt.bytes());
	synchronized(made) {
	    BufferedImage ret = made.get(copy);
	    if(ret == null)
		made.put(copy, ret = sprimg(mkspr(owner, copy.clone())));
	    return(ret);
	}
    }

    public static BufferedImage fixsz(BufferedImage img) {
	Coord sz = PUtils.imgsz(img);
	int msz = Math.max(sz.x, sz.y);
	int nsz = Math.max((int)Math.round(Math.pow(2, Math.round(Math.log(sz.x) / Math.log(2)))),
			   (int)Math.round(Math.pow(2, Math.round(Math.log(sz.y) / Math.log(2)))));
	BufferedImage ret = TexI.mkbuf(new Coord(nsz, nsz));
	java.awt.Graphics g = ret.getGraphics();
	int w = (sz.x * nsz) / msz, h = (sz.y * nsz) / msz;
	g.drawImage(img, (nsz - w) / 2, (nsz - h) / 2, (nsz + w) / 2, (nsz + h) / 2, 0, 0, sz.x, sz.y, null);
	g.dispose();
	return(ret);
    }

    public static final Map<BufferedImage, TexL> fixed = new CacheMap<>();
    public static TexL fixup(final BufferedImage img) {
	TexL tex;
	synchronized(fixed) {
	    tex = fixed.get(img);
	    if(tex == null) {
		BufferedImage fimg = img;
		Coord sz = PUtils.imgsz(fimg);
		if((sz.x != sz.y) || (sz.x != Tex.nextp2(sz.x)) || (sz.y != Tex.nextp2(sz.y))) {
		    fimg = fixsz(fimg);
		    sz = PUtils.imgsz(fimg);
		}
		final BufferedImage timg = fimg;
		tex = new TexL(sz) {
			public BufferedImage fill() {
			    return(timg);
			}
		    };
		tex.mipmap(Mipmapper.dav);
		tex.img.magfilter(Texture.Filter.LINEAR).minfilter(Texture.Filter.LINEAR).mipfilter(Texture.Filter.LINEAR);
		fixed.put(img, tex);
	    }
	}
	return(tex);
    }

	public static BufferedImage create(JSONObject object) {
		if(object.has("layer"))
		{
			LinkedList<Indir<Resource>> layReses = new LinkedList<>();
			JSONArray jlayer = (JSONArray) object.get("layer");
			for(int i = 0 ; i < jlayer.length(); i++)
			{
				layReses.add(Resource.remote().loadwait((String) jlayer.get(i)).indir());
			}
			return new Layered(null, layReses).image();
		}
		else if (object.has("static"))
		{
			return Resource.remote().loadwait((String)object.get("static")).layer(imgc).img;
		}
		return null;
	}

	public static JSONObject save(GSprite spr){
		JSONObject res = new JSONObject();
		if(spr instanceof Layered)
		{
			JSONArray jlay = new JSONArray();
			Layered lspr = (Layered) spr;
			for (Layer lay : lspr.lay)
			{
				if (lay instanceof Image)
				{
					Resource.Image ilay = (Resource.Image) ((Image) lay).img;
					String name = ilay.getres().name;
					jlay.put(name);
				}
			}
			res.put("layer", jlay);
			return res;
		}
		else if(spr instanceof  StaticGSprite)
		{
			Resource.Image ilay = (Resource.Image) ((StaticGSprite) spr).img;
			String name = ilay.getres().name;
			res.put("static", name);
			return res;
		}
		return null;
	}
}
