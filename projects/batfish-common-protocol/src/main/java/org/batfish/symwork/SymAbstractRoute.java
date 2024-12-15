package org.batfish.symwork;


public abstract class SymAbstractRoute{


  public String _nextHop;

  public String _node;

  public boolean _nonRouting;

  public String _vrf;

  public SymAbstractRoute()
  {

  }


  public SymAbstractRoute(String nextHop,String node,boolean nonRouting,String vrf)
  {
    this._nextHop=nextHop;
    this._node=node;
    this._nonRouting=nonRouting;
    this._vrf=vrf;
  }

  public void SetNextHop(String nextHop)
  {
    this._nextHop=nextHop;
  }

  public void SetNode(String node)
  {
    this._node=node;
  }

  public void SetNonRouting(boolean nonRouting)
  {
    this._nonRouting=nonRouting;
  }

  public void SetVrf(String vrf)
  {
    this._vrf=vrf;
  }

}
