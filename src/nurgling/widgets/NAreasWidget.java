package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.tools.*;

import java.util.*;
import java.util.concurrent.*;

public class NAreasWidget extends Window
{
    public Dropbox adrop;

    public IngredientContainer in_items;
    public IngredientContainer out_items;
    public NAreasWidget()
    {
        super(UI.scale(new Coord(600,500)), "Areas Settings");
        prev = add(new Button(UI.scale(150), "Create area"){
            @Override
            public void click()
            {
                super.click();
                NUtils.getGameUI().msg("Please, select area");
                new Thread(new NAreaSelector(NAreaSelector.Mode.CREATE)).start();
            }
        });

        Widget change = add(new Button(UI.scale(150), "Change area"){
            @Override
            public void click()
            {
                super.click();
                if(adrop.sel!=null)
                {
                    ((NMapView)NUtils.getGameUI().map).changeArea((String) adrop.sel);
                }
            }
        }, prev.pos("ur").adds(5, 0));

        add(new Button(UI.scale(150), "Remove area"){
            @Override
            public void click()
            {
                super.click();
                if(adrop.sel!=null)
                {
                    ((NMapView)NUtils.getGameUI().map).removeArea((String) adrop.sel);
                    NConfig.needAreasUpdate();
                    adrop.sel = null;
                }
            }
        }, change.pos("ur").adds(5, 0));

        prev = add(new AreaList(UI.scale(new Coord(300,200))), prev.pos("bl").adds(0, 10));

        prev = adrop = add(new Dropbox<String>(UI.scale(200), 5, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                return new LinkedList<>(((NMapView)NUtils.getGameUI().map).areas()).get(i);
            }

            @Override
            protected int listitems() {
                return ((NMapView)NUtils.getGameUI().map).areas().size();
            }

            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }

            @Override
            public void change(String item) {
                super.change(item);
                ((NMapView)NUtils.getGameUI().map).selectArea(item);
            }
        }, prev.pos("bl").adds(0, 10));

        add(new Button(UI.scale(100), "Edit name"){
            @Override
            public void click()
            {
                super.click();
                if(adrop.sel!=null)
                {
                    NArea area = ((NMapView)NUtils.getGameUI().map).findArea((String)adrop.sel);
                    NEditAreaName.changeName(area);
                }
            }
        }, prev.pos("ur").adds(5, -10));

        prev = add(in_items = new IngredientContainer(), prev.pos("bl").add(0,5));
        add(out_items = new IngredientContainer(), prev.pos("ur").adds(5, 0));

        pack();
    }

    public void removeArea(String area)
    {
        areas.remove(area);
    }


    public class AreaItem extends Widget{
        Label text;
        IButton remove;

        @Override
        public void resize(Coord sz) {
            remove.move(new Coord(sz.x - NStyle.removei[0].sz().x - UI.scale(5),  remove.c.y));
            super.resize(sz);
        }

        public AreaItem(String text){
            this.text = add(new Label(text));
            remove = add(new IButton(NStyle.removei[0].back,NStyle.removei[1].back,NStyle.removei[2].back){
                @Override
                public void click() {
                    ((NMapView)NUtils.getGameUI().map).removeArea(AreaItem.this.text.text());
                    NConfig.needAreasUpdate();
                }
            },this.text.pos("ur").add(UI.scale(5),UI.scale(1) ));
            remove.settip(Resource.remote().loadwait("nurgling/hud/buttons/removeItem/u").flayer(Resource.tooltip).t);

            pack();
        }
    }
    private ConcurrentHashMap<String, AreaItem> areas = new ConcurrentHashMap<>();

    public void addArea(String val)
    {
        areas.put(val, new AreaItem(val));
    }

    public class AreaList extends SListBox<AreaItem, Widget> {
        AreaList(Coord sz) {
            super(sz, UI.scale(15));
        }

        protected List<AreaItem> items() {return new ArrayList<>(areas.values());}

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(UI.scale(120)-UI.scale(6), sz.y));
        }

        protected Widget makeitem(AreaItem item, int idx, Coord sz) {
            return(new ItemWidget<AreaItem>(this, sz, item) {
                {
                    //item.resize(new Coord(searchF.sz.x - removei[0].sz().x  + UI.scale(4), item.sz.y));
                    add(item);
                }

                public boolean mousedown(Coord c, int button) {
                    boolean psel = sel == item;
                    super.mousedown(c, button);
                    if(!psel) {
                        String value = item.text.text();
                        NUtils.getGameUI().itemsForSearch.install(value);
                    }
                    return(true);
                }
            });
        }
    }
}
