package nurgling.widgets.settings;

import haven.*;
import nurgling.conf.CowsHerd;

public class Cows extends Window {
    NEntryListSet els;
    NSettinsSetI totalAdult;
    NSettinsSetI gap;
    NSettinsSetD milkQuality;
    NSettinsSetD meatQuality;
    NSettinsSetD hideQuality;
    NSettinsSetD meatq1;
    NSettinsSetD meatq2;
    NSettinsSetD meatqth;
    NSettinsSetD milk1;
    NSettinsSetD milk2;
    NSettinsSetD milkth;
    NSettinsSetD coverbreed;

    CheckBox ic;
    CheckBox dk;
    CheckBox ignorebd;
    CheckBox ignoreqp;

    public Cows() {
        super(new Coord(100,100), "Cows Herds");
        prev = els = add(new NEntryListSet(CowsHerd.getKeySet()) {
            @Override
            public void nsave() {
                String name = this.get();
                if(!name.isEmpty()) {
                    CowsHerd gh = new CowsHerd(name);
                    CowsHerd.set(gh);
                    totalAdult.setVal(gh.adultCows);
                    gap.setVal(gh.breedingGap);
                    coverbreed.setVal(gh.coverbreed);
                    meatQuality.setVal(gh.meatq);
                    milkQuality.setVal(gh.milkq);
                    hideQuality.setVal(gh.hideq);
                    meatq1.setVal(gh.meatquan1);
                    meatqth.setVal(gh.meatquanth);
                    meatq2.setVal(gh.meatquan2);
                    milk1.setVal(gh.milkquan1);
                    milkth.setVal(gh.milkquanth);
                    milk2.setVal(gh.milkquan2);
                    ic.set(gh.ignoreChildren);
                    dk.set(gh.disable_killing);
                    ignorebd.set(gh.ignoreBD);
                    ignoreqp.set(gh.disable_q_percentage);
                }
            }
            @Override
            public void oldsave()
            {
                CowsHerd gh = CowsHerd.get(name.text());
                {
                    gh.adultCows = totalAdult.get();
                    gh.breedingGap = gap.get();
                    gh.coverbreed = coverbreed.get();
                    gh.meatq = meatQuality.get();
                    gh.milkq = milkQuality.get();
                    gh.hideq = hideQuality.get();
                    gh.meatquan1 = meatq1.get();
                    gh.meatquanth = meatqth.get();
                    gh.meatquan2 = meatq2.get();
                    gh.milkquan1 = milk1.get();
                    gh.milkquanth = milkth.get();
                    gh.milkquan2 = milk2.get();
                    gh.disable_killing = dk.a;
                    gh.ignoreBD = ignorebd.a;
                    gh.ignoreChildren = ic.a;
                    gh.disable_q_percentage = ignoreqp.a;
                }
                CowsHerd.set(gh);
            }

            @Override
            public void nchange() {
                CowsHerd gh = CowsHerd.get(name.text());;
                totalAdult.setVal(gh.adultCows);
                gap.setVal(gh.breedingGap);
                coverbreed.setVal(gh.coverbreed);
                meatQuality.setVal(gh.meatq);
                milkQuality.setVal(gh.milkq);
                hideQuality.setVal(gh.hideq);
                meatq1.setVal(gh.meatquan1);
                meatqth.setVal(gh.meatquanth);
                meatq2.setVal(gh.meatquan2);
                milk1.setVal(gh.milkquan1);
                milkth.setVal(gh.milkquanth);
                milk2.setVal(gh.milkquan2);
                ic.set(gh.ignoreChildren);
                dk.set(gh.disable_killing);
                ignorebd.set(gh.ignoreBD);
                ignoreqp.set(gh.disable_q_percentage);
                CowsHerd.setCurrent(name.text());
            }

            @Override
            public void ndelete() {
                CowsHerd.remove(name.text());
                if(CowsHerd.getCurrent()!=null)
                    update(CowsHerd.getCurrent().name);
                else
                    update("");
            }
        });
        prev = add(new Label("Main settings:"), prev.pos("bl").add(0, 5));
        ic = (CheckBox)(prev = add (new CheckBox("Save cubs"){
            @Override
            public void changed(boolean val) {
                super.changed(val);
            }
        }, prev.pos("bl").add(0, UI.scale(5))));

        dk = (CheckBox)(prev = add (new CheckBox("Disable slaughting"){
            @Override
            public void changed(boolean val) {
                super.changed(val);
            }
        }, prev.pos("bl").add(0, UI.scale(5))));

        ignorebd = (CheckBox)(prev = add (new CheckBox("Ignore breading for female"){
            @Override
            public void changed(boolean val) {
                super.changed(val);
            }
        }, prev.pos("bl").add(0, UI.scale(5))));

        ignoreqp = (CheckBox)(prev = add (new CheckBox("Ignore quality for % score") {
            @Override
            public void changed(boolean val) {
                super.changed(val);
            }
        }, prev.pos("bl").add(0, UI.scale(5))));

        if(CowsHerd.getCurrent()!=null) {
            ignoreqp.set(CowsHerd.getCurrent().disable_q_percentage);
            ignorebd.set(CowsHerd.getCurrent().ignoreBD);
            dk.set(CowsHerd.getCurrent().disable_killing);
            ic.set(CowsHerd.getCurrent().ignoreChildren);
        }

        prev = totalAdult = add(new NSettinsSetI("Total adult:"), prev.pos("bl").add(0, 5));
        prev = gap = add(new NSettinsSetI("Gap:"), prev.pos("bl").add(0, 5));
        prev = coverbreed = add(new NSettinsSetD("Overbreed ( 0.0 ... 0.3 ):"), prev.pos("bl").add(0, 5));
        prev = add(new Label("Rank settings:"), prev.pos("bl").add(0, 15));
        prev = add(new Label("All coefficients are arbitrary, only relations between them matters."), prev.pos("bl").add(0, 5));
        prev = meatQuality = add(new NSettinsSetD("Meat:"), prev.pos("bl").add(0, 5));
        prev = milkQuality = add(new NSettinsSetD("Milk:"), prev.pos("bl").add(0, 5));
        prev = hideQuality = add(new NSettinsSetD("Hide:"), prev.pos("bl").add(0, 5));
        prev = add(new Label("Follow stats may be tracked with different coefficients below and above threshold. If you want to ignore threshold, set both coefficients equal."), prev.pos("bl").add(0, 5));
        prev = add(new Label("If you want to track stat up to threshold, but ignore stat gain over threshold simply set second coefficient to zero."), prev.pos("bl").add(0, 5));
        prev = meatq1 = add(new NSettinsSetD("Meat quantity 1:"), prev.pos("bl").add(0, 5));
        prev = meatqth = add(new NSettinsSetD("Meat quantity threshold:"), prev.pos("bl").add(0, 5));
        prev = meatq2 = add(new NSettinsSetD("Meat quantity 2:"), prev.pos("bl").add(0, 5));
        prev = milk1 = add(new NSettinsSetD("Milk quantity 1:"), prev.pos("bl").add(0, 5));
        prev = milkth = add(new NSettinsSetD("Milk quantity threshold:"), prev.pos("bl").add(0, 5));
        prev = milk2 = add(new NSettinsSetD("Milk quantity 2:"), prev.pos("bl").add(0, 5));
        pack();
    }

    @Override
    public void show() {
        if(CowsHerd.getCurrent()!=null)
            els.update(CowsHerd.getCurrent().name);
        super.show();
    }

    @Override
    public void wdgmsg(String msg, Object... args) {
        if(msg.equals("close"))
        {
            hide();
        }
        else {
            super.wdgmsg(msg, args);
        }
    }

    @Override
    public void resize(Coord sz) {
        super.resize(sz);
        els.resize(sz.x-UI.scale(5), els.sz.y);
    }
}
