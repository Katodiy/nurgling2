package nurgling.widgets.bots;

import haven.*;
import nurgling.NUtils;
import nurgling.conf.NFishingSettings;
import nurgling.i18n.L10n;

import java.util.ArrayList;

public class Fishing extends Window implements Checkable {
    private final Button fishbtn;
    UsingTools usingTools;
    UsingTools fishlines;
    UsingTools baits;
    UsingTools hooks;
    Label baitLab;
    Label targLab;
    CheckBox noPilesCheck;
    CheckBox useInventoryToolsCheck;
    public NFishingSettings prop;
    private FishingTarget fishwnd = null;
    public Fishing() {
        super(UI.scale(200,400), L10n.get("fishing.wnd_title"));
        NFishingSettings fishSet = NFishingSettings.get(NUtils.getUI().sessInfo);
        if (fishSet == null) fishSet = new NFishingSettings("", "");
        final NFishingSettings finalFishSet = fishSet;
        prev = add(new Label(L10n.get("fishing.tool")));

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
                        baitLab.settext(L10n.get("fishing.lure"));
                        targLab.visible = true;
                        fishbtn.visible = true;
                        Fishing.this.pack();
                    }
                    else
                    {
                        baits.tools = UsingTools.initByCategories("Bait");
//                        targets.visible = false;
                        baitLab.settext(L10n.get("fishing.bait"));
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
        if(finalFishSet.tool!=null)
        {
            for(UsingTools.Tool tl : UsingTools.Tools.fishrot)
            {
                if (tl.name.equals(finalFishSet.tool)) {
                    usingTools.s = tl;
                    break;
                }
            }
        }
        prev = add(noPilesCheck = new CheckBox(L10n.get("fishing.no_piles"))
        {
            {
                a = finalFishSet.noPiles;
            }
        }, prev.pos("bl").add(UI.scale(0,5)));
        prev = add(useInventoryToolsCheck = new CheckBox(L10n.get("fishing.use_inv_tools"))
        {
            {
                a = finalFishSet.useInventoryTools;
            }
        }, prev.pos("bl").add(UI.scale(0,5)));
        prev = add(new Label(L10n.get("fishing.fishline")), prev.pos("bl").add(UI.scale(0,5)));
        prev = add(fishlines = new UsingTools(UsingTools.initByCategories("Fishline"), false), prev.pos("bl").add(UI.scale(0,5)));
        if(finalFishSet.fishline!=null)
        {
            for(UsingTools.Tool tl : fishlines.tools)
            {
                if (tl.name.equals(finalFishSet.fishline)) {
                    fishlines.s = tl;
                    break;
                }
            }
        }


        if(usingTools.s==null || !usingTools.s.name.equals("Primitive Casting-Rod")) {
            prev = add(baitLab = new Label(L10n.get("fishing.bait")), prev.pos("bl").add(UI.scale(0,5)));
            prev = add(baits = new UsingTools(UsingTools.initByCategories("Bait"), false), prev.pos("bl").add(UI.scale(0, 5)));
            if (finalFishSet.bait != null) {
                for (UsingTools.Tool tl : baits.tools) {
                    if (tl.name.equals(finalFishSet.bait)) {
                        baits.s = tl;
                        break;
                    }
                }
            }
        }
        else
        {
            prev = add(baitLab = new Label(L10n.get("fishing.lure")), prev.pos("bl").add(UI.scale(0,5)));
            prev = add(baits = new UsingTools(UsingTools.initByCategories("Lures"), false), prev.pos("bl").add(UI.scale(0, 5)));
            if (finalFishSet.bait != null) {
                for (UsingTools.Tool tl : baits.tools) {
                    if (tl.name.equals(finalFishSet.bait)) {
                        baits.s = tl;
                        break;
                    }
                }
            }
        }
        prev = add(new Label(L10n.get("fishing.hook")), prev.pos("bl").add(UI.scale(0,5)));
        prev = add(hooks = new UsingTools(UsingTools.initByCategories("Hooks"), false), prev.pos("bl").add(UI.scale(0,5)));
        if(finalFishSet.hook!=null)
        {
            for(UsingTools.Tool tl : hooks.tools)
            {
                if (tl.name.equals(finalFishSet.hook)) {
                    hooks.s = tl;
                    break;
                }
            }
        }

        prev = add(targLab = new Label(L10n.get("fishing.targets")), prev.pos("bl").add(UI.scale(0,5)));
        if(fishwnd==null) {
            fishwnd = NUtils.getGameUI().add(new FishingTarget(finalFishSet));
        }
        fishwnd.hide();
        prev = add(fishbtn = new Button(UI.scale(150), L10n.get("fishing.fish")){
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
        prev = add(new Button(UI.scale(150), L10n.get("botwnd.start")){
            @Override
            public void click() {
                super.click();
                prop = NFishingSettings.get(NUtils.getUI().sessInfo);
                if (prop != null) {
                    if(usingTools.s!=null)
                        prop.tool = usingTools.s.name;
                    if(fishlines.s!=null)
                        prop.fishline = fishlines.s.name;
                    if(hooks.s!=null)
                        prop.hook = hooks.s.name;
                    if(baits.s!=null)
                        prop.bait = baits.s.name;
                    prop.targets = fishwnd.settings.targets;
                    prop.noPiles = noPilesCheck.a;
                    prop.useInventoryTools = useInventoryToolsCheck.a;
                    NFishingSettings.set(prop);
                }
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
