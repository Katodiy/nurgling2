package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Window;
import haven.res.lib.itemtex.ItemTex;
import nurgling.*;
import nurgling.tools.VSpec;
import org.json.JSONObject;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class NCatSelection extends Window {
    public static Text.Foundry fnd = new Text.Foundry(Text.sans.deriveFont(java.awt.Font.BOLD), 14).aa(true);
    private TextEntry searchBox;
    private final List<Category> categories = new ArrayList<>();
    private final List<Element> allElements = new ArrayList<>();
    private final Map<String, List<Element>> categoryElements = new HashMap<>();
    private CategoryList categoryList;
    private ElementList elementList;

    public NCatSelection() {
        super(UI.scale(new Coord(600, 400)), "Category Selection");
        add(new Label("Categories:"),UI.scale(5,5));
        // Инициализация категорий и элементов из VSpec
        Set<String> categoryNames = VSpec.categories.keySet();
        for (String categoryName : categoryNames) {
            categories.add(new Category(categoryName));
            List<Element> elements = new ArrayList<>();
            for (JSONObject obj : VSpec.categories.get(categoryName)) {
                String name = obj.getString("name");
                Element element = new Element(name, obj);
                elements.add(element);
                allElements.add(element); // Добавляем все элементы в общий список
            }
            elements.sort(Comparator.comparing(Element::getName));
            categoryElements.put(categoryName, elements);
        }
        categories.sort(Comparator.comparing(Category::getName));

        // Создаем список категорий слева
        categoryList = add(new CategoryList(UI.scale(200, 280)), UI.scale(10, 30));
        categoryList.setItems(categories);

        // Создаем список элементов справа
        elementList = add(new ElementList(UI.scale(350, 280)), UI.scale(220, 30));

        searchBox = add(new TextEntry(UI.scale(350), "") {
            @Override
            public void changed() {
                updateElementList(); // Обновляем список при вводе в поисковой строке
            }
        }, elementList.pos("ul").sub(UI.scale(0, 30)));

        categoryList.setOnCategorySelected(category -> {
            List<Element> elements = categoryElements.get(category.getName());
            elementList.setItems(elements);
        });
        pack();
    }

    // Метод для обновления списка элементов справа
    private void updateElementList() {
        String searchText = searchBox.text().toLowerCase(); // Получаем текст из поисковой строки

        if (searchText.isEmpty()) {
            // Если строка поиска пустая, отображаем элементы выбранной категории
            Category selectedCategory = categoryList.getSelected();
            if (selectedCategory != null) {
                List<Element> elements = categoryElements.get(selectedCategory.getName());
                elementList.setItems(elements);
            }
        } else {
            // Если введен текст, фильтруем элементы по поисковому запросу
            Set<Element> filteredElements = new TreeSet<>(Comparator.comparing(Element::getName)); // Используем TreeSet для сортировки и удаления дубликатов

            for (Element element : allElements) {
                if (element.getName().toLowerCase().contains(searchText)) {
                    filteredElements.add(element); // Добавляем только уникальные элементы
                }
            }

            // Преобразуем Set обратно в List для отображения в элементе UI
            elementList.setItems(new ArrayList<>(filteredElements));
        }
    }

//    private void addAllElementsToArea(int areaId, String categoryName) {
//        List<Element> elements = categoryElements.get(categoryName);
//        if (elements != null) {
//            for (Element element : elements) {
//                addElementToArea(element);
//            }
//        }
//    }

    // Класс для представления категории
    public static class Category {
        private final String name;

        public Category(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Класс для представления элемента в категории
    public static class Element {
        private final String name;
        private final JSONObject res;

        public Element(String name, JSONObject res) {
            this.name = name;
            this.res = res;
        }

        public String getName() {
            return name;
        }

        public JSONObject getRes() {
            return res;
        }
    }

    public class CategoryList extends SListBox<Category, Widget> {
        private final List<Category> internalCategories = new ArrayList<>();
        private Consumer<Category> onCategorySelected;
        private Category selected; // Переменная для хранения выбранной категории

        public CategoryList(Coord sz) {
            super(sz, UI.scale(24));
        }
        @Override
        public void change(Category item) {
            selected = item; // Сохраняем выбранную категорию
            onCategorySelected.accept(selected);
            super.change(item);
        }

        @Override
        protected List<Category> items() {
            return internalCategories;
        }

        @Override
        protected Widget makeitem(Category item, int idx, Coord sz) {
            return new ItemWidget<Category>(this, sz, item) {
                {
                    add(new CategoryWidget(item));
                }

                @Override
                public void draw(GOut g) {
                    // Проверяем, является ли элемент выбранным, и если да, изменяем его цвет
                    if (selected == item) {
                        g.chcolor(200, 200, 255, 255); // Подсвечиваем синим фоном
                    } else {
                        g.chcolor();
                    }
                    super.draw(g);
                }
            };
        }

        public Category getSelected() {
            return selected;
        }

        public void setItems(List<Category> newCategories) {
            internalCategories.clear();
            internalCategories.addAll(newCategories);
        }

        public void setOnCategorySelected(Consumer<Category> onCategorySelected) {
            this.onCategorySelected = onCategorySelected;
        }

        public int itemh() {
            return UI.scale(32);
        }
    }

    public class CategoryWidget extends Widget {
        private final Category category;
        private final Label label;
        private final IButton addToInputButton; // Кнопка для добавления в IN
        private final IButton addToOutputButton;

        public CategoryWidget(Category category) {
            this.category = category;
            int desiredHeight = UI.scale(24);
            // Инициализация кнопки для добавления в IN
            add(label = new Label(category.getName()), UI.scale(5, desiredHeight/2 - UI.scale(6)));
            addToInputButton = add(new IButton(NStyle.toTake[0].back,NStyle.toTake[1].back,NStyle.toTake[2].back){
                @Override
                public void click() {
                    addToInput();
                }
            }, UI.scale(145, desiredHeight/2 - NStyle.toTake[0].sz().y/2));
            addToInputButton.tooltip = Text.render("Add to 'Take'").tex();

            // Инициализация кнопки для добавления в OUT
            addToOutputButton = add(new IButton(NStyle.toPut[0].back,NStyle.toPut[1].back,NStyle.toPut[2].back){
                @Override
                public void click() {
                    addToOutput();
                }
            }, UI.scale(165, desiredHeight/2 - NStyle.toTake[0].sz().y/2));
            addToOutputButton.tooltip = Text.render("Add to 'Put'").tex();
            pack();
        }
        // Метод для добавления в IN
        private void addToInput() {
            for (JSONObject obj : VSpec.categories.get(category.getName())) {
                NUtils.getGameUI().areas.in_items.addItem(obj.getString("name"), obj);
            }
        }

        // Метод для добавления в OUT
        private void addToOutput() {
            for (JSONObject obj : VSpec.categories.get(category.getName())) {
                NUtils.getGameUI().areas.out_items.addItem(obj.getString("name"), obj);
            }
        }

    }

    public class ElementList extends SListBox<Element, Widget> {
        private List<Element> internalElements = new ArrayList<>();
        public ElementList(Coord sz) {
            super(sz, UI.scale(32));
        }

        @Override
        protected List<Element> items() {
            return internalElements;
        }

        @Override
        protected Widget makeitem(Element item, int idx, Coord sz) {
            return new ItemWidget<Element>(this, sz, item) {
                {
                    // Загружаем иконку элемента и добавляем к виджету
                    Tex icon = new TexI(ItemTex.create(item.res));
                    add(new ElementWidget(item, icon), Coord.z);
                }
            };
        }

        public void setItems(List<Element> newElements) {
            internalElements.clear();
            if (newElements != null) {
                internalElements.addAll(newElements);
            }
            reset(); // Обновляем список для перерисовки
        }
    }

    public class ElementWidget extends Widget {
        private final Element element;
        private final Tex icon;
        private final Label label;
        private final IButton addToInputButton; // Кнопка для добавления в IN
        private final IButton addToOutputButton;

        public ElementWidget(Element element, Tex icon) {
            this.element = element;
            this.icon = icon;
            int desiredHeight = UI.scale(32);
            // Инициализация кнопки для добавления в IN
            add(label = new Label(element.getName(), fnd), UI.scale(70, desiredHeight/2 - UI.scale(11)));
            addToInputButton = add(new IButton(NStyle.toTake[0].back,NStyle.toTake[1].back,NStyle.toTake[2].back){
                @Override
                public void click() {
                    addToInput();
                }
            }, UI.scale(64 + 230, desiredHeight/2 - NStyle.toTake[0].sz().y/2));
            addToInputButton.tooltip = Text.render("Add to 'Take'").tex();

            // Инициализация кнопки для добавления в OUT
            addToOutputButton = add(new IButton(NStyle.toPut[0].back,NStyle.toPut[1].back,NStyle.toPut[2].back){
                @Override
                public void click() {
                    addToOutput();
                }
            }, UI.scale(64 + 250, desiredHeight/2 - NStyle.toTake[0].sz().y/2));
            addToOutputButton.tooltip = Text.render("Add to 'Put'").tex();
            pack();
            sz.y = desiredHeight;
        }
        // Метод для добавления в IN
        private void addToInput() {
            if(NUtils.getGameUI().areas.al.sel.area!=null)
                NUtils.getGameUI().areas.in_items.addItem(element.name, element.res);
        }

        // Метод для добавления в OUT
        private void addToOutput() {
            if(NUtils.getGameUI().areas.al.sel.area!=null)
                NUtils.getGameUI().areas.out_items.addItem(element.name, element.res);
        }
        @Override
        public void draw(GOut g) {
            super.draw(g);
            int desiredHeight = UI.scale(32);
            int scaledWidth = UI.scale((int) (icon.sz().x * desiredHeight / icon.sz().y));

            g.image(icon, Coord.z, new Coord(scaledWidth,desiredHeight));
        }
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
        }
        super.wdgmsg(msg, args);
    }
}
