package nurgling.widgets;

import haven.*;

import java.awt.Color;
import java.awt.image.BufferedImage;

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
        MINE_SUPPORT("Mine Support", "gfx/terobjs/minesupport", "paginae/bld/minesupport", 100),
        STONE_COLUMN("Stone Column", "gfx/terobjs/column", "paginae/bld/column", 125),
        MINE_BEAM("Mine Beam", "gfx/terobjs/minebeam", "paginae/bld/minebeam", 150);

        public final String menuName;
        public final String resourcePath;
        public final String paginaPath;
        public final int radius;

        SupportType(String menuName, String resourcePath, String paginaPath, int radius) {
            this.menuName = menuName;
            this.resourcePath = resourcePath;
            this.paginaPath = paginaPath;
            this.radius = radius;
        }

        public int getTileRadius() {
            return radius / 11;
        }
    }

    // Current selections
    private Direction selectedDirection = Direction.NORTH;
    private TunnelSide selectedTunnelSide = TunnelSide.WEST;
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
    private IButton btnDirN, btnDirS, btnDirE, btnDirW;
    private IButton btnTunnelLeft, btnTunnelRight;
    private IButton btnTunnelUp, btnTunnelDown;
    private IButton btnWingLeft, btnWingRight;
    private IButton btnWingSideUp, btnWingSideDown;
    private IButton btnWingSideLeft, btnWingSideRight;
    private Dropbox<SupportType> supportTypeDropbox;
    private PreviewGrid previewGrid;
    private SupportIconWidget supportIconWidget;
    private Widget columnIconWidget;
    private Label tunnelSideOptionsLabel;
    private Label wingSideOptionsLabel;

    // Selection outlines
    private SelectionFrame selDirN, selDirS, selDirE, selDirW;
    private SelectionFrame selTunnelFirst, selTunnelSecond;
    private SelectionFrame selWingSideFirst, selWingSideSecond;
    private SelectionFrame selWingLeft, selWingRight;

    private static final Color COLOR_SELECTION = new Color(255, 200, 50);

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
    private static final Color COLOR_SUPPORT = new Color(205, 92, 92);
    private static final Color COLOR_TUNNEL = new Color(100, 180, 255);
    private static final Color COLOR_WING = new Color(100, 220, 150);
    private static final Color COLOR_ARROW = new Color(255, 215, 0);
    private static final Color COLOR_GRID = new Color(60, 60, 60);
    private static final Color COLOR_PREVIEW_BORDER = new Color(210, 160, 60);

    // Button images
    private static final BufferedImage[] BTN_N = loadBtnSet("nurgling/hud/buttons/n/cbtn");
    private static final BufferedImage[] BTN_S = loadBtnSet("nurgling/hud/buttons/s/cbtn");
    private static final BufferedImage[] BTN_E = loadBtnSet("nurgling/hud/buttons/e/cbtn");
    private static final BufferedImage[] BTN_W = loadBtnSet("nurgling/hud/buttons/w/cbtn");
    private static final BufferedImage[] BTN_LEFT = loadBtnSet("nurgling/hud/buttons/left_new/cbtn");
    private static final BufferedImage[] BTN_RIGHT = loadBtnSet("nurgling/hud/buttons/right_new/cbtn");
    private static final BufferedImage[] BTN_UP = loadBtnSet("nurgling/hud/buttons/up_new/cbtn");
    private static final BufferedImage[] BTN_DOWN = loadBtnSet("nurgling/hud/buttons/down_new/cbtn");

    // Static images
    private static final Tex WINDROSE = Resource.loadtex("nurgling/hud/tunneling/windrose");

    private static BufferedImage[] loadBtnSet(String basePath) {
        return new BufferedImage[] {
            Resource.loadsimg(basePath + "u"),
            Resource.loadsimg(basePath + "d"),
            Resource.loadsimg(basePath + "h")
        };
    }

    public TunnelingDialog() {
        super(new Coord(560, 620), "Tunneling Bot");
        initializeWidgets();
    }

    private void initializeWidgets() {
        int y = 20;
        int leftMargin = 20;

        // === 1. SUPPORT TYPE with icon ===
        // Label and dropdown on same baseline, with proper spacing
        add(new Label("Support Type:"), new Coord(leftMargin, y + 4));

        int dropWidth = 200;
        int dropHeight = 28;
        int dropX = leftMargin + 115;
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
                g.text(item.menuName + " (r:" + item.getTileRadius() + " tiles)", new Coord(5, 3));
            }

            @Override
            public void change(SupportType item) {
                super.change(item);
                selectedSupportType = item;
                if (supportIconWidget != null) {
                    supportIconWidget.updateIcon(item);
                }
                updatePreview();
            }
        };
        supportTypeDropbox.change(selectedSupportType);
        add(supportTypeDropbox, new Coord(dropX, y));

        // Support icon next to dropdown
        supportIconWidget = new SupportIconWidget(selectedSupportType, true);
        add(supportIconWidget, new Coord(dropX + dropWidth + 15, y - 10));

        y += 55;

        // === 2. DIRECTION with windrose and WINGS with column ===
        int btnGap = 5;

        // Create buttons first to get their sizes
        btnDirN = new IButton(BTN_N[0], BTN_N[1], BTN_N[2]) {
            @Override
            public void click() { selectDirection(Direction.NORTH); }
        };
        btnDirS = new IButton(BTN_S[0], BTN_S[1], BTN_S[2]) {
            @Override
            public void click() { selectDirection(Direction.SOUTH); }
        };
        btnDirW = new IButton(BTN_W[0], BTN_W[1], BTN_W[2]) {
            @Override
            public void click() { selectDirection(Direction.WEST); }
        };
        btnDirE = new IButton(BTN_E[0], BTN_E[1], BTN_E[2]) {
            @Override
            public void click() { selectDirection(Direction.EAST); }
        };

        Coord windroseSize = WINDROSE.sz();

        // Calculate positions from LEFT to RIGHT:
        // [Direction:] gap [W][windrose][E] gap [Wings:] gap [←][column][→]

        int dirLabelWidth = 65;  // "Direction:" label width
        int labelToAssemblyGap = 40;

        // Direction label starts at leftMargin
        // W button starts after label + gap
        int wButtonLeftEdge = leftMargin + dirLabelWidth + labelToAssemblyGap;

        // Calculate compassCenterX so W button is at wButtonLeftEdge
        // W button X = compassCenterX - windroseSize.x/2 - btnGap - btnDirW.sz.x
        // So: compassCenterX = wButtonLeftEdge + btnDirW.sz.x + btnGap + windroseSize.x/2
        int compassCenterX = wButtonLeftEdge + btnDirW.sz.x + btnGap + windroseSize.x / 2;
        int compassCenterY = y + windroseSize.y / 2 + btnDirN.sz.y + btnGap;

        // Direction label - vertically centered with compass, to the LEFT
        add(new Label("Direction:"), new Coord(leftMargin, compassCenterY - 6));

        // Windrose in center
        add(new Widget(windroseSize) {
            @Override
            public void draw(GOut g) {
                g.image(WINDROSE, Coord.z);
            }
        }, new Coord(compassCenterX - windroseSize.x / 2, compassCenterY - windroseSize.y / 2));

        // Direction buttons around windrose (with selection frames)
        Coord posN = new Coord(compassCenterX - btnDirN.sz.x / 2, compassCenterY - windroseSize.y / 2 - btnGap - btnDirN.sz.y);
        Coord posS = new Coord(compassCenterX - btnDirS.sz.x / 2, compassCenterY + windroseSize.y / 2 + btnGap);
        Coord posW = new Coord(compassCenterX - windroseSize.x / 2 - btnGap - btnDirW.sz.x, compassCenterY - btnDirW.sz.y / 2);
        Coord posE = new Coord(compassCenterX + windroseSize.x / 2 + btnGap, compassCenterY - btnDirE.sz.y / 2);

        selDirN = addSelectionFrame(btnDirN, posN);
        selDirS = addSelectionFrame(btnDirS, posS);
        selDirW = addSelectionFrame(btnDirW, posW);
        selDirE = addSelectionFrame(btnDirE, posE);

        add(btnDirN, posN);
        add(btnDirS, posS);
        add(btnDirW, posW);
        add(btnDirE, posE);

        // Wings section - starts after direction section
        int directionSectionRightEdge = compassCenterX + windroseSize.x / 2 + btnGap + btnDirE.sz.x;
        int sectionGap = 25;
        int wingsLabelX = directionSectionRightEdge + sectionGap;
        int wingsLabelWidth = 45;  // "Wings:" label width
        int wingBtnGap = 10;

        // Wings label - vertically centered, to the LEFT of wing buttons
        add(new Label("Wings:"), new Coord(wingsLabelX, compassCenterY - 6));

        // Create wing buttons to get sizes
        btnWingLeft = new IButton(BTN_LEFT[0], BTN_LEFT[1], BTN_LEFT[2]) {
            @Override
            public void click() {
                if (selectedDirection.isVertical()) {
                    wingWest = !wingWest;
                } else {
                    wingNorth = !wingNorth;
                }
                updateWingToggleSelection();
                updatePreview();
            }
        };
        btnWingRight = new IButton(BTN_RIGHT[0], BTN_RIGHT[1], BTN_RIGHT[2]) {
            @Override
            public void click() {
                if (selectedDirection.isVertical()) {
                    wingEast = !wingEast;
                } else {
                    wingSouth = !wingSouth;
                }
                updateWingToggleSelection();
                updatePreview();
            }
        };

        // Column icon (created to get size)
        columnIconWidget = new SupportIconWidget(SupportType.STONE_COLUMN, true);

        // Left button starts after Wings label + gap
        int leftBtnLeftEdge = wingsLabelX + wingsLabelWidth + labelToAssemblyGap;

        // Calculate wingCenterX so left button is at leftBtnLeftEdge
        // Left button X = wingCenterX - columnIconWidget.sz.x/2 - wingBtnGap - btnWingLeft.sz.x
        // So: wingCenterX = leftBtnLeftEdge + btnWingLeft.sz.x + wingBtnGap + columnIconWidget.sz.x/2
        int wingCenterX = leftBtnLeftEdge + btnWingLeft.sz.x + wingBtnGap + columnIconWidget.sz.x / 2;

        // Position wing elements (with selection frames for wing toggle buttons)
        Coord wingLeftPos = new Coord(wingCenterX - columnIconWidget.sz.x / 2 - wingBtnGap - btnWingLeft.sz.x, compassCenterY - btnWingLeft.sz.y / 2);
        Coord wingRightPos = new Coord(wingCenterX + columnIconWidget.sz.x / 2 + wingBtnGap, compassCenterY - btnWingRight.sz.y / 2);

        selWingLeft = addSelectionFrame(btnWingLeft, wingLeftPos);
        selWingRight = addSelectionFrame(btnWingRight, wingRightPos);

        add(columnIconWidget, new Coord(wingCenterX - columnIconWidget.sz.x / 2, compassCenterY - columnIconWidget.sz.y / 2));
        add(btnWingLeft, wingLeftPos);
        add(btnWingRight, wingRightPos);

        y = compassCenterY + windroseSize.y / 2 + btnDirS.sz.y + btnGap + 25;

        // === 3. TUNNEL SIDE and WING SIDE ===
        // Buttons swap based on direction:
        // Vertical (N/S): Tunnel=←→, WingSide=↑↓
        // Horizontal (E/W): Tunnel=↑↓, WingSide=←→

        int arrowBtnGap = 10;

        // Tunnel Side buttons - both sets (left/right for vertical, up/down for horizontal)
        btnTunnelLeft = new IButton(BTN_LEFT[0], BTN_LEFT[1], BTN_LEFT[2]) {
            @Override
            public void click() { selectTunnelSide(0); }
        };
        btnTunnelRight = new IButton(BTN_RIGHT[0], BTN_RIGHT[1], BTN_RIGHT[2]) {
            @Override
            public void click() { selectTunnelSide(1); }
        };
        btnTunnelUp = new IButton(BTN_UP[0], BTN_UP[1], BTN_UP[2]) {
            @Override
            public void click() { selectTunnelSide(0); }
        };
        btnTunnelDown = new IButton(BTN_DOWN[0], BTN_DOWN[1], BTN_DOWN[2]) {
            @Override
            public void click() { selectTunnelSide(1); }
        };

        // Wing Side buttons - both sets (up/down for vertical, left/right for horizontal)
        btnWingSideUp = new IButton(BTN_UP[0], BTN_UP[1], BTN_UP[2]) {
            @Override
            public void click() { selectWingSide(0); }
        };
        btnWingSideDown = new IButton(BTN_DOWN[0], BTN_DOWN[1], BTN_DOWN[2]) {
            @Override
            public void click() { selectWingSide(1); }
        };
        btnWingSideLeft = new IButton(BTN_LEFT[0], BTN_LEFT[1], BTN_LEFT[2]) {
            @Override
            public void click() { selectWingSide(0); }
        };
        btnWingSideRight = new IButton(BTN_RIGHT[0], BTN_RIGHT[1], BTN_RIGHT[2]) {
            @Override
            public void click() { selectWingSide(1); }
        };

        // Tunnel Side: label on two lines, then buttons (aligned with Direction section)
        int tunnelLabelWidth = 75;
        int tunnelBtnStartX = leftMargin + tunnelLabelWidth + labelToAssemblyGap;

        add(new Label("Tunnel Side:"), new Coord(leftMargin, y));
        tunnelSideOptionsLabel = new Label("(East/West)");
        add(tunnelSideOptionsLabel, new Coord(leftMargin, y + 16));

        Coord tunnelPos1 = new Coord(tunnelBtnStartX, y + 5);
        Coord tunnelPos2 = new Coord(tunnelBtnStartX + btnTunnelLeft.sz.x + arrowBtnGap, y + 5);

        selTunnelFirst = addSelectionFrame(btnTunnelLeft, tunnelPos1);
        selTunnelSecond = addSelectionFrame(btnTunnelRight, tunnelPos2);

        add(btnTunnelLeft, tunnelPos1);
        add(btnTunnelRight, tunnelPos2);
        add(btnTunnelUp, tunnelPos1);
        add(btnTunnelDown, tunnelPos2);
        // Initially hide horizontal tunnel buttons
        btnTunnelUp.hide();
        btnTunnelDown.hide();

        // Wing Side: label on two lines, then buttons (aligned with Wings section)
        int wingSideLabelX = wingsLabelX;
        int wingSideLabelWidth = 75;
        int wingSideBtnStartX = wingSideLabelX + wingSideLabelWidth + labelToAssemblyGap;
        add(new Label("Wing Side:"), new Coord(wingSideLabelX, y));
        wingSideOptionsLabel = new Label("(North/South)");
        add(wingSideOptionsLabel, new Coord(wingSideLabelX, y + 16));

        Coord wingSidePos1 = new Coord(wingSideBtnStartX, y + 5);
        Coord wingSidePos2 = new Coord(wingSideBtnStartX + btnWingSideUp.sz.x + arrowBtnGap, y + 5);

        selWingSideFirst = addSelectionFrame(btnWingSideUp, wingSidePos1);
        selWingSideSecond = addSelectionFrame(btnWingSideDown, wingSidePos2);

        add(btnWingSideUp, wingSidePos1);
        add(btnWingSideDown, wingSidePos2);
        add(btnWingSideLeft, wingSidePos1);
        add(btnWingSideRight, wingSidePos2);
        // Initially hide horizontal wing side buttons
        btnWingSideLeft.hide();
        btnWingSideRight.hide();

        y += btnTunnelLeft.sz.y + 30;

        // === 4. PREVIEW with border (left) and LEGEND (right) ===
        add(new Label("Preview:"), new Coord(leftMargin, y));
        y += 25; // Space between label and preview

        previewGrid = new PreviewGrid();
        add(previewGrid, new Coord(leftMargin, y));

        // Legend aligned to bottom of preview
        int legendX = leftMargin + previewGrid.sz.x + 25;
        int previewBottom = y + previewGrid.sz.y;
        int legendItemHeight = 22;
        addColoredLegend(legendX, previewBottom - 3 * legendItemHeight, COLOR_SUPPORT, "Support");
        addColoredLegend(legendX, previewBottom - 2 * legendItemHeight, COLOR_TUNNEL, "Tunnel");
        addColoredLegend(legendX, previewBottom - legendItemHeight, COLOR_WING, "Wing");

        y += previewGrid.sz.y + 25;

        // === 5. BUTTONS (left aligned) ===
        int btnWidth = 140;
        Button confirmButton = new Button(btnWidth, "Start") {
            @Override
            public void click() { confirm(); }
        };
        add(confirmButton, new Coord(leftMargin, y));

        Button cancelButton = new Button(btnWidth, "Cancel") {
            @Override
            public void click() { cancel(); }
        };
        add(cancelButton, new Coord(leftMargin + btnWidth + 15, y));

        // Initialize state
        selectDirection(Direction.NORTH);
        updatePreview();
    }

    // Selection frame constants
    private static final int SEL_BORDER_WIDTH = 2;
    private static final int SEL_PADDING = 3;

    // Selection frame widget to show outline around selected buttons
    private class SelectionFrame extends Widget {
        public SelectionFrame(Coord buttonSize) {
            super(buttonSize.add(SEL_PADDING * 2, SEL_PADDING * 2));
        }

        @Override
        public void draw(GOut g) {
            g.chcolor(COLOR_SELECTION);
            // Draw border (4 rectangles for outline)
            g.frect(Coord.z, new Coord(sz.x, SEL_BORDER_WIDTH)); // top
            g.frect(Coord.z, new Coord(SEL_BORDER_WIDTH, sz.y)); // left
            g.frect(new Coord(sz.x - SEL_BORDER_WIDTH, 0), new Coord(SEL_BORDER_WIDTH, sz.y)); // right
            g.frect(new Coord(0, sz.y - SEL_BORDER_WIDTH), new Coord(sz.x, SEL_BORDER_WIDTH)); // bottom
            g.chcolor();
        }
    }

    private SelectionFrame addSelectionFrame(IButton btn, Coord btnPos) {
        SelectionFrame frame = new SelectionFrame(btn.sz);
        add(frame, btnPos.sub(SEL_PADDING, SEL_PADDING));
        frame.hide();
        return frame;
    }

    // Widget to display support type icon loaded from game resources
    private class SupportIconWidget extends Widget {
        private Tex icon = null;
        private SupportType currentType;
        private boolean showBorder;

        public SupportIconWidget(SupportType type, boolean showBorder) {
            super(new Coord(48, 48));
            this.currentType = type;
            this.showBorder = showBorder;
            loadIcon();
        }

        private void loadIcon() {
            try {
                Resource res = Resource.remote().loadwait(currentType.paginaPath);
                if (res != null) {
                    Resource.Image img = res.layer(Resource.imgc);
                    if (img != null) {
                        icon = img.tex();
                    }
                }
            } catch (Exception e) {
                icon = null;
            }
        }

        public void updateIcon(SupportType type) {
            this.currentType = type;
            loadIcon();
        }

        @Override
        public void draw(GOut g) {
            if (showBorder) {
                // Draw border (2 pixel thick)
                g.chcolor(COLOR_PREVIEW_BORDER);
                g.frect(Coord.z, new Coord(sz.x, 2)); // top
                g.frect(Coord.z, new Coord(2, sz.y)); // left
                g.frect(new Coord(sz.x - 2, 0), new Coord(2, sz.y)); // right
                g.frect(new Coord(0, sz.y - 2), new Coord(sz.x, 2)); // bottom
                g.chcolor();
            }

            if (icon != null) {
                Coord iconSz = icon.sz();
                Coord pos = sz.sub(iconSz).div(2);
                g.image(icon, pos);
            }
        }
    }

    private void addColoredLegend(int x, int y, Color color, String text) {
        Widget colorBox = new Widget(new Coord(UI.scale(14), UI.scale(14))) {
            @Override
            public void draw(GOut g) {
                g.chcolor(color);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
        };
        add(colorBox, new Coord(x, y));
        add(new Label(text), new Coord(x + UI.scale(18), y));
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

        // Update dynamic labels based on direction
        if (tunnelSideOptionsLabel != null) {
            tunnelSideOptionsLabel.settext(dir.isVertical() ? "(East/West)" : "(North/South)");
        }
        if (wingSideOptionsLabel != null) {
            wingSideOptionsLabel.settext(dir.isVertical() ? "(North/South)" : "(East/West)");
        }

        // Update button visibility based on direction
        // Vertical direction: Tunnel uses ←→, WingSide uses ↑↓
        // Horizontal direction: Tunnel uses ↑↓, WingSide uses ←→
        if (btnTunnelLeft != null) {
            if (dir.isVertical()) {
                btnTunnelLeft.show();
                btnTunnelRight.show();
                btnTunnelUp.hide();
                btnTunnelDown.hide();
                btnWingSideUp.show();
                btnWingSideDown.show();
                btnWingSideLeft.hide();
                btnWingSideRight.hide();
            } else {
                btnTunnelLeft.hide();
                btnTunnelRight.hide();
                btnTunnelUp.show();
                btnTunnelDown.show();
                btnWingSideUp.hide();
                btnWingSideDown.hide();
                btnWingSideLeft.show();
                btnWingSideRight.show();
            }
        }

        // Update direction selection frames
        updateDirectionSelection(dir);

        // Reset tunnel side selection to first option
        updateTunnelSideSelection(0);

        // Reset wing side selection to first option
        updateWingSideSelection(0);

        // Reset wing toggle selections
        updateWingToggleSelection();

        updatePreview();
    }

    private void selectTunnelSide(int index) {
        TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
        if (index >= 0 && index < sides.length) {
            selectedTunnelSide = sides[index];
            updateTunnelSideSelection(index);
            updatePreview();
        }
    }

    private void selectWingSide(int index) {
        TunnelSide[] sides = selectedDirection.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
        if (index >= 0 && index < sides.length) {
            selectedWingSide = sides[index];
            updateWingSideSelection(index);
            updatePreview();
        }
    }

    private void updateDirectionSelection(Direction dir) {
        if (selDirN == null) return;
        selDirN.hide();
        selDirS.hide();
        selDirE.hide();
        selDirW.hide();
        switch (dir) {
            case NORTH: selDirN.show(); break;
            case SOUTH: selDirS.show(); break;
            case EAST: selDirE.show(); break;
            case WEST: selDirW.show(); break;
        }
    }

    private void updateTunnelSideSelection(int index) {
        if (selTunnelFirst == null) return;
        selTunnelFirst.hide();
        selTunnelSecond.hide();
        if (index == 0) {
            selTunnelFirst.show();
        } else {
            selTunnelSecond.show();
        }
    }

    private void updateWingSideSelection(int index) {
        if (selWingSideFirst == null) return;
        selWingSideFirst.hide();
        selWingSideSecond.hide();
        if (index == 0) {
            selWingSideFirst.show();
        } else {
            selWingSideSecond.show();
        }
    }

    private void updateWingToggleSelection() {
        if (selWingLeft == null) return;
        // Show selection based on wing state
        boolean leftActive = selectedDirection.isVertical() ? wingWest : wingNorth;
        boolean rightActive = selectedDirection.isVertical() ? wingEast : wingSouth;
        if (leftActive) {
            selWingLeft.show();
        } else {
            selWingLeft.hide();
        }
        if (rightActive) {
            selWingRight.show();
        } else {
            selWingRight.hide();
        }
    }

    private void updatePreview() {
        // PreviewGrid reads state directly, just trigger redraw
        if (previewGrid != null) {
            previewGrid.redraw();
        }
    }

    // Custom widget for visual grid preview
    private class PreviewGrid extends Widget {
        private static final int CELL_SIZE = 10;
        private static final int GRID_SIZE = 19;
        private static final int BORDER_WIDTH = 4;

        public PreviewGrid() {
            super(new Coord(CELL_SIZE * GRID_SIZE + BORDER_WIDTH * 2, CELL_SIZE * GRID_SIZE + BORDER_WIDTH * 2));
        }

        @Override
        public void draw(GOut g) {
            // Draw border
            g.chcolor(COLOR_PREVIEW_BORDER);
            g.frect(Coord.z, sz);

            // Draw background inside border
            g.chcolor(COLOR_BG);
            g.frect(new Coord(BORDER_WIDTH, BORDER_WIDTH),
                    new Coord(CELL_SIZE * GRID_SIZE, CELL_SIZE * GRID_SIZE));

            // Offset all drawing by border width
            Coord offset = new Coord(BORDER_WIDTH, BORDER_WIDTH);

            // Draw grid lines
            g.chcolor(COLOR_GRID);
            for (int i = 0; i <= GRID_SIZE; i++) {
                g.line(offset.add(i * CELL_SIZE, 0), offset.add(i * CELL_SIZE, GRID_SIZE * CELL_SIZE), 1);
                g.line(offset.add(0, i * CELL_SIZE), offset.add(GRID_SIZE * CELL_SIZE, i * CELL_SIZE), 1);
            }

            int center = GRID_SIZE / 2;
            int radius = selectedSupportType.getTileRadius();

            if (selectedDirection.isVertical()) {
                drawVerticalPreview(g, offset, center, radius);
            } else {
                drawHorizontalPreview(g, offset, center, radius);
            }

            g.chcolor();
        }

        private void drawVerticalPreview(GOut g, Coord offset, int center, int radius) {
            boolean tunnelEast = (selectedTunnelSide == TunnelSide.EAST);
            int tunnelX = tunnelEast ? center + 1 : center - 1;
            int wingYOffset = (selectedWingSide == TunnelSide.NORTH) ? -1 : 1;

            int support1Y = center - radius / 2;
            int support2Y = center + radius / 2;

            // Draw supports
            g.chcolor(COLOR_SUPPORT);
            fillCell(g, offset, center, support1Y);
            fillCell(g, offset, center, support2Y);

            // Draw tunnel
            g.chcolor(COLOR_TUNNEL);
            for (int y = support1Y; y <= support2Y; y++) {
                fillCell(g, offset, tunnelX, y);
            }

            // Draw wings
            g.chcolor(COLOR_WING);
            int[] supportYs = {support1Y, support2Y};
            for (int supY : supportYs) {
                int wingY = supY + wingYOffset;
                if (wingY < 0 || wingY >= GRID_SIZE) continue;

                if (wingWest) {
                    for (int x = tunnelX; x >= Math.max(0, center - radius); x--) {
                        fillCell(g, offset, x, wingY);
                    }
                }
                if (wingEast) {
                    for (int x = tunnelX; x <= Math.min(GRID_SIZE - 1, center + radius); x++) {
                        fillCell(g, offset, x, wingY);
                    }
                }
            }

            // Draw arrow
            g.chcolor(COLOR_ARROW);
            int arrowY = (selectedDirection == Direction.NORTH) ? support1Y - 2 : support2Y + 2;
            if (arrowY >= 0 && arrowY < GRID_SIZE) {
                drawArrow(g, offset, tunnelX, arrowY, selectedDirection);
            }
        }

        private void drawHorizontalPreview(GOut g, Coord offset, int center, int radius) {
            boolean tunnelSouth = (selectedTunnelSide == TunnelSide.SOUTH);
            int tunnelY = tunnelSouth ? center + 1 : center - 1;
            int wingXOffset = (selectedWingSide == TunnelSide.WEST) ? -1 : 1;

            int support1X = center - radius / 2;
            int support2X = center + radius / 2;

            // Draw supports
            g.chcolor(COLOR_SUPPORT);
            fillCell(g, offset, support1X, center);
            fillCell(g, offset, support2X, center);

            // Draw tunnel
            g.chcolor(COLOR_TUNNEL);
            for (int x = support1X; x <= support2X; x++) {
                fillCell(g, offset, x, tunnelY);
            }

            // Draw wings
            g.chcolor(COLOR_WING);
            int[] supportXs = {support1X, support2X};
            for (int supX : supportXs) {
                int wingX = supX + wingXOffset;
                if (wingX < 0 || wingX >= GRID_SIZE) continue;

                if (wingNorth) {
                    for (int y = tunnelY; y >= Math.max(0, center - radius); y--) {
                        fillCell(g, offset, wingX, y);
                    }
                }
                if (wingSouth) {
                    for (int y = tunnelY; y <= Math.min(GRID_SIZE - 1, center + radius); y++) {
                        fillCell(g, offset, wingX, y);
                    }
                }
            }

            // Draw arrow
            g.chcolor(COLOR_ARROW);
            int arrowX = (selectedDirection == Direction.EAST) ? support2X + 2 : support1X - 2;
            if (arrowX >= 0 && arrowX < GRID_SIZE) {
                drawArrow(g, offset, arrowX, tunnelY, selectedDirection);
            }
        }

        private void fillCell(GOut g, Coord offset, int gridX, int gridY) {
            g.frect(offset.add(gridX * CELL_SIZE + 1, gridY * CELL_SIZE + 1),
                    new Coord(CELL_SIZE - 1, CELL_SIZE - 1));
        }

        private void drawArrow(GOut g, Coord offset, int gridX, int gridY, Direction dir) {
            int cx = offset.x + gridX * CELL_SIZE + CELL_SIZE / 2;
            int cy = offset.y + gridY * CELL_SIZE + CELL_SIZE / 2;
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

        public void redraw() {
            // Widget will be redrawn on next frame
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
