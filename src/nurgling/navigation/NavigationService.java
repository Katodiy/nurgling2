package nurgling.navigation;

import haven.*;
import nurgling.*;
import nurgling.actions.Results;
import nurgling.actions.bots.RoutePointNavigator;
import nurgling.areas.NArea;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;

import java.util.List;

/**
 * Singleton service that abstracts navigation system selection.
 * Tries ChunkNav first, falls back to Routes system.
 *
 * This service provides a clean API for navigation without requiring
 * callers to know which navigation system is being used.
 */
public class NavigationService {

    private static final NavigationService INSTANCE = new NavigationService();

    private NavigationService() {
        // Private constructor for singleton
    }

    public static NavigationService getInstance() {
        return INSTANCE;
    }

    /**
     * Navigate to an area using the best available navigation system.
     * Tries ChunkNav first, falls back to Routes.
     *
     * @param area The target area to navigate to
     * @param gui The game UI
     * @return Results.SUCCESS() if navigation succeeded, Results.FAIL() otherwise
     */
    public Results navigateToArea(NArea area, NGameUI gui) throws InterruptedException {
        if (area == null || gui == null) {
            return Results.FAIL();
        }

        // Try ChunkNav first
        Results chunkNavResult = tryChunkNav(area, gui);
        if (chunkNavResult.IsSuccess()) {
            return chunkNavResult;
        }

        // Fallback to Routes
        Results routesResult = tryRoutes(area, gui);
        if (routesResult.IsSuccess()) {
            return routesResult;
        }

        // Neither system could navigate
        System.out.println("NavigationService: No navigation data available for area " + area.name);
        return Results.FAIL();
    }

    /**
     * Navigate to an area only if navigation is needed (area not visible or too far).
     *
     * @param area The target area to navigate to
     * @param gui The game UI
     * @return Results.SUCCESS() if no navigation needed or navigation succeeded
     */
    public Results navigateToAreaIfNeeded(NArea area, NGameUI gui) throws InterruptedException {
        if (area == null || gui == null) {
            return Results.FAIL();
        }

        // Check if we need to navigate (area not visible or too far)
        boolean needsNavigation = !area.isVisible() || area.getCenter2d() == null ||
                                  area.getCenter2d().dist(NUtils.player().rc) > 450;

        if (!needsNavigation) {
            return Results.SUCCESS();
        }

        return navigateToArea(area, gui);
    }

    /**
     * Check if navigation is available to an area using any system.
     *
     * @param area The target area
     * @return true if navigation is possible via ChunkNav or Routes
     */
    public boolean isNavigationAvailable(NArea area) {
        if (area == null) {
            return false;
        }

        NGameUI gui = NUtils.getGameUI();
        if (gui == null || gui.map == null) {
            return false;
        }

        // Check ChunkNav
        ChunkNavManager chunkNav = getChunkNavManager(gui);
        if (chunkNav != null && chunkNav.isInitialized()) {
            ChunkPath path = chunkNav.planToArea(area);
            if (path != null) {
                return true;
            }
        }

        // Check Routes
        RouteGraph graph = getRouteGraph(gui);
        if (graph != null) {
            RoutePoint routePoint = graph.findAreaRoutePoint(area);
            if (routePoint != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculate distance to an area using the best available navigation system.
     *
     * @param area The target area
     * @param gui The game UI
     * @return Distance estimate, or Double.MAX_VALUE if unreachable
     */
    public double getDistanceToArea(NArea area, NGameUI gui) {
        if (area == null || gui == null || gui.map == null) {
            return Double.MAX_VALUE;
        }

        // Try ChunkNav first
        ChunkNavManager chunkNav = getChunkNavManager(gui);
        if (chunkNav != null && chunkNav.isInitialized()) {
            ChunkPath path = chunkNav.planToArea(area);
            if (path != null) {
                return path.totalCost;
            }
        }

        // Fallback to Routes
        RouteGraph graph = getRouteGraph(gui);
        if (graph != null) {
            List<RoutePoint> routePoints = graph.findPath(
                graph.findNearestPointToPlayer(gui),
                graph.findAreaRoutePoint(area)
            );
            if (routePoints != null) {
                // Scale route points to be comparable with ChunkNav costs
                return routePoints.size() * 100.0;
            }
        }

        return Double.MAX_VALUE;
    }

    /**
     * Try navigation using ChunkNav system.
     */
    private Results tryChunkNav(NArea area, NGameUI gui) throws InterruptedException {
        ChunkNavManager chunkNav = getChunkNavManager(gui);
        if (chunkNav == null || !chunkNav.isInitialized()) {
            return Results.FAIL();
        }

        ChunkPath path = chunkNav.planToArea(area);
        if (path == null) {
            return Results.FAIL();
        }

        System.out.println("NavigationService: Using ChunkNav to navigate to " + area.name);
        Results result = chunkNav.navigateToArea(area, gui);

        if (result.IsSuccess()) {
            System.out.println("NavigationService: ChunkNav navigation succeeded");
        } else {
            System.out.println("NavigationService: ChunkNav navigation failed, will try Routes fallback");
        }

        return result;
    }

    /**
     * Try navigation using Routes system.
     */
    private Results tryRoutes(NArea area, NGameUI gui) throws InterruptedException {
        RouteGraph graph = getRouteGraph(gui);
        if (graph == null) {
            return Results.FAIL();
        }

        RoutePoint routePoint = graph.findAreaRoutePoint(area);
        if (routePoint == null) {
            return Results.FAIL();
        }

        System.out.println("NavigationService: Using Routes to navigate to " + area.name);
        new RoutePointNavigator(routePoint, area.id).run(gui);
        System.out.println("NavigationService: Routes navigation completed");

        return Results.SUCCESS();
    }

    /**
     * Get the ChunkNavManager from the GUI.
     */
    private ChunkNavManager getChunkNavManager(NGameUI gui) {
        if (gui == null || gui.map == null || !(gui.map instanceof NMapView)) {
            return null;
        }
        return ((NMapView) gui.map).getChunkNavManager();
    }

    /**
     * Get the RouteGraph from the GUI.
     */
    private RouteGraph getRouteGraph(NGameUI gui) {
        if (gui == null || gui.map == null || !(gui.map instanceof NMapView)) {
            return null;
        }
        return ((NMapView) gui.map).routeGraphManager.getGraph();
    }
}
