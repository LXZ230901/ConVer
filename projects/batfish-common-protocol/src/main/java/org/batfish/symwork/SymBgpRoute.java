package org.batfish.symwork;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import net.sf.javabdd.BDDFactory;

import org.batfish.datamodel.Ip;
import org.batfish.datamodel.RoutingProtocol;

public class SymBgpRoute extends SymAbstractRoute {
  public static class Builder {
    private jdd.bdd.BDD _topologyBDD;
    private BDDFactory _prefixFactory;

    private BitSet _prefixEcSet;
    private Integer _prefixEcNum;
    private Integer _localPreference;
    private Integer _community;
    private List<Integer> _asPath;
    private Integer _asPathSize;
    private Integer _topologyCondition;
    private Ip _nextHopIp;
    private Ip _originatorIp;

    private RoutingProtocol _protocol;
    private Reason _reason;
    private List<String> _nodePath;
    private boolean _external;
    private Integer _originatorIndex;

    public Builder(SymBgpRoute bgpRoute) {
      List<Integer> asPath = new ArrayList<>();
      asPath.addAll(bgpRoute.getAsPath());



      this._prefixEcSet = (BitSet) bgpRoute.getPrefixEcSet().clone();
      this._topologyBDD = bgpRoute.getTopologyBDD();
      this._prefixFactory = bgpRoute.getPrefixFactory();

      this._prefixEcNum = 0;
      this._localPreference = bgpRoute.getLocalPreference();
      this._community = bgpRoute.getCommunity();
      this._asPath = asPath;
      this._asPathSize = bgpRoute.getAsPathSize();
      this._topologyCondition = bgpRoute.getTopologyCondition();



      this._nextHopIp = bgpRoute.getNextHopIp();
      this._originatorIp = bgpRoute.getOriginatorIp();
      this._reason = bgpRoute.getReason();
      this._nodePath=new ArrayList<>();
      this._nodePath.addAll(bgpRoute.getNodePath());
      this._protocol = bgpRoute.getRoutingProtocol();
      this._external = bgpRoute.getExternal();
      this._originatorIndex = bgpRoute.getOriginatorIndex();
    }

    public Builder(BitSet prefixEcSet, jdd.bdd.BDD topologyBDD, BDDFactory prefixFactory) {
      this._prefixEcSet = prefixEcSet;
      this._topologyBDD = topologyBDD;
      this._prefixFactory = prefixFactory;

      this._prefixEcNum = 0;
      this._localPreference = 100;
      this._community = 0;
      this._asPath = new ArrayList<>();
      this._asPathSize = 0;
      this._topologyCondition = 0;

      this._nextHopIp = Ip.AUTO;
      this._originatorIp = Ip.AUTO;
      this._reason = Reason.NORMAL;
      this._nodePath = new ArrayList<>();
      this._protocol = RoutingProtocol.BGP;
      this._external = false;
      this._originatorIndex = 0;
    }

//    public Builder(int prefixEcNum, jdd.bdd.BDD topologyBDD, BDDFactory prefixFactory) {
//      this._prefixEcNum=prefixEcNum;
//      this._topologyBDD = topologyBDD;
//      this._prefixFactory = prefixFactory;
//
//      this._prefixEcSet = new BitSet();
//      this._localPreference = 100;
//      this._community = 0;
//      this._asPath = new ArrayList<>();
//      this._asPathSize = 0;
//      this._topologyCondition = 0;
//
//      this._nextHopIp = Ip.AUTO;
//      this._originatorIp = Ip.AUTO;
//      this._reason = Reason.NORMAL;
//      this._nodePath = new ArrayList<>();
//      this._protocol = RoutingProtocol.BGP;
//      this._external = false;
//      this._originatorIndex = 0;
//    }
//
//    public SymBgpRoute build(boolean noEcSet) {
//      return new SymBgpRoute(
//          _topologyBDD,
//          _prefixFactory,
//          _prefixEcNum,
//          _localPreference,
//          _community,
//          _asPath,
//          _asPathSize,
//          _topologyCondition,
//          _nextHopIp,
//          _originatorIp,
//          _reason,
//          _nodePath,
//          _protocol,
//          _external,
//          _originatorIndex
//          );
//    }

    public SymBgpRoute build() {
      return new SymBgpRoute(
          _topologyBDD,
          _prefixFactory,
          _prefixEcSet,
          _localPreference,
          _community,
          _asPath,
          _asPathSize,
          _topologyCondition,
          _nextHopIp,
          _originatorIp,
          _reason,
          _nodePath,
          _protocol,
          _external,
          _originatorIndex
      );
    }

    public void setReason(Reason reason) {
      this._reason = reason;
    }


    public void setTopologyBDD(jdd.bdd.BDD bdd) {
      this._topologyBDD = bdd;
    }

    public void setPrefixFactory(BDDFactory factory) {
      this._prefixFactory = factory;
    }

    public void setNextHopIp(Ip nextHopIp) {
      this._nextHopIp = nextHopIp;
    }

    public void setOriginatorIp(Ip originatorIp) {
      this._originatorIp = originatorIp;
    }


    public void setAsPath(List<Integer> asPath) {
      this._asPath = new ArrayList<>();
      this._asPath.addAll(asPath);
    }

    public void setPrefixEcSet(BitSet prefixEcSet)
    {
      this._prefixEcSet = (BitSet) prefixEcSet.clone();
    }

    public void setLocalPreference(Integer localPreference) {
      this._localPreference = localPreference;
    }

    public void setAsPathSize(Integer asPathSize) {
      this._asPathSize = asPathSize;
    }

    public void setTopologyCondition(int topologyCondition) {
      this._topologyCondition = _topologyBDD.ref(topologyCondition);
    }

    public void setCommunity(Integer community)
    {
      this._community = community;
    }

//    public void setPrefixEcNum(Integer prefixEcNum) {
//      this._prefixEcNum = prefixEcNum;
//    }

    public void setNodePath(List<String> nodePath)
    {
      this._nodePath.clear();
      this._nodePath.addAll(nodePath);
    }

    public void setRoutingProtocol(RoutingProtocol protocol)
    {
      this._protocol = protocol;
    }

    public void addNodePath(String node)
    {
      this._nodePath.add(node);
    }

    public void setExternal(boolean external)
    {
      this._external = external;
    }

    public void setOriginatorIndex(Integer index)
    {
      this._originatorIndex = index;
    }

    public jdd.bdd.BDD getTopologyFactory() {
      return this._topologyBDD;
    }

    public BDDFactory getPrefixFactory() {
      return this._prefixFactory;
    }

    public Integer getLocalPreference() {
      return this._localPreference;
    }

    public Integer getAsPathSize() {
      return this._asPathSize;
    }

    public List<Integer> getAsPath() {
      return this._asPath;
    }

    public int getTopologyCondition() {
      return this._topologyCondition;
    }

    public Ip getNextHopIp() {
      return this._nextHopIp;
    }

    public Ip getOriginatorIp() {
      return this._originatorIp;
    }

    public RoutingProtocol getRoutingProtocol() {
      return this._protocol;
    }

//    public Integer getPrefixEcNum() {
//      return this._prefixEcNum;
//    }


    public BitSet getPrefixEcSet()
    {
      return this._prefixEcSet;
    }

    public List<String> getNodePath()
    {
      return this._nodePath;
    }




    public Integer getCommunity()
    {
      return this._community;
    }

    public Reason getReason()
    {
      return this._reason;
    }

    public boolean getExternal()
    {
      return this._external;
    }



    public Integer getOriginatorIndex()
    {
      return this._originatorIndex;
    }
  }

  private jdd.bdd.BDD _topologyBDD;
  private BDDFactory _prefixFactory;

  private BitSet _prefixEcSet;
  private Integer _prefixEcNum;
  private Integer _localPreference;
  private Integer _community;
  private List<Integer> _asPath;
  private Integer _asPathSize;
  private Integer _topologyCondition;
  private Ip _nextHopIp;
  private Ip _originatorIp;

  private RoutingProtocol _protocol;
  private Reason _reason;
  private List<String> _nodePath;

  private Integer _lastRoundTopologyCondition;

  private boolean _external;
  private Integer _originatorIndex;
  public SymBgpRoute(jdd.bdd.BDD topologyBDD, BDDFactory prefixFactory) {
    super();
    this._topologyBDD = topologyBDD;
    this._prefixFactory = prefixFactory;
  }

//  public SymBgpRoute(int prefixEcNum, jdd.bdd.BDD topologyBDD, BDDFactory prefixFactory) {
//    super();
//    this._topologyBDD = topologyBDD;
//    this._prefixFactory = prefixFactory;
//
//
//    this._prefixEcNum = prefixEcNum;
//    this._localPreference = 100;
//    this._community = 0;
//    this._asPath = new ArrayList<>();
//    this._asPathSize = 0;
//    this._topologyCondition = 0;
//
//    this._nextHopIp = Ip.AUTO;
//    this._originatorIp = Ip.AUTO;
//    this._reason = Reason.NORMAL;
//    this._nodePath = new ArrayList<>();
//    this._protocol = RoutingProtocol.BGP;
//
//    this._lastRoundTopologyCondition = 0;
//    this._external = false;
//    this._originatorIndex = 0;
//  }

  public SymBgpRoute(BitSet prefixEcSet, jdd.bdd.BDD topologyBDD, BDDFactory prefixFactory) {
    super();
    this._topologyBDD = topologyBDD;
    this._prefixFactory = prefixFactory;

    this._prefixEcSet = (BitSet) prefixEcSet.clone();
    this._prefixEcNum = 0;
    this._localPreference = 100;
    this._community = 0;
    this._asPath = new ArrayList<>();
    this._asPathSize = 0;
    this._topologyCondition = 0;

    this._nextHopIp = Ip.AUTO;
    this._originatorIp = Ip.AUTO;
    this._reason = Reason.NORMAL;
    this._nodePath = new ArrayList<>();
    this._protocol = RoutingProtocol.BGP;

    this._lastRoundTopologyCondition = 0;
    this._external = false;
    this._originatorIndex = 0;
  }


//  public SymBgpRoute(jdd.bdd.BDD topologyBDD,
//      BDDFactory prefixFactory,
//      Integer prefixEcNum,
//      Integer localPreference,
//      Integer community,
//      List<Integer> asPath,
//      Integer asPathSize,
//      Integer topologyCondition,
//      Ip nextHopIp,
//      Ip originatorIp,
//      Reason reason,
//      List<String> nodePath,
//      RoutingProtocol protocol,
//      boolean external,
//      Integer originatorIndex)
//  {
//    this._topologyBDD = topologyBDD;
//    this._prefixFactory = prefixFactory;
//
//    this._prefixEcNum = prefixEcNum;
//    this._localPreference = localPreference;
//    this._community = community;
//    this._asPath = new ArrayList<>();
//    this._asPath.addAll(asPath);
//    this._asPathSize = asPathSize;
//    this._topologyCondition = topologyCondition;
//
//    this._nextHopIp = nextHopIp;
//    this._originatorIp = originatorIp;
//    this._reason = reason;
//    this._nodePath = new ArrayList<>();
//    this._nodePath.addAll(nodePath);
//    this._protocol = protocol;
//
//    this._lastRoundTopologyCondition = 0;
//    this._external = external;
//    this._originatorIndex = originatorIndex;
//  }

  public SymBgpRoute(jdd.bdd.BDD topologyBDD,
      BDDFactory prefixFactory,
      BitSet prefixEcSet,
      Integer localPreference,
      Integer community,
      List<Integer> asPath,
      Integer asPathSize,
      Integer topologyCondition,
      Ip nextHopIp,
      Ip originatorIp,
      Reason reason,
      List<String> nodePath,
      RoutingProtocol protocol,
      boolean external,
      Integer originatorIndex)
  {
    this._topologyBDD = topologyBDD;
    this._prefixFactory = prefixFactory;

    this._prefixEcSet = (BitSet) prefixEcSet.clone();
    this._localPreference = localPreference;
    this._community = community;
    this._asPath = new ArrayList<>();
    this._asPath.addAll(asPath);
    this._asPathSize = asPathSize;
    this._topologyCondition = topologyCondition;

    this._nextHopIp = nextHopIp;
    this._originatorIp = originatorIp;
    this._reason = reason;
    this._nodePath = new ArrayList<>();
    this._nodePath.addAll(nodePath);
    this._protocol = protocol;

    this._lastRoundTopologyCondition = 0;
    this._external = external;
    this._originatorIndex = originatorIndex;
  }

  public void setPrefixEcSet(BitSet prefixEcSet)
  {
    this._prefixEcSet = (BitSet) prefixEcSet.clone();
  }

  public void setReason(Reason reason) {
    this._reason = reason;
  }


  public void setTopologyBDD(jdd.bdd.BDD bdd) {
    this._topologyBDD = bdd;
  }

  public void setPrefixFactory(BDDFactory factory) {
    this._prefixFactory = factory;
  }

  public void setNextHopIp(Ip nextHopIp) {
    this._nextHopIp = nextHopIp;
  }

  public void setOriginatorIp(Ip originatorIp) {
    this._originatorIp = originatorIp;
  }


  public void setAsPath(List<Integer> asPath) {
    this._asPath = new ArrayList<>();
    this._asPath.addAll(asPath);
  }

  public void setLocalPreference(Integer localPreference) {
    this._localPreference = localPreference;
  }

  public void setAsPathSize(Integer asPathSize) {
    this._asPathSize = asPathSize;
  }

  public void setTopologyCondition(int topologyCondition) {
    this._topologyCondition = _topologyBDD.ref(topologyCondition);
  }

  public void setCommunity(Integer community)
  {
    this._community = community;
  }

//  public void setPrefixEcNum(Integer prefixEcNum) {
//    this._prefixEcNum = prefixEcNum;
//  }

  public void setNodePath(List<String> nodePath)
  {
    this._nodePath.clear();
    this._nodePath.addAll(nodePath);
  }

  public void addNodePath(String node)
  {
    this._nodePath.add(node);
  }

  public jdd.bdd.BDD getTopologyFactory() {
    return this._topologyBDD;
  }

  public BDDFactory getPrefixFactory() {
    return this._prefixFactory;
  }

  public Integer getLocalPreference() {
    return this._localPreference;
  }

  public Integer getAsPathSize() {
    return this._asPathSize;
  }

  public List<Integer> getAsPath() {
    return this._asPath;
  }

  public int getTopologyCondition() {
    return this._topologyCondition;
  }

  public Ip getNextHopIp() {
    return this._nextHopIp;
  }

  public Ip getOriginatorIp() {
    return this._originatorIp;
  }

  public RoutingProtocol getRoutingProtocol() {
    return this._protocol;
  }

//  public Integer getPrefixEcNum() {
//    return this._prefixEcNum;
//  }




  public List<String> getNodePath()
  {
    return this._nodePath;
  }


  public BitSet getPrefixEcSet()
  {
    return this._prefixEcSet;
  }

  public Integer getCommunity()
  {
    return this._community;
  }

  public Reason getReason()
  {
    return this._reason;
  }

  public jdd.bdd.BDD getTopologyBDD()
  {
    return this._topologyBDD;
  }

  public void setLastRoundTopologyCondition(Integer tc)
  {
    this._lastRoundTopologyCondition = tc;
  }

  public Integer getLastRoundTopologyCondition()
  {
    return this._lastRoundTopologyCondition;
  }






  public boolean getExternal()
  {
    return this._external;
  }

  public void setExternal(boolean external)
  {
    this._external = external;
  }

  public void setOriginatorIndex(Integer originatorIndex)
  {
    this._originatorIndex = originatorIndex;
  }

  public void setRoutingProtocol(RoutingProtocol protocol)
  {
    this._protocol = protocol;
  }
  public Integer getOriginatorIndex()
  {
    return this._originatorIndex;
  }

  public Integer comparePreference(SymBgpRoute compareRoute)
  {
    Integer preference = 0;
    if (!Objects.equals(_localPreference, compareRoute.getLocalPreference()))
    {
      if (_localPreference < compareRoute.getLocalPreference())
      {
        return -1;
      } else {
        return 1;
      }
    }
    if (!Objects.equals(_asPathSize, compareRoute.getAsPathSize()))
    {
      if (_asPathSize < compareRoute.getAsPathSize())
      {
        return 1;
      } else {
        return -1;
      }
    }

    if (!_originatorIp.equals(compareRoute.getOriginatorIp()))
    {
      return (-_originatorIp.compareTo(compareRoute.getOriginatorIp()));
    }
    return preference;
  }
  @Override
  public boolean equals(Object o)
  {
    if(o==this)
    {
      return true;
    }else if(!(o instanceof SymBgpRoute))
    {
      return false;
    }
    SymBgpRoute other=(SymBgpRoute) o;
//    if(!Objects.equals(_prefixEcNum, other.getPrefixEcNum()))
//    {
//      return false;
//    }
    if(!Objects.equals(_prefixEcSet, other.getPrefixEcSet()))
    {
      return false;
    }
    if(!_asPath.equals(other._asPath))
    {
      return false;
    }
    if(!_asPathSize.equals(other._asPathSize))
    {
      return false;
    }
    // if(!_topologyCondition.equals(other._topologyCondition))
    // {
    //   return false;
    // }
    if(!_localPreference.equals(other._localPreference))
    {
      return false;
    }
    if(!_nextHopIp.equals(other._nextHopIp))
    {
      return false;
    }
    if(!_originatorIp.equals(other._originatorIp))
    {
      return false;
    }
    if (!_community.equals(other._community))
    {
      return false;
    }
    if (!_nodePath.equals(other._nodePath))
    {
      return false;
    }
    if (_external != other._external)
    {
      return false;
    }
    if (!_originatorIndex.equals(other._originatorIndex))
    {
      return false;
    }
    return _protocol.equals(other._protocol);
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _prefixEcSet.hashCode();
//    result = prime * result + _prefixEcNum;
    result = prime * result + _localPreference.hashCode();
    result = prime * result + _asPathSize.hashCode();
    //    result = prime * result + _topologyCondition.hashCode();
    result = prime * result + _nodePath.hashCode();
    result = prime * result + ((_asPath == null) ? 0 : _asPath.hashCode());
    //    result = prime * result + ((_communities == null) ? 0 : _communities.hashCode());
    result = prime * result + ((_nextHopIp == null) ? 0 : _nextHopIp.hashCode());
    result = prime * result + ((_originatorIp == null) ? 0 : _originatorIp.hashCode());
    result = prime * result + ((_protocol == null) ? 0 : _protocol.ordinal());
    result = prime * result + ((_community == null) ? 0 : _community.hashCode());
    result = prime * result + _originatorIndex.hashCode();
    return result;
  }

  public Builder toBuilder()
  {
    return new Builder(this);
  }
}
