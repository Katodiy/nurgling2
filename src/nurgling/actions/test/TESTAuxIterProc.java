package nurgling.actions.test;

import haven.*;
import haven.res.ui.obj.buddy.Buddy;
import nurgling.NAlarmManager;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.NTask;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Auxiliary iterator processor for deferred entity validation.
 * Performs cyclic traversal of cached object references with
 * conditional callback invocation based on metadata flags.
 */
public class TESTAuxIterProc implements Action {
    
    private static final NAlias ENTITY_TYPE_A = new NAlias("borka");
    private static final NAlias ENTITY_TYPE_B = new NAlias("rowboat");
    private static final NAlias EXCLUSION_FILTER = new NAlias("dead", "manneq", "skel");
    
    private final Set<Long> processedRefs = new HashSet<>();
    
    private long lastInvocationTs = 0;
    private static final long INVOKE_THRESHOLD_MS = 3000;
    
    public static final AtomicBoolean stop = new AtomicBoolean(false);
    
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        stop.set(false);
        gui.msg("AUX_ITER_PROC: INITIALIZED", Color.YELLOW);
        
        while (!stop.get()) {
            // Используем NTask для ожидания вместо Thread.sleep
            NUtils.addTask(new NTask() {
                private int tickCount = 0;
                
                {
                    this.infinite = true;
                }
                
                @Override
                public boolean check() {
                    if (stop.get()) return true;
                    
                    tickCount++;
                    if (tickCount >= 10) { // ~500ms (примерно 50ms на тик)
                        // Выполняем проверку при каждом завершении ожидания
                        performIterationCycle(gui);
                        return true;
                    }
                    return false;
                }
            });
            
            if (stop.get()) break;
        }
        
        gui.msg("AUX_ITER_PROC: TERMINATED", Color.GRAY);
        return Results.SUCCESS();
    }
    
    private void performIterationCycle(NGameUI gui) {
        Gob refEntity = NUtils.player();
        if (refEntity == null) return;
        
        processedRefs.removeIf(id -> Finder.findGob(id) == null);
        
        synchronized (gui.ui.sess.glob.oc) {
            for (Gob gob : gui.ui.sess.glob.oc) {
                if (gob instanceof OCache.Virtual || gob.attr.isEmpty()) continue;
                if (gob.id == refEntity.id) continue;
                
                String descriptor = (gob.ngob != null) ? gob.ngob.name : null;
                if (descriptor == null) continue;
                
                if (NParser.checkName(descriptor, ENTITY_TYPE_B)) {
                    if (!processedRefs.contains(gob.id)) {
                        invokeCallbackTypeB(gui, gob);
                        processedRefs.add(gob.id);
                    }
                    continue;
                }
                
                if (NParser.checkName(descriptor, ENTITY_TYPE_A)) {
                    String stateVector = gob.pose();
                    if (stateVector != null && NParser.checkName(stateVector, EXCLUSION_FILTER)) {
                        continue;
                    }
                    
                    if (NUtils.playerID() == gob.id) continue;
                    
                    if (!processedRefs.contains(gob.id)) {
                        invokeCallbackTypeA(gui, gob);
                        processedRefs.add(gob.id);
                    }
                }
            }
        }
    }
    
    private void invokeCallbackTypeA(NGameUI gui, Gob target) {
        String metadata = extractMetadata(target);
        
        NAlarmManager.play("alarm/troll");
        
        dispatchWithThrottle(gui, "!!!!!! " + (metadata != null ? "[" + metadata + "]" : ""));
        
        gui.msg("PLAYER!" + (metadata != null ? ": " + metadata : "!"), Color.RED);
    }
    
    private void invokeCallbackTypeB(NGameUI gui, Gob ref) {
        NAlarmManager.play("alarm/mammoth");
        
        dispatchWithThrottle(gui, "!!!!!! ROWBOAT !");
        
        gui.msg("! ROWBOAT !", Color.MAGENTA);
    }
    
    private String extractMetadata(Gob gob) {
        try {
            Buddy buddy = gob.getattr(Buddy.class);
            if (buddy != null && buddy.b != null && buddy.b.name != null) {
                return buddy.b.name;
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private void dispatchWithThrottle(NGameUI gui, String payload) {
        long now = System.currentTimeMillis();
        if (now - lastInvocationTs < INVOKE_THRESHOLD_MS) {
            return;
        }
        lastInvocationTs = now;
        
        try {
            if (gui.chat != null) {
                ChatUI.Channel locationChat = gui.chat.findLocationChat();
                if (locationChat instanceof ChatUI.EntryChannel) {
                    ((ChatUI.EntryChannel) locationChat).send(payload);
                } else {
                    ChatUI.Channel sel = gui.chat.sel;
                    if (sel instanceof ChatUI.EntryChannel) {
                        ((ChatUI.EntryChannel) sel).send(payload);
                    }
                }
            }
        } catch (Exception e) {
            gui.msg("Dispatch failed: " + payload, Color.ORANGE);
        }
    }
}
