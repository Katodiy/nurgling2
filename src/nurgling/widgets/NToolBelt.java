package nurgling.widgets;

import haven.*;

import haven.KeyBinding;

import java.awt.*;
import java.awt.List;
import java.awt.event.KeyEvent;
import java.util.*;

import static haven.Inventory.invsq;
import nurgling.*;
import nurgling.conf.*;
import nurgling.tools.*;

public class NToolBelt extends Widget implements DTarget, DropTarget {
    private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 12);
    public static final int GAP = 10;
    public static final int PAD = 2;
    public static final int BTNSZ = 17;
    public static final Coord INVSZ = invsq.sz();
    public static final Color BG_COLOR = new Color(43, 54, 35, 202);

    private ArrayList<KeyBinding> beltkeys;
    private HashMap<Integer, String> usercfg = new HashMap<>();
//    private final NGameUI.NButtonBeltSlot[] custom;
    private final int group;
    private final int start;
    private final int size;
    Tex[] keys;
    private GameUI.BeltSlot last = null;
    private Tex ttip = null;
    private boolean vertical = false;


    public NToolBelt(String name, int start, int group, ArrayList<KeyBinding> beltkeys) {
        this(name, start, group, beltkeys.size(), beltkeys);
    }

    public NToolBelt(String name, int start, int group, int size) {
        this(name, start, group, size, NToolBeltProp.get(name).getKb());
    }


    public NToolBelt(String name, int start, int group, int size, ArrayList<KeyBinding> beltkeys) {
        super( new Coord(0,0) );
        this.start = start;
        this.group = group;
        this.size = size;
//        custom = new NGameUI.NButtonBeltSlot[size];
//        loadBelt();
        updateButtons(beltkeys);
        sz = beltc(size - 1).add(INVSZ);
    }

//    private void loadBelt() {
//        String user_id = NUtils.getUI().sessInfo.username + "/" + NUtils.getUI().sessInfo.characterInfo.chrid;
//        if (NConfiguration.getInstance().allKeys.get(user_id) != null) {
//            Map<Integer, String> keys = NConfiguration.getInstance().allKeys.get(user_id).get(name);
//            if (keys != null) {
//                for (Integer idx : keys.keySet()) {
//                    usercfg.put(idx, keys.get(idx));
//                }
//            }
//        }
//    }


    public void updateButtons(ArrayList<KeyBinding> kb){
        beltkeys = kb;
        keys = new Tex[size];
        int i = 0;
        if (beltkeys != null) {
            for (KeyBinding beltkey : beltkeys) {
                if (beltkey != null) {
                    String hotKey;
                    int mode  = 0;
                    if( beltkey.key != null)
                    {
                        hotKey = KeyEvent.getKeyText(beltkey.key.code);
                        mode = beltkey.key.modmatch;

                        if (NParser.checkName(hotKey, new NAlias("Num")))
                        {
                            hotKey = "N" + hotKey.substring(hotKey.indexOf("-") + 1);
                        }
                        if (NParser.checkName(hotKey, new NAlias("inus")))
                        {
                            hotKey = "-";
                        }
                        else if (NParser.checkName(hotKey, new NAlias("quals")))
                        {
                            hotKey = "=";
                        }
                        if ((mode & KeyMatch.C) != 0)
                            hotKey = "C" + hotKey;
                        if ((mode & KeyMatch.S) != 0)
                            hotKey = "S" + hotKey;
                        if ((mode & KeyMatch.M) != 0)
                            hotKey = "A" + hotKey;
                        keys[i++] = NStyle.hotkey.render(hotKey).tex();
                    }
                }
            }
        }
    }

    @Override
    protected void attached() {
        super.attached();
        for (int i = 0; i < size; i++) {
            String res = usercfg.get(slot(i));
//            if(res != null) {
//                NBotsInfo.NButton p = NUtils.getGameUI().botsInfo.find(res);
//                if(p!=null)
//                    custom[i] = NUtils.getGameUI().new NButtonBeltSlot(i, p);
//            }
        }
    }

    private void resize() {
        sz = beltc(size - 1).add(INVSZ);
    }

//    private void toggle(Boolean state) {
//        locked = state != null ? state : false;
//        NConfiguration.getInstance().toolBelts.get(name).isLocked = locked;
//        NConfiguration.getInstance().write();
//        draggable(!locked);
//        update_buttons();
//    }

    @Override
    public void flip(boolean val) {
        vertical = val;
        resize();
    }

    private GameUI.BeltSlot belt(int slot) {
        if(slot < 0) {return null;}
        GameUI.BeltSlot res = null;// = custom[slot - start];
        if(ui != null && NUtils.getGameUI() != null && NUtils.getGameUI().belt[slot] != null) {
            res = NUtils.getGameUI().belt[slot];
        }
        return res;
    }

    private Coord beltc(int i) {
        return vertical ?
                new Coord(0, BTNSZ + ((INVSZ.y + PAD) * i) + (GAP * (i / group))) :
                new Coord(BTNSZ + ((INVSZ.x + PAD) * i) + (GAP * (i / group)), 0);
    }

    private int beltslot(Coord c) {
        for (int i = 0; i < size; i++) {
            if(c.isect(beltc(i), invsq.sz())) {
                return slot(i);
            }
        }
        return (-1);
    }

//    private void setcustom(int slot, NBotsInfo.NButton p) {
//        NGameUI.NButtonBeltSlot pslot = custom[slot - start];
//        if((pslot == null && p != null) || (pslot != null && pslot.button != p)) {
//            custom[slot - start] = p != null ? NUtils.getGameUI().new NButtonBeltSlot(slot, p) : null;
//            usercfg.put(slot, p != null ? p.res.get().name : null);
//            NConfiguration.saveButtons(name,custom);
//        }
//    }

//    private NBotsInfo.NButton getcustom(GameUI.BeltSlot slot) {
//        if(slot instanceof NGameUI.NButtonBeltSlot) {
//            return ((NGameUI.NButtonBeltSlot) slot).button;
//        } else {
//            return null;
//        }
//    }

//    private NBotsInfo.NButton getcustom(Resource res) {
//        return NUtils.getGameUI().botsInfo.find(res);
//    }

    @Override
    public void draw(GOut g) {
        for (int i = 0; i < size; i++) {
            Coord c = beltc(i);
            int slot = slot(i);
            g.image(invsq, c);
            try {
                GameUI.BeltSlot item = belt(slot);
                if (item != null) {
                    item.spr().draw(g.reclip(c.add(1, 1), invsq.sz().sub(2, 2)));
                }
            } catch (Loading ignored) {
            }
            if (keys[i] != null) {
                g.aimage(keys[i], c.add(INVSZ.sub(2, 0)), 1, 1);
            }
        }
        super.draw(g);
    }

    private int slot(int i) {return i + start;}

    @Override
    public boolean globtype(char key, KeyEvent ev) {
//        if(NConfiguration.getInstance().toolBelts.get(name).isEnable) {
            if (!visible || beltkeys == null) {
                return false;
            }
            for (int i = 0; i < beltkeys.size(); i++) {
                if ((beltkeys.get(i).key != null && ev.getKeyCode() == beltkeys.get(i).key.code && ui.modflags() == beltkeys.get(i).key.modmatch)) {
                    keyact(slot(i));
                    return true;
                }
            }
//        }
        return false;
    }

    public void keyact(final int slot) {
//        NBotsInfo.NButton pagina = getcustom(belt(slot));
//        if(belt(slot)!=null) {
//            if (((NMenuGrid) NUtils.getGameUI().menu).isCrafting(((NMenuGrid)NUtils.getGameUI().menu).paginafor(belt(slot).res.get()))) {
//                ((NMenuGrid) NUtils.getGameUI().menu).lastCraft = ((NMenuGrid)NUtils.getGameUI().menu).paginafor(belt(slot).res.get());
//            }
//        }
//        if(pagina != null) {
//            pagina.click();
//            return;
//        }
        MapView map = NUtils.getGameUI().map;
        if(map != null) {
            Coord mvc = map.rootxlate(ui.mc);
            if(mvc.isect(Coord.z, map.sz)) {
                map.new Hittest(mvc) {
                    protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                        Object[] args = {slot, 1, ui.modflags(), mc.floor(OCache.posres)};
                        if(inf != null) { args = Utils.extend(args, inf.clickargs());}
                        NUtils.getGameUI().wdgmsg("belt", args);
                    }

                    protected void nohit(Coord pc) {
                        NUtils.getGameUI().wdgmsg("belt", slot, 1, ui.modflags());
                    }
                }.run();
            }
        }
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        int slot = beltslot(c);
        if(slot != -1) {
            if(button == 1) {
//                NBotsInfo.NButton pagina = getcustom(belt(slot)); //FIXME: re-implement custom actions
//                if (pagina != null) {
//                    pagina.click();
//                }
//                else {
                    GameUI.BeltSlot item = belt(slot);
                    if (item != null) {
                        MenuGrid.Pagina pag = NUtils.getGameUI().menu.paginafor(item.res);
                        if (pag != null) {
//                            if (((NMenuGrid) NUtils.getGameUI().menu).isCrafting(pag)) {
//                                ((NMenuGrid) NUtils.getGameUI().menu).lastCraft = pag;
//                            }
                        }
                    }
                    NUtils.getGameUI().wdgmsg("belt", slot, 1, ui.modflags());
//                }
            } else if(button == 3) {
                NUtils.getGameUI().wdgmsg("setbelt", slot, 1);
//                setcustom(slot, null);
            }
            if(belt(slot) != null) {return true;}
        }
        return super.mousedown(c, button);
    }

    @Override
    public void mousemove(Coord c) {
        super.mousemove(c);
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        int slot = beltslot(c);
        if(slot < 0) {return super.tooltip(c, prev);}
        GameUI.BeltSlot item = belt(slot);
        if(item == null) {return super.tooltip(c, prev);}
        if(last != item) {
            if(ttip != null) {ttip.dispose();}
            ttip = null;
            try {
                if(NUtils.getGameUI().menu!=null) {
                    MenuGrid.Pagina p = NUtils.getGameUI().menu.paginafor(item.res);
                    last = item;
                }
            } catch (Loading ignored) {}
        }
        return ttip;
    }

    public boolean drop(Coord c, Coord ul) {
        int slot = beltslot(c);
        if(slot != -1) {
            NUtils.getGameUI().wdgmsg("setbelt", slot, 0);
            return true;
        }
        return false;
    }

    public boolean iteminteract(Coord c, Coord ul) {return false;}

    public boolean dropthing(Coord c, Object thing) {
        int slot = beltslot(c);
        if(slot != -1) {
            if(thing instanceof Resource) {
                Resource res = (Resource) thing;
                if(res.layer(Resource.action) != null) {
//                    NBotsInfo.NButton pagina = getcustom(res);
//                    if(pagina != null) {
//                        setcustom(slot, pagina);
//                        NUtils.getGameUI().wdgmsg("setbelt", slot, 1); //clear default action in this slot
//                    } else {
//                        setcustom(slot, null);
                        NUtils.getGameUI().wdgmsg("setbelt", slot, res.name);
//                    }
                    return true;
                }
            }
        }
        return false;
    }
}
