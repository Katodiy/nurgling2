package nurgling.styles;

import java.awt.Color;

/**
 * Centralized style constants for tooltips.
 * All spacing values are in logical pixels and should be scaled with UI.scale() when used.
 */
public final class TooltipStyle {
    private TooltipStyle() {} // Prevent instantiation

    // ============ COLORS ============

    /** LP (Learning Points) value color - purple */
    public static final Color COLOR_LP = new Color(210, 178, 255);  // #D2B2FF

    /** LP/H and LP/H/W value color - cyan */
    public static final Color COLOR_LPH = new Color(0, 238, 255);  // #00EEFF

    /** Study time value color - green */
    public static final Color COLOR_STUDY_TIME = new Color(153, 255, 132);  // #99FF84

    /** Mental weight value color - pink */
    public static final Color COLOR_MENTAL_WEIGHT = new Color(255, 148, 232);  // #FF94E8

    /** EXP cost value color - yellow */
    public static final Color COLOR_EXP_COST = new Color(255, 255, 130);  // #FFFF82

    /** Resource path text color - gray */
    public static final Color COLOR_RESOURCE_PATH = new Color(128, 128, 128);

    /** Tooltip background color - dark green-gray at 90% opacity */
    public static final Color COLOR_TOOLTIP_BG = new Color(37, 43, 41, 230);  // #252B29 @ 90%

    /** Tooltip border color - yellow */
    public static final Color COLOR_TOOLTIP_BORDER = new Color(244, 247, 21, 192);

    // ============ SPACING (logical pixels - use UI.scale()) ============

    /** Spacing between major sections (Name, LP Info group, Resource) */
    public static final int SECTION_SPACING = 10;

    /** Spacing within LP Info group (LP line, Study time, Mental weight) */
    public static final int INTERNAL_SPACING = 7;

    /** Horizontal spacing between elements on the same line */
    public static final int HORIZONTAL_SPACING = 7;

    /** Spacing between quality icon and quality number */
    public static final int ICON_TO_TEXT_SPACING = 3;

    /** Outer padding around tooltip content (top, left, right) */
    public static final int OUTER_PADDING = 10;

    /** Outer padding for bottom of tooltip content */
    public static final int OUTER_PADDING_BOTTOM = 7;

    /** GLPanel background margin beyond tooltip content */
    public static final int GLPANEL_MARGIN = 2;

    // ============ FONT SIZES (logical pixels - use UI.scale()) ============

    /** Font size for item name and quality */
    public static final int FONT_SIZE_NAME = 12;

    /** Font size for curio stats (LP, study time, etc.) */
    public static final int FONT_SIZE_BODY = 11;

    /** Font size for resource path */
    public static final int FONT_SIZE_RESOURCE = 9;
}
