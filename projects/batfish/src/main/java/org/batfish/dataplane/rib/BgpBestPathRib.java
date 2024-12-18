package org.batfish.dataplane.rib;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.dataplane.exceptions.BestPathSelectionException;

public class BgpBestPathRib extends AbstractRib<BgpRoute> {

  private static final long serialVersionUID = 1L;

  private final Rib _mainRib;

  private final BgpTieBreaker _tieBreaker;

  /**
   * Construct an instance with given owner and previous instance.
   *
   * @param backupRoutes a set of alternative (non-best routes)
   */
  public BgpBestPathRib(
      BgpTieBreaker tieBreaker,
      @Nullable Map<Prefix, SortedSet<BgpRoute>> backupRoutes,
      @Nullable Rib mainRib) {
    super(backupRoutes);
    _tieBreaker = tieBreaker;
    _mainRib = mainRib;
  }

  /**
   * Construct an initial best-path RIB with no age information for tie-breaking
   *
   * @param backupRoutes a set of alternative (non-best routes)
   * @return A new instance
   */
  public static BgpBestPathRib initial(
      @Nullable Map<Prefix, SortedSet<BgpRoute>> backupRoutes, @Nullable Rib mainRib) {
    return new BgpBestPathRib(BgpTieBreaker.ARRIVAL_ORDER, backupRoutes, mainRib);
  }

  @Override
  public int comparePreference(BgpRoute newRoute, BgpRoute oldRoute) {

    int res;

    /*
     * first compare local preference
     */
    res = Integer.compare(newRoute.getLocalPreference(), oldRoute.getLocalPreference());
    if (res != 0) {
      return res;
    }

    /*
     * on non-juniper, prefer aggregates (these routes won't appear on
     * juniper)
     */

    res =
        Integer.compare(
            getAggregatePreference(newRoute.getProtocol()),
            getAggregatePreference(oldRoute.getProtocol()));
    if (res != 0) {
      return res;
    }

    /*
     * then compare as path size (shorter is better, hence reversal)
     */
    res = Integer.compare(oldRoute.getAsPath().size(), newRoute.getAsPath().size());
    if (res != 0) {
      return res;
    }

    /*
     * origin type (IGP better than EGP, which is better than INCOMPLETE)
     */
    res =
        Integer.compare(
            newRoute.getOriginType().getPreference(), oldRoute.getOriginType().getPreference());
    if (res != 0) {
      return res;
    }

    /*
     * then compare MED
     *
     * TODO: handle presence/absence of always-compare-med, noting that
     * normally we only do this comparison if the first AS is the same in the
     * paths for both routes
     */

    /*
     * next prefer eBGP over iBGP
     */
    res = Integer.compare(getTypeCost(oldRoute.getProtocol()), getTypeCost(newRoute.getProtocol()));
    if (res != 0) {
      return res;
    }

    /*
     * Prefer the path with the lowest IGP metric to the BGP next hop
     */
    res = Long.compare(getIgpCostToNextHopIp(oldRoute), getIgpCostToNextHopIp(newRoute));
    if (res != 0) {
      return res;
    }

    /*
     * The remaining criteria are used for tie-breaking to end up with a
     * single best-path.
     */

    /*
     * Break tie with process's chosen tie-breaking mechanism
     */
    boolean bothEbgp =
        newRoute.getProtocol() == RoutingProtocol.BGP
            && oldRoute.getProtocol() == RoutingProtocol.BGP;
    switch (_tieBreaker) {
      case ARRIVAL_ORDER:
        if (!bothEbgp) {
          break;
        }
        /* Flip compare because older is better */
        res =
            Long.compare(
                _logicalArrivalTime.getOrDefault(oldRoute, _logicalClock),
                _logicalArrivalTime.getOrDefault(newRoute, _logicalClock));
        if (res != 0) {
          return res;
        }
        break;

      case ROUTER_ID:
        if (!bothEbgp) {
          break;
        }
        /* Prefer the route that comes from the BGP router with the lowest router ID. */
        res = oldRoute.getOriginatorIp().compareTo(newRoute.getOriginatorIp());
        if (res != 0) {
          return res;
        }
        break;

      case CLUSTER_LIST_LENGTH:
        /* Prefer the path with the minimum cluster list length. lhs/rhs flipped because lower
         * length is preferred.
         */
        res = Integer.compare(oldRoute.getClusterList().size(), newRoute.getClusterList().size());
        if (res != 0) {
          return res;
        }
        break;
      default:
        throw new BestPathSelectionException("Unhandled tie-breaker: " + _tieBreaker);
    }

    /* Prefer the route that comes from the BGP router with the lowest router ID. */
    res = oldRoute.getOriginatorIp().compareTo(newRoute.getOriginatorIp());
    if (res != 0) {
      return res;
    }

    /* Prefer the path with the minimum cluster list length. lhs/rhs flipped because lower
     * length is preferred.
     */
    res = Integer.compare(oldRoute.getClusterList().size(), newRoute.getClusterList().size());
    if (res != 0) {
      return res;
    }

    /* Prefer the path that comes from the lowest neighbor address.
     * Flipped because lower address is preferred.
     */
    res = oldRoute.getReceivedFromIp().compareTo(newRoute.getReceivedFromIp());
    if (res != 0) {
      return res;
    }
    if (newRoute.equals(oldRoute)) {
      // This is ok, because routes are stored in sets
      return 0;
    } else {
      return 1;
    }
  }

  private static int getAggregatePreference(RoutingProtocol protocol) {
    if (protocol == RoutingProtocol.AGGREGATE) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * Return the map of route prefixes to best AS path.
   *
   * @return an immutable map
   */
  public Map<Prefix, AsPath> getBestAsPaths() {
    return getRoutes()
        .stream()
        .collect(ImmutableMap.toImmutableMap(AbstractRoute::getNetwork, BgpRoute::getAsPath));
  }

  /**
   * Return the map of route prefixes to best AS path.
   *
   * @return an immutable map
   */
  public Map<Prefix, AsPath> getAllAsPaths() {
    return getAllRoutes()
        .stream()
        .collect(ImmutableMap.toImmutableMap(AbstractRoute::getNetwork, BgpRoute::getAsPath));
  }

  public List<BgpRoute> getMultiRoutes()
  {
    return getEveryRoutes();
  }

  //对应拓扑结构
  public Map<Prefix, BDD> getTopologyCondition()
  {
    Set<BgpRoute> routesTemp=getRoutes();
    List<BgpRoute> routes=new ArrayList<>(routesTemp);
    Map<Prefix,BDD> ansMap=new HashMap<>();
    for(int i=0;i<routes.size();i++)
    {
      BDD tempBDD=routes.get(i).getTopologyCondition();
      ansMap.put(routes.get(i).getNetwork(),tempBDD);
    }
    return ansMap;
  }

  private static int getTypeCost(RoutingProtocol protocol) {
    switch (protocol) {
      case AGGREGATE:
        return 0;
      case BGP: // eBGP
        return 1;
      case IBGP:
        return 2;
        // $CASES-OMITTED$
      default:
        throw new BatfishException("Invalid BGP protocol: '" + protocol + "'");
    }
  }

  /**
   * Attempt to calculate the cost to reach given routes next hop IP.
   *
   * @param route bgp route
   * @return if next hop IP matches a route we have, returns the metric for that route; otherwise
   *     {@link Long#MAX_VALUE}
   */
  private long getIgpCostToNextHopIp(BgpRoute route) {
    if (_mainRib == null) {
      return Long.MAX_VALUE;
    }
    Set<AbstractRoute> s = _mainRib.longestPrefixMatch(route.getNextHopIp());
    return s == null || s.isEmpty() ? Long.MAX_VALUE : s.iterator().next().getMetric();
  }
}
