package nurgling.overlays;

import haven.*;
import haven.render.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Draws a line from the player to a selected quest giver in the game world
 */
public class NQuestGiverLineOverlay implements RenderTree.Node, Rendered {
    /**
     * Vertex layout for line rendering
     */
    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(
            new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));

    /**
     * Z-coordinate offset for line above ground
     */
    private static final float Z = 1f;

    /**
     * Gold color for quest giver line
     */
    private static final Color LINE_COLOR = new Color(255, 215, 0, 255);

    /**
     * Rendering state (color, line width)
     */
    private final Pipe.Op state;

    /**
     * Collection of rendering slots
     */
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

    /**
     * Current line model
     */
    private Model model;

    /**
     * Target quest giver position
     */
    private Coord2d targetPos;

    /**
     * Reference to the player gob
     */
    private final Supplier<Gob> playerSupplier;

    /**
     * Creates new quest giver line overlay
     * @param playerSupplier Supplier to get the player Gob
     */
    public NQuestGiverLineOverlay(Supplier<Gob> playerSupplier) {
        this.playerSupplier = playerSupplier;
        this.state = Pipe.Op.compose(
                new BaseColor(LINE_COLOR),
                new States.LineWidth(3.0f),
                Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth)
        );
    }

    /**
     * Sets the target quest giver position
     * @param pos World position of quest giver
     */
    public void setTarget(Coord2d pos) {
        this.targetPos = pos;
        updateLine();
    }

    /**
     * Updates the line based on player and target positions
     */
    private void updateLine() {
        if(targetPos == null) {
            model = null;
            updateSlots();
            return;
        }

        try {
            Gob player = playerSupplier.get();
            if(player == null) {
                model = null;
                updateSlots();
                return;
            }

            Coord3f playerPos = player.getc();
            Coord3f targetPos3f = new Coord3f((float)targetPos.x, (float)targetPos.y, 0);

            System.out.println("Drawing 3D line - Player: " + playerPos + " Target: " + targetPos3f);

            // Get terrain height at target position if possible
            // For now, we'll use a fixed Z offset
            float[] data = new float[6];
            data[0] = playerPos.x;
            data[1] = -playerPos.y;
            data[2] = playerPos.z + Z;
            data[3] = targetPos3f.x;
            data[4] = -targetPos3f.y;
            data[5] = Z;

            VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4,
                    DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
            VertexArray va = new VertexArray(LAYOUT, vbo);

            model = new Model(Model.Mode.LINES, va, null);
            updateSlots();
        } catch(Exception e) {
            // Silently handle errors
            model = null;
            updateSlots();
        }
    }

    /**
     * Updates all rendering slots
     */
    private void updateSlots() {
        Collection<RenderTree.Slot> tslots;
        synchronized(slots) {
            tslots = new ArrayList<>(slots);
        }
        try {
            tslots.forEach(RenderTree.Slot::update);
        } catch(Exception ignored) {
        }
    }

    /**
     * Called each game tick to update the line
     */
    public void tick() {
        if(targetPos != null) {
            updateLine();
        }
    }

    @Override
    public void added(RenderTree.Slot slot) {
        slot.ostate(state);
        synchronized(slots) {
            slots.add(slot);
        }
    }

    @Override
    public void removed(RenderTree.Slot slot) {
        synchronized(slots) {
            slots.remove(slot);
        }
    }

    @Override
    public void draw(Pipe context, Render out) {
        if(model != null) {
            out.draw(context, model);
        }
    }

    /**
     * Simple supplier interface for Gob
     */
    public interface Supplier<T> {
        T get();
    }
}
