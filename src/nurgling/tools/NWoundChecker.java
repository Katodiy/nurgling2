package nurgling.tools;

import haven.*;
import nurgling.NUtils;

import java.util.List;

/**
 * Utility class for checking wounds in Health & Wounds window.
 * Used to stop bots when dangerous wounds are detected.
 */
public class NWoundChecker {
    
    // Resource name for Scrapes & Cuts wound
    public static final String SCRAPES_CUTS_RES = "paginae/wound/scrapesncuts";
    
    /**
     * Check if player has "Scrapes & Cuts" wound with damage >= threshold
     * @param damageThreshold minimum damage to trigger (e.g. 4)
     * @return true if wound exists with damage >= threshold
     */
    public static boolean hasScrapesAndCutsAboveThreshold(int damageThreshold) {
        try {
            CharWnd chrwdg = NUtils.getGameUI().chrwdg;
            if (chrwdg == null || chrwdg.wound == null) {
                return false;
            }
            
            WoundWnd woundWnd = chrwdg.wound;
            if (woundWnd.wounds == null) {
                return false;
            }
            
            for (WoundWnd.Wound wound : woundWnd.wounds.wounds) {
                try {
                    String resName = wound.res.get().name;
                    if (SCRAPES_CUTS_RES.equals(resName)) {
                        int damage = getWoundDamage(wound);
                        if (damage >= damageThreshold) {
                            return true;
                        }
                    }
                } catch (Loading l) {
                    // Resource not loaded yet, skip
                }
            }
        } catch (Exception e) {
            // Silently ignore errors
        }
        return false;
    }
    
    /**
     * Extract damage value from wound's ItemInfo
     * Damage info is typically in the format: [resId, currentDamage, maxDamage, healRate]
     */
    private static int getWoundDamage(WoundWnd.Wound wound) {
        try {
            List<ItemInfo> info = wound.info();
            if (info == null) return 0;
            
            for (ItemInfo inf : info) {
                // The Damage class is loaded from resources, check by class name
                String className = inf.getClass().getSimpleName();
                if (className.equals("Damage")) {
                    // Try to get damage value via reflection since class is loaded dynamically
                    try {
                        java.lang.reflect.Field dmgField = inf.getClass().getField("dmg");
                        if (dmgField != null) {
                            return ((Number) dmgField.get(inf)).intValue();
                        }
                    } catch (NoSuchFieldException e) {
                        // Try 'damage' field name
                        try {
                            java.lang.reflect.Field damageField = inf.getClass().getField("damage");
                            if (damageField != null) {
                                return ((Number) damageField.get(inf)).intValue();
                            }
                        } catch (NoSuchFieldException e2) {
                            // Try getting all fields to find numeric damage value
                            for (java.lang.reflect.Field f : inf.getClass().getFields()) {
                                if (f.getType() == int.class || f.getType() == Integer.class) {
                                    int val = ((Number) f.get(inf)).intValue();
                                    // Assume first reasonable numeric field is damage
                                    if (val > 0 && val < 1000) {
                                        return val;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Fallback: try to parse from raw data
            // Raw data format: [[nameResId], [damageResId, currentDmg, maxDmg, healRate], [treatmentsResId, count]]
            if (wound.rawinfo != null && wound.rawinfo.data != null) {
                for (Object obj : wound.rawinfo.data) {
                    if (obj instanceof Object[]) {
                        Object[] arr = (Object[]) obj;
                        // Look for damage array (has 4 elements typically)
                        if (arr.length >= 3 && arr.length <= 5) {
                            // Skip if first element looks like a name resource ID (high number alone)
                            if (arr.length == 1) continue;
                            
                            // Check if second element could be damage value
                            if (arr.length >= 2 && arr[1] instanceof Number) {
                                int possibleDamage = ((Number) arr[1]).intValue();
                            // Damage is usually a small number, resource IDs are large
                            if (possibleDamage > 0 && possibleDamage < 100) {
                                    return possibleDamage;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore errors
        }
        return 0;
    }
}

