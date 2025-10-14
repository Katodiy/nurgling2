package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.resutil.Curiosity;
import nurgling.*;
import nurgling.iteminfo.NCuriosity;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Extension for adding Study Desk specific functionality to inventory windows
 */
public class StudyDeskInventoryExtension {

    /**
     * Adds a Plan button and details panel to inventory windows that belong to Study Desk containers
     * @param inventory The inventory to potentially extend
     */
    public static void addPlanButtonIfStudyDesk(NInventory inventory) {
        if (inventory != null && isStudyDeskInventory(inventory)) {
            addPlanButton(inventory);
            addDetailsPanel(inventory);
        }
    }

    /**
     * Checks if the given inventory belongs to a Study Desk container
     * @param inventory The inventory to check
     * @return true if this is a Study Desk inventory
     */
    public static boolean isStudyDeskInventory(NInventory inventory) {
        if (inventory.parentGob == null) return false;
        // Get the drawable attribute from the gob
        Drawable drawable = inventory.parentGob.getattr(Drawable.class);
        if (drawable != null && drawable.getres() != null) {
            String resName = drawable.getres().name;
            return "gfx/terobjs/studydesk".equals(resName);
        }
        return false;
    }

    /**
     * Adds the Plan button to the inventory window
     * @param inventory The inventory to add the button to
     */
    private static void addPlanButton(NInventory inventory) {
        if (inventory.parent == null) return;

        Button planButton = new Button(UI.scale(50), "Plan") {
            @Override
            public void click() {
                openStudyDeskPlanner(inventory);
            }
        };

        // Position the button below the inventory grid, centered
        inventory.parent.add(planButton, new Coord(
            inventory.sz.x / 2 - UI.scale(25),
            inventory.sz.y + UI.scale(5)
        ));
    }

    /**
     * Opens the Study Desk Planner widget positioned next to the study desk inventory
     */
    private static void openStudyDeskPlanner(NInventory inventory) {
        NGameUI gameUI = NUtils.getGameUI();
        if (gameUI != null) {
            // Calculate position to the right of the study desk window
            Coord plannerPos = calculatePlannerPosition(inventory);

            // Get study desk gob hash
            String gobHash = null;
            if (inventory.parentGob != null && inventory.parentGob.ngob != null) {
                gobHash = inventory.parentGob.ngob.hash;
            }

            if (gameUI.studyDeskPlanner == null) {
                gameUI.studyDeskPlanner = new StudyDeskPlannerWidget();
                gameUI.add(gameUI.studyDeskPlanner, plannerPos);
                if (gobHash != null) {
                    gameUI.studyDeskPlanner.setStudyDeskHash(gobHash);
                }
                gameUI.studyDeskPlanner.show(); // Explicitly show on first creation
            } else {
                // Toggle visibility for subsequent clicks
                if (gameUI.studyDeskPlanner.visible()) {
                    gameUI.studyDeskPlanner.hide();
                } else {
                    // Reposition and set gob hash before showing
                    gameUI.studyDeskPlanner.move(plannerPos);
                    if (gobHash != null) {
                        gameUI.studyDeskPlanner.setStudyDeskHash(gobHash);
                    }
                    gameUI.studyDeskPlanner.show();
                }
            }
        }
    }

    /**
     * Calculates the position for the planner widget next to the study desk
     */
    private static Coord calculatePlannerPosition(NInventory inventory) {
        if (inventory.parent != null && inventory.parent instanceof Window) {
            Window window = (Window) inventory.parent;
            // Position to the right of the window with a small gap
            Coord windowPos = window.c;
            Coord windowSize = window.sz;
            return new Coord(windowPos.x + windowSize.x + UI.scale(10), windowPos.y);
        }
        // Fallback to default position if we can't determine window position
        return new Coord(200, 100);
    }

    /**
     * Adds a details panel showing curio information
     */
    private static void addDetailsPanel(NInventory inventory) {
        if (inventory.parent == null) return;

        // Position the panel to the right of the inventory
        Coord panelPos = new Coord(inventory.sz.x + UI.scale(10), 0);

        // Reserve space for Total LP at the bottom
        int scrollHeight = inventory.sz.y - UI.scale(25);
        Coord scrollSize = new Coord(UI.scale(160), scrollHeight);

        // Create the content panel with scrolling support
        StudyDeskDetailsPanel detailsPanel = new StudyDeskDetailsPanel(new Coord(scrollSize.x, UI.scale(50)), inventory);

        // Wrap in a Scrollport
        Scrollport scrollport = new Scrollport(scrollSize);
        scrollport.cont.add(detailsPanel, Coord.z);
        inventory.parent.add(scrollport, panelPos);

        // Add Total LP label below the scrollport
        Label totalLPLabel = new Label("Total LP: 0");
        inventory.parent.add(totalLPLabel, new Coord(panelPos.x, panelPos.y + scrollHeight + UI.scale(5)));

        // Store reference for updates
        detailsPanel.totalLPLabel = totalLPLabel;
        detailsPanel.scrollport = scrollport;
    }

    /**
     * Panel that displays details about curios in the study desk
     */
    public static class StudyDeskDetailsPanel extends Widget {
        private final NInventory inventory;
        private static final Text.Foundry fnd = new Text.Foundry(Text.sans, 10);
        private static final int LINE_HEIGHT = UI.scale(30);
        private Map<String, CurioInfo> cachedInfo = new HashMap<>();
        private int lastItemCount = -1;
        Label totalLPLabel;
        Scrollport scrollport;

        public StudyDeskDetailsPanel(Coord sz, NInventory inventory) {
            super(sz);
            this.inventory = inventory;
        }

        @Override
        public void tick(double dt) {
            super.tick(dt);

            // Update every 10 ticks like NInventory does
            if (NUtils.getTickId() % 10 == 0) {
                cachedInfo = calculateCurioInfo();

                // Check if we need to rebuild (item count changed)
                if (cachedInfo.size() != lastItemCount) {
                    lastItemCount = cachedInfo.size();
                    rebuildContent(cachedInfo);
                }
            }
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);

            // Sort alphabetically by item name
            List<CurioInfo> sortedCurios = new ArrayList<>(cachedInfo.values());
            sortedCurios.sort(Comparator.comparing(a -> a.name, String.CASE_INSENSITIVE_ORDER));

            // Calculate total LP
            int totalLP = 0;
            for (CurioInfo info : cachedInfo.values()) {
                totalLP += info.totalLP;
            }
            updateTotalLP(totalLP);

            int y = 0;
            for (CurioInfo info : sortedCurios) {
                // Draw icon if available
                if (info.resource != null) {
                    try {
                        Resource.Image img = info.resource.layer(Resource.imgc);
                        if (img != null) {
                            TexI scaledImg = new TexI(img.scaled());
                            Coord iconSize = UI.scale(new Coord(16, 16));
                            g.image(scaledImg, new Coord(0, y), iconSize);
                        }
                    } catch (Exception e) {
                        // Skip icon if there's an issue
                    }
                }

                // Draw quantity and time text on first line (convert to real time)
                int realTime = (int)(info.totalTime / NCuriosity.server_ratio);
                String timeText = String.format("x%d - %s", info.count, formatTime(realTime));
                Text t = fnd.render(timeText, Color.WHITE);
                g.image(t.tex(), new Coord(UI.scale(20), y + 2));

                // Draw LP text on second line
                String lpText = String.format("LP: %,d", info.totalLP);
                Text lpTex = fnd.render(lpText, new Color(192, 192, 255));
                g.image(lpTex.tex(), new Coord(UI.scale(20), y + UI.scale(14)));

                y += LINE_HEIGHT;
            }
        }

        private void rebuildContent(Map<String, CurioInfo> curioInfo) {
            // Calculate required height - ensure it's enough to trigger scrollbar
            int contentHeight = curioInfo.size() * LINE_HEIGHT + UI.scale(10);
            // Ensure minimum height
            contentHeight = Math.max(contentHeight, UI.scale(50));

            // Force resize if different
            Coord newSize = new Coord(sz.x, contentHeight);
            if (!sz.equals(newSize)) {
                resize(newSize);

                // Update scrollport container to recalculate scrollbar
                if (scrollport != null && scrollport.cont != null) {
                    scrollport.cont.update();
                }
            }
        }

        private void updateTotalLP(int totalLP) {
            if (totalLPLabel != null) {
                String totalText = String.format("Total LP: %,d", totalLP);
                totalLPLabel.settext(totalText);
                totalLPLabel.setcolor(new Color(255, 215, 0)); // Gold color
            }
        }

        private Map<String, CurioInfo> calculateCurioInfo() {
            Map<String, CurioInfo> curioInfo = new HashMap<>();

            try {
                ArrayList<WItem> items = inventory.getItems();

                for (WItem witem : items) {
                    if (witem.item == null) continue;

                    // Get item info
                    List<ItemInfo> itemInfos = witem.item.info();
                    if (itemInfos == null) continue;

                    // Check if this is a curio
                    Curiosity curiosity = ItemInfo.find(Curiosity.class, itemInfos);
                    if (curiosity == null) continue;

                    // Get resource name for grouping
                    String resourceName = null;
                    String displayName = "Unknown";
                    Resource resource = null;

                    if (witem.item.getres() != null) {
                        resource = witem.item.getres();
                        resourceName = resource.name;

                        // Try to get display name
                        if (witem.item instanceof NGItem) {
                            String name = ((NGItem) witem.item).name();
                            if (name != null && !name.isEmpty()) {
                                displayName = name;
                            }
                        }
                    }

                    String key = resourceName != null ? resourceName : displayName;

                    // Add or update curio info
                    CurioInfo info = curioInfo.get(key);
                    if (info == null) {
                        info = new CurioInfo(displayName, resource, curiosity.time, curiosity.exp);
                        curioInfo.put(key, info);
                    } else {
                        info.count++;
                        info.totalTime += curiosity.time;
                        info.totalLP += curiosity.exp;
                    }
                }
            } catch (Exception e) {
                // Silently fail if we can't get items
            }

            return curioInfo;
        }

        private String formatTime(int seconds) {
            if (seconds == 0) {
                return "0s";
            }

            int days = seconds / 86400;
            int hours = (seconds % 86400) / 3600;
            int minutes = (seconds % 3600) / 60;
            int secs = seconds % 60;

            StringBuilder sb = new StringBuilder();
            if (days > 0) {
                sb.append(days).append("d");
            }
            if (hours > 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(hours).append("h");
            }
            if (minutes > 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(minutes).append("m");
            }
            if (secs > 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(secs).append("s");
            }

            return sb.toString();
        }

        private static class CurioInfo {
            String name;
            Resource resource;
            int studyTime;
            int learningPoints;
            int count = 1;
            int totalTime;
            int totalLP;

            CurioInfo(String name, Resource resource, int studyTime, int learningPoints) {
                this.name = name;
                this.resource = resource;
                this.studyTime = studyTime;
                this.learningPoints = learningPoints;
                this.totalTime = studyTime;
                this.totalLP = learningPoints;
            }
        }
    }
}