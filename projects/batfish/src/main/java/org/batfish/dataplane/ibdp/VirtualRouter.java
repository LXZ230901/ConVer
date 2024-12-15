package org.batfish.dataplane.ibdp;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.batfish.common.util.CollectionUtil.toImmutableSortedMap;
import static org.batfish.datamodel.MultipathEquivalentAsPathMatchMode.EXACT_PATH;
import static org.batfish.dataplane.rib.AbstractRib.importRib;
import static org.batfish.dataplane.rib.RibDelta.importRibDelta;
import static org.batfish.dataplane.rib.RibDelta.importRibDeltaWithTopology;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.graph.Network;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.BgpTieBreaker;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Fib;
import org.batfish.datamodel.FibImpl;
import org.batfish.datamodel.GeneratedRoute;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.InterfaceAddress;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpAccessListLine;
import org.batfish.datamodel.IsisEdge;
import org.batfish.datamodel.IsisInterfaceLevelSettings;
import org.batfish.datamodel.IsisInterfaceMode;
import org.batfish.datamodel.IsisInterfaceSettings;
import org.batfish.datamodel.IsisLevel;
import org.batfish.datamodel.IsisLevelSettings;
import org.batfish.datamodel.IsisNode;
import org.batfish.datamodel.IsisProcess;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.IsisTopology;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.MultipathEquivalentAsPathMatchMode;
import org.batfish.datamodel.OriginType;
import org.batfish.datamodel.OspfArea;
import org.batfish.datamodel.OspfAreaSummary;
import org.batfish.datamodel.OspfExternalRoute;
import org.batfish.datamodel.OspfExternalType1Route;
import org.batfish.datamodel.OspfExternalType2Route;
import org.batfish.datamodel.OspfInterAreaRoute;
import org.batfish.datamodel.OspfInternalRoute;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfMetricType;
import org.batfish.datamodel.OspfProcess;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RipInternalRoute;
import org.batfish.datamodel.RipProcess;
import org.batfish.datamodel.Route;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.Vrf;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.dataplane.rib.BgpBestPathRib;
import org.batfish.dataplane.rib.BgpMultipathRib;
import org.batfish.dataplane.rib.ConnectedRib;
import org.batfish.dataplane.rib.IsisLevelRib;
import org.batfish.dataplane.rib.IsisRib;
import org.batfish.dataplane.rib.OspfExternalType1Rib;
import org.batfish.dataplane.rib.OspfExternalType2Rib;
import org.batfish.dataplane.rib.OspfInterAreaRib;
import org.batfish.dataplane.rib.OspfIntraAreaRib;
import org.batfish.dataplane.rib.OspfRib;
import org.batfish.dataplane.rib.Rib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RipInternalRib;
import org.batfish.dataplane.rib.RipRib;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;
import org.batfish.dataplane.rib.StaticRib;
import org.batfish.isis.IsisMultiRib;
import org.batfish.isis.IsisMultiRouteAdvertisement;
import org.batfish.symwork.SymBgpFib;
import org.batfish.symwork.SymBgpRib;
import org.batfish.symwork.SymBgpRoute;
import org.batfish.symwork.SymContectRIB;
import org.batfish.symwork.SymPolicyAnswer;
import org.batfish.symwork.SymPrefixList;
import org.batfish.symwork.SymRIB;
import org.batfish.symwork.SymRouteAdvertisement;
import org.batfish.symwork.SymStaticRIB;

public class VirtualRouter extends ComparableStructure<String> {

  private static final long serialVersionUID = 1L;

  //对应的拓扑边编码
  private HashMap<String,Integer> _edgeEncode = new HashMap<>();

  public  BDDFactory _bddFactory;

  /** Route dependency tracker for BGP aggregate routes */
  private transient RouteDependencyTracker<BgpRoute, AbstractRoute> _bgpAggDeps =
      new RouteDependencyTracker<>();

  /** Builder for constructing {@link RibDelta} as pertains to the best-path BGP RIB */
  private transient RibDelta.Builder<BgpRoute> _bgpBestPathDeltaBuilder;

  /** Best-path BGP RIB */
  transient BgpBestPathRib _bgpBestPathRib;

  /** Incoming messages into this router from each bgp neighbor (indexed by prefix) */
  transient SortedMap<UndirectedBgpSession, Queue<RouteAdvertisement<AbstractRoute>>>
      _bgpIncomingRoutes;

  //BGP的进入方向的消息队列
  transient SortedMap<UndirectedBgpSession, Queue<SymRouteAdvertisement<SymBgpRoute>>>
      _symBgpIncomingRoutes;
  transient SortedMap<UndirectedBgpSession, Map<Integer,List<SymBgpRoute>>>
      _denyList;


  /** Builder for constructing {@link RibDelta} as pertains to the multipath BGP RIB */
  private transient RibDelta.Builder<BgpRoute> _bgpMultiPathDeltaBuilder;

  /** BGP multipath RIB */
  transient BgpMultipathRib _bgpMultipathRib;

  /** Parent configuration for this Virtual router */
  private final Configuration _c;

  /** The RIB containing connected routes */
  private transient ConnectedRib _connectedRib;

  /** Helper RIB containing best paths obtained with external BGP */
  transient BgpBestPathRib _ebgpBestPathRib;

  /** Helper RIB containing all paths obtained with external BGP */
  transient BgpMultipathRib _ebgpMultipathRib;

  /**
   * Helper RIB containing paths obtained with external eBGP during current iteration. An Adj-RIB of
   * sorts.
   */
  transient BgpMultipathRib _ebgpStagingRib;

  /** FIB (forwarding information base) built from the main RIB */
  Fib _fib;

  /** RIB containing generated routes */
  private transient Rib _generatedRib;

  /** Helper RIB containing best paths obtained with iBGP */
  transient BgpBestPathRib _ibgpBestPathRib;

  /** Helper RIB containing all paths obtained with iBGP */
  transient BgpMultipathRib _ibgpMultipathRib;

  /**
   * Helper RIB containing paths obtained with iBGP during current iteration. An Adj-RIB of sorts.
   */
  transient BgpMultipathRib _ibgpStagingRib;

  /**
   * The independent RIB contains connected and static routes, which are unaffected by BDP
   * iterations (hence, independent).
   */
  transient Rib _independentRib;

  /** The finalized RIB, a combination different protocol RIBs */
  Rib _mainRib;

  //最终的符号化路由表
  public SymRIB _symRib;
  //符号化的直连的路由表
  public SymContectRIB _symContectRib;
  //静态接口路由表
  public SymStaticRIB _symStaticRib;
  //最终的BGP路由表
  public SymBgpRib _symBgpRib;

  public Set<Prefix> _connectPrefix;

  /** Keeps track of changes to the main RIB */
  private transient RibDelta.Builder<AbstractRoute> _mainRibRouteDeltaBuiler;

  transient OspfExternalType1Rib _ospfExternalType1Rib;

  transient OspfExternalType1Rib _ospfExternalType1StagingRib;

  transient OspfExternalType2Rib _ospfExternalType2Rib;

  transient OspfExternalType2Rib _ospfExternalType2StagingRib;

  private transient RibDelta.Builder<OspfExternalRoute> _ospfExternalDeltaBuiler;

  @VisibleForTesting
  transient SortedMap<Prefix, Queue<RouteAdvertisement<OspfExternalRoute>>>
      _ospfExternalIncomingRoutes;

  transient OspfInterAreaRib _ospfInterAreaRib;

  transient OspfInterAreaRib _ospfInterAreaStagingRib;

  transient OspfIntraAreaRib _ospfIntraAreaRib;

  transient OspfIntraAreaRib _ospfIntraAreaStagingRib;

  private transient Map<Prefix, VirtualRouter> _ospfNeighbors;

  transient OspfRib _ospfRib;

  /** Set of all valid OSPF external routes that we know about */
  private Map<Prefix, SortedSet<OspfExternalType1Route>> _receivedOspExternalType1Routes;

  private Map<Prefix, SortedSet<OspfExternalType2Route>> _receivedOspExternalType2Routes;

  /** Set of all valid BGP "advertisements" we have received */
  private Map<Prefix, SortedSet<BgpRoute>> _receivedBgpRoutes;

  Set<BgpAdvertisement> _receivedBgpAdvertisements;

  transient RipInternalRib _ripInternalRib;

  transient RipInternalRib _ripInternalStagingRib;

  transient RipRib _ripRib;

  Set<BgpAdvertisement> _sentBgpAdvertisements;

  transient StaticRib _staticInterfaceRib;

  transient StaticRib _staticRib;

  public final Vrf _vrf;

  public BDDFactory _prefixFactory;


  //BGP in ACL
  Map<String,BDD> _inDstAcl;

  Map<String,BDD> _inSrcAcl;

  //isisAdvertisement
  SortedMap<IsisEdge, Queue<RouteAdvertisement<IsisRoute>>> _isisNavieIncomingRoutes;
  SortedMap<IsisEdge, Queue<SymRouteAdvertisement<SymBgpRoute>>> _isisIncomingRoutes;
  SortedMap<IsisEdge, Queue<IsisMultiRouteAdvertisement>> _isisMultiIncomingRoutes;

  //isisRib
//  IsisRib _isisL1Rib;
//  IsisRib _isisL2Rib;
//  private IsisRib _isisRib;
  IsisLevelRib _isisL1Rib;
  IsisLevelRib _isisL2Rib;
  private IsisLevelRib _isisL1StagingRib;
  private IsisLevelRib _isisL2StagingRib;
  private IsisRib _isisRib;

  public Builder<IsisRoute> _isisRibDeltaBuilder;
  public boolean _isisRibChanged;

  public IsisMultiRib _isisMultiRib;

  public SortedMap<String, Map<Integer,List<SymBgpRoute>>> _denyRouteListChanged;

  public Map<String,Integer> _linkTopology;

  public  jdd.bdd.BDD _topologyBDD;

  private HashMap<BgpNeighbor, HashSet<SymRouteAdvertisement<SymBgpRoute>>> _externalRouteList;

  private SymBgpFib _symBgpFib;

  VirtualRouter(final String name, final Configuration c) {
    super(name);
    _c = c;
    _vrf = c.getVrfs().get(name);
    initRibs();
    // Keep track of sent and received advertisements
    _receivedBgpAdvertisements = new LinkedHashSet<>();
    _sentBgpAdvertisements = new LinkedHashSet<>();
    _receivedOspExternalType1Routes = new TreeMap<>();
    _receivedOspExternalType2Routes = new TreeMap<>();
    _receivedBgpRoutes = new TreeMap<>();
    _bgpIncomingRoutes = new TreeMap<>();
    _symBgpIncomingRoutes = new TreeMap<>();
    _denyList=new TreeMap<>();
    _symRib=new SymRIB();
    _symContectRib=new SymContectRIB();
    _symStaticRib=new SymStaticRIB();
    //_symBgpRib=new SymBgpRib(this._prefixFactory);
    _inDstAcl=new HashMap<>();
    _inSrcAcl=new HashMap<>();
    _linkTopology=new HashMap<>();

    _externalRouteList = new HashMap<>();
    _symBgpFib = new SymBgpFib();
  }


  public void initSymBgpRib(jdd.bdd.BDD topologyBDD,int topologyFilter,Map<String,Integer> topologyEdgeBDD, boolean multiPath)
  {
    _symBgpRib=new SymBgpRib(topologyBDD,topologyFilter,_c.getHostname(), multiPath);
    _linkTopology=topologyEdgeBDD;
    _topologyBDD=topologyBDD;
  }

  public void initInAcl(SortedMap<String, Node> nodes)
  {
    for(String srcNode:_c.getIncomingEdges().keySet())
    {
//      if(!_inSrcAcl.containsKey(srcNode))
//      {
//        _inSrcAcl.put(srcNode,_prefixFactory.zero());
//        _inDstAcl.put(srcNode,_prefixFactory.zero());
//      }
      Edge edge=_c.getIncomingEdges().get(srcNode);
      String srcIntName=edge.getInt1();
      String dstIntName=edge.getInt2();
      Interface srcInt=nodes.get(srcNode).getConfiguration().getInterfaces().get(srcIntName);
      Interface dstInt=_c.getInterfaces().get(dstIntName);
      BDD srcDeniedAcl=_prefixFactory.zero();
      BDD dstDeniedAcl=_prefixFactory.zero();
      if(srcInt.getOutgoingFilter()!=null)
      {
        List<IpAccessListLine> srcIpAclLine=srcInt.getOutgoingFilter().getLines();
        for(IpAccessListLine line:srcIpAclLine)
        {
          if(line.getMatchCondition() instanceof MatchHeaderSpace)
          {
            MatchHeaderSpace matchHeader=(MatchHeaderSpace) line.getMatchCondition();
            HeaderSpace headerSpace=matchHeader.getHeaderspace();
            String dstIp=headerSpace.getDstIps().toString();
            String srcIp=headerSpace.getSrcIps().toString();
            if (dstIp!=null)
            {
              String targetIp=dstIp.substring(dstIp.indexOf("=")+1,dstIp.length()-1);
              BDD tempDstIp=convertAclToBDD(targetIp);
              if(line.getAction()== LineAction.REJECT)
              {
                dstDeniedAcl=dstDeniedAcl.or(tempDstIp);
                //_inDstAcl.get(srcNode).or(tempDstIp);
              }else{
                dstDeniedAcl=dstDeniedAcl.or(tempDstIp.not());
                //_inDstAcl.get(srcNode).or(tempDstIp.not());
              }
            }
            if(srcIp!=null)
            {
              String targetIp=srcIp.substring(srcIp.indexOf("=")+1,srcIp.length()-1);
              BDD tempSrcIp=convertAclToBDD(targetIp);
              if(line.getAction()==LineAction.REJECT)
              {
                srcDeniedAcl=srcDeniedAcl.or(tempSrcIp);
                //_inSrcAcl.get(srcNode).or(tempSrcIp);
              }else{
                srcDeniedAcl=srcDeniedAcl.or(tempSrcIp.not());
                //_inSrcAcl.get(srcNode).or(tempSrcIp.not());
              }
            }
          }
        }
      }
      if(dstInt.getIncomingFilter()!=null)
      {
        List<IpAccessListLine> dstIpAclLine=dstInt.getIncomingFilter().getLines();
        for(IpAccessListLine line:dstIpAclLine)
        {
          if(line.getMatchCondition() instanceof MatchHeaderSpace)
          {
            MatchHeaderSpace matchHeader=(MatchHeaderSpace) line.getMatchCondition();
            HeaderSpace headerSpace=matchHeader.getHeaderspace();
            String dstIp=headerSpace.getDstIps().toString();
            String srcIp=headerSpace.getSrcIps().toString();
            if (dstIp!=null)
            {
              BDD tempDstIp=convertAclToBDD(dstIp);
              if(line.getAction()== LineAction.REJECT)
              {
                dstDeniedAcl=dstDeniedAcl.or(tempDstIp);
               // _inDstAcl.get(srcNode).or(tempDstIp);
              }else{
                dstDeniedAcl=dstDeniedAcl.or(tempDstIp.not());
               // _inDstAcl.get(srcNode).or(tempDstIp.not());
              }
            }
            if(srcIp!=null)
            {
              BDD tempSrcIp=convertAclToBDD(srcIp);
              if(line.getAction()==LineAction.REJECT)
              {
                srcDeniedAcl=srcDeniedAcl.or(tempSrcIp);
               // _inSrcAcl.get(srcNode).or(tempSrcIp);
              }else{
                srcDeniedAcl=srcDeniedAcl.or(tempSrcIp.not());
               // _inSrcAcl.get(srcNode).or(tempSrcIp.not());
              }
            }
          }
        }
      }
      _inDstAcl.put(srcNode,dstDeniedAcl);
      _inSrcAcl.put(srcNode,srcDeniedAcl);
    }
  }

  public BDD convertAclToBDD(String Ip)
  {
    BDD prefixActualIp=_prefixFactory.one();
    if(Ip.equals("0.0.0.0/0"))
    {
      return prefixActualIp;
    }
    int prefixIpLen=Integer.parseInt(Ip.substring(Ip.indexOf("/")+1,Ip.length()));
    String prefixIp[]=Ip.substring(0,Ip.indexOf("/")).split("\\.");
    String targetIp="";
    for(int i=0;i<prefixIp.length;i++)
    {
      Integer subIp=Integer.parseInt(prefixIp[i]);
      targetIp=targetIp+String.format("%8s",Integer.toBinaryString(subIp)).replace(" ","0");
    }
    for(int i=0;i<prefixIpLen;i++)
    {
      if(targetIp.charAt(i)=='1')
      {
        prefixActualIp = prefixActualIp.and( _prefixFactory.ithVar(i) );
      }else{
        prefixActualIp = prefixActualIp.and( _prefixFactory.nithVar(i) );
      }
    }
    return prefixActualIp;
  }

  public void setPrefixFactory(BDDFactory prefixFactory)
  {
    _prefixFactory=prefixFactory;
  }

  public void setBddFactory(BDDFactory bddFactory)
  {
    _bddFactory=bddFactory;
  }

  //对应拓扑边编码
  public void setEdgeEncode(HashMap<String,Integer> edgeEncode)
  {
    _edgeEncode.putAll(edgeEncode);
  }

  public HashMap<String,Integer> getEdgeEncode()
  {
    return _edgeEncode;
  }

  /**
   * Initializes helper data structures and easy-to-compute RIBs that are not affected by BDP
   * iterations (e.g., static route RIB, connected route RIB, etc.)
   */
  @VisibleForTesting
  void initForIgpComputation() {
    _mainRibRouteDeltaBuiler = new RibDelta.Builder<>(_mainRib);
    initConnectedRib();
    initStaticRib();
    importRib(_independentRib, _connectedRib);
    importRib(_independentRib, _staticInterfaceRib);
    importRib(_mainRib, _independentRib);
  }

  //计算之前的初始化
//  void initForComputation()
//  {
//    initSymBgpRib();
//  }
  /**
   * Prep for the Egp part of the computation
   *
   * @param externalAdverts a set of external BGP advertisements
   * @param allNodes map of all network nodes, keyed by hostname
   * @param bgpTopology the bgp peering relationships
   */
  void initForEgpComputation(
      Set<BgpAdvertisement> externalAdverts,
      final Map<String, Node> allNodes,
      Topology topology,
      Network<BgpNeighbor, BgpSession> bgpTopology) {
    //同样是初始化,只不过没有考虑外部路由宣告
    initQueuesAndDeltaBuilders(allNodes, topology, bgpTopology);
  }



  void symInitBgpQueuesAndDeltaBuilders(final Map<String,Node> allNodes,Topology topology,Network<BgpNeighbor,BgpSession> bgpTopology)
  {
    if (_vrf.getBgpProcess() == null) {
      _symBgpIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _vrf.getBgpProcess()
          .getNeighbors()
          .values()
          .forEach(
              n -> {
                if (bgpTopology.nodes().contains(n) && !bgpTopology.adjacentNodes(n).isEmpty()) {
                  //给来自于这个邻居的BGP路由消息创建一个消息队列
                  for (BgpSession session : bgpTopology.incidentEdges(n)) {
                    //如果消息队列为空,则创建一个
                    _symBgpIncomingRoutes.computeIfAbsent(
                        UndirectedBgpSession.from(session), s -> new ConcurrentLinkedQueue<>());
                  }
                } else {
                  _externalRouteList.put(n, new HashSet<>());
                }
              });
    }
  }

  /**
   * Initializes RIB delta builders and protocol message queues.
   *
   * @param allNodes map of all network nodes, keyed by hostname
   * @param topology Layer 3 network topology
   * @param bgpTopology the bgp peering relationships
   */
  @VisibleForTesting
  void initQueuesAndDeltaBuilders(
      final Map<String, Node> allNodes,
      final Topology topology,
      Network<BgpNeighbor, BgpSession> bgpTopology) {

    // Initialize message queues for each BGP neighbor
    // 为每个BGP邻居初始化消息队列
    initBgpQueues(bgpTopology);

    // Initialize message queues for each Ospf
    // 为每个OSPF邻居初始化消息队列
    if (_vrf.getOspfProcess() == null) {
      _ospfExternalIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _ospfNeighbors = getOspfNeighbors(allNodes, topology);
      if (_ospfNeighbors == null) {
        _ospfExternalIncomingRoutes = ImmutableSortedMap.of();
      } else {
        _ospfExternalIncomingRoutes =
            _ospfNeighbors
                .keySet()
                .stream()
                .collect(
                    ImmutableSortedMap.toImmutableSortedMap(
                        Prefix::compareTo,
                        Function.identity(),
                        p -> new ConcurrentLinkedQueue<>()));
      }
    }
  }

  void initBgpQueues(Network<BgpNeighbor, BgpSession> bgpTopology) {
    if (_vrf.getBgpProcess() == null) {
      _bgpIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _vrf.getBgpProcess()
          .getNeighbors()
          .values()
          .forEach(
              n -> {
                if (bgpTopology.nodes().contains(n)) {
                  //给来自于这个邻居的BGP路由消息创建一个消息队列
                  for (BgpSession session : bgpTopology.incidentEdges(n)) {
                    //如果消息队列为空,则创建一个
                    _bgpIncomingRoutes.computeIfAbsent(
                        UndirectedBgpSession.from(session), s -> new ConcurrentLinkedQueue<>());
                  }
                }
              });
    }
  }

  /**
   * Activate generated routes.
   *
   * @return a new {@link RibDelta} if a new route has been activated, otherwise {@code null}
   */
  @VisibleForTesting
  RibDelta<AbstractRoute> activateGeneratedRoutes() {
    RibDelta.Builder<AbstractRoute> builder = new Builder<>(_generatedRib);

    // Loop over all generated routes and check whether any of the contributing routes have changed
    for (GeneratedRoute gr : _vrf.getGeneratedRoutes()) {
      boolean active = true;
      String generationPolicyName = gr.getGenerationPolicy();
      GeneratedRoute.Builder grb = new GeneratedRoute.Builder();
      grb.setNetwork(gr.getNetwork());
      grb.setAdmin(gr.getAdministrativeCost());
      grb.setMetric(gr.getMetric() != null ? gr.getMetric() : 0);
      grb.setAttributePolicy(gr.getAttributePolicy());
      grb.setGenerationPolicy(gr.getGenerationPolicy());
      grb.setTopologyCondition(gr.getTopologyCondition());//对应拓扑条件
      boolean discard = gr.getDiscard();
      grb.setDiscard(discard);
      if (discard) {
        grb.setNextHopInterface(Interface.NULL_INTERFACE_NAME);
      }
      if (generationPolicyName != null) {
        RoutingPolicy generationPolicy = _c.getRoutingPolicies().get(generationPolicyName);
        if (generationPolicy != null) {
          active = false;
          for (AbstractRoute contributingCandidate : _mainRib.getRoutes()) {
            boolean accept =
                generationPolicy.process(contributingCandidate, grb, null, _key, Direction.OUT);
            if (accept) {
              if (!discard) {
                grb.setNextHopIp(contributingCandidate.getNextHopIp());
              }
              active = true;
              break;
            }
          }
        }
      }
      if (active) {
        GeneratedRoute newGr = grb.build();
        // Routes have been changed
        RibDelta<AbstractRoute> d = _generatedRib.mergeRouteGetDelta(newGr);
        builder.from(d);
      }
    }
    return builder.build();
  }

  /**
   * Recompute generated routes. If new generated routes were activated, process them into the main
   * RIB. Check if any BGP aggregates were affected by the new generated routes.
   */
  void recomputeGeneratedRoutes() {
    RibDelta<AbstractRoute> d;
    RibDelta.Builder<AbstractRoute> generatedRouteDeltaBuilder = new Builder<>(_mainRib);
    do {
      d = activateGeneratedRoutes();
      generatedRouteDeltaBuilder.from(d);
    } while (d != null);

    d = generatedRouteDeltaBuilder.build();
    // Update main rib as well
    _mainRibRouteDeltaBuiler.from(importRibDelta(_mainRib, d));

    /*
     * Check dependencies for BGP aggregates.
     *
     * Updates from these BGP deltas into mainRib will be handled in finalizeBgp routes
     */
    if (d != null) {
      d.getActions()
          .stream()
          .filter(RouteAdvertisement::isWithdrawn)
          .forEach(
              r -> {
                _bgpBestPathDeltaBuilder.from(
                    _bgpAggDeps.deleteRoute(r.getRoute(), _bgpBestPathRib));
                _bgpMultiPathDeltaBuilder.from(
                    _bgpAggDeps.deleteRoute(r.getRoute(), _bgpMultipathRib));
              });
    }
  }

  /**
   * Re-activate static routes at the beginning of an iteration. Directly adds a static route R to
   * the main RIB if there exists an active route to the R's next-hop-ip.
   */
  void activateStaticRoutes() {
    for (StaticRoute sr : _staticRib.getRoutes()) {
      // See if we have any routes matching this route's next hop IP
      Set<AbstractRoute> matchingRoutes = _mainRib.longestPrefixMatch(sr.getNextHopIp());
      Prefix staticRoutePrefix = sr.getNetwork();

      for (AbstractRoute matchingRoute : matchingRoutes) {
        Prefix matchingRoutePrefix = matchingRoute.getNetwork();
        /*
         * check to make sure matching route's prefix does not totally
         * contain this static route's prefix
         */
        if (!matchingRoutePrefix.containsPrefix(staticRoutePrefix)) {
          _mainRibRouteDeltaBuiler.from(_mainRib.mergeRouteGetDelta(sr));
          break; // break out of the inner loop but continue processing static routes
        }
      }
    }
  }

  /** Compute the FIB from the main RIB */
  public void computeFib() {
    _fib = new FibImpl(_mainRib);
  }

  /**
   * Decides whether the current OSPF summary route metric needs to be changed based on the given
   * route's metric.
   *
   * <p>Routes from the same area or outside of areaPrefix have no effect on the summary metric.
   *
   * @param route The route in question, whose metric is considered
   * @param areaPrefix The Ip prefix of the OSPF area
   * @param currentMetric The current summary metric for the area
   * @param areaNum Area number.
   * @param useMin Whether to use the older RFC 1583 computation, which takes the minimum of metrics
   *     as opposed to the newer RFC 2328, which uses the maximum
   * @return the newly computed summary metric.
   */
  @Nullable
  static Long computeUpdatedOspfSummaryMetric(
      OspfInternalRoute route,
      Prefix areaPrefix,
      @Nullable Long currentMetric,
      long areaNum,
      boolean useMin) {
    Prefix contributingRoutePrefix = route.getNetwork();
    // Only update metric for different areas and if the area prefix contains the route prefix
    if (areaNum == route.getArea() || !areaPrefix.containsPrefix(contributingRoutePrefix)) {
      return currentMetric;
    }
    long contributingRouteMetric = route.getMetric();
    // Definitely update if we have no previous metric
    if (currentMetric == null) {
      return contributingRouteMetric;
    }
    // Take the best metric between the route's and current available.
    // Routers (at least Cisco and Juniper) default to min metric unless using RFC2328 with
    // RFC1583 compatibility explicitly disabled, in which case they default to max.
    if (useMin) {
      return Math.min(currentMetric, contributingRouteMetric);
    }
    return Math.max(currentMetric, contributingRouteMetric);
  }

  boolean computeInterAreaSummaries() {
    OspfProcess proc = _vrf.getOspfProcess();//判断有没有运行OSPF协议
    boolean changed = false;
    // Ensure we have a running OSPF process on the VRF, otherwise bail.
    if (proc == null) {
      return false;
    }
    // Admin cost for the given protocol
    int admin = RoutingProtocol.OSPF_IA.getSummaryAdministrativeCost(_c.getConfigurationFormat());
    // Determine whether to use min metric by default, based on RFC1583 compatibility setting.
    // Routers (at least Cisco and Juniper) default to min metric unless using RFC2328 with
    // RFC1583 compatibility explicitly disabled, in which case they default to max.
    boolean useMin = firstNonNull(proc.getRfc1583Compatible(), true);

    // Compute summaries for each area
    for (Entry<Long, OspfArea> e : proc.getAreas().entrySet()) {
      long areaNum = e.getKey();
      OspfArea area = e.getValue();
      for (Entry<Prefix, OspfAreaSummary> e2 : area.getSummaries().entrySet()) {
        Prefix prefix = e2.getKey();
        OspfAreaSummary summary = e2.getValue();

        // Only advertised summaries can contribute
        if (!summary.getAdvertised()) {
          continue;
        }

        Long metric = summary.getMetric();
        if (summary.getMetric() == null) {
          // No metric was configured; compute it from any possible contributing routes.
          for (OspfIntraAreaRoute contributingRoute : _ospfIntraAreaRib.getRoutes()) {
            metric =
                computeUpdatedOspfSummaryMetric(contributingRoute, prefix, metric, areaNum, useMin);
          }
          for (OspfInterAreaRoute contributingRoute : _ospfInterAreaRib.getRoutes()) {
            metric =
                computeUpdatedOspfSummaryMetric(contributingRoute, prefix, metric, areaNum, useMin);
          }
        }

        // No routes contributed to the summary, nothing to construct
        if (metric == null) {
          continue;
        }

        // Non-null metric means we generate a new summary and put it in the RIB
        OspfInterAreaRoute summaryRoute =
            new OspfInterAreaRoute(prefix, Ip.ZERO, admin, metric, areaNum);
        if (_ospfInterAreaStagingRib.mergeRouteGetDelta(summaryRoute) != null) {
          changed = true;
        }
      }
    }
    return changed;
  }

  /**
   * Initializes BGP RIBs prior to any dataplane iterations based on the external BGP advertisements
   * coming into the network
   *
   * @param externalAdverts a set of external BGP advertisements
   * @param ipOwners mapping of IPs to their owners in our network
   * @param bgpTopology the bgp peering relationships
   */
  // 初始化路由表,主要是考虑外部路由宣告的情况
  void initBaseBgpRibs(
      Set<BgpAdvertisement> externalAdverts,
      Map<Ip, Set<String>> ipOwners,
      final Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology) {

    //判断是否运行BGP协议
    BgpProcess proc = _vrf.getBgpProcess();
    if (proc == null) {
      // Nothing to do
      return;
    }

    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
    // RibDelta主要记录对RIB表的操作为action
    Map<BgpMultipathRib, RibDelta.Builder<BgpRoute>> ribDeltas = new IdentityHashMap<>();
    ribDeltas.put(_ebgpStagingRib, new Builder<>(_ebgpStagingRib));
    ribDeltas.put(_ibgpStagingRib, new Builder<>(_ibgpStagingRib));

    // initialize admin costs for routes
    int ebgpAdmin = RoutingProtocol.BGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int ibgpAdmin = RoutingProtocol.IBGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());

    // Process each BGP advertisement
    // 处理每个外部的路由宣告
    for (BgpAdvertisement advert : externalAdverts) {

      // If it is not for us, ignore it
      if (!advert.getDstNode().equals(_c.getHostname())) {
        continue;
      }

      // If we don't own the IP for this advertisement, ignore it
      // 判断这个路由宣告是不是发给该节点的
      Ip dstIp = advert.getDstIp();
      Set<String> dstIpOwners = ipOwners.get(dstIp);
      String hostname = _c.getHostname();
      if (dstIpOwners == null || !dstIpOwners.contains(hostname)) {
        continue;
      }

      Ip srcIp = advert.getSrcIp();
      // 如果是发给自己的,则看与发送的AS是不是邻居关系
      // TODO: support passive bgp connections
      Prefix srcPrefix = new Prefix(srcIp, Prefix.MAX_PREFIX_LENGTH);
      BgpNeighbor neighbor = _vrf.getBgpProcess().getNeighbors().get(srcPrefix);
      if (neighbor == null) {
        continue;
      }

      // Build a route based on the type of this advertisement
      // 如果是邻居关系则为这个advertisement增加一个路由,下述过程为设置BGP路由的类型
      BgpAdvertisementType type = advert.getType();
      BgpRoute.Builder outgoingRouteBuilder = new BgpRoute.Builder();
      boolean ebgp;
      boolean received;
      switch (type) {
        case EBGP_RECEIVED:
          ebgp = true;
          received = true;
          break;

        case EBGP_SENT:
          ebgp = true;
          received = false;
          break;

        case IBGP_RECEIVED:
          ebgp = false;
          received = true;
          break;

        case IBGP_SENT:
          ebgp = false;
          received = false;
          break;

        case EBGP_ORIGINATED:
        case IBGP_ORIGINATED:
        default:
          throw new BatfishException("Missing or invalid bgp advertisement type");
      }
      //判断路由消息要操作的路由表已经相应的协议是什么?
      BgpMultipathRib targetRib = ebgp ? _ebgpStagingRib : _ibgpStagingRib;
      RoutingProtocol targetProtocol = ebgp ? RoutingProtocol.BGP : RoutingProtocol.IBGP;

      if (received) {
        int admin = ebgp ? ebgpAdmin : ibgpAdmin;
        AsPath asPath = advert.getAsPath();

        BDD topologyCondition = advert.get_topologyCondition();//对应拓扑条件

        SortedSet<Long> clusterList = advert.getClusterList();
        SortedSet<Long> communities = ImmutableSortedSet.copyOf(advert.getCommunities());
        int localPreference = advert.getLocalPreference();
        long metric = advert.getMed();
        Prefix network = advert.getNetwork();
        Ip nextHopIp = advert.getNextHopIp();
        Ip originatorIp = advert.getOriginatorIp();
        OriginType originType = advert.getOriginType();
        RoutingProtocol srcProtocol = advert.getSrcProtocol();
        int weight = advert.getWeight();
        BgpRoute.Builder builder = new BgpRoute.Builder();
        builder.setAdmin(admin);
        builder.setAsPath(asPath.getAsSets());
        builder.setClusterList(clusterList);
        builder.setCommunities(communities);
        builder.setLocalPreference(localPreference);
        builder.setMetric(metric);
        builder.setNetwork(network);
        builder.setNextHopIp(nextHopIp);
        builder.setOriginatorIp(originatorIp);
        builder.setOriginType(originType);
        builder.setProtocol(targetProtocol);

        builder.setTopologyCondition(topologyCondition);//对应拓扑条件

        // TODO: support external route reflector clients
        builder.setReceivedFromIp(advert.getSrcIp());
        builder.setReceivedFromRouteReflectorClient(false);
        builder.setSrcProtocol(srcProtocol);
        // TODO: possibly support setting tag
        builder.setWeight(weight);
        BgpRoute route = builder.build();
        ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(route));// 接受的路由加到路由表当中去
      } else {
        int localPreference;
        if (ebgp) {
          localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
        } else {
          localPreference = advert.getLocalPreference();
        }
        outgoingRouteBuilder.setAsPath(advert.getAsPath().getAsSets());
        outgoingRouteBuilder.setCommunities(ImmutableSortedSet.copyOf(advert.getCommunities()));
        outgoingRouteBuilder.setLocalPreference(localPreference);
        outgoingRouteBuilder.setMetric(advert.getMed());
        outgoingRouteBuilder.setNetwork(advert.getNetwork());
        outgoingRouteBuilder.setNextHopIp(advert.getNextHopIp());
        outgoingRouteBuilder.setOriginatorIp(advert.getOriginatorIp());
        outgoingRouteBuilder.setOriginType(advert.getOriginType());
        outgoingRouteBuilder.setProtocol(targetProtocol);
        outgoingRouteBuilder.setReceivedFromIp(advert.getSrcIp());

        outgoingRouteBuilder.setTopologyCondition(advert.get_topologyCondition());//对应拓扑条件
        // TODO:
        // outgoingRouteBuilder.setReceivedFromRouteReflectorClient(...);
        outgoingRouteBuilder.setSrcProtocol(advert.getSrcProtocol());
        BgpRoute transformedOutgoingRoute = outgoingRouteBuilder.build();
        BgpRoute.Builder transformedIncomingRouteBuilder = new BgpRoute.Builder();

        // Incoming originatorIp
        transformedIncomingRouteBuilder.setOriginatorIp(transformedOutgoingRoute.getOriginatorIp());

        // Incoming receivedFromIp
        transformedIncomingRouteBuilder.setReceivedFromIp(
            transformedOutgoingRoute.getReceivedFromIp());

        // Incoming clusterList
        transformedIncomingRouteBuilder
            .getClusterList()
            .addAll(transformedOutgoingRoute.getClusterList());

        // Incoming receivedFromRouteReflectorClient
        transformedIncomingRouteBuilder.setReceivedFromRouteReflectorClient(
            transformedOutgoingRoute.getReceivedFromRouteReflectorClient());

        // Incoming asPath
        transformedIncomingRouteBuilder.setAsPath(transformedOutgoingRoute.getAsPath().getAsSets());

        // Incoming communities
        transformedIncomingRouteBuilder
            .getCommunities()
            .addAll(transformedOutgoingRoute.getCommunities());

        // Incoming protocol
        transformedIncomingRouteBuilder.setProtocol(targetProtocol);

        // Incoming network
        transformedIncomingRouteBuilder.setNetwork(transformedOutgoingRoute.getNetwork());

        // Incoming nextHopIp
        transformedIncomingRouteBuilder.setNextHopIp(transformedOutgoingRoute.getNextHopIp());

        // Incoming originType
        transformedIncomingRouteBuilder.setOriginType(transformedOutgoingRoute.getOriginType());

        // Incoming localPreference
        transformedIncomingRouteBuilder.setLocalPreference(
            transformedOutgoingRoute.getLocalPreference());

        // Incoming admin
        int admin = ebgp ? ebgpAdmin : ibgpAdmin;
        transformedIncomingRouteBuilder.setAdmin(admin);

        // Incoming metric
        transformedIncomingRouteBuilder.setMetric(transformedOutgoingRoute.getMetric());

        // Incoming srcProtocol
        transformedIncomingRouteBuilder.setSrcProtocol(targetProtocol);
        String importPolicyName = neighbor.getImportPolicy();

        transformedIncomingRouteBuilder.setTopologyCondition(transformedOutgoingRoute.getTopologyCondition());//对应拓扑条件
        // TODO: ensure there is always an import policy

        if (ebgp
            && transformedOutgoingRoute.getAsPath().containsAs(neighbor.getLocalAs())
            && !neighbor.getAllowLocalAsIn()) {
          // skip routes containing peer's AS unless
          // disable-peer-as-check (getAllowRemoteAsOut) is set
          continue;
        }

        /*
         * CREATE INCOMING ROUTE
         */
        // 判断有没有对应入方向的路由策略,如果有则进行判断
        boolean acceptIncoming = true;
        if (importPolicyName != null) {
          RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
          if (importPolicy != null) {
            acceptIncoming =
                importPolicy.process(
                    transformedOutgoingRoute,
                    transformedIncomingRouteBuilder,
                    advert.getSrcIp(),
                    _key,
                    Direction.IN);
          }
        }
        if (acceptIncoming) {
          BgpRoute transformedIncomingRoute = transformedIncomingRouteBuilder.build();
          ribDeltas.get(targetRib).from(targetRib.mergeRouteGetDelta(transformedIncomingRoute));
        }
      }
    }

    // Propagate received routes through all the RIBs and send out appropriate messages
    // to neighbors'
    // 向邻居发送这个路由
    // 每个节点,真正的路由传播函数,收集路由并向外传播,同时更新自己的路由表
    Map<BgpMultipathRib, RibDelta<BgpRoute>> deltas =
        ribDeltas
            .entrySet()
            .stream()
            .filter(e -> e.getValue().build() != null)
            .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().build()));

    finalizeBgpRoutesAndQueueOutgoingMessages(
        proc.getMultipathEbgp(), proc.getMultipathIbgp(), deltas, allNodes, bgpTopology);
  }

  /** Initialize Intra-area OSPF routes from the interface prefixes */
  private void initIntraAreaOspfRoutes() {
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return; // nothing to do
    }
    /*
     * init intra-area routes from connected routes
     * For each interface within an OSPF area and each interface prefix,
     * construct a new OSPF-IA route. Put it in the IA RIB.
     */
    proc.getAreas()
        .forEach(
            (areaNum, area) -> {
              for (String ifaceName : area.getInterfaces()) {
                Interface iface = _c.getInterfaces().get(ifaceName);
                if (iface.getActive()) {
                  Set<Prefix> allNetworkPrefixes =
                      iface
                          .getAllAddresses()
                          .stream()
                          .map(InterfaceAddress::getPrefix)
                          .collect(Collectors.toSet());
                  int interfaceOspfCost = iface.getOspfCost();
                  for (Prefix prefix : allNetworkPrefixes) {
                    long cost = interfaceOspfCost;
                    boolean stubNetwork = iface.getOspfPassive() || iface.getOspfPointToPoint();
                    if (stubNetwork) {
                      if (proc.getMaxMetricStubNetworks() != null) {
                        cost = proc.getMaxMetricStubNetworks();
                      }
                    } else if (proc.getMaxMetricTransitLinks() != null) {
                      cost = proc.getMaxMetricTransitLinks();
                    }
                    OspfIntraAreaRoute route =
                        new OspfIntraAreaRoute(
                            prefix,
                            null,
                            RoutingProtocol.OSPF.getDefaultAdministrativeCost(
                                _c.getConfigurationFormat()),
                            cost,
                            areaNum);
                    _ospfIntraAreaRib.mergeRouteGetDelta(route);
                  }
                }
              }
            });
  }

  /** Initialize RIP routes from the interface prefixes */
  @VisibleForTesting
  void initBaseRipRoutes() {
    if (_vrf.getRipProcess() == null) {
      return; // nothing to do
    }

    // init internal routes from connected routes
    for (String ifaceName : _vrf.getRipProcess().getInterfaces()) {
      Interface iface = _vrf.getInterfaces().get(ifaceName);
      if (iface.getActive()) {
        Set<Prefix> allNetworkPrefixes =
            iface
                .getAllAddresses()
                .stream()
                .map(InterfaceAddress::getPrefix)
                .collect(Collectors.toSet());
        long cost = RipProcess.DEFAULT_RIP_COST;
        for (Prefix prefix : allNetworkPrefixes) {
          RipInternalRoute route =
              new RipInternalRoute(
                  prefix,
                  null,
                  RoutingProtocol.RIP.getDefaultAdministrativeCost(_c.getConfigurationFormat()),
                  cost);
          _ripInternalRib.mergeRouteGetDelta(route);
        }
      }
    }
  }

  /**
   * This function creates BGP routes from generated routes that go into the BGP RIB, but cannot be
   * imported into the main RIB. The purpose of these routes is to prevent the local router from
   * accepting advertisements less desirable than the local generated ones for the given network.
   */
  void initBgpAggregateRoutes() {
    // first import aggregates
    switch (_c.getConfigurationFormat()) {
      case JUNIPER:
      case JUNIPER_SWITCH:
        return;
        // $CASES-OMITTED$
      default:
        break;
    }
    for (AbstractRoute grAbstract : _generatedRib.getRoutes()) {
      GeneratedRoute gr = (GeneratedRoute) grAbstract;
      BgpRoute.Builder b = new BgpRoute.Builder();
      b.setAdmin(gr.getAdministrativeCost());
      b.setAsPath(gr.getAsPath().getAsSets());
      b.setMetric(gr.getMetric());
      b.setSrcProtocol(RoutingProtocol.AGGREGATE);
      b.setProtocol(RoutingProtocol.AGGREGATE);
      b.setNetwork(gr.getNetwork());
      b.setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE);
      b.setTopologyCondition(gr.getTopologyCondition());
      /* Note: Origin type and originator IP should get overwritten, but are needed initially */
      b.setOriginatorIp(_vrf.getBgpProcess().getRouterId());
      b.setOriginType(OriginType.INCOMPLETE);
      b.setReceivedFromIp(Ip.ZERO);
      BgpRoute br = b.build();

      /*
       * TODO: tests for this
       * 1. Really hope setNonRouting(true) prevents this from being in the main RIB
       * 2. General functionality of aggregate routes is not well tested
       */
      br.setNonRouting(true);
      RibDelta<BgpRoute> d1 = _bgpMultipathRib.mergeRouteGetDelta(br);
      _bgpBestPathDeltaBuilder.from(d1);
      RibDelta<BgpRoute> d2 = _bgpBestPathRib.mergeRouteGetDelta(br);
      _bgpMultiPathDeltaBuilder.from(d2);
      if (d1 != null || d2 != null) {
        _bgpAggDeps.addRouteDependency(br, gr);
      }
    }
  }

  /**
   * Initialize the connected RIB -- a RIB containing connected routes (i.e., direct connections to
   * neighbors).
   */
  @VisibleForTesting
  void initConnectedRib() {
    // Look at all connected interfaces
    _connectPrefix=new HashSet<>();
    for (Interface i : _vrf.getInterfaces().values()) {
      if (i.getActive()) { // Make sure the interface is active
        // Create a route for each interface prefix
        for (InterfaceAddress ifaceAddress : i.getAllAddresses()) {
          Prefix prefix = ifaceAddress.getPrefix();
          _connectPrefix.add(prefix);
          ConnectedRoute cr = new ConnectedRoute(prefix, i.getName());
          _connectedRib.mergeRoute(cr);
        }
      }
    }
  }

  @Nullable
  @VisibleForTesting
  OspfExternalRoute computeOspfExportRoute(
      AbstractRoute potentialExportRoute, RoutingPolicy exportPolicy, OspfProcess proc) {
    OspfExternalRoute.Builder outputRouteBuilder = new OspfExternalRoute.Builder();
    // Export based on the policy result of processing the potentialExportRoute
    boolean accept =
        exportPolicy.process(potentialExportRoute, outputRouteBuilder, null, _key, Direction.OUT);
    if (!accept) {
      return null;
    }
    OspfMetricType metricType = outputRouteBuilder.getOspfMetricType();
    outputRouteBuilder.setAdmin(
        outputRouteBuilder
            .getOspfMetricType()
            .toRoutingProtocol()
            .getDefaultAdministrativeCost(_c.getConfigurationFormat()));
    outputRouteBuilder.setNetwork(potentialExportRoute.getNetwork());
    Long maxMetricExternalNetworks = proc.getMaxMetricExternalNetworks();
    long costToAdvertiser;
    if (maxMetricExternalNetworks != null) {
      if (metricType == OspfMetricType.E1) {
        outputRouteBuilder.setMetric(maxMetricExternalNetworks);
      }
      costToAdvertiser = maxMetricExternalNetworks;
    } else {
      costToAdvertiser = 0L;
    }
    outputRouteBuilder.setCostToAdvertiser(costToAdvertiser);
    outputRouteBuilder.setAdvertiser(_c.getHostname());
    outputRouteBuilder.setArea(OspfRoute.NO_AREA);
    outputRouteBuilder.setLsaMetric(outputRouteBuilder.getMetric());
    OspfExternalRoute outputRoute = outputRouteBuilder.build();
    outputRoute.setNonRouting(true);
    return outputRoute;
  }

  void initOspfExports() {
    OspfProcess proc = _vrf.getOspfProcess();
    // Nothing to do
    if (proc == null) {
      return;
    }

    // get OSPF export policy name
    String exportPolicyName = _vrf.getOspfProcess().getExportPolicy();
    if (exportPolicyName == null) {
      return; // nothing to export
    }

    RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(exportPolicyName);
    if (exportPolicy == null) {
      return; // nothing to export
    }

    // For each route in the previous RIB, compute an export route and add it to the appropriate
    // RIB.
    RibDelta.Builder<OspfExternalType1Route> d1 = new Builder<>(_ospfExternalType1Rib);
    RibDelta.Builder<OspfExternalType2Route> d2 = new Builder<>(_ospfExternalType2Rib);
    for (AbstractRoute potentialExport : _mainRib.getRoutes()) {
      OspfExternalRoute outputRoute = computeOspfExportRoute(potentialExport, exportPolicy, proc);
      if (outputRoute == null) {
        continue; // no need to export
      }
      if (outputRoute.getOspfMetricType() == OspfMetricType.E1) {
        d1.from(_ospfExternalType1Rib.mergeRouteGetDelta((OspfExternalType1Route) outputRoute));
      } else { // assuming here that MetricType exists. Or E2 is the default
        d2.from(_ospfExternalType2Rib.mergeRouteGetDelta((OspfExternalType2Route) outputRoute));
      }
    }
    queueOutgoingOspfExternalRoutes(d1.build(), d2.build());
  }

  /** Initialize all ribs on this router. All RIBs will be empty */
  @VisibleForTesting
  void initRibs() {
    _connectedRib = new ConnectedRib();
    // If bgp process is null, doesn't matter
    MultipathEquivalentAsPathMatchMode mpTieBreaker =
        _vrf.getBgpProcess() == null
            ? EXACT_PATH
            : _vrf.getBgpProcess().getMultipathEquivalentAsPathMatchMode();
    _ebgpMultipathRib = new BgpMultipathRib(mpTieBreaker);
    _ebgpStagingRib = new BgpMultipathRib(mpTieBreaker);
    _generatedRib = new Rib();
    _ibgpMultipathRib = new BgpMultipathRib(mpTieBreaker);
    _ibgpStagingRib = new BgpMultipathRib(mpTieBreaker);
    _independentRib = new Rib();
    _mainRib = new Rib();
    _ospfExternalType1Rib =
        new OspfExternalType1Rib(getHostname(), _receivedOspExternalType1Routes);
    _ospfExternalType2Rib =
        new OspfExternalType2Rib(getHostname(), _receivedOspExternalType2Routes);
    _ospfExternalType1StagingRib = new OspfExternalType1Rib(getHostname(), null);
    _ospfExternalType2StagingRib = new OspfExternalType2Rib(getHostname(), null);
    _ospfInterAreaRib = new OspfInterAreaRib();
    _ospfInterAreaStagingRib = new OspfInterAreaRib();
    _ospfIntraAreaRib = new OspfIntraAreaRib(this);
    _ospfIntraAreaStagingRib = new OspfIntraAreaRib(this);
    _ospfRib = new OspfRib();
    _ripInternalRib = new RipInternalRib();
    _ripInternalStagingRib = new RipInternalRib();
    _ripRib = new RipRib();
    _staticRib = new StaticRib();
    _staticInterfaceRib = new StaticRib();
    _bgpMultipathRib = new BgpMultipathRib(mpTieBreaker);

    _ebgpMultipathRib = new BgpMultipathRib(mpTieBreaker);
    _ibgpMultipathRib = new BgpMultipathRib(mpTieBreaker);
    BgpTieBreaker tieBreaker =
        _vrf.getBgpProcess() == null
            ? BgpTieBreaker.ARRIVAL_ORDER
            : _vrf.getBgpProcess().getTieBreaker();
    _ebgpBestPathRib = new BgpBestPathRib(tieBreaker, null, _mainRib);
    _ibgpBestPathRib = new BgpBestPathRib(tieBreaker, null, _mainRib);
    _bgpBestPathRib = new BgpBestPathRib(tieBreaker, _receivedBgpRoutes, _mainRib);
    // ISIS
    _isisRib = new IsisRib(isL1Only());
    _isisL1Rib = new IsisLevelRib(true);
    _isisL2Rib = new IsisLevelRib(true);
//    _isisL1Rib=new IsisRib();
//    _isisL2Rib=new IsisRib();
//    _isisRib=new IsisRib();
    _isisL1StagingRib = new IsisLevelRib(false);
    _isisL2StagingRib = new IsisLevelRib(false);

    _isisMultiRib = new IsisMultiRib();
  }

  /** Initialize the static route RIB from the VRF config. */
  void initStaticRib() {
    for (StaticRoute sr : _vrf.getStaticRoutes()) {
      String nextHopInt = sr.getNextHopInterface();
      if (!nextHopInt.equals(Route.UNSET_NEXT_HOP_INTERFACE)
          && !Interface.NULL_INTERFACE_NAME.equals(nextHopInt)
          && (_c.getInterfaces().get(nextHopInt) == null
          || !_c.getInterfaces().get(nextHopInt).getActive())) {
        continue;
      }
      // interface route
      if (sr.getNextHopIp().equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
        _staticInterfaceRib.mergeRouteGetDelta(sr);
      } else {
        _staticRib.mergeRouteGetDelta(sr);
      }
    }
  }

  /**
   * Compute a set of BGP advertisements to the outside of the network. Done after the dataplane
   * computation has converged.
   *
   * @param ipOwners mapping of IPs to their owners (nodes)
   * @return a number of sent out advertisements
   */
  int computeBgpAdvertisementsToOutside(Map<Ip, Set<String>> ipOwners) {
    int numAdvertisements = 0;

    // If we have no BGP process, nothing to do
    if (_vrf.getBgpProcess() == null) {
      return numAdvertisements;
    }

    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      Ip localIp = neighbor.getLocalIp();
      String hostname = _c.getHostname();
      if (localIp == null || Ip.AUTO.equals(localIp) || neighbor.getDynamic()) {
        // Skip neighbors for which cannot reasonably determine localIp
        continue;
      }
      Ip remoteIp = neighbor.getPrefix().getStartIp();
      if (ipOwners.get(remoteIp) != null) {
        // Skip if neighbor is not outside the network
        continue;
      }

      int localAs = neighbor.getLocalAs();
      int remoteAs = neighbor.getRemoteAs();
      String remoteHostname = remoteIp.toString();
      String remoteVrfName = _vrf.getName();
      RoutingPolicy exportPolicy = _c.getRoutingPolicies().get(neighbor.getExportPolicy());
      boolean ebgpSession = localAs != remoteAs;
      RoutingProtocol targetProtocol = ebgpSession ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
      Set<AbstractRoute> candidateRoutes = Collections.newSetFromMap(new IdentityHashMap<>());

      // Add IGP routes
      Set<AbstractRoute> activeRoutes = Collections.newSetFromMap(new IdentityHashMap<>());
      activeRoutes.addAll(_mainRib.getRoutes());
      for (AbstractRoute candidateRoute : activeRoutes) {
        if (candidateRoute.getProtocol() != RoutingProtocol.BGP
            && candidateRoute.getProtocol() != RoutingProtocol.IBGP) {
          candidateRoutes.add(candidateRoute);
        }
      }

      /*
       * bgp advertise-external
       *
       * When this is set, add best eBGP path independently of whether
       * it is preempted by an iBGP or IGP route. Only applicable to
       * iBGP sessions.
       */
      boolean advertiseExternal = !ebgpSession && neighbor.getAdvertiseExternal();
      if (advertiseExternal) {
        candidateRoutes.addAll(_ebgpBestPathRib.getRoutes());
      }

      /*
       * bgp advertise-inactive
       *
       * When this is set, add best BGP path independently of whether
       * it is preempted by an IGP route. Only applicable to eBGP
       * sessions.
       */
      boolean advertiseInactive = ebgpSession && neighbor.getAdvertiseInactive();
      /* Add best bgp paths if they are active, or if advertise-inactive */
      for (AbstractRoute candidateRoute : _bgpBestPathRib.getRoutes()) {
        if (advertiseInactive || activeRoutes.contains(candidateRoute)) {
          candidateRoutes.add(candidateRoute);
        }
      }

      /* Add all bgp paths if additional-paths active for this session */
      boolean additionalPaths =
          !ebgpSession
              && neighbor.getAdditionalPathsSend()
              && neighbor.getAdditionalPathsSelectAll();
      if (additionalPaths) {
        candidateRoutes.addAll(_bgpMultipathRib.getRoutes());
      }
      for (AbstractRoute route : candidateRoutes) {
        BgpRoute.Builder transformedOutgoingRouteBuilder = new BgpRoute.Builder();
        RoutingProtocol routeProtocol = route.getProtocol();
        boolean routeIsBgp =
            routeProtocol == RoutingProtocol.IBGP || routeProtocol == RoutingProtocol.BGP;

        // originatorIP
        Ip originatorIp;
        if (!ebgpSession && routeProtocol == RoutingProtocol.IBGP) {
          BgpRoute bgpRoute = (BgpRoute) route;
          originatorIp = bgpRoute.getOriginatorIp();
        } else {
          originatorIp = _vrf.getBgpProcess().getRouterId();
        }
        transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);
        transformedOutgoingRouteBuilder.setReceivedFromIp(neighbor.getLocalIp());

        /*
         * clusterList, receivedFromRouteReflectorClient, (originType for bgp remote route)
         */
        if (routeIsBgp) {
          BgpRoute bgpRoute = (BgpRoute) route;
          transformedOutgoingRouteBuilder.setOriginType(bgpRoute.getOriginType());
          if (ebgpSession
              && bgpRoute.getAsPath().containsAs(neighbor.getRemoteAs())
              && !neighbor.getAllowRemoteAsOut()) {
            // skip routes containing peer's AS unless
            // disable-peer-as-check (getAllowRemoteAsOut) is set
            continue;
          }
          /*
           * route reflection: reflect everything received from
           * clients to clients and non-clients. reflect everything
           * received from non-clients to clients. Do not reflect to
           * originator
           */

          Ip routeOriginatorIp = bgpRoute.getOriginatorIp();
          /*
           *  iBGP speaker should not send out routes to iBGP neighbor whose router-id is
           *  same as originator id of advertisement
           */
          if (!ebgpSession && remoteIp.equals(routeOriginatorIp)) {
            continue;
          }
          if (routeProtocol == RoutingProtocol.IBGP && !ebgpSession) {
            boolean routeReceivedFromRouteReflectorClient =
                bgpRoute.getReceivedFromRouteReflectorClient();
            boolean sendingToRouteReflectorClient = neighbor.getRouteReflectorClient();
            transformedOutgoingRouteBuilder.getClusterList().addAll(bgpRoute.getClusterList());
            if (!routeReceivedFromRouteReflectorClient && !sendingToRouteReflectorClient) {
              continue;
            }
            if (sendingToRouteReflectorClient) {
              // sender adds its local cluster id to clusterlist of
              // new route
              transformedOutgoingRouteBuilder.getClusterList().add(neighbor.getClusterId());
            }
          }
        }

        // Outgoing asPath
        // Outgoing communities
        if (routeIsBgp) {
          BgpRoute bgpRoute = (BgpRoute) route;
          transformedOutgoingRouteBuilder.setAsPath(bgpRoute.getAsPath().getAsSets());
          if (neighbor.getSendCommunity()) {
            transformedOutgoingRouteBuilder.getCommunities().addAll(bgpRoute.getCommunities());
          }
        }
        if (ebgpSession) {
          SortedSet<Integer> newAsPathElement = new TreeSet<>();
          newAsPathElement.add(localAs);
          transformedOutgoingRouteBuilder.getAsPath().add(0, newAsPathElement);
        }

        // Outgoing protocol
        transformedOutgoingRouteBuilder.setProtocol(targetProtocol);
        transformedOutgoingRouteBuilder.setNetwork(route.getNetwork());

        // Outgoing metric
        if (routeIsBgp) {
          transformedOutgoingRouteBuilder.setMetric(route.getMetric());
        }

        // Outgoing nextHopIp
        // Outgoing localPreference
        Ip nextHopIp;
        int localPreference;
        if (ebgpSession || !routeIsBgp) {
          nextHopIp = neighbor.getLocalIp();
          localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
        } else {
          nextHopIp = route.getNextHopIp();
          BgpRoute ibgpRoute = (BgpRoute) route;
          localPreference = ibgpRoute.getLocalPreference();
        }
        if (nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {
          // should only happen for ibgp
          String nextHopInterface = route.getNextHopInterface();
          InterfaceAddress nextHopAddress = _c.getInterfaces().get(nextHopInterface).getAddress();
          if (nextHopAddress == null) {
            throw new BatfishException("route's nextHopInterface has no address");
          }
          nextHopIp = nextHopAddress.getIp();
        }
        transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
        transformedOutgoingRouteBuilder.setLocalPreference(localPreference);

        // Outgoing srcProtocol
        transformedOutgoingRouteBuilder.setSrcProtocol(route.getProtocol());

        /*
         * CREATE OUTGOING ROUTE
         */
        boolean acceptOutgoing =
            exportPolicy.process(
                route,
                transformedOutgoingRouteBuilder,
                remoteIp,
                neighbor.getPrefix(),
                remoteVrfName,
                Direction.OUT);
        if (acceptOutgoing) {
          BgpRoute transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();
          // Record sent advertisement
          BgpAdvertisementType sentType =
              ebgpSession ? BgpAdvertisementType.EBGP_SENT : BgpAdvertisementType.IBGP_SENT;
          Ip sentOriginatorIp = transformedOutgoingRoute.getOriginatorIp();
          SortedSet<Long> sentClusterList =
              ImmutableSortedSet.copyOf(transformedOutgoingRoute.getClusterList());
          AsPath sentAsPath = transformedOutgoingRoute.getAsPath();
          SortedSet<Long> sentCommunities =
              ImmutableSortedSet.copyOf(transformedOutgoingRoute.getCommunities());
          Prefix sentNetwork = route.getNetwork();
          Ip sentNextHopIp;
          String sentSrcNode = hostname;
          String sentSrcVrf = _vrf.getName();
          Ip sentSrcIp = neighbor.getLocalIp();
          String sentDstNode = remoteHostname;
          String sentDstVrf = remoteVrfName;
          Ip sentDstIp = remoteIp;
          int sentWeight = -1;
          if (ebgpSession) {
            sentNextHopIp = nextHopIp;
          } else {
            sentNextHopIp = transformedOutgoingRoute.getNextHopIp();
          }
          int sentLocalPreference = transformedOutgoingRoute.getLocalPreference();
          long sentMed = transformedOutgoingRoute.getMetric();
          OriginType sentOriginType = transformedOutgoingRoute.getOriginType();
          RoutingProtocol sentSrcProtocol = targetProtocol;
          BgpAdvertisement sentAdvert =
              new BgpAdvertisement(
                  sentType,
                  sentNetwork,
                  sentNextHopIp,
                  sentSrcNode,
                  sentSrcVrf,
                  sentSrcIp,
                  sentDstNode,
                  sentDstVrf,
                  sentDstIp,
                  sentSrcProtocol,
                  sentOriginType,
                  sentLocalPreference,
                  sentMed,
                  sentOriginatorIp,
                  sentAsPath,
                  ImmutableSortedSet.copyOf(sentCommunities),
                  ImmutableSortedSet.copyOf(sentClusterList),
                  sentWeight,
                  null);//对应路由宣告,需要更改的地
          _sentBgpAdvertisements.add(sentAdvert);
          numAdvertisements++;
        }
      }
    }
    return numAdvertisements;
  }
  public void processIsisMessage(Map<Ip,Set<String>> ipOwners,Network<BgpNeighbor,BgpSession> bgpTopology,IsisTopology isisTopology,HashMap<String,BgpSession> nodeNamedBgpTopology,SortedMap<String, Map<Integer,List<SymBgpRoute>>> denyList)
  {
//    if(_vrf.getIsisProcess()==null)
//    {
//      _isisIncomingRoutes=null;
//      return ;
//    }
//    _isisIncomingRoutes.forEach(
//        (edge, queue) -> {
////          Ip nextHopIp = edge.getNode1().getInterface(configurations).getAddress().getIp();
//          //wai bian de shi shi ji lian jie de bian , li mian de shi bgp lin ju gou cheng de bian
////          Interface iface = _c.getInterfaces().get(edge.getNode1().getInterfaceName());
//          String N2name=edge.getNode2().getNode();
//          String N1name=edge.getNode1().getNode();
//          Interface iface = _c.getInterfaces().get(edge.getNode2().getInterfaceName());
//          while (queue.peek() != null) {
//            SymRouteAdvertisement<SymBgpRoute> routeAdvert = queue.remove();
//            Queue<SymBgpRoute> processOutputRoute=new LinkedList<>();
//            processOutputRoute.add(routeAdvert.getRoute());
//            while(!processOutputRoute.isEmpty())
//            {
//              SymBgpRoute remoteRoute = processOutputRoute.poll();
//
//              SymBounder lastBgpLP=remoteRoute.getLocalPreference();
//              SymCommunity lastBgpCommunity=remoteRoute._symCommunities;
//
//              String nodeName = remoteRoute.getNextNode() + "-" + _c.getHostname();//zhe shi pan duan isis lin ju guan xi de jie dian
//              BgpSession bgpEdge = nodeNamedBgpTopology.get(nodeName);
//              String Link_name=nodeName;
//              if (bgpEdge != null) {
//                IsisLevel routeLevel = remoteRoute.getIsisLevel();
//                IsisInterfaceLevelSettings isisLevelSettings =
//                    routeLevel == IsisLevel.LEVEL_1 ? iface.getIsis().getLevel1() : iface.getIsis().getLevel2();
//                //dang jie kou wei passive de shi hou bu chuan bo zhe ge lu you
//                if (isisLevelSettings.getMode() != IsisInterfaceMode.ACTIVE) {
//                  continue;
//                }
//                //mo ren de isis lu you de cost
//                int adminCost = 115;
//                long incrementalMetric = firstNonNull(isisLevelSettings.getCost(), IsisRoute.DEFAULT_METRIC);
//                remoteRoute.setIsisMetric(incrementalMetric + remoteRoute.getIsisMetric());
//
//                //ISIS chu li ji ben wan cheng , xia mian jiu shi yi xie iBGP de chu li le .
//                BgpNeighbor neighbor = bgpEdge.getSrc();
//                Ip localIp = neighbor.getLocalIp();
//                String hostname = _c.getHostname();
//                int remoteAs = neighbor.getRemoteAs();
//
//                // Setup helper vars
//                BgpNeighbor remoteBgpNeighbor = bgpEdge.getDst();
//                Configuration remoteConfig = remoteBgpNeighbor.getOwner();
//                String remoteHostname = remoteConfig.getHostname();
//                String remoteVrfName = remoteBgpNeighbor.getVrf();
//                Vrf remoteVrf = remoteConfig.getVrfs().get(remoteVrfName);
//                //获取这个节点的所有的路由策略,获取对应邻居的路由策略
//                RoutingPolicy remoteExportPolicy = remoteConfig.getRoutingPolicies().get(remoteBgpNeighbor.getExportPolicy());
//
//                String edgeName = edge.getNode1().getNode() + "-" + edge.getNode2().getNode();
//                BDD linkTopology = _bddFactory.ithVar(_edgeEncode.get(edgeName));
//
//                //chu li ju ti de lu you xiao xi
//                // originatorIP
//                Ip originatorIp = remoteRoute.getOriginatorIp();
//                RoutingProtocol remoteEgpRouteProtocol = remoteRoute.getEgpProtocol();
//                RoutingProtocol remoteIgpRouteProtocol=remoteRoute.getIgpProtocol();
//                String nextNode=remoteRoute.getNextNode();
//
//                //起源路由与之前的路由一样
//                SymBgpRoute.Builder transformedOutgoingRouteBuilder = new SymBgpRoute.Builder(
//                    remoteRoute);
//
//                boolean external=remoteRoute.getExternal();
//
//                org.batfish.symwork.Reason reason=routeAdvert._reason;
//
//                transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);
//                transformedOutgoingRouteBuilder.setReceivedFromIp(remoteBgpNeighbor.getLocalIp());//接受到的自治系统的ip就更新为邻居的ip
//                transformedOutgoingRouteBuilder.setReason(org.batfish.symwork.Reason.NORMAL);
//                transformedOutgoingRouteBuilder.setAsPathSize(remoteRoute.getAsPathSize().getLowerbound(),remoteRoute.getAsPathSize().getUpperbound());
//
//                transformedOutgoingRouteBuilder.setOriginType(remoteRoute.getOriginType());
//
//                transformedOutgoingRouteBuilder.setTopologyCondition(remoteRoute.getTopologyCondition().id());
//                remoteRoute.getTopologyCondition().free();
//
//                transformedOutgoingRouteBuilder.setAsPath(new AsPath(remoteRoute.getAsPath().getAsSets()));
//
//                transformedOutgoingRouteBuilder._asNodePath.add(remoteRoute.getNextNode());
//
//                transformedOutgoingRouteBuilder.setNodePath(remoteRoute.getNodePath());
//                transformedOutgoingRouteBuilder.addNodePath(edge.getNode1().getNode());
//
//
//                if (remoteRoute.getNodePath().contains(edge.getNode2().getNode())) {
//                  continue;
//                }
//
//                Ip remoteOriginatorIp = remoteRoute.getOriginatorIp();
//                //iBGP lu you bu chuan bo gei qi de lin ju
//                if (remoteOriginatorIp != null && _vrf.getBgpProcess()
//                    .getRouterId()
//                    .equals(remoteOriginatorIp)) {
//                  continue;
//                }
//
//                //              if (remoteEgpRouteProtocol == RoutingProtocol.IBGP) {
//                //                continue;
//                //              }
//
//                //community zhi you she zhi chuan bo cai neng chaun bo
//                if (remoteBgpNeighbor.getSendCommunity()) {
//                  transformedOutgoingRouteBuilder._symCommunities=new SymCommunity(remoteRoute._symCommunities);
//                  //                transformedOutgoingRouteBuilder._arbitraryCommunity=remoteRoute._arbitraryCommunity?true:false;
//                  //                transformedOutgoingRouteBuilder.getCommunities()
//                  //                    .addAll(remoteRoute.getCommunities());
//                }else{
//                  transformedOutgoingRouteBuilder._symCommunities=new SymCommunity(false);
//                }
//
//                transformedOutgoingRouteBuilder.setTopologyCondition(transformedOutgoingRouteBuilder.getTopologyCondition().and(linkTopology));
//
//
//
//                transformedOutgoingRouteBuilder.setEgpProtocol(RoutingProtocol.IBGP);
//                transformedOutgoingRouteBuilder.setIgpProtocol(remoteIgpRouteProtocol);
//                //              metric dao di shi gan shen me yong de ?
//                //              transformedOutgoingRouteBuilder.setMetric(remoteRoute.getMetric());
//
//                Ip nextHopIp = remoteBgpNeighbor.getLocalIp();
//                SymBounder localPreference = remoteRoute.getLocalPreference();
//
//                transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
//                transformedOutgoingRouteBuilder.setLocalPreference(localPreference.getLowerbound(),
//                    localPreference.getUpperbound());
//                transformedOutgoingRouteBuilder.setSrcProtocol(remoteRoute.getSrcProtocol());//SRCProtocol qi shi jiu shi shang yi ge jie dain de lu you xie yi
//
//                transformedOutgoingRouteBuilder.setPrefixFactory(this._prefixFactory);
//                transformedOutgoingRouteBuilder.setTopologyFactory(this._bddFactory);
//                transformedOutgoingRouteBuilder.setExternal(external);
//                transformedOutgoingRouteBuilder.setNextNode(nextNode);
//
//                //set relatedNode
//                transformedOutgoingRouteBuilder._relatedNode.addAll(remoteRoute.getRelatedNode());
//                //set prefixEcNum
//                transformedOutgoingRouteBuilder._prefixEcNum=remoteRoute.getPrefixEcNum();
//
//                transformedOutgoingRouteBuilder.setSrcAcl(remoteRoute.getSrcAcl().or(_inSrcAcl.get(edge.getNode1().getNode())));
//                transformedOutgoingRouteBuilder.setDstAcl(remoteRoute.getDstAcl().or(_inDstAcl.get(edge.getNode1().getNode())));
//
//                transformedOutgoingRouteBuilder._isisMetric=remoteRoute.getIsisMetric();
//                SymPolicyAnswer acceptOutgoing = remoteExportPolicy.symProcess(
//                    remoteRoute,
//                    transformedOutgoingRouteBuilder,
//                    localIp,
//                    remoteBgpNeighbor.getPrefix(),
//                    remoteVrfName,
//                    Direction.OUT);
//                if(acceptOutgoing._divideRotue!=null)
//                {
//                  processOutputRoute.add(acceptOutgoing._divideRotue);
//                }
//
//                if(!acceptOutgoing._accept)
//                {
//                  SymBgpRoute denyRoute=cloneBgpRoute(routeAdvert.getRoute());
//                  denyRoute.setTopologyCondition(andBDD(denyRoute.getTopologyCondition(),linkTopology));
//                  if(!denyList.get(Link_name).containsKey(denyRoute.getPrefixEcNum()))
//                  {
//                    if(routeAdvert._reason== org.batfish.symwork.Reason.WITHDRAW)
//                    {
//                      continue;
//                    }
//                    denyList.get(Link_name).put(denyRoute.getPrefixEcNum(),new ArrayList<>());
//                    denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).add(denyRoute);
//                  }else{
//                    if(reason== org.batfish.symwork.Reason.ADD)
//                    {
//                      denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).add(denyRoute);
//                    }else if(reason==org.batfish.symwork.Reason.UPDATE){
//                      List<SymBgpRoute> denyRouteList=denyList.get(Link_name).get(denyRoute.getPrefixEcNum());
//                      int denyNum=-1;
//                      for(int denyIndex=0;denyIndex<denyRouteList.size();denyIndex++)
//                      {
//                        if(denyRouteList.get(denyIndex).equalsUpdate(remoteRoute))
//                        {
//                          denyNum=denyIndex;
//                          break;
//                        }
//                      }
//                      if(denyNum!=-1)
//                      {
//                        denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).remove(denyNum);
//                        denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).add(denyRoute);
//                      }else{
//                        denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).add(denyRoute);
//                      }
//                    }else{
//                      List<SymBgpRoute> denyRouteList=denyList.get(Link_name).get(denyRoute.getPrefixEcNum());
//                      int denyNum=-1;
//                      for(int denyIndex=0;denyIndex<denyRouteList.size();denyIndex++)
//                      {
//                        if(denyRouteList.get(denyIndex).equalsWithDraw(remoteRoute))
//                        {
//                          denyNum=denyIndex;
//                          break;
//                        }
//                      }
//                      if(denyNum!=-1)
//                      {
//                        denyList.get(Link_name).remove(denyNum);
//                      }
//                    }
//                  }
//                }
//
//                if (acceptOutgoing._accept) {
//                  Queue<SymBgpRoute> processInputRoute=new LinkedList<>();
//                  processInputRoute.add(transformedOutgoingRouteBuilder.build());
//                  while(!processInputRoute.isEmpty())
//                  {
//                    SymBgpRoute transformedOutgoingRoute = processInputRoute.poll();
//                    Ip sentReceivedFromIp = transformedOutgoingRoute.getReceivedFromIp();
//                    Ip sentOriginatorIp = transformedOutgoingRoute.getOriginatorIp();
//                    AsPath sentAsPath = transformedOutgoingRoute.getAsPath();
//                    OriginType sentOriginType = transformedOutgoingRoute.getOriginType();
//                    RoutingProtocol sentSrcProtocol = RoutingProtocol.IBGP;
//                    Ip sentNextHopIp = nextHopIp;
//                    String srcNode = remoteRoute.getSrcNode();
//                    BDD sentTopologyCondition = transformedOutgoingRoute.getTopologyCondition();
//                    SymBounder sentAsPathSize = transformedOutgoingRoute.getAsPathSize();
//                    SymBounder sentLocalPreference = transformedOutgoingRoute.getLocalPreference();
//                    int prefixEc=transformedOutgoingRoute.getPrefixEcNum();
//
//                    SymBgpRoute.Builder transformedIncomingRouteBuilder = new SymBgpRoute.Builder(prefixEc,
//                        this._bddFactory,
//                        this._prefixFactory);
//                    org.batfish.symwork.Reason sentReason = org.batfish.symwork.Reason.fromName(
//                        transformedOutgoingRoute.getReason().toString());
//
//                    transformedIncomingRouteBuilder._isisMetric=transformedOutgoingRoute._isisMetric;
//
//                    //set relatedNode
//                    transformedIncomingRouteBuilder._relatedNode.addAll(transformedOutgoingRoute.getRelatedNode());
//
//                    //set prefixEcNum
//                    transformedIncomingRouteBuilder._prefixEcNum = transformedOutgoingRoute.getPrefixEcNum();
//
//                    // Incoming originatorIp
//                    transformedIncomingRouteBuilder.setOriginatorIp(sentOriginatorIp);
//
//                    // Incoming receivedFromIp
//                    transformedIncomingRouteBuilder.setReceivedFromIp(sentReceivedFromIp);
//
//                    transformedIncomingRouteBuilder.setNodePath(transformedOutgoingRoute.getNodePath());
//
//                    // Incoming asPath
//                    transformedIncomingRouteBuilder.setAsPath(new AsPath(sentAsPath.getAsSets()));
//
//                    // Source Node
//                    transformedIncomingRouteBuilder.setSrcNode(srcNode);
//
//                    // Incoming protocol
//                    transformedIncomingRouteBuilder.setEgpProtocol(RoutingProtocol.IBGP);
//                    transformedIncomingRouteBuilder.setIgpProtocol(remoteIgpRouteProtocol);
//
//                    transformedIncomingRouteBuilder.setExternal(external);
//
//
//                    // Incoming nextHopIp
//                    transformedIncomingRouteBuilder.setNextHopIp(sentNextHopIp);
//
//                    // Incoming localPreference
//                    transformedIncomingRouteBuilder.setLocalPreference(sentLocalPreference.getLowerbound(),
//                        sentLocalPreference.getUpperbound());
//                    transformedIncomingRouteBuilder.setAsPathSize(sentAsPathSize.getLowerbound(),
//                        sentAsPathSize.getUpperbound());
//                    transformedIncomingRouteBuilder.setTopologyCondition(sentTopologyCondition);
//
//                    //reason
//                    transformedIncomingRouteBuilder.setReason(sentReason);
//
//                    // Incoming originType
//                    transformedIncomingRouteBuilder.setOriginType(sentOriginType);
//
//                    // Incoming srcProtocol
//                    transformedIncomingRouteBuilder.setSrcProtocol(sentSrcProtocol);
//
//                    //                transformedIncomingRouteBuilder.getCommunities().addAll(transformedOutgoingRouteBuilder.getCommunities());
//
//                    transformedIncomingRouteBuilder._symCommunities=new SymCommunity(transformedOutgoingRoute._symCommunities);
//                    //Acl
//                    transformedIncomingRouteBuilder.setDstAcl(transformedOutgoingRoute.getDstAcl());
//                    transformedIncomingRouteBuilder.setSrcAcl(transformedOutgoingRoute.getSrcAcl());
//
//                    transformedIncomingRouteBuilder.setNextNode(nextNode);
//
//                    String importPolicyName = neighbor.getImportPolicy();
//                    SymPolicyAnswer acceptIncoming=new SymPolicyAnswer();
//
//
//                    if (importPolicyName != null) {
//                      RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
//                      if (importPolicy != null) {
//                        acceptIncoming = importPolicy.symProcess(
//                            transformedOutgoingRoute,
//                            transformedIncomingRouteBuilder,
//                            remoteBgpNeighbor.getLocalIp(),
//                            remoteBgpNeighbor.getPrefix(),
//                            _key,
//                            Direction.IN);
//                      }
//                      if(acceptIncoming._divideRotue!=null)
//                      {
//                        processInputRoute.add(acceptIncoming._divideRotue);
//                      }
//                    }
//
//                    if(!acceptIncoming._accept)
//                    {
//                      SymBgpRoute denyRoute=cloneBgpRoute(routeAdvert.getRoute());
//                      denyRoute.setTopologyCondition(andBDD(denyRoute.getTopologyCondition(),linkTopology));
//                      if(!denyList.get(Link_name).containsKey(denyRoute.getPrefixEcNum()))
//                      {
//                        if(routeAdvert._reason== org.batfish.symwork.Reason.WITHDRAW)
//                        {
//                          continue;
//                        }
//                        denyList.get(Link_name).put(denyRoute.getPrefixEcNum(),new ArrayList<>());
//                        denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).add(denyRoute);
//                      }else{
//                        if(reason== org.batfish.symwork.Reason.ADD)
//                        {
//                          denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).add(denyRoute);
//                        }else if(reason==org.batfish.symwork.Reason.UPDATE){
//                          List<SymBgpRoute> denyRouteList=denyList.get(Link_name).get(denyRoute.getPrefixEcNum());
//                          int denyNum=-1;
//                          for(int denyIndex=0;denyIndex<denyRouteList.size();denyIndex++)
//                          {
//                            if(denyRouteList.get(denyIndex).equalsUpdate(remoteRoute))
//                            {
//                              denyNum=denyIndex;
//                              break;
//                            }
//                          }
//                          if(denyNum!=-1)
//                          {
//                            denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).remove(denyNum);
//                            denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).add(denyRoute);
//                          }else{
//                            denyList.get(Link_name).get(denyRoute.getPrefixEcNum()).add(denyRoute);
//                          }
//                        }else{
//                          List<SymBgpRoute> denyRouteList=denyList.get(Link_name).get(denyRoute.getPrefixEcNum());
//                          int denyNum=-1;
//                          for(int denyIndex=0;denyIndex<denyRouteList.size();denyIndex++)
//                          {
//                            if(denyRouteList.get(denyIndex).equalsWithDraw(remoteRoute))
//                            {
//                              denyNum=denyIndex;
//                              break;
//                            }
//                          }
//                          if(denyNum!=-1)
//                          {
//                            denyList.get(Link_name).remove(denyNum);
//                          }
//                        }
//                      }
//                    }
//
//                    if (acceptIncoming._accept) {
//                      SymBgpRoute addRoute=transformedIncomingRouteBuilder.build();
//                      addRoute.saveLastAttribute(lastBgpLP.getLowerbound(),lastBgpLP.getUpperbound(),lastBgpCommunity);
//                      if (reason == org.batfish.symwork.Reason.ADD) {
//                        this._symBgpRib.AddRoute(addRoute);
//                      } else if (reason == org.batfish.symwork.Reason.UPDATE) {
//                        this._symBgpRib.UpdateRoute(addRoute);
//                      } else if (reason== org.batfish.symwork.Reason.WITHDRAW){
//                        this._symBgpRib.removeRoute(addRoute);
//                      }
//                    }
//                  }
//                }
//              }
//            }
//          }
//        });
  }


  public static int findMin(BDD expression)
  {
    //    List<Object> allSolutions = expression.allsat();
    int minFalseVariables = Integer.MAX_VALUE;

    for (Object solution : expression.allsat()) {
      byte[] assignment = (byte[]) solution;
      int falseCount = 0;
      for (byte b : assignment) {
        if (b == 0) {
          falseCount++;
        }
      }
      minFalseVariables = Math.min(minFalseVariables, falseCount);
    }
    return minFalseVariables;
  }

  public void processSymBgpMessage(Map<Ip,Set<String>> ipOwners,Network<BgpNeighbor,BgpSession> bgpTopology)
  {
    try {
      if (_vrf.getBgpProcess() == null) {
        return;
      }
      for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
        if (!bgpTopology.nodes().contains(neighbor)) {
          continue;
        }
        for (BgpNeighbor remoteBgpNeighbor : bgpTopology.adjacentNodes(neighbor)) {
          Ip localIp = neighbor.getLocalIp();
          String hostname = _c.getHostname();

          Queue<SymRouteAdvertisement<SymBgpRoute>> queue = _symBgpIncomingRoutes.get(
              UndirectedBgpSession.from(neighbor, remoteBgpNeighbor));

          // Setup helper vars
          Configuration remoteConfig = remoteBgpNeighbor.getOwner();
          String remoteHostname = remoteConfig.getHostname();
          String remoteVrfName = remoteBgpNeighbor.getVrf();
          Vrf remoteVrf = remoteConfig.getVrfs().get(remoteVrfName);
          //获取这个节点的所有的路由策略,获取对应邻居的路由策略
          RoutingPolicy remoteExportPolicy = remoteConfig.getRoutingPolicies().get(remoteBgpNeighbor.getExportPolicy());

          int remoteAs = neighbor.getRemoteAs();
          //writed by LXZ,这部分是获取路径条件的编码,后面路径编码后在更新这一部分
          //LinkTopology 编码
          boolean ebgpSession = !neighbor.getLocalAs().equals(neighbor.getRemoteAs());

          RoutingProtocol targetProtocol = RoutingProtocol.BGP;
          if (!ebgpSession) // zhe li zhi chu li ebgp xiao xi
          {
            targetProtocol = RoutingProtocol.IBGP;
          }
          String n1_name = UndirectedBgpSession.from(neighbor, remoteBgpNeighbor)
              .getFirst()
              .getOwner()
              .getHostname();
          String n2_name = UndirectedBgpSession.from(neighbor, remoteBgpNeighbor)
              .getSecond()
              .getOwner()
              .getHostname();
          String Link_name = n1_name + "-" + n2_name;
          if(_linkTopology.get(Link_name)==null)
          {
            System.out.println("process route error - do not have link topology variable");
            continue;
          }
          Integer linkNum=_linkTopology.get(Link_name);


          // Process all candidate routes and queue outgoing messages
          while (queue.peek() != null) {
            SymRouteAdvertisement<SymBgpRoute> remoteRouteAdvert = queue.remove();//首先从消息队列当中获取这个路由宣告然后并删除这一个消息
            org.batfish.symwork.Reason reason = remoteRouteAdvert._reason;
            Queue<SymBgpRoute> processOutputRoute = new LinkedList<>();
            processOutputRoute.add(remoteRouteAdvert.getRoute());
            while (!processOutputRoute.isEmpty()) {
              SymBgpRoute remoteRoute = processOutputRoute.poll();//从宣告消息当中获取这个路由,remoteRoute为传输过来的路由?

              SymBgpRoute.Builder transformedOutgoingRouteBuilder = new SymBgpRoute.Builder(remoteRoute.getPrefixEcSet(),
                  this._topologyBDD,
                  this._prefixFactory);
              transformedOutgoingRouteBuilder.setExternal(remoteRoute.getExternal());
              //transformedOutgoingRouteBuilder为路由消息的builder,字面意思是转换后的路由消息?代表其是在接受路由的时候才进行相关的处理的?
              //          RoutingProtocol remoteRouteProtocol = remoteRoute.getEgpProtocol();//remoteRouteProtocol为传输过来的路由使用的路由协议?
              //          String originalAs=remoteRoute.getAsPath().toString();
              //          boolean remoteRouteIsBgp =
              //              remoteRouteProtocol == RoutingProtocol.IBGP
              //                  || remoteRouteProtocol == RoutingProtocol.BGP;//判断其使用的路由协议是不是BGP协议

              // originatorIP
              // 根据不同的运行协议进行不同的originatorIp的判断
              Ip originatorIp;
              if (!ebgpSession && remoteRoute.getRoutingProtocol().equals(RoutingProtocol.IBGP))
              {
                originatorIp = remoteRoute.getOriginatorIp();
              } else
              {
                originatorIp = remoteVrf.getBgpProcess().getRouterId();
              }
              //起源路由与之前的路由一样
              transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);
              transformedOutgoingRouteBuilder.setReason(reason);
              transformedOutgoingRouteBuilder.setAsPathSize(remoteRoute.getAsPathSize());
              if (ebgpSession && remoteRoute.getAsPath().contains(remoteBgpNeighbor.getRemoteAs())
                  && !remoteBgpNeighbor.getAllowRemoteAsOut()) {
                continue;
              }
              if (!ebgpSession && remoteRoute.getRoutingProtocol().equals(RoutingProtocol.IBGP))
              {
                continue;
              }
              // Outgoing asPath
              // Outgoing communities
              // 然后是处理路由的aspath\communities\拓扑条件
              transformedOutgoingRouteBuilder.setAsPath(remoteRoute.getAsPath());
              transformedOutgoingRouteBuilder.setTopologyCondition(remoteRoute.getTopologyCondition());

              //先不考虑community的事情
              if (remoteBgpNeighbor.getSendCommunity()) {
                transformedOutgoingRouteBuilder.setCommunity(remoteRoute.getCommunity());
              } else {
                transformedOutgoingRouteBuilder.setCommunity(0);
              }

              //处理ASpath\拓扑条件\协议\ip等等

              int topologyCondition=transformedOutgoingRouteBuilder.getTopologyCondition();


              topologyCondition=andBDD(topologyCondition,linkNum);
              _topologyBDD.deref(topologyCondition);
              transformedOutgoingRouteBuilder.setTopologyCondition(topologyCondition);

              transformedOutgoingRouteBuilder.setNodePath(remoteRoute.getNodePath());
              transformedOutgoingRouteBuilder.addNodePath(remoteBgpNeighbor.getOwner().getHostname());
              
              transformedOutgoingRouteBuilder.setRoutingProtocol(targetProtocol);
              transformedOutgoingRouteBuilder.setPrefixFactory(this._prefixFactory);
              transformedOutgoingRouteBuilder.setTopologyBDD(this._topologyBDD);

              //set prefixEcNum
              transformedOutgoingRouteBuilder.setPrefixEcSet(remoteRoute.getPrefixEcSet());

              //设置localPreference
              if (!ebgpSession)
              {
                transformedOutgoingRouteBuilder.setLocalPreference(remoteRoute.getLocalPreference());
                transformedOutgoingRouteBuilder.setNextHopIp(remoteRoute.getNextHopIp());
              } else {
                Ip nextHopIp = remoteBgpNeighbor.getLocalIp();
                transformedOutgoingRouteBuilder.setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE);
                transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
                transformedOutgoingRouteBuilder.getAsPath().add(remoteAs);
                int asPathSize = transformedOutgoingRouteBuilder.getAsPathSize() + 1;
                transformedOutgoingRouteBuilder.setAsPathSize(asPathSize);
              }


              /*
               * CREATE OUTGOING ROUTE
               */
              // 具体产生新的路由消息,这一部分其实是处理具体的能不能接收的判断\
              SymPolicyAnswer acceptOutgoing = remoteExportPolicy.symProcess(remoteRoute,
                  transformedOutgoingRouteBuilder,
                  localIp,
                  remoteBgpNeighbor.getPrefix(),
                  remoteVrfName,
                  Direction.OUT);
              if (acceptOutgoing._accept && acceptOutgoing._divideRotue != null)
              {
                processOutputRoute.add(acceptOutgoing._divideRotue);
              }
              //            SymPolicyAnswer acceptOutgoing=new SymPolicyAnswer();
              //            acceptOutgoing._accept=true;
              if (acceptOutgoing._accept) {
                // Record sent advertisement
                /*
                 * CREATE INCOMING ROUTE
                 */
                // 创建进入的路由
                Queue<SymBgpRoute> processInputRoute = new LinkedList<>();
                processInputRoute.add(transformedOutgoingRouteBuilder.build());
                while (!processInputRoute.isEmpty()) {
                  SymBgpRoute transformedOutgoingRoute = processInputRoute.poll();

                  SymBgpRoute.Builder transformedIncomingRouteBuilder = new SymBgpRoute.Builder(transformedOutgoingRoute);

                  String importPolicyName = neighbor.getImportPolicy();
                  SymPolicyAnswer acceptIncoming = new SymPolicyAnswer();

                  if (importPolicyName != null) {
                    RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
                    if (importPolicy != null) {
                      acceptIncoming = importPolicy.symProcess(transformedOutgoingRoute,
                          transformedIncomingRouteBuilder,
                          remoteBgpNeighbor.getLocalIp(),
                          remoteBgpNeighbor.getPrefix(),
                          _key,
                          Direction.IN);
                    }
                  }

                  if (acceptIncoming._accept && acceptIncoming._divideRotue != null) {
                    processInputRoute.add(acceptIncoming._divideRotue);
                  }

                  if (acceptIncoming._accept) {
                    SymBgpRoute addRoute = transformedIncomingRouteBuilder.build();
                    if (reason == org.batfish.symwork.Reason.WITHDRAW) {
                      this._symBgpRib.removeRoute(addRoute);
                    } else if (reason == org.batfish.symwork.Reason.ADD) {
                      addRoute.setReason(org.batfish.symwork.Reason.ADD);
                      this._symBgpRib.addRoute(addRoute);
                    } else if (reason == org.batfish.symwork.Reason.UPDATE) {
                      addRoute.setReason(org.batfish.symwork.Reason.UPDATE);
                      this._symBgpRib.updateRoute(remoteRoute);
                    } else {
                      continue;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }catch (Exception e)
    {
      System.out.println("process-error");
      e.printStackTrace();
    }
  }

  public void symQueueStagingBgpMessages(
      final Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology)
  {
      if(_vrf.getBgpProcess() == null || _vrf.getBgpProcess().getNeighbors().size()==0)
      {
        return ;
      }
      
      _symBgpRib.computeChangedRoute();
      List<SymBgpRoute> updatedRoute = _symBgpRib.getChanRoutes();
      
      
      List<SymBgpRoute.Builder> updatedRouteBuilder = updatedRoute.stream().map(route -> new SymBgpRoute.Builder(route)).collect(Collectors.toList());

      for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
        if (!bgpTopology.nodes().contains(neighbor) || bgpTopology.adjacentNodes(neighbor).isEmpty()) {
          List<SymRouteAdvertisement<SymBgpRoute>> advertisementList=convertAdvert(updatedRouteBuilder);
          _externalRouteList.get(neighbor).addAll(advertisementList);
        } else {
          for (BgpNeighbor remoteNeighbor : bgpTopology.adjacentNodes(neighbor)) {
            VirtualRouter remoteVirtualRouter = getRemoteBgpNeighborVR(remoteNeighbor, allNodes);
            if (remoteVirtualRouter == null) {
              continue;
            }
            UndirectedBgpSession session=UndirectedBgpSession.from(neighbor, remoteNeighbor);
            List<SymRouteAdvertisement<SymBgpRoute>> advertisementList=convertAdvert(updatedRouteBuilder);
            remoteVirtualRouter.getSymBgpIncomingRoutes().get(session).addAll(advertisementList);
          }
        }
      }

//       Set<SymBgpRoute> withDrawRoute=this._symBgpRib.getWithDrawRoute();
//       HashMap<Integer,Set<TreeNode>> deleteTreeNodes=this._symBgpRib.getDeleteTreeNodes();
//       for(Integer routeIndex:deleteTreeNodes.keySet())
//       {
//         for(TreeNode deleteNode:deleteTreeNodes.get(routeIndex))
//         {
//           if(deleteNode.external)
//           {
//             routeIndexForest.get(routeIndex).deleteNode(deleteNode,true);
//           }else{
//             routeIndexForest.get(routeIndex).deleteNode(deleteNode);
//           }
//         }
//       }

//       Integer asNumber = _vrf.getBgpProcess().getNeighbors().values().stream()
//         .map(BgpNeighbor::getLocalAs)
//         .findFirst()
//         .orElse(-1);
//       if(asNumber==-1)
//       {
//         return ;
//       }
//       for (SymBgpRoute route:withDrawRoute)
//       {
//         try {
//           Integer routePrefixEc = route.getPrefixEcNum();
//           Integer routeIndex = route.getIndex();
//           String asPath = route.getAsPath().getAsPathString();
//           String actualAsPath = asPath + " " + asNumber;
//           List<TreeNode> descendants;
//           if(route._external)
//           {
//             descendants = routeIndexForest.get(routeIndex)
//                 .getAllChilds(new TreeNode(_c.getHostname(),
//                     route.getIndex(),
//                     route.getAsPath().hashCode(),
//                     route.getAsPathSize().hashCode(),
//                     route.getAsPathSize(),route.getAsPath()),true);
//           }else{
//             descendants = routeIndexForest.get(routeIndex)
//                 .getAllChilds(new TreeNode(_c.getHostname(),
//                     route.getIndex(),
//                     route.getAsPath().hashCode(),
//                     route.getAsPathSize().hashCode(),
//                     route.getAsPathSize(),route.getAsPath()));
//           }
//           Set<String> descendantsValues = descendants.stream().map(node -> node.value).collect(Collectors.toSet());
//           List<Node> filteredNodes = allNodes.entrySet().stream().filter(entry -> descendantsValues.contains(entry.getKey())) // 使用 Set 进行匹配
//               .map(Map.Entry::getValue).collect(Collectors.toList());
//           filteredNodes
//               //            .parallelStream()
//               .forEach(node -> {
//                 for (VirtualRouter vr : node.getVirtualRouters().values()) {
//                   vr._symBgpRib.clearTreeNodes();
//                   vr._symBgpRib.removeRoute(routePrefixEc, routeIndex, actualAsPath, route.getAsPathSize());
//                   HashMap<TreeNode, Set<TreeNode>> updateTreeNodes = vr._symBgpRib.getUpdateTreeNodes();
// //                  for(TreeNode treeNode:repalceNodes.keySet())
// //                  {
// //                    routeIndexForest.get(treeNode.getIndex()).updateNodes(treeNode,repalceNodes.get(treeNode));
// //                  }
//                   for (TreeNode treeNode : updateTreeNodes.keySet()) {
//                     routeIndexForest.get(treeNode.getIndex()).updateNodes(treeNode, updateTreeNodes.get(treeNode));
//                   }
//                 }
//               });
//           if (route._external) {
//             routeIndexForest.get(routeIndex)
//                 .deleteNode(new TreeNode(_c.getHostname(),
//                     route.getIndex(),
//                     route.getAsPath().hashCode(),
//                     route.getAsPathSize().hashCode(),
//                     route.getAsPathSize(),route.getAsPath()), true);
//           } else {
//             routeIndexForest.get(routeIndex)
//                 .deleteNode(new TreeNode(_c.getHostname(),
//                     route.getIndex(),
//                     route.getAsPath().hashCode(),
//                     route.getAsPathSize().hashCode(),
//                     route.getAsPathSize(),route.getAsPath()));
//           }
//         }catch (Exception e)
//         {
//           System.out.println("withdrawError");
//           e.printStackTrace();
//         }
//       }
//       List<SymBgpRoute> updatedRoute=this._symBgpRib.getChangedRoute();
//       List<SymBgpRoute.Builder> updatedRouteBuilder=new ArrayList<>();
//       for(int i=0;i<updatedRoute.size();i++)
//       {
//           updatedRouteBuilder.add(new SymBgpRoute.Builder(updatedRoute.get(i)));
//       }
//       if(updatedRouteBuilder.size()==0)
//       {
//         return ;
//       }
// //      final List<SymBgpRoute.Builder> finalUpdatedRouteBuilder=updatedRouteBuilder;
// //      final Network<BgpNeighbor, BgpSession> finalBgpTopology=bgpTopology;
// //      _vrf
// //          .getBgpProcess()
// //          .getNeighbors()
// //          .values()
// ////          .parallelStream()
// //          .forEach(
// //              neighbor->{
// //                if(finalBgpTopology.nodes().contains(neighbor))
// //                {
// //                  finalBgpTopology
// //                      .adjacentNodes(neighbor)
// ////                      .parallelStream()
// //                      .forEach(
// //                          remoteNeighbor->{
// //                            VirtualRouter remoteVirtualRouter = getRemoteBgpNeighborVR(remoteNeighbor, allNodes);
// //                            if (remoteVirtualRouter != null && !neighbor.getLocalAs().equals(remoteNeighbor.getLocalAs())) {
// //                              UndirectedBgpSession session=UndirectedBgpSession.from(neighbor, remoteNeighbor);
// //                              List<SymRouteAdvertisement<SymBgpRoute>> advertisementList=convertAdvert(finalUpdatedRouteBuilder);
// //                              for(int j=0;j<advertisementList.size();j++)
// //                              {
// //                                advertisementList.get(j).getRoute().setIsisMetric(0);
// //                                remoteVirtualRouter._symBgpIncomingRoutes.get(session).add(advertisementList.get(j));
// //                              }
// //                            }
// //                          }
// //                      );
// //                }
// //              }
// //          );
//       for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
//         if (!bgpTopology.nodes().contains(neighbor)) {
//           continue;
//         }
//         for (BgpNeighbor remoteNeighbor : bgpTopology.adjacentNodes(neighbor)) {
//           // Queue for this neighbor
//           // 查看这个BGP邻居实例所在的vrf
//           VirtualRouter remoteVirtualRouter = getRemoteBgpNeighborVR(remoteNeighbor, allNodes);
//           if (remoteVirtualRouter == null) {
//             continue;
//           }
//           if(neighbor.getLocalAs().equals(remoteNeighbor.getLocalAs()))
//           {
//             continue;
//           }
//           UndirectedBgpSession session=UndirectedBgpSession.from(neighbor, remoteNeighbor);
//           List<SymRouteAdvertisement<SymBgpRoute>> advertisementList=convertAdvert(updatedRouteBuilder);
//           for(int j=0;j<advertisementList.size();j++)
//           {
//             advertisementList.get(j).getRoute().setIsisMetric(0);
//             remoteVirtualRouter._symBgpIncomingRoutes.get(session).add(advertisementList.get(j));
//           }
//         }
//       }
  }

//  public void symQueueStagingIsisMessages(
//      final Map<String, Node> allNodes,
//      IsisTopology isisTopology,
//      Map<String, Configuration> configurations)
//  {
//
//    if (_vrf.getIsisProcess() == null || _isisIncomingRoutes == null) {
//      return;
//    }
//    List<SymBgpRoute> updatedRoute=this._symBgpRib.GetLastAttributeChangedRoute();
//    List<SymBgpRoute.Builder> updatedRouteBuilder=new ArrayList<>();
//    for(int i=0;i<updatedRoute.size();i++)
//    {
//      updatedRouteBuilder.add(new SymBgpRoute.Builder(updatedRoute.get(i)));
//    }
//    if(updatedRouteBuilder.size()==0)
//    {
//      return ;
//    }
//
//    // Loop over neighbors, enqueue messages
//    for (IsisEdge edge : _isisIncomingRoutes.keySet()) {
//      List<SymRouteAdvertisement<SymBgpRoute>> advertisementList=convertAdvert(updatedRouteBuilder);
//      // Do not queue routes on non-active ISIS interface levels
//      Interface iface = edge.getNode2().getInterface(configurations);
//      IsisInterfaceLevelSettings level1Settings = iface.getIsis().getLevel1();
//      IsisInterfaceLevelSettings level2Settings = iface.getIsis().getLevel2();
//      IsisLevel activeLevels = null;
//      if (level1Settings != null && level1Settings.getMode() == IsisInterfaceMode.ACTIVE) {
//        activeLevels = IsisLevel.LEVEL_1;
//      }
//      if (level2Settings != null && level2Settings.getMode() == IsisInterfaceMode.ACTIVE) {
//        activeLevels = IsisLevel.union(activeLevels, IsisLevel.LEVEL_2);
//      }
//
//      if (activeLevels == null) {
//        continue;
//      }
//
//      //mu qian zhi kao lv zai isis-level2/1-2 lu you qi shang chuan bo
////      if(activeLevels.includes(IsisLevel.LEVEL_1)&&!activeLevels.includes(IsisLevel.LEVEL_2))
////      {
////        continue;
////      }
//
//      VirtualRouter remoteVr =
//          allNodes
//              .get(edge.getNode1().getNode())
//              .getVirtualRouters().get(edge.getNode1().getInterface(configurations).getVrfName());
//      Queue<SymRouteAdvertisement<SymBgpRoute>> queue = remoteVr._isisIncomingRoutes.get(edge.reverse());
//      IsisLevel circuitType = edge.getCircuitType();
//
//      if (circuitType.includes(IsisLevel.LEVEL_1) && activeLevels.includes(IsisLevel.LEVEL_1)) {//circuitType ying gai shi zhi zhe tiao bian de lei xing level1-2 he level2 lian jie de bian ying gai jiu saun level2
//        for(int j=0;j<advertisementList.size();j++)
//        {
//          advertisementList.get(j).getRoute().setIsisLevel(IsisLevel.LEVEL_1);
//          advertisementList.get(j).getRoute().setIgpProtocol(RoutingProtocol.ISIS_L1);
//          queue.add(advertisementList.get(j));
//          //ru guo ni shi level1 de wo zhi ba level 1 chuan gei ni ,level 2 zhi chuan level 2,zhi you level1-2 chuan suo you
//          //liang bian xu yao you yi ge guan xi de pi pei!!!
//        }
//      }
//      if (circuitType.includes(IsisLevel.LEVEL_2) && activeLevels.includes(IsisLevel.LEVEL_2)) {
//        for(int j=0;j<advertisementList.size();j++)
//        {
////          if(advertisementList.get(j).getRoute().getIsisLevel()!=null&&advertisementList.get(j).getRoute().getIsisLevel()==IsisLevel.LEVEL_1&&!upgradeL1Routes)
////          {
////            continue;
////          }
//          advertisementList.get(j).getRoute().setIsisLevel(IsisLevel.LEVEL_2);
//          advertisementList.get(j).getRoute().setIgpProtocol(RoutingProtocol.ISIS_L2);
//          queue.add(advertisementList.get(j));
//          //ru guo ni shi level1 de wo zhi ba level 1 chuan gei ni ,level 2 zhi chuan level 2,zhi you level1-2 chuan suo you
//          //liang bian xu yao you yi ge guan xi de pi pei!!!
//        }
//      }
//    }
//  }

  public List<SymRouteAdvertisement<SymBgpRoute>> convertAdvert( List<SymBgpRoute.Builder> updatedRouteBuilder)
  {
    List<SymRouteAdvertisement<SymBgpRoute>> advertisementList=new ArrayList<>();
    for(int i=0;i<updatedRouteBuilder.size();i++)
    {
      SymBgpRoute temp=updatedRouteBuilder.get(i).build();
      SymRouteAdvertisement<SymBgpRoute> advert=new SymRouteAdvertisement<SymBgpRoute>(temp);
      advert._reason= org.batfish.symwork.Reason.fromName(temp.getReason().toString());
      advert._route.setReason(org.batfish.symwork.Reason.NORMAL);
      advertisementList.add(advert);
    }
    return advertisementList;
  }
  /**
   * Process BGP messages from neighbors, return a list of delta changes to the RIBs
   *
   * @param ipOwners Mapping of IPs to a set of node names that own that IP
   * @param bgpTopology the bgp peering relationships
   * @return List of {@link RibDelta objects}
   */
  //要改
  Map<BgpMultipathRib, RibDelta<BgpRoute>> processBgpMessages(
      Map<Ip, Set<String>> ipOwners, Network<BgpNeighbor, BgpSession> bgpTopology) {
//
//    // If we have no BGP process, nothing to do
//    if (_vrf.getBgpProcess() == null) {
//      return null;
//    }
//
//    // Keep track of changes to the RIBs using delta builders, keyed by RIB type
//    Map<BgpMultipathRib, RibDelta.Builder<BgpRoute>> ribDeltas = new IdentityHashMap<>();
//    ribDeltas.put(_ebgpStagingRib, new Builder<>(_ebgpStagingRib));
//    ribDeltas.put(_ibgpStagingRib, new Builder<>(_ibgpStagingRib));
//
//    // Init default admin costs
//    int ebgpAdminCost =
//        RoutingProtocol.BGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
//    int ibgpAdminCost =
//        RoutingProtocol.IBGP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
//
//    // Process updates from each neighbor
//    // 处理来自邻居的路由消息
//    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
//      if (!bgpTopology.nodes().contains(neighbor)) {
//        continue;
//      }
//      for (BgpNeighbor remoteBgpNeighbor : bgpTopology.adjacentNodes(neighbor)) {
//        if(!bgpTopology.nodes().contains(remoteBgpNeighbor))
//        {
//          continue;
//        }
//        Ip localIp = neighbor.getLocalIp();
//        String hostname = _c.getHostname();
//
//        Queue<RouteAdvertisement<AbstractRoute>> queue =
//            _bgpIncomingRoutes.get(UndirectedBgpSession.from(neighbor, remoteBgpNeighbor));
//        Queue<SymRouteAdvertisement<SymBgpRoute>> symQueue=_symBgpIncomingRoutes.get(UndirectedBgpSession.from(neighbor,remoteBgpNeighbor));
//
//        // Setup helper vars
//        Configuration remoteConfig = remoteBgpNeighbor.getOwner();
//        String remoteHostname = remoteConfig.getHostname();
//        String remoteVrfName = remoteBgpNeighbor.getVrf();
//        Vrf remoteVrf = remoteConfig.getVrfs().get(remoteVrfName);
//        //获取这个节点的所有的路由策略,获取对应邻居的路由策略
//        RoutingPolicy remoteExportPolicy =
//            remoteConfig.getRoutingPolicies().get(remoteBgpNeighbor.getExportPolicy());
//        System.out.println("-------------------------路由策略详解:----------------------");
//        System.out.println("Node:"+remoteConfig.getHostname());
//        System.out.println("RoutingPolicyName:"+remoteExportPolicy.getName());
//        System.out.println("RoutingPolicyComment:");
//        List<Statement> statePrint=remoteExportPolicy.getStatements();
//        for(int i=0;i<statePrint.size();i++)
//        {
//            System.out.println("  comment:"+statePrint.get(i).getComment());
//            System.out.println("  toString:"+statePrint.get(i).toString());
//            if(statePrint.get(i) instanceof If)
//            {
//                System.out.println("    TrueStateMent:"+((If) statePrint.get(i)).getTrueStatements());
//                System.out.println("    FalseStatement:"+((If) statePrint.get(i)).getFalseStatements());
//                System.out.println("    Guard:"+((If) statePrint.get(i)).getGuard().toString());
//                BooleanExpr guard=((If) statePrint.get(i)).getGuard();
//                if(guard instanceof Conjunction)
//                {
//                  System.out.println("    Conjunction:"+((Conjunction) guard).getConjuncts().toString());
//                }
//                if(guard instanceof Disjunction)
//                {
//                  System.out.println("    Disjunction:"+((Disjunction) guard).getDisjuncts().toString());
//                }
//                if(guard instanceof CallExpr)
//                {
//                  RoutingPolicy callPolicy=remoteConfig.getRoutingPolicies().get(((CallExpr) guard).getCalledPolicyName());
//                  System.out.println("      CallPolicyName:"+callPolicy.toString());
//                  System.out.println("      CallPolicyStatement:"+callPolicy.getStatements().toString());
//                  for(int j=0;j<callPolicy.getStatements().size();j++)
//                  {
//                    System.out.println("        CallPolicyStatementClass:"+callPolicy.getStatements().get(j).getClass());
//                    Statement callStatement=callPolicy.getStatements().get(j);
//                    if(callStatement instanceof If)
//                    {
//                      System.out.println("        CallStatementTrueStateMent:"+((If) callStatement).getTrueStatements());
//                      System.out.println("        CallStatementFalseStatement:"+((If) callStatement).getFalseStatements());
//                      System.out.println("        CallStatementGuard:"+((If) callStatement).getGuard().toString());
//                      BooleanExpr guardCallStatement=((If) callStatement).getGuard();
//                      if(guardCallStatement instanceof Disjunction)
//                      {
//                        System.out.println("          CallGardDisjunction:"+((Disjunction) guardCallStatement).getDisjuncts().toString());
//                        for(int n=0;n<((Disjunction) guardCallStatement).getDisjuncts().size();n++)
//                        {
//                          System.out.println("            DisjunctionContent:"+((Disjunction) guardCallStatement).getDisjuncts().get(n));
//                          BooleanExpr callConjunction=((Disjunction) guardCallStatement).getDisjuncts().get(n);
//                          if(callConjunction instanceof Conjunction)
//                          {
//                            System.out.println("              ConjunctionContent:"+((Conjunction) callConjunction).getConjuncts().toString());
//                            for(int b=0;b<((Conjunction) callConjunction).getConjuncts().size();b++)
//                            {
//                              BooleanExpr  nextConjunction=((Conjunction) callConjunction).getConjuncts().get(b);
//                              if(nextConjunction instanceof MatchPrefixSet)
//                              {
//                                MatchPrefixSet callPrefixSet=((MatchPrefixSet)nextConjunction);
//                                PrefixSetExpr callExplicitPrefixSet=callPrefixSet.getPrefixSet();
//                                System.out.println("                MatchPrefixSetConjunct:"+callPrefixSet.toString());
//                                System.out.println("                MatchPrefixSetClass:"+((MatchPrefixSet)nextConjunction).getPrefixSet().getClass());
//                                System.out.println("                MatchPrefixClass:"+((MatchPrefixSet)nextConjunction).getPrefix().getClass());
//                                if(callExplicitPrefixSet instanceof ExplicitPrefixSet)
//                                {
//                                  System.out.println("                  CallPrefixSetPrefixSpace:"+((ExplicitPrefixSet) callExplicitPrefixSet).getPrefixSpace().toString());
//                                }
//                              }
//                            }
//                          }
//                        }
//                      }
//                    }
//                  }
//                  System.out.println("CallExpr:"+((CallExpr)guard).toString());
//                }
//            }
//        }
//        System.out.println("-----------------------------END--------------------------");
//
//
//        System.out.println("----------routingpolicy:------------------");
//        //一个owner里边有多个的bgpneighbor,其中是以prefix前缀来区分的,所以要选择这个bgpneighbor的路由策略,不是依靠源和目的端点来确定的
//        System.out.println("remoteConfig.getRoutingPolicies():"+remoteConfig.getRoutingPolicies().toString());
//        System.out.println("remoteBgpNeighbor:"+remoteBgpNeighbor.toString());
//        System.out.println("remoteBgpNeighbor.getExportPolicy():"+remoteBgpNeighbor.getExportPolicy());
//        System.out.println("owner:"+this.getHostname()+";Neighbor:"+remoteBgpNeighbor.getOwner().getHostname());
//        System.out.println("policyOwner:"+remoteBgpNeighbor.getOwner().getHostname()+";name:"+remoteExportPolicy.getName()+"; statements:"+remoteExportPolicy.getStatements().toString());
//        System.out.println("---------------End------------------------");
//        //";  source"+remoteExportPolicy.getSources().toString()+"; statements:"+remoteExportPolicy.getStatements().toString()
//        int remoteAs = neighbor.getRemoteAs();
//        //writed by LXZ,这部分是获取路径条件的编码,后面路径编码后在更新这一部分
//        //LinkTopology 编码
//        String n1_name= UndirectedBgpSession.from(neighbor, remoteBgpNeighbor).getFirst().getOwner().getHostname();
//        String n2_name= UndirectedBgpSession.from(neighbor, remoteBgpNeighbor).getSecond().getOwner().getHostname();
//        String Link_name=n1_name+"-"+n2_name;
//        int len = _edgeEncode.size() / 2;
//        BDDFactory factory = BDDFactory.init(len,len);
//        factory.setVarNum(len);
//        System.out.println("link_num:"+_edgeEncode.get(Link_name));
//        BDD linkTopology = _bddFactory.ithVar(_edgeEncode.get(Link_name));
//
//        boolean ebgpSession = !neighbor.getLocalAs().equals(neighbor.getRemoteAs());
//        BgpMultipathRib targetRib = ebgpSession ? _ebgpStagingRib : _ibgpStagingRib;
//        RoutingProtocol targetProtocol = ebgpSession ? RoutingProtocol.BGP : RoutingProtocol.IBGP;
//
//
//        System.out.println("queue_new:"+queue.toString());
//
//        // Process all candidate routes and queue outgoing messages
//        while (queue.peek() != null) {
//          RouteAdvertisement<AbstractRoute> remoteRouteAdvert = queue.remove();//首先从消息队列当中获取这个路由宣告然后并删除这一个消息
//          System.out.println("receiveRoute:"+remoteRouteAdvert.toString());
//          AbstractRoute remoteRoute = remoteRouteAdvert.getRoute();//从宣告消息当中获取这个路由,remoteRoute为传输过来的路由?
//          BgpRoute.Builder transformedOutgoingRouteBuilder = new BgpRoute.Builder(_bddFactory);//transformedOutgoingRouteBuilder为路由消息的builder,字面意思是转换后的路由消息?代表其是在接受路由的时候才进行相关的处理的?
//          RoutingProtocol remoteRouteProtocol = remoteRoute.getProtocol();//remoteRouteProtocol为传输过来的路由使用的路由协议?
//          boolean remoteRouteIsBgp =
//              remoteRouteProtocol == RoutingProtocol.IBGP
//                  || remoteRouteProtocol == RoutingProtocol.BGP;//判断其使用的路由协议是不是BGP协议
//
//          // originatorIP
//          // 根据不同的运行协议进行不同的originatorIp的判断
//          Ip originatorIp;
//          System.out.println("...................................10.........................");
//          if (!ebgpSession && remoteRouteProtocol == RoutingProtocol.IBGP) {
//            BgpRoute bgpRemoteRoute = (BgpRoute) remoteRoute;
//            originatorIp = bgpRemoteRoute.getOriginatorIp();
//          } else {
//            originatorIp = remoteVrf.getBgpProcess().getRouterId();
//          }
//          System.out.println(".............................11.........................");
//          //起源路由与之前的路由一样
//          transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);
//          transformedOutgoingRouteBuilder.setReceivedFromIp(remoteBgpNeighbor.getLocalIp());//接受到的自治系统的ip就更新为邻居的ip
//          // note whether new route is received from route reflector client
//          transformedOutgoingRouteBuilder.setReceivedFromRouteReflectorClient(
//              !ebgpSession && neighbor.getRouteReflectorClient());
//
//          /*
//           * clusterList, receivedFromRouteReflectorClient, (originType
//           * for bgp remote route)
//           */
//          System.out.println(".............................12.........................");
//          if (remoteRouteIsBgp) {
//            BgpRoute temp=(BgpRoute) remoteRoute;
//            System.out.println(".............................13.........................");
//            System.out.println(remoteRoute.getProtocol());
//            System.out.println(remoteRoute.toString());
//            BgpRoute bgpRemoteRoute = temp;
//            System.out.println(".............................14.........................");
//            bgpRemoteRoute.getTopologyCondition().printSet();
//            System.out.println("------------receive:"+bgpRemoteRoute.getTopologyCondition().toString());
//            System.out.println(".............................15.........................");
//            transformedOutgoingRouteBuilder.setOriginType(bgpRemoteRoute.getOriginType());
//            if (ebgpSession
//                && bgpRemoteRoute.getAsPath().containsAs(remoteBgpNeighbor.getRemoteAs())//去除会产生环路的路由,针对这一部分路由不进行处理
//                && !remoteBgpNeighbor.getAllowRemoteAsOut()) {
//              // skip routes containing peer's AS unless
//              // disable-peer-as-check (getAllowRemoteAsOut) is set
//              continue;
//            }
//            /*
//             * route reflection: reflect everything received from
//             * clients to clients and non-clients. reflect everything
//             * received from non-clients to clients. Do not reflect to
//             * originator
//             */
//
//            Ip remoteOriginatorIp = bgpRemoteRoute.getOriginatorIp();
//
//            /*
//             *  iBGP speaker should not send out routes to iBGP neighbor whose router-id is
//             *  same as originator id of advertisement
//             */
//            if (!ebgpSession
//                && remoteOriginatorIp != null
//                && _vrf.getBgpProcess().getRouterId().equals(remoteOriginatorIp)) { //iBgp路由应该是不传播给其邻居的
//              continue;
//            }
//            if (remoteRouteProtocol == RoutingProtocol.IBGP && !ebgpSession) {
//              /*
//               *  The remote route is iBGP. The session is iBGP. We consider whether to reflect, and
//               *  modify the outgoing route as appropriate.
//               *  这一部分主要是判断路由是否进行反射,如果要进行反射的话就设这为正确的路由,路由反射的话其实ASpath等字段是不会改变的,所以应该不是我们需要考虑的地方
//               */
//              boolean remoteRouteReceivedFromRouteReflectorClient =
//                  bgpRemoteRoute.getReceivedFromRouteReflectorClient();
//              boolean sendingToRouteReflectorClient = remoteBgpNeighbor.getRouteReflectorClient();
//
//              Ip remoteReceivedFromIp = bgpRemoteRoute.getReceivedFromIp();
//              boolean remoteRouteOriginatedByRemoteNeighbor = remoteReceivedFromIp.equals(Ip.ZERO);
//              if (!remoteRouteReceivedFromRouteReflectorClient
//                  && !sendingToRouteReflectorClient
//                  && !remoteRouteOriginatedByRemoteNeighbor) {
//                /*
//                 * Neither reflecting nor originating this iBGP route, so don't send
//                 */
//                continue;
//              }
//              transformedOutgoingRouteBuilder
//                  .getClusterList()
//                  .addAll(bgpRemoteRoute.getClusterList());
//              if (!remoteRouteOriginatedByRemoteNeighbor) {
//                // we are reflecting, so we need to get the clusterid associated with the
//                // remoteRoute
//                BgpNeighbor remoteReceivedFromSession =
//                    remoteVrf
//                        .getBgpProcess()
//                        .getNeighbors()
//                        .get(new Prefix(remoteReceivedFromIp, Prefix.MAX_PREFIX_LENGTH));
//                long newClusterId = remoteReceivedFromSession.getClusterId();
//                transformedOutgoingRouteBuilder.getClusterList().add(newClusterId);
//              }
//              Set<Long> localClusterIds = _vrf.getBgpProcess().getClusterIds();
//              Set<Long> outgoingClusterList = transformedOutgoingRouteBuilder.getClusterList();
//              if (localClusterIds.stream().anyMatch(outgoingClusterList::contains)) {
//                /*
//                 *  receiver will reject new route if it contains any of its local cluster ids
//                 */
//                continue;
//              }
//            }
//          }
//
//          // Outgoing asPath
//          // Outgoing communities
//          // 然后是处理路由的aspath\communities\拓扑条件
//          if (remoteRouteIsBgp) {
//            BgpRoute bgpRemoteRoute = (BgpRoute) remoteRoute;
//            transformedOutgoingRouteBuilder.setAsPath(bgpRemoteRoute.getAsPath().getAsSets());
//            transformedOutgoingRouteBuilder.setTopologyCondition(bgpRemoteRoute.getTopologyCondition());
//            if (remoteBgpNeighbor.getSendCommunity()) {
//              transformedOutgoingRouteBuilder
//                  .getCommunities()
//                  .addAll(bgpRemoteRoute.getCommunities());
//            }
//          }
//          if (ebgpSession) { //只有是Ebgp才会处理相应的AsPath和拓扑条件
//            SortedSet<Integer> newAsPathElement = new TreeSet<>();
//            newAsPathElement.add(remoteAs);
//            transformedOutgoingRouteBuilder.getAsPath().add(0, newAsPathElement);//增加Aspath的信息
//            System.out.println("--------------before:"+transformedOutgoingRouteBuilder.getTopologyCondition().toString());
//            transformedOutgoingRouteBuilder.setTopologyCondition(transformedOutgoingRouteBuilder.getTopologyCondition().and(linkTopology));//增加拓扑条件
//            System.out.println("--------------after:"+transformedOutgoingRouteBuilder.getTopologyCondition().toString());
//          }
//
//          // Outgoing protocol
//          transformedOutgoingRouteBuilder.setProtocol(targetProtocol);
//          transformedOutgoingRouteBuilder.setNetwork(remoteRoute.getNetwork());
//
//          //setRelatedNode
//          System.out.println("..........................................");
//          // Outgoing metric
//          if (remoteRouteIsBgp) {
//            transformedOutgoingRouteBuilder.setMetric(remoteRoute.getMetric());
//          }
//          System.out.println(".....................1.....................");
//          // Outgoing nextHopIp
//          // Outgoing localPreference
//          Ip nextHopIp;
//          int localPreference;
//          if (ebgpSession || !remoteRouteIsBgp) {
//            nextHopIp = remoteBgpNeighbor.getLocalIp();//如果是Ebgp的话路由消息就将下一跳更新为了传播来路由的AS的ip
//            localPreference = BgpRoute.DEFAULT_LOCAL_PREFERENCE;
//          } else {
//            nextHopIp = remoteRoute.getNextHopIp();//如果是Ibgp的话其实是不更新下一跳的路由消息的,因为这是iBGP的消息的特性
//            BgpRoute remoteIbgpRoute = (BgpRoute) remoteRoute;
//            localPreference = remoteIbgpRoute.getLocalPreference();
//          }
//          System.out.println(".....................2.....................");
//          if (nextHopIp.equals(Route.UNSET_ROUTE_NEXT_HOP_IP)) {//如果没有下一跳的路由信息进行处理
//            // should only happen for ibgp
//            String nextHopInterface = remoteRoute.getNextHopInterface();
//            InterfaceAddress nextHopAddress =
//                remoteVrf.getInterfaces().get(nextHopInterface).getAddress();
//            if (nextHopAddress == null) {
//              throw new BatfishException("remote route's nextHopInterface has no address");
//            }
//            nextHopIp = nextHopAddress.getIp();
//          }
//          System.out.println(".....................3.....................");
//          transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);//设置新的路由消息的下一跳的地址信息
//          transformedOutgoingRouteBuilder.setLocalPreference(localPreference);//设置路由消息的新的localPreference
//
//          // Outgoing srcProtocol
//          transformedOutgoingRouteBuilder.setSrcProtocol(remoteRoute.getProtocol());//设置路由消息的新的协议
//          System.out.println(".....................4.....................");
//          /*
//           * CREATE OUTGOING ROUTE
//           */
//          // 具体产生新的路由消息,这一部分其实是处理具体的能不能接收的判断
//          boolean acceptOutgoing =
//              remoteExportPolicy.process(
//                  remoteRoute,
//                  transformedOutgoingRouteBuilder,
//                  localIp,
//                  remoteBgpNeighbor.getPrefix(),
//                  remoteVrfName,
//                  Direction.OUT);
//          System.out.println(".....................5.....................");
//          if (acceptOutgoing) {
//            System.out.println(".....................6.....................");
//            BgpRoute transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();//如果真的能接受的话,那么就形成真正的路由为transformedOutgoingRoute
//            System.out.println("--------------route:"+transformedOutgoingRoute.toString());
//            // Record sent advertisement
//            BgpAdvertisementType sentType =
//                ebgpSession ? BgpAdvertisementType.EBGP_SENT : BgpAdvertisementType.IBGP_SENT;//将这个路由消息设置为SENT类型的
//            Ip sentReceivedFromIp = transformedOutgoingRoute.getReceivedFromIp();//设置相应的接受到路由消息的ip地址以及相应的起源的ip地址
//            Ip sentOriginatorIp = transformedOutgoingRoute.getOriginatorIp();
//            SortedSet<Long> sentClusterList =
//                ImmutableSortedSet.copyOf(transformedOutgoingRoute.getClusterList());
//            boolean sentReceivedFromRouteReflectorClient =
//                transformedOutgoingRoute.getReceivedFromRouteReflectorClient();
//            AsPath sentAsPath = transformedOutgoingRoute.getAsPath();
//            SortedSet<Long> sentCommunities =
//                ImmutableSortedSet.copyOf(transformedOutgoingRoute.getCommunities());
//            Prefix sentNetwork = remoteRoute.getNetwork();
//            Ip sentNextHopIp;
//            String sentSrcNode = remoteHostname;
//            String sentSrcVrf = remoteVrfName;
//            Ip sentSrcIp = remoteBgpNeighbor.getLocalIp();
//            String sentDstNode = hostname;
//            String sentDstVrf = _vrf.getName();
//            Ip sentDstIp = neighbor.getLocalIp();
//            BDD sentTopologyCondition=transformedOutgoingRoute.getTopologyCondition();//对应拓扑条件,这里我们也需要这只相应的拓扑条件
//            int sentWeight = -1;
//            if (ebgpSession) {
//              sentNextHopIp = nextHopIp;
//            } else {
//              sentNextHopIp = transformedOutgoingRoute.getNextHopIp();
//            }
//            int sentLocalPreference = transformedOutgoingRoute.getLocalPreference();
//            long sentMed = transformedOutgoingRoute.getMetric();
//            OriginType sentOriginType = transformedOutgoingRoute.getOriginType();
//            RoutingProtocol sentSrcProtocol = targetProtocol;
//            BgpRoute.Builder transformedIncomingRouteBuilder = new BgpRoute.Builder(_bddFactory);
//
//            // Incoming originatorIp
//            transformedIncomingRouteBuilder.setOriginatorIp(sentOriginatorIp);
//
//            // Incoming receivedFromIp
//            transformedIncomingRouteBuilder.setReceivedFromIp(sentReceivedFromIp);
//
//            // Incoming clusterList
//            transformedIncomingRouteBuilder.getClusterList().addAll(sentClusterList);
//
//            // Incoming receivedFromRouteReflectorClient
//            transformedIncomingRouteBuilder.setReceivedFromRouteReflectorClient(
//                sentReceivedFromRouteReflectorClient);
//
//            // Incoming asPath
//            transformedIncomingRouteBuilder.setAsPath(sentAsPath.getAsSets());
//
//            // Incoming communities
//            transformedIncomingRouteBuilder.getCommunities().addAll(sentCommunities);
//
//            // Incoming protocol
//            transformedIncomingRouteBuilder.setProtocol(targetProtocol);
//
//            // Incoming network
//            transformedIncomingRouteBuilder.setNetwork(sentNetwork);
//
//            // Incoming nextHopIp
//            transformedIncomingRouteBuilder.setNextHopIp(sentNextHopIp);
//
//            // Incoming localPreference
//            transformedIncomingRouteBuilder.setLocalPreference(sentLocalPreference);
//
//            // Incoming admin
//            int admin = ebgpSession ? ebgpAdminCost : ibgpAdminCost;
//            transformedIncomingRouteBuilder.setAdmin(admin);
//
//            // Incoming metric
//            transformedIncomingRouteBuilder.setMetric(sentMed);
//
//            // Incoming originType
//            transformedIncomingRouteBuilder.setOriginType(sentOriginType);
//
//            // Incoming srcProtocol
//            transformedIncomingRouteBuilder.setSrcProtocol(sentSrcProtocol);
//            String importPolicyName = neighbor.getImportPolicy();
//
//            System.out.println("-------------------sentTopologyCondition:"+sentTopologyCondition.toString());
//            //对应拓扑条件的处理
//            transformedIncomingRouteBuilder.setTopologyCondition(sentTopologyCondition);
//            System.out.println("-------------------transformedIncomingRouteBuilder:"+transformedIncomingRouteBuilder.getTopologyCondition().toString());
//            // TODO: ensure there is always an import policy
//
//            if (transformedOutgoingRoute.getAsPath().containsAs(neighbor.getLocalAs())//判断是否会产生相应的回路
//                && !neighbor.getAllowLocalAsIn()) {
//              // skip routes containing peer's AS unless
//              // disable-peer-as-check (getAllowRemoteAsOut) is set
//              continue;
//            }
//            //上边为什么又重新计算一个?
//            BgpAdvertisement sentAdvert =
//                new BgpAdvertisement(
//                    sentType,
//                    sentNetwork,
//                    sentNextHopIp,
//                    sentSrcNode,
//                    sentSrcVrf,
//                    sentSrcIp,
//                    sentDstNode,
//                    sentDstVrf,
//                    sentDstIp,
//                    sentSrcProtocol,
//                    sentOriginType,
//                    sentLocalPreference,
//                    sentMed,
//                    sentOriginatorIp,
//                    sentAsPath,
//                    ImmutableSortedSet.copyOf(sentCommunities),
//                    ImmutableSortedSet.copyOf(sentClusterList),
//                    sentWeight,
//                    sentTopologyCondition);
//            System.out.println("-------------------SentRoute:"+sentAdvert.toString());
//            if (!remoteRouteAdvert.isWithdrawn()) {
//              _sentBgpAdvertisements.add(sentAdvert);
//            }
//
//            /*
//             * CREATE INCOMING ROUTE
//             */
//            // 创建进入的路由
//            boolean acceptIncoming = true;
//            if (importPolicyName != null) {
//              System.out.println("----------routingpolicy-Import:------------------");
//              //一个owner里边有多个的bgpneighbor,其中是以prefix前缀来区分的,所以要选择这个bgpneighbor的路由策略,不是依靠源和目的端点来确定的
//              System.out.println("importPolicyName:"+importPolicyName);
//              System.out.println("importPolicy:"+_c.getRoutingPolicies().get(importPolicyName));
//              System.out.println("---------------End------------------------");
//              RoutingPolicy importPolicy = _c.getRoutingPolicies().get(importPolicyName);
//              if (importPolicy != null) {
//                acceptIncoming =
//                    importPolicy.process(
//                        transformedOutgoingRoute,
//                        transformedIncomingRouteBuilder,
//                        remoteBgpNeighbor.getLocalIp(),
//                        remoteBgpNeighbor.getPrefix(),
//                        _key,
//                        Direction.IN);
//              }
//            }
//            if (acceptIncoming) {
//              BgpRoute transformedIncomingRoute = transformedIncomingRouteBuilder.build();
//              BgpAdvertisementType receivedType =
//                  ebgpSession
//                      ? BgpAdvertisementType.EBGP_RECEIVED
//                      : BgpAdvertisementType.IBGP_RECEIVED;
//              Prefix receivedNetwork = sentNetwork;
//              Ip receivedNextHopIp = sentNextHopIp;
//              String receivedSrcNode = sentSrcNode;
//              String receivedSrcVrf = sentSrcVrf;
//              Ip receivedSrcIp = sentSrcIp;
//              String receivedDstNode = sentDstNode;
//              String receivedDstVrf = sentDstVrf;
//              Ip receivedDstIp = sentDstIp;
//              RoutingProtocol receivedSrcProtocol = sentSrcProtocol;
//              OriginType receivedOriginType = transformedIncomingRoute.getOriginType();
//              int receivedLocalPreference = transformedIncomingRoute.getLocalPreference();
//              long receivedMed = transformedIncomingRoute.getMetric();
//              Ip receivedOriginatorIp = sentOriginatorIp;
//              AsPath receivedAsPath = transformedIncomingRoute.getAsPath();
//              BDD receiveTopologyCondition = transformedIncomingRoute.getTopologyCondition();
//              System.out.println("-------------------receiveTopologyCondition:"+receiveTopologyCondition.toString());
//              SortedSet<Long> receivedCommunities =
//                  ImmutableSortedSet.copyOf(transformedIncomingRoute.getCommunities());
//              SortedSet<Long> receivedClusterList = ImmutableSortedSet.copyOf(sentClusterList);
//              int receivedWeight = transformedIncomingRoute.getWeight();
//
//              BgpAdvertisement receivedAdvert =
//                  new BgpAdvertisement(
//                      receivedType,
//                      receivedNetwork,
//                      receivedNextHopIp,
//                      receivedSrcNode,
//                      receivedSrcVrf,
//                      receivedSrcIp,
//                      receivedDstNode,
//                      receivedDstVrf,
//                      receivedDstIp,
//                      receivedSrcProtocol,
//                      receivedOriginType,
//                      receivedLocalPreference,
//                      receivedMed,
//                      receivedOriginatorIp,
//                      receivedAsPath,
//                      receivedCommunities,
//                      receivedClusterList,
//                      receivedWeight,
//                      receiveTopologyCondition);
//              System.out.println("receiveRoute:"+receivedAdvert.toString());
//
//              if (remoteRouteAdvert.isWithdrawn()) {
//                // Note this route was removed
//                ribDeltas.get(targetRib).remove(transformedIncomingRoute, Reason.WITHDRAW);
//                SortedSet<BgpRoute> b =
//                    _receivedBgpRoutes.get(transformedIncomingRoute.getNetwork());
//                if (b != null) {
//                  b.remove(transformedIncomingRoute);
//                }
//              } else {
//                // Merge into staging rib, note delta
//                System.out.println("Stage Route:");
//                ribDeltas
//                    .get(targetRib)
//                    .from(targetRib.mergeRouteGetDeltaStage(transformedIncomingRoute,true));
//                if (!remoteRouteAdvert.isWithdrawn()) {
//                  _receivedBgpAdvertisements.add(receivedAdvert);
//                }
//                _receivedBgpRoutes
//                    .computeIfAbsent(transformedIncomingRoute.getNetwork(), k -> new TreeSet<>())
//                    .add(transformedIncomingRoute);
//              }
//            }
//          }
//        }
//      }
//    }
//    // Return built deltas from RibDelta builders
//    System.out.println(".....................7.....................");
//    Map<BgpMultipathRib, RibDelta<BgpRoute>> builtDeltas = new IdentityHashMap<>();
//    ribDeltas.forEach(
//        (rib, deltaBuilder) -> {
//          RibDelta<BgpRoute> delta = deltaBuilder.build();
//          if (delta != null) {
//            builtDeltas.put(rib, delta);
//          }
//        });
//    System.out.println(".....................8.....................");
    return null;
  }

  /**
   * Propagate OSPF external routes from our neighbors by reading OSPF route "advertisements" from
   * our queues.
   *
   * @param allNodes map of all nodes, keyed by hostname
   * @param topology the Layer-3 network topology
   * @return a pair of {@link RibDelta}s, for Type1 and Type2 routes
   */
  @Nullable
  public Entry<RibDelta<OspfExternalType1Route>, RibDelta<OspfExternalType2Route>>
      propagateOspfExternalRoutes(final Map<String, Node> allNodes, Topology topology) {
    String node = _c.getHostname();
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return null;
    }
    int admin = RoutingProtocol.OSPF.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so OSPF won't produce anything
      return null;
    }

    RibDelta.Builder<OspfExternalType1Route> builderType1 =
        new RibDelta.Builder<>(_ospfExternalType1StagingRib);
    RibDelta.Builder<OspfExternalType2Route> builderType2 =
        new RibDelta.Builder<>(_ospfExternalType2StagingRib);

    for (Edge edge : edges) {
      if (!edge.getNode1().equals(node)) {
        continue;
      }
      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }
      String neighborName = edge.getNode2();
      Node neighbor = allNodes.get(neighborName);
      String neighborInterfaceName = edge.getInt2();
      OspfArea area = connectingInterface.getOspfArea();
      Configuration nc = neighbor.getConfiguration();
      Interface neighborInterface = nc.getInterfaces().get(neighborInterfaceName);
      String neighborVrfName = neighborInterface.getVrfName();
      VirtualRouter neighborVirtualRouter =
          allNodes.get(neighborName).getVirtualRouters().get(neighborVrfName);

      OspfArea neighborArea = neighborInterface.getOspfArea();
      if (connectingInterface.getOspfEnabled()
          && !connectingInterface.getOspfPassive()
          && neighborInterface.getOspfEnabled()
          && !neighborInterface.getOspfPassive()
          && area != null
          && neighborArea != null
          && area.getName().equals(neighborArea.getName())) {
        /*
         * We have an ospf neighbor relationship on this edge. So we
         * should add all ospf external type 1(2) routes from this
         * neighbor into our ospf external type 1(2) staging rib. For
         * type 1, the cost of the route increases each time. For type 2,
         * the cost remains constant, but we must keep track of cost to
         * advertiser as a tie-breaker.
         */
        long connectingInterfaceCost = connectingInterface.getOspfCost();
        long incrementalCost =
            proc.getMaxMetricTransitLinks() != null
                ? proc.getMaxMetricTransitLinks()
                : connectingInterfaceCost;

        Queue<RouteAdvertisement<OspfExternalRoute>> q =
            _ospfExternalIncomingRoutes.get(connectingInterface.getAddress().getPrefix());
        while (q.peek() != null) {
          RouteAdvertisement<OspfExternalRoute> routeAdvert = q.remove();
          OspfExternalRoute neighborRoute = routeAdvert.getRoute();
          boolean withdraw = routeAdvert.isWithdrawn();
          if (neighborRoute instanceof OspfExternalType1Route) {
            long oldArea = neighborRoute.getArea();
            long connectionArea = area.getName();
            long newArea;
            long baseMetric = neighborRoute.getMetric();
            long baseCostToAdvertiser = neighborRoute.getCostToAdvertiser();
            newArea = connectionArea;
            if (oldArea != OspfRoute.NO_AREA) {
              Long maxMetricSummaryNetworks =
                  neighborVirtualRouter._vrf.getOspfProcess().getMaxMetricSummaryNetworks();
              if (connectionArea != oldArea) {
                if (connectionArea != 0L && oldArea != 0L) {
                  continue;
                }
                if (maxMetricSummaryNetworks != null) {
                  baseMetric = maxMetricSummaryNetworks + neighborRoute.getLsaMetric();
                  baseCostToAdvertiser = maxMetricSummaryNetworks;
                }
              }
            }
            long newMetric = baseMetric + incrementalCost;
            long newCostToAdvertiser = baseCostToAdvertiser + incrementalCost;
            OspfExternalType1Route newRoute =
                new OspfExternalType1Route(
                    neighborRoute.getNetwork(),
                    neighborInterface.getAddress().getIp(),
                    admin,
                    newMetric,
                    neighborRoute.getLsaMetric(),
                    newArea,
                    newCostToAdvertiser,
                    neighborRoute.getAdvertiser());
            if (withdraw) {
              builderType1.remove(newRoute, Reason.WITHDRAW);
              SortedSet<OspfExternalType1Route> backups =
                  _receivedOspExternalType1Routes.get(newRoute.getNetwork());
              if (backups != null) {
                backups.remove(newRoute);
              }
            } else {
              builderType1.from(_ospfExternalType1StagingRib.mergeRouteGetDelta(newRoute));
              _receivedOspExternalType1Routes
                  .computeIfAbsent(newRoute.getNetwork(), k -> new TreeSet<>())
                  .add(newRoute);
            }

          } else if (neighborRoute instanceof OspfExternalType2Route) {
            long oldArea = neighborRoute.getArea();
            long connectionArea = area.getName();
            long newArea;
            long baseCostToAdvertiser = neighborRoute.getCostToAdvertiser();
            if (oldArea == OspfRoute.NO_AREA) {
              newArea = connectionArea;
            } else {
              newArea = oldArea;
              Long maxMetricSummaryNetworks =
                  neighborVirtualRouter._vrf.getOspfProcess().getMaxMetricSummaryNetworks();
              if (connectionArea != oldArea && maxMetricSummaryNetworks != null) {
                baseCostToAdvertiser = maxMetricSummaryNetworks;
              }
            }
            long newCostToAdvertiser = baseCostToAdvertiser + incrementalCost;
            OspfExternalType2Route newRoute =
                new OspfExternalType2Route(
                    neighborRoute.getNetwork(),
                    neighborInterface.getAddress().getIp(),
                    admin,
                    neighborRoute.getMetric(),
                    neighborRoute.getLsaMetric(),
                    newArea,
                    newCostToAdvertiser,
                    neighborRoute.getAdvertiser());
            if (withdraw) {
              builderType2.remove(newRoute, Reason.WITHDRAW);
              SortedSet<OspfExternalType2Route> backups =
                  _receivedOspExternalType2Routes.get(newRoute.getNetwork());
              if (backups != null) {
                backups.remove(newRoute);
              }
            } else {
              builderType2.from(_ospfExternalType2StagingRib.mergeRouteGetDelta(newRoute));
              _receivedOspExternalType2Routes
                  .computeIfAbsent(newRoute.getNetwork(), k -> new TreeSet<>())
                  .add(newRoute);
            }
          }
        }
      }
    }
    return new SimpleEntry<>(builderType1.build(), builderType2.build());
  }

  /**
   * Construct an OSPF Inter-Area route and put into our staging rib. Note, no route validity checks
   * are performed, (i.e., whether the route should even go into the staging rib). {@link
   * #propagateOspfInternalRoutesFromNeighbor} takes care of such logic.
   *
   * @param neighborRoute the route to propagate
   * @param nextHopIp nextHopIp for this route (the neighbor's IP)
   * @param incrementalCost OSPF cost of the interface from which this route came (added to route
   *     cost)
   * @param adminCost OSPF administrative distance
   * @param areaNum area number of the route
   * @return True if the route was added to the inter-area staging RIB
   */
  @VisibleForTesting
  boolean stageOspfInterAreaRoute(
      OspfInternalRoute neighborRoute,
      Long maxMetricSummaryNetworks,
      Ip nextHopIp,
      long incrementalCost,
      int adminCost,
      long areaNum) {
    long newCost;
    if (maxMetricSummaryNetworks != null) {
      newCost = maxMetricSummaryNetworks + incrementalCost;
    } else {
      newCost = neighborRoute.getMetric() + incrementalCost;
    }
    OspfInterAreaRoute newRoute =
        new OspfInterAreaRoute(neighborRoute.getNetwork(), nextHopIp, adminCost, newCost, areaNum);
    return _ospfInterAreaStagingRib.mergeRoute(newRoute);
  }

  private static boolean isOspfInterAreaFromInterAreaPropagationAllowed(
      long areaNum, Node neighbor, OspfInternalRoute neighborRoute, OspfArea neighborArea) {
    long neighborRouteAreaNum = neighborRoute.getArea();
    // May only propagate to or from area 0
    if (areaNum != neighborRouteAreaNum && areaNum != 0L && neighborRouteAreaNum != 0L) {
      return false;
    }
    Prefix neighborRouteNetwork = neighborRoute.getNetwork();
    String neighborSummaryFilterName = neighborArea.getSummaryFilter();
    boolean hasSummaryFilter = neighborSummaryFilterName != null;
    boolean allowed = !hasSummaryFilter;

    // If there is a summary filter, run the route through it
    if (hasSummaryFilter) {
      RouteFilterList neighborSummaryFilter =
          neighbor.getConfiguration().getRouteFilterLists().get(neighborSummaryFilterName);
      allowed = neighborSummaryFilter.permits(neighborRouteNetwork);
    }
    return allowed;
  }

  private static boolean isOspfInterAreaFromIntraAreaPropagationAllowed(
      long areaNum, Node neighbor, OspfInternalRoute neighborRoute, OspfArea neighborArea) {
    long neighborRouteAreaNum = neighborRoute.getArea();
    // May only propagate to or from area 0
    if (areaNum == neighborRouteAreaNum || (areaNum != 0L && neighborRouteAreaNum != 0L)) {
      return false;
    }
    Prefix neighborRouteNetwork = neighborRoute.getNetwork();
    String neighborSummaryFilterName = neighborArea.getSummaryFilter();
    boolean hasSummaryFilter = neighborSummaryFilterName != null;
    boolean allowed = !hasSummaryFilter;

    // If there is a summary filter, run the route through it
    if (hasSummaryFilter) {
      RouteFilterList neighborSummaryFilter =
          neighbor.getConfiguration().getRouteFilterLists().get(neighborSummaryFilterName);
      allowed = neighborSummaryFilter.permits(neighborRouteNetwork);
    }
    return allowed;
  }

  boolean propagateOspfInterAreaRouteFromIntraAreaRoute(
      Node neighbor,
      OspfIntraAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    return isOspfInterAreaFromIntraAreaPropagationAllowed(
            areaNum, neighbor, neighborRoute, neighborInterface.getOspfArea())
        && stageOspfInterAreaRoute(
            neighborRoute,
            neighborInterface.getVrf().getOspfProcess().getMaxMetricSummaryNetworks(),
            neighborInterface.getAddress().getIp(),
            incrementalCost,
            adminCost,
            areaNum);
  }

  /**
   * Propagate OSPF Internal routes from a single neighbor.
   *
   * @param proc The receiving OSPF process
   * @param neighbor the neighbor
   * @param connectingInterface interface on which we are connected to the neighbor
   * @param neighborInterface interface that the neighbor uses to connect to us
   * @param adminCost route administrative distance
   * @return true if new routes have been added to our staging RIB
   */
  boolean propagateOspfInternalRoutesFromNeighbor(
      OspfProcess proc,
      Node neighbor,
      Interface connectingInterface,
      Interface neighborInterface,
      int adminCost) {
    OspfArea area = connectingInterface.getOspfArea();
    OspfArea neighborArea = neighborInterface.getOspfArea();
    // Ensure that the link (i.e., both interfaces) has OSPF enabled and OSPF areas are set
    if (!connectingInterface.getOspfEnabled()
        || connectingInterface.getOspfPassive()
        || !neighborInterface.getOspfEnabled()
        || neighborInterface.getOspfPassive()
        || area == null
        || neighborArea == null
        || !area.getName().equals(neighborArea.getName())) {
      return false;
    }
    /*
     * An OSPF neighbor relationship exists on this edge. So we examine all intra- and inter-area
     * routes belonging to the neighbor to see what should be propagated to this router. We add the
     * incremental cost associated with our settings and the connecting interface, and use the
     * neighborInterface's address as the next hop ip.
     */
    int connectingInterfaceCost = connectingInterface.getOspfCost();
    long incrementalCost =
        proc.getMaxMetricTransitLinks() != null
            ? proc.getMaxMetricTransitLinks()
            : connectingInterfaceCost;
    Long areaNum = area.getName();
    VirtualRouter neighborVirtualRouter =
        neighbor.getVirtualRouters().get(neighborInterface.getVrfName());
    boolean changed = false;
    for (OspfIntraAreaRoute neighborRoute : neighborVirtualRouter._ospfIntraAreaRib.getRoutes()) {
      changed |=
          propagateOspfIntraAreaRoute(
              neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
      changed |=
          propagateOspfInterAreaRouteFromIntraAreaRoute(
              neighbor, neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
    }
    for (OspfInterAreaRoute neighborRoute : neighborVirtualRouter._ospfInterAreaRib.getRoutes()) {
      changed |=
          propagateOspfInterAreaRouteFromInterAreaRoute(
              neighbor, neighborRoute, incrementalCost, neighborInterface, adminCost, areaNum);
    }
    return changed;
  }

  boolean propagateOspfInterAreaRouteFromInterAreaRoute(
      Node neighbor,
      OspfInterAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    return isOspfInterAreaFromInterAreaPropagationAllowed(
            areaNum, neighbor, neighborRoute, neighborInterface.getOspfArea())
        && stageOspfInterAreaRoute(
            neighborRoute,
            neighborInterface.getVrf().getOspfProcess().getMaxMetricSummaryNetworks(),
            neighborInterface.getAddress().getIp(),
            incrementalCost,
            adminCost,
            areaNum);
  }

  boolean propagateOspfIntraAreaRoute(
      OspfIntraAreaRoute neighborRoute,
      long incrementalCost,
      Interface neighborInterface,
      int adminCost,
      long areaNum) {
    long newCost = neighborRoute.getMetric() + incrementalCost;
    Ip nextHopIp = neighborInterface.getAddress().getIp();
    OspfIntraAreaRoute newRoute =
        new OspfIntraAreaRoute(neighborRoute.getNetwork(), nextHopIp, adminCost, newCost, areaNum);
    return neighborRoute.getArea() == areaNum && (_ospfIntraAreaStagingRib.mergeRoute(newRoute));
  }

  /**
   * Propagate OSPF internal routes from every valid OSPF neighbor
   *
   * @param nodes mapping of node names to instances.
   * @param topology network topology
   * @return true if new routes have been added to the staging RIB
   */
  boolean propagateOspfInternalRoutes(Map<String, Node> nodes, Topology topology) {
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return false; // nothing to do
    }

    boolean changed = false;
    String node = _c.getHostname();

    // Default OSPF admin cost for constructing new routes
    int adminCost = RoutingProtocol.OSPF.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so OSPF won't produce anything
      return false;
    }

    for (Edge edge : edges) {
      if (!edge.getNode1().equals(node)) {
        continue;
      }

      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }

      String neighborName = edge.getNode2();
      Node neighbor = nodes.get(neighborName);
      Interface neighborInterface = neighbor.getConfiguration().getInterfaces().get(edge.getInt2());

      changed |=
          propagateOspfInternalRoutesFromNeighbor(
              proc, neighbor, connectingInterface, neighborInterface, adminCost);
    }
    return changed;
  }

  /**
   * Process RIP routes from our neighbors.
   *
   * @param nodes Mapping of node names to Node instances
   * @param topology The network topology
   * @return True if the rib has changed as a result of route propagation
   */
  boolean propagateRipInternalRoutes(Map<String, Node> nodes, Topology topology) {
    boolean changed = false;

    // No rip process, nothing to do
    if (_vrf.getRipProcess() == null) {
      return false;
    }

    String node = _c.getHostname();
    int admin = RoutingProtocol.RIP.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so RIP won't produce anything
      return false;
    }

    for (Edge edge : edges) {
      // Do not accept routes from ourselves
      if (!edge.getNode1().equals(node)) {
        continue;
      }

      // Get interface
      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }

      // Get the neighbor and its interface + VRF
      String neighborName = edge.getNode2();
      Node neighbor = nodes.get(neighborName);
      String neighborInterfaceName = edge.getInt2();
      Interface neighborInterface =
          neighbor.getConfiguration().getInterfaces().get(neighborInterfaceName);
      String neighborVrfName = neighborInterface.getVrfName();
      VirtualRouter neighborVirtualRouter =
          nodes.get(neighborName).getVirtualRouters().get(neighborVrfName);

      if (connectingInterface.getRipEnabled()
          && !connectingInterface.getRipPassive()
          && neighborInterface.getRipEnabled()
          && !neighborInterface.getRipPassive()) {
        /*
         * We have a RIP neighbor relationship on this edge. So we should add all RIP routes
         * from this neighbor into our RIP internal staging rib, adding the incremental cost
         * (?), and using the neighborInterface's address as the next hop ip
         */
        for (RipInternalRoute neighborRoute : neighborVirtualRouter._ripInternalRib.getRoutes()) {
          long newCost = neighborRoute.getMetric() + RipProcess.DEFAULT_RIP_COST;
          Ip nextHopIp = neighborInterface.getAddress().getIp();
          RipInternalRoute newRoute =
              new RipInternalRoute(neighborRoute.getNetwork(), nextHopIp, admin, newCost);
          if (_ripInternalStagingRib.mergeRouteGetDelta(newRoute) != null) {
            changed = true;
          }
        }
      }
    }
    return changed;
  }

  /**
   * Convert a given RibDelta into {@link RouteAdvertisement} objects and enqueue them onto a given
   * queue.
   *
   * @param queue the message queue
   * @param delta {@link RibDelta} representing changes.
   */
  @VisibleForTesting
  static <R extends AbstractRoute, D extends R> void queueDelta(
      Queue<RouteAdvertisement<R>> queue, @Nullable RibDelta<D> delta) {
    if (delta == null) {
      // Nothing to do
      return;
    }
    for (RouteAdvertisement<D> r : delta.getActions()) {
      // REPLACE does not make sense across routers, update with WITHDRAW
      Reason reason = r.getReason() == Reason.REPLACE ? Reason.WITHDRAW : r.getReason();
      queue.add(new RouteAdvertisement<>(r.getRoute(), r.isWithdrawn(), reason));
    }
  }


  /**
   * 将要发送的路由增加到邻居队列里
   * Queue advertised BGP routes to all BGP neighbors.
   *
   * @param bgpBestPathDelta a {@link RibDelta} indicating what changed in the {@link
   *     #_bgpBestPathRib}
   * @param ebgpBestPathDelta {@link RibDelta} indicating what changed in the {@link
   *     #_bgpBestPathRib}
   * @param bgpMultiPathDelta a {@link RibDelta} indicating what changed in the {@link
   *     #_bgpMultipathRib}
   * @param mainDelta a {@link RibDelta} indicating what changed in the {@link #_mainRib}
   * @param allNodes map of all nodes in the network, keyed by hostname
   * @param bgpTopology the bgp peering relationships
   */
  private void queueOutgoingBgpRoutes(
      @Nullable RibDelta<BgpRoute> bgpBestPathDelta,
      RibDelta<BgpRoute> ebgpBestPathDelta,
      @Nullable RibDelta<BgpRoute> bgpMultiPathDelta,
      @Nullable RibDelta<AbstractRoute> mainDelta,
      final Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology) {
    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      if (!bgpTopology.nodes().contains(neighbor)) {
        continue;
      }
      for (BgpNeighbor remoteNeighbor : bgpTopology.adjacentNodes(neighbor)) {
        // Queue for this neighbor
        // 查看这个BGP邻居实例所在的vrf
        VirtualRouter remoteVirtualRouter = getRemoteBgpNeighborVR(remoteNeighbor, allNodes);
        if (remoteVirtualRouter == null) {
          continue;
        }

        Set<BgpSession> sessions = bgpTopology.edgesConnecting(neighbor, remoteNeighbor);
        if (sessions.isEmpty()) {
          sessions = bgpTopology.edgesConnecting(remoteNeighbor, neighbor);
          if (sessions.isEmpty()) {
            continue;
          }
        }
        BgpSession session = sessions.iterator().next();
        //获取这条边的消息队列
        Queue<RouteAdvertisement<AbstractRoute>> queue =
            remoteVirtualRouter._bgpIncomingRoutes.get(
                UndirectedBgpSession.from(neighbor, remoteNeighbor));

        Builder<AbstractRoute> finalBuilder = new Builder<>(null);

        // Definitely queue mainRib updates
        finalBuilder.from(mainDelta);
        // These knobs control which additional BGP routes get advertised
        if (session.getAdvertiseExternal()) {
          /*
           * Advertise external ensures that even if we withdrew an external route from the RIB
           */
          finalBuilder.from(ebgpBestPathDelta);
        }
        if (session.getAdvertiseInactive()) {
          /*
           * In case BGP routes were deleted from the main RIB
           * (e.g., preempted by a better IGP route)
           * and advertiseInactive is true, re-add inactive BGP routes from the BGP best-path RIB.
           * If the BGP routes are already active, this will have no effect.
           */
          if (mainDelta != null) {
            for (Prefix p : mainDelta.getPrefixes()) {
              if (_bgpBestPathRib.getRoutes(p) == null) {
                continue;
              }
              finalBuilder.add(_bgpBestPathRib.getRoutes(p));
            }
          }
        }
        if (session.getAdditionalPaths()) {
          finalBuilder.from(bgpMultiPathDelta);
        }
        queueDelta(queue, finalBuilder.build());
      }
    }
  }

  /**
   * Send out OSPF External route updates to our neighbors
   *
   * @param type1delta A {@link RibDelta} containing diffs with respect to OSPF Type1 external
   *     routes
   * @param type2delta A {@link RibDelta} containing diffs with respect to OSPF Type2 external
   *     routes
   */
  private void queueOutgoingOspfExternalRoutes(
      @Nullable RibDelta<OspfExternalType1Route> type1delta,
      @Nullable RibDelta<OspfExternalType2Route> type2delta) {
    if (_vrf.getOspfProcess() == null) {
      return;
    }
    if (_ospfNeighbors != null) {
      _ospfNeighbors.forEach(
          (key, remoteVR) -> {
            // Get remote neighbor's queue by prefix
            Queue<RouteAdvertisement<OspfExternalRoute>> q =
                remoteVR._ospfExternalIncomingRoutes.get(key);
            queueDelta(q, type1delta);
            queueDelta(q, type2delta);
          });
    }
  }

  /**
   * Propagate BGP routes received from neighbours into the appropriate RIBs. As the propagation is
   * happening, queue appropriate outgoing messages to neighbors as well.
   *
   * @param multipathEbgp whether or not EBGP is multipath
   * @param multipathIbgp whether or not IBGP is multipath
   * @param stagingDeltas a map of RIB to corresponding delta. Keys are expected to contain {@link
   *     #_ebgpStagingRib} and {@link #_ibgpStagingRib}
   * @param bgpTopology the bgp peering relationships
   */
  void finalizeBgpRoutesAndQueueOutgoingMessages(//每个节点,真正的路由传播函数,收集路由并向外传播,同时更新自己的路由表
      boolean multipathEbgp,
      boolean multipathIbgp,
      Map<BgpMultipathRib, RibDelta<BgpRoute>> stagingDeltas,
      final Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology) {

    RibDelta<BgpRoute> ebgpStagingDelta = stagingDeltas.get(_ebgpStagingRib);
    RibDelta<BgpRoute> ibgpStagingDelta = stagingDeltas.get(_ibgpStagingRib);

    Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> e;
    RibDelta<BgpRoute> ebgpBestPathDelta;
    if (multipathEbgp) {
      e = syncBgpDeltaPropagation(_bgpBestPathRib, _bgpMultipathRib, ebgpStagingDelta);
      ebgpBestPathDelta = e.getKey();
      _bgpBestPathDeltaBuilder.from(e.getKey());
      _bgpMultiPathDeltaBuilder.from(e.getValue());
    } else {
      //路由导入这部分是需要更改的
      ebgpBestPathDelta = importRibDelta(_bgpBestPathRib, ebgpStagingDelta);
      _bgpBestPathDeltaBuilder.from(ebgpBestPathDelta);
      _bgpMultiPathDeltaBuilder.from(ebgpBestPathDelta);
    }

    if (multipathIbgp) {
      e = syncBgpDeltaPropagation(_bgpBestPathRib, _bgpMultipathRib, ibgpStagingDelta);
      _bgpBestPathDeltaBuilder.from(e.getKey());
      _bgpMultiPathDeltaBuilder.from(e.getValue());
    } else {
      RibDelta<BgpRoute> ibgpBestPathDelta = importRibDelta(_bgpBestPathRib, ibgpStagingDelta);
      _bgpBestPathDeltaBuilder.from(ibgpBestPathDelta);
      _bgpMultiPathDeltaBuilder.from(ibgpBestPathDelta);
    }

    _bgpMultiPathDeltaBuilder.build();
    //importRibDelta(_mainRib, _bgpMultiPathDeltaBuilder.build());

    _mainRibRouteDeltaBuiler.from(importRibDeltaWithTopology(_mainRib, _bgpMultiPathDeltaBuilder.build()));

    //将要发送的路由更新消息设定到节点的特定消息队列当中
    queueOutgoingBgpRoutes(
        _bgpBestPathDeltaBuilder.build(),
        ebgpBestPathDelta,
        _bgpMultiPathDeltaBuilder.build(),
        _mainRibRouteDeltaBuiler.build(),
        allNodes,
        bgpTopology);
  }

  static Entry<RibDelta<BgpRoute>, RibDelta<BgpRoute>> syncBgpDeltaPropagation(
      BgpBestPathRib bestPathRib, BgpMultipathRib multiPathRib, RibDelta<BgpRoute> delta) {

    // Build our fist attempt at best path delta
    Builder<BgpRoute> bestDeltaBuldiler = new Builder<>(bestPathRib);
    bestDeltaBuldiler.from(importRibDelta(bestPathRib, delta));//将之前变化的路由增加到最优路由表当中去
    RibDelta<BgpRoute> bestDelta = bestDeltaBuldiler.build();

    Builder<BgpRoute> mpBuilder = new Builder<>(multiPathRib);

    mpBuilder.from(importRibDelta(multiPathRib, bestDelta));
    if (bestDelta != null) {
      /*
       * Handle mods to the best path RIB
       */
      // 删除或者修正最佳路由表
      for (Prefix p : bestDelta.getPrefixes()) {
        List<RouteAdvertisement<BgpRoute>> actions = bestDelta.getActions(p);
        if (actions != null) {
          if (actions
              .stream()
              .map(RouteAdvertisement::getReason)
              .anyMatch(Predicate.isEqual(Reason.REPLACE))) {
            /*
             * Clear routes for prefixes where best path RIB was modified, because
             * a better route was chosen, and whatever we had in multipathRib is now invalid
             */
            mpBuilder.from(multiPathRib.clearRoutes(p));
          } else if (actions
              .stream()
              .map(RouteAdvertisement::getReason)
              .anyMatch(Predicate.isEqual(Reason.WITHDRAW))) {
            /*
             * Routes for that prefix were withdrawn. See if we have anything in the multipath RIB
             * to fix it.
             * Create a fake delta, let the routes fight it out for best path in the merge process
             */
            RibDelta<BgpRoute> fakeDelta =
                new Builder<BgpRoute>(null).add(multiPathRib.getRoutes(p)).build();
            bestDeltaBuldiler.from(importRibDelta(bestPathRib, fakeDelta));
          }
        }
      }
    }
    // 更新最佳路径
    // Set the (possibly updated) best path delta
    bestDelta = bestDeltaBuldiler.build();
    // Update best paths
    multiPathRib.setBestAsPaths(bestPathRib.getBestAsPaths());
    // Only iterate over valid prefixes (ones in best-path RIB) and see if anything should go into
    // multi-path RIB
    for (Prefix p : bestPathRib.getPrefixes()) {
      mpBuilder.from(importRibDelta(multiPathRib, delta, p));
    }
    return new SimpleImmutableEntry<>(bestDelta, mpBuilder.build());
  }

  /**
   * Merges staged OSPF external routes into the "real" OSPF-external RIBs
   *
   * @param type1Delta a {@link RibDelta} indicating changes to be made to {@link
   *     #_ospfExternalType1Rib}
   * @param type2Delta a {@link RibDelta} indicating changes to be made to {@link
   *     #_ospfExternalType2Rib}
   */
  boolean unstageOspfExternalRoutes(
      RibDelta<OspfExternalType1Route> type1Delta, RibDelta<OspfExternalType2Route> type2Delta) {
    RibDelta<OspfExternalType1Route> d1 = importRibDelta(_ospfExternalType1Rib, type1Delta);
    RibDelta<OspfExternalType2Route> d2 = importRibDelta(_ospfExternalType2Rib, type2Delta);
    queueOutgoingOspfExternalRoutes(d1, d2);
    Builder<OspfRoute> ospfDeltaBuilder = new Builder<>(_ospfRib);
    ospfDeltaBuilder.from(importRibDelta(_ospfRib, d1));
    ospfDeltaBuilder.from(importRibDelta(_ospfRib, d2));
    _mainRibRouteDeltaBuiler.from(importRibDelta(_mainRib, ospfDeltaBuilder.build()));
    return d1 != null || d2 != null;
  }

  /** Merges staged OSPF internal routes into the "real" OSPF-internal RIBs */
  void unstageOspfInternalRoutes() {
    importRib(_ospfIntraAreaRib, _ospfIntraAreaStagingRib);
    importRib(_ospfInterAreaRib, _ospfInterAreaStagingRib);
  }

  /** Merges staged RIP routes into the "real" RIP RIB */
  void unstageRipInternalRoutes() {
    importRib(_ripInternalRib, _ripInternalStagingRib);
  }

  /** Re-initialize RIBs (at the start of each iteration). */
  void reinitForNewIteration(final Map<String, Node> allNodes) {
    _mainRibRouteDeltaBuiler = new Builder<>(_mainRib);
    _bgpBestPathDeltaBuilder = new RibDelta.Builder<>(_bgpBestPathRib);
    _bgpMultiPathDeltaBuilder = new RibDelta.Builder<>(_bgpMultipathRib);
    _ospfExternalDeltaBuiler = new RibDelta.Builder<>(null);

    /*
     * RIBs not read from can just be re-initialized
     */
    _ospfRib = new OspfRib();
    _ripRib = new RipRib();

    /*
     * Staging RIBs can also be re-initialized
     */
    MultipathEquivalentAsPathMatchMode mpTieBreaker =
        _vrf.getBgpProcess() == null
            ? EXACT_PATH
            : _vrf.getBgpProcess().getMultipathEquivalentAsPathMatchMode();
    _ebgpStagingRib = new BgpMultipathRib(mpTieBreaker);
    _ibgpStagingRib = new BgpMultipathRib(mpTieBreaker);
    _ospfExternalType1StagingRib = new OspfExternalType1Rib(getHostname(), null);
    _ospfExternalType2StagingRib = new OspfExternalType2Rib(getHostname(), null);

    /*
     * Add routes that cannot change (does not affect below computation)
     */
    _mainRibRouteDeltaBuiler.from(importRib(_mainRib, _independentRib));

    /*
     * Re-add independent OSPF routes to ospfRib for tie-breaking
     */
    importRib(_ospfRib, _ospfIntraAreaRib);
    importRib(_ospfRib, _ospfInterAreaRib);
    /*
     * Re-add independent RIP routes to ripRib for tie-breaking
     */
    importRib(_ripRib, _ripInternalRib);
  }

  /**
   * Merge intra/inter OSPF RIBs into a general OSPF RIB, then merge that into the independent RIB
   */
  void importOspfInternalRoutes() {
    importRib(_ospfRib, _ospfIntraAreaRib);
    importRib(_ospfRib, _ospfInterAreaRib);
    importRib(_independentRib, _ospfRib);
  }

  /**
   * Check if RIBs that contribute to the dataplane "dependent routes" computation have any routes
   * that still need to be merged. I.e., if this method returns true, we cannot converge yet.
   *
   * @return true if there are any routes remaining, in need of merging in to the RIBs
   */
  boolean hasOutstandingRoutes() {
    return _ospfExternalDeltaBuiler.build() != null
        || _mainRibRouteDeltaBuiler.build() != null
        || _bgpBestPathDeltaBuilder.build() != null
        || _bgpMultiPathDeltaBuilder.build() != null;
  }


  //LXZ用来判断消息队列是否为空
  public boolean symHasProcessedAllMessages(Network<BgpNeighbor, BgpSession> bgpTopology) {
    // Check the BGP message queues
    if (_vrf.getBgpProcess() != null) {
      for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
        if (!bgpTopology.nodes().contains(neighbor)) {
          continue;
        }
        for (BgpSession session : bgpTopology.incidentEdges(neighbor)) {
          Queue<SymRouteAdvertisement<SymBgpRoute>> queue =
              _symBgpIncomingRoutes.get(UndirectedBgpSession.from(session));
          if (!queue.isEmpty()) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Check if this router has processed all its incoming BGP messages (i.e., all router queues are
   * empty)
   *
   * @return true if all queues are empty
   */
  public boolean hasProcessedAllMessages(Network<BgpNeighbor, BgpSession> bgpTopology) {
    // Check the BGP message queues
    if (_vrf.getBgpProcess() != null) {
      for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
        if (!bgpTopology.nodes().contains(neighbor)) {
          continue;
        }
        for (BgpSession session : bgpTopology.incidentEdges(neighbor)) {
          Queue<RouteAdvertisement<AbstractRoute>> queue =
              _bgpIncomingRoutes.get(UndirectedBgpSession.from(session));
          if (!queue.isEmpty()) {
            return false;
          }
        }
      }
    }
    // Check the OSPF external message queues
    if (_vrf.getOspfProcess() != null) {
      for (Queue<RouteAdvertisement<OspfExternalRoute>> queue :
          _ospfExternalIncomingRoutes.values()) {
        if (!queue.isEmpty()) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Queues initial round of outgoing BGP messages based on the state of the RIBs prior to any data
   * plane iterations.
   *
   * @param allNodes all nodes in the network
   */
  public void queueInitialBgpMessages(
      final Network<BgpNeighbor, BgpSession> bgpTopology, final Map<String, Node> allNodes) {
    if (_vrf.getBgpProcess() == null) {
      // nothing to do
      return;
    }
    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      if (!bgpTopology.nodes().contains(neighbor)) {
        continue;
      }
      for (BgpSession session : bgpTopology.incidentEdges(neighbor)) {
        newBgpSessionEstablishedHook(UndirectedBgpSession.from(session), allNodes);
      }
    }
  }

  public void symQueueInitialBgpMessages(
      final Network<BgpNeighbor, BgpSession> bgpTopology, final Map<String, Node> allNodes) {
    if (_vrf.getBgpProcess() == null) {
      // nothing to do
      return;
    }
    for (BgpNeighbor neighbor : _vrf.getBgpProcess().getNeighbors().values()) {
      if (!bgpTopology.nodes().contains(neighbor)) {
        continue;
      }
      for(BgpNeighbor remoteNeighbor:bgpTopology.adjacentNodes(neighbor))
      {
        VirtualRouter reomteVirtualRouter=getRemoteBgpNeighborVR(remoteNeighbor,allNodes);
        if(reomteVirtualRouter==null)
        {
          continue;
        }
        UndirectedBgpSession session=UndirectedBgpSession.from(neighbor,remoteNeighbor);
        symNewBgpSessionEstablishedHook(session, allNodes);
      }
//      for (BgpSession session : bgpTopology.incidentEdges(neighbor)) {
//        symNewBgpSessionEstablishedHook(UndirectedBgpSession.from(session), allNodes);
//      }
    }
  }

  private void symNewBgpSessionEstablishedHook(
      UndirectedBgpSession session, Map<String, Node> allNodes) {
    BgpNeighbor remoteNeighbor =
        !_vrf.getBgpProcess().getNeighbors().values().contains(session.getFirst())
            ? session.getFirst()
            : session.getSecond();
    VirtualRouter remoteVr = getRemoteBgpNeighborVR(remoteNeighbor, allNodes);
    if (remoteVr == null) {
      return;
    }

    // Call this on the neighbor's VR!
    //remoteVr.symEnqueBgpMessages(session, _symBgpRib.getRoutes());
    // remoteVr.symSimplifiedEnqueBgpMessages(session, _symBgpRib.getSimplifiedRoutes());
  }

  private void symSimplifiedEnqueBgpMessages(
      final UndirectedBgpSession session, Map<Integer,List<SymBgpRoute>> routes) {
    /*
     * Add route advertisement.
     * Note: export filtering is done in the processBgpMessages, here we queue everything from the
     * main RIB
     */
    for(Integer prefix:routes.keySet())
    {
      List<SymBgpRoute> actualRoute=routes.get(prefix);
      List<SymBgpRoute.Builder> updatedRouteBuilder=new ArrayList<>();
      for(int i=0;i<actualRoute.size();i++)
      {
        updatedRouteBuilder.add(new SymBgpRoute.Builder(actualRoute.get(i)));
      }
      if(updatedRouteBuilder.size()==0)
      {
        continue;
      }
      List<SymRouteAdvertisement<SymBgpRoute>> advertisementList=convertAdvert(updatedRouteBuilder);
      for(int i=0;i<advertisementList.size();i++)
      {
        _symBgpIncomingRoutes.get(session).add(advertisementList.get(i));
      }
    }
    //    _symBgpIncomingRoutes
    //        .get(session)
    //        .addAll(
    //            routes
    //                .stream()
    //                .map(RouteAdvertisement<AbstractRoute>::new)
    //                .collect(ImmutableSet.toImmutableSet()));
  }

  private void symEnqueBgpMessages(
      final UndirectedBgpSession session, Map<SymPrefixList,List<SymBgpRoute>> routes) {
    /*
     * Add route advertisement.
     * Note: export filtering is done in the processBgpMessages, here we queue everything from the
     * main RIB
     */
    for(SymPrefixList prefix:routes.keySet())
    {
      List<SymBgpRoute> actualRoute=routes.get(prefix);
      List<SymBgpRoute.Builder> updatedRouteBuilder=new ArrayList<>();
      for(int i=0;i<actualRoute.size();i++)
      {
        updatedRouteBuilder.add(new SymBgpRoute.Builder(actualRoute.get(i)));
      }
      if(updatedRouteBuilder.size()==0)
      {
        continue;
      }
      List<SymRouteAdvertisement<SymBgpRoute>> advertisementList=convertAdvert(updatedRouteBuilder);
      for(int i=0;i<advertisementList.size();i++)
      {
        _symBgpIncomingRoutes.get(session).add(advertisementList.get(i));
      }
    }
//    _symBgpIncomingRoutes
//        .get(session)
//        .addAll(
//            routes
//                .stream()
//                .map(RouteAdvertisement<AbstractRoute>::new)
//                .collect(ImmutableSet.toImmutableSet()));
  }

  private void enqueBgpMessages(
      final UndirectedBgpSession session, final Set<AbstractRoute> routes) {
    /*
     * Add route advertisement.
     * Note: export filtering is done in the processBgpMessages, here we queue everything from the
     * main RIB
     */
    _bgpIncomingRoutes
        .get(session)
        .addAll(
            routes
                .stream()
                .map(RouteAdvertisement<AbstractRoute>::new)
                .collect(ImmutableSet.toImmutableSet()));
  }

  private void newBgpSessionEstablishedHook(
      UndirectedBgpSession session, Map<String, Node> allNodes) {
    BgpNeighbor remoteNeighbor =
        !_vrf.getBgpProcess().getNeighbors().values().contains(session.getFirst())
            ? session.getFirst()
            : session.getSecond();
    VirtualRouter remoteVr = getRemoteBgpNeighborVR(remoteNeighbor, allNodes);
    if (remoteVr == null) {
      return;
    }

    // Call this on the neighbor's VR!
    remoteVr.enqueBgpMessages(session, _mainRib.getRoutes());
  }

  /**
   * Lookup the VirtualRouter owner of a remote BGP neighbor.
   *
   * @param remoteBgpNeighbor the {@link BgpNeighbor} that belongs to a different {@link
   *     VirtualRouter}
   * @param allNodes map containing all network nodes
   * @return a {@link VirtualRouter} that owns the {@code neighbor.getRemoteBgpNeighbor()}
   */
  @Nullable
  @VisibleForTesting
  static VirtualRouter getRemoteBgpNeighborVR(
      @Nonnull BgpNeighbor remoteBgpNeighbor, final Map<String, Node> allNodes) {
    String remoteHostname = remoteBgpNeighbor.getOwner().getHostname();
    String remoteVrfName = remoteBgpNeighbor.getVrf();
    return allNodes.get(remoteHostname).getVirtualRouters().get(remoteVrfName);
  }

  /**
   * This method aids in de-serialization of {@link VirtualRouter}
   *
   * @param in input stream to de-serialize from
   * @throws IOException if processing the stream fails
   * @throws ClassNotFoundException if deserialization cannot complete due to an unknown class
   */
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _bgpIncomingRoutes = ImmutableSortedMap.of();
  }

  /**
   * Compute our OSPF neighbors.
   *
   * @param allNodes map of all network nodes, keyed by hostname
   * @param topology the Layer-3 network topology
   * @return A sorted map of neighbor prefixes to their names
   */
  @Nullable
  SortedMap<Prefix, VirtualRouter> getOspfNeighbors(
      final Map<String, Node> allNodes, Topology topology) {
    // Check we have ospf process
    OspfProcess proc = _vrf.getOspfProcess();
    if (proc == null) {
      return null;
    }

    String node = _c.getHostname();
    SortedSet<Edge> edges = topology.getNodeEdges().get(node);
    if (edges == null) {
      // there are no edges, so OSPF won't produce anything
      return null;
    }

    SortedMap<Prefix, VirtualRouter> neighbors = new TreeMap<>();
    for (Edge edge : edges) {
      if (!edge.getNode1().equals(node)) {
        continue;
      }
      String connectingInterfaceName = edge.getInt1();
      Interface connectingInterface = _vrf.getInterfaces().get(connectingInterfaceName);
      if (connectingInterface == null) {
        // wrong vrf, so skip
        continue;
      }
      String neighborName = edge.getNode2();
      Node neighbor = allNodes.get(neighborName);
      String neighborInterfaceName = edge.getInt2();
      OspfArea area = connectingInterface.getOspfArea();
      Configuration nc = neighbor.getConfiguration();
      Interface neighborInterface = nc.getInterfaces().get(neighborInterfaceName);
      String neighborVrfName = neighborInterface.getVrfName();
      VirtualRouter neighborVirtualRouter =
          allNodes.get(neighborName).getVirtualRouters().get(neighborVrfName);

      OspfArea neighborArea = neighborInterface.getOspfArea();
      if (connectingInterface.getOspfEnabled()
          && !connectingInterface.getOspfPassive()
          && neighborInterface.getOspfEnabled()
          && !neighborInterface.getOspfPassive()
          && area != null
          && neighborArea != null
          && area.getName().equals(neighborArea.getName())) {
        neighbors.put(connectingInterface.getAddress().getPrefix(), neighborVirtualRouter);
      }
    }

    return ImmutableSortedMap.copyOf(neighbors);
  }

  public Configuration getConfiguration() {
    return _c;
  }

  public ConnectedRib getConnectedRib() {
    return _connectedRib;
  }

  public Fib getFib() {
    return _fib;
  }

  public Rib getMainRib() {
    return _mainRib;
  }

  public BgpBestPathRib getBgpBestPathRib() {
    return _bgpBestPathRib;
  }

  /** Convenience method to get the VirtualRouter's hostname */
  String getHostname() {
    return _c.getHostname();
  }

  /**
   * Compute the "hashcode" of this router for the iBDP purposes. The hashcode is computed from the
   * following datastructures:
   *
   * <ul>
   *   <li>"external" RIBs ({@link #_mainRib}, {@link #_ospfExternalType1Rib}, {@link
   *       #_ospfExternalType2Rib}
   *   <li>message queues ({@link #_bgpIncomingRoutes} and {@link #_ospfExternalIncomingRoutes})
   * </ul>
   *
   * @return integer hashcode
   */
  int computeIterationHashCode() {
    if(_vrf.getBgpProcess()!=null)
    {
//      return _symBgpRib.getSimplifiedRoutes().hashCode();
      return _symBgpRib.hashCode();
    }else if(_vrf.getIsisProcess()!=null)
    {
      return _isisRib.getRoutes().hashCode()+_isisMultiRib.getRouteNumber();
    }else{
      return 0;
    }
  }

  int computeIterationMessageQueueHashCode() {
    int hashcode=1;
    if(_vrf.getBgpProcess()!=null) {
      for(UndirectedBgpSession session:_symBgpIncomingRoutes.keySet())
      {
        hashcode = hashcode*32 + session.hashCode();
        for(SymRouteAdvertisement<SymBgpRoute> route:_symBgpIncomingRoutes.get(session))
        {
          hashcode = hashcode*32 + route.hashCode();
        }
      }
      return hashcode;
    }
    return 0;
  }

  public SymPrefixList prefixToSymPrefix(String prefix,BDDFactory factory)
  {

    int prefixIpLen=Integer.parseInt(prefix.substring(prefix.indexOf("/")+1,prefix.length()));
    String prefixIp[]=prefix.substring(0,prefix.indexOf("/")).split("\\.");
    String targetIp="";
    for(int i=0;i<prefixIp.length;i++)
    {
      Integer subIp=Integer.parseInt(prefixIp[i]);
      targetIp=targetIp+String.format("%8s",Integer.toBinaryString(subIp)).replace(" ","0");
    }

    BDD prefixActualIp=factory.one();
    for(int i=0;i<prefixIpLen;i++)
    {
      if(targetIp.charAt(i)=='1')
      {
        prefixActualIp = prefixActualIp.and( factory.ithVar(i) );
      }else{
        prefixActualIp = prefixActualIp.and( factory.nithVar(i) );
      }
    }

    StringBuilder prefixMask=new StringBuilder("00000000000000000000000000000000");
    prefixMask.replace(prefixIpLen-1,prefixIpLen,"1");
    Long prefixActualLen=Long.parseLong(prefixMask.toString(),2);
    SymPrefixList answer=new SymPrefixList(prefixActualIp,prefixActualLen);
    return answer;
  }

  public void initIsisQueues(IsisTopology isisTopology) {
    Network<IsisNode, IsisEdge> network = isisTopology.getNetwork();
    // Initialize message queues for each IS-IS circuit
    if (_vrf.getIsisProcess() == null) {
      _isisIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _isisIncomingRoutes =
          _c.getAllInterfaces().keySet().stream()
              .map(ifaceName -> new IsisNode(_c.getHostname(), ifaceName))
              .filter(network.nodes()::contains)
              .flatMap(n -> network.inEdges(n).stream())
              .collect(
                  toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    }
  }

  public void initIsisMultiQueuesAndFactory(IsisTopology isisTopology) {
    _isisMultiRib.setEdgeBddFactory(_bddFactory);
    Network<IsisNode, IsisEdge> network = isisTopology.getNetwork();
    // Initialize message queues for each IS-IS circuit
    if (_vrf.getIsisProcess() == null) {
      _isisMultiIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _isisMultiIncomingRoutes =
          _c.getAllInterfaces().keySet().stream()
              .map(ifaceName -> new IsisNode(_c.getHostname(), ifaceName))
              .filter(network.nodes()::contains)
              .flatMap(n -> network.inEdges(n).stream())
              .collect(
                  toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    }
  }

  public void initIsisBGPQueues(IsisTopology isisTopology) {
    Network<IsisNode, IsisEdge> network = isisTopology.getNetwork();
    // Initialize message queues for each IS-IS circuit
    if (_vrf.getIsisProcess() == null) {
      _isisIncomingRoutes = ImmutableSortedMap.of();
    } else {
      _isisIncomingRoutes =
          _c.getAllInterfaces().keySet().stream()
              .map(ifaceName -> new IsisNode(_c.getHostname(), ifaceName))
              .filter(network.nodes()::contains)
              .flatMap(n -> network.inEdges(n).stream())
              .collect(
                  toImmutableSortedMap(Function.identity(), e -> new ConcurrentLinkedQueue<>()));
    }
  }

  private boolean isL1Only() {
    IsisProcess proc = _vrf.getIsisProcess();
    if (proc == null) {
      return false;
    }
    return proc.getLevel1() != null && proc.getLevel2() == null;
  }

  @Nullable
  public RibDelta<IsisRoute> propagateIsisRoutes(Map<String, Configuration> configurations) {
    if (_vrf.getIsisProcess() == null) {
      return null;
    }
    RibDelta.Builder<IsisRoute> lDeltaBuilder = new RibDelta.Builder<>(null);
    _isisNavieIncomingRoutes.forEach(
        (edge, queue) -> {
          Ip nextHopIp = edge.getNode1().getInterface(configurations).getAddress().getIp();
          Interface iface = edge.getNode2().getInterface(configurations);
          while (queue.peek() != null) {
            RouteAdvertisement<IsisRoute> routeAdvert = queue.remove();
            IsisRoute neighborRoute = routeAdvert.getRoute();
            IsisLevel routeLevel = neighborRoute.getLevel();
            IsisInterfaceLevelSettings isisLevelSettings =
                routeLevel == IsisLevel.LEVEL_1
                    ? iface.getIsis().getLevel1()
                    : iface.getIsis().getLevel2();

            // Do not propagate route if ISIS interface is not active at this level
            if (isisLevelSettings.getMode() != IsisInterfaceMode.ACTIVE) {
              continue;
            }
            boolean withdraw = routeAdvert.isWithdrawn();
            int adminCost =
                neighborRoute
                    .getProtocol()
                    .getDefaultAdministrativeCost(_c.getConfigurationFormat());
            long incrementalMetric =
                firstNonNull(isisLevelSettings.getCost(), IsisRoute.DEFAULT_METRIC);
            IsisRoute newRoute =
                neighborRoute.toBuilder()
                    .setAdmin(adminCost)
                    .setLevel(routeLevel)
                    .setMetric(incrementalMetric + neighborRoute.getMetric())
                    .setNextHopIp(nextHopIp)
                    // Just imported, so set nonrouting false
                    .setNonRouting(false)
                    .addNodePath(edge.getNode1().getNode())
                    .build();
            if (withdraw) {
              lDeltaBuilder.remove(newRoute, Reason.WITHDRAW);
            } else {
              lDeltaBuilder.from(_isisRib.mergeRouteGetDelta(newRoute));
            }
          }
        });
    return lDeltaBuilder.build();
  }

  public void propagateMultiIsisRoutes(Map<String, Configuration> configurations) {
    if (_vrf.getIsisProcess() == null) {
      return ;
    }
    RibDelta.Builder<IsisRoute> lDeltaBuilder = new RibDelta.Builder<>(null);
    _isisMultiIncomingRoutes.forEach(
        (edge, queue) -> {
          Ip nextHopIp = edge.getNode1().getInterface(configurations).getAddress().getIp();
          Interface iface = edge.getNode2().getInterface(configurations);
          while (queue.peek() != null) {
            IsisMultiRouteAdvertisement routeAdvert = queue.remove();
            IsisRoute neighborRoute = routeAdvert.getRoute();
            if(neighborRoute.getNodePath().contains(_c.getHostname()))
            {
              continue;
            }
            IsisLevel routeLevel = neighborRoute.getLevel();
            IsisInterfaceLevelSettings isisLevelSettings =
                routeLevel == IsisLevel.LEVEL_1
                    ? iface.getIsis().getLevel1()
                    : iface.getIsis().getLevel2();

            // Do not propagate route if ISIS interface is not active at this level
            if (isisLevelSettings.getMode() != IsisInterfaceMode.ACTIVE) {
              continue;
            }
            int adminCost =
                neighborRoute
                    .getProtocol()
                    .getDefaultAdministrativeCost(_c.getConfigurationFormat());
            long incrementalMetric =
                firstNonNull(isisLevelSettings.getCost(), IsisRoute.DEFAULT_METRIC);
            BDD linkTopology=_bddFactory.ithVar(_linkTopology.get(edge.getNode2().getNode()+"-"+edge.getNode1().getNode()));
            BDD topologyCondition=neighborRoute.getTopologyCondition().and(linkTopology);
            if(topologyCondition.isZero())
            {
              continue;
            }
            IsisRoute newRoute =
                neighborRoute.toBuilder()
                    .setAdmin(adminCost)
                    .setLevel(routeLevel)
                    .setMetric(incrementalMetric + neighborRoute.getMetric())
                    .setNextHopIp(nextHopIp)
                    // Just imported, so set nonrouting false
                    .setNonRouting(false)
                    .addNodePath(edge.getNode1().getNode())
                    .setTopologyCondition(topologyCondition)
                    .build();
            _isisMultiRib.addRoute(newRoute);
          }
        });
    return ;
  }

  void initIsisExports(int numIterations, Map<String, Node> allNodes, Map<String, Configuration> configurations) {
    /* TODO: https://github.com/batfish/batfish/issues/1703 */
    IsisProcess proc = _vrf.getIsisProcess();
    if (proc == null) {
      return; // nothing to do
    }
    RibDelta.Builder<IsisRoute> d = new RibDelta.Builder<>(null);
    /*
     * init L1 and L2 routes from connected routes
     */
    int l1Admin = RoutingProtocol.ISIS_L1.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int l2Admin = RoutingProtocol.ISIS_L2.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    IsisLevelSettings l1Settings = proc.getLevel1();
    IsisLevelSettings l2Settings = proc.getLevel2();
    IsisRoute.Builder ifaceRouteBuilder =
        new IsisRoute.Builder()
            .setArea(proc.getNetAddress().getAreaId().toString())
            .setSystemId(Long.toString(proc.getNetAddress().getSystemId()))
            .setNodePath(new ArrayList<>());
    _c.getActiveInterfaces(_vrf.getName())
        .values()
        .forEach(
            iface ->
                generateAllIsisInterfaceRoutes(
                    d, l1Admin, l2Admin, l1Settings, l2Settings, ifaceRouteBuilder, iface));

    // export default route for L1 neighbors on L1L2 routers that are not overloaded
//    if (l1Settings != null && l2Settings != null && !proc.getOverload()) {
//      IsisRoute defaultRoute =
//          new IsisRoute.Builder()
//              .setAdmin(l1Admin)
//              .setArea(proc.getNetAddress().getAreaId().toString())
//              .setAttach(true)
//              .setLevel(IsisLevel.LEVEL_1)
//              .setMetric(0L)
//              .setNetwork(Prefix.ZERO)
//              .setNextHopIp(Route.UNSET_ROUTE_NEXT_HOP_IP)
//              .setProtocol(RoutingProtocol.ISIS_L1)
//              .setNextHop("")
//              .setSystemId(Long.toString(proc.getNetAddress().getSystemId()))
//              .build();
//      d1.from(_isisL1Rib.mergeRouteGetDelta(defaultRoute));
//    }

//    if (numIterations == 1) {
//      // Add initial routes from main rib
//      importRibDelta(_isisRib,d1.build());
//      importRibDelta(_isisRib,d2.build());
//    }
//    addRedistributedRoutesToDeltas(d1, d2, proc);
//    _routesForIsisRedistribution = RibDelta.builder();
//
    queueOutgoingIsisRoutes(allNodes, configurations, d.build());
  }

  void initMultiIsisExports(Map<String, Node> allNodes, Map<String, Configuration> configurations) {
    /* TODO: https://github.com/batfish/batfish/issues/1703 */
    IsisProcess proc = _vrf.getIsisProcess();
    if (proc == null) {
      return; // nothing to do
    }
    /*
     * init L1 and L2 routes from connected routes
     */
    int l1Admin = RoutingProtocol.ISIS_L1.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    int l2Admin = RoutingProtocol.ISIS_L2.getDefaultAdministrativeCost(_c.getConfigurationFormat());
    IsisLevelSettings l1Settings = proc.getLevel1();
    IsisLevelSettings l2Settings = proc.getLevel2();
    IsisRoute.Builder ifaceRouteBuilder =
        new IsisRoute.Builder()
            .setArea(proc.getNetAddress().getAreaId().toString())
            .setSystemId(Long.toString(proc.getNetAddress().getSystemId()))
            .setNodePath(new ArrayList<>())
            .setTopologyCondition(_bddFactory.one());
    _c.getActiveInterfaces(_vrf.getName())
        .values()
        .forEach(
            iface ->
                generateMultiAllIsisInterfaceRoutes(
                    l1Admin, l2Admin, l1Settings, l2Settings, ifaceRouteBuilder, iface));
    queueMultiOutgoingIsisRoutes(allNodes, configurations);
  }

  private void generateAllIsisInterfaceRoutes(
      Builder<IsisRoute> d,
      int l1Admin,
      int l2Admin,
      @Nullable IsisLevelSettings l1Settings,
      @Nullable IsisLevelSettings l2Settings,
      IsisRoute.Builder routeBuilder,
      Interface iface) {
    IsisInterfaceSettings ifaceSettings = iface.getIsis();
    if (ifaceSettings == null) {
      return;
    }
    IsisInterfaceLevelSettings ifaceL1Settings = ifaceSettings.getLevel1();
    IsisInterfaceLevelSettings ifaceL2Settings = ifaceSettings.getLevel2();
    if (ifaceL1Settings != null && l1Settings != null) {
      generateIsisInterfaceRoutesPerLevel(l1Admin, routeBuilder, iface, IsisLevel.LEVEL_1)
          .forEach(r -> d.from(_isisRib.mergeRouteGetDelta(r)));
    }
    if (ifaceL2Settings != null && l2Settings != null) {
      generateIsisInterfaceRoutesPerLevel(l2Admin, routeBuilder, iface, IsisLevel.LEVEL_2)
          .forEach(r -> d.from(_isisRib.mergeRouteGetDelta(r)));
    }
  }

  private void generateMultiAllIsisInterfaceRoutes(
      int l1Admin,
      int l2Admin,
      @Nullable IsisLevelSettings l1Settings,
      @Nullable IsisLevelSettings l2Settings,
      IsisRoute.Builder routeBuilder,
      Interface iface) {
    IsisInterfaceSettings ifaceSettings = iface.getIsis();
    if (ifaceSettings == null) {
      return;
    }
    IsisInterfaceLevelSettings ifaceL1Settings = ifaceSettings.getLevel1();
    IsisInterfaceLevelSettings ifaceL2Settings = ifaceSettings.getLevel2();
    boolean levelAlready=false;
    if (ifaceL1Settings != null && l1Settings != null) {
      levelAlready=true;
      generateIsisInterfaceRoutesPerLevel(l1Admin, routeBuilder, iface, IsisLevel.LEVEL_1)
          .forEach(r -> _isisMultiRib.addRoute(r));
    }
    if (!levelAlready&&ifaceL2Settings != null && l2Settings != null) {
      generateIsisInterfaceRoutesPerLevel(l2Admin, routeBuilder, iface, IsisLevel.LEVEL_2)
          .forEach(r -> _isisMultiRib.addRoute(r));
    }
  }

  private static Set<IsisRoute> generateIsisInterfaceRoutesPerLevel(
      int adminCost, IsisRoute.Builder routeBuilder, Interface iface, IsisLevel level) {
    IsisInterfaceLevelSettings ifaceLevelSettings =
        level == IsisLevel.LEVEL_1 ? iface.getIsis().getLevel1() : iface.getIsis().getLevel2();
    RoutingProtocol isisProtocol =
        level == IsisLevel.LEVEL_1 ? RoutingProtocol.ISIS_L1 : RoutingProtocol.ISIS_L2;
    long metric =
        ifaceLevelSettings.getMode() == IsisInterfaceMode.PASSIVE
            ? 0L
            : firstNonNull(ifaceLevelSettings.getCost(), IsisRoute.DEFAULT_METRIC);
    routeBuilder.setAdmin(adminCost).setLevel(level).setMetric(metric).setProtocol(isisProtocol).setOriginatorIp(iface.getAddress().getIp());
    Set<IsisRoute> answer=new HashSet<>();
    Prefix prefix=iface.getAddress().getPrefix();
    Ip ip=iface.getAddress().getIp();
    routeBuilder.setNetwork(prefix);
    routeBuilder.setNextHopIp(ip);
    routeBuilder.setNextHop(iface.getOwner().getHostname());//xia yi tiao
    IsisRoute isisRoute=routeBuilder.build();
//    answer.add(routeBuilder.setNetwork(iface.getAddress().getPrefix()).setNextHopIp(iface.getAddress().getIp()).build());
    answer.add(isisRoute);
    return answer;
  }

  private void queueOutgoingIsisRoutes(
      @Nonnull Map<String, Node> allNodes,
      Map<String, Configuration> configurations,
      @Nonnull RibDelta<IsisRoute> lDelta) {
    if (_vrf.getIsisProcess() == null || _isisNavieIncomingRoutes == null) {
      return;
    }

    // Loop over neighbors, enqueue messages
    for (IsisEdge edge : _isisNavieIncomingRoutes.keySet()) {
      // Do not queue routes on non-active ISIS interface levels
      Interface iface = edge.getNode2().getInterface(configurations);
      IsisInterfaceLevelSettings level1Settings = iface.getIsis().getLevel1();
      IsisInterfaceLevelSettings level2Settings = iface.getIsis().getLevel2();
      IsisLevel activeLevels = null;
      if (level1Settings != null && level1Settings.getMode() == IsisInterfaceMode.ACTIVE) {
        activeLevels = IsisLevel.LEVEL_1;
      }
      if (level2Settings != null && level2Settings.getMode() == IsisInterfaceMode.ACTIVE) {
        activeLevels = IsisLevel.union(activeLevels, IsisLevel.LEVEL_2);
      }
      if (activeLevels == null) {
        continue;
      }

      VirtualRouter remoteVr =
          allNodes
              .get(edge.getNode1().getNode())
              .getVirtualRouters().get(edge.getNode1().getInterface(configurations).getVrfName());
      Queue<RouteAdvertisement<IsisRoute>> queue = remoteVr._isisNavieIncomingRoutes.get(edge.reverse());
      IsisLevel circuitType = edge.getCircuitType();
      if (circuitType.includes(IsisLevel.LEVEL_1) && activeLevels.includes(IsisLevel.LEVEL_1)) {
        RibDelta.Builder<IsisRoute> upgradedRoutes = new RibDelta.Builder<>(null);
        lDelta
            .getActions()
            .forEach(
                ra -> {
                  IsisRoute route = ra.getRoute();
                  RoutingProtocol upgradedProtocol=RoutingProtocol.ISIS_L1;
                  int upgradedAdmin =
                      upgradedProtocol.getDefaultAdministrativeCost(_c.getConfigurationFormat());
                  IsisRoute upgradeRoute=ra.getRoute().toBuilder()
                      .setAdmin(upgradedAdmin)
                      .setLevel(IsisLevel.LEVEL_1)
                      .setProtocol(upgradedProtocol)
                      .build();
                  if(ra.isWithdrawn())
                  {
                    upgradedRoutes.remove(upgradeRoute,ra.getReason());
                  }else{
                    upgradedRoutes.add(upgradeRoute);
                  }
                });
        queueDelta(queue, upgradedRoutes.build());
        Queue<RouteAdvertisement<IsisRoute>> tempQ=queue;
      }
      if (circuitType.includes(IsisLevel.LEVEL_2) && activeLevels.includes(IsisLevel.LEVEL_2)) {
        RibDelta.Builder<IsisRoute> upgradedRoutes = new RibDelta.Builder<>(null);
        lDelta
            .getActions()
            .forEach(
                ra -> {
                  IsisRoute route = ra.getRoute();
                  RoutingProtocol upgradedProtocol=RoutingProtocol.ISIS_L2;
                  int upgradedAdmin =
                      upgradedProtocol.getDefaultAdministrativeCost(_c.getConfigurationFormat());
                  IsisRoute upgradeRoute=ra.getRoute().toBuilder()
                      .setAdmin(upgradedAdmin)
                      .setLevel(IsisLevel.LEVEL_2)
                      .setProtocol(upgradedProtocol)
                      .build();
                  if(ra.isWithdrawn())
                  {
                    upgradedRoutes.remove(upgradeRoute,ra.getReason());
                  }else{
                    upgradedRoutes.add(upgradeRoute);
                  }
                });
        queueDelta(queue, upgradedRoutes.build());
        Queue<RouteAdvertisement<IsisRoute>> tempQ=queue;
      }


    }
  }

  public void queueMultiOutgoingIsisRoutes(
      @Nonnull Map<String, Node> allNodes,
      Map<String, Configuration> configurations) {
    if (_vrf.getIsisProcess() == null || _isisMultiIncomingRoutes == null) {
      return;
    }
    // Loop over neighbors, enqueue messages
    List<IsisRoute.Builder> changedRoutes=_isisMultiRib.getChangedRoute();
    if(changedRoutes.size()==0)
    {
      return;
    }
    _isisRibChanged=true;
    for (IsisEdge edge : _isisMultiIncomingRoutes.keySet()) {
      // Do not queue routes on non-active ISIS interface levels
      Interface iface = edge.getNode2().getInterface(configurations);
      IsisInterfaceLevelSettings level1Settings = iface.getIsis().getLevel1();
      IsisInterfaceLevelSettings level2Settings = iface.getIsis().getLevel2();
      IsisLevel activeLevels = null;
      if (level1Settings != null && level1Settings.getMode() == IsisInterfaceMode.ACTIVE) {
        activeLevels = IsisLevel.LEVEL_1;
      }
      if (level2Settings != null && level2Settings.getMode() == IsisInterfaceMode.ACTIVE) {
        activeLevels = IsisLevel.union(activeLevels, IsisLevel.LEVEL_2);
      }
      if (activeLevels == null) {
        continue;
      }

      VirtualRouter remoteVr =
          allNodes
              .get(edge.getNode1().getNode())
              .getVirtualRouters().get(edge.getNode1().getInterface(configurations).getVrfName());
      Queue<IsisMultiRouteAdvertisement> queue = remoteVr._isisMultiIncomingRoutes.get(edge.reverse());
      IsisLevel circuitType = edge.getCircuitType();
      if (circuitType.includes(IsisLevel.LEVEL_1) && activeLevels.includes(IsisLevel.LEVEL_1)) {
        for(IsisRoute.Builder addRouteBuilder:changedRoutes)
        {
          IsisRoute addRuoute=addRouteBuilder.build();
          addRuoute.setIsisLevel(IsisLevel.LEVEL_1);
          addRuoute.setRoutingProtocol(RoutingProtocol.ISIS_L1);
          queue.add(new IsisMultiRouteAdvertisement(addRuoute));
        }
      }
      if (circuitType.includes(IsisLevel.LEVEL_2) && activeLevels.includes(IsisLevel.LEVEL_2)) {
        for(IsisRoute.Builder addRouteBuilder:changedRoutes)
        {
          IsisRoute addRuoute=addRouteBuilder.build();
          addRuoute.setIsisLevel(IsisLevel.LEVEL_2);
          addRuoute.setRoutingProtocol(RoutingProtocol.ISIS_L2);
          queue.add(new IsisMultiRouteAdvertisement(addRuoute));
        }
      }
    }
  }

  boolean unstageIsisRoutes(
      Map<String, Node> allNodes,
      Map<String, Configuration> configurations,
      RibDelta<IsisRoute> lDelta) {
    if(lDelta==null)
    {
      return false;
    }
    queueOutgoingIsisRoutes(allNodes, configurations , lDelta);
    this._isisRibChanged= !lDelta.isEmpty();
    return !lDelta.isEmpty();
  }
  public IsisRib getIsisRib()
  {
    return _isisRib;
  }
  public IsisMultiRib getMultiIsisRib()
  {
    return _isisMultiRib;
  }

  public void initSymBgpRoute(Map<SymPrefixList,Integer> prefixEC,final Network<BgpNeighbor, BgpSession> bgpTopology, final Map<String, Node> allNodes, List<Integer> PECNumber)
  {
    if(_vrf.getBgpProcess()==null || !_vrf.getName().equals("default"))
    {
      return ;
    }





    for (Integer pecNumber : PECNumber)
    {
      BitSet prefixEcSet = new BitSet();
      prefixEcSet.set(pecNumber);
      SymBgpRoute initalRoute = new SymBgpRoute(prefixEcSet, _topologyBDD, _prefixFactory);
      initalRoute.setLocalPreference(100);
      initalRoute.setCommunity(0);
      initalRoute.setAsPath(new ArrayList<>());
      initalRoute.setAsPathSize(0);
      initalRoute.setTopologyCondition(1);
      initalRoute.setNextHopIp(Ip.AUTO);
      initalRoute.setOriginatorIp(_vrf.getBgpProcess().getRouterId());
      initalRoute.setRoutingProtocol(RoutingProtocol.BGP);
      initalRoute.setReason(org.batfish.symwork.Reason.ADD);
      initalRoute.setNodePath(new ArrayList<>());
      initalRoute.setExternal(false);
      initalRoute.setOriginatorIndex(0);
      _symBgpRib.addRoute(initalRoute);
    }
//    System.out.println("step:init bgp:step1");
    // List<IsisRoute> igpRoutes=_isisMultiRib.getRoutes();
    
//    System.out.println("step:init bgp:step2");
    // zhe li ke neng hai xu yao dui tuo pu tiao jian chu li yi xia zi
//    for(IsisRoute igpRoute:igpRoutes)
//    {
//      if(_connectPrefix.contains(igpRoute.getNetwork()))
//      {
//        continue;
//      }
//      boolean canNetwork=false;
//      for(PrefixSpace p:_c.getNetworkPrefix())
//      {
//        if(p.containsPrefix(igpRoute.getNetwork()))
//        {
//          canNetwork=true;
//          break;
//        }
//      }
//      if(!canNetwork)
//      {
//        continue;
//      }
//      SymPrefixList symPrefix= SingleConvertPrefixToSymPrefix.convert(igpRoute.getNetwork().toString(),_prefixFactory);
//      if(!prefixEC.containsKey(symPrefix))
//      {
//        continue;
//      }else{
//        SymBgpRoute symBgpRoute=new SymBgpRoute(prefixEC.get(symPrefix),_bddFactory,_prefixFactory);
//        symBgpRoute.setEgpProtocol(RoutingProtocol.ISIS);
//        symBgpRoute.setIgpProtocol(RoutingProtocol.ISIS);
//        symBgpRoute.setNextNode(_c.getHostname());
//        symBgpRoute.setSrcNode(_c.getHostname());
//        symBgpRoute.setTopologyCondition(igpRoute.getTopologyCondition());
//        symBgpRoute.setReceivedFromIp(igpRoute.getNextHopIp());
//        symBgpRoute.setAsPath(new AsPath(new ArrayList<>()));
//        symBgpRoute.setNextHopIp(igpRoute._nextHopIp);
//        symBgpRoute.setSrcProtocol(igpRoute.getProtocol());
//        symBgpRoute.setNodePath(igpRoute.getNodePath());
//        symBgpRoute.setIsisAdmin(igpRoute.getAdministrativeCost());
//        symBgpRoute.setIsisMetric(igpRoute.getMetric());
//        symBgpRoute.setOriginatorIp(igpRoute.getOriginatorIp());
//        symBgpRoute.saveLastBgpMessage();
//        symBgpRoute._symCommunities=new SymCommunity(false);
//        _symBgpRib.AddRoute(symBgpRoute);
//      }
//    }
//    System.out.println("step:init bgp:step3");
//     Set<AbstractRoute> allRoutes=_mainRib.getRoutes();
//     for(AbstractRoute allRoute:allRoutes)
//     {
//       boolean canNetwork=false;
//       for(PrefixSpace p:_c.getNetworkPrefix())
//       {
//         if(p.containsPrefix(allRoute.getNetwork()))
//         {
//           canNetwork=true;
//           break;
//         }
//       }
//       if(!canNetwork)
//       {
//         continue;
//       }
// //      System.out.println("step:init bgp process-convert symprefix");
//       SymPrefixList symPrefix= SingleConvertPrefixToSymPrefix.convert(allRoute.getNetwork().toString(),_prefixFactory);
// //      System.out.println("step:init bgp process-convert symprefix-end");
//       if(!prefixEC.containsKey(symPrefix))
//       {
//         continue;
//       }else{
// //        System.out.println("step:init bgp process-convert symbgproute");
//         SymBgpRoute symBgpRoute=new SymBgpRoute(prefixEC.get(symPrefix),_topologyBDD,_prefixFactory);
//         symBgpRoute.setEgpProtocol(RoutingProtocol.BGP);
//         symBgpRoute.setIgpProtocol(RoutingProtocol.CONNECTED);
//         symBgpRoute.setNextNode(_c.getHostname());
//         symBgpRoute.setSrcNode(_c.getHostname());
//         symBgpRoute.setTopologyCondition(1);
//         symBgpRoute.setReceivedFromIp(allRoute.getNextHopIp());
//         symBgpRoute.setAsPath(new AsPath(new ArrayList<>()));
//         symBgpRoute.setNextHopIp(null);
//         symBgpRoute.setSrcProtocol(RoutingProtocol.BGP);
//         symBgpRoute.setNodePath(new ArrayList<>());
//         symBgpRoute.setIsisAdmin(0);
//         symBgpRoute.setIsisMetric(0);
//         symBgpRoute.saveLastBgpMessage();
//         symBgpRoute._symCommunities=new SymCommunity(false);
//         symBgpRoute.setIndex(routeIndex[0]);
//         boolean alreadyAdd=_symBgpRib.AddRoute(symBgpRoute);
//         routeIndexForest.put(routeIndex[0],new RouteForest());
//         routeIndexForest.get(routeIndex[0]).addNode(new TreeNode(_c.getHostname(),symBgpRoute.getIndex(),symBgpRoute.getAsPath().hashCode(),symBgpRoute.getAsPathSize().hashCode(),symBgpRoute.getAsPathSize(),symBgpRoute.getAsPath()));
//         routeIndex[0]++;
// //        System.out.println("step:init bgp:step4");
//       }
//     }
//    System.out.println("step:init bgp:step5");
//    _symBgpRib.clearTreeNodes();
//    _symBgpRib.clearDeleteTreeNodes();
//    _symBgpRib.computeChangedRoute();


//    symQueueStagingBgpMessages(allNodes,bgpTopology,routeIndexForest);
  }



  // public SymBgpRoute cloneBgpRoute(SymBgpRoute route)
  // {
  //   SymBgpRoute answerRoute=new SymBgpRoute(route._prefixEcNum,route.getTopologyBDD(),route.getPrefixFactory());
  //   answerRoute.setAsPath(route.getAsPath());
  //   SymBounder bounder= route.getAsPathSize();
  //   answerRoute.setAsPathSize(route.getAsPathSize().getLowerbound(),route.getAsPathSize().getUpperbound());
  //   answerRoute.setTopologyCondition(route.getTopologyCondition());
  //   answerRoute.setLocalPreference(route.getLocalPreference().getLowerbound(),route.getLocalPreference().getUpperbound());
  //   answerRoute.setNextHopIp(route.getNextHopIp());
  //   answerRoute.setOriginatorIp(route.getOriginatorIp());
  //   answerRoute.setOriginType(route.getOriginType());
  //   answerRoute.setEgpProtocol(route.getEgpProtocol());
  //   answerRoute.setIgpProtocol(route.getIgpProtocol());
  //   answerRoute.setSrcProtocol(route.getSrcProtocol());
  //   answerRoute.setReceivedFromIp(route.getReceivedFromIp());
  //   answerRoute._symCommunities=new SymCommunity(route._symCommunities);
  //   answerRoute._reason=route._reason;
  //   answerRoute._external=route._external;
  //   answerRoute.setSrcNode(route.getSrcNode());
  //   answerRoute._relatedNode.addAll(route.getRelatedNode());
  //   answerRoute.setPrefixEcNum(route.getPrefixEcNum());
  //   answerRoute.setSrcAcl(route.getSrcAcl());
  //   answerRoute.setDstAcl(route.getDstAcl());

  //   answerRoute._outBGPTopologyCondition=route._outBGPTopologyCondition;
  //   answerRoute._outISISTopologyCondition=route._outISISTopologyCondition;

  //   answerRoute.setIsisMetric(route.getIsisMetric());
  //   answerRoute.setNextNode(route.getNextNode());
  //   answerRoute.setNodePath(route.getNodePath());
  //   answerRoute._asNodePath=new ArrayList<>();
  //   answerRoute._asNodePath.addAll(route._asNodePath);
  //   if(route.getIsisLevel()!=null)
  //   {
  //     if(route.getIsisLevel().includes(IsisLevel.LEVEL_1)&&route.getIsisLevel().includes(IsisLevel.LEVEL_2))
  //     {
  //       answerRoute.setIsisLevel(IsisLevel.union(IsisLevel.LEVEL_1,IsisLevel.LEVEL_2));
  //     }else if(route.getIsisLevel().includes(IsisLevel.LEVEL_2)){
  //       answerRoute.setIsisLevel(IsisLevel.LEVEL_2);
  //     }
  //   }
  //   answerRoute._lastLocalPreference=new SymLocalPreference(route._lastLocalPreference.getLowerBound(),route._lastLocalPreference.getUpperBound());
  //   answerRoute._lastCommunities=new SymCommunity(route._symCommunities);
  //   //    answerRoute._lastCommunities.addAll(route.getCommunities());
  //   answerRoute.setIndex(route.getIndex());
  //   return answerRoute;
  // }

  public synchronized int andBDD(int tc1,int tc2)
  {
    synchronized (_topologyBDD)
    {
      return _topologyBDD.and(tc1,tc2);
    }
  }

//  public BDD andBDD(BDD tc1,BDD tc2)
//  {
//      return tc1.and(tc2);
//  }

  public SortedMap<UndirectedBgpSession, Queue<SymRouteAdvertisement<SymBgpRoute>>> getSymBgpIncomingRoutes()
  {
    return _symBgpIncomingRoutes;
  }

  public HashMap<BgpNeighbor, HashSet<SymRouteAdvertisement<SymBgpRoute>>> getExternalRouteList()
  {
    return _externalRouteList;
  }

  public SymBgpFib getSymBgpFib()
  {
    return _symBgpFib;
  }

  public SymBgpRib getSymBgpRib()
  {
    return _symBgpRib;
  }
}
