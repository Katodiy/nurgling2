package nurgling.widgets;

import haven.*;
import nurgling.tools.EncyclopediaManager;
import nurgling.tools.MarkdownToImageRenderer;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

public class EncyclopediaWindow extends Window {
    private final EncyclopediaManager manager;
    private SListBox<String, Widget> documentList;
    private Widget contentArea;
    private Widget scrollableContent;
    
    public EncyclopediaWindow() {
        super(UI.scale(new Coord(800, 600)), "Encyclopedia");
        manager = new EncyclopediaManager();
        
        setupUI();
        loadDefaultDocument();
    }
    
    private void setupUI() {
        int margin = UI.scale(5);

        // Document list on the left  
        int listY = margin; // Lower to match content panel alignment
        
        documentList = add(new SListBox<String, Widget>(Coord.of(UI.scale(200), UI.scale(300)), UI.scale(16)) {
            @Override
            protected List<String> items() {
                return getDocumentKeys();
            }
            
            @Override
            protected Widget makeitem(String item, int idx, Coord sz) {
                return new ItemWidget<String>(this, sz, item) {
                    @Override
                    public void draw(GOut g) {
                        g.chcolor(new java.awt.Color(30, 30, 30, 180));
                        g.frect(Coord.z, sz);
                        g.chcolor();

                        g.text(manager.getDocumentTitle(item), Coord.z);
                    }
                    
                    @Override
                    public boolean mousedown(MouseDownEvent ev) {
                        if (ev.b == 1) {
                            loadDocument(item);
                            return true;
                        }
                        return super.mousedown(ev);
                    }
                };
            }
        }, new Coord(margin, listY));
        
        // Content area on the right
        int contentX = documentList.pos("ur").x + UI.scale(10);
        int contentWidth = sz.x - contentX - margin;
        int contentHeight = sz.y - listY - UI.scale(30);

        contentArea = add(new Scrollport(new Coord(contentWidth-UI.scale(70), contentHeight-UI.scale(70))), new Coord(contentX, listY - UI.scale(10)));
        scrollableContent = new Widget(new Coord(contentWidth, UI.scale(50))) {
            @Override
            public void pack() {
                // Auto-resize based on children
                resize(contentsz());
            }
        };
        ((Scrollport)contentArea).cont.add(scrollableContent, Coord.z);
    }
    
    
    private List<String> getDocumentKeys() {
        return manager.getAllDocumentKeys().stream().sorted().collect(Collectors.toList());
    }
    
    private void loadDefaultDocument() {
        // Try to load welcome.md if it exists
        List<String> documentKeys = getDocumentKeys();
        String welcomeKey = documentKeys.stream()
            .filter(key -> key.contains("welcome"))
            .findFirst()
            .orElse(null);
        
        if (welcomeKey != null) {
            loadDocument(welcomeKey);
        } else if (!documentKeys.isEmpty()) {
            // Fallback to first document if welcome.md not found
            loadDocument(documentKeys.get(0));
        }
    }
    
    private void loadDocument(String documentKey) {
        String content = manager.getDocumentContent(documentKey);
        if (content != null) {
            displayDocument(documentKey, content);
        } else {
            // Clear content area
            for (Widget child : scrollableContent.children()) {
                child.destroy();
            }
        }
    }
    
    
    private void displayDocument(String documentKey, String content) {
        // Clear existing content
        for (Widget child : scrollableContent.children()) {
            child.destroy();
        }
        
        int margin = UI.scale(10);
        int availableWidth = ((Scrollport)contentArea).cont.sz.x - margin * 2;
        
        // Create a rendered image widget for the content
        Widget documentWidget = createMarkdownImageWidget(documentKey, content, availableWidth);
        scrollableContent.add(documentWidget, new Coord(margin, UI.scale(10)));
        
        // Use pack() to auto-resize based on children
        scrollableContent.pack();
        
        // Manually trigger scrollport update to recalculate scrollbar and reset scroll position
        Scrollport sp = (Scrollport)contentArea;
        sp.cont.update();
        
        // Reset scroll position to top
        if (sp.bar != null) {
            sp.bar.val = sp.bar.min;
            sp.bar.changed();
        }
    }
    
    private Widget createMarkdownImageWidget(String documentKey, String content, int maxWidth) {
        BufferedImage image = MarkdownToImageRenderer.renderMarkdownToImage(content, maxWidth, documentKey);
        
        // Convert to Haven texture and create widget
        final TexI tex = new TexI(image);
        return new Widget(new Coord(image.getWidth(), image.getHeight())) {
            @Override
            public void draw(GOut g) {
                g.image(tex, Coord.z);
            }
        };
    }
    

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}