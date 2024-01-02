package nurgling.widgets.settings;

import nurgling.NUtils;

public class NSettinsSetI extends NSettinsSetW {
    public NSettinsSetI(String label, int val) {
        super(label);
        this.val = val;
    }

    public NSettinsSetI(String label) {
        super(label);
        this.val = 0;
    }

    public void setVal(int val) {
        this.val = val;
        setText(String.valueOf(val));
    }

    int val;

    @Override
    void parseValue() {
        if(textEntry.text().isEmpty())
        {
            textEntry.settext("0");
            val=0;
        }
        try {
            val=Integer.parseInt(textEntry.text());
        }
        catch (NumberFormatException e)
        {
            NUtils.getGameUI().error("Incorrect format");
        }
    }

    public int get() {
        return val;
    }
}
