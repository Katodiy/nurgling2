package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.resutil.Curiosity;
import nurgling.*;
import nurgling.i18n.L10n;

import java.awt.*;
import java.util.function.Predicate;

public class NStudyReport extends Widget {
    public Widget study;
    public int totalExp = 0;
    public int totalAttention = 0;
    public int totalExpCost = 0;
    public int totalLph = 0;
    
    private static final int PADDING = UI.scale(20);
    private static final int INFO_HEIGHT = UI.scale(100);
    private Widget fixedContainer;
    private ICheckBox btnLock;
    public boolean locked = false;
    
    // Mirror widget that displays study without being its parent
    private class StudyMirror extends Widget implements DTarget {
        public StudyMirror(Coord sz) {
            super(sz);
        }
        
        @Override
        public void draw(GOut g) {
            if(study != null && study.visible) {
                try {
                    study.draw(g);
                } catch(Exception e) {
                    // Ignore drawing errors
                }
            }
        }
        
        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if(locked)
                return true;
            if(study != null && ev.c.isect(Coord.z, sz)) {
                MouseDownEvent forwarded = new MouseDownEvent(ev.c, ev.b);
                try {
                    return forwarded.dispatch(study);
                } catch(Exception e) {
                    // Ignore errors
                }
            }
            return super.mousedown(ev);
        }
        
        @Override
        public boolean mouseup(MouseUpEvent ev) {
            if(locked)
                return super.mouseup(ev);
            if(study != null && ev.c.isect(Coord.z, sz)) {
                MouseUpEvent forwarded = new MouseUpEvent(ev.c, ev.b);
                try {
                    return forwarded.dispatch(study);
                } catch(Exception e) {
                    // Ignore errors
                }
            }
            return super.mouseup(ev);
        }
        
        @Override
        public void mousemove(MouseMoveEvent ev) {
            if(!locked && study != null) {
                MouseMoveEvent forwarded = new MouseMoveEvent(ev.c);
                try {
                    forwarded.dispatch(study);
                } catch(Exception e) {
                    // Ignore errors
                }
            }
            super.mousemove(ev);
        }
        
        @Override
        public Object tooltip(Coord c, Widget prev) {
            if(!locked && study != null && c.isect(Coord.z, sz)) {
                for(Widget wdg = study.lchild; wdg != null; wdg = wdg.prev) {
                    if(!wdg.visible)
                        continue;
                    Coord cc = c.sub(wdg.c);
                    if(cc.isect(Coord.z, wdg.sz)) {
                        Object tooltip = wdg.tooltip(cc, prev);
                        if(tooltip != null)
                            return tooltip;
                    }
                }
            }
            return super.tooltip(c, prev);
        }
        
        @Override
        public boolean drop(Coord cc, Coord ul) {
            if(locked)
                return false;
            if(study instanceof DTarget) {
                try {
                    return ((DTarget)study).drop(cc, ul);
                } catch(Exception e) {
                    // Ignore errors
                }
            }
            return false;
        }
        
        @Override
        public boolean iteminteract(Coord cc, Coord ul) {
            if(locked)
                return false;
            if(study instanceof DTarget) {
                try {
                    return ((DTarget)study).iteminteract(cc, ul);
                } catch(Exception e) {
                    // Ignore errors
                }
            }
            return false;
        }
    }
    
    public NStudyReport(Widget study) {
        this.study = study;
        
        Coord studySz = study.sz;
        Coord containerSz = new Coord(studySz.x + PADDING * 2, studySz.y + INFO_HEIGHT + PADDING * 2);

        fixedContainer = new Widget(containerSz) {
            @Override
            public void pack() {
                // Don't auto-resize
            }
        };
        
        // Add mirror widget that will display study
        fixedContainer.add(new StudyMirror(studySz), new Coord(PADDING, PADDING));
        
        int infoY = studySz.y + PADDING + UI.scale(5);
        int labelX = PADDING;
        int valueX = containerSz.x - PADDING;
        int lineHeight = UI.scale(20);
        
        Widget plbl, pval;
        plbl = fixedContainer.add(new Label(L10n.get("study.attention")), new Coord(labelX, infoY));
        pval = fixedContainer.adda(new CharWnd.RLabel<Pair<Integer, Integer>>(
            () -> new Pair<>(totalAttention, (ui == null) ? 0 : ui.sess.glob.getcattr("int").comp),
            n -> String.format("%,d/%,d", n.a, n.b),
            new Color(255, 192, 255, 255)),
            new Coord(valueX, infoY), 1.0, 0.0);
        
        infoY += lineHeight;
        plbl = fixedContainer.add(new Label(L10n.get("study.exp_cost")), new Coord(labelX, infoY));
        pval = fixedContainer.adda(new CharWnd.RLabel<Integer>(
            () -> totalExpCost,
            Utils::thformat,
            new Color(255, 255, 192, 255)),
            new Coord(valueX, infoY), 1.0, 0.0);
        
        infoY += lineHeight;
        plbl = fixedContainer.add(new Label(L10n.get("study.lp")), new Coord(labelX, infoY));
        pval = fixedContainer.adda(new CharWnd.RLabel<Integer>(
            () -> totalExp,
            Utils::thformat,
            new Color(192, 192, 255, 255)),
            new Coord(valueX, infoY), 1.0, 0.0);
        
        infoY += lineHeight;
        plbl = fixedContainer.add(new Label(L10n.get("study.lph")), new Coord(labelX, infoY));
        pval = fixedContainer.adda(new CharWnd.RLabel<Integer>(
            () -> totalLph,
            Utils::thformat,
            new Color(192, 255, 192, 255)),
            new Coord(valueX, infoY), 1.0, 0.0);
        
        add(fixedContainer, Coord.z);
        resize(containerSz);
        
        btnLock = add(new ICheckBox(NStyle.locki[0], NStyle.locki[1], NStyle.locki[2], NStyle.locki[3]) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                locked = val;
            }
        }, new Coord(containerSz.x - NStyle.locki[0].sz().x - UI.scale(5), UI.scale(8)));
    }
    
    private void updateStudyItems() {
        totalExp = 0;
        totalAttention = 0;
        totalExpCost = 0;
        totalLph = 0;
        
        if(study != null) {
            for(GItem item : study.children(GItem.class)) {
                try {
                    nurgling.iteminfo.NCuriosity ci = ItemInfo.find(nurgling.iteminfo.NCuriosity.class, item.info());
                    if(ci != null) {
                        totalExp += ci.exp;
                        totalAttention += ci.mw;
                        totalExpCost += ci.enc;
                        totalLph += nurgling.iteminfo.NCuriosity.lph(ci.lph);
                    }
                } catch(Loading l) {
                }
            }
        }
    }

    @Override
    public void tick(double dt) {
        updateStudyItems();
        super.tick(dt);
    }
    
    @Override
    public void draw(GOut g) {
        NDraggableWidget.drawBg(g, sz, ui);
        haven.Window.wbox.draw(g, Coord.z, sz);
        super.draw(g);
    }
    
}

