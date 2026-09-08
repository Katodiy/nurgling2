package haven;

import nurgling.*;
import nurgling.conf.NCombatData;
import nurgling.widgets.NDraggableWidget;
import nurgling.widgets.NEquipory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Combat HUD. The Fightsess widget itself covers the whole screen and paints nothing;
 * everything is drawn by two child widgets that live in their own draggable frames so
 * players can place the openings readout and the move bar independently:
 *
 *   FightBuffsInfo - openings, IP, attack cooldown, last moves, agility, health/stam
 *   FightActions   - the move icons with hotkey captions and damage prediction
 *
 * Both panels lay out around a single anchor point the way the fullscreen version does,
 * so their internal geometry matches pixel for pixel regardless of where they are moved.
 */
public class NFightsess extends Fightsess {

    public NFightsessBuffsAndInfo buffsAndInfo;
    public NFightsessActions actionsWidget;

    /* Hurricane's move-bar pitch: narrower horizontally than the stock 50 so ten moves
     * fit on one row, taller vertically to leave room for the two captions. */
    public static final int apitch = UI.scale(45);
    public static final int apitch2 = UI.scale(62);

    public static final Text.Foundry ipFoundry = new Text.Foundry(Text.serif.deriveFont(Font.BOLD), 22);
    public static final Text.Foundry openingFoundry = new Text.Foundry(Text.dfont.deriveFont(Font.BOLD), 10);
    public static final Text.Foundry keybindFoundry = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 14);
    public static final Text.Foundry damageFoundry = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 11);

    public static final Color stamBarBlue = new Color(47, 58, 207, 200);
    public static final Color hpBarGreen  = new Color(0, 166, 10, 255);
    public static final Color hpBarGray   = new Color(113, 113, 113, 255);
    public static final Color hpBarRed    = new Color(168, 0, 0, 255);
    public static final Color hpBarYellow = new Color(182, 165, 0, 255);

    private static final Coord BARSZ = UI.scale(new Coord(234, 22));

    /* Rendered text is expensive relative to a frame, and the same handful of strings
     * recur for a whole fight, so everything stroked goes through one bounded LRU.
     * Keys embed the colour so a settings change produces new entries rather than
     * serving stale ones. */
    private static final LinkedHashMap<String, Tex> texcache = new LinkedHashMap<String, Tex>(128, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Tex> eldest) {
            return(size() > 256);
        }
    };

    private static Tex stroked(String text, Color c, Text.Foundry fnd, boolean halo) {
        String key = fnd.hashCode() + "|" + c.getRGB() + "|" + (halo ? 1 : 0) + "|" + text;
        Tex t = texcache.get(key);
        if(t == null) {
            Text.Line line = Text.renderstroked(text, c, Color.BLACK, fnd);
            t = halo ? PUtils.strokeTex(line) : line.tex();
            texcache.put(key, t);
        }
        return(t);
    }

    private static final Map<String, Tex> valuecache = new HashMap<>();

    /**
     * The opening percentage, sized to fill its tile.
     *
     * A Text.Line carries the font's leading and descent as transparent padding, so
     * scaling one to a box leaves the digits looking roughly a third too small. Laying
     * the glyphs out from their own outline instead means the box is the digits, and
     * the number reads at a glance from across the screen.
     */
    private static Tex openingValueTex(int pct, Coord tile) {
        String key = pct + "@" + tile.x + "x" + tile.y;
        Tex t = valuecache.get(key);
        if(t == null) {
            t = renderFitted(Integer.toString(pct), tile);
            valuecache.put(key, t);
        }
        return(t);
    }

    private static Tex renderFitted(String str, Coord tile) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        FontRenderContext frc = pg.getFontRenderContext();

        /* Size from the tile height alone, so 7, 62 and 100 all render at the same
         * height instead of the number visibly shrinking as an opening climbs past
         * 9 and 99. A bold sans cap height runs near 0.72em, so the first guess lands
         * close and the corrections only trim.
         *
         * The fractions leave a margin of tile colour showing all round the number -
         * the colour is what identifies the opening, so the digits must not crowd it
         * out. Sizing off the glyph outline means the stroke is the only thing between
         * the digits and the margin, so these read tighter than they look. */
        int boxh = (int)Math.round(tile.y * 0.66);
        float size = Math.max(1f, boxh * 1.4f);
        Font font = Text.sans.deriveFont(Font.BOLD, size);
        Rectangle ink = font.createGlyphVector(frc, str).getPixelBounds(null, 0, 0);
        for(int i = 0; (i < 3) && (ink.height > 0); i++) {
            double fit = boxh / (double)ink.height;
            if((fit > 0.99) && (fit < 1.01))
                break;
            size = Math.max(1f, (float)(size * fit));
            font = Text.sans.deriveFont(Font.BOLD, size);
            ink = font.createGlyphVector(frc, str).getPixelBounds(null, 0, 0);
        }

        /* Outline thickness tracks the glyph so the number stays readable over every
         * opening colour without swallowing the digits at small UI scales. */
        int pad = Math.min(UI.scale(2), Math.max(UI.scale(1), Math.round(size / 14f)));

        /* Longer numbers are condensed rather than scaled down - keeping full height
         * is what makes the value readable at a glance mid-fight. */
        int boxw = (int)Math.round(tile.x * 0.78) - (pad * 2);
        if((ink.width > boxw) && (ink.width > 0)) {
            font = font.deriveFont(AffineTransform.getScaleInstance(boxw / (double)ink.width, 1.0));
            ink = font.createGlyphVector(frc, str).getPixelBounds(null, 0, 0);
        }
        pg.dispose();

        Coord sz = new Coord(Math.max(1, ink.width) + (pad * 2), Math.max(1, ink.height) + (pad * 2));
        BufferedImage img = TexI.mkbuf(sz);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        int ox = pad - ink.x, oy = pad - ink.y;
        g.setColor(Color.BLACK);
        for(int dx = -pad; dx <= pad; dx += pad) {
            for(int dy = -pad; dy <= pad; dy += pad) {
                if((dx != 0) || (dy != 0))
                    g.drawString(str, ox + dx, oy + dy);
            }
        }
        g.setColor(Color.WHITE);
        g.drawString(str, ox, oy);
        g.dispose();
        return(new TexI(img));
    }

    /** Enemy opening percentages indexed by NCombatData colour slot; drives damage prediction. */
    public final int[] openingArr = new int[4];

    /* The enemy's combat meditation pulses once it is worth interrupting. */
    private int combatMedShift = 0;
    private boolean combatMedUp = true;

    /* Weapon loadout, refreshed on a timer because players swap weapons mid-fight. */
    private double loadoutChecked = 0;
    private String loadoutIssue = null;
    private String loadoutReported = null;
    private NCombatData.Loadout loadout = NCombatData.Loadout.UNKNOWN;

    public NFightsess(int nact) {
        super(nact);
        buffsAndInfo = new NFightsessBuffsAndInfo();
        actionsWidget = new NFightsessActions();
    }

    @Override
    public void draw(GOut g) {
        updatepos();
    }

    /* The fullscreen widget paints nothing, so it must not claim tooltips either -
     * the two panels own every hoverable element now. */
    @Override
    public Object tooltip(Coord c, Widget prev) {
        return(null);
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        pulseCombatMed();
        updateOpenings();
        updateLoadout();
    }

    private boolean incombat() {
        return((fv != null) && !fv.lsrel.isEmpty());
    }

    private void pulseCombatMed() {
        int fps = Math.max(1, NUtils.getFps());
        int step = 2400 / fps;
        if(combatMedUp) {
            combatMedShift += step;
            if(combatMedShift >= 255) {
                combatMedShift = 255;
                combatMedUp = false;
            }
        } else {
            combatMedShift -= step;
            if(combatMedShift <= 0) {
                combatMedShift = 0;
                combatMedUp = true;
            }
        }
    }

    /* Kept up to date here rather than inside the openings panel so damage prediction
     * still works when that panel is hidden. */
    private void updateOpenings() {
        Arrays.fill(openingArr, 0);
        if((fv == null) || (fv.current == null))
            return;
        for(Buff buff : fv.current.buffs.children(Buff.class)) {
            try {
                if(buff.res == null)
                    continue;
                int idx = NCombatData.openingIndex(buff.res.get().name);
                if(idx >= 0)
                    openingArr[idx] = openingValue(buff);
            } catch(Loading ignored) {
            }
        }
    }

    /* Resolved off this widget's own UI rather than the global accessor, so a second
     * session's equipment never leaks into this session's damage numbers. */
    private void updateLoadout() {
        double now = Utils.rtime();
        if(now - loadoutChecked < 1.0)
            return;
        loadoutChecked = now;
        /* A null read means "not determinable right now", not "unarmed" - keep the last
         * known weapon rather than briefly zeroing every damage estimate. */
        StringBuilder why = new StringBuilder();
        NCombatData.Loadout read = NCombatData.readLoadout(ui, why);
        if(read != null) {
            loadout = read;
            loadoutIssue = null;
            /* Announced when it first resolves and whenever the weapon changes. The fight log's
             * own banner is written on the first tick of a fight, before this has had a chance
             * to run, so without this the only record of the weapon is a stale "not read yet". */
            String now2 = (read.basedmg > 0)
                    ? String.format("%s base %d ql %.0f str %.0f",
                                    (read.weaponName == null) ? "weapon" : read.weaponName,
                                    read.basedmg, read.weaponQl, read.strength)
                    : "unarmed";
            if(!now2.equals(loadoutReported)) {
                loadoutReported = now2;
                System.out.println("[loadout] " + now2);
            }
        } else {
            /* Said once per distinct reason, not once per tick. */
            String issue = why.toString();
            if(!issue.equals(loadoutIssue)) {
                loadoutIssue = issue;
                System.out.println("[loadout] cannot read equipment: " + issue);
            }
        }
    }

    /** This session's opening percentages against the current target. */
    public int[] openings() {
        return(openingArr.clone());
    }

    /** This session's weapon/strength profile, as used for damage prediction. */
    public NCombatData.Loadout loadout() {
        return(loadout);
    }

    static int openingValue(Buff buff) {
        Double m = buff.ameteri.get();
        return((m == null) ? 0 : (int)(100 * m));
    }

    /**
     * Openings strongest-first so the one worth attacking is always nearest the centre,
     * with any stance pushed to the far end where it never shifts the others around.
     */
    private static List<Buff> layoutOpenings(Bufflist bl) {
        List<Buff> ls = new ArrayList<>(bl.children(Buff.class));
        ls.sort((a, b) -> Integer.compare(openingValue(b), openingValue(a)));
        Buff maneuver = null;
        for(Buff buff : ls) {
            try {
                if((buff.res != null) && NCombatData.MANEUVERS.contains(buff.res.get().name)) {
                    maneuver = buff;
                    break;
                }
            } catch(Loading ignored) {
            }
        }
        if((maneuver != null) && (ls.size() > 1)) {
            ls.remove(maneuver);
            ls.add(maneuver);
        }
        return(ls);
    }

    private static boolean flag(NConfig.Key key, boolean def) {
        Object v = NConfig.get(key);
        return((v instanceof Boolean) ? (Boolean)v : def);
    }

    private static String fmt1(double value) {
        double r = Math.round(value * 10) / 10.0;
        return((r % 1 == 0) ? Integer.toString((int)r) : Double.toString(r));
    }

    private static String fmt2(double value) {
        double r = Math.round(value * 100) / 100.0;
        return((r % 1 == 0) ? Integer.toString((int)r) : Double.toString(r));
    }

    /* ------------------------------------------------------------------ */

    public class NFightsessBuffsAndInfo extends Widget {
        /* Four openings plus a stance per side reach 278px out from the anchor, and the
         * meters are 234 wide, so 580 is the narrowest width that clips nothing. The
         * anchor sits 88px down so the last-move row (-80) and the stamina bar (+92)
         * both land inside the 180px height. */
        private static final int ANCHOR = 88;

        public NFightsessBuffsAndInfo() {
            super(UI.scale(580, 180));
        }

        private int anchor() {
            return(UI.scale(ANCHOR));
        }

        @Override
        public void draw(GOut g) {
            if(!incombat())
                return;
            NGameUI gui = ui.gui;
            if((gui == null) || (gui.map == null))
                return;

            double now = Utils.rtime();
            int x = sz.x / 2, y = anchor();
            boolean letters = flag(NConfig.Key.combatShowOpeningsAsLetters, false);

            int loc = -Buff.cframe.sz().x - UI.scale(80);
            for(Buff buff : layoutOpenings(fv.buffs)) {
                drawOpening(g, buff, new Coord(x + loc, y - UI.scale(20)), letters, false);
                loc -= UI.scale(40);
            }

            if(fv.current != null) {
                int rloc = UI.scale(80);
                for(Buff buff : layoutOpenings(fv.current.buffs)) {
                    drawOpening(g, buff, new Coord(x + rloc, y - UI.scale(20)), letters, true);
                    rloc += UI.scale(40);
                }
                Color mic = NConfig.getColor(NConfig.Key.combatColorMyIP, NCombatData.DEF_MYIP);
                Color eic = NConfig.getColor(NConfig.Key.combatColorEnemyIP, NCombatData.DEF_ENEMYIP);
                g.aimage(stroked(Integer.toString(fv.current.ip), mic, ipFoundry, true),
                         new Coord(x - UI.scale(40), y - UI.scale(30)), 1, 0.5);
                g.aimage(stroked(Integer.toString(fv.current.oip), eic, ipFoundry, true),
                         new Coord(x + UI.scale(40), y - UI.scale(30)), 0, 0.5);
            }

            Coord cdc = new Coord(x, y);
            if(now < fv.atkct) {
                double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
                g.chcolor(225, 0, 0, 220);
                g.fellipse(cdc, UI.scale(new Coord(24, 24)), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
                g.chcolor();
                g.aimage(stroked(fmt1(fv.atkct - now), Color.WHITE, Text.std, false), cdc, 0.5, 0.5);
            }
            g.image(cdframe, cdc.sub(cdframe.sz().div(2)));

            /* How long the last move actually took to come off cooldown - the raw
             * number nurgling derives its agility estimate from. */
            double lastcd = fv.atkct - fv.atkcs;
            if(lastcd > 0) {
                g.aimage(stroked(fmt2(lastcd), Color.WHITE, Text.std, false),
                         new Coord(x, y - UI.scale(40)), 0.5, 0.5);
            }

            drawLastAct(g, now, true, new Coord(x - UI.scale(69), y - UI.scale(80)));
            if(fv.current != null) {
                drawLastAct(g, now, false, new Coord(x + UI.scale(69), y - UI.scale(80)));
                if(flag(NConfig.Key.combatShowEstimatedAgility, true))
                    drawAgility(g, new Coord(x, y + UI.scale(34)));
            }

            if(flag(NConfig.Key.combatShowHealthBar, true)) {
                IMeter hpm = gui.getimeter("hp");
                IMeter.Meter hp = gui.getmeter("hp", 0);
                if((hpm != null) && (hp != null))
                    drawHealthBar(g, hpm, hp, new Coord(x - BARSZ.x / 2, y + UI.scale(44)));
            }
            if(flag(NConfig.Key.combatShowStaminaBar, true)) {
                IMeter.Meter stam = gui.getmeter("stam", 0);
                if(stam != null)
                    drawStamBar(g, stam, new Coord(x - BARSZ.x / 2, y + UI.scale(70)));
            }
        }

        private void drawOpening(GOut g, Buff buff, Coord pos, boolean letters, boolean enemy) {
            try {
                Resource res = buff.res.get();
                String name = res.name;
                Tex img = res.flayer(Resource.imgc).tex();
                Coord isz = img.sz();

                g.chcolor(255, 255, 255, 255);
                Double ameter = buff.ameteri.get();
                int pct = 0;
                if(ameter != null) {
                    pct = (int)(100 * ameter);
                    g.image(Buff.cframe, pos.sub(UI.scale(3), UI.scale(3)));
                    Coord mc = pos.add(0, UI.scale(34));
                    g.chcolor(0, 0, 0, 255);
                    g.frect(mc, Buff.ametersz);
                    g.chcolor(255, 255, 255, 255);
                    g.frect(mc, new Coord((int)Math.floor(ameter * Buff.ametersz.x), Buff.ametersz.y));
                } else {
                    g.image(Buff.frame, pos.sub(UI.scale(3), UI.scale(3)));
                }

                Color oc = NCombatData.openingColor(name);
                /* A flat colour tile has nothing else in it, so the percentage takes the
                 * whole square. A letter glyph or a stance icon already fills the tile,
                 * so there the number stays a small corner label. */
                boolean bare = false;
                if(oc != null) {
                    g.chcolor(oc);
                    Tex letter = letters ? NCombatData.letterTex(name) : null;
                    if(letter != null) {
                        g.image(letter, pos, isz);
                    } else {
                        g.frect(pos, isz);
                        bare = true;
                    }
                    g.chcolor(Color.WHITE);
                } else {
                    if(enemy && name.equals("paginae/atk/combmed") && (pct > 70))
                        g.chcolor(255, 255 - combatMedShift, 255 - combatMedShift, 255);
                    g.image(img, pos);
                    g.chcolor(255, 255, 255, 255);
                }

                if(pct > 0) {
                    if(bare)
                        g.aimage(openingValueTex(pct, isz), pos.add(isz.div(2)), 0.5, 0.5);
                    else
                        g.aimage(stroked(Integer.toString(pct), Color.WHITE, openingFoundry, false),
                                 pos.add(isz).sub(1, 1), 1, 1);
                }
            } catch(Loading ignored) {
            }
        }

        private void drawLastAct(GOut g, double now, boolean mine, Coord at) {
            try {
                Indir<Resource> lastact = mine ? fv.lastact : fv.current.lastact;
                if(mine) {
                    if(lastact != NFightsess.this.lastact1) {
                        NFightsess.this.lastact1 = lastact;
                        NFightsess.this.lastacttip1 = null;
                    }
                } else {
                    if(lastact != NFightsess.this.lastact2) {
                        NFightsess.this.lastact2 = lastact;
                        NFightsess.this.lastacttip2 = null;
                    }
                }
                if(lastact == null)
                    return;
                double lastuse = mine ? fv.lastuse : fv.current.lastuse;
                Tex ut = lastact.get().flayer(Resource.imgc).tex();
                /* Both icons are pinned to the inside edge of the gap, so the right
                 * one grows leftwards. */
                Coord useul = mine ? at : at.sub(ut.sz().x, 0);
                g.image(ut, useul);
                g.image(useframe, useul.sub(useframeo));
                double a = now - lastuse;
                if(a < 1) {
                    Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
                    g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
                    g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                    g.chcolor();
                }
            } catch(Loading ignored) {
            }
        }

        /* nurgling derives an agility factor from how far the observed cooldown drifts
         * from the move's book value; above 1 means the opponent is out-agiling you. */
        private void drawAgility(GOut g, Coord at) {
            g.aimage(stroked("Est. Agi: ", Color.WHITE, Text.std, false), at, 1, 0.5);
            double d = fv.current.agi_delta;
            if(d <= 0) {
                g.aimage(stroked("Unknown", Color.WHITE, Text.std, false), at, 0, 0.5);
            } else {
                Color c = (d > 1.0)
                        ? NConfig.getColor(NConfig.Key.combatColorEnemyIP, NCombatData.DEF_ENEMYIP)
                        : NConfig.getColor(NConfig.Key.combatColorMyIP, NCombatData.DEF_MYIP);
                g.aimage(stroked(fmt2(d) + "x", c, Text.std, false), at, 0, 0.5);
            }
        }

        /* Soft health and the sparring flag are read off this session's own meter widget,
         * not IMeter's statics - those belong to whichever session updated last. */
        private void drawHealthBar(GOut g, IMeter hpm, IMeter.Meter m, Coord sc) {
            int w1 = (int)Math.ceil(BARSZ.x * m.a);
            int w2 = (int)Math.ceil(BARSZ.x * (hpm.softHealthPercent / 100));
            if(hpm.isSparring) {
                /* Sparring cannot cost hard health, so the bar shows soft health only. */
                g.chcolor(hpBarGray);
                g.frect(sc, BARSZ);
                g.chcolor(hpBarGreen);
                g.frect(sc, new Coord(w2, BARSZ.y));
                g.chcolor(Color.BLACK);
                g.line(new Coord(sc.x + BARSZ.x, sc.y), new Coord(sc.x + BARSZ.x, sc.y + BARSZ.y), 2);
                g.rect(sc, BARSZ);
            } else {
                g.chcolor(hpBarYellow);
                g.frect(sc, new Coord(w1, BARSZ.y));
                g.chcolor(hpBarRed);
                g.frect(sc, new Coord(w2, BARSZ.y));
                g.chcolor(Color.BLACK);
                g.line(new Coord(sc.x + w1, sc.y), new Coord(sc.x + w1, sc.y + BARSZ.y), 2);
                g.rect(sc, BARSZ);
            }
            g.chcolor(Color.WHITE);
            String text = hpm.currentHealth;
            if(!hpm.isSparring && flag(NConfig.Key.combatIncludeHHPText, false))
                text += " (" + fmt1((int)(m.a * 100)) + "% HHP)";
            if(!text.isEmpty())
                g.aimage(stroked(text, Color.WHITE, Text.num12boldFnd, false), sc.add(BARSZ.div(2)), 0.5, 0.5);
            g.chcolor();
        }

        private void drawStamBar(GOut g, IMeter.Meter m, Coord sc) {
            int w1 = (int)Math.ceil(BARSZ.x * m.a);
            g.chcolor(stamBarBlue);
            g.frect(sc, new Coord(w1, BARSZ.y));
            g.chcolor(Color.BLACK);
            g.line(new Coord(sc.x + w1, sc.y), new Coord(sc.x + w1, sc.y + BARSZ.y), 2);
            g.rect(sc, BARSZ);
            g.chcolor(Color.WHITE);
            g.aimage(stroked(fmt1((int)(m.a * 100)), Color.WHITE, Text.num12boldFnd, false),
                     sc.add(BARSZ.div(2)), 0.5, 0.5);
            g.chcolor();
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            if(!incombat())
                return(null);
            int x = sz.x / 2, y = anchor();

            int loc = -Buff.cframe.sz().x - UI.scale(80);
            for(Buff buff : layoutOpenings(fv.buffs)) {
                Coord dc = new Coord(x + loc, y - UI.scale(20));
                if(c.isect(dc, buff.sz)) {
                    Object ret = buff.tooltip(c.sub(dc), NFightsess.this.prevtt);
                    if(ret != null) {
                        NFightsess.this.prevtt = buff;
                        return(ret);
                    }
                }
                loc -= UI.scale(40);
            }

            if(fv.current == null)
                return(null);

            int rloc = UI.scale(80);
            for(Buff buff : layoutOpenings(fv.current.buffs)) {
                Coord dc = new Coord(x + rloc, y - UI.scale(20));
                if(c.isect(dc, buff.sz)) {
                    Object ret = buff.tooltip(c.sub(dc), NFightsess.this.prevtt);
                    if(ret != null) {
                        NFightsess.this.prevtt = buff;
                        return(ret);
                    }
                }
                rloc += UI.scale(40);
            }

            try {
                Indir<Resource> lastact = NFightsess.this.lastact1;
                if(lastact != null) {
                    Coord usesz = lastact.get().flayer(Resource.imgc).sz;
                    if(c.isect(new Coord(x - UI.scale(69), y - UI.scale(80)), usesz)) {
                        if(NFightsess.this.lastacttip1 == null)
                            NFightsess.this.lastacttip1 = Text.render(lastact.get().flayer(Resource.tooltip).text());
                        return(NFightsess.this.lastacttip1);
                    }
                }
                lastact = NFightsess.this.lastact2;
                if(lastact != null) {
                    Coord usesz = lastact.get().flayer(Resource.imgc).sz;
                    if(c.isect(new Coord(x + UI.scale(69) - usesz.x, y - UI.scale(80)), usesz)) {
                        if(NFightsess.this.lastacttip2 == null)
                            NFightsess.this.lastacttip2 = Text.render(lastact.get().flayer(Resource.tooltip).text());
                        return(NFightsess.this.lastacttip2);
                    }
                }
            } catch(Loading ignored) {
            }
            return(null);
        }
    }

    /* ------------------------------------------------------------------ */

    public class NFightsessActions extends Widget {
        private boolean singlerow;

        public NFightsessActions() {
            super(Coord.z);
            this.singlerow = flag(NConfig.Key.combatSingleRowMoves, false);
            this.sz = sizefor(singlerow);
        }

        private Coord sizefor(boolean single) {
            int rl = single ? 10 : 5;
            int rows = Math.max(1, (actions.length + rl - 1) / rl);
            return(new Coord(UI.scale(32) + ((rl - 1) * apitch) + UI.scale(36),
                             UI.scale(10) + ((rows - 1) * apitch2) + UI.scale(32) + UI.scale(40)));
        }

        /* The icon's top-left corner, matching the fullscreen layout: columns are
         * centred on the panel, rows stack downwards from the top margin. */
        private Coord actc(int i, int rl) {
            int col = i % rl, row = i / rl;
            return(new Coord((sz.x / 2) - UI.scale(16) + (apitch * col) - (((rl - 1) * apitch) / 2),
                             UI.scale(10) + (row * apitch2)));
        }

        @Override
        public void tick(double dt) {
            super.tick(dt);
            boolean want = flag(NConfig.Key.combatSingleRowMoves, false);
            if(want != singlerow) {
                singlerow = want;
                /* Growing the content is not enough: the draggable frame around it
                 * carries its own size and would clip the extra columns. */
                Coord nsz = sizefor(singlerow);
                if(parent instanceof NDraggableWidget)
                    parent.resize(nsz.add(NDraggableWidget.delta));
                else
                    resize(nsz);
            }
        }

        @Override
        public void draw(GOut g) {
            if(!incombat() || (actions == null))
                return;
            double now = Utils.rtime();
            int rl = singlerow ? 10 : 5;
            boolean hotkeys = flag(NConfig.Key.combatShowHotkeys, true);
            boolean predict = flag(NConfig.Key.combatShowDamagePrediction, true);

            for(int i = 0; i < actions.length; i++) {
                Action act = actions[i];
                if(act == null)
                    continue;
                try {
                    Coord ca = actc(i, rl);
                    Tex img = act.res.get().flayer(Resource.imgc).tex();
                    Coord hsz = img.sz().div(2);
                    g.image(img, ca);
                    if(now < act.ct) {
                        double a = (now - act.cs) / (act.ct - act.cs);
                        g.chcolor(0, 0, 0, 132);
                        g.prect(ca.add(hsz), hsz.inv(), hsz, (1.0 - a) * Math.PI * 2);
                        g.chcolor();
                    }

                    int infoY = 0;
                    if(hotkeys && (i < kb_acts.length) && (kb_acts[i].key() != KeyMatch.nil)) {
                        infoY += 8;
                        g.aimage(stroked(kb_acts[i].key().name(), Color.WHITE, keybindFoundry, false),
                                 ca.add(img.sz().x / 2, img.sz().y + UI.scale(infoY)), 0.5, 0.5);
                    }
                    if(predict) {
                        String dmg = predictDamage(act.res.get().basename());
                        if(dmg != null) {
                            infoY += 12;
                            g.aimage(stroked(dmg, Color.RED, damageFoundry, false),
                                     ca.add(img.sz().x / 2, img.sz().y + UI.scale(infoY)), 0.5, 0.5);
                        }
                    }

                    if(i == use)
                        g.image(indframe, ca.sub(indframeo));
                    else if(i == useb)
                        g.image(indbframe, ca.sub(indbframeo));
                    else
                        g.image(actframe, ca.sub(actframeo));
                } catch(Loading ignored) {
                }
            }
        }

        private String predictDamage(String basename) {
            int dmg = NCombatData.predictedDamage(basename, openingArr, loadout);
            return((dmg < 0) ? null : Integer.toString(dmg));
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            if(!incombat() || (actions == null))
                return(null);
            int rl = singlerow ? 10 : 5;
            for(int i = 0; i < actions.length; i++) {
                Indir<Resource> act = (actions[i] == null) ? null : actions[i].res;
                if(act == null)
                    continue;
                try {
                    Coord ca = actc(i, rl);
                    Tex img = act.get().flayer(Resource.imgc).tex();
                    if(c.isect(ca, img.sz())) {
                        String tip = act.get().flayer(Resource.tooltip).text();
                        if((i < kb_acts.length) && (kb_acts[i].key() != KeyMatch.nil))
                            tip += " ($b{$col[255,128,0]{" + kb_acts[i].key().name() + "}})";
                        if((NFightsess.this.acttip == null) || !NFightsess.this.acttip.text.equals(tip))
                            NFightsess.this.acttip = RichText.render(tip, -1);
                        return(NFightsess.this.acttip);
                    }
                } catch(Loading ignored) {
                }
            }
            return(null);
        }
    }
}
