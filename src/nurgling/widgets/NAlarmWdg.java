package nurgling.widgets;

import haven.*;
import haven.res.ui.obj.buddy.Buddy;
import nurgling.NAlarmManager;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.LocalizedResourceTimer;
import nurgling.conf.NKinProp;
import nurgling.overlays.NDirArrow;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static haven.Text.sans;

public class NAlarmWdg extends Widget
{
    final public static ArrayList<Long> borkas = new ArrayList();
    final ArrayList<Long> alarms = new ArrayList<>();
    final ArrayList<String> resourceTimerAlarms = new ArrayList<>(); // Resource timer IDs in alarm state
    final ArrayList<String> resourceTimerWarnings = new ArrayList<>(); // Resource timer IDs in warning state (10min left)
    private static final Text.Furnace active_title = new PUtils.BlurFurn(new Text.Foundry(sans, 15, Color.WHITE).aa(true), 2, 1, new Color(36, 25, 25));
    TexI numberAlarm = null;
    public NAlarmWdg() {
        super();
        sz = NStyle.alarm[0].sz();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        synchronized (borkas) {
            ArrayList<Long> forRemove = new ArrayList();
            for (Long id : borkas) {
                Gob gob;
                if ((gob = Finder.findGob(id)) == null) {
                    forRemove.add(id);
                } else {
                    if (NUtils.playerID() != -1) {
                        if (id == NUtils.playerID())
                            forRemove.add(id);
                    }
                    String pose = gob.pose();
                    if (pose != null) {
                        if (NParser.checkName(pose, new NAlias("dead")))
                            forRemove.add(id);
                    }
                }
            }
            borkas.removeAll(forRemove);
            Gob player = NUtils.player();
            if (player != null) {
                for (Long id : borkas) {
                    if (!alarms.contains(id)) {
                        Gob gob = Finder.findGob(id);
                        if(gob!=null) {
                            String pose = gob.pose();
                            if (pose != null) {
                                if (NParser.checkName(pose, new NAlias("dead"))) {
                                    Buddy buddy = gob.getattr(Buddy.class);
                                    if (buddy == null) {
                                        NKinProp kinProp = NKinProp.get(0);
                                        if (kinProp.alarm) {
                                            addAlarm(id);

                                        }
                                        if (kinProp.arrow) {
                                            synchronized (player.ols) {
                                                player.addol(new NDirArrow(NUtils.player(), Color.WHITE, 50, gob, null));
                                            }
                                        }
                                    } else {
                                        if (buddy.b != null) {
                                            NKinProp kinProp = NKinProp.get(buddy.b.group);
                                            if (kinProp.alarm) {
                                                addAlarm(id);
                                            }
                                            if (kinProp.arrow) {
                                                synchronized (player.ols) {
                                                    player.addol(new NDirArrow(NUtils.player(), BuddyWnd.gc[buddy.b.group], 50, gob, null));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            forRemove.clear();
            for (Long id : alarms) {
                if (Finder.findGob(id) == null) {
                    forRemove.add(id);
                }
            }
            for (Long id : forRemove) {
                alarms.remove(id);
            }
            
            // Check resource timers for alarms and warnings
            checkResourceTimers();
            
            // Update alarm count display (combine PvP alarms + resource alarms)
            int totalAlarms = alarms.size() + resourceTimerAlarms.size();
            if (totalAlarms > 0) {
                numberAlarm = new TexI(active_title.render(String.valueOf(totalAlarms)).img);
            } else {
                numberAlarm = null;
            }
        }
    }

    public static boolean isInAlarm(Long id)
    {
        if(NUtils.getGameUI()==null)
            return false;
        if(NUtils.getGameUI().alarmWdg.alarms.contains(id))
        {
            Gob gob = Finder.findGob(id);
            if(gob == null)
                return false;
            Buddy buddy = gob.getattr(Buddy.class);
            if (buddy == null) {
                return true;
            } else {
                if (buddy != null && buddy.b != null) {
                    if (BuddyWnd.gc[buddy.b.group] == Color.WHITE || BuddyWnd.gc[buddy.b.group] == Color.RED) {
                        return true;
                    }
                }
                else
                {
                    NUtils.getGameUI().alarmWdg.alarms.remove(id);
                    return false;
                }
            }
        }
        return false;
    }

    public static void addBorka(long id) {
        synchronized (borkas) {
            borkas.add(id);
        }
    }

    private void addAlarm(Long id) {
        synchronized (alarms) {
            NAlarmManager.play("alarm/alarm");
            alarms.add(id);
        }
    }
    
    /**
     * Check all resource timers and update alarm/warning states
     */
    private void checkResourceTimers() {
        if (NUtils.getGameUI() == null || NUtils.getGameUI().localizedResourceTimerService == null) {
            return;
        }
        
        java.util.Collection<LocalizedResourceTimer> allTimers = NUtils.getGameUI().localizedResourceTimerService.getAllTimers();
        
        // Clear old resource timer alarms/warnings for timers that no longer exist
        resourceTimerAlarms.removeIf(timerId -> 
            allTimers.stream().noneMatch(t -> t.getResourceId().equals(timerId)));
        resourceTimerWarnings.removeIf(timerId -> 
            allTimers.stream().noneMatch(t -> t.getResourceId().equals(timerId)));
        
        for (LocalizedResourceTimer timer : allTimers) {
            String timerId = timer.getResourceId();
            long remainingMs = timer.getRemainingTime();
            long tenMinutesMs = 10 * 60 * 1000; // 10 minutes in milliseconds
            
            if (timer.isExpired()) {
                // Timer is ready - add to alarms if not already there
                if (!resourceTimerAlarms.contains(timerId)) {
                    resourceTimerAlarms.add(timerId);
                    resourceTimerWarnings.remove(timerId); // Remove from warnings
                    NAlarmManager.play("alarm/alarm"); // Different sound for ready resources
                }
            } else if (remainingMs <= tenMinutesMs) {
                // Timer has 10 minutes or less - add to warnings if not already there
                if (!resourceTimerWarnings.contains(timerId) && !resourceTimerAlarms.contains(timerId)) {
                    resourceTimerWarnings.add(timerId);
                    NAlarmManager.play("alarm/alarm"); // Warning sound
                }
            } else {
                // Timer has more than 10 minutes - remove from both lists
                resourceTimerAlarms.remove(timerId);
                resourceTimerWarnings.remove(timerId);
            }
        }
    }

    @Override
    public void draw(GOut g) {
        int id = (int) (NUtils.getTickId() / 5) % 12;
        
        // Show urgent alarm animation for PvP threats
        if(!alarms.isEmpty()) {
            g.image(NStyle.alarm[id], new Coord(sz.x / 2 - NStyle.alarm[0].sz().x / 2, sz.y / 2 - NStyle.alarm[0].sz().y / 2));
            if (numberAlarm != null) {
                g.image(numberAlarm, new Coord(sz.x / 2 + UI.scale(12), sz.y / 2 - UI.scale(24)));
            }
        }
        // Show question animation for resource notifications (less urgent)
        else if(!resourceTimerAlarms.isEmpty()) {
            g.image(NStyle.question[id], new Coord(sz.x / 2 - NStyle.question[0].sz().x / 2, sz.y / 2 - NStyle.question[0].sz().y / 2));
            if (numberAlarm != null) {
                g.image(numberAlarm, new Coord(sz.x / 2 + UI.scale(12), sz.y / 2 - UI.scale(24)));
            }
        }
        
        super.draw(g);
    }
}