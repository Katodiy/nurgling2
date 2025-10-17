package nurgling.actions.bots;

import haven.*;
import nurgling.NGameUI;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.actions.Action;
import nurgling.actions.Results;
import nurgling.areas.NArea;
import nurgling.overlays.NCustomBauble;

import java.awt.image.BufferedImage;

public class SelectAreaWithRotation implements Action {

    public SelectAreaWithRotation() {

    }

    public SelectAreaWithRotation(BufferedImage image) {
        this.image = image;
    }

    public SelectAreaWithRotation(BufferedImage image, BufferedImage Spr) {
        this.image = image;
        this.spr = Spr;
    }

    BufferedImage image = null;
    BufferedImage spr = null;
    public NArea.Space result;
    public boolean isRotated = false; // User's rotation choice
    private DirectionButton dirButton = null;

    @Override
    public Results run(NGameUI gui) throws InterruptedException {

        if (!((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.get()) {
            Gob player = NUtils.player();
            ((NMapView) NUtils.getGameUI().map).isAreaSelectionMode.set(true);

            // Add direction button to the UI
            dirButton = new DirectionButton();
            gui.add(dirButton, gui.sz.x - 150, 10); // Top-right corner

            if(image!=null && player!=null) {
                player.addcustomol(new NCustomBauble(player,image, spr,((NMapView) NUtils.getGameUI().map).isAreaSelectionMode));
            }

            nurgling.tasks.SelectArea sa;
            NUtils.getUI().core.addTask(sa = new nurgling.tasks.SelectArea());
            if (sa.getResult() != null) {
                result = sa.getResult();
                isRotated = dirButton.isRotated;
            }

            // Clean up button
            if(dirButton != null) {
                dirButton.destroy();
                dirButton = null;
            }
        }
        else {
            return Results.FAIL();
        }
        return Results.SUCCESS();
    }

    public Pair<Coord2d,Coord2d> getRCArea() {
        Coord begin = null;
        Coord end = null;
        for (Long id : result.space.keySet()) {
            MCache.Grid grid = NUtils.getGameUI().map.glob.map.findGrid(id);
            haven.Area area = result.space.get(id).area;
            Coord b = area.ul.add(grid.ul);
            Coord e = area.br.add(grid.ul);
            begin = (begin != null) ? new Coord(Math.min(begin.x, b.x), Math.min(begin.y, b.y)) : b;
            end = (end != null) ? new Coord(Math.max(end.x, e.x), Math.max(end.y, e.y)) : e;
        }
        if (begin != null)
            return new Pair<Coord2d, Coord2d>(begin.mul(MCache.tilesz), end.sub(1, 1).mul(MCache.tilesz).add(MCache.tilesz));
        return null;
    }

    public boolean getRotation() {
        return isRotated;
    }

    /**
     * Direction button widget shown during area selection
     */
    private class DirectionButton extends Widget {
        private boolean isRotated = false;
        private Button btn;
        private Text directionText;

        public DirectionButton() {
            super(new Coord(140, 60));

            btn = new Button(120, "Rotate", this::toggle);
            add(btn, 10, 30);

            updateText();
        }

        private void toggle() {
            isRotated = !isRotated;
            updateText();
        }

        private void updateText() {
            String direction = isRotated ? "East-West" : "North-South";
            directionText = Text.render("Direction: " + direction);
        }

        @Override
        public void draw(GOut g) {
            // Draw background
            g.chcolor(0, 0, 0, 180);
            g.frect(Coord.z, sz);
            g.chcolor();

            // Draw direction text
            if(directionText != null) {
                g.image(directionText.tex(), new Coord(10, 5));
            }

            super.draw(g);
        }
    }
}
