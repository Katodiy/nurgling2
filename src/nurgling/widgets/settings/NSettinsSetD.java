package nurgling.widgets.settings;

import nurgling.NUtils;

public class NSettinsSetD extends NSettinsSetW {
    public NSettinsSetD(String label, double val) {
        super(label);
        this.val = val;
    }

    double val;

    public NSettinsSetD(String label) {
        super(label);
        this.val = 0.;
    }

    public void setVal(double val) {
        this.val = val;
        setText(String.valueOf(val));
    }

    @Override
    void parseValue() {
        if(textEntry.text().isEmpty())
        {
            textEntry.settext("0");
            val = 0.;
        }
        try {
            val = Double.parseDouble(textEntry.text());
        }
        catch (NumberFormatException e)
        {
            NUtils.getGameUI().error("Incorrect format");
        }
    }

    public double get() {
        return val;
    }
}
