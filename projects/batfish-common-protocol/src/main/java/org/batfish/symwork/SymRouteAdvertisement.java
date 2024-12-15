package org.batfish.symwork;

public class SymRouteAdvertisement<R extends SymAbstractRoute> {
  public R _route;
  public boolean _withdraw;
  public Reason _reason;

  public SymRouteAdvertisement(R route,boolean withdraw,Reason reason)
  {
    this._route=route;
    this._withdraw=withdraw;
    this._reason=reason;
  }

  public SymRouteAdvertisement(R route)
  {
    this._route=route;
    this._withdraw=false;
    this._reason= Reason.ADD;
  }

  public R getRoute()
  {
    return this._route;
  }
}
