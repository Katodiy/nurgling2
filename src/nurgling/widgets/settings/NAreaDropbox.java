package nurgling.widgets.settings;

import haven.*;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.widgets.NDropbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Виджет выбора зоны из списка доступных зон
 */
public class NAreaDropbox extends NDropbox<NAreaDropbox.AreaEntry> {
    
    public final List<AreaEntry> areas = new ArrayList<>();
    private OnAreaSelected listener;
    
    public static class AreaEntry {
        public final int id;
        public final String name;
        
        public AreaEntry(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public interface OnAreaSelected {
        void onAreaSelected(int areaId);
    }
    
    public NAreaDropbox(int w) {
        super(w, 10, UI.scale(16));
        reloadAreas();
    }
    
    public void setListener(OnAreaSelected listener) {
        this.listener = listener;
    }
    
    public void reloadAreas() {
        areas.clear();
        // Add "No area binding" option
        areas.add(new AreaEntry(-1, "No zone binding"));
        
        if (NUtils.getGameUI() != null && NUtils.getGameUI().map != null) {
            NMapView map = (NMapView) NUtils.getGameUI().map;
            if (map.glob != null && map.glob.map != null && map.glob.map.areas != null) {
                for (NArea area : map.glob.map.areas.values()) {
                    areas.add(new AreaEntry(area.id, area.name));
                }
                // Sort by name
                areas.subList(1, areas.size()).sort(Comparator.comparing(a -> a.name));
            }
        }
        
        if (sel == null && !areas.isEmpty()) {
            sel = areas.get(0);
        }
    }
    
    public void setSelectedAreaId(int areaId) {
        for (AreaEntry entry : areas) {
            if (entry.id == areaId) {
                sel = entry;
                return;
            }
        }
        // If not found, select "No binding"
        if (!areas.isEmpty()) {
            sel = areas.get(0);
        }
    }
    
    public int getSelectedAreaId() {
        return sel != null ? sel.id : -1;
    }
    
    @Override
    protected AreaEntry listitem(int i) {
        return areas.get(i);
    }
    
    @Override
    protected int listitems() {
        return areas.size();
    }
    
    @Override
    protected void drawitem(GOut g, AreaEntry item, int idx) {
        g.text(item.name, Coord.z);
    }
    
    @Override
    public void change(AreaEntry item) {
        super.change(item);
        if (listener != null && item != null) {
            listener.onAreaSelected(item.id);
        }
    }
}

