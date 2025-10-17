package nurgling.widgets;

import haven.*;
import nurgling.NHitBox;

public class TrellisDirectionDialog extends Window {
    private boolean isRotated = false;
    private boolean[] rotationRef = null;
    private boolean[] confirmRef = null;
    private Label directionLabel;
    private Button rotateButton;
    private Button confirmButton;

    public TrellisDirectionDialog() {
        super(UI.scale(new Coord(200, 100)), "Trellis Placement");
        initializeWidgets();
    }

    private void initializeWidgets() {
        int y = UI.scale(5);

        // Direction label
        directionLabel = new Label("Direction: North-South");
        add(directionLabel, UI.scale(new Coord(10, y)));
        y += UI.scale(25);

        // Rotate button
        rotateButton = new Button(UI.scale(85), "Rotate") {
            @Override
            public void click() {
                toggleDirection();
            }
        };
        add(rotateButton, UI.scale(new Coord(10, y)));

        // Confirm button
        confirmButton = new Button(UI.scale(85), "Confirm") {
            @Override
            public void click() {
                confirm();
            }
        };
        add(confirmButton, UI.scale(new Coord(105, y)));
    }

    public void setReferences(boolean[] rotationRef, boolean[] confirmRef) {
        this.rotationRef = rotationRef;
        this.confirmRef = confirmRef;
        this.isRotated = rotationRef[0];
        updateDirectionLabel();
    }

    private void toggleDirection() {
        isRotated = !isRotated;
        if (rotationRef != null) {
            rotationRef[0] = isRotated;
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
        String direction = isRotated ? "East-West" : "North-South";
        directionLabel.settext("Direction: " + direction);
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
