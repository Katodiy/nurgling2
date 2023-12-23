package nurgling.overlays;

import haven.*;
import static haven.Resource.imgc;
import haven.render.*;
import haven.res.ui.rbuff.*;
import nurgling.*;
import nurgling.iteminfo.*;

import java.awt.*;
import java.util.*;

public class NRelation extends Sprite implements RenderTree.Node, PView.Render2D
{
	static final TexI eq = new TexI(Resource.loadsimg("marks/agifactor/eq"));
	static final TexI uk = new TexI(Resource.loadsimg("marks/agifactor/uk"));
	static final TexI l1 = new TexI(Resource.loadsimg("marks/agifactor/l1"));
	static final TexI l2 = new TexI(Resource.loadsimg("marks/agifactor/l2"));
	static final TexI l3 = new TexI(Resource.loadsimg("marks/agifactor/l3"));
	static final TexI g1 = new TexI(Resource.loadsimg("marks/agifactor/g1"));
	static final TexI g2 = new TexI(Resource.loadsimg("marks/agifactor/g2"));
	static final TexI g3 = new TexI(Resource.loadsimg("marks/agifactor/g3"));
	TexI mip = null;
	TexI eip = null;
	Coord3f pos = new Coord3f(0, 0, 25);
	TexI stance = null;

	double duration_left = 0;
	double sub_duration_left = 0;
	TexI duration_lefttex = null;
	TexI agidelta = uk;
	double bar_delta = 0;
	double bar_sub_delta = 0;

	public static class RelBuff
	{
		public TexI bg;
		public TexI text;

		public RelBuff( TexI text,TexI bg)
		{
			this.bg = bg;
			this.text = text;
		}
	}
	ArrayList<RelBuff> buffs = new ArrayList<>();
	Gob gob;

	static int delta;
	public static HashMap<Integer, TexI> corn = new HashMap<>();
	static
	{
		for (int i =0 ; i <= 10; i ++)
			corn.put(i,new TexI(Resource.loadsimg("nurgling/hud/openings/red/" + String.valueOf(i))));
		delta = corn.get(0).sz().x;
	}

	public static HashMap<Integer, TexI> dizz = new HashMap<>();
	static
	{
		for (int i =0 ; i <= 10; i ++)
			dizz.put(i,new TexI(Resource.loadsimg("nurgling/hud/openings/blue/" + String.valueOf(i))));
	}

	public static HashMap<Integer, TexI> reel = new HashMap<>();
	static
	{
		for (int i =0 ; i <= 10; i ++)
			reel.put(i,new TexI(Resource.loadsimg("nurgling/hud/openings/yellow/" + String.valueOf(i))));
	}

	public static HashMap<Integer, TexI> gren = new HashMap<>();
	static
	{
		for (int i =0 ; i <= 10; i ++)
			gren.put(i,new TexI(Resource.loadsimg("nurgling/hud/openings/green/" + String.valueOf(i))));
	}

	public NRelation(Gob gob)
	{
		super(gob, null);
		this.gob = gob;
	}


	@Override
	public void gtick(Render g)
	{
		super.gtick(g);
	}


	@Override
	public boolean tick(double dt)
	{
		int size = NUtils.getGameUI().fv.lsrel.size();
		buffs.clear();
		bar_delta = 0;
		bar_sub_delta = 0;
		duration_left = 0;
		sub_duration_left = 0;
		for(int i = 0 ; i<size; i++)
		{
			if(NUtils.getGameUI().fv.lsrel.get(i)!=null && NUtils.getGameUI().fv.lsrel.get(i).gobid == gob.id)
			{
				Fightview.Relation rel = NUtils.getGameUI().fv.lsrel.get(i);
				mip = (TexI)NStyle.mip.render(String.valueOf(rel.ip)).tex();
				eip = (TexI)NStyle.eip.render(String.valueOf(rel.oip)).tex();
				for(Widget buff: rel.buffs.children())
				{
					if(buff instanceof Buff)
					{

							Buff b = (Buff) buff;
							if(b.res instanceof Session.CachedRes.Ref)
							{
								Session.CachedRes.Ref ref = ((Session.CachedRes.Ref)b.res);
								String resnm = ref.resnm();
								if(resnm!=null)
								{
									int val = (int) Math.min(10, b.ameter / 10);
									if (resnm.equals("paginae/atk/cornered"))
									{
										buffs.add(new RelBuff((TexI) NStyle.openings.render(String.valueOf(b.ameter)).tex(), corn.get(val)));
									}
									else if (resnm.equals("paginae/atk/dizzy"))
									{
										buffs.add(new RelBuff((TexI) NStyle.openings.render(String.valueOf(b.ameter)).tex(), dizz.get(val)));
									}
									else if (resnm.equals("paginae/atk/reeling"))
									{
										buffs.add(new RelBuff((TexI) NStyle.openings.render(String.valueOf(b.ameter)).tex(), reel.get(val)));
									}
									else if (resnm.equals("paginae/atk/offbalance"))
									{
										buffs.add(new RelBuff((TexI) NStyle.openings.render(String.valueOf(b.ameter)).tex(), gren.get(val)));
									}
									else
									{
										stance = new TexI(Resource.remote().loadwait(resnm).layer(imgc).img);
									}
								}

							}
					}
				}
				if(rel.duration>0)
				{
					duration_left = rel.actend - Utils.rtime();
					bar_delta = duration_left/rel.duration;
					duration_lefttex = (TexI) NStyle.openings.render(String.format("%.2f", duration_left)).tex();
				}
				else
				{
					duration_left = 0;
					if(rel.pairend!=null)
					{
						duration_left = rel.pairend.a - Utils.rtime();
						bar_delta = duration_left/rel.pairdur.a;
						duration_lefttex = (TexI) NStyle.openings.render(String.format("%.2f", duration_left)).tex();
						if(duration_left<0)
						{
							sub_duration_left = rel.pairend.b - Utils.rtime();
							bar_sub_delta = sub_duration_left/rel.pairdur.a;
						}
						else
						{
							sub_duration_left = rel.pairend.b - rel.pairend.a;
							bar_sub_delta = (rel.pairend.b - rel.pairend.a)/rel.pairdur.a;
						}
					}
				}

				if(rel.agi_delta!=-1)
				{
					if(rel.agi_delta > 1.09)
					{
						agidelta = l3;
					}
					else if (rel.agi_delta > 1.06)
					{
						agidelta = l2;
					}
					else if (rel.agi_delta > 1.032)
					{
						agidelta = l1;
					}
					else if (rel.agi_delta > 0.98)
					{
						agidelta = eq;
					}
					else if (rel.agi_delta > 0.96)
					{
						agidelta = g1;
					}
					else if (rel.agi_delta > 0.93)
					{
						agidelta = g2;
					}
					else
					{
						agidelta = g3;
					}
				}
				return false;
			}
		}
		return true;
	}

	@Override
	public void draw(GOut g, Pipe state)
	{

		Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
		Coord start = new Coord(sc);
		sc = sc.sub(((buffs.size()-1)*delta)/2, 0);

		for(RelBuff buff : buffs)
		{
			g.aimage(buff.bg, sc, 0.5, 0.5);
			g.aimage(buff.text, sc, 0.5, 0.5);
			sc.x += delta;
		}
		if(mip!=null)
		{
			g.aimage(mip, start.sub(UI.scale(30,35)), 0.5, 0.5);
		}

		if(eip!=null)
		{
			g.aimage(eip, start.add(UI.scale(30,-35)), 0.5, 0.5);
		}
		if(stance!=null)
		{
			g.aimage(stance, start.sub(UI.scale(0,35)).sub(stance.sz().div(2)),0,0);
		}
		Coord bar_pos = start.sub(UI.scale(30,65)).sub(UI.scale(10,10));
		if(duration_left>0)
		{
			g.chcolor(new Color(250, 143, 36, 190));
			g.frect(bar_pos,UI.scale((int)(80*bar_delta),20));
			bar_pos = bar_pos.add(UI.scale((int)(80*bar_delta),0));
			g.chcolor();

		}
		if(sub_duration_left>0)
		{
			g.chcolor(new Color(82, 159, 206, 190));
			g.frect(bar_pos,UI.scale((int)(80*bar_sub_delta),20));
			g.chcolor();
		}
		if(duration_left>0)
		{
			g.aimage(duration_lefttex, start.sub(UI.scale(0, 65)), 0.5, 0.5);
		}
		g.aimage(agidelta, start.add(UI.scale(50,-35)), 0.5, 0.5);
	}
}