package nurgling.widgets;

import haven.*;
import nurgling.overlays.NTexMarker;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;

import java.util.ArrayList;

import java.util.*;

public class SearchWidget extends Window {
    private final TextEntry input;
    private final Button findBtn;
    private final Button cancelBtn;

    private final List<Gob.Overlay> searchOverlays = new ArrayList<>();
    private ArrayList<Gob> gobs = null;

    public SearchWidget(String title) {
        super(UI.scale(new Coord(320, 60)), title);

        int margin = UI.scale(10);

        input = add(new TextEntry(UI.scale(200), ""), new Coord(margin, margin));

        findBtn = add(new Button(UI.scale(60), "Find") {
            @Override
            public void click() {
                doSearch();
            }
        }, input.pos("ur").adds(UI.scale(10), 0));

        cancelBtn = add(new Button(UI.scale(60), "Cancel") {
            @Override
            public void click() {
                cleanupAndHide();
            }
        }, findBtn.pos("ur").adds(UI.scale(10), 0));

        pack();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            cleanupAndHide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean keydown(KeyDownEvent ev) {
        if (ev.code == java.awt.event.KeyEvent.VK_ESCAPE) {
            cleanupAndHide();
            return true;
        }

        if (ev.code == java.awt.event.KeyEvent.VK_ENTER) {
            doSearch();
            return true;
        }

        return super.keydown(ev);
    }

    private void doSearch() {
        removeSearchOverlays();

        if(input.text().isEmpty()) {
            return;
        }

        gobs = Finder.findGobs(new NAlias(input.text()));
        for (Gob gob : gobs) {
            NTexMarker marker = new NTexMarker(gob, new TexI(Resource.loadsimg("nurgling/hud/buttons/down_v2/u")), () -> false);
            Gob.Overlay overlay = new Gob.Overlay(gob, marker);
            gob.addol(overlay, true);
            searchOverlays.add(overlay);
        }
    }

    public void cleanupAndHide() {
        removeSearchOverlays();
        input.settext("");
        gobs = null;
        hide();
    }

    private void removeSearchOverlays() {
        for (Gob.Overlay overlay : searchOverlays) {
            overlay.remove();
        }
        searchOverlays.clear();
    }
}
