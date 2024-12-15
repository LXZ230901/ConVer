package org.batfish.symwork;

//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Queue;
//import java.util.Set;
//import java.util.TreeSet;
//import net.sf.javabdd.BDD;
//import net.sf.javabdd.BDDFactory;
//import org.batfish.datamodel.IsisLevel;

public class SymTempBgpRib {

//  Map<SymPrefixList, List<SymBgpRoute>> _rib;
//  Map<Integer,List<SymBgpRoute>> _simplifiedRib;
//  Set<SymPrefixList> _changedPrefix;
//  Set<Integer> _simplifiedChangePrefix;
//  BDDFactory _topologyfactory;
//  Set<SymBgpRoute> _withDrawRoute;
//
//  boolean _multiPtahEgp;
//  public SymBgpRib(BDDFactory factory,boolean multiPtahEgp)
//  {
//    _multiPtahEgp=multiPtahEgp;
//    this._rib=new HashMap<>();
//    this._changedPrefix=new HashSet<>();
//    this._simplifiedChangePrefix=new HashSet<>();
//    this._topologyfactory=factory;
//    this._simplifiedRib=new HashMap<>();
//    this._withDrawRoute=new HashSet<>();
//    for(Integer prefix:this._simplifiedRib.keySet())
//    {
//      for(int i=0;i<this._simplifiedRib.get(prefix).size();i++)
//      {
//        this._simplifiedRib.get(prefix).get(i)._reason=Reason.NORMAL;
//      }
//    }
//    for(SymPrefixList prefix:this._rib.keySet())
//    {
//      for(int i=0;i<this._rib.get(prefix).size();i++)
//      {
//        this._rib.get(prefix).get(i)._reason=Reason.NORMAL;
//      }
//    }
//  }
//  public void InitRib()
//  {
//    for(SymPrefixList prefix:this._changedPrefix)
//    {
//      for(int i=0;i<this._rib.get(prefix).size();i++)
//      {
//        this._rib.get(prefix).get(i)._reason=Reason.NORMAL;
//      }
//    }
//    for(Integer prefix:this._simplifiedRib.keySet())
//    {
//      for(int i=0;i<this._simplifiedRib.get(prefix).size();i++)
//      {
//        this._simplifiedRib.get(prefix).get(i)._reason=Reason.NORMAL;
//      }
//    }
//    this._withDrawRoute.clear();
//    this._changedPrefix.clear();
//    this._simplifiedChangePrefix.clear();
//  }
//  public void UpdateRoute(SymBgpRoute route)
//  {
//    Integer routeNetWork=route.getPrefixEcNum();
//    if(!_simplifiedRib.keySet().contains(routeNetWork))
//    {
//      this._simplifiedRib.put(routeNetWork, new ArrayList<>());
//      this._simplifiedRib.get(routeNetWork).add(route);
//      this._simplifiedRib.get(routeNetWork).get(0)._reason=Reason.ADD;
//    }else{
//      List<SymBgpRoute> tempRoute=this._simplifiedRib.get(routeNetWork);
//      int location=-1;
//      for(int i=0;i< tempRoute.size();i++)
//      {
//        if(tempRoute.get(i).equalsExceptTopologyCondition(route))
//        {
//          location=i;
//          break;
//        }
//      }
//      if(location==-1)
//      {
//        this._simplifiedRib.put(routeNetWork, new ArrayList<>());
//        this._simplifiedRib.get(routeNetWork).add(route);
//        this._simplifiedRib.get(routeNetWork).get(0)._reason=Reason.ADD;
//      }else{
//        this._simplifiedRib.get(routeNetWork).remove(location);
//        this._simplifiedRib.get(routeNetWork).add(location,route);
//        this._simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//      }
//    }
//    return ;
//  }
//  //  public void UpdateRoute(SymBgpRoute route)
//  //  {
//  //    SymPrefixList routeNetWork=route.getNetwork();
//  //    if(!_rib.keySet().contains(routeNetWork))
//  //    {
//  //      this._rib.put(routeNetWork, new ArrayList<>());
//  //      this._rib.get(routeNetWork).add(route);
//  //      this._rib.get(routeNetWork).get(0)._reason=Reason.ADD;
//  //    }else{
//  //      List<SymBgpRoute> tempRoute=this._rib.get(routeNetWork);
//  //      int location=-1;
//  //      for(int i=0;i< tempRoute.size();i++)
//  //      {
//  //        if(tempRoute.get(i).getOriginatorIp().equals(route.getOriginatorIp()))
//  //        {
//  //          location=i;
//  //          break;
//  //        }
//  //      }
//  //      if(location==-1)
//  //      {
//  //        this._rib.put(routeNetWork, new ArrayList<>());
//  //        this._rib.get(routeNetWork).add(route);
//  //        this._rib.get(routeNetWork).get(0)._reason=Reason.ADD;
//  //      }else{
//  //        this._rib.get(routeNetWork).remove(location);
//  //        this._rib.get(routeNetWork).add(location,route);
//  //        this._rib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//  //      }
//  //    }
//  //    return ;
//  //  }
//
//  public int GetChanged()
//  {
//    return this._changedPrefix.size();
//  }
//
//  public static int findMin(BDD expression)
//  {
//    //    List<Object> allSolutions = expression.allsat();
//    int minFalseVariables = Integer.MAX_VALUE;
//
//    for (Object solution : expression.allsat()) {
//      byte[] assignment = (byte[]) solution;
//      int falseCount = 0;
//      for (byte b : assignment) {
//        if (b == 0) {
//          falseCount++;
//        }
//      }
//      minFalseVariables = Math.min(minFalseVariables, falseCount);
//    }
//    return minFalseVariables;
//  }
//  public List<SymBgpRoute> GetChangedRoute()
//  {
//    List<SymBgpRoute> answer=new ArrayList<>();
//    //    for(Integer prefix:this._simplifiedChangePrefix)
//    //    {
//    //      BDD topologyCondition=this._topologyfactory.one();
//    //      List<SymBgpRoute> tempRoute=this._simplifiedRib.get(prefix);
//    //      boolean changed=false;
//    //      for(int i=0;i<tempRoute.size();i++)
//    //      {
//    //        if(tempRoute.get(i)._reason==Reason.NORMAL&&changed==false)
//    //        {
//    //          topologyCondition=topologyCondition.and(tempRoute.get(i).getTopologyCondition().not());
//    //          continue;
//    //        }else{
//    //          changed=true;
//    //          SymBgpRoute addRoute=cloneBgpRoute(tempRoute.get(i));
//    //          BDD a=tempRoute.get(i).getTopologyCondition();
//    //          a=a.and(topologyCondition);
//    //          if(!a.equals(_topologyfactory.zero()))
//    //          {
//    //            addRoute.setTopologyCondition(a);
//    //            answer.add(addRoute);
//    //          }
//    //          //          addRoute.setTopologyCondition(a);
//    //          //          //addRoute.setTopologyCondition(tempRoute.get(i).getTopologyCondition().and(topologyCondition));
//    //          //          answer.add(addRoute);
//    //          topologyCondition=topologyCondition.and(tempRoute.get(i).getTopologyCondition().not());
//    //          if(topologyCondition.equals(_topologyfactory.zero()))
//    //            return answer;
//    //          continue;
//    //        }
//    //      }
//    //    }
//    //new topologyCondition update
//    for(Integer prefix:this._simplifiedChangePrefix)
//    {
//      ArrayList<Integer> removedRoute=new ArrayList<>();
//      boolean changed=false;
//      for(int i=0;i<this._simplifiedRib.get(prefix).size();i++)
//      {
//        SymBgpRoute route=this._simplifiedRib.get(prefix).get(i);
//        List<SymBgpRoute> tempRoute=this._simplifiedRib.get(prefix);
//        if(route.getReason()==Reason.NORMAL&& !changed)
//        {
//          continue;
//        }else{
//          changed=true;
//          BDD tc=_topologyfactory.one();
//          for(int j=0;j<i;j++)
//          {
//            if((tempRoute.get(j).getExternal().equals(route.getExternal())&& route._external&&route.getOriginatorIp().equals(tempRoute.get(j).getOriginatorIp()))||tempRoute.get(j).comparePreference(route))
//            {
//              continue;
//            }else{
//              tc=tc.and(tempRoute.get(j).getTopologyCondition().not());
//            }
//          }
//          tc=tc.and(route.getTopologyCondition());
//          if(findMin(tc)>0||tc.isZero())
//          {
//            SymBgpRoute outputRoute=cloneBgpRoute(route);
//            outputRoute._reason=Reason.WITHDRAW;
//            answer.add(outputRoute);
//            removedRoute.add(i);
//            continue;
//          }
//          if(!tc.isZero())
//          {
//            SymBgpRoute outputRoute=cloneBgpRoute(route);
//            if(outputRoute.getReason()==Reason.NORMAL)
//            {
//              outputRoute._reason=Reason.UPDATE;
//            }
//            outputRoute.setTopologyCondition(tc);
//            answer.add(outputRoute);
//          }
//        }
//      }
//      for(int i=removedRoute.size()-1;i>=0;i--)
//      {
//        this._simplifiedRib.get(prefix).remove(removedRoute.get(i));
//      }
//    }
//    answer.addAll(_withDrawRoute);
//    return answer;
//  }
//
//  public void removeRoute(SymBgpRoute symBgpRoute)
//  {
//    if(_simplifiedRib.get(symBgpRoute.getPrefixEcNum()).contains(symBgpRoute))
//    {
//      int index=_simplifiedRib.get(symBgpRoute.getPrefixEcNum()).indexOf(symBgpRoute);
//      _simplifiedRib.get(symBgpRoute.getPrefixEcNum()).get(index)._reason=Reason.WITHDRAW;
//      _withDrawRoute.add(_simplifiedRib.get(symBgpRoute.getPrefixEcNum()).get(index));
//      _simplifiedRib.get(symBgpRoute.getPrefixEcNum()).remove(index);
//    }
//  }
//
//  public List<SymBgpRoute> GetLastAttributeChangedRoute()
//  {
//    List<SymBgpRoute> answer=new ArrayList<>();
//
//    //new topologyCondition update
//    for(Integer prefix:this._simplifiedChangePrefix)
//    {
//      boolean changed=false;
//      for(int i=0;i<this._simplifiedRib.get(prefix).size();i++)
//      {
//        SymBgpRoute route=this._simplifiedRib.get(prefix).get(i);
//        List<SymBgpRoute> tempRoute=this._simplifiedRib.get(prefix);
//        if(route.getReason()==Reason.NORMAL&& !changed)
//        {
//          continue;
//        }else{
//          changed=true;
//          BDD tc=_topologyfactory.one();
//          for(int j=0;j<i;j++)
//          {
//            if((tempRoute.get(j).getExternal().equals(route.getExternal())&& route._external&&route.getOriginatorIp().equals(tempRoute.get(j).getOriginatorIp()))||tempRoute.get(j).comparePreference(route))
//            {
//              continue;
//            }else{
//              tc=tc.and(tempRoute.get(j).getTopologyCondition().not());
//            }
//          }
//          tc=tc.and(route.getTopologyCondition());
//          if(!tc.isZero())
//          {
//            SymBgpRoute outputRoute=cloneBgpRoute(route);
//            if(outputRoute.getReason()==Reason.NORMAL)
//            {
//              outputRoute._reason=Reason.UPDATE;
//            }
//            outputRoute.setTopologyCondition(tc);
//            outputRoute.convertToLastBgpMessage();
//            answer.add(outputRoute);
//          }
//        }
//      }
//    }
//    return answer;
//  }
//
//  //  public List<SymBgpRoute> GetChangedRoute()
//  //  {
//  //    List<SymBgpRoute> answer=new ArrayList<>();
//  //    Boolean changed=false;
//  //    for(SymPrefixList prefix:this._changedPrefix)
//  //    {
//  //      BDD topologyCondition=this._topologyfactory.one();
//  //      List<SymBgpRoute> tempRoute=this._rib.get(prefix);
//  //      for(int i=0;i<tempRoute.size();i++)
//  //      {
//  //        if(tempRoute.get(i)._reason==Reason.NORMAL&&changed==false)
//  //        {
//  //          topologyCondition=topologyCondition.and(tempRoute.get(i).getTopologyCondition().not());
//  //          continue;
//  //        }else{
//  //          changed=true;
//  //          SymBgpRoute addRoute=cloneBgpRoute(tempRoute.get(i));
//  //          BDD a=tempRoute.get(i).getTopologyCondition();
//  //          a=a.and(topologyCondition);
//  //          if(!a.equals(_topologyfactory.zero()))
//  //          {
//  //            addRoute.setTopologyCondition(a);
//  //            answer.add(addRoute);
//  //          }
//  ////          addRoute.setTopologyCondition(a);
//  ////          //addRoute.setTopologyCondition(tempRoute.get(i).getTopologyCondition().and(topologyCondition));
//  ////          answer.add(addRoute);
//  //          topologyCondition=topologyCondition.and(tempRoute.get(i).getTopologyCondition().not());
//  //          if(topologyCondition.equals(_topologyfactory.zero()))
//  //            return answer;
//  //          continue;
//  //        }
//  //      }
//  //    }
//  //    return answer;
//  //  }
//  //  public void AddRoute(SymBgpRoute addRoute)
//  //  {
//  //    SymPrefixList routeNetWork=addRoute.GetNetwork();
//  //    this._changedPrefix.add(routeNetWork);
//  //    if(!this._rib.containsKey(routeNetWork))
//  //    {
//  //      this._rib.put(routeNetWork, new ArrayList<>());
//  //      this._rib.get(routeNetWork).add(addRoute);
//  //      this._rib.get(routeNetWork).get(0)._reason=Reason.ADD;
//  //    }else{
//  //      //主要就是进行比较,这部分在路由策略之后完成
//  //      Queue<SymBgpRoute> addQueue=new LinkedList<SymBgpRoute>();
//  //      addQueue.add(addRoute);
//  //      while(!addQueue.isEmpty())
//  //      {
//  //        SymBgpRoute route=addQueue.remove();
//  //        boolean hasIt=false;
//  //        for(SymBgpRoute temp:_rib.get(routeNetWork))
//  //        {
//  //          if(temp.equals(route))
//  //          {
//  //            hasIt=true;
//  //            break;
//  //          }
//  //        }
//  //        if(hasIt)
//  //        {
//  //          continue;
//  //        }
//  //        SymBounder addLocalPreference=route.getLocalPreference();
//  //        int lowerLocalPreference=addLocalPreference.getLowerbound();
//  //        int upperLocalPreference=addLocalPreference.getUpperbound();
//  //        int location=0;
//  //        boolean alreadyInject=false;
//  //        for(;location<_rib.get(routeNetWork).size();)
//  //        {
//  //          SymBgpRoute tempRoute=_rib.get(routeNetWork).get(location);
//  //          int tempUpperLocalPreference=tempRoute.getLocalPreference().getUpperbound();
//  //          int tempLowerLocalPreference=tempRoute.getLocalPreference().getLowerbound();
//  //          if(lowerLocalPreference>tempRoute.getLocalPreference().getUpperbound())
//  //          {
//  //            alreadyInject=true;
//  //            _rib.get(routeNetWork).add(location,route);
//  //            _rib.get(routeNetWork).get(location)._reason= Reason.ADD;
//  //            break;
//  //          }
//  //          if(upperLocalPreference<tempRoute.getLocalPreference().getLowerbound())
//  //          {
//  //            location++;
//  //            continue;
//  //          }
//  //          if(upperLocalPreference<=tempRoute.getLocalPreference().getUpperbound()&&
//  //              lowerLocalPreference>=tempRoute.getLocalPreference().getLowerbound())//对应的是被包含的一部分
//  //          {
//  //            if((lowerLocalPreference==upperLocalPreference&&tempLowerLocalPreference==tempUpperLocalPreference)||
//  //                (lowerLocalPreference==tempLowerLocalPreference&&upperLocalPreference==tempUpperLocalPreference))
//  //            {
//  //              //进入Aspath的判断
//  //              Answer an=processAsPath(location,route,routeNetWork,addQueue);
//  //              if(an.alreadyInject)
//  //              {
//  //                alreadyInject=true;
//  //                addQueue= an.queue;
//  //                break;
//  //              }else{
//  //                location=an.location;
//  //                addQueue=an.queue;
//  //                continue;
//  //              }
//  //            }else if(upperLocalPreference==tempUpperLocalPreference){
//  //              //需要进行后续的划分,主要是划分成两个路由
//  //              SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);//应该向路由表中新增加的路由
//  //              originalAddRoute.setLocalPreference(tempLowerLocalPreference,lowerLocalPreference-1);//
//  //              _rib.get(routeNetWork).get(location).setLocalPreference(lowerLocalPreference,tempUpperLocalPreference);
//  //              if(_rib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//  //              {
//  //                _rib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//  //              }
//  //              addQueue.add(originalAddRoute);// 重新添加/直接加
//  //              Answer an=processAsPath(location,route,routeNetWork,addQueue);//通过比较ASpath来判断到底加在哪里
//  //              if(an.alreadyInject)
//  //              {
//  //                alreadyInject=true;
//  //                addQueue= an.queue;
//  //                break;
//  //              }else{
//  //                location=an.location;
//  //                addQueue=an.queue;
//  //                continue;
//  //              }
//  //            }else if(lowerLocalPreference==tempLowerLocalPreference){
//  //              SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);
//  //              originalAddRoute.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//  //              _rib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//  //              if(_rib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//  //              {
//  //                _rib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//  //              }
//  //              addQueue.add(originalAddRoute);
//  //              Answer an=processAsPath(location,route,routeNetWork,addQueue);
//  //              if(an.alreadyInject)
//  //              {
//  //                alreadyInject=true;
//  //                addQueue= an.queue;
//  //                break;
//  //              }else{
//  //                location=an.location;
//  //                addQueue=an.queue;
//  //                continue;
//  //              }
//  //            }else{
//  //              SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//  //              SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//  //              originalAddRouteUpper.setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//  //              originalAddRouteLower.setLocalPreference(tempLowerLocalPreference,lowerLocalPreference-1);
//  //              _rib.get(routeNetWork).get(location).setLocalPreference(lowerLocalPreference,upperLocalPreference);
//  //              if(_rib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//  //              {
//  //                _rib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//  //              }
//  //              addQueue.add(originalAddRouteLower);
//  //              addQueue.add(originalAddRouteUpper);
//  //              Answer an=processAsPath(location,route,routeNetWork,addQueue);
//  //              if(an.alreadyInject)
//  //              {
//  //                alreadyInject=true;
//  //                addQueue= an.queue;
//  //                break;
//  //              }else{
//  //                location= an.location;
//  //                addQueue= an.queue;
//  //                continue;
//  //              }
//  //            }
//  //          }
//  //          if(upperLocalPreference>tempRoute.getLocalPreference().getUpperbound()&&
//  //              lowerLocalPreference<tempRoute.getLocalPreference().getLowerbound())//对应的是包含比较路由的范围的一部分,以为内前边已经处理了相等的情况,所以这里就不用处理了
//  //          {
//  //            SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//  //            SymBgpRoute addRouteLower=cloneBgpRoute(route);
//  //            addRouteUpper.setLocalPreference(tempUpperLocalPreference+1,upperLocalPreference);
//  //            addRouteLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//  //            route.setLocalPreference(tempLowerLocalPreference,tempUpperLocalPreference);
//  //            addQueue.add(addRouteUpper);
//  //            addQueue.add(addRouteLower);
//  //            Answer an=processAsPath(location,route,routeNetWork,addQueue);
//  //            if(an.alreadyInject)
//  //            {
//  //              alreadyInject=true;
//  //              addQueue= an.queue;
//  //              break;
//  //            }else{
//  //              location=an.location;
//  //              addQueue= an.queue;
//  //              continue;
//  //            }
//  //          }
//  //          if(upperLocalPreference<tempRoute.getLocalPreference().getUpperbound()&&
//  //              lowerLocalPreference<tempRoute.getLocalPreference().getLowerbound())
//  //          {
//  //            SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//  //            SymBgpRoute addRouterLower=cloneBgpRoute(route);
//  //            SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//  //            addRouterUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//  //            addRouterLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//  //            originalAddRouteLower.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//  //            _rib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//  //            if(_rib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//  //            {
//  //              _rib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//  //            }
//  //            addQueue.add(originalAddRouteLower);
//  //            addQueue.add(addRouterUpper);
//  //            addQueue.add(addRouterLower);
//  //            continue;
//  //          }
//  //          if(upperLocalPreference>tempRoute.getLocalPreference().getUpperbound()&&
//  //              lowerLocalPreference>tempRoute.getLocalPreference().getLowerbound())
//  //          {
//  //            SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//  //            SymBgpRoute addRouterLower=cloneBgpRoute(route);
//  //            SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//  //            addRouterUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//  //            addRouterLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//  //            originalAddRouteUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//  //            _rib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//  //            if(_rib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//  //            {
//  //              _rib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//  //            }
//  //            addQueue.add(originalAddRouteUpper);
//  //            addQueue.add(addRouterUpper);
//  //            addQueue.add(addRouterLower);
//  //            continue;
//  //          }
//  //        }
//  //        if (!alreadyInject)
//  //        {
//  //          _rib.get(routeNetWork).add(location,route);
//  //          _rib.get(routeNetWork).get(location)._reason=Reason.ADD;
//  //        }
//  //      }
//  //    }
//  //  }
//
//  public void AddRoute(SymBgpRoute addRoute,int a)
//  {
//    Integer routeNetWork=addRoute.getPrefixEcNum();
//    this._simplifiedChangePrefix.add(routeNetWork);
//    if(!this._simplifiedRib.containsKey(routeNetWork))
//    {
//      this._simplifiedRib.put(routeNetWork,new ArrayList<>());
//      this._simplifiedRib.get(routeNetWork).add(addRoute);
//      this._simplifiedRib.get(routeNetWork).get(0)._reason=Reason.ADD;
//    }else{
//      System.out.println("rib-Size"+this._simplifiedRib.get(routeNetWork).size());
//      //主要就是进行比较,这部分在路由策略之后完成
//      Queue<SymBgpRoute> addQueue=new LinkedList<SymBgpRoute>();
//      addQueue.add(addRoute);
//      while(!addQueue.isEmpty())
//      {
//        SymBgpRoute route=addQueue.remove();
//        boolean hasIt=false;
//        for(SymBgpRoute temp:_simplifiedRib.get(routeNetWork))
//        {
//          if(temp.equals(route))
//          {
//            hasIt=true;
//            break;
//          }
//        }
//        if(hasIt)
//        {
//          continue;
//        }
//        int location=0;
//        boolean alreadyInject=false;
//        for(;location<_simplifiedRib.get(routeNetWork).size();)
//        {
//          SymBgpRoute tempRoute=_simplifiedRib.get(routeNetWork).get(location);
//          if(route.getAdmin()>tempRoute.getAdmin())
//          {
//            location++;
//          }else if(route.getAdmin()<tempRoute.getAdmin())
//          {
//            alreadyInject=true;
//            _simplifiedRib.get(routeNetWork).add(location,route);
//            _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.ADD;
//          }else{
//            SymBounder addLocalPreference=route.getLocalPreference();
//            int lowerLocalPreference=addLocalPreference.getLowerbound();
//            int upperLocalPreference=addLocalPreference.getUpperbound();
//
//            int tempUpperLocalPreference=tempRoute.getLocalPreference().getUpperbound();
//            int tempLowerLocalPreference=tempRoute.getLocalPreference().getLowerbound();
//
//            //shou xian chu li de ying gai shi ISIS de tiao jian
//            if(route.getIsisMetric()>tempRoute.getIsisMetric())
//            {
//              location++;
//              continue;
//            }else if(route.getIsisMetric()<tempRoute.getIsisMetric())
//            {
//              alreadyInject=true;
//              _simplifiedRib.get(routeNetWork).add(location,route);
//              _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.ADD;
//              break;
//            }else{
//              //ISIS pan duan jie shu zhi hou cai hui kao lv BGP
//              if(lowerLocalPreference>tempRoute.getLocalPreference().getUpperbound())
//              {
//                alreadyInject=true;
//                _simplifiedRib.get(routeNetWork).add(location,route);
//                _simplifiedRib.get(routeNetWork).get(location)._reason= Reason.ADD;
//                break;
//              }
//              if(upperLocalPreference<tempRoute.getLocalPreference().getLowerbound())
//              {
//                location++;
//                continue;
//              }
//              if(upperLocalPreference<=tempRoute.getLocalPreference().getUpperbound()&&
//                  lowerLocalPreference>=tempRoute.getLocalPreference().getLowerbound())//对应的是被包含的一部分
//              {
//                if((lowerLocalPreference==upperLocalPreference&&tempLowerLocalPreference==tempUpperLocalPreference)||
//                    (lowerLocalPreference==tempLowerLocalPreference&&upperLocalPreference==tempUpperLocalPreference))
//                {
//                  //进入Aspath的判断
//                  SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//                  if(an.alreadyInject)
//                  {
//                    alreadyInject=true;
//                    addQueue= an.queue;
//                    break;
//                  }else{
//                    location=an.location;
//                    addQueue=an.queue;
//                    continue;
//                  }
//                }else if(upperLocalPreference==tempUpperLocalPreference){
//                  //需要进行后续的划分,主要是划分成两个路由
//                  SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);//应该向路由表中新增加的路由
//                  originalAddRoute.setLocalPreference(tempLowerLocalPreference,lowerLocalPreference-1);//
//                  _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(lowerLocalPreference,tempUpperLocalPreference);
//                  if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//                  {
//                    _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//                  }
//                  addQueue.add(originalAddRoute);// 重新添加/直接加
//                  SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);//通过比较ASpath来判断到底加在哪里
//                  if(an.alreadyInject)
//                  {
//                    alreadyInject=true;
//                    addQueue= an.queue;
//                    break;
//                  }else{
//                    location=an.location;
//                    addQueue=an.queue;
//                    continue;
//                  }
//                }else if(lowerLocalPreference==tempLowerLocalPreference){
//                  SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);
//                  originalAddRoute.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//                  _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//                  if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//                  {
//                    _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//                  }
//                  addQueue.add(originalAddRoute);
//                  SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//                  if(an.alreadyInject)
//                  {
//                    alreadyInject=true;
//                    addQueue= an.queue;
//                    break;
//                  }else{
//                    location=an.location;
//                    addQueue=an.queue;
//                    continue;
//                  }
//                }else{
//                  SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//                  SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//                  originalAddRouteUpper.setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//                  originalAddRouteLower.setLocalPreference(tempLowerLocalPreference,lowerLocalPreference-1);
//                  _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(lowerLocalPreference,upperLocalPreference);
//                  if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//                  {
//                    _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//                  }
//                  addQueue.add(originalAddRouteLower);
//                  addQueue.add(originalAddRouteUpper);
//                  SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//                  if(an.alreadyInject)
//                  {
//                    alreadyInject=true;
//                    addQueue= an.queue;
//                    break;
//                  }else{
//                    location= an.location;
//                    addQueue= an.queue;
//                    continue;
//                  }
//                }
//              }
//              if(upperLocalPreference>=tempRoute.getLocalPreference().getUpperbound()&&
//                  lowerLocalPreference<=tempRoute.getLocalPreference().getLowerbound())//对应的是包含比较路由的范围的一部分,以为内前边已经处理了相等的情况,所以这里就不用处理了
//              {
//                if(upperLocalPreference==tempRoute.getLocalPreference().getUpperbound())
//                {
//                  SymBgpRoute addRouteLower=cloneBgpRoute(route);
//                  addRouteLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//                  route.setLocalPreference(tempLowerLocalPreference,tempUpperLocalPreference);
//                  addQueue.add(addRouteLower);
//                  SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//                  if(an.alreadyInject)
//                  {
//                    alreadyInject=true;
//                    addQueue= an.queue;
//                    break;
//                  }else{
//                    location=an.location;
//                    addQueue= an.queue;
//                    continue;
//                  }
//                }else if(lowerLocalPreference==tempRoute.getLocalPreference().getLowerbound()){
//                  SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//                  addRouteUpper.setLocalPreference(tempUpperLocalPreference+1,upperLocalPreference);
//                  route.setLocalPreference(tempLowerLocalPreference,tempUpperLocalPreference);
//                  addQueue.add(addRouteUpper);
//                  SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//                  if(an.alreadyInject)
//                  {
//                    alreadyInject=true;
//                    addQueue= an.queue;
//                    break;
//                  }else{
//                    location=an.location;
//                    addQueue= an.queue;
//                    continue;
//                  }
//                }else{
//                  SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//                  SymBgpRoute addRouteLower=cloneBgpRoute(route);
//                  addRouteUpper.setLocalPreference(tempUpperLocalPreference+1,upperLocalPreference);
//                  addRouteLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//                  route.setLocalPreference(tempLowerLocalPreference,tempUpperLocalPreference);
//                  addQueue.add(addRouteUpper);
//                  addQueue.add(addRouteLower);
//                  SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//                  if(an.alreadyInject)
//                  {
//                    alreadyInject=true;
//                    addQueue= an.queue;
//                    break;
//                  }else{
//                    location=an.location;
//                    addQueue= an.queue;
//                    continue;
//                  }
//                }
//
//              }
//              if(upperLocalPreference<tempRoute.getLocalPreference().getUpperbound()&&
//                  lowerLocalPreference<tempRoute.getLocalPreference().getLowerbound())
//              {
//                SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//                SymBgpRoute addRouterLower=cloneBgpRoute(route);
//                SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//                addRouterUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//                addRouterLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//                originalAddRouteLower.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//                _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//                if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//                {
//                  _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//                }
//                addQueue.add(originalAddRouteLower);
//                addQueue.add(addRouterUpper);
//                addQueue.add(addRouterLower);
//                continue;
//              }
//              if(upperLocalPreference>tempRoute.getLocalPreference().getUpperbound()&&
//                  lowerLocalPreference>tempRoute.getLocalPreference().getLowerbound())
//              {
//                SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//                SymBgpRoute addRouterLower=cloneBgpRoute(route);
//                SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//                addRouterUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//                addRouterLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//                originalAddRouteUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//                _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//                if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//                {
//                  _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//                }
//                addQueue.add(originalAddRouteUpper);
//                addQueue.add(addRouterUpper);
//                addQueue.add(addRouterLower);
//                continue;
//              }
//            }
//          }
//          //yin wei mei ci route dou hui geng gai suo yi fu zhi ying gai shi zai zhe li
//
//        }
//        if (!alreadyInject)
//        {
//          _simplifiedRib.get(routeNetWork).add(location,route);
//          _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.ADD;
//        }
//      }
//    }
//  }
//
//  //prefixEcNum you hua
//  public void AddRoute(SymBgpRoute addRoute)
//  {
//    Integer routeNetWork=addRoute.getPrefixEcNum();
//    this._simplifiedChangePrefix.add(routeNetWork);
//    if(!this._simplifiedRib.containsKey(routeNetWork))
//    {
//      this._simplifiedRib.put(routeNetWork,new ArrayList<>());
//      this._simplifiedRib.get(routeNetWork).add(addRoute);
//      this._simplifiedRib.get(routeNetWork).get(0)._reason=Reason.ADD;
//    }else{
//      //主要就是进行比较,这部分在路由策略之后完成
//      Queue<SymBgpRoute> addQueue=new LinkedList<SymBgpRoute>();
//      addQueue.add(addRoute);
//      while(!addQueue.isEmpty())
//      {
//        SymBgpRoute route=addQueue.remove();
//        boolean hasIt=false;
//        //        for(SymBgpRoute temp:_simplifiedRib.get(routeNetWork))
//        //        {
//        //          if(temp.equals(route))
//        //          {
//        //            hasIt=true;
//        //            break;
//        //          }
//        //        }
//        //        if(hasIt)
//        //        {
//        //          continue;
//        //        }
//        int location=0;
//        boolean alreadyInject=false;
//        for(;location<_simplifiedRib.get(routeNetWork).size();)
//        {
//          //yin wei mei ci route dou hui geng gai suo yi fu zhi ying gai shi zai zhe li
//          SymBounder addLocalPreference=route.getLocalPreference();
//          int lowerLocalPreference=addLocalPreference.getLowerbound();
//          int upperLocalPreference=addLocalPreference.getUpperbound();
//
//          SymBgpRoute tempRoute=_simplifiedRib.get(routeNetWork).get(location);
//          int tempUpperLocalPreference=tempRoute.getLocalPreference().getUpperbound();
//          int tempLowerLocalPreference=tempRoute.getLocalPreference().getLowerbound();
//
//          //shou xian chu li de ying gai shi ISIS de tiao jian
//          //ISIS pan duan jie shu zhi hou cai hui kao lv BGP
//          if(lowerLocalPreference>tempRoute.getLocalPreference().getUpperbound())
//          {
//            alreadyInject=true;
//            _simplifiedRib.get(routeNetWork).add(location,route);
//            _simplifiedRib.get(routeNetWork).get(location)._reason= Reason.ADD;
//            break;
//          }
//          if(upperLocalPreference<tempRoute.getLocalPreference().getLowerbound())
//          {
//            location++;
//            continue;
//          }
//          if(upperLocalPreference<=tempRoute.getLocalPreference().getUpperbound()&&
//              lowerLocalPreference>=tempRoute.getLocalPreference().getLowerbound())//对应的是被包含的一部分
//          {
//            if((lowerLocalPreference==upperLocalPreference&&tempLowerLocalPreference==tempUpperLocalPreference)||
//                (lowerLocalPreference==tempLowerLocalPreference&&upperLocalPreference==tempUpperLocalPreference))
//            {
//              //进入Aspath的判断
//              SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//              if(an.alreadyInject)
//              {
//                alreadyInject=true;
//                addQueue= an.queue;
//                break;
//              }else{
//                location=an.location;
//                addQueue=an.queue;
//                continue;
//              }
//            }else if(upperLocalPreference==tempUpperLocalPreference){
//              //需要进行后续的划分,主要是划分成两个路由
//              SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);//应该向路由表中新增加的路由
//              originalAddRoute.setLocalPreference(tempLowerLocalPreference,lowerLocalPreference-1);//
//              _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(lowerLocalPreference,tempUpperLocalPreference);
//              if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//              {
//                _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//              }
//              addQueue.add(originalAddRoute);// 重新添加/直接加
//              SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);//通过比较ASpath来判断到底加在哪里
//              if(an.alreadyInject)
//              {
//                alreadyInject=true;
//                addQueue= an.queue;
//                break;
//              }else{
//                location=an.location;
//                addQueue=an.queue;
//                continue;
//              }
//            }else if(lowerLocalPreference==tempLowerLocalPreference){
//              SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);
//              originalAddRoute.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//              _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//              if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//              {
//                _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//              }
//              addQueue.add(originalAddRoute);
//              SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//              if(an.alreadyInject)
//              {
//                alreadyInject=true;
//                addQueue= an.queue;
//                break;
//              }else{
//                location=an.location;
//                addQueue=an.queue;
//                continue;
//              }
//            }else{
//              SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//              SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//              originalAddRouteUpper.setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//              originalAddRouteLower.setLocalPreference(tempLowerLocalPreference,lowerLocalPreference-1);
//              _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(lowerLocalPreference,upperLocalPreference);
//              if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//              {
//                _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//              }
//              addQueue.add(originalAddRouteLower);
//              addQueue.add(originalAddRouteUpper);
//              SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//              if(an.alreadyInject)
//              {
//                alreadyInject=true;
//                addQueue= an.queue;
//                break;
//              }else{
//                location= an.location;
//                addQueue= an.queue;
//                continue;
//              }
//            }
//          }
//          if(upperLocalPreference>=tempRoute.getLocalPreference().getUpperbound()&&
//              lowerLocalPreference<=tempRoute.getLocalPreference().getLowerbound())//对应的是包含比较路由的范围的一部分,以为内前边已经处理了相等的情况,所以这里就不用处理了
//          {
//            if(upperLocalPreference==tempRoute.getLocalPreference().getUpperbound())
//            {
//              SymBgpRoute addRouteLower=cloneBgpRoute(route);
//              addRouteLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//              route.setLocalPreference(tempLowerLocalPreference,tempUpperLocalPreference);
//              addQueue.add(addRouteLower);
//              SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//              if(an.alreadyInject)
//              {
//                alreadyInject=true;
//                addQueue= an.queue;
//                break;
//              }else{
//                location=an.location;
//                addQueue= an.queue;
//                continue;
//              }
//            }else if(lowerLocalPreference==tempRoute.getLocalPreference().getLowerbound()){
//              SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//              addRouteUpper.setLocalPreference(tempUpperLocalPreference+1,upperLocalPreference);
//              route.setLocalPreference(tempLowerLocalPreference,tempUpperLocalPreference);
//              addQueue.add(addRouteUpper);
//              SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//              if(an.alreadyInject)
//              {
//                alreadyInject=true;
//                addQueue= an.queue;
//                break;
//              }else{
//                location=an.location;
//                addQueue= an.queue;
//                continue;
//              }
//            }else{
//              SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//              SymBgpRoute addRouteLower=cloneBgpRoute(route);
//              addRouteUpper.setLocalPreference(tempUpperLocalPreference+1,upperLocalPreference);
//              addRouteLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//              route.setLocalPreference(tempLowerLocalPreference,tempUpperLocalPreference);
//              addQueue.add(addRouteUpper);
//              addQueue.add(addRouteLower);
//              SymBgpRib.Answer an=processAsPath(location,route,routeNetWork,addQueue);
//              if(an.alreadyInject)
//              {
//                alreadyInject=true;
//                addQueue= an.queue;
//                break;
//              }else{
//                location=an.location;
//                addQueue= an.queue;
//                continue;
//              }
//            }
//
//          }
//          if(upperLocalPreference<tempRoute.getLocalPreference().getUpperbound()&&
//              lowerLocalPreference<tempRoute.getLocalPreference().getLowerbound())
//          {
//            SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//            SymBgpRoute addRouterLower=cloneBgpRoute(route);
//            SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//            addRouterUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//            addRouterLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//            originalAddRouteLower.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//            _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//            if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//            {
//              _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//            }
//            addQueue.add(originalAddRouteLower);
//            addQueue.add(addRouterUpper);
//            addQueue.add(addRouterLower);
//            continue;
//          }
//          if(upperLocalPreference>tempRoute.getLocalPreference().getUpperbound()&&
//              lowerLocalPreference>tempRoute.getLocalPreference().getLowerbound())
//          {
//            SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//            SymBgpRoute addRouterLower=cloneBgpRoute(route);
//            SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//            addRouterUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//            addRouterLower.setLocalPreference(lowerLocalPreference,tempLowerLocalPreference-1);
//            originalAddRouteUpper.setLocalPreference(tempLowerLocalPreference,upperLocalPreference);
//            _simplifiedRib.get(routeNetWork).get(location).setLocalPreference(upperLocalPreference+1,tempUpperLocalPreference);
//            if(_simplifiedRib.get(routeNetWork).get(location)._reason!=Reason.ADD)
//            {
//              _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.UPDATE;
//            }
//            addQueue.add(originalAddRouteUpper);
//            addQueue.add(addRouterUpper);
//            addQueue.add(addRouterLower);
//            continue;
//          }
//        }
//        if (!alreadyInject)
//        {
//          _simplifiedRib.get(routeNetWork).add(location,route);
//          _simplifiedRib.get(routeNetWork).get(location)._reason=Reason.ADD;
//        }
//      }
//    }
//  }
//  public class Answer{
//    public int location;
//    public boolean alreadyInject;
//    public Queue<SymBgpRoute> queue;
//    public Answer()
//    {
//      location=-1;
//      alreadyInject=false;
//    }
//  }
//
//  public SymBgpRib.Answer processAsPath(int location,SymBgpRoute route,Integer routeNetwork,Queue<SymBgpRoute> addQueue)
//  {
//    SymBgpRib.Answer answer=new SymBgpRib.Answer();
//    SymBgpRoute tempRoute=_simplifiedRib.get(routeNetwork).get(location);
//    SymBounder OriginalAsPathSize=_simplifiedRib.get(routeNetwork).get(location).getAsPathSize();
//    int originalLowerAsPathSize=OriginalAsPathSize.getLowerbound();
//    int orihinalUpperAsPathSize=OriginalAsPathSize.getUpperbound();
//    int addUpperAsPathSize=route.getAsPathSize().getUpperbound();
//    int addLowerAsPathSize=route.getAsPathSize().getLowerbound();
//    int addConfirmAsPathSize=route.getAsPath().size();
//    int originalConfirmAsPAthSize=_simplifiedRib.get(routeNetwork).get(location).getAsPath().size();
//    int addFinalLower=addLowerAsPathSize+addConfirmAsPathSize;
//    int addFinalUpper=addUpperAsPathSize==Integer.MAX_VALUE?addUpperAsPathSize:addUpperAsPathSize+addConfirmAsPathSize;//这一部分未来可能会有溢出范围的错误.
//    int originalFinalLower=originalLowerAsPathSize+originalConfirmAsPAthSize;
//    int originalFinalUpper=originalConfirmAsPAthSize==Integer.MAX_VALUE?orihinalUpperAsPathSize:orihinalUpperAsPathSize+originalConfirmAsPAthSize;
//    if(addFinalUpper<originalFinalLower)
//    {
//      _simplifiedRib.get(routeNetwork).add(location,route);
//      _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//      answer.alreadyInject=true;
//      answer.queue=addQueue;
//      return answer;
//    }
//    if(addFinalLower>originalFinalUpper)
//    {
//      answer.alreadyInject=false;
//      answer.location=location+1;
//      answer.queue=addQueue;
//      return answer;
//    }
//    if(addFinalUpper<=originalFinalUpper&&
//        addFinalLower>=originalFinalLower)//对应的是被包含的一部分
//    {
//      if((addFinalUpper==addFinalLower&&originalFinalUpper==originalFinalLower)||
//          (addFinalLower==originalFinalLower&&addFinalUpper==originalFinalUpper))
//      {
//        answer=processIsis(location,route,routeNetwork,addQueue);
//        //        _simplifiedRib.get(routeNetwork).add(location,route);
//        //        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//        //        answer.alreadyInject=true;
//        //        answer.queue=addQueue;
//        return answer;
//        //进入Aspath的判断
//      }else if(addFinalUpper==originalFinalUpper){
//        //需要进行后续的划分,主要是划分成两个路由
//        SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);//应该向路由表中新增加的路由
//        originalAddRoute.setAsPathSize(addFinalLower-originalConfirmAsPAthSize,orihinalUpperAsPathSize);//x+originalConfirmAsPAthSize=addFinalLower
//        _simplifiedRib.get(routeNetwork).get(location).setAsPathSize(originalLowerAsPathSize,addFinalLower-originalConfirmAsPAthSize-1);
//        if(_simplifiedRib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//        {
//          _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//        }
//        addQueue.add(originalAddRoute);
//        addQueue.add(route);
//        answer.alreadyInject=false;
//        answer.location=location+1;
//        answer.queue=addQueue;
//        return answer;
//      }else if(addFinalLower==originalFinalLower){
//        SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);
//        originalAddRoute.setAsPathSize(addFinalUpper-originalConfirmAsPAthSize+1,orihinalUpperAsPathSize);
//        _simplifiedRib.get(routeNetwork).get(location).setAsPathSize(originalLowerAsPathSize,addFinalUpper-originalConfirmAsPAthSize);//x+originalConfirmAsPAthSize=addFinalUpper
//        if(_simplifiedRib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//        {
//          _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//        }
//        //        _simplifiedRib.get(routeNetwork).add(location,route);
//        //        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//        addQueue.add(originalAddRoute);
//        //Process External
//        processIsis(location,route,routeNetwork,addQueue);
//        //        answer.alreadyInject=true;
//        //        answer.queue=addQueue;
//        return answer;
//      }else{
//        SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//        SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//        originalAddRouteUpper.setAsPathSize(addFinalUpper-originalConfirmAsPAthSize+1,orihinalUpperAsPathSize);//x+originalConfirmAsPathSize=addFinalUpper
//        originalAddRouteLower.setLocalPreference(originalLowerAsPathSize,addFinalLower-originalConfirmAsPAthSize-1);
//        _simplifiedRib.get(routeNetwork).get(location).setAsPathSize(addFinalLower-originalConfirmAsPAthSize,addFinalUpper-originalConfirmAsPAthSize);//x+originalConfirmAsPathSize=addFinalUpper x+originalConfirmAsPathSize=addFinalLower
//        if(_simplifiedRib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//        {
//          _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//        }
//        addQueue.add(originalAddRouteLower);
//        addQueue.add(originalAddRouteUpper);
//        //Process External
//        processIsis(location,route,routeNetwork,addQueue);
//        //        _simplifiedRib.get(routeNetwork).add(location,route);
//        //        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//        //        answer.alreadyInject=true;
//        //        answer.queue=addQueue;
//        return answer;
//      }
//    }
//    if(addFinalLower<=originalFinalLower&&
//        addFinalUpper>=originalFinalUpper)//对应的是包含比较路由的范围的一部分,以为内前边已经处理了相等的情况,所以这里就不用处理了
//    {
//      if(addFinalUpper==originalFinalUpper){
//        //需要进行后续的划分,主要是划分成两个路由
//        SymBgpRoute addRouteLower=cloneBgpRoute(route);
//        route.setAsPathSize(originalFinalLower-addConfirmAsPathSize+1,addUpperAsPathSize);//x+addConfirmSize=originalFinalLower
//        addRouteLower.setAsPathSize(addLowerAsPathSize,originalFinalLower-addConfirmAsPathSize);
//        //        _simplifiedRib.get(routeNetwork).add(location,route);
//        //        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//        _simplifiedRib.get(routeNetwork).add(location,addRouteLower);
//        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//
//        //Process Extrernal
//        answer=processIsis(location+1,route,routeNetwork,addQueue);
//        //        answer.alreadyInject=true;
//        //        answer.queue=addQueue;
//        //answer.location=location+1;
//        return answer;
//      }else if(addFinalLower==originalFinalLower){
//        SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//        addRouteUpper.setAsPathSize(originalFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);//x+addConfirmSize=originalFinalUpper
//        addQueue.add(addRouteUpper);
//        route.setAsPathSize(addLowerAsPathSize,originalFinalUpper-addConfirmAsPathSize);
//        //chu li As-Path xiang deng de qing kuang
//        answer=processIsis(location,route,routeNetwork,addQueue);
//        return answer;
//      }else{
//        SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//        SymBgpRoute addRouteLower=cloneBgpRoute(route);
//        addRouteLower.setAsPathSize(addLowerAsPathSize,originalFinalLower-addConfirmAsPathSize-1);//x+addConfirmSize=originalFinalLower x+addConfirmSize=originalFinalUpper
//        addRouteUpper.setAsPathSize(originalFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);
//        route.setAsPathSize(originalFinalLower-addConfirmAsPathSize,originalFinalUpper-addConfirmAsPathSize);
//        //        _simplifiedRib.get(routeNetwork).add(location,route);
//        //        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//        _simplifiedRib.get(routeNetwork).add(location,addRouteLower);
//        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//        addQueue.add(addRouteUpper);
//        //Process Extrernal
//        answer=processIsis(location+1,route,routeNetwork,addQueue);
//        return answer;
//      }
//    }
//    if(addFinalUpper<originalFinalUpper&&
//        addFinalLower<originalFinalLower )
//    {
//      SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//      addRouterUpper.setAsPathSize(originalFinalLower-addConfirmAsPathSize,addUpperAsPathSize);//x+addConfrimSize=originalFinalLower; y+originalConfirmSize=addFinalUpper
//      route.setAsPathSize(addLowerAsPathSize,originalFinalLower-addConfirmAsPathSize-1);
//      SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//      originalAddRouteUpper.setAsPathSize(addFinalUpper-originalConfirmAsPAthSize+1,orihinalUpperAsPathSize);
//      _simplifiedRib.get(routeNetwork).get(location).setAsPathSize(originalLowerAsPathSize,addFinalUpper-originalConfirmAsPAthSize);
//      if(_simplifiedRib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//      {
//        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//      }
//      //      _simplifiedRib.get(routeNetwork).add(location,addRouterUpper);
//      //      _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//      addQueue.add(addRouterUpper);
//      _simplifiedRib.get(routeNetwork).add(location,route);
//      _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//      addQueue.add(originalAddRouteUpper);
//      answer.alreadyInject=true;
//      answer.queue=addQueue;
//      return answer;
//    }
//    if(addFinalUpper>originalFinalUpper&&
//        addFinalLower>originalFinalLower)
//    {
//      SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//      addRouterUpper.setAsPathSize(originalFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);//x+addConfirmSize=originalFinalUpper y+originalConfirmSize=addFinalLower
//      //      SymBgpRoute addRouterLower=cloneBgpRoute(route);
//      //      addRouterLower.setAsPathSize(addLowerAsPathSize,originalFinalUpper-addConfirmAsPathSize);
//      route.setAsPathSize(addLowerAsPathSize,originalFinalUpper-addConfirmAsPathSize);
//      SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//      originalAddRouteLower.setAsPathSize(originalLowerAsPathSize,addFinalLower-originalConfirmAsPAthSize-1);
//      _simplifiedRib.get(routeNetwork).get(location).setAsPathSize(addFinalLower-originalConfirmAsPAthSize,orihinalUpperAsPathSize);
//      if(_simplifiedRib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//      {
//        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//      }
//      //      _simplifiedRib.get(routeNetwork).add(location,addRouterLower);
//      //      _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//      _simplifiedRib.get(routeNetwork).add(location,originalAddRouteLower);
//      _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//      addQueue.add(addRouterUpper);
//      //Process Extrernal
//      answer=processIsis(location+1,route,routeNetwork,addQueue);
//      //      answer.alreadyInject=true;
//      //      answer.queue=addQueue;
//      return answer;
//    }
//    return answer;
//  }
//
//  public SymBgpRib.Answer processExternal(int location,SymBgpRoute route,Integer routeNetwork,Queue<SymBgpRoute> addQueue)
//  {
//    SymBgpRib.Answer answer=new SymBgpRib.Answer();
//    if(_simplifiedRib.get(routeNetwork).get(location).getExternal().equals(route.getExternal()))
//    {
//      _simplifiedRib.get(routeNetwork).add(location,route);
//      _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//      answer.alreadyInject=true;
//      answer.queue=addQueue;
//      return answer;
//    }else if(_simplifiedRib.get(routeNetwork).get(location).getExternal()){
//      _simplifiedRib.get(routeNetwork).add(location,route);
//      _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//      answer.alreadyInject=true;
//      answer.queue=addQueue;
//      return answer;
//    }else if(!_simplifiedRib.get(routeNetwork).get(location).getExternal()){
//      answer.alreadyInject=false;
//      answer.location=location+1;
//      answer.queue=addQueue;
//      return answer;
//    }
//    return answer;
//  }
//
//  public SymBgpRib.Answer processIsis(int location,SymBgpRoute route,Integer routeNetwork,Queue<SymBgpRoute> addQueue)
//  {
//    SymBgpRib.Answer answer=new SymBgpRib.Answer();
//    if(_multiPtahEgp)
//    {
//      if(_simplifiedRib.get(routeNetwork).get(location).getIsisMetric()<=route.getIsisMetric())
//      {
//        answer.alreadyInject=false;
//        answer.location=location+1;
//        answer.queue=addQueue;
//        return answer;
//      }else{
//        _simplifiedRib.get(routeNetwork).add(location,route);
//        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//        answer.alreadyInject=true;
//        answer.queue=addQueue;
//        return answer;
//      }
//    }else
//    {
//      if(_simplifiedRib.get(routeNetwork).get(location).getIsisMetric()<route.getIsisMetric())
//      {
//        answer.alreadyInject=false;
//        answer.location=location+1;
//        answer.queue=addQueue;
//        return answer;
//      }else if(_simplifiedRib.get(routeNetwork).get(location).getIsisMetric()>route.getIsisMetric()){
//        _simplifiedRib.get(routeNetwork).add(location,route);
//        _simplifiedRib.get(routeNetwork).get(location)._reason=Reason.ADD;
//        answer.alreadyInject=true;
//        answer.queue=addQueue;
//        return answer;
//      }else{
//        answer.alreadyInject=true;
//        answer.queue=addQueue;
//        return answer;
//      }
//    }
//  }
//
//  //  public Answer processAsPath(int location,SymBgpRoute route,SymPrefixList routeNetwork,Queue<SymBgpRoute> addQueue)
//  //  {
//  //    Answer answer=new Answer();
//  //    SymBgpRoute tempRoute=_rib.get(routeNetwork).get(location);
//  //    SymBounder OriginalAsPathSize=_rib.get(routeNetwork).get(location).getAsPathSize();
//  //    int originalLowerAsPathSize=OriginalAsPathSize.getLowerbound();
//  //    int orihinalUpperAsPathSize=OriginalAsPathSize.getUpperbound();
//  //    int addUpperAsPathSize=route.getAsPathSize().getUpperbound();
//  //    int addLowerAsPathSize=route.getAsPathSize().getLowerbound();
//  //    int addConfirmAsPathSize=route.getAsPath().size();
//  //    int originalConfirmAsPAthSize=_rib.get(routeNetwork).get(location).getAsPath().size();
//  //    int addFinalLower=addLowerAsPathSize+addConfirmAsPathSize;
//  //    int addFinalUpper=addUpperAsPathSize==Integer.MAX_VALUE?addUpperAsPathSize:addUpperAsPathSize+addConfirmAsPathSize;//这一部分未来可能会有溢出范围的错误.
//  //    int originalFinalLower=originalLowerAsPathSize+originalConfirmAsPAthSize;
//  //    int originalFinalUpper=originalConfirmAsPAthSize==Integer.MAX_VALUE?orihinalUpperAsPathSize:orihinalUpperAsPathSize+originalConfirmAsPAthSize;
//  //    if(addFinalUpper<originalFinalLower)
//  //    {
//  //      _rib.get(routeNetwork).add(location,route);
//  //      _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //      answer.alreadyInject=true;
//  //      answer.queue=addQueue;
//  //      return answer;
//  //    }
//  //    if(addFinalLower>originalFinalUpper)
//  //    {
//  //      answer.alreadyInject=false;
//  //      answer.location=location+1;
//  //      answer.queue=addQueue;
//  //      return answer;
//  //    }
//  //    if(addFinalUpper<=originalFinalUpper&&
//  //        addFinalLower>=originalFinalLower)//对应的是被包含的一部分
//  //    {
//  //      if((addFinalUpper==addFinalLower&&originalFinalUpper==originalFinalLower)||
//  //          (addFinalLower==originalFinalLower&&addFinalUpper==originalFinalUpper))
//  //      {
//  //        _rib.get(routeNetwork).add(location,route);
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //        answer.alreadyInject=true;
//  //        answer.queue=addQueue;
//  //        return answer;
//  //        //进入Aspath的判断
//  //      }else if(addFinalUpper==originalFinalUpper){
//  //        //需要进行后续的划分,主要是划分成两个路由
//  //        SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);//应该向路由表中新增加的路由
//  //        originalAddRoute.setAsPathSize(addFinalLower-originalConfirmAsPAthSize,orihinalUpperAsPathSize);//x+originalConfirmAsPAthSize=addFinalLower
//  //        _rib.get(routeNetwork).get(location).setAsPathSize(originalLowerAsPathSize,addFinalLower-originalConfirmAsPAthSize-1);
//  //        if(_rib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//  //        {
//  //          _rib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//  //        }
//  //        addQueue.add(originalAddRoute);
//  //        addQueue.add(route);
//  //        answer.alreadyInject=false;
//  //        answer.location=location+1;
//  //        answer.queue=addQueue;
//  //        return answer;
//  //      }else if(addFinalLower==originalFinalLower){
//  //        SymBgpRoute originalAddRoute=cloneBgpRoute(tempRoute);
//  //        originalAddRoute.setAsPathSize(addFinalUpper-originalConfirmAsPAthSize+1,orihinalUpperAsPathSize);
//  //        _rib.get(routeNetwork).get(location).setAsPathSize(originalLowerAsPathSize,addFinalUpper-originalConfirmAsPAthSize);//x+originalConfirmAsPAthSize=addFinalUpper
//  //        if(_rib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//  //        {
//  //          _rib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//  //        }
//  //        _rib.get(routeNetwork).add(location,route);
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //        addQueue.add(originalAddRoute);
//  //        answer.alreadyInject=true;
//  //        answer.queue=addQueue;
//  //        return answer;
//  //      }else{
//  //        SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//  //        SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//  //        originalAddRouteUpper.setAsPathSize(addFinalUpper-originalConfirmAsPAthSize+1,orihinalUpperAsPathSize);//x+originalConfirmAsPathSize=addFinalUpper
//  //        originalAddRouteLower.setLocalPreference(originalLowerAsPathSize,addFinalLower-originalConfirmAsPAthSize-1);
//  //        _rib.get(routeNetwork).get(location).setAsPathSize(addFinalLower-originalConfirmAsPAthSize,addFinalUpper-originalConfirmAsPAthSize);//x+originalConfirmAsPathSize=addFinalUpper x+originalConfirmAsPathSize=addFinalLower
//  //        if(_rib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//  //        {
//  //          _rib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//  //        }
//  //        addQueue.add(originalAddRouteLower);
//  //        addQueue.add(originalAddRouteUpper);
//  //        _rib.get(routeNetwork).add(location,route);
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //        answer.alreadyInject=true;
//  //        answer.queue=addQueue;
//  //        return answer;
//  //      }
//  //    }
//  //    if(addFinalLower<=originalFinalLower&&
//  //        addFinalUpper>=originalFinalUpper)//对应的是包含比较路由的范围的一部分,以为内前边已经处理了相等的情况,所以这里就不用处理了
//  //    {
//  //      if(addFinalUpper==originalFinalUpper){
//  //        //需要进行后续的划分,主要是划分成两个路由
//  //        SymBgpRoute addRouteLower=cloneBgpRoute(route);
//  //        route.setAsPathSize(originalFinalLower-addConfirmAsPathSize+1,addUpperAsPathSize);//x+addConfirmSize=originalFinalLower
//  //        addRouteLower.setAsPathSize(addLowerAsPathSize,originalFinalLower-addConfirmAsPathSize);
//  //        _rib.get(routeNetwork).add(location,route);
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //        _rib.get(routeNetwork).add(location,addRouteLower);
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //        answer.alreadyInject=true;
//  //        answer.queue=addQueue;
//  //        //answer.location=location+1;
//  //        return answer;
//  //      }else if(addFinalLower==originalFinalLower){
//  //        SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//  //        addRouteUpper.setAsPathSize(originalFinalUpper-addConfirmAsPathSize+1,addLowerAsPathSize);//x+addConfirmSize=originalFinalUpper
//  //        addQueue.add(addRouteUpper);
//  //        route.setAsPathSize(addLowerAsPathSize,originalFinalUpper-addConfirmAsPathSize);
//  //        _rib.get(routeNetwork).add(location,route);
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //        answer.alreadyInject=true;
//  //        answer.queue=addQueue;
//  //        return answer;
//  //      }else{
//  //        SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//  //        SymBgpRoute addRouteLower=cloneBgpRoute(route);
//  //        addRouteLower.setAsPathSize(addLowerAsPathSize,originalFinalLower-addConfirmAsPathSize-1);//x+addConfirmSize=originalFinalLower x+addConfirmSize=originalFinalUpper
//  //        addRouteUpper.setAsPathSize(originalFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);
//  //        route.setAsPathSize(originalFinalLower-addConfirmAsPathSize,originalFinalUpper-addConfirmAsPathSize);
//  //        _rib.get(routeNetwork).add(location,route);
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //        _rib.get(routeNetwork).add(location,addRouteLower);
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //        addQueue.add(addRouteUpper);
//  //        answer.alreadyInject=true;
//  //        answer.queue=addQueue;
//  //        return answer;
//  //      }
//  //    }
//  //    if(addFinalUpper<originalFinalUpper&&
//  //        addFinalLower<originalFinalLower )
//  //    {
//  //      SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//  //      addRouterUpper.setAsPathSize(originalFinalLower-addConfirmAsPathSize,addUpperAsPathSize);//x+addConfrimSize=originalFinalLower; y+originalConfirmSize=addFinalUpper
//  //      route.setAsPathSize(addLowerAsPathSize,originalFinalLower-addConfirmAsPathSize-1);
//  //      SymBgpRoute originalAddRouteUpper=cloneBgpRoute(tempRoute);
//  //      originalAddRouteUpper.setAsPathSize(addFinalUpper-originalConfirmAsPAthSize+1,orihinalUpperAsPathSize);
//  //      _rib.get(routeNetwork).get(location).setAsPathSize(originalLowerAsPathSize,addFinalUpper-originalConfirmAsPAthSize);
//  //      if(_rib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//  //      {
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//  //      }
//  //      _rib.get(routeNetwork).add(location,addRouterUpper);
//  //      _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //      _rib.get(routeNetwork).add(location,route);
//  //      _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //      addQueue.add(originalAddRouteUpper);
//  //      answer.alreadyInject=true;
//  //      answer.queue=addQueue;
//  //      return answer;
//  //    }
//  //    if(addFinalUpper>originalFinalUpper&&
//  //        addFinalLower>originalFinalLower)
//  //    {
//  //      SymBgpRoute addRouterUpper=cloneBgpRoute(route);
//  //      addRouterUpper.setAsPathSize(originalFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);//x+addConfirmSize=originalFinalUpper y+originalConfirmSize=addFinalLower
//  //      SymBgpRoute addRouterLower=cloneBgpRoute(route);
//  //      addRouterLower.setAsPathSize(addLowerAsPathSize,originalFinalUpper-addConfirmAsPathSize);
//  //      SymBgpRoute originalAddRouteLower=cloneBgpRoute(tempRoute);
//  //      originalAddRouteLower.setAsPathSize(originalLowerAsPathSize,addFinalLower-originalConfirmAsPAthSize-1);
//  //      _rib.get(routeNetwork).get(location).setAsPathSize(addFinalLower-originalConfirmAsPAthSize,orihinalUpperAsPathSize);
//  //      if(_rib.get(routeNetwork).get(location)._reason!=Reason.ADD)
//  //      {
//  //        _rib.get(routeNetwork).get(location)._reason=Reason.UPDATE;
//  //      }
//  //      _rib.get(routeNetwork).add(location,addRouterLower);
//  //      _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //      _rib.get(routeNetwork).add(location,originalAddRouteLower);
//  //      _rib.get(routeNetwork).get(location)._reason=Reason.ADD;
//  //      addQueue.add(addRouterUpper);
//  //      answer.alreadyInject=true;
//  //      answer.queue=addQueue;
//  //      return answer;
//  //    }
//  //    return answer;
//  //  }
//  public Map<SymPrefixList, List<SymBgpRoute>> getRoutes()
//  {
//    return this._rib;
//  }
//  public Map<Integer,List<SymBgpRoute>> getSimplifiedRoutes()
//  {
//    return this._simplifiedRib;
//  }
//  public SymBgpRoute cloneBgpRoute(SymBgpRoute route)
//  {
//    SymBgpRoute answerRoute=new SymBgpRoute(route._prefixEcNum,route.getTopologyFactory(),route.getPrefixFactory());
//    answerRoute.setAsPath(route.getAsPath());
//    SymBounder bounder= route.getAsPathSize();
//    answerRoute.setAsPathSize(route.getAsPathSize().getLowerbound(),route.getAsPathSize().getUpperbound());
//    answerRoute.setTopologyCondition(route.getTopologyCondition());
//    answerRoute.setLocalPreference(route.getLocalPreference().getLowerbound(),route.getLocalPreference().getUpperbound());
//    answerRoute.setNextHopIp(route.getNextHopIp());
//    answerRoute.setOriginatorIp(route.getOriginatorIp());
//    answerRoute.setOriginType(route.getOriginType());
//    answerRoute.setEgpProtocol(route.getEgpProtocol());
//    answerRoute.setIgpProtocol(route.getIgpProtocol());
//    answerRoute.setSrcProtocol(route.getSrcProtocol());
//    answerRoute.setReceivedFromIp(route.getReceivedFromIp());
//    answerRoute.getCommunities().addAll(route.getCommunities());
//    answerRoute._reason=route._reason;
//    answerRoute._external=route._external;
//    answerRoute.setSrcNode(route.getSrcNode());
//    answerRoute._relatedNode.addAll(route.getRelatedNode());
//    answerRoute.setPrefixEcNum(route.getPrefixEcNum());
//    answerRoute.setSrcAcl(route.getSrcAcl());
//    answerRoute.setDstAcl(route.getDstAcl());
//
//    answerRoute.setIsisMetric(route.getIsisMetric());
//    answerRoute.setNextNode(route.getNextNode());
//    answerRoute.setNodePath(route.getNodePath());
//    if(route.getIsisLevel()!=null)
//    {
//      if(route.getIsisLevel().includes(IsisLevel.LEVEL_1)&&route.getIsisLevel().includes(IsisLevel.LEVEL_2))
//      {
//        answerRoute.setIsisLevel(IsisLevel.union(IsisLevel.LEVEL_1,IsisLevel.LEVEL_2));
//      }else if(route.getIsisLevel().includes(IsisLevel.LEVEL_2)){
//        answerRoute.setIsisLevel(IsisLevel.LEVEL_2);
//      }
//    }
//    answerRoute._lastLocalPreference=new SymLocalPreference(route._lastLocalPreference.getLowerBound(),route._lastLocalPreference.getUpperBound());
//    answerRoute._lastCommunities=new TreeSet<>();
//    answerRoute._lastCommunities.addAll(route.getCommunities());
//    return answerRoute;
//  }
//
//  public Map<Integer,List<SymBgpRoute>> getSimplifiedRib()
//  {
//    return _simplifiedRib;
//  }

}
