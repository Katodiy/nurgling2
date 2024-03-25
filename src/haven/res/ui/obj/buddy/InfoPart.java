/* Preprocessed source code */
package haven.res.ui.obj.buddy;

import haven.*;
import haven.render.*;
import java.util.*;
import java.awt.Color;

@haven.FromResource(name = "ui/obj/buddy", version = 3)
public interface InfoPart {
    public void draw(CompImage cmp, RenderContext ctx);
    public default int order() {return(0);}
    public default boolean auto() {return(false);}
}

/* >objdelta: Buddy */
