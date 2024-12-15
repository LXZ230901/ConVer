package org.batfish.dataplane.ibdp;

import static org.batfish.common.util.CommonUtil.initBgpTopology;
import static org.batfish.common.util.CommonUtil.initSpecialBgpTopology;
import static org.batfish.dataplane.rib.AbstractRib.importRib;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.graph.Network;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Version;
import org.batfish.common.util.CommonUtil;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpAdvertisement;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.datamodel.BgpProcess;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSession;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IsisEdge;
import org.batfish.datamodel.IsisLevel;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.IsisTopology;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RouteFilterLine;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.answers.BdpAnswerElement;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.expr.ExplicitPrefixSet;
import org.batfish.datamodel.routing_policy.expr.InlineCommunitySet;
import org.batfish.dataplane.TracerouteEngineImpl;
import org.batfish.dataplane.ibdp.schedule.IbdpSchedule;
import org.batfish.dataplane.ibdp.schedule.IbdpSchedule.Schedule;
import org.batfish.dataplane.rib.BgpMultipathRib;
import org.batfish.dataplane.rib.Rib;
import org.batfish.dataplane.rib.RibDelta;
import org.batfish.dataplane.rib.RouteAdvertisement;
import org.batfish.extwork.ExtNeighborComputer;
import org.batfish.extwork.ExtNeighborFeature;
import org.batfish.extwork.ForwardingEngine;
import org.batfish.symwork.AECComputeResult;
import org.batfish.symwork.AECEnvironment;
import org.batfish.symwork.CECEngine;
import org.batfish.symwork.CECOpEngine;
import org.batfish.symwork.ConvertPrefixToPrefixEC;
import org.batfish.symwork.ConvertPrefixToSymPrefix;
import org.batfish.symwork.ExternalNeighbor;
import org.batfish.symwork.PrefixLengthUnit;
import org.batfish.symwork.PrefixRelationshipGenerator;
import org.batfish.symwork.RouteForest;
import org.batfish.symwork.SingleConvertPrefixToSymPrefix;
import org.batfish.symwork.SymBgpFib;
import org.batfish.symwork.SymBgpRib;
import org.batfish.symwork.SymBgpRoute;
import org.batfish.symwork.SymBgpRoute.Builder;
import org.batfish.symwork.SymBounder;
import org.batfish.symwork.SymCommunity;
import org.batfish.symwork.SymPolicyAnswer;
import org.batfish.symwork.SymPrefixList;
import org.batfish.symwork.SymRouteAdvertisement;
import org.batfish.symwork.TreeNode;
import org.batfish.symwork.IsisRouteAdvertisement.Reason;

//通过此来计算控制平面和数据平面
@SuppressWarnings("unused")
public class IncrementalBdpEngine {

  private int _numIterations;

  private final BatfishLogger _bfLogger;

  private final BiFunction<String, Integer, AtomicInteger> _newBatch;

  private final IncrementalDataPlaneSettings _settings;


  public IncrementalBdpEngine(
      IncrementalDataPlaneSettings settings,
      BatfishLogger logger,
      BiFunction<String, Integer, AtomicInteger> newBatch) {
    _settings = settings;
    _bfLogger = logger;
    _newBatch = newBatch;
  }

  IncrementalDataPlane computeDataPlane(
      boolean differentialContext,
      Map<String, Configuration> configurations,
      Topology topology,
      Set<BgpAdvertisement> externalAdverts,
      BdpAnswerElement ae) {
    _bfLogger.resetTimer();
    IncrementalDataPlane dp = new IncrementalDataPlane();
    _bfLogger.info("\nComputing Data Plane using iBDP\n");


    long timestampBegin = System.currentTimeMillis();
    System.out.println("Step:come in");

    // compute community atom
    Set<Automaton> communityAtomSet = CECEngine.computeCommunityAtom(configurations);
    HashMap<Automaton, Integer> communityAtomToIntger = new HashMap<>();
    Integer communityAtomIndex = 0;
    for (Automaton communityAtom : communityAtomSet)
    {
      communityAtomToIntger.put(communityAtom, communityAtomIndex);
      communityAtomIndex++;
    }
    for (Configuration configuration : configurations.values())
    {
      for (CommunityList communityList : configuration.getCommunityLists().values())
      {
        for (CommunityListLine communityListLine : communityList.getLines())
        {
          communityListLine.toCommunityAtom(communityAtomToIntger);
        }
      }
      for (InlineCommunitySet inlineCommunitySet : configuration.getInlineCommunity())
      {
        inlineCommunitySet.toCommunityAtom(communityAtomToIntger);
      }
    }
    long timestampComputeCEC = System.currentTimeMillis();
    System.out.println("ComputeCommunityAtom Time:" + (timestampComputeCEC - timestampBegin));
//
//
//
//
//
//
//
//
//
    //SetPrefixFactory,StateTopologyFactory
    BDDFactory prefixFactory=BDDFactory.init(32,32);
    prefixFactory.setVarNum(32);

    jdd.bdd.BDD topologyBDD=new jdd.bdd.BDD(10000,10000);
    HashMap<String,Integer> topologyEdgeBDD=new HashMap<>();
    List<Integer> topologyEdgeBDDList=new ArrayList<>();

    Map<Ip, Set<String>> ipOwners = CommonUtil.computeIpNodeOwners(configurations, true);
    Map<Ip, String> ipOwnersSimple = CommonUtil.computeIpOwnersSimple(ipOwners);

    Map<String,Set<Ip>> ownersIp=new HashMap<>();
    for(Ip ip:ipOwners.keySet())
    {
      for (String nodeName:ipOwners.get(ip))
      {
        ownersIp.putIfAbsent(nodeName,new HashSet<>());
        ownersIp.get(nodeName).add(ip);
      }
    }

    //将配置ip/ipOwners/ipOwnerSimple保存到控制平面当中.
    dp.initIpOwners(configurations, ipOwners, ipOwnersSimple);

    // Generate our nodes, keyed by name, sorted for determinism
    ImmutableSortedMap.Builder<String, Node> builderd =
        new ImmutableSortedMap.Builder<>(Ordering.natural());

    configurations.values().forEach(c -> builderd.put(c.getHostname(), new Node(c)));
    ImmutableSortedMap<String, Node> nodes = builderd.build();

    HashMap<String,BgpSession> nodeNameBgpTopology=new HashMap<>();




    //为控制平面创建nodes和topology,对链路的编码也可以放在这一部分,也可以放在dp当中--这一部分的拓扑和BGP的拓扑不太一样所以后续是否还考虑?
    dp.setNodes(nodes);
    dp.setTopology(topology);

    Network<BgpNeighbor, BgpSession> bgpTopology =
        initSpecialBgpTopology(
            configurations, dp.getIpOwners(), false, false, TracerouteEngineImpl.getInstance(), dp ,nodeNameBgpTopology);//不再进行可达性验证
//    Network<BgpNeighbor, BgpSession> bgpTopology =
//        initBgpTopology(
//            configurations, dp.getIpOwners(), false, false, TracerouteEngineImpl.getInstance(), dp);//不再进行可达性验证

//    计算外部邻居，同时确定每个外部邻居对应的入口策略和出口策略，用于后续划分CEC等价类
    ExtNeighborFeature extNeighborFeature = ExtNeighborComputer.computeExternalNeighbor(nodes, bgpTopology);


// 将对应CommunityList中的Community转换成符号化的Community
    System.out.println("Slice CEC begin!");
    long timeStampsSliceCecBegin = System.currentTimeMillis();
    jdd.bdd.BDD communityAtomBDD = new jdd.bdd.BDD(10000, 10000);
    HashMap<Integer, Integer> communityAtomToBDD = new HashMap<>();
    for (int i = 0; i< communityAtomIndex; i++)
    {
      Integer communityBddIndex = communityAtomBDD.createVar();
      communityAtomToBDD.put(i, communityBddIndex);
    }

    for (Configuration configuration : configurations.values())
    {
      for (CommunityList communityList : configuration.getCommunityLists().values())
      {
        for (CommunityListLine communityListLine : communityList.getLines())
        {
          communityListLine.toSymCommunityAtom(communityAtomToBDD);
        }
      }
      for (InlineCommunitySet inlineCommunitySet : configuration.getInlineCommunity())
      {
        inlineCommunitySet.toSymCommunityAtom(communityAtomToBDD);
      }
    }
    CECOpEngine.setCECBdd(communityAtomBDD);
//    确定内部路由策略划分的等价类
    Set<Integer> internalInitialCEC = new HashSet<>();
    for (Configuration configuration : configurations.values())
    {
      for (RoutingPolicy routingPolicy : configuration.getRoutingPolicies().values())
      {
        if (routingPolicy.getValidPolicy() && extNeighborFeature.getInternalPolicy().get(configuration.getHostname()).contains(routingPolicy.getName()))
        {
          AECEnvironment aecEnvironment = new AECEnvironment(configuration, communityAtomToBDD, communityAtomBDD);
          AECComputeResult aecComputeResult = routingPolicy.computeAEC(1, aecEnvironment);
          if (aecComputeResult.getValidAEC())
          {
            internalInitialCEC.addAll(aecComputeResult.getCECSet());
          }
        }
      }
    }





//  确定对应的出口策略
    Set<Integer> exportPolicyInitialCEC = new HashSet<>();
    for (Configuration configuration : configurations.values())
    {
      for (RoutingPolicy routingPolicy : configuration.getRoutingPolicies().values())
      {
        if (routingPolicy.getValidPolicy() && extNeighborFeature.getExternalExportPolicy().get(configuration.getHostname()).contains(routingPolicy.getName()))
        {
          AECEnvironment aecEnvironment = new AECEnvironment(configuration, communityAtomToBDD, communityAtomBDD);
          AECComputeResult aecComputeResult = routingPolicy.computeAEC(1, aecEnvironment);
          if (aecComputeResult.getValidAEC())
          {
            exportPolicyInitialCEC.addAll(aecComputeResult.getCECSet());
          }
        }
      }
    }


//    针对于每一个外部邻居划分CEC等价类
    HashMap<ExternalNeighbor, HashSet<Integer>> perNeighborCEC = new HashMap<>();
    int maxCompputeCEC = 0;
    for (ExternalNeighbor externalNeighbor : extNeighborFeature.getExtNeighborLink().keySet())
    {
      Set<Integer> perNeighborInitialCEC = new HashSet<>();
      perNeighborInitialCEC.addAll(internalInitialCEC);
      perNeighborInitialCEC.addAll(exportPolicyInitialCEC);
      for (BgpNeighbor bgpNeighbor : extNeighborFeature.getExtNeighborLink().get(externalNeighbor))
      {
        Configuration configuration = bgpNeighbor.getOwner();





        for (String importPolicyName : bgpNeighbor.getImportPolicySources())
        {
          RoutingPolicy importRoutingPolicy = configuration.getRoutingPolicies().get(importPolicyName);






          if (importRoutingPolicy.getValidPolicy())
          {
            AECEnvironment aecEnvironment = new AECEnvironment(configuration, communityAtomToBDD, communityAtomBDD);
            AECComputeResult aecComputeResult = importRoutingPolicy.computeAEC(1, aecEnvironment);
            if (aecComputeResult.getValidAEC())
            {
              perNeighborInitialCEC.addAll(aecComputeResult.getCECSet());
            }
          }
        }
        if (perNeighborInitialCEC.size() > maxCompputeCEC)
        {
          maxCompputeCEC = perNeighborInitialCEC.size();
        }
      }





      if (!perNeighborCEC.containsKey(externalNeighbor))
      {
        perNeighborCEC.put(externalNeighbor, new HashSet<>());
      }
      perNeighborInitialCEC.add(1);
      Set<Integer> extportPolicyComputeCEC = new HashSet<>();
      for (Integer outCEC : perNeighborInitialCEC)
      {
        Integer currentCEC = outCEC;
        Set<Integer> addCEC = new HashSet<>();
        Set<Integer> removeCEC = new HashSet<>();
        for (Integer inCEC : extportPolicyComputeCEC)
        {
          Integer andCEC = communityAtomBDD.and(currentCEC, inCEC);
          if(andCEC != 0)
          {
            Integer notCEC = communityAtomBDD.and(inCEC, communityAtomBDD.not(andCEC));
            if (notCEC != 0)
            {
              addCEC.add(notCEC);
              communityAtomBDD.ref(notCEC);
            }
            addCEC.add(andCEC);
            removeCEC.add(inCEC);
            currentCEC = communityAtomBDD.and(currentCEC, communityAtomBDD.not(andCEC));
            if(currentCEC == 0)
            {
              break;
            }
          }
        }
        extportPolicyComputeCEC.removeAll(removeCEC);
        extportPolicyComputeCEC.addAll(addCEC);
        if (currentCEC != 0)
        {
          extportPolicyComputeCEC.add(currentCEC);
          communityAtomBDD.ref(currentCEC);
        }
      }
      perNeighborCEC.get(externalNeighbor).addAll(extportPolicyComputeCEC);
    }
    long timeStampsSliceCecEnd = System.currentTimeMillis();
    System.out.println("Slice CEC End! Time is :" + (timeStampsSliceCecEnd - timeStampsSliceCecBegin));

//    划分前缀等价类
    System.out.println("Slice PEC Begin!");
    long timestampPECSliceBegin = System.currentTimeMillis();
    //通过prefixInPLAndBGP来保存在路由策略当中出现和在network命令当中出现的前缀信息
    HashMap<String,HashSet<String>> prefixInPLAndBgp=new HashMap<>();

    HashMap<String,List<String>> prefixNetwork=new HashMap<>();

    //extract netWork prefix
    nodes
        .values()
        .forEach(
            n->{
              List<PrefixSpace> networkPrefix=n.getConfiguration().getNetworkPrefix();
              if(!prefixNetwork.containsKey(n.getName()))
              {
                prefixNetwork.put(n.getName(),new ArrayList<>());
              }
              for(PrefixSpace prefix:networkPrefix)
              {
                for(PrefixRange prefixRange:prefix.getPrefixRanges())
                {
                  String Prefix=prefixRange.toString();
                  String node_name=n.getConfiguration().getHostname();
                  prefixNetwork.get(node_name).add(Prefix);
                  //                  if(prefixOwners.containsKey(Prefix))
                  //                  {
                  //                    prefixOwners.get(node_name).add(SingleConvertPrefixToSymPrefix.convert(Prefix,prefixFactory));
                  //                  }else{
                  //                    prefixOwners.put(node_name,new ArrayList<>());
                  //                    prefixOwners.get(node_name).add(SingleConvertPrefixToSymPrefix.convert(Prefix,prefixFactory));
                  //                  }
                  if(prefixInPLAndBgp.containsKey(Prefix))
                  {
                    prefixInPLAndBgp.get(Prefix).add("["+Prefix.substring(Prefix.indexOf("/")+1,Prefix.length())+","+Prefix.substring(Prefix.indexOf("/")+1,Prefix.length())+"]");
                  }else{
                    HashSet<String> len=new HashSet<>();
                    len.add("["+Prefix.substring(Prefix.indexOf("/")+1,Prefix.length())+","+Prefix.substring(Prefix.indexOf("/")+1,Prefix.length())+"]");
                    prefixInPLAndBgp.put(Prefix,len);
                  }
                }
              }
            }
        );

    //extract prefix in routing-policy
    for(String node_name: nodes.keySet())
    {
      NavigableMap<String, RouteFilterList> temp=nodes.get(node_name).getConfiguration().getRouteFilterLists();//这个RouteFilterList需要更高的
      for(String FilterName:temp.keySet())
      {
        List<RouteFilterLine> filterLines=temp.get(FilterName).getLines();
        for(int i=0;i<filterLines.size();i++)
        {
          String Prefix=filterLines.get(i).getPrefix().toString();
          if(prefixInPLAndBgp.containsKey(Prefix))
          {
            prefixInPLAndBgp.get(Prefix).add(filterLines.get(i).getLengthRange().toString());
          }else{
            HashSet<String> len=new HashSet<>();
            len.add(filterLines.get(i).getLengthRange().toString());
            prefixInPLAndBgp.put(Prefix,len);
          }
        }
      }
    }


    int prefixNumber = 0;
    for (String prefix : prefixInPLAndBgp.keySet())
    {
      prefixNumber += prefixInPLAndBgp.get(prefix).size();
    }
    //通过ConvertPrefixToPrefixEC将前缀转换为前缀等价类,最终转换完成的前缀等价类保存在answer当中
    System.out.println("Step:convert prefix EC");
    HashMap<String, List<String>> allPrefix = new HashMap<>();
    HashMap<Integer, HashSet<PrefixLengthUnit>> allPrefixByLen = new HashMap<>();
    for (String prefix : prefixInPLAndBgp.keySet())
    {
      allPrefix.put(prefix, new ArrayList<>());
      allPrefix.get(prefix).addAll(prefixInPLAndBgp.get(prefix));
      int prefixIpLen=Integer.parseInt(prefix.substring(prefix.indexOf("/")+1,prefix.length()));
      if (!allPrefixByLen.containsKey(prefixIpLen))
      {
        allPrefixByLen.put(prefixIpLen, new HashSet<>());
      }
      allPrefixByLen.get(prefixIpLen).add(new PrefixLengthUnit(prefix, prefixInPLAndBgp.get(prefix)));
    }
    PrefixRelationshipGenerator prefixRelationshipGenerator = new PrefixRelationshipGenerator();
    for (Integer len : allPrefixByLen.keySet())
    {
      prefixRelationshipGenerator.addPrefix(allPrefixByLen.get(len));
    }
    Set<SymPrefixList> PEC = prefixRelationshipGenerator.computePEC(prefixFactory);
    
    HashMap<SymPrefixList, Integer> PECToPECNum = new HashMap<>();
    HashMap<Integer, SymPrefixList> PECNumToPEC = new HashMap<>();
    HashMap<String, HashSet<SymPrefixList>> prefixToPEC = new HashMap<>();
    Integer pecNum = 0;
    for (SymPrefixList pec : PEC)
    {
        PECToPECNum.put(pec, pecNum);
        PECNumToPEC.put(pecNum, pec);
        pecNum++;
        for (String prefix : pec.getRelatedPrefix())
        {
          if (! prefixToPEC.containsKey(prefix))
          {
            prefixToPEC.put(prefix, new HashSet<>());
          }
          prefixToPEC.get(prefix).add(pec);
        }
    }



    PrefixRelationshipGenerator.setPrefixToPEC(prefixToPEC);
    PrefixRelationshipGenerator.setPrefixFactory(prefixFactory);
    PrefixRelationshipGenerator.setPECToPECNum(PECToPECNum);
    long timestampPECSliceEnd = System.currentTimeMillis();

    System.out.println("Slice PEC end! Time is: " + (timestampPECSliceEnd - timestampPECSliceBegin));









    // 针对于每一个邻居生成对应的外部路由宣告
    System.out.println("Generate External Route!");
    BitSet allPrefixEc = new BitSet();
    for (Integer pec : PECToPECNum.values())
    {
      allPrefixEc.set(pec);
    }
    long timestampExternalRouteGeneratorBegin = System.currentTimeMillis();
    HashMap<ExternalNeighbor, List<SymBgpRoute.Builder>> extBgpRoute = new HashMap<>();
    Integer extRouteIndex = 0;
    HashMap<ExternalNeighbor, HashMap<Integer, Integer>> extNeighborToVariable = new HashMap<>();
    for (ExternalNeighbor externalNeighbor : extNeighborFeature.getExtNeighbor())
    {
      if (!extBgpRoute.containsKey(externalNeighbor))
      {
        extBgpRoute.put(externalNeighbor, new ArrayList<>());
        extNeighborToVariable.put(externalNeighbor, new HashMap<>());
      }
      for (Integer cec : perNeighborCEC.get(externalNeighbor))
      {
        Integer extNeighborVariable = topologyBDD.createVar();
        extRouteIndex++;
        extNeighborToVariable.get(externalNeighbor).put(extRouteIndex, extNeighborVariable);

        BitSet extPrefix = (BitSet) allPrefixEc.clone();
        SymBgpRoute.Builder extRouteBuilder = new Builder(extPrefix, topologyBDD, prefixFactory);
        extRouteBuilder.setLocalPreference(100);
        extRouteBuilder.setCommunity(cec);
        List<Integer> asPath = new ArrayList<>();
        asPath.add(externalNeighbor.getAsNumber());
        extRouteBuilder.setAsPath(asPath);
        extRouteBuilder.setAsPathSize(1);
        extRouteBuilder.setTopologyCondition(extNeighborVariable);
        extRouteBuilder.setNextHopIp(externalNeighbor.getPrefix().getStartIp());
        extRouteBuilder.setOriginatorIp(externalNeighbor.getPrefix().getStartIp());
        extRouteBuilder.setRoutingProtocol(RoutingProtocol.BGP);
        extRouteBuilder.setReason(org.batfish.symwork.Reason.ADD);

        List<String> nodePath = new ArrayList<>();
        nodePath.add(externalNeighbor.getAsNumber() + "-" + externalNeighbor.getPrefix().toString());
        extRouteBuilder.setNodePath(nodePath);






        extRouteBuilder.setExternal(true);
        extRouteBuilder.setOriginatorIndex(extRouteIndex);
        extBgpRoute.get(externalNeighbor).add(extRouteBuilder);
//        for (Integer pec : PECToPECNum.values())
//        {
//          SymBgpRoute.Builder extRouteBuilder = new Builder(pec, topologyBDD, prefixFactory);
//          extRouteBuilder.setLocalPreference(100);
//          extRouteBuilder.setCommunity(cec);
//          List<Integer> asPath = new ArrayList<>();
//          asPath.add(externalNeighbor.getAsNumber());
//          extRouteBuilder.setAsPath(asPath);
//          extRouteBuilder.setAsPathSize(1);
//          extRouteBuilder.setTopologyCondition(extNeighborVariable);
//          extRouteBuilder.setNextHopIp(externalNeighbor.getPrefix().getStartIp());
//          extRouteBuilder.setOriginatorIp(externalNeighbor.getPrefix().getStartIp());
//          extRouteBuilder.setRoutingProtocol(RoutingProtocol.BGP);
//          extRouteBuilder.setReason(org.batfish.symwork.Reason.ADD);
//
//          List<String> nodePath = new ArrayList<>();
//          nodePath.add(externalNeighbor.getAsNumber() + "-" + externalNeighbor.getPrefix().toString());
//          extRouteBuilder.setNodePath(nodePath);
//
//
//
//
//
//
//          extRouteBuilder.setExternal(true);
//          extRouteBuilder.setOriginatorIndex(extRouteIndex);
//          extBgpRoute.get(externalNeighbor).add(extRouteBuilder);
//        }
      }
    }
    long timestampExternalRouteGeneratorEnd = System.currentTimeMillis();
    System.out.println("Generate External Route End, time is" + (timestampExternalRouteGeneratorEnd - timestampExternalRouteGeneratorBegin));


    //confirm the relationship of prefixEc
    //    HashMap<Integer,ArrayList<Integer>> prefixEcRelationship=new HashMap<>();
    //    for(Integer ec:prefixEC.values())
    //    {
    //      prefixEcRelationship.put(ec,new ArrayList<>());
    //    }
    //    for(SymPrefixList outPrefixList:answer)
    //    {
    //      HashMap<BDD,Long> outPrefix=outPrefixList.getPrefixList();
    //      int outPrefixEc=prefixEC.get(outPrefixList);
    //      for(SymPrefixList inPrefixList:answer)
    //      {
    //        HashMap<BDD,Long> inPrefix=inPrefixList.getPrefixList();
    //        int inPrefixEc=prefixEC.get(inPrefixList);
    //        for(BDD outIp:outPrefix.keySet())
    //        {
    //          for(BDD inIp:inPrefix.keySet())
    //          {
    //            if(!outIp.and(inIp).isZero()&&(outPrefix.get(outIp)&inPrefix.get(inIp))!=0)
    //            {
    //              prefixEcRelationship.get(outPrefixEc).add(inPrefixEc);
    //            }
    //          }
    //        }
    //      }
    //    }




    // she zhi jie dian jian de lin jie guan xi
//    try {
//      nodes.values().forEach(n -> {
//        String name = n.getName();
//        Set<Edge> edge = topology.getNodeEdges().get(name);
//        for (Edge tempEdge : edge) {
//          if (tempEdge.getNode2().equals(n.getName())) {
//            n.getConfiguration().getIncomingEdges().put(tempEdge.getNode1(), tempEdge);
//          }
//        }
//      });
//    }catch (Exception e)
//    {
//      System.out.println("set-edge-link-error"+e);
//      e.printStackTrace();
//    }


    IsisTopology isisTopology=IsisTopology.initIsisTopology(
        configurations, topology);
    System.out.println(isisTopology.getNetwork().toString());

    //ROUTEPOLICY DENYROUTE
    SortedMap<String, Map<Integer,List<SymBgpRoute>>> denyList=new TreeMap<>();

    //writed by lxz,为每一条边编码,同时将解析完的边存储到特定路径
    //每一条边进行BDD编码
    //BEGIN
    Integer edge_num=0;
    HashMap<String,Integer> topology_edge_num=new HashMap<>();

    System.out.println("Step:edge encode");
    long timeStampsEdgeEncodeBegin = System.currentTimeMillis();
    try {
      for (Node n : nodes.values()) {
        for (VirtualRouter vr : n.getVirtualRouters().values())
        {
          if (vr._vrf.getBgpProcess() == null || !vr._vrf.getName().equals("default"))
          {
            continue;
          }
          for (BgpNeighbor neighbor : vr._vrf.getBgpProcess().getNeighbors().values())
          {
            if (!bgpTopology.nodes().contains(neighbor) || bgpTopology.adjacentNodes(neighbor).size() == 0)
            {
              continue;
            }
            for (BgpNeighbor remoteNeighbor : bgpTopology.adjacentNodes(neighbor))
            {
              String edgeName = neighbor.getOwner().getHostname() + "-" + remoteNeighbor.getOwner().getHostname();
              String reverseName = remoteNeighbor.getOwner().getHostname() + "-" + neighbor.getOwner().getHostname();
              if (!topologyEdgeBDD.containsKey(edgeName) && !topologyEdgeBDD.containsKey(reverseName))
              {
                int edgeBDD = topologyBDD.createVar();
                topologyEdgeBDDList.add(edgeBDD);
                topologyEdgeBDD.put(edgeName, edgeBDD);
                topologyEdgeBDD.put(reverseName, edgeBDD);
                edge_num++;
              }
            }
          }
        }
      }

      // for (Node n : nodes.values()) {
      //   for (Edge edge : n.getConfiguration().getIncomingEdges().values()) {
      //     String edgeName = edge.getNode1() + "-" + edge.getNode2();
      //     String edgeNameReverse = edge.getNode2() + "-" + edge.getNode1();
      //     denyList.put(edgeName, new HashMap<>());
      //     denyList.put(edgeNameReverse, new HashMap<>());
      //     if (!topologyEdgeBDD.containsKey(edgeName)) {
      //       int edgeBDD=topologyBDD.createVar();
      //       topologyEdgeBDDList.add(edgeBDD);
      //       topologyEdgeBDD.put(edgeName, edgeBDD);
      //       topologyEdgeBDD.put(edgeNameReverse, edgeBDD);
      //       edge_num++;
      //     }
      //   }
      //   for (VirtualRouter vr : n.getVirtualRouters().values()) {
      //     if (vr._vrf.getBgpProcess() == null) {
      //       continue;
      //     }
      //     for (BgpNeighbor neighbor : vr._vrf.getBgpProcess().getNeighbors().values()) {
      //       if (neighbor.getLocalAs().equals(neighbor.getRemoteAs())) {
      //         continue;
      //       }
      //       if (!bgpTopology.nodes().contains(neighbor)||bgpTopology.adjacentNodes(neighbor).size()==0) {
      //         int edgeBDD=topologyBDD.createVar();
      //         topologyEdgeBDDList.add(edgeBDD);
      //         topologyEdgeBDD.put(
      //             n.getConfiguration().getHostname() + "-" + neighbor.getRemoteAs(),
      //             edgeBDD);
      //         topologyEdgeBDD.put(
      //             neighbor.getRemoteAs() + "-" + n.getConfiguration().getHostname(),
      //             edgeBDD);
      //         edge_num++;
      //       }
      //     }
      //   }
      // }
    }catch (Exception e){
      System.out.println("edge-encode-error:");
      e.printStackTrace();
    }



    //baseTopology
    int KFailures=0;
    int kFailuresTopology=0;
    //k=0
       int topologyFilter=0;
       int topologyFilterBase=1;

    if (KFailures == 0)
    {
      for(int edgeBDD:topologyEdgeBDD.values())
      {
        topologyFilterBase=topologyBDD.and(topologyFilterBase,edgeBDD);
      }
      topologyFilter=topologyBDD.or(topologyFilter,topologyFilterBase);
      kFailuresTopology=topologyBDD.or(topologyFilterBase,kFailuresTopology);

      topologyBDD.ref(topologyFilterBase);
      topologyBDD.ref(topologyFilter);
      topologyBDD.ref(kFailuresTopology);
    } else if (KFailures == 1)
    {
      for(int edgeBDD:topologyEdgeBDD.values())
      {
        topologyFilterBase=topologyBDD.and(topologyFilterBase,edgeBDD);
      }
      topologyBDD.ref(topologyFilterBase);
      topologyFilter=topologyBDD.or(topologyFilterBase,topologyFilter);

      try {
        for(int edgeBDD:topologyEdgeBDDList)
        {
          int temp=topologyBDD.exists(topologyFilterBase,edgeBDD);
          temp=topologyBDD.and(temp,topologyBDD.not(edgeBDD));
          topologyBDD.ref(temp);
          topologyFilter=topologyBDD.or(topologyFilter,temp);
          kFailuresTopology=topologyBDD.or(kFailuresTopology,temp);
          topologyBDD.deref(temp);
        }
        topologyBDD.ref(topologyFilter);
        topologyBDD.ref(kFailuresTopology);
      }catch (Exception e)
      {
        e.printStackTrace();
      }
    } else if (KFailures == 2)
    {
      HashSet<String> convertSet=new HashSet<>();
      for(int edgeBDD:topologyEdgeBDDList)
      {
        topologyFilterBase=topologyBDD.and(topologyFilterBase,edgeBDD);
      }
      try {
        topologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, topologyFilterBase));
        topologyBDD.ref(topologyFilterBase);
        for (int index1=0;index1<topologyEdgeBDDList.size();index1++)
        {
          for (int index2=index1+1;index2<topologyEdgeBDDList.size();index2++)
          {
            if(convertSet.contains(topologyEdgeBDDList.get(index1)+"-"+topologyEdgeBDDList.get(index2)))
            {
              continue;
            }
            convertSet.add(topologyEdgeBDDList.get(index1)+"-"+topologyEdgeBDDList.get(index2));
            convertSet.add(topologyEdgeBDDList.get(index2)+"-"+topologyEdgeBDDList.get(index1));
            int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
            int temp2 = topologyBDD.ref(topologyBDD.exists(temp, topologyEdgeBDDList.get(index2)));
            int temp3 = topologyBDD.ref(topologyBDD.and(temp2, topologyBDD.not(topologyEdgeBDDList.get(index1))));
            int temp4 = topologyBDD.ref(topologyBDD.and(temp3, topologyBDD.not(topologyEdgeBDDList.get(index2))));
            int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp4));
            int tempKFailuresTopology = topologyBDD.ref(topologyBDD.or(kFailuresTopology, temp4));
            topologyBDD.deref(topologyFilter);
            topologyBDD.deref(kFailuresTopology);
            topologyFilter=tempTopologyFilter;
            kFailuresTopology=tempKFailuresTopology;
            topologyBDD.deref(temp);
            topologyBDD.deref(temp2);
            topologyBDD.deref(temp3);
            topologyBDD.deref(temp4);
          }
          int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
          int temp2 = topologyBDD.ref(topologyBDD.and(temp, topologyBDD.not(topologyEdgeBDDList.get(index1))));
          int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp));
          topologyBDD.deref(topologyFilter);
          topologyFilter=tempTopologyFilter;
          topologyBDD.deref(temp);
          topologyBDD.deref(temp2);
        }
        for (int outEdgeBDD : topologyEdgeBDDList) {
          for (int inEdgeBDD : topologyEdgeBDDList) {
            if (outEdgeBDD == inEdgeBDD) {
              continue;
            }
            if(convertSet.contains(outEdgeBDD+"-"+inEdgeBDD))
            {
              continue;
            }
            convertSet.add(outEdgeBDD+"-"+inEdgeBDD);
            convertSet.add(inEdgeBDD+"-"+outEdgeBDD);
            int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, outEdgeBDD));
            int temp2 = topologyBDD.ref(topologyBDD.exists(temp, inEdgeBDD));
            int temp3 = topologyBDD.ref(topologyBDD.and(temp2, topologyBDD.not(outEdgeBDD)));
            int temp4 = topologyBDD.ref(topologyBDD.and(temp3, topologyBDD.not(inEdgeBDD)));
            int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp4));
            int tempKFailuresTopology = topologyBDD.ref(topologyBDD.or(kFailuresTopology, temp4));
            topologyBDD.deref(topologyFilter);
            topologyBDD.deref(kFailuresTopology);
            topologyFilter=tempTopologyFilter;
            kFailuresTopology=tempKFailuresTopology;
            topologyBDD.deref(temp);
            topologyBDD.deref(temp2);
            topologyBDD.deref(temp3);
            topologyBDD.deref(temp4);
          }
          int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, outEdgeBDD));
          int temp2 = topologyBDD.ref(topologyBDD.and(temp, topologyBDD.not(outEdgeBDD)));
          int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp));
          topologyBDD.deref(topologyFilter);
          topologyFilter=tempTopologyFilter;
          topologyBDD.deref(temp);
          topologyBDD.deref(temp2);
        }
      }catch (Exception e)
      {
        System.out.println("topologyFilter-error");
        e.printStackTrace();
      }
    } else if (KFailures == 3)
    {
       HashSet<String> convertSet=new HashSet<>();
       for(int edgeBDD:topologyEdgeBDDList)
       {
         topologyFilterBase=topologyBDD.and(topologyFilterBase,edgeBDD);
       }
       try {
         topologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, topologyFilterBase));
         topologyBDD.ref(topologyFilterBase);
         for (int index1=0; index1<topologyEdgeBDDList.size(); index1++)
         {
           for (int index2=index1+1; index2<topologyEdgeBDDList.size(); index2++)
           {
             for (int index3=index2+1; index3<topologyEdgeBDDList.size(); index3++)
             {
               int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
               int temp2 = topologyBDD.ref(topologyBDD.exists(temp, topologyEdgeBDDList.get(index2)));
               int temp3 = topologyBDD.ref(topologyBDD.exists(temp2, topologyEdgeBDDList.get(index3)));
               int temp4 = topologyBDD.ref(topologyBDD.and(temp3, topologyBDD.not(topologyEdgeBDDList.get(index1))));
               int temp5 = topologyBDD.ref(topologyBDD.and(temp4, topologyBDD.not(topologyEdgeBDDList.get(index2))));
               int temp6 = topologyBDD.ref(topologyBDD.and(temp5, topologyBDD.not(topologyEdgeBDDList.get(index3))));
               int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp6));
               int tempKFailuresTopology = topologyBDD.ref(topologyBDD.or(kFailuresTopology, temp6));
               topologyBDD.deref(topologyFilter);
               topologyBDD.deref(kFailuresTopology);
               topologyFilter=tempTopologyFilter;
               kFailuresTopology=tempKFailuresTopology;
               topologyBDD.deref(temp);
               topologyBDD.deref(temp2);
               topologyBDD.deref(temp3);
               topologyBDD.deref(temp4);
               topologyBDD.deref(temp5);
               topologyBDD.deref(temp6);
             }

             if(convertSet.contains(topologyEdgeBDDList.get(index1)+"-"+topologyEdgeBDDList.get(index2)))
             {
               continue;
             }
             convertSet.add(topologyEdgeBDDList.get(index1)+"-"+topologyEdgeBDDList.get(index2));
             convertSet.add(topologyEdgeBDDList.get(index2)+"-"+topologyEdgeBDDList.get(index1));
             int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
             int temp2 = topologyBDD.ref(topologyBDD.exists(temp, topologyEdgeBDDList.get(index1)));
             int temp3 = topologyBDD.ref(topologyBDD.and(temp2, topologyBDD.not(topologyEdgeBDDList.get(index1))));
             int temp4 = topologyBDD.ref(topologyBDD.and(temp3, topologyBDD.not(topologyEdgeBDDList.get(index1))));
             int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp4));
             int tempKFailuresTopology = topologyBDD.ref(topologyBDD.or(kFailuresTopology, temp4));
             topologyBDD.deref(topologyFilter);
             topologyBDD.deref(kFailuresTopology);
             topologyFilter=tempTopologyFilter;
             kFailuresTopology=tempKFailuresTopology;
             topologyBDD.deref(temp);
             topologyBDD.deref(temp2);
             topologyBDD.deref(temp3);
             topologyBDD.deref(temp4);
           }
           int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
           int temp2 = topologyBDD.ref(topologyBDD.and(temp, topologyBDD.not(topologyEdgeBDDList.get(index1))));
           int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp));
           topologyBDD.deref(topologyFilter);
           topologyFilter=tempTopologyFilter;
           topologyBDD.deref(temp);
           topologyBDD.deref(temp2);
         }
       }catch (Exception e)
       {
         System.out.println("topologyFilter-error");
         e.printStackTrace();
       }
    }
    //k=1
    //    int topologyFilter=0;
    //    int topologyFilterBase=1;
    //    for(int edgeBDD:topologyEdgeBDD.values())
    //    {
    //      topologyFilterBase=topologyBDD.and(topologyFilterBase,edgeBDD);
    //    }
    //
    //    topologyBDD.ref(topologyFilterBase);
    //    topologyFilter=topologyBDD.or(topologyFilterBase,topologyFilter);
    //
    //    try {
    //      for(int edgeBDD:topologyEdgeBDDList)
    //      {
    //        int temp=topologyBDD.exists(topologyFilterBase,edgeBDD);
    //        temp=topologyBDD.and(temp,topologyBDD.not(edgeBDD));
    //        topologyBDD.ref(temp);
    //        topologyFilter=topologyBDD.or(topologyFilter,temp);
    //        kFailuresTopology=topologyBDD.or(kFailuresTopology,temp);
    //        topologyBDD.deref(temp);
    //      }
    //      topologyBDD.ref(topologyFilter);
    //      topologyBDD.ref(kFailuresTopology);
    //    }catch (Exception e)
    //    {
    //      e.printStackTrace();
    //    }

    //k = 2
//    int topologyFilter=0;
//    int topologyFilterBase=1;
//    HashSet<String> convertSet=new HashSet<>();
//    for(int edgeBDD:topologyEdgeBDDList)
//    {
//      topologyFilterBase=topologyBDD.and(topologyFilterBase,edgeBDD);
//    }
//    try {
//      topologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, topologyFilterBase));
//      topologyBDD.ref(topologyFilterBase);
//      for (int index1=0;index1<topologyEdgeBDDList.size();index1++)
//      {
//        for (int index2=index1+1;index2<topologyEdgeBDDList.size();index2++)
//        {
//          if(convertSet.contains(topologyEdgeBDDList.get(index1)+"-"+topologyEdgeBDDList.get(index2)))
//          {
//            continue;
//          }
//          convertSet.add(topologyEdgeBDDList.get(index1)+"-"+topologyEdgeBDDList.get(index2));
//          convertSet.add(topologyEdgeBDDList.get(index2)+"-"+topologyEdgeBDDList.get(index1));
//          int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
//          int temp2 = topologyBDD.ref(topologyBDD.exists(temp, topologyEdgeBDDList.get(index2)));
//          int temp3 = topologyBDD.ref(topologyBDD.and(temp2, topologyBDD.not(topologyEdgeBDDList.get(index1))));
//          int temp4 = topologyBDD.ref(topologyBDD.and(temp3, topologyBDD.not(topologyEdgeBDDList.get(index2))));
//          int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp4));
//          int tempKFailuresTopology = topologyBDD.ref(topologyBDD.or(kFailuresTopology, temp4));
//          topologyBDD.deref(topologyFilter);
//          topologyBDD.deref(kFailuresTopology);
//          topologyFilter=tempTopologyFilter;
//          kFailuresTopology=tempKFailuresTopology;
//          topologyBDD.deref(temp);
//          topologyBDD.deref(temp2);
//          topologyBDD.deref(temp3);
//          topologyBDD.deref(temp4);
//        }
//        int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
//        int temp2 = topologyBDD.ref(topologyBDD.and(temp, topologyBDD.not(topologyEdgeBDDList.get(index1))));
//        int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp));
//        topologyBDD.deref(topologyFilter);
//        topologyFilter=tempTopologyFilter;
//        topologyBDD.deref(temp);
//        topologyBDD.deref(temp2);
//      }
//      for (int outEdgeBDD : topologyEdgeBDDList) {
//        for (int inEdgeBDD : topologyEdgeBDDList) {
//          if (outEdgeBDD == inEdgeBDD) {
//            continue;
//          }
//          if(convertSet.contains(outEdgeBDD+"-"+inEdgeBDD))
//          {
//            continue;
//          }
//          convertSet.add(outEdgeBDD+"-"+inEdgeBDD);
//          convertSet.add(inEdgeBDD+"-"+outEdgeBDD);
//          int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, outEdgeBDD));
//          int temp2 = topologyBDD.ref(topologyBDD.exists(temp, inEdgeBDD));
//          int temp3 = topologyBDD.ref(topologyBDD.and(temp2, topologyBDD.not(outEdgeBDD)));
//          int temp4 = topologyBDD.ref(topologyBDD.and(temp3, topologyBDD.not(inEdgeBDD)));
//          int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp4));
//          int tempKFailuresTopology = topologyBDD.ref(topologyBDD.or(kFailuresTopology, temp4));
//          topologyBDD.deref(topologyFilter);
//          topologyBDD.deref(kFailuresTopology);
//          topologyFilter=tempTopologyFilter;
//          kFailuresTopology=tempKFailuresTopology;
//          topologyBDD.deref(temp);
//          topologyBDD.deref(temp2);
//          topologyBDD.deref(temp3);
//          topologyBDD.deref(temp4);
//        }
//        int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, outEdgeBDD));
//        int temp2 = topologyBDD.ref(topologyBDD.and(temp, topologyBDD.not(outEdgeBDD)));
//        int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp));
//        topologyBDD.deref(topologyFilter);
//        topologyFilter=tempTopologyFilter;
//        topologyBDD.deref(temp);
//        topologyBDD.deref(temp2);
//      }
//    }catch (Exception e)
//    {
//      System.out.println("topologyFilter-error");
//      e.printStackTrace();
//    }

    //k=3
    // int topologyFilter=0;
    // int topologyFilterBase=1;
    // HashSet<String> convertSet=new HashSet<>();
    // for(int edgeBDD:topologyEdgeBDDList)
    // {
    //   topologyFilterBase=topologyBDD.and(topologyFilterBase,edgeBDD);
    // }
    // try {
    //   topologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, topologyFilterBase));
    //   topologyBDD.ref(topologyFilterBase);
    //   for (int index1=0; index1<topologyEdgeBDDList.size(); index1++)
    //   {
    //     for (int index2=index1+1; index2<topologyEdgeBDDList.size(); index2++)
    //     {
    //       for (int index3=index2+1; index3<topologyEdgeBDDList.size(); index3++)
    //       {
    //         int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
    //         int temp2 = topologyBDD.ref(topologyBDD.exists(temp, topologyEdgeBDDList.get(index2)));
    //         int temp3 = topologyBDD.ref(topologyBDD.exists(temp2, topologyEdgeBDDList.get(index3)));
    //         int temp4 = topologyBDD.ref(topologyBDD.and(temp3, topologyBDD.not(topologyEdgeBDDList.get(index1))));
    //         int temp5 = topologyBDD.ref(topologyBDD.and(temp4, topologyBDD.not(topologyEdgeBDDList.get(index2))));
    //         int temp6 = topologyBDD.ref(topologyBDD.and(temp5, topologyBDD.not(topologyEdgeBDDList.get(index3))));
    //         int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp6));
    //         int tempKFailuresTopology = topologyBDD.ref(topologyBDD.or(kFailuresTopology, temp6));
    //         topologyBDD.deref(topologyFilter);
    //         topologyBDD.deref(kFailuresTopology);
    //         topologyFilter=tempTopologyFilter;
    //         kFailuresTopology=tempKFailuresTopology;
    //         topologyBDD.deref(temp);
    //         topologyBDD.deref(temp2);
    //         topologyBDD.deref(temp3);
    //         topologyBDD.deref(temp4);
    //         topologyBDD.deref(temp5);
    //         topologyBDD.deref(temp6);
    //       }

    //       if(convertSet.contains(topologyEdgeBDDList.get(index1)+"-"+topologyEdgeBDDList.get(index2)))
    //       {
    //         continue;
    //       }
    //       convertSet.add(topologyEdgeBDDList.get(index1)+"-"+topologyEdgeBDDList.get(index2));
    //       convertSet.add(topologyEdgeBDDList.get(index2)+"-"+topologyEdgeBDDList.get(index1));
    //       int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
    //       int temp2 = topologyBDD.ref(topologyBDD.exists(temp, topologyEdgeBDDList.get(index1)));
    //       int temp3 = topologyBDD.ref(topologyBDD.and(temp2, topologyBDD.not(topologyEdgeBDDList.get(index1))));
    //       int temp4 = topologyBDD.ref(topologyBDD.and(temp3, topologyBDD.not(topologyEdgeBDDList.get(index1))));
    //       int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp4));
    //       int tempKFailuresTopology = topologyBDD.ref(topologyBDD.or(kFailuresTopology, temp4));
    //       topologyBDD.deref(topologyFilter);
    //       topologyBDD.deref(kFailuresTopology);
    //       topologyFilter=tempTopologyFilter;
    //       kFailuresTopology=tempKFailuresTopology;
    //       topologyBDD.deref(temp);
    //       topologyBDD.deref(temp2);
    //       topologyBDD.deref(temp3);
    //       topologyBDD.deref(temp4);
    //     }
    //     int temp = topologyBDD.ref(topologyBDD.exists(topologyFilterBase, topologyEdgeBDDList.get(index1)));
    //     int temp2 = topologyBDD.ref(topologyBDD.and(temp, topologyBDD.not(topologyEdgeBDDList.get(index1))));
    //     int tempTopologyFilter = topologyBDD.ref(topologyBDD.or(topologyFilter, temp));
    //     topologyBDD.deref(topologyFilter);
    //     topologyFilter=tempTopologyFilter;
    //     topologyBDD.deref(temp);
    //     topologyBDD.deref(temp2);
    //   }
    // }catch (Exception e)
    // {
    //   System.out.println("topologyFilter-error");
    //   e.printStackTrace();
    // }

    long timeStampsEdgeEncodeEnd = System.currentTimeMillis();
    System.out.println("Edge encode end, time is: " + (timeStampsEdgeEncodeEnd - timeStampsEdgeEncodeBegin));
    List<String> pruneNode=new ArrayList<>();
    nodes
        .values()
        .forEach(
            n->{
              // if(n.getConfiguration()._incomingEdges.values().size()<KFailures+1)
              // {
              //   pruneNode.add(n.getConfiguration().getHostname());
              // }
              for (VirtualRouter vr : n.getVirtualRouters().values())
              {
                if (vr._vrf.getBgpProcess() == null || !vr._vrf.getName().equals("default"))
                {
                  continue;
                }
                int outEdgeNum = 0;
                for (BgpNeighbor neighbor : vr._vrf.getBgpProcess().getNeighbors().values())
                {
                  if (!bgpTopology.nodes().contains(neighbor) || bgpTopology.adjacentNodes(neighbor).size() == 0)
                  {
                    continue;
                  }
                  outEdgeNum++;
                }
                if (outEdgeNum < KFailures + 1)
                {
                  pruneNode.add(n.getConfiguration().getHostname());
                }
              }
            }
        );

    //set PrefixFactory and TopologyFactory for each node
    // 初始化路由表，并且初始化对应的IGP路由
    System.out.println("Step:set prefix Factory");
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                vr.setPrefixFactory(prefixFactory);
                vr.initForIgpComputation(); // 初始化静态路由和直连路由
                vr.activateStaticRoutes();
//                vr.initSymBgpRib(topologyBDD,topologyFilter,topologyEdgeBDD); // 初始化路由表
                vr.symInitBgpQueuesAndDeltaBuilders(nodes,topology,bgpTopology); //初始化BGP的消息队列
              }
              for(String Rl:n.getConfiguration().getRouteFilterLists().keySet())
              {
                n.getConfiguration().getRouteFilterLists().get(Rl).setBDDFactory(prefixFactory);
              }
            });
    boolean multiPath = true;
    for (Node n:nodes.values())
    {
      for(VirtualRouter vr:n.getVirtualRouters().values())
      {
        vr.initSymBgpRib(topologyBDD,topologyFilter,topologyEdgeBDD, multiPath);
      }
      for(String Rl:n.getConfiguration().getRouteFilterLists().keySet())
      {
        for (RouteFilterLine rlLine : n.getConfiguration().getRouteFilterLists().get(Rl).getLines())
        {
          rlLine.toPEC();
        }
      }
      for (ExplicitPrefixSet explicitPrefixSet : n.getConfiguration().getExplicitPrefixSet())
      {
        explicitPrefixSet.toPEC();
      }
    }



    // System.out.println("Step:set isis queue");
    // nodes
    //     .values()
    //     .forEach(
    //         n->{
    //           for(VirtualRouter vr:n.getVirtualRouters().values())
    //           {
    //             vr.initIsisQueues(isisTopology);
    //           }
    //         }
    //     );

//    nodes
//        .values()
//        .forEach(
//            n->{
//              for(VirtualRouter vr:n.getVirtualRouters().values())
//              {
//                vr.initIsisExports(1,nodes,configurations);
//              }
//            }
//        );
//    boolean isisIsOscillating = computeIsisRoutes(nodes,ipOwners,configurations,isisTopology,topology,ae);
//    boolean isisIsOscillatings = computeMultiIsisRoutes(nodes,ipOwners,configurations,isisTopology,topology,ae);









// 初始化BGP路由表
    HashMap<String,List<Prefix>> prefixOwnersActualPrefix=new HashMap<>();
    HashMap<String,List<Integer>> prefixOwnersPrefixEc=new HashMap<>();

    // 获取每个节点拥有的前缀
    nodes
        .values()
        .forEach(
            n->{
              prefixOwnersActualPrefix.put(n.getConfiguration().getHostname(),new ArrayList<>());
              prefixOwnersPrefixEc.put(n.getConfiguration().getHostname(),new ArrayList<>());
            }
        );
    try{
      nodes
          .values()
          .forEach(
              n->{
                for(VirtualRouter vr:n.getVirtualRouters().values())
                {
                  if (!vr._vrf.getName().equals("default"))
                  {
                    continue;
                  }
                  Set<AbstractRoute> allRoutes=vr._mainRib.getRoutes();
                  for(AbstractRoute allRoute:allRoutes)
                  {
                    prefixOwnersActualPrefix.get(n.getConfiguration().getHostname()).add(allRoute.getNetwork());
                    SymPrefixList symPrefix= SingleConvertPrefixToSymPrefix.convert(allRoute.getNetwork().toString(),prefixFactory);
                    if(!PECToPECNum.containsKey(symPrefix))
                    {
                      System.out.println("prefixToPECNum error - do not have the static or connect prefix!");
                      continue;
                    }else{
                      prefixOwnersPrefixEc.get(n.getConfiguration().getHostname()).add(PECToPECNum.get(symPrefix));
                    }
                  }
                }
              }
          );
    }catch (Exception e){
      System.out.println("set prefix factory:"+e);
    }


// 过滤掉通过node prune方法判断为无效的节点拥有的前缀，简化计算的方法
    Set<Integer> prunePrefix=new HashSet<>();
    for(String nodeName:pruneNode)
    {
      prunePrefix.addAll(prefixOwnersPrefixEc.get(nodeName));
    }

// 初始化每个节点的BGP路由表
    try {
      initSymBgpInitialRoute(PECToPECNum, bgpTopology, nodes, pruneNode, prefixOwnersPrefixEc);
    }catch (Exception e)
    {
      e.printStackTrace();
    }
// 路由表初始化结束


    boolean isOscillating=false;
    try{
//      isOscillating = symComputeNonMonotonicPortionOfDataPlane( nodes, topology, dp, externalAdverts, ae, true, bgpTopology,externalRouteAdvertisement,isisTopology,nodeNameBgpTopology,configurations,ipOwners,denyList,routeIndexForest);
    }catch (Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Step:process external bgp");
    HashMap<Integer,Integer> routeIdnexToPrefixEC=new HashMap<>();
    boolean isOscillatingExternal=false;
    try {
      isOscillatingExternal=symComputeExternalMessageDataPlane(nodes, topology, dp, bgpTopology, nodeNameBgpTopology, configurations, ipOwners, extBgpRoute, ae);
    }catch (Exception e){
      e.printStackTrace();
    }
    if(isOscillatingExternal)
    {
      System.out.println("Oscilating!!!");
    }
    System.out.println("Step:end");
    long timestampMiddle = System.currentTimeMillis();
    System.out.println("simulate-time:"+(timestampMiddle - timestampBegin ));

    HashMap<ExternalNeighbor, HashMap<BgpNeighbor, List<SymBgpRoute>>> violationBTERoute = new HashMap<>();

//    判断每个邻居能够收到的外部路由宣告，保存在externalNeighborReceivedRouteList里面
    RegExp BTECommunity = new RegExp("11537:888");
    Automaton BTECommunityAutomaton = BTECommunity.toAutomaton();
    Integer BTEVar = communityAtomToBDD.get(communityAtomToIntger.get(BTECommunityAutomaton));
    HashMap<ExternalNeighbor, HashMap<BgpNeighbor, List<SymBgpRoute>>> externalNeighborReceivedRouteList = new HashMap<>();
    for (Node n : nodes.values())
    {
      for (VirtualRouter vr : n.getVirtualRouters().values())
      {
        if (vr._vrf.getBgpProcess() == null || !vr._vrf.getName().equals("default"))
        {
          continue;
        }
        for (BgpNeighbor neighbor : vr._vrf.getBgpProcess().getNeighbors().values())
        {
          if (!bgpTopology.nodes().contains(neighbor) || bgpTopology.adjacentNodes(neighbor).size() == 0)
          {
            if (neighbor.getRemoteAs() == 11537)
            {
              continue;
            }
            ExternalNeighbor externalNeighbor = new ExternalNeighbor(neighbor.getPrefix(), neighbor.getRemoteAs());

            if (!externalNeighborReceivedRouteList.containsKey(externalNeighbor))
            {
              externalNeighborReceivedRouteList.put(externalNeighbor, new HashMap<>());
            }
            if (!externalNeighborReceivedRouteList.get(externalNeighbor).containsKey(neighbor))
            {
              externalNeighborReceivedRouteList.get(externalNeighbor).put(neighbor, new ArrayList<>());
            }
            for (SymRouteAdvertisement<SymBgpRoute> extAdvertisement : vr.getExternalRouteList().get(neighbor))
            {
              SymBgpRoute extRoute = extAdvertisement.getRoute();
              Queue<SymBgpRoute> processOutputRoute=new LinkedList<>();
              processOutputRoute.add(extRoute);
              while(!processOutputRoute.isEmpty())
              {
                SymBgpRoute outputRoute=processOutputRoute.poll();
                SymBgpRoute.Builder transformedOutgoingRouteBuilder=new SymBgpRoute.Builder(outputRoute);
                String exportPolicyName = neighbor.getExportPolicy();

                SymPolicyAnswer acceptOutgoing=new SymPolicyAnswer();
                if (exportPolicyName != null) {
                  RoutingPolicy exportPolicy = vr.getConfiguration().getRoutingPolicies().get(exportPolicyName);
                  if (exportPolicy != null) {
                    acceptOutgoing =
                        exportPolicy.symProcess(
                            outputRoute,
                            transformedOutgoingRouteBuilder,
                            neighbor.getPrefix().getStartIp(),
                            neighbor.getPrefix(),
                            vr._vrf.getName(),
                            Direction.OUT);
                  }
                  if(acceptOutgoing._divideRotue!=null)
                  {
                    processOutputRoute.add(acceptOutgoing._divideRotue);
                  }
                }
                if (acceptOutgoing._accept)
                {
                  if (communityAtomBDD.and(transformedOutgoingRouteBuilder.getCommunity(), BTEVar) != 0)
                  {
                    System.out.println("break!");
                  }
                  transformedOutgoingRouteBuilder.addNodePath(n.getName());
                  externalNeighborReceivedRouteList.get(externalNeighbor).get(neighbor).add(transformedOutgoingRouteBuilder.build());
                }
              }

              SymBgpRoute extRouteTemp = extAdvertisement.getRoute();
              Queue<SymBgpRoute> processOutputRouteTemp=new LinkedList<>();
              processOutputRouteTemp.add(extRouteTemp);
              while(!processOutputRouteTemp.isEmpty())
              {
                SymBgpRoute outputRoute=processOutputRouteTemp.poll();
                SymBgpRoute.Builder transformedOutgoingRouteBuilder=new SymBgpRoute.Builder(outputRoute);
                String exportPolicyName = neighbor.getExportPolicy();

                SymPolicyAnswer acceptOutgoing=new SymPolicyAnswer();
                if (exportPolicyName != null) {
                  RoutingPolicy exportPolicy = vr.getConfiguration().getRoutingPolicies().get(exportPolicyName);
                  if (exportPolicy != null) {
                    acceptOutgoing =
                        exportPolicy.symProcess(
                            outputRoute,
                            transformedOutgoingRouteBuilder,
                            neighbor.getPrefix().getStartIp(),
                            neighbor.getPrefix(),
                            vr._vrf.getName(),
                            Direction.OUT);
                  }
                  if(acceptOutgoing._divideRotue!=null)
                  {
                    processOutputRouteTemp.add(acceptOutgoing._divideRotue);
                  }
                }
              }
            }
          }
        }
      }
    }


    for (ExternalNeighbor externalNeighbor : externalNeighborReceivedRouteList.keySet())
    {
      for (BgpNeighbor neighbor : externalNeighborReceivedRouteList.get(externalNeighbor).keySet())
      {
        for (SymBgpRoute route : externalNeighborReceivedRouteList.get(externalNeighbor).get(neighbor))
        {
          if (communityAtomBDD.and(route.getCommunity(), BTEVar) != 0)
          {
            if (!violationBTERoute.containsKey(externalNeighbor))
            {
              violationBTERoute.put(externalNeighbor, new HashMap<>());
            }
            if (!violationBTERoute.get(externalNeighbor).containsKey(neighbor))
            {
              violationBTERoute.get(externalNeighbor).put(neighbor, new ArrayList<>());
            }
            violationBTERoute.get(externalNeighbor).get(neighbor).add(route);
          }
        }
      }
    }

    System.out.println("BTE verification !");
    long  timestampsReachabilityBegin = System.currentTimeMillis();
    ForwardingEngine forwardingEngine = new ForwardingEngine(topologyBDD);
    forwardingEngine.generatingForwardingTable(nodes, topologyBDD);
    forwardingEngine.getAllPairReachability(allPrefixEc);
    long timestampsReachabilityEnd = System.currentTimeMillis();
    System.out.println("All pair reachability: " + (timestampsReachabilityEnd - timestampsReachabilityBegin));
//    HashMap<String, SymBgpFib> forwardingEngine;
//    for (Node n : nodes.values())
//    {
//      for (VirtualRouter vr : n.getVirtualRouters().values())
//      {
//        if (vr._vrf.getBgpProcess() == null || !vr._vrf.getName().equals("default"))
//        {
//          continue;
//        }
//        vr.getSymBgpRib().prepareForForwarding();
//        SymBgpFib symBgpFib = vr.getSymBgpFib();
//        SymBgpRib symBgpRib = vr.getSymBgpRib();
//        for (BitSet prefix : symBgpRib.getRib().keySet())
//        {
//          if (!symBgpFib.getFib().containsKey(prefix))
//          {
//            symBgpFib.getFib().put(prefix, new HashMap<>());
//          }
//          for (List<SymBgpRoute> routeList : symBgpRib.getRib().get(prefix))
//          {
//            for (SymBgpRoute route : routeList)
//            {
//              String nexthopNode = route.getNodePath().get(route.getNodePath().size() - 1);
//              if (! symBgpFib.getFib().get(prefix).containsKey(nexthopNode))
//              {
//                symBgpFib.getFib().get(prefix).put(nexthopNode, route.getTopologyCondition());
//              } else {
//                Integer forwardingTopologyCondition = symBgpFib.getFib().get(prefix).get(nexthopNode);
//                forwardingTopologyCondition = topologyBDD.ref(topologyBDD.or(forwardingTopologyCondition, route.getTopologyCondition()));
//                symBgpFib.getFib().get(prefix).put(nexthopNode, forwardingTopologyCondition);
//              }
//            }
//          }
//        }
//      }
//    }


    System.out.println("verification finish !");
//    try {
//      for(Integer routeIndex1:routeIdnexToPrefixEC.keySet())
//      {
//        Integer routePrefixEc=routeIdnexToPrefixEC.get(routeIndex1);
//        String asPath="5";
//        List<SortedSet<Integer>> asPathList=new ArrayList<>();
//
//        AsPath asPath1=new AsPath(asPathList);
//        SortedSet<Integer> newAsPathElement = new TreeSet<>();
//        newAsPathElement.add(5);
//
//        asPath1.addAsPath(newAsPathElement);
//
//
//        SymBounder aspathSize1=new SymBounder(0,Integer.MAX_VALUE);
//        List<TreeNode> descendants = routeIndexForest.get(routeIndex1).getAllChilds(new TreeNode("5",asPath1.hashCode(),aspathSize1.hashCode(),aspathSize1));
//        Set<String> descendantsValues = descendants.stream()
//            .map(node -> node.value)
//            .collect(Collectors.toSet());
//        List<Node> filteredNodes = nodes.entrySet().stream()
//            .filter(entry -> descendantsValues.contains(entry.getKey())) // 使用 Set 进行匹配
//            .map(Map.Entry::getValue)
//            .collect(Collectors.toList());
//        nodes.values()
//            //            .parallelStream()
//            .forEach(node -> {
//              for (VirtualRouter vr : node.getVirtualRouters().values()) {
//                vr._symBgpRib.clearTreeNodes();
//                vr._symBgpRib.removeRoute(routePrefixEc, routeIndex1, "5", new SymBounder(1, 3));
//                HashMap<TreeNode, Set<TreeNode>> repalceNodes = vr._symBgpRib.getReplaceTreeNodes();
//                HashMap<TreeNode, Set<TreeNode>> updateTreeNodes = vr._symBgpRib.getUpdateTreeNodes();
//                HashMap<TreeNode, Integer> updateIndexs = vr._symBgpRib.getUpdateRouteIndex();
//                routeIndexForest.get(routeIndex1).updateNodes(repalceNodes, true);
//                for (TreeNode treeNode : updateTreeNodes.keySet()) {
//                  Integer updateIndex = updateIndexs.get(treeNode);
//                  routeIndexForest.get(updateIndex).updateNodes(treeNode, updateTreeNodes.get(treeNode));
//                }
//              }
//            });
//      }
//    }catch (Exception e)
//    {
//      System.out.println("with-Draw-error");
//      e.printStackTrace();
//    }

    nodes
        .values()
        .forEach(
            n->{
              for(VirtualRouter vr:n.getVirtualRouters().values())
              {
                vr._symBgpRib.computeChangedRoute();
              }
            }
        );

//    System.out.println("------------------------最终的路由信息:-------------------");//对应拓扑结构
//    for(String node_name: node_print.keySet())
//    {
//      SortedMap<String, VirtualRouter> vrf_print = node_print.get(node_name).getVirtualRouters();
////      if(!node_name.equals("edge-62"))
////      {
////        continue;
////      }
//      System.out.println(node_name+":");
//      for(String vrf_name : vrf_print.keySet())
//      {
//        SymBgpRib rib=vrf_print.get(vrf_name)._symBgpRib;
//        Map<Integer,List<SymBgpRoute>> route=rib.getSimplifiedRoutes();
//        for(Integer prefix:route.keySet())
//        {;
//          System.out.println();
//          System.out.println();
//          System.out.println("---前缀的信息:---");
////          System.out.println(route.get(prefix).get(0).getPrefixEcNum());
//          System.out.println(prefix);
//          for(int i=0;i<route.get(prefix).size();i++)
//          {
//            System.out.println("Node-Path:"+route.get(prefix).get(i).getNodePath());
//            System.out.println("As-Path:"+route.get(prefix).get(i).getAsPath().toString());
//            System.out.println("permit-ommunity:"+route.get(prefix).get(i)._symCommunities._permitCommunities.toString());
//            System.out.println("deny-ommunity:"+route.get(prefix).get(i)._symCommunities._denyCommunities.toString());
//            System.out.println("拓扑对应的条件:");
//            route.get(prefix).get(i).getTopologyCondition().printSet();
//            System.out.println("Localpreference:"+route.get(prefix).get(i).getLocalPreference().getLowerbound()+"-"+route.get(prefix).get(i).getLocalPreference().getUpperbound());
//            System.out.println("AsPath:"+route.get(prefix).get(i).getAsPathSize().getLowerbound()+"-"+route.get(prefix).get(i).getAsPathSize().getUpperbound());
//            System.out.println("最终的AsPath长度:");
//            if(route.get(prefix).get(i).getAsPathSize().getUpperbound()==Integer.MAX_VALUE)
//            {
//              System.out.println(route.get(prefix).get(i).getAsPathSize().getLowerbound()+route.get(prefix).get(i).getAsPath().size()+"-"+route.get(prefix).get(i).getAsPathSize().getUpperbound());
//            }else{
//              int low=route.get(prefix).get(i).getAsPathSize().getLowerbound()+route.get(prefix).get(i).getAsPath().size();
//              int up=route.get(prefix).get(i).getAsPathSize().getUpperbound()+route.get(prefix).get(i).getAsPath().size();
//              System.out.println(low+"-"+up);
//            }
//            System.out.println("SrcACL:");
//            route.get(prefix).get(i).getSrcAcl().printSet();
//            System.out.println("DstAcl:");
//            route.get(prefix).get(i).getDstAcl().printSet();
//            System.out.println();
//          }
//
//        }
//      }
//      System.out.println();
//      System.out.println();
//      System.out.println();
//      System.out.println();
//      System.out.println();
//    }
//    System.out.println("----------------------------End-------------------------");

//    System.out.println("encode-edges:");
//    System.out.println(topology_edge_num.toString());
//    //reachability verifaction
////    String srcNode="R2";
////    String dstNode="R1";
////    getReachability(srcNode,dstNode,dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,factory,1);
//
    System.out.println();
    System.out.println();
//    System.out.println("----------verification answer:----------");
//    for(String dstNode:nodes.keySet())
//    {
//      for(String srcNode:nodes.keySet())
//      {
//        if(srcNode.equals(dstNode))
//        {
//          continue;
//        }
//        long timestampEnd = System.currentTimeMillis();
//        System.out.println("verification:"+(timestampEnd-timestampBegin));
//        getActualReachability(srcNode,dstNode,"70.0.5.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,kFailures);
//      }
//    }
//    List<String> unreachPairs=new ArrayList<>();
//    for(String srcNodeName:nodes.keySet())
//    {
//      for(String dstNodeName:prefixNetwork.keySet())
//      {
//        boolean reach=getActualReachability(srcNodeName,dstNodeName,"prefix",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,topologyBDD,prefixFactory,2,kFailuresTopology);
//        if(!reach)
//        {
//          unreachPairs.add(srcNodeName+"-"+dstNodeName);
//        }
//      }
//    }
    long timestampEnd3 = System.currentTimeMillis();
    String time="k0-verification:"+(timestampEnd3-timestampBegin);

    Path timePath= Paths.get("k0-symbolicTime");
    Path answerPath= Paths.get("k0-symbolicAnswer");

    StringBuilder answerVerification=new StringBuilder();
//    for(String unreach:unreachPairs)
//    {
//      answerVerification.append(unreach).append("\n");
//    }
    try {
      Files.write(timePath, time.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
      Files.write(answerPath, answerVerification.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      System.out.println("写入文件时出现错误: " + e.getMessage());
    }

//    getActualReachability("edge-6","edge-19","70.0.5.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,kFailures);
//    long timestampEnd = System.currentTimeMillis();
//    System.out.println("verification:"+(timestampEnd-timestampBegin));
    //    getActualReachability("edge-22","edge-29","70.0.4.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,1);
//    long timestampEnd = System.currentTimeMillis();
//    System.out.println("verification:"+(timestampEnd-timestampBegin));

//    getActualReachability("edge-20","edge-71","70.0.71.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,1);
//    long timestampEnd = System.currentTimeMillis();
//    System.out.println("one-verification:"+(timestampEnd-timestampBegin));

//    getActualReachability("edge-20","edge-79","70.0.79.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,1);
//    long timestampEnd2 = System.currentTimeMillis();
//    System.out.println("two-verification:"+(timestampEnd2-timestampBegin));


//    getActualReachability("edge-20","edge-39","70.0.39.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,1);
//    long timestampEnd3 = System.currentTimeMillis();
//    System.out.println("three-verification:"+(timestampEnd3-timestampBegin));

//    getActualReachability("edge-6","edge-19","70.0.19.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,1);
//    long timestampEnd = System.currentTimeMillis();
//    System.out.println("one-verification:"+(timestampEnd-timestampBegin));

//    getActualReachability("edge-6","edge-15","70.0.15.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,1);
//    long timestampEnd2 = System.currentTimeMillis();
//    System.out.println("two-verification:"+(timestampEnd2-timestampBegin));

//    getActualReachability("edge-6","edge-11","70.0.11.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,1);
//    long timestampEnd3 = System.currentTimeMillis();
//    System.out.println("three-verification:"+(timestampEnd3-timestampBegin));



//   wayPoint("R4","AS5","R2",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,1);
//    valleyFree("as65000_R1","AS65001","AS65002","",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,bgpTopology,nodes);
    //    getActualReachability("edge-62","70.0.92.0/24",dp,prefixOwnersPrefixEc,prefixEcToSymPrefix,prefixEcRelationship,prefixEC,ipOwners,factory,prefixFactory,0);
//    long timestampEnd = System.currentTimeMillis();
//    System.out.println("verification:"+(timestampEnd-timestampBegin));

//      for(SymPrefixList prefix:dstPrefix)
//      {
//        if(!srcRib.keySet().contains(prefix))
//        {
//          System.out.println("zhen dui yu qian zhui:");
//          for(BDD ip:prefix._prefixList.keySet())
//          {
//            ip.printSet();
//            System.out.println("chang du:"+prefix._prefixList.get(ip));
//          }
//          System.out.println("bu ke da!");
//          continue;
//        }
//        List<SymBgpRoute> initalRoute=srcRib.get(prefix);
//        List<SymBgpRoute> routes=new ArrayList<>();
//        BDDFactory factory1=initalRoute.get(0).getTopologyFactory();
//        BDD newTopologyC=factory1.one();
//        for(int i=0;i<initalRoute.size();i++)
//        {
//          if(!newTopologyC.and(initalRoute.get(i).getTopologyCondition()).equals(factory1.zero()))
//          {
//            SymBgpRoute temp=cloneBgpRoute(initalRoute.get(i));
//            temp.setTopologyCondition(newTopologyC.and(initalRoute.get(i).getTopologyCondition()));
//            routes.add(temp);
//          }
//          newTopologyC=newTopologyC.and(initalRoute.get(i).getTopologyCondition().not());
//          if(newTopologyC.equals(factory1.zero()))
//          {
//            break;
//          }
//        }
//        int firstExternalPos=-1;
//        int firstInternalPos=-1;
//        int secondExternalPos=-1;
//        for(int i=0;i<routes.size();i++)
//        {
//          if(routes.get(i)._external&&firstExternalPos==-1)
//          {
//            firstExternalPos=i;
//          }else if(!routes.get(i)._external&&firstInternalPos==-1)
//          {
//            firstInternalPos=i;
//          }else if(routes.get(i)._external&&firstExternalPos!=-1&&firstInternalPos!=-1)
//          {
//            secondExternalPos=i;
//            break;
//          }
//        }
//        if(firstExternalPos == -1)
//        {
//          for(int j=0;j<routes.size();j++)
//          {
//            System.out.println("---------------"+"ke da nei bu lu you:"+j+"---------------");
//            System.out.println("yuan jie dian:"+routes.get(j).getOriginatorIp());
//            System.out.println("as-path:"+routes.get(j).getAsPath().getAsPathString());
//            System.out.println("localpreference:lower:"+routes.get(j).getLocalPreference().getLowerbound()+";upper:"+routes.get(j).getLocalPreference().getUpperbound());
//            System.out.println("as-pathSize:lower:"+routes.get(j).getAsPathSize().getLowerbound()+";upper:"+routes.get(j).getAsPathSize().getUpperbound());
//            System.out.println("dui ying de tuo pu tiao jian:");
//            routes.get(j).getTopologyCondition().printSet();
//          }
//        }else if(firstExternalPos==0)
//        {
//          System.out.println("bu ke da!");
//          int lastExternalPos=firstInternalPos==-1?routes.size():firstInternalPos;
//          System.out.println("you yu wai bu lu you de cun zai:");
//          for(int j=0;j<lastExternalPos;j++)
//          {
//            System.out.println("---------------"+"wai bu lu you:"+j+"---------------");
//            System.out.println("yuan jie dian:"+routes.get(j).getOriginatorIp());
//            System.out.println("as-path:"+routes.get(j).getAsPath().getAsPathString());
//            System.out.println("localpreference:lower:"+routes.get(j).getLocalPreference().getLowerbound()+";upper:"+routes.get(j).getLocalPreference().getUpperbound());
//            System.out.println("as-pathSize:lower:"+routes.get(j).getAsPathSize().getLowerbound()+";upper:"+routes.get(j).getAsPathSize().getUpperbound());
//            System.out.println("dui ying de tuo pu tiao jian:");
//            routes.get(j).getTopologyCondition().printSet();
//          }
//          if(firstInternalPos==-1)
//          {
//            System.out.println("you yu wai bu lu you de cun zai,mei you nei bu lu you shi qi ke da!");
//          }else{
//            int lastInternalPos=secondExternalPos==-1?routes.size():secondExternalPos;
//            for(int j=firstInternalPos;j<lastInternalPos;j++)
//            {
//              System.out.println("---------------"+"ke da nei bu lu you:"+j+"---------------");
//              System.out.println("yuan jie dian:"+routes.get(j).getOriginatorIp());
//              System.out.println("as-path:"+routes.get(j).getAsPath().getAsPathString());
//              System.out.println("localpreference:lower:"+routes.get(j).getLocalPreference().getLowerbound()+";upper:"+routes.get(j).getLocalPreference().getUpperbound());
//              System.out.println("as-pathSize:lower:"+routes.get(j).getAsPathSize().getLowerbound()+";upper:"+routes.get(j).getAsPathSize().getUpperbound());
//              System.out.println("dui ying de tuo pu tiao jian:");
//              routes.get(j).getTopologyCondition().printSet();
//            }
//          }
//          if(secondExternalPos!=-1)
//          {
//            System.out.println("dang tuo po tiao jian wei ru xia qing kuang shi , reng hui dao xiang wai bu lu you:");
//            System.out.println("---------------"+"ke da nei bu lu you:"+secondExternalPos+"---------------");
//            System.out.println("yuan jie dian:"+routes.get(secondExternalPos).getOriginatorIp());
//            System.out.println("as-path:"+routes.get(secondExternalPos).getAsPath().getAsPathString());
//            System.out.println("localpreference:lower:"+routes.get(secondExternalPos).getLocalPreference().getLowerbound()+";upper:"+routes.get(secondExternalPos).getLocalPreference().getUpperbound());
//            System.out.println("as-pathSize:lower:"+routes.get(secondExternalPos).getAsPathSize().getLowerbound()+";upper:"+routes.get(secondExternalPos).getAsPathSize().getUpperbound());
//            System.out.println("dui ying de tuo pu tiao jian:");
//            routes.get(secondExternalPos).getTopologyCondition().printSet();
//
//          }
//        }else if(firstExternalPos != 0)
//        {
//          System.out.println("tong guo xia shu nei bu lu you ke da:");
//          for(int j=0;j<firstInternalPos;j++)
//          {
//            System.out.println("---------------"+"ke da nei bu lu you:"+j+"---------------");
//            System.out.println("yuan jie dian:"+routes.get(j).getOriginatorIp());
//            System.out.println("as-path:"+routes.get(j).getAsPath().getAsPathString());
//            System.out.println("localpreference:lower:"+routes.get(j).getLocalPreference().getLowerbound()+";upper:"+routes.get(j).getLocalPreference().getUpperbound());
//            System.out.println("as-pathSize:lower:"+routes.get(j).getAsPathSize().getLowerbound()+";upper:"+routes.get(j).getAsPathSize().getUpperbound());
//            System.out.println("dui ying de tuo pu tiao jian:");
//            routes.get(j).getTopologyCondition().printSet();
//          }
//
//          System.out.println("dang tuo po tiao jian wei ru xia qing kuang shi , reng hui dao xiang wai bu lu you:");
//          System.out.println("---------------"+"ke da nei bu lu you:"+firstExternalPos+"---------------");
//          System.out.println("yuan jie dian:"+routes.get(firstExternalPos).getOriginatorIp());
//          System.out.println("as-path:"+routes.get(firstExternalPos).getAsPath().getAsPathString());
//          System.out.println("localpreference:lower:"+routes.get(firstExternalPos).getLocalPreference().getLowerbound()+";upper:"+routes.get(firstExternalPos).getLocalPreference().getUpperbound());
//          System.out.println("as-pathSize:lower:"+routes.get(firstExternalPos).getAsPathSize().getLowerbound()+";upper:"+routes.get(firstExternalPos).getAsPathSize().getUpperbound());
//          System.out.println("dui ying de tuo pu tiao jian:");
//          routes.get(firstExternalPos).getTopologyCondition().printSet();
//        }
//      }

    //you xuan guan xi de yan zheng
//    Answer answerT=new Answer();
//    String srcNode="aggregation-1";
//    String n1="edge-2";
//    String n2="edge-3";
//    Map<SymPrefixList,List<SymBgpRoute>> srcRib=dp.getBgpRib().get(srcNode);
//    for(SymPrefixList prefix:srcRib.keySet())
//    {
//      List<SymBgpRoute> routes=srcRib.get(prefix);
//      BDD tc=factory.one();
//      for(int i=0;i<routes.size();i++)
//      {
//        SymBgpRoute route1=routes.get(i);
//        if(route1.getSrcNode().equals(n2))
//        {
//          for(int j=i+1;j<routes.size();j++)
//          {
//            if(routes.get(j).getSrcNode().equals(n1)&&!tc.and(route1.getTopologyCondition()).and(routes.get(j).getTopologyCondition()).equals(factory.zero()))
//            {
//              answerT.setVerified(false);
//              answerT.addCounter(route1,routes.get(j),tc.and(route1.getTopologyCondition()).and(routes.get(j).getTopologyCondition()));
//            }
//          }
//        }
//        tc=tc.and(route1.getTopologyCondition().not());
//      }
//    }
//    System.out.println("you xuan guan xi yan zheng jie guo wei:"+answerT._verified);
//    System.out.println("fan li wei:");
//    for(int i=0;i<answerT._counter.size();i++)
//    {
//      CounterRoute counterRoute=answerT._counter.get(i);
//      System.out.println("zhen dui qian zhui:");
//      System.out.println("---前缀的信息:---");
//      for(BDD ip:counterRoute._route2.getNetwork()._prefixList.keySet())
//      {
//        ip.printSet();
//        System.out.println("长度:"+counterRoute._route2.getNetwork()._prefixList.get(ip));
//      }
//      System.out.println("dang tuo pu tiao jian wei shi:");
//      counterRoute._topologyCondition.printSet();
//      System.out.println("ying you xuan lu you:"+counterRoute._route2.toString());
//      System.out.println("qi yuan jie dian:"+counterRoute._route2._srcNode);
//      System.out.println("local-preference:lower:"+counterRoute._route2.getLocalPreference().getLowerbound()+"; upper:"+counterRoute._route2.getLocalPreference().getUpperbound());
//      System.out.println("as-path-size:lower:"+counterRoute._route2.getAsPathSize().getLowerbound()+"; upper:"+counterRoute._route2.getAsPathSize().getUpperbound());
//      if(counterRoute._route2.getCommunities()!=null)
//          System.out.println("community:"+counterRoute._route2.getCommunities().toString());
//      System.out.println("as-path:"+counterRoute._route2.getAsPath().toString());
//      System.out.println("dan shi que you xuan le lu you:"+counterRoute._route1.toString());
//      System.out.println("qi yuan jie dian:"+counterRoute._route1._srcNode);
//      System.out.println("local-preference:lower:"+counterRoute._route1.getLocalPreference().getLowerbound()+"; upper:"+counterRoute._route1.getLocalPreference().getUpperbound());
//      System.out.println("as-path-size:lower:"+counterRoute._route1.getAsPathSize().getLowerbound()+"; upper:"+counterRoute._route1.getAsPathSize().getUpperbound());
//      if(counterRoute._route1.getCommunities()!=null)
//        System.out.println("community:"+counterRoute._route1.getCommunities().toString());
//      System.out.println("as-path:"+counterRoute._route1.getAsPath().toString());
//    }


    System.out.println("-----------已经完成符号化执行:-------------");

    // 到这里BGP的控制平面应该就计算完了
//    if (isOscillating) {
//      // If we are oscillating here, network has no stable solution.
//      // 表示没有收敛的状态
//      throw new BdpOscillationException("Network has no stable solution");
//    }
    ae.setVersion(Version.getVersion());
    _bfLogger.printElapsedTime();
    return dp;
  }


  public static int findMax(BDD expression)
  {
    //    List<Object> allSolutions = expression.allsat();
    int maxFalseVariables = Integer.MIN_VALUE;
    for (Object solution : expression.allsat()) {
      byte[] assignment = (byte[]) solution;
      int falseCount = 0;
      for (byte b : assignment) {
        if (b == 0) {
          falseCount++;
        }
      }
      maxFalseVariables = Math.max(maxFalseVariables, falseCount);
    }
    return maxFalseVariables;
  }

  public void initSymBgpInitialRoute(Map<SymPrefixList,Integer> prefixEC,final Network<BgpNeighbor, BgpSession> bgpTopology, final Map<String, Node> nodes, List<String> pruneNode, HashMap<String,List<Integer>> prefixOwnersPrefixEc)
  {
    nodes
        .values()
//        .parallelStream()
        .forEach(
            n->{
              if(!pruneNode.contains(n.getConfiguration().getHostname()))
              {
                for(VirtualRouter vr:n.getVirtualRouters().values())
                {
                  vr.initSymBgpRoute(prefixEC,bgpTopology,nodes, prefixOwnersPrefixEc.get(n.getConfiguration().getHostname()));
                }
              }
            }
        );
  }



  /**
   * Perform one iteration of the "dependent routes" dataplane computation. Dependent routes refers
   * to routes that could change because other routes have changed. For example, this includes:
   *
   * <ul>
   *   <li>static routes with next hop IP
   *   <li>aggregate routes
   *   <li>EGP routes (various protocols)
   * </ul>
   *
   * @param nodes nodes that are participating in the computation
   * @param topology network Topology
   * @param dp data place instance
   * @param iteration iteration number (for stats tracking)
   * @param allNodes all nodes in the network (for correct neighbor referencing)
   * @param bgpTopology the bgp peering relationships
   */


  private void computeDependentRoutesIteration(
      Map<String, Node> nodes,
      Topology topology,
      IncrementalDataPlane dp,
      int iteration,
      Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology) {

    // (Re)initialization of dependent route calculation
    // 每次迭代之前初始化各个表,包括转发表和阶段性的路由表,后续这一部分都是不需要的
    AtomicInteger reinitializeDependentCompleted =
        _newBatch.apply("Iteration " + iteration + ": Reinitialize dependent routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {

                /*
                 * For RIBs that do not require comparison to previous version, just re-init
                 */
                // 这里初始化的其实是每次迭代产生的临时的转发表,因为每一次迭代的转发表都和之前不相关,所以直接重新初始化就可以了,因为我们的目标是不计算Fib所以这一部分应该不需要
                vr.reinitForNewIteration(allNodes);
              }
              //reinitializeDependentCompleted.incrementAndGet();
            });

    // Static nextHopIp routes
    // 处理静态路由
//    AtomicInteger recomputeStaticCompleted =
//        _newBatch.apply(
//            "Iteration " + iteration + ": Recompute static routes with next-hop IP", nodes.size());
//    nodes
//        .values()
//        .parallelStream()
//        .forEach(
//            n -> {
//              for (VirtualRouter vr : n.getVirtualRouters().values()) {
//                //设置静态路由
//                //每次都计算传来的静态路由
//                vr.activateStaticRoutes();
//              }
//              recomputeStaticCompleted.incrementAndGet();
//            });

    // Generated/aggregate routes
    // 处理聚合路由,这一部分也是后续需要考虑的
//    AtomicInteger recomputeAggregateCompleted =
//        _newBatch.apply(
//            "Iteration " + iteration + ": Recompute aggregate/generated routes", nodes.size());
//    nodes
//        .values()
//        .parallelStream()
//        .forEach(
//            n -> {
//              for (VirtualRouter vr : n.getVirtualRouters().values()) {
//                //设置聚合路由
//                vr.recomputeGeneratedRoutes();
//              }
//              recomputeAggregateCompleted.incrementAndGet();
//            });//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    // OSPF external routes
    // recompute exports
    // 导出OSPF路由,即传播的其实是OSPF路由提供的-下面是有关OSPF的部分,因为只考虑BGP路由所以这一部分后续可能并不需要
//    nodes
//        .values()
//        .parallelStream()
//        .forEach(
//            n -> {
//              for (VirtualRouter vr : n.getVirtualRouters().values()) {
//                // 设置OSPF的导出
//                vr.initOspfExports();
//              }
//            });//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11

    // Re-propagate OSPF exports
    // 计算OSPF路由
    // 也就是说在计算BGP路由之前,需要重新计算IGP产生的路由
//    AtomicBoolean ospfExternalChanged = new AtomicBoolean(true);
//    int ospfExternalSubIterations = 0;
//    while (ospfExternalChanged.get()) {
//      ospfExternalSubIterations++;
//      AtomicInteger propagateOspfExternalCompleted =
//          _newBatch.apply(
//              "Iteration "
//                  + iteration
//                  + ": Propagate OSPF external routes: subIteration: "
//                  + ospfExternalSubIterations,
//              nodes.size());
//      ospfExternalChanged.set(false);
//      node
//          .values()
//          .parallelStream()
//          .forEach(
//              n -> {
//                for (VirtualRouter vr : n.getVirtualRouters().values()) {
//                  Entry<RibDelta<OspfExternalType1Route>, RibDelta<OspfExternalType2Route>> p =
//                      vr.propagateOspfExternalRoutes(allNodes, topology);
//                  if (p != null && vr.unstageOspfExternalRoutes(p.getKey(), p.getValue())) {
//                    ospfExternalChanged.set(true);
//                  }
//                }
//                propagateOspfExternalCompleted.incrementAndGet();
//              });
//    }//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11

    //最后再计算BGP路由,计算BGP路由消息迭代计算
    computeIterationOfBgpRoutes(nodes, dp, iteration, allNodes, bgpTopology);
  }

  private void computeIterationOfBgpRoutes(
      Map<String, Node> nodes,
      IncrementalDataPlane dp,
      int iteration,
      Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology) {
    // BGP routes
    // first let's initialize nodes-level generated/aggregate routes
    // 首先初始化聚合路由,只考虑BGP这一部分也是不需要的
//    nodes
//        .values()
//        .parallelStream()
//        .forEach(
//            n -> {
//              for (VirtualRouter vr : n.getVirtualRouters().values()) {
//                if (vr._vrf.getBgpProcess() != null) {
//                  vr.initBgpAggregateRoutes();
//                }
//              }
//            });
    AtomicInteger propagateBgpCompleted =
        _newBatch.apply("Iteration " + iteration + ": Propagate BGP routes", nodes.size());
    System.out.println(nodes.toString());
    //.parallelStream()不使用并行计算了
    nodes//注意这里要用单步运行
        .values()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {//可能的出错的地方
                BgpProcess proc = vr._vrf.getBgpProcess();
                if (proc == null) {
                  continue;
                }
                //里面会更新阶段性路由,处理接受到的路由消息
                Map<BgpMultipathRib, RibDelta<BgpRoute>> deltas  =
                    vr.processBgpMessages(dp.getIpOwners(), bgpTopology);
                //更新自己的路由表,并向外传递路由
                //这是更新总的路由,阶段性路由表
                System.out.println("更新的消息:");
                for(BgpMultipathRib temp : deltas.keySet())
                {
                  RibDelta<BgpRoute> pr = deltas.get(temp);
                  System.out.println(pr.getRoutes().toString());
                }
                System.out.println("Final_route:");
                //将阶段性路由合并到总的路由表当中去
                vr.finalizeBgpRoutesAndQueueOutgoingMessages(
                    proc.getMultipathEbgp(),
                    proc.getMultipathIbgp(),
                    deltas,
                    allNodes,
                    bgpTopology);
              }
              propagateBgpCompleted.incrementAndGet();
            });
  }

  /**
   * Run {@link VirtualRouter#computeFib} on all of the given nodes (and their virtual routers)
   *
   * @param nodes mapping of node names to node instances
   */
  private void computeFibs(Map<String, Node> nodes) {
    AtomicInteger completed = _newBatch.apply("Computing FIBs", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                vr.computeFib();//计算内部路由形成的转发表
              }
              completed.incrementAndGet();
            });
  }

  /**
   * Compute the IGP portion of the dataplane.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param topology The topology representing Layer 3 adjacencies between interface of the nodes
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   */
  private void computeIgpDataPlane(
      SortedMap<String, Node> nodes, Topology topology, BdpAnswerElement ae) {
    //node相当于又创建了一个节点变量,里边保存着这个节点的配置文件,以及相应的路由表,里面的每一个路由进程都被保存成了一个路由实例?
    int numOspfInternalIterations;

    /*
     * For each virtual router, setup the initial easy-to-do routes, init protocol-based RIBs,
     * queue outgoing messages to neighbors
     */
    System.out.println("------------------------最终的路由信息11:-------------------");//对应拓扑结构

    for(String node_name: nodes.keySet())
    {
      SortedMap<String, VirtualRouter> vrf_print = nodes.get(node_name).getVirtualRouters();
      System.out.println(node_name+":");
      for(String vrf_name : vrf_print.keySet())
      {
        Rib Rib_print = vrf_print.get(vrf_name)._mainRib;
        Set<AbstractRoute> AsPath_print = Rib_print.getRoutes();
        System.out.println(AsPath_print.toString());
        //        Map<Prefix, BDD> Topology_print = Rib_print.getTopologyCondition();
        //        for(Prefix prefix_print : AsPath_print.keySet())
        //        {
        //          AsPath aspath_temp = AsPath_print.get(prefix_print);
        //          BDD Topology_temp = Topology_print.get(prefix_print);
        //          System.out.println("Prefix:"+prefix_print.toString());
        //          System.out.println("Aspath:"+ aspath_temp.getAsPathString());
        //          System.out.println("Topology:"+Topology_temp.toString());
        //        }
      }
    }
    System.out.println("----------------------------End-------------------------");
    AtomicInteger initialCompleted =
        _newBatch.apply(
            "Compute initial connected and static routes, ospf setup, bgp setup", nodes.size());
    //这一部分主要是初始化路由配置?
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                vr.initForIgpComputation();
              }
              initialCompleted.incrementAndGet();
            });

    // OSPF internal routes
    // 计算OSPF的路由
    numOspfInternalIterations = initOspfInternalRoutes(nodes, topology);

    // RIP internal routes
    // 计算RIP的路由
    initRipInternalRoutes(nodes, topology);


    // Activate static routes
    // 计算静态路由
    AtomicInteger staticRoutesAfterIgp =
        _newBatch.apply("Compute static routes after IGP protocol convergence", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                importRib(vr._mainRib, vr._independentRib);
                vr.activateStaticRoutes();
              }
              staticRoutesAfterIgp.incrementAndGet();
            });
    // Set iteration stats in the answer
    ae.setOspfInternalIterations(numOspfInternalIterations);
  }

  private void symComputeDependentRoutesIteration(
      Map<String, Node> nodes,
      Topology topology,
      IncrementalDataPlane dp,
      int iteration,
      Map<String, Node> allNodes,
      Network<BgpNeighbor, BgpSession> bgpTopology,
      HashMap<String,BgpSession> nodeNamedBgpTopology,
      Map<String, Configuration> configurations
      ) {

    // (Re)initialization of dependent route calculation
    // 每次迭代之前初始化各个表,包括转发表和阶段性的路由表,后续这一部分都是不需要的
    AtomicInteger reinitializeDependentCompleted =
        _newBatch.apply("Iteration " + iteration + ": Reinitialize dependent routes", nodes.size());
    final Network<BgpNeighbor, BgpSession> finalBgpTopology=bgpTopology;
    final Map<Ip,Set<String>> ipOwners=dp.getIpOwners();

    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {

                /*
                 * For RIBs that do not require comparison to previous version, just re-init
                 */
                vr._symBgpRib.reinitRib();
              }
            });

    try {
      nodes.values()
          .parallelStream()
          .forEach(n -> {
            for (VirtualRouter vr : n.getVirtualRouters().values()) {
              try {
                BgpProcess proc = vr._vrf.getBgpProcess();
                if (proc == null) {
                  continue;
                }
                vr.processSymBgpMessage(ipOwners, finalBgpTopology);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          });
    }catch (Exception e)
    {
      System.out.println("process-bgp-error");
      e.printStackTrace();
    }
    nodes
        .values()
//        .parallelStream()
        .forEach(
            n->{
              for (VirtualRouter vr:n.getVirtualRouters().values())
              {
                try{
                  BgpProcess proc = vr._vrf.getBgpProcess();
                  if (proc == null) {
                    continue;
                  }
                  vr.symQueueStagingBgpMessages(allNodes, bgpTopology);
                }catch (Exception e)
                {
                  System.out.println("queueerror");
                  e.printStackTrace();
                }
              }
            }
        );
  }

  public boolean symComputeExternalMessageDataPlane(
      SortedMap<String, Node> nodes,
      Topology topology,
      IncrementalDataPlane dp,
      Network<BgpNeighbor, BgpSession> bgpTopology,
      HashMap<String,BgpSession> nodeNamedBgpTopology,
      Map<String,Configuration> configurations,
      Map<Ip, Set<String>> ipOwners,
      HashMap<ExternalNeighbor, List<SymBgpRoute.Builder>> externalRouteAdvert,
      BdpAnswerElement ae
  )
  {
    HashMap<Integer,Integer> routeIndexToPrefixEC=new HashMap<>();
    long timeComputeBegin = System.currentTimeMillis();
    nodes
        .values()
//        .parallelStream()
        .forEach( n -> {
          for(VirtualRouter vr:n.getVirtualRouters().values())
          {

            if(vr._vrf.getBgpProcess()==null || !vr._vrf.getName().equals("default"))
            {
              continue;
            }
            for(BgpNeighbor neighbor:vr._vrf.getBgpProcess().getNeighbors().values())
            {
              if(!bgpTopology.nodes().contains(neighbor)||bgpTopology.adjacentNodes(neighbor).size()==0)
              {

                ExternalNeighbor externalNeighbor = new ExternalNeighbor(neighbor.getPrefix(), neighbor.getRemoteAs());
                if (!externalRouteAdvert.containsKey(externalNeighbor))
                {
                  continue;
                }

                for (SymBgpRoute.Builder extRouteBuilder : externalRouteAdvert.get(externalNeighbor))
                {
                  SymBgpRoute extRoute = extRouteBuilder.build();
                  Queue<SymBgpRoute> processInputRoute=new LinkedList<>();
                  processInputRoute.add(extRoute);
                  while(!processInputRoute.isEmpty())
                  {
                    SymBgpRoute inputRoute=processInputRoute.poll();
                    SymBgpRoute.Builder transformedIncomingRouteBuilder=new SymBgpRoute.Builder(inputRoute);
                    String importPolicyName = neighbor.getImportPolicy();

                    if (inputRoute.getAsPath().contains(neighbor.getLocalAs())
                        && !neighbor.getAllowLocalAsIn()) {
                      // skip routes containing peer's AS unless
                      // disable-peer-as-check (getAllowRemoteAsOut) is set
                      continue;
                    }
                    SymPolicyAnswer acceptIncoming=new SymPolicyAnswer();
                    if (importPolicyName != null) {
                      RoutingPolicy importPolicy = vr.getConfiguration().getRoutingPolicies().get(importPolicyName);
                      if (importPolicy != null) {
                        acceptIncoming =
                            importPolicy.symProcess(
                                inputRoute,
                                transformedIncomingRouteBuilder,
                                neighbor.getPrefix().getStartIp(),
                                neighbor.getPrefix(),
                                vr._vrf.getName(),
                                Direction.IN);
                      }
                      if(acceptIncoming._divideRotue!=null)
                      {
                        processInputRoute.add(acceptIncoming._divideRotue);
                      }
                    }
                    if (acceptIncoming._accept)
                    {
                      SymBgpRoute addRoute=transformedIncomingRouteBuilder.build();
                      vr._symBgpRib.addRoute(addRoute);
                    }
                  }
                }
              }
            }
//            vr.symQueueStagingBgpMessages(nodes, bgpTopology);
          }
        });
    long timeComputeEnd = System.currentTimeMillis();
    System.out.println("Time compute : " + (timeComputeEnd - timeComputeBegin));
    // 注入外部路由宣告
    for(Node n:nodes.values())
    {
      for(VirtualRouter vr:n.getVirtualRouters().values())
      {

        if(vr._vrf.getBgpProcess()==null || !vr._vrf.getName().equals("default"))
        {
          continue;
        }
//        for(BgpNeighbor neighbor:vr._vrf.getBgpProcess().getNeighbors().values())
//        {
//          if(!bgpTopology.nodes().contains(neighbor)||bgpTopology.adjacentNodes(neighbor).size()==0)
//          {
//
//            ExternalNeighbor externalNeighbor = new ExternalNeighbor(neighbor.getPrefix(), neighbor.getRemoteAs());
//            if (!externalRouteAdvert.containsKey(externalNeighbor))
//            {
//              continue;
//            }
//
//            for (SymBgpRoute.Builder extRouteBuilder : externalRouteAdvert.get(externalNeighbor))
//            {
//              SymBgpRoute extRoute = extRouteBuilder.build();
//              Queue<SymBgpRoute> processInputRoute=new LinkedList<>();
//              processInputRoute.add(extRoute);
//              while(!processInputRoute.isEmpty())
//              {
//                SymBgpRoute inputRoute=processInputRoute.poll();
//                SymBgpRoute.Builder transformedIncomingRouteBuilder=new SymBgpRoute.Builder(inputRoute);
//                String importPolicyName = neighbor.getImportPolicy();
//
//                if (inputRoute.getAsPath().contains(neighbor.getLocalAs())
//                    && !neighbor.getAllowLocalAsIn()) {
//                  // skip routes containing peer's AS unless
//                  // disable-peer-as-check (getAllowRemoteAsOut) is set
//                  continue;
//                }
//                SymPolicyAnswer acceptIncoming=new SymPolicyAnswer();
//                if (importPolicyName != null) {
//                  RoutingPolicy importPolicy = vr.getConfiguration().getRoutingPolicies().get(importPolicyName);
//                  if (importPolicy != null) {
//                    acceptIncoming =
//                        importPolicy.symProcess(
//                            inputRoute,
//                            transformedIncomingRouteBuilder,
//                            neighbor.getPrefix().getStartIp(),
//                            neighbor.getPrefix(),
//                            vr._vrf.getName(),
//                            Direction.IN);
//                  }
//                  if(acceptIncoming._divideRotue!=null)
//                  {
//                    processInputRoute.add(acceptIncoming._divideRotue);
//                  }
//                }
//                if (acceptIncoming._accept)
//                {
//                  SymBgpRoute addRoute=transformedIncomingRouteBuilder.build();
//                  vr._symBgpRib.addRoute(addRoute);
//                }
//              }
//            }
//          }
//        }
        long timeComputeEnd1 = System.currentTimeMillis();
        vr.symQueueStagingBgpMessages(nodes, bgpTopology);
        long timeQueueEnd = System.currentTimeMillis();
        System.out.println("Time queue : " + (timeQueueEnd - timeComputeEnd1));
      }
    }

//    nodes
//        .values()
//        .forEach(
//            n-> {
//              for(VirtualRouter vr:n.getVirtualRouters().values())
//              {
//                if(vr._vrf.getBgpProcess()==null)
//                {
//                  continue;
//                }
//                vr._symBgpRib.clearDeleteTreeNodes();
//                vr._symBgpRib.clearTreeNodes();
//                vr._symBgpRib.computeChangedRoute();
//              }
//            }
//        );

//    nodes
//        .values()
//        .forEach(
//            n->{
//              for(VirtualRouter vr:n.getVirtualRouters().values())
//              {
//                if(vr._vrf.getBgpProcess()==null)
//                {
//                  continue;
//                }
//                vr.symQueueStagingBgpMessages(nodes, bgpTopology,routeIndexForest);
//              }
//            }
//        );

    Map<Integer, SortedSet<Integer>> iterationsByHashCode = new HashMap<>();
    Map<Integer, SortedSet<Integer>> iterationsByMessageQueueHashCode = new HashMap<>();

    AtomicBoolean dependentRoutesChanged = new AtomicBoolean(false);

    // Go into iteration mode, until the routes converge (or oscillation is detected)
    // 然后就是迭代进行路由消息计算传播直到最后的收敛
    do {
      _numIterations++;
      AtomicBoolean currentChangedMonitor;
      currentChangedMonitor = dependentRoutesChanged;
      currentChangedMonitor.set(false);

      System.out.println("compute-external-begin");

      // Compute node schedule
      // 计算能够并行计算的所有节点,这一部分可能不需要更改
      IbdpSchedule schedule = IbdpSchedule.getSchedule(_settings, nodes, bgpTopology);

      // compute dependent routes for each allowable set of nodes until we cover all nodes
      // 迭代路由消息的传递计算
      while (schedule.hasNext()) {
        Map<String, Node> iterationNodes = schedule.next();
        // 真正的计算路由
        // 针对每个节点,计算收到的路由更新自己的路由表,然后把路由消息送到邻居节点的消息队列当中
        symComputeDependentRoutesIteration(//注意这里需要使用单步
            iterationNodes, topology, dp, _numIterations, nodes, bgpTopology,nodeNamedBgpTopology,configurations);
      }

      //这一步主要是本次迭代消息的记录
      /*
       * Perform various bookkeeping at the end of the iteration:
       * - Collect sizes of certain RIBs this iteration
       * - Compute iteration hashcode
       * - Check for oscillations
       */
      computeIterationStatistics(nodes, ae, _numIterations);
//      if(_numIterations>3)
//      {
//        nodes
//            .values()
//            .forEach(
//                n->{
//                  for(VirtualRouter vr:n.getVirtualRouters().values())
//                  {
//                    for(List<SymBgpRoute> routeList:vr._symBgpRib.getRib().get(0))
//                    {
//                      vr._symBgpRib._topologyBDD.printSet(routeList.get(0)._outBGPTopologyCondition);
//                      System.out.println();
//                      System.out.println();
//                      System.out.println();
//                    }
//                  }
//                }
//            );
//      }

      // Check to see if hash has changed
      AtomicInteger checkFixedPointCompleted =
          _newBatch.apply(
              "Iteration " + _numIterations + ": Check if fixed-point reached", nodes.size());

      // This hashcode uniquely identifies the iteration (i.e., network state)
      // 这一部分主要是通过哈希函数判断到底能不能收敛,这一部分需要更改
      int iterationHashCode = computeIterationHashCode(nodes);
      SortedSet<Integer> iterationsWithThisHashCode =
          iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());
      int iterationMessageHashCode = computeIterationMessgaeQueueHashCode(nodes);
      SortedSet<Integer> iterationsWithMessageQueueHashCode =
          iterationsByMessageQueueHashCode.computeIfAbsent(iterationMessageHashCode, h -> new TreeSet<>());

      if (iterationsWithMessageQueueHashCode.isEmpty()) {
        iterationsWithMessageQueueHashCode.add(_numIterations);
      } else {
        // If oscillation detected, switch to a more restrictive schedule
        System.out.println("pre-oscillation");
        if (_settings.getScheduleName() != Schedule.NODE_SERIALIZED) {
          _bfLogger.debugf(
              "Switching to a more restrictive schedule %s, iteration %d\n",
              Schedule.NODE_SERIALIZED, _numIterations);
          _settings.setScheduleName(Schedule.NODE_SERIALIZED);
        } else {
          return true; // Found an oscillation
        }
      }
      if(_numIterations>20)
      {
        _settings.setScheduleName(Schedule.NODE_SERIALIZED);
      }
      if(_numIterations>50)
      {
        return true;
      }
      System.out.println("computeExternal-End");

      symCompareToPreviousIteration(nodes, dependentRoutesChanged, checkFixedPointCompleted);
    } while (!symAreQueuesEmpty(nodes, bgpTopology) || dependentRoutesChanged.get());

    return false;
  }

  //符号化计算路由传播的迭代过程
  public boolean symComputeNonMonotonicPortionOfDataPlane(
      SortedMap<String, Node> nodes,
      Topology topology,
      IncrementalDataPlane dp,
      Set<BgpAdvertisement> externalAdverts,
      BdpAnswerElement ae,
      boolean firstPass,
      Network<BgpNeighbor, BgpSession> bgpTopology,
      List<SymBgpRoute.Builder> externalRouteAdvert,
      IsisTopology isisTopology,
      HashMap<String,BgpSession> nodeNamedBgpTopology,
      Map<String,Configuration> configurations,
      Map<Ip, Set<String>> ipOwners,
      SortedMap<String, Map<Integer,List<SymBgpRoute>>> denyList,
      HashMap<Integer, RouteForest> routeIndexForest,
      boolean multiPath
  )
  {
    Map<Integer, SortedSet<Integer>> iterationsByHashCode = new HashMap<>();

    AtomicBoolean dependentRoutesChanged = new AtomicBoolean(false);

    // Go into iteration mode, until the routes converge (or oscillation is detected)
    // 然后就是迭代进行路由消息计算传播直到最后的收敛
    do {
      _numIterations++;
      AtomicBoolean currentChangedMonitor;
      currentChangedMonitor = dependentRoutesChanged;
      currentChangedMonitor.set(false);

      // Compute node schedule
      // 计算能够并行计算的所有节点,这一部分可能不需要更改
      IbdpSchedule schedule = IbdpSchedule.getSchedule(_settings, nodes, bgpTopology);

      // compute dependent routes for each allowable set of nodes until we cover all nodes
      // 迭代路由消息的传递计算
      while (schedule.hasNext()) {
        Map<String, Node> iterationNodes = schedule.next();
        // 真正的计算路由
        // 针对每个节点,计算收到的路由更新自己的路由表,然后把路由消息送到邻居节点的消息队列当中
        symComputeDependentRoutesIteration(//注意这里需要使用单步
            iterationNodes, topology, dp, _numIterations, nodes, bgpTopology,nodeNamedBgpTopology,configurations);
      }

      //这一步主要是本次迭代消息的记录
      /*
       * Perform various bookkeeping at the end of the iteration:
       * - Collect sizes of certain RIBs this iteration
       * - Compute iteration hashcode
       * - Check for oscillations
       */
      System.out.println("step:computeIterationStatistics");
      computeIterationStatistics(nodes, ae, _numIterations);

      // Check to see if hash has changed
      AtomicInteger checkFixedPointCompleted =
          _newBatch.apply(
              "Iteration " + _numIterations + ": Check if fixed-point reached", nodes.size());

      // This hashcode uniquely identifies the iteration (i.e., network state)
      // 这一部分主要是通过哈希函数判断到底能不能收敛,这一部分需要更改
      System.out.println("step:compute Hash");
      int iterationHashCode = computeIterationHashCode(nodes);
      SortedSet<Integer> iterationsWithThisHashCode =
          iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());

      if (iterationsWithThisHashCode.isEmpty()) {
        System.out.println("step:addHashCode");
        iterationsWithThisHashCode.add(_numIterations);
      } else {
        System.out.println("pre-oscillation");
        // If oscillation detected, switch to a more restrictive schedule
        if (_settings.getScheduleName() != Schedule.NODE_SERIALIZED) {
          _bfLogger.debugf(
              "Switching to a more restrictive schedule %s, iteration %d\n",
              Schedule.NODE_SERIALIZED, _numIterations);
          _settings.setScheduleName(Schedule.NODE_COLORED);
        } else {
          return true; // Found an oscillation
        }
      }

      System.out.println("step:symCompareToPreviousIteration");
      symCompareToPreviousIteration(nodes, dependentRoutesChanged, checkFixedPointCompleted);
      System.out.println("step:symCompareToPreviousIteration-End");
    } while (!symAreQueuesEmpty(nodes, bgpTopology) || dependentRoutesChanged.get());


    dp.setBgpTopology(bgpTopology);
    ae.setDependentRoutesIterations(_numIterations);

    return false;
  }

  private void symCompareToPreviousIteration(
      Map<String, Node> nodes,
      AtomicBoolean dependentRoutesChanged,
      AtomicInteger checkFixedPointCompleted) {
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                if (!vr._symBgpRib.getChanRoutes().isEmpty()) {
                  dependentRoutesChanged.set(true);
                }
              }
              checkFixedPointCompleted.incrementAndGet();
            });
  }


  private boolean symAreQueuesEmpty(
      Map<String, Node> nodes, Network<BgpNeighbor, BgpSession> bgpTopology) {
    AtomicBoolean areEmpty = new AtomicBoolean(true);
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                if (!vr.symHasProcessedAllMessages(bgpTopology)) {
                  areEmpty.set(false);
                }
              }
            });

    return areEmpty.get();
  }


  /**
   * Compute the EGP portion of the dataplane. Must be called after IGP has converged.
   *
   * @param nodes A dictionary of configuration-wrapping Bdp nodes keyed by name
   * @param topology The topology representing Layer 3 adjacencies between interface of the nodes
   * @param dp The output data plane
   * @param externalAdverts the set of external BGP advertisements
   * @param ae The output answer element in which to store a report of the computation. Also
   *     contains the current recovery iteration.
   * @return true iff the computation is oscillating
   */
  private boolean computeNonMonotonicPortionOfDataPlane(
      SortedMap<String, Node> nodes,
      Topology topology,
      IncrementalDataPlane dp,
      Set<BgpAdvertisement> externalAdverts,
      BdpAnswerElement ae,
      boolean firstPass,
      Network<BgpNeighbor, BgpSession> bgpTopology) {

    /*
     * Initialize all routers and their message queues (can be done as parallel as possible)
     */
    //主要是进行一些初始化的工作,为每个节点和其邻居之间创建消息队列
    if (firstPass) {
      AtomicInteger setupCompleted =
          _newBatch.apply("Initialize virtual routers for iBDP-external", nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n.getVirtualRouters().values()) {
                  //为每个节点EGP路由的计算初始化,主要是初始化路由消息的传递队列,后边计算每个节点的路由器的时候从自己的消息队列里面取就可了
                  vr.initForEgpComputation(externalAdverts, nodes, topology, bgpTopology);
                  vr.symInitBgpQueuesAndDeltaBuilders(nodes,topology,bgpTopology);
                }
                setupCompleted.incrementAndGet();
              });

//      System.out.println("消息队列内容:");
//      nodes
//          .values()
//          .forEach(
//              n -> {
//                for (VirtualRouter vr : n.getVirtualRouters().values()) {
//                  //为每个节点EGP路由的计算初始化,主要是初始化路由消息的传递队列,后边计算每个节点的路由器的时候从自己的消息队列里面取就可了
//                  System.out.println("name:"+n.getName());
//                  for(UndirectedBgpSession name:vr._bgpIncomingRoutes.keySet())
//                  {
//                    System.out.println(name.getFirst()+";"+name.getSecond());
//                    System.out.println(vr._bgpIncomingRoutes.get(name).toString());
//                  }
//                }
//                System.out.println("1111111111111111");
//              });
//      System.out.println("----------------------------------------");
      // Queue initial outgoing messages
      // 初始化外部发送进来的路由宣告消息,后续将这一部分放到最后进行/或者也是一开始进行,计算量可能少一点
      AtomicInteger queueInitial = _newBatch.apply("Queue initial bgp messages", nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n.getVirtualRouters().values()) {
                  // 初始化基本的BGP路由表,主要是考虑外部的路由宣告
                  // 初始化路由表,主要是考虑外部路由宣告的情况
                  //vr.initBaseBgpRibs(externalAdverts, dp.getIpOwners(), nodes, bgpTopology);!!!!!!!!!!!!!!!!!!!!!!!!!!!
                  // 这里将路由宣告送到消息队列当中
                  vr.queueInitialBgpMessages(bgpTopology, nodes);
                  vr.symQueueInitialBgpMessages(bgpTopology,nodes);
                  System.out.println("-----消息队列当中的内容:-----");
                  for(UndirectedBgpSession session:vr._bgpIncomingRoutes.keySet())
                  {
                    Queue<RouteAdvertisement<AbstractRoute>> q=vr._bgpIncomingRoutes.get(session);
                    while(!q.isEmpty())
                    {
                      RouteAdvertisement<AbstractRoute> printQ=q.poll();
                      System.out.println(printQ.getRoute().fullString());
                    }
                    System.out.println("-----End-----");
                  }
                  System.out.println("-----符号消息队列当中的内容:-----");
                  for(UndirectedBgpSession session:vr._symBgpIncomingRoutes.keySet())
                  {
                    Queue<SymRouteAdvertisement<SymBgpRoute>> q=vr._symBgpIncomingRoutes.get(session);
                    while(!q.isEmpty())
                    {
                      SymRouteAdvertisement<SymBgpRoute> printQ=q.poll();
                      System.out.println(printQ.getRoute().getPrefixEcSet());
                    }
                    System.out.println("------End------");
                  }
                }
                queueInitial.incrementAndGet();
              });
    }

//    System.out.println("------------------------最终的路由信息6:-------------------");//对应拓扑结构
//
//    for(String node_name: nodes.keySet())
//    {
//      SortedMap<String, VirtualRouter> vrf_print = nodes.get(node_name).getVirtualRouters();
//      System.out.println(node_name+":");
//      for(String vrf_name : vrf_print.keySet())
//      {
//        Rib Rib_print = vrf_print.get(vrf_name)._mainRib;
//        Set<AbstractRoute> AsPath_print = Rib_print.getRoutes();
//        System.out.println(AsPath_print.toString());
//        //        Map<Prefix, BDD> Topology_print = Rib_print.getTopologyCondition();
//        //        for(Prefix prefix_print : AsPath_print.keySet())
//        //        {
//        //          AsPath aspath_temp = AsPath_print.get(prefix_print);
//        //          BDD Topology_temp = Topology_print.get(prefix_print);
//        //          System.out.println("Prefix:"+prefix_print.toString());
//        //          System.out.println("Aspath:"+ aspath_temp.getAsPathString());
//        //          System.out.println("Topology:"+Topology_temp.toString());
//        //        }
//      }
//    }
//    System.out.println("----------------------------End-------------------------");
    /*
     * Setup maps to track iterations. We need this for oscillation detection.
     * Specifically, if we detect that an iteration hashcode (a hash of all the nodes' RIBs)
     * has been previously encountered, we switch our schedule to a more restrictive one.
     * 这部分就是不断迭代进行路由表的计算
     */


    Map<Integer, SortedSet<Integer>> iterationsByHashCode = new HashMap<>();

    AtomicBoolean dependentRoutesChanged = new AtomicBoolean(false);



    // Go into iteration mode, until the routes converge (or oscillation is detected)
    // 然后就是迭代进行路由消息计算传播直到最后的收敛
    do {
      _numIterations++;
      AtomicBoolean currentChangedMonitor;
      currentChangedMonitor = dependentRoutesChanged;
      currentChangedMonitor.set(false);

      // Compute node schedule
      // 计算能够并行计算的所有节点,这一部分可能不需要更改
      IbdpSchedule schedule = IbdpSchedule.getSchedule(_settings, nodes, bgpTopology);

      // compute dependent routes for each allowable set of nodes until we cover all nodes
      // 迭代路由消息的传递计算
      while (schedule.hasNext()) {
        Map<String, Node> iterationNodes = schedule.next();
        System.out.println("---------------iterationNodes:"+iterationNodes.toString());
        // 真正的计算路由
        // 针对每个节点,计算收到的路由更新自己的路由表,然后把路由消息送到邻居节点的消息队列当中
        computeDependentRoutesIteration(//注意这里需要使用单步
            iterationNodes, topology, dp, _numIterations, nodes, bgpTopology);
      }

      //这一步主要是本次迭代消息的记录
      /*
       * Perform various bookkeeping at the end of the iteration:
       * - Collect sizes of certain RIBs this iteration
       * - Compute iteration hashcode
       * - Check for oscillations
       */
      computeIterationStatistics(nodes, ae, _numIterations);

      // Check to see if hash has changed
      AtomicInteger checkFixedPointCompleted =
          _newBatch.apply(
              "Iteration " + _numIterations + ": Check if fixed-point reached", nodes.size());

      // This hashcode uniquely identifies the iteration (i.e., network state)
      // 这一部分主要是通过哈希函数判断到底能不能收敛
      int iterationHashCode = computeIterationHashCode(nodes);
      SortedSet<Integer> iterationsWithThisHashCode =
          iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());

      if (iterationsWithThisHashCode.isEmpty()) {
        iterationsWithThisHashCode.add(_numIterations);
      } else {
        // If oscillation detected, switch to a more restrictive schedule
        if (_settings.getScheduleName() != Schedule.NODE_SERIALIZED) {
          _bfLogger.debugf(
              "Switching to a more restrictive schedule %s, iteration %d\n",
              Schedule.NODE_SERIALIZED, _numIterations);
          _settings.setScheduleName(Schedule.NODE_SERIALIZED);
        } else {
          return true; // Found an oscillation
        }
      }

      compareToPreviousIteration(nodes, dependentRoutesChanged, checkFixedPointCompleted);
    } while (!areQueuesEmpty(nodes, bgpTopology) || dependentRoutesChanged.get());

    // After convergence, compute BGP advertisements sent to the outside of the network
    // 这一部分主要是计算向外传播的路由
    AtomicInteger computeBgpAdvertisementsToOutsideCompleted =
        _newBatch.apply("Compute BGP advertisements sent to outside", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                vr.computeBgpAdvertisementsToOutside(dp.getIpOwners());
              }
              computeBgpAdvertisementsToOutsideCompleted.incrementAndGet();
            });
    // 设置BGP拓扑
    dp.setBgpTopology(bgpTopology);
    ae.setDependentRoutesIterations(_numIterations);
    return false; // No oscillations
  }

  private void compareToPreviousIteration(
      Map<String, Node> nodes,
      AtomicBoolean dependentRoutesChanged,
      AtomicInteger checkFixedPointCompleted) {
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                if (vr.hasOutstandingRoutes()) {
                  dependentRoutesChanged.set(true);
                }
              }
              checkFixedPointCompleted.incrementAndGet();
            });
  }

  /**
   * Check that the routers have processed all messages, queues are empty and there is nothing else
   * to do (i.e., we've converged to a stable network solution)
   *
   * @param nodes nodes to check
   * @param bgpTopology the bgp peering relationships
   * @return true iff all queues are empty
   */
  private boolean areQueuesEmpty(
      Map<String, Node> nodes, Network<BgpNeighbor, BgpSession> bgpTopology) {
    AtomicInteger computeQueuesAreEmpty =
        _newBatch.apply("Check for convergence (are queues empty?)", nodes.size());
    AtomicBoolean areEmpty = new AtomicBoolean(true);
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                if (!vr.hasProcessedAllMessages(bgpTopology)) {
                  areEmpty.set(false);
                }
              }
              computeQueuesAreEmpty.incrementAndGet();
            });

    return areEmpty.get();
  }

  /**
   * Compute the hashcode that uniquely identifies the state of hte network at a given iteration
   *
   * @param nodes map of nodes
   * @return integer hashcode
   */
  private static int computeIterationHashCode(Map<String, Node> nodes) {
    return nodes
        .values()
        .parallelStream()
        .flatMap(node -> node.getVirtualRouters().values().stream())
        .mapToInt(VirtualRouter::computeIterationHashCode)
        .sum();
  }

  private static int computeIterationMessgaeQueueHashCode(Map<String, Node> nodes) {
    return nodes
        .values()
        .parallelStream()
        .flatMap(node -> node.getVirtualRouters().values().stream())
        .mapToInt(VirtualRouter::computeIterationMessageQueueHashCode)
        .sum();
  }

  private void computeIterationStatistics(
      Map<String, Node> nodes, BdpAnswerElement ae, int dependentRoutesIterations) {
    int numBgpBestPathRibRoutes =
        nodes
            .values()
            .stream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .mapToInt(vr -> vr.getBgpBestPathRib().getRoutes().size())
            .sum();
    ae.getBgpBestPathRibRoutesByIteration().put(dependentRoutesIterations, numBgpBestPathRibRoutes);
    int numIsisRibRoutes =
        nodes
            .values()
            .stream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .mapToInt(vr -> vr.getIsisRib().getRoutes().size())
            .sum();
    ae.getBgpBestPathRibRoutesByIteration().put(dependentRoutesIterations, numIsisRibRoutes);
    int numBgpMultipathRibRoutes =
        nodes
            .values()
            .stream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .mapToInt(vr -> vr._bgpMultipathRib.getRoutes().size())
            .sum();
    ae.getBgpMultipathRibRoutesByIteration()
        .put(dependentRoutesIterations, numBgpMultipathRibRoutes);
    int numMainRibRoutes =
        nodes
            .values()
            .stream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .mapToInt(vr -> vr._mainRib.getRoutes().size())
            .sum();
    ae.getMainRibRoutesByIteration().put(dependentRoutesIterations, numMainRibRoutes);
  }


  private void computeMultiIterationStatistics(
      Map<String, Node> nodes, BdpAnswerElement ae, int dependentRoutesIterations) {
    int numIsisRibRoutes =
        nodes
            .values()
            .stream()
            .flatMap(n -> n.getVirtualRouters().values().stream())
            .mapToInt(vr -> vr.getMultiIsisRib().getRouteNumber())
            .sum();
    ae.getBgpBestPathRibRoutesByIteration().put(dependentRoutesIterations, numIsisRibRoutes);
  }

  /**
   * Return the main RIB routes for each node. Map structure: Hostname -> VRF name -> Set of routes
   */
  SortedMap<String, SortedMap<String, SortedSet<AbstractRoute>>> getRoutes(
      IncrementalDataPlane dp) {
    // Scan through all Nodes and their virtual routers, retrieve main rib routes
    return dp.getNodes()
        .entrySet()
        .stream()
        .collect(
            ImmutableSortedMap.toImmutableSortedMap(
                Comparator.naturalOrder(),
                Entry::getKey,
                nodeEntry ->
                    nodeEntry
                        .getValue()
                        .getVirtualRouters()
                        .entrySet()
                        .stream()
                        .collect(
                            ImmutableSortedMap.toImmutableSortedMap(
                                Comparator.naturalOrder(),
                                Entry::getKey,
                                vrfEntry ->
                                    ImmutableSortedSet.copyOf(
                                        vrfEntry.getValue().getMainRib().getRoutes())))));
  }

  /**
   * Run the IGP OSPF computation until convergence.
   *
   * @param nodes list of nodes for which to initialize the OSPF routes
   * @param topology the network topology
   * @return the number of iterations it took for internal OSPF routes to converge
   */
  //计算OSPF路由
  int initOspfInternalRoutes(Map<String, Node> nodes, Topology topology) {
    AtomicBoolean ospfInternalChanged = new AtomicBoolean(true);
    int ospfInternalIterations = 0;
    while (ospfInternalChanged.get()) {//直到收敛
      ospfInternalIterations++;
      ospfInternalChanged.set(false);

      //这一部分是获取不同area间的路由关系的一个函数
      AtomicInteger ospfInterAreaSummaryCompleted =
          _newBatch.apply(
              "Compute OSPF Inter-area summaries: iteration " + ospfInternalIterations,
              nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n.getVirtualRouters().values()) {
                  if (vr.computeInterAreaSummaries()) {
                    ospfInternalChanged.set(true);//
                  }
                }
                ospfInterAreaSummaryCompleted.incrementAndGet();
              });
      //这一部分是从链接的节点处获得相应的路由
      //大致就是查看接口的area以及对连接口的area看是否匹配,看看能否传递路由.然后查询相应的路由表,查看路由的area看看能否相互传递,若能相互传递则更新
      AtomicInteger ospfInternalCompleted =
          _newBatch.apply(
              "Compute OSPF Internal routes: iteration " + ospfInternalIterations, nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n.getVirtualRouters().values()) {
                  if (vr.propagateOspfInternalRoutes(nodes, topology)) {
                    ospfInternalChanged.set(true);
                  }
                }
                ospfInternalCompleted.incrementAndGet();
              });
      //这一部分是把阶段性的路由表更新到总的路由表当中去,因为可能有不同的路由实例都在运行从而会导致路由表的不同
      AtomicInteger ospfInternalUnstageCompleted =
          _newBatch.apply(
              "Unstage OSPF Internal routes: iteration " + ospfInternalIterations, nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n.getVirtualRouters().values()) {
                  vr.unstageOspfInternalRoutes();
                }
                ospfInternalUnstageCompleted.incrementAndGet();
              });
    }
    //这一部分是合并interal和intra路由到总的OSPF的路由表
    AtomicInteger ospfInternalImportCompleted =
        _newBatch.apply("Import OSPF Internal routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                vr.importOspfInternalRoutes();
              }
              ospfInternalImportCompleted.incrementAndGet();
            });
    return ospfInternalIterations;
  }

  /**
   * Run the IGP RIP computation until convergence
   *
   * @param nodes nodes for which to initialize the routes, keyed by name
   * @param topology network topology
   * @return number of iterations it took to complete the initialization
   */
  private int initRipInternalRoutes(SortedMap<String, Node> nodes, Topology topology) {
    /*
     * Consider this method to be a simulation within a simulation. Since RIP routes are not
     * affected by other protocols, we propagate all RIP routes amongst the nodes prior to
     * processing other routing protocols (e.g., OSPF & BGP)
     */
    AtomicBoolean ripInternalChanged = new AtomicBoolean(true);
    int ripInternalIterations = 0;
    while (ripInternalChanged.get()) {
      ripInternalIterations++;
      ripInternalChanged.set(false);
      //这一部分相当于从邻居节点计算路由,过程也是遍历这个节点的每一个边,然后获取邻居节点的RIB route信息来更新自己的RIB route信息
      AtomicInteger ripInternalCompleted =
          _newBatch.apply(
              "Compute RIP Internal routes: iteration " + ripInternalIterations, nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n.getVirtualRouters().values()) {
                  if (vr.propagateRipInternalRoutes(nodes, topology)) {
                    ripInternalChanged.set(true);
                  }
                }
                ripInternalCompleted.incrementAndGet();
              });
      //这一部分也是把哥哥路由实例的路由信息合并到总的路由表当中
      AtomicInteger ripInternalUnstageCompleted =
          _newBatch.apply(
              "Unstage RIP Internal routes: iteration " + ripInternalIterations, nodes.size());
      nodes
          .values()
          .parallelStream()
          .forEach(
              n -> {
                for (VirtualRouter vr : n.getVirtualRouters().values()) {
                  vr.unstageRipInternalRoutes();
                }
                ripInternalUnstageCompleted.incrementAndGet();
              });
    }
    //这一部分是形成最终的一个路由表
    AtomicInteger ripInternalImportCompleted =
        _newBatch.apply("Import RIP Internal routes", nodes.size());
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                importRib(vr._ripRib, vr._ripInternalRib);
                importRib(vr._independentRib, vr._ripRib);
              }
              ripInternalImportCompleted.incrementAndGet();
            });
    return ripInternalIterations;
  }
  public boolean computeIsisRoutes(
      SortedMap<String, Node> nodes,
      Map<Ip, Set<String>> ipOwners,
      Map<String, Configuration> configurations,
      IsisTopology isisTopology,
      Topology topology,
      BdpAnswerElement ae
      )
  {
    Map<Integer, SortedSet<Integer>> iterationsByHashCode = new HashMap<>();

    Schedule currentSchedule = _settings.getScheduleName();

    AtomicBoolean dependentRoutesChanged = new AtomicBoolean(false);

    nodes
        .values()
        .forEach(
            n->{
              for(VirtualRouter vr:n.getVirtualRouters().values())
              {
                vr.initIsisExports(0,nodes,configurations);
              }
            }
        );

    // Go into iteration mode, until the routes converge (or oscillation is detected)
    do {
      _numIterations++;
      dependentRoutesChanged.set(false);
      // Compute node schedule
      IbdpSchedule schedule = IbdpSchedule.getIsisSchedule(_settings, nodes, isisTopology);


      // compute dependent routes for each allowable set of nodes until we cover all nodes
      int nodeSet = 0;
      while (schedule.hasNext()) {
        Map<String, Node> iterationNodes = schedule.next();
        String iterationlabel = String.format("Iteration %d Schedule %d", _numIterations, nodeSet);
        computeDependentRoutesIteration(
            iterationNodes,iterationlabel,nodes,configurations,_numIterations
        );
//        computeDependentRoutesIteration(
//            iterationVrs, iterationlabel, nodes, networkConfigurations, provider, _numIterations);
        ++nodeSet;
      }

      // Tell each VR that a route computation round has ended.
      // This must be the last thing called on a VR in a routing round.
//      vrs.parallelStream().forEach(VirtualRouter::endOfEgpRound);

      /*
       * Perform various bookkeeping at the end of the iteration:
       * - Collect sizes of certain RIBs this iteration
       * - Compute iteration hashcode
       * - Check for oscillations
       */
      AtomicInteger checkFixedPointCompleted =
          _newBatch.apply(
              "Iteration " + _numIterations + ": Check if fixed-point reached", nodes.size());
      computeIterationStatistics(nodes, ae, _numIterations);

      // This hashcode uniquely identifies the iteration (i.e., network state)
      int iterationHashCode = computeIterationHashCode(nodes);
      SortedSet<Integer> iterationsWithThisHashCode =
          iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());

      if (iterationsWithThisHashCode.isEmpty()) {
        iterationsWithThisHashCode.add(_numIterations);
      } else {
        // If oscillation detected, switch to a more restrictive schedule
        if (currentSchedule != Schedule.NODE_SERIALIZED) {
          currentSchedule = Schedule.NODE_SERIALIZED;
        } else {
          return true; // Found an oscillation
        }
      }
      IsisCompareToPreviousIteration(nodes,dependentRoutesChanged,checkFixedPointCompleted);
    } while (!isisAreQueuesEmpty(nodes) || dependentRoutesChanged.get());
    return false;
  }

  public boolean computeMultiIsisRoutes(
      SortedMap<String, Node> nodes,
      Map<Ip, Set<String>> ipOwners,
      Map<String, Configuration> configurations,
      IsisTopology isisTopology,
      Topology topology,
      BdpAnswerElement ae
  )
  {
    Map<Integer, SortedSet<Integer>> iterationsByHashCode = new HashMap<>();

    Schedule currentSchedule = _settings.getScheduleName();

    AtomicBoolean dependentRoutesChanged = new AtomicBoolean(false);

    nodes
        .values()
        .forEach(
            n->{
              for(VirtualRouter vr:n.getVirtualRouters().values())
              {
                vr.initIsisMultiQueuesAndFactory(isisTopology);
              }
            }
        );
    nodes
        .values()
        .forEach(
            n->{
              for(VirtualRouter vr:n.getVirtualRouters().values())
              {
                vr.initMultiIsisExports(nodes,configurations);
              }
            }
        );



    // Go into iteration mode, until the routes converge (or oscillation is detected)
    do {
      _numIterations++;
      dependentRoutesChanged.set(false);
      // Compute node schedule
      IbdpSchedule schedule = IbdpSchedule.getIsisSchedule(_settings, nodes, isisTopology);


      // compute dependent routes for each allowable set of nodes until we cover all nodes
      int nodeSet = 0;
      while (schedule.hasNext()) {
        Map<String, Node> iterationNodes = schedule.next();
        computeMultiIsisDependentRoutesIteration(
            iterationNodes,nodes,configurations
        );
        ++nodeSet;
      }

      AtomicInteger checkFixedPointCompleted =
          _newBatch.apply(
              "Iteration " + _numIterations + ": Check if fixed-point reached", nodes.size());
      computeMultiIterationStatistics(nodes, ae, _numIterations);

      // This hashcode uniquely identifies the iteration (i.e., network state)
      int iterationHashCode = computeIterationHashCode(nodes);
      SortedSet<Integer> iterationsWithThisHashCode =
          iterationsByHashCode.computeIfAbsent(iterationHashCode, h -> new TreeSet<>());

      if (iterationsWithThisHashCode.isEmpty()) {
        iterationsWithThisHashCode.add(_numIterations);
      } else {
        // If oscillation detected, switch to a more restrictive schedule
        if (currentSchedule != Schedule.NODE_SERIALIZED) {
          currentSchedule = Schedule.NODE_SERIALIZED;
        } else {
          return true; // Found an oscillation
        }
      }
      IsisCompareToPreviousIteration(nodes,dependentRoutesChanged,checkFixedPointCompleted);
    } while (!multiIsisAreQueuesEmpty(nodes) || dependentRoutesChanged.get());
    return false;
  }

  private void IsisCompareToPreviousIteration(
      Map<String, Node> nodes,
      AtomicBoolean dependentRoutesChanged,
      AtomicInteger checkFixedPointCompleted) {
    nodes
        .values()
        .parallelStream()
        .forEach(
            n -> {
              for (VirtualRouter vr : n.getVirtualRouters().values()) {
                if (vr._isisRibChanged=true) {
                  dependentRoutesChanged.set(true);
                  break;
                }
              }
              checkFixedPointCompleted.incrementAndGet();
            });
  }

  private static void computeDependentRoutesIteration(
      Map<String, Node> iterationNodes,
      String iterationLabel,
      Map<String, Node> allNodes,
      Map<String, Configuration> configurations,
      int iteration) {

    // IS-IS route propagation
    AtomicBoolean isisChanged = new AtomicBoolean(true);
    int isisSubIterations = 0;
    while (isisChanged.get()) {
      isisSubIterations++;
//      LOGGER.info("{}: Recompute IS-IS routes: subIteration {}", iterationLabel, isisSubIterations);
      isisChanged.set(false);
      iterationNodes
          .values()
          .forEach(
              n->{
                for(VirtualRouter vr:n.getVirtualRouters().values())
                {
                  RibDelta<IsisRoute> p =
                      vr.propagateIsisRoutes(configurations);
                  if((p!=null)
                      &&vr.unstageIsisRoutes(allNodes,configurations,p))
                  {
                    isisChanged.set(true);
                  }
                }
              }
          );
    }
  }

  private static void computeMultiIsisDependentRoutesIteration(
      Map<String, Node> iterationNodes,
      Map<String, Node> allNodes,
      Map<String, Configuration> configurations) {

    iterationNodes
        .values()
        .forEach(
            n->{
              for (VirtualRouter vr:n.getVirtualRouters().values())
              {
                vr._isisMultiRib.initIsisMultiRib();
              }
            }
        );
    // IS-IS route propagation
    AtomicBoolean isisChanged = new AtomicBoolean(true);
    iterationNodes
        .values()
        .forEach(
            n->{
              for(VirtualRouter vr:n.getVirtualRouters().values())
              {
                vr.propagateMultiIsisRoutes(configurations);
                vr.queueMultiOutgoingIsisRoutes(allNodes,configurations);
              }
            });
  }

  public boolean isisAreQueuesEmpty(Map<String, Node> nodes)
  {
    for(Node n:nodes.values())
    {
      for(VirtualRouter vr:n.getVirtualRouters().values())
      {
        for(IsisEdge edge:vr._isisNavieIncomingRoutes.keySet())
        {
          if(!vr._isisNavieIncomingRoutes.get(edge).isEmpty())
          {
            return true;
          }
        }
      }
    }
    return false;
  }
  public boolean multiIsisAreQueuesEmpty(Map<String, Node> nodes)
  {
    for(Node n:nodes.values())
    {
      for(VirtualRouter vr:n.getVirtualRouters().values())
      {
        for(IsisEdge edge:vr._isisMultiIncomingRoutes.keySet())
        {
          if(!vr._isisMultiIncomingRoutes.get(edge).isEmpty())
          {
            return true;
          }
        }
      }
    }
    return false;
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



//  public void valleyFree(String middleNode,String srcAs,String leakAs,String prefix,IncrementalDataPlane dp,HashMap<String,List<Integer>> prefixOwnersPrefixEc,Map<Integer,SymPrefixList> prefixEcToSymPrefix,HashMap<Integer,ArrayList<Integer>> prefixEcRelationship,Map<SymPrefixList,Integer> prefixEc,Map<Ip, Set<String>> ipOwners,
//      jdd.bdd.BDD topologyBDD,BDDFactory prefixFactory,Network<BgpNeighbor,BgpSession> bgpTopology,SortedMap<String, Node> nodes)
//  {
//    Integer srcAsNum=Integer.valueOf(srcAs.substring(2));
//    Integer leakAsNum=Integer.valueOf(leakAs.substring(2));
//    List<SymBgpRoute> leakRoutes=new ArrayList<>();
//    List<SymBgpRoute> processRoutes=dp.getBgpRib(middleNode).getOutGoingRoute();
//    for(VirtualRouter vr:nodes.get(middleNode).getVirtualRouters().values())
//    {
//      if(vr._vrf.getBgpProcess()==null)
//      {
//        continue;
//      }
//      for(BgpNeighbor neighbor:vr._vrf.getBgpProcess().getNeighbors().values())
//      {
//        if(bgpTopology.adjacentNodes(neighbor).size()==0&&neighbor.getRemoteAs().equals(leakAsNum))
//        {
//          //获取这个节点的所有的路由策略,获取对应邻居的路由策略
//          RoutingPolicy remoteExportPolicy =
//              vr.getConfiguration().getRoutingPolicies().get(neighbor.getExportPolicy());
//          for(SymBgpRoute outRoute:processRoutes)
//          {
//            if(!outRoute.getNodePath().get(0).equals(srcAs.substring(2)))
//            {
//              continue;
//            }
//            Queue<SymBgpRoute> processOutputRoute=new LinkedList<>();
//            processOutputRoute.add(cloneBgpRoute(outRoute));
//            while(!processOutputRoute.isEmpty())
//            {
//              SymBgpRoute remoteRoute = processOutputRoute.poll();//从宣告消息当中获取这个路由,remoteRoute为传输过来的路由?
//
//              SymBgpRoute.Builder transformedOutgoingRouteBuilder=new SymBgpRoute.Builder(remoteRoute.getPrefixEcNum(),topologyBDD,prefixFactory);//transformedOutgoingRouteBuilder为路由消息的builder,字面意思是转换后的路由消息?代表其是在接受路由的时候才进行相关的处理的?
//              //          RoutingProtocol remoteRouteProtocol = remoteRoute.getEgpProtocol();//remoteRouteProtocol为传输过来的路由使用的路由协议?
//              //          String originalAs=remoteRoute.getAsPath().toString();
//              //          boolean remoteRouteIsBgp =
//              //              remoteRouteProtocol == RoutingProtocol.IBGP
//              //                  || remoteRouteProtocol == RoutingProtocol.BGP;//判断其使用的路由协议是不是BGP协议
//
//              // originatorIP
//              // 根据不同的运行协议进行不同的originatorIp的判断
//              Ip originatorIp;
//
//              originatorIp = vr._vrf.getBgpProcess().getRouterId();
//
//
//              //起源路由与之前的路由一样
//              transformedOutgoingRouteBuilder.setOriginatorIp(originatorIp);
//              transformedOutgoingRouteBuilder.setReceivedFromIp(neighbor.getLocalIp());
//              transformedOutgoingRouteBuilder.setReason(org.batfish.symwork.Reason.NORMAL);
//              transformedOutgoingRouteBuilder.setAsPathSize(remoteRoute.getAsPathSize().getLowerbound(),remoteRoute.getAsPathSize().getUpperbound());
//              /*
//               * clusterList, receivedFromRouteReflectorClient, (originType
//               * for bgp remote route)
//               */
//              transformedOutgoingRouteBuilder.setOriginType(remoteRoute.getOriginType());
//
//              /*
//               * route reflection: reflect everything received from
//               * clients to clients and non-clients. reflect everything
//               * received from non-clients to clients. Do not reflect to
//               * originator
//               */
//
//
//              // Outgoing asPath
//              // Outgoing communities
//              // 然后是处理路由的aspath\communities\拓扑条件
//              transformedOutgoingRouteBuilder.setAsPath(new AsPath(remoteRoute.getAsPath().getAsSets()));
//              transformedOutgoingRouteBuilder.setTopologyCondition(remoteRoute.getTopologyCondition());
//
//              //先不考虑community的事情
//              if(neighbor.getSendCommunity())
//              {
//                transformedOutgoingRouteBuilder._symCommunities=new SymCommunity(remoteRoute._symCommunities);
//                //            transformedOutgoingRouteBuilder._arbitraryCommunity=remoteRoute._arbitraryCommunity?true:false;
//                //            transformedOutgoingRouteBuilder.getCommunities().addAll(remoteRoute.getCommunities());
//              }else{
//                transformedOutgoingRouteBuilder._symCommunities=new SymCommunity(false);
//              }
//
//              boolean external=remoteRoute.getExternal();
//              //处理ASpath\拓扑条件\协议\ip等等
//              SortedSet<Integer> newAsPathElement = new TreeSet<>();
//              newAsPathElement.add(neighbor.getLocalAs());
//              transformedOutgoingRouteBuilder.getAsPath().add(newAsPathElement);
//
//
//              //              transformedOutgoingRouteBuilder.setTopologyCondition(transformedOutgoingRouteBuilder.getTopologyCondition().and(linkTopology));
//
//              transformedOutgoingRouteBuilder.setNodePath(remoteRoute.getNodePath());
//              transformedOutgoingRouteBuilder.addNodePath(neighbor.getOwner().getHostname());
//              transformedOutgoingRouteBuilder.setExternal(external);
//              //set relatedNode
//              transformedOutgoingRouteBuilder._relatedNode.addAll(remoteRoute.getRelatedNode());
//              //set prefixEcNum
//              transformedOutgoingRouteBuilder._prefixEcNum=remoteRoute.getPrefixEcNum();
//
//
//              //设置metric,暂时不需要
//              // Outgoing localPreference
//              //          if(remoteRouteIsBgp)
//              //          {
//              //            transformedOutgoingRouteBuilder.setMetric(remoteRoute.getMetric());
//              //          }
//
//
//              //设置localPreference
//              SymBounder localPreference=remoteRoute.getLocalPreference();
//              Ip nextHopIp=neighbor.getLocalIp();//之考虑ebgp所以下一跳的Ip就是邻居的Ip
//              transformedOutgoingRouteBuilder.setSrcNode(remoteRoute.getSrcNode());
//              transformedOutgoingRouteBuilder.setLocalPreference(BgpRoute.DEFAULT_LOCAL_PREFERENCE,BgpRoute.DEFAULT_LOCAL_PREFERENCE);
//              transformedOutgoingRouteBuilder.setNextHopIp(nextHopIp);
//              transformedOutgoingRouteBuilder.setSrcProtocol(remoteRoute.getEgpProtocol());//后续注意一下getRoutingProtocol和getProcotol的区别
//
//              //she zhi chu lai de ACL
//              //               transformedOutgoingRouteBuilder.setSrcAcl(remoteRoute.getSrcAcl().or(vr._inSrcAcl.get(remoteNode)));
//              //               transformedOutgoingRouteBuilder.setDstAcl(remoteRoute.getDstAcl().or(_inDstAcl.get(remoteNode)));
//              SymPolicyAnswer acceptOutgoing =
//                  remoteExportPolicy.symProcess(
//                      outRoute,
//                      transformedOutgoingRouteBuilder,
//                      neighbor.getLocalIp(),
//                      neighbor.getPrefix(),
//                      vr._vrf.getName(),
//                      Direction.OUT
//                  );
//              if(acceptOutgoing._divideRotue!=null)
//              {
//                processOutputRoute.add(acceptOutgoing._divideRotue);
//              }
//              if(acceptOutgoing._accept)
//              {
//                leakRoutes.add(transformedOutgoingRouteBuilder.build());
//              }
//            }
//          }
//        }
//      }
//    }
//    if(leakRoutes.size()==0)
//    {
//      System.out.println("不会泄露相关路由！");
//    }else{
//      int leakNum=1;
//      for(SymBgpRoute leakRoute:leakRoutes)
//      {
//        System.out.println("------泄露的路由宣告："+leakNum+"------");
//        {
//          System.out.println("路由前缀信息：");
//          SymPrefixList prefixList=prefixEcToSymPrefix.get(leakRoute.getPrefixEcNum());
//          for(BDD ip:prefixList._prefixList.keySet())
//          {
//            System.out.print("Ip:");
//            ip.printSet();
//            String dstSymMask = String.format("%32s", Long.toString(prefixList._prefixList.get(ip),2)).replace(' ', '0');
//            System.out.println("Mask:"+dstSymMask);
//            System.out.println();
//          }
//          System.out.print("生效链路状态条件：");
//          System.out.println("数据包转发路径："+leakRoute.getNodePath().toString());
//          System.out.println("导致转发的路由属性：");
//          System.out.println("As-Path:"+leakRoute.getAsPath().toString());
//          System.out.println("符号化的路径长度范围："+leakRoute.getAsPathSize().getLowerbound()+"-"+leakRoute.getAsPathSize().getUpperbound());
//          System.out.println("Local-Preference属性值:"+leakRoute.getLocalPreference().getLowerbound());
//          System.out.println("符号化团体属性值：");
//          System.out.print("   携带的值：");
//          for(Long community:leakRoute._symCommunities._permitCommunities)
//          {
//            System.out.print(CommonUtil.longToCommunity(community)+" ");
//          }
//          System.out.println();
//          System.out.print("   禁止的值：");
//          for(Long community:leakRoute._symCommunities._denyCommunities)
//          {
//            System.out.print(CommonUtil.longToCommunity(community)+" ");
//          }
//          System.out.println();
//        }
//        leakNum++;
//      }
//    }
//  }


//  public boolean getActualReachability(String srcNode,String dstNode,String dstIp,IncrementalDataPlane dp,HashMap<String,List<Integer>> prefixOwnersPrefixEc,Map<Integer,SymPrefixList> prefixEcToSymPrefix,HashMap<Integer,ArrayList<Integer>> prefixEcRelationship,Map<SymPrefixList,Integer> prefixEc,Map<Ip, Set<String>> ipOwners,BDDFactory factory,BDDFactory prefixFactory,int kFailures)
//  {
////    Prefix dstPrefix=Prefix.parse(dstIp);
//    HashMap<Prefix, List<IsisRoute>> isisRib=dp.getIsisRib().get(srcNode);
//    List<IsisRoute> isisRoute=new ArrayList<>();
////    if(isisRib.containsKey(dstPrefix))
////    {
////      isisRoute.addAll(isisRib.get(dstPrefix));
////    }
//    if(isisRoute.size()!=0)
//    {
//      Map<Integer,List<List<SymBgpRoute>>> srcRib=dp.getBgpRib().get(srcNode);
//
//      SymPrefixList dstSymPrefixList=SingleConvertPrefixToSymPrefix.convert(dstIp,prefixFactory);
//      HashMap<BDD,Long> dstSymPrefix=dstSymPrefixList.getPrefixList();
//
//      HashMap<HashMap<BDD,String>,List<List<SymBgpRoute>>> subBgpRoutes=new HashMap<>();
//
//      for(Integer bgpRibPrefixEc:srcRib.keySet())
//      {
//        HashMap<BDD,Long> bgpRibSymPrefix=prefixEcToSymPrefix.get(bgpRibPrefixEc).getPrefixList();
//        HashMap<BDD,String> subPrefix=new HashMap<>();
//        StringBuilder prefixLen=new StringBuilder("00000000000000000000000000000000");
//        for(BDD ribSymIp:bgpRibSymPrefix.keySet())
//        {
//          String ribSymMask = String.format("%32s", Long.toString(bgpRibSymPrefix.get(ribSymIp),2)).replace(' ', '0');
//          int ribMaxMask=ribSymMask.lastIndexOf("1");
//          StringBuilder ribSymMaskBuilder=new StringBuilder(ribSymMask);
//          for(BDD dstSymIp:dstSymPrefix.keySet())
//          {
//            String dstSymMask = String.format("%32s", Long.toString(dstSymPrefix.get(dstSymIp),2)).replace(' ', '0');
//            int dstMaxMask = dstSymMask.lastIndexOf("1");
//            if(!ribSymIp.and(dstSymIp).isZero()&&ribMaxMask>dstMaxMask)
//            {
//              subPrefix.put(ribSymIp.and(dstSymIp),ribSymMaskBuilder.replace(0,dstMaxMask+1,prefixLen.substring(0,dstMaxMask+1)).toString());
//            }
//          }
//        }
//        if(subPrefix.size()!=0)
//        {
//          subBgpRoutes.put(subPrefix,srcRib.get(bgpRibPrefixEc));
//        }
//      }
//      if(subBgpRoutes.size()!=0)
//      {
////        System.out.println("May contains sub-prefix route from external that affect the reachability for the prefix!");
////        System.out.println("The routes are:");
//        for(HashMap<BDD,String> prefix:subBgpRoutes.keySet())
//        {
////          System.out.println("prefix information:");
//          for(BDD ip:prefix.keySet())
//          {
////            System.out.print("prefix-ip:");
////            ip.printSet();
////            System.out.println("mask-len:"+prefix.get(ip));
////            System.out.println();
//          }
////          System.out.println();
////          System.out.println("------------------------------------");
////          System.out.println("best sub-route information:");
//          List<SymBgpRoute> routeList=subBgpRoutes.get(prefix).get(0);
//          for(SymBgpRoute externalRoute:routeList)
//          {
////            System.out.println("src as:"+externalRoute.getAsPath().getAsSets().get(0));
////            System.out.println("local preference:"+externalRoute.getLocalPreference().getLowerbound()+"-"+externalRoute.getLocalPreference().getUpperbound());
////            System.out.println("formal as-path size:"+externalRoute.getAsPathSize().getLowerbound()+"-"+externalRoute.getAsPathSize().getUpperbound());
////            System.out.println("confirmed as-path:"+externalRoute.getAsPath().toString());
////            System.out.println("node path:"+externalRoute.getNodePath().toString());
////            System.out.print("topology-condition:");
////            externalRoute.getTopologyCondition().printSet();
//          }
//        }
//      }else{
//        BDD tc=factory.zero();
//        for(IsisRoute route:isisRoute)
//        {
//          tc=tc.or(route.getTopologyCondition());
//        }
//        tc=tc.not();
//        int failToleracne=findMin(tc);
//        if(failToleracne<=kFailures)
//        {
//          System.out.println("Failed:cant tolerate "+kFailures+"fails about prefix:"+dstIp);
//        }else{
//          System.out.println("Verified:can tolerate "+kFailures+"fails about prefix:"+dstIp);
//        }
//        tc.free();
//        System.out.println();
//        System.out.println();
//      }
//    }else{
//      List<Integer> dstPrefixList=prefixOwnersPrefixEc.get(dstNode);
//      Map<Integer,List<List<SymBgpRoute>>> srcRib=dp.getBgpRib().get(srcNode);
////      SymPrefixList dstSymPrefixList=SingleConvertPrefixToSymPrefix.convert(dstIp,prefixFactory);
//
//      if(dstPrefixList.size()==0)
//      {
//        return false;
//      }
//      SymPrefixList dstSymPrefixList=prefixEcToSymPrefix.get(dstPrefixList.get(0));
//      if(prefixEc.get(dstSymPrefixList)==null)
//      {
////        System.out.print("No Route");
//        return false;
//      }
//      Integer prefix=prefixEc.get(dstSymPrefixList);
////      System.out.println("Verification prefix ec number:"+prefix);
////      System.out.println("Specific prefix information:");
////      for(BDD ip:dstSymPrefixList._prefixList.keySet())
////      {
////        System.out.print("Ip:");
////        ip.printSet();
////        String dstSymMask = String.format("%32s", Long.toString(dstSymPrefixList._prefixList.get(ip),2)).replace(' ', '0');
////        System.out.println("Mask:"+dstSymMask);
////        System.out.println();
////      }
//      System.out.println();
//      System.out.println();
//      if(srcRib.containsKey(prefix))
//      {
//        if(srcRib.get(prefix)==null)
//        {
////          System.out.println("dont have related route about prefix:"+prefix);
//          return false;
//        }else{
//          BDD tc=factory.zero();
//          List<SymBgpRoute> externalRoutes=new ArrayList<>();
//          for(List<SymBgpRoute> routeList:srcRib.get(prefix))
//          {
//            for(SymBgpRoute route:routeList)
//            {
//              if(route._external)
//              {
//                externalRoutes.add(route);
//              }else{
//                tc=tc.or(route.getTopologyCondition());
//              }
//            }
//            if(externalRoutes.size()!=0)
//            {
//              break;
//            }
//          }
//          if(externalRoutes.size()==0)
//          {
//            tc=tc.not();
//            int failToleracne=findMin(tc);
//            if(failToleracne<=kFailures)
//            {
////              System.out.println("cant tolerate "+kFailures+"fails about prefix:"+prefix);
//              return false;
//            }else{
////              System.out.println("can tolerate "+kFailures+"fails about prefix:"+prefix);
//            }
//          }else{
//            tc=tc.not();
//            int failToleracne=findMin(tc);
//            if(failToleracne<=kFailures)
//            {
////              System.out.println("cant tolerate "+kFailures+"fails about prefix:"+prefix);
//              return false;
//            }else{
////              System.out.println("can tolerate "+kFailures+"fails about prefix:"+prefix);
//            }
////            System.out.println();
////            System.out.println();
////            System.out.println("when topologyCondition is:");
////            tc.printSet();
////            System.out.println("the flows will goes to external according to the routes:");
//            for(SymBgpRoute externalRoute:externalRoutes)
//            {
////              System.out.println("src as:"+externalRoute.getAsPath().getAsSets().get(0));
////              System.out.println("local preference:"+externalRoute.getLocalPreference().getLowerbound()+"-"+externalRoute.getLocalPreference().getUpperbound());
////              System.out.println("formal as-path size:"+externalRoute.getAsPathSize().getLowerbound()+"-"+externalRoute.getAsPathSize().getUpperbound());
////              System.out.println("confirmed as-path:"+externalRoute.getAsPath().toString());
////              System.out.println("node path:"+externalRoute.getNodePath().toString());
////              System.out.println();
//            }
//            return false;
//          }
//          tc.free();
//        }
//
////        System.out.println("-------------------------sub-prefix affect:-------------------------");
//
//
//        HashMap<HashMap<BDD,String>,List<List<SymBgpRoute>>> subBgpRoutes=new HashMap<>();
//
//        boolean subAffect=false;
//        for(Integer bgpRibPrefixEc:srcRib.keySet())
//        {
//          HashMap<BDD,Long> bgpRibSymPrefix=prefixEcToSymPrefix.get(bgpRibPrefixEc).getPrefixList();
//          HashMap<BDD,String> subPrefix=new HashMap<>();
//          StringBuilder prefixLen=new StringBuilder("00000000000000000000000000000000");
//          for(BDD ribSymIp:bgpRibSymPrefix.keySet())
//          {
//            String ribSymMask = String.format("%32s", Long.toString(bgpRibSymPrefix.get(ribSymIp),2)).replace(' ', '0');
//            int ribMaxMask=ribSymMask.lastIndexOf("1");
//            StringBuilder ribSymMaskBuilder=new StringBuilder(ribSymMask);
//            for(BDD dstSymIp:dstSymPrefixList._prefixList.keySet())
//            {
//              String dstSymMask = String.format("%32s", Long.toString(dstSymPrefixList._prefixList.get(dstSymIp),2)).replace(' ', '0');
//              int dstMaxMask = dstSymMask.lastIndexOf("1");
//              if(!ribSymIp.and(dstSymIp).isZero()&&ribMaxMask>dstMaxMask)
//              {
//                subPrefix.put(ribSymIp.and(dstSymIp),ribSymMaskBuilder.replace(0,dstMaxMask+1,prefixLen.substring(0,dstMaxMask+1)).toString());
//              }
//            }
//          }
//          if(subPrefix.size()!=0)
//          {
//            subBgpRoutes.put(subPrefix,srcRib.get(bgpRibPrefixEc));
//          }
//        }
//        if(subBgpRoutes.size()!=0)
//        {
//          subAffect=true;
////          System.out.println("May contains sub-prefix route from external that affect the reachability for the prefix!");
////          System.out.println("The routes are:");
//          for(HashMap<BDD,String> prefixSub:subBgpRoutes.keySet())
//          {
////            System.out.println("prefix information:");
//            for(BDD ip:prefixSub.keySet())
//            {
////              System.out.print("prefix-ip:");
////              ip.printSet();
////              System.out.println("mask-len:"+prefixSub.get(ip));
////              System.out.println();
//            }
////            System.out.println();
////            System.out.println("------------------------------------");
////            System.out.println("best sub-route information:");
//            List<SymBgpRoute> routeList=subBgpRoutes.get(prefixSub).get(0);
//            for(SymBgpRoute externalRoute:routeList)
//            {
////              System.out.println("src as:"+externalRoute.getAsPath().getAsSets().get(0));
////              System.out.println("local preference:"+externalRoute.getLocalPreference().getLowerbound()+"-"+externalRoute.getLocalPreference().getUpperbound());
////              System.out.println("formal as-path size:"+externalRoute.getAsPathSize().getLowerbound()+"-"+externalRoute.getAsPathSize().getUpperbound());
////              System.out.println("confirmed as-path:"+externalRoute.getAsPath().toString());
////              System.out.println("node path:"+externalRoute.getNodePath().toString());
////              System.out.print("topology-condition:");
////              externalRoute.getTopologyCondition().printSet();
//            }
//          }
//          return false;
//        }
//
//
//        //sub-prefix affect
////        List<Integer> containsPrefix=prefixEcRelationship.get(prefix);
////        Boolean subAffect=false;
////        for(Integer cPrefix:containsPrefix)
////        {
////          BDD subTc=factory.zero();
////          boolean affect=false;
////          List<SymBgpRoute> affectRoutes=new ArrayList<>();
////          if(srcRib.get(cPrefix)!=null)
////          {
////            List<List<SymBgpRoute>> subRoutes=srcRib.get(cPrefix);
////            for(List<SymBgpRoute> routeList:subRoutes)
////            {
////              for(SymBgpRoute route:routeList)
////              {
////                if(!route.getNodePath().get(0).equals(dstNode))
////                {
////                  affect=true;
////                  affectRoutes.add(route);
////                }
////              }
////              if(affect)
////              {
////                break;
////              }
////              for(SymBgpRoute route:routeList)
////              {
////                subTc=subTc.or(route.getTopologyCondition());
////              }
////            }
////          }
////          subTc=subTc.not();
////          if(affectRoutes.size()==0)
////          {
////            continue;
////          }else{
////            subAffect=true;
////            System.out.println("about sub-prefix,it will prefer sub-prefix route about prefix:"+cPrefix);
////            int failToleracne=findMin(subTc);
////            if(failToleracne<=kFailures)
////            {
////              System.out.println("cant tolerate "+kFailures+"fails about sub-prefix:"+prefix);
////            }else{
////              System.out.println("can tolerate "+kFailures+"fails about sub-prefix:"+prefix);
////            }
////            System.out.println();
////            System.out.println();
////            System.out.println("when topologyCondition is:");
////            subTc.printSet();
////            System.out.println("the flows will goes to other nodes according to the routes:");
////            for(SymBgpRoute affectRoute:affectRoutes)
////            {
////              System.out.println("src as:"+affectRoute.getAsPath().getAsSets().get(0));
////              System.out.println("local preference:"+affectRoute.getLocalPreference().getLowerbound()+"-"+affectRoute.getLocalPreference().getUpperbound());
////              System.out.println("formal as-path size:"+affectRoute.getAsPathSize().getLowerbound()+"-"+affectRoute.getAsPathSize().getUpperbound());
////              System.out.println("confirmed as-path:"+affectRoute.getAsPath().toString());
////              System.out.println("node path:"+affectRoute.getNodePath().toString());
////              System.out.println();
////            }
////          }
////          subTc.free();
////        }
//        if(!subAffect)
//        {
////          System.out.println("no sub-prefix route affect!");
//          return true;
//        }
//      }else{
////        System.out.print("dont have route to the prefix:"+dstIp);
//        return false;
//      }
//    }
//    return true;
//  }









//  public boolean getActualReachability(String srcNode,String dstNode,String dstIp,IncrementalDataPlane dp,HashMap<String,List<Integer>> prefixOwnersPrefixEc,Map<Integer,SymPrefixList> prefixEcToSymPrefix,HashMap<Integer,ArrayList<Integer>> prefixEcRelationship,Map<SymPrefixList,Integer> prefixEc,Map<Ip, Set<String>> ipOwners,
//      jdd.bdd.BDD topologyBDD,BDDFactory prefixFactory,int kFailures,int kFailuresTopology)
//  {
//    List<Integer> dstPrefixList=prefixOwnersPrefixEc.get(dstNode);
//    Map<Integer,List<List<SymBgpRoute>>> srcRib=dp.getBgpRib().get(srcNode);
//    if(dstPrefixList.size()==0)
//    {
//      return false;
//    }
//    SymPrefixList dstSymPrefixList=prefixEcToSymPrefix.get(dstPrefixList.get(0));
//    if(prefixEc.get(dstSymPrefixList)==null)
//    {
//      return false;
//    }
//    for(Integer prefix:dstPrefixList)
//    {
//      if(srcRib.containsKey(prefix))
//      {
//        if(srcRib.get(prefix)==null)
//        {
//          return false;
//        }else {
//          int tc = 0;
//          List<SymBgpRoute> externalRoutes = new ArrayList<>();
//          for (List<SymBgpRoute> routeList : srcRib.get(prefix)) {
//            for (SymBgpRoute route : routeList) {
//              if (route.getExternal()) {
//                externalRoutes.add(route);
//              } else {
//                tc = topologyBDD.ref(topologyBDD.or(tc,route.getTopologyCondition()));
//              }
//            }
//            if (externalRoutes.size() != 0) {
//              break;
//            }
//          }
//          if (topologyBDD.and(tc,kFailuresTopology)==0) {
//            return false;
//          }
//          topologyBDD.deref(tc);
//        }
//        //        if(externalRoutes.size()==0)
//        //        {
//        //          tc=tc.not();
//        //          int failToleracne=findMin(tc);
//        //          if(failToleracne<=kFailures)
//        //          {
//        //            return false;
//        //          }else{
//        //            //              System.out.println("can tolerate "+kFailures+"fails about prefix:"+prefix);
//        //          }
//        //        }else{
//        //          tc=tc.not();
//        //          int failToleracne=findMin(tc);
//        //          if(failToleracne<=kFailures)
//        //          {
//        //            //              System.out.println("cant tolerate "+kFailures+"fails about prefix:"+prefix);
//        //            return false;
//        //          }else{
//        //            //              System.out.println("can tolerate "+kFailures+"fails about prefix:"+prefix);
//        //          }
//        //          return false;
//        //        }
//        //        tc.free();
//        //      }
//        //        System.out.println("-------------------------sub-prefix affect:-------------------------");
//
////        for (Integer subPrefix : prefixEcRelationship.get(prefix)) {
////          if (srcRib.get(subPrefix) != null) {
////            List<List<SymBgpRoute>> routeList = srcRib.get(subPrefix);
////            for (List<SymBgpRoute> routes : routeList) {
////              for (SymBgpRoute route : routes) {
////                if (!route.getSrcNode().equals(dstNode)) {
////                  return false;
////                }
////              }
////            }
////          }
////        }
//        //      HashMap<Integer,List<List<SymBgpRoute>>> subBgpRoutes=new HashMap<>();
//        //      boolean subAffect=false;
//        //      for(Integer bgpRibPrefixEc:srcRib.keySet())
//        //      {
//        //        HashMap<BDD,Long> bgpRibSymPrefix=prefixEcToSymPrefix.get(bgpRibPrefixEc).getPrefixList();
//        //        HashMap<BDD,String> subPrefix=new HashMap<>();
//        //        StringBuilder prefixLen=new StringBuilder("00000000000000000000000000000000");
//        //        for(BDD ribSymIp:bgpRibSymPrefix.keySet())
//        //        {
//        //          String ribSymMask = String.format("%32s", Long.toString(bgpRibSymPrefix.get(ribSymIp),2)).replace(' ', '0');
//        //          int ribMaxMask=ribSymMask.lastIndexOf("1");
//        //          StringBuilder ribSymMaskBuilder=new StringBuilder(ribSymMask);
//        //          for(BDD dstSymIp:dstSymPrefixList._prefixList.keySet())
//        //          {
//        //            String dstSymMask = String.format("%32s", Long.toString(dstSymPrefixList._prefixList.get(dstSymIp),2)).replace(' ', '0');
//        //            int dstMaxMask = dstSymMask.lastIndexOf("1");
//        //            if(!ribSymIp.and(dstSymIp).isZero()&&ribMaxMask>dstMaxMask)
//        //            {
//        //              subPrefix.put(ribSymIp.and(dstSymIp),ribSymMaskBuilder.replace(0,dstMaxMask+1,prefixLen.substring(0,dstMaxMask+1)).toString());
//        //            }
//        //          }
//        //        }
//        //        if(subPrefix.size()!=0)
//        //        {
//        //          subBgpRoutes.put(subPrefix,srcRib.get(bgpRibPrefixEc));
//        //        }
//        //      }
//        //      if(subBgpRoutes.size()!=0)
//        //      {
//        //        subAffect=true;
//        //        //          System.out.println("May contains sub-prefix route from external that affect the reachability for the prefix!");
//        //        //          System.out.println("The routes are:");
//        //        for(HashMap<BDD,String> prefixSub:subBgpRoutes.keySet())
//        //        {
//        //          //            System.out.println("prefix information:");
//        //          for(BDD ip:prefixSub.keySet())
//        //          {
//        //            //              System.out.print("prefix-ip:");
//        //            //              ip.printSet();
//        //            //              System.out.println("mask-len:"+prefixSub.get(ip));
//        //            //              System.out.println();
//        //          }
//        //          //            System.out.println();
//        //          //            System.out.println("------------------------------------");
//        //          //            System.out.println("best sub-route information:");
//        //          List<SymBgpRoute> routeList=subBgpRoutes.get(prefixSub).get(0);
//        //          for(SymBgpRoute externalRoute:routeList)
//        //          {
//        //            //              System.out.println("src as:"+externalRoute.getAsPath().getAsSets().get(0));
//        //            //              System.out.println("local preference:"+externalRoute.getLocalPreference().getLowerbound()+"-"+externalRoute.getLocalPreference().getUpperbound());
//        //            //              System.out.println("formal as-path size:"+externalRoute.getAsPathSize().getLowerbound()+"-"+externalRoute.getAsPathSize().getUpperbound());
//        //            //              System.out.println("confirmed as-path:"+externalRoute.getAsPath().toString());
//        //            //              System.out.println("node path:"+externalRoute.getNodePath().toString());
//        //            //              System.out.print("topology-condition:");
//        //            //              externalRoute.getTopologyCondition().printSet();
//        //          }
//        //        }
//        //        return false;
//        //      }
//        //
//        //
//        //      //sub-prefix affect
//        //      //        List<Integer> containsPrefix=prefixEcRelationship.get(prefix);
//        //      //        Boolean subAffect=false;
//        //      //        for(Integer cPrefix:containsPrefix)
//        //      //        {
//        //      //          BDD subTc=factory.zero();
//        //      //          boolean affect=false;
//        //      //          List<SymBgpRoute> affectRoutes=new ArrayList<>();
//        //      //          if(srcRib.get(cPrefix)!=null)
//        //      //          {
//        //      //            List<List<SymBgpRoute>> subRoutes=srcRib.get(cPrefix);
//        //      //            for(List<SymBgpRoute> routeList:subRoutes)
//        //      //            {
//        //      //              for(SymBgpRoute route:routeList)
//        //      //              {
//        //      //                if(!route.getNodePath().get(0).equals(dstNode))
//        //      //                {
//        //      //                  affect=true;
//        //      //                  affectRoutes.add(route);
//        //      //                }
//        //      //              }
//        //      //              if(affect)
//        //      //              {
//        //      //                break;
//        //      //              }
//        //      //              for(SymBgpRoute route:routeList)
//        //      //              {
//        //      //                subTc=subTc.or(route.getTopologyCondition());
//        //      //              }//
//        //      //              }
//        //      //          }
//        //      //          subTc=subTc.not();
//        //      //          if(affectRoutes.size()==0)
//        //      //          {
//        //      //            continue;
//        //      //          }else{
//        //      //            subAffect=true;
//        //      //            System.out.println("about sub-prefix,it will prefer sub-prefix route about prefix:"+cPrefix);
//        //      //            int failToleracne=findMin(subTc);
//        //      //            if(failToleracne<=kFailures)
//        //      //            {
//        //      //              System.out.println("cant tolerate "+kFailures+"fails about sub-prefix:"+prefix);
//        //      //            }else{
//        //      //              System.out.println("can tolerate "+kFailures+"fails about sub-prefix:"+prefix);
//        //      //            }
//        //      //            System.out.println();
//        //      //            System.out.println();
//        //      //            System.out.println("when topologyCondition is:");
//        //      //            subTc.printSet();
//        //      //            System.out.println("the flows will goes to other nodes according to the routes:");
//        //      //            for(SymBgpRoute affectRoute:affectRoutes)
//        //      //            {
//        //      //              System.out.println("src as:"+affectRoute.getAsPath().getAsSets().get(0));
//        //      //              System.out.println("local preference:"+affectRoute.getLocalPreference().getLowerbound()+"-"+affectRoute.getLocalPreference().getUpperbound());
//        //      //              System.out.println("formal as-path size:"+affectRoute.getAsPathSize().getLowerbound()+"-"+affectRoute.getAsPathSize().getUpperbound());
//        //      //              System.out.println("confirmed as-path:"+affectRoute.getAsPath().toString());
//        //      //              System.out.println("node path:"+affectRoute.getNodePath().toString());
//        //      //              System.out.println();
//        //      //            }
//        //      //          }
//        //      //          subTc.free();
//        //      //        }
//        //      if(!subAffect)
//        //      {
//        //        //          System.out.println("no sub-prefix route affect!");
//        //        return true;
//        //      }
//      }else{
//        return false;
//      }
//    }
//    return true;
//  }


//  public void getReachability(String srcNode,String dstNode,IncrementalDataPlane dp,HashMap<String,List<Integer>> prefixOwnersPrefixEc,Map<Integer,SymPrefixList> prefixEcToSymPrefix,HashMap<Integer,ArrayList<Integer>> prefixEcRelationship,
//      jdd.bdd.BDD topologyBDD,int kFailures)
//  {
//    HashMap<String,Map<Integer,List<List<SymBgpRoute>>>> rib=dp.getBgpRib();
//    List<Integer> dstPrefix=prefixOwnersPrefixEc.get(dstNode);
//    System.out.println("dst prefix"+dstPrefix.toString());
//    Map<Integer,List<List<SymBgpRoute>>> srcRib=dp.getBgpRib().get(srcNode);
//    for(Integer prefix:dstPrefix)
//    {
//      System.out.println("For prefix:"+prefix);
//
//      SymPrefixList symPrefix=prefixEcToSymPrefix.get(prefix);
//      System.out.println("---前缀的信息:---");
//      for(BDD ip:symPrefix._prefixList.keySet())
//      {
//        ip.printSet();
//        System.out.println("长度:"+Long.toString(symPrefix._prefixList.get(ip),2));
//      }
//      if(srcRib.get(prefix)==null)
//      {
//        System.out.println("dont have related route about prefix:"+prefix);
//      }else{
//        int tc=0;
//        List<SymBgpRoute> externalRoutes=new ArrayList<>();
//        for(List<SymBgpRoute> routeList:srcRib.get(prefix))
//        {
//          for(SymBgpRoute route:routeList)
//          {
//            if(route._external)
//            {
//              externalRoutes.add(route);
//            }else{
//              tc=topologyBDD.ref(topologyBDD.or(tc,route.getTopologyCondition()));
//            }
//          }
//          if(externalRoutes.size()!=0)
//          {
//            break;
//          }
//        }
//        if(externalRoutes.size()==0)
//        {
//          tc=tc.not();
//          int failToleracne=findMin(tc);
//          if(failToleracne<=kFailures)
//          {
//            System.out.println("cant tolerate "+kFailures+"fails about prefix:"+prefix);
//          }else{
//            System.out.println("can tolerate "+kFailures+"fails about prefix:"+prefix);
//          }
//        }else{
//          tc=tc.not();
//          int failToleracne=findMin(tc);
//          if(failToleracne<=kFailures)
//          {
//            System.out.println("cant tolerate "+kFailures+"fails about prefix:"+prefix);
//          }else{
//            System.out.println("can tolerate "+kFailures+"fails about prefix:"+prefix);
//          }
//          System.out.println();
//          System.out.println();
//          System.out.println("when topologyCondition is:");
//          tc.printSet();
//          System.out.println("the flows will goes to external according to the routes:");
//          for(SymBgpRoute externalRoute:externalRoutes)
//          {
//            System.out.println("src as:"+externalRoute.getAsPath().getAsSets().get(0));
//            System.out.println("local preference:"+externalRoute.getLocalPreference().getLowerbound()+"-"+externalRoute.getLocalPreference().getUpperbound());
//            System.out.println("formal as-path size:"+externalRoute.getAsPathSize().getLowerbound()+"-"+externalRoute.getAsPathSize().getUpperbound());
//            System.out.println("confirmed as-path:"+externalRoute.getAsPath().toString());
//          }
//        }
//        tc.free();
//      }
//
//      //sub-prefix affect
//      List<Integer> containsPrefix=prefixEcRelationship.get(prefix);
//      for(Integer cPrefix:containsPrefix)
//      {
//        BDD subTc=factory.zero();
//        boolean affect=false;
//        List<SymBgpRoute> affectRoutes=new ArrayList<>();
//        if(srcRib.get(cPrefix)!=null)
//        {
//          List<List<SymBgpRoute>> subRoutes=srcRib.get(cPrefix);
//          for(List<SymBgpRoute> routeList:subRoutes)
//          {
//            for(SymBgpRoute route:routeList)
//            {
//              if(!route.getNodePath().get(0).equals(dstNode))
//              {
//                affect=true;
//                affectRoutes.add(route);
//              }
//            }
//            if(affect)
//            {
//              break;
//            }
//            for(SymBgpRoute route:routeList)
//            {
//              subTc=subTc.or(route.getTopologyCondition());
//            }
//          }
//        }
//        subTc=subTc.not();
//        if(affectRoutes.size()==0)
//        {
//          continue;
//        }else{
//          int failToleracne=findMin(subTc);
//          if(failToleracne<=kFailures)
//          {
//            System.out.println("cant tolerate "+kFailures+"fails about sub-prefix:"+prefix);
//          }else{
//            System.out.println("can tolerate "+kFailures+"fails about sub-prefix:"+prefix);
//          }
//          System.out.println();
//          System.out.println();
//          System.out.println("when topologyCondition is:");
//          subTc.printSet();
//          System.out.println("the flows will goes to other nodes according to the routes:");
//          for(SymBgpRoute affectRoute:affectRoutes)
//          {
//            System.out.println("src as:"+affectRoute.getAsPath().getAsSets().get(0));
//            System.out.println("local preference:"+affectRoute.getLocalPreference().getLowerbound()+"-"+affectRoute.getLocalPreference().getUpperbound());
//            System.out.println("formal as-path size:"+affectRoute.getAsPathSize().getLowerbound()+"-"+affectRoute.getAsPathSize().getUpperbound());
//            System.out.println("confirmed as-path:"+affectRoute.getAsPath().toString());
//            System.out.println("node path:"+affectRoute.getNodePath().toString());
//            System.out.println();
//          }
//        }
//        subTc.free();
//      }
//    }
//  }
}
