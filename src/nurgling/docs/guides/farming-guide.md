# Farming Guide

This guide covers automated farming with Nurgling2.

![Farm Layout](../images/img.png)

## Crop Types

### Grains
- **Wheat**: Basic grain, easy to grow
- **Barley**: Used for brewing
- **Rye**: Hardy crop for difficult soil

### Vegetables  
- **Turnips**: Fast growing root vegetable
- **Carrots**: Nutritious orange roots
- **Onions**: Flavor enhancer

## Automation Setup

### Basic Farm Layout
1. Clear and plow the area
2. Set up irrigation if needed
3. Configure the farming bot
4. Monitor crop growth stages

### Bot Configuration
- Set crop type in bot settings
- Define farm boundaries
- Configure harvest conditions
- Set storage containers

## Tips

- **Soil Quality**: Better soil = better yields
- **Timing**: Plant after seasons change
- **Storage**: Plan adequate storage space
- **Monitoring**: Check bot progress regularly

## Sample Bot Configuration

Here's a basic configuration example:

```
botType=farming
cropType=wheat
areaRadius=10
harvestThreshold=mature
storageContainer=barrel
```

This configuration sets up a wheat farming bot with a 10-tile radius.

## Troubleshooting

**Bot stops working:**
- Check if inventory is full
- Verify water availability  
- Ensure tools are not broken
- Check for obstacle interference

**Low yields:**
- Improve soil quality
- Check timing of planting
- Verify proper watering
- Consider fertilizer use