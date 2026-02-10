package nurgling.widgets;


import haven.*;
import nurgling.NConfig;
import nurgling.NUtils;
import nurgling.i18n.L10n;

public class NEditFolderName extends Window {
    private final TextEntry te;
    String rootPath;
    String path;
    boolean isCreating = false;
    private final NAreasWidget areasWidget;

    public NEditFolderName(NAreasWidget areasWidget) {
        super(UI.scale(new Coord(260, 100)), L10n.get("edit.folder_name"));
        this.areasWidget = areasWidget;

        prev = add(te = new TextEntry(UI.scale(200), ""), UI.scale(5, 5));
        add(new Button(UI.scale(60), L10n.get("edit.save")) {
            @Override
            public void click() {
                if (!te.text().isEmpty()) {
                    String newpath = rootPath + "/" + te.text().trim();
                    if(!isCreating)
                        areasWidget.changePath(newpath, rootPath + "/" + path);
                    path = newpath;
                    NConfig.needAreasUpdate();
                    areasWidget.showPath((isCreating)?path:rootPath);
                    NEditFolderName.this.hide();
                }
            }
        }, prev.pos("ur").adds(5, -6));

        add(new Button(UI.scale(60), L10n.get("edit.cancel")) {
            @Override
            public void click() {
                NEditFolderName.this.hide();
                // РџРѕРєР°Р·С‹РІР°РµРј РїСЂРµРґС‹РґСѓС‰РµРµ РѕРєРЅРѕ
                areasWidget.showPath(rootPath);
            }
        }, prev.pos("ur").adds(70, -6));

        pack();
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
        }
        else
        {
            super.wdgmsg(msg, args);
        }
    }

    public static void changeName(String rootPath, String path)
    {
        NUtils.getGameUI().nefn.isCreating = false;
        NUtils.getGameUI().nefn.te.settext(path);
        NUtils.getGameUI().nefn.setPath(path);
        NUtils.getGameUI().nefn.setRootPath(rootPath);
        NUtils.getGameUI().nefn.show();
        NUtils.getGameUI().nefn.raise();
        // Position relative to areas widget if it exists
        if(NUtils.getGameUI().nefn.areasWidget != null && NUtils.getGameUI().nefn.areasWidget.visible()) {
            NUtils.getGameUI().nefn.c = NUtils.getGameUI().nefn.areasWidget.c.add(
                (NUtils.getGameUI().nefn.areasWidget.sz.x - NUtils.getGameUI().nefn.sz.x) / 2,
                (NUtils.getGameUI().nefn.areasWidget.sz.y - NUtils.getGameUI().nefn.sz.y) / 2
            );
        }
    }

    public static void createFolder(String rootPath)
    {
        NUtils.getGameUI().nefn.isCreating = true;
        NUtils.getGameUI().nefn.setRootPath(rootPath);
        NUtils.getGameUI().nefn.setPath("");
        NUtils.getGameUI().nefn.show();
        NUtils.getGameUI().nefn.raise();
        // Position relative to areas widget if it exists
        if(NUtils.getGameUI().nefn.areasWidget != null && NUtils.getGameUI().nefn.areasWidget.visible()) {
            NUtils.getGameUI().nefn.c = NUtils.getGameUI().nefn.areasWidget.c.add(
                (NUtils.getGameUI().nefn.areasWidget.sz.x - NUtils.getGameUI().nefn.sz.x) / 2,
                (NUtils.getGameUI().nefn.areasWidget.sz.y - NUtils.getGameUI().nefn.sz.y) / 2
            );
        }
    }
}
