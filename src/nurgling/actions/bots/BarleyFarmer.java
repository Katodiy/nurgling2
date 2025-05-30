package nurgling.actions.bots;

import nurgling.NConfig;
import nurgling.NGameUI;
import nurgling.actions.*;
import nurgling.areas.NArea;
import nurgling.routes.Route;
import nurgling.tools.NAlias;
import nurgling.widgets.Specialisation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import static nurgling.NUtils.getGameUI;


public class BarleyFarmer implements Action {

    HashMap<Integer, Route> routes = new HashMap<>();
    Route route = null;
    private Results walker;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        loadRoutes();

        for (Route route : routes.values()) {
            if (route.hasSpecialization("farm1")) {
                this.route = route;
                break;
            }
        }

        if (this.route != null) {
            getGameUI().msg("farm1 route found!");
            walker = new RouteWalker(this.route, true).run(gui);
        }

        NArea.Specialisation field = new NArea.Specialisation(Specialisation.SpecName.crop.toString(), "Barley");
        NArea.Specialisation seed = new NArea.Specialisation(Specialisation.SpecName.seed.toString(), "Barley");
        NArea.Specialisation trough = new NArea.Specialisation(Specialisation.SpecName.trough.toString());
        NArea.Specialisation swill = new NArea.Specialisation(Specialisation.SpecName.swill.toString());
        ArrayList<NArea.Specialisation> req = new ArrayList<>();
        req.add(field);
        req.add(seed);
        ArrayList<NArea.Specialisation> opt = new ArrayList<>();
        req.add(trough);
        opt.add(swill);

        if(new Validator(req, opt).run(gui).IsSuccess())
        {
            new HarvestCrop(NArea.findSpec(field),NArea.findSpec(seed),NArea.findSpec(trough),NArea.findSpec(swill),new NAlias("plants/barley"),new NAlias("barley"),3, false).run(gui);
            if(NArea.findOut("Straw", 1)!=null)
                new CollectItemsToPile(NArea.findSpec(field).getRCArea(),NArea.findOut("Straw", 1).getRCArea(),new NAlias("straw", "Straw")).run(gui);
            new SeedCrop(NArea.findSpec(field),NArea.findSpec(seed),new NAlias("plants/barley"),new NAlias("Barley"), false).run(gui);
            return Results.SUCCESS();
        }

        return Results.FAIL();
    }

    private void loadRoutes() {
        if(new File(NConfig.current.path_routes).exists())
        {
            StringBuilder contentBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(NConfig.current.path_routes), StandardCharsets.UTF_8))
            {
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
            }
            catch (IOException ignore)
            {
            }

            if (!contentBuilder.toString().isEmpty())
            {
                JSONObject main = new JSONObject(contentBuilder.toString());
                JSONArray array = (JSONArray) main.get("routes");
                for (int i = 0; i < array.length(); i++)
                {
                    Route route = new Route((JSONObject) array.get(i));
                    this.routes.put(route.id, route);
                }
            }
        }
    }
}
