package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NFishingSettings;

import java.util.ArrayList;

public class Fishing extends Window implements Checkable {
    private final Button fishbtn;
    UsingTools usingTools;
    UsingTools fishlines;
    UsingTools baits;
    UsingTools hooks;
    Label baitLab;
    Label targLab;
    public NFishingSettings prop;
    private FishingTarget fishwnd = null;
    public Fishing() {
        super(UI.scale(200,400), "Auto Fishing");
        NFishingSettings fishSet = NFishingSettings.get(NUtils.getUI().sessInfo);
        prev = add(new Label("Tool:"));

        prev = add(usingTools = new UsingTools(UsingTools.Tools.fishrot, false)
        {
            @Override
            public void update() {
                if(s!=null)
                {
                    if(s.name.equals("Primitive Casting-Rod"))
                    {
                        baits.tools = UsingTools.initByCategories("Lures");
//                        targets.visible = true;
                        baitLab.settext("Lure:");
                        targLab.visible = true;
                        fishbtn.visible = true;
                        Fishing.this.pack();
                    }
                    else
                    {
                        baits.tools = UsingTools.initByCategories("Bait");
//                        targets.visible = false;
                        baitLab.settext("Bait:");
                        targLab.visible = false;
                        fishbtn.visible = false;
                        Fishing.this.pack();
                    }
                    boolean found = false;
                    if(baits.s!=null) {
                        for (Tool t : baits.tools) {
                            if (baits.s==t) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if(!found)
                    {
                        baits.s = null;
                    }
                }
            }
        }, prev.pos("bl").add(UI.scale(0,5)));
        if(fishSet.tool!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.fishrot)
            {
                if (tl.name.equals(fishSet.tool)) {
                    usingTools.s = tl;
                    break;
                }
            }
        }
        prev = add(new CheckBox("Repair from container")
        {
            {
                a = fishSet.repfromcont;
            }
        }, prev.pos("bl").add(UI.scale(0,5)));
        prev = add(new Label("Fish line:"), prev.pos("bl").add(UI.scale(0,5)));
        prev = add(fishlines = new UsingTools(UsingTools.initByCategories("Fishline"), false), prev.pos("bl").add(UI.scale(0,5)));
        if(fishSet.fishline!=null)
        {
            for(UsingTools.Tool tl : fishlines.tools)
            {
                if (tl.name.equals(fishSet.fishline)) {
                    fishlines.s = tl;
                    break;
                }
            }
        }


        if(usingTools.s==null || !usingTools.s.name.equals("Primitive Casting-Rod")) {
            prev = add(baitLab = new Label("Bite:"), prev.pos("bl").add(UI.scale(0,5)));
            prev = add(baits = new UsingTools(UsingTools.initByCategories("Bait"), false), prev.pos("bl").add(UI.scale(0, 5)));
            if (fishSet.bait != null) {
                for (UsingTools.Tool tl : baits.tools) {
                    if (tl.name.equals(fishSet.bait)) {
                        baits.s = tl;
                        break;
                    }
                }
            }
        }
        else
        {
            prev = add(baitLab = new Label("Lure:"), prev.pos("bl").add(UI.scale(0,5)));
            prev = add(baits = new UsingTools(UsingTools.initByCategories("Lures"), false), prev.pos("bl").add(UI.scale(0, 5)));
            if (fishSet.bait != null) {
                for (UsingTools.Tool tl : baits.tools) {
                    if (tl.name.equals(fishSet.bait)) {
                        baits.s = tl;
                        break;
                    }
                }
            }
        }
        prev = add(new Label("Hook:"), prev.pos("bl").add(UI.scale(0,5)));
        prev = add(hooks = new UsingTools(UsingTools.initByCategories("Hooks"), false), prev.pos("bl").add(UI.scale(0,5)));
        if(fishSet.hook!=null)
        {
            for(UsingTools.Tool tl : hooks.tools)
            {
                if (tl.name.equals(fishSet.hook)) {
                    hooks.s = tl;
                    break;
                }
            }
        }

        prev = add(targLab = new Label("Targets:"), prev.pos("bl").add(UI.scale(0,5)));
        if(fishwnd==null) {
            fishwnd = NUtils.getGameUI().add(new FishingTarget(fishSet));
        }
        fishwnd.hide();
        prev = add(fishbtn = new Button(UI.scale(150),"Fish"){
            @Override
            public void click() {
                super.click();
                fishwnd.show();
                fishwnd.c = Fishing.this.c.add(this.c).add(UI.scale(0,25));
            }
        }, prev.pos("bl").add(UI.scale(0,5)));
        if(usingTools.s!=null && usingTools.s.name.equals("Primitive Casting-Rod")) {
            fishbtn.show();
            targLab.show();
        }
        else
        {
            fishbtn.hide();
            targLab.hide();
        }
        prev = add(new Button(UI.scale(150), "Start"){
            @Override
            public void click() {
                super.click();
                prop = NFishingSettings.get(NUtils.getUI().sessInfo);
                if(usingTools.s!=null)
                    prop.tool = usingTools.s.name;
                if(fishlines.s!=null)
                    prop.fishline = fishlines.s.name;
                if(hooks.s!=null)
                    prop.hook = hooks.s.name;
                if(baits.s!=null)
                    prop.bait = baits.s.name;
                ArrayList<String> fish = new ArrayList<>();
                for(FishingTarget.FishItem item : fishwnd.fitems)
                {
                    if(item.enabled.a)
                        fish.add(item.text.text());
                }
                prop.targets = fish;
                NFishingSettings.set(prop);
                isReady = true;
                fishwnd.destroy();
            }
        }, prev.pos("bl").add(UI.scale(0,5)));
        pack();

    }

    @Override
    public boolean check() {
        return isReady;
    }

    boolean isReady = false;
}
