package nurgling.widgets;

import haven.*;
import nurgling.i18n.L10n;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * A dialog widget for selecting a quantity using a slider
 */
public class NQuantitySelector extends Window {
    
    private final int maxValue;
    private int currentValue;
    private final Consumer<Integer> onConfirm;
    
    private final Label valueLabel;
    private final HSlider slider;
    
    public NQuantitySelector(String title, int maxValue, Consumer<Integer> onConfirm) {
        super(UI.scale(new Coord(250, 100)), title);
        this.maxValue = maxValue;
        this.currentValue = Math.min(maxValue, 1);
        this.onConfirm = onConfirm;
        
        int y = UI.scale(10);
        int margin = UI.scale(10);
        
        // Value label
        add(new Label("1"), new Coord(margin, y));
        valueLabel = add(new Label(String.valueOf(currentValue)), new Coord(UI.scale(120), y));
        add(new Label(String.valueOf(maxValue)), new Coord(UI.scale(200), y));
        
        y += UI.scale(25);
        
        // Slider
        slider = add(new HSlider(UI.scale(230), 1, maxValue, currentValue) {
            @Override
            public void changed() {
                currentValue = val;
                valueLabel.settext(String.valueOf(currentValue));
            }
        }, new Coord(margin, y));
        
        y += UI.scale(30);
        
        // Buttons
        int buttonWidth = UI.scale(80);
        int spacing = UI.scale(20);
        int totalWidth = buttonWidth * 2 + spacing;
        int startX = (UI.scale(250) - totalWidth) / 2;
        
        add(new Button(buttonWidth, "OK") {
            @Override
            public void click() {
                confirm();
            }
        }, new Coord(startX, y));
        
        add(new Button(buttonWidth, L10n.get("common.cancel")) {
            @Override
            public void click() {
                cancel();
            }
        }, new Coord(startX + buttonWidth + spacing, y));
        
        pack();
    }
    
    private void confirm() {
        hide();
        onConfirm.accept(currentValue);
        reqdestroy();
    }
    
    private void cancel() {
        hide();
        reqdestroy();
    }
    
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            cancel();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
    
    @Override
    public boolean keydown(KeyDownEvent ev) {
        if (ev.code == java.awt.event.KeyEvent.VK_ESCAPE) {
            cancel();
            return true;
        } else if (ev.code == java.awt.event.KeyEvent.VK_ENTER) {
            confirm();
            return true;
        }
        return super.keydown(ev);
    }
}

