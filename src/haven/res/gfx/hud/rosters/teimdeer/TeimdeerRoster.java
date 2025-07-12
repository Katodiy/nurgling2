/* Preprocessed source code */
/* $use: ui/croster */

package haven.res.gfx.hud.rosters.teimdeer;

import haven.*;
import haven.res.gfx.hud.rosters.cow.Ochs;
import haven.res.ui.croster.*;
import java.util.*;

@haven.FromResource(name = "gfx/hud/rosters/teimdeer", version = 2)
public class TeimdeerRoster extends CattleRoster<Teimdeer> {
    public static List<Column> cols = initcols(
	new Column<Entry>("Name", Comparator.comparing((Entry e) -> e.name), 200),

	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/sex", 2),      Comparator.comparing((Teimdeer e) -> e.buck).reversed(), 20).runon(),
	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/growth", 2),   Comparator.comparing((Teimdeer e) -> e.fawn).reversed(), 20).runon(),
	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/deadp", 3),    Comparator.comparing((Teimdeer e) -> e.dead).reversed(), 20).runon(),
	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/pregnant", 2), Comparator.comparing((Teimdeer e) -> e.pregnant).reversed(), 20).runon(),
	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/lactate", 1),  Comparator.comparing((Teimdeer e) -> e.lactate).reversed(), 20).runon(),
	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/owned", 1),    Comparator.comparing((Teimdeer e) -> ((e.owned ? 1 : 0) | (e.mine ? 2 : 0))).reversed(), 20),

	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/quality", 2), Comparator.comparing((Teimdeer e) -> e.q).reversed()),

	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/meatquantity", 1), Comparator.comparing((Teimdeer e) -> e.meat).reversed()),
	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/milkquantity", 1), Comparator.comparing((Teimdeer e) -> e.milk).reversed()),

	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/meatquality", 1), Comparator.comparing((Teimdeer e) -> e.meatq).reversed()),
	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/milkquality", 1), Comparator.comparing((Teimdeer e) -> e.milkq).reversed()),
	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/hidequality", 1), Comparator.comparing((Teimdeer e) -> e.hideq).reversed()),

	new Column<Teimdeer>(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/breedingquality", 1), Comparator.comparing((Teimdeer e) -> e.seedq).reversed()),
	new Column<Teimdeer>(Resource.local().load("nurgling/hud/rang", 1), Comparator.comparing(Teimdeer::rang).reversed())
    );
    protected List<Column> cols() {return(cols);}

    public static CattleRoster mkwidget(UI ui, Object... args) {
	return(new TeimdeerRoster());
    }

    public Teimdeer parse(Object... args) {
	int n = 0;
	UID id = (UID)args[n++];
	String name = (String)args[n++];
	Teimdeer ret = new Teimdeer(id, name);
	ret.grp = (Integer)args[n++];
	int fl = (Integer)args[n++];
	ret.buck = (fl & 1) != 0;
	ret.fawn = (fl & 2) != 0;
	ret.dead = (fl & 4) != 0;
	ret.pregnant = (fl & 8) != 0;
	ret.lactate = (fl & 16) != 0;
	ret.owned = (fl & 32) != 0;
	ret.mine = (fl & 64) != 0;
	ret.q = ((Number)args[n++]).doubleValue();
	ret.meat = (Integer)args[n++];
	ret.milk = (Integer)args[n++];
	ret.meatq = (Integer)args[n++];
	ret.milkq = (Integer)args[n++];
	ret.hideq = (Integer)args[n++];
	ret.seedq = (Integer)args[n++];
	return(ret);
    }

    public TypeButton button() {
	return(typebtn(Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/btn-teimdeer", 1),
		       Resource.classres(TeimdeerRoster.class).pool.load("gfx/hud/rosters/btn-teimdeer-d", 1)));
    }
}
