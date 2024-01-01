package nurgling.tasks;

import haven.Gob;
import haven.res.ui.croster.CattleId;
import nurgling.NUtils;
import nurgling.tools.NParser;

public class AnimalIsDead implements NTask
{
    Gob animal;
    public AnimalIsDead(Gob animal)
    {
        this.animal = animal;
    }

    @Override
    public boolean check()
    {
        return animal.pose()!=null && NParser.checkName(animal.pose(), "knocked");
    }

}
