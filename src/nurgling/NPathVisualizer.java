package nurgling;

import haven.*;
import haven.render.*;


import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Visualizes movement paths for different game objects with configurable colors and styles.
 * Supports categorization of paths (player, enemies, friends etc.) and dynamic updates.
 */
public class NPathVisualizer implements RenderTree.Node
{
    /**
     * Default path categories to display
     */
    public static final HashSet<PathCategory> DEF_CATEGORIES = new HashSet<>(Arrays.asList(PathCategory.ME, PathCategory.FOE));

    /**
     * Vertex layout for path line rendering
     */
    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(
            new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));

    /**
     * Z-coordinate offset for path lines above ground
     */
    private static final float Z = 1f;

    /**
     * Current path queue to visualize
     */
    public NPathQueue path;

    /**
     * PathFinder debug lines
     */
    public List<Pair<Coord3f, Coord3f>> pflines = null;

    /**
     * Collection of rendering slots
     */
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

    /**
     * Set of currently tracked moving objects
     */
    private final Set<Moving> moves = new HashSet<>();

    /**
     * Map of path categories to their visual representations
     */
    private final Map<PathCategory, MovingPath> paths = new HashMap<>();

    /**
     * Creates new path visualizer and initializes paths for all categories.
     */
    public NPathVisualizer()
    {
        for (PathCategory cat : PathCategory.values())
        {
            paths.put(cat, new MovingPath(cat.state));
        }
    }

    @Override
    public void added(RenderTree.Slot slot)
    {
        synchronized (slots)
        {
            slots.add(slot);
        }
        for (MovingPath path : paths.values())
        {
            slot.add(path);
        }
    }

    @Override
    public void removed(RenderTree.Slot slot)
    {
        synchronized (slots)
        {
            slots.remove(slot);
        }
    }

    /**
     * Updates all path visualizations based on current game state.
     * Processes moving objects, path queues and route graphs.
     */
    public void update()
    {
        final Set<PathCategory> pathCategories = NConfig.getPathCategories();
        if (pathCategories.isEmpty()) return;

        final EnumMap<PathCategory, List<Pair<Coord3f, Coord3f>>> categorized = new EnumMap<>(PathCategory.class);
        for (PathCategory c : PathCategory.values())
        {
            categorized.put(c, new ArrayList<>());
        }

        synchronized (moves)
        {
            for (Moving m : moves)
            {
                try
                {
                    categorized.get(categorize(m)).add(new Pair<>(m.getc(), m.gett()));
                } catch (Loading ignored)
                {
                }
            }
        }

        if (path != null)
        {
            List<Pair<Coord3f, Coord3f>> queued = path.lines();
            if (!queued.isEmpty())
            {
                categorized.get(PathCategory.QUEUED).addAll(queued);
                if (!pathCategories.contains(PathCategory.ME))
                {
                    pathCategories.add(PathCategory.ME);
                }
            }
        }

        for (PathCategory cat : PathCategory.values())
        {
            List<Pair<Coord3f, Coord3f>> lines = categorized.get(cat);
            MovingPath mp = paths.get(cat);
            if (!pathCategories.contains(cat) || lines == null || lines.isEmpty())
            {
                mp.update(null);
            } else
            {
                mp.update(lines);
            }
        }
    }

    /**
     * Cache for MCache.Grid objects to optimize grid lookups.
     * Reduces repeated calls to map.findGrid() for the same grid ID.
     */
    static class Cache {
        /** Last accessed grid ID */
        long gid;

        /** Cached grid instance */
        MCache.Grid grid;

        /** Reference to the game map */
        MCache map;

        /**
         * Creates new cache instance for specified map.
         * @param map Game map to use for grid lookups
         */
        public Cache(MCache map) {
            this.map = map;
        }

        /**
         * Gets grid by ID, using cached value when possible.
         * @param id Grid ID to look up
         * @return Found grid instance or null if not found
         */
        MCache.Grid get(long id) {
            // Return cached grid if ID matches
            if (gid == id && grid != null) {
                return grid;
            }

            // Update cache with new grid
            gid = id;
            grid = map.findGrid(id);
            return grid;
        }
    }


    /**
     * Updates visualizations for graph point connections.
     */
    /**
     * Helper class for generating unique keys for point pairs.
     */
    private static final class IntPair
    {
        /**
         * Generates unique key for two point IDs.
         *
         * @param a First point ID
         * @param b Second point ID
         * @return Combined key
         */
        static int key(int a, int b)
        {
            return (a << 16) | (b & 0xFFFF);
        }
    }

    /**
     * Determines path category for a moving object.
     *
     * @param m Moving object to categorize
     * @return Appropriate path category
     */
    private PathCategory categorize(Moving m)
    {
        Gob gob = m.gob;
        if (gob.id == NUtils.playerID())
        {
            return PathCategory.ME;
        } else
        {
            return PathCategory.OTHER;
        }
    }

    /**
     * Converts path coordinates to float array for rendering.
     *
     * @param lines List of path segments
     * @return Float array with vertex data
     */
    private static float[] convert(List<Pair<Coord3f, Coord3f>> lines)
    {
        float[] ret = new float[lines.size() * 6];
        int i = 0;
        for (Pair<Coord3f, Coord3f> line : lines)
        {
            ret[i++] = line.a.x;
            ret[i++] = -line.a.y;
            if (!(Boolean) NConfig.get(NConfig.Key.flatsurface))
                ret[i++] = line.a.z + Z;
            else
                ret[i++] = Z;
            ret[i++] = line.b.x;
            ret[i++] = -line.b.y;
            if (!(Boolean) NConfig.get(NConfig.Key.flatsurface))
                ret[i++] = line.b.z + Z;
            else
                ret[i++] = Z;
        }
        return ret;
    }

    /**
     * Adds moving object to path visualization.
     *
     * @param moving Object to track
     */
    public void addPath(Moving moving)
    {
        if (moving == null)
        {
            return;
        }
        synchronized (moves)
        {
            moves.add(moving);
        }
    }

    /**
     * Removes moving object from path visualization.
     *
     * @param moving Object to stop tracking
     */
    public void removePath(Moving moving)
    {
        if (moving == null)
        {
            return;
        }
        synchronized (moves)
        {
            moves.remove(moving);
        }
    }

    /**
     * Updates visualizer state (called each game tick).
     *
     * @param dt Time since last update
     */
    public void tick(double dt)
    {
        update();
    }

    /**
     * Internal class representing a single visualized path.
     */
    private static class MovingPath implements RenderTree.Node, Rendered
    {
        /**
         * Rendering state (color, line width etc.)
         */
        private final Pipe.Op state;

        /**
         * Collection of rendering slots
         */
        public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

        /**
         * Current path model
         */
        private Model model;

        /**
         * Creates new path visualization.
         *
         * @param state Initial rendering state
         */
        public MovingPath(Pipe.Op state)
        {
            this.state = state;
        }

        @Override
        public void added(RenderTree.Slot slot)
        {
            slot.ostate(state);
            synchronized (slots)
            {
                slots.add(slot);
            }
        }

        @Override
        public void removed(RenderTree.Slot slot)
        {
            synchronized (slots)
            {
                slots.remove(slot);
            }
        }

        @Override
        public void draw(Pipe context, Render out)
        {
            if (model != null)
            {
                out.draw(context, model);
            }
        }

        /**
         * Updates path visualization with new segments.
         *
         * @param lines New path segments to display (null to clear)
         */
        public void update(List<Pair<Coord3f, Coord3f>> lines)
        {
            if (lines == null || lines.isEmpty())
            {
                model = null;
            } else
            {
                float[] data = convert(lines);
                VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4,
                        DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
                VertexArray va = new VertexArray(LAYOUT, vbo);

                model = new Model(Model.Mode.LINES, va, null);
            }

            Collection<RenderTree.Slot> tslots;
            synchronized (slots)
            {
                tslots = new ArrayList<>(slots);
            }
            try
            {
                tslots.forEach(RenderTree.Slot::update);
            } catch (Exception ignored)
            {
            }
        }
    }

    /**
     * Path categories with their visual styles.
     */
    public enum PathCategory
    {
        /**
         * Player's path
         */
        ME(new Color(118, 254, 196, 255), true),

        /**
         * Queued path
         */
        QUEUED(new Color(112, 204, 164, 255), true),

        /**
         * Friend's path
         */
        FRIEND(new Color(109, 211, 251, 255)),

        /**
         * Enemy's path
         */
        FOE(new Color(255, 134, 154, 255), true),

        /**
         * Aggressive animal's path
         */
        AGGRESSIVE_ANIMAL(new Color(255, 179, 122, 255), true),

        /**
         * PathFinder debug path
         */
        PF(new Color(220, 255, 64, 255), true),

        /**
         * Graph PathFinder path
         */
        GPF(new Color(255, 137, 43, 255), true),

        /**
         * Area connections path
         */
        GPFAREAS(new Color(31, 222, 10, 255), true),

        /**
         * Other objects' paths
         */
        OTHER(new Color(187, 187, 187, 255));

        /**
         * Rendering state for this category
         */
        private final Pipe.Op state;

        /**
         * Display color
         */
        public final Color color;

        /**
         * Creates new path category with top-layer rendering.
         *
         * @param col Display color
         * @param top Whether to render above other objects
         */
        PathCategory(Color col, boolean top)
        {
            state = Pipe.Op.compose(
                    new BaseColor(col),
                    new States.LineWidth(1.5f),
                    top ? Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth) : null
            );
            color = col;
        }

        /**
         * Creates new path category with default rendering.
         *
         * @param col Display color
         */
        PathCategory(Color col)
        {
            this(col, false);
        }
    }
}