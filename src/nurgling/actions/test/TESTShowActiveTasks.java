package nurgling.actions.test;

import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.tasks.NTask;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TESTShowActiveTasks implements Action {
    @Override
    public Results run(NGameUI gui) throws InterruptedException {
        if (gui.ui.core == null) {
            gui.msg("NCore is not available");
            return Results.ERROR("NCore not available");
        }

        String taskList = gui.ui.core.toString();
        
        if (taskList.isEmpty()) {
            gui.msg("No active tasks in NCore");
        } else {
            String[] tasks = taskList.split("\\|");
            gui.msg("Active tasks count: " + tasks.length);
            
            for (int i = 0; i < tasks.length; i++) {
                if (!tasks[i].isEmpty()) {
                    gui.msg("Task " + (i + 1) + ": " + tasks[i]);
                }
            }
        }
        
        return Results.SUCCESS();
    }
}
