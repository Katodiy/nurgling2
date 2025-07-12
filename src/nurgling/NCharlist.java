package nurgling;
import haven.Charlist;

public class NCharlist extends Charlist {
    public static NCharlist instance;

    public NCharlist(int height ) {
        super ( height );
        instance = this;
    }

    public static void play() {
        if(NConfig.botmod!=null && instance!=null) {
            for ( Char c: instance.chars) {
                if(c.name.equals ( NConfig.botmod.character  ))
                {
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
        if(msg == "add") {
            NCharlist.play();
        }
    }
}
