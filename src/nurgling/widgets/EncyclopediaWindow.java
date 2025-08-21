package nurgling.widgets;

import haven.*;
import nurgling.NMapView;
import nurgling.NUtils;
import nurgling.tools.EncyclopediaManager;
import nurgling.tools.MarkdownToImageRenderer;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

public class EncyclopediaWindow extends Window {
    private final EncyclopediaManager manager;
    private String currentDocumentKey;
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
        
        // Title at the top
        prev = add(new Label("Documentation"), new Coord(margin, margin));
        
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
        java.io.File file = manager.getDocumentFile(documentKey);
        if (file != null && file.exists()) {
            currentDocumentKey = documentKey;
            String title = getDocumentTitle(documentKey);
            titleLabel.settext(title);
            displayDocument(file);
        } else {
            titleLabel.settext("Document not found: " + documentKey);
            // Clear content area
            for (Widget child : scrollableContent.children()) {
                child.destroy();
            }
        }
    }
    
    private String getDocumentTitle(String documentKey) {
        // Extract title from filename, handling nested paths
        String filename = documentKey;
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        if (filename.endsWith(".md")) {
            filename = filename.substring(0, filename.length() - 3);
        }
        // Convert dashes to spaces and capitalize
        return filename.replace("-", " ").replace("_", " ");
    }
    
    private void displayDocument(java.io.File file) {
        // Clear existing content
        for (Widget child : scrollableContent.children()) {
            child.destroy();
        }
        
        int margin = UI.scale(10);
        int availableWidth = ((Scrollport)contentArea).cont.sz.x - margin * 2;
        
        // Create a rendered image widget for the file
        Widget documentWidget = createMarkdownImageWidget(file, availableWidth);
        scrollableContent.add(documentWidget, new Coord(margin, UI.scale(10)));
        
        // Use pack() to auto-resize based on children
        scrollableContent.pack();
        
        // Manually trigger scrollport update to recalculate scrollbar
        Scrollport sp = (Scrollport)contentArea;
        sp.cont.update();
    }
    
    private Widget createMarkdownImageWidget(java.io.File file, int maxWidth) {
        // Read the raw markdown text directly from file
        String rawMarkdown = readFileContent(file);
        BufferedImage image = MarkdownToImageRenderer.renderMarkdownToImage(rawMarkdown, maxWidth);
        
        // Convert to Haven texture and create widget
        final TexI tex = new TexI(image);
        return new Widget(new Coord(image.getWidth(), image.getHeight())) {
            @Override
            public void draw(GOut g) {
                g.image(tex, Coord.z);
            }
        };
    }
    
    private String readFileContent(java.io.File file) {
        try {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            System.err.println("Could not read file: " + file.getPath() + " - " + e.getMessage());
            return "Error reading file: " + file.getName();
        }
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