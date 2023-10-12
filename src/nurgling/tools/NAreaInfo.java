package nurgling.tools;

public class NAreaInfo
{
    String name;

    String resname;
    NAlias items = new NAlias();
    NAlias ws = new NAlias();
    NAlias ic = new NAlias();
    NAlias containers = new NAlias();

    public NAlias getItems()
    {
        return items;
    }

    public NAlias getWorkstations()
    {
        return ws;
    }

    public NAlias getICategories()
    {
        return ic;
    }

    public NAlias getContainers()
    {
        return containers;
    }

    public NAreaInfo(String name)
    {
        this.name = name;
    }
}
