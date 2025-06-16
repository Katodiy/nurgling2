package nurgling;
import haven.Button;
import haven.Charlist;
import haven.RemoteUI;
import haven.UI;

public class NCharlist extends Charlist {
    static NCharlist instance;

    Button logout;

    public NCharlist(int height ) {
        super ( height );
        instance = this;
        System.out.println("initiated charlist");
    }

    public static void play(){
        System.out.println("Trying to play");
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

    protected void added() {
        parent.setfocus(this);
        logout = parent.add(new Button(UI.scale(90), "Log out") {
            @Override
            public void click() {
                RemoteUI rui = (RemoteUI) ui.rcvr;
                synchronized (rui.sess) {
                    rui.sess.close();
                }
            }
        }, UI.scale(121, 553));
    }

    @Override
    public void uimsg(String msg, Object... args) {
        super.uimsg(msg, args);
        if(msg == "add") {
            System.out.println("MSG is add");
            NCharlist.play();
        }

    }
}