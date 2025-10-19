# Fishing Location Saving & Search System

The **Fishing Location System** allows you to save your fishing spots, complete with detailed information about catch rates, moon phases, and the equipment you used. Never lose track of a good fishing hole again!

## Overview

This system helps you:
- Save fishing locations with all relevant details
- Track which fish are available at each location
- Record catch percentages, time of day, and moon phases
- Remember what fishing equipment you used
- Search and filter your saved locations
- Quickly navigate back to productive fishing spots

## Saving a Fishing Location

### Step 1: Go Fishing

Cast your line at any fishing location in the game. When you catch something, the "This is bait" window will appear showing available fish.

**Add screenshot of "This is bait" fishing window**

### Step 2: Click the Save Button

Next to each fish name in the fishing window, you'll see a small **"Save"** button.

**Add screenshot of Save button next to fish name in fishing window**

When you click **Save**, the system automatically records:
- **Fish type** (e.g., Smelt, Trout, Pike)
- **Catch percentage** (e.g., 13%, 95%) - the final percentage shown after the calculation
- **In-game time** when you saved it (e.g., 14:30)
- **Moon phase** at that moment (e.g., Full Moon, Waxing Crescent)
- **Your fishing equipment**:
  - Hook type
  - Line type
  - Bait or lure used
- **Map coordinates** of your exact position

### Step 3: Confirmation

After clicking Save, you'll see a confirmation message at the bottom of your screen:

```
Saved Smelt location (13%)
```

**Add screenshot of confirmation message**

The location is now saved to your computer and will persist across game sessions.

## Viewing Saved Fishing Locations

### Location Tooltips

Hover your mouse over any saved fish location icon to see a simple tooltip showing the fish name:

**Add screenshot of tooltip showing "Smelt"**

## Viewing Fishing Location Details

### Opening the Details Window

**Right-click** on any saved fish location icon (on either minimap or full map) to open the detailed information window.

**Add screenshot of right-clicking fish location icon**

### Details Window Contents

The Fish Location Details window displays all saved information:

**Add screenshot of Fish Location Details window**

The window shows:
- **Fish name** (e.g., "Smelt")
- **Catch Rate** (e.g., "13%")
- **Time** when saved (e.g., "14:30")
- **Moon phase** when saved (e.g., "Waxing Crescent")
- **Equipment section** showing:
  - Rod: Primitive Casting-Rod
  - Hook: Bone Hook
  - Line: Spindly Fishline
  - Bait: Earthworm

### Buttons

The details window has two buttons:
- **Delete**: Permanently removes this saved location
- **Close**: Closes the window

## Searching for Fishing Locations

### Opening the Fish Search Menu

On the full map window, look for the **"Fish Menu"** button near the bottom-right corner, next to the search bar.

**Add screenshot of Fish Menu button location on map**

Click **Fish Menu** to open the search interface.

### Fish Search Window

The Fish Search window provides powerful filtering options:

**Add screenshot of Fish Search window**

### Search Filters

#### Fish Name Filter
Use the dropdown to select a specific fish type, or choose **"Any"** to see all fish.

**Add screenshot of fish name dropdown**

The dropdown is automatically populated with all fish types you've saved. For example:
- Any
- Asp
- Bream
- Chub
- Pike
- Smelt
- Trout

#### Moon Phase Filter
Use this dropdown to find fish that were caught during a specific moon phase.

**Add screenshot of moon phase dropdown**

Options include:
- Any
- New Moon
- Waxing Crescent
- First Quarter
- Waxing Gibbous
- Full Moon
- Waning Gibbous
- Last Quarter
- Waning Crescent

*Note:* Only moon phases that appear in your saved data will be shown.

#### Minimum Percentage Filter
Enter a number to show only locations where the catch rate was above that percentage.

**Add screenshot of percentage filter field**

Examples:
- Enter `50` to find locations with 50% or higher catch rates
- Enter `0` to see all locations regardless of percentage
- Enter `90` to find only the best fishing spots

### Performing a Search

1. Select your desired filters (fish name, moon phase, minimum percentage)
2. Click the **Search** button

**Add screenshot of Search button**

### Viewing Search Results

The results list displays all matching fishing locations:

**Add screenshot of search results list**

Each result shows:
- Fish name
- Catch percentage
- Moon phase
- Time of day

For example: `Smelt - 13% - Waxing Crescent @ 14:30`

### Navigating to a Result

Click on any result in the list to pan the map to that fishing location.

**Add screenshot of clicking a search result**

The full map will automatically center on the selected fishing spot, and you'll see a confirmation message:

```
Map centered on Smelt location
```

*Note:* If the fishing location is in a different area (different map segment), you'll see a message:
```
Fish location is in a different area
```

## Deleting a Fishing Location

There are two ways to delete a saved fishing location:

### Method 1: From Details Window
1. Right-click the fish location icon on the map
2. Click the **Delete** button in the details window
3. Confirmation message appears: `Removed Smelt location`

### Method 2: Direct Right-Click
On the full map or minimap, right-click a fish location icon and select delete from the details window.

## Tips and Best Practices

### Saving Locations
- **Save multiple fish at the same spot**: If a location has several fish types with good percentages, save each one separately
- **Save during different moon phases**: The same location might have different fish during different moon phases
- **Note the equipment**: The system automatically saves your current fishing setup, so you can remember what worked

### Equipment Tracking
The system records your exact fishing setup when you save a location:
- **Casting rods** are often used with lures for specific fish
- **Bushcraft poles** are used with bait for general fishing
- Different hooks, lines, and baits can affect your results
- Review saved equipment to recreate successful setups

### Moon Phase Importance
Many fish in Haven & Hearth are affected by moon phases:
- Save locations during different phases to build a complete database
- Use the moon phase filter to find fish available during the current moon
- Full Moon often affects certain fish behaviors

### Search Strategies

**Finding the best spots:**
- Set minimum percentage to 70% or higher
- Select "Any" for fish name
- See all your premium fishing locations

**Finding specific fish:**
- Select the fish name
- Set percentage to 0
- Choose "Any" moon phase
- See everywhere that fish appears

**Finding current opportunities:**
- Check the current moon phase (look at your calendar)
- Set the moon phase filter to match
- Find fish available right now

**Finding rare fish:**
- Filter by the rare fish name
- Sort through results to find best percentage
- Note the moon phase and equipment used

## Storage and Persistence

All fishing locations are saved to a file on your computer:
```
C:\Users\[YourName]\AppData\Roaming\Haven and Hearth\fish_locations.nurgling.json
```

This means:
- Your fishing locations persist across game sessions
- They're saved locally, not on the game server
- You can back up this file to preserve your fishing database
- If you reinstall the game, copy this file to keep your locations

## Icon Colors

- **Blue circles**: Saved fishing locations (on minimap and full map)
- The icon appears on both the minimap and the full map for easy visibility

## Troubleshooting

### "I don't see the Save button in the fishing window"
- Make sure you're looking at the "This is bait" window that appears when you catch something
- The Save button appears next to each fish name in the list
- If you don't see it, you may need to update your client

### "Clicking a search result says 'different area'"
- The saved location is in a different part of the world (different map segment)
- You'll need to travel to that area first, then use the search again
- The system can only pan to locations in your current map area

### "My saved locations disappeared"
- Check if the file still exists at the AppData location listed above
- The file might have been corrupted or deleted
- Consider backing up this file regularly

### "The percentage doesn't match what I remember"
- The system saves the FINAL percentage (the last one shown after all calculations)
- This is the result after base percentage × multipliers
- This is the actual catch rate you experienced

## Advanced Usage

### Building a Fishing Database
Create a comprehensive fishing guide for your village:
1. Visit fishing spots around your village
2. Save all fish at each location during different moon phases
3. Note the best percentages for each fish
4. Share location coordinates with your villagers

### Seasonal Planning
Use the search system to plan your fishing activities:
- Check which fish are available during current moon
- Identify the best equipment for target fish
- Plan fishing trips to locations with multiple high-percentage fish

### Equipment Experimentation
Track which equipment combinations work best:
1. Try different rods, hooks, lines, and bait
2. Save locations with each combination
3. Compare percentages to find optimal setups
4. Review saved equipment details to replicate success

## Summary

The Fishing Location System helps you:
- ✓ Never forget a good fishing spot
- ✓ Track what equipment works best
- ✓ Plan fishing around moon phases
- ✓ Find fish quickly with powerful search
- ✓ Build a comprehensive fishing database
- ✓ Share knowledge with your village

Happy fishing! 🎣
