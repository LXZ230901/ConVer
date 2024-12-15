package org.batfish.extwork;
import com.google.common.collect.ImmutableSortedMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import jdd.bdd.BDD;
import org.batfish.dataplane.ibdp.Node;
import org.batfish.dataplane.ibdp.VirtualRouter;
import org.batfish.symwork.SymBgpFib;
import org.batfish.symwork.SymBgpRib;
import org.batfish.symwork.SymBgpRoute;
public class ForwardingEngine {
  private HashMap<String, SymBgpFib> _forwardingTable;
  private HashMap<String, ForwardingNode> _forwardingTopology;
  private BDD _topologyBDD;
  public ForwardingEngine(BDD topologyBDD)
  {
    _forwardingTable = new HashMap<>();
    _forwardingTopology = new HashMap<>();
    _topologyBDD = topologyBDD;
  }
  public void generatingForwardingTable(ImmutableSortedMap<String, Node> nodes, BDD topologyBDD)
  {
    for (Node n : nodes.values())
    {
      for (VirtualRouter vr : n.getVirtualRouters().values())
      {
        if (vr._vrf.getBgpProcess() == null || !vr._vrf.getName().equals("default"))
        {
          continue;
        }
        vr.getSymBgpRib().prepareForForwarding();
        SymBgpFib symBgpFib = vr.getSymBgpFib();
        SymBgpRib symBgpRib = vr.getSymBgpRib();
        for (BitSet prefix : symBgpRib.getRib().keySet())
        {
          if (!symBgpFib.getFib().containsKey(prefix))
          {
            symBgpFib.getFib().put(prefix, new HashMap<>());
          }
          for (List<SymBgpRoute> routeList : symBgpRib.getRib().get(prefix))
          {
            for (SymBgpRoute route : routeList)
            {
              String nexthopNode = "";
              if (route.getNodePath().size() == 0)
              {
                nexthopNode = "local";
              } else {
                nexthopNode = route.getNodePath().get(route.getNodePath().size() - 1);
              }
              if (! symBgpFib.getFib().get(prefix).containsKey(nexthopNode))
              {
                symBgpFib.getFib().get(prefix).put(nexthopNode, route.getTopologyCondition());
              } else {
                Integer forwardingTopologyCondition = symBgpFib.getFib().get(prefix).get(nexthopNode);
                forwardingTopologyCondition = topologyBDD.ref(topologyBDD.or(forwardingTopologyCondition, route.getTopologyCondition()));
                symBgpFib.getFib().get(prefix).put(nexthopNode, forwardingTopologyCondition);
              }
            }
          }
        }
        _forwardingTable.put(n.getName(), symBgpFib);
        _forwardingTopology.put(n.getName(), new ForwardingNode(n.getName(), symBgpFib.getFib()));
      }
    }
  }
  public void getAllPairReachability(BitSet prefix)
  {
    boolean forwardingFinish = false;
    for (String nodeName : _forwardingTopology.keySet())
    {
      ForwardingPacket forwardingPacket = new ForwardingPacket(prefix, new ArrayList<>(), 1);
      _forwardingTopology.get(nodeName).addForwardingPacket(forwardingPacket);
    }
    while (!forwardingFinish)
    {
      _forwardingTopology.values()
          .parallelStream()
          .forEach(ForwardingNode::matchForwardingPacket);



      for (ForwardingNode forwardingNode : _forwardingTopology.values())
      {
        HashMap<ForwardingPacket, HashMap<String, Integer>> matchForwardingpacket = forwardingNode.getMatchForwardingPacket();
        for (ForwardingPacket packet : matchForwardingpacket.keySet())
        {
          if (packet.getNodePath().contains(forwardingNode.getNodeName()))
          {
            forwardingNode.addLoopForwardingPacket(packet);
            continue;
          }
          ForwardingPacket.Builder packetBuilder = packet.toBuilder();
          HashMap<String, Integer> nexthopTuple = matchForwardingpacket.get(packet);
          for (String nexthop : nexthopTuple.keySet())
          {
            if (nexthop.equals("local"))
            {
              forwardingNode.addReceivePacket(packetBuilder.build());
              continue;
            }
            Integer topologyCondition = nexthopTuple.get(nexthop);
            Integer andTopologyCondition = _topologyBDD.ref(_topologyBDD.and(topologyCondition, packet.getTopologyCondition()));
            if ( andTopologyCondition != 0 )
            {
              ForwardingPacket tempPacket = packetBuilder.build();
              tempPacket.setTopologyCondition(andTopologyCondition);
              tempPacket.getNodePath().add(forwardingNode.getNodeName());
              _forwardingTopology.get(nexthop).addForwardingPacket(tempPacket);
            }
          }
        }
      }

      forwardingFinish = forwardingFinish();
    }
  }

  public boolean forwardingFinish()
  {
    boolean forwardingFinish = _forwardingTopology.values()
        .parallelStream()
        .allMatch(forwardingNode -> forwardingNode.getForwardingPacket().isEmpty());

    return forwardingFinish;
  }
}
