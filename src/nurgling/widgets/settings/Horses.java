package nurgling.widgets.settings;

import haven.*;
import nurgling.conf.HorseHerd;


public class Horses extends Window {
    NEntryListSet els;
    NSettinsSetI totalAdult;
    NSettinsSetI gap;
    NSettinsSetD meta;
    NSettinsSetD enduran;
    NSettinsSetD meatQuality;
    NSettinsSetD hideQuality;
    NSettinsSetD meatq1;
    NSettinsSetD meatq2;
    NSettinsSetD meatqth;
    NSettinsSetD stam1;
    NSettinsSetD stam2;
    NSettinsSetD stamth;
    NSettinsSetD coverbreed;

    CheckBox ic;
    CheckBox dk;
    CheckBox ignorebd;

    String current;
    public Horses() {
        super(new Coord(100,100), "Horses Herds");
        prev = els = add(new NEntryListSet(HorseHerd.getKeySet()) {
            @Override
            public void nsave() {
                String name = this.get();
                if(!name.isEmpty()) {
                    HorseHerd gh = new HorseHerd(name);
                    HorseHerd.set(gh);
                    totalAdult.setVal(gh.adultHorse);
                    gap.setVal(gh.breedingGap);
                    coverbreed.setVal(gh.coverbreed);
                    meatQuality.setVal(gh.meatq);
                    hideQuality.setVal(gh.hideq);
                    enduran.setVal(gh.enduran);
                    meta.setVal(gh.meta);
                    meatq1.setVal(gh.meatquan1);
                    meatqth.setVal(gh.meatquanth);
                    meatq2.setVal(gh.meatquan2);
                    stam1.setVal(gh.stam1);
                    stamth.setVal(gh.stamth);
                    stam2.setVal(gh.stam2);
                    ic.set(gh.ignoreChildren);
                    dk.set(gh.disable_killing);
                }
            }
            @Override
            public void oldsave()
            {
                HorseHerd gh = HorseHerd.get(name.text());
                {
                    gh.adultHorse = totalAdult.get();
                    gh.breedingGap = gap.get();
                    gh.meatq = meatQuality.get();
                    gh.hideq = hideQuality.get();
                    gh.coverbreed = coverbreed.get();
                    gh.meatquan1 = meatq1.get();
                    gh.meatquanth = meatqth.get();
                    gh.meatquan2 = meatq2.get();
                    gh.enduran = enduran.get();
                    gh.meta = meta.get();
                    gh.meatquan1 = meatq1.get();
                    gh.meatquan2 = meatq2.get();
                    gh.meatquanth = meatqth.get();
                    gh.stam1 = stam1.get();
                    gh.stamth = stamth.get();
                    gh.stam2 = stam2.get();
                    gh.ignoreChildren = ic.a;
                    gh.disable_killing = dk.a;
                }
                HorseHerd.set(gh);
            }

            @Override
            public void nchange() {
                HorseHerd gh = HorseHerd.get(name.text());;
                totalAdult.setVal(gh.adultHorse);
                gap.setVal(gh.breedingGap);
                coverbreed.setVal(gh.coverbreed);
                meatQuality.setVal(gh.meatq);
                hideQuality.setVal(gh.hideq);
                meatq1.setVal(gh.meatquan1);
                meatqth.setVal(gh.meatquanth);
                meatq2.setVal(gh.meatquan2);
                enduran.setVal(gh.enduran);
                meta.setVal(gh.meta);
                stam1.setVal(gh.stam1);
                stamth.setVal(gh.stamth);
                stam2.setVal(gh.stam2);
                HorseHerd.setCurrent(name.text());
            }

            @Override
            public void ndelete() {
                HorseHerd.remove(name.text());
                if(HorseHerd.getCurrent()!=null)
                    update(HorseHerd.getCurrent().name);
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

        if(HorseHerd.getCurrent()!=null) {
            ignorebd.set(HorseHerd.getCurrent().ignoreBD);
            dk.set(HorseHerd.getCurrent().disable_killing);
            ic.set(HorseHerd.getCurrent().ignoreChildren);
        }

        prev = totalAdult = add(new NSettinsSetI("Total adult:"), prev.pos("bl").add(0, 5));
        prev = gap = add(new NSettinsSetI("Gap:"), prev.pos("bl").add(0, 5));
        prev = coverbreed = add(new NSettinsSetD("Overbreed ( 0.0 ... 0.3 ):"), prev.pos("bl").add(0, 5));
        prev = add(new Label("Rank settings:"), prev.pos("bl").add(0, 15));
        prev = add(new Label("All coefficients are arbitrary, only relations between them matters."), prev.pos("bl").add(0, 5));
        prev = meatQuality = add(new NSettinsSetD("Meat:"), prev.pos("bl").add(0, 5));
        prev = hideQuality = add(new NSettinsSetD("Hide:"), prev.pos("bl").add(0, 5));
        prev = enduran = add(new NSettinsSetD("Endurance:"), prev.pos("bl").add(0, 5));
        prev = meta = add(new NSettinsSetD("Metabolism:"), prev.pos("bl").add(0, 5));
        prev = add(new Label("Follow stats may be tracked with different coefficients below and above threshold. If you want to ignore threshold, set both coefficients equal."), prev.pos("bl").add(0, 5));
        prev = add(new Label("If you want to track stat up to threshold, but ignore stat gain over threshold simply set second coefficient to zero."), prev.pos("bl").add(0, 5));
        prev = meatq1 = add(new NSettinsSetD("Meat quantity 1:"), prev.pos("bl").add(0, 5));
        prev = meatqth = add(new NSettinsSetD("Meat quantity threshold:"), prev.pos("bl").add(0, 5));
        prev = meatq2 = add(new NSettinsSetD("Meat quantity 2:"), prev.pos("bl").add(0, 5));
        prev = stam1 = add(new NSettinsSetD("Stamina 1:"), prev.pos("bl").add(0, 5));
        prev = stamth = add(new NSettinsSetD("Stamina threshold:"), prev.pos("bl").add(0, 5));
        prev = stam2 = add(new NSettinsSetD("Stamina snout 2:"), prev.pos("bl").add(0, 5));        pack();
    }

    @Override
    public void show() {
        if(HorseHerd.getCurrent()!=null)
            els.update(HorseHerd.getCurrent().name);
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
