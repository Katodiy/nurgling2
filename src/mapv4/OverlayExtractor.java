package mapv4;

import haven.*;

import java.util.ArrayList;
import java.util.List;

public class OverlayExtractor {

    // Overlay resource patterns that the server recognizes
    private static final String[] SUPPORTED_PATTERNS = {
        "cplot-f", "cplot-o",           // Claims
        "vlg-f", "vlg-o", "vlg-sar",    // Village
        "prov/"                          // Province/Realm
    };

    public static List<OverlayData> extractOverlays(MCache.Grid grid, long gridId) {
        List<OverlayData> result = new ArrayList<>();

        if (grid.ols == null || grid.ol == null) {
            return result;
        }

        for (int i = 0; i < grid.ols.length && i < grid.ol.length; i++) {
            try {
                Indir<Resource> olRef = grid.ols[i];
                boolean[] mask = grid.ol[i];

                if (olRef == null || mask == null) {
                    continue;
                }

                Resource olRes = olRef.get();
                if (olRes == null) {
                    continue;
                }

                String resourceName = olRes.name;

                // Filter to only supported overlay types
                if (!isSupportedOverlay(resourceName)) {
                    continue;
                }

                // Convert boolean mask to sparse indices
                int[] tiles = toSparseIndices(mask);
                if (tiles.length == 0) {
                    continue;
                }

                // Extract tag from resource name
                String tag = extractTag(resourceName);

                result.add(new OverlayData(tag, resourceName, tiles));

            } catch (Loading e) {
                // Resource not loaded yet, skip
            } catch (Exception e) {
                // Skip problematic overlays
            }
        }

        return result;
    }

    private static boolean isSupportedOverlay(String resourceName) {
        if (resourceName == null) return false;

        // Skip custom overlays (areas-o*, minesup-o, areash-o)
        if (resourceName.contains("areas-o") ||
            resourceName.contains("minesup-o") ||
            resourceName.contains("areash-o")) {
            return false;
        }

        // Check for supported patterns
        for (String pattern : SUPPORTED_PATTERNS) {
            if (resourceName.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static String extractTag(String resourceName) {
        if (resourceName.contains("cplot")) return "cplot";
        if (resourceName.contains("vlg")) return "vlg";
        if (resourceName.contains("prov")) return "prov";
        return "unknown";
    }

    private static int[] toSparseIndices(boolean[] mask) {
        // Count true values first
        int count = 0;
        for (boolean b : mask) {
            if (b) count++;
        }

        // Build sparse array
        int[] indices = new int[count];
        int idx = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                indices[idx++] = i;
            }
        }
        return indices;
    }

    // Compute hash for change detection
    public static int computeHash(List<OverlayData> overlays) {
        int hash = 0;
        for (OverlayData od : overlays) {
            hash = 31 * hash + od.hashCode();
        }
        return hash;
    }
}
