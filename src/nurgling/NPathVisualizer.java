package nurgling;

import haven.*;
import haven.render.*;
import nurgling.NGob;
import nurgling.routes.RouteGraph;
import nurgling.routes.RoutePoint;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Визуализатор путей для объектов в игре.
 * Отображает пути перемещения различных категорий объектов (игрок, друзья, враги и т.д.) 
 * с разными цветами и стилями.
 */
public class NPathVisualizer implements RenderTree.Node {
    // Категории путей, отображаемые по умолчанию
    public static final HashSet<PathCategory> DEF_CATEGORIES = new HashSet<>(Arrays.asList(PathCategory.ME, PathCategory.FOE));
    // Формат вершин для рендеринга линий
    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));

    public NPathQueue path; // Очередь путей для отображения
    public List<Pair<Coord3f, Coord3f>> pflines = null; // Линии пути для PathFinder
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1); // Слоты рендеринга
    private final Set<Moving> moves = new HashSet<>(); // Набор перемещающихся объектов
    private final Map<PathCategory, MovingPath> paths = new HashMap<>(); // Категории путей

    public NPathVisualizer() {
        // Инициализация путей для всех категорий
        for (PathCategory cat : PathCategory.values()) {
            paths.put(cat, new MovingPath(cat.state));
        }
    }

    @Override
    public void added(RenderTree.Slot slot) {
        // Добавление слота рендеринга
        synchronized (slots) {slots.add(slot);}
        for (MovingPath path : paths.values()) {
            slot.add(path);
        }
    }

    @Override
    public void removed(RenderTree.Slot slot) {
        // Удаление слота рендеринга
        synchronized (slots) {slots.remove(slot);}
    }

    /**
     * Обновление визуализации путей на основе текущего состояния
     */
    private void update() {
        Set<Moving> tmoves;

        synchronized (moves) {
            tmoves = new HashSet<>(moves);
        }

        Map<PathCategory, List<Pair<Coord3f, Coord3f>>> categorized = new HashMap<>();

        for (Moving m : tmoves) {
            PathCategory category = categorize(m);
            if(!categorized.containsKey(category)) {
                categorized.put(category, new LinkedList<>());
            }
            try {
                categorized.get(category).add(new Pair<>(
                        m.getc(), // Текущая позиция
                        m.gett()  // Целевая позиция
                ));
            }catch (Loading ignore)
            {
//                e.printStackTrace();
            }


        }

        // Получаем выбранные пользователем категории для отображения
        Set<PathCategory> selected = NConfig.getPathCategories();
        if( path != null) {
            // Добавляем пути из очереди
            List<Pair<Coord3f, Coord3f>> lines = path.lines();
            categorized.put(PathCategory.QUEUED, lines);
            // Автоматически добавляем категории, если есть активный путь
            if(!selected.contains(PathCategory.ME) && lines.size() > 1) {
                selected.add(PathCategory.ME);
            }
            if(selected.contains(PathCategory.ME)) {selected.add(PathCategory.QUEUED);}
        }

        // Обновляем отображение для каждой категории
        for (PathCategory cat : PathCategory.values()) {
            List<Pair<Coord3f, Coord3f>> lines = categorized.get(cat);
            MovingPath path = paths.get(cat);
            if(!selected.contains(cat) || lines == null || lines.isEmpty()) {
                if(path != null) {
                    path.update(null); // Скрываем путь, если не выбран или пуст
                }
            } else {
                path.update(lines); // Обновляем путь
            }
        }

        // Особый случай для путей PathFinder
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().routesWidget.visible){
            HashSet<Integer> added = new HashSet<>();
            if(NUtils.getGameUI().map != null) {
                RouteGraph graph = ((NMapView)NUtils.getGameUI().map).routeGraphManager.getGraph();
                ArrayList<Pair<Coord3f, Coord3f>> gpf = new ArrayList<>();
                for(RoutePoint point : graph.getPoints())
                {
                    if(NUtils.getGameUI().map.glob.map.findGrid(point.gridId)!=null)
                    {
                        Coord3f one3f = point.toCoord3f(NUtils.getGameUI().map.glob.map);
                        for(Integer nei:point.getNeighbors())
                        {
                            Integer hash = (new Pair<>(point.hashCode(),nei.hashCode())).hashCode();
                            if(!added.contains(hash))
                            {
                                if(NUtils.getGameUI().map.glob.map.findGrid(point.gridId)!=null) {
                                    if(graph.getPoint(nei) != null) {
                                        Coord3f another3f = graph.getPoint(nei).toCoord3f(NUtils.getGameUI().map.glob.map);
                                        if(one3f!=null && another3f!=null)
                                        {
                                            gpf.add(new Pair<>(another3f,one3f));
                                            added.add(hash);
                                            added.add((new Pair<>(nei.hashCode(), point.hashCode())).hashCode());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                MovingPath path = paths.get(PathCategory.GPF);
                path.update(gpf);
            }
        }
    }

    /**
     * Определение категории пути для объекта
     */
    private PathCategory categorize(Moving m) {
        Gob gob =  m.gob;
        if(gob.id==NUtils.playerID()) {
            return PathCategory.ME; // Путь игрока
        } else {
            return PathCategory.OTHER; // Другие объекты
        }
    }

    // Высота отрисовки линий над поверхностью
    private static final float Z = 1f;

    /**
     * Конвертация координат пути в массив float для рендеринга
     */
    private static float[] convert(List<Pair<Coord3f, Coord3f>> lines) {
        float[] ret = new float[lines.size() * 6];
        int i = 0;
        for (Pair<Coord3f, Coord3f> line : lines) {
            // Координаты начала линии
            ret[i++] = line.a.x;
            ret[i++] = -line.a.y; // Инвертируем Y для корректного отображения
            // Учитываем настройку плоской поверхности
            if(!(Boolean) NConfig.get(NConfig.Key.flatsurface))
                ret[i++] = line.a.z + Z;
            else
                ret[i++] = Z;
            // Координаты конца линии
            ret[i++] = line.b.x;
            ret[i++] = -line.b.y;
            if(!(Boolean) NConfig.get(NConfig.Key.flatsurface))
                ret[i++] = line.b.z + Z;
            else
                ret[i++] = Z;
        }
        return ret;
    }

    /**
     * Добавление пути для отображения
     */
    public void addPath(Moving moving) {
        if(moving == null) {return;}
        synchronized (moves) { moves.add(moving); }
    }

    /**
     * Удаление пути из отображения
     */
    public void removePath(Moving moving) {
        if(moving == null) {return;}
        synchronized (moves) { moves.remove(moving); }
    }

    /**
     * Обновление состояния визуализатора
     */
    public void tick(double dt) {
        update();
    }

    /**
     * Внутренний класс для отображения одного пути
     */
    private static class MovingPath implements RenderTree.Node, Rendered {
        private final Pipe.Op state; // Состояние рендеринга (цвет, толщина линии и т.д.)
        public final Collection<RenderTree.Slot> slots = new ArrayList<>(1); // Слоты рендеринга
        private Model model; // Модель для отображения

        public MovingPath(Pipe.Op state) {
            this.state = state;
        }

        @Override
        public void added(RenderTree.Slot slot) {
            slot.ostate(state);
            synchronized (slots) {slots.add(slot);}
        }

        @Override
        public void removed(RenderTree.Slot slot) {
            synchronized (slots) {slots.remove(slot);}
        }

        @Override
        public void draw(Pipe context, Render out) {
            if(model != null) {
                out.draw(context, model);
            }
        }

        /**
         * Обновление данных пути
         */
        public void update(List<Pair<Coord3f, Coord3f>> lines) {
            if(lines == null || lines.isEmpty()) {
                model = null; // Нет данных - скрываем модель
            } else {
                // Конвертируем координаты и создаем буфер вершин
                float[] data = convert(lines);
                VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
                VertexArray va = new VertexArray(LAYOUT, vbo);

                // Создаем модель линий
                model = new Model(Model.Mode.LINES, va, null);
            }

            // Обновляем все слоты рендеринга
            Collection<RenderTree.Slot> tslots;
            synchronized (slots) { tslots = new ArrayList<>(slots); }
            try {
                tslots.forEach(RenderTree.Slot::update);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Категории путей с настройками отображения
     */
    public enum PathCategory {
        ME(new Color(118, 254, 196, 255), true),          // Путь игрока
        QUEUED(new Color(112, 204, 164, 255), true),      // Путь в очереди
        FRIEND(new Color(109, 211, 251, 255)),           // Путь друга
        FOE(new Color(255, 134, 154, 255), true),         // Путь врага
        AGGRESSIVE_ANIMAL(new Color(255, 179, 122, 255), true), // Путь агрессивного животного
        PF(new Color(220, 255, 64, 255), true),          // Путь PathFinder
        GPF(new Color(255, 137, 43, 255), true),          // Путь PathFinder
        OTHER(new Color(187, 187, 187, 255));            // Прочие пути

        private final Pipe.Op state; // Состояние рендеринга
        public final Color color;    // Цвет отображения

        PathCategory(Color col, boolean top) {
            // Настройки рендеринга: цвет, толщина линии, отображение поверх других объектов
            state = Pipe.Op.compose(
                    new BaseColor(col),
                    new States.LineWidth(1.5f),
                    top ? Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth) : null
            );
            color = col;
        }

        PathCategory(Color col) {
            this(col, false);
        }
    }
}