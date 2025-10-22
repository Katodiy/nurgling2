# Fishing Locations Save System

The **Fishing Locations Save** system allows you to save fishing spots, including catch rates, moon phases, time of day, and equipment used.

## How It Works

The system records detailed fishing data when you save locations:

1. Fish type and catch percentage
2. Time of day and moon phase
3. Map coordinates
4. Fishing equipment (rod, hook, line, bait)
5. Timestamp of when location was saved

## Saving a Fishing Location

When the fishing bite window appears:

![Fishing Window with Save Button](../images/features/fish-save.png)

Click the save button for the fish to be saved to the persistent file. The location is automatically added to your map and database.

## Viewing Saved Locations on Map

Fishing locations appear as fish icons on both the minimap and full map:

![Fish Location Icons on Map](../images/features/fish-location-icons.png)

### Map Features

- **Fish Icons:** Each saved location displays as a fish icon
- **Tooltip:** Hover over any icon to see the fish name
- **Right-Click Details:** Right-click any icon to view full information or delete this location

## Fish Location Details Window

Right-click a fish icon to open the detailed information window:

![Fish Location Details Window](../images/features/fish-location-details.png)

## Searching Fishing Locations

Access the search feature through the map window (top right corner of full map window):

![Fish Search Button](../images/features/fish-search-menu.png)

Click the **Fish Search** button to open the search interface.

### Search Filters

**Fish Name Filter:**
- Select specific fish species from dropdown
- Choose "Any" to search all fish types
- Dropdown shows only fish you've saved

**Moon Phase Filter:**
- Filter by specific moon phase
- Choose "Any" to ignore moon phase

**Minimum Percentage:**
- Enter a number (e.g., "50" for 50% or higher)
- Filters out locations below this catch rate
- Leave at "0" to show all percentages

### Using Search Results

1. Set your desired filters
2. Click **Search** button
3. Results appear showing: `[Fish Name] - [Percentage] - [Moon Phase] @ [Time]`
4. Example: `Salmon - 87% - Full Moon @ 12:45`
5. **Click any result** to center the map on that location

This makes it easy to find the best fishing spots for specific conditions.

## Storage Location

Fishing location data is saved to:

%APPDATA%/Haven and Hearth/fish_locations.nurgling.json

### Data Persistence
- Locations persist across game sessions
- Data includes full history with timestamps
- Back up this file to preserve your fishing database
- Share with friends by copying the JSON file
