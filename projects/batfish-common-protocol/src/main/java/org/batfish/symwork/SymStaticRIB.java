package org.batfish.symwork;

import java.util.HashSet;
import java.util.Set;

public class SymStaticRIB {
  Set<SymStaticRoute> _rib;
  public SymStaticRIB()
  {
    this._rib=new HashSet<>();
  }
  public void AddRoute(SymStaticRoute symStaticRoute)
  {
    System.out.println("44444");
    this._rib.add(symStaticRoute);
    System.out.println("55555");
  }
  public Set<SymStaticRoute> getRoutes()
  {
    return this._rib;
  }
}
