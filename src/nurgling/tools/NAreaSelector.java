package nurgling.tools;

import nurgling.*;
import nurgling.areas.*;
import nurgling.tasks.*;

public class NAreaSelector implements Runnable
{
    protected NArea.Space result;

    public enum Mode
    {
        CREATE,
        CHANGE,
        SELECT
    }

    Mode mode = Mode.CREATE;

    public NAreaSelector(Mode mode)
    {
        this.mode = mode;
    }

    public static void changeArea(NArea area)
    {
        new Thread(new NAreaSelector(area,Mode.CHANGE)).start();
    }

    NArea area = null;
    private NAreaSelector(NArea area, Mode mode)
    {
        this.area = area;
        this.mode = mode;
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
                if(mode!=Mode.SELECT)
                {
                    NUtils.getGameUI().areas.createMode = true;
                    NUtils.getGameUI().areas.hide();
                    NUtils.getGameUI().areas.createMode = false;
                }
                NUtils.getUI().core.addTask(sa = new SelectArea());
                if (sa.getResult() != null)
                {
                    result = sa.getResult();
                }
                if(mode!=Mode.SELECT)
                {
                    if(result!=null)
                    {
                        if(mode == Mode.CREATE)
                        {
                            ((NMapView) NUtils.getGameUI().map).addArea(result);
                        }
                        else if(mode == Mode.CHANGE)
                        {
                            area.space = result;
                            area.grids_id.clear();
                            area.grids_id.addAll(area.space.space.keySet());
                            for(NArea.VArea space: area.space.space.values())
                                space.isVis = false;
                            ((NMapView) NUtils.getGameUI().map).createAreaLabel(area.id);
                            area.inWork = false;
                        }
                        NConfig.needAreasUpdate();
                    }
                    NUtils.getGameUI().areas.show();
                }
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
