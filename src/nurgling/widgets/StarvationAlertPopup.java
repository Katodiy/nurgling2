package nurgling.widgets;

import haven.*;

import java.awt.Color;
import java.awt.event.KeyEvent;

/**
 * Non-modal popup window for starvation warnings.
 * Displays a message with an OK button to dismiss.
 */
public class StarvationAlertPopup extends haven.Window {

    private static final int POPUP_WIDTH = 320;
    private static final int POPUP_HEIGHT = 140;
    private static final int MARGIN = 15;

    private final boolean critical;

    /**
     * Create a starvation alert popup.
     *
     * @param title    Window title
     * @param message  Message to display (can contain newlines)
     * @param critical If true, uses more urgent styling
     */
    public StarvationAlertPopup(String title, String message, boolean critical) {
        super(new Coord(UI.scale(POPUP_WIDTH), UI.scale(POPUP_HEIGHT)), title);
        this.critical = critical;

        int y = UI.scale(10);

        // Warning icon text for critical alerts
        if (critical) {
            Label warningLabel = add(new Label("!! WARNING !!"), new Coord(UI.scale(MARGIN), y));
            warningLabel.setcolor(new Color(255, 80, 80));
            y += UI.scale(24);
        }

        // Split message by newlines and add each line
        String[] lines = message.split("\n");
        for (String line : lines) {
            Label lineLabel = add(new Label(line), new Coord(UI.scale(MARGIN), y));
            if (critical) {
                lineLabel.setcolor(new Color(255, 200, 200));
            }
            y += UI.scale(18);
        }

        // Add some spacing before button
        y += UI.scale(15);

        // OK button centered
        int btnWidth = UI.scale(80);
        int btnX = (UI.scale(POPUP_WIDTH) - btnWidth) / 2;
        add(new Button(btnWidth, "OK") {
            @Override
            public void click() {
                close();
            }
        }, new Coord(btnX, y));

        pack();
    }

    @Override
    public boolean keydown(Widget.KeyDownEvent ev) {
        // Close on ESC or Enter
        if (ev.code == KeyEvent.VK_ESCAPE || ev.code == KeyEvent.VK_ENTER) {
            close();
            return true;
        }
        return super.keydown(ev);
    }

    /**
     * Close and destroy the popup
     */
    public void close() {
        hide();
        destroy();
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            close();
        } else {
            super.wdgmsg(msg, args);
        }
    }

}
