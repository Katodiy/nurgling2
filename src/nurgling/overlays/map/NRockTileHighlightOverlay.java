package nurgling.overlays.map;

import haven.*;
import haven.render.*;
import nurgling.*;
import nurgling.tools.RockResourceMapper;

import java.awt.*;
import java.util.*;

/**
 * Overlay that highlights rock tiles in mines when corresponding bumbling icons are selected.
 * This allows users to see both surface rocks (bumblings) and underground rock tiles
 * when they enable a rock type in Icon Settings.
 */
public class NRockTileHighlightOverlay extends NOverlay {

    public static final int ROCK_TILE_OVERLAY = -2;

    private Set<String> selectedTileResources = new HashSet<>();
    private boolean needsUpdate = false;
    private boolean isEnabled = true;

    // Cache for Icon Settings to detect changes
    private Map<GobIcon.Setting.ID, Boolean> lastIconStates = new HashMap<>();

    public NRockTileHighlightOverlay() {
        super(ROCK_TILE_OVERLAY);
        bc = new Color(255, 200, 100, 100); // Orange-ish color for rock highlights
        System.out.println("Rock Tile Highlight Overlay initialized!");
    }

    /**
     * Updates which rock tile resources should be highlighted based on Icon Settings.
     */
    public void updateSelectedRocks() {
        if (NUtils.getGameUI() == null || NUtils.getGameUI().ui == null) {
            return;
        }

        try {
            GobIcon.Settings iconConf = NUtils.getGameUI().iconconf;
            if (iconConf == null) {
                return;
            }

            Set<String> newSelectedGobResources = new HashSet<>();
            Map<GobIcon.Setting.ID, Boolean> currentStates = new HashMap<>();

            // Collect all selected icons from the settings
            synchronized (iconConf.settings) {
                for (GobIcon.Setting setting : iconConf.settings.values()) {
                    if (setting.show && setting.icon != null && setting.icon.res != null) {
                        String resName = setting.icon.res.name;
                        newSelectedGobResources.add(resName);
                        currentStates.put(setting.id, setting.show);
                    }
                }
            }

            // Check if anything changed
            boolean changed = !currentStates.equals(lastIconStates);
            lastIconStates = currentStates;

            if (changed) {
                // Convert selected gob resources to tile resources
                selectedTileResources = RockResourceMapper.getTileResourcesToHighlight(newSelectedGobResources);
                needsUpdate = true;
                requpdate2 = true;  // Trigger map re-render

                // DEBUG: Print what we're highlighting
                if (!selectedTileResources.isEmpty()) {
                    System.out.println("=== ROCK TILE OVERLAY ===");
                    System.out.println("Selected gob resources: " + newSelectedGobResources);
                    System.out.println("Tile resources to highlight: " + selectedTileResources);
                    System.out.println("========================");
                    System.out.println("DEBUG: Setting requpdate2=true to trigger re-render");
                }
            }
        } catch (Exception e) {
            // Silently ignore errors to avoid breaking the game
        }
    }

    // Track which tiles we've already printed to avoid spam
    private Set<String> printedTileTypes = new HashSet<>();
    private boolean hasLoggedCheck = false;

    /**
     * Checks if a tile should be highlighted based on its resource name.
     */
    private boolean shouldHighlightTile(Coord gc) {
        if (!hasLoggedCheck) {
            System.out.println("DEBUG: shouldHighlightTile() called! isEnabled=" + isEnabled + ", selectedTileResources.size=" + selectedTileResources.size());
            hasLoggedCheck = true;
        }

        if (!isEnabled || selectedTileResources.isEmpty()) {
            return false;
        }

        try {
            MCache map = NUtils.getGameUI().map.glob.map;
            int tileId = map.gettile(gc);

            if (tileId < 0 || tileId >= map.nsets.length) {
                return false;
            }

            Resource.Spec tileSpec = map.nsets[tileId];
            if (tileSpec == null) {
                return false;
            }

            String tileResourceName = tileSpec.name;

            // DEBUG: Print each unique tile type we encounter
            if (!printedTileTypes.contains(tileResourceName)) {
                System.out.println("DEBUG: Tile type encountered: " + tileResourceName);
                printedTileTypes.add(tileResourceName);
            }

            if (selectedTileResources.contains(tileResourceName)) {
                System.out.println("ROCK TILE FOUND (MATCH): " + tileResourceName + " at " + gc);
                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Check if feature is enabled in config
        boolean configEnabled = (Boolean) NConfig.get(NConfig.Key.highlightRockTiles);
        if (isEnabled != configEnabled) {
            setEnabled(configEnabled);
        }

        // Periodically check if icon settings changed
        if (isEnabled) {
            updateSelectedRocks();
        }
    }

    @Override
    public RenderTree.Node makenol(MapMesh mm, Long grid_id, Coord grid_ul) {
        System.out.println("DEBUG: makenol() called! grid_id=" + grid_id + " grid_ul=" + grid_ul);

        if (mm.olvert == null) {
            mm.olvert = mm.makeolvbuf();
        }

        class Buf implements Tiler.MCons {
            short[] fl = new short[16];
            int fn = 0;

            public void faces(MapMesh m, Tiler.MPart d) {
                while (fn + d.f.length > fl.length) {
                    fl = Utils.extend(fl, fl.length * 2);
                }
                for (int fi : d.f) {
                    fl[fn++] = (short) mm.olvert.vl[d.v[fi].vi];
                }
            }
        }

        Coord t = new Coord();
        Buf buf = new Buf();

        System.out.println("DEBUG: Checking tiles in mesh, size=" + mm.sz);

        int checkCount = 0;
        // Check each tile in the mesh
        for (t.y = 0; t.y < mm.sz.y; t.y++) {
            for (t.x = 0; t.x < mm.sz.x; t.x++) {
                checkCount++;
                Coord gc = t.add(mm.ul);

                if (shouldHighlightTile(gc)) {
                    mm.map.tiler(mm.map.gettile(gc)).lay(mm, t, gc, buf, false);
                }
            }
        }

        System.out.println("DEBUG: Checked " + checkCount + " tiles");

        if (buf.fn == 0) {
            return null;
        }

        haven.render.Model mod = new haven.render.Model(
            haven.render.Model.Mode.TRIANGLES,
            mm.olvert.dat,
            new haven.render.Model.Indices(buf.fn, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
                DataBuffer.Filler.of(Arrays.copyOf(buf.fl, buf.fn)))
        );

        return new MapMesh.ShallowWrap(mod, new MapMesh.NOLOrder(id));
    }

    @Override
    public RenderTree.Node makenolol(MapMesh mm, Long grid_id, Coord grid_ul) {
        if (mm.olvert == null) {
            mm.olvert = mm.makeolvbuf();
        }

        class Buf implements Tiler.MCons {
            int mask;
            short[] fl = new short[16];
            int fn = 0;

            public void faces(MapMesh m, Tiler.MPart d) {
                byte[] ef = new byte[d.v.length];
                for (int i = 0; i < d.v.length; i++) {
                    if (d.tcy[i] == 0.0f) ef[i] |= 1;
                    if (d.tcx[i] == 1.0f) ef[i] |= 2;
                    if (d.tcy[i] == 1.0f) ef[i] |= 4;
                    if (d.tcx[i] == 0.0f) ef[i] |= 8;
                }
                while (fn + (d.f.length * 2) > fl.length) {
                    fl = Utils.extend(fl, fl.length * 2);
                }
                for (int i = 0; i < d.f.length; i += 3) {
                    for (int a = 0; a < 3; a++) {
                        int b = (a + 1) % 3;
                        if ((ef[d.f[i + a]] & ef[d.f[i + b]] & mask) != 0) {
                            fl[fn++] = (short) mm.olvert.vl[d.v[d.f[i + a]].vi];
                            fl[fn++] = (short) mm.olvert.vl[d.v[d.f[i + b]].vi];
                        }
                    }
                }
            }
        }

        Area a = Area.sized(mm.ul, mm.sz);
        Buf buf = new Buf();

        // Create boolean grid to track highlighted tiles
        boolean[][] highlighted = new boolean[mm.sz.x][mm.sz.y];
        for (int y = 0; y < mm.sz.y; y++) {
            for (int x = 0; x < mm.sz.x; x++) {
                Coord gc = Coord.of(x, y).add(mm.ul);
                highlighted[x][y] = shouldHighlightTile(gc);
            }
        }

        // Draw borders only where highlighted tiles meet non-highlighted tiles
        for (Coord t : a) {
            int localX = t.x - mm.ul.x;
            int localY = t.y - mm.ul.y;

            if (localX >= 0 && localX < mm.sz.x && localY >= 0 && localY < mm.sz.y &&
                highlighted[localX][localY]) {

                buf.mask = 0;

                // Check each cardinal direction
                for (int d = 0; d < 4; d++) {
                    Coord neighbor = t.add(Coord.uecw[d]);
                    int nx = neighbor.x - mm.ul.x;
                    int ny = neighbor.y - mm.ul.y;

                    // If neighbor is out of bounds or not highlighted, draw border
                    if (nx < 0 || nx >= mm.sz.x || ny < 0 || ny >= mm.sz.y || !highlighted[nx][ny]) {
                        buf.mask |= 1 << d;
                    }
                }

                if (buf.mask != 0) {
                    mm.map.tiler(mm.map.gettile(t)).lay(mm, t.sub(a.ul), t, buf, false);
                }
            }
        }

        if (buf.fn == 0) {
            return null;
        }

        haven.render.Model mod = new haven.render.Model(
            haven.render.Model.Mode.LINES,
            mm.olvert.dat,
            new haven.render.Model.Indices(buf.fn, NumberFormat.UINT16, DataBuffer.Usage.STATIC,
                DataBuffer.Filler.of(Arrays.copyOf(buf.fl, buf.fn)))
        );

        return new MapMesh.ShallowWrap(mod,
            Pipe.Op.compose(new MapMesh.NOLOrder(id), new States.LineWidth(2)));
    }

    @Override
    public boolean requpdate() {
        boolean result = needsUpdate;
        needsUpdate = false;
        if (result) {
            requpdate2 = false;  // Reset after update processed
        }
        return result;
    }

    /**
     * Sets whether this overlay is enabled.
     */
    public void setEnabled(boolean enabled) {
        if (this.isEnabled != enabled) {
            this.isEnabled = enabled;
            needsUpdate = true;
        }
    }

    /**
     * Returns whether this overlay is enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }
}
