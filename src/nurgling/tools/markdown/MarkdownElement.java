package nurgling.tools.markdown;

public abstract class MarkdownElement {
    public abstract String getType();
    public abstract String getContent();
    
    public static class Header extends MarkdownElement {
        public final int level;
        public final String text;
        
        public Header(int level, String text) {
            this.level = level;
            this.text = text;
        }
        
        @Override
        public String getType() {
            return "header";
        }
        
        @Override
        public String getContent() {
            return text;
        }
    }
    
    public static class Paragraph extends MarkdownElement {
        public final String text;
        
        public Paragraph(String text) {
            this.text = text;
        }
        
        @Override
        public String getType() {
            return "paragraph";
        }
        
        @Override
        public String getContent() {
            return text;
        }
    }
    
    public static class Bold extends MarkdownElement {
        public final String text;
        
        public Bold(String text) {
            this.text = text;
        }
        
        @Override
        public String getType() {
            return "bold";
        }
        
        @Override
        public String getContent() {
            return text;
        }
    }
    
    public static class Italic extends MarkdownElement {
        public final String text;
        
        public Italic(String text) {
            this.text = text;
        }
        
        @Override
        public String getType() {
            return "italic";
        }
        
        @Override
        public String getContent() {
            return text;
        }
    }
    
    public static class Link extends MarkdownElement {
        public final String text;
        public final String url;
        
        public Link(String text, String url) {
            this.text = text;
            this.url = url;
        }
        
        @Override
        public String getType() {
            return "link";
        }
        
        @Override
        public String getContent() {
            return text;
        }
    }
    
    public static class Image extends MarkdownElement {
        public final String alt;
        public final String src;
        
        public Image(String alt, String src) {
            this.alt = alt;
            this.src = src;
        }
        
        @Override
        public String getType() {
            return "image";
        }
        
        @Override
        public String getContent() {
            return alt;
        }
    }
    
    public static class Text extends MarkdownElement {
        public final String text;
        
        public Text(String text) {
            this.text = text;
        }
        
        @Override
        public String getType() {
            return "text";
        }
        
        @Override
        public String getContent() {
            return text;
        }
    }
}