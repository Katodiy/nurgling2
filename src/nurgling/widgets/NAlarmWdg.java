package nurgling.widgets;

import haven.*;
import haven.res.ui.obj.buddy.Buddy;
import nurgling.NAlarmManager;
import nurgling.NStyle;
import nurgling.NUtils;
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

import static nurgling.overlays.NBarrelOverlay.bsans;

public class NAlarmWdg extends Widget
{
    final public static ArrayList<Long> borkas = new ArrayList();
    final ArrayList<Long> alarms = new ArrayList<>();
    private static final Text.Furnace active_title = new PUtils.BlurFurn(new Text.Foundry(bsans, 15, Color.WHITE).aa(true), 2, 1, new Color(36, 25, 25));
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
                        if (NParser.checkName(pose, new NAlias(new ArrayList<String>(Arrays.asList("dead", "mannequin")))))
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
            forRemove.clear();
            for (Long id : alarms) {
                if (Finder.findGob(id) == null) {
                    forRemove.add(id);
                }
            }
            for (Long id : forRemove) {
                alarms.remove(id);
            }
            if (!alarms.isEmpty()) {
                numberAlarm = new TexI(active_title.render(String.valueOf(alarms.size())).img);
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

    @Override
    public void draw(GOut g) {
        if(!alarms.isEmpty()) {
            int id = (int) (NUtils.getTickId() / 5) % 12;
            g.image(NStyle.alarm[id], new Coord(sz.x / 2 - NStyle.alarm[0].sz().x / 2, sz.y / 2 - NStyle.alarm[0].sz().y / 2));
            g.image(numberAlarm, new Coord(sz.x / 2 + UI.scale(12), sz.y / 2 - UI.scale(24)));
        }
        super.draw(g);
    }
}