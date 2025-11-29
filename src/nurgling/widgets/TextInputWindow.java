package nurgling.widgets;

import haven.*;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class TextInputWindow extends Window {
    
    private TextEntry textEntry;
    private Button okButton;
    private Button cancelButton;
    private Consumer<String> onSubmit;
    private boolean submitted = false;
    
    public TextInputWindow(String title, String prompt, Consumer<String> onSubmit) {
        super(new Coord(UI.scale(300), UI.scale(120)), title);
        this.onSubmit = onSubmit;
        
        Widget prev = add(new Label(prompt));
        
        prev = add(textEntry = new TextEntry(UI.scale(280), "") {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                if (ev.code == KeyEvent.VK_ENTER) {
                    submit();
                    return true;
                } else if (ev.code == KeyEvent.VK_ESCAPE) {
                    close();
                    return true;
                }
                return super.keydown(ev);
            }
        }, prev.pos("bl").add(UI.scale(0, 10)));
        
        Widget buttonRow = add(new Widget(new Coord(UI.scale(280), UI.scale(30))), prev.pos("bl").add(UI.scale(0, 10)));
        
        okButton = buttonRow.add(new Button(UI.scale(80), "OK") {
            @Override
            public void click() {
                super.click();
                submit();
            }
        }, new Coord(UI.scale(50), 0));
        
        cancelButton = buttonRow.add(new Button(UI.scale(80), "Cancel") {
            @Override
            public void click() {
                super.click();
                close();
            }
        }, new Coord(UI.scale(150), 0));
        
        pack();
        
        // Focus text entry
        setfocus(textEntry);
    }
    
    private void submit() {
        String text = textEntry.text().trim();
        if (!text.isEmpty() && onSubmit != null) {
            submitted = true;
            onSubmit.accept(text);
        }
        close();
    }
    
    private void close() {
        if (!submitted && onSubmit != null) {
            onSubmit.accept(null);
        }
        hide();
        destroy();
    }
    
    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            close();
        } else {
            super.wdgmsg(msg, args);
        }
    }
}
