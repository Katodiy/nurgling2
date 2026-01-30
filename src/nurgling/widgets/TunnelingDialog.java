package nurgling.widgets;

import haven.*;

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
    private int directionIndex = 0;
    private int tunnelSideIndex = 0;
    private int supportTypeIndex = 0;
    private int wingOptionIndex = 0;
    private int wingSideIndex = 0;

    // Reference arrays for communication with bot
    private int[] directionRef = null;
    private int[] tunnelSideRef = null;
    private int[] supportTypeRef = null;
    private int[] wingOptionRef = null;
    private int[] wingSideRef = null;
    private boolean[] confirmRef = null;
    private boolean[] cancelRef = null;

    // Dropboxes
    private Dropbox<Direction> directionDropbox;
    private Dropbox<String> tunnelSideDropbox;
    private Dropbox<SupportType> supportTypeDropbox;
    private Dropbox<String> wingOptionDropbox;
    private Dropbox<String> wingSideDropbox;

    // Position for dynamic dropboxes
    private Coord tunnelSidePos;
    private Coord wingOptionPos;
    private Coord wingSidePos;

    // Flag to prevent rebuilding during initialization
    private boolean initialized = false;

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final SupportType[] SUPPORT_TYPES = SupportType.values();

    // Tunnel side options depend on direction (perpendicular to travel direction)
    private static final TunnelSide[] VERTICAL_TUNNEL_SIDES = {TunnelSide.EAST, TunnelSide.WEST};
    private static final TunnelSide[] HORIZONTAL_TUNNEL_SIDES = {TunnelSide.NORTH, TunnelSide.SOUTH};

    // Wing side options depend on direction (parallel to travel direction, perpendicular to wing direction)
    // For N/S travel: wings go E/W, so wing offset is N or S
    // For E/W travel: wings go N/S, so wing offset is E or W
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
        super(UI.scale(new Coord(300, 225)), "Tunneling Bot");
        initializeWidgets();
    }

    private void initializeWidgets() {
        int y = UI.scale(10);
        int col1 = UI.scale(10);
        int col2 = UI.scale(110);
        int dropWidth = UI.scale(150);
        int dropHeight = UI.scale(18);

        // Row 1: Direction selection
        add(new Label("Direction:"), new Coord(col1, y + 3));
        directionDropbox = new Dropbox<Direction>(dropWidth, DIRECTIONS.length, dropHeight) {
            @Override
            protected Direction listitem(int i) {
                return DIRECTIONS[i];
            }

            @Override
            protected int listitems() {
                return DIRECTIONS.length;
            }

            @Override
            protected void drawitem(GOut g, Direction item, int idx) {
                g.text(item.name, new Coord(3, 1));
            }

            @Override
            public void change(Direction item) {
                super.change(item);
                directionIndex = indexOf(item);
                if (directionRef != null) {
                    directionRef[0] = directionIndex;
                }
                // Rebuild dependent dropboxes (only after initialization)
                if (initialized) {
                    rebuildTunnelSideDropbox();
                    rebuildWingOptionDropbox();
                    rebuildWingSideDropbox();
                }
            }

            private int indexOf(Direction d) {
                for (int i = 0; i < DIRECTIONS.length; i++) {
                    if (DIRECTIONS[i] == d) return i;
                }
                return 0;
            }
        };
        directionDropbox.change(DIRECTIONS[directionIndex]);
        add(directionDropbox, new Coord(col2, y));
        y += UI.scale(28);

        // Row 2: Tunnel side selection
        add(new Label("Tunnel Side:"), new Coord(col1, y + 3));
        tunnelSidePos = new Coord(col2, y);
        createTunnelSideDropbox();
        y += UI.scale(28);

        // Row 3: Support type selection
        add(new Label("Support Type:"), new Coord(col1, y + 3));
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
                g.text(item.menuName + " (r:" + (item.radius / 11) + ")", new Coord(3, 1));
            }

            @Override
            public void change(SupportType item) {
                super.change(item);
                supportTypeIndex = indexOf(item);
                if (supportTypeRef != null) {
                    supportTypeRef[0] = supportTypeIndex;
                }
            }

            private int indexOf(SupportType s) {
                for (int i = 0; i < SUPPORT_TYPES.length; i++) {
                    if (SUPPORT_TYPES[i] == s) return i;
                }
                return 0;
            }
        };
        supportTypeDropbox.change(SUPPORT_TYPES[supportTypeIndex]);
        add(supportTypeDropbox, new Coord(col2, y));
        y += UI.scale(28);

        // Row 4: Wing option selection
        add(new Label("Wings:"), new Coord(col1, y + 3));
        wingOptionPos = new Coord(col2, y);
        createWingOptionDropbox();
        y += UI.scale(28);

        // Row 5: Wing side selection (which side of support the wings are offset to)
        add(new Label("Wing Side:"), new Coord(col1, y + 3));
        wingSidePos = new Coord(col2, y);
        createWingSideDropbox();
        y += UI.scale(35);

        // Row 6: Confirm and Cancel buttons
        Button confirmButton = new Button(UI.scale(100), "Start") {
            @Override
            public void click() {
                confirm();
            }
        };
        add(confirmButton, new Coord(UI.scale(45), y));

        Button cancelButton = new Button(UI.scale(100), "Cancel") {
            @Override
            public void click() {
                cancel();
            }
        };
        add(cancelButton, new Coord(UI.scale(155), y));

        // Mark initialization complete - now direction changes can trigger rebuilds
        initialized = true;
    }

    private void createTunnelSideDropbox() {
        final TunnelSide[] sides = getCurrentTunnelSides();
        int dropWidth = UI.scale(150);
        int dropHeight = UI.scale(18);

        tunnelSideDropbox = new Dropbox<String>(dropWidth, sides.length, dropHeight) {
            @Override
            protected String listitem(int i) {
                return sides[i].name;
            }

            @Override
            protected int listitems() {
                return sides.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, new Coord(3, 1));
            }

            @Override
            public void change(String item) {
                super.change(item);
                for (int i = 0; i < sides.length; i++) {
                    if (sides[i].name.equals(item)) {
                        tunnelSideIndex = i;
                        break;
                    }
                }
                if (tunnelSideRef != null) {
                    tunnelSideRef[0] = tunnelSideIndex;
                }
            }
        };

        if (tunnelSideIndex >= sides.length) {
            tunnelSideIndex = 0;
        }
        tunnelSideDropbox.change(sides[tunnelSideIndex].name);
        add(tunnelSideDropbox, tunnelSidePos);
    }

    private void rebuildTunnelSideDropbox() {
        if (tunnelSideDropbox != null) {
            tunnelSideDropbox.destroy();
        }
        tunnelSideIndex = 0;
        if (tunnelSideRef != null) {
            tunnelSideRef[0] = 0;
        }
        createTunnelSideDropbox();
    }

    private void createWingOptionDropbox() {
        final WingOption[] options = getCurrentWingOptions();
        int dropWidth = UI.scale(150);
        int dropHeight = UI.scale(18);

        wingOptionDropbox = new Dropbox<String>(dropWidth, options.length, dropHeight) {
            @Override
            protected String listitem(int i) {
                return options[i].name;
            }

            @Override
            protected int listitems() {
                return options.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, new Coord(3, 1));
            }

            @Override
            public void change(String item) {
                super.change(item);
                for (int i = 0; i < options.length; i++) {
                    if (options[i].name.equals(item)) {
                        wingOptionIndex = i;
                        break;
                    }
                }
                if (wingOptionRef != null) {
                    wingOptionRef[0] = wingOptionIndex;
                }
            }
        };

        if (wingOptionIndex >= options.length) {
            wingOptionIndex = 0;
        }
        wingOptionDropbox.change(options[wingOptionIndex].name);
        add(wingOptionDropbox, wingOptionPos);
    }

    private void rebuildWingOptionDropbox() {
        if (wingOptionDropbox != null) {
            wingOptionDropbox.destroy();
        }
        wingOptionIndex = 0;
        if (wingOptionRef != null) {
            wingOptionRef[0] = 0;
        }
        createWingOptionDropbox();
    }

    private void createWingSideDropbox() {
        final TunnelSide[] sides = getCurrentWingSides();
        int dropWidth = UI.scale(150);
        int dropHeight = UI.scale(18);

        wingSideDropbox = new Dropbox<String>(dropWidth, sides.length, dropHeight) {
            @Override
            protected String listitem(int i) {
                return sides[i].name;
            }

            @Override
            protected int listitems() {
                return sides.length;
            }

            @Override
            protected void drawitem(GOut g, String item, int idx) {
                g.text(item, new Coord(3, 1));
            }

            @Override
            public void change(String item) {
                super.change(item);
                for (int i = 0; i < sides.length; i++) {
                    if (sides[i].name.equals(item)) {
                        wingSideIndex = i;
                        break;
                    }
                }
                if (wingSideRef != null) {
                    wingSideRef[0] = wingSideIndex;
                }
            }
        };

        if (wingSideIndex >= sides.length) {
            wingSideIndex = 0;
        }
        wingSideDropbox.change(sides[wingSideIndex].name);
        add(wingSideDropbox, wingSidePos);
    }

    private void rebuildWingSideDropbox() {
        if (wingSideDropbox != null) {
            wingSideDropbox.destroy();
        }
        wingSideIndex = 0;
        if (wingSideRef != null) {
            wingSideRef[0] = 0;
        }
        createWingSideDropbox();
    }

    private TunnelSide[] getCurrentTunnelSides() {
        Direction dir = DIRECTIONS[directionIndex];
        return dir.isVertical() ? VERTICAL_TUNNEL_SIDES : HORIZONTAL_TUNNEL_SIDES;
    }

    private TunnelSide[] getCurrentWingSides() {
        Direction dir = DIRECTIONS[directionIndex];
        return dir.isVertical() ? VERTICAL_WING_SIDES : HORIZONTAL_WING_SIDES;
    }

    private WingOption[] getCurrentWingOptions() {
        Direction dir = DIRECTIONS[directionIndex];
        return dir.isVertical() ? VERTICAL_WING_OPTIONS : HORIZONTAL_WING_OPTIONS;
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

        // Initialize from refs if they have values
        if (directionRef != null && directionRef[0] >= 0 && directionRef[0] < DIRECTIONS.length) {
            this.directionIndex = directionRef[0];
            directionDropbox.change(DIRECTIONS[directionIndex]);
        }
        if (supportTypeRef != null && supportTypeRef[0] >= 0 && supportTypeRef[0] < SUPPORT_TYPES.length) {
            this.supportTypeIndex = supportTypeRef[0];
            supportTypeDropbox.change(SUPPORT_TYPES[supportTypeIndex]);
        }
        // Tunnel side, wing option, and wing side are rebuilt when direction changes
    }

    private void confirm() {
        if (confirmRef != null) {
            confirmRef[0] = true;
        }
        hide();
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
