package org.batfish.datamodel;

import com.google.common.graph.Network;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.batfish.symwork.SymBgpRib;

public interface DataPlane extends Serializable {

  Network<BgpNeighbor, BgpSession> getBgpTopology();

  Map<String, Configuration> getConfigurations();

  Map<String, Map<String, Fib>> getFibs();

  /**
   * Return the map of Ip owners (as computed during dataplane computation). Map structure: Ip ->
   * Set of hostnames
   */
  Map<Ip, Set<String>> getIpOwners();

  /** Return the set of all (main) RIBs. Map structure: hostname -> VRF name -> GenericRib */
  SortedMap<String, SortedMap<String, GenericRib<AbstractRoute>>> getRibs();

  //SortedMap<String, SymBgpRib> getBgpRibs();

  Topology getTopology();

  SortedSet<Edge> getTopologyEdges();
}
