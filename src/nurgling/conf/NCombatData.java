package nurgling.conf;

import haven.*;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.widgets.NEquipory;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Static combat reference data for the combat HUD: which buffs are maneuvers rather
 * than openings, which colour each opening carries, and the damage profile of every
 * attack move.
 *
 * The four openings are ordered green / yellow / red / blue throughout; that ordering
 * is the index into the opening-percentage array the damage predictor reads, so it must
 * stay in sync between {@link #openingIndex} and the {@link AttackInfo} colour arrays.
 */
public class NCombatData {
    /** Buffs that occupy the openings row but are stances, not openings. Drawn last. */
    public static final Set<String> MANEUVERS = new HashSet<>(Arrays.asList(
            "paginae/atk/toarms", "paginae/atk/shield", "paginae/atk/parry",
            "paginae/atk/oakstance", "paginae/atk/dorg", "paginae/atk/chinup",
            "paginae/atk/bloodlust", "paginae/atk/combmed"));

    public static final String OFFBALANCE = "paginae/atk/offbalance";
    public static final String REELING    = "paginae/atk/reeling";
    public static final String CORNERED   = "paginae/atk/cornered";
    public static final String DIZZY      = "paginae/atk/dizzy";

    public static final int GREEN = 0, YELLOW = 1, RED = 2, BLUE = 3;

    public static final Color DEF_GREEN  = new Color(0, 128, 3, 255);
    public static final Color DEF_YELLOW = new Color(217, 177, 20, 255);
    public static final Color DEF_RED    = new Color(192, 28, 28, 255);
    public static final Color DEF_BLUE   = new Color(39, 82, 191, 255);
    public static final Color DEF_MYIP    = new Color(0, 201, 4, 255);
    public static final Color DEF_ENEMYIP = new Color(245, 0, 0, 255);

    /** Opening resource name to its slot in the opening-percentage array, or -1. */
    public static int openingIndex(String resnm) {
        if(resnm == null)
            return(-1);
        switch(resnm) {
        case OFFBALANCE: return(GREEN);
        case REELING:    return(YELLOW);
        case CORNERED:   return(RED);
        case DIZZY:      return(BLUE);
        default:         return(-1);
        }
    }

    /** User-configured colour for an opening, or null when the buff is not an opening. */
    public static Color openingColor(String resnm) {
        switch(openingIndex(resnm)) {
        case GREEN:  return(NConfig.getColor(NConfig.Key.combatColorOffbalance, DEF_GREEN));
        case YELLOW: return(NConfig.getColor(NConfig.Key.combatColorReeling, DEF_YELLOW));
        case RED:    return(NConfig.getColor(NConfig.Key.combatColorCornered, DEF_RED));
        case BLUE:   return(NConfig.getColor(NConfig.Key.combatColorDizzy, DEF_BLUE));
        default:     return(null);
        }
    }

    private static final Map<Integer, Tex> letters = new HashMap<>();

    /**
     * Blocky white glyph on a black tile, used when openings are shown as letters
     * instead of flat colour. Generated rather than shipped as artwork so the tile
     * tracks the UI scale and needs no resource-pipeline entry; the caller tints it,
     * which leaves the letter coloured and the tile black.
     */
    public static Tex letterTex(String resnm) {
        int idx = openingIndex(resnm);
        if(idx < 0)
            return(null);
        synchronized(letters) {
            Tex t = letters.get(idx);
            if(t == null) {
                t = renderLetter("GYRB".charAt(idx));
                letters.put(idx, t);
            }
            return(t);
        }
    }

    private static Tex renderLetter(char ch) {
        int s = UI.scale(32);
        String str = String.valueOf(ch);
        BufferedImage img = TexI.mkbuf(new Coord(s, s));
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, s, s);
        g.setFont(Text.sans.deriveFont(Font.BOLD, s * 0.86f));
        FontMetrics m = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(str, (s - m.stringWidth(str)) / 2, ((s - m.getHeight()) / 2) + m.getAscent());
        g.dispose();
        return(new TexI(img));
    }

    public static class AttackInfo {
        /** Indices into the opening-percentage array that this move scales off. */
        public final int[] colors;
        /** Melee moves multiply weapon damage; unarmed moves deal a flat amount. */
        public final boolean mc;
        public final int dmg;
        public final double dmgMul;

        /** Unarmed: fixed damage, no weapon multiplier. */
        public AttackInfo(int[] colors, int dmg) {
            this.colors = colors;
            this.dmg = dmg;
            this.dmgMul = 0;
            this.mc = false;
        }

        /** Melee: multiplier applied to the equipped weapon's damage. */
        public AttackInfo(int[] colors, double dmgMul) {
            this.colors = colors;
            this.dmgMul = dmgMul;
            this.dmg = 0;
            this.mc = true;
        }
    }

    public static final Map<String, AttackInfo> ATTACKS = new HashMap<>();
    static {
        ATTACKS.put("fullcircle", new AttackInfo(new int[]{RED, YELLOW}, 1.0d));
        ATTACKS.put("knockteeth", new AttackInfo(new int[]{RED}, 30));
        ATTACKS.put("cleave",     new AttackInfo(new int[]{RED, BLUE}, 1.5d));
        ATTACKS.put("gojug",      new AttackInfo(new int[]{RED, GREEN}, 40));
        ATTACKS.put("chop",       new AttackInfo(new int[]{GREEN}, 1.0d));
        ATTACKS.put("haymaker",   new AttackInfo(new int[]{YELLOW}, 20));
        ATTACKS.put("kick",       new AttackInfo(new int[]{YELLOW}, 25));
        ATTACKS.put("lefthook",   new AttackInfo(new int[]{BLUE}, 15));
        ATTACKS.put("lowblow",    new AttackInfo(new int[]{BLUE}, 20));
        ATTACKS.put("pow",        new AttackInfo(new int[]{GREEN}, 10));
        ATTACKS.put("punchboth",  new AttackInfo(new int[]{GREEN, YELLOW}, 10));
        ATTACKS.put("barrage",    new AttackInfo(new int[]{RED}, 0.25d));
        ATTACKS.put("ravenbite",  new AttackInfo(new int[]{GREEN, YELLOW}, 1.1d));
        ATTACKS.put("ripapart",   new AttackInfo(new int[]{GREEN, YELLOW, RED, BLUE}, 50));
        ATTACKS.put("sideswipe",  new AttackInfo(new int[]{YELLOW}, 0.75d));
        ATTACKS.put("sting",      new AttackInfo(new int[]{GREEN, BLUE}, 1.25d));
        ATTACKS.put("sos",        new AttackInfo(new int[]{YELLOW, BLUE}, 1.0d));
        ATTACKS.put("takedown",   new AttackInfo(new int[]{YELLOW, RED}, 40));
        ATTACKS.put("uppercut",   new AttackInfo(new int[]{GREEN, BLUE}, 30));
    }

    /** Everything outside the move table that a damage estimate depends on. */
    public static class Loadout {
        /** Weapon damage normalised back to ql 10, or 0 when no weapon is held. */
        public final int basedmg;
        public final double weaponQl;
        public final double strength;
        /** Display name of the held weapon, or null. Lets callers tell an axe from a spear. */
        public final String weaponName;
        /**
         * False before the equipment has ever been read successfully. Distinguishing this
         * from "no weapon" matters: for the first second of a session the equipment window
         * does not exist yet, and reporting that as unarmed is simply wrong.
         */
        public final boolean known;

        public Loadout(int basedmg, double weaponQl, double strength) {
            this(basedmg, weaponQl, strength, true, null);
        }

        public Loadout(int basedmg, double weaponQl, double strength, boolean known) {
            this(basedmg, weaponQl, strength, known, null);
        }

        public Loadout(int basedmg, double weaponQl, double strength, boolean known, String weaponName) {
            this.basedmg = basedmg;
            this.weaponQl = weaponQl;
            this.strength = strength;
            this.known = known;
            this.weaponName = weaponName;
        }

        /** Placeholder used until the equipment has been read for the first time. */
        public static final Loadout UNKNOWN = new Loadout(0, 10, 1, false, null);
    }

    /**
     * Reads strength and the held weapon for one session. Resolved off the passed UI
     * rather than the global accessor, so a second session's equipment never leaks into
     * this session's numbers.
     */
    public static Loadout readLoadout(UI ui) {
        return(readLoadout(ui, null));
    }

    /**
     * @param why when non-null, receives the reason a null is being returned. A loadout that
     *            never resolves silently zeroes every damage estimate, and the symptom - a
     *            fighter that ranks its moves oddly - looks nothing like the cause, so the
     *            cause is worth being able to ask for.
     */
    public static Loadout readLoadout(UI ui, StringBuilder why) {
        double strength = 1;
        try {
            Glob.CAttr str = ui.sess.glob.getcattr("str");
            if(str != null)
                strength = Math.max(1, str.comp);
        } catch(Loading ignored) {
        }
        NEquipory eq = equipory(ui);
        if(eq == null) {
            if(why != null) {
                why.append((ui == null) ? "no ui"
                        : (ui.gui == null) ? "no gui"
                        : (ui.gui.equwnd == null) ? "equipment window not created yet"
                        : "equipment window has no Equipory child");
            }
            return(null);
        }
        try {
            for(NEquipory.Slots slot : new NEquipory.Slots[]{NEquipory.Slots.HAND_LEFT, NEquipory.Slots.HAND_RIGHT}) {
                WItem wi = eq.quickslots[slot.idx];
                if((wi == null) || (wi.item == null))
                    continue;
                int dmg = weaponDamage(wi.item.info);
                if(dmg <= 0)
                    continue;
                double ql = 10;
                if((wi.item instanceof NGItem) && (((NGItem)wi.item).quality != null))
                    ql = Math.max(1, ((NGItem)wi.item).quality);
                String nm = (wi.item instanceof NGItem) ? ((NGItem)wi.item).name() : null;
                return(new Loadout((int)Math.ceil(dmg / Math.sqrt(ql / 10)), ql, strength, true, nm));
            }
        } catch(Loading l) {
            /* Item info still streaming. Reporting "unarmed" here would silently zero every
             * weapon damage estimate, so report "unknown" and let the caller keep what it had. */
            if(why != null)
                why.append("item info still loading (").append(l.getMessage()).append(")");
            return(null);
        }
        return(new Loadout(0, 10, strength));
    }

    private static NEquipory equipory(UI ui) {
        if((ui == null) || (ui.gui == null) || (ui.gui.equwnd == null))
            return(null);
        for(Widget w = ui.gui.equwnd.lchild; w != null; w = w.prev) {
            if(w instanceof NEquipory)
                return((NEquipory)w);
        }
        return(null);
    }

    /**
     * Expected damage of a move against a set of opponent openings. Multi-colour moves
     * combine their openings as the chance that at least one applies; the result is squared
     * because damage scales with the square of the opening.
     *
     * @return the estimate, or -1 when the move is not a damaging attack.
     */
    public static int predictedDamage(String basename, int[] openings, Loadout lo) {
        AttackInfo attack = ATTACKS.get(basename);
        if((attack == null) || (openings == null) || (lo == null))
            return(-1);
        double opening;
        if(attack.colors.length > 1) {
            opening = 1;
            for(int slot : attack.colors)
                opening *= 1.0 - (openings[slot] / 100.0);
            opening = 1.0 - opening;
        } else {
            opening = openings[attack.colors[0]] / 100.0;
        }
        double mul = opening * opening;
        if(attack.mc) {
            double wdmg = lo.basedmg * Math.sqrt(Math.sqrt(lo.weaponQl * lo.strength) / 10);
            return((int)Math.ceil(wdmg * attack.dmgMul * mul));
        }
        return((int)Math.ceil(attack.dmg * Math.sqrt(lo.strength / 10) * mul));
    }

    /**
     * Pulls the "dmg" field off the dynamically loaded Damage tooltip class. The class
     * is compiled from a resource at runtime, so it can only be reached reflectively.
     */
    public static int weaponDamage(List<ItemInfo> info) {
        if(info == null)
            return(0);
        for(ItemInfo inf : info) {
            if(!inf.getClass().getSimpleName().equals("Damage"))
                continue;
            try {
                Field f = inf.getClass().getField("dmg");
                return(((Number)f.get(inf)).intValue());
            } catch(NoSuchFieldException | IllegalAccessException | ClassCastException e) {
                return(0);
            }
        }
        return(0);
    }
}
