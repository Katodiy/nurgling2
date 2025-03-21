package nurgling.tasks;

import haven.Coord;
import haven.Gob;
import haven.WItem;
import haven.Widget;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.conf.NPrepBlocksProp;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

public class WaitPrepSmokingState extends NTask
{
    public WaitPrepSmokingState(Gob log, int target_size)
    {
        this.player = NUtils.player();
        this.log = log;
        this.target_size = target_size;
    }


    Gob player;
    Gob log;
    int target_size;
    NPrepBlocksProp prop;
    public enum State
    {
        WORKING,
        LOGNOTFOUND,
        TIMEFORDRINK,
        DANGER,
        NOFREESPACE
    }

    State state = State.WORKING;
    @Override
    public boolean check() {
        int space = NUtils.getGameUI().getInventory().calcNumberFreeCoord(new Coord(1, 2));
        if (Finder.findGob(log.id) == null) {
            state = State.LOGNOTFOUND;

        }
        else {

                if (getCount()>=target_size) {
                    state = State.NOFREESPACE;
                }
                else if (NUtils.getEnergy() < 0.22) {
                    state = State.DANGER;
                } else if (NUtils.getStamina() <= 0.45) {
                    state = State.TIMEFORDRINK;
                } else if (space <= 1 && space >=0) {
                    if(NUtils.getGameUI().getInventory().calcFreeSpace()<=2 || space==0)
                        state = State.NOFREESPACE;
                }

        }
        return state != State.WORKING;
    }

    public State getState() {
        return state;
    }

    private int getCount()
    {
        int count = 0;
        for (Widget widget = NUtils.getGameUI().getInventory().child; widget != null; widget = widget.next) {
            if (widget instanceof WItem) {
                WItem item = (WItem) widget;
                String item_name;
                if ((item_name = ((NGItem) item.item).name()) != null) {
                    if (NParser.checkName(item_name, new NAlias("block", "Block")))
                        count++;
                }
            }
        }
        return count;
    }
}