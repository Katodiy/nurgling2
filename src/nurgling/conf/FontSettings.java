package nurgling.conf;

import haven.*;
import nurgling.widgets.nsettings.Fonts;
import org.json.JSONObject;

import java.awt.*;
import java.util.*;

public class FontSettings implements JConf {
    private static final Font serif = new Font("Serif", Font.PLAIN, 10);
    private static final Font sans  = new Font("Sans", Font.PLAIN, 10);
    private static final Font fraktur = Resource.local().loadwait("ui/fraktur").flayer(Resource.Font.class).font;
    private static final Font roboto = Resource.local().loadwait("nurgling/font/roboto").flayer(Resource.Font.class).font.deriveFont(Font.PLAIN);

    private static final Font helvetica = Resource.local().loadwait("nurgling/font/helvetica").flayer(Resource.Font.class).font.deriveFont(Font.PLAIN);

    public Font getFont(String name)
    {
        if(name.equals( "Inter"))
            return helvetica;
        else if(name.equals("Roboto"))
            return roboto;
        else if(name.equals("Sans"))
            return sans;
        else if(name.equals("Serif"))
            return serif;
        else if(name.equals("Fractur"))
            return fraktur;
        return null;
    }

    public Text.Foundry getFoundary(Fonts.FontType fontType) {
        switch (fontType)
        {
            case DEFAULT:
            {
                return new Text.Foundry(getFont(defaultFont.family),defaultFont.size);
            }
            case UI:
            {
                return new Text.Foundry(getFont(uiFont.family),uiFont.size);
            }
            case QUESTS:
            {
                return new Text.Foundry(getFont(questsFont.family),questsFont.size);
            }
            case BARRELS:
            {
                return new Text.Foundry(getFont(barrelsFont.family),barrelsFont.size, barrelsFont.color);
            }
            case CHARACTERS:
            {
                return new Text.Foundry(getFont(charactersFont.family),charactersFont.size);
            }
        }
        return null;
    }

    public static class FontConfig {
        public String family;
        public int size;
        public boolean isColorable = false;
        public Color color = Color.BLACK;

        public FontConfig() {}
        public FontConfig(String family, int size) {
            this.family = family;
            this.size = size;
        }

        public FontConfig(String family, int size, boolean isColorable, Color color) {
            this.family = family;
            this.size = size;
            this.isColorable = isColorable;
            this.color = color;
        }

        public Map<String, Object> toJson() {
            Map<String, Object> ret = new HashMap<>();
            ret.put("family", family);
            ret.put("size", size);
            ret.put("isColorable", isColorable);
                Map<String, Object> jcolor = new HashMap<>();
            jcolor.put("red", color.getRed());
            jcolor.put("green", color.getGreen());
            jcolor.put("blue", color.getBlue());
            jcolor.put("alpha", color.getAlpha());
            ret.put("color", jcolor);
            return ret;
        }

        public FontConfig(Map<String, Object> map) {
            this.family = (String)map.get("family");
            this.size = ((Number)map.get("size")).intValue();
            if(map.containsKey("isColorable"))
            {
                Map<String, Object> jcolor = (Map<String, Object>)map.get("color");
                color = new Color((Integer) jcolor.get("red"),(Integer)jcolor.get("green"),(Integer)jcolor.get("blue"),(Integer)jcolor.get("alpha"));
                isColorable = (Boolean)map.get("isColorable");
            }
        }
    }

    public FontConfig defaultFont = new FontConfig("Sans", 12);
    public FontConfig uiFont = new FontConfig("Sans", 12);
    public FontConfig questsFont = new FontConfig("Sans", 12);
    public FontConfig barrelsFont = new FontConfig("Sans", 12, true, Color.YELLOW);
    public FontConfig charactersFont = new FontConfig("Sans", 12);

    public JSONObject toJson() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("type", "FontSettings");
        ret.put("default", defaultFont.toJson());
        ret.put("ui", uiFont.toJson());
        ret.put("quests", questsFont.toJson());
        ret.put("barrels", barrelsFont.toJson());
        ret.put("characters", charactersFont.toJson());
        return new JSONObject(ret);
    }

    public FontSettings(Map<String, Object> map) {
        if(map.containsKey("default"))
            defaultFont = new FontConfig((Map<String, Object>)map.get("default"));
        if(map.containsKey("ui"))
            uiFont = new FontConfig((Map<String, Object>)map.get("ui"));
        if(map.containsKey("quests"))
            questsFont = new FontConfig((Map<String, Object>)map.get("quests"));
        if(map.containsKey("barrels"))
            barrelsFont = new FontConfig((Map<String, Object>)map.get("barrels"));
        if(map.containsKey("characters"))
            charactersFont = new FontConfig((Map<String, Object>)map.get("characters"));
    }

    public FontSettings() {}
}