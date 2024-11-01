package nurgling.widgets.bots;

import haven.*;
import haven.Label;
import haven.Window;
import haven.res.lib.itemtex.ItemTex;
import nurgling.*;
import nurgling.conf.NFishingSettings;
import nurgling.tools.VSpec;
import nurgling.widgets.NAreasWidget;
import nurgling.widgets.NCatSelection;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class FishingTarget extends Window {
    public static Text.Foundry fnd = new Text.Foundry(Text.sans.deriveFont(java.awt.Font.BOLD), 14).aa(true);
    public FishList fishList;
    NFishingSettings settings;
    public FishingTarget(NFishingSettings settings) {
        super(UI.scale(300,400), "Fish");
        this.settings = settings;
        add(fishList = new FishList(UI.scale(300,400)));
        items.addAll(VSpec.categories.get("Fish"));
        pack();
    }

    @Override
    public void show() {
        super.show();
    }

    private final ArrayList<JSONObject> items = new ArrayList<>();

    public class FishList extends SListBox<JSONObject, Widget> {
        FishList(Coord sz) {
            super(sz, UI.scale(32));
        }

        protected List<JSONObject> items() {
            synchronized (items) {
                return items;
            }
        }

        protected Widget makeitem(JSONObject item, int idx, Coord sz) {
            return(new ItemWidget<JSONObject>(this, sz, item){
                {
                    FishItem fi;
                    add(fi = new FishItem(item), Coord.z);
                    fi.enabled.a = settings.targets.contains(item.getString("name"));
                }
            });
        }


    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args)
    {
        if(msg.equals("close"))
            hide();
        else
        {
            super.wdgmsg(sender, msg, args);
        }
    }

    public class FishItem extends Widget{
        Label text;
        CheckBox enabled;
        TexI icon;
        public FishItem(JSONObject fishObj){
            super(UI.scale(300,32));
            this.enabled = add(new CheckBox(""){
                @Override
                public void set(boolean a) {
                    super.set(a);
                    if(a)
                    {
                        if(!settings.targets.contains(text.text()))
                            settings.targets.add(text.text());
                    }
                    else {
                        settings.targets.remove(text.text());
                    }
                }
            }, UI.scale(0,9));
            this.text = add(new Label(fishObj.getString("name"), fnd),enabled.pos("ur").adds(UI.scale(100,-4)));
            this.icon = new TexI(ItemTex.create(fishObj));
        }



        @Override
        public void draw(GOut g) {
            super.draw(g);
            int desiredHeight = UI.scale(32);
            int scaledWidth = UI.scale((int) (icon.sz().x * desiredHeight / icon.sz().y));
            g.image(icon, new Coord(enabled.sz.x,0).add(UI.scale( 1,0)), new Coord(scaledWidth,desiredHeight));
        }
    }

}
