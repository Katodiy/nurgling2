package nurgling;

import haven.*;
import nurgling.tools.*;

public class NProperties
{
    public static class Crop
    {
        public long specstage;
        public long maxstage;

        private Crop(long specstage, long maxstage) {
            this.specstage = specstage;
            this.maxstage = maxstage;
        }

        private Crop() {
        }

        public static Crop getCrop(Gob gob)
        {
            String name = gob.ngob.name;
            int cropstgmaxval = 0;
            for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
                int stg = layer.id / 10;
                if (stg > cropstgmaxval) {
                    cropstgmaxval = stg;
                }
            }
            if (NParser.checkName(name, "turnip"))
                return new NProperties.Crop(1, cropstgmaxval);
            else if (NParser.checkName(name, "carrot"))
                return new NProperties.Crop(3, cropstgmaxval);
            else if (NParser.checkName(name, "hemp"))
                return new NProperties.Crop(cropstgmaxval - 1, cropstgmaxval);
            else
                return new NProperties.Crop(-1, cropstgmaxval);
        }
    }
}
