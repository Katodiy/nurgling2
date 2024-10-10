package nurgling.widgets;

import haven.*;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import nurgling.tools.NParser;

import java.util.ArrayList;

public class NAlarmWdg extends Widget
{
    final public static ArrayList<Long> borkas = new ArrayList();
    ArrayList<Long> alarms = new ArrayList<>();


    public NAlarmWdg() {
        super();
        sz = NStyle.alarm[0].sz();
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        synchronized (borkas) {
            ArrayList<Long> forRemove = new ArrayList();
            for(Long id: borkas) {
                Gob gob;
                if((gob = Finder.findGob(id))==null) {
                    forRemove.add(id);
                }
                else
                {
                    if(NUtils.playerID()!=-1)
                    {
                        if(id == NUtils.playerID())
                            forRemove.add(id);
                    }
                    String pose = gob.pose();
                    if(pose!=null) {
                        if(NParser.checkName(pose, new NAlias("dead")))
                            forRemove.add(id);
                    }
                }
            }
            borkas.removeAll(forRemove);
        }
    }

    public static void addBorka(long id) {
        synchronized (borkas) {
            borkas.add(id);
        }
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