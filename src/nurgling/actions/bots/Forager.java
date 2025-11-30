package nurgling.actions.bots;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.areas.NContext;
import nurgling.conf.NAreaRad;
import nurgling.conf.NDiscordNotification;
import nurgling.conf.NForagerProp;
import nurgling.routes.*;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.widgets.NAlarmWdg;

import java.util.ArrayList;
import java.util.HashSet;

public class Forager implements Action {
    
    private HashSet<Long> processedGobs = new HashSet<>();
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        nurgling.widgets.bots.Forager w = null;
        NForagerProp prop = null;
        try {
            NUtils.getUI().core.addTask(new nurgling.tasks.WaitCheckable(
                NUtils.getGameUI().add((w = new nurgling.widgets.bots.Forager()), UI.scale(200, 200))
            ));
            prop = w.prop;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            if (w != null)
                w.destroy();
        }
        
        if (prop == null) {
            return Results.ERROR("No configuration");
        }
        
        NForagerProp.PresetData preset = prop.presets.get(prop.currentPreset);
        if (preset == null || preset.foragerPath == null) {
            return Results.ERROR("No path configured");
        }
        
        ForagerPath path = preset.foragerPath;
        
        if (path.getSectionCount() == 0) {
            return Results.ERROR("Path has no sections");
        }
        
        
        // Get dangerous animal patterns from NConfig
        @SuppressWarnings("unchecked")
        ArrayList<NAreaRad> animalRads = (ArrayList<NAreaRad>) NConfig.get(NConfig.Key.animalrad);
        ArrayList<String> dangerousAnimals = new ArrayList<>();
        if (animalRads != null) {
            for (NAreaRad rad : animalRads) {
                dangerousAnimals.add(rad.name);
            }
        }
        
        // Get first waypoint to navigate to start
        MiniMap.Location sessloc = gui.mmap.sessloc;
        if(sessloc == null) {
            return Results.ERROR("Cannot get sessloc");
        }
        Coord2d startPos = path.waypoints.get(0).toWorldCoord(sessloc);
        if(startPos == null) {
            return Results.ERROR("Cannot get start position - waypoint not in current segment");
        }
        
        new PathFinder(startPos).run(gui);
        
        // Check and unload inventory before starting
        if (preset.freeInventory && isInventoryFull(gui)) {
            unloadInventory(gui);
            // Return to start
            new PathFinder(startPos).run(gui);
        }
        
        // Main loop through sections
        for (int i = 0; i < path.getSectionCount(); i++)
        {
            ForagerSection section = path.getSection(i);
            if (section == null) continue;

            
            // Check for dangerous players
            if (!NAlarmWdg.borkas.isEmpty()) {
                if (!preset.onPlayerAction.equals("nothing")) {
                    performSafetyAction(gui, preset.onPlayerAction);
                    return Results.SUCCESS();
                }
            }
            
            // Check for dangerous animals in radius 200
            for (String animalPattern : dangerousAnimals) {
                // Skip bats if ignoreBats is enabled
                if (preset.ignoreBats && animalPattern.contains("bat")) {
                    continue;
                }
                
                Gob animal = Finder.findGob(NUtils.player().rc, new NAlias(animalPattern), null, 200.0);
                if (animal != null) {
                    performSafetyAction(gui, preset.onAnimalAction);
                    return Results.SUCCESS();
                }
            }

            // Check if there are any target objects near the section endpoint (within 1 tile = 11 units)
            Coord2d sectionEnd = section.endPoint;
            Gob targetGob = findGobNear(sectionEnd, 11.0);

            if (targetGob != null)
            {
                // Go to the object if found within 1 tile
                new PathFinder(targetGob).run(gui);

            } else
            {
                // Go to the endpoint if no objects found nearby
                new PathFinder(sectionEnd).run(gui);
            }

            // Process actions for this section
            processSection(gui, section, preset.actions, dangerousAnimals, preset);

            // Check inventory after each section
            if (isInventoryFull(gui)) {
                if (preset.freeInventory) {
                    Coord2d currentPos = NUtils.player().rc;
                    unloadInventory(gui);
                    new PathFinder(currentPos).run(gui);
                } else {
                    new TravelToHearthFire().run(gui);
                    new FreeInventory2(new NContext(gui)).run(gui);
                    return Results.SUCCESS();
                }
            }
        }
        
        // After completing all sections, perform finish action
        performSafetyAction(gui, preset.afterFinishAction);
        
        return Results.SUCCESS();
    }
    
    private void processSection(NGameUI gui, ForagerSection section, java.util.List<ForagerAction> actions, 
                                 ArrayList<String> dangerousAnimals, NForagerProp.PresetData preset) throws InterruptedException {
        double radius = 250.0;
        
        // Use actions from preset, not from section
        for (ForagerAction action : actions) {
            // Check for safety before each action
            if (!NAlarmWdg.borkas.isEmpty() && !preset.onPlayerAction.equals("nothing")) {
                performSafetyAction(gui, preset.onPlayerAction);
                return;
            }
            
            for (String animalPattern : dangerousAnimals) {
                // Skip bats if ignoreBats is enabled
                if (preset.ignoreBats && animalPattern.contains("bat")) {
                    continue;
                }
                
                Gob animal = Finder.findGob(NUtils.player().rc, new NAlias(animalPattern), null, 200.0);
                if (animal != null) {
                    performSafetyAction(gui, preset.onAnimalAction);
                    return;
                }
            }
            
            processAction(gui, action, section.getCenterPoint(), radius, preset);
        }
    }
    
    private void processAction(NGameUI gui, ForagerAction action, Coord2d center, double radius, NForagerProp.PresetData preset) throws InterruptedException {
        ArrayList<Gob> gobs = Finder.findGobs(center, new NAlias(action.targetObjectPattern), null, radius);
        
        // Filter out already processed gobs
        gobs.removeIf(gob -> processedGobs.contains(gob.id));
        
        if (gobs.isEmpty()) {
            return;
        }
        
        
        switch (action.actionType) {
            case PICK:
                for (Gob gob : gobs) {
                    if (isInventoryFull(gui)) {
                        if (preset.freeInventory) {
                            Coord2d currentPos = NUtils.player().rc;
                            unloadInventory(gui);
                            new PathFinder(currentPos).run(gui);
                        } else {
                            break;
                        }
                    }

                    new PathFinder(gob).run(gui);
                    new SelectFlowerAction("Pick", gob).run(gui);
                    NUtils.getUI().core.addTask(new nurgling.tasks.WaitGobRemoval(gob.id));
                    
                    // Mark as processed
                    processedGobs.add(gob.id);
                }
                break;
                
            case FLOWER_ACTION:
                for (Gob gob : gobs) {
                    new PathFinder(gob).run(gui);
                    new SelectFlowerAction(action.actionName, gob).run(gui);
                    
                    // Wait for pose change
                    NUtils.getUI().core.addTask(new nurgling.tasks.WaitPose(NUtils.player(), "gfx/borka/idle"));
                    
                    // Mark as processed
                    processedGobs.add(gob.id);
                }
                break;
                
            case CHAT_NOTIFY:
                if (!gobs.isEmpty()) {
                    String message = String.format("Found %d %s objects!", gobs.size(), action.targetObjectPattern);
                    
                    // Send notification based on target
                    if (action.notifyTarget == ForagerAction.NotifyTarget.DISCORD) {
                        // Send Discord notification using general client settings
                        NDiscordNotification discordSettings = NDiscordNotification.get("general");
                        if (discordSettings != null && discordSettings.webhookUrl != null && !discordSettings.webhookUrl.isEmpty()) {
                            gui.msgToDiscord(discordSettings, message);
                        }
                    } else if (action.notifyTarget == ForagerAction.NotifyTarget.CHAT) {
                        // Send message to chat channel
                        if (action.chatChannelName != null && !action.chatChannelName.isEmpty()) {
                            // Find chat channel by name and send message
                            ChatUI.Channel targetChannel = findChatChannelByName(gui, action.chatChannelName);
                            if (targetChannel != null && targetChannel instanceof ChatUI.EntryChannel) {
                                ((ChatUI.EntryChannel) targetChannel).send(message);
                            }
                        }
                    }
                    
                    // Mark all notified objects as processed
                    for (Gob gob : gobs) {
                        processedGobs.add(gob.id);
                    }
                    
                    // Pause for 5 minutes (18000 frames at 60fps)
                    NUtils.getUI().core.addTask(new nurgling.tasks.WaitTicks(18000));
                    
                    // Signal to stop the bot after pause
                    throw new InterruptedException("CHAT_NOTIFY action triggered - stopping bot");
                }
                break;
        }
    }
    
    private boolean isInventoryFull(NGameUI gui) throws InterruptedException
    {

        if (gui.vhand != null) {
            return true;
        }

        if (gui.getInventory() != null) {
            return gui.getInventory().getFreeSpace() <= 4;
        }

        return false;
    }
    
    private void unloadInventory(NGameUI gui) throws InterruptedException {
        new FreeInventory2(new NContext(gui)).run(gui);
    }
    
    private void performSafetyAction(NGameUI gui, String action) throws InterruptedException {
        switch (action) {
            case "logout":
                gui.act("lo");
                break;
            case "travel hearth":
                gui.act("travel", "hearth");
                break;
            case "nothing":
            default:
                // Do nothing
                break;
        }
    }
    
    private Gob findGobNear(Coord2d pos, double radius) {
        synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc) {
                if (!(gob instanceof OCache.Virtual || gob.attr.isEmpty() || gob.getClass().getName().contains("GlobEffector"))) {
                    if (gob.id != NUtils.playerID() && gob.rc.dist(pos) <= radius && !(gob instanceof MapView.Plob) && gob.id > 0) {
                        return gob;
                    }
                }
            }
        }
        return null;
    }
    
    private ChatUI.Channel findChatChannelByName(NGameUI gui, String channelName) {
        if (gui.chat == null) return null;
        
        for (Widget w = gui.chat.child; w != null; w = w.next) {
            if (w instanceof ChatUI.Channel) {
                ChatUI.Channel chan = (ChatUI.Channel) w;
                if (chan.name().equalsIgnoreCase(channelName)) {
                    return chan;
                }
            }
        }
        return null;
    }
}
