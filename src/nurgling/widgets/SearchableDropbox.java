package nurgling.widgets;

import haven.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class SearchableDropbox<T> extends Dropbox<T> {
    private Listbox<T> lb; // Основной дроплист
    private SearchableDroplist dl; // Фильтрованный дроплист

    public SearchableDropbox(int w, int listh, int itemh) {
        super(w, listh, itemh);
        this.canfocus = true; // Позволяем виджету получать фокус
    }

    public void destroyListbox() {
        if (this.lb != null) {
            this.lb.reqdestroy(); // Уничтожаем основной Listbox
            this.lb = null;
        }
    }

    public void destroyDroplist() {
        if (this.dl != null) {
            this.dl.reqdestroy(); // Уничтожаем фильтрованный дроплист
            this.dl = null;
        }
    }

    @Override
    public boolean mousedown(Coord c, int btn) {

        // При нажатии на элемент дропбокса, убираем текущий лист и открываем новый.
        if ((dl == null) && (btn == 1)) {
            destroyListbox();  // Уничтожаем основной дроплист перед созданием нового
            destroyDroplist(); // Уничтожаем старый фильтрованный дроплист, если был
            lb = new Listbox<T>(sz.x, listh, itemh) {
                @Override
                protected T listitem(int i) {
                    return SearchableDropbox.this.listitem(i);
                }

                @Override
                protected int listitems() {
                    return SearchableDropbox.this.listitems();
                }

                @Override
                protected void drawitem(GOut g, T item, int idx) {
                    SearchableDropbox.this.drawitem(g, item, idx);
                }

                @Override
                protected void itemclick(T item, int button) {
                    SearchableDropbox.this.change(item);
                    destroyListbox(); // Закрываем основной дроплист при выборе элемента
                }
            };
            // Показываем основной дроплист
            SearchableDropbox.this.ui.root.add(lb, SearchableDropbox.this.rootpos());
            return true;
        }

        if (super.mousedown(c, btn))
            return true;
        return true;

    }

    @Override
    public boolean keydown(KeyEvent ev) {
        // Проверяем, был ли закрыт предыдущий фильтрованный список или он отсутствует
        if (dl == null) {
            destroyListbox();  // Уничтожаем основной дроплист перед фильтрацией
            dl = new SearchableDroplist(); // Создаем новый фильтрованный список
        }

        // Обрабатываем нажатие клавиши в фильтрованном дроплисте
        if (super.keydown(ev))
            return true;
        return dl.keydown(ev);
    }

    private class SearchableDroplist extends Widget {
        private UI.Grab grab = null;
        private TextEntry filter;
        private List<T> fullitems = new ArrayList<>();
        private List<T> filtereditems = new ArrayList<>();
        private Listbox<T> listbox;

        private SearchableDroplist() {
            super(new Coord(SearchableDropbox.this.sz.x, 0));

            // Инициализация полного списка
            for (int i = 0; i < SearchableDropbox.this.listitems(); i++) {
                fullitems.add(SearchableDropbox.this.listitem(i));
            }

            // Создаем поле для ввода текста
            filter = new TextEntry(SearchableDropbox.this.sz.x, "") {
                @Override
                public boolean keydown(KeyEvent ev) {
                    boolean ret = super.keydown(ev);
                    refilter(); // Фильтруем элементы списка
                    return ret;
                }

                @Override
                public void activate(String text) {
                    if (!filtereditems.isEmpty()) {
                        T selectedItem = filtereditems.get(0);
                        SearchableDropbox.this.change(selectedItem);
                    }
                    reqdestroy(); // Закрываем поле ввода при активации
                }
            };

            //filter.canactivate = true; // Позволяем закрывать поле ввода клавишей Enter
            add(filter, new Coord(0, 0));

            // Изначально мы не создаем Listbox
            this.sz = new Coord(SearchableDropbox.this.sz.x, filter.sz.y);

            // Добавляем Droplist в UI
            SearchableDropbox.this.ui.root.add(this, SearchableDropbox.this.rootpos());
            grab = ui.grabmouse(this); // Перехватываем ввод мыши

            // Переносим фокус на фильтр
            setfocus(filter);
        }
        @Override
        public boolean keydown(KeyEvent ev) {
            // Проверяем, был ли закрыт предыдущий фильтрованный список или он отсутствует
            filter.settext("");
            filter.show();
            return super.keydown(ev);
        }
        void refilter() {
            String text = filter.text().toLowerCase().trim();
            filtereditems.clear();

            if (text.isEmpty()) {
                // Не показываем список, если фильтр пустой
                if (listbox != null) {
                    ui.destroy(listbox);
                    listbox = null;
                }
                this.sz = new Coord(sz.x, filter.sz.y);
            } else {
                // Фильтруем элементы
                for (T item : fullitems) {
                    if (item.toString().toLowerCase().contains(text)) {
                        filtereditems.add(item);
                    }
                }
                if (listbox == null) {
                    // Создаем Listbox с отфильтрованными элементами
                    int listHeight = Math.min(10, filtereditems.size());
                    listbox = new Listbox<T>(SearchableDropbox.this.sz.x, listHeight, SearchableDropbox.this.itemh) {
                        @Override
                        protected T listitem(int i) {
                            return filtereditems.get(i);
                        }

                        @Override
                        protected int listitems() {
                            return filtereditems.size();
                        }

                        @Override
                        protected void drawitem(GOut g, T item, int idx) {
                            SearchableDropbox.this.drawitem(g, item, idx);
                        }

                        @Override
                        protected void itemclick(T item, int button) {
                            SearchableDropbox.this.change(item);
                            reqdestroy(); // Закрываем дроплист после выбора элемента
                            filter.hide();
                        }
                    };
                    add(listbox, new Coord(0, filter.sz.y));
                } else {
                    // Обновляем существующий Listbox
                    listbox.h = Math.min(10, filtereditems.size());
                    listbox.resize(new Coord(listbox.sz.x, listbox.h * listbox.itemh));
                    listbox.sb.val = 0; // Сбрасываем позицию скроллбара
                    listbox.sb.max = Math.max(filtereditems.size() - listbox.h, 0);
                    listbox.display();
                }
                this.sz = new Coord(sz.x, filter.sz.y + listbox.sz.y);
            }
        }

        @Override
        public void destroy() {
            if (grab != null) {
                grab.remove();
                grab = null;
            }
            super.destroy();
            SearchableDropbox.this.dl = null; // Сбрасываем переменную в главном классе
            SearchableDropbox.this.show(true); // Показываем основной дроплист, если поле закрыто
        }
    }
}
