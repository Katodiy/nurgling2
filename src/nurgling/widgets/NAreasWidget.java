package nurgling.widgets;

import haven.*;
import nurgling.*;
import nurgling.areas.*;
import nurgling.tools.*;

import java.util.*;
import java.util.concurrent.*;

public class NAreasWidget extends Window
{
    public IngredientContainer in_items;
    public IngredientContainer out_items;

    AreaList al;
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

        prev = add(al = new AreaList(UI.scale(new Coord(300,200))), prev.pos("bl").adds(0, 10));

//        add(new Button(UI.scale(100), "Edit name"){
//            @Override
//            public void click()
//            {
//                super.click();
//                if(adrop.sel!=null)
//                {
//                    NArea area = ((NMapView)NUtils.getGameUI().map).findArea((String)adrop.sel);
//                    NEditAreaName.changeName(area);
//                }
//            }
//        }, prev.pos("ur").adds(5, -10));

        prev = add(in_items = new IngredientContainer(), prev.pos("ur").add(5,0));
        add(out_items = new IngredientContainer(), prev.pos("ur").adds(5, 0));

        pack();
    }

    public void removeArea(int id)
    {
        areas.remove(id);
        if(NUtils.getGameUI()!=null && NUtils.getGameUI().map!=null)
        {
            MapView.NOverlay nol = NUtils.getGameUI().map.nols.get(id);
            nol.remove();
            NUtils.getGameUI().map.nols.remove(id);
        }
    }

    @Override
    public void show()
    {
        if(areas.isEmpty() && !NUtils.getGameUI().map.glob.map.areas.isEmpty())
        {
            for (NArea area : NUtils.getGameUI().map.glob.map.areas.values())
                addArea(area.id, area.name);
        }
        super.show();
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
            },new Coord(al.sz.x - NStyle.removei[0].sz().x, 0).sub(UI.scale(5),UI.scale(1) ));
            remove.settip(Resource.remote().loadwait("nurgling/hud/buttons/removeItem/u").flayer(Resource.tooltip).t);

            pack();
        }

        @Override
        public boolean mousedown(Coord c, int button)
        {
            if(button==3)
            {
                opts(c);
                return true;
            }
            else
            {
                return super.mousedown(c, button);
            }
        }

        final ArrayList<String> opt = new ArrayList<String>(){
            {
                add("Select area space");
                add("Set color");
                add("Edit name");
            }
        };

        NFlowerMenu menu;

        public void opts( Coord c ) {
            if(menu == null) {
                menu = new NFlowerMenu(opt.toArray(new String[0])) {
                    public boolean mousedown(Coord c, int button) {
                        if(super.mousedown(c, button))
                            nchoose(null);
                        return(true);
                    }

                    public void destroy() {
                        menu = null;
                        super.destroy();
                    }

                    @Override
                    public void nchoose(NPetal option)
                    {
                        if(option!=null)
                        {
                            if (option.name.equals("Select area space"))
                            {
                                ((NMapView)NUtils.getGameUI().map).changeArea(AreaItem.this.text.text());
                            }
                            else if (option.name.equals("Set color"))
                            {

                            }
                            else if (option.name.equals("Edit name"))
                            {

                            }
                        }
                        uimsg("cancel");
                    }

                };
            }
            Widget par = parent;
            Coord pos = c;
            while(par!=null && !(par instanceof GameUI))
            {
                pos = pos.add(par.c);
                par = par.parent;
            }
            ui.root.add(menu, pos.add(UI.scale(25,38)));
        }


    }
    private ConcurrentHashMap<Integer, AreaItem> areas = new ConcurrentHashMap<>();

    public void addArea(int id, String val)
    {
        areas.put(id, new AreaItem(val));
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

        @Override
        public void wdgmsg(String msg, Object... args)
        {
            super.wdgmsg(msg, args);
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
}
