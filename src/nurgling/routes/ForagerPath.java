package nurgling.routes;

import haven.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ForagerPath {
    
    private static final double SECTION_LENGTH = 50.0;
    
    public String name;
    public List<ForagerWaypoint> waypoints;
    public List<ForagerSection> sections;
    
    public ForagerPath(String name) {
        this.name = name;
        this.waypoints = new ArrayList<>();
        this.sections = new ArrayList<>();
    }
    
    public void addWaypoint(ForagerWaypoint wp) {
        waypoints.add(wp);
    }
    
    public void removeLastWaypoint() {
        if (!waypoints.isEmpty()) {
            waypoints.remove(waypoints.size() - 1);
        }
    }
    
    public void generateSections() {
        sections.clear();
        
        if (waypoints.size() < 2) {
            return;
        }
        
        MCache mcache = nurgling.NUtils.getGameUI().map.glob.map;
        int sectionIndex = 0;
        Coord2d currentStart = waypoints.get(0).toCoord2d(mcache);
        if(currentStart == null) return;
        
        for (int i = 1; i < waypoints.size(); i++) {
            Coord2d nextPoint = waypoints.get(i).toCoord2d(mcache);
            if(nextPoint == null) continue;
            double distance = currentStart.dist(nextPoint);
            
            if (distance <= SECTION_LENGTH) {
                // Points are close, create one section
                sections.add(new ForagerSection(currentStart, nextPoint, sectionIndex++));
                currentStart = nextPoint;
            } else {
                // Points are far, create intermediate sections
                int numSections = (int) Math.ceil(distance / SECTION_LENGTH);
                double stepX = (nextPoint.x - currentStart.x) / numSections;
                double stepY = (nextPoint.y - currentStart.y) / numSections;
                
                for (int j = 0; j < numSections; j++) {
                    Coord2d sectionStart = new Coord2d(
                        currentStart.x + stepX * j,
                        currentStart.y + stepY * j
                    );
                    Coord2d sectionEnd = new Coord2d(
                        currentStart.x + stepX * (j + 1),
                        currentStart.y + stepY * (j + 1)
                    );
                    sections.add(new ForagerSection(sectionStart, sectionEnd, sectionIndex++));
                }
                currentStart = nextPoint;
            }
        }
    }
    
    public ForagerSection getSection(int index) {
        if (index >= 0 && index < sections.size()) {
            return sections.get(index);
        }
        return null;
    }
    
    public int getSectionCount() {
        return sections.size();
    }
    
    public void save(String directory) throws IOException {
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        Path filePath = dirPath.resolve(name + ".json");
        JSONObject json = toJson();
        Files.write(filePath, json.toString(2).getBytes());
    }
    
    public static ForagerPath load(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String content = new String(Files.readAllBytes(path));
        JSONObject json = new JSONObject(content);
        return new ForagerPath(json);
    }
    
    public ForagerPath(JSONObject json) {
        this.name = json.getString("name");
        this.waypoints = new ArrayList<>();
        this.sections = new ArrayList<>();
        
        // Load waypoints (grid-based)
        if (json.has("waypoints")) {
            JSONArray waypointsArray = json.getJSONArray("waypoints");
            for (int i = 0; i < waypointsArray.length(); i++) {
                JSONObject wpJson = waypointsArray.getJSONObject(i);
                waypoints.add(new ForagerWaypoint(wpJson));
            }
        }
        
        // Always generate sections from waypoints (don't load from JSON)
        // Sections use world coordinates which are session-specific
        generateSections();
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        
        // Save only waypoints (grid-based, persistent between sessions)
        JSONArray waypointsArray = new JSONArray();
        for (ForagerWaypoint wp : waypoints) {
            waypointsArray.put(wp.toJson());
        }
        json.put("waypoints", waypointsArray);
        
        // Don't save sections - they will be regenerated from waypoints
        // because they use world coordinates which are session-specific
        
        return json;
    }
    
    @Override
    public String toString() {
        return String.format("ForagerPath[%s: %d waypoints, %d sections]", 
            name, waypoints.size(), sections.size());
    }
}
