package nurgling.headless;

import haven.Locked;
import haven.render.*;

import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stub DrawList implementation for headless mode.
 * Provides minimal RenderList functionality without actual rendering.
 */
public class HeadlessDrawList implements DrawList {
    private final ReentrantLock lock = new ReentrantLock();
    private String description = "headless-drawlist";

    @Override
    public void draw(Render out) {
        // No-op: nothing to draw in headless mode
    }

    @Override
    public String stats() {
        return "headless";
    }

    @Override
    public DrawList desc(Object desc) {
        this.description = String.valueOf(desc);
        return this;
    }

    @Override
    public void add(Slot<? extends Rendered> slot) {
        // No-op: don't track slots in headless mode
    }

    @Override
    public void remove(Slot<? extends Rendered> slot) {
        // No-op
    }

    @Override
    public void update(Slot<? extends Rendered> slot) {
        // No-op
    }

    @Override
    public void update(Pipe group, int[] statemask) {
        // No-op
    }

    @Override
    public void dispose() {
        // No-op
    }
}
