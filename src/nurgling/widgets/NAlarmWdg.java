package nurgling.widgets;

import haven.*;
import haven.res.ui.obj.buddy.Buddy;
import nurgling.NAlarmManager;
import nurgling.NConfig;
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
import java.util.Arrays;
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
    // Cache last known group for each character to handle temporary null buddy during group changes
    private final HashMap<Long, Integer> lastKnownGroup = new HashMap<>();
    // Track frame counter for characters without buddy to delay alarm
    private final HashMap<Long, Integer> unknownPlayerFrameCounter = new HashMap<>();

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
                        // Remove dead characters and mannequins from alarm list
                        if (NParser.checkName(pose, new NAlias("dead", "manneq", "skel")))
                            forRemove.add(id);
                    }
                }
            }
            borkas.removeAll(forRemove);
            // Clean up cached groups and frame counters for removed characters
            for (Long id : forRemove) {
                lastKnownGroup.remove(id);
                unknownPlayerFrameCounter.remove(id);
            }
            Gob player = NUtils.player();
            if (player != null) {
                // Check all borkas and manage alarms/arrows dynamically
                for (Long id : borkas) {
                    Gob gob = Finder.findGob(id);
                    if(gob!=null) {
                        String pose = gob.pose();
                        // Skip mannequins, skeletons, and dead characters
                        if (pose == null || !NParser.checkName(pose, new NAlias("dead", "manneq", "skel"))) {
                            Buddy buddy = gob.getattr(Buddy.class);

                            // Determine actual group - use cached if buddy is temporarily null
                            int group = 0;
                            boolean buddyLoaded = (buddy != null && buddy.b != null);
                            boolean shouldDelayAlarm = false;

                            if (buddyLoaded) {
                                group = buddy.b.group;
                                lastKnownGroup.put(id, group); // Cache the group
                                unknownPlayerFrameCounter.remove(id); // Reset counter if buddy loaded
                            } else if (lastKnownGroup.containsKey(id)) {
                                // Use cached group if buddy is temporarily null during group change
                                group = lastKnownGroup.get(id);
                                unknownPlayerFrameCounter.remove(id); // Reset counter if we have cache
                            } else {
                                // Buddy is null and no cache - might be loading, delay alarm
                                int frameCount = unknownPlayerFrameCounter.getOrDefault(id, 0);
                                frameCount++;
                                unknownPlayerFrameCounter.put(id, frameCount);

                                int alarmDelayFrames = ((Number) NConfig.get(NConfig.Key.alarmDelayFrames)).intValue();
                                if (frameCount < alarmDelayFrames) {
                                    shouldDelayAlarm = true;
                                }
                                // After delay frames elapsed, treat as unknown (group 0 - white)
                            }

                            NKinProp kinProp = NKinProp.get(group);
                            Color arrowColor = BuddyWnd.gc[group];

                            // Check if should be in alarm (only WHITE and RED groups)
                            boolean isWhiteOrRed = (arrowColor.equals(Color.WHITE) || arrowColor.equals(Color.RED));
                            boolean shouldAlarm = kinProp.alarm && isWhiteOrRed && !shouldDelayAlarm;
                            boolean isAlarmed = alarms.contains(id);

                            if (shouldAlarm && !isAlarmed) {
                                // Add alarm if needed (only after delay period)
                                addAlarm(id);
                            } else if (!shouldAlarm && isAlarmed) {
                                // Remove alarm if settings changed or group changed
                                alarms.remove(id);
                                // Arrow will auto-remove via tick() when not in alarm
                            }

                            // Check arrow separately - manage arrow color changes
                            if (shouldAlarm && kinProp.arrow) {
                                // Find existing arrow for this gob
                                NDirArrow existingArrow = null;
                                Gob.Overlay existingOverlay = null;
                                for (Gob.Overlay ol : new ArrayList<>(player.ols)) {
                                    if (ol.spr instanceof NDirArrow) {
                                        NDirArrow arrow = (NDirArrow) ol.spr;
                                        if (arrow.target == gob) {
                                            existingArrow = arrow;
                                            existingOverlay = ol;
                                            break;
                                        }
                                    }
                                }

                                if (existingArrow != null) {
                                    // Arrow exists - check if color matches
                                    if (!existingArrow.arrowColor.equals(arrowColor)) {
                                        // Color changed - remove old and create new
                                        existingOverlay.remove();
                                        player.addol(new NDirArrow(NUtils.player(), arrowColor, 50, gob, null));
                                    }
                                } else {
                                    // No arrow - create new
                                    player.addol(new NDirArrow(NUtils.player(), arrowColor, 50, gob, null));
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

            // Get kin properties for this character
            NKinProp kinProp = (buddy == null || buddy.b == null) ? NKinProp.get(0) : NKinProp.get(buddy.b.group);

            // Arrow should only exist if arrow setting is enabled
            if (!kinProp.arrow) {
                return false;
            }

            if (buddy == null) {
                // Unknown player - should be in alarm (white group)
                return true;
            } else if (buddy.b != null) {
                // Known player - check if WHITE or RED
                Color groupColor = BuddyWnd.gc[buddy.b.group];
                return (groupColor.equals(Color.WHITE) || groupColor.equals(Color.RED));
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
            // Trigger auto actions immediately when alarm is added
            checkAutoActions();
        }
    }

    /**
     * Check if auto hearth or auto logout should be triggered
     */
    private void checkAutoActions() {
        // Check player pose to avoid re-triggering while already logging out or teleporting
        Gob player = NUtils.player();
        if (player == null) {
            return;
        }

        String pose = player.pose();
        if (pose != null && (pose.equals("pointhome") || pose.equals("logout"))) {
            // Player is already teleporting or logging out - don't trigger again
            return;
        }

        boolean autoLogout = (Boolean) NConfig.get(NConfig.Key.autoLogoutOnUnknown);
        boolean autoHearth = (Boolean) NConfig.get(NConfig.Key.autoHearthOnUnknown);

        if (autoLogout) {
            // Logout takes priority over hearth
            NUtils.getGameUI().msg("Enemy spotted! Logging out!", Color.WHITE);
            NUtils.getGameUI().act("lo");
        } else if (autoHearth) {
            // Try to use hearth secret
            NUtils.getGameUI().msg("Enemy spotted! Using hearth secret!", Color.WHITE);
            NUtils.getGameUI().act("travel", "hearth");
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
            
            if (timer.isExpired()) {
                // Timer is ready - add to alarms if not already there
                if (!resourceTimerAlarms.contains(timerId)) {
                    resourceTimerAlarms.add(timerId);
                    resourceTimerWarnings.remove(timerId); // Remove from warnings
                    NAlarmManager.play("alarm/question"); // Different sound for ready resources
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
        // Show question animation for resource notifications (less urgent) with fade effect
        else if(!resourceTimerAlarms.isEmpty()) {
            // Create fade in/out effect using alpha (10% to 100% and back)
            int fadeFrame = (int) (NUtils.getTickId() / 3) % 20; // Slower animation for smooth fade
            float alpha;
            
            if (fadeFrame <= 10) {
                // Fade in: 10% to 100%
                alpha = 0.1f + (fadeFrame * 0.09f);
            } else {
                // Fade out: 100% to 10%
                alpha = 1.0f - ((fadeFrame - 10) * 0.09f);
            }
            
            // Apply alpha and draw question icon
            g.chcolor(255, 255, 255, (int)(alpha * 255));
            Coord questionPos = new Coord(sz.x / 2 - NStyle.question[0].sz().x / 2, sz.y / 2 - NStyle.question[0].sz().y / 2);
            g.image(NStyle.question[0], questionPos);

            // Place number badge in the top-right empty circle of the question mark
            if (numberAlarm != null) {
                // Position in top-right area where the small circle is
                Coord numberPos = new Coord(
                    questionPos.x + (NStyle.question[0].sz().x * 3 / 4) - numberAlarm.sz().x / 2,  // 3/4 right
                    questionPos.y + (NStyle.question[0].sz().y / 4) - numberAlarm.sz().y / 2 - UI.scale(4)  // 1/4 down, then up 5 pixels
                );
                g.image(numberAlarm, numberPos);
            }
            
            g.chcolor(); // Reset color
        }
        
        super.draw(g);
    }
}