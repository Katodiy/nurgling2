package nurgling.tasks;

import haven.GItem;
import haven.WItem;
import nurgling.NGItem;
import nurgling.NUtils;
import nurgling.tools.NParser;
import nurgling.widgets.NEquipory;

import java.util.ArrayList;
import java.util.Arrays;

public class WaitItemInEquip implements NTask
{
    String name;
    GItem item = null;
    ArrayList<NEquipory.Slots> slots = new ArrayList<>();
    public WaitItemInEquip(String name,NEquipory.Slots slot)
    {
        this.name = name;
        this.slots.add(slot);
    }

    public WaitItemInEquip(WItem item,NEquipory.Slots slot)
    {
        this.item = item.item;
        this.slots.add(slot);
    }

    public WaitItemInEquip(WItem item,NEquipory.Slots []slot)
    {
        this.item = item.item;
        this.slots.addAll(Arrays.asList(slot));
    }

    public WaitItemInEquip(GItem item,NEquipory.Slots slot)
    {
        this.item = item;
        this.slots.add(slot);
    }

    public WaitItemInEquip()
    {

    }

    @Override
    public boolean check() {
        if (NUtils.getEquipment() == null) {
            return false;
        }
        if (item != null) {
            if (((NGItem) item).name() == null)
                return false;
            else
                name = ((NGItem) item).name();
            WItem res;
            for(NEquipory.Slots slot : slots) {
                if ((res = NUtils.getEquipment().quickslots[slot.idx]) != null &&
                        res.item.info != null &&
                        ((NGItem) res.item).name() != null &&
                        NParser.checkName(((NGItem) res.item).name(), name))
                    return true;
            }
        } else {
            WItem res;
            for(NEquipory.Slots slot : slots) {
                if((res = NUtils.getEquipment().quickslots[slot.idx]) != null &&
                        res.item.info != null &&
                        ((NGItem) res.item).name() != null)
                    return true;
            }
        }
        return false;
    }

}
