package nurgling.headless;

import haven.render.Environment;
import haven.render.FillBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Stub FillBuffer implementation for headless mode.
 * Provides a simple in-memory buffer that doesn't actually upload to GPU.
 */
public class HeadlessFillBuffer implements FillBuffer {
    private final int size;
    private final HeadlessEnvironment env;
    private ByteBuffer buffer;

    public HeadlessFillBuffer(HeadlessEnvironment env, int size) {
        this.env = env;
        this.size = size;
        this.buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean compatible(Environment env) {
        return env instanceof HeadlessEnvironment;
    }

    @Override
    public ByteBuffer push() {
        buffer.clear();
        return buffer;
    }

    @Override
    public void pull(ByteBuffer buf) {
        buffer.clear();
        if (buf.remaining() <= size) {
            buffer.put(buf);
        }
        buffer.flip();
    }

    @Override
    public void dispose() {
        buffer = null;
    }
}
