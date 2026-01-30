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

        public int getTileRadius() {
            return radius / 11;
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
    private Button btnDirN, btnDirS, btnDirE, btnDirW;
    private Button btnTunnelLeft, btnTunnelRight;
    private Button btnWingN, btnWingS, btnWingE, btnWingW;
    private Button btnWingSideA, btnWingSideB;
    private Dropbox<SupportType> supportTypeDropbox;
    private PreviewGrid previewGrid;
    private Label tunnelSideLabel;

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final SupportType[] SUPPORT_TYPES = SupportType.values();

    // Tunnel side options depend on direction
    private static final TunnelSide[] VERTICAL_TUNNEL_SIDES = {TunnelSide.WEST, TunnelSide.EAST};
    private static final TunnelSide[] HORIZONTAL_TUNNEL_SIDES = {TunnelSide.NORTH, TunnelSide.SOUTH};

    // Wing side options depend on direction
    private static final TunnelSide[] VERTICAL_WING_SIDES = {TunnelSide.NORTH, TunnelSide.SOUTH};
    private static final TunnelSide[] HORIZONTAL_WING_SIDES = {TunnelSide.WEST, TunnelSide.EAST};

    // Wing options depend on direction
    private static final WingOption[] VERTICAL_WING_OPTIONS = {
            WingOption.NONE, WingOption.EAST, WingOption.WEST, WingOption.BOTH_EW
    };
    private static final WingOption[] HORIZONTAL_WING_OPTIONS = {
            WingOption.NONE, WingOption.NORTH, WingOption.SOUTH, WingOption.BOTH_NS
    };

    // Colors for preview grid
    private static final Color COLOR_BG = new Color(30, 30, 30);
    private static final Color COLOR_SUPPORT = new Color(205, 92, 92); // Indian red (brighter)
    private static final Color COLOR_TUNNEL = new Color(100, 180, 255); // Bright blue
    private static final Color COLOR_WING = new Color(100, 220, 150); // Bright green
    private static final Color COLOR_ARROW = new Color(255, 215, 0); // Gold
    private static final Color COLOR_GRID = new Color(60, 60, 60);

    public TunnelingDialog() {
        super(UI.scale(new Coord(340, 470)), "Tunneling Bot");
        initializeWidgets();
    }

    private void initializeWidgets() {
        int windowWidth = UI.scale(340);
        int y = UI.scale(10);
        int centerX = windowWidth / 2;

        // === 1. SUPPORT TYPE (first, centered) ===
        int dropWidth = UI.scale(180);
        int dropHeight = UI.scale(18);
        int supportRowWidth = UI.scale(90) + dropWidth; // label + dropdown
        int supportStartX = (windowWidth - supportRowWidth) / 2;

        add(new Label("Support Type:"), new Coord(supportStartX, y + UI.scale(3)));
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
                g.text(item.menuName + " (r:" + item.getTileRadius() + " tiles)", new Coord(3, 1));
            }

            @Override
            public void change(SupportType item) {
                super.change(item);
                selectedSupportType = item;
                updatePreview();
            }
        };
        supportTypeDropbox.change(selectedSupportType);
        add(supportTypeDropbox, new Coord(supportStartX + UI.scale(90), y));
        y += UI.scale(35);

        // === 2. DIRECTION AND TUNNEL SIDE (side by side, centered) ===
        int groupSpacing = UI.scale(40);
        int compassWidth = UI.scale(90);
        int sideSelectWidth = UI.scale(100);
        int totalRowWidth = compassWidth + groupSpacing + sideSelectWidth;
        int rowStartX = (windowWidth - totalRowWidth) / 2;

        // Direction label and compass
        int compassCenterX = rowStartX + compassWidth / 2;
        add(new Label("Direction:"), new Coord(compassCenterX - UI.scale(30), y));
        y += UI.scale(20);

        int compassCenterY = y + UI.scale(25);
        int btnSize = UI.scale(28);
        int spacing = UI.scale(30);

        btnDirN = new Button(btnSize, "N") {
            @Override
            public void click() { selectDirection(Direction.NORTH); }
        };
        add(btnDirN, new Coord(compassCenterX - btnSize/2, compassCenterY - spacing));

        btnDirS = new Button(btnSize, "S") {
            @Override
            public void click() { selectDirection(Direction.SOUTH); }
        };
        add(btnDirS, new Coord(compassCenterX - btnSize/2, compassCenterY + spacing - btnSize));

        btnDirW = new Button(btnSize, "W") {
            @Override
            public void click() { selectDirection(Direction.WEST); }
        };
        add(btnDirW, new Coord(compassCenterX - spacing - btnSize/2, compassCenterY - btnSize/2));

        btnDirE = new Button(btnSize, "E") {
            @Override
            public void click() { selectDirection(Direction.EAST); }
        };
        add(btnDirE, new Coord(compassCenterX + spacing - btnSize/2, compassCenterY - btnSize/2));

        // Tunnel side selector (right of direction)
        int tunnelSideX = rowStartX + compassWidth + groupSpacing;
        add(new Label("Tunnel Side:"), new Coord(tunnelSideX, y - UI.scale(20)));
        tunnelSideLabel = new Label("");
        add(tunnelSideLabel, new Coord(tunnelSideX, y - UI.scale(5)));

        btnTunnelLeft = new Button(UI.scale(38), "") {
            @Override
            public void click() { selectTunnelSide(0); }
        };
        add(btnTunnelLeft, new Coord(tunnelSideX, y + UI.scale(12)));

        btnTunnelRight = new Button(UI.scale(38), "") {
            @Override
            public void click() { selectTunnelSide(1); }
        };
        add(btnTunnelRight, new Coord(tunnelSideX + UI.scale(42), y + UI.scale(12)));

        y += UI.scale(70);

        // === 3. WINGS AND WING SIDE (side by side, centered) ===
        int wingCenterX = rowStartX + compassWidth / 2;
        add(new Label("Wings:"), new Coord(wingCenterX - UI.scale(20), y));
        y += UI.scale(20);

        int wingCenterY = y + UI.scale(25);
        int wingBtnSize = UI.scale(28);
        int wingSpacing = UI.scale(30);

        btnWingN = new Button(wingBtnSize, "N") {
            @Override
            public void click() {
                wingNorth = !wingNorth;
                updateWingButtons();
                updatePreview();
            }
        };
        add(btnWingN, new Coord(wingCenterX - wingBtnSize/2, wingCenterY - wingSpacing));

        btnWingS = new Button(wingBtnSize, "S") {
            @Override
            public void click() {
                wingSouth = !wingSouth;
                updateWingButtons();
                updatePreview();
            }
        };
        add(btnWingS, new Coord(wingCenterX - wingBtnSize/2, wingCenterY + wingSpacing - wingBtnSize));

        btnWingW = new Button(wingBtnSize, "W") {
            @Override
            public void click() {
                wingWest = !wingWest;
                updateWingButtons();
                updatePreview();
            }
        };
        add(btnWingW, new Coord(wingCenterX - wingSpacing - wingBtnSize/2, wingCenterY - wingBtnSize/2));

        btnWingE = new Button(wingBtnSize, "E") {
            @Override
            public void click() {
                wingEast = !wingEast;
                updateWingButtons();
                updatePreview();
            }
        };
        add(btnWingE, new Coord(wingCenterX + wingSpacing - wingBtnSize/2, wingCenterY - wingBtnSize/2));

        // Center dot for wings
        add(new Label("\u25A0"), new Coord(wingCenterX - UI.scale(4), wingCenterY - UI.scale(6)));

        // Wing side selector (right of wings)
        int wingSideX = rowStartX + compassWidth + groupSpacing;
        add(new Label("Wing Side:"), new Coord(wingSideX, y - UI.scale(20)));

        btnWingSideA = new Button(UI.scale(38), "") {
            @Override
            public void click() { selectWingSide(0); }
        };
        add(btnWingSideA, new Coord(wingSideX, y + UI.scale(12)));

        btnWingSideB = new Button(UI.scale(38), "") {
            @Override
            public void click() { selectWingSide(1); }
        };
        add(btnWingSideB, new Coord(wingSideX + UI.scale(42), y + UI.scale(12)));

        y += UI.scale(75);

        // === 4. PREVIEW WITH COLORED LEGEND (centered) ===
        int legendTotalWidth = UI.scale(220);
        int legendStartX = (windowWidth - legendTotalWidth) / 2;

        add(new Label("Preview:"), new Coord(legendStartX, y));
        y += UI.scale(16);

        addColoredLegend(legendStartX, y, COLOR_SUPPORT, "Support");
        addColoredLegend(legendStartX + UI.scale(75), y, COLOR_TUNNEL, "Tunnel");
        addColoredLegend(legendStartX + UI.scale(145), y, COLOR_WING, "Wing");
        y += UI.scale(18);

        // === 5. PREVIEW GRID (centered) ===
        previewGrid = new PreviewGrid();
        int previewX = (windowWidth - previewGrid.sz.x) / 2;
        add(previewGrid, new Coord(previewX, y));
        y += previewGrid.sz.y + UI.scale(15);

        // === BUTTONS (centered) ===
        int btnWidth = UI.scale(100);
        int totalBtnWidth = btnWidth * 2 + UI.scale(20);
        int btnStartX = (windowWidth - totalBtnWidth) / 2;

        Button confirmButton = new Button(btnWidth, "Start") {
            @Override
            public void click() { confirm(); }
        };
        add(confirmButton, new Coord(btnStartX, y));

        Button cancelButton = new Button(btnWidth, "Cancel") {
            @Override
            public void click() { cancel(); }
        };
        add(cancelButton, new Coord(btnStartX + btnWidth + UI.scale(20), y));

        // Initialize state
        selectDirection(Direction.NORTH);
        updateTunnelSideButtons();
        updateWingSideButtons();
        updateWingButtons();
        updatePreview();
    }

    private void addColoredLegend(int x, int y, Color color, String text) {
        // Add a colored widget for the legend
        Widget colorBox = new Widget(new Coord(UI.scale(12), UI.scale(12))) {
            @Override
            public void draw(GOut g) {
                g.chcolor(color);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
        };
        add(colorBox, new Coord(x, y));
        add(new Label(text), new Coord(x + UI.scale(15), y));
    }

    private void selectDirection(Direction dir) {
        selectedDirection = dir;

        TunnelSide[] tunnelSides = dir.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
        TunnelSide[] wingSides = dir.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
        selectedTunnelSide = tunnelSides[0];
        selectedWingSide = wingSides[0];

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
        if (btnDirN == null) return;
        updateButtonHighlight(btnDirN, selectedDirection == Direction.NORTH);
        updateButtonHighlight(btnDirS, selectedDirection == Direction.SOUTH);
        updateButtonHighlight(btnDirE, selectedDirection == Direction.EAST);
        updateButtonHighlight(btnDirW, selectedDirection == Direction.WEST);
    }

    private void updateButtonHighlight(Button btn, boolean selected) {
        if (btn == null || btn.text == null) return;
        String base = btn.text.text.replace("[", "").replace("]", "");
        btn.change(selected ? "[" + base + "]" : base);
    }

    private void updateTunnelSideButtons() {
        if (btnTunnelLeft == null) return;
        TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
        btnTunnelLeft.change(sides[0].name.substring(0, 1));
        btnTunnelRight.change(sides[1].name.substring(0, 1));

        updateButtonHighlight(btnTunnelLeft, selectedTunnelSide == sides[0]);
        updateButtonHighlight(btnTunnelRight, selectedTunnelSide == sides[1]);

        tunnelSideLabel.settext("(" + sides[0].name + "/" + sides[1].name + ")");
    }

    private void updateWingSideButtons() {
        if (btnWingSideA == null) return;
        TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
        btnWingSideA.change(sides[0].name.substring(0, 1));
        btnWingSideB.change(sides[1].name.substring(0, 1));

        updateButtonHighlight(btnWingSideA, selectedWingSide == sides[0]);
        updateButtonHighlight(btnWingSideB, selectedWingSide == sides[1]);
    }

    private void updateWingButtons() {
        if (btnWingN == null) return;
        boolean isVertical = selectedDirection.isVertical();

        btnWingN.show(!isVertical);
        btnWingS.show(!isVertical);
        btnWingE.show(isVertical);
        btnWingW.show(isVertical);

        if (isVertical) {
            updateButtonHighlight(btnWingE, wingEast);
            updateButtonHighlight(btnWingW, wingWest);
        } else {
            updateButtonHighlight(btnWingN, wingNorth);
            updateButtonHighlight(btnWingS, wingSouth);
        }
    }

    private void updatePreview() {
        // PreviewGrid reads state directly
    }

    // Custom widget for visual grid preview - larger size showing actual proportions
    private class PreviewGrid extends Widget {
        private static final int CELL_SIZE = 8;
        private static final int GRID_SIZE = 19; // 19x19 grid

        public PreviewGrid() {
            super(new Coord(CELL_SIZE * GRID_SIZE, CELL_SIZE * GRID_SIZE));
        }

        @Override
        public void draw(GOut g) {
            // Fill background
            g.chcolor(COLOR_BG);
            g.frect(Coord.z, sz);

            // Draw grid lines
            g.chcolor(COLOR_GRID);
            for (int i = 0; i <= GRID_SIZE; i++) {
                g.line(new Coord(i * CELL_SIZE, 0), new Coord(i * CELL_SIZE, sz.y), 1);
                g.line(new Coord(0, i * CELL_SIZE), new Coord(sz.x, i * CELL_SIZE), 1);
            }

            int center = GRID_SIZE / 2; // Center cell (10 for 21x21)
            int radius = selectedSupportType.getTileRadius();

            boolean isVertical = selectedDirection.isVertical();

            if (isVertical) {
                drawVerticalPreview(g, center, radius);
            } else {
                drawHorizontalPreview(g, center, radius);
            }

            g.chcolor();
        }

        private void drawVerticalPreview(GOut g, int center, int radius) {
            boolean tunnelEast = (selectedTunnelSide == TunnelSide.EAST);
            int tunnelX = tunnelEast ? center + 1 : center - 1;
            int wingYOffset = (selectedWingSide == TunnelSide.NORTH) ? -1 : 1;

            // Support positions (two supports, spaced by radius)
            int support1Y = center - radius / 2;
            int support2Y = center + radius / 2;

            // Draw supports
            g.chcolor(COLOR_SUPPORT);
            fillCell(g, center, support1Y);
            fillCell(g, center, support2Y);

            // Draw tunnel (vertical line)
            g.chcolor(COLOR_TUNNEL);
            for (int y = support1Y; y <= support2Y; y++) {
                fillCell(g, tunnelX, y);
            }

            // Draw wings at each support
            g.chcolor(COLOR_WING);
            int[] supportYs = {support1Y, support2Y};
            for (int supY : supportYs) {
                int wingY = supY + wingYOffset;
                if (wingY < 0 || wingY >= GRID_SIZE) continue;

                if (wingWest) {
                    for (int x = tunnelX; x >= Math.max(0, center - radius); x--) {
                        fillCell(g, x, wingY);
                    }
                }
                if (wingEast) {
                    for (int x = tunnelX; x <= Math.min(GRID_SIZE - 1, center + radius); x++) {
                        fillCell(g, x, wingY);
                    }
                }
            }

            // Draw direction arrow
            g.chcolor(COLOR_ARROW);
            int arrowY = (selectedDirection == Direction.NORTH) ? support1Y - 2 : support2Y + 2;
            if (arrowY >= 0 && arrowY < GRID_SIZE) {
                drawArrow(g, tunnelX, arrowY, selectedDirection);
            }
        }

        private void drawHorizontalPreview(GOut g, int center, int radius) {
            boolean tunnelSouth = (selectedTunnelSide == TunnelSide.SOUTH);
            int tunnelY = tunnelSouth ? center + 1 : center - 1;
            int wingXOffset = (selectedWingSide == TunnelSide.WEST) ? -1 : 1;

            // Support positions (two supports, spaced by radius)
            int support1X = center - radius / 2;
            int support2X = center + radius / 2;

            // Draw supports
            g.chcolor(COLOR_SUPPORT);
            fillCell(g, support1X, center);
            fillCell(g, support2X, center);

            // Draw tunnel (horizontal line)
            g.chcolor(COLOR_TUNNEL);
            for (int x = support1X; x <= support2X; x++) {
                fillCell(g, x, tunnelY);
            }

            // Draw wings at each support
            g.chcolor(COLOR_WING);
            int[] supportXs = {support1X, support2X};
            for (int supX : supportXs) {
                int wingX = supX + wingXOffset;
                if (wingX < 0 || wingX >= GRID_SIZE) continue;

                if (wingNorth) {
                    for (int y = tunnelY; y >= Math.max(0, center - radius); y--) {
                        fillCell(g, wingX, y);
                    }
                }
                if (wingSouth) {
                    for (int y = tunnelY; y <= Math.min(GRID_SIZE - 1, center + radius); y++) {
                        fillCell(g, wingX, y);
                    }
                }
            }

            // Draw direction arrow
            g.chcolor(COLOR_ARROW);
            int arrowX = (selectedDirection == Direction.EAST) ? support2X + 2 : support1X - 2;
            if (arrowX >= 0 && arrowX < GRID_SIZE) {
                drawArrow(g, arrowX, tunnelY, selectedDirection);
            }
        }

        private void fillCell(GOut g, int gridX, int gridY) {
            g.frect(new Coord(gridX * CELL_SIZE + 1, gridY * CELL_SIZE + 1),
                    new Coord(CELL_SIZE - 1, CELL_SIZE - 1));
        }

        private void drawArrow(GOut g, int gridX, int gridY, Direction dir) {
            int cx = gridX * CELL_SIZE + CELL_SIZE / 2;
            int cy = gridY * CELL_SIZE + CELL_SIZE / 2;
            int size = CELL_SIZE / 2;

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
        if (selectedDirection.isVertical()) {
            if (wingEast && wingWest) return 3;
            if (wingEast) return 1;
            if (wingWest) return 2;
            return 0;
        } else {
            if (wingNorth && wingSouth) return 3;
            if (wingNorth) return 1;
            if (wingSouth) return 2;
            return 0;
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

    // Static accessors for bot
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
