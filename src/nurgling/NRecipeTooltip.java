package nurgling;

import haven.*;
import nurgling.styles.TooltipStyle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom tooltip builder for recipe tooltips (MenuGrid pagina).
 * Renders recipe name, ingredients with quantities, skills, and description.
 */
public class NRecipeTooltip {

    // Cached foundries
    private static Text.Foundry nameFoundry = null;
    private static Text.Foundry quantityFoundry = null;
    private static Text.Foundry skillLabelFoundry = null;
    private static Text.Foundry skillNameFoundry = null;
    private static Text.Foundry descFoundry = null;

    private static Text.Foundry getNameFoundry() {
        if (nameFoundry == null) {
            nameFoundry = TooltipStyle.createFoundry(true, 12, Color.WHITE);  // Semibold
        }
        return nameFoundry;
    }

    private static Text.Foundry getQuantityFoundry() {
        if (quantityFoundry == null) {
            quantityFoundry = TooltipStyle.createFoundry(true, 11, Color.WHITE);
        }
        return quantityFoundry;
    }

    private static Text.Foundry getSkillLabelFoundry() {
        if (skillLabelFoundry == null) {
            skillLabelFoundry = TooltipStyle.createFoundry(true, 11, Color.WHITE);
        }
        return skillLabelFoundry;
    }

    private static Text.Foundry getSkillNameFoundry() {
        if (skillNameFoundry == null) {
            skillNameFoundry = TooltipStyle.createFoundry(false, 11, Color.WHITE);
        }
        return skillNameFoundry;
    }

    private static Text.Foundry getDescFoundry() {
        if (descFoundry == null) {
            descFoundry = TooltipStyle.createFoundry(false, 9, Color.WHITE);
        }
        return descFoundry;
    }

    /**
     * Build a recipe tooltip from the given name, key binding, and item info list.
     *
     * @param name Recipe name
     * @param key  Key binding (unused, kept for API compatibility)
     * @param info List of ItemInfo from the pagina
     * @return Rendered tooltip image
     */
    public static BufferedImage build(String name, KeyMatch key, List<ItemInfo> info) {
        // Render name - all white, semibold 12px
        BufferedImage nameImg = TooltipStyle.cropTopOnly(renderName(name));
        BufferedImage ret = nameImg;

        if (info != null && !info.isEmpty()) {
            // Extract Inputs, Skills, Cost, and Pagina
            Object inputsInfo = null;
            Object skillsInfo = null;
            Object costInfo = null;
            String paginaStr = null;

            for (ItemInfo ii : info) {
                String className = ii.getClass().getSimpleName();
                if (className.equals("Inputs")) {
                    inputsInfo = ii;
                } else if (className.equals("Skills")) {
                    skillsInfo = ii;
                } else if (className.equals("Cost")) {
                    costInfo = ii;
                } else if (ii instanceof ItemInfo.Pagina) {
                    paginaStr = ((ItemInfo.Pagina) ii).str;
                }
            }

            // Render Inputs line (icons with quantities)
            BufferedImage inputsLine = null;
            if (inputsInfo != null) {
                inputsLine = TooltipStyle.cropTopOnly(renderInputsLine(inputsInfo));
            }

            // Render Skills line
            BufferedImage skillsLine = null;
            if (skillsInfo != null) {
                skillsLine = TooltipStyle.cropTopOnly(renderSkillsLine(skillsInfo));
            }

            // Render Cost line (for skills)
            BufferedImage costLine = null;
            if (costInfo != null) {
                costLine = TooltipStyle.cropTopOnly(renderCostLine(costInfo));
            }

            // Render description (Pagina) with word wrap at 200px
            BufferedImage descImg = null;
            if (paginaStr != null && !paginaStr.isEmpty()) {
                descImg = renderWrappedText(paginaStr, UI.scale(200));
            }

            // Combine with baseline-to-top spacings:
            // Name to ingredients/cost: 7px (from name baseline to next line top)
            // Between body lines: 10-12px
            // To description: 10px

            // Get font descents for baseline-relative spacing
            int nameDescent = TooltipStyle.getFontDescent(12);
            int bodyDescent = TooltipStyle.getFontDescent(11);

            // Track if we've added any content after name (for proper spacing)
            boolean hasBodyContent = false;

            if (inputsLine != null) {
                int spacing = UI.scale(7) - nameDescent;
                ret = ItemInfo.catimgs(spacing, ret, inputsLine);
                hasBodyContent = true;
            }
            if (skillsLine != null) {
                int spacing = hasBodyContent ? (UI.scale(12) - bodyDescent) : (UI.scale(7) - nameDescent);
                ret = ItemInfo.catimgs(spacing, ret, skillsLine);
                hasBodyContent = true;
            }
            if (costLine != null) {
                // Cost is always 7px from name (baseline-to-top), uses 12px font
                int costDescent = TooltipStyle.getFontDescent(12);
                int spacing = hasBodyContent ? (UI.scale(10) - bodyDescent) : (UI.scale(7) - nameDescent);
                ret = ItemInfo.catimgs(spacing, ret, costLine);
                hasBodyContent = true;
            }
            if (descImg != null) {
                int spacing = hasBodyContent ? (UI.scale(10) - bodyDescent) : (UI.scale(7) - nameDescent);
                ret = ItemInfo.catimgs(spacing, ret, descImg);
            }
        }

        // Add 10px padding around the content
        ret = addPadding(ret);

        return ret;
    }

    /**
     * Add 10px padding around the tooltip content.
     */
    private static BufferedImage addPadding(BufferedImage img) {
        if (img == null) return null;
        int padding = UI.scale(10);
        int newWidth = img.getWidth() + padding * 2;
        int newHeight = img.getHeight() + padding * 2;
        BufferedImage result = TexI.mkbuf(new Coord(newWidth, newHeight));
        Graphics g = result.getGraphics();
        g.drawImage(img, padding, padding, null);
        g.dispose();
        return result;
    }

    /**
     * Render recipe name - all white, semibold 12px.
     */
    private static BufferedImage renderName(String name) {
        return getNameFoundry().render(name, Color.WHITE).img;
    }

    /**
     * Render inputs line: icon + "xN" for each input.
     */
    private static BufferedImage renderInputsLine(Object inputsInfo) {
        try {
            Field inputsField = inputsInfo.getClass().getDeclaredField("inputs");
            inputsField.setAccessible(true);
            Object[] inputs = (Object[]) inputsField.get(inputsInfo);

            if (inputs == null || inputs.length == 0) return null;

            List<BufferedImage> parts = new ArrayList<>();
            int gap = UI.scale(4);

            int iconToNumGap = UI.scale(3);

            for (Object input : inputs) {
                // Get img and num fields
                Field imgField = input.getClass().getDeclaredField("img");
                Field numField = input.getClass().getDeclaredField("num");
                imgField.setAccessible(true);
                numField.setAccessible(true);

                BufferedImage icon = (BufferedImage) imgField.get(input);
                int num = numField.getInt(input);

                // Render "xN" text
                BufferedImage numImg = getQuantityFoundry().render("x" + num, Color.WHITE).img;

                // Combine icon + 3px gap + number
                int w = icon.getWidth() + iconToNumGap + numImg.getWidth();
                int h = Math.max(icon.getHeight(), numImg.getHeight());
                BufferedImage combined = TexI.mkbuf(new Coord(w, h));
                Graphics g = combined.getGraphics();
                g.drawImage(icon, 0, (h - icon.getHeight()) / 2, null);
                g.drawImage(numImg, icon.getWidth() + iconToNumGap, (h - numImg.getHeight()) / 2, null);
                g.dispose();

                parts.add(combined);
            }

            // Compose all parts horizontally with gap
            return composeHorizontalWithGap(parts, gap);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Render skills line: "Skills:" + 3px + icon + 3px + skill name.
     */
    private static BufferedImage renderSkillsLine(Object skillsInfo) {
        try {
            Field skillsField = skillsInfo.getClass().getDeclaredField("skills");
            skillsField.setAccessible(true);
            Resource[] skills = (Resource[]) skillsField.get(skillsInfo);

            if (skills == null || skills.length == 0) return null;

            int gap3 = UI.scale(3);

            // Render "Skills:" label (no trailing space - we'll add 3px gap)
            BufferedImage labelImg = getSkillLabelFoundry().render("Skills:", Color.WHITE).img;

            // Build the line manually with specific gaps
            List<BufferedImage> allParts = new ArrayList<>();
            allParts.add(labelImg);

            for (int i = 0; i < skills.length; i++) {
                Resource skill = skills[i];

                // Get skill icon and scale to 12x12
                BufferedImage scaledIcon = null;
                Resource.Image imgLayer = skill.layer(Resource.imgc);
                if (imgLayer != null) {
                    BufferedImage icon = imgLayer.img;
                    int iconSize = UI.scale(12);
                    scaledIcon = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = scaledIcon.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(icon, 0, 0, iconSize, iconSize, null);
                    g2d.dispose();
                }

                // Get skill name from tooltip layer
                Resource.Tooltip tt = skill.layer(Resource.tooltip);
                String skillName = (tt != null) ? tt.t : skill.name;
                // Extract just the skill name from path if needed
                if (skillName.contains("/")) {
                    skillName = skillName.substring(skillName.lastIndexOf("/") + 1);
                    // Capitalize first letter
                    skillName = Character.toUpperCase(skillName.charAt(0)) + skillName.substring(1);
                }

                BufferedImage skillNameImg = getSkillNameFoundry().render(skillName, Color.WHITE).img;

                // Add comma before skill if not first
                if (i > 0) {
                    BufferedImage comma = getSkillNameFoundry().render(", ", Color.WHITE).img;
                    allParts.add(comma);
                }

                // Add icon and name with 3px gaps
                if (scaledIcon != null) {
                    allParts.add(scaledIcon);
                }
                allParts.add(skillNameImg);
            }

            // Compose with specific gaps: 3px after "Skills:", 3px between icon and name
            return composeSkillsLine(allParts, gap3);
        } catch (Exception e) {
            return null;
        }
    }

    // Cost color #FFFF82
    private static final Color COLOR_COST = new Color(0xFF, 0xFF, 0x82);

    /**
     * Render cost line: value + " EXP" in yellow, 12px semibold.
     */
    private static BufferedImage renderCostLine(Object costInfo) {
        try {
            Field encField = costInfo.getClass().getDeclaredField("enc");
            encField.setAccessible(true);
            int cost = encField.getInt(costInfo);

            if (cost <= 0) return null;

            // Render cost value with color #FFFF82, 12px semibold + " EXP"
            Text.Foundry costFoundry = TooltipStyle.createFoundry(true, 12, COLOR_COST);
            BufferedImage costImg = costFoundry.render(Utils.thformat(cost) + " EXP", COLOR_COST).img;

            return costImg;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Compose skills line with specific gaps:
     * - 3px after "Skills:" label
     * - 3px between icon and skill name
     * - No gap for comma (it has built-in spacing)
     */
    private static BufferedImage composeSkillsLine(List<BufferedImage> parts, int gap) {
        if (parts.isEmpty()) return null;

        // Calculate total width
        int totalWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < parts.size(); i++) {
            BufferedImage img = parts.get(i);
            totalWidth += img.getWidth();
            maxHeight = Math.max(maxHeight, img.getHeight());
            // Add gap after label (index 0) and after each icon (even indices after 0, before name)
            if (i == 0) {
                totalWidth += gap; // Gap after "Skills:"
            } else if (i > 0 && i < parts.size() - 1) {
                // Check if current is icon (12x12) and next is text
                BufferedImage next = parts.get(i + 1);
                if (img.getWidth() == UI.scale(12) && img.getHeight() == UI.scale(12) && next.getHeight() != UI.scale(12)) {
                    totalWidth += gap; // Gap between icon and name
                }
            }
        }

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;
        for (int i = 0; i < parts.size(); i++) {
            BufferedImage img = parts.get(i);
            g.drawImage(img, x, (maxHeight - img.getHeight()) / 2, null);
            x += img.getWidth();
            // Add gap after label and after icons
            if (i == 0) {
                x += gap; // Gap after "Skills:"
            } else if (i > 0 && i < parts.size() - 1) {
                BufferedImage next = parts.get(i + 1);
                if (img.getWidth() == UI.scale(12) && img.getHeight() == UI.scale(12) && next.getHeight() != UI.scale(12)) {
                    x += gap; // Gap between icon and name
                }
            }
        }
        g.dispose();
        return result;
    }

    /**
     * Compose multiple images horizontally with no gap.
     */
    private static BufferedImage composeHorizontal(BufferedImage... imgs) {
        int totalWidth = 0;
        int maxHeight = 0;
        for (BufferedImage img : imgs) {
            if (img != null) {
                totalWidth += img.getWidth();
                maxHeight = Math.max(maxHeight, img.getHeight());
            }
        }
        if (totalWidth == 0) return null;

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;
        for (BufferedImage img : imgs) {
            if (img != null) {
                g.drawImage(img, x, (maxHeight - img.getHeight()) / 2, null);
                x += img.getWidth();
            }
        }
        g.dispose();
        return result;
    }

    /**
     * Compose list of images horizontally with specified gap.
     */
    private static BufferedImage composeHorizontalWithGap(List<BufferedImage> imgs, int gap) {
        if (imgs.isEmpty()) return null;

        int totalWidth = 0;
        int maxHeight = 0;
        for (BufferedImage img : imgs) {
            totalWidth += img.getWidth();
            maxHeight = Math.max(maxHeight, img.getHeight());
        }
        totalWidth += gap * (imgs.size() - 1);

        BufferedImage result = TexI.mkbuf(new Coord(totalWidth, maxHeight));
        Graphics g = result.getGraphics();
        int x = 0;
        for (int i = 0; i < imgs.size(); i++) {
            BufferedImage img = imgs.get(i);
            g.drawImage(img, x, (maxHeight - img.getHeight()) / 2, null);
            x += img.getWidth();
            if (i < imgs.size() - 1) {
                x += gap;
            }
        }
        g.dispose();
        return result;
    }

    /**
     * Render text with word wrapping at specified max width.
     * Uses Open Sans 9px regular font with baseline-to-top spacing between lines.
     */
    private static BufferedImage renderWrappedText(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return null;

        Text.Foundry fnd = getDescFoundry();

        // Create temporary image to get font metrics
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tmp.createGraphics();
        Font font = fnd.font;
        FontMetrics fm = g2d.getFontMetrics(font);
        g2d.dispose();

        // Split text into words
        String[] words = text.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else {
                String testLine = currentLine + " " + word;
                int testWidth = fm.stringWidth(testLine);
                if (testWidth <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    // Line is full, start new line
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
        }
        // Add last line
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        if (lines.isEmpty()) return null;

        // Render each line and crop top
        List<BufferedImage> lineImages = new ArrayList<>();
        for (String line : lines) {
            BufferedImage lineImg = fnd.render(line, Color.WHITE).img;
            lineImages.add(TooltipStyle.cropTopOnly(lineImg));
        }

        // Use baseline-to-top spacing: desired line spacing minus font descent
        // For 9px font, use natural line height (ascent + descent + leading)
        int descent = TooltipStyle.getFontDescent(9);
        int lineSpacing = UI.scale(2) - descent;  // Small gap from baseline to next line top
        if (lineSpacing < 0) lineSpacing = 0;

        return ItemInfo.catimgs(lineSpacing, lineImages.toArray(new BufferedImage[0]));
    }
}
