package nurgling.widgets;

import haven.*;
import haven.Window;
import nurgling.*;
import nurgling.conf.*;

import java.awt.*;

public class NKinSettings extends Window
{
    final ICheckBox btn;
    GroupSelector gs;
    CheckBox ring;
    CheckBox alarm;
    CheckBox arrow;
    CheckBox hil;

    public NKinSettings(ICheckBox btn)
    {
        super(UI.scale(300, 150), "Kin Notification");
        this.btn = btn;
        prev = add(gs = new GroupSelector(0));
        prev = add(ring = new CheckBox("Selection circle")
        {

            public void set(boolean val)
            {
                NKinProp prop = new NKinProp(gs.group,alarm.a,arrow.a,val,hil.a);
                NKinProp.set(prop);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));
        prev = add(arrow = new CheckBox("Arrow to character")
        {

            public void set(boolean val)
            {
                NKinProp prop = new NKinProp(gs.group,alarm.a,val,ring.a,hil.a);
                NKinProp.set(prop);
                a = val;
            }
        }, prev.pos("bl").adds(0, 5));
        prev = add(alarm = new CheckBox("Alarm")
        {

            public void set(boolean val)
            {
                NKinProp prop = new NKinProp(gs.group,val,arrow.a,ring.a, hil.a);
                NKinProp.set(prop);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));

        prev = add(hil = new CheckBox("Hide in list")
        {

            public void set(boolean val)
            {
                NKinProp prop = new NKinProp(gs.group,alarm.a,arrow.a,ring.a,val);
                NKinProp.set(prop);
                a = val;
            }

        }, prev.pos("bl").adds(0, 5));

        gs.changed(0);
        pack();
    }

    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if(msg.equals("close"))
        {
            hide();
            btn.a = false;
        }
        else
        {
            super.wdgmsg(msg, args);
        }
    }

    public static final int margin3 = 4 * UI.scale(5);

    public class GroupSelector extends Widget {
        public int group;
        public GroupRect[] groups = new GroupRect[BuddyWnd.gc.length];

        public GroupSelector(int group) {
            super(new Coord(BuddyWnd.gc.length * margin3, margin3));
            this.group = group;
            for (int i = 0; i < BuddyWnd.gc.length; ++i) {
                groups[i] = new GroupRect(this, i, group == i);
                add(groups[i], new Coord(i * margin3, 0));
            }
        }

        protected void changed(int group) {
            NKinProp prop = NKinProp.get(group);
            ring.a = prop.ring;
            alarm.a = prop.alarm;
            arrow.a = prop.arrow;
            hil.a = prop.hideinlist;
        }

        public void update(int group) {
            if(group == this.group)
                return;
            if(this.group >= 0)
                groups[this.group].unselect();
            this.group = group;
            if(group >= 0)
                groups[group].select();
        }

        public void select(int group) {
            update(group);
            changed(group);
        }
    }

    public static class GroupRect extends Widget {
        final private static Coord offset = UI.scale(new Coord(2, 2));
        final private static Coord selsz = UI.scale(new Coord(19, 19));
        final private static Coord colsz = selsz.sub(offset.mul(2));
        final private GroupSelector selector;
        final private int group;
        private boolean selected;

        public GroupRect(GroupSelector selector, int group, boolean selected) {
            super(new Coord(margin3, margin3));
            this.selector = selector;
            this.group = group;
            this.selected = selected;
        }

        public void draw(GOut g) {
            if (selected) {
                g.chcolor(Color.LIGHT_GRAY);
                g.frect(Coord.z, selsz);
            }
            g.chcolor(BuddyWnd.gc[group]);
            g.frect(offset, colsz);
            g.chcolor();
        }

        public boolean mousedown(Coord c, int button) {
            selector.select(group);
            return (true);
        }

        public void select() {
            selected = true;
        }

        public void unselect() {
            selected = false;
        }
    }
}
