package nurgling.actions.test;

import haven.*;
import nurgling.*;
import nurgling.actions.*;
import nurgling.tools.*;

import java.util.*;

/*
* You need chest with branches (30 branch min)
* */

public class TESTbranchinvtransferpacks extends Test
{
    public static final NAlias branch = new NAlias("Branch");

    public TESTbranchinvtransferpacks()
    {
        this.container = "Chest";
    }

    String container;

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        new OpenTargetContainer(container, TestUtils.findGob("chest")).run(gui);
        new TransferItems(gui.getInventory(container), gui.getInventory().getItems(branch), 10).run(gui);
        new TransferItems(gui.getInventory(container), gui.getInventory().getItems(branch), 10).run(gui);
        new TransferItems(gui.getInventory(container), gui.getInventory().getItems(branch), 10).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);

        new OpenTargetContainer(container, TestUtils.findGob("chest")).run(gui);
        new TransferItems(gui.getInventory(), gui.getInventory(container).getItems(branch), 10).run(gui);
        new TransferItems(gui.getInventory(), gui.getInventory(container).getItems(branch), 10).run(gui);
        new TransferItems(gui.getInventory(), gui.getInventory(container).getItems(branch), 10).run(gui);
        new CloseTargetWindow(NUtils.getGameUI().getWindow(container)).run(gui);
    }
}
