package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Window;
import nurgling.NConfig;
import nurgling.NFlowerMenu;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.actions.ReadJsonAction;
import nurgling.cookbook.FavoriteRecipeManager;
import nurgling.cookbook.Recipe;
import nurgling.cookbook.connection.RecipeHashFetcher;
import nurgling.i18n.L10n;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static haven.CharWnd.ifnd;

// Utility imports for layered image creation

public class NCookBook extends Window {

    public static int x_shift = UI.scale(360);
    public static int btnx_shift = UI.scale(8);
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
    private FavoriteRecipeManager favoriteManager = null;

    private ICheckBox onetwo; // Добавляем поле для хранения кнопки onetwo
    private ICheckBox[] statButtons; // Массив для хранения кнопок статов
    private ICheckBox activeStatButton = null;
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
        sorting.add(new Sort("2", true)); // Только по убыванию
        sorting.add(new Sort("1", true)); // Только по убыванию
    }

    static int col1 = UI.scale(45);    // Name
    static int col2 = UI.scale(555);   // Ingredients
    static int col3 = UI.scale(855);   // FEP/Hunger (расширен с 555+250)
    static int col4 = UI.scale(935);   // Total FEP (расширен)
    static int col5 = UI.scale(1015);  // Hunger (расширен)
    static int col6 = UI.scale(1095);  // Energy (расширен)

    public NCookBook() {
        super(UI.scale(new Coord(1300, 550)), L10n.get("cookbook.window_title"));
        statButtons = new ICheckBox[9];

        searchF = add(new TextEntry(UI.scale(1290), "") {
            @Override
            public boolean keydown(KeyDownEvent e) {
                boolean res = super.keydown(e);
                if(e.code==10)
                {
                    if (ui.core.databaseManager == null || !ui.core.databaseManager.isReady()) {
                        return res; // Database not ready
                    }
                    rhf = new RecipeHashFetcher(ui.core.databaseManager, searchF.text());
                    ui.core.databaseManager.submitTask(rhf);
                    disable();
                }
                return res;
            }
        },UI.scale(3,0));

        int headerY = searchF.pos("br").y + UI.scale(25); // Подняли выше для выравнивания с кнопками


        Coord headerPos = new Coord(0, searchF.pos("br").y + UI.scale(35));
        add(new Label(L10n.get("cookbook.col_name")), new Coord(col1, headerY));
        add(new Label(L10n.get("cookbook.col_ingredients")), new Coord(col2, headerY));
        add(new Label(L10n.get("cookbook.col_fep_hunger")), new Coord(col3, headerY));
        add(new Label(L10n.get("cookbook.col_total_fep")), new Coord(col4, headerY));
        add(new Label(L10n.get("cookbook.col_hunger")), new Coord(col5, headerY));
        add(new Label(L10n.get("cookbook.col_energy")), new Coord(col6, headerY));


        // Кнопки сортировки
        prev = add(statButtons[0] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/str/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/str/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/str/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/str/dh"))) {
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                // Деактивируем все другие кнопки
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                // Получаем значение для сортировки из состояния onetwo
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Strength +" + num, true);
            }

        }, new Coord(x_shift/2+UI.scale(5),searchF.pos("br").y+UI.scale(10)));

        // Аналогично для остальных кнопок статов...
        prev = add(statButtons[1] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/agi/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/agi/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/agi/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/agi/dh"))) {
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Agility +" + num, true);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(statButtons[2] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/int/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/int/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/int/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/int/dh"))) {
            int id = 0;
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Intelligence +" + num, true);
            }
        }, prev.pos("ur").add(btnx_shift, 0));


        prev = add(statButtons[3] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/cons/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/cons/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/cons/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/cons/dh"))) {
            int id = 0;
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Constitution +" + num, true);
            }
        }, prev.pos("ur").add(btnx_shift, 0));


        prev = add(statButtons[4] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/per/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/per/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/per/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/per/dh"))) {
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Perception +" + num, true);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(statButtons[5] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/cha/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/cha/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/cha/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/cha/dh"))) {
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Charisma +" + num, true);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(statButtons[6] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/dex/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/dex/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/dex/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/dex/dh"))) {
            int id = 0;
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Dexterity +" + num, true);
            }

        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(statButtons[7] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/wil/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/wil/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/wil/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/wil/dh"))) {
            int id = 0;
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Will +" + num, true);
            }
        }, prev.pos("ur").add(btnx_shift, 0));


        prev = add(statButtons[8] = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/psy/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/psy/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/psy/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/psy/dh"))) {
            @Override
            public void click() {
                super.click();
                action();
            }

            public void action()
            {
                for (ICheckBox btn : statButtons) {
                    if (btn != this) btn.set(false);
                }
                activeStatButton = this;
                String num = onetwo.state() ? "2" : "1";
                sortRecipes("Psyche +" + num, true);
            }
        }, prev.pos("ur").add(btnx_shift, 0));

        prev = add(onetwo = new ICheckBox(new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/onetwo/u")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/onetwo/d")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/onetwo/h")),
                new TexI(Resource.loadsimg("nurgling/hud/buttons/cookbook/onetwo/dh"))) {
            @Override
            public void changed(boolean val) {
                super.changed(val);
                if (activeStatButton != null) {
                    activeStatButton.action();
                }
            }
        }, new Coord(x_shift/2-UI.scale(32),searchF.pos("br").y+UI.scale(10)));



        // Список рецептов
        prev = add(rl = new ReceiptsList(UI.scale(new Coord(1290,400))),UI.scale(0, headerY + UI.scale(30)));

        IButton imp = add(new IButton(Resource.loadsimg("nurgling/hud/buttons/cookbook/download/u"),
                        Resource.loadsimg("nurgling/hud/buttons/cookbook/download/d"),
                        Resource.loadsimg("nurgling/hud/buttons/cookbook/download/h")) {
            @Override
            public void click() {
                java.awt.EventQueue.invokeLater(() -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileNameExtensionFilter("food-info2 file", "json"));
                    if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                        return;
                    if(fc.getSelectedFile()!=null)
                    {
                        Thread t;
                        (t = new Thread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                new ReadJsonAction(fc.getSelectedFile().getAbsolutePath()).run(NUtils.getGameUI());
                            }
                        }, "food-info2_download")).start();
                        NUtils.getGameUI().biw.addObserve(t);
                    }
                });
            }
        }, new Coord(rl.pos("ur").x - UI.scale(32), searchF.pos("br").y+UI.scale(10)));
        imp.settip(L10n.get("cookbook.btn_import"));

        // Кнопки пагинации
        prev = add(new IButton(Resource.loadsimg("nurgling/hud/buttons/cookbook/left/u"),
                Resource.loadsimg("nurgling/hud/buttons/cookbook/left/d"),
                Resource.loadsimg("nurgling/hud/buttons/cookbook/left/h")) {
            @Override
            public void click() {
                if (currentPage > 0) {
                    currentPage--;
                    updateDisplayedRecipes();
                }
            }
        }, prev.pos("br").add(UI.scale(-74, 5)));

        add(new IButton(Resource.loadsimg("nurgling/hud/buttons/cookbook/right/u"),
                Resource.loadsimg("nurgling/hud/buttons/cookbook/right/d"),
                Resource.loadsimg("nurgling/hud/buttons/cookbook/right/h")) {
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
            // First, sort by favorite status (favorites first)
            if (r1.isFavorite() != r2.isFavorite()) {
                return r1.isFavorite() ? -1 : 1;
            }
            // Then sort by FEP value
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
        if (show && (Boolean) NConfig.get(NConfig.Key.ndbenable) && ui.core.databaseManager!=null && ui.core.databaseManager.isReady()) {
            if (favoriteManager == null) {
                favoriteManager = new FavoriteRecipeManager(ui.core.databaseManager);
            }
            rhf = new RecipeHashFetcher(ui.core.databaseManager,
                    RecipeHashFetcher.genFep(currentSortType, currentSortDesc));
            ui.core.databaseManager.submitTask(rhf);
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
        
        static final TexI favoriteIcon = new TexI(Resource.loadimg("nurgling/hud/star"));
        Recipe recipe;
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

        /**
         * Create recipe icon, handling layered sprites (meat, fish, etc.)
         * Resource names with '+' separator indicate layered sprites.
         */
        private static TexI createRecipeIcon(Recipe recipe) {
            String resourceName = recipe.getResourceName();
            try {
                if (resourceName != null && resourceName.contains("+")) {
                    // Layered sprite - combine multiple images
                    String[] layers = resourceName.split("\\+");
                    BufferedImage combined = null;
                    Graphics2D g = null;
                    
                    for (String layer : layers) {
                        try {
                            Resource res = Resource.remote().loadwait(layer.trim());
                            Resource.Image imgLayer = res.layer(Resource.imgc);
                            if (imgLayer != null) {
                                BufferedImage layerImg = imgLayer.scaled();
                                if (combined == null) {
                                    // Initialize with first layer size
                                    combined = new BufferedImage(
                                        layerImg.getWidth(), 
                                        layerImg.getHeight(), 
                                        BufferedImage.TYPE_INT_ARGB
                                    );
                                    g = combined.createGraphics();
                                }
                                // Draw layer with offset
                                g.drawImage(layerImg, imgLayer.o.x, imgLayer.o.y, null);
                            }
                        } catch (Exception e) {
                            // Skip failed layer
                        }
                    }
                    
                    if (g != null) {
                        g.dispose();
                    }
                    
                    if (combined != null) {
                        return new TexI(combined);
                    }
                }
                
                // Standard single-layer sprite
                return new TexI(Resource.remote().loadwait(resourceName).layer(Resource.imgc).img);
                
            } catch (Exception e) {
                // Fallback: return empty/default icon
                return new TexI(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB));
            }
        }

        public RecieptItem(Recipe recipe) {
            this.recipe = recipe;
            recName = recipe.getName();
            this.text = add(new Label(recName),UI.scale(45,y_pos));
            icon = createRecipeIcon(recipe);
            BufferedImage bi = new BufferedImage(x_shift, UI.scale(60), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = bi.createGraphics();
            int len = UI.scale(120);
            BufferedImage wi = new BufferedImage(UI.scale(len), UI.scale(32), BufferedImage.TYPE_INT_ARGB);
            Graphics2D weight = wi.createGraphics();
            double cur = 0;
            double total = 0;
            for(String fep: recipe.getFeps().keySet()) {
                graphics.setColor(fstyle.get(fep).color);
                graphics.fillRect(fstyle.get(fep).pos.x, fstyle.get(fep).pos.y, UI.scale(40), UI.scale(30));
                if (fep.contains("2")) {
                    BufferedImage textImage = NStyle.gmeter.render(String.format("%.2f", recipe.getFeps().get(fep).val)).img;

                    int rectWidth = UI.scale(40);
                    int rectHeight = UI.scale(24);

                    int rectX = fstyle.get(fep).pos.x;
                    int rectY = fstyle.get(fep).pos.y;

                    int textX = rectX + (rectWidth - textImage.getWidth()) / 2;
                    int textY = rectY + (rectHeight - textImage.getHeight()) / 2;

                    graphics.drawImage(textImage, textX, textY, null);
                } else {
                    BufferedImage textImage = NStyle.meter.render(String.format("%.2f", recipe.getFeps().get(fep).val)).img;

                    int rectWidth = UI.scale(40);
                    int rectHeight = UI.scale(30);

                    int rectX = fstyle.get(fep).pos.x;
                    int rectY = fstyle.get(fep).pos.y;

                    int textX = rectX + (rectWidth - textImage.getWidth()) / 2;
                    int textY = rectY + (rectHeight - textImage.getHeight()) / 2;

                    graphics.drawImage(textImage, textX, textY, null);
                }
                weight.setColor(fstyle.get(fep).color);
                weight.fillRect((int)Math.floor(cur*len), 0, (int)Math.ceil(recipe.getFeps().get(fep).weigth*len), UI.scale(32));
                cur += recipe.getFeps().get(fep).weigth;
                total += recipe.getFeps().get(fep).val;
            }
            weightscale = new TexI(wi);
            StringBuilder str = new StringBuilder();
            for(String ingName: recipe.getIngredients().keySet()) {
                Recipe.IngredientInfo ingInfo = recipe.getIngredients().get(ingName);
                str.append(ingName).append(": ").append(Utils.odformat2(ingInfo.percentage,2)).append("%").append("\040");
            }
            ing = new TexI(ingfnd.render(str.toString(), UI.scale(250)).img);

            add(new Label(Utils.odformat2(total/recipe.getHunger(),2)), UI.scale(col3, y_pos));
            add(new Label(Utils.odformat2(total,2)), UI.scale(col4, y_pos));
            add(new Label(Utils.odformat2(recipe.getHunger(),2)), UI.scale(col5, y_pos));
            add(new Label(Utils.odformat2(recipe.getEnergy(),2)), UI.scale(col6, y_pos));

            feps = new TexI(bi);

            sz = UI.scale(1285,60);
        }

        @Override
        public void draw(GOut g) {
            g.image(icon,UI.scale(4,12), UI.scale(32,32));
            // Draw favorite icon in top-left corner
            if (recipe.isFavorite()) {
                g.image(favoriteIcon, UI.scale(2, 2), UI.scale(16, 16));
            }
            g.image(feps,UI.scale(180,5));
            g.image(ing,UI.scale(555,0));
            g.image(weightscale,new Coord(col6+UI.scale(50), UI.scale(12)));
            super.draw(g);
        }



        @Override
        public void draw(GOut g, boolean strict) {
            super.draw(g, strict);
        }

        NFlowerMenu menu;
        
        @Override
        public boolean mousedown(MouseDownEvent ev) {
            if (ev.b == 3) { // Right click
                showContextMenu();
                return true;
            }
            return super.mousedown(ev);
        }
        
        private void showContextMenu() {
            if (menu == null) {
                // Find NCookBook parent
                NCookBook cookbook = null;
                Widget par = RecieptItem.this.parent;
                while (par != null) {
                    if (par instanceof NCookBook) {
                        cookbook = (NCookBook) par;
                        break;
                    }
                    par = par.parent;
                }
                
                if (cookbook == null) {
                    return;
                }
                
                final NCookBook finalCookbook = cookbook;
                String[] opts = new String[] { 
                    recipe.isFavorite() ? L10n.get("cookbook.remove_from_favorites") : L10n.get("cookbook.add_to_favorites")
                };
                
                menu = new NFlowerMenu(opts) {
                    public boolean mousedown(MouseDownEvent ev) {
                        if (super.mousedown(ev))
                            nchoose(null);
                        return true;
                    }
                    
                    public void destroy() {
                        menu = null;
                        super.destroy();
                    }
                    
                    @Override
                    public void nchoose(NPetal option) {
                        if (option != null) {
                            try {
                                if (finalCookbook.favoriteManager != null) {
                                    finalCookbook.favoriteManager.toggleFavorite(recipe.getHash());
                                    recipe.setFavorite(!recipe.isFavorite());
                                    // Re-sort to update display
                                    finalCookbook.sortRecipes(finalCookbook.currentSortType, finalCookbook.currentSortDesc);
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        uimsg("cancel");
                    }
                };
                
                menu.shiftMode = true;
                par = RecieptItem.this.parent;
                Coord pos = RecieptItem.this.c.add(UI.scale(60, 60));
                while (par != null && !(par instanceof GameUI)) {
                    pos = pos.add(par.c);
                    par = par.parent;
                }
                ui.root.add(menu, pos);
            }
        }

        @Override
        public boolean mouseup(MouseUpEvent ev) {
            if (ev.b == 1) { // Left click
                for(MenuGrid.Pagina pg: NUtils.getGameUI().menu.paginae)
                {
                    if(Objects.equals(pg.button().name(), recName))
                    {
                        pg.button().use(new MenuGrid.Interaction(1, 0));
                        break;
                    }
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
            super.resize(new Coord(UI.scale(1290, sz.y)));
        }

        protected Widget makeitem(RecieptItem item, int idx, Coord sz) {
            return(new ItemWidget<RecieptItem>(this, sz.add(UI.scale(0,5)), item) {
                {
                    //item.resize(new Coord(searchF.sz.x - removei[0].sz().x  + UI.scale(4), item.sz.y));
                    add(item);
                }

                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    // Pass right-click to the RecieptItem for context menu
                    if (ev.b == 3) {
                        return item.mousedown(ev);
                    }
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