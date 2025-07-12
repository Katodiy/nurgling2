package nurgling.actions;

import nurgling.*;

import java.util.ArrayList;

public interface Action {
    default ArrayList<Action> getSupp() {
        return new ArrayList<>();
    }

    Results run(NGameUI gui)
            throws InterruptedException;
}
