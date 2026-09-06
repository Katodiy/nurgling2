package nurgling.widgets.nsettings.search;

import haven.Scrollport;
import haven.Widget;
import nurgling.tools.NFuzzy;

import java.util.ArrayList;
import java.util.List;

/**
 * One searchable string from the settings tree.
 *
 * <p>Entries are produced by {@link SettingsIndexer} from the live widget trees of the settings
 * panels, so an entry can never describe a setting that is not actually on screen. Everything
 * needed to navigate back to the setting travels with the entry: the action that opens its panel,
 * the optional reveal step for settings that live on a hidden tab, the scroll port that owns the
 * widget, and the widget itself.
 */
public class SettingEntry {
    public enum Kind {
	/** A control that stores a value: a checkbox, or the label naming a slider/dropdown/field. */
	SETTING,
	/** A button that does something rather than storing a value. */
	ACTION,
	/** A "&#9679; Foo" group heading inside a panel. */
	SECTION,
	/** A category or panel in the sidebar itself. */
	PANEL,
    }

    public final Kind kind;
    public final String label;
    /** Sidebar category, e.g. "General". Null for a category entry. */
    public final String category;
    /** Sidebar panel, e.g. "Quality of life". Null for a category or panel entry. */
    public final String panel;
    /** Nearest preceding section heading, or null. */
    public final String section;
    /** The widget to scroll to and highlight, or null for sidebar entries. */
    public final Widget target;
    /** The scroll port that owns {@link #target}, or null if it is not inside one. */
    public final Scrollport scroll;
    /** Makes {@link #target} reachable (switches to its tab), or null if it always is. */
    public final Runnable reveal;
    /** Selects the owning panel in the sidebar and shows it. */
    public final Runnable open;

    private final String lcLabel, lcSection, lcPanel, lcCategory;
    private final List<String> lcKeywords;

    public SettingEntry(Kind kind, String label, String category, String panel, String section,
			List<String> keywords, Widget target, Scrollport scroll,
			Runnable reveal, Runnable open) {
	this.kind = kind;
	this.label = label;
	this.category = category;
	this.panel = panel;
	this.section = section;
	this.target = target;
	this.scroll = scroll;
	this.reveal = reveal;
	this.open = open;

	this.lcLabel = NFuzzy.fold(label);
	this.lcSection = NFuzzy.fold(section);
	this.lcPanel = NFuzzy.fold(panel);
	this.lcCategory = NFuzzy.fold(category);
	this.lcKeywords = new ArrayList<>();
	if(keywords != null) {
	    for(String kw : keywords) {
		String f = NFuzzy.fold(kw);
		if(!f.isEmpty() && !lcKeywords.contains(f))
		    lcKeywords.add(f);
	    }
	}
    }

    /** "General &#8250; Quality of life", or just the category for a panel entry. */
    public String crumb() {
	if((category != null) && (panel != null))
	    return(category + " \u203A " + panel);
	if(category != null)
	    return(category);
	if(panel != null)
	    return(panel);
	return("");
    }

    /* Field weights. The label is what the user is looking at, so it dominates; the
     * untranslated key is next so English terms survive a translated client; the
     * surrounding context is a weak signal that only breaks ties. */
    private static final double W_LABEL = 1.00;
    private static final double W_KEYWORD = 0.75;
    private static final double W_SECTION = 0.60;
    private static final double W_PANEL = 0.50;
    private static final double W_CATEGORY = 0.35;

    /**
     * Best weighted score of one already-folded query token against any field of this entry.
     *
     * @return {@link NFuzzy#NO_MATCH} if the token appears nowhere
     */
    public double score(String token) {
	double best = NFuzzy.NO_MATCH;
	int fields = 0;

	/* NO_MATCH is negative infinity, so Math.max folds a miss away on its own. */
	double s = NFuzzy.score(lcLabel, token);
	if(s != NFuzzy.NO_MATCH) {
	    best = Math.max(best, s * W_LABEL);
	    fields++;
	}

	double kw = NFuzzy.NO_MATCH;
	for(String k : lcKeywords)
	    kw = Math.max(kw, NFuzzy.score(k, token));
	if(kw != NFuzzy.NO_MATCH) {
	    best = Math.max(best, kw * W_KEYWORD);
	    fields++;
	}

	s = NFuzzy.score(lcSection, token);
	if(s != NFuzzy.NO_MATCH) {
	    best = Math.max(best, s * W_SECTION);
	    fields++;
	}
	s = NFuzzy.score(lcPanel, token);
	if(s != NFuzzy.NO_MATCH) {
	    best = Math.max(best, s * W_PANEL);
	    fields++;
	}
	s = NFuzzy.score(lcCategory, token);
	if(s != NFuzzy.NO_MATCH) {
	    best = Math.max(best, s * W_CATEGORY);
	    fields++;
	}

	if(best == NFuzzy.NO_MATCH)
	    return(NFuzzy.NO_MATCH);
	/* A token that turns up in more than one field is a better sign than one that
	 * only grazes the label, but not enough to outrank a solid single-field hit. */
	if(fields > 1)
	    best += 20;
	return(best);
    }

    /** Shorter labels are more specific; used to break ties between equal scores. */
    public int length() {
	return(lcLabel.length());
    }
}
