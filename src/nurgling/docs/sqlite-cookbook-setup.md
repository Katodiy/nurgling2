# Setting Up SQLite Database

This guide shows you how to enable SQLite database storage for your cookbook recipes. Recipes that you make or import are stored in the cookbook and will only persist through restarts if a database is setup. SQLite is the easiest option to setup.

## Why Use SQLite Database?

- Recipes are saved permanently and won't be lost when you restart the client
- Faster searching and filtering of recipes
- Better performance with large recipe collections
- Easy backup and restore of your recipe collection

## Enabling SQLite Database

### Enable Database in Settings

1. Open the Nurgling2 settings window
2. Navigate to the **General** -> **Database** section
3. Check the box for **"Enable using Database"**
4. Select **"SQLight"** in the **"Database Type"**
5. Click **"Initialize New Database"** button
6. Click **"Save"**

![Settings Window - Cookbook Section](images/database_settings.png)
