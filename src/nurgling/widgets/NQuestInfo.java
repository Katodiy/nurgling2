package nurgling.widgets;

import haven.*;
import haven.Window;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.NStyle;
import nurgling.NUtils;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static haven.ItemInfo.catimgsh;
import static nurgling.widgets.NDraggableWidget.drawBg;

public class NQuestInfo extends Widget
{

    Text.Furnace fnd2 = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 14, Color.white).aa(true), 2, 1, Color.BLACK);
    Text.Furnace fnd1 = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 14, new Color(222, 205, 171)).aa(true), 2, 1, Color.BLACK);
    Text.Furnace gfnd2_under = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 14, new Color(222, 205, 171)).aa(true), 2, 1, Color.BLACK);
    public static final RichText.Foundry numfnd1 = new RichText.Foundry(new ChatUI.ChatParser(TextAttribute.FONT, Text.dfont.deriveFont(UI.scale(18f)), TextAttribute.FOREGROUND, Color.YELLOW));
    Text.Furnace active_title = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 18, new Color(217, 127, 59)).aa(true), 2, 1, new Color(94, 56, 56));
    Text.Furnace unactive_title = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 18, new Color(147, 131, 131)).aa(true), 2, 1, new Color(94, 56, 56));
    Text.Furnace credo_title = new PUtils.BlurFurn(new Text.Foundry(Text.sans, 18, new Color(126, 198, 194)).aa(true), 2, 1, new Color(94, 56, 56));

    public NQuestInfo() {
        super();
        lastUpdate.set(0);
        huntingT.clear();
        forageT.clear();
        Widget prev = add(modebtn = new NMiniMapWnd.NMenuCheckBox("nurgling/hud/buttons/questmode", null, "Switch mode"), UI.scale(margin.x)/2, UI.scale(margin.y)/2).changed(a -> {mode = (mode == Mode.QUESTGIVERS?Mode.TASKS:Mode.QUESTGIVERS);needUpdate.set(true);});
        add(hidebtn = new NMiniMapWnd.NMenuCheckBox("nurgling/hud/buttons/eye", null, "Hide credo"), prev.pos("ur")).changed(a -> {NConfig.set(NConfig.Key.hidecredo,a);needUpdate.set(true);});
        hidebtn.a = (boolean) NConfig.get(NConfig.Key.hidecredo);
    }
    NMiniMapWnd.NMenuCheckBox modebtn = null;
    NMiniMapWnd.NMenuCheckBox hidebtn = null;
    enum Mode
    {
        QUESTGIVERS,
        TASKS
    }


    Mode mode = Mode.QUESTGIVERS;
    private Collection<QuestImage> imgs = new ArrayList<>();
    HashMap<String,QuestGiver> qgconds = new HashMap<String,QuestGiver>();
    HashMap<Condition.State,Targets> taskconds = new HashMap<Condition.State,Targets>();
    private Tex glowon = null;
    public static final AtomicInteger lastUpdate = new AtomicInteger(0);
    private final Set<String> items = new HashSet<>();
    class Targets
    {
        ArrayList<Condition> conditions = new ArrayList<Condition>();
    }

    class QuestGiver
    {
        ArrayList<Condition> myConditions = new ArrayList<Condition>();
        ArrayList<Condition> otherConditions = new ArrayList<Condition>();

        int completed = 0;
        int uncompleted = 0;
    }

    public static boolean isHuntingTarget(String target)
    {
        if(target!=null)
            for(String ht:huntingT)
            {
                if(target.contains(ht))
                {
                    return true;
                }
            }
        return false;
    }

    public static boolean isForageTarget(String target)
    {
        if(target!=null)
            for(String ht:forageT)
            {
                if(target.contains(ht))
                {
                    return true;
                }
            }
        return false;
    }

    @Override
    public void dispose() {
        markers.clear();
        super.dispose();
    }

    void update() {
        imgs.clear();
        for (String qname : qgconds.keySet()) {
            setMarkersProp(qname, null);
        }
        qgconds.clear();
        taskconds.clear();
        huntingT.clear();
        items.clear();
        forageT.clear();
        for(Condition.State st: Condition.State.values()) {
            taskconds.put(st, new Targets());
        }
        QuestGiver credo = new QuestGiver();
        for (NQuest quest : quests.values()) {
            boolean isReady = true;
            for (Condition cond : quest.conditions) {
                Condition.QuestsGiver qg = null;
                if (cond.state == Condition.State.TELL) {
                    quest.questGiver = ((Condition.QuestsGiver) cond.attrs.get(Condition.QuestsGiver.class)).name;
                }
                else if ((qg = ((Condition.QuestsGiver) cond.attrs.get(Condition.QuestsGiver.class)))!=null)
                {
                    if (!qgconds.containsKey(qg.name)) {
                        qgconds.put(qg.name, new QuestGiver());
                    }
                }
                if(cond.state == Condition.State.BRING && !cond.ready)
                {
                    items.add(((Condition.BringItem) cond.attrs.get(Condition.BringItem.class)).name);
                }
                if(!cond.ready) {
                    if (cond.state == Condition.State.KILL) {
                        huntingT.add(((Condition.HuntTarget) cond.attrs.get(Condition.HuntTarget.class)).name);
                    } else if (cond.state == Condition.State.PICK) {
                        forageT.add(((Condition.PickTarget) cond.attrs.get(Condition.PickTarget.class)).name);
                    }
                    if (cond.state != Condition.State.TELL)
                        isReady = false;
                }
                if(cond.state!=null)
                {
                    switch (cond.state) {
                        case TELL:
                            break;
                        default: {
                            if(!cond.ready)
                                taskconds.get(cond.state).conditions.add(cond);
                        }
                    }
                }
            }
            if (quest.questGiver != null) {
                QuestGiver qg;
                if (!qgconds.containsKey(quest.questGiver)) {
                    qgconds.put(quest.questGiver, new QuestGiver());
                }
                qg = qgconds.get(quest.questGiver);

                if (isReady) {
                    qg.completed++;
                } else {
                    qg.uncompleted++;
                }
            }
        }

        for (NQuest quest : quests.values()) {
            for (Condition cond : quest.conditions) {
                if (quest.questGiver != null) {
                    if (qgconds.containsKey(quest.questGiver)) {
                        qgconds.get(quest.questGiver).myConditions.add(cond);
                    }
                    Condition.QuestsGiver qg = (Condition.QuestsGiver)cond.attrs.get(Condition.QuestsGiver.class);
                    if(qg!=null && cond.state!= Condition.State.TELL)
                    {
                        qgconds.get(qg.name).otherConditions.add(cond);
                    }
                }
                else
                {
                    if(!(Boolean)NConfig.get(NConfig.Key.hidecredo))
                        credo.myConditions.add(cond);
                }
            }
        }
        for (String qname : qgconds.keySet()) {
            QuestGiver qg = qgconds.get(qname);
            HashSet<String> prop = new HashSet<>();
            if(qg.completed!=0)
            {
                prop.add("tell");
            }
            for(Condition cond : qg.otherConditions) {
                if(!cond.ready) {
                    if (cond.state == Condition.State.BRING) {
                        prop.add("bring");
                    } else if (cond.state == Condition.State.GREET || cond.state == Condition.State.VISIT) {
                        prop.add("greet");
                    } else if (cond.state == Condition.State.RAGE) {
                        prop.add("rage");
                    } else if (cond.state == Condition.State.WAVE) {
                        prop.add("wave");
                    } else if (cond.state == Condition.State.LAUGH) {
                        prop.add("laugh");
                    }
                }
            }
            setMarkersProp(qname, prop);
        }
        if (mode == Mode.QUESTGIVERS) {
            if(!credo.myConditions.isEmpty()) {
                imgs.add(new QuestImage(credo_title.render("Credo:").img, -1));
                for (Condition cond : credo.myConditions)
                {
                    imgs.add(new QuestImage(fnd1.render(cond.target).img, cond.questId));
                }
            }
            for (String qname : qgconds.keySet()) {
                QuestGiver qg = qgconds.get(qname);
                if (!qg.myConditions.isEmpty()) {
                    imgs.add(new QuestImage(catimgsh(5, active_title.render(qname).img, numfnd1.render(String.format("($col[128,255,128]{%d}|$col[255,128,128]{%d})", qg.completed, qg.uncompleted)).img), qg.myConditions.get(0).questId));
                } else {
                    if (!qg.otherConditions.isEmpty()) {
                        for (Condition cond : qg.otherConditions)
                        {
                            if(!cond.ready)
                            {
                                imgs.add(new QuestImage(unactive_title.render(qname).img, cond.questId));
                                break;
                            }
                        }
                    }
                }
                for (Condition cond : qg.myConditions) {
                    if (cond.state != Condition.State.TELL && !cond.ready)
                        imgs.add(new QuestImage(fnd1.render(cond.target).img, cond.questId));
                }
                for (Condition cond : qg.otherConditions) {
                    if(!cond.ready)
                        imgs.add(new QuestImage(fnd2.render(cond.target).img, cond.questId));
                }
            }
        } else if (mode == Mode.TASKS) {
            addTargets("Bring", Condition.State.BRING);
            addTargets("Foraging:", Condition.State.PICK);
            addTargets("Hunting:", Condition.State.KILL);
            addTargets("Conversation:", Condition.State.GREET, Condition.State.VISIT, Condition.State.RAGE, Condition.State.WAVE, Condition.State.LAUGH);
            addTargets("Attributes:", Condition.State.GAIN);
            addTargets("Craft:", Condition.State.CREATE);
            addTargets("Other:", Condition.State.CAVE, Condition.State.LIGHT);
        }
        if (!imgs.isEmpty()) {
            glowon = new TexI(ncatimgs(1, imgs.toArray(new QuestImage[0])));
            Coord rsz = new Coord(glowon.sz().x, glowon.sz().y).add(UI.scale(this.margin).mul(2)).add(new Coord(0, modebtn.sz.y));
            rsz.y = Math.min(NUtils.getGameUI().sz.y - NDraggableWidget.delta.y,rsz.y);
            resize(rsz);
        } else {
            glowon = null;
            Coord nsz = UI.scale(this.margin).mul(2).add(new Coord(0, modebtn.sz.y));
            Coord rsz = new Coord(Math.max(nsz.x, modebtn.sz.x + margin.x * 2), Math.max(nsz.y, modebtn.sz.y + margin.y * 2));
            rsz.y = Math.min(NUtils.getGameUI().sz.y - NDraggableWidget.delta.y,rsz.y);
            resize(rsz);
        }
        if (parent != null)
            parent.resize(sz.add(NDraggableWidget.delta));
        needUpdate.set(false);
        lastUpdate.set(lastUpdate.get()+1);
    }

    static class QuestImage {
        public Pair<Coord, Coord> area = new Pair<>(new Coord(), new Coord());
        public BufferedImage img;
        public int id;

        public QuestImage(BufferedImage img, int id) {
            this.img = img;
            this.id = id;
        }
    }

    void addTargets(String name, Condition.State... states) {
        if(states.length>0) {
            boolean notEmpty = false;
            for (Condition.State state : states) {
                Targets cand = taskconds.get(state);
                if (cand != null && !cand.conditions.isEmpty()) {
                    notEmpty = true;
                    break;
                }
            }
            if(!notEmpty)
                return;
            imgs.add(new QuestImage(active_title.render(name).img, -1));
            for (Condition.State state : states) {
                Targets cand = taskconds.get(state);
                if (cand != null) {
                    for (Condition condition : cand.conditions) {
                        imgs.add(new QuestImage(gfnd2_under.render(condition.target).img, condition.questId));
                    }
                }
            }
        }
    }

    private BufferedImage ncatimgs(int margin, QuestImage... imgs) {
        int w = 0, h = -margin;
        for (QuestImage img : imgs) {
            if (img == null)
                continue;
            if (img.img.getWidth() > w)
                w = img.img.getWidth();
            h += img.img.getHeight() + margin;
        }
        BufferedImage ret = TexI.mkbuf(new Coord(w, h));
        Graphics g = ret.getGraphics();
        int y = 0;
        for (QuestImage img : imgs) {
            if (img == null)
                continue;
            img.area.a.x = 0;
            img.area.a.y = y;
            g.drawImage(img.img, 0, y, null);
            y += img.img.getHeight() + margin;
            img.area.b.x = img.img.getWidth();
            img.area.b.y = y - margin;
        }
        g.dispose();
        return (ret);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        Coord pos = new Coord(c.x, c.y).sub(UI.scale(this.margin)).sub(new Coord(0,modebtn.sz.y));
        if (imgs != null) {
            for (QuestImage img : imgs) {
                if (img.id >= 0) {
                    if (img.area.a.x <= pos.x && pos.x <= img.area.b.x && img.area.a.y <= pos.y && pos.y <= img.area.b.y) {
                        NUtils.getGameUI().chrwdg.show();
                        NUtils.getGameUI().chrwdg.questtab.showtab();
                        NUtils.getGameUI().chrwdg.wdgmsg("qsel", img.id);
                        return true;
                    }
                }
            }
        }
        return super.mousedown(c, button);
    }

    AtomicBoolean needUpdate = new AtomicBoolean(false);

    public static final HashSet<String> huntingT = new HashSet<>();
    public static final HashSet<String> forageT = new HashSet<>();

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if(!forRemove.isEmpty())
        {
            for(Integer i : forRemove)
            {
                quests.remove(i);
            }
            forRemove.clear();
            needUpdate.set(true);
        }
        for(NQuest q : quests.values())
        {
            if(!q.request && q.conditions.isEmpty()) {
                q.request = true;
                NUtils.getGameUI().chrwdg.wdgmsg("qsel", q.id);
            }
        }
        if(needUpdate.get())
            update();
    }
    Coord margin = new Coord(10,10);
    public static final IBox pbox = Window.wbox;
    @Override
    public void draw(GOut g) {
        Coord margin = UI.scale(this.margin);
        if (glowon != null) {
            drawBg(g.reclip(new Coord(0,modebtn.sz.y), glowon.sz().add(margin.mul(2))), glowon.sz().add(margin.mul(2)));
            pbox.draw(g, new Coord(0,modebtn.sz.y), glowon.sz().add(margin.mul(2)));

            g.image(glowon, margin.add(new Coord(0,modebtn.sz.y)));
        }
        super.draw(g);
    }

    public void updateConds(int id, Object[] args) {
        NQuest quest = quests.get(id);

        if(quest != null) {
            quest.request = false;
            quest.conditions.clear();
            int a = 0;
            while (a < args.length) {
                try {
                    if(args[a] instanceof String) {
                        String desc = (String) args[a++];
                        int st = Utils.iv(args[a++]);
                        String status = (String) args[a++];
                        Condition cond = new Condition(st != 0, desc, id, status);
                        quest.conditions.add(cond);
                    }
                    else
                        a++;
                }
                catch(ClassCastException e) {
                    int b = 1;
                }

            }
        }
        else
        {
            NUtils.getGameUI().error("NOT FOUND");
        }
        needUpdate.set(true);
    }


    public void removeQuest(int id) {
        synchronized (forRemove) {
            forRemove.add(id);
        }
    }

    final ArrayList<Integer> forRemove = new ArrayList<>();
    final HashMap<Integer, NQuest> quests = new HashMap<>();

    public void addQuest(int id) {
        synchronized (quests) {
            NQuest q = quests.put(id,new NQuest(id));
        }
    }

    static class NQuest
    {
        public boolean request = false;
        int id;
        ArrayList<Condition> conditions = new ArrayList<Condition>();
        String questGiver = null;
        public NQuest(int id) {
            this.id = id;
        }
    }

    static class Condition{
        boolean ready;
        String target;
        State state;
        int questId;
        enum State
        {
            TELL,
            KILL,
            PICK,
            BRING,
            VISIT,
            GREET,
            LAUGH,
            RAGE,
            WAVE,
            GAIN,
            CAVE,
            LIGHT,
            CREATE
        }

        public Condition(boolean ready, String target, int questId, String status) {
            this.ready = ready;
            this.target = target;
            this.questId = questId;

            if (target.contains("Bring"))
            {
                this.state = State.BRING;
                attrs.put(QuestsGiver.class, new QuestsGiver(target));
                attrs.put(BringItem.class, new BringItem(target));
//                bring_t.add(new Task(qid, c));
            }
            else if (target.contains("Pick"))
            {
                this.state = State.PICK;
                attrs.put(PickTarget.class, new PickTarget(target));
            }
            else if (target.contains("Kill") || target.contains("Raid") || target.contains("Defeat") ) {
                this.state = State.KILL;
                attrs.put(HuntTarget.class, new HuntTarget(target));
            }
            else if (target.contains("Catch"))
            {
                this.state = State.PICK;
                attrs.put(PickTarget.class, new PickTarget(target));
            }
            else if (target.contains("Greet") || (target.contains("Visit") && !target.contains("cave")) || target.contains("wave") || target.contains("laugh") || target.contains("rage"))
            {
                attrs.put(QuestsGiver.class, new QuestsGiver(target));
                if(target.contains("Greet") || (target.contains("Visit") && !target.contains("cave")))
                {
                    this.state = State.GREET;
                }
                else if(target.contains("wave"))
                {
                    this.state = State.WAVE;
                }
                else if(target.contains("laugh"))
                {
                    this.state = State.LAUGH;
                }
                else if(target.contains("rage"))
                {
                    this.state = State.RAGE;
                }
            }
            else if (target.contains("Gain"))
            {
                this.state = State.GAIN;
            }
            else if (target.contains("Create"))
            {
                this.state = State.CREATE;
            }
            else if (target.contains("Tell"))
            {
                this.state = State.TELL;
                attrs.put(QuestsGiver.class,new QuestsGiver(target));
            }
            else if (target.contains("cave"))
            {
                this.state = State.CAVE;
            }
            else if (target.contains("Light"))
            {
                this.state = State.LIGHT;
            }
            if(status!=null)
            {
                this.target += " " + status;
            }
        }

        class QuestsGiver
        {
            public String name;

            public QuestsGiver(String info) {
                if (info.contains("Tell") || (info.contains("Visit") && !info.contains("cave"))) {
                    name = info.contains("Tell") ? info.substring(5, info.indexOf(" ", 6)) : info.substring(6);
                }
                else
                {
                    if (info.contains("Greet") || (info.contains("Visit") && !info.contains("cave"))) {
                        name = info.substring(6);
                    } else if (info.contains(" to ")) {
                        name = info.substring(info.indexOf(" to ") + 4);
                    } else if (info.contains(" at ")) {
                        name = info.substring(info.indexOf(" at ") + 4);
                    }
                }
            }
        }

        class BringItem
        {
            public String name = null;

            public BringItem(String info) {
                if( info.contains("Bring")){

                    if(info.contains(" a ") || info.contains(" an ")) {
                        if(info.contains(" a "))
                            name = info.substring(info.indexOf(" a ") + 3, info.indexOf("to ") - 1);
                        else
                            name = info.substring(info.indexOf(" an ") + 4, info.indexOf("to ") - 1);
                    }
                    else
                    {
                        name = info.substring(6, info.indexOf("to ") - 1);
                    }
                }
            }
        }

        class PickTarget
        {
            public String name;

            public PickTarget(String info) {
                info = info.toLowerCase();
                String bufname;
                int ind = info.indexOf(" a ");
                if (ind != -1)
                    bufname = info.substring(info.indexOf(" a ") + 3);
                else {
                    ind = info.indexOf(" an ");
                    if (ind != -1)
                        bufname = info.substring(info.indexOf(" an ") + 4);
                    else
                        bufname = info.substring(info.indexOf(" ") + 1);
                }

                if (!bufname.isEmpty()) {
                    if (bufname.contains("blueberr"))
                        bufname = "blueberr";
                    else if (bufname.contains("lingon"))
                        bufname = "lingon";
                    else if (bufname.contains("woodgrouse hen"))
                        bufname = "woodgrouse-f";
                    else if (bufname.contains("morel"))
                        bufname = "lorchel";
                    else if (bufname.contains("yellowf"))
                        bufname = "yellowf";
                    else if (bufname.contains("hen"))
                        bufname = "chicken/chicken";
                    else if (bufname.contains("cock"))
                        bufname = "chicken/roast";
                    else if (bufname.contains("chantrell"))
                        bufname = "herbs/chantrell";
                    else if (bufname.contains("rat"))
                        bufname = "rat/rat";
                    name = (bufname.replaceAll("\\s+", "")).replaceAll("'+", "");
                }
            }
        }

        class HuntTarget {
            public String name;

            public HuntTarget(String info) {
                info = info.toLowerCase();
                String bufname;
                int ind = info.indexOf(" a ");
                if (ind != -1)
                    bufname = info.substring(info.indexOf(" a ") + 3);
                else {
                    ind = info.indexOf(" an ");
                    if (ind != -1)
                        bufname = info.substring(info.indexOf(" an ") + 4);
                    else
                        bufname = info.substring(info.indexOf(" ") + 1);
                }

                if (!bufname.isEmpty()) {
                    if (bufname.contains("mouflon"))
                        bufname = "sheep";
                    else if (bufname.contains("auroch"))
                        bufname = "cattle";
                    else if (bufname.contains("horse"))
                        bufname = "horse/horse";
                    else if (info.contains("Raid a")) {
                        if (bufname.contains("bird"))
                            bufname = "nest";
                        else
                            bufname = "anthill";
                    }
                    else {
                        bufname += "kritter/";
                    }
                }
                name = (bufname.replaceAll("\\s+", "")).replaceAll("'+", "");
            }
        }


        public Map<Class<?>, Object> attrs = new HashMap<>();

        public <C> C getattr(Class<C> c) {
            Object attr = this.attrs.get(c);
            if(!c.isInstance(attr))
                return(null);
            return(c.cast(attr));
        }

    }

    public class MarkerInfo{
        public String name;
        public Coord2d coord;
        public long seg;
        public HashSet<String> prop;

        public MarkerInfo(String name, Coord2d coord, long seg) {
            this.name = name;
            this.coord = coord;
            this.seg = seg;
        }
    }

    final static HashSet<MarkerInfo> markers = new HashSet<>();
    public void addMarkerCoord(Coord2d tmp, String nm, long seg) {
        synchronized (markers) {
            for (MarkerInfo mi : markers) {
                if (mi.name.equals(nm)) {
                    mi.coord = tmp;
                    mi.seg = seg;
                    return;
                }
            }
            markers.add(new MarkerInfo(nm, tmp, seg));
        }
        lastUpdate.set(lastUpdate.get()+1);
    }

    public static MarkerInfo getMarkerInfo(Gob gob)
    {
        synchronized (markers) {
            for (MarkerInfo mi : markers) {
                if (NUtils.getGameUI().mapfile.playerSegmentId() == mi.seg) {
                    if (gob.rc.dist(mi.coord) < 1)
                        return (mi);
                }
            }
        }
        return(null);
    }

    void setMarkersProp(String name, HashSet<String> props)
    {
        synchronized (markers) {
            for (MarkerInfo mi : markers) {
                if (mi.name.equals(name)) {
                    mi.prop = props;
                    return;
                }
            }
            MarkerInfo mi = new MarkerInfo(name, null, -1);
            mi.prop = props;
            markers.add(mi);
            lastUpdate.set(lastUpdate.get()+1);
        }
    }

    public boolean isQuestedItem(NGItem item){
        for(String name : items)
        {
            if(item.name()!=null && item.name().toLowerCase().contains(name))
                return true;
        }

        return false;
    }
}