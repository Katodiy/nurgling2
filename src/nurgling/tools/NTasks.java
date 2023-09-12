package nurgling.tools;

import haven.*;
import nurgling.*;

public class NTasks
{
    public static class IsMoving extends NCore.Task{

        @Override
        public boolean check()
        {
            if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null && NUtils.getGameUI().map.player()!=null)
            {
                Drawable drawable = (Drawable) NUtils.getGameUI().map.player().getattr(Drawable.class);
                if(drawable!=null)
                {
                    String pose;
                    return drawable instanceof Composite && (pose = ((Composite) drawable).current_pose) != null && NParser.checkName(pose, "borka/walking", "borka/running");
                }
            }
            return false;
        }
    }
}
