/* Preprocessed source code */
package haven.res.lib.globfx;

import haven.*;
import haven.render.*;
import haven.render.RenderTree.Slot;
import java.util.*;
import java.lang.reflect.*;
import java.lang.ref.*;

@haven.FromResource(name = "lib/globfx", version = 12)
public interface Effect extends RenderTree.Node {
    public boolean tick(float dt);
    public void gtick(Render out);
}
