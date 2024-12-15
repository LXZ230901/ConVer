package org.batfish.symwork;

import java.util.HashMap;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.RoutingProtocol;

public class SymRoute {
  private HashMap<BDD,Integer> _prefix;
  private int _admin;
  private AsPath _asPath;
  private SymAspathSize _asPathSize;
  private BDD _topologyCondition;
  private SymLocalPreference _localPreference;
  private SymMed _med;
  private Ip _nextHopIp;
  private Ip _originatorIp;
  private OriginType _originType;
  private RoutingProtocol _protocol;
  private Ip _receivedFromIp;
  private RoutingProtocol _srcProtocol;
  private int _weight;
  public void setLocalPreference(int lower,int upper)
  {
    this._localPreference.setLp(lower,upper);
  }
  public void setMed(int lower,int upper)
  {
    this._med.setLp(lower,upper);
  }
  public void setAsPathSize(int lower,int upper)
  {
    this._med.setLp(lower,upper);
  }
  public SymBounder getLocalPreference()
  {
    return new SymBounder(_localPreference.getLowerBound(),_localPreference.getUpperBound());
  }
  public SymBounder getAsPathSize()
  {
    return new SymBounder(_asPathSize.getLowerBound(),_asPathSize.getUpperBound());
  }
  public SymMed getMed()
  {
    return this._med;
  }
}
