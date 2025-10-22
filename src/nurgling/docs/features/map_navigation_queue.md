# Map Navigation Queue & Retry Settings

The **Map Navigation Queue** system allows you to queue multiple navigation destinations and configure retry behavior when navigation fails.

## Accessing Navigation Settings

Navigation settings can be accessed through the main settings panel:

![Navigation Settings Button](../images/features/navigation_settings_button.png)

## Queue System

The queue system lets you add multiple destinations that will be processed sequentially:

![Navigation Queue](../images/features/navigation_queue.png)

### How to Use

1. Right-click on the map to open the context menu
2. Select **"Add to Navigation Queue"**
3. Your character will navigate to queued destinations in order
4. The queue displays all pending destinations

## Retry Settings

When navigation fails (blocked path, obstacle, etc.), the retry system determines whether to attempt again:

![Retry Settings](../images/features/navigation_retry_settings.png)

### Configuration Options

- **Max Retries:** Number of attempts before giving up (default: 3)
- **Retry Delay:** Time to wait between retry attempts (in seconds)
- **Auto-Clear Failed:** Automatically remove failed destinations from queue

### Usage Tips

- Increase max retries for areas with moving obstacles (animals, players)
- Use longer retry delays if temporary blockages are common
- Enable auto-clear to prevent queue buildup from permanently blocked paths
