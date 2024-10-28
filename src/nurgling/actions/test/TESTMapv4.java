package nurgling.actions.test;

import haven.Connection;
import haven.Gob;
import mapv4.Connector;
import mapv4.NMappingClient;
import mapv4.Requestor;
import nurgling.NGameUI;
import nurgling.NUtils;
import nurgling.actions.OpenTargetContainer;
import nurgling.actions.PathFinder;
import nurgling.areas.NArea;
import nurgling.tools.Finder;
import nurgling.tools.NAlias;
import org.json.JSONObject;

import java.util.ArrayList;

/*
* find all chest in area TestArea (req)
* */

public class TESTMapv4 extends Test
{

    public TESTMapv4()
    {
        this.num = 1;
    }

    @Override
    public void body(NGameUI gui) throws InterruptedException
    {
        NMappingClient mc = NUtils.getUI().core.mappingClient;
        NUtils.getGameUI().msg("REQUESTER state: " + String.valueOf(mc.reqTread.isAlive()));
        NUtils.getGameUI().msg("Connector state: " + String.valueOf(mc.reqTread.isAlive()));
        synchronized (mc.requestor.list) {
            NUtils.getGameUI().msg("req queue size: " + String.valueOf(mc.requestor.list.size()));
            for(Requestor.MapperTask mt : mc.requestor.list )
            {
                NUtils.getGameUI().msg(mt.toString());
            }
        }
        synchronized (mc.connector.msgs) {
            NUtils.getGameUI().msg("con queue size: " + String.valueOf(mc.connector.msgs.size()));
            for (JSONObject msg: mc.connector.msgs)
            {
                NUtils.getGameUI().msg(msg.toString());
            }
        }
        NUtils.getGameUI().msg("Current tasks in system: " + NUtils.getUI().core.toString());
    }
}
