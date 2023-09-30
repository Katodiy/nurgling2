package nurgling.overlays;

import haven.*;
import haven.render.*;
import nurgling.*;

public class NCropMarker extends Sprite implements RenderTree.Node, PView.Render2D
{
	TexI img = null;
	Coord3f pos = new Coord3f(0, 0, 0);

	long currentStage = -2;
	NProperties.Crop crop;


	public NCropMarker(Gob gob)
	{
		super(gob, null);
		this.crop = NProperties.Crop.getCrop((Gob)owner);
		updateMarker();
	}

	void updateMarker()
	{

	}

	@Override
	public void gtick(Render g)
	{
		super.gtick(g);
	}


	@Override
	public boolean tick(double dt)
	{
		Gob gob = (Gob) owner;
		if ((Boolean) NConfig.get(NConfig.Key.showCropStage) && currentStage != gob.ngob.getModelAttribute())
		{
			if (gob.ngob.getModelAttribute() == crop.maxstage)
			{
				if (crop.maxstage == 0)
				{
					img = NStyle.iCropMap.get(NStyle.CropMarkers.GRAY);
				}
				else
				{
					img = NStyle.iCropMap.get(NStyle.CropMarkers.GREEN);
				}
			}
			else if (gob.ngob.getModelAttribute() == 0)
			{
				img = NStyle.iCropMap.get(NStyle.CropMarkers.RED);
			}
			else
			{
				if (crop.maxstage > 1 && crop.maxstage < 7)
				{
					if (gob.ngob.getModelAttribute() == crop.specstage)
					{
						img = NStyle.iCropMap.get(NStyle.CropMarkers.BLUE);
					}
					else
					{
						img = NStyle.getCropTexI( gob.ngob.getModelAttribute(), crop.maxstage);
					}
				}
			}
			currentStage = gob.ngob.getModelAttribute();
		}
		return super.tick(dt);
	}

	@Override
	public void draw(GOut g, Pipe state)
	{
		if ((Boolean) NConfig.get(NConfig.Key.showCropStage) && img != null)
		{
			Coord sc = Homo3D.obj2view(pos, state, Area.sized(g.sz())).round2();
			g.aimage(img, sc, 0.5, 0.5);
		}
	}
}