package nurgling.headless;

import haven.render.*;

/**
 * Stub Environment implementation for headless mode.
 * Provides minimal rendering infrastructure that allows hit-testing
 * code to execute without crashing.
 *
 * Key design decisions:
 * - render() returns HeadlessRender which immediately invokes pget callbacks
 * - drawlist() returns HeadlessDrawList which accepts but ignores draw commands
 * - fillbuf() returns HeadlessFillBuffer for buffer operations
 * - compatible() returns true for headless objects, false for real GL objects
 */
public class HeadlessEnvironment implements Environment {
    private static final String LOG_PREFIX = "[HeadlessEnv] ";
    private final HeadlessCaps caps = new HeadlessCaps();

    public HeadlessEnvironment() {
        log("HeadlessEnvironment initialized");
    }

    @Override
    public Render render() {
        return new HeadlessRender(this);
    }

    @Override
    public FillBuffer fillbuf(DataBuffer target, int from, int to) {
        return new HeadlessFillBuffer(this, to - from);
    }

    @Override
    public DrawList drawlist() {
        return new HeadlessDrawList();
    }

    @Override
    public void submit(Render cmd) {
        // No-op: render commands are processed immediately in HeadlessRender
        // The callbacks in pget() are already fired, so nothing more to do
    }

    @Override
    public boolean compatible(DrawList ob) {
        return ob instanceof HeadlessDrawList;
    }

    @Override
    public boolean compatible(Texture ob) {
        // Return false for real textures - this causes Clicklist to recreate
        // its draw list with our headless version
        return false;
    }

    @Override
    public boolean compatible(DataBuffer ob) {
        return false;
    }

    @Override
    public Caps caps() {
        return caps;
    }

    @Override
    public void dispose() {
        log("HeadlessEnvironment disposed");
    }

    private static void log(String message) {
        System.out.println(LOG_PREFIX + message);
    }

    /**
     * Stub Caps implementation for headless mode.
     */
    private static class HeadlessCaps implements Caps {
        @Override
        public String vendor() {
            return "Headless";
        }

        @Override
        public String driver() {
            return "HeadlessDriver";
        }

        @Override
        public String device() {
            return "HeadlessDevice";
        }
    }
}
