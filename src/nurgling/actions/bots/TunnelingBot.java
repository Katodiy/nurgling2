package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tasks.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;
import nurgling.widgets.TunnelingDialog;
import nurgling.widgets.TunnelingDialog.Direction;
import nurgling.widgets.TunnelingDialog.TunnelSide;
import nurgling.widgets.TunnelingDialog.SupportType;
import nurgling.widgets.TunnelingDialog.WingOption;

import java.util.ArrayList;

import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class TunnelingBot implements Action {

    // All support types that can be used as a starting point
    private static final NAlias ALL_SUPPORTS = new NAlias(
            "minebeam", "column", "towercap", "ladder", "minesupport", "naturalminesupport"
    );

    // Tiles that need mining
    private static final NAlias MINEABLE_TILES = new NAlias("rock", "tiles/cave");

    // Tile size in world units
    private static final double TILE_SIZE = 11.0;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        // Show configuration dialog
        int[] directionRef = new int[]{0};
        int[] tunnelSideRef = new int[]{0};
        int[] supportTypeRef = new int[]{0};
        int[] wingOptionRef = new int[]{0};
        int[] wingSideRef = new int[]{0};
        boolean[] confirmRef = new boolean[]{false};
        boolean[] cancelRef = new boolean[]{false};

        TunnelingDialog dialog = new TunnelingDialog();
        dialog.setReferences(directionRef, tunnelSideRef, supportTypeRef, wingOptionRef, wingSideRef, confirmRef, cancelRef);
        gui.add(dialog, UI.scale(200, 200));

        // Wait for user input
        NUtils.addTask(new NTask() {
            @Override
            public boolean check() {
                return confirmRef[0] || cancelRef[0];
            }
        });

        dialog.destroy();

        if (cancelRef[0]) {
            return Results.SUCCESS();
        }

        // Get configuration
        Direction direction = TunnelingDialog.getDirection(directionRef[0]);
        TunnelSide tunnelSide = TunnelingDialog.getTunnelSide(directionRef[0], tunnelSideRef[0]);
        SupportType supportType = TunnelingDialog.getSupportType(supportTypeRef[0]);
        WingOption wingOption = TunnelingDialog.getWingOption(directionRef[0], wingOptionRef[0]);
        TunnelSide wingSide = TunnelingDialog.getWingSide(directionRef[0], wingSideRef[0]);

        gui.msg("Tunneling: " + direction.name + ", Side: " + tunnelSide.name +
                ", Support: " + supportType.menuName + ", Wings: " + wingOption.name + ", Wing Side: " + wingSide.name);

        // Find nearest support of ANY type as starting point
        Gob startSupport = findNearestSupport(gui);
        if (startSupport == null) {
            return Results.ERROR("No mine support found nearby. Stand near a support to start.");
        }

        gui.msg("Starting from support at: " + startSupport.rc);

        // Main tunneling loop
        Coord2d currentSupportPos = startSupport.rc;
        int iteration = 0;

        while (true) {
            iteration++;
            gui.msg("Tunneling iteration " + iteration);

            // Calculate next support position (on the support axis, not tunnel axis)
            int placementDistance = supportType.radius;
            Coord2d dirVector = new Coord2d(direction.dx, direction.dy);
            Coord2d nextSupportPos = currentSupportPos.add(dirVector.mul(placementDistance));

            // Snap to tile grid for proper alignment
            Coord nextSupportTile = nextSupportPos.div(tilesz).floor();
            nextSupportPos = tileCenter(nextSupportTile);

            // Calculate tunnel position (offset from support axis)
            // The tunnel runs parallel to the support line, but offset by tunnelSide
            Coord2d tunnelOffset = new Coord2d(tunnelSide.dx * TILE_SIZE, tunnelSide.dy * TILE_SIZE);

            // Mine the main tunnel path (offset from support line)
            Results mineResult = mineTunnelPath(gui, currentSupportPos, nextSupportPos, direction, tunnelOffset);
            if (!mineResult.IsSuccess()) {
                return mineResult;
            }

            // Mine the tile where support will be placed
            // This is on the support axis, not the tunnel axis
            Coord nextSupportTileCoord = nextSupportPos.div(tilesz).floor();
            Results supportTileResult = mineTileIfNeeded(gui, nextSupportTileCoord);
            if (!supportTileResult.IsSuccess()) {
                return supportTileResult;
            }

            handleBumlings(gui);

            // Place the new support
            Results buildResult = placeSupport(gui, nextSupportPos, supportType);
            if (!buildResult.IsSuccess()) {
                return buildResult;
            }

            // Handle any bumlings (dropped stones)
            handleBumlings(gui);

            // Dig wings based on wing option
            if (wingOption != WingOption.NONE) {
                // First, mine connector from tunnel to wing start area
                Results connectorResult = mineWingConnector(gui, nextSupportPos, direction, tunnelSide, wingSide);
                if (!connectorResult.IsSuccess()) {
                    gui.msg("Wing connector mining had issues, skipping wings");
                } else {
                    Results wingResult = mineWings(gui, nextSupportPos, direction, wingOption, wingSide, supportType.radius);
                    if (!wingResult.IsSuccess()) {
                        gui.msg("Wing mining had issues, continuing anyway");
                        // Continue anyway, wings are optional
                    }
                }
            }

            // Update current support position for next iteration
            currentSupportPos = nextSupportPos;

            // Check for stop conditions
            if (!canContinueTunneling(gui, currentSupportPos, direction, supportType.radius)) {
                gui.msg("Cannot continue tunneling - obstacle or map edge detected");
                break;
            }
        }

        return Results.SUCCESS();
    }

    private Coord2d tileCenter(Coord tile) {
        return new Coord2d(tile.x * tilesz.x + tilesz.x / 2, tile.y * tilesz.y + tilesz.y / 2);
    }

    private Gob findNearestSupport(NGameUI gui) throws InterruptedException {
        ArrayList<Gob> supports = Finder.findGobs(ALL_SUPPORTS);
        if (supports.isEmpty()) {
            return null;
        }

        Gob player = NUtils.player();
        if (player == null) {
            return null;
        }

        Gob nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Gob support : supports) {
            double dist = support.rc.dist(player.rc);
            if (dist < minDist) {
                minDist = dist;
                nearest = support;
            }
        }

        return nearest;
    }

    /**
     * Mine the tunnel path from current support to next support position.
     * The tunnel is offset from the support axis by tunnelOffset.
     * IMPORTANT: Must mine tiles in order from current position outward,
     * because you can only mine tiles adjacent to already-open areas.
     */
    private Results mineTunnelPath(NGameUI gui, Coord2d fromSupport, Coord2d toSupport,
                                    Direction direction, Coord2d tunnelOffset)
            throws InterruptedException {

        // Calculate tunnel start and end positions (offset from support positions)
        Coord2d tunnelStart = fromSupport.add(tunnelOffset);
        Coord2d tunnelEnd = toSupport.add(tunnelOffset);

        Coord fromTile = tunnelStart.div(tilesz).floor();
        Coord toTile = tunnelEnd.div(tilesz).floor();

        ArrayList<Coord> tilesToMine = new ArrayList<>();

        // Build list of tiles along the tunnel path IN ORDER from start to end
        // This is critical - we must mine from the open area outward
        if (direction.isVertical()) {
            // North/South: tunnel runs along Y axis at offset X
            int x = fromTile.x;
            if (direction == Direction.NORTH) {
                // Mining north: start from current position, go toward lower Y values
                for (int y = fromTile.y; y >= toTile.y; y--) {
                    tilesToMine.add(new Coord(x, y));
                }
            } else {
                // Mining south: start from current position, go toward higher Y values
                for (int y = fromTile.y; y <= toTile.y; y++) {
                    tilesToMine.add(new Coord(x, y));
                }
            }
        } else {
            // East/West: tunnel runs along X axis at offset Y
            int y = fromTile.y;
            if (direction == Direction.WEST) {
                // Mining west: start from current position, go toward lower X values
                for (int x = fromTile.x; x >= toTile.x; x--) {
                    tilesToMine.add(new Coord(x, y));
                }
            } else {
                // Mining east: start from current position, go toward higher X values
                for (int x = fromTile.x; x <= toTile.x; x++) {
                    tilesToMine.add(new Coord(x, y));
                }
            }
        }

        // Mine each tile in order (from open area outward)
        for (Coord tile : tilesToMine) {
            Results result = mineTileIfNeeded(gui, tile);
            if (!result.IsSuccess()) {
                return result;
            }
        }

        return Results.SUCCESS();
    }

    private Results mineTileIfNeeded(NGameUI gui, Coord tilePos) throws InterruptedException {
        // Check if tile needs mining
        if (!needsMining(gui, tilePos)) {
            return Results.SUCCESS(); // Already open, skip
        }

        // Check for dangerous conditions
        if (!isSafeToMine(gui, tilePos)) {
            return Results.ERROR("Unsafe to mine - check support health or loose rocks");
        }

        // Convert tile to world coordinates (center of tile)
        Coord2d worldPos = tileCenter(tilePos);

        // Navigate close to the tile
        PathFinder pf = new PathFinder(NGob.getDummy(worldPos, 0,
                new NHitBox(new Coord2d(-5.5, -5.5), new Coord2d(5.5, 5.5))), true);
        pf.isHardMode = true;
        pf.run(gui);

        // Restore resources if needed
        new RestoreResources().run(gui);

        // Mine the tile
        Resource resBefore = gui.ui.sess.glob.map.tilesetr(gui.ui.sess.glob.map.gettile(tilePos));

        while (needsMining(gui, tilePos)) {
            // Clear any stones that may have fallen during previous mining
            handleBumlings(gui);

            NUtils.mine(worldPos);
            gui.map.wdgmsg("sel", tilePos, tilePos, 0);

            // Wait for tile to change
            if (NUtils.getStamina() > 0.4) {
                Coord finalTilePos = tilePos;
                Resource finalResBefore = resBefore;
                NUtils.addTask(new NTask() {
                    @Override
                    public boolean check() {
                        Resource current = gui.ui.sess.glob.map.tilesetr(
                                gui.ui.sess.glob.map.gettile(finalTilePos));
                        return current != finalResBefore;
                    }
                });
            }

            // Update resource reference
            resBefore = gui.ui.sess.glob.map.tilesetr(gui.ui.sess.glob.map.gettile(tilePos));

            // Check stamina/energy
            if (!new RestoreResources().run(gui).IsSuccess()) {
                return Results.ERROR("Cannot restore resources");
            }
        }

        NUtils.getDefaultCur();
        return Results.SUCCESS();
    }

    private boolean needsMining(NGameUI gui, Coord tilePos) {
        Resource res = gui.ui.sess.glob.map.tilesetr(gui.ui.sess.glob.map.gettile(tilePos));
        if (res == null) {
            return false;
        }
        return NParser.checkName(res.name, MINEABLE_TILES);
    }

    private boolean isSafeToMine(NGameUI gui, Coord tilePos) throws InterruptedException {
        Coord2d worldPos = tileCenter(tilePos);

        // Check for loose rocks
        Gob looserock = Finder.findGob(new NAlias("looserock"));
        if (looserock != null && looserock.rc.dist(worldPos) < 93.5) {
            return false;
        }

        // Check support health
        ArrayList<Gob> supports = Finder.findGobs(ALL_SUPPORTS);
        for (Gob support : supports) {
            double dist = support.rc.dist(worldPos);
            int supportRadius = getSupportRadius(support);

            if (dist <= supportRadius) {
                GobHealth health = support.getattr(GobHealth.class);
                if (health != null && health.hp <= 0.25) {
                    return false;
                }
            }
        }

        return true;
    }

    private int getSupportRadius(Gob support) {
        if (support.ngob == null || support.ngob.name == null) {
            return 100;
        }

        String name = support.ngob.name;
        if (name.contains("minebeam")) return 150;
        if (name.contains("column")) return 125;
        if (name.contains("naturalminesupport")) return 92;
        return 100; // Default for minesupport, ladder, towercap
    }

    private Results placeSupport(NGameUI gui, Coord2d pos, SupportType supportType)
            throws InterruptedException {
        // Check if there's already a support at this location
        Gob existingSupport = Finder.findGob(pos);
        if (existingSupport != null && NParser.isIt(existingSupport, ALL_SUPPORTS)) {
            gui.msg("Support already exists at this location, skipping placement");
            return Results.SUCCESS();
        }

        // Navigate to position
        PathFinder pf = new PathFinder(NGob.getDummy(pos, 0,
                new NHitBox(new Coord2d(-5.5, -5.5), new Coord2d(5.5, 5.5))), true);
        pf.isHardMode = true;
        pf.run(gui);

        // Activate build menu for support type
        boolean menuActivated = false;
        for (MenuGrid.Pagina pag : gui.menu.paginae) {
            if (pag.button() != null && pag.button().name().equals(supportType.menuName)) {
                pag.button().use(new MenuGrid.Interaction(1, 0));
                menuActivated = true;
                break;
            }
        }

        if (!menuActivated) {
            return Results.ERROR("Cannot find " + supportType.menuName + " in build menu");
        }

        // Wait for placement object
        NUtils.addTask(new WaitPlob());

        // Place the support
        gui.map.wdgmsg("place", pos.floor(posres), 0, 1, 0);

        // Wait for construction window
        NUtils.addTask(new WaitConstructionObject(pos));
        NUtils.addTask(new WaitWindow(supportType.menuName));

        // Build the support
        Window window = gui.getWindow(supportType.menuName);
        if (window == null) {
            return Results.ERROR("Construction window did not open");
        }

        NUtils.startBuild(window);

        // Wait for build to complete
        NUtils.addTask(new NTask() {
            int count = 0;

            @Override
            public boolean check() {
                return gui.prog != null || count++ > 100;
            }
        });

        WaitBuildState wbs = new WaitBuildState();
        NUtils.addTask(wbs);

        if (wbs.getState() == WaitBuildState.State.TIMEFORDRINK) {
            if (!new Drink(0.9, false).run(gui).IsSuccess()) {
                return Results.ERROR("Cannot drink");
            }
        } else if (wbs.getState() == WaitBuildState.State.DANGER) {
            return Results.ERROR("Low energy");
        }

        // Wait for window to close (construction complete)
        String windowName = supportType.menuName;
        NUtils.addTask(new NTask() {
            int count = 0;
            @Override
            public boolean check() {
                return gui.getWindow(windowName) == null || count++ > 100;
            }
        });

        // Verify the support was actually built
        Coord2d targetPos = pos;
        final Gob[] builtSupport = {null};
        NUtils.addTask(new NTask() {
            int count = 0;
            @Override
            public boolean check() {
                ArrayList<Gob> nearbySupports = Finder.findGobs(ALL_SUPPORTS);
                for (Gob support : nearbySupports) {
                    if (support.rc.dist(targetPos) < 15) {
                        builtSupport[0] = support;
                        return true;
                    }
                }
                return count++ > 50; // Timeout after checking
            }
        });

        if (builtSupport[0] == null) {
            return Results.ERROR("Failed to build support - check if you have required resources");
        }

        gui.msg("Support placed successfully");
        return Results.SUCCESS();
    }

    private void handleBumlings(NGameUI gui) throws InterruptedException {
        Gob bumling = Finder.findGob(new NAlias("bumlings"));

        if (bumling != null && bumling.rc.dist(NUtils.player().rc) <= 20) {
            new PathFinder(bumling).run(gui);

            int maxAttempts = 10;
            int attempts = 0;

            while (bumling != null && Finder.findGob(bumling.id) != null && attempts < maxAttempts) {
                attempts++;

                NUtils.drop(NUtils.getGameUI().vhand);

                new SelectFlowerAction("Chip stone", bumling).run(gui);

                WaitChipperState wcs = new WaitChipperState(bumling, true);
                NUtils.getUI().core.addTask(wcs);

                switch (wcs.getState()) {
                    case BUMLINGNOTFOUND:
                        bumling = null;
                        break;
                    case BUMLINGFORDRINK:
                        new RestoreResources().run(gui);
                        bumling = Finder.findGob(bumling.id);
                        break;
                    case DANGER:
                        gui.msg("Warning: Low energy while chipping stones");
                        return;
                }

                // Re-check if bumling still exists
                if (bumling != null) {
                    bumling = Finder.findGob(bumling.id);
                }
            }
        }
    }

    /**
     * Mine a connector path from the tunnel to the wing start position.
     * If wingSide is in the same direction as travel, we extend the tunnel one more tile,
     * then mine from there to the wing start.
     */
    private Results mineWingConnector(NGameUI gui, Coord2d supportPos, Direction direction,
                                       TunnelSide tunnelSide, TunnelSide wingSide)
            throws InterruptedException {

        Coord supportTile = supportPos.div(tilesz).floor();

        // Calculate the tunnel end tile (at support position, offset by tunnelSide)
        Coord tunnelEndTile = new Coord(supportTile.x + tunnelSide.dx, supportTile.y + tunnelSide.dy);

        // First, navigate back to the tunnel area (which is definitely open)
        Coord2d tunnelEndPos = tileCenter(tunnelEndTile);
        PathFinder pf = new PathFinder(NGob.getDummy(tunnelEndPos, 0,
                new NHitBox(new Coord2d(-5.5, -5.5), new Coord2d(5.5, 5.5))), true);
        pf.isHardMode = true;
        pf.run(gui);

        // Extend tunnel one more tile in the main direction
        // This opens up access to the wing start area
        Coord extendedTunnelTile = new Coord(tunnelEndTile.x + direction.dx, tunnelEndTile.y + direction.dy);
        Results extendResult = mineTileIfNeeded(gui, extendedTunnelTile);
        if (!extendResult.IsSuccess()) {
            return extendResult;
        }

        // Now mine from extended tunnel to the wing start tile
        // Wing start is at support + wingSide offset
        Coord wingStartTile = new Coord(supportTile.x + wingSide.dx, supportTile.y + wingSide.dy);

        // Mine the tile connecting extended tunnel to wing start (perpendicular to main direction)
        // This is the tile at: extendedTunnel position, but moved toward wing start
        if (!extendedTunnelTile.equals(wingStartTile)) {
            // Mine from extended tunnel toward wing start (one tile perpendicular)
            Coord connectorTile = new Coord(extendedTunnelTile.x - tunnelSide.dx,
                                             extendedTunnelTile.y - tunnelSide.dy);
            Results connectorResult = mineTileIfNeeded(gui, connectorTile);
            if (!connectorResult.IsSuccess()) {
                return connectorResult;
            }
        }

        return Results.SUCCESS();
    }

    /**
     * Mine wings based on the wing option selected by user.
     * Wings are offset from the support position by wingSide, similar to how the main tunnel is offset.
     */
    private Results mineWings(NGameUI gui, Coord2d supportPos, Direction mainDirection,
                              WingOption wingOption, TunnelSide wingSide, int supportRadius)
            throws InterruptedException {

        if (wingOption == WingOption.NONE) {
            return Results.SUCCESS();
        }

        // Calculate wing offset (same concept as tunnel offset)
        Coord2d wingOffset = new Coord2d(wingSide.dx * TILE_SIZE, wingSide.dy * TILE_SIZE);
        Coord2d wingStartPos = supportPos.add(wingOffset);
        Coord wingStartTile = wingStartPos.div(tilesz).floor();

        // Full radius in tiles (no safety margin for wings)
        int wingTiles = supportRadius / 11;

        // Determine which directions to mine based on wing option
        boolean mineEast = false, mineWest = false, mineNorth = false, mineSouth = false;

        switch (wingOption) {
            case EAST:
                mineEast = true;
                break;
            case WEST:
                mineWest = true;
                break;
            case BOTH_EW:
                mineEast = true;
                mineWest = true;
                break;
            case NORTH:
                mineNorth = true;
                break;
            case SOUTH:
                mineSouth = true;
                break;
            case BOTH_NS:
                mineNorth = true;
                mineSouth = true;
                break;
            default:
                break;
        }

        // Mine wings in each enabled direction, starting from the offset position
        if (mineEast) {
            gui.msg("Mining wing: East");
            for (int i = 0; i <= wingTiles; i++) {
                Coord wingTile = new Coord(wingStartTile.x + i, wingStartTile.y);
                Results result = mineTileIfNeeded(gui, wingTile);
                if (!result.IsSuccess()) {
                    break; // Stop this wing but continue with others
                }
            }
        }

        if (mineWest) {
            gui.msg("Mining wing: West");
            for (int i = 0; i <= wingTiles; i++) {
                Coord wingTile = new Coord(wingStartTile.x - i, wingStartTile.y);
                Results result = mineTileIfNeeded(gui, wingTile);
                if (!result.IsSuccess()) {
                    break;
                }
            }
        }

        if (mineNorth) {
            gui.msg("Mining wing: North");
            for (int i = 0; i <= wingTiles; i++) {
                Coord wingTile = new Coord(wingStartTile.x, wingStartTile.y - i);
                Results result = mineTileIfNeeded(gui, wingTile);
                if (!result.IsSuccess()) {
                    break;
                }
            }
        }

        if (mineSouth) {
            gui.msg("Mining wing: South");
            for (int i = 0; i <= wingTiles; i++) {
                Coord wingTile = new Coord(wingStartTile.x, wingStartTile.y + i);
                Results result = mineTileIfNeeded(gui, wingTile);
                if (!result.IsSuccess()) {
                    break;
                }
            }
        }

        return Results.SUCCESS();
    }

    private boolean canContinueTunneling(NGameUI gui, Coord2d currentPos, Direction direction, int radius)
            throws InterruptedException {
        // Check if the next target position is within reasonable bounds
        int placementDistance = radius;
        Coord2d nextPos = currentPos.add(new Coord2d(direction.dx, direction.dy).mul(placementDistance));
        Coord nextTile = nextPos.div(tilesz).floor();

        // Try to get tile info - if we can't, we're probably at map edge
        try {
            gui.ui.sess.glob.map.gettile(nextTile);
        } catch (Exception e) {
            return false;
        }

        // Check for obstacles at next position
        Gob obstacle = Finder.findGob(nextTile);
        if (obstacle != null && !NParser.isIt(obstacle, ALL_SUPPORTS) &&
                !NParser.checkName(obstacle.ngob.name, new NAlias("bumlings"))) {
            gui.msg("Obstacle detected: " + obstacle.ngob.name);
            return false;
        }

        return true;
    }
}
