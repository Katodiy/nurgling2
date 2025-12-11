package nurgling;
import haven.Charlist;

public class NCharlist extends Charlist {
    public static NCharlist instance;

    public NCharlist(int height ) {
        super ( height );
        instance = this;
        System.out.println("[NCharlist] Created, botmod=" + (NConfig.botmod != null ? "set" : "null"));
    }

    public static void play() {
        System.out.println("[NCharlist] play() called, botmod=" + (NConfig.botmod != null) + ", instance=" + (instance != null));
        if(NConfig.botmod!=null && instance!=null) {
            System.out.println("[NCharlist] Looking for character: '" + NConfig.botmod.character + "'");
            System.out.println("[NCharlist] Available characters: " + instance.chars.size());
            for ( Char c: instance.chars) {
                System.out.println("[NCharlist]   - '" + c.name + "'");
                if(c.name.equals ( NConfig.botmod.character  ))
                {
                    System.out.println("[NCharlist] Found match! Sending play message");
                    instance.wdgmsg ( "play", NConfig.botmod.character );
                    instance = null;
                    break;
                }
            }
        }
    }

    @Override
    public void uimsg(String msg, Object... args) {
        super.uimsg(msg, args);
        if("add".equals(msg)) {
            NCharlist.play();
        }
    }
}
