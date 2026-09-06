package nurgling.widgets.nsettings.search;

import haven.Button;
import haven.CheckBox;
import haven.Label;
import haven.ListWidget;
import haven.Listbox;
import haven.SListWidget;
import haven.Scrollport;
import haven.Widget;
import nurgling.i18n.L10n;
import nurgling.tools.NFuzzy;
import nurgling.widgets.nsettings.Panel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the settings search index by walking the live widget trees of the settings panels.
 *
 * <p>There is no registry of settings anywhere in the client: a setting is a checkbox constructed
 * inline with a translated string and wired to an NConfig key in the panel's
 * load/save pair. Rather than hand-maintaining a parallel list that would silently drift the first
 * time someone adds a checkbox, this harvests the strings straight out of the widgets that are
 * already on screen. The index therefore describes exactly what the user can see, and it hands back
 * the widget object itself, which is what makes jumping to a result possible at all.
 *
 * <p>The panels are all constructed eagerly by the settings window and merely hidden, so the whole
 * tree is available from the moment the window exists.
 */
public class SettingsIndexer {
    /** Group headings are written as "&#9679; Foo" throughout the settings panels. */
    private static final char BULLET = '\u25CF';

    /**
     * Harvest every searchable string in one panel.
     *
     * @param panel The panel to walk
     * @param category Its sidebar category, for the breadcrumb
     * @param name Its sidebar name, for the breadcrumb
     * @param open Action that selects and shows this panel
     */
    public static List<SettingEntry> index(Panel panel, String category, String name, Runnable open) {
	List<SettingEntry> out = new ArrayList<>();
	Map<Widget, Runnable> reveals = panel.searchReveal();
	Ctx ctx = new Ctx(category, name, open, reveals, out);
	/* The panel itself is hidden until selected, so its own visibility is not a signal;
	 * start below it. */
	scan(panel, ctx, null, null, null);
	return(out);
    }

    /**
     * Rank an index against a query.
     *
     * @param index Everything that could match
     * @param query Raw user input; whitespace splits it into tokens that must all match
     * @param limit Most results to return
     */
    public static List<SettingEntry> search(List<SettingEntry> index, String query, int limit) {
	String[] tokens = NFuzzy.fold(query).trim().split("\\s+");
	List<String> real = new ArrayList<>();
	for(String t : tokens) {
	    if(!t.isEmpty())
		real.add(t);
	}
	if(real.isEmpty())
	    return(Collections.emptyList());

	List<Scored> found = new ArrayList<>();
	entries: for(SettingEntry e : index) {
	    double total = 0;
	    for(String t : real) {
		double s = e.score(t);
		/* Every token has to land somewhere, so a second word narrows rather
		 * than widens the result set. */
		if(s == NFuzzy.NO_MATCH)
		    continue entries;
		total += s;
	    }
	    found.add(new Scored(e, total / real.size()));
	}

	found.sort(Comparator
		   .comparingDouble((Scored s) -> -s.score)
		   .thenComparingInt(s -> s.entry.length())
		   .thenComparing(s -> s.entry.label));

	List<SettingEntry> out = new ArrayList<>(Math.min(limit, found.size()));
	for(Scored s : found) {
	    if(out.size() >= limit)
		break;
	    out.add(s.entry);
	}
	return(out);
    }

    private static class Scored {
	final SettingEntry entry;
	final double score;

	Scored(SettingEntry entry, double score) {
	    this.entry = entry;
	    this.score = score;
	}
    }

    /* Everything that is constant for one panel's walk. */
    private static class Ctx {
	final String category, panel;
	final Runnable open;
	final Map<Widget, Runnable> reveals;
	final List<SettingEntry> out;
	final Set<String> seen = new HashSet<>();

	Ctx(String category, String panel, Runnable open, Map<Widget, Runnable> reveals, List<SettingEntry> out) {
	    this.category = category;
	    this.panel = panel;
	    this.open = open;
	    this.reveals = reveals;
	    this.out = out;
	}
    }

    /*
     * Depth-first in child order, which is also the order the panels lay their widgets out in, so
     * "the section this setting belongs to" is simply the last heading seen among its siblings.
     * Sections are tracked per parent, which is what makes the two-column QoL panel come out right:
     * each column carries its own headings and neither leaks into the other.
     */
    private static void scan(Widget parent, Ctx ctx, Scrollport scroll, Runnable reveal, String inherited) {
	String section = inherited;
	for(Widget w = parent.child; w != null; w = w.next) {
	    Runnable wreveal = reveal;
	    Runnable declared = ctx.reveals.get(w);
	    if(declared != null)
		wreveal = declared;

	    /* A hidden subtree with no way to reveal it is unreachable, so indexing it would
	     * only produce results that go nowhere. Only a widget that registered itself as a
	     * reveal group survives being hidden; something hidden deeper inside such a group
	     * stays hidden after the group is revealed, so it is still skipped. */
	    if(!w.visible && (declared == null))
		continue;

	    /* List rows hold user data (scenarios, presets, orders) rather than settings, and
	     * they are rebuilt as the list scrolls, so anything harvested from one would be a
	     * result pointing at a widget that no longer exists. */
	    if((w instanceof SListWidget) || (w instanceof ListWidget) || (w instanceof Listbox))
		continue;

	    Scrollport wscroll = (w instanceof Scrollport) ? (Scrollport)w : scroll;

	    String text = text(w);
	    if(text != null) {
		boolean heading = isHeading(text);
		String label = clean(text);
		if(usable(label)) {
		    if(heading)
			section = label;
		    if(ctx.seen.add(label.toLowerCase())) {
			SettingEntry.Kind kind = heading ? SettingEntry.Kind.SECTION
			    : (w instanceof Button) ? SettingEntry.Kind.ACTION
			    : SettingEntry.Kind.SETTING;
			ctx.out.add(new SettingEntry(kind, label, ctx.category, ctx.panel,
						     heading ? null : section, keywords(label, text),
						     w, wscroll, wreveal, ctx.open));
		    }
		}
	    }

	    scan(w, ctx, wscroll, wreveal, section);
	}
    }

    /* The three widget types that carry a setting's name. Sliders, dropdowns and text fields are
     * all introduced by a neighbouring Label, so indexing labels covers them too. */
    private static String text(Widget w) {
	if(w instanceof Label)
	    return(((Label)w).texts);
	if(w instanceof CheckBox)
	    return(((CheckBox)w).label());
	if(w instanceof Button)
	    return((((Button)w).text == null) ? null : ((Button)w).text.text);
	return(null);
    }

    private static boolean isHeading(String text) {
	return(!text.isEmpty() && (text.charAt(0) == BULLET));
    }

    /* Strip the heading bullet and any trailing colon, so "&#9679; Visual" and "Corner:" index
     * under the names the user would actually type. */
    private static String clean(String text) {
	String s = text.trim();
	if(isHeading(s))
	    s = s.substring(1).trim();
	while(s.endsWith(":"))
	    s = s.substring(0, s.length() - 1).trim();
	return(s);
    }

    /* Value read-outs ("10", "35%"), separators and single letters are not settings. */
    private static boolean usable(String label) {
	if(label.length() < 2)
	    return(false);
	for(int i = 0; i < label.length(); i++) {
	    if(Character.isLetter(label.charAt(i)))
		return(true);
	}
	return(false);
    }

    /* The untranslated key a label came from, split into words, so "night vision" still finds
     * qol.night_vision when the client is running in Russian. */
    private static List<String> keywords(String label, String raw) {
	/* The cleaned label is what the property file usually holds, but some strings carry
	 * their own trailing colon, so fall back to what was actually rendered. */
	List<String> keys = L10n.keysFor(label);
	if(keys.isEmpty())
	    keys = L10n.keysFor(raw.trim());
	if(keys.isEmpty())
	    return(Collections.emptyList());
	List<String> out = new ArrayList<>();
	for(String key : keys) {
	    out.add(key);
	    for(String part : key.split("[._]")) {
		if(part.length() > 1)
		    out.add(part);
	    }
	}
	return(out);
    }
}
