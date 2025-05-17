package nurgling.tools;

import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.widgets.NMiniMap;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class FogArea {
    final NMiniMap miniMap;
    private final List<Rectangle> rectangles = new ArrayList<>();
    private Coord lastUL, lastBR;

    Rectangle newRect = null;


    public FogArea(NMiniMap miniMap) {
        this.miniMap = miniMap; // вызов основного конструктора
        if(new File(NConfig.current.path_fog).exists()) {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_fog), StandardCharsets.UTF_8)) {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            } catch (IOException ignore) {
            }

            if (!contentBuilder.toString().isEmpty()) {

                JSONObject json = new JSONObject(contentBuilder.toString());
                if (json.has("rectangles")) {
                    JSONArray rectanglesArray = json.getJSONArray("rectangles");
                    for (int i = 0; i < rectanglesArray.length(); i++) {
                        JSONObject rectJson = rectanglesArray.getJSONObject(i);

                        // Получаем данные из JSON
                        JSONObject jul = rectJson.getJSONObject("ul");
                        JSONObject jbr = rectJson.getJSONObject("br");
                        long seg_id = rectJson.getLong("seg");

                        // Восстанавливаем координаты
                        long ul_grid_id = jul.getLong("grid_id");
                        long br_grid_id = jbr.getLong("grid_id");
                        Coord cul = new Coord(jul.getInt("x"), jul.getInt("y"));
                        Coord cbr = new Coord(jbr.getInt("x"), jbr.getInt("y"));

                        // Создаем временный прямоугольник
                        Rectangle rect = new Rectangle(cul, cbr, ul_grid_id,br_grid_id,seg_id);


                        // Добавляем в список
                        rectangles.add(rect);
                    }

                }
            }
        }
    }

    public class Rectangle {
        public Coord ul, br;
        public long ul_id, br_id;
        public long seg_id;
        HashSet<Long> history = new HashSet<>();
        boolean loading = true;

        Coord cul;
        Coord cbr;

        public Rectangle(Coord ul, Coord br, long seg_id) {
            this.seg_id = seg_id;
            this.ul = ul;
            this.br = br;
            trySetGridId();
        }

        public Rectangle(Coord cul, Coord cbr, long ul_id, long br_id, long seg_id) {
            this.seg_id = seg_id;
            this.cul = cul;
            this.cbr = cbr;
            this.ul_id = ul_id;
            this.br_id = br_id;

            history = new HashSet<>();
            history.add(ul_id);
            history.add(br_id);

        }

        private void trySetGridId() {
            if(ul!=null && br!=null && miniMap.getSegmentById(seg_id)!=null) {
                Coord ul_gc = ul.div(MCache.cmaps);
                Coord br_gc = br.div(MCache.cmaps);

                ul_id = miniMap.getSegmentById(seg_id).map.getOrDefault(ul_gc, -1L);
                br_id = miniMap.getSegmentById(seg_id).map.getOrDefault(br_gc, -1L);

                cul = ul.sub(ul_gc.mul(MCache.cmaps));
                cbr = br.sub(br_gc.mul(MCache.cmaps));

                if (ul_id == -1 || br_id == -1) {
                   return;
                }
                loading = false;
            }
        }

        JSONObject toJson()
        {
            JSONObject res = new JSONObject();
            JSONObject jul = new JSONObject();
            if(cul != null && cbr != null) {
                jul.put("x", cul.x);
                jul.put("y", cul.y);
                jul.put("grid_id", ul_id);
                res.put("ul", jul);
                JSONObject jbr = new JSONObject();
                jbr.put("x", cbr.x);
                jbr.put("y", cbr.y);
                jbr.put("grid_id", br_id);
                res.put("br", jbr);
                res.put("seg", seg_id);
                return res;
            }
            return null;
        }

        public int width()  { return br.x - ul.x; }
        public int height() { return br.y - ul.y; }

        // Проверка пересечения с другим прямоугольником
        public boolean overlaps(Rectangle other) {
            if(!sameGrid(other)) return false;
            return !(br.x <= other.ul.x || ul.x >= other.br.x ||
                    br.y <= other.ul.y || ul.y >= other.br.y);
        }

        public boolean sameGrid(Rectangle other) {
            if( !loading && !other.loading)
            {
                return seg_id == other.seg_id;
            }
            return false;
        }
        
        public void tick(double dt) {
            if (loading) {
                if (ul == null || br == null) {
                    // 1. Получаем сегмент карты
                    MapFile.Segment seg = miniMap.file.segments.get(seg_id);
                    if (seg == null) {
                        throw new IllegalArgumentException("Сегмент " + seg_id + " не найден");
                    }

                    Coord ul_gc = findGridCoord(seg, ul_id);
                    Coord br_gc = findGridCoord(seg, br_id);

                    this.ul = ul_gc.mul(MCache.cmaps).add(cul);
                    this.br = br_gc.mul(MCache.cmaps).add(cbr);

                    if (this.ul == null || this.br == null) {
                        throw new IllegalArgumentException("Координаты вне миникарты");
                    }
                    loading = false;
                }
                else if (ul_id == -1 || br_id == -1) {
                    trySetGridId();
                }
            }
        }


        private Coord findGridCoord(MapFile.Segment seg, long gridId) {
            for (Map.Entry<Coord, Long> entry : seg.map.entrySet()) {
                if (entry.getValue() == gridId) {
                    return entry.getKey();
                }
            }
            throw new IllegalArgumentException("Грид " + gridId + " не найден в сегменте " + seg.id);
        }

        // Разделяет текущий прямоугольник на части, не перекрывающиеся с `other`
        public List<Rectangle> subtract(Rectangle other) {
            List<Rectangle> result = new ArrayList<>();
            if (!overlaps(other)) {
                result.add(this);
                return result;
            }

            // Возможные оставшиеся области после вычитания
            // 1. Слева от other
            if (ul.x < other.ul.x) {
                result.add(new Rectangle(ul, new Coord(other.ul.x, br.y), other.seg_id));
            }
            // 2. Справа от other
            if (br.x > other.br.x) {
                result.add(new Rectangle(new Coord(other.br.x, ul.y), br, other.seg_id));
            }
            // 3. Сверху от other (между left/right)
            int midLeft = Math.max(ul.x, other.ul.x);
            int midRight = Math.min(br.x, other.br.x);
            if (ul.y < other.ul.y && midLeft < midRight) {
                result.add(new Rectangle(
                        new Coord(midLeft, ul.y),
                        new Coord(midRight, other.ul.y), other.seg_id));
            }
            // 4. Снизу от other (между left/right)
            if (br.y > other.br.y && midLeft < midRight) {
                result.add(new Rectangle(
                        new Coord(midLeft, other.br.y),
                        new Coord(midRight, br.y), other.seg_id));
            }

            return result;
        }
    }

    public void tick(double dt)
    {
        if(NUtils.getGameUI()!=null) {
            if (newRect != null && newRect.loading) {
                newRect.tick(dt);
            }
            for(Rectangle rect: rectangles)
            {
                if(rect.loading)
                {
                    rect.tick(dt);
                }
            }
        }
        updateNew();
    }

    /**
     * Добавляет новый прямоугольник, исключая его пересечения с существующими.
     */
    public void addWithoutOverlaps(Coord ul, Coord br, long id) {

        if (ul.equals(lastUL) && br.equals(lastBR))
            return; // Координаты не изменились

        lastUL = ul;
        lastBR = br;

        newRect = new Rectangle(ul, br, id);
    }

    void updateNew()
    {
        if(newRect!=null && !newRect.loading) {
            for(Rectangle rect : rectangles)
            {
                if((rect.history.contains(newRect.br_id) || rect.history.contains(newRect.ul_id) ) && rect.loading)
                    return;
            }

            List<Rectangle> nonOverlappingParts = new ArrayList<>();
            nonOverlappingParts.add(newRect);

            // Вычитаем все существующие прямоугольники из нового
            for (Rectangle existing : rectangles) {
                if (!existing.loading && newRect.sameGrid(existing)) {
                    List<Rectangle> temp = new ArrayList<>();
                    for (Rectangle part : nonOverlappingParts) {
                        if(!part.loading) {
                            temp.addAll(part.subtract(existing));
                        }
                    }
                    nonOverlappingParts = temp;
                    if (nonOverlappingParts.isEmpty()) break;
                }
            }

            // Добавляем оставшиеся части
            rectangles.addAll(nonOverlappingParts);
            mergeRectangles();
            newRect = null;
        }
    }

    /**
     * Объединяет прямоугольники, если они:
     * 1. Соприкасаются или пересекаются,
     * 2. Имеют одинаковую длину грани по оси соприкосновения.
     */
    private void mergeRectangles() {
        boolean merged;
        do {
            merged = false;
            outer:
            for (int i = 0; i < rectangles.size(); i++) {
                Rectangle a = rectangles.get(i);
                if(a.loading) continue;
                for (int j = i + 1; j < rectangles.size(); j++) {
                    Rectangle b = rectangles.get(j);
                    if(b.loading || !a.sameGrid(b)) continue;
                    Optional<Rectangle> mergedRect = tryMerge(a, b);
                    if (mergedRect.isPresent()) {
                        // Удаляем сначала больший индекс, потом меньший
                        ArrayList<Rectangle> forDelete = new ArrayList<>();
                        forDelete.add(a);
                        forDelete.add(b);
                        mergedRect.get().history.addAll(a.history);
                        mergedRect.get().history.addAll(b.history);
                        rectangles.add(mergedRect.get());
                        rectangles.removeAll(forDelete);
                        merged = true;
                        break outer;
                    }
                }
            }
        } while (merged);
        NConfig.needFogUpdate();
    }


    /**
     * Пытается объединить два прямоугольника, если:
     * 1. Они соприкасаются или пересекаются,
     * 2. Длина грани соприкосновения одинакова.
     * Возвращает Optional<Rectangle>, если объединение возможно.
     */
    private Optional<Rectangle> tryMerge(Rectangle a, Rectangle b) {
        if(!a.sameGrid(b)) return Optional.empty();
        // Проверяем, что прямоугольники соприкасаются или пересекаются
        boolean intersectsOrTouches =
                a.br.x >= b.ul.x && a.ul.x <= b.br.x &&
                        a.br.y >= b.ul.y && a.ul.y <= b.br.y;

        if (!intersectsOrTouches)
            return Optional.empty();

        // Проверяем, можно ли объединить по горизонтали (левая/правая грань)
        if (a.ul.y == b.ul.y && a.height() == b.height() && (a.br.x == b.ul.x || a.ul.x == b.br.x)) {
            Coord newUL = new Coord(Math.min(a.ul.x, b.ul.x), a.ul.y);
            Coord newBR = new Coord(Math.max(a.br.x, b.br.x), a.br.y);
            return Optional.of(new Rectangle(newUL, newBR, a.seg_id));
        }

        // Проверяем, можно ли объединить по вертикали (верхняя/нижняя грань)
        if (a.ul.x == b.ul.x && a.width() == b.width() && (a.br.y == b.ul.y || a.ul.y == b.br.y)) {
            Coord newUL = new Coord(a.ul.x, Math.min(a.ul.y, b.ul.y));
            Coord newBR = new Coord(a.br.x, Math.max(a.br.y, b.br.y));
            return Optional.of(new Rectangle(newUL, newBR, a.seg_id));
        }

        return Optional.empty();
    }

    public List<Rectangle> getCoveredAreas() {
        return rectangles;
    }

    public void clear() {
        rectangles.clear();
        lastUL = null;
        lastBR = null;
    }

    public JSONObject toJson()
    {
        JSONArray result = new JSONArray();
        for(Rectangle rectangle: rectangles)
        {
            JSONObject jrect = rectangle.toJson();
            if(jrect!=null) {
                result.put(jrect);
            }
        }
        JSONObject doc = new JSONObject();
        doc.put("rectangles",result);
        return doc;
    }
}