package nurgling.db.service;

import nurgling.db.DatabaseManager;
import nurgling.db.dao.RouteDao;
import nurgling.routes.Route;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for Route business logic
 */
public class RouteService {
    private final DatabaseManager databaseManager;
    private final RouteDao routeDao;
    
    // Sync related fields
    private ScheduledExecutorService syncScheduler;
    private volatile boolean syncEnabled = false;
    private RouteSyncCallback syncCallback;

    public interface RouteSyncCallback {
        void onRoutesUpdated(List<Route> updatedRoutes);
        void onRouteDeleted(int routeId);
        void onFullSync(Map<Integer, Route> allRoutes);
    }

    public RouteService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.routeDao = new RouteDao();
    }

    /**
     * Load all routes for a profile
     */
    public Map<Integer, Route> loadRoutes(String profile) throws SQLException {
        Map<Integer, Route> routes = new HashMap<>();
        List<RouteDao.RouteData> routeDataList = databaseManager.executeOperation(
            adapter -> routeDao.loadRoutesByProfile(adapter, profile)
        );
        
        for (RouteDao.RouteData data : routeDataList) {
            Route route = convertToRoute(data);
            if (route != null) {
                routes.put(route.id, route);
            }
        }
        return routes;
    }

    /**
     * Load routes asynchronously
     */
    public CompletableFuture<Map<Integer, Route>> loadRoutesAsync(String profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadRoutes(profile);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load routes", e);
            }
        });
    }

    /**
     * Export routes to database (batch operation)
     */
    public int exportRoutesToDatabase(Map<Integer, Route> routes, String profile) throws SQLException {
        List<Route> routesCopy = new ArrayList<>(routes.values());
        
        return databaseManager.executeOperation(adapter -> {
            int count = 0;
            for (Route route : routesCopy) {
                JSONObject json = route.toJson();
                
                int newVersion = routeDao.saveRoute(adapter,
                    route.id,
                    route.name,
                    route.path,
                    json.toString(),
                    profile);
                route.version = newVersion;
                count++;
            }
            return count;
        });
    }

    /**
     * Export routes asynchronously
     */
    public CompletableFuture<Integer> exportRoutesToDatabaseAsync(Map<Integer, Route> routes, String profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return exportRoutesToDatabase(routes, profile);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to export routes", e);
            }
        });
    }

    /**
     * Save a single route to database
     */
    public void saveRoute(Route route, String profile) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            JSONObject json = route.toJson();
            int newVersion = routeDao.saveRoute(adapter,
                route.id,
                route.name,
                route.path,
                json.toString(),
                profile);
            route.version = newVersion;
            return null;
        });
    }

    /**
     * Save a single route asynchronously
     */
    public CompletableFuture<Void> saveRouteAsync(Route route, String profile) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveRoute(route, profile);
            } catch (SQLException e) {
                System.err.println("Failed to save route to database: " + e.getMessage());
            }
        });
    }

    /**
     * Delete a route
     */
    public void deleteRoute(int routeId, String profile) throws SQLException {
        databaseManager.executeOperation(adapter -> {
            routeDao.deleteRoute(adapter, routeId, profile);
            return null;
        });
    }

    /**
     * Delete a route asynchronously
     */
    public CompletableFuture<Void> deleteRouteAsync(int routeId, String profile) {
        return CompletableFuture.runAsync(() -> {
            try {
                deleteRoute(routeId, profile);
            } catch (SQLException e) {
                System.err.println("Failed to delete route from database: " + e.getMessage());
            }
        });
    }

    /**
     * Start periodic sync
     */
    public void startSync(String profile, int intervalSeconds, RouteSyncCallback callback) {
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            return;
        }
        
        this.syncCallback = callback;
        this.syncEnabled = true;
        this.syncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Route-Sync-Worker");
            t.setDaemon(true);  // Daemon thread won't prevent JVM shutdown
            return t;
        });
        
        syncScheduler.scheduleAtFixedRate(() -> {
            if (!syncEnabled) return;
            
            if (!databaseManager.isReady()) {
                return;
            }

            String currentProfile = getCurrentProfile();
            if (currentProfile == null || currentProfile.isEmpty()) {
                return;
            }

            try {
                Map<Integer, Integer> localVersions = getLocalRouteVersions();
                List<Route> updates = checkForUpdatesWithVersions(currentProfile, localVersions);
                if (!updates.isEmpty() && syncCallback != null) {
                    syncCallback.onRoutesUpdated(updates);
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg != null && !msg.contains("no such table") && !msg.contains("no such column")) {
                    System.err.println("Route sync error: " + msg);
                }
            }
        }, 5, intervalSeconds, TimeUnit.SECONDS);
        
        System.out.println("Route sync started with interval: " + intervalSeconds + " seconds");
    }

    /**
     * Stop sync
     */
    public void stopSync() {
        syncEnabled = false;
        if (syncScheduler != null) {
            syncScheduler.shutdown();
            syncScheduler = null;
        }
    }

    /**
     * Check for updates using version comparison
     */
    public List<Route> checkForUpdatesWithVersions(String profile, Map<Integer, Integer> localVersions) throws SQLException {
        List<Route> updatedRoutes = new ArrayList<>();

        if (!databaseManager.isReady()) {
            return updatedRoutes;
        }

        Map<Integer, Integer> dbVersions = databaseManager.executeOperation(
            adapter -> routeDao.getAllRouteVersions(adapter, profile)
        );

        List<Integer> routesToLoad = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : dbVersions.entrySet()) {
            int routeId = entry.getKey();
            int dbVersion = entry.getValue();
            int localVersion = localVersions.getOrDefault(routeId, 0);

            if (dbVersion > localVersion) {
                routesToLoad.add(routeId);
            }
        }

        // Check for deleted routes
        List<Integer> routesToDelete = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : localVersions.entrySet()) {
            int routeId = entry.getKey();
            if (!dbVersions.containsKey(routeId)) {
                routesToDelete.add(routeId);
            }
        }
        if (!routesToDelete.isEmpty() && syncCallback != null) {
            for (int routeId : routesToDelete) {
                syncCallback.onRouteDeleted(routeId);
            }
        }

        if (!routesToLoad.isEmpty()) {
            System.out.println("Route sync: loading " + routesToLoad.size() + " routes from DB");
            for (int routeId : routesToLoad) {
                RouteDao.RouteData data = databaseManager.executeOperation(
                    adapter -> routeDao.loadRoute(adapter, routeId, profile)
                );
                if (data != null) {
                    Route route = convertToRoute(data);
                    if (route != null) {
                        updatedRoutes.add(route);
                    }
                }
            }
        }

        return updatedRoutes;
    }

    /**
     * Convert RouteData to Route
     */
    private Route convertToRoute(RouteDao.RouteData data) {
        try {
            JSONObject json = new JSONObject(data.getData());
            // Add id, name, path to json if not present
            json.put("id", data.getId());
            json.put("name", data.getName());
            json.put("path", data.getPath());
            
            Route route = new Route(json);
            route.version = data.getVersion();
            return route;
        } catch (Exception e) {
            System.err.println("Failed to convert route data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get versions of routes from local cache
     */
    private Map<Integer, Integer> getLocalRouteVersions() {
        Map<Integer, Integer> versions = new HashMap<>();
        try {
            if (nurgling.NUtils.getGameUI() != null && 
                nurgling.NUtils.getGameUI().map != null) {
                
                nurgling.NMapView map = (nurgling.NMapView) nurgling.NUtils.getGameUI().map;
                Map<Integer, Route> routes = map.getRouteGraphManager().getRoutes();
                if (routes != null) {
                    for (Map.Entry<Integer, Route> entry : routes.entrySet()) {
                        versions.put(entry.getKey(), entry.getValue().version);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return versions;
    }

    /**
     * Get current profile dynamically
     */
    private String getCurrentProfile() {
        try {
            if (nurgling.NUtils.getGameUI() != null) {
                String genus = nurgling.NUtils.getGameUI().getGenus();
                if (genus != null && !genus.isEmpty()) {
                    return genus;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}




