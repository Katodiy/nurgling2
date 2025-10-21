package nurgling.widgets;

import haven.*;
import nurgling.NConfig;

public class NCal extends Cal {
    @Override
    public void tick(double dt) {
        super.tick(dt);
        // TEST: Enable all icons
        eventNames.clear();
        eventNames.add("dawn");
        eventNames.add("mantle");
        eventNames.add("wolf");
        eventNames.add("rain");
    }

    @Override
    public boolean checkhit(Coord c) {
        // Account for the centered calendar graphic in the larger widget
        Coord center = sz.div(2).sub(bg.sz().div(2));
        return Utils.checkhit(dsky.scaled(), c.sub(center).sub(dsky.o));
    }

    @Override
    public void draw(GOut g) {
        Astronomy a = ui.sess.glob.ast;
        long now = System.currentTimeMillis();
        Coord center = sz.div(2).sub(bg.sz().div(2));
        g.image(a.night ? nsky : dsky, center);
        int mp = (int)Math.round(a.mp * (double)moon.f.length) % moon.f.length;
        Resource.Image moon = Cal.moon.f[mp][0];
        Resource.Image sun = Cal.sun.f[(int)((now / Cal.sun.d) % Cal.sun.f.length)][0];
        Coord mc = Coord.sc((a.dt + 0.25) * 2 * Math.PI, hbr).add(sz.div(2)).sub(moon.ssz.div(2));
        Coord sc = Coord.sc((a.dt + 0.75) * 2 * Math.PI, hbr).add(sz.div(2)).sub(sun.ssz.div(2));
        g.chcolor(a.mc);
        g.image(moon, mc);
        g.chcolor();
        g.image(sun, sc);

        g.image((a.night ? nlnd : dlnd)[a.is], center);
        g.image(bg, center);

        boolean verboseMode = false;
        Object verboseVal = NConfig.get(NConfig.Key.verboseCal);
        if(verboseVal instanceof Boolean) {
            verboseMode = (Boolean)verboseVal;
        }

        if(verboseMode) {
            drawVerboseMode(g, a, mp);
        } else {
            drawDefaultMode(g);
        }
    }

    private void drawDefaultMode(GOut g) {
        // Draw event icons in a 2x2 grid to the right of the calendar
        int horizontalSpacing = 30;
        int verticalSpacing = 30;
        int startX = sz.x / 2 + UI.scale(45);
        int startY = sz.y / 2 - UI.scale(10);
        int iconIndex = 0;
        for(String key : eventNames) {
            int row = iconIndex / 2;
            int col = iconIndex % 2;
            Coord iconPos = new Coord(startX + (col * horizontalSpacing), startY + (row * verticalSpacing));
            g.aimage(events.get(key), iconPos, 0.5, 0.5);
            iconIndex++;
        }
    }

    private void drawVerboseMode(GOut g, Astronomy a, int mp) {
        // Get season info - use existing calculated fields from Astronomy
        int daysLeft = a.srday;
        // Convert total game time to real time: game_seconds / server_ratio = real_seconds
        int totalGameSeconds = a.srday * 86400 + a.srhh * 3600 + a.srmm * 60;
        double realSeconds = totalGameSeconds / 3.29;
        double rlDaysLeft = realSeconds / 86400.0;

        // Get moon phase
        String moonPhase = Astronomy.phase[mp];

        // Left side text - right-aligned to point toward calendar
        int leftX = sz.x / 2 - UI.scale(40);
        int baseY = sz.y / 2;
        int lineHeight = UI.scale(16);

        // Line 1: "Season: Summer"
        String seasonLine = String.format("Season: %s", a.season());
        // Line 2: "58d left (17.8 RL days)"
        String daysLeftLine = String.format("%dd left (%.1f RL days)", daysLeft, rlDaysLeft);
        // Line 3: "Full Moon"
        String moonPhaseLine = String.format("Moon phase: %s", moonPhase);

        g.atext(seasonLine, new Coord(leftX, baseY - lineHeight), 1, 0.5);
        g.atext(daysLeftLine, new Coord(leftX, baseY), 1, 0.5);
        g.atext(moonPhaseLine, new Coord(leftX, baseY + lineHeight), 1, 0.5);

        // Draw event icons in a 2x2 grid to the right (same as default mode)
        int horizontalSpacing = 30;
        int verticalSpacing = 30;
        int startX = sz.x / 2 + UI.scale(45);
        int startY = sz.y / 2 - UI.scale(10);
        int iconIndex = 0;
        for(String key : eventNames) {
            int row = iconIndex / 2;
            int col = iconIndex % 2;
            Coord iconPos = new Coord(startX + (col * horizontalSpacing), startY + (row * verticalSpacing));
            g.aimage(events.get(key), iconPos, 0.5, 0.5);
            iconIndex++;
        }
    }
}
