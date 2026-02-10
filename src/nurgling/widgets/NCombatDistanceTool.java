package nurgling.widgets;

import haven.*;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.i18n.L10n;
import nurgling.tools.Finder;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static haven.OCache.posres;

public class NCombatDistanceTool extends haven.Window implements Runnable {
    private static final Map<String, Double> animalDistances = new HashMap<>();
    private static final Map<String, Double> vehicleDistances = new HashMap<>();

    static {
        animalDistances.put("gfx/kritter/adder/adder", 17.1);
        animalDistances.put("gfx/kritter/ant/ant", 15.2);
        animalDistances.put("gfx/kritter/cattle/cattle", 27.0);
        animalDistances.put("gfx/kritter/badger/badger", 19.9);
        animalDistances.put("gfx/kritter/bear/bear", 24.7);
        animalDistances.put("gfx/kritter/boar/boar", 25.1);
        animalDistances.put("gfx/kritter/caveangler/caveangler", 27.2);
        animalDistances.put("gfx/kritter/cavelouse/cavelouse", 22.0);
        animalDistances.put("gfx/kritter/fox/fox", 18.1);
        animalDistances.put("gfx/kritter/horse/horse", 23.0);
        animalDistances.put("gfx/kritter/lynx/lynx", 20.0);
        animalDistances.put("gfx/kritter/mammoth/mammoth", 30.3);
        animalDistances.put("gfx/kritter/moose/moose", 25.0);
        animalDistances.put("gfx/kritter/orca/orca", 49.25);
        animalDistances.put("gfx/kritter/reddeer/reddeer", 25.0);
        animalDistances.put("gfx/kritter/roedeer/roedeer", 22.0);
        animalDistances.put("gfx/kritter/spermwhale/spermwhale", 112.2);
        animalDistances.put("gfx/kritter/goat/wildgoat", 18.9);
        animalDistances.put("gfx/kritter/wolf/wolf", 25.0);
        animalDistances.put("gfx/kritter/wolverine/wolverine", 21.0);
        animalDistances.put("gfx/borka/body", 55.0);
        animalDistances.put("gfx/kritter/troll/troll", 30.0);
        animalDistances.put("gfx/kritter/greyseal/greyseal", 22.0);
        animalDistances.put("gfx/kritter/walrus/walrus", 28.0);
        animalDistances.put("gfx/kritter/bat/bat", 18.0);
        animalDistances.put("gfx/kritter/goldeneagle/goldeneagle", 20.0);

        vehicleDistances.put("gfx/terobjs/vehicle/rowboat", 13.3);
        vehicleDistances.put("gfx/terobjs/vehicle/dugout", 7.4);
        vehicleDistances.put("gfx/terobjs/vehicle/snekkja", 29.35);
        vehicleDistances.put("gfx/terobjs/vehicle/knarr", 54.5);
        vehicleDistances.put("gfx/kritter/horse/stallion", 5.4);
        vehicleDistances.put("gfx/kritter/horse/mare", 5.4);
    }

    private final NGameUI gui;
    private volatile boolean stop;
    private final haven.Label currentDistanceLabel;
    private String distanceValue = "";
    private Thread updateThread;

    public NCombatDistanceTool(NGameUI gui) {
        super(UI.scale(180, 60), L10n.get("combat.distance_title"), true);
        this.gui = gui;
        this.stop = false;

        Widget prev;

        prev = add(new haven.Label(L10n.get("combat.set_distance")), 0, UI.scale(6));

        prev = add(new TextEntry(UI.scale(80), distanceValue) {
            @Override
            protected void changed() {
                distanceValue = this.buf.line();
            }
        }, prev.pos("ur").adds(5, 0));

        prev = add(new Button(UI.scale(40), L10n.get("common.go")) {
            @Override
            public void click() {
                moveToDistance();
                defocus();
            }
        }, prev.pos("ur").adds(5, -2));

        add(new Button(UI.scale(50), L10n.get("combat.auto")) {
            @Override
            public void click() {
                tryToAutoDistance();
                defocus();
            }
        }, prev.pos("ur").adds(5, 0));

        currentDistanceLabel = new haven.Label(L10n.get("combat.current_dist") + ": " + L10n.get("combat.no_target"));
        add(currentDistanceLabel, UI.scale(0, 40));
        pack();
    }

    public void start() {
        stop = false;
        updateThread = new Thread(this, "CombatDistanceTool");
        updateThread.start();
    }

    @Override
    public void run() {
        DecimalFormat df = new DecimalFormat("#.##");
        while (!stop) {
            try {
                if (gui.fv != null && gui.fv.current != null) {
                    double dist = getDistance(gui.fv.current.gobid);
                    if (dist < 0) {
                        currentDistanceLabel.settext("Current dist: No target");
                    } else {
                        currentDistanceLabel.settext("Current dist: " + df.format(dist) + " units");
                    }
                } else {
                    currentDistanceLabel.settext("Current dist: No target");
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void tryToAutoDistance() {
        if (gui == null || gui.map == null || gui.fv == null || gui.fv.current == null) {
            return;
        }

        Gob player = NUtils.player();
        if (player == null) {
            return;
        }

        double baseDistance = 0.0;
        double vehicleOffset = 0.0;

        Long vehicleId = getPlayerVehicleId(player);
        if (vehicleId != null) {
            Gob vehicle = Finder.findGob(vehicleId);
            if (vehicle != null && vehicle.ngob != null && vehicle.ngob.name != null) {
                Double offset = vehicleDistances.get(vehicle.ngob.name);
                if (offset != null) {
                    vehicleOffset = offset;
                }
            }
        }

        Gob enemy = getEnemy();
        if (enemy != null && enemy.ngob != null && enemy.ngob.name != null) {
            Double dist = animalDistances.get(enemy.ngob.name);
            if (dist != null) {
                baseDistance = dist;
            }
        }

        if (baseDistance > 0) {
            moveToDistance(baseDistance + vehicleOffset);
        } else {
            gui.msg("Unknown enemy type - no auto distance available", Color.YELLOW);
        }
    }

    private Long getPlayerVehicleId(Gob player) {
        Moving mv = player.getattr(Moving.class);
        if (mv instanceof Following) {
            return ((Following) mv).tgt;
        }
        return null;
    }

    private void moveToDistance() {
        try {
            double distance = Double.parseDouble(distanceValue);
            moveToDistance(distance);
        } catch (NumberFormatException e) {
            gui.error("Invalid distance format. Use numbers like 25.5");
        }
    }

    private void moveToDistance(double distance) {
        Gob enemy = getEnemy();
        Gob player = NUtils.player();
        if (enemy != null && player != null) {
            double angle = enemy.rc.angle(player.rc);
            Coord2d target = getNewCoord(enemy, distance, angle);
            gui.map.wdgmsg("click", Coord.z, target.floor(posres), 1, 0);
        } else {
            gui.msg("No visible target", Color.WHITE);
        }
    }

    private Coord2d getNewCoord(Gob enemy, double distance, double angle) {
        return new Coord2d(
                enemy.rc.x + distance * Math.cos(angle),
                enemy.rc.y + distance * Math.sin(angle)
        );
    }

    private Gob getEnemy() {
        if (gui.fv != null && gui.fv.current != null) {
            return Finder.findGob(gui.fv.current.gobid);
        }
        return null;
    }

    private double getDistance(long gobId) {
        Gob enemy = Finder.findGob(gobId);
        Gob player = NUtils.player();
        if (enemy != null && player != null) {
            return enemy.rc.dist(player.rc);
        }
        return -1;
    }

    private void defocus() {
        if (gui.portrait != null) {
            setfocus(gui.portrait);
        }
    }

    public void stopTool() {
        stop = true;
        if (updateThread != null) {
            updateThread.interrupt();
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stopTool();
            reqdestroy();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public void destroy() {
        stopTool();
        super.destroy();
    }
}
