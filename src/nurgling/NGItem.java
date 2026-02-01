package nurgling;

import haven.*;
import static haven.Inventory.sqsz;

import haven.res.ui.stackinv.ItemStack;
import haven.res.ui.tt.slots.ISlots;
import haven.res.ui.tt.stackn.StackName;
import monitoring.ItemWatcher;
import nurgling.iteminfo.NCuriosity;
import nurgling.iteminfo.NFoodInfo;
import nurgling.tools.VSpec;
import nurgling.widgets.NQuestInfo;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NGItem extends GItem
{
    public boolean isSearched = false;
    public boolean isQuested = false;
    int lastQuestUpdate = 0;
    String name = null;
    public Float quality = null;
    public long meterUpdated = 0;
    public int hardArmor = 0;
    public int softArmor = 0;

    boolean sent = false;
    boolean checkedForFood = false; // Optimization: only check NFoodInfo once
    boolean addedToInventoryCache = false; // Track if item was added to container cache for DB sync
    boolean isStackContainer = false; // True if this item is a stack container (holds other items)
    ItemWatcher.ItemInfo cachedItemInfo = null; // Reference to ItemInfo in cache for removal
    public NGItem(Indir<Resource> res, Message sdt)
    {
        super(res, sdt);
    }

    public NGItem(Indir<Resource> res)
    {
        super(res);
    }

    public String name()
    {
        return name;
    }

    public boolean needlongtip()
    {
        for (ItemInfo inf : info()) {
            if (inf instanceof NFoodInfo) {
                return ((NFoodInfo) inf).needToolTip;
            }
            else if (inf instanceof NCuriosity) {
                return ((NCuriosity) inf).needUpdate();
            }
            if (inf instanceof ISlots) {
                return this.ui.modshift!=((ISlots)inf).isShifted;
            }
        }
        return false;
    }

    public ArrayList<NContent> content(){
        return content;
    }

    public boolean findContent(String item) {
        for(NGItem.NContent content : content())
        {
            if(content.name().endsWith(item))
            {
                return true;
            }

        }
        return false;
    }

    public static class NContent
    {
        private double quality = -1;
        private String name = null;

        public NContent(double quality, String name)
        {
            this.quality = quality;
            this.name = name;
        }

        public double quality()
        {
            return quality;
        }

        public String name()
        {
            return name;
        }

        public String type() {
            String regex = "of\\s+([A-Za-z]+)\\s*";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(name);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        }

    }

    private ArrayList<NContent> content = new ArrayList<>();

    public Coord sprsz()
    {
        if (spr != null)
        {
            return spr.sz().div(new Coord(sqsz.x - UI.scale(1), sqsz.y - UI.scale(1)));
        }
        return null;
    }

    @Override
    public void tick(double dt)
    {
        super.tick(dt);
        if (name == null && spr != null)
        {
            if (!res.get().name.contains("coin"))
            {
                if (res.get() != null)
                {
                    name = ItemInfo.Name.Default.get(this);
                }
            }
            else
            {
                name = StackName.getname(this);
            }
            if(name!=null)
            {
                if(NUtils.getGameUI().map.clickedGob!=null)
                {
                    // Exclude tools from LPExplorer tracking
                    if(!name.contains(" Axe") && !name.contains(" Saw"))
                    {
                        VSpec.checkLpExplorer(NUtils.getGameUI().map.clickedGob.gob, name);
                    }
                }
                
            }

        }
        if(name!= null) {
            // Check if this is a stack container (has contents with ItemStack)
            // This must be checked early while contents is still available
            if (!isStackContainer && contents != null && contents instanceof ItemStack) {
                isStackContainer = true;
            }
            
            // Check if transfer attempt timed out (item still exists after TRANSFER_TIMEOUT_TICKS)
            // This means transfer failed (e.g. no space in target inventory)
            if (wasTransferred && transferAttemptTick > 0) {
                long currentTick = NUtils.getTickId();
                if (currentTick > transferAttemptTick + TRANSFER_TIMEOUT_TICKS) {
                    // Transfer failed - reset flag so item uses normal removal logic
                    wasTransferred = false;
                    transferAttemptTick = 0;
                }
            }
            
            // Try to add to inventory cache for DB sync
            // This is checked every tick until successfully added (quality might not be ready initially)
            if (!addedToInventoryCache && (Boolean) NConfig.get(NConfig.Key.ndbenable)) {
                tryAddToInventoryCache();
            }
            if((Boolean)NConfig.get(NConfig.Key.ndbenable)) {
                // Optimization: only check NFoodInfo once per item after info is loaded
                // checkedForFood prevents repeated getInfo() calls every tick
                if (!sent && !checkedForFood && info != null) {
                    NFoodInfo foodInfo = getInfo(NFoodInfo.class);
                    if (foodInfo != null) {
                        checkedForFood = true;
                        
                        // Early cache check using quick key (name + energy)
                        // This prevents creating tasks for recipes we've already seen
                        String quickKey = name + "|" + (int)(foodInfo.energy() * 100);
                        if (NCore.isRecipeQuickCached(quickKey)) {
                            sent = true; // Already processed, skip
                            nurgling.db.DatabaseManager.incrementSkippedRecipe();
                        } else {
                            NCore.addRecipeQuickCache(quickKey);
                            sent = true; // Set immediately to prevent duplicate submissions
                            ui.core.writeNGItem(this);
                        }
                    } else {
                        // Not a food item - mark as checked so we don't check every tick
                        checkedForFood = true;
                    }
                }
            }
            if (lastQuestUpdate < NQuestInfo.lastUpdate.get()) {
                isQuested = NUtils.getGameUI().questinfo.isQuestedItem(this);
                lastQuestUpdate = NQuestInfo.lastUpdate.get();
            }
        }
    }


    // Flag to indicate item was transferred/dropped (should be removed from cache immediately if successful)
    private boolean wasTransferred = false;
    private long transferAttemptTick = 0;
    private static final int TRANSFER_TIMEOUT_TICKS = 30; // If item still exists after this many ticks, transfer failed
    
    public boolean wasTransferred() {
        return wasTransferred;
    }
    
    @Override
    public void wdgmsg(String msg, Object... args)
    {
        if (name != null)
        {
            if (msg.equals("take") || (msg.equals("iact")))
            {
                if (NUtils.getGameUI() != null && NUtils.getGameUI().getCharInfo() != null)
                {
                    NUtils.getGameUI().getCharInfo().setCandidate(name());
                    if (msg.equals("iact"))
                    {
                        NUtils.getGameUI().getCharInfo().setFlowerCandidate(this);
                    }
                }
            }
            // Mark item as transferred for immediate cache removal (if successful)
            if (msg.equals("transfer") || msg.equals("drop")) {
                wasTransferred = true;
                transferAttemptTick = NUtils.getTickId();
            }
        }
        super.wdgmsg(msg, args);
    }


    @Override
    public void uimsg(String name, Object... args) {
        super.uimsg(name, args);
        if(name.equals("tt") || name.equals("meter")) {
            meterUpdated = System.currentTimeMillis();
        }
    }
    @Override
    protected void updateraw()
    {
        if (rawinfo != null)
        {
            content.clear();
            for (Object o : rawinfo.data)
            {
                if (o instanceof Object[])
                {
                    Object[] a = (Object[]) o;
                    if (a[0] instanceof Integer)
                    {
                        if(nurgling.NUtils.getUI().sess!=null) {
                            String resName = NUtils.getUI().sess.getResName((Integer) a[0]);
                            if (resName != null) {
                                switch (resName) {
                                    case "ui/tt/q/quality":
                                        if (a.length >= 2) quality = (Float) a[1];
                                        break;
                                    case "ui/tt/armor":
                                        if (a.length >= 3) {
                                            hardArmor = (Integer) a[1];  // Р–РµСЃС‚РєР°СЏ Р±СЂРѕРЅСЏ
                                            softArmor = (Integer) a[2];  // РњСЏРіРєР°СЏ Р±СЂРѕРЅСЏ
                                        }
                                        break;
                                    case "ui/tt/cont":
                                        double q = -1;
                                        String name = null;
                                        for (Object so : a) {
                                            if (so instanceof Object[]) {
                                                Object[] cont = (Object[]) so;
                                                for (Object sso : cont) {
                                                    if (sso instanceof Object[]) {
                                                        Object[] b = (Object[]) sso;
                                                        if (b[0] instanceof Integer) {
                                                            String resName2 = NUtils.getUI().sess.getResName((Integer) b[0]);
                                                            if (b.length >= 2) if (resName2 != null) {
                                                                if (resName2.equals("ui/tt/cn")) {
                                                                    name = (String) b[1];
                                                                } else if (resName2.equals("ui/tt/q/quality")) {
                                                                    q = (Float) b[1];
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (name != null && q != -1) {
                                            content.add(new NContent(q, name));
                                        }
                                        break;
                                    case "ui/tt/coin":
                                        this.name = (String) a[1];
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public <C extends ItemInfo> C getInfo(Class<C> c){
        if(info!=null)
        {
            for(ItemInfo inf : info)
            {
                if(c.isInstance(inf))
                    return (C)inf;
            }
        }
        return null;
    }
    
    /**
     * Try to add this item to the parent inventory's cache for DB sync.
     * Called when item name is first determined.
     */
    private void tryAddToInventoryCache() {
        if (addedToInventoryCache || name == null) return;
        
        // Find the parent NInventory
        NInventory inv = findParentInventory();
        if (inv == null) {
            return; // No inventory, skip silently
        }
        
        // Check if this inventory should be indexed
        if (!inv.isIndexable()) {
            addedToInventoryCache = true; // Mark as handled, but don't add to cache
            return;
        }
        
        // Skip if no container hash yet
        if (inv.parentGob == null || inv.parentGob.ngob == null || inv.parentGob.ngob.hash == null) {
            return; // Will try again later
        }
        
        String containerHash = inv.parentGob.ngob.hash;
        boolean fromStack = (parent instanceof haven.res.ui.stackinv.ItemStack);
        
        // Skip stack containers (items that hold other items inside)
        // The isStackContainer flag is set in tick() when contents is available
        // Also check: if quality is null and NOT from stack, it's likely a stack container
        if (isStackContainer && !fromStack) {
            System.out.println("NGItem.cache: Skipped stack container (flag): " + name);
            addedToInventoryCache = true;
            return;
        }
        
        // Additional check: items with null quality that are NOT from inside a stack are stack containers
        // (Items from inside stacks have quality, stack containers have null quality)
        if (quality == null && !fromStack) {
            // Double check by looking at contents
            if (contents != null && contents instanceof ItemStack) {
                isStackContainer = true;
                System.out.println("NGItem.cache: Skipped stack container (contents): " + name);
                addedToInventoryCache = true;
                return;
            }
            // Also check Amount info - stacks have this
            if (getInfo(GItem.Amount.class) != null) {
                isStackContainer = true;
                System.out.println("NGItem.cache: Skipped stack container (amount): " + name);
                addedToInventoryCache = true;
                return;
            }
        }
        
        // Skip negative quality items
        if (quality != null && quality < 0) {
            addedToInventoryCache = true;
            return;
        }
        
        // Calculate quality - items with null/0 quality should not be saved
        // (they are stack containers or items without quality info)
        if (quality == null || quality <= 0) {
            System.out.println("NGItem.cache: Skipped zero quality: " + name + " q=" + quality);
            addedToInventoryCache = true;
            return;
        }
        
        double q = quality;
        
        // Determine coordinates and stack index
        Coord itemCoord;
        int stackIndex = -1;
        
        if (fromStack) {
            // Item is inside a stack - use stack container's coords + item's index in stack
            Widget stackParent = parent.parent; // ContentsWindow
            if (stackParent instanceof GItem.ContentsWindow) {
                GItem.ContentsWindow contWnd = (GItem.ContentsWindow) stackParent;
                GItem stackContainer = contWnd.cont;
                if (stackContainer != null && stackContainer.parent instanceof NInventory) {
                    // Get stack container's WItem to find its coords
                    for (Widget w = ((NInventory)stackContainer.parent).child; w != null; w = w.next) {
                        if (w instanceof WItem && ((WItem)w).item == stackContainer) {
                            itemCoord = ((WItem)w).c;
                            // Get index in stack by counting items before this one
                            if (parent instanceof haven.res.ui.stackinv.ItemStack) {
                                haven.res.ui.stackinv.ItemStack stack = (haven.res.ui.stackinv.ItemStack) parent;
                                int idx = 0;
                                for (java.util.Map.Entry<GItem, WItem> e : stack.wmap.entrySet()) {
                                    if (e.getKey() == this) {
                                        stackIndex = idx;
                                        break;
                                    }
                                    idx++;
                                }
                            }
                            cachedItemInfo = new ItemWatcher.ItemInfo(name, q, itemCoord, containerHash, stackIndex);
                            inv.iis.add(cachedItemInfo);
                            addedToInventoryCache = true;
                            inv.lastUpdate = NUtils.getTickId();
                            System.out.println("NGItem.cache: ADDED (stack): " + name + " q=" + q + " idx=" + stackIndex + " iis.size=" + inv.iis.size());
                            return;
                        }
                    }
                }
            }
            // Fallback if we couldn't find coords
            itemCoord = wi != null ? wi.c : Coord.z;
        } else {
            // Regular item - use its own coords
            itemCoord = wi != null ? wi.c : Coord.z;
        }
        
        cachedItemInfo = new ItemWatcher.ItemInfo(name, q, itemCoord, containerHash, stackIndex);
        inv.iis.add(cachedItemInfo);
        addedToInventoryCache = true;
        inv.lastUpdate = NUtils.getTickId();
        System.out.println("NGItem.cache: ADDED: " + name + " q=" + q + " iis.size=" + inv.iis.size());
    }
    
    /**
     * Find the parent NInventory for this item (handles items inside stacks)
     */
    private NInventory findParentInventory() {
        if (parent instanceof NInventory) {
            return (NInventory) parent;
        } else if (parent instanceof haven.res.ui.stackinv.ItemStack) {
            // Item inside a stack - find NInventory through ContentsWindow
            Widget stackParent = parent.parent; // ContentsWindow
            if (stackParent instanceof GItem.ContentsWindow) {
                GItem.ContentsWindow contWnd = (GItem.ContentsWindow) stackParent;
                GItem stackContainer = contWnd.cont;
                if (stackContainer != null && stackContainer.parent instanceof NInventory) {
                    return (NInventory) stackContainer.parent;
                }
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        if (name != null && (Boolean) NConfig.get(NConfig.Key.ndbenable)) {
            // Try to add to cache if not already added (handles fast open/close when tick() didn't run)
            if (!addedToInventoryCache) {
                tryAddToInventoryCache();
            }
            
            // If item was added to cache, handle removal
            if (addedToInventoryCache && cachedItemInfo != null) {
                NInventory inv = findParentInventory();
                
                if (inv != null) {
                    if (wasTransferred) {
                        // Item was transferred/dropped - remove from cache immediately
                        inv.iis.removeIf(item -> 
                            item.name.equals(cachedItemInfo.name) &&
                            item.c.equals(cachedItemInfo.c) &&
                            item.q == cachedItemInfo.q &&
                            item.stackIndex == cachedItemInfo.stackIndex
                        );
                        System.out.println("NGItem.destroy: Immediate removal (transferred): " + name);
                    } else {
                        // Schedule cache removal with a delay
                        // If container closes (reqdestroy), pending removals are cleared and cache is synced
                        // If container stays open (item consumed), the removal will be processed in tick()
                        long removeAtTick = NUtils.getTickId() + 15; // 15 ticks delay
                        inv.pendingCacheRemovals.add(new NInventory.PendingCacheRemoval(cachedItemInfo, removeAtTick));
                        System.out.println("NGItem.destroy: Scheduled removal: " + name + " at tick " + removeAtTick);
                    }
                }
            }
        }
        super.destroy();
    }
    
    /**
     * Generate a hash for this item for database identification
     * Must match ItemWatcher.generateItemHash() format
     */
    private String generateItemHash() {
        if (name == null || wi == null) return null;
        // Format quality the same way as tryAddToInventoryCache and ItemWatcher.ItemInfo
        // Use 0 for null/negative quality to match what we store in cache
        double q = (quality != null && quality > 0) ? quality : 0;
        q = Double.parseDouble(Utils.odformat2(q, 2));
        String data = name + wi.c.toString() + q;
        return NUtils.calculateSHA256(data);
    }

    public static boolean validateItem(WItem item)
    {
        if ((((NGItem) item.item).name()) == null) {
            return false;
        }
        if(((NGItem) item.item).quality == null && ((NGItem) item.item).getInfo(GItem.Amount.class)!=null)
        {
            return (item.item.contents != null && !((ItemStack) item.item.contents).wmap.isEmpty()) || ((NGItem) item.item).name().contains("Pickling Jar");
        }
        return true;
    }
}
