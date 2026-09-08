package nurgling.contextmenu;

import haven.Gob;

import java.util.ArrayList;
import java.util.List;

public class GobContextRegistry {
    private static final List<GobContextAction> actions = new ArrayList<>();

    public static void register(GobContextAction action) {
        actions.add(action);
    }

    public static List<GobContextAction> getActionsFor(Gob gob) {
        if (gob == null || gob.ngob == null || gob.ngob.name == null)
            return List.of();
        List<GobContextAction> result = new ArrayList<>();
        for (GobContextAction action : actions) {
            if (action.appliesTo(gob))
                result.add(action);
        }
        return result;
    }

    static {
        register(new FillEmptyContainersAction());
        register(new ChopAndRemoveStumpAction());
        register(new RemoveStumpAction());
        register(new FillTroughWithSwillAction());
        register(new FillBarrelsFromVehicleAction());
        register(new EmptyBarrelsIntoCisternAction());
        register(new LoadVehicleAction());
        register(new UnloadVehicleAction());
        register(new SaveTreeLocationAction());
        register(new SaveBushLocationAction());
        register(new CutDownAreaAction());
        register(new ChipStoneAreaAction());
        register(new ShearWoolAreaAction());
        register(new LightAction());
        register(new KilnFuelAction());
        register(new HTableTimesAction());
        // Registered last so the generic entry sits below the object-specific ones.
        register(new ConfigureGobAction());
    }
}
