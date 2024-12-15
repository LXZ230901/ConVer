package org.batfish.datamodel;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import net.sf.javabdd.BDD;
import org.batfish.symwork.Reason;

public class IsisRoute extends AbstractRoute{

  public static class Builder {

    private String _area;
    private boolean _attach;
    private boolean _down;
    private IsisLevel _level;
    private boolean _overload;
    private RoutingProtocol _protocol;
    private String _systemId;
    public boolean _nonRouting;
    public String _nextHop;
    public boolean _nonForwarding;
    public long _tag;

    public Prefix _network;

    private String _node;

    private long _metric;

    public int _admin;

    public Ip _nextHopIp;

    public List<String> _nodePath;

    public BDD _topologyCodition;

    public Reason _reason;

    private Ip _originatorIp;

    @Nonnull
    public IsisRoute build() {
      return new IsisRoute(
          _admin,
          requireNonNull(_area),
          _attach,
          _down,
          requireNonNull(_level),
          _metric,
          requireNonNull(_network),
          requireNonNull(_nextHop),
          _overload,
          requireNonNull(_protocol),
          requireNonNull(_systemId),
          _tag,
          _nonForwarding,
          _nonRouting,
          _nextHopIp,
          _nodePath,
          _topologyCodition,
          _reason,
          _originatorIp);
    }

    @Nonnull
    protected Builder getThis() {
      return this;
    }

    public Builder setArea(@Nonnull String area) {
      _area = area;
      return this;
    }

    public Builder setAttach(boolean attach) {
      _attach = attach;
      return this;
    }

    public Builder setDown(boolean down) {
      _down = down;
      return this;
    }

    public Builder setLevel(@Nonnull IsisLevel level) {
      _level = level;
      return this;
    }

    public Builder setOverload(boolean overload) {
      _overload = overload;
      return this;
    }

    public Builder setProtocol(@Nonnull RoutingProtocol protocol) {
      _protocol = protocol;
      return this;
    }

    public Builder setSystemId(@Nonnull String systemId) {
      _systemId = systemId;
      return this;
    }

    public Builder setNextHop(String nextHop)
    {
      _nextHop=nextHop;
      return this;
    }

    public Builder setNonForwarding(boolean nonForwarding)
    {
      _nonForwarding=nonForwarding;
      return this;
    }

    public Builder setNonRouting(boolean nonRouting)
    {
      _nonRouting=nonRouting;
      return this;
    }

    public Builder setTag(long tag)
    {
      _tag=tag;
      return this;
    }

    public Builder setAdmin(int admin)
    {
      _admin=admin;
      return this;
    }

    public Builder setMetric(long metric)
    {
      _metric=metric;
      return this;
    }

    public Builder setNetwork(Prefix network)
    {
      _network=network;
      return this;
    }

    public Builder setNextHopIp(Ip nextHopIp)
    {
      _nextHopIp=nextHopIp;
      return this;
    }

    public Builder addNodePath(String node)
    {
      _nodePath.add(node);
      return this;
    }

    public Builder setNodePath(List<String> nodePath)
    {
      _nodePath=new ArrayList<>();
      _nodePath.addAll(nodePath);
      return this;
    }

    public Builder setTopologyCondition(BDD topologyCondition)
    {
      _topologyCodition=topologyCondition;
      return this;
    }

    public Builder setReason(Reason reason)
    {
      _reason=reason;
      return this;
    }

    public Builder setOriginatorIp(Ip originatorIp)
    {
      _originatorIp=originatorIp;
      return this;
    }
  }

  /** Default Isis route metric, unless one is explicitly specified */
  public static final long DEFAULT_METRIC = 10L;

  private static final String PROP_AREA = "area";
  private static final String PROP_ATTACH = "attach";
  private static final String PROP_DOWN = "down";
  private static final String PROP_LEVEL = "level";
  private static final String PROP_OVERLOAD = "overload";
  private static final String PROP_SYSTEM_ID = "systemId";

  private String _area;

  private boolean _attach;

  private boolean _down;

  private IsisLevel _level;

  private long _metric;

  private boolean _overload;

  private RoutingProtocol _protocol;

  private  String _systemId;

  public Prefix _network;

  public String _nextHop;

  private String _node;

  public boolean _nonRouting;

  private String _vrf;

  public int _admin;
  public boolean _nonForwarding;
  public long _tag;
  public Ip _nextHopIp;
  public String _nextHopInterface;

  public List<String> _nodePath;

  public Reason _reason;

  public BDD _topologyCondition;

  public Ip _originatorIp;

  private IsisRoute(
      int administrativeCost,
      @Nonnull String area,
      boolean attach,
      boolean down,
      @Nonnull IsisLevel level,
      long metric,
      @Nonnull Prefix network,
      @Nonnull String nextHop,
      boolean overload,
      @Nonnull RoutingProtocol protocol,
      @Nonnull String systemId,
      long tag,
      boolean nonForwarding,
      boolean nonRouting,
      Ip nextHopIp,
      List<String> nodePath,
      BDD topologyCondition,
      Reason reason,
      Ip originatorIp) {
    super(network);
    _network=network;
    _admin=administrativeCost;
    _tag=tag;
    _nonRouting=nonRouting;
    _nonForwarding=nonForwarding;
    _area = area;
    _attach = attach;
    _down = down;
    _level = level;
    _metric = metric;
    _nextHop = nextHop;
    _overload = overload;
    _protocol = protocol;
    _systemId = systemId;
    _nextHopIp = nextHopIp;
    _nodePath=new ArrayList<>();
    _nodePath.addAll(nodePath);
    _topologyCondition=topologyCondition;
    _reason=reason;
    _originatorIp=originatorIp;
  }

  /** */
  private static final long serialVersionUID = 1L;


  public static Builder builder() {
    return new Builder();
  }

  public @Nonnull String getArea() {
    return _area;
  }

  /** Attach bit is set on default route originated by L1L2 routers to L1 neighbors. */
  public boolean getAttach() {
    return _attach;
  }

  /**
   * A "down" bit indicating that this route has already been leaked from level 2 down to level 1.
   */
  public boolean getDown() {
    return _down;
  }

  public @Nonnull IsisLevel getLevel() {
    return _level;
  }

  @Override
  public Long getMetric() {
    return _metric;
  }


  /** Overload bit indicates this route came through an overloaded interface level. */
  public boolean getOverload() {
    return _overload;
  }

  public @Nonnull RoutingProtocol getProtocol() {
    return _protocol;
  }

  @Nonnull
  public String getSystemId() {
    return _systemId;
  }

  public List<String> getNodePath()
  {
    return _nodePath;
  }

  public void setNodePath(List<String> nodePath)
  {
    _nodePath=new ArrayList<>();
    _nodePath.addAll(nodePath);
  }

  public void addNodePath(String node)
  {
    _nodePath.add(node);
  }

  /////// Keep #toBuilder, #equals, and #hashCode in sync ////////

  public Builder toBuilder() {
    return new Builder()
        .setAdmin(_admin)
        .setArea(_area)
        .setAttach(_attach)
        .setDown(_down)
        .setLevel(_level)
        .setMetric(_metric)
        .setNetwork(_network)
        .setNextHop(_nextHop)
        .setNonForwarding(_nonForwarding)
        .setNonRouting(_nonRouting)
        .setOverload(_overload)
        .setProtocol(_protocol)
        .setSystemId(_systemId)
        .setTag(_tag)
        .setNextHopIp(_nextHopIp)
        .setNodePath(_nodePath)
        .setOriginatorIp(_originatorIp);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IsisRoute)) {
      return false;
    }
    IsisRoute rhs = (IsisRoute) o;
    return _admin == rhs._admin
        && _area.equals(rhs._area)
        && _attach == rhs._attach
        && _down == rhs._down
        && _level == rhs._level
        && _metric == rhs._metric
        && _network.equals(rhs._network)
        && _nextHop.equals(rhs._nextHop)
        && _nonForwarding == rhs._nonForwarding
        && _nonRouting == rhs._nonRouting
        && _overload == rhs._overload
        && _protocol == rhs._protocol
        && _systemId.equals(rhs._systemId)
        && _tag == rhs._tag
        && _nodePath.equals(rhs._nodePath)
        && _originatorIp.equals(rhs._originatorIp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _admin,
        _area,
        _attach,
        _down,
        _level.ordinal(),
        _metric,
        _network,
        _nextHop,
        _nonForwarding,
        _nonRouting,
        _overload,
        _protocol.ordinal(),
        _systemId,
        _tag);
  }

  public int getAdministrativeCost()
  {
    return _admin;
  }

  @Override
  public int getTag()
  {
    return (int)_tag;
  }

  public void setNextHopIp(Ip nextHopIp)
  {
    _nextHopIp=nextHopIp;
  }

  @Override
  public Ip getNextHopIp()
  {
    return _nextHopIp;
  }

  @Override
  public String getNextHopInterface()
  {
    return _nextHopInterface;
  }

  @Override
  public String protocolRouteString()
  {
    return null;
  }

  public void setIsisLevel(IsisLevel isisLevel)
  {
    this._level=isisLevel;
  }

  public void setRoutingProtocol(RoutingProtocol protocol)
  {
    this._protocol=protocol;
  }

  public void setTopologyCondition(BDD topologyCondition)
  {
    this._topologyCondition=topologyCondition;
  }

  public BDD getTopologyCondition()
  {
    return this._topologyCondition;
  }

  public Ip getOriginatorIp()
  {
    return this._originatorIp;
  }

  public int routeCompare(AbstractRoute rhs)
  {
    if(_admin<rhs.getAdministrativeCost())
    {
      return 1;
    }
    if(_admin> rhs.getAdministrativeCost())
    {
      return -1;
    }
    if(_metric< rhs.getMetric())
    {
      return 1;
    }
    if(_metric> rhs.getMetric())
    {
      return -1;
    }
    return 0;
  }
}
