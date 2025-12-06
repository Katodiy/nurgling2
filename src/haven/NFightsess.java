package haven;

import nurgling.*;
import nurgling.overlays.*;

public class NFightsess extends Fightsess {

    public NFightsessBuffsAndInfo buffsAndInfo;
    public NFightsessActions actionsWidget;

    public NFightsess(int nact) {
        super(nact);
        buffsAndInfo = new NFightsessBuffsAndInfo(this);
        actionsWidget = new NFightsessActions(this);
    }

    @Override
    public void draw(GOut g) {
        updatepos();
    }

    public class NFightsessBuffsAndInfo extends Widget {
        private final Fightsess parent;

        public NFightsessBuffsAndInfo(Fightsess parent) {
            super(UI.scale(500, 200));
            this.parent = parent;
        }

        @Override
        public void draw(GOut g) {
            if(parent.fv == null) return;
            GameUI gui = NUtils.getGameUI();
            if(gui == null || gui.map == null) return;
            // Don't draw if not in combat
            if(parent.fv.lsrel.isEmpty()) return;
            double now = Utils.rtime();
            Coord guisz = gui.map.sz;
            // Center of widget
            Coord center = sz.div(2);
            Coord buffc = center.sub(0, UI.scale(50));

            // Draw player buffs on the left
            for(Buff buff : parent.fv.buffs.children(Buff.class)) {
                Coord pos = buffc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), 0);
                NRelation.RelBuff rb = parent.fv.altbuffs.get(buff);
                if(rb == null) {
                    buff.draw(g.reclip(pos, buff.sz));
                } else {
                    Coord bsz = buff.sz.sub(0, UI.scale(5));
                    g.reclip(pos, bsz).image(rb.bg, Coord.z, bsz);
                    g.reclip(pos, bsz).image(rb.text, bsz.div(2).sub(rb.text.sz().div(2)));
                }
            }

            // Draw opponent info if exists
            if(parent.fv.current != null) {
                // Draw opponent buffs on the right
                for(Buff buff : parent.fv.current.buffs.children(Buff.class)) {
                    Coord pos = buffc.add(buff.c.x + UI.scale(20), 0);
                    NRelation.RelBuff rb = parent.fv.altrelbuffs.get(buff);
                    if(rb == null) {
                        buff.draw(g.reclip(pos, buff.sz));
                    } else {
                        Coord bsz = buff.sz.sub(0, UI.scale(5));
                        g.reclip(pos, bsz).image(rb.bg, Coord.z, bsz);
                        g.reclip(pos, bsz).image(rb.text, bsz.div(2).sub(rb.text.sz().div(2)));
                    }
                }

                // Draw IP text below lastact icons
                g.aimage(parent.ip.get().tex(), center.add(-UI.scale(70), UI.scale(60)), 1, 0.5);
                g.aimage(parent.oip.get().tex(), center.add(UI.scale(70), UI.scale(60)), 0, 0.5);
            }

            // Draw openings timer
            {
                Coord cdc = center.add(0, UI.scale(20));
                if(now < parent.fv.atkct) {
                    double a = (now - parent.fv.atkcs) / (parent.fv.atkct - parent.fv.atkcs);
                    g.chcolor(255, 0, 128, 224);
                    g.fellipse(cdc, UI.scale(new Coord(24, 24)), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
                    g.chcolor();
                    g.aimage(NStyle.openings.render(String.format("%.1f", (parent.fv.atkct - now))).tex(), cdc, 0.5, 0.5);
                }
                g.image(cdframe, cdc.sub(cdframe.sz().div(2)));
            }

            // Draw player last action
            try {
                Indir<Resource> lastact = parent.fv.lastact;
                if(lastact != parent.lastact1) {
                    parent.lastact1 = lastact;
                    parent.lastacttip1 = null;
                }
                double lastuse = parent.fv.lastuse;
                if(lastact != null) {
                    Tex ut = lastact.get().flayer(Resource.imgc).tex();
                    Coord useul = center.add(-UI.scale(65), UI.scale(20)).sub(ut.sz().div(2));
                    g.image(ut, useul);
                    g.image(useframe, useul.sub(useframeo));
                    double a = now - lastuse;
                    if(a < 1) {
                        Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
                        g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
                        g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                        g.chcolor();
                    }
                }
            } catch(Loading l) {
            }

            // Draw opponent last action
            if(parent.fv.current != null) {
                try {
                    Indir<Resource> lastact = parent.fv.current.lastact;
                    if(lastact != parent.lastact2) {
                        parent.lastact2 = lastact;
                        parent.lastacttip2 = null;
                    }
                    double lastuse = parent.fv.current.lastuse;
                    if(lastact != null) {
                        Tex ut = lastact.get().flayer(Resource.imgc).tex();
                        Coord useul = center.add(UI.scale(65), UI.scale(20)).sub(ut.sz().div(2));
                        g.image(ut, useul);
                        g.image(useframe, useul.sub(useframeo));
                        double a = now - lastuse;
                        if(a < 1) {
                            Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
                            g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
                            g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                            g.chcolor();
                        }
                    }
                } catch(Loading l) {
                }
            }
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            if(parent.fv == null) return null;
            // Don't show tooltips if not in combat
            if(parent.fv.lsrel.isEmpty()) return null;

            Coord center = sz.div(2);
            Coord buffc = center.sub(0, UI.scale(50));

            // Check player buffs
            for(Buff buff : parent.fv.buffs.children(Buff.class)) {
                Coord pos = buffc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), 0);
                Coord dc = pos;
                if(c.isect(dc, buff.sz)) {
                    Object ret = buff.tooltip(c.sub(dc), parent.prevtt);
                    if(ret != null) {
                        parent.prevtt = buff;
                        return ret;
                    }
                }
            }

            // Check opponent buffs
            if(parent.fv.current != null) {
                for(Buff buff : parent.fv.current.buffs.children(Buff.class)) {
                    Coord pos = buffc.add(buff.c.x + UI.scale(20), 0);
                    Coord dc = pos;
                    if(c.isect(dc, buff.sz)) {
                        Object ret = buff.tooltip(c.sub(dc), parent.prevtt);
                        if(ret != null) {
                            parent.prevtt = buff;
                            return ret;
                        }
                    }
                }

                // Check player last action tooltip
                {
                    Indir<Resource> lastact = parent.lastact1;
                    if(lastact != null) {
                        Coord usesz = lastact.get().flayer(Resource.imgc).sz;
                        Coord lac = center.add(-UI.scale(65), UI.scale(20));
                        if(c.isect(lac.sub(usesz.div(2)), usesz)) {
                            if(parent.lastacttip1 == null)
                                parent.lastacttip1 = Text.render(lastact.get().flayer(Resource.tooltip).t);
                            return parent.lastacttip1;
                        }
                    }
                }

                // Check opponent last action tooltip
                {
                    Indir<Resource> lastact = parent.lastact2;
                    if(lastact != null) {
                        Coord usesz = lastact.get().flayer(Resource.imgc).sz;
                        Coord lac = center.add(UI.scale(65), UI.scale(20));
                        if(c.isect(lac.sub(usesz.div(2)), usesz)) {
                            if(parent.lastacttip2 == null)
                                parent.lastacttip2 = Text.render(lastact.get().flayer(Resource.tooltip).t);
                            return parent.lastacttip2;
                        }
                    }
                }
            }
            return null;
        }
    }

    public class NFightsessActions extends Widget {
        private final Fightsess parent;

        public NFightsessActions(Fightsess parent) {
            super(UI.scale(300, 200));
            this.parent = parent;
        }

        @Override
        public void draw(GOut g) {
            // Don't draw if not in combat
            if(parent.fv == null || parent.fv.lsrel.isEmpty()) return;
            
            double now = Utils.rtime();
            Coord center = sz.div(2);

            // Draw action buttons
            if(parent.actions == null) return;
            for(int i = 0; i < parent.actions.length; i++) {
                // Calculate position within widget (not using actc which is for screen coords)
                int rl = 5; // row length
                int col = i % rl;
                int row = i / rl;
                // Position relative to center, starting from top
                Coord relPos = new Coord((actpitch * col) - (((rl - 1) * actpitch) / 2), 
                                         UI.scale(-20) + (row * actpitch));
                Coord ca = center.add(relPos);
                Action act = parent.actions[i];
                try {
                    if(act != null) {
                        Resource res = act.res.get();
                        Tex img = res.flayer(Resource.imgc).tex();
                        Coord ic = ca.sub(img.sz().div(2));
                        g.image(img, ic);
                        if(now < act.ct) {
                            double a = (now - act.cs) / (act.ct - act.cs);
                            g.chcolor(0, 0, 0, 128);
                            g.prect(ca, ic.sub(ca), ic.add(img.sz()).sub(ca), (1.0 - a) * Math.PI * 2);
                            g.chcolor();
                        }
                        if(i == parent.use) {
                            g.image(indframe, ic.sub(indframeo));
                        } else if(i == parent.useb) {
                            g.image(indbframe, ic.sub(indbframeo));
                        } else {
                            g.image(actframe, ic.sub(actframeo));
                        }
                        
                        // Draw hotkey label below the icon
                        if(i < Fightsess.keytips.length && Fightsess.kb_acts[i].key() != KeyMatch.nil) {
                            String keytext = Fightsess.keytips[i];
                            Text keylabel = NStyle.meter.render(keytext);
                            // Position label below the frame, centered
                            Coord labelpos = ic.add(img.sz().x / 2, img.sz().y + UI.scale(8));
                            g.aimage(keylabel.tex(), labelpos, 0.5, 0.5);
                        }
                    }
                } catch(Loading l) {
                }
            }
        }

        @Override
        public Object tooltip(Coord c, Widget prev) {
            // Don't show tooltips if not in combat
            if(parent.fv == null || parent.fv.lsrel.isEmpty()) return null;
            
            Coord center = sz.div(2);
            final int rl = 5;
            for(int i = 0; i < parent.actions.length; i++) {
                // Calculate position within widget (same as in draw)
                int col = i % rl;
                int row = i / rl;
                Coord relPos = new Coord((actpitch * col) - (((rl - 1) * actpitch) / 2), 
                                         UI.scale(-20) + (row * actpitch));
                Coord ca = center.add(relPos);
                Indir<Resource> act = (parent.actions[i] == null) ? null : parent.actions[i].res;
                if(act != null) {
                    Tex img = act.get().flayer(Resource.imgc).tex();
                    ca = ca.sub(img.sz().div(2));
                    if(c.isect(ca, img.sz())) {
                        String tip = act.get().flayer(Resource.tooltip).t;
                        if(kb_acts[i].key() != KeyMatch.nil)
                            tip += " ($b{$col[255,128,0]{" + kb_acts[i].key().name() + "}})";
                        if((parent.acttip == null) || !parent.acttip.text.equals(tip))
                            parent.acttip = RichText.render(tip, -1);
                        return parent.acttip;
                    }
                }
            }
            return null;
        }
    }
}
