package nurgling.tools;

import nurgling.*;
import nurgling.areas.*;
import nurgling.tasks.*;

public class NAreaSelector implements Runnable
{
    protected NArea.Space result;

    boolean createMode = false;

    public NAreaSelector(boolean createMode)
    {
        this.createMode = createMode;
    }

    @Override
    public void run()
    {
        if (!((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.get())
        {
            ((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.set(true);
            try
            {
                SelectArea sa;
                if(createMode)
                {
                    NUtils.getGameUI().areasWidget.hide();
                }
                NUtils.getUI().core.addTask(sa = new SelectArea());
                if (sa.getResult() != null)
                {
                    result = sa.getResult();
                }
                if(createMode)
                {
                    if(result!=null)
                    {
                        ((NMapView) NUtils.getGameUI().map).addArea(result);
                        NConfig.needAreasUpdate();
                    }
                    NUtils.getGameUI().areasWidget.show();
                }
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
