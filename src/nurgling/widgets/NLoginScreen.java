package nurgling.widgets;

import haven.*;
import haven.Label;
import haven.Window;
import nurgling.*;
import nurgling.conf.*;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class NLoginScreen extends LoginScreen
{

    private boolean msgMode = false;

    ArrayList<NLoginDataItem> loginItems = new ArrayList<>();

    int marg = UI.scale(10);

    public NLoginScreen(String hostname)
    {
        super(hostname);
        add(new LoginList(new Coord(UI.scale(200), UI.scale(bg.sz().y - marg * 2))), new Coord(marg, marg));
        optbtn.move(new Coord(UI.scale(680), UI.scale(30)));

        adda(new StatusLabel(hostname, 0.5), bgc.x, bg.sz().y, 0.5, 1);
        ArrayList<NLoginData> logpass = (ArrayList<NLoginData>) NConfig.get(NConfig.Key.credentials);
        if (logpass != null)
        {
            for (NLoginData item : logpass)
            {
                loginItems.add(new NLoginDataItem(item));
            }
        }
        try
        {
            if (new File("ver").exists())
            {
                URL upd_url = new URL((String) Objects.requireNonNull(NConfig.get(NConfig.Key.baseurl)));
                ReadableByteChannel rbc = null;
                try {
                    rbc = Channels.newChannel(upd_url.openStream());
                }
                catch (Exception ignored)
                {
                }
                FileOutputStream fos = null;
                fos = new FileOutputStream("tmp_ver");
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get("tmp_ver")), StandardCharsets.UTF_8));
                String line = reader.readLine();
                reader.close();
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get("ver")), StandardCharsets.UTF_8));
                String line2 = reader2.readLine();
                reader2.close();
                if (!line2.contains(line))
                {
                    Window win = adda(new Window(new Coord(UI.scale(150, 40)), "Attention")
                    {
                        @Override
                        public void wdgmsg(String msg, Object... args)
                        {
                            if (msg.equals("close"))
                            {
                                hide();
                            }
                            else
                            {
                                super.wdgmsg(msg, args);
                            }
                        }
                    }, bgc.x, bg.sz().y / 8, 0.5, 0.5);

                    win.add(new Label("New version available!"));
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void progress(String p)
    {
        super.progress(p);
        if (ui.core.isBotmod())
        {
            wdgmsg("login", new Object[]{new AuthClient.NativeCred(ui.core.getBotMod().user, ui.core.getBotMod().pass), false});
        }
    }

    public void wdgmsg(
            Widget sender,
            String msg,
            Object... args
    )
    {

        if (sender == this && !msgMode)
        {
            Credbox clogin = (Credbox) login;
            if (!clogin.pass.text().isEmpty())
            {
                saveLoginPass(clogin.user.text(), clogin.pass.text());
            }
            else
            {
                if (args[0] != null && args[0] instanceof AuthClient.TokenCred) ;
                saveLoginToken(clogin.user.text(), ((AuthClient.TokenCred) args[0]).token);
            }
        }
        super.wdgmsg(sender, msg, args);
    }

    void saveLoginPass(String login, String pass)
    {
        ArrayList<NLoginData> logpass;
        if (!pass.isEmpty())
        {

            logpass = (ArrayList<NLoginData>) (NConfig.get(NConfig.Key.credentials));
            if (logpass == null)
            {
                logpass = new ArrayList<>();
            }
            boolean isFound = false;
            for (NLoginData item : logpass)
            {
                if (item.name.equals(login))
                {
                    if (!pass.equals(item.pass))
                    {
                        item.pass = pass;
                        item.isTokenUsed = false;
                        NConfig.set(NConfig.Key.credentials, logpass);
                    }
                    isFound = true;
                }
            }
            if (!isFound)
            {
                logpass.add(new NLoginData(login, pass));
                NConfig.set(NConfig.Key.credentials, logpass);
            }
        }
    }


    void saveLoginToken(String login, byte[] buff)
    {
        ArrayList<NLoginData> logpass;
        if (buff.length > 0)
        {

            logpass = (ArrayList<NLoginData>) NConfig.get(NConfig.Key.credentials);
            if (logpass == null)
            {
                logpass = new ArrayList<>();
            }
            boolean isFound = false;
            for (NLoginData item : logpass)
            {
                if (item.name.equals(login))
                {
                    if (item.token != null && item.token.length == buff.length)
                    {
                        for (int i = 0; i < buff.length; i++)
                            if (buff[i] != item.token[i])
                            {
                                item.token = buff;
                                item.isTokenUsed = true;
                                item.pass = "";
                                NConfig.set(NConfig.Key.credentials, logpass);
                            }

                    }
                    else
                    {
                        item.token = buff;
                        item.isTokenUsed = true;
                        item.pass = "";
                        NConfig.set(NConfig.Key.credentials, logpass);
                    }
                    isFound = true;
                }
            }
            if (!isFound)
            {
                logpass.add(new NLoginData(login, buff));
                NConfig.set(NConfig.Key.credentials, logpass);
            }
        }
    }

    public void removeToken()
    {
        ArrayList<NLoginData> logpass = ((ArrayList<NLoginData>) NConfig.get(NConfig.Key.credentials));
        if (logpass != null)
            for (NLoginData item : logpass)
            {
                if (item.name.equals(((Credbox)login).user.text()))
                {
                    logpass.remove(item);
                    NConfig.set(NConfig.Key.credentials, logpass);
                    for (NLoginDataItem item1 : loginItems)
                    {
                        if (item1.nd == item)
                            loginItems.remove(item1);
                        break;
                    }
                    break;
                }

            }
    }


    public class LoginList extends SListBox<NLoginDataItem, Widget>
    {
        LoginList(Coord sz)
        {
            super(sz, UI.scale(25));
            pack();
        }

        protected List<NLoginDataItem> items()
        {
            return (loginItems);
        }

        protected Widget makeitem(NLoginDataItem item, int idx, Coord sz)
        {
            return (new ItemWidget<NLoginDataItem>(this, sz, item)
            {
                {
                    int len = 0;
                    int h = 0;
                    for (NLoginDataItem pL : loginItems)
                    {
                        len = Math.max(len, pL.sz.x);
                        h = pL.sz.y;
                    }
                    len = Math.max(len, UI.scale(250));
                    item.resize(new Coord(len, h));
                    add(item);
                }

                public boolean mousedown(Coord c, int button)
                {
                    boolean psel = sel == item;
                    super.mousedown(c, button);
                    return (true);
                }
            });
        }
    }


    public class NLoginDataItem extends Widget
    {
        NLoginData nd;
        Label text;
        IButton remove;

        @Override
        public void resize(Coord sz)
        {
            super.resize(sz);
            int x = sz.x - NStyle.removei[0].sz().x - UI.scale(65);
            remove.move(new Coord(x, sz.y / 2 - remove.sz.y / 2 + UI.scale(2)));
        }

        public NLoginDataItem(NLoginData nd)
        {
            this.nd = nd;
            this.text = add(new Label(nd.name, NStyle.fcomboitems), new Coord(UI.scale(7), 0));

            remove = add(new IButton(NStyle.removei[0].back, NStyle.removei[1].back, NStyle.removei[2].back)
            {
                @Override
                public void click()
                {
                    ArrayList<NLoginData> ld = ((ArrayList<NLoginData>) NConfig.get(NConfig.Key.credentials));
                    ld.remove(nd);
                    NConfig.set(NConfig.Key.credentials, ld);
                    loginItems.remove(NLoginDataItem.this);
                }
            });
            remove.settip(Resource.remote().loadwait("nurgling/hud/buttons/removeItem/u").flayer(Resource.tooltip).t);
            pack();
        }

        @Override
        public boolean mousedown(Coord c, int button)
        {
            boolean res = super.mousedown(c, button);
            if (!res)
            {
                msgMode = true;
                if (!nd.isTokenUsed)
                    NLoginScreen.this.wdgmsg("login", new Object[]{new AuthClient.NativeCred(nd.name, nd.pass), false});
                else
                    NLoginScreen.this.wdgmsg("login", new Object[]{new AuthClient.TokenCred(nd.name, nd.token), false});
                msgMode = false;
            }
            return res;
        }
    }
}
