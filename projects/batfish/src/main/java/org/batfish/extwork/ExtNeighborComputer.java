package org.batfish.extwork;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.Network;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpSession;
import org.batfish.dataplane.ibdp.Node;
import org.batfish.dataplane.ibdp.VirtualRouter;
import org.batfish.symwork.ExternalNeighbor;

public class ExtNeighborComputer {
  public static ExtNeighborFeature computeExternalNeighbor(
      ImmutableSortedMap<String, Node> nodes
      , Network<BgpNeighbor, BgpSession> bgpTopology)
  {
    ExtNeighborFeature extNeighborFeature = new ExtNeighborFeature();
    HashMap<ExternalNeighbor, List<BgpNeighbor>> extNeighborLink = new HashMap<>();
    HashMap<String, HashSet<String>> externalExportPolicy = new HashMap<>();
    HashMap<String, HashSet<String>> externalImportPolicy = new HashMap<>();
    Set<ExternalNeighbor> extNeighbor = new HashSet<>();
    HashMap<String, HashSet<String>> internalPolicy = new HashMap<>();
    for (Node n : nodes.values())
    {
      if (!internalPolicy.containsKey(n.getName()))
      {
        internalPolicy.put(n.getName(), new HashSet<>());
      }
      if (!externalExportPolicy.containsKey(n.getName()))
      {
        externalExportPolicy.put(n.getName(), new HashSet<>());
      }
      if (!externalImportPolicy.containsKey(n.getName()))
      {
        externalImportPolicy.put(n.getName(), new HashSet<>());
      }
      for (VirtualRouter vr : n.getVirtualRouters().values())
      {
        if (!vr._vrf.getName().equals("default"))
        {
          continue;
        }
        if (vr._vrf.getBgpProcess() == null)
        {
          continue;
        }
        for (BgpNeighbor neighbor : vr._vrf.getBgpProcess().getNeighbors().values())
        {
          if (!bgpTopology.nodes().contains(neighbor) || bgpTopology.adjacentNodes(neighbor).size()==0)
          {
            int remoteAs = neighbor.getRemoteAs();
            if (remoteAs == 11537)
            {
              continue;
            }
            ExternalNeighbor tempExternalNeighbor = new ExternalNeighbor(neighbor.getPrefix(), neighbor.getRemoteAs());
            extNeighbor.add(tempExternalNeighbor);
            if (!extNeighborLink.containsKey(tempExternalNeighbor))
            {
              extNeighborLink.put(tempExternalNeighbor, new ArrayList<>());
            }
            extNeighborLink.get(tempExternalNeighbor).add(neighbor);
            if (neighbor.getExportPolicySources() != null)
            {
              externalExportPolicy.get(n.getName()).addAll(neighbor.getExportPolicySources());
            }
            if (neighbor.getImportPolicySources() != null)
            {
              externalImportPolicy.get(n.getName()).addAll(neighbor.getImportPolicySources());
            }
          } else {
            if (neighbor.getExportPolicySources() != null)
            {
              internalPolicy.get(n.getName()).addAll(neighbor.getExportPolicySources());
            }
            if (neighbor.getImportPolicySources() != null)
            {
              internalPolicy.get(n.getName()).addAll(neighbor.getImportPolicySources());
            }
          }
        }
      }
    }
    extNeighborFeature.setExtNeighbor(extNeighbor);
    extNeighborFeature.setExtNeighborLink(extNeighborLink);
    extNeighborFeature.setExternalExportPolicy(externalExportPolicy);
    extNeighborFeature.setExternalImportPolicy(externalImportPolicy);
    extNeighborFeature.setInternalPolicy(internalPolicy);
    return extNeighborFeature;
  }
}
