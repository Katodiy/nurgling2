package nurgling.overlays.map;

import haven.*;
import nurgling.NConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages baseline claims for hostile claim detection.
 * Stores fingerprints of known/friendly claims and identifies new/hostile ones.
 */
public class ClaimBaseline {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Represents a single claim's fingerprint using bounding box
     */
    public static class ClaimFingerprint {
        public final String type;  // "cplot" or "vlg"
        public final long segmentId;
        public final Coord ul;  // Upper-left corner
        public final Coord br;  // Bottom-right corner

        public ClaimFingerprint(String type, long segmentId, Coord ul, Coord br) {
            this.type = type;
            this.segmentId = segmentId;
            this.ul = ul;
            this.br = br;
        }

        /**
         * Check if another claim matches this one (same claim, possibly expanded)
         */
        public boolean matches(ClaimFingerprint other) {
            if (!type.equals(other.type) || segmentId != other.segmentId) {
                return false;
            }

            // Calculate overlap percentage
            int overlapLeft = Math.max(ul.x, other.ul.x);
            int overlapRight = Math.min(br.x, other.br.x);
            int overlapTop = Math.max(ul.y, other.ul.y);
            int overlapBottom = Math.min(br.y, other.br.y);

            if (overlapLeft >= overlapRight || overlapTop >= overlapBottom) {
                return false; // No overlap
            }

            int overlapArea = (overlapRight - overlapLeft) * (overlapBottom - overlapTop);
            int thisArea = (br.x - ul.x) * (br.y - ul.y);
            int otherArea = (other.br.x - other.ul.x) * (other.br.y - other.ul.y);
            int minArea = Math.min(thisArea, otherArea);

            // Consider it a match if 75%+ of the smaller claim overlaps
            return (overlapArea * 100 / minArea) >= 75;
        }

        public JSONObject toJSON() {
            JSONObject obj = new JSONObject();
            obj.put("type", type);
            obj.put("seg", segmentId);
            obj.put("ul_x", ul.x);
            obj.put("ul_y", ul.y);
            obj.put("br_x", br.x);
            obj.put("br_y", br.y);
            return obj;
        }

        public static ClaimFingerprint fromJSON(JSONObject obj) {
            String type = obj.getString("type");
            long seg = obj.getLong("seg");
            Coord ul = new Coord(obj.getInt("ul_x"), obj.getInt("ul_y"));
            Coord br = new Coord(obj.getInt("br_x"), obj.getInt("br_y"));
            return new ClaimFingerprint(type, seg, ul, br);
        }
    }

    private List<ClaimFingerprint> baselineClaims = new ArrayList<>();
    private String timestamp = null;

    /**
     * Load baseline from config
     */
    public static ClaimBaseline load() {
        ClaimBaseline baseline = new ClaimBaseline();
        Object data = NConfig.get(NConfig.Key.claimBaseline);

        if (data instanceof String) {
            try {
                JSONObject json = new JSONObject((String) data);
                baseline.timestamp = json.optString("timestamp", null);

                JSONArray claims = json.getJSONArray("claims");
                for (int i = 0; i < claims.length(); i++) {
                    baseline.baselineClaims.add(ClaimFingerprint.fromJSON(claims.getJSONObject(i)));
                }
            } catch (Exception e) {
                // Invalid baseline data, start fresh
                baseline.baselineClaims.clear();
                baseline.timestamp = null;
            }
        }

        return baseline;
    }

    /**
     * Save baseline to config
     */
    public void save() {
        JSONObject json = new JSONObject();
        json.put("timestamp", timestamp);

        JSONArray claimsArray = new JSONArray();
        for (ClaimFingerprint claim : baselineClaims) {
            claimsArray.put(claim.toJSON());
        }
        json.put("claims", claimsArray);

        NConfig.set(NConfig.Key.claimBaseline, json.toString());
        NConfig.needUpdate();
    }

    /**
     * Reset baseline with current claims
     */
    public void reset(List<ClaimFingerprint> currentClaims) {
        this.baselineClaims = new ArrayList<>(currentClaims);
        this.timestamp = DATE_FORMAT.format(Instant.now());
        save();
    }

    /**
     * Check if a claim is hostile (not in baseline)
     */
    public boolean isHostile(ClaimFingerprint claim) {
        if (baselineClaims.isEmpty()) {
            // No baseline set - all claims are hostile
            return true;
        }

        for (ClaimFingerprint baseline : baselineClaims) {
            if (baseline.matches(claim)) {
                return false; // Found in baseline
            }
        }

        return true; // Not in baseline = hostile
    }

    /**
     * Get timestamp of last baseline reset
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Check if baseline has been set
     */
    public boolean isSet() {
        return !baselineClaims.isEmpty();
    }

    /**
     * Get number of baseline claims
     */
    public int getCount() {
        return baselineClaims.size();
    }
}
