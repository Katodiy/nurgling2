package nurgling;

import haven.*;
import haven.render.*;

public class GhostAlpha extends GAttrib implements Gob.SetupMod {
    private static final Pipe.Op ghostState = Pipe.Op.compose(
        new BaseColor(new java.awt.Color(100, 150, 255, 128)),  // Blue tint with 50% alpha
        new States.Facecull(States.Facecull.Mode.NONE)
    );
    
    public GhostAlpha(Gob gob) {
        super(gob);
    }
    
    @Override
    public Pipe.Op gobstate() {
        return ghostState;
    }
}
