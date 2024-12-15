package org.batfish.datamodel;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import javax.annotation.Nonnull;

public class IsisRouteOlder {

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

    @Nonnull
    public IsisRouteOlder build() {
      return new IsisRouteOlder(
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
          _nextHopIp);
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


  private IsisRouteOlder(
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
      Ip nextHopIp) {
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

  public @Nonnull long getMetric() {
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
        .setNextHopIp(_nextHopIp);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IsisRouteOlder)) {
      return false;
    }
    IsisRouteOlder rhs = (IsisRouteOlder) o;
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
        && _tag == rhs._tag;
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

  public long getTag()
  {
    return _tag;
  }

  public void setNextHopIp(Ip nextHopIp)
  {
    _nextHopIp=nextHopIp;
  }
}
