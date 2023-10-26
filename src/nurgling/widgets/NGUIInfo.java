package nurgling.widgets;

import haven.*;
import static haven.CharWnd.attrw;
import static haven.CharWnd.ifnd;
import nurgling.*;
import static nurgling.NStyle.nifnd;

public class NGUIInfo extends Window
{

    public static final int xs = UI.scale(500);
    public static final int ys = UI.scale(200);
    public NGUIInfo()
    {
        super(new Coord(xs,ys), "Nurgling GUI settings");
        //hide();

        RichTextBox rb;
        prev = add(rb = new RichTextBox(UI.scale(new Coord(500, 180)), "$img[nurgling/hud/dragmode/title] Welcome to custom client Nurgling! \n" +
                    "Arrange widgets as you wish. You can adjust the visibility of widgets. Close this window when you're done. You can call this mode again in the Interface settings", nifnd));
        rb.bg = null;
        add(new Button(UI.scale(200),"Close"){
            @Override
            public void click()
            {
                super.click();
                closeEvent();
            }
        }, prev.pos("bl").add(xs/2- UI.scale(100),UI.scale(5)));
        pack();
    }

    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if(msg.equals("close"))
        {
            closeEvent();
        }
        else
        {
            super.wdgmsg(msg, args);
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args)
    {
        super.wdgmsg(sender, msg, args);
    }

    private void closeEvent()
    {
        NUtils.getUI().core.mode = NCore.Mode.IDLE;
        this.hide();
    }
}
