package nurgling.actions;

import haven.Gob;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.areas.NContext;
import nurgling.tasks.NTask;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;

import java.util.ArrayList;
import java.util.Arrays;

import static nurgling.areas.NContext.workstation_spec_map;

public class PrepareCauldron implements Action {
    
    private final Gob cauldron;
    private final NContext context;
    private final boolean needFuel;
    private final boolean needFire;
    private final boolean needWater;
    
    public PrepareCauldron(Gob cauldron, NContext context, boolean needFuel, boolean needFire, boolean needWater) {
        this.cauldron = cauldron;
        this.context = context;
        this.needFuel = needFuel;
        this.needFire = needFire;
        this.needWater = needWater;
    }
    
    public PrepareCauldron(Gob cauldron, NContext context) {
        this(cauldron, context, true, true, true);
    }
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (cauldron == null) {
            return Results.ERROR("Cauldron is null");
        }
        
        if (needFuel && (cauldron.ngob.getModelAttribute() & 1) == 0) {
            ArrayList<Gob> cauldrons = new ArrayList<>(Arrays.asList(cauldron));
            if (!new FillFuelPowOrCauldron(context, cauldrons, 1).run(gui).IsSuccess()) {
                return Results.ERROR("Failed to fill cauldron with fuel");
            }
        }
        
        if (needFire && (cauldron.ngob.getModelAttribute() & 2) == 0) {
            ArrayList<String> flighted = new ArrayList<>();
            flighted.add(cauldron.ngob.hash);
            if (!new LightGob(flighted, 2).run(gui).IsSuccess()) {
                return Results.ERROR("Failed to light cauldron");
            }
        }
        
        if (needWater && (cauldron.ngob.getModelAttribute() & 4) == 0) {
            new FillFluid(cauldron, context, workstation_spec_map.get("gfx/terobjs/cauldron"), new NAlias("water"), 4).run(gui);
            
            NUtils.addTask(new NTask() {
                @Override
                public boolean check() {
                    Gob currentCauldron = Finder.findGob(cauldron.id);
                    if (currentCauldron == null) {
                        return true;
                    }
                    return (currentCauldron.ngob.getModelAttribute() & 8) == 8;
                }
            });
        }
        
        return Results.SUCCESS();
    }
}
