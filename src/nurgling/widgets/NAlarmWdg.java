package nurgling.widgets;

import haven.*;
import nurgling.NStyle;
import nurgling.NUtils;

import java.util.ArrayList;

public class NAlarmWdg extends Widget
{
    ArrayList<Long> alarms = new ArrayList<>();


    public NAlarmWdg() {
        super();
        sz = NStyle.alarm[0].sz();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
    }

    @Override
    public void draw(GOut g) {
        if(!alarms.isEmpty()) {
            int id = (int) (NUtils.getTickId() / 5) % 12;
            g.image(NStyle.alarm[id], new Coord(sz.x / 2 - NStyle.alarm[0].sz().x / 2, sz.y / 2 - NStyle.alarm[0].sz().y / 2));
        }
        super.draw(g);
    }
}