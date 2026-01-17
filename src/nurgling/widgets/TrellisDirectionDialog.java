package nurgling.widgets;

import haven.*;
import nurgling.NHitBox;
import nurgling.i18n.L10n;

public class TrellisDirectionDialog extends Window {
    // 0 = NS-East, 1 = NS-West, 2 = EW-North, 3 = EW-South, 4 = NS-Center, 5 = EW-Center
    private int orientation = 0;
    private int[] orientationRef = null;
    private boolean[] confirmRef = null;
    private Label directionLabel;
    private Button rotateButton;
    private Button confirmButton;

    private static final String[] ORIENTATION_NAMES = {
        "North-South (East)",
        "North-South (West)",
        "East-West (North)",
        "East-West (South)",
        "North-South (Center)",
        "East-West (Center)"
    };

    public TrellisDirectionDialog() {
        super(UI.scale(new Coord(250, 100)), L10n.get("trellis.direction_title"));
        initializeWidgets();
    }

    private void initializeWidgets() {
        int y = UI.scale(5);

        // Direction label
        directionLabel = new Label(ORIENTATION_NAMES[0]);
        add(directionLabel, UI.scale(new Coord(10, y)));
        y += UI.scale(25);

        // Rotate button
        rotateButton = new Button(UI.scale(115), L10n.get("common.rotate")) {
            @Override
            public void click() {
                toggleDirection();
            }
        };
        add(rotateButton, UI.scale(new Coord(10, y)));

        // Confirm button
        confirmButton = new Button(UI.scale(115), L10n.get("common.confirm")) {
            @Override
            public void click() {
                confirm();
            }
        };
        add(confirmButton, UI.scale(new Coord(130, y)));
    }

    public void setReferences(int[] orientationRef, boolean[] confirmRef) {
        this.orientationRef = orientationRef;
        this.confirmRef = confirmRef;
        this.orientation = orientationRef[0];
        updateDirectionLabel();
    }

    private void toggleDirection() {
        orientation = (orientation + 1) % 6;
        if (orientationRef != null) {
            orientationRef[0] = orientation;
        }
        updateDirectionLabel();
    }

    private void confirm() {
        if (confirmRef != null) {
            confirmRef[0] = true;
        }
        hide();
    }

    private void updateDirectionLabel() {
        directionLabel.settext(ORIENTATION_NAMES[orientation]);
    }

    @Override
    public boolean keydown(KeyDownEvent ev) {
        if(ev.code == java.awt.event.KeyEvent.VK_ESCAPE) {
            confirm(); // Confirm on escape
            return true;
        }
        return super.keydown(ev);
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if(msg.equals("close")) {
            confirm(); // Confirm on close
        } else {
            super.wdgmsg(msg, args);
        }
    }
}
