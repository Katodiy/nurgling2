package nurgling.gattrr;

import java.awt.Color;
import java.util.Random;

import haven.GAttrib;
import haven.Gob;
import haven.Matrix4f;
import haven.Utils;
import haven.render.*;
import haven.resutil.CrackTex;
import nurgling.overlays.*;

public class NCustomScale extends GAttrib implements Gob.SetupMod {

    public NCustomScale(Gob gob) {
        super(gob);
    }

    private float scale = 0.25f;

    @Override
    public Pipe.Op gobstate() {
        return new Location(new Matrix4f(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, scale, 0,
                0, 0, 0, 1));
    }

}
