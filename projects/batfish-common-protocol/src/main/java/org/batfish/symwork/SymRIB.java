package org.batfish.symwork;

import java.util.ArrayList;

public class SymRIB {
    ArrayList<SymAbstractRoute> _rib;
    public SymRIB()
    {
        _rib=new ArrayList<>();
    }
    public void AddRoute(int index,SymAbstractRoute symRoute)
    {
        this._rib.add(index,symRoute);
    }
    public void DeleteRoute(int index)
    {
        this._rib.remove(index);
    }
    public ArrayList<SymAbstractRoute> GetRoutes()
    {
        return this._rib;
    }
    public void ImportRib(ArrayList<SymAbstractRoute> rib)
    {
        this._rib=rib;
    }
}
