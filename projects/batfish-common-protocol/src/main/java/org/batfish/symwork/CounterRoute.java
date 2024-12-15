package org.batfish.symwork;

import net.sf.javabdd.BDD;

public class CounterRoute {
  public SymBgpRoute _route1;
  public SymBgpRoute _route2;
  public BDD _topologyCondition;
  public CounterRoute(SymBgpRoute route1,SymBgpRoute route2,BDD topologyCondition)
  {
    _route1=route1;
    _route2=route2;
    _topologyCondition=topologyCondition;
  }
}
