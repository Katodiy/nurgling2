/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.WritableRaster;
import haven.render.Location;
import static haven.Inventory.invsq;

import nurgling.*;
import nurgling.widgets.*;

public class GameUI extends ConsoleHost implements Console.Directory, UI.MessageWidget {
    private static final int blpw = UI.scale(142), brpw = UI.scale(142);
    public final String chrid, genus;
    public final long plid;
    public Widget portrait;
    public MenuGrid menu;
    public MapView map;
    public GobIcon.Settings iconconf;
    public MiniMap mmap;
    public Fightview fv;
    protected List<Widget> meters = new LinkedList<Widget>();
    private Text lastmsg;
    private double msgtime;
    private Window invwnd;
    public Window equwnd;
    private Window makewnd;
    private Window srchwnd;
    public Window iconwnd;
    private Coord makewndc = Utils.getprefc("makewndc", new Coord(400, 200));
    public Inventory maininv;
    public CharWnd chrwdg;
    public NMapWnd mapfile;
    private Widget qqview;
    public BuddyWnd buddies;
    public final NZergwnd zerg;
    public NAreasWidget areas;
    public final Collection<Polity> polities = new ArrayList<Polity>();
    public HelpWnd help;
    public OptWnd opts;
    public Collection<DraggedItem> hand = new LinkedList<DraggedItem>();
    public WItem vhand;
    public ChatUI chat;
    public ChatUI.Channel syslog;
    public Progress prog = null;
    private boolean afk = false;
    public BeltSlot[] belt = new BeltSlot[144];
    public final Map<Integer, String> polowners = new HashMap<Integer, String>();
    public Bufflist buffs;
	public NMiniMapWnd mmapw = null;
    public static abstract class BeltSlot {
	public final int idx;

	public BeltSlot(int idx) {
	    this.idx = idx;
	}

	public abstract void draw(GOut g);
	public abstract void use(MenuGrid.Interaction iact);
    }

    private static final OwnerContext.ClassResolver<ResBeltSlot> beltctxr = new OwnerContext.ClassResolver<ResBeltSlot>()
	.add(GameUI.class, slot -> slot.wdg())
	.add(Glob.class, slot -> slot.wdg().ui.sess.glob)
	.add(Session.class, slot -> slot.wdg().ui.sess);
    public class ResBeltSlot extends BeltSlot implements GSprite.Owner, RandomSource {
	public final ResData rdt;

	public ResBeltSlot(int idx, ResData rdt) {
	    super(idx);
	    this.rdt = rdt;
	}

	private GSprite spr = null;
	public GSprite spr() {
	    GSprite ret = this.spr;
	    if(ret == null)
		ret = this.spr = GSprite.create(this, rdt.res.get(), new MessageBuf(rdt.sdt));
	    return(ret);
	}

	public void draw(GOut g) {
	    try {
		spr().draw(g);
	    } catch(Loading l) {}
	}

	public void use(MenuGrid.Interaction iact) {
	    Object[] args = {idx, iact.btn, iact.modflags};
	    if(iact.mc != null) {
		args = Utils.extend(args, iact.mc.floor(OCache.posres));
		if(iact.click != null)
		    args = Utils.extend(args, iact.click.clickargs());
	    }
	    GameUI.this.wdgmsg("belt", args);
	}

	public Resource getres() {return(rdt.res.get());}
	public Random mkrandoom() {return(new Random(System.identityHashCode(this)));}
	public <T> T context(Class<T> cl) {return(beltctxr.context(cl, this));}
	private GameUI wdg() {return(GameUI.this);}
    }

    public static class PagBeltSlot extends BeltSlot {
	public final MenuGrid.Pagina pag;

	public PagBeltSlot(int idx, MenuGrid.Pagina pag) {
	    super(idx);
	    this.pag = pag;
	}

	public void draw(GOut g) {
	    try {
		MenuGrid.PagButton btn = pag.button();
		btn.draw(g, btn.spr());
	    } catch(Loading l) {
	    }
	}

	public void use(MenuGrid.Interaction iact) {
	    try {
		pag.scm.use(pag.button(), iact, false);
	    } catch(Loading l) {
	    }
	}

	public static MenuGrid.Pagina resolve(MenuGrid scm, Indir<Resource> resid) {
	    Resource res = resid.get();
	    Resource.AButton act = res.layer(Resource.action);
	    /* XXX: This is quite a hack. Is there a better way? */
	    if((act != null) && (act.ad.length == 0))
		return(scm.paginafor(res.indir()));
	    return(scm.paginafor(resid));
	}
    }

    /* XXX: Remove me */
    public BeltSlot mkbeltslot(int idx, ResData rdt) {
	Resource res = rdt.res.get();
	Resource.AButton act = res.layer(Resource.action);
	if(act != null) {
	    if(act.ad.length == 0)
		return(new PagBeltSlot(idx, menu.paginafor(res.indir())));
	    return(new PagBeltSlot(idx, menu.paginafor(rdt.res)));
	}
	return(new ResBeltSlot(idx, rdt));
    }

    public abstract class Belt extends Widget implements DTarget, DropTarget {
	public Belt(Coord sz) {
	    super(sz);
	}

	public void act(int idx, MenuGrid.Interaction iact) {
	    if(belt[idx] != null)
		belt[idx].use(iact);
	}

	public void keyact(int slot) {
	    if(map != null) {
		BeltSlot si = belt[slot];
		Coord mvc = map.rootxlate(ui.mc);
		if(mvc.isect(Coord.z, map.sz)) {
		    map.new Hittest(mvc) {
			    protected void hit(Coord pc, Coord2d mc, ClickData inf) {
				act(slot, new MenuGrid.Interaction(1, ui.modflags(), mc, inf));
			    }
			    
			    protected void nohit(Coord pc) {
				act(slot, new MenuGrid.Interaction(1, ui.modflags()));
			    }
			}.run();
		}
	    }
	}

	public abstract int beltslot(Coord c);

	public boolean mousedown(Coord c, int button) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(button == 1)
		    act(slot, new MenuGrid.Interaction(1, ui.modflags()));
		if(button == 3)
		    GameUI.this.wdgmsg("setbelt", slot, null);
		return(true);
	    }
	    return(super.mousedown(c, button));
	}

	public boolean drop(Coord c, Coord ul) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		GameUI.this.wdgmsg("setbelt", slot, 0);
		return(true);
	    }
	    return(false);
	}

	public boolean iteminteract(Coord c, Coord ul) {return(false);}

	public boolean dropthing(Coord c, Object thing) {
	    int slot = beltslot(c);
	    if(slot != -1) {
		if(thing instanceof MenuGrid.Pagina) {
		    MenuGrid.Pagina pag = (MenuGrid.Pagina)thing;
		    try {
			if(pag.id instanceof Indir)
			    GameUI.this.wdgmsg("setbelt", slot, "res", pag.res().name);
			else
			    GameUI.this.wdgmsg("setbelt", slot, "pag", pag.id);
		    } catch(Loading l) {
		    }
		    return(true);
		}
	    }
	    return(false);
	}
    }

    @RName("gameui")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    String chrid = (String)args[0];
	    long plid = Utils.uiv(args[1]);
	    String genus = "";
	    if(args.length > 2)
		genus = (String)args[2];
	    return(new NGameUI(chrid, plid, genus, (NUI)ui));
	}
    }
	NResizableWidget chatwdg;
    private final Coord minimapc;
    private final Coord menugridc;
    public GameUI(String chrid, long plid, String genus, NUI nui) {
	this.chrid = chrid;
	nui.sessInfo.characterInfo = add(new NCharacterInfo(chrid, nui));
	this.plid = plid;
	this.genus = genus;
	setcanfocus(true);
	setfocusctl(true);

	chat = new ChatUI();
	chatwdg = new NResizableWidget(chat,"ChatUI",new Coord(400,200));
	add(chatwdg);
	add(new MapMenu(), 0, 0);
	minimapc = new Coord(UI.scale(4), UI.scale(34));
	Tex rbtnbg = Resource.loadtex("gfx/hud/csearch-bg");
	Img brframe = add(new Img(Resource.loadtex("gfx/hud/brframe")), rbtnbg.sz().x - UI.scale(22), 0);
	brframe.hide();
	menugridc = brframe.c.add(UI.scale(20), UI.scale(34));
	Img rbtnimg =add(new Img(rbtnbg), 0, sz.y - rbtnbg.sz().y);
	rbtnimg.hide();
	add(new NDraggableWidget(new MainMenu(), "mainmenu", UI.scale(280,52)));
	menubuttons(rbtnimg);
	portrait = add(new NDraggableWidget(Frame.with(new Avaview(Avaview.dasz, plid, "avacam"), false),"portrait", UI.scale(120, 108)));
	add(new NDraggableWidget(buffs = new Bufflist(),"bufflist",Coord.z));
	add(new NDraggableWidget(new Cal(),"Calendar",UI.scale(160,90)));
	syslog = chat.add(new ChatUI.Log("System"));
	opts = add(new OptWnd());
	opts.hide();
	zerg = add(new NZergwnd(), Utils.getprefc("wndc-zerg", UI.scale(new Coord(187, 50))));
	zerg.hide();
	add(areas = new NAreasWidget(),new Coord(sz.x/2 - NGUIInfo.xs/2,sz.y/5 ));
	areas.hide();
    }

    protected void attached() {
	iconconf = loadiconconf();
	super.attached();
    }

    public static final KeyBinding kb_srch = KeyBinding.get("scm-srch", KeyMatch.forchar('Z', KeyMatch.C));
    private void menubuttons(Widget bg) {
	add(new MenuButton("csearch", kb_srch, "Search actions...") {
		public void click() {
		    if(menu == null)
			return;
		    if(srchwnd == null) {
			srchwnd = new MenuSearch(menu);
			fitwdg(GameUI.this.add(srchwnd, Utils.getprefc("wndc-srch", new Coord(200, 200))));
		    } else {
			if(!srchwnd.hasfocus) {
			    this.setfocus(srchwnd);
			} else {
			    ui.destroy(srchwnd);
			    srchwnd = null;
			}
		    }
		}
	    }, bg.c);
    }

    protected void added() {
	resize(parent.sz);
	ui.cons.out = new java.io.PrintWriter(new java.io.Writer() {
		StringBuilder buf = new StringBuilder();
		
		public void write(char[] src, int off, int len) {
		    List<String> lines = new ArrayList<String>();
		    synchronized(this) {
			buf.append(src, off, len);
			int p;
			while((p = buf.indexOf("\n")) >= 0) {
			    String ln = buf.substring(0, p).replace("\t", "        ");
			    lines.add(ln);
			    buf.delete(0, p + 1);
			}
		    }
		    for(String ln : lines) {
			syslog.append(ln, Color.WHITE);
		    }
		}
		
		public void close() {}
		public void flush() {}
	    });
	Debug.log = ui.cons.out;
	opts.c = sz.sub(opts.sz).div(2);
    }

    public void dispose() {
	savewndpos();
	Debug.log = new java.io.PrintWriter(System.err);
	ui.cons.clearout();
	super.dispose();
    }

    public static class Hidewnd extends Window {
	public Hidewnd(Coord sz, String cap, boolean lg) {
	    super(sz, cap, lg);
	}

	public Hidewnd(Coord sz, String cap) {
	    super(sz, cap);
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if((sender == this) && msg.equals("close")) {
		this.hide();
		return;
	    }
	    super.wdgmsg(sender, msg, args);
	}
    }

    static class Zergwnd extends Hidewnd {
	Tabs tabs = new Tabs(Coord.z, Coord.z, this);
	final TButton kin, pol, pol2;

	class TButton extends IButton {
	    Tabs.Tab tab = null;
	    final Tex inv;

	    TButton(String nm, boolean g) {
		super("gfx/hud/buttons/" + nm, "u", "d", null);
		if(g)
		    inv = Resource.loadtex("gfx/hud/buttons/" + nm + "g");
		else
		    inv = null;
	    }

	    public void draw(GOut g) {
		if((tab == null) && (inv != null))
		    g.image(inv, Coord.z);
		else
		    super.draw(g);
	    }

	    public void click() {
		if(tab != null) {
		    tabs.showtab(tab);
		    repack();
		}
	    }
	}

	Zergwnd() {
	    super(Coord.z, "Kith & Kin", true);
	    kin = add(new TButton("kin", false));
	    kin.tooltip = Text.render("Kin");
	    pol = add(new TButton("pol", true));
	    pol2 = add(new TButton("rlm", true));
	}

	private void repack() {
	    tabs.indpack();
	    kin.c = new Coord(0, tabs.curtab.contentsz().y + UI.scale(20));
	    pol.c = new Coord(kin.c.x + kin.sz.x + UI.scale(10), kin.c.y);
	    pol2.c = new Coord(pol.c.x + pol.sz.x + UI.scale(10), pol.c.y);
	    this.pack();
	}

	Tabs.Tab ntab(Widget ch, TButton btn) {
	    Tabs.Tab tab = add(tabs.new Tab() {
		    public void cresize(Widget ch) {
			repack();
		    }
		}, tabs.c);
	    tab.add(ch, Coord.z);
	    btn.tab = tab;
	    repack();
	    return(tab);
	}

	void dtab(TButton btn) {
	    btn.tab.destroy();
	    btn.tab = null;
	    repack();
	}

	void addpol(Polity p) {
	    /* This isn't very nice. :( */
	    TButton btn = p.cap.equals("Village")?pol:pol2;
	    ntab(p, btn);
	    btn.tooltip = Text.render(p.cap);
	}
    }

    static class DraggedItem {
	final GItem item;
	final Coord dc;

	DraggedItem(GItem item, Coord dc) {
	    this.item = item; this.dc = dc;
	}
    }

    private void updhand() {
	if((hand.isEmpty() && (vhand != null)) || ((vhand != null) && !hand.contains(vhand.item))) {
	    ui.destroy(vhand);
	    vhand = null;
	}
	if(!hand.isEmpty() && (vhand == null)) {
	    DraggedItem fi = hand.iterator().next();
	    vhand = add(new ItemDrag(fi.dc, fi.item));
	}
    }

    private String mapfilename() {
	StringBuilder buf = new StringBuilder();
	buf.append(genus);
	String chrid = Utils.getpref("mapfile/" + this.chrid, "");
	if(!chrid.equals("")) {
	    if(buf.length() > 0) buf.append('/');
	    buf.append(chrid);
	}
	return(buf.toString());
    }

    public Coord optplacement(Widget child, Coord org) {
	Set<Window> closed = new HashSet<>();
	Set<Coord> open = new HashSet<>();
	open.add(org);
	Coord opt = null;
	double optscore = Double.NEGATIVE_INFINITY;
	Coord plc = null;
	{
	    Gob pl = map.player();
	    if(pl != null) {
		Coord3f raw = pl.placed.getc();
		if(raw != null)
		    plc = map.screenxf(raw).round2();
	    }
	}
	Area parea = Area.sized(Coord.z, sz);
	while(!open.isEmpty()) {
	    Coord cur = Utils.take(open);
	    double score = 0;
	    Area tarea = Area.sized(cur, child.sz);
	    if(parea.isects(tarea)) {
		double outside = 1.0 - (((double)parea.overlap(tarea).area()) / ((double)tarea.area()));
		if((outside > 0.75) && !cur.equals(org))
		    continue;
		score -= Math.pow(outside, 2) * 100;
	    } else {
		if(!cur.equals(org))
		    continue;
		score -= 100;
	    }
	    {
		boolean any = false;
		for(Widget wdg = this.child; wdg != null; wdg = wdg.next) {
		    if(!(wdg instanceof Window))
			continue;
		    Window wnd = (Window)wdg;
		    if(!wnd.visible())
			continue;
		    Area warea = wnd.parentarea(this);
		    if(warea.isects(tarea)) {
			any = true;
			score -= ((double)warea.overlap(tarea).area()) / ((double)tarea.area());
			if(!closed.contains(wnd)) {
			    open.add(new Coord(wnd.c.x - child.sz.x, cur.y));
			    open.add(new Coord(cur.x, wnd.c.y - child.sz.y));
			    open.add(new Coord(wnd.c.x + wnd.sz.x, cur.y));
			    open.add(new Coord(cur.x, wnd.c.y + wnd.sz.y));
			    closed.add(wnd);
			}
		    }
		}
		if(!any)
		    score += 10;
	    }
	    if(plc != null) {
		if(tarea.contains(plc))
		    score -= 100;
		else
		    score -= (1 - Math.pow(tarea.closest(plc).dist(plc) / sz.dist(Coord.z), 0.5)) * 1.5;
	    }
	    score -= (cur.dist(org) / sz.dist(Coord.z)) * 0.75;
	    if(score > optscore) {
		optscore = score;
		opt = cur;
	    }
	}
	return(opt);
    }

    private void savewndpos() {
	if(invwnd != null)
	    Utils.setprefc("wndc-inv", invwnd.c);
	if(equwnd != null)
	    Utils.setprefc("wndc-equ", equwnd.c);
	if(chrwdg != null)
	    Utils.setprefc("wndc-chr", chrwdg.c);
	if(zerg != null)
	    Utils.setprefc("wndc-zerg", zerg.c);
	if(mapfile != null) {
	    Utils.setprefc("wndc-map", mapfile.c);
	    Utils.setprefc("wndsz-map", mapfile.csz());
	}
    }

    private final BMap<String, Window> wndids = new HashBMap<String, Window>();

    public void addchild(Widget child, Object... args) {
	String place = ((String)args[0]).intern();
	if(place == "mapview") {
	    child.resize(sz);
	    map = add((MapView)child, Coord.z);
	    map.lower();
	    if(mmap != null)
		ui.destroy(mmap);
	    if(mapfile != null) {
		ui.destroy(mapfile);
		mapfile = null;
	    }
	    ResCache mapstore = ResCache.global;
	    if(MapFile.mapbase.get() != null)
		mapstore = HashDirCache.get(MapFile.mapbase.get());
	    if(mapstore != null) {
		MapFile file;
		try {
		    file = MapFile.load(mapstore, mapfilename());
			if(!(Boolean) NConfig.get(NConfig.Key.autoMapper)) {
				NUtils.getUI().core.mappingClient.requestor.processMap(file, (m) -> {
					if(m instanceof MapFile.PMarker) {
						return (Boolean) NConfig.get(NConfig.Key.unloadgreen) && ((MapFile.PMarker)m).color.equals(Color.GREEN);
					}
					return true;
				});
			}
		} catch(java.io.IOException e) {
		    /* XXX: Not quite sure what to do here. It's
		     * certainly not obvious that overwriting the
		     * existing mapfile with a new one is better. */
		    throw(new RuntimeException("failed to load mapfile", e));
		}
		add(new NResizableWidget((mmapw = new NMiniMapWnd("MiniMap", (NMapView) map, file)), "minimap", new Coord(250, 250)));
		mmap = mmapw.miniMap;
		mmap.lower();
		mapfile = new NMapWnd(file, map, Utils.getprefc("wndsz-map", UI.scale(new Coord(700, 500))), "Map");
		mapfile.show(Utils.getprefb("wndvis-map", false));
		add(mapfile, Utils.getprefc("wndc-map", new Coord(50, 50)));
	    }
	} else if(place == "menu") {
	    NMenuGridWdg mwdg = new NMenuGridWdg();
		menu = mwdg.setMenuGrid((MenuGrid)child);
		add(new NDraggableWidget(mwdg,"menugrid",new Coord(mwdg.sz).add(NDraggableWidget.delta)));
	} else if(place == "fight") {
	   add(new NDraggableWidget( fv = (Fightview)child,"Fightview",UI.scale(230,380)));
	} else if(place == "fsess") {
	    add(child);
	} else if(place == "inv") {
	    invwnd = new Hidewnd(Coord.z, "Inventory") {
		    public void cresize(Widget ch) {
			pack();
		    }

			@Override
			public boolean keydown(KeyEvent ev) {
				if(ev.getKeyCode() == KeyEvent.VK_TAB)
				{
						return false;
				}
				else {
					return super.keydown(ev);
				}
			}
		};
	    invwnd.add(maininv = (Inventory)child, Coord.z);
	    invwnd.pack();
	    invwnd.hide();
	    add(invwnd, Utils.getprefc("wndc-inv", new Coord(100, 100)));
	} else if(place == "equ") {
	    equwnd = new Hidewnd(Coord.z, "Equipment");
	    equwnd.add(child, Coord.z);
	    equwnd.pack();
	    equwnd.hide();
	    add(equwnd, Utils.getprefc("wndc-equ", new Coord(400, 10)));
	} else if(place == "hand") {
	    GItem g = add((GItem)child);
	    Coord lc = (Coord)args[1];
	    hand.add(new DraggedItem(g, lc));
	    updhand();
	} else if(place == "chr") {
	    chrwdg = add((CharWnd)child, Utils.getprefc("wndc-chr", new Coord(300, 50)));
	    chrwdg.hide();
	} else if(place == "craft") {
	    String cap = "";
	    Widget mkwdg = child;
	    if(mkwdg instanceof Makewindow)
		cap = ((Makewindow)mkwdg).rcpnm;
	    if(cap.equals(""))
		cap = "Crafting";
	    makewnd = new Window(Coord.z, cap, true) {
		    public void wdgmsg(Widget sender, String msg, Object... args) {
			if((sender == this) && msg.equals("close")) {
			    mkwdg.wdgmsg("close");
			    return;
			}
			super.wdgmsg(sender, msg, args);
		    }
		    public void cdestroy(Widget w) {
			if(w == mkwdg) {
			    ui.destroy(this);
			    makewnd = null;
			}
		    }
		    public void destroy() {
			Utils.setprefc("makewndc", makewndc = this.c);
			super.destroy();
		    }
		};
	    makewnd.add(mkwdg, Coord.z);
	    makewnd.pack();
	    fitwdg(add(makewnd, makewndc));
	} else if(place == "buddy") {
	    zerg.ntab(buddies = (BuddyWnd)child, zerg.kin);
	} else if(place == "pol") {
	    Polity p = (Polity)child;
	    polities.add(p);
	    zerg.addpol(p);
	} else if(place == "chat") {
	    chat.addchild(child);
	} else if(place == "party") {
	    add(new NDraggableWidget(child,"party",child.sz.add(NDraggableWidget.delta)), portrait.pos("bl").adds(0, 10));
	} else if(place == "meter") {
		if(child instanceof IMeter)
	    	add(new NDraggableWidget(child, "meter" + ((IMeter)child).name,IMeter.fsz));
		else if(child instanceof Speedget)
			add(new NDraggableWidget(child, "speedmeter" ,IMeter.ssz));
	    meters.add(child);
	} else if(place == "buff") {
	    buffs.addchild(child);
	} else if(place == "qq") {
	    if(qqview != null)
		qqview.reqdestroy();
	    final Widget cref = qqview = child;
	} else if(place == "misc") {
	    Coord c;
	    int a = 1;
	    if(args[a] instanceof Coord) {
		c = (Coord)args[a++];
	    } else if(args[a] instanceof Coord2d) {
		c = ((Coord2d)args[a++]).mul(new Coord2d(this.sz.sub(child.sz))).round();
		c = optplacement(child, c);
	    } else if(args[a] instanceof String) {
		c = relpos((String)args[a++], child, (args.length > a) ? ((Object[])args[a++]) : new Object[] {}, 0);
	    } else {
		throw(new UI.UIException("Illegal gameui child", place, args));
	    }
	    while(a < args.length) {
		Object opt = args[a++];
		if(opt instanceof Object[]) {
		    Object[] opta = (Object[])opt;
		    switch((String)opta[0]) {
		    case "id":
			String wndid = (String)opta[1];
			if(child instanceof Window) {
			    c = Utils.getprefc(String.format("wndc-misc/%s", (String)opta[1]), c);
			    if(!wndids.containsKey(wndid)) {
				c = fitwdg(child, c);
				wndids.put(wndid, (Window)child);
			    } else {
				c = optplacement(child, c);
			    }
			}
			break;
		    case "obj":
			if(child instanceof Window) {
			    ((Window)child).settrans(new GobTrans(map, Utils.uiv(opta[1])));
			}
			break;
		    }
		}
	    }
	    add(child, c);
	} else if(place == "abt") {
	    add(child, Coord.z);
	} else {
	    throw(new UI.UIException("Illegal gameui child", place, args));
	}
    }

    public static class GobTrans implements Window.Transition<GobTrans.Anim, GobTrans.Anim> {
	public static final double time = 0.1;
	public final MapView map;
	public final long gobid;

	public GobTrans(MapView map, long gobid) {
	    this.map = map;
	    this.gobid = gobid;
	}

	private Coord oc() {
	    Gob gob = map.ui.sess.glob.oc.getgob(gobid);
	    if(gob == null)
		return(null);
	    Location.Chain loc = Utils.el(gob.getloc());
	    if(loc == null)
		return(null);
	    return(map.screenxf(loc.fin(Matrix4f.id).mul4(Coord3f.o).invy()).round2());
	}

	public class Anim extends Window.NormAnim {
	    public final Window wnd;
	    private Coord oc;

	    public Anim(Window wnd, boolean hide, Anim from) {
		super(time, from, hide);
		this.wnd = wnd;
		this.oc = wnd.c.add(wnd.sz.div(2));
	    }

	    public void draw(GOut g, Tex tex) {
		GOut pg = g.reclipl(wnd.c.inv(), wnd.parent.sz);
		Coord cur = oc();
		if(cur != null)
		    this.oc = cur;
		Coord sz = tex.sz();
		double na = Utils.smoothstep(this.na);
		pg.chcolor(255, 255, 255, (int)(na * 255));
		double fac = 1.0 - na;
		Coord c = this.oc.sub(sz.div(2)).mul(1.0 - na).add(wnd.c.mul(na));
		pg.image(tex, c.add((int)(sz.x * fac * 0.5), (int)(sz.y * fac * 0.5)),
			 Coord.of((int)(sz.x * (1.0 - fac)), (int)(sz.y * (1.0 - fac))));
	    }
	}

	public Anim show(Window wnd, Anim hide) {return(new Anim(wnd, false, hide));}
	public Anim hide(Window wnd, Anim show) {return(new Anim(wnd, true,  show));}
    }

    public void cdestroy(Widget w) {
	if(w instanceof Window) {
	    String wndid = wndids.reverse().get((Window)w);
	    if(wndid != null) {
		wndids.remove(wndid);
		Utils.setprefc(String.format("wndc-misc/%s", wndid), w.c);
	    }
	}
	if(w instanceof GItem) {
	    for(Iterator<DraggedItem> i = hand.iterator(); i.hasNext();) {
		DraggedItem di = i.next();
		if(di.item == w) {
		    i.remove();
		    updhand();
		}
	    }
	} else if(polities.contains(w)) {
	    polities.remove(w);
	    zerg.dtab(zerg.pol);
	} else if(w == chrwdg) {
	    chrwdg = null;
	}
	meters.remove(w);
    }

    public static class Progress extends Widget {
	private static final Resource.Anim progt = Resource.local().loadwait("gfx/hud/prog").layer(Resource.animc);
	public double prog;
	private TexI curi;
	private String tip;

	public Progress(double prog) {
	    super(progt.f[0][0].ssz);
	    set(prog);
	}

	public void set(double prog) {
	    int fr = Utils.clip((int)Math.floor(prog * progt.f.length), 0, progt.f.length - 2);
	    int bf = Utils.clip((int)(((prog * progt.f.length) - fr) * 255), 0, 255);
	    WritableRaster buf = PUtils.imgraster(progt.f[fr][0].ssz);
	    PUtils.blit(buf, progt.f[fr][0].scaled().getRaster(), Coord.z);
	    PUtils.blendblit(buf, progt.f[fr + 1][0].scaled().getRaster(), Coord.z, bf);
	    if(this.curi != null)
		this.curi.dispose();
	    this.curi = new TexI(PUtils.rasterimg(buf));

	    double d = Math.abs(prog - this.prog);
	    int dec = Math.max(0, (int)Math.round(-Math.log10(d)) - 2);
	    this.tip = String.format("%." + dec + "f%%", prog * 100);
	    this.prog = prog;
	}

	public void draw(GOut g) {
	    g.image(curi, Coord.z);
		TexI label = new TexI(NStyle.openings.render(String.format("%.0f %%",prog*100)).img);
		Coord pos= new Coord(curi.sz.x/2 - label.sz.x/2,0);
		g.aimage(label, pos,0,0);
	}

	public boolean checkhit(Coord c) {
	    return(Utils.checkhit(curi.back, c, 10));
	}

	public Object tooltip(Coord c, Widget prev) {
	    if(checkhit(c))
		return(tip);
	    return(super.tooltip(c, prev));
	}
    }

	public static final Tex cells = Resource.loadtex("nurgling/hud/cell");

    public void draw(GOut g) {
	Widget next;
	boolean mapViewReady = false;
	for(Widget wdg = child; wdg != null; wdg = next) {
		next = wdg.next;
		if(wdg instanceof MapView)
		{
			mapViewReady = true;
		}
		if(!wdg.visible)
			continue;
		Coord cc = xlate(wdg.c, true);
		GOut g2 = g.reclip(cc, wdg.sz);
		wdg.draw(g2);
		if(mapViewReady)
		{
			if(ui.core.mode== NCore.Mode.DRAG && ui.core.enablegrid)
			{
				for (int x = 0; x + cells.sz().x < sz.x +cells.sz().x; x += cells.sz().x)
				{
					for (int y = 0; y + cells.sz().y < sz.y + cells.sz().y; y += cells.sz().y)
					{
						g.image(cells, new Coord(x, y));
					}
				}
			}
			mapViewReady = false;
		}
	}


	int by = sz.y;
	if(chat.visible())
	    by = Math.min(by, chat.c.y);
	if(cmdline != null) {
	    drawcmd(g, new Coord(blpw + UI.scale(10), by -= UI.scale(20)));
	} else if(lastmsg != null) {
	    if((Utils.rtime() - msgtime) > 3.0) {
		lastmsg = null;
	    } else {
		g.chcolor(0, 0, 0, 192);
		Coord pos = chatwdg.c.sub(UI.scale(-20,lastmsg.tex().sz().y + 5));
		g.frect(new Coord(pos.x + UI.scale(8), pos.y), lastmsg.sz().add(UI.scale(4), UI.scale(4)));
		g.chcolor();
		g.image(lastmsg.tex(), new Coord(pos.x + UI.scale(10), pos.y));
	    }
	}
    }
    
    private String iconconfname() {
	StringBuilder buf = new StringBuilder();
	buf.append("data/mm-icons-2");
	if(genus != null)
	    buf.append("/" + genus);
	if(ui.sess != null)
	    buf.append("/" + ui.sess.username);
	return(buf.toString());
    }

    private GobIcon.Settings loadiconconf() {
	String nm = iconconfname();
	try {
	    return(GobIcon.Settings.load(ui, nm));
	} catch(Exception e) {
	    new Warning(e, "could not load icon-conf").issue();
	}
	return(new GobIcon.Settings(ui, nm));
    }

    public class CornerMap extends MiniMap implements Console.Directory {
	public CornerMap(Coord sz, MapFile file) {
	    super(sz, file);
	    follow(new MapLocator(map));
	}

	public boolean dragp(int button) {
	    return(false);
	}

	public boolean clickmarker(DisplayMarker mark, Location loc, int button, boolean press) {
	    if(mark.m instanceof MapFile.SMarker) {
		Gob gob = MarkerID.find(ui.sess.glob.oc, (MapFile.SMarker)mark.m);
		if(gob != null)
		    mvclick(map, null, loc, gob, button);
	    }
	    return(false);
	}

	public boolean clickicon(DisplayIcon icon, Location loc, int button, boolean press) {
	    if(press) {
		mvclick(map, null, loc, icon.gob, button);
		return(true);
	    }
	    return(false);
	}

	public boolean clickloc(Location loc, int button, boolean press) {
	    if(press) {
		mvclick(map, null, loc, null, button);
		return(true);
	    }
	    return(false);
	}

	public void draw(GOut g) {
		// TODO подложка для карты
	    //g.image(bg, Coord.z, UI.scale(bg.sz()));
	    super.draw(g);
	}

	protected boolean allowzoomout() {
	    /* XXX? The corner-map has the property that its size
	     * makes it so that the one center grid will very commonly
	     * touch at least one border, making indefinite zoom-out
	     * possible. That will likely cause more problems than
	     * it's worth given the resulting workload in generating
	     * zoomgrids for very high zoom levels, especially when
	     * done by mistake, so lock to an arbitrary five levels of
	     * zoom, at least for now. */
	    if(zoomlevel >= 5)
		return(false);
	    return(super.allowzoomout());
	}
	private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
	{
	    cmdmap.put("rmseg", new Console.Command() {
		    public void run(Console cons, String[] args) {
			MiniMap.Location loc = curloc;
			if(loc != null) {
			    try(Locked lk = new Locked(file.lock.writeLock())) {
				file.segments.remove(loc.seg.id);
			    }
			}
		    }
		});
	}
	public Map<String, Console.Command> findcmds() {
	    return(cmdmap);
	}
    }

    private Coord lastsavegrid = null;
    private int lastsaveseq = -1;
    private void mapfiletick() {
	MapView map = this.map;
	MiniMap mmap = this.mmap;
	if((map == null) || (mmap == null))
	    return;
	Gob pl = ui.sess.glob.oc.getgob(map.plgob);
	Coord gc;
	if(pl == null)
	    gc = map.cc.floor(MCache.tilesz).div(MCache.cmaps);
	else
	    gc = pl.rc.floor(MCache.tilesz).div(MCache.cmaps);
	try {
	    MCache.Grid grid = ui.sess.glob.map.getgrid(gc);
	    if((grid != null) && (!Utils.eq(gc, lastsavegrid) || (lastsaveseq != grid.seq))) {
		mmap.file.update(ui.sess.glob.map, gc);
		lastsavegrid = gc;
		lastsaveseq = grid.seq;
	    }
	} catch(Loading l) {
	}
    }

    private double lastwndsave = 0;
    public void tick(double dt) {
	super.tick(dt);
	double now = Utils.rtime();
	if(now - lastwndsave > 60) {
	    savewndpos();
	    lastwndsave = now;
	}
	double idle = now - ui.lastevent;
	if(!afk && (idle > 300)) {
	    afk = true;
	    wdgmsg("afk");
	} else if(afk && (idle <= 300)) {
	    afk = false;
	}
	mapfiletick();
    }
    
    public void uimsg(String msg, Object... args) {
	if(msg == "err") {
	    String err = (String)args[0];
	    ui.error(err);
	} else if(msg == "msg") {
	    String text = (String)args[0];
	    ui.msg(text);
	} else if(msg == "prog") {
	    if(args.length > 0) {
		double p = Utils.dv(args[0]) / 100.0;
		if(prog == null)
		    prog = adda(new Progress(p), 0.5, 0.35);
		else
		    prog.set(p);
	    } else {
		if(prog != null) {
		    prog.reqdestroy();
		    prog = null;
		}
	    }
	} else if(msg == "setbelt") {
	    int slot = Utils.iv(args[0]);
	    if(args.length < 2) {
		belt[slot] = null;
	    } else {
		Indir<Resource> res = ui.sess.getresv(args[1]);
		Message sdt = Message.nil;
		if(args.length > 2)
		    sdt = new MessageBuf((byte[])args[2]);
		ResData rdt = new ResData(res, sdt);
		ui.sess.glob.loader.defer(() -> {
			belt[slot] = mkbeltslot(slot, rdt);
		    }, null);
	    }
	} else if(msg == "setbelt2") {
	    int slot = Utils.iv(args[0]);
	    if(args.length < 2) {
		belt[slot] = null;
	    } else {
		switch((String)args[1]) {
		case "p": {
		    Object id = args[2];
		    belt[slot] = new PagBeltSlot(slot, menu.paginafor(id, null));
		    break;
		}
		case "r": {
		    Indir<Resource> res = ui.sess.getresv(args[2]);
		    ui.sess.glob.loader.defer(() -> {
			    belt[slot] = new PagBeltSlot(slot, PagBeltSlot.resolve(menu, res));
			}, null);
		    break;
		}
		case "d": {
		    Indir<Resource> res = ui.sess.getresv(args[2]);
		    Message sdt = Message.nil;
		    if(args.length > 2)
			sdt = new MessageBuf((byte[])args[3]);
		    belt[slot] = new ResBeltSlot(slot, new ResData(res, sdt));
		    break;
		}
		}
	    }
	} else if(msg == "polowner") {
	    int id = Utils.iv(args[0]);
	    String o = (String)args[1];
	    boolean n = Utils.bv(args[2]);
	    if(o != null)
		o = o.intern();
	    String cur = polowners.get(id);
	    if(map != null) {
		if((o != null) && (cur == null)) {
		    if(n)
			map.setpoltext(id, "Entering " + o);
		} else if((o == null) && (cur != null)) {
		    map.setpoltext(id, "Leaving " + cur);
		}
	    }
	    polowners.put(id, o);
	} else if(msg == "showhelp") {
	    Indir<Resource> res = ui.sess.getresv(args[0]);
	    if(help == null)
		help = adda(new HelpWnd(res), 0.5, 0.25);
	    else
		help.res = res;
	} else if(msg == "map-mark") {
	    long gobid = Utils.uiv(args[0]);
	    long oid = ((Number)args[1]).longValue();
	    Indir<Resource> res = ui.sess.getresv(args[2]);
	    String nm = (String)args[3];
	    if(mapfile != null)
		mapfile.markobj(gobid, oid, res, nm);
	} else if(msg == "map-icons") {
	    GobIcon.Settings conf = this.iconconf;
	    int tag = Utils.iv(args[0]);
	    if(args.length < 2) {
		if(conf.tag != tag)
		    wdgmsg("map-icons", conf.tag);
	    } else {
		conf.receive(args);
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == chrwdg) && (msg == "close")) {
	    chrwdg.hide();
	    return;
	} else if((sender == mapfile) && (msg == "close")) {
	    mapfile.hide();
	    Utils.setprefb("wndvis-map", false);
	    return;
	} else if((sender == help) && (msg == "close")) {
	    ui.destroy(help);
	    help = null;
	    return;
	} else if((sender == srchwnd) && (msg == "close")) {
	    ui.destroy(srchwnd);
	    srchwnd = null;
	    return;
	} else if((sender == iconwnd) && (msg == "close")) {
	    ui.destroy(iconwnd);
	    iconwnd = null;
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }

    private static final int fitmarg = UI.scale(100);
    private Coord fitwdg(Widget wdg, Coord c) {
	Coord ret = new Coord(c);
	ret.x = Math.max(ret.x, Math.min(0, fitmarg - wdg.sz.x));
	ret.y = Math.max(ret.y, Math.min(0, fitmarg - wdg.sz.y));
	ret.x = Math.min(ret.x, sz.x - Math.min(fitmarg, wdg.sz.x));
	ret.y = Math.min(ret.y, sz.y - Math.min(fitmarg, wdg.sz.y));
	return(ret);
    }

    public void fitwdg(Widget wdg) {
	wdg.c = fitwdg(wdg, wdg.c);
    }

    public boolean wndstate(Window wnd) {
	if(wnd == null)
	    return(false);
	return(wnd.visible());
    }

    public void togglewnd(Window wnd) {
	if(wnd != null) {
	    if(wnd.show(!wnd.visible())) {
		wnd.raise();
		fitwdg(wnd);
		setfocus(wnd);
	    }
	}
    }

    public static class MenuButton extends IButton {
	MenuButton(String base, KeyBinding gkey, String tooltip) {
	    super("gfx/hud/" + base, "", "-d", "-h");
	    setgkey(gkey);
	    settip(tooltip);
	}
    }

    public static class MenuCheckBox extends ICheckBox {
	MenuCheckBox(String base, KeyBinding gkey, String tooltip) {
	    super("nurgling/hud/buttons/" + base, "u", "d", "h", "dh");
	    setgkey(gkey);
	    settip(tooltip);
	}
    }

    public static final KeyBinding kb_inv = KeyBinding.get("inv", KeyMatch.forcode(KeyEvent.VK_TAB, 0));
    public static final KeyBinding kb_equ = KeyBinding.get("equ", KeyMatch.forchar('E', KeyMatch.C));
    public static final KeyBinding kb_chr = KeyBinding.get("chr", KeyMatch.forchar('T', KeyMatch.C));
    public static final KeyBinding kb_bud = KeyBinding.get("bud", KeyMatch.forchar('B', KeyMatch.C));
    public static final KeyBinding kb_areas = KeyBinding.get("areas", KeyMatch.forchar('L', KeyMatch.C));
    public static final KeyBinding kb_opt = KeyBinding.get("opt", KeyMatch.forchar('O', KeyMatch.C));
    public class MainMenu extends Widget {
	public MainMenu() {
	    super(Coord.z);
	    prev = add(new MenuCheckBox("rbtn/inv/", kb_inv, "Inventory"), 0, 0).state(() -> wndstate(invwnd)).click(() -> togglewnd(invwnd));
	    prev = add(new MenuCheckBox("rbtn/equ/", kb_equ, "Equipment"), prev.pos("ur").add(UI.scale(10),0)).state(() -> wndstate(equwnd)).click(() -> togglewnd(equwnd));
	    prev = add(new MenuCheckBox("rbtn/chr/", kb_chr, "Character Sheet"), prev.pos("ur").add(UI.scale(10),0)).state(() -> wndstate(chrwdg)).click(() -> togglewnd(chrwdg));
	    prev = add(new MenuCheckBox("rbtn/areas/", kb_areas, "Areas Settings"), prev.pos("ur").add(UI.scale(10),0)).state(() -> wndstate(areas)).click(() -> togglewnd(areas));
	    prev = add(new MenuCheckBox("rbtn/bud/", kb_bud, "Kith & Kin"), prev.pos("ur").add(UI.scale(10),0)).state(() -> wndstate(zerg)).click(() -> togglewnd(zerg));
	    add(new MenuCheckBox("rbtn/opt/", kb_opt, "Options"), prev.pos("ur").add(UI.scale(10),0)).state(() -> wndstate(opts)).click(() -> togglewnd(opts));
		pack();
	}

	public void draw(GOut g) {
	    super.draw(g);
	}
    }

    public static final KeyBinding kb_map = KeyBinding.get("map", KeyMatch.forchar('A', KeyMatch.C));
    public static final KeyBinding kb_claim = KeyBinding.get("ol-claim", KeyMatch.nil);
    public static final KeyBinding kb_vil = KeyBinding.get("ol-vil", KeyMatch.nil);
    public static final KeyBinding kb_rlm = KeyBinding.get("ol-rlm", KeyMatch.nil);
    public static final KeyBinding kb_ico = KeyBinding.get("map-icons", KeyMatch.nil);
    private static final Tex mapmenubg = Resource.loadtex("gfx/hud/lbtn-bg");
    public class MapMenu extends Widget {
	private void toggleol(String tag, boolean a) {
	    if(map != null) {
		if(a)
		    map.enol(tag);
		else
		    map.disol(tag);
	    }
	}

	public MapMenu() {
	    super(mapmenubg.sz());
//	    add(new MenuCheckBox("lbtn-claim", kb_claim, "Display personal claims"), 0, 0).changed(a -> toggleol("cplot", a));
//	    add(new MenuCheckBox("lbtn-vil", kb_vil, "Display village claims"), 0, 0).changed(a -> toggleol("vlg", a));
//	    add(new MenuCheckBox("lbtn-rlm", kb_rlm, "Display provinces"), 0, 0).changed(a -> toggleol("prov", a));
//	    add(new MenuCheckBox("lbtn-map", kb_map, "Map")).state(() -> wndstate(mapfile)).click(() -> {
//		    togglewnd(mapfile);
//		    if(mapfile != null)
//			Utils.setprefb("wndvis-map", mapfile.visible());
//		});
//	    add(new MenuCheckBox("lbtn-ico", kb_ico, "Icon settings"), 0, 0).state(() -> wndstate(iconwnd)).click(() -> {
//		    if(iconconf == null)
//			return;
//		    if(iconwnd == null) {
//			iconwnd = new GobIcon.SettingsWindow(iconconf, () -> Utils.defer(GameUI.this::saveiconconf));
//			fitwdg(GameUI.this.add(iconwnd, Utils.getprefc("wndc-icon", new Coord(200, 200))));
//		    } else {
//			ui.destroy(iconwnd);
//			iconwnd = null;
//		    }
//		});
	}

	public void draw(GOut g) {
	    super.draw(g);
	}
    }

    public static final KeyBinding kb_shoot = KeyBinding.get("screenshot", KeyMatch.forchar('S', KeyMatch.M));
    public static final KeyBinding kb_hide = KeyBinding.get("ui-toggle", KeyMatch.nil);
    public static final KeyBinding kb_logout = KeyBinding.get("logout", KeyMatch.nil);
    public static final KeyBinding kb_switchchr = KeyBinding.get("logout-cs", KeyMatch.nil);
    public boolean globtype(char key, KeyEvent ev) {
	if(key == ':') {
	    entercmd();
	    return(true);
	} else if(kb_shoot.key().match(ev) && (Screenshooter.screenurl.get() != null)) {
	    Screenshooter.take(this, Screenshooter.screenurl.get());
	    return(true);
	} else if(kb_hide.key().match(ev)) {
	    toggleui();
	    return(true);
	} else if(kb_logout.key().match(ev)) {
	    act("lo");
	    return(true);
	} else if(kb_switchchr.key().match(ev)) {
	    act("lo", "cs");
	    return(true);
	} else if((key == 27) && (map != null) && !map.hasfocus) {
	    setfocus(map);
	    return(true);
	}
	return(super.globtype(key, ev));
    }
    
    public boolean mousedown(Coord c, int button) {
	return(super.mousedown(c, button));
    }

    private int uimode = 1;
    public void toggleui(int mode) {

    }

    public void resetui() {
	uimode = 1;
    }

    public void toggleui() {
	toggleui((uimode + 1) % 3);
    }

	public static final Coord margin = UI.scale(new Coord(10,10));
    public void resize(Coord sz) {
	super.resize(sz);
	Widget next;
	for (Widget wdg = child; wdg != null; wdg = next)
	{
		next = wdg.next;
//		if (wdg instanceof NDraggableWidget)
//		{
//			if(wdg.c.x+wdg.sz.x>sz.x - margin.x)
//				wdg.c.x = sz.x - wdg.sz.x - margin.x;
//			else
//				wdg.c.x = ((NDraggableWidget) wdg).target_c.x;
//			if(wdg.c.y+wdg.sz.y>sz.y - margin.y)
//				wdg.c.y = sz.y - wdg.sz.y - margin.y;
//			else
//				wdg.c.y = ((NDraggableWidget) wdg).target_c.y;
//		}
	}

	if(map != null)
	    map.resize(sz);
	if(prog != null)
	    prog.move(sz.sub(prog.sz).mul(0.5, 0.35));
    }
    
    public void presize() {
	resize(parent.sz);
    }
    
    public void msg(String msg, Color color, Color logcol) {
	msgtime = Utils.rtime();
	lastmsg = RootWidget.msgfoundry.render(msg, color);
	syslog.append(msg, logcol);
    }

    public void msg(String msg, Color color) {
	msg(msg, color, color);
    }

	public void msg(String msg) {
		msg(msg, new Color(255,255,255));
	}

    public void msg(String msg, Color color, Audio.Clip sfx) {
	msg(msg, color);
	ui.sfxrl(sfx);
    }



    public void error(String msg) {
	ui.error(msg);
    }
    
    public void act(String... args) {
	wdgmsg("act", (Object[])args);
    }

    public void act(int mods, Coord mc, Gob gob, String... args) {
	int n = args.length;
	Object[] al = new Object[n];
	System.arraycopy(args, 0, al, 0, n);
	if(mc != null) {
	    al = Utils.extend(al, al.length + 2);
	    al[n++] = mods;
	    al[n++] = mc;
	    if(gob != null) {
		al = Utils.extend(al, al.length + 2);
		al[n++] = (int)gob.id;
		al[n++] = gob.rc;
	    }
	}
	wdgmsg("act", al);
    }

    public class FKeyBelt extends Belt implements DTarget, DropTarget {
	public final int beltkeys[] = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
				       KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
				       KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12};
	public int curbelt = 0;

	public FKeyBelt() {
	    super(UI.scale(new Coord(450, 34)));
	}

	private Coord beltc(int i) {
	    return(new Coord((((invsq.sz().x + UI.scale(2)) * i) + (10 * (i / 4))), 0));
	}
    
	public int beltslot(Coord c) {
	    for(int i = 0; i < 12; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public void draw(GOut g) {
	    for(int i = 0; i < 12; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null)
			belt[slot].draw(g.reclip(c.add(UI.scale(1), UI.scale(1)), invsq.sz().sub(UI.scale(2), UI.scale(2))));
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(UI.scale(2), 0)), 1, 1, "F%d", i + 1);
		g.chcolor();
	    }
	}
	
	public boolean globtype(char key, KeyEvent ev) {
	    boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	    for(int i = 0; i < beltkeys.length; i++) {
		if(ev.getKeyCode() == beltkeys[i]) {
		    if(M) {
			curbelt = i;
			return(true);
		    } else {
			keyact(i + (curbelt * 12));
			return(true);
		    }
		}
	    }
	    return(false);
	}
    }
    
    private static final Tex nkeybg = Resource.loadtex("gfx/hud/hb-main");
    public class NKeyBelt extends Belt {
	public int curbelt = 0;
	final Coord pagoff = UI.scale(new Coord(5, 25));

	public NKeyBelt() {
	    super(nkeybg.sz());
	}
	
	private Coord beltc(int i) {
	    return(pagoff.add(UI.scale((36 * i) + (10 * (i / 5))), 0));
	}
    
	public int beltslot(Coord c) {
	    for(int i = 0; i < 10; i++) {
		if(c.isect(beltc(i), invsq.sz()))
		    return(i + (curbelt * 12));
	    }
	    return(-1);
	}
    
	public void draw(GOut g) {
	    g.image(nkeybg, Coord.z);
	    for(int i = 0; i < 10; i++) {
		int slot = i + (curbelt * 12);
		Coord c = beltc(i);
		g.image(invsq, beltc(i));
		try {
		    if(belt[slot] != null) {
			belt[slot].draw(g.reclip(c.add(UI.scale(1), UI.scale(1)), invsq.sz().sub(UI.scale(2), UI.scale(2))));
		    }
		} catch(Loading e) {}
		g.chcolor(156, 180, 158, 255);
		FastText.aprintf(g, c.add(invsq.sz().sub(UI.scale(2), 0)), 1, 1, "%d", (i + 1) % 10);
		g.chcolor();
	    }
	    super.draw(g);
	}
	
	public boolean globtype(char key, KeyEvent ev) {
	    int c = ev.getKeyCode();
	    if((c < KeyEvent.VK_0) || (c > KeyEvent.VK_9))
		return(false);
	    int i = Utils.floormod(c - KeyEvent.VK_0 - 1, 10);
	    boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	    if(M) {
		curbelt = i;
	    } else {
		keyact(i + (curbelt * 12));
	    }
	    return(true);
	}
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("afk", new Console.Command() {
		public void run(Console cons, String[] args) {
		    afk = true;
		    wdgmsg("afk");
		}
	    });
	cmdmap.put("act", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Object[] ad = new Object[args.length - 1];
		    System.arraycopy(args, 1, ad, 0, ad.length);
		    wdgmsg("act", ad);
		}
	    });
	cmdmap.put("chrmap", new Console.Command() {
		public void run(Console cons, String[] args) {
		    Utils.setpref("mapfile/" + chrid, args[1]);
		}
	    });
	cmdmap.put("tool", new Console.Command() {
		public void run(Console cons, String[] args) {
		    try {
			Object[] wargs = new Object[args.length - 2];
			for(int i = 0; i < wargs.length; i++)
			    wargs[i] = args[i + 2];
			add(gettype(args[1]).create(ui, wargs), 200, 200);
		    } catch(RuntimeException e) {
			e.printStackTrace(Debug.log);
		    }
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
