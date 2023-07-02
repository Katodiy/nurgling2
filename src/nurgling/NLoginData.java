package nurgling;


import haven.*;
import org.json.*;

import java.util.*;

public class NLoginData {
    public String name = "";
    public String pass = "";
    public byte[] token;
    public boolean isTokenUsed = false;
    
    public NLoginData(String name, String pass) {
        this.name = name;
        this.pass = pass;
    }

    public NLoginData(String name, byte[] token) {
        this.name = name;
        this.token = token;
        this.isTokenUsed = true;
    }
    
    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof NLoginData ))return false;
        NLoginData ol = ( NLoginData )other;
        return ol.name.equals(name) && ol.pass.equals(pass);
    }

    @Override
    public String toString() {
        MessageBuf msg = new MessageBuf();
        msg.adduint8('l');
        msg.addstring(name);
        if(!pass.isEmpty()) {
            msg.adduint8('p');
            msg.addstring(pass);
        }
        if(isTokenUsed) {
            msg.adduint8('t');
            msg.adduint8(isTokenUsed ? 1 : 0);
            msg.addbytes(token);
        }
        return "NLoginData" + Arrays.toString(msg.fin());
    }

    NLoginData(String value)
    {
        String [] arr = value.substring(value.lastIndexOf("[")+1,value.lastIndexOf("]")).split(", ");
        ArrayList<Byte> buf = new ArrayList<>();
        for(String item: arr)
        {
            buf.add(Byte.parseByte(item));
        }
        byte[] bbuf = new byte[buf.size()];
        int j = 0;
        for(Byte b: buf)
            bbuf[j++] = b;
        MessageBuf mbuf = new MessageBuf(bbuf);
        while (!mbuf.eom()){
            switch (mbuf.uint8())
            {
                case 'l': {
                    name = mbuf.string();
                    break;
                }
                case 'p':
                {
                    pass = mbuf.string();
                    break;
                }
                case 't':
                {
                    isTokenUsed = mbuf.uint8()==1;
                    token = mbuf.bytes();
                }
            }
        }
    }
}