package org.batfish.symwork;

import java.util.ArrayList;
import java.util.List;
import net.sf.javabdd.BDD;

public class Answer {
  public boolean _verified;
  public List<CounterRoute> _counter;
  public Answer()
  {
    _verified=true;
    _counter=new ArrayList<>();
  }
  public void setVerified(boolean verified)
  {
    this._verified=verified;
  }
  public void addCounter(SymBgpRoute route1,SymBgpRoute route2, BDD topologyCondition)
  {
    _counter.add(new CounterRoute(route1,route2,topologyCondition));
  }
}
