package nurgling.pf;

import haven.*;
import haven.Window;
import nurgling.*;

import java.awt.*;
import java.util.*;


public class NPFMap
{
    public boolean waterMode = false;
    public Cell[][] cells;
    // 1 hitbox
    // 0 have path
    // 2 unpathable tiles
    // 4 pf been here
    // 7 approach point (marked blue)
    // 8 pf line
    // 9 pf turn
    public Coord begin;
    Coord end;
    int dsize;
    public int size;
    long currentTransport = -1;

    public CellsArray addGob(Gob gob) {
        CellsArray ca;

        if (gob.ngob != null && gob.ngob.hitBox != null && (ca = gob.ngob.getCA()) != null && NUtils.player() != null && gob.id != NUtils.player().id && gob.getattr(Following.class) == null) {
            CellsArray old = new CellsArray(ca.x_len, ca.y_len);
            old.begin = ca.begin;
            old.end = ca.end;
            if (ca.end.x >= begin.x && ca.begin.x <= end.x &&
                    ca.end.y >= begin.y && ca.begin.y <= end.y) {
                for (int i = 0; i < ca.x_len; i++)
                    for (int j = 0; j < ca.y_len; j++) {
                        int ii = i + ca.begin.x - begin.x;
                        int jj = j + ca.begin.y - begin.y;
                        if (ii > 0 && (ii + 1) < size && jj > 0 && (jj + 1) < size) {
                            old.cells[i][j] = cells[ii][jj].val;

                            if (ca.cells[i][j] != 0) {
                                if (cells[ii][jj].val != 1)
                                    cells[ii][jj].val = ca.cells[i][j];
                                cells[ii][jj].content.add(gob.id);
                            }
                        }
                    }
            }
            return old;
        }
        return null;
    }

    public void setCellArray(CellsArray ca) {
        if (ca.end.x >= begin.x && ca.begin.x <= end.x &&
                ca.end.y >= begin.y && ca.begin.y <= end.y) {
            for (int i = 0; i < ca.x_len; i++)
                for (int j = 0; j < ca.y_len; j++) {
                    int ii = i + ca.begin.x - begin.x;
                    int jj = j + ca.begin.y - begin.y;
                    if (ii > 0 && (ii + 1) < size && jj > 0 && (jj + 1) < size) {
                        cells[ii][jj].val = ca.cells[i][j];
                        //TODO placeable vs passable
                        cells[ii][jj].fullVal.force((ca.cells[i][j] == 0) ? CellType.Placeable : CellType.Blocked);
                    }
                }
        }
    }

    public enum  CellType {
        Land((byte) 0, (byte) -1, -1),
        Bog((byte) 1, (byte) -1, -1),
        Shallow((byte) 2, (byte) -1, -1),
        ShallowOcean((byte) 3, (byte) -1, -1),
        Deep((byte) 4, (byte) -1, -1),
        DeepOcean((byte) 5, (byte) -1, -1),
        OpenSea((byte) 6, (byte) -1, -1),

        Placeable((byte) -1, (byte) 0, -1),
        Passable((byte) -1, (byte) 1, -1),
        Blocked((byte) -1, (byte) 2, -1),
        Forbidden((byte) -1, (byte) 3, -1),

        PavedFloor((byte) -1, (byte) -1, 1),
        GrassFloor((byte) -1, (byte) -1, 0.8),
        ForestFloor((byte) -1, (byte) -1, 0.6),
        SubmergedWalk((byte) -1, (byte) -1, 0.2),

        Default((byte) 0, (byte) 0, 1);

        CellType(byte land, byte obstruct, double moveSpeed) {
            landType = land;
            obstructionState = obstruct;
            speedK = moveSpeed;
        }

        void cumulate(CellType other) {
            if (other.landType != -1)
                if (landType < other.landType)
                    landType = other.landType;
            if (other.obstructionState != -1)
                if (obstructionState < other.obstructionState)
                    obstructionState = other.obstructionState;
            if (other.speedK != -1)
                if (speedK > other.speedK)
                    speedK = other.speedK;
        }
        void force(CellType other) {
            if (other.landType != -1)
                landType = other.landType;
            if (other.obstructionState != -1)
                obstructionState = other.obstructionState;
            if (other.speedK != -1)
                speedK = other.speedK;
        }

        private byte landType;
        //0 walkable
        //1 bog
        //2 shallow
        //3 shallow ocean
        //4 deep
        //5 deep ocean
        //6 open sea

        private byte obstructionState;
        //0 passable & placeable
        //1 non-placeable
        //2 blocked by hit-box
        //3 blocked by immovable object

        private double speedK;
        public short pfColour = -1;
        //0 traversable
        //1

        public boolean isPfVisited() {
            return pfVisited;
        }

        public void visitedByPf() {
            this.pfVisited = true;
        }


        private boolean pfVisited = false;

        public boolean isPlace_able() {
            return ((landType == 0) && (obstructionState < 1));
        }

        public boolean isWalk_able() {
            return ((landType <= 3) && (obstructionState < 2));
        }

        public boolean isSwim_able() {
            return ((landType >= 1) && (landType <= 6) && (obstructionState < 2));
        }

        public boolean isSail_able() {
            return ((landType >= 2) && (landType <= 6) && (obstructionState < 2));
        }

        public boolean isBlockedByMajorObject() {
            return (obstructionState == 2);
        }

        public boolean isBlockedByHitbox() {
            return (obstructionState == 3);
        }

        public boolean isBlocked() {
            return (obstructionState == 3) || (obstructionState == 2);
        }

        public double getMoveSpeed() {
            return speedK;
        }
    }

    public static class Cell
    {
        public Cell(Coord pos)
        {
            this.pos = pos;
        }

        public Coord pos;

        public short val;
        public CellType fullVal = CellType.Default;

        public ArrayList<Long> content = new ArrayList<>();
    }



    public NPFMap(Coord2d src, Coord2d tgt, int mul) throws InterruptedException {
        Coord2d a = new Coord2d(Math.min(src.x, tgt.x), Math.min(src.y, tgt.y));
        Coord2d b = new Coord2d(Math.max(src.x, tgt.x), Math.max(src.y, tgt.y));
        Coord center = Utils.toPfGrid((a.add(b)).div(2));
        dsize = Math.max(8,((int) Math.ceil(b.dist(a) / MCache.tilehsz.x)) * mul);
        size = 2 * dsize + 1;
        if(dsize>120) {
            NUtils.getGameUI().error("Unable to build grid of required size");
            throw new InterruptedException();
        }

        cells = new Cell[size][size];
        begin = center.sub(dsize,dsize);
        end = center.add(dsize,dsize);
        for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < size; j++)
            {
                cells[i][j] = new Cell( begin.add(i,j));
                if(i == 0 || j == 0 || i == size-1 || j == size-1)
                    cells[i][j].val=2;
            }
        }
    }

    public NPFMap(Coord2d src, Coord2d dst, int mul, boolean waterMode)throws InterruptedException
    {
        this(src,dst,mul);
        this.waterMode = waterMode;
    }

    public Coord getBegin()
    {
        return begin;
    }

    public Coord getEnd()
    {
        return end;
    }

    public Cell[][] getCells()
    {
        return cells;
    }

    public int getSize()
    {
        return size;
    }

    public void build()
    {
        if(NUtils.playerID()!=-1) {
            Following fl = NUtils.player().getattr(Following.class);
            if(fl!= null)
            {
                currentTransport = fl.tgt;
            }
        }
        synchronized (NUtils.getGameUI().ui.sess.glob.oc)
        {

            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
            {
                if(gob.id!=currentTransport)
                    addGob(gob);
            }
        }
        for (int i = 0; i < size; i += 1)
        {
            for (int j = 0; j < size; j += 1)
            {

                if (cells[i][j].val == 0)
                {
                    ArrayList<Coord> cand = new ArrayList<>();
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos).add(new Coord2d(-MCache.tileqsz.x,MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos).add(new Coord2d(MCache.tileqsz.x,-MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos).add(new Coord2d(-MCache.tileqsz.x,-MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos).add(new Coord2d(MCache.tileqsz.x,MCache.tileqsz.y))).div(MCache.tilesz).floor());

                    for(Coord c : cand) {
                        String name = NUtils.getGameUI().ui.sess.glob.map.tilesetname(NUtils.getGameUI().ui.sess.glob.map.gettile(c));
                        if(!waterMode) {
                            if (name != null && (name.startsWith("gfx/tiles/cave") || name.startsWith("gfx/tiles/rocks") || name.equals("gfx/tiles/deep") || name.equals("gfx/tiles/odeep"))) {
                                cells[i][j].val = 2;
                            }
                        }
                        else
                        {
                            if (name != null && !(name.startsWith("gfx/tiles/water") || name.startsWith("gfx/tiles/owater") || name.equals("gfx/tiles/deep") || name.equals("gfx/tiles/odeep"))) {
                                cells[i][j].val = 2;
                            }
                        }
                    }
                }
            }
        }
    }

    public ArrayList<Coord> checkCA(CellsArray ca) {
        ArrayList<Coord> result = new ArrayList<>();
        if ((ca.begin.x >= begin.x && ca.begin.x <= end.x ||
                ca.end.x >= begin.x && ca.end.x <= end.x) &&
                (ca.begin.y >= begin.y && ca.begin.y <= end.y ||
                        ca.end.y >= begin.y && ca.end.y <= end.y))
        {
            for (int i = 0; i < ca.x_len; i++)
                for (int j = 0; j < ca.y_len; j++)
                {
                    int ii = i + ca.begin.x - begin.x;
                    int jj = j + ca.begin.y - begin.y;
                    if (ii > 0 && ii < size && jj > 0 && jj < size)
                    {
                        if(ca.cells[i][j] != 0 && cells[ii][jj].val !=0)
                        {
                            result.add(new Coord(ii,jj));
                        }
                    }
                }
        }
        return result;
    }

    public static Window wnd = null;
    public static void print(int size, Cell[][] cells)
    {
        if(NUtils.getUI().core.debug)
        {
            Coord csz = new Coord(UI.scale(10), UI.scale(10));
            if(wnd!=null)
                wnd.destroy();
            wnd = NUtils.getUI().root.add(new Window(new Coord(size * UI.scale(10), size * UI.scale(10)), "PFMAP")
            {
                @Override
                public void draw(GOut g)
                {
                    super.draw(g);
                    for (int i = 0; i < size; i++)
                    {
                        for (int j = size - 1; j >= 0; j--)
                        {
                            if (cells[i][j].val == 1) {
                                g.chcolor(Color.RED);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else if (cells[i][j].val == 0)
                                g.chcolor(Color.GREEN);
                            else if (cells[i][j].val == 4)
                            {
                                g.chcolor(Color.YELLOW);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else if (cells[i][j].val == 7)
                                g.chcolor(Color.BLUE);
                            else if (cells[i][j].val == 8)
                            {
                                g.chcolor(Color.MAGENTA);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else if (cells[i][j].val == 9)
                            {
                                g.chcolor(Color.CYAN);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else
                                g.chcolor(Color.BLACK);
                            g.rect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                        }
                    }
                }

                public void wdgmsg(Widget sender, String msg, Object... args)
                {
                    if ((sender == this) && (msg == "close"))
                    {
                        destroy();
                    }
                    else
                    {
                        super.wdgmsg(sender, msg, args);
                    }
                }

            }, new Coord(UI.scale(100), UI.scale(100)));
            NUtils.getUI().bind(wnd, 7002);
        }
    }
}
