package org.batfish.datamodel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.Nullable;

public class MockDataPlane implements DataPlane {

  public static class Builder {

    private Network<BgpNeighbor, BgpSession> _bgpTopology;

    private Map<String, Configuration> _configurations;

    private Map<String, Map<String, Fib>> _fibs;

    private SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> _ribs;

    @Nullable private Topology _topology;

    private SortedSet<Edge> _topologyEdges;

    Map<Ip, Set<String>> _ipOwners;

    private Builder() {
      _bgpTopology = NetworkBuilder.directed().build();
      _configurations = ImmutableMap.of();
      _fibs = ImmutableMap.of();
      _ribs = ImmutableSortedMap.of();
      _topologyEdges = ImmutableSortedSet.of();
      _ipOwners = ImmutableMap.of();
    }

    public MockDataPlane build() {
      return new MockDataPlane(
          _bgpTopology, _configurations, _fibs, _ipOwners, _ribs, _topology, _topologyEdges);
    }

    public Builder setBgpTopology(Network<BgpNeighbor, BgpSession> bgpTopology) {
      _bgpTopology = bgpTopology;
      return this;
    }

    public Builder setIpOwners(Map<Ip, Set<String>> owners) {
      _ipOwners = owners;
      return this;
    }

    public Builder setRibs(SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs) {
      _ribs = ribs;
      return this;
    }

    public Builder setTopology(Topology topology) {
      _topology = topology;
      return this;
    }

    public Builder setTopologyEdges(SortedSet<Edge> topologyEdges) {
      _topologyEdges = topologyEdges;
      return this;
    }
  }

  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  private final Network<BgpNeighbor, BgpSession> _bgpTopology;

  private final Map<String, Configuration> _configurations;

  private final Map<String, Map<String, Fib>> _fibs;

  private final Map<Ip, Set<String>> _ipOwners;

  private final SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> _ribs;

  @Nullable private final Topology _topology;

  private final SortedSet<Edge> _topologyEdges;

  private MockDataPlane(
      Network<BgpNeighbor, BgpSession> bgpTopology,
      Map<String, Configuration> configurations,
      Map<String, Map<String, Fib>> fibs,
      Map<Ip, Set<String>> ipOwners,
      SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> ribs,
      @Nullable Topology topology,
      SortedSet<Edge> topologyEdges) {
    _bgpTopology = bgpTopology;
    _configurations = configurations;
    _fibs = fibs;
    _ribs = ImmutableSortedMap.copyOf(ribs);
    _topology = topology;
    _topologyEdges = ImmutableSortedSet.copyOf(topologyEdges);
    _ipOwners = ipOwners;
  }

  @Override
  public Map<String, Map<String, Fib>> getFibs() {
    return _fibs;
  }

  @Override
  public Map<Ip, Set<String>> getIpOwners() {
    return _ipOwners;
  }

  @Override
  public SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> getRibs() {
    return _ribs;
  }

  @Override
  @Nullable
  public Topology getTopology() {
    return _topology;
  }

  @Override
  public SortedSet<Edge> getTopologyEdges() {
    return _topologyEdges;
  }

  @Override
  public Network<BgpNeighbor, BgpSession> getBgpTopology() {
    return _bgpTopology;
  }

  @Override
  public Map<String, Configuration> getConfigurations() {
    return _configurations;
  }
}
