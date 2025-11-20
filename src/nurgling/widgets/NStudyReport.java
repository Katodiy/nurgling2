package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.resutil.Curiosity;
import nurgling.*;

import java.awt.*;

public class NStudyReport extends Widget {
    public Widget study;
    public int totalExp = 0;
    public int totalAttention = 0;
    public int totalExpCost = 0;
    
    private static final int PADDING = UI.scale(20);
    private static final int INFO_HEIGHT = UI.scale(60);
    private Widget fixedContainer;
    private ICheckBox btnLock;
    public boolean locked = false;
    
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
        
        fixedContainer.add(study, new Coord(PADDING, PADDING));
        
        int infoY = studySz.y+ 3 * PADDING/2;
        int labelX = PADDING;
        int valueX = containerSz.x - PADDING;
        int lineHeight = UI.scale(18);
        
        Widget plbl, pval;
        plbl = fixedContainer.add(new Label("Attention:"), new Coord(labelX, infoY));
        pval = fixedContainer.adda(new CharWnd.RLabel<Pair<Integer, Integer>>(
            () -> new Pair<>(totalAttention, (ui == null) ? 0 : ui.sess.glob.getcattr("int").comp),
            n -> String.format("%,d/%,d", n.a, n.b),
            new Color(255, 192, 255, 255)),
            new Coord(valueX, infoY), 1.0, 0.0);
        
        infoY += lineHeight;
        plbl = fixedContainer.add(new Label("Exp cost:"), new Coord(labelX, infoY));
        pval = fixedContainer.adda(new CharWnd.RLabel<Integer>(
            () -> totalExpCost,
            Utils::thformat,
            new Color(255, 255, 192, 255)),
            new Coord(valueX, infoY), 1.0, 0.0);
        
        infoY += lineHeight;
        plbl = fixedContainer.add(new Label("LP:"), new Coord(labelX, infoY));
        pval = fixedContainer.adda(new CharWnd.RLabel<Integer>(
            () -> totalExp,
            Utils::thformat,
            new Color(192, 192, 255, 255)),
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
        
        if(study != null) {
            for(GItem item : study.children(GItem.class)) {
                try {
                    Curiosity ci = ItemInfo.find(Curiosity.class, item.info());
                    if(ci != null) {
                        totalExp += ci.exp;
                        totalAttention += ci.mw;
                        totalExpCost += ci.enc;
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
        NDraggableWidget.drawBg(g, sz);
        haven.Window.wbox.draw(g, Coord.z, sz);
        super.draw(g);
    }
    
    @Override
    public boolean mousedown(MouseDownEvent ev) {
        if(locked && study != null) {
            Coord studyPos = study.parentpos(this);
            if(ev.c.isect(studyPos, study.sz)) {
                return true;
            }
        }
        return super.mousedown(ev);
    }
}

