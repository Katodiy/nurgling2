package nurgling.tasks;

import java.awt.event.KeyEvent;
import nurgling.NUtils;

public class WaitKeyPress extends NTask {
    private final int keyCode;
    private static volatile int lastKeyPressed = -1;
    
    public WaitKeyPress(int keyCode) {
        this.keyCode = keyCode;
        this.infinite = true; // Ожидаем бесконечно пока не нажмут клавишу
    }
    
    public static void setLastKeyPressed(int key) {
        lastKeyPressed = key;
    }

    @Override
    public boolean check() {
        boolean result = lastKeyPressed == keyCode;
        if (result) {
            lastKeyPressed = -1; // Сбрасываем после использования
        }
        return result;
    }
}
