package nurgling.widgets;

import haven.*;

import java.awt.Color;

public class TunnelingDialog extends Window {
    public enum Direction {
        NORTH("North", 0, -1),
        SOUTH("South", 0, 1),
        EAST("East", 1, 0),
        WEST("West", -1, 0);

        public final String name;
        public final int dx;
        public final int dy;

        Direction(String name, int dx, int dy) {
            this.name = name;
            this.dx = dx;
            this.dy = dy;
        }

        public Direction opposite() {
            switch (this) {
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case EAST: return WEST;
                case WEST: return EAST;
                default: return this;
            }
        }

        public boolean isVertical() {
            return this == NORTH || this == SOUTH;
        }

        public boolean isHorizontal() {
            return this == EAST || this == WEST;
        }
    }

    public enum TunnelSide {
        EAST("East", 1, 0),
        WEST("West", -1, 0),
        NORTH("North", 0, -1),
        SOUTH("South", 0, 1);

        public final String name;
        public final int dx;
        public final int dy;

        TunnelSide(String name, int dx, int dy) {
            this.name = name;
            this.dx = dx;
            this.dy = dy;
        }
    }

    public enum WingOption {
        NONE("None"),
        EAST("East only"),
        WEST("West only"),
        BOTH_EW("Both East & West"),
        NORTH("North only"),
        SOUTH("South only"),
        BOTH_NS("Both North & South");

        public final String name;

        WingOption(String name) {
            this.name = name;
        }
    }

    public enum SupportType {
        MINE_SUPPORT("Mine Support", "gfx/terobjs/minesupport", 100),
        STONE_COLUMN("Stone Column", "gfx/terobjs/column", 125),
        MINE_BEAM("Mine Beam", "gfx/terobjs/minebeam", 150);

        public final String menuName;
        public final String resourcePath;
        public final int radius;

        SupportType(String menuName, String resourcePath, int radius) {
            this.menuName = menuName;
            this.resourcePath = resourcePath;
            this.radius = radius;
        }
    }

    // Current selections
    private Direction selectedDirection = Direction.NORTH;
    private TunnelSide selectedTunnelSide = TunnelSide.EAST;
    private TunnelSide selectedWingSide = TunnelSide.NORTH;
    private SupportType selectedSupportType = SupportType.MINE_SUPPORT;
    private boolean wingNorth = false;
    private boolean wingSouth = false;
    private boolean wingEast = false;
    private boolean wingWest = false;

    // Reference arrays for communication with bot
    private int[] directionRef = null;
    private int[] tunnelSideRef = null;
    private int[] supportTypeRef = null;
    private int[] wingOptionRef = null;
    private int[] wingSideRef = null;
    private boolean[] confirmRef = null;
    private boolean[] cancelRef = null;

    // UI Elements
    private Button btnNorth, btnSouth, btnEast, btnWest;
    private Button btnTunnelLeft, btnTunnelRight;
    private Button btnWingN, btnWingS, btnWingE, btnWingW;
    private Button btnWingSideA, btnWingSideB;
    private Dropbox<SupportType> supportTypeDropbox;
    private PreviewGrid previewGrid;
    private Label tunnelSideLabel;
    private Label wingSideLabel;

    // Colors for preview grid
    private static final Color COLOR_BG = new Color(40, 40, 40);
    private static final Color COLOR_SUPPORT = new Color(139, 90, 43); // Brown
    private static final Color COLOR_TUNNEL = new Color(100, 149, 237); // Blue
    private static final Color COLOR_WING = new Color(50, 205, 50); // Green
    private static final Color COLOR_ARROW = new Color(255, 215, 0); // Gold

    // Custom widget for visual grid preview
    private class PreviewGrid extends Widget {
        private static final int CELL_SIZE = 12;
        private static final int GRID_SIZE = 7; // 7x7 grid

        public PreviewGrid() {
            super(new Coord(CELL_SIZE * GRID_SIZE, CELL_SIZE * GRID_SIZE));
        }

        @Override
        public void draw(GOut g) {
            // Fill background
            g.chcolor(COLOR_BG);
            g.frect(Coord.z, sz);

            // Draw grid lines
            g.chcolor(Color.DARK_GRAY);
            for (int i = 0; i <= GRID_SIZE; i++) {
                g.line(new Coord(i * CELL_SIZE, 0), new Coord(i * CELL_SIZE, sz.y), 1);
                g.line(new Coord(0, i * CELL_SIZE), new Coord(sz.x, i * CELL_SIZE), 1);
            }

            int center = GRID_SIZE / 2; // Center cell (3 for 7x7)

            boolean isVertical = selectedDirection.isVertical();

            if (isVertical) {
                drawVerticalPreview(g, center);
            } else {
                drawHorizontalPreview(g, center);
            }

            g.chcolor();
        }

        private void drawVerticalPreview(GOut g, int center) {
            // For N/S travel: supports are vertically aligned, tunnel parallel, wings go E/W
            boolean tunnelEast = (selectedTunnelSide == TunnelSide.EAST);
            int tunnelX = tunnelEast ? center + 1 : center - 1;
            int supportX = center;

            // Wing side offset: for N/S travel, wing side is N or S (vertical offset)
            int wingYOffset = (selectedWingSide == TunnelSide.NORTH) ? -1 : 1;

            // Draw supports (vertical line in center)
            g.chcolor(COLOR_SUPPORT);
            for (int y = 1; y < GRID_SIZE - 1; y += 2) {
                fillCell(g, supportX, y);
            }

            // Draw tunnel (vertical line offset from center)
            g.chcolor(COLOR_TUNNEL);
            for (int y = 1; y < GRID_SIZE - 1; y++) {
                fillCell(g, tunnelX, y);
            }

            // Draw wings (horizontal, offset from supports by wing side)
            // Wings connect to tunnel and extend outward to edge
            g.chcolor(COLOR_WING);
            for (int y = 1; y < GRID_SIZE - 1; y += 2) {
                int wingY = y + wingYOffset;
                if (wingY < 0 || wingY >= GRID_SIZE) continue;

                if (wingWest) {
                    // West wing: from X=0 to tunnel (inclusive)
                    for (int x = 0; x <= tunnelX; x++) {
                        fillCell(g, x, wingY);
                    }
                }
                if (wingEast) {
                    // East wing: from tunnel to edge (inclusive)
                    for (int x = tunnelX; x < GRID_SIZE; x++) {
                        fillCell(g, x, wingY);
                    }
                }
            }

            // Draw direction arrow
            g.chcolor(COLOR_ARROW);
            int arrowY = (selectedDirection == Direction.NORTH) ? 0 : GRID_SIZE - 1;
            drawArrow(g, tunnelX, arrowY, selectedDirection);
        }

        private void drawHorizontalPreview(GOut g, int center) {
            // For E/W travel: supports are horizontally aligned, tunnel parallel, wings go N/S
            boolean tunnelSouth = (selectedTunnelSide == TunnelSide.SOUTH);
            int tunnelY = tunnelSouth ? center + 1 : center - 1;
            int supportY = center;

            // Wing side offset: for E/W travel, wing side is E or W (horizontal offset)
            int wingXOffset = (selectedWingSide == TunnelSide.WEST) ? -1 : 1;

            // Draw supports (horizontal line in center)
            g.chcolor(COLOR_SUPPORT);
            for (int x = 1; x < GRID_SIZE - 1; x += 2) {
                fillCell(g, x, supportY);
            }

            // Draw tunnel (horizontal line offset from center)
            g.chcolor(COLOR_TUNNEL);
            for (int x = 1; x < GRID_SIZE - 1; x++) {
                fillCell(g, x, tunnelY);
            }

            // Draw wings (vertical, offset from supports by wing side)
            // Wings connect to tunnel and extend outward to edge
            g.chcolor(COLOR_WING);
            for (int x = 1; x < GRID_SIZE - 1; x += 2) {
                int wingX = x + wingXOffset;
                if (wingX < 0 || wingX >= GRID_SIZE) continue;

                if (wingNorth) {
                    // North wing: from Y=0 to tunnel (inclusive)
                    for (int y = 0; y <= tunnelY; y++) {
                        fillCell(g, wingX, y);
                    }
                }
                if (wingSouth) {
                    // South wing: from tunnel to edge (inclusive)
                    for (int y = tunnelY; y < GRID_SIZE; y++) {
                        fillCell(g, wingX, y);
                    }
                }
            }

            // Draw direction arrow
            g.chcolor(COLOR_ARROW);
            int arrowX = (selectedDirection == Direction.EAST) ? GRID_SIZE - 1 : 0;
            drawArrow(g, arrowX, tunnelY, selectedDirection);
        }

        private void fillCell(GOut g, int gridX, int gridY) {
            g.frect(new Coord(gridX * CELL_SIZE + 1, gridY * CELL_SIZE + 1),
                    new Coord(CELL_SIZE - 2, CELL_SIZE - 2));
        }

        private void drawArrow(GOut g, int gridX, int gridY, Direction dir) {
            int cx = gridX * CELL_SIZE + CELL_SIZE / 2;
            int cy = gridY * CELL_SIZE + CELL_SIZE / 2;
            int size = CELL_SIZE / 2 - 1;

            Coord center = new Coord(cx, cy);
            Coord tip, left, right;

            switch (dir) {
                case NORTH:
                    tip = new Coord(cx, cy - size);
                    left = new Coord(cx - size, cy + size);
                    right = new Coord(cx + size, cy + size);
                    break;
                case SOUTH:
                    tip = new Coord(cx, cy + size);
                    left = new Coord(cx - size, cy - size);
                    right = new Coord(cx + size, cy - size);
                    break;
                case EAST:
                    tip = new Coord(cx + size, cy);
                    left = new Coord(cx - size, cy - size);
                    right = new Coord(cx - size, cy + size);
                    break;
                case WEST:
                    tip = new Coord(cx - size, cy);
                    left = new Coord(cx + size, cy - size);
                    right = new Coord(cx + size, cy + size);
                    break;
                default:
                    return;
            }

            g.line(tip, left, 2);
            g.line(tip, right, 2);
            g.line(left, right, 2);
        }
    }

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final SupportType[] SUPPORT_TYPES = SupportType.values();

    // Tunnel side options depend on direction
    private static final TunnelSide[] VERTICAL_TUNNEL_SIDES = {TunnelSide.EAST, TunnelSide.WEST};
    private static final TunnelSide[] HORIZONTAL_TUNNEL_SIDES = {TunnelSide.NORTH, TunnelSide.SOUTH};

    // Wing side options depend on direction
    private static final TunnelSide[] VERTICAL_WING_SIDES = {TunnelSide.NORTH, TunnelSide.SOUTH};
    private static final TunnelSide[] HORIZONTAL_WING_SIDES = {TunnelSide.EAST, TunnelSide.WEST};

    // Wing options depend on direction
    private static final WingOption[] VERTICAL_WING_OPTIONS = {
            WingOption.NONE, WingOption.EAST, WingOption.WEST, WingOption.BOTH_EW
    };
    private static final WingOption[] HORIZONTAL_WING_OPTIONS = {
            WingOption.NONE, WingOption.NORTH, WingOption.SOUTH, WingOption.BOTH_NS
    };

    public TunnelingDialog() {
        super(UI.scale(new Coord(340, 370)), "Tunneling Bot");
        initializeWidgets();
    }

    private void initializeWidgets() {
        int y = UI.scale(5);

        // === DIRECTION COMPASS ===
        add(new Label("Dig towards:"), new Coord(UI.scale(10), y));
        y += UI.scale(18);

        int compassCenterX = UI.scale(60);
        int compassCenterY = y + UI.scale(25);
        int btnSize = UI.scale(30);
        int spacing = UI.scale(32);

        // North button
        btnNorth = new Button(btnSize, "\u2191") {
            @Override
            public void click() {
                selectDirection(Direction.NORTH);
            }
        };
        add(btnNorth, new Coord(compassCenterX - btnSize/2, compassCenterY - spacing - btnSize/2));

        // South button
        btnSouth = new Button(btnSize, "\u2193") {
            @Override
            public void click() {
                selectDirection(Direction.SOUTH);
            }
        };
        add(btnSouth, new Coord(compassCenterX - btnSize/2, compassCenterY + spacing - btnSize/2));

        // West button
        btnWest = new Button(btnSize, "\u2190") {
            @Override
            public void click() {
                selectDirection(Direction.WEST);
            }
        };
        add(btnWest, new Coord(compassCenterX - spacing - btnSize/2, compassCenterY - btnSize/2));

        // East button
        btnEast = new Button(btnSize, "\u2192") {
            @Override
            public void click() {
                selectDirection(Direction.EAST);
            }
        };
        add(btnEast, new Coord(compassCenterX + spacing - btnSize/2, compassCenterY - btnSize/2));

        // === TUNNEL SIDE SELECTOR ===
        int tunnelSideX = UI.scale(150);
        add(new Label("Tunnel side:"), new Coord(tunnelSideX, y));
        y += UI.scale(18);

        tunnelSideLabel = new Label("");
        add(tunnelSideLabel, new Coord(tunnelSideX, y));

        btnTunnelLeft = new Button(UI.scale(40), "") {
            @Override
            public void click() {
                selectTunnelSide(0);
            }
        };
        add(btnTunnelLeft, new Coord(tunnelSideX, y + UI.scale(15)));

        btnTunnelRight = new Button(UI.scale(40), "") {
            @Override
            public void click() {
                selectTunnelSide(1);
            }
        };
        add(btnTunnelRight, new Coord(tunnelSideX + UI.scale(50), y + UI.scale(15)));

        y += UI.scale(70);

        // === SUPPORT TYPE ===
        add(new Label("Support:"), new Coord(UI.scale(10), y + UI.scale(3)));
        int dropWidth = UI.scale(180);
        int dropHeight = UI.scale(18);
        supportTypeDropbox = new Dropbox<SupportType>(dropWidth, SUPPORT_TYPES.length, dropHeight) {
            @Override
            protected SupportType listitem(int i) {
                return SUPPORT_TYPES[i];
            }

            @Override
            protected int listitems() {
                return SUPPORT_TYPES.length;
            }

            @Override
            protected void drawitem(GOut g, SupportType item, int idx) {
                g.text(item.menuName + " (r:" + (item.radius / 11) + " tiles)", new Coord(3, 1));
            }

            @Override
            public void change(SupportType item) {
                super.change(item);
                selectedSupportType = item;
                updatePreview();
            }
        };
        supportTypeDropbox.change(selectedSupportType);
        add(supportTypeDropbox, new Coord(UI.scale(70), y));
        y += UI.scale(28);

        // === WING TOGGLE CROSS ===
        add(new Label("Wings (click to toggle):"), new Coord(UI.scale(10), y));
        y += UI.scale(18);

        int wingCenterX = UI.scale(60);
        int wingCenterY = y + UI.scale(25);
        int wingBtnSize = UI.scale(28);
        int wingSpacing = UI.scale(30);

        // Wing North button
        btnWingN = new Button(wingBtnSize, "N") {
            @Override
            public void click() {
                wingNorth = !wingNorth;
                updateWingButtons();
                updatePreview();
            }
        };
        add(btnWingN, new Coord(wingCenterX - wingBtnSize/2, wingCenterY - wingSpacing - wingBtnSize/2));

        // Wing South button
        btnWingS = new Button(wingBtnSize, "S") {
            @Override
            public void click() {
                wingSouth = !wingSouth;
                updateWingButtons();
                updatePreview();
            }
        };
        add(btnWingS, new Coord(wingCenterX - wingBtnSize/2, wingCenterY + wingSpacing - wingBtnSize/2));

        // Wing West button
        btnWingW = new Button(wingBtnSize, "W") {
            @Override
            public void click() {
                wingWest = !wingWest;
                updateWingButtons();
                updatePreview();
            }
        };
        add(btnWingW, new Coord(wingCenterX - wingSpacing - wingBtnSize/2, wingCenterY - wingBtnSize/2));

        // Wing East button
        btnWingE = new Button(wingBtnSize, "E") {
            @Override
            public void click() {
                wingEast = !wingEast;
                updateWingButtons();
                updatePreview();
            }
        };
        add(btnWingE, new Coord(wingCenterX + wingSpacing - wingBtnSize/2, wingCenterY - wingBtnSize/2));

        // Center indicator
        add(new Label("\u2588"), new Coord(wingCenterX - UI.scale(4), wingCenterY - UI.scale(7)));

        // === WING SIDE SELECTOR ===
        int wingSideX = UI.scale(150);
        wingSideLabel = new Label("Wing side:");
        add(wingSideLabel, new Coord(wingSideX, y));

        btnWingSideA = new Button(UI.scale(40), "") {
            @Override
            public void click() {
                selectWingSide(0);
            }
        };
        add(btnWingSideA, new Coord(wingSideX, y + UI.scale(15)));

        btnWingSideB = new Button(UI.scale(40), "") {
            @Override
            public void click() {
                selectWingSide(1);
            }
        };
        add(btnWingSideB, new Coord(wingSideX + UI.scale(50), y + UI.scale(15)));

        y += UI.scale(70);

        // === PREVIEW GRID ===
        add(new Label("Preview:"), new Coord(UI.scale(10), y));

        // Add legend
        add(new Label("\u25A0=Support"), new Coord(UI.scale(80), y));
        add(new Label("\u25A0=Tunnel"), new Coord(UI.scale(160), y));
        add(new Label("\u25A0=Wings"), new Coord(UI.scale(230), y));
        y += UI.scale(15);

        previewGrid = new PreviewGrid();
        add(previewGrid, new Coord(UI.scale(10), y));
        y += UI.scale(90);

        // === BUTTONS ===
        Button confirmButton = new Button(UI.scale(100), "Start") {
            @Override
            public void click() {
                confirm();
            }
        };
        add(confirmButton, new Coord(UI.scale(60), y));

        Button cancelButton = new Button(UI.scale(100), "Cancel") {
            @Override
            public void click() {
                cancel();
            }
        };
        add(cancelButton, new Coord(UI.scale(180), y));

        // Initialize
        selectDirection(Direction.NORTH);
        updateTunnelSideButtons();
        updateWingSideButtons();
        updateWingButtons();
        updatePreview();
    }

    private void selectDirection(Direction dir) {
        selectedDirection = dir;

        // Reset tunnel side and wing side to first option for new direction
        TunnelSide[] tunnelSides = dir.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
        TunnelSide[] wingSides = dir.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
        selectedTunnelSide = tunnelSides[0];
        selectedWingSide = wingSides[0];

        // Reset wings - only show applicable wings for direction
        wingNorth = false;
        wingSouth = false;
        wingEast = false;
        wingWest = false;

        updateDirectionButtons();
        updateTunnelSideButtons();
        updateWingSideButtons();
        updateWingButtons();
        updatePreview();
    }

    private void selectTunnelSide(int index) {
        TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
        if (index >= 0 && index < sides.length) {
            selectedTunnelSide = sides[index];
            updateTunnelSideButtons();
            updatePreview();
        }
    }

    private void selectWingSide(int index) {
        TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
        if (index >= 0 && index < sides.length) {
            selectedWingSide = sides[index];
            updateWingSideButtons();
            updatePreview();
        }
    }

    private void updateDirectionButtons() {
        if (btnNorth == null) return; // Not yet initialized
        // Visual feedback - selected direction gets highlighted
        updateButtonHighlight(btnNorth, selectedDirection == Direction.NORTH);
        updateButtonHighlight(btnSouth, selectedDirection == Direction.SOUTH);
        updateButtonHighlight(btnEast, selectedDirection == Direction.EAST);
        updateButtonHighlight(btnWest, selectedDirection == Direction.WEST);
    }

    private void updateButtonHighlight(Button btn, boolean selected) {
        if (btn == null || btn.text == null) return;
        // We can't easily change button color, but we can change text
        // Selected buttons will have brackets
        String base = btn.text.text.replace("[", "").replace("]", "");
        btn.change(selected ? "[" + base + "]" : base);
    }

    private void updateTunnelSideButtons() {
        if (btnTunnelLeft == null) return; // Not yet initialized
        TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
        btnTunnelLeft.change(sides[0].name.substring(0, 1));
        btnTunnelRight.change(sides[1].name.substring(0, 1));

        updateButtonHighlight(btnTunnelLeft, selectedTunnelSide == sides[0]);
        updateButtonHighlight(btnTunnelRight, selectedTunnelSide == sides[1]);

        tunnelSideLabel.settext("(" + sides[0].name + " / " + sides[1].name + ")");
    }

    private void updateWingSideButtons() {
        if (btnWingSideA == null) return; // Not yet initialized
        TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
        btnWingSideA.change(sides[0].name.substring(0, 1));
        btnWingSideB.change(sides[1].name.substring(0, 1));

        updateButtonHighlight(btnWingSideA, selectedWingSide == sides[0]);
        updateButtonHighlight(btnWingSideB, selectedWingSide == sides[1]);
    }

    private void updateWingButtons() {
        if (btnWingN == null) return; // Not yet initialized
        // Only enable wing buttons that are perpendicular to travel direction
        boolean isVertical = selectedDirection.isVertical();

        // For vertical travel (N/S), wings go E/W
        // For horizontal travel (E/W), wings go N/S
        btnWingN.show(!isVertical);
        btnWingS.show(!isVertical);
        btnWingE.show(isVertical);
        btnWingW.show(isVertical);

        // Update visual state
        if (isVertical) {
            updateButtonHighlight(btnWingE, wingEast);
            updateButtonHighlight(btnWingW, wingWest);
        } else {
            updateButtonHighlight(btnWingN, wingNorth);
            updateButtonHighlight(btnWingS, wingSouth);
        }
    }

    private void updatePreview() {
        // PreviewGrid reads state directly and redraws automatically
        // Nothing to do here - the widget's draw() method handles everything
    }

    private String getDirectionArrow() {
        switch (selectedDirection) {
            case NORTH: return "\u2191";
            case SOUTH: return "\u2193";
            case EAST: return "\u2192";
            case WEST: return "\u2190";
            default: return "?";
        }
    }

    public void setReferences(int[] directionRef, int[] tunnelSideRef, int[] supportTypeRef,
                              int[] wingOptionRef, int[] wingSideRef, boolean[] confirmRef, boolean[] cancelRef) {
        this.directionRef = directionRef;
        this.tunnelSideRef = tunnelSideRef;
        this.supportTypeRef = supportTypeRef;
        this.wingOptionRef = wingOptionRef;
        this.wingSideRef = wingSideRef;
        this.confirmRef = confirmRef;
        this.cancelRef = cancelRef;
    }

    private void confirm() {
        // Convert current selections to indices for the bot
        if (directionRef != null) {
            directionRef[0] = selectedDirection.ordinal();
        }
        if (tunnelSideRef != null) {
            TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
            for (int i = 0; i < sides.length; i++) {
                if (sides[i] == selectedTunnelSide) {
                    tunnelSideRef[0] = i;
                    break;
                }
            }
        }
        if (supportTypeRef != null) {
            supportTypeRef[0] = selectedSupportType.ordinal();
        }
        if (wingOptionRef != null) {
            wingOptionRef[0] = calculateWingOptionIndex();
        }
        if (wingSideRef != null) {
            TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
            for (int i = 0; i < sides.length; i++) {
                if (sides[i] == selectedWingSide) {
                    wingSideRef[0] = i;
                    break;
                }
            }
        }
        if (confirmRef != null) {
            confirmRef[0] = true;
        }
        hide();
    }

    private int calculateWingOptionIndex() {
        // Convert wing booleans to wing option index
        if (selectedDirection.isVertical()) {
            // Wings are E/W
            if (wingEast && wingWest) return 3; // BOTH_EW
            if (wingEast) return 1; // EAST
            if (wingWest) return 2; // WEST
            return 0; // NONE
        } else {
            // Wings are N/S
            if (wingNorth && wingSouth) return 3; // BOTH_NS
            if (wingNorth) return 1; // NORTH
            if (wingSouth) return 2; // SOUTH
            return 0; // NONE
        }
    }

    private void cancel() {
        if (cancelRef != null) {
            cancelRef[0] = true;
        }
        hide();
    }

    @Override
    public boolean keydown(KeyDownEvent ev) {
        if (ev.code == java.awt.event.KeyEvent.VK_ESCAPE) {
            cancel();
            return true;
        }
        return super.keydown(ev);
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            cancel();
        } else {
            super.wdgmsg(msg, args);
        }
    }

    // Static accessors for bot to use
    public static Direction getDirection(int index) {
        if (index >= 0 && index < DIRECTIONS.length) {
            return DIRECTIONS[index];
        }
        return DIRECTIONS[0];
    }

    public static TunnelSide getTunnelSide(int directionIndex, int tunnelSideIndex) {
        Direction dir = getDirection(directionIndex);
        TunnelSide[] sides = dir.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
        if (tunnelSideIndex >= 0 && tunnelSideIndex < sides.length) {
            return sides[tunnelSideIndex];
        }
        return sides[0];
    }

    public static SupportType getSupportType(int index) {
        if (index >= 0 && index < SUPPORT_TYPES.length) {
            return SUPPORT_TYPES[index];
        }
        return SUPPORT_TYPES[0];
    }

    public static WingOption getWingOption(int directionIndex, int wingOptionIndex) {
        Direction dir = getDirection(directionIndex);
        WingOption[] options = dir.isVertical() ? VERTICAL_WING_OPTIONS : HORIZONTAL_WING_OPTIONS;
        if (wingOptionIndex >= 0 && wingOptionIndex < options.length) {
            return options[wingOptionIndex];
        }
        return options[0];
    }

    public static TunnelSide getWingSide(int directionIndex, int wingSideIndex) {
        Direction dir = getDirection(directionIndex);
        TunnelSide[] sides = dir.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
        if (wingSideIndex >= 0 && wingSideIndex < sides.length) {
            return sides[wingSideIndex];
        }
        return sides[0];
    }
}
