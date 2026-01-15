/* Preprocessed source code */
package haven.res.ui.tt.stackn;

import haven.*;

/* >tt: StackName */
@haven.FromResource(name = "ui/tt/stackn", version = 3)
public class StackName implements ItemInfo.InfoFactory {
    /* XXX: Remove me. Waiting on custom clients merging get in ItemInfo.Name.Default. */
    public static String getname(ItemInfo.Owner owner) {
	if(owner instanceof ItemInfo.SpriteOwner) {
	    GSprite spr = ((ItemInfo.SpriteOwner)owner).sprite();
	    if(spr instanceof ItemInfo.Name.Dynamic)
		return(((ItemInfo.Name.Dynamic)spr).name());
	}
	if(!(owner instanceof ItemInfo.ResOwner))
	    return(null);
	Resource res = ((ItemInfo.ResOwner)owner).resource();
	Resource.Tooltip tt = res.layer(Resource.tooltip);
	if(tt == null)
		return "";
//	    throw(new RuntimeException("Item resource " + res + " is missing default tooltip"));
	return(tt.text());
    }

    public ItemInfo build(ItemInfo.Owner owner, ItemInfo.Raw raw, Object... args) {
	String nm = getname(owner);
	if(nm == null)
	    return(null);
	return(new ItemInfo.Name(owner, nm + ", stack of"));
    }
}
