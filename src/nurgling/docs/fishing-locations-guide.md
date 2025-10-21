# Fishing Location System

Save and search fishing spots with catch rates, moon phases, and equipment info.

## Saving a Location

When the "This is bait" fishing window appears, click the **Save** button next to any fish name.

**Add screenshot of Save button in fishing window**

The system saves:
- Fish type, catch percentage (the final %), time, and moon phase
- Your fishing equipment (rod, hook, line, bait)
- Map coordinates

## Viewing Saved Locations

**Map Icons**: Fish icons on minimap and full map

**Tooltip**: Hover over icon to see fish name

**Add screenshot of fish location icon with tooltip**

**Details Window**: Right-click icon to view full details

**Add screenshot of Fish Location Details window**

Shows:
- Fish name, catch rate, time, moon phase
- Equipment used (rod, hook, line, bait)
- Delete and Close buttons

## Searching Locations

Click **Fish Search** button (top-right of map window)

**Add screenshot of Fish Search button on map**

### Filters

- **Fish Name**: Select specific fish or "Any"
- **Moon Phase**: Filter by moon phase or "Any"
- **Min Percentage**: Enter number (e.g., 50 for 50%+ catches)

**Add screenshot of Fish Search window**

Click **Search** to see results showing: `Smelt - 13% - Waxing Crescent @ 14:30`

**Click any result** to center the map on that location.

## Storage

Saved to: `C:\Users\[YourName]\AppData\Roaming\Haven and Hearth\fish_locations.nurgling.json`

Persists across game sessions. Back up this file to preserve your data.
