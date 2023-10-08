package nurgling;

import haven.*;
import nurgling.tasks.*;

public class NISBox extends ISBox
{
    public NISBox(Indir<Resource> res, int rem, int av, int bi)
    {
        super(res, rem, av, bi);
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

    public int getFreeSpace() throws InterruptedException
    {
        GetFreeSpace gfs = new GetFreeSpace(this);
        NUtils.getUI().core.addTask(gfs);
        return gfs.result();
    }

    public void transfer(int amount)
    {
        for (int i = 0; i < amount; i++)
        {
            wdgmsg("xfer2", -1, 1);
        }
    }
}
