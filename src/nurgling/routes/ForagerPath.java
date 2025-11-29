package nurgling.routes;

import haven.Coord2d;
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
    public List<Coord2d> waypoints;
    public List<ForagerSection> sections;
    
    public ForagerPath(String name) {
        this.name = name;
        this.waypoints = new ArrayList<>();
        this.sections = new ArrayList<>();
    }
    
    public void addWaypoint(Coord2d point) {
        waypoints.add(point);
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
        
        int sectionIndex = 0;
        Coord2d currentStart = waypoints.get(0);
        
        for (int i = 1; i < waypoints.size(); i++) {
            Coord2d nextPoint = waypoints.get(i);
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
        
        // Load waypoints
        if (json.has("waypoints")) {
            JSONArray waypointsArray = json.getJSONArray("waypoints");
            for (int i = 0; i < waypointsArray.length(); i++) {
                JSONObject pointJson = waypointsArray.getJSONObject(i);
                waypoints.add(new Coord2d(pointJson.getDouble("x"), pointJson.getDouble("y")));
            }
        }
        
        // Load sections
        if (json.has("sections")) {
            JSONArray sectionsArray = json.getJSONArray("sections");
            for (int i = 0; i < sectionsArray.length(); i++) {
                sections.add(new ForagerSection(sectionsArray.getJSONObject(i)));
            }
        } else {
            // If no sections saved, generate them from waypoints
            generateSections();
        }
    }
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        
        // Save waypoints
        JSONArray waypointsArray = new JSONArray();
        for (Coord2d point : waypoints) {
            JSONObject pointJson = new JSONObject();
            pointJson.put("x", point.x);
            pointJson.put("y", point.y);
            waypointsArray.put(pointJson);
        }
        json.put("waypoints", waypointsArray);
        
        // Save sections
        JSONArray sectionsArray = new JSONArray();
        for (ForagerSection section : sections) {
            sectionsArray.put(section.toJson());
        }
        json.put("sections", sectionsArray);
        
        return json;
    }
    
    @Override
    public String toString() {
        return String.format("ForagerPath[%s: %d waypoints, %d sections]", 
            name, waypoints.size(), sections.size());
    }
}
