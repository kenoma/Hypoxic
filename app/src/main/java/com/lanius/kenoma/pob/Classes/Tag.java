package com.lanius.kenoma.pob.Classes;

public class Tag
{
    public String name = null;
    public boolean selected = false;

    public Tag(String name, boolean selected)
    {
        super();
        this.name = name;
        this.selected = selected;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

}
