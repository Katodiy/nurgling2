package nurgling;

import haven.*;
import nurgling.tasks.*;

import java.awt.event.*;
import java.util.*;

public class NISBox extends ISBox
{
    private TextEntry.NumberValue value;
    private TakeButton take;

    private int rem;


    public NISBox(Indir<Resource> res, int rem, int av, int bi)
    {
        super(res, rem, av, bi);
        this.rem = rem;
    }

    public int calcFreeSpace()
    {
        if ( label == null || label.text == null ) {
            return -1;
        }
        int sep = label.text.indexOf ( '/' );
        if ( sep > 0 ) {
            String count = label.text.substring ( 0, sep );
            String capacity = label.text.substring ( sep + 1 );
            try {
                return Integer.parseInt ( capacity ) - Integer.parseInt ( count );
            }
            catch ( NumberFormatException nfe ) {
            }
        }
        return -1;
    }

    public int calcCount()
    {
        if ( label == null || label.text == null ) {
            return -1;
        }
        int sep = label.text.indexOf ( '/' );
        if ( sep > 0 ) {
            String count = label.text.substring ( 0, sep );
            try {
                return Integer.parseInt ( count );
            }
            catch ( NumberFormatException nfe ) {
            }
        }
        return -1;
    }

    public int getFreeSpace() throws InterruptedException
    {
        GetFreeSpace gfs = new GetFreeSpace(this);
        NUtils.getUI().core.addTask(gfs);
        return gfs.result();
    }

    public int total() throws InterruptedException
    {
        GetPileSize gps = new GetPileSize(this);
        NUtils.getUI().core.addTask(gps);
        return gps.getResult();
    }

    public void transfer(int amount)
    {
        for (int i = 0; i < amount; i++)
        {
            wdgmsg("xfer2", -1, 1);
        }
    }

    public void put(int amount)
    {
        for (int i = 0; i < amount; i++)
        {
            wdgmsg("xfer2", 1, 1);
        }
    }



    private class TakeButton extends Button{
        public TakeButton(int w, String text) {
            super(w, text);
        }

        @Override
        public void click () {
            int amount = 1;
            try {
                amount = Integer.parseInt(value.text());
            } catch (Exception ignored) {
            }
            if (amount > rem) {
                amount = rem;
            }
            if (amount > 0) {
                transfer(amount);
            }

        }
    }

    protected void added() {
        if(parent instanceof Window) {
            boolean isStockpile = "Stockpile".equals(((Window) parent).cap);
            if(isStockpile) {
                take = new TakeButton(UI.scale(60), "Take");
                value = new TextEntry.NumberValue(UI.scale(40), "");
                parent.add(value, UI.scale(45, 50));
                value.canactivate = true;


                parent.add(take, UI.scale(87, 44));
                take.canactivate = true;
            }
        }
    }
}
