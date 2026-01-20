package nurgling.headless;

import haven.Area;
import haven.FColor;
import haven.render.*;
import haven.render.sl.FragData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * Stub Render implementation for headless mode.
 * Accepts all rendering commands but doesn't actually render.
 * Immediately invokes callbacks with default/zeroed data.
 */
public class HeadlessRender implements Render {
    private final HeadlessEnvironment env;
    private static final String LOG_PREFIX = "[HeadlessRender] ";

    public HeadlessRender(HeadlessEnvironment env) {
        this.env = env;
    }

    @Override
    public Environment env() {
        return env;
    }

    @Override
    public void submit(Render sub) {
        // No-op: accept submission but don't process
    }

    @Override
    public void draw(Pipe pipe, Model data) {
        // No-op: nothing to draw in headless mode
    }

    @Override
    public void clear(Pipe pipe, FragData buf, FColor val) {
        // No-op
    }

    @Override
    public void clear(Pipe pipe, double val) {
        // No-op
    }

    @Override
    public <T extends DataBuffer> void update(T buf, DataBuffer.PartFiller<? super T> data, int from, int to) {
        // No-op
    }

    @Override
    public <T extends DataBuffer> void update(T buf, DataBuffer.Filler<? super T> data) {
        // No-op
    }

    /**
     * Pixel get operation - immediately invokes callback with zeroed buffer.
     * This is critical for hit-testing to complete without blocking.
     */
    @Override
    public void pget(Pipe pipe, FragData buf, Area area, VectorFormat fmt, ByteBuffer dstbuf, Consumer<ByteBuffer> callback) {
        // Clear the destination buffer to zeros
        dstbuf.clear();
        for (int i = 0; i < dstbuf.capacity(); i++) {
            dstbuf.put(i, (byte) 0);
        }
        dstbuf.position(0);
        dstbuf.limit(dstbuf.capacity());

        // Immediately invoke callback - this allows hit-testing to complete
        // Zeroed data means "nothing found" which triggers nohit() in Maptest/Hittest
        if (callback != null) {
            callback.accept(dstbuf);
        }
    }

    /**
     * Pixel get from texture image - immediately invokes callback with zeroed buffer.
     */
    @Override
    public void pget(Texture.Image img, VectorFormat fmt, ByteBuffer dstbuf, Consumer<ByteBuffer> callback) {
        // Clear the destination buffer to zeros
        dstbuf.clear();
        for (int i = 0; i < dstbuf.capacity(); i++) {
            dstbuf.put(i, (byte) 0);
        }
        dstbuf.position(0);
        dstbuf.limit(dstbuf.capacity());

        // Immediately invoke callback
        if (callback != null) {
            callback.accept(dstbuf);
        }
    }

    @Override
    public void timestamp(Consumer<Long> callback) {
        // Immediately invoke callback with current time
        if (callback != null) {
            callback.accept(System.nanoTime());
        }
    }

    @Override
    public void fence(Runnable callback) {
        // Immediately invoke callback
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void dispose() {
        // No-op
    }
}
