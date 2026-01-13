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
                    int selectedAreaId = -1;
                    if(result!=null)
                    {
                        if(mode == Mode.CREATE)
                        {
                            ((NMapView) NUtils.getGameUI().map).addArea(result);
                            // Find the newly created area (highest id)
                            for(NArea a : ((NMapView) NUtils.getGameUI().map).glob.map.areas.values()) {
                                if(a.id > selectedAreaId) {
                                    selectedAreaId = a.id;
                                }
                            }
                        }
                        else if(mode == Mode.CHANGE)
                        {
                            area.space = result;
                            area.lastLocalChange = System.currentTimeMillis();
                            area.grids_id.clear();
                            area.grids_id.addAll(area.space.space.keySet());
                            for(NArea.VArea space: area.space.space.values())
                                space.isVis = false;
                            ((NMapView) NUtils.getGameUI().map).createAreaLabel(area.id);
                            area.inWork = false;
                            selectedAreaId = area.id;
                        }
                        NConfig.needAreasUpdate();
                    }
                    NUtils.getGameUI().areas.show();
                    if(selectedAreaId >= 0) {
                        NUtils.getGameUI().areas.selectAreaById(selectedAreaId);
                    }
                }
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
