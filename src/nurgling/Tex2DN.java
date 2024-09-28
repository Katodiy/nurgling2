package nurgling;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.sl.Type.SAMPLER2D;

public class Tex2DN extends State implements Disposable {

//    static final Slot<Tex2DN> slot = new Slot<>(Slot.Type.DRAW, Tex2DN.class);
    final Texture2D.Sampler2D tex;
    final Texture2D.Sampler2D src;

    public Tex2DN(Texture2D.Sampler2D tex, Texture2D.Sampler2D src)
    {
        this.tex = tex;
        this.src = src;
    }


    public Uniform tex2d(Uniform.Data<Object> data)
    {
        if(data == null)
            return null;
        return new Uniform(Type.SAMPLER2D, data.value, data.deps);
    }

//
//    private static final ShaderMacro shader = prog ->
//    {
//        Tex2D.get(prog).tex2d(new Uniform.Data<Object>(p ->
//        {
//            if(p == null)
//            {
//                return null;
//            }
//            TexRender.TexDraw draw = p.get(TexRender.TexDraw.slot);
//            TexRender.TexClip clip = p.get(TexRender.TexClip.slot);
//            if ((draw != null) && (clip != null))
//            {
//                if (draw.tex != clip.tex)
//                {
//                    throw (new RuntimeException(String.format("TexRender does not support different draw (%s) and clip (%s) textures", draw.tex, clip.tex)));
//                }
//                if (draw.tex.img == p.get(slot).src)
//                {
//                    return p.get(slot).tex;
//                }
//                else
//                {
//                    return (draw.tex.img);
//                }
//            }
//            else if (draw != null)
//            {
//                return (draw.tex.img);
//            }
//            else if (clip != null)
//            {
//                return (clip.tex.img);
//            }
//            else
//            {
//                return p.get(slot).tex;
//            }
//
//        }, TexRender.TexDraw.slot, TexRender.TexClip.slot, slot));
//        Tex2DN.mod1.modify(prog);
//    };


//    private static final ShaderMacro mod1 =new ShaderMacro()
//    {        @Override
//        public void modify(ProgramContext prog)
//        {
//            final ValBlock.Value tex2d = Tex2D.get(prog).color();
//            tex2d.force();
//        }
//    };

    public ShaderMacro shader()
    {
        return null;
//        return (shader);
    }

    public void apply(Pipe p)
    {
//        p.put(slot, this);
    }

    @Override
    public void dispose() {
        tex.dispose();
    }
}
