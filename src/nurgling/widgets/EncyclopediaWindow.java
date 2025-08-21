package nurgling.widgets;

import haven.*;
import nurgling.tools.EncyclopediaManager;
import nurgling.tools.markdown.MarkdownDocument;
import nurgling.tools.markdown.MarkdownElement;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

public class EncyclopediaWindow extends Window {
    private final EncyclopediaManager manager;
    private MarkdownDocument currentDocument;
    private TextEntry searchBox;
    private Listbox<String> documentList;
    private Widget contentArea;
    private Widget scrollableContent;
    private Label titleLabel;
    
    public EncyclopediaWindow() {
        super(UI.scale(new Coord(800, 600)), "Encyclopedia");
        manager = EncyclopediaManager.getInstance();
        
        setupUI();
        loadDocumentList();
    }
    
    private void setupUI() {
        int margin = UI.scale(5);
        Widget prev;
        
        // Search box at the top
        prev = add(new Label("Search:"), new Coord(margin, margin));
        searchBox = add(new TextEntry(UI.scale(200), "") {
            @Override
            public boolean keydown(KeyDownEvent ev) {
                if (ev.code == KeyEvent.VK_ENTER) {
                    performSearch();
                    return true;
                }
                return super.keydown(ev);
            }
        }, prev.pos("ur").adds(5, 0));
        
        prev = add(new Button(UI.scale(60), "Search") {
            @Override
            public void click() {
                super.click();
                performSearch();
            }
        }, searchBox.pos("ur").adds(5, 0));
        
        // Document list on the left
        int listY = prev.pos("bl").y + UI.scale(10);
        add(new Label("Documents:"), new Coord(margin, listY));
        
        documentList = add(new Listbox<String>(UI.scale(200), 20, UI.scale(16)) {
            @Override
            protected String listitem(int i) {
                List<String> docs = getDocumentKeys();
                return i < docs.size() ? formatDocumentName(docs.get(i)) : "";
            }
            
            @Override
            protected int listitems() {
                return getDocumentKeys().size();
            }
            
            @Override
            protected void drawitem(GOut g, String item, int i) {
                g.text(item, Coord.z);
            }
            
            @Override
            public void change(String item) {
                super.change(item);
                if (item != null) {
                    // Find the actual document key for the selected display item
                    List<String> docs = getDocumentKeys();
                    for (int i = 0; i < docs.size(); i++) {
                        if (formatDocumentName(docs.get(i)).equals(item)) {
                            loadDocument(docs.get(i));
                            break;
                        }
                    }
                }
            }
        }, new Coord(margin, listY + UI.scale(20)));
        
        // Content area on the right
        int contentX = documentList.pos("ur").x + UI.scale(10);
        int contentWidth = sz.x - contentX - margin;
        int contentHeight = sz.y - listY - UI.scale(50);
        
        // Title label for current document
        titleLabel = add(new Label("Select a document"), 
            new Coord(contentX, listY));
        
        // Content display area with scrolling - exact copy of NInventory pattern  
        contentArea = add(new Scrollport(new Coord(contentWidth-UI.scale(70), contentHeight-UI.scale(70))), new Coord(contentX, titleLabel.pos("bl").y + UI.scale(10)));
        scrollableContent = new Widget(new Coord(contentWidth, UI.scale(50))) {
            @Override
            public void pack() {
                // Auto-resize based on children
                resize(contentsz());
            }
        };
        ((Scrollport)contentArea).cont.add(scrollableContent, Coord.z);
    }
    
    private void loadDocumentList() {
        documentList.change(null); // Refresh the list
    }
    
    private List<String> getDocumentKeys() {
        return manager.getAllDocumentKeys().stream().sorted().collect(Collectors.toList());
    }
    
    private String formatDocumentName(String key) {
        if (key.contains("/")) {
            return key; // Show full path for nested documents
        } else {
            // Remove .md extension for display
            return key.endsWith(".md") ? key.substring(0, key.length() - 3) : key;
        }
    }
    
    private void loadDocument(String documentKey) {
        MarkdownDocument doc = manager.getDocument(documentKey);
        if (doc != null) {
            currentDocument = doc;
            titleLabel.settext(doc.getTitle());
            displayDocument(doc);
        } else {
            titleLabel.settext("Document not found: " + documentKey);
            // Clear content area
            for (Widget child : contentArea.children()) {
                child.destroy();
            }
        }
    }
    
    private void displayDocument(MarkdownDocument document) {
        // Clear existing content
        for (Widget child : scrollableContent.children()) {
            child.destroy();
        }
        
        int y = UI.scale(10);
        int margin = UI.scale(10);
        int availableWidth = ((Scrollport)contentArea).cont.sz.x - margin * 2; // Use fixed scrollport content width
        
        // Render each element
        for (MarkdownElement element : document.elements) {
            Widget elementWidget = createElement(element, availableWidth);
            if (elementWidget != null) {
                scrollableContent.add(elementWidget, new Coord(margin, y));
                y += elementWidget.sz.y + UI.scale(8);
            }
        }
        
        // Use pack() to auto-resize based on children like NInventory does
        scrollableContent.pack();
        
        // Manually trigger scrollport update to recalculate scrollbar
        Scrollport sp = (Scrollport)contentArea;
        sp.cont.update();
        
        // Debug scrollbar info
        System.out.println("Scrollbar max: " + sp.bar.max);
        System.out.println("Scrollbar position: " + sp.bar.c);
        System.out.println("Scrollbar size: " + sp.bar.sz);
        System.out.println("Scrollport size: " + sp.sz);
    }
    
    private Widget createElement(MarkdownElement element, int maxWidth) {
        if (element instanceof MarkdownElement.Header) {
            MarkdownElement.Header header = (MarkdownElement.Header) element;
            // Headers in a distinctive color
            return createWrappedText(header.text, maxWidth, new Color(255, 255, 128));
            
        } else if (element instanceof MarkdownElement.Paragraph) {
            MarkdownElement.Paragraph para = (MarkdownElement.Paragraph) element;
            // Regular text in standard UI color
            return createWrappedText(para.text, maxWidth, Color.WHITE);
            
        } else if (element instanceof MarkdownElement.Text) {
            MarkdownElement.Text text = (MarkdownElement.Text) element;
            return createWrappedText(text.text, maxWidth, Color.WHITE);
        }
        
        return null;
    }
    
    private Widget createWrappedText(String text, int maxWidth, Color color) {
        // Try to manually break up text that's too long
        String[] lines = wrapText(text, maxWidth);
        
        if (lines.length == 1) {
            // Single line, use regular label
            Label label = new Label(text);
            label.setcolor(color);
            return label;
        } else {
            // Multiple lines, create a container widget with proper spacing
            int lineHeight = UI.scale(15); // Increased from 12 to 15 for better spacing
            Widget container = new Widget(new Coord(maxWidth, lines.length * lineHeight + UI.scale(5))); // Extra padding
            int y = 0;
            for (String line : lines) {
                Label label = new Label(line);
                label.setcolor(color);
                container.add(label, new Coord(0, y));
                y += lineHeight;
            }
            return container;
        }
    }
    
    private String[] wrapText(String text, int maxWidth) {
        // Simple word wrapping - estimate characters per line
        int avgCharWidth = UI.scale(7); // Rough estimate
        int charsPerLine = Math.max(10, maxWidth / avgCharWidth);
        
        if (text.length() <= charsPerLine) {
            return new String[]{text};
        }
        
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= charsPerLine) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    private void performSearch() {
        String query = searchBox.text().trim();
        if (query.isEmpty()) {
            loadDocumentList();
            return;
        }
        
        List<MarkdownDocument> results = manager.searchDocuments(query);
        // For now, just show the first result
        if (!results.isEmpty()) {
            displayDocument(results.get(0));
            titleLabel.settext("Search Results for: " + query);
        }
    }
    
    @Override
    public void wdgmsg(String msg, Object... args) {
        if (msg.equals("close")) {
            hide();
        } else {
            super.wdgmsg(msg, args);
        }
    }
    
    @Override
    public void show() {
        super.show();
        manager.loadDocuments(); // Ensure documents are loaded
        loadDocumentList();
    }
}