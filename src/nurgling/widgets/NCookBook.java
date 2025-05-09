package nurgling.widgets;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.res.lib.itemtex.ItemTex;
import nurgling.NConfig;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.actions.ReadJsonAction;
import nurgling.actions.bots.AutoChooser;
import nurgling.actions.bots.Craft;
import nurgling.cookbook.Recipe;
import nurgling.cookbook.connection.RecipeHashFetcher;
import nurgling.cookbook.connection.RecipeLoader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static haven.CharWnd.ifnd;

public class NCookBook extends Window {

    public static int x_shift = UI.scale(360);
    public static int btnx_shift = UI.scale(12);
    private ReceiptsList rl;
    public static final RichText.Foundry ingfnd = new RichText.Foundry(RichText.ImageSource.res(Resource.remote()),
            java.awt.font.TextAttribute.FAMILY, "SansSerif", java.awt.font.TextAttribute.SIZE, UI.scale(12)).aa(true);

    private static final int PAGE_SIZE = 20;
    private int currentPage = 0;
    private ArrayList<Recipe> allRecipes = new ArrayList<>();
    private ArrayList<Recipe> sortedRecipes = new ArrayList<>();
    private String currentSortType = "Strength +2";
    private boolean currentSortDesc = true;

    private TextEntry searchF;
    RecipeHashFetcher rhf = null;

    RecipeHashFetcher currentRhf = null;
    static class Sort {
        String num;
        boolean desc;

        public Sort(String num, boolean desc) {
            this.num = num;
            this.desc = desc;
        }
    }

    static ArrayList<Sort> sorting = new ArrayList<>();
    static {
        sorting.add(new Sort("2", true));
        sorting.add(new Sort("1", true));
        sorting.add(new Sort("1", false));
        sorting.add(new Sort("2", false));
    }

    public NCookBook() {
        super(UI.scale(new Coord(1200, 510)), "Cook Book");

        searchF = add(new TextEntry(UI.scale(1190), "") {
            @Override
            public boolean keydown(KeyDownEvent e) {
                boolean res = super.keydown(e);
                if(e.code==10)
                {
                    rhf = new RecipeHashFetcher(ui.core.poolManager.connection,
                            searchF.text());
                    ui.core.poolManager.submitTask(rhf);
                    disable();
                }
                return res;
            }
        },UI.scale(3,0));

        // Кнопки сортировки
        Widget prev = add(new Button(30, "Str") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Strength +" + sr.num, sr.desc);
            }
        },new Coord(x_shift/2+UI.scale(7),searchF.pos("br").y+UI.scale(10)));

        prev = add(new Button(30, "Agi") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Agility +" + sr.num, sr.desc);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(new Button(30, "Int") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Intelligence +" + sr.num, sr.desc);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(new Button(30, "Con") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Constitution +" + sr.num, sr.desc);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(new Button(30, "Per") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Perception +" + sr.num, sr.desc);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(new Button(30, "Cha") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Charisma +" + sr.num, sr.desc);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(new Button(30, "Dex") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Dexterity +" + sr.num, sr.desc);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(new Button(30, "Wil") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Will +" + sr.num, sr.desc);
            }
        }, prev.pos("ur").add(btnx_shift, 0));


        prev = add(new Button(30, "Psy") {
            int id = 0;
            @Override
            public void click() {
                Sort sr = sorting.get(id++ % 4);
                sortRecipes("Psyche +" + sr.num, sr.desc);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        // Кнопка импорта
        IButton imp = add(new IButton(NStyle.importb[0].back, NStyle.importb[1].back, NStyle.importb[2].back) {
            @Override
            public void click() {
                // ... существующий код импорта ...
            }
        }, prev.pos("ur").adds(UI.scale(25, 0)));
        imp.settip("Import");

        // Список рецептов
        prev = add(rl = new ReceiptsList(UI.scale(new Coord(1190,400))), UI.scale(0, 50+searchF.pos("br").y));

        // Кнопки пагинации
        prev = add(new Button(UI.scale(100), "Back") {
            @Override
            public void click() {
                if (currentPage > 0) {
                    currentPage--;
                    updateDisplayedRecipes();
                }
            }
        }, prev.pos("br").add(UI.scale(-220, 5)));

        add(new Button(UI.scale(100), "Next") {
            @Override
            public void click() {
                int maxPage = (sortedRecipes.size() + PAGE_SIZE - 1) / PAGE_SIZE - 1;
                if (currentPage < maxPage) {
                    currentPage++;
                    updateDisplayedRecipes();
                }
            }
        }, prev.pos("ur").add(UI.scale(10, 0)));
        pack();
    }

    private final ArrayList<RecieptItem> items = new ArrayList<>();

    private void sortRecipes(String fepType, boolean desc) {
        sortedRecipes = new ArrayList<>(allRecipes);
        sortedRecipes.sort((r1, r2) -> {
            double val1 = r1.getFeps().containsKey(fepType) ? r1.getFeps().get(fepType).val : 0;
            double val2 = r2.getFeps().containsKey(fepType) ? r2.getFeps().get(fepType).val : 0;
            return desc ? Double.compare(val2, val1) : Double.compare(val1, val2);
        });
        currentSortType = fepType;
        currentSortDesc = desc;
        currentPage = 0;
        updateDisplayedRecipes();
    }

    private void updateDisplayedRecipes() {
        int startIdx = currentPage * PAGE_SIZE;
        int endIdx = Math.min(startIdx + PAGE_SIZE, sortedRecipes.size());

        items.clear();
        for (int i = startIdx; i < endIdx; i++) {
            items.add(new RecieptItem(sortedRecipes.get(i)));
        }
    }

    @Override
    public void tick(double dt) {
        super.tick(dt);
        if (rhf != null && rhf.ready.get()) {
            allRecipes = rhf.getRecipes();
            rhf = null;
            sortRecipes(currentSortType, currentSortDesc);
            enable();
        }
    }

    @Override
    public boolean show(boolean show) {
        if (show) {
            rhf = new RecipeHashFetcher(ui.core.poolManager.connection,
                    RecipeHashFetcher.genFep(currentSortType, currentSortDesc));
            ui.core.poolManager.submitTask(rhf);
            disable();
        }
        return super.show(show);
    }

    public static class RecieptItem extends Widget{
        Label text;
        TexI feps;

        TexI icon;
        TexI weightscale;

        TexI ing;
        @Override
        public void resize(Coord sz) {
            super.resize(sz);
        }

        private static class FepStyle
        {
            Color color;
            Coord pos;

            public FepStyle(Color color, Coord pos) {
                this.color = color;
                this.pos = pos;
            }
        }

        private static HashMap<String, FepStyle> fstyle = new HashMap<>();
        static {
            fstyle.put("Strength +1", new FepStyle(new Color(249, 151, 144,255),UI.scale(0,0)));
            fstyle.put("Strength +2", new FepStyle(new Color(236, 79, 68,255),UI.scale(0,30)));
            fstyle.put("Agility +1", new FepStyle(new Color(115, 146, 255, 255),UI.scale(40,0)));
            fstyle.put("Agility +2", new FepStyle(new Color(53, 81, 226,255),UI.scale(40,30)));
            fstyle.put("Intelligence +1", new FepStyle(new Color(154, 248, 255, 255),UI.scale(80,0)));
            fstyle.put("Intelligence +2", new FepStyle(new Color(2, 176, 189,255),UI.scale(80,30)));
            fstyle.put("Constitution +1", new FepStyle(new Color(242, 127, 202, 255),UI.scale(120,0)));
            fstyle.put("Constitution +2", new FepStyle(new Color(194, 20, 133,255),UI.scale(120,30)));
            fstyle.put("Perception +1", new FepStyle(new Color(249, 185, 118, 255),UI.scale(160,0)));
            fstyle.put("Perception +2", new FepStyle(new Color(217, 113, 3,255),UI.scale(160,30)));
            fstyle.put("Charisma +1", new FepStyle(new Color(128, 255, 161, 255),UI.scale(200,0)));
            fstyle.put("Charisma +2", new FepStyle(new Color(0, 203, 54,255),UI.scale(200,30)));
            fstyle.put("Dexterity +1", new FepStyle(new Color(255, 245, 153, 255),UI.scale(240,0)));
            fstyle.put("Dexterity +2", new FepStyle(new Color(227, 206, 13,255),UI.scale(240,30)));
            fstyle.put("Will +1", new FepStyle(new Color(229, 255, 85, 255),UI.scale(280,0)));
            fstyle.put("Will +2", new FepStyle(new Color(169, 199, 0,255),UI.scale(280,30)));
            fstyle.put("Psyche +1", new FepStyle(new Color(181, 109, 255, 255),UI.scale(320,0)));
            fstyle.put("Psyche +2", new FepStyle(new Color(116, 66, 168,255),UI.scale(320,30)));
        }


        private static int y_pos = 20;
        private String recName;

        public RecieptItem(Recipe recipe) {
            recName = recipe.getName();
            this.text = add(new Label(recName),UI.scale(45,y_pos));
            icon = new TexI(Resource.remote().loadwait(recipe.getResourceName()).layer(Resource.imgc).img);
            BufferedImage bi = new BufferedImage(x_shift,UI.scale(60), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = bi.createGraphics();
            int len = UI.scale(180);
            BufferedImage wi = new BufferedImage(UI.scale(len),UI.scale(32), BufferedImage.TYPE_INT_ARGB);
            Graphics2D weight = wi.createGraphics();
            double cur = 0;
            double total = 0;
            for(String fep: recipe.getFeps().keySet()) {
                graphics.setColor(fstyle.get(fep).color);
                graphics.fillRect(fstyle.get(fep).pos.x, fstyle.get(fep).pos.y, UI.scale(40), UI.scale(30));
                if (fep.contains("2")) {
                    graphics.drawImage(NStyle.gmeter.render(String.format("%.2f", recipe.getFeps().get(fep).val)).img, fstyle.get(fep).pos.x + UI.scale(1), fstyle.get(fep).pos.y, null);
                } else {
                    graphics.drawImage(NStyle.meter.render(String.format("%.2f", recipe.getFeps().get(fep).val)).img, fstyle.get(fep).pos.x + UI.scale(1), fstyle.get(fep).pos.y + UI.scale(1), null);
                }
                weight.setColor(fstyle.get(fep).color);
                weight.fillRect((int)Math.floor(cur*len),0,(int)Math.ceil(recipe.getFeps().get(fep).weigth*len),UI.scale(32));
                cur+=recipe.getFeps().get(fep).weigth;
                total +=recipe.getFeps().get(fep).val;
            }
            weightscale = new TexI(wi);
            StringBuilder str = new StringBuilder();
            for(String ing: recipe.getIngredients().keySet())
            {
                str.append(ing).append(": ").append(Utils.odformat2(recipe.getIngredients().get(ing),2)).append("%").append("\040");
            }
            ing = new TexI(ingfnd.render(str.toString(), UI.scale(250)).img);

            add(new Label(Utils.odformat2(total/recipe.getHunger(),2)),UI.scale(555+250,y_pos));
            add(new Label(Utils.odformat2(total,2)),UI.scale(595+250,y_pos));
            add(new Label(Utils.odformat2(recipe.getHunger(),2)),UI.scale(635+250,y_pos));
            add(new Label(Utils.odformat2(recipe.getEnergy(),2)),UI.scale(675+250,y_pos));

            feps = new TexI(bi);

            sz = UI.scale(1185,60);
        }

        @Override
        public void draw(GOut g) {
            g.image(icon,UI.scale(4,12), UI.scale(32,32));
            g.image(feps,UI.scale(180,5));
            g.image(ing,UI.scale(555,0));
            g.image(weightscale,UI.scale(745+250,12));
            super.draw(g);
        }



        @Override
        public void draw(GOut g, boolean strict) {
            super.draw(g, strict);
        }

        @Override
        public boolean mouseup(MouseUpEvent ev) {
            for(MenuGrid.Pagina pg: NUtils.getGameUI().menu.paginae)
            {
                if(Objects.equals(pg.button().name(), recName))
                {
                    pg.button().use(new MenuGrid.Interaction(1, 0));
                    break;
                }
            }
            return super.mouseup(ev);
        }
    }

    // Класс ReceiptsList остается без изменений
    public class ReceiptsList extends SListBox<RecieptItem, Widget> {
        ReceiptsList(Coord sz) {
            super(sz, UI.scale(60));
        }

        protected List<RecieptItem> items() {
            synchronized (items) {
                return items;
            }
        }

        @Override
        public void resize(Coord sz) {
            super.resize(new Coord(UI.scale(1190, sz.y)));
        }

        protected Widget makeitem(RecieptItem item, int idx, Coord sz) {
            return(new ItemWidget<RecieptItem>(this, sz.add(UI.scale(0,5)), item) {
                {
                    //item.resize(new Coord(searchF.sz.x - removei[0].sz().x  + UI.scale(4), item.sz.y));
                    add(item);
                }

                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    boolean psel = sel == item;
                    super.mousedown(ev);
                    if(!psel) {
                        String value = item.text.text();
                    }
                    return super.mousedown(ev);
                }

            });
        }




        @Override
        public void wdgmsg(String msg, Object... args)
        {
            super.wdgmsg(msg, args);
        }

        Color bg = new Color(30,40,40,160);
        @Override
        public void draw(GOut g)
        {
            g.chcolor(bg);
            g.frect(Coord.z, g.sz());
            super.draw(g);
        }

        @Override
        public void change(RecieptItem item) {
            super.change(item);
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