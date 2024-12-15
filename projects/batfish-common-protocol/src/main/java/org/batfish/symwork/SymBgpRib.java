package org.batfish.symwork;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.symwork.SymBgpRoute.Builder;

public class SymBgpRib {


  private jdd.bdd.BDD _topologyBDD;
  private int _topologyFilter;
  private String _nodename;

  private Map<BitSet,List<List<SymBgpRoute>>> _rib;
  private Set<BitSet> _changedPrefix;
  private List<SymBgpRoute> _withDrawRoute;

  private List<SymBgpRoute> _changedRoute;

  private boolean _multiPath;

  public SymBgpRib(jdd.bdd.BDD topologyBDD, int topologyFilter, String nodeName, boolean multiPath)
  {
    _topologyBDD = topologyBDD;
    _topologyFilter = topologyFilter;
    _nodename = nodeName;
    _rib = new HashMap<>();
    _changedPrefix = new HashSet<>();
    _withDrawRoute = new ArrayList<>();
    _changedRoute = new ArrayList<>();
    _multiPath = multiPath;
  }

  public Map<BitSet,List<List<SymBgpRoute>>> getRib()
  {
    return this._rib;
  }









  public void addRoute(SymBgpRoute addRoute)
  {
    addRoute.setReason(Reason.ADD);


    BitSet routePrefixEcSet = addRoute.getPrefixEcSet();


    if (_rib.containsKey(routePrefixEcSet))
    {
      boolean alreadyInject = false;
      int addIndex = 0;
      List<List<SymBgpRoute>> routeList = _rib.get(routePrefixEcSet);
      for (List<SymBgpRoute> routes : routeList)
      {
        if (routes.isEmpty())
        {
          continue;
        }
        SymBgpRoute comapreRoute = routes.get(0);
        int preference = addRoute.comparePreference(comapreRoute);
        if (preference == 0)
        {
          //          routes.add(addRoute);
          alreadyInject = true;
          break;
        } else if (preference > 0)
        {
          break;
        } else {
          addIndex++;
        }
      }
      if (alreadyInject && _multiPath)
      {
        routeList.get(addIndex).add(addRoute);
      }
      if (!alreadyInject)
      {
        List<SymBgpRoute> addRoutes = new ArrayList<>();
        addRoutes.add(addRoute);
        routeList.add(addIndex, addRoutes);
      }
      _changedPrefix.add(routePrefixEcSet);
    } else {
      HashMap<BitSet, HashSet<BitSet>> reSliceInRibEc = new HashMap<>();
      HashSet<BitSet> reSliceAddEc = new HashSet<>();
      BitSet addEc = (BitSet) routePrefixEcSet.clone();
      for (BitSet inRibPrefix : _rib.keySet())
      {
        if (addEc.isEmpty())
        {
          break;
        }
        BitSet andPrefixEc = (BitSet) inRibPrefix.clone();
        andPrefixEc.and(addEc);
        if (andPrefixEc.isEmpty())
        {
          continue;
        } else {
          if (andPrefixEc.equals(inRibPrefix))
          {
            reSliceAddEc.add(andPrefixEc);
            addEc.andNot(andPrefixEc);
          } else if (andPrefixEc.equals(addEc)) {
            BitSet remainRibEc = (BitSet) inRibPrefix.clone();
            remainRibEc.andNot(andPrefixEc);
            HashSet<BitSet> newRibEc = new HashSet<>();
            newRibEc.add(remainRibEc);
            newRibEc.add(andPrefixEc);
            reSliceInRibEc.put(inRibPrefix, newRibEc);
            reSliceAddEc.add(addEc);
            break;
          } else {
            BitSet remainRibEc = (BitSet) inRibPrefix.clone();
            remainRibEc.andNot(andPrefixEc);
            HashSet<BitSet> newRibEc = new HashSet<>();
            newRibEc.add(remainRibEc);
            newRibEc.add(andPrefixEc);
            reSliceInRibEc.put(inRibPrefix, newRibEc);

            reSliceAddEc.add(andPrefixEc);
            addEc.andNot(andPrefixEc);
          }
        }
      }
      if (!addEc.isEmpty())
      {
        reSliceAddEc.add(addEc);
      }

      for (BitSet originalInRibEc : reSliceInRibEc.keySet())
      {
        List<List<SymBgpRoute>> routeList = _rib.get(originalInRibEc);
        HashMap<BitSet, List<List<SymBgpRoute>>> newInRibRouteList = new HashMap<>();
        reSliceInRibEc.get(originalInRibEc).forEach(bitSet ->
            newInRibRouteList.computeIfAbsent(bitSet, k -> new ArrayList<>()));
        for (int i = 0; i < routeList.size(); i++)
        {
          HashMap<BitSet, List<SymBgpRoute>> newInRibRoute = new HashMap<>();
          reSliceInRibEc.get(originalInRibEc).forEach(bitSet ->
              newInRibRoute.computeIfAbsent(bitSet, k -> new ArrayList<>()));
          for (int j = 0; j < routeList.get(i).size(); j++)
          {
            SymBgpRoute.Builder originalRouteBuilder = new Builder(routeList.get(i).get(j));
            for (BitSet newEc : reSliceInRibEc.get(originalInRibEc))
            {
              SymBgpRoute newRoute = originalRouteBuilder.build();
              newRoute.setPrefixEcSet(newEc);
              newInRibRoute.get(newEc).add(newRoute);
            }
          }
          for (BitSet newEc : reSliceInRibEc.get(originalInRibEc))
          {
            newInRibRouteList.get(newEc).add(newInRibRoute.get(newEc));
          }
        }
        _rib.remove(originalInRibEc);
        _rib.putAll(newInRibRouteList);
      }
      SymBgpRoute.Builder newAddRouteBuilder = new Builder(addRoute);
      for (BitSet newAddEc : reSliceAddEc)
      {
        SymBgpRoute newAddRoute = newAddRouteBuilder.build();
        newAddRoute.setPrefixEcSet(newAddEc);
        addConcreteRoute(newAddRoute);
      }
      _changedPrefix.removeAll(reSliceInRibEc.keySet());
      for (HashSet<BitSet> newInRibEc : reSliceInRibEc.values())
      {
        _changedPrefix.addAll(newInRibEc);
      }
      _changedPrefix.addAll(reSliceAddEc);
    }
  }

  public void addConcreteRoute(SymBgpRoute addRoute)
  {
    addRoute.setReason(Reason.ADD);


    BitSet routePrefixEcSet = addRoute.getPrefixEcSet();


    if (_rib.containsKey(routePrefixEcSet))
    {
      boolean alreadyInject = false;
      int addIndex = 0;
      List<List<SymBgpRoute>> routeList = _rib.get(routePrefixEcSet);
      for (List<SymBgpRoute> routes : routeList)
      {
        if (routes.isEmpty())
        {
          continue;
        }
        SymBgpRoute comapreRoute = routes.get(0);
        int preference = addRoute.comparePreference(comapreRoute);
        if (preference == 0)
        {
          //          routes.add(addRoute);
          alreadyInject = true;
          break;
        } else if (preference > 0)
        {
          break;
        } else {
          addIndex++;
        }
      }
      if (alreadyInject && _multiPath)
      {
        routeList.get(addIndex).add(addRoute);
      }
      if (!alreadyInject)
      {
        List<SymBgpRoute> addRoutes = new ArrayList<>();
        addRoutes.add(addRoute);
        routeList.add(addIndex, addRoutes);
      }
    } else {
      _rib.put(routePrefixEcSet, new ArrayList<>());
      List<SymBgpRoute> routeList = new ArrayList<>();
      routeList.add(addRoute);
      _rib.get(routePrefixEcSet).add(routeList);
    }
  }

  public void updateRoute(SymBgpRoute updateRoute)
  {
    BitSet updateEc = updateRoute.getPrefixEcSet();
    if (_rib.containsKey(updateEc))
    {
      updateConcreteRoute(updateRoute);
    } else {
      BitSet tempUpdateEc = (BitSet) updateEc.clone();
      HashMap<BitSet, HashSet<BitSet>> reSliceInRibEc = new HashMap<>();
      HashSet<BitSet> reSliceUpdateEc = new HashSet<>();
      for (BitSet inRibEc : _rib.keySet())
      {
        if (tempUpdateEc.isEmpty())
        {
          break;
        }
        BitSet andEc = (BitSet) inRibEc.clone();
        andEc.and(tempUpdateEc);
        if (andEc.isEmpty())
        {
          continue;
        } else {
          if (andEc.equals(inRibEc))
          {
            reSliceUpdateEc.add(andEc);
            tempUpdateEc.andNot(andEc);
          } else if (andEc.equals(tempUpdateEc)) {
            BitSet remainRibEc = (BitSet) inRibEc.clone();
            remainRibEc.andNot(andEc);
            HashSet<BitSet> newRibEc = new HashSet<>();
            newRibEc.add(remainRibEc);
            newRibEc.add(andEc);
            reSliceInRibEc.put(inRibEc, newRibEc);
            reSliceUpdateEc.add(tempUpdateEc);
            break;
          } else {
            BitSet remainRibEc = (BitSet) inRibEc.clone();
            remainRibEc.andNot(andEc);
            HashSet<BitSet> newRibEc = new HashSet<>();
            newRibEc.add(remainRibEc);
            newRibEc.add(andEc);
            reSliceInRibEc.put(inRibEc, newRibEc);

            reSliceUpdateEc.add(andEc);
            tempUpdateEc.andNot(andEc);
          }
        }
      }

      if (!tempUpdateEc.isEmpty())
      {
        reSliceUpdateEc.add(tempUpdateEc);
      }

      for (BitSet originalInRibEc : reSliceInRibEc.keySet())
      {
        List<List<SymBgpRoute>> routeList = _rib.get(originalInRibEc);
        HashMap<BitSet, List<List<SymBgpRoute>>> newInRibRouteList = new HashMap<>();
        reSliceInRibEc.get(originalInRibEc).forEach(bitSet ->
            newInRibRouteList.computeIfAbsent(bitSet, k -> new ArrayList<>()));
        for (int i = 0; i < routeList.size(); i++)
        {
          HashMap<BitSet, List<SymBgpRoute>> newInRibRoute = new HashMap<>();
          reSliceInRibEc.get(originalInRibEc).forEach(bitSet ->
              newInRibRoute.computeIfAbsent(bitSet, k -> new ArrayList<>()));
          for (int j = 0; j < routeList.get(i).size(); j++)
          {
            SymBgpRoute.Builder originalRouteBuilder = new Builder(routeList.get(i).get(j));
            for (BitSet newEc : reSliceInRibEc.get(originalInRibEc))
            {
              SymBgpRoute newRoute = originalRouteBuilder.build();
              newRoute.setPrefixEcSet(newEc);
              newInRibRoute.get(newEc).add(newRoute);
            }
          }
          for (BitSet newEc : reSliceInRibEc.get(originalInRibEc))
          {
            newInRibRouteList.get(newEc).add(newInRibRoute.get(newEc));
          }
        }
        _rib.remove(originalInRibEc);
        _rib.putAll(newInRibRouteList);
      }

      _changedPrefix.removeAll(reSliceInRibEc.keySet());
      for (HashSet<BitSet> newInRibEc : reSliceInRibEc.values())
      {
        _changedPrefix.addAll(newInRibEc);
      }

      SymBgpRoute.Builder newAddRouteBuilder = new Builder(updateRoute);
      for (BitSet newUpdateEc : reSliceUpdateEc)
      {
        SymBgpRoute newAddRoute = newAddRouteBuilder.build();
        newAddRoute.setPrefixEcSet(newUpdateEc);
        updateConcreteRoute(newAddRoute);
      }
    }
  }

  public void updateConcreteRoute(SymBgpRoute updateRoute)
  {
    BitSet routePrefix = updateRoute.getPrefixEcSet();
    _changedPrefix.add(routePrefix);
    if (!_rib.containsKey(routePrefix))
    {
      _rib.put(routePrefix, new ArrayList<>());
      List<SymBgpRoute> addRoutes = new ArrayList<>();
      _rib.get(routePrefix).add(addRoutes);
      return;
    }

    boolean alreadyUpdate = false;
    for (List<SymBgpRoute> routes : _rib.get(routePrefix))
    {
      if (routes.contains(updateRoute))
      {
        int updateIndex = routes.indexOf(updateRoute);
        routes.remove(updateIndex);
        updateRoute.setReason(Reason.UPDATE);
        routes.add(updateRoute);
        alreadyUpdate = true;
        break;
      }
    }
    if (!alreadyUpdate)
    {
      addConcreteRoute(updateRoute);
    }
  }



  public void removeConcreteRoute(SymBgpRoute removeRoute)
  {
    BitSet routePrefix = removeRoute.getPrefixEcSet();
    if (!_rib.containsKey(routePrefix))
    {
      return;
    }
    for (List<SymBgpRoute> routes : _rib.get(routePrefix))
    {
      if (routes.contains(removeRoute))
      {
        _changedPrefix.add(routePrefix);
        if (routes.get(routes.indexOf(removeRoute)).getReason() != Reason.ADD)
        {
          routes.remove(removeRoute);
          removeRoute.setReason(Reason.WITHDRAW);
          _withDrawRoute.add(removeRoute);
        } else {
          routes.remove(removeRoute);
        }
        break;
      }
    }
  }

  public void removeRoute(SymBgpRoute removeRoute)
  {
    BitSet removeEc = removeRoute.getPrefixEcSet();
    if (_rib.containsKey(removeEc))
    {
//      if (removeRoute.getAsPath().size() == 0)
//      {
//        System.out.println("withdraw error!");
//      }
      removeConcreteRoute(removeRoute);
    } else {
      BitSet tempRemoveEc = (BitSet) removeEc.clone();
      HashMap<BitSet, HashSet<BitSet>> reSliceInRibEc = new HashMap<>();
      HashSet<BitSet> reSliceRemoveEc = new HashSet<>();
      for (BitSet inRibEc : _rib.keySet())
      {
        if (tempRemoveEc.isEmpty())
        {
          break;
        }
        BitSet andEc = (BitSet) inRibEc.clone();
        andEc.and(tempRemoveEc);
        if (andEc.isEmpty())
        {
          continue;
        } else {
          if (andEc.equals(inRibEc))
          {
            reSliceRemoveEc.add(andEc);
            tempRemoveEc.andNot(andEc);
          } else if (andEc.equals(tempRemoveEc)) {
            BitSet remainRibEc = (BitSet) inRibEc.clone();
            remainRibEc.andNot(andEc);
            HashSet<BitSet> newRibEc = new HashSet<>();
            newRibEc.add(remainRibEc);
            newRibEc.add(andEc);
            reSliceInRibEc.put(inRibEc, newRibEc);
            reSliceRemoveEc.add(tempRemoveEc);
            break;
          } else {
            BitSet remainRibEc = (BitSet) inRibEc.clone();
            remainRibEc.andNot(andEc);
            HashSet<BitSet> newRibEc = new HashSet<>();
            newRibEc.add(remainRibEc);
            newRibEc.add(andEc);
            reSliceInRibEc.put(inRibEc, newRibEc);

            reSliceRemoveEc.add(andEc);
            tempRemoveEc.andNot(andEc);
          }
        }
      }

      if (!tempRemoveEc.isEmpty())
      {
        reSliceRemoveEc.add(tempRemoveEc);
      }

      for (BitSet originalInRibEc : reSliceInRibEc.keySet())
      {
        List<List<SymBgpRoute>> routeList = _rib.get(originalInRibEc);
        HashMap<BitSet, List<List<SymBgpRoute>>> newInRibRouteList = new HashMap<>();
        reSliceInRibEc.get(originalInRibEc).forEach(bitSet ->
            newInRibRouteList.computeIfAbsent(bitSet, k -> new ArrayList<>()));
        for (int i = 0; i < routeList.size(); i++)
        {
          HashMap<BitSet, List<SymBgpRoute>> newInRibRoute = new HashMap<>();
          reSliceInRibEc.get(originalInRibEc).forEach(bitSet ->
              newInRibRoute.computeIfAbsent(bitSet, k -> new ArrayList<>()));
          for (int j = 0; j < routeList.get(i).size(); j++)
          {
            SymBgpRoute.Builder originalRouteBuilder = new Builder(routeList.get(i).get(j));
            for (BitSet newEc : reSliceInRibEc.get(originalInRibEc))
            {
              SymBgpRoute newRoute = originalRouteBuilder.build();
              newRoute.setPrefixEcSet(newEc);
              newInRibRoute.get(newEc).add(newRoute);
            }
          }
          for (BitSet newEc : reSliceInRibEc.get(originalInRibEc))
          {
            newInRibRouteList.get(newEc).add(newInRibRoute.get(newEc));
          }
        }
        _rib.remove(originalInRibEc);
        _rib.putAll(newInRibRouteList);
      }
      _changedPrefix.removeAll(reSliceInRibEc.keySet());
      for (HashSet<BitSet> newInRibEc : reSliceInRibEc.values())
      {
        _changedPrefix.addAll(newInRibEc);
      }

      SymBgpRoute.Builder newAddRouteBuilder = new Builder(removeRoute);
      for (BitSet newRemoveEc : reSliceRemoveEc)
      {
        SymBgpRoute newAddRoute = newAddRouteBuilder.build();
        newAddRoute.setPrefixEcSet(newRemoveEc);
        removeConcreteRoute(newAddRoute);
      }
    }
  }

  public void computeChangedRoute()
  {
    _changedRoute.clear();
    List<SymBgpRoute> changedRoute = new ArrayList<>();
    HashSet<BitSet> emptyPrefix = new HashSet<>();
    boolean consrderExternal = false;
    for(BitSet changedPrefix : _changedPrefix)
    {
      int currentTopology = 1;
      boolean findChanged = false;
      HashSet<Integer> computeIndex = new HashSet<>();
      List<List<SymBgpRoute>> routerList = _rib.get(changedPrefix);
      for (int j = 0; j < routerList.size(); j++)
      {
        List<SymBgpRoute> routes = routerList.get(j);
        Set<SymBgpRoute> removeRoute = new HashSet<>();
        if (currentTopology == 0)
        {
          for (SymBgpRoute ribBgpRoute : routes)
          {
            if (ribBgpRoute.getExternal() && consrderExternal && computeIndex.contains(ribBgpRoute.getOriginatorIndex()))
            {
              System.out.println("External route compute : re-compute topology condition!");
              int tempCurrentTopology = 0;
              for (int i = 0; i < routerList.indexOf(routes); i++)
              {
                for (SymBgpRoute tempRoute : routerList.get(i))
                {
                  if (tempRoute.getOriginatorIndex().equals(ribBgpRoute.getOriginatorIndex()))
                  {
                    continue;
                  } else {
                    tempCurrentTopology = _topologyBDD.ref(_topologyBDD.or(tempCurrentTopology, tempRoute.getTopologyCondition()));
                  }
                }
              }
              int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(_topologyBDD.not(tempCurrentTopology), ribBgpRoute.getTopologyCondition()));
              newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter));
              if (newTopologyCondition == 0)
              {
                if (!ribBgpRoute.getReason().equals(Reason.ADD))
                {
                  SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
                  withDrawRoute.setReason(Reason.WITHDRAW);
                  changedRoute.add(withDrawRoute);
                }
                removeRoute.add(ribBgpRoute);
              } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
              {
                SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
                newRoute.setReason(ribBgpRoute.getReason());
                newRoute.setTopologyCondition(newTopologyCondition);
                ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
                changedRoute.add(newRoute);
              }
            } else {
              if (!ribBgpRoute.getReason().equals(Reason.ADD))
              {
                SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
                withDrawRoute.setReason(Reason.WITHDRAW);
                changedRoute.add(withDrawRoute);
              }
              removeRoute.add(ribBgpRoute);
            }
          }
        } else if (!findChanged)
        {
          for (SymBgpRoute ribBgpRoute : routes)
          {
            if (!ribBgpRoute.getReason().equals(Reason.NORMAL))
            {
              findChanged = true;
              if (ribBgpRoute.getExternal() && consrderExternal && computeIndex.contains(ribBgpRoute.getOriginatorIndex()))
              {
                System.out.println("External route compute : re-compute topology condition!");
                int tempCurrentTopology = 0;
                for (int i = 0; i < routerList.indexOf(routes); i++)
                {
                  for (SymBgpRoute tempRoute : routerList.get(i))
                  {
                    if (tempRoute.getOriginatorIndex().equals(ribBgpRoute.getOriginatorIndex()))
                    {
                      continue;
                    } else {
                      tempCurrentTopology = _topologyBDD.ref(_topologyBDD.or(tempCurrentTopology, tempRoute.getTopologyCondition()));
                    }
                  }
                }
                int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(_topologyBDD.not(tempCurrentTopology), ribBgpRoute.getTopologyCondition()));
                newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter ));
                if (newTopologyCondition == 0)
                {
                  if (!ribBgpRoute.getReason().equals(Reason.ADD))
                  {
                    SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
                    withDrawRoute.setReason(Reason.WITHDRAW);
                    changedRoute.add(withDrawRoute);
                  }
                  removeRoute.add(ribBgpRoute);
                } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
                {
                  SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
                  newRoute.setReason(ribBgpRoute.getReason());
                  newRoute.setTopologyCondition(newTopologyCondition);
                  ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
                  changedRoute.add(newRoute);
                }
              } else {


                int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(currentTopology, ribBgpRoute.getTopologyCondition()));
                newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter));
                if (newTopologyCondition == 0)
                {
                  if (!ribBgpRoute.getReason().equals(Reason.ADD))
                  {
                    SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
                    withDrawRoute.setReason(Reason.WITHDRAW);
                    changedRoute.add(withDrawRoute);
                  }
                  removeRoute.add(ribBgpRoute);
                } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
                {
                  SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
                  newRoute.setReason(ribBgpRoute.getReason());
                  newRoute.setTopologyCondition(newTopologyCondition);
                  ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
                  changedRoute.add(newRoute);
                }
              }
            }
          }
        } else {
          for (SymBgpRoute ribBgpRoute : routes)
          {
            if (ribBgpRoute.getExternal() && consrderExternal && computeIndex.contains(ribBgpRoute.getOriginatorIndex()))
            {
              System.out.println("External route compute : re-compute topology condition!");
              int tempCurrentTopology = 0;
              for (int i = 0; i < routerList.indexOf(routes); i++)
              {
                for (SymBgpRoute tempRoute : routerList.get(i))
                {
                  if (tempRoute.getOriginatorIndex().equals(ribBgpRoute.getOriginatorIndex()))
                  {
                    continue;
                  } else {
                    tempCurrentTopology = _topologyBDD.ref(_topologyBDD.or(tempCurrentTopology, tempRoute.getTopologyCondition()));
                  }
                }
              }
              int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(_topologyBDD.not(tempCurrentTopology), ribBgpRoute.getTopologyCondition()));
              newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter));
              if (newTopologyCondition == 0)
              {
                if (!ribBgpRoute.getReason().equals(Reason.ADD))
                {
                  SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
                  withDrawRoute.setReason(Reason.WITHDRAW);
                  changedRoute.add(withDrawRoute);
                }
                removeRoute.add(ribBgpRoute);
              } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
              {
                SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
                ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
                newRoute.setTopologyCondition(newTopologyCondition);
                newRoute.setReason(ribBgpRoute.getReason());
                changedRoute.add(newRoute);
              }
            } else {


              int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(currentTopology, ribBgpRoute.getTopologyCondition()));
              newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter));
              if (newTopologyCondition == 0)
              {
                if (!ribBgpRoute.getReason().equals(Reason.ADD))
                {
                  SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
                  withDrawRoute.setReason(Reason.WITHDRAW);
                  changedRoute.add(withDrawRoute);
                }
                removeRoute.add(ribBgpRoute);
              } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
              {
                SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
                newRoute.setReason(ribBgpRoute.getReason());
                newRoute.setTopologyCondition(newTopologyCondition);
                ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
                changedRoute.add(newRoute);
              }
            }
          }
        }
        routes.removeAll(removeRoute);
        if (routes.isEmpty())
        {
          emptyPrefix.add(changedPrefix);
        }
        for (SymBgpRoute ribBgpRoute : routes)
        {
          computeIndex.add(ribBgpRoute.getOriginatorIndex());
          currentTopology = _topologyBDD.ref(_topologyBDD.and(currentTopology, _topologyBDD.not(ribBgpRoute.getTopologyCondition())));
        }
        currentTopology = _topologyBDD.ref(_topologyBDD.and(currentTopology, _topologyFilter));
      }
    }
    for (BitSet prefix : emptyPrefix)
    {
      List<List<SymBgpRoute>> routerList = _rib.get(prefix);
      routerList.removeIf(routes -> routes.size() == 0);
    }
    _changedRoute.addAll(changedRoute);
    _changedRoute.addAll(_withDrawRoute);
    return ;
  }


  public void reinitRib()
  {
    for (BitSet prefix : _changedPrefix)
    {
      List<List<SymBgpRoute>> routerList = _rib.get(prefix);
      routerList.parallelStream().forEach(routes ->
          routes.forEach(route -> route.setReason(Reason.NORMAL))
      );
    }
    _changedPrefix.clear();
    _changedRoute.clear();
    _withDrawRoute.clear();
  }

  public List<SymBgpRoute> getChanRoutes()
  {
    return _changedRoute;
  }


  public void prepareForForwarding()
  {
    for (BitSet prefix : _rib.keySet())
    {
      int currentTopology = 1;
      List<List<SymBgpRoute>> routerList = _rib.get(prefix);
      for (List<SymBgpRoute> routes : routerList)
      {
        int tempCurrentTopology = currentTopology;
        for (SymBgpRoute route : routes)
        {
          tempCurrentTopology = _topologyBDD.ref(_topologyBDD.and(tempCurrentTopology, _topologyBDD.not(route.getTopologyCondition())));
        }
        for (SymBgpRoute route : routes)
        {
          Integer forwardingTopologyCondition = _topologyBDD.ref(_topologyBDD.and(currentTopology, route.getTopologyCondition()));
          route.setTopologyCondition(forwardingTopologyCondition);
        }
        currentTopology = tempCurrentTopology;
      }
    }
  }

//  普通的PrefixEcNum版本
//  private jdd.bdd.BDD _topologyBDD;
//  private int _topologyFilter;
//  private String _nodename;
//
//  private Map<Integer,List<List<SymBgpRoute>>> _rib;
//  private Set<Integer> _changedPrefix;
//  private List<SymBgpRoute> _withDrawRoute;
//
//  private List<SymBgpRoute> _changedRoute;
//
//  private boolean _multiPath;
//
//  public SymBgpRib(jdd.bdd.BDD topologyBDD, int topologyFilter, String nodeName, boolean multiPath)
//  {
//    _topologyBDD = topologyBDD;
//    _topologyFilter = topologyFilter;
//    _nodename = nodeName;
//    _rib = new HashMap<>();
//    _changedPrefix = new HashSet<>();
//    _withDrawRoute = new ArrayList<>();
//    _changedRoute = new ArrayList<>();
//    _multiPath = multiPath;
//  }
//
//  public Map<Integer,List<List<SymBgpRoute>>> getRib()
//  {
//    return this._rib;
//  }
//  public void addRoute(SymBgpRoute addRoute)
//  {
//    addRoute.setReason(Reason.ADD);
//
//    int routePrefix = addRoute.getPrefixEcNum();
//    _changedPrefix.add(routePrefix);
//    if (!_rib.containsKey(routePrefix))
//    {
//      _rib.put(routePrefix, new ArrayList<>());
//    }
//
//    boolean alreadyInject = false;
//    int addIndex = 0;
//    List<List<SymBgpRoute>> routeList = _rib.get(routePrefix);
//
//
//    for (List<SymBgpRoute> routes : routeList)
//    {
//      if (routes.isEmpty())
//      {
//        continue;
//      }
//      SymBgpRoute comapreRoute = routes.get(0);
//      int preference = addRoute.comparePreference(comapreRoute);
//      if (preference == 0)
//      {
////          routes.add(addRoute);
//          alreadyInject = true;
//          break;
//      } else if (preference > 0)
//      {
//          break;
//      } else {
//          addIndex++;
//      }
//    }
//    if (alreadyInject && _multiPath)
//    {
//      routeList.get(addIndex).add(addRoute);
//    }
//    if (!alreadyInject)
//    {
//      List<SymBgpRoute> addRoutes = new ArrayList<>();
//      addRoutes.add(addRoute);
//      routeList.add(addIndex, addRoutes);
//    }
//  }
//
//  public void updateRoute(SymBgpRoute updateRoute)
//  {
//    int routePrefix = updateRoute.getPrefixEcNum();
//    _changedPrefix.add(routePrefix);
//    if (!_rib.containsKey(routePrefix))
//    {
//      _rib.put(routePrefix, new ArrayList<>());
//      List<SymBgpRoute> addRoutes = new ArrayList<>();
//      _rib.get(routePrefix).add(addRoutes);
//      return;
//    }
//    boolean alreadyUpdate = false;
//    for (List<SymBgpRoute> routes : _rib.get(routePrefix))
//    {
//      if (routes.contains(updateRoute))
//      {
//        int updateIndex = routes.indexOf(updateRoute);
//        routes.remove(updateIndex);
//        updateRoute.setReason(Reason.UPDATE);
//        routes.add(updateRoute);
//        alreadyUpdate = true;
//        break;
//      }
//    }
//    if (!alreadyUpdate)
//    {
//      addRoute(updateRoute);
//    }
//  }
//
//  public void removeRoute(SymBgpRoute removeRoute)
//  {
//    int routePrefix = removeRoute.getPrefixEcNum();
//    if (!_rib.containsKey(routePrefix))
//    {
//      return;
//    }
//    for (List<SymBgpRoute> routes : _rib.get(routePrefix))
//    {
//      if (routes.contains(removeRoute))
//      {
//        _changedPrefix.add(routePrefix);
//        routes.remove(removeRoute);
//        removeRoute.setReason(Reason.WITHDRAW);
//        _withDrawRoute.add(removeRoute);
//        break;
//      }
//    }
//  }
//
//
//  public void computeChangedRoute()
//  {
//    _changedRoute.clear();
//    List<SymBgpRoute> changedRoute = new ArrayList<>();
//    HashSet<Integer> emptyPrefix = new HashSet<>();
//    boolean consrderExternal = false;
//    for(Integer changedPrefix : _changedPrefix)
//    {
//      int currentTopology = 1;
//      boolean findChanged = false;
//      HashSet<Integer> computeIndex = new HashSet<>();
//      List<List<SymBgpRoute>> routerList = _rib.get(changedPrefix);
//      for (int j = 0; j < routerList.size(); j++)
//      {
//        List<SymBgpRoute> routes = routerList.get(j);
//        Set<SymBgpRoute> removeRoute = new HashSet<>();
//        if (currentTopology == 0)
//        {
//          for (SymBgpRoute ribBgpRoute : routes)
//          {
//            if (ribBgpRoute.getExternal() && consrderExternal && computeIndex.contains(ribBgpRoute.getOriginatorIndex()))
//            {
//              System.out.println("External route compute : re-compute topology condition!");
//              int tempCurrentTopology = 0;
//                for (int i = 0; i < routerList.indexOf(routes); i++)
//                {
//                  for (SymBgpRoute tempRoute : routerList.get(i))
//                  {
//                    if (tempRoute.getOriginatorIndex().equals(ribBgpRoute.getOriginatorIndex()))
//                    {
//                      continue;
//                    } else {
//                      tempCurrentTopology = _topologyBDD.ref(_topologyBDD.or(tempCurrentTopology, tempRoute.getTopologyCondition()));
//                    }
//                  }
//                }
//                int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(_topologyBDD.not(tempCurrentTopology), ribBgpRoute.getTopologyCondition()));
//                newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter));
//                if (newTopologyCondition == 0)
//                {
//                  if (!ribBgpRoute.getReason().equals(Reason.ADD))
//                  {
//                    SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
//                    withDrawRoute.setReason(Reason.WITHDRAW);
//                    changedRoute.add(withDrawRoute);
//                  }
//                  removeRoute.add(ribBgpRoute);
//                } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
//                {
//                  SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
//                  newRoute.setReason(ribBgpRoute.getReason());
//                  newRoute.setTopologyCondition(newTopologyCondition);
//                  ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
//                  changedRoute.add(newRoute);
//                }
//            } else {
//              if (!ribBgpRoute.getReason().equals(Reason.ADD))
//              {
//                SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
//                withDrawRoute.setReason(Reason.WITHDRAW);
//                changedRoute.add(withDrawRoute);
//              }
//              removeRoute.add(ribBgpRoute);
//            }
//          }
//        } else if (!findChanged)
//        {
//          for (SymBgpRoute ribBgpRoute : routes)
//          {
//            if (!ribBgpRoute.getReason().equals(Reason.NORMAL))
//            {
//              findChanged = true;
//              if (ribBgpRoute.getExternal() && consrderExternal && computeIndex.contains(ribBgpRoute.getOriginatorIndex()))
//              {
//                System.out.println("External route compute : re-compute topology condition!");
//                int tempCurrentTopology = 0;
//                for (int i = 0; i < routerList.indexOf(routes); i++)
//                {
//                  for (SymBgpRoute tempRoute : routerList.get(i))
//                  {
//                    if (tempRoute.getOriginatorIndex().equals(ribBgpRoute.getOriginatorIndex()))
//                    {
//                      continue;
//                    } else {
//                      tempCurrentTopology = _topologyBDD.ref(_topologyBDD.or(tempCurrentTopology, tempRoute.getTopologyCondition()));
//                    }
//                  }
//                }
//                int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(_topologyBDD.not(tempCurrentTopology), ribBgpRoute.getTopologyCondition()));
//                newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter ));
//                if (newTopologyCondition == 0)
//                {
//                  if (!ribBgpRoute.getReason().equals(Reason.ADD))
//                  {
//                    SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
//                    withDrawRoute.setReason(Reason.WITHDRAW);
//                    changedRoute.add(withDrawRoute);
//                  }
//                  removeRoute.add(ribBgpRoute);
//                } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
//                {
//                  SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
//                  newRoute.setReason(ribBgpRoute.getReason());
//                  newRoute.setTopologyCondition(newTopologyCondition);
//                  ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
//                  changedRoute.add(newRoute);
//                }
//              } else {
//
//
//                int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(currentTopology, ribBgpRoute.getTopologyCondition()));
//                newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter));
//                if (newTopologyCondition == 0)
//                {
//                  if (!ribBgpRoute.getReason().equals(Reason.ADD))
//                  {
//                    SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
//                    withDrawRoute.setReason(Reason.WITHDRAW);
//                    changedRoute.add(withDrawRoute);
//                  }
//                  removeRoute.add(ribBgpRoute);
//                } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
//                {
//                  SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
//                  newRoute.setReason(ribBgpRoute.getReason());
//                  newRoute.setTopologyCondition(newTopologyCondition);
//                  ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
//                  changedRoute.add(newRoute);
//                }
//              }
//            }
//          }
//        } else {
//          for (SymBgpRoute ribBgpRoute : routes)
//          {
//              if (ribBgpRoute.getExternal() && consrderExternal && computeIndex.contains(ribBgpRoute.getOriginatorIndex()))
//              {
//                System.out.println("External route compute : re-compute topology condition!");
//                int tempCurrentTopology = 0;
//                for (int i = 0; i < routerList.indexOf(routes); i++)
//                {
//                  for (SymBgpRoute tempRoute : routerList.get(i))
//                  {
//                    if (tempRoute.getOriginatorIndex().equals(ribBgpRoute.getOriginatorIndex()))
//                    {
//                      continue;
//                    } else {
//                      tempCurrentTopology = _topologyBDD.ref(_topologyBDD.or(tempCurrentTopology, tempRoute.getTopologyCondition()));
//                    }
//                  }
//                }
//                int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(_topologyBDD.not(tempCurrentTopology), ribBgpRoute.getTopologyCondition()));
//                newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter));
//                if (newTopologyCondition == 0)
//                {
//                  if (!ribBgpRoute.getReason().equals(Reason.ADD))
//                  {
//                    SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
//                    withDrawRoute.setReason(Reason.WITHDRAW);
//                    changedRoute.add(withDrawRoute);
//                  }
//                  removeRoute.add(ribBgpRoute);
//                } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
//                {
//                  SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
//                  ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
//                  newRoute.setTopologyCondition(newTopologyCondition);
//                  newRoute.setReason(ribBgpRoute.getReason());
//                  changedRoute.add(newRoute);
//                }
//              } else {
//
//
//                int newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(currentTopology, ribBgpRoute.getTopologyCondition()));
//                newTopologyCondition = _topologyBDD.ref(_topologyBDD.and(newTopologyCondition, _topologyFilter));
//                if (newTopologyCondition == 0)
//                {
//                  if (!ribBgpRoute.getReason().equals(Reason.ADD))
//                  {
//                    SymBgpRoute withDrawRoute = ribBgpRoute.toBuilder().build();
//                    withDrawRoute.setReason(Reason.WITHDRAW);
//                    changedRoute.add(withDrawRoute);
//                  }
//                  removeRoute.add(ribBgpRoute);
//                } else if(newTopologyCondition != ribBgpRoute.getLastRoundTopologyCondition())
//                {
//                  SymBgpRoute newRoute = ribBgpRoute.toBuilder().build();
//                  newRoute.setReason(ribBgpRoute.getReason());
//                  newRoute.setTopologyCondition(newTopologyCondition);
//                  ribBgpRoute.setLastRoundTopologyCondition(newTopologyCondition);
//                  changedRoute.add(newRoute);
//                }
//              }
//          }
//        }
//        routes.removeAll(removeRoute);
//        if (routes.isEmpty())
//        {
//          emptyPrefix.add(changedPrefix);
//        }
//        for (SymBgpRoute ribBgpRoute : routes)
//        {
//          computeIndex.add(ribBgpRoute.getOriginatorIndex());
//          currentTopology = _topologyBDD.ref(_topologyBDD.and(currentTopology, _topologyBDD.not(ribBgpRoute.getTopologyCondition())));
//        }
//        currentTopology = _topologyBDD.ref(_topologyBDD.and(currentTopology, _topologyFilter));
//      }
//    }
//    for (Integer prefix : emptyPrefix)
//    {
//      List<List<SymBgpRoute>> routerList = _rib.get(prefix);
//      routerList.removeIf(routes -> routes.size() == 0);
//    }
//    _changedRoute.addAll(changedRoute);
//    _changedRoute.addAll(_withDrawRoute);
//    return ;
//  }
//
//
//  public void reinitRib()
//  {
//    for (Integer prefix : _changedPrefix)
//    {
//      List<List<SymBgpRoute>> routerList = _rib.get(prefix);
//      routerList.parallelStream().forEach(routes ->
//          routes.forEach(route -> route.setReason(Reason.NORMAL))
//      );
//    }
//    _changedPrefix.clear();
//    _changedRoute.clear();
//    _withDrawRoute.clear();
//  }
//
//  public List<SymBgpRoute> getChanRoutes()
//  {
//    return _changedRoute;
//  }











//   public Map<Integer,List<List<SymBgpRoute>>> _rib;
//   Set<Integer> _simplifiedChangePrefix;
//   BDDFactory _topologyfactory;
//   Set<SymBgpRoute> _withDrawRoute;

//   String _nodeName;

//   HashMap<TreeNode,Set<TreeNode>> _updateTreeNodes;
// //  Set<TreeNode> _addTreeNodes;

//   HashMap<TreeNode,Set<TreeNode>> _addTreeNodes;
//   HashMap<TreeNode,Integer> _updateRouteIndex;

//   HashMap<Integer,Set<TreeNode>> _deleteTreeNodes;

//   int _k=2;

//   public List<SymBgpRoute> _changedRoute;

//   public HashMap<TreeNode, Set<TreeNode>> _replaceTreeNodes;

//   public HashMap<TreeNode,TreeNode> _originalNodePairs;

//   public jdd.bdd.BDD _topologyBDD;
//   int _topologyFilter;


//   public SymBgpRib(jdd.bdd.BDD factory,int topologyFilter,String nodeName)
//   {
//     this._rib=new HashMap<>();
//     this._topologyFilter=topologyFilter;
//     this._simplifiedChangePrefix=new HashSet<>();
//     this._topologyBDD=factory;
//     this._withDrawRoute=new HashSet<>();
//     _nodeName=nodeName;
//     _updateTreeNodes=new HashMap<>();
//     _addTreeNodes=new HashMap<>();
//     _replaceTreeNodes=new HashMap<>();
//     _updateRouteIndex=new HashMap<>();
//     _deleteTreeNodes=new HashMap<>();
//     _originalNodePairs=new HashMap<>();
//     _changedRoute=new ArrayList<>();
//     _withDrawRoute=new HashSet<>();
//   }
//   public void InitRib()
//   {
// //    for(List<List<SymBgpRoute>> routeList:_rib.values())
// //    {
// //      for(List<SymBgpRoute> routes:routeList)
// //      {
// //        for(SymBgpRoute route:routes)
// //        {
// //          route._reason=Reason.NORMAL;
// //        }
// //      }
// //    }
//     //
//     _rib
//         .values()
// //        .parallelStream()
//         .forEach(
//             routeList->{
//               routeList
//                   .parallelStream()
//                   .forEach(
//                       routes->{
//                         routes
//                             .forEach(
//                                 route->{
//                                   route._reason=Reason.NORMAL;
//                                 }
//                             );
//                       }
//                   );
//             }
//         );
//     this._withDrawRoute.clear();
//     this._simplifiedChangePrefix.clear();
//     this._changedRoute.clear();
//   }

//   public boolean UpdateRoute(SymBgpRoute updateRouteSource,TreeNode fatherTreeNode)
//   {
// //    System.out.println("step:Update");
//     Integer routeNetWork=updateRouteSource.getPrefixEcNum();
//     _simplifiedChangePrefix.add(routeNetWork);
//     boolean newAdd=false;
//     Queue<SymBgpRoute> updateQueue=new LinkedList<>();
//     updateQueue.add(updateRouteSource);
//     while(!updateQueue.isEmpty())
//     {
//       SymBgpRoute route=updateQueue.poll();
//       if(!_rib.keySet().contains(routeNetWork))
//       {
//         _rib.put(routeNetWork, new ArrayList<>());
//         _rib.get(routeNetWork).add(new ArrayList<>());
//         route._reason=Reason.ADD;
//         _rib.get(routeNetWork).get(0).add(route);
//         _addTreeNodes.computeIfAbsent(fatherTreeNode,k->new HashSet<>());
//         _addTreeNodes.get(fatherTreeNode).add(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()));
//         _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),fatherTreeNode);
//         newAdd=true;
//       }else{
//         List<List<SymBgpRoute>> routes=_rib.get(routeNetWork);
//         int location=-1;
//         int index=-1;

//         for(int i=0;i<routes.size();i++)
//         {
//           for(int j=0;j<routes.get(i).size();j++)
//           {
//             if(routes.get(i).get(j).equalsUpdate(route))
//             {
//               location=i;
//               index=j;
//               break;
//             }
//           }
//           //        if(routes.get(i).contains(route))
//           //        {
//           //          location=i;
//           //          break;
//           //        }
//         }
//         if(location==-1)
//         {
//           route._reason=Reason.NORMAL;
//           _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),fatherTreeNode);
//           AddRoute(route);
//           newAdd=true;
//         }else{
//           SymBgpRoute originalRoute=_rib.get(routeNetWork).get(location).get(index);
// //          route._outBGPTopologyCondition=originalRoute._outBGPTopologyCondition;
//           if(originalRoute.equalsExceptTopologyCondition(route))
//           {
//             _rib.get(routeNetWork).get(location).remove(index);
//             _rib.get(routeNetWork).get(location).add(index,route);
//             if(originalRoute._reason!=Reason.ADD)
//             {
//               _rib.get(routeNetWork).get(location).get(index)._reason=Reason.UPDATE;
//             }
//           }else {
//             if (originalRoute.getAsPathSize().getLowerbound() <= route.getAsPathSize().getLowerbound()
//                 && originalRoute.getAsPathSize().getUpperbound() >= route.getAsPathSize()
//                 .getUpperbound()) {
//               if (originalRoute.getAsPathSize().getLowerbound() == route.getAsPathSize()
//                   .getLowerbound()) {
//                 SymBgpRoute originalRouteUpper = cloneBgpRoute(originalRoute);
//                 TreeNode thisNode = new TreeNode(
//                     _nodeName,
//                     originalRoute.getIndex(),
//                     originalRoute.getAsPath().hashCode(),
//                     originalRoute.getAsPathSize().hashCode(),
//                     originalRoute.getAsPathSize(),route.getAsPath());
//                 if (originalRoute._reason == Reason.NORMAL) {
//                   TreeNode originalNode = thisNode;
// //                  if (originalNode == null) {
// //                    System.out.println("break");
// //                  }
//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteUpper.setAsPathSize(route.getAsPathSize().getUpperbound() + 1,
//                       originalRouteUpper.getAsPathSize().getUpperbound());
//                   originalRoute.equalsExceptTopologyCondition(route);
//                   originalRouteUpper._reason = Reason.UPDATE;
//                   route._reason = Reason.UPDATE;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteUpper.getIndex(),
//                       originalRouteUpper.getAsPath().hashCode(),
//                       originalRouteUpper.getAsPathSize().hashCode(),
//                       originalRouteUpper.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);

//                   addQueue.add(route);
//                   addQueue.add(originalRouteUpper);
//                   AddRoute(addQueue, routeNetWork);
// //                  System.out.println("break");
//                 } else if (originalRoute._reason == Reason.ADD) {
//                   TreeNode originalNode = _originalNodePairs.get(thisNode);
// //                  if (originalNode == null) {
// //                    System.out.println("break");
// //                  }
//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteUpper.setAsPathSize(route.getAsPathSize().getUpperbound() + 1,
//                       originalRouteUpper.getAsPathSize().getUpperbound());
//                   originalRouteUpper._reason = Reason.ADD;
//                   route._reason = Reason.ADD;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteUpper.getIndex(),
//                       originalRouteUpper.getAsPath().hashCode(),
//                       originalRouteUpper.getAsPathSize().hashCode(),
//                       originalRouteUpper.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);

//                   _originalNodePairs.remove(thisNode);
//                   _addTreeNodes.get(originalNode).remove(thisNode);

//                   addQueue.add(route);
//                   addQueue.add(originalRouteUpper);
//                   AddRoute(addQueue, routeNetWork);
// //                  System.out.println("break");
//                 } else if (originalRoute._reason == Reason.UPDATE) {
//                   TreeNode originalNode = _originalNodePairs.getOrDefault(thisNode,thisNode);
// //                  if (originalNode == null) {
// //                    System.out.println("break");
// //                  }
//                   _updateTreeNodes.computeIfAbsent(originalNode, k -> new HashSet<>());
//                   _updateTreeNodes.get(originalNode).remove(thisNode);

//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteUpper.setAsPathSize(route.getAsPathSize().getUpperbound() + 1,
//                       originalRouteUpper.getAsPathSize().getUpperbound());
//                   originalRouteUpper._reason = Reason.UPDATE;
//                   route._reason = Reason.UPDATE;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteUpper.getIndex(),
//                       originalRouteUpper.getAsPath().hashCode(),
//                       originalRouteUpper.getAsPathSize().hashCode(),
//                       originalRouteUpper.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);

//                   _originalNodePairs.remove(thisNode);
//                   addQueue.add(route);
//                   addQueue.add(originalRouteUpper);
//                   AddRoute(addQueue, routeNetWork);
// //                  System.out.println("break");
//                 }
//               } else if (originalRoute.getAsPathSize().getUpperbound() == route.getAsPathSize()
//                   .getUpperbound()) {
//                 SymBgpRoute originalRouteLower = cloneBgpRoute(originalRoute);
//                 TreeNode thisNode = new TreeNode(
//                     _nodeName,
//                     originalRoute.getIndex(),
//                     originalRoute.getAsPath().hashCode(),
//                     originalRoute.getAsPathSize().hashCode(),
//                     originalRoute.getAsPathSize(),route.getAsPath());

//                 if (originalRoute._reason == Reason.NORMAL) {
//                   TreeNode originalNode = thisNode;
//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteLower.setAsPathSize(originalRouteLower.getAsPathSize().getLowerbound(),
//                       route.getAsPathSize().getLowerbound() - 1);
//                   originalRouteLower._reason = Reason.UPDATE;
//                   route._reason = Reason.UPDATE;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteLower.getIndex(),
//                       originalRouteLower.getAsPath().hashCode(),
//                       originalRouteLower.getAsPathSize().hashCode(),
//                       originalRouteLower.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);

//                   addQueue.add(originalRouteLower);
//                   addQueue.add(route);
//                   AddRoute(addQueue, routeNetWork);
//                 } else if (originalRoute._reason == Reason.ADD) {
//                   TreeNode originalNode = _originalNodePairs.get(thisNode);

// //                  if (originalNode == null) {
// //                    System.out.println("break");
// //                  }

//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteLower.setAsPathSize(originalRouteLower.getAsPathSize().getLowerbound(),
//                       route.getAsPathSize().getLowerbound() - 1);
//                   originalRouteLower._reason = Reason.ADD;
//                   route._reason = Reason.ADD;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteLower.getIndex(),
//                       originalRouteLower.getAsPath().hashCode(),
//                       originalRouteLower.getAsPathSize().hashCode(),
//                       originalRouteLower.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);

//                   _originalNodePairs.remove(thisNode);
//                   _addTreeNodes.get(originalNode).remove(thisNode);

//                   addQueue.add(originalRouteLower);
//                   addQueue.add(route);
//                   AddRoute(addQueue, routeNetWork);
//                 } else if (originalRoute._reason == Reason.UPDATE) {
//                   TreeNode originalNode = _originalNodePairs.getOrDefault(thisNode,thisNode);
//                   _updateTreeNodes.computeIfAbsent(originalNode, k -> new HashSet<>());
//                   if (originalNode == null) {
//                     System.out.println("break");
//                   }
//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteLower.setAsPathSize(originalRouteLower.getAsPathSize().getLowerbound(),
//                       route.getAsPathSize().getLowerbound() - 1);
//                   originalRouteLower._reason = Reason.UPDATE;
//                   route._reason = Reason.UPDATE;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteLower.getIndex(),
//                       originalRouteLower.getAsPath().hashCode(),
//                       originalRouteLower.getAsPathSize().hashCode(),
//                       originalRouteLower.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);

//                   _originalNodePairs.remove(thisNode);
//                   if(!_updateTreeNodes.containsKey(originalNode))
//                   {
//                     System.out.println("break");
//                   }
//                   _updateTreeNodes.get(originalNode).remove(thisNode);

//                   addQueue.add(originalRouteLower);
//                   addQueue.add(route);
//                   AddRoute(addQueue, routeNetWork);
//                 }
//               } else {
//                 TreeNode thisNode = new TreeNode(
//                     _nodeName,
//                     originalRoute.getIndex(),
//                     originalRoute.getAsPath().hashCode(),
//                     originalRoute.getAsPathSize().hashCode(),
//                     originalRoute.getAsPathSize(),route.getAsPath());
//                 SymBgpRoute originalRouteLower = cloneBgpRoute(originalRoute);
//                 SymBgpRoute originalRouteUpper = cloneBgpRoute(originalRoute);
//                 if (originalRoute._reason == Reason.NORMAL) {
//                   TreeNode originalNode = thisNode;

//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteLower.setAsPathSize(originalRouteLower.getAsPathSize().getLowerbound(),
//                       route.getAsPathSize().getLowerbound() - 1);
//                   originalRouteUpper.setAsPathSize(route.getAsPathSize().getUpperbound() + 1,
//                       originalRouteUpper.getAsPathSize().getUpperbound());
//                   originalRouteLower._reason = Reason.UPDATE;
//                   originalRouteUpper._reason = Reason.UPDATE;
//                   route._reason = Reason.UPDATE;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteUpper.getIndex(),
//                       originalRouteUpper.getAsPath().hashCode(),
//                       originalRouteUpper.getAsPathSize().hashCode(),
//                       originalRouteUpper.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteLower.getIndex(),
//                       originalRouteLower.getAsPath().hashCode(),
//                       originalRouteLower.getAsPathSize().hashCode(),
//                       originalRouteLower.getAsPathSize(),route.getAsPath()), originalNode);

//                   addQueue.add(originalRouteLower);
//                   addQueue.add(route);
//                   addQueue.add(originalRouteUpper);
//                   AddRoute(addQueue, routeNetWork);
//                 } else if (originalRoute._reason == Reason.ADD) {
//                   TreeNode originalNode = _originalNodePairs.get(thisNode);
// //                  if (originalNode == null) {
// //                    System.out.println("break");
// //                  }

//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteLower.setAsPathSize(originalRouteLower.getAsPathSize().getLowerbound(),
//                       route.getAsPathSize().getLowerbound() - 1);
//                   originalRouteUpper.setAsPathSize(route.getAsPathSize().getUpperbound() + 1,
//                       originalRouteUpper.getAsPathSize().getUpperbound());
//                   originalRouteLower._reason = Reason.ADD;
//                   originalRouteUpper._reason = Reason.ADD;
//                   route._reason = Reason.ADD;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteUpper.getIndex(),
//                       originalRouteUpper.getAsPath().hashCode(),
//                       originalRouteUpper.getAsPathSize().hashCode(),
//                       originalRouteUpper.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteLower.getIndex(),
//                       originalRouteLower.getAsPath().hashCode(),
//                       originalRouteLower.getAsPathSize().hashCode(),
//                       originalRouteLower.getAsPathSize(),route.getAsPath()), originalNode);

//                   _originalNodePairs.remove(thisNode);
//                   _addTreeNodes.get(originalNode).remove(thisNode);

//                   addQueue.add(originalRouteLower);
//                   addQueue.add(route);
//                   addQueue.add(originalRouteUpper);
//                   AddRoute(addQueue, routeNetWork);
//                 } else if (originalRoute._reason == Reason.UPDATE) {
//                   TreeNode originalNode = _originalNodePairs.getOrDefault(thisNode,thisNode);
//                   _updateTreeNodes.computeIfAbsent(originalNode, k -> new HashSet<>());
// //                  if (originalNode == null) {
// //                    System.out.println("break");
// //                  }
//                   Queue<SymBgpRoute> addQueue = new LinkedList<>();
//                   _rib.get(routeNetWork).get(location).remove(index);
//                   if (_rib.get(routeNetWork).get(location).size() == 0) {
//                     _rib.get(routeNetWork).remove(location);
//                   }
//                   originalRouteLower.setAsPathSize(originalRouteLower.getAsPathSize().getLowerbound(),
//                       route.getAsPathSize().getLowerbound() - 1);
//                   originalRouteUpper.setAsPathSize(route.getAsPathSize().getUpperbound() + 1,
//                       originalRouteUpper.getAsPathSize().getUpperbound());
//                   originalRouteLower._reason = Reason.UPDATE;
//                   originalRouteUpper._reason = Reason.UPDATE;
//                   route._reason = Reason.UPDATE;

//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteUpper.getIndex(),
//                       originalRouteUpper.getAsPath().hashCode(),
//                       originalRouteUpper.getAsPathSize().hashCode(),
//                       originalRouteUpper.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(_nodeName,
//                       route.getIndex(),
//                       route.getAsPath().hashCode(),
//                       route.getAsPathSize().hashCode(),
//                       route.getAsPathSize(),route.getAsPath()), originalNode);
//                   _originalNodePairs.put(new TreeNode(
//                       _nodeName,
//                       originalRouteLower.getIndex(),
//                       originalRouteLower.getAsPath().hashCode(),
//                       originalRouteLower.getAsPathSize().hashCode(),
//                       originalRouteLower.getAsPathSize(),route.getAsPath()), originalNode);

//                   _originalNodePairs.remove(thisNode);
//                   _updateTreeNodes.get(originalNode).remove(thisNode);

//                   addQueue.add(originalRouteLower);
//                   addQueue.add(route);
//                   addQueue.add(originalRouteUpper);
//                   AddRoute(addQueue, routeNetWork);
//                 }
//               }
//             }else if(route.getAsPathSize().getLowerbound()<=originalRoute.getAsPathSize().getLowerbound()&&route.getAsPathSize().getUpperbound()>=originalRoute.getAsPathSize().getUpperbound()){
//               if(originalRoute.getAsPathSize().getLowerbound()==route.getAsPathSize().getLowerbound())
//               {
//                 SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//                 addRouteUpper.setAsPathSize(originalRoute.getAsPathSize().getUpperbound()+1,route.getAsPathSize().getUpperbound());
//                 route.setAsPathSize(route.getAsPathSize().getLowerbound(),originalRoute.getAsPathSize().getUpperbound());
//                 updateQueue.add(route);
//                 updateQueue.add(addRouteUpper);
//               }else if(originalRoute.getAsPathSize().getUpperbound()==route.getAsPathSize().getUpperbound())
//               {
//                 SymBgpRoute addRouteLower=cloneBgpRoute(route);
//                 addRouteLower.setAsPathSize(route.getAsPathSize().getLowerbound(),originalRoute.getAsPathSize().getLowerbound()-1);
//                 route.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),route.getAsPathSize().getUpperbound());
//                 updateQueue.add(route);
//                 updateQueue.add(addRouteLower);
//               }else {
//                 SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//                 SymBgpRoute addRouteLower=cloneBgpRoute(route);
//                 addRouteUpper.setAsPathSize(originalRoute.getAsPathSize().getUpperbound()+1,route.getAsPathSize().getUpperbound());
//                 addRouteLower.setAsPathSize(route.getAsPathSize().getLowerbound(),originalRoute.getAsPathSize().getLowerbound()-1);
//                 route.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),originalRoute.getAsPathSize().getUpperbound());
//                 updateQueue.add(route);
//                 updateQueue.add(addRouteUpper);
//                 updateQueue.add(addRouteLower);
//               }
//             }else if(originalRoute.getAsPathSize().getUpperbound()>=route.getAsPathSize().getUpperbound()&&originalRoute.getAsPathSize().getLowerbound()>=route.getAsPathSize().getLowerbound())
//             {
//               SymBgpRoute addRouteLower=cloneBgpRoute(route);
//               addRouteLower.setAsPathSize(route.getAsPathSize().getLowerbound(),originalRoute.getAsPathSize().getLowerbound()-1);
//               route.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),route.getAsPathSize().getUpperbound());
//               updateQueue.add(route);
//               updateQueue.add(addRouteLower);
//             }else{
//               SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//               addRouteUpper.setAsPathSize(originalRoute.getAsPathSize().getUpperbound()+1,route.getAsPathSize().getUpperbound());
//               route.setAsPathSize(route.getAsPathSize().getLowerbound(),originalRoute.getAsPathSize().getUpperbound());
//               updateQueue.add(route);
//               updateQueue.add(addRouteUpper);
//             }
//           }
//         }
//       }
//     }
//     return newAdd;
//   }

//   public int GetChanged()
//   {
//     return this._simplifiedChangePrefix.size();
//   }

//   public int findMin(BDD expression)
//   {
//     //    List<Object> allSolutions = expression.allsat();
//     int minFalseVariables = Integer.MAX_VALUE;
// //    System.out.println("step:find all solution:");
//     expression.printSet();

// //    System.out.println("step:find min formal size:"+expression.allsat().size());
// //    System.out.println("step:find all solution:"+expression.allsat().toString());
//     for (Object solution : expression.allsat()) {
//       byte[] assignment = (byte[]) solution;
//       int falseCount = 0;
//       for (byte b : assignment) {
//         if (b == 0) {
//           falseCount++;
//         }
//       }
//       minFalseVariables = Math.min(minFalseVariables, falseCount);
//       if(minFalseVariables<=this._k)
//       {
//         break;
//       }
//     }
//     return minFalseVariables;
//   }

//   //filter update
// //  public void computeChangedRoute()
// //  {
// //    List<SymBgpRoute> answer=new ArrayList<>();
// //    answer.addAll(_withDrawRoute);
// //    //new topologyCondition update
// //    for(Integer prefix:_simplifiedChangePrefix)
// //    {
// //      List<List<SymBgpRoute>> routes=_rib.get(prefix);
// //      boolean changed=false;
// //      BDD tc=_topologyfactory.one();
// //      for(int i=0;i<routes.size();i++)
// //      {
// //        List<SymBgpRoute> routeList=routes.get(i);
// //        if(tc.isZero())
// //        {
// //          Iterator<SymBgpRoute> iterator=routeList.iterator();
// //          while(iterator.hasNext())
// //          {
// //            SymBgpRoute bgpRoute=iterator.next();
// //            SymBgpRoute outputRoute=cloneBgpRoute(bgpRoute);
// //            if(bgpRoute._reason==Reason.ADD)
// //            {
// //              iterator.remove();
// //              continue;
// //            }
// //            outputRoute._reason=Reason.WITHDRAW;
// //            answer.add(outputRoute);
// //            iterator.remove();
// //          }
// //        }else if(!changed)
// //        {
// ////          System.out.println("step:!chaged");
// //          Iterator<SymBgpRoute> iterator=routeList.iterator();
// //          while(iterator.hasNext())
// //          {
// //            SymBgpRoute symBgpRoute=iterator.next();
// //            if(!(symBgpRoute.getReason()==Reason.NORMAL))
// //            {
// //              changed=true;
// //              SymBgpRoute outputRoute=cloneBgpRoute(symBgpRoute);
// //              BDD outTc=_topologyfactory.one();
// //              //这一部分主要处理的情况是：对于两个来自于同一个外部邻居的路由，如果是某一个路由分割开的两个路由，两个路由的拓扑条件应该不相互影响。
// ////              System.out.println("step:!changed compute filter tc");
// //              if(outputRoute._external)
// //              {
// //                BDD tempTc=_topologyfactory.one();
// //                for(int j=0;j<i;j++)
// //                {
// //                  List<SymBgpRoute> routeListTemp=routes.get(j);
// //                  for(SymBgpRoute routeTemp:routeListTemp)
// //                  {
// //                    if(routeTemp.getOriginatorIp().equals(outputRoute.getOriginatorIp())&&routeTemp.getNodePath().equals(outputRoute.getNodePath()))
// //                    {
// //                      continue;
// //                    }
// //                    tempTc=tempTc.and(routeTemp.getTopologyCondition().not());
// //                  }
// //                }
// //                outTc=tempTc.and(outputRoute.getTopologyCondition());
// //                tempTc.free();
// //              }else{
// //                outTc=tc.and(outputRoute.getTopologyCondition());
// //              }
// ////              System.out.println("step:!changed compute filter tc-end");
// ////              System.out.println("step:!changed filter");
// //              outTc=outTc.and(_topologyFilter);
// //              if(outTc.isZero())
// //              {
// //                if(symBgpRoute._reason==Reason.ADD)
// //                {
// ////                  System.out.println("step:!changed filter end1");
// //                  iterator.remove();
// //                  continue;
// //                }
// //                outputRoute._reason=Reason.WITHDRAW;
// //                answer.add(outputRoute);
// //                iterator.remove();
// ////                System.out.println("step:!changed filter end2");
// //                continue;
// //              }else{
// //                outputRoute.setTopologyCondition(outTc);
// //                outputRoute._reason=symBgpRoute.getReason()==Reason.ADD?Reason.ADD:Reason.UPDATE;
// //              }
// ////              System.out.println("step:!changed filter end3");
// //              if(symBgpRoute._outBGPTopologyCondition!=null&&outTc.equals(symBgpRoute._outBGPTopologyCondition))
// //              {
// //                continue;
// //              }
// //              symBgpRoute._outBGPTopologyCondition=outTc;
// //              answer.add(outputRoute);
// //            }else{
// //              symBgpRoute._outBGPTopologyCondition= symBgpRoute.getTopologyCondition().and(tc);
// //            }
// //          }
// ////          System.out.println("step:!chaged-end");
// //        }else{
// //          Iterator<SymBgpRoute> iterator=routeList.iterator();
// ////          System.out.println("step:chaged");
// //          while(iterator.hasNext())
// //          {
// //            SymBgpRoute symBgpRoute=iterator.next();
// //            SymBgpRoute outputRoute=cloneBgpRoute(symBgpRoute);
// //            BDD outTc=_topologyfactory.one();
// //            //这一部分主要处理的情况是：对于两个来自于同一个外部邻居的路由，如果是某一个路由分割开的两个路由，两个路由的拓扑条件应该不相互影响。
// ////            System.out.println("step:changed compute filter tc");
// //            if(outputRoute._external)
// //            {
// //              BDD tempTc=_topologyfactory.one();
// //              for(int j=0;j<i;j++)
// //              {
// //                List<SymBgpRoute> routeListTemp=routes.get(j);
// //                for(SymBgpRoute routeTemp:routeListTemp)
// //                {
// //                  if(routeTemp.getOriginatorIp().equals(outputRoute.getOriginatorIp())&&routeTemp.getNodePath().equals(outputRoute.getNodePath()))
// //                  {
// //                    continue;
// //                  }
// //                  tempTc=tempTc.and(routeTemp.getTopologyCondition().not());
// //                }
// //              }
// //              outTc=tempTc.and(outputRoute.getTopologyCondition());
// //            }else{
// //              outTc=tc.and(outputRoute.getTopologyCondition());
// //            }
// ////            System.out.println("step:changed compute filter tc end");
// //
// ////            System.out.println("step:changed filter-k");
// //            outTc=outTc.and(_topologyFilter);
// //            if(outTc.isZero())
// //            {
// //              outputRoute._reason=Reason.WITHDRAW;
// //              outTc.free();
// //              if(symBgpRoute._reason==Reason.ADD)
// //              {
// //                iterator.remove();
// ////                System.out.println("step:changed filter-end1");
// //                continue;
// //              }
// //              answer.add(outputRoute);
// //              iterator.remove();
// ////              System.out.println("step:changed filter-end2");
// //              continue;
// //            }
// ////            System.out.println("step:changed filter-end3");
// //            if(symBgpRoute._outBGPTopologyCondition!=null&&outTc.equals(symBgpRoute._outBGPTopologyCondition))
// //            {
// //              continue;
// //            }
// //            if(symBgpRoute.getReason()!=Reason.ADD)
// //            {
// //              outputRoute._reason=Reason.UPDATE;
// //            }else{
// //              outputRoute._reason=Reason.ADD;
// //            }
// //            outputRoute.setTopologyCondition(outTc);
// //            symBgpRoute._outBGPTopologyCondition=outTc;
// //            answer.add(outputRoute);
// //          }
// ////          System.out.println("step:chaged-end");
// //        }
// //        if(i==(routes.size()-1))
// //        {
// //          continue;
// //        }
// ////        System.out.println("step:compute-tc");
// //        BDD tcTemp=_topologyfactory.zero();
// //        for (SymBgpRoute symBgpRoute:routeList) {
// //          //zhe li ka zhu le
// //          //tcTemp.printSet();
// //          tcTemp=tcTemp.or(symBgpRoute.getTopologyCondition());
// //        }
// ////        System.out.println("step:middle-compute-tc");
// //        tc=tc.and(tcTemp.not());
// //        tcTemp.free();
// ////        System.out.println("step:compute-tc-end");
// //      }
// //      routes.removeIf(List::isEmpty);
// ////      for(int i=routes.size()-1;i>=0;i--) {
// ////        List<SymBgpRoute> routeList = routes.get(i);
// ////        if(routeList.size()==0)
// ////        {
// ////          _rib.get(prefix).remove(i);
// ////        }
// ////      }
// //      tc.free();
// //    }
// //    _changedRoute=answer;
// //    return ;
// //  }

//   public void computeChangedRoute()
//   {
//     long timestampstart = System.currentTimeMillis();
//     long t=0;
//     long tt=0;
//     long tt1=0;
//     List<SymBgpRoute> answer=new ArrayList<>();
//     //new topologyCondition update
//     for(Integer prefix:_simplifiedChangePrefix)
//     {
//       List<List<SymBgpRoute>> routes=_rib.get(prefix);
//       boolean changed=false;
//       int tc=1;
//       for(int i=0;i<routes.size();i++)
//       {
//         List<SymBgpRoute> routeList=routes.get(i);
//         if(tc==0)
//         {
//           Iterator<SymBgpRoute> iterator=routeList.iterator();
//           while(iterator.hasNext())
//           {
//             SymBgpRoute bgpRoute=iterator.next();
//             SymBgpRoute outputRoute=cloneBgpRoute(bgpRoute);
//             if(bgpRoute._reason==Reason.ADD)
//             {
//               if(!_deleteTreeNodes.containsKey(bgpRoute.getIndex()))
//               {
//                 _deleteTreeNodes.put(bgpRoute.getIndex(), new HashSet<>());
//               }
//               TreeNode deleteNode=new TreeNode(_nodeName,
//                   bgpRoute.getIndex(), bgpRoute.getAsPath().hashCode(),bgpRoute.getAsPathSize().hashCode(),bgpRoute.getAsPathSize(),bgpRoute.getAsPath());
//               deleteNode.prefixEc=bgpRoute.getPrefixEcNum();
//               if(bgpRoute._external)
//               {
//                   deleteNode.external=true;
//               }
//               _deleteTreeNodes.get(bgpRoute.getIndex()).add(deleteNode);
//               _topologyBDD.deref(bgpRoute.getTopologyCondition());
//               _topologyBDD.deref(bgpRoute._outBGPTopologyCondition);
//               _topologyBDD.deref(bgpRoute._notTopologyCondition);
//               _topologyBDD.deref(outputRoute.getTopologyCondition());
//               _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//               iterator.remove();
//               continue;
//             }
// //            outputRoute._reason=Reason.WITHDRAW;
// //            answer.add(outputRoute);
//             _withDrawRoute.add(outputRoute);
//             _topologyBDD.deref(bgpRoute.getTopologyCondition());
//             _topologyBDD.deref(bgpRoute._outBGPTopologyCondition);
//             _topologyBDD.deref(bgpRoute._notTopologyCondition);
//             _topologyBDD.deref(bgpRoute._lastUnchangedCndition);
//             _topologyBDD.deref(outputRoute.getTopologyCondition());
//             _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//             iterator.remove();
//           }
//         }else if(!changed)
//         {
//           //          System.out.println("step:!chaged");
//           Iterator<SymBgpRoute> iterator=routeList.iterator();
//           while(iterator.hasNext())
//           {
//             SymBgpRoute symBgpRoute=iterator.next();
//             if(!(symBgpRoute.getReason()==Reason.NORMAL))
//             {
// //              System.out.println("changed-pos:"+i+"; rib-size:"+routes.size());
//               changed=true;
//               if(i==0)
//               {
//                 tc=1;
//               }else{
//                 int tcUnchanegd = routes.get(i-1).get(0)._lastUnchangedCndition;
//                 for(SymBgpRoute route:routes.get(i-1))
//                 {
//                   int not=0;
//                   if(route._notTopologyCondition==0)
//                   {
//                     not = _topologyBDD.ref(_topologyBDD.not(route.getTopologyCondition()));
//                     route._notTopologyCondition=not;
//                   }
//                   not=route._notTopologyCondition;
//                   tcUnchanegd = andBDD(tcUnchanegd,not);
//                 }
//                 _topologyBDD.ref(tcUnchanegd);
//                 tc=tcUnchanegd;
//               }
//               SymBgpRoute outputRoute=cloneBgpRoute(symBgpRoute);
//               int outTc=1;
//               //这一部分主要处理的情况是：对于两个来自于同一个外部邻居的路由，如果是某一个路由分割开的两个路由，两个路由的拓扑条件应该不相互影响。
//               //              System.out.println("step:!changed compute filter tc");
//               if(outputRoute._external)
//               {
//                 int tempTc=1;
//                 for(int j=0;j<i;j++)
//                 {
//                   List<SymBgpRoute> routeListTemp=routes.get(j);
//                   for(SymBgpRoute routeTemp:routeListTemp)
//                   {
//                     if(routeTemp.getOriginatorIp()==null)
//                     {
//                       continue;
//                     }
//                     if(routeTemp.getOriginatorIp().equals(outputRoute.getOriginatorIp())&&routeTemp.getNodePath().equals(outputRoute.getNodePath()))
//                     {
//                       continue;
//                     }
//                     int tempTc1 = _topologyBDD.ref(andBDD(tempTc,_topologyBDD.not(routeTemp.getTopologyCondition())));
// //                    int tempTc1= _topologyBDD.ref(_topologyBDD.and(tempTc,_topologyBDD.not(routeTemp.getTopologyCondition())));
//                     _topologyBDD.deref(tempTc);
//                     tempTc=tempTc1;
//                   }
//                 }
// //                int tempOutTc=_topologyBDD.ref(_topologyBDD.and(tempTc,outputRoute.getTopologyCondition()));
//                 int tempOutTc=_topologyBDD.ref(andBDD(tempTc,outputRoute.getTopologyCondition()));
//                 _topologyBDD.deref(outTc);
//                 outTc=tempOutTc;
//                 _topologyBDD.deref(tempTc);
//               }else{
// //                int tempOutTc=_topologyBDD.ref(_topologyBDD.and(tc,outputRoute.getTopologyCondition()));
//                 int tempOutTc=_topologyBDD.ref(andBDD(tc,outputRoute.getTopologyCondition()));
//                 _topologyBDD.deref(outTc);
//                 outTc=tempOutTc;
//               }
//               //              System.out.println("step:!changed compute filter tc-end");
//               //              System.out.println("step:!changed filter");
// //              int tempOutTc=_topologyBDD.ref(_topologyBDD.and(outTc,_topologyFilter));
//               int tempOutTc=_topologyBDD.ref(andBDD(outTc,_topologyFilter));
//               _topologyBDD.deref(outTc);
//               outTc=tempOutTc;
//               if(outTc==0)
//               {
//                 if(symBgpRoute._reason==Reason.ADD)
//                 {
//                   //                  System.out.println("step:!changed filter end1");
//                   if(!_deleteTreeNodes.containsKey(symBgpRoute.getIndex()))
//                   {
//                     _deleteTreeNodes.put(symBgpRoute.getIndex(), new HashSet<>());
//                   }
//                   TreeNode deleteNode=new TreeNode(_nodeName,
//                       symBgpRoute.getIndex(), symBgpRoute.getAsPath().hashCode(),symBgpRoute.getAsPathSize().hashCode(),symBgpRoute.getAsPathSize(),symBgpRoute.getAsPath());
//                   deleteNode.prefixEc= symBgpRoute.getPrefixEcNum();
//                   if(symBgpRoute._external)
//                   {
//                     deleteNode.external=true;
//                   }
//                   _deleteTreeNodes.get(symBgpRoute.getIndex()).add(deleteNode);

//                   _topologyBDD.deref(symBgpRoute.getTopologyCondition());
//                   _topologyBDD.deref(symBgpRoute._outBGPTopologyCondition);
//                   _topologyBDD.deref(outputRoute.getTopologyCondition());
//                   _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//                   iterator.remove();
//                   continue;
//                 }
// //                outputRoute._reason=Reason.WITHDRAW;
// //                answer.add(outputRoute);
//                 _withDrawRoute.add(outputRoute);
//                 _topologyBDD.deref(symBgpRoute.getTopologyCondition());
//                 _topologyBDD.deref(symBgpRoute._outBGPTopologyCondition);
//                 _topologyBDD.deref(symBgpRoute._notTopologyCondition);
//                 _topologyBDD.deref(symBgpRoute._lastUnchangedCndition);
//                 _topologyBDD.deref(outputRoute.getTopologyCondition());
//                 _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//                 iterator.remove();
//                 //                System.out.println("step:!changed filter end2");
//                 continue;
//               }else{
//                 _topologyBDD.deref(outputRoute.getTopologyCondition());
//                 outputRoute.setTopologyCondition(outTc);
//                 outputRoute._reason=symBgpRoute.getReason()==Reason.ADD?Reason.ADD:Reason.UPDATE;
//               }
//               //              System.out.println("step:!changed filter end3");
// //              if(symBgpRoute._outBGPTopologyCondition!=null&&outTc.equals(symBgpRoute._outBGPTopologyCondition))
// //              {
// //                continue;
// //              }
//               if(outTc==symBgpRoute._outBGPTopologyCondition)
//               {
//                 _topologyBDD.deref(outputRoute.getTopologyCondition());
//                 _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//                 continue;
//               }
//               _topologyBDD.ref(tc);
//               symBgpRoute._lastUnchangedCndition=tc;
//               symBgpRoute._outBGPTopologyCondition=outTc;
//               answer.add(outputRoute);
//             }else{
//               //geng gai
// ////              int tempOutTc=_topologyBDD.ref(_topologyBDD.and(symBgpRoute.getTopologyCondition(),tc));
// //              int tempOutTc=_topologyBDD.ref(andBDD(symBgpRoute.getTopologyCondition(),tc));
// ////              _topologyBDD.deref(outTc);
// //              _topologyBDD.deref(symBgpRoute.getTopologyCondition());
// //              symBgpRoute._outBGPTopologyCondition=tempOutTc;
//             }
//           }
//           //          System.out.println("step:!chaged-end");
//         }else{
//           Iterator<SymBgpRoute> iterator=routeList.iterator();
//           //          System.out.println("step:chaged");
//           while(iterator.hasNext())
//           {
//             SymBgpRoute symBgpRoute=iterator.next();
//             SymBgpRoute outputRoute=cloneBgpRoute(symBgpRoute);
//             int outTc=1;
//             //这一部分主要处理的情况是：对于两个来自于同一个外部邻居的路由，如果是某一个路由分割开的两个路由，两个路由的拓扑条件应该不相互影响。
//             //            System.out.println("step:changed compute filter tc");
//             if(outputRoute._external)
//             {
//               int tempTc=1;
//               for(int j=0;j<i;j++)
//               {
//                 List<SymBgpRoute> routeListTemp=routes.get(j);
//                 for(SymBgpRoute routeTemp:routeListTemp)
//                 {
//                   if(routeTemp.getOriginatorIp().equals(outputRoute.getOriginatorIp())&&routeTemp.getNodePath().equals(outputRoute.getNodePath()))
//                   {
//                     continue;
//                   }
// //                  int tempTc1=_topologyBDD.ref(_topologyBDD.and(tempTc,_topologyBDD.not(routeTemp.getTopologyCondition())));
//                   int tempTc1=_topologyBDD.ref(andBDD(tempTc,_topologyBDD.not(routeTemp.getTopologyCondition())));
//                   _topologyBDD.deref(tempTc);
//                   tempTc=tempTc1;
//                 }
//               }
// //              int tempOutTc=_topologyBDD.ref(_topologyBDD.and(tempTc,outputRoute.getTopologyCondition()));
//               int tempOutTc=_topologyBDD.ref(andBDD(tempTc,outputRoute.getTopologyCondition()));
//               _topologyBDD.deref(outTc);
//               outTc=tempOutTc;
//               _topologyBDD.deref(tempTc);
//             }else{
//               int tempOutTc=_topologyBDD.ref(andBDD(tc, outputRoute.getTopologyCondition()));
// //              try {
// //                tempOutTc = _topologyBDD.ref(_topologyBDD.and(tc, outputRoute.getTopologyCondition()));
// //              }catch (Exception e)
// //              {
// //                e.printStackTrace();
// //              }
//               _topologyBDD.deref(outTc);
//               outTc=tempOutTc;
//             }
//             //            System.out.println("step:changed compute filter tc end");

//             //            System.out.println("step:changed filter-k");
// //            int tempOutTc=_topologyBDD.ref(_topologyBDD.and(outTc,_topologyFilter));
//             int tempOutTc=_topologyBDD.ref(andBDD(outTc,_topologyFilter));
//             _topologyBDD.deref(outTc);
//             outTc=tempOutTc;
//             if(outTc==0)
//             {
//               outputRoute._reason=Reason.WITHDRAW;
//               _topologyBDD.deref(outTc);
//               if(symBgpRoute._reason==Reason.ADD)
//               {
//                 if(!_deleteTreeNodes.containsKey(symBgpRoute.getIndex()))
//                 {
//                   _deleteTreeNodes.put(symBgpRoute.getIndex(), new HashSet<>());
//                 }
//                 TreeNode deleteNode=new TreeNode(_nodeName,
//                     symBgpRoute.getIndex(), symBgpRoute.getAsPath().hashCode(),symBgpRoute.getAsPathSize().hashCode(),symBgpRoute.getAsPathSize(),symBgpRoute.getAsPath());
//                 deleteNode.prefixEc= symBgpRoute.getPrefixEcNum();
//                 if(symBgpRoute._external)
//                 {
//                   deleteNode.external=true;
//                 }
//                 _deleteTreeNodes.get(symBgpRoute.getIndex()).add(deleteNode);
//                 //                System.out.println("step:changed filter-end1");
//                 _topologyBDD.deref(outputRoute.getTopologyCondition());
//                 _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//                 _topologyBDD.deref(symBgpRoute.getTopologyCondition());
//                 _topologyBDD.deref(symBgpRoute._outBGPTopologyCondition);
//                 _topologyBDD.deref(symBgpRoute._notTopologyCondition);
//                 _topologyBDD.deref(symBgpRoute._lastUnchangedCndition);
//                 iterator.remove();
//                 continue;
//               }
// //              answer.add(outputRoute);
// //              _topologyBDD.deref(outputRoute.getTopologyCondition());
// //              _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//               _topologyBDD.deref(symBgpRoute.getTopologyCondition());
//               _topologyBDD.deref(symBgpRoute._outBGPTopologyCondition);
//               _topologyBDD.deref(symBgpRoute._notTopologyCondition);
//               _topologyBDD.deref(outputRoute.getTopologyCondition());
//               _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//               _topologyBDD.deref(symBgpRoute._lastUnchangedCndition);
//               _withDrawRoute.add(outputRoute);
//               iterator.remove();
//               //              System.out.println("step:changed filter-end2");
//               continue;
//             }
//             //            System.out.println("step:changed filter-end3");
//             if(outTc==symBgpRoute._outBGPTopologyCondition)
//             {
//               _topologyBDD.deref(outputRoute.getTopologyCondition());
//               _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//               continue;
//             }
//             if(symBgpRoute.getReason()!=Reason.ADD)
//             {
//               outputRoute._reason=Reason.UPDATE;
//             }else{
//               outputRoute._reason=Reason.ADD;
//             }
//             outputRoute.setTopologyCondition(outTc);
//             symBgpRoute._outBGPTopologyCondition=outTc;
//             _topologyBDD.ref(tc);
//             symBgpRoute._lastUnchangedCndition=tc;
//             answer.add(outputRoute);
//           }
//           //          System.out.println("step:chaged-end");
//         }
//         if(i==(routes.size()-1))
//         {
//           continue;
//         }
//         //        System.out.println("step:compute-tc");
//         int tcTemp=0;
//         long t1 = System.currentTimeMillis();
//         int tcnew=1;
//         if(changed)
//         {
//           for (SymBgpRoute symBgpRoute:routeList) {
//             //zhe li ka zhu le
//             //tcTemp.printSet();
//             long t3 = System.currentTimeMillis();
//             if(symBgpRoute._notTopologyCondition==0)
//             {
//               symBgpRoute._notTopologyCondition=_topologyBDD.ref(_topologyBDD.not(symBgpRoute.getTopologyCondition()));
//             }
//             int not=symBgpRoute._notTopologyCondition;
//             long t4 = System.currentTimeMillis();
//             tt=tt+t4-t3;
//             long t5 = System.currentTimeMillis();
//             int tcTemp2=_topologyBDD.ref(andBDD(tc,not));
//             long t6 = System.currentTimeMillis();
//             tt1=tt1+t6-t5;
//             _topologyBDD.deref(tc);
//             tc=tcTemp2;
//           }
//         }
// //        for (SymBgpRoute symBgpRoute:routeList) {
// //          //zhe li ka zhu le
// //          //tcTemp.printSet();
// //          int tcTemp2=_topologyBDD.ref(_topologyBDD.or(tcTemp,symBgpRoute.getTopologyCondition()));
// //          _topologyBDD.deref(tcTemp);
// //          tcTemp=tcTemp2;
// //        }
//         //        System.out.println("step:middle-compute-tc");
// //        int tc2=_topologyBDD.ref(_topologyBDD.and(tc,_topologyBDD.not(tcTemp)));

// //        int tc2=_topologyBDD.ref(andBDD(_topologyFilter,andBDD(tc,tcnew)));
// //        _topologyBDD.deref(tc);
// //        tc=tc2;
// //        _topologyBDD.deref(tcTemp);
//         long t2 = System.currentTimeMillis();
//         t=t+t2-t1;
//         //        System.out.println("step:compute-tc-end");
//       }
//       routes.removeIf(List::isEmpty);
//       //      for(int i=routes.size()-1;i>=0;i--) {
//       //        List<SymBgpRoute> routeList = routes.get(i);
//       //        if(routeList.size()==0)
//       //        {
//       //          _rib.get(prefix).remove(i);
//       //        }
//       //      }
//       _topologyBDD.deref(tc);
//     }
//     _changedRoute=answer;
// //    if (t>1500)
// //    {
// //      System.out.println("computeChangedbreak");
// //    }
//     _rib
//         .values()
//         //        .parallelStream()
//         .forEach(
//             routeList->{
//               routeList
//                   .parallelStream()
//                   .forEach(
//                       routes->{
//                         routes
//                             .forEach(
//                                 route->{
//                                   route._reason=Reason.NORMAL;
//                                 }
//                             );
//                       }
//                   );
//             }
//         );
//     _simplifiedChangePrefix.clear();
//     long timestampend = System.currentTimeMillis();
//     return ;
//   }

//   public void resetRibReason()
//   {
//     _rib
//         .values()
//         //        .parallelStream()
//         .forEach(
//             routeList->{
//               routeList
//                   .parallelStream()
//                   .forEach(
//                       routes->{
//                         routes
//                             .forEach(
//                                 route->{
//                                   route._reason=Reason.NORMAL;
//                                 }
//                             );
//                       }
//                   );
//             }
//         );
//   }

//   public List<SymBgpRoute> getChangedRoute()
//   {
//     return _changedRoute;
//   }

// //  public List<SymBgpRoute> getOutGoingRoute()
// //  {
// //    List<SymBgpRoute> answer=new ArrayList<>();
// //    //new topologyCondition update
// //    for(Integer prefix:_rib.keySet())
// //    {
// //      List<List<SymBgpRoute>> routes=_rib.get(prefix);
// //      boolean changed=false;
// //      BDD tc=_topologyfactory.one();
// //      for(int i=0;i<routes.size();i++) {
// //        List<SymBgpRoute> routeList = routes.get(i);
// //        if (tc.isZero()) {
// //          continue;
// //        } else {
// //          for (SymBgpRoute symBgpRoute : routeList) {
// //            SymBgpRoute outputRoute = cloneBgpRoute(symBgpRoute);
// //            BDD outTc = _topologyfactory.one();
// //            //这一部分主要处理的情况是：对于两个来自于同一个外部邻居的路由，如果是某一个路由分割开的两个路由，两个路由的拓扑条件应该不相互影响。
// //            //              System.out.println("step:!changed compute filter tc");
// //            if (outputRoute._external) {
// //              BDD tempTc = _topologyfactory.one();
// //              for (int j = 0; j < i; j++) {
// //                List<SymBgpRoute> routeListTemp = routes.get(j);
// //                for (SymBgpRoute routeTemp : routeListTemp) {
// //                  if (routeTemp.getOriginatorIp().equals(outputRoute.getOriginatorIp()) && routeTemp.getNodePath().equals(outputRoute.getNodePath())) {
// //                    continue;
// //                  }
// //                  tempTc = tempTc.and(routeTemp.getTopologyCondition().not());
// //                }
// //              }
// //              outTc = tempTc.and(outputRoute.getTopologyCondition());
// //              tempTc.free();
// //            } else {
// //              outTc = tc.and(outputRoute.getTopologyCondition());
// //            }
// //            if (outTc.isZero()) {
// //              continue;
// //            }
// //            outputRoute.setTopologyCondition(outTc);
// //            answer.add(outputRoute);
// //          }
// //        }
// //      }
// //    }
// //    return answer;
// //  }

//   public void removeRoute(SymBgpRoute symBgpRoute)
//   {
// //    System.out.println("step:With draw");
//     for(int i=_rib.get(symBgpRoute.getPrefixEcNum()).size()-1;i>=0;i--)
//     {
//       List<SymBgpRoute> routeList=_rib.get(symBgpRoute.getPrefixEcNum()).get(i);
//       Iterator<SymBgpRoute> removeIt=routeList.iterator();
//       while(removeIt.hasNext())
//       {
//         SymBgpRoute ribRoute=removeIt.next();
//         if(ribRoute.equalsWithDraw(symBgpRoute))
//         {
//           _simplifiedChangePrefix.add(symBgpRoute.getPrefixEcNum());
//           if(i!=(_rib.get(symBgpRoute.getPrefixEcNum()).size()-1))
//           {
//             for(int j=0;j<_rib.get(symBgpRoute.getPrefixEcNum()).get(i+1).size();j++)
//             {
//               _rib.get(symBgpRoute.getPrefixEcNum()).get(i+1).get(j)._reason=Reason.UPDATE;
//             }
//           }
//           if(ribRoute._reason!=Reason.ADD)
//           {
//             SymBgpRoute outputRoute=cloneBgpRoute(ribRoute);
//             _topologyBDD.deref(outputRoute._outBGPTopologyCondition);
//             _topologyBDD.deref(outputRoute.getTopologyCondition());
//             outputRoute._reason=Reason.WITHDRAW;
//             _withDrawRoute.add(outputRoute);
//           }
//           _topologyBDD.deref(ribRoute._outBGPTopologyCondition);
//           _topologyBDD.deref(ribRoute.getTopologyCondition());
//           _topologyBDD.deref(ribRoute._notTopologyCondition);
//           removeIt.remove();
//           break;
//         }
//       }
//       if(routeList.size()==0)
//       {
//         _rib.get(symBgpRoute.getPrefixEcNum()).remove(i);
//       }
//       //shan chu wei 0 de list
//     }
//   }

//   public Integer removeRoute(Integer routePrefixEc,Integer routeIndex,String asPath,SymBounder aspathSize)
//   {
//     //    System.out.println("step:With draw");
//     Integer asPathHash=0;
//     List<SymBgpRoute> replaceRoutes=new ArrayList<>();
//     int replaceLocation=-1;
//     boolean alreadyWithdraw=false;
//     for(int i=_rib.get(routePrefixEc).size()-1;i>=0;i--)
//     {
//       List<SymBgpRoute> routeList=_rib.get(routePrefixEc).get(i);
//       Iterator<SymBgpRoute> removeIt=routeList.iterator();
//       while(removeIt.hasNext())
//       {
//         SymBgpRoute ribRoute=removeIt.next();
//         if(ribRoute.getIndex().equals(routeIndex)&&ribRoute.getAsPath().subPreAsPath(asPath))
//         {
//           if(ribRoute.getAsPathSize().getLowerbound()>aspathSize.getUpperbound()||ribRoute.getAsPathSize().getUpperbound()<aspathSize.getLowerbound())
//           {
//             continue;
//           }
//           if(ribRoute.getAsPathSize().getLowerbound()==aspathSize.getLowerbound()&&ribRoute.getAsPathSize().getUpperbound()==aspathSize.getUpperbound())
//           {
//             _simplifiedChangePrefix.add(routePrefixEc);
//             if(i!=(_rib.get(routePrefixEc).size()-1))
//             {
//               for(int j=0;j<_rib.get(routePrefixEc).get(i+1).size();j++)
//               {
//                 if(_rib.get(routePrefixEc).get(i+1).get(j)._reason!=Reason.ADD)
//                 {
//                   _rib.get(routePrefixEc).get(i+1).get(j)._reason=Reason.UPDATE;
//                 }
//               }
//               asPathHash=ribRoute.getAsPath().hashCode();
//             }
//             _topologyBDD.deref(ribRoute.getTopologyCondition());
//             _topologyBDD.deref(ribRoute._outBGPTopologyCondition);
//             _topologyBDD.deref(ribRoute._notTopologyCondition);
//             removeIt.remove();
//             alreadyWithdraw=true;
// //            break;
//           }else if(ribRoute.getAsPathSize().getLowerbound()>=aspathSize.getLowerbound()&&ribRoute.getAsPathSize().getUpperbound()<=aspathSize.getUpperbound())
//           {
//             _simplifiedChangePrefix.add(routePrefixEc);
//             if(i!=(_rib.get(routePrefixEc).size()-1))
//             {
//               for(int j=0;j<_rib.get(routePrefixEc).get(i+1).size();j++)
//               {
//                 _rib.get(routePrefixEc).get(i+1).get(j)._reason=Reason.UPDATE;
//               }
//               asPathHash=ribRoute.getAsPath().hashCode();
//             }
//             _topologyBDD.deref(ribRoute.getTopologyCondition());
//             _topologyBDD.deref(ribRoute._outBGPTopologyCondition);
//             _topologyBDD.deref(ribRoute._notTopologyCondition);
//             removeIt.remove();
//           }else if(ribRoute.getAsPathSize().getLowerbound()<=aspathSize.getLowerbound()&&ribRoute.getAsPathSize().getUpperbound()>=aspathSize.getUpperbound()){
//             _simplifiedChangePrefix.add(routePrefixEc);
//             if(routeList.size()==1)
//             {
//               if(ribRoute.getAsPathSize().getLowerbound()==aspathSize.getLowerbound())
//               {
//                 TreeNode originNode=new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath());
//                 _replaceTreeNodes.put(originNode,new HashSet<>());
//                 _updateTreeNodes.put(originNode,new HashSet<>());
//                 SymBounder originBounder=new SymBounder(ribRoute.getAsPathSize().getLowerbound(),aspathSize.getUpperbound());
//                 ribRoute.setAsPathSize(aspathSize.getUpperbound()+1,ribRoute.getAsPathSize().getUpperbound());

//                 if(ribRoute._reason!=Reason.ADD)
//                 {
//                   ribRoute._reason=Reason.UPDATE;
//                 }
// //                _replaceTreeNodes.get(originNode).add(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()));
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()));
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,ribRoute.getAsPath()));
//                 _originalNodePairs.put(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()),originNode);
//                 _originalNodePairs.put(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,ribRoute.getAsPath()),originNode);
//               }else if(ribRoute.getAsPathSize().getUpperbound()==aspathSize.getUpperbound())
//               {
//                 TreeNode originNode=new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath());
//                 _replaceTreeNodes.put(originNode,new HashSet<>());
//                 _updateTreeNodes.put(originNode,new HashSet<>());
//                 SymBounder originBounder=new SymBounder(aspathSize.getLowerbound(),ribRoute.getAsPathSize().getUpperbound());
//                 ribRoute.setAsPathSize(ribRoute.getAsPathSize().getLowerbound(),aspathSize.getLowerbound()-1);
//                 if(ribRoute._reason!=Reason.ADD)
//                 {
//                   ribRoute._reason=Reason.UPDATE;
//                 }
// //                _replaceTreeNodes.get(originNode).add(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()));
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()));
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,ribRoute.getAsPath()));
//                 _originalNodePairs.put(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()),originNode);
//                 _originalNodePairs.put(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,ribRoute.getAsPath()),originNode);
//               }else{
//                 TreeNode originNode=new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath());
//                 _replaceTreeNodes.put(originNode,new HashSet<>());
//                 _updateTreeNodes.put(originNode,new HashSet<>());
//                 SymBgpRoute replaceUpperRoute=cloneBgpRoute(ribRoute);
//                 SymBounder originBounder=new SymBounder(aspathSize.getLowerbound(),aspathSize.getUpperbound());
//                 replaceUpperRoute.setAsPathSize(aspathSize.getUpperbound()+1,ribRoute.getAsPathSize().getUpperbound());
//                 ribRoute.setAsPathSize(ribRoute.getAsPathSize().getLowerbound(),aspathSize.getLowerbound()-1);
//                 if(ribRoute._reason!=Reason.ADD)
//                 {
//                   ribRoute._reason=Reason.UPDATE;
//                   replaceUpperRoute._reason=Reason.UPDATE;
//                 }
//                 replaceRoutes.add(replaceUpperRoute);
// //                _replaceTreeNodes.get(originNode).add(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()));
// //                _replaceTreeNodes.get(originNode).add(new TreeNode(_nodeName,replaceUpperRoute.getIndex(),replaceUpperRoute.getAsPath().hashCode(),replaceUpperRoute.getAsPathSize().hashCode(),replaceUpperRoute.getAsPathSize(),replaceUpperRoute.getAsPath()));
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()));
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,replaceUpperRoute.getIndex(),replaceUpperRoute.getAsPath().hashCode(),replaceUpperRoute.getAsPathSize().hashCode(),replaceUpperRoute.getAsPathSize(),replaceUpperRoute.getAsPath()));
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,replaceUpperRoute.getIndex(),replaceUpperRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,replaceUpperRoute.getAsPath()));
//                 _originalNodePairs.put(new TreeNode(_nodeName,replaceUpperRoute.getIndex(),replaceUpperRoute.getAsPath().hashCode(),replaceUpperRoute.getAsPathSize().hashCode(),replaceUpperRoute.getAsPathSize(),replaceUpperRoute.getAsPath()),originNode);
//                 _originalNodePairs.put(new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath()),originNode);
//                 _originalNodePairs.put(new TreeNode(_nodeName,replaceUpperRoute.getIndex(),replaceUpperRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,replaceUpperRoute.getAsPath()),originNode);
//                 replaceLocation=i;
//               }
//             }else{
//               if(ribRoute.getAsPathSize().getLowerbound()==aspathSize.getLowerbound())
//               {
//                 TreeNode originNode=new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath());
//                 SymBgpRoute replaceRotue=cloneBgpRoute(ribRoute);
//                 SymBounder originBounder=new SymBounder(ribRoute.getAsPathSize().getLowerbound(),aspathSize.getUpperbound());
//                 replaceRotue.setAsPathSize(aspathSize.getUpperbound()+1,ribRoute.getAsPathSize().getUpperbound());
//                 _originalNodePairs.put(new TreeNode(_nodeName,replaceRotue.getIndex(),replaceRotue.getAsPath().hashCode(),replaceRotue.getAsPathSize().hashCode(),replaceRotue.getAsPathSize(),replaceRotue.getAsPath()),originNode);
//                 _updateTreeNodes.put(originNode,new HashSet<>());
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,replaceRotue.getIndex(),replaceRotue.getAsPath().hashCode(),originBounder.hashCode(),originBounder,replaceRotue.getAsPath()));
//                 if(ribRoute._reason!=Reason.ADD)
//                 {
//                   replaceRotue._reason=Reason.UPDATE;
//                 }
//                 replaceRoutes.add(replaceRotue);
//               }else if(ribRoute.getAsPathSize().getUpperbound()==aspathSize.getUpperbound())
//               {
//                 TreeNode originNode=new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath());
//                 SymBgpRoute replaceRotue=cloneBgpRoute(ribRoute);
//                 SymBounder originBounder=new SymBounder(aspathSize.getLowerbound(),ribRoute.getAsPathSize().getUpperbound());
//                 replaceRotue.setAsPathSize(ribRoute.getAsPathSize().getLowerbound(),aspathSize.getLowerbound()-1);
//                 _updateTreeNodes.put(originNode,new HashSet<>());
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,replaceRotue.getIndex(),replaceRotue.getAsPath().hashCode(),originBounder.hashCode(),originBounder,replaceRotue.getAsPath()));
//                 _originalNodePairs.put(new TreeNode(_nodeName,replaceRotue.getIndex(),replaceRotue.getAsPath().hashCode(),replaceRotue.getAsPathSize().hashCode(),replaceRotue.getAsPathSize(),replaceRotue.getAsPath()),originNode);
//                 if(ribRoute._reason!=Reason.ADD)
//                 {
//                   replaceRotue._reason=Reason.UPDATE;
//                 }
//                 replaceRoutes.add(replaceRotue);
//               }else{
//                 TreeNode originNode=new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath());
//                 SymBgpRoute replaceLowerRoute=cloneBgpRoute(ribRoute);
//                 SymBgpRoute replaceUpperRoute=cloneBgpRoute(ribRoute);
//                 SymBounder originBounder=new SymBounder(aspathSize.getLowerbound(),aspathSize.getUpperbound());
//                 replaceLowerRoute.setAsPathSize(ribRoute.getAsPathSize().getLowerbound(),aspathSize.getLowerbound()-1);
//                 replaceUpperRoute.setAsPathSize(aspathSize.getUpperbound()+1,ribRoute.getAsPathSize().getUpperbound());
//                 if(ribRoute._reason!=Reason.ADD)
//                 {
//                   replaceLowerRoute._reason=Reason.UPDATE;
//                   replaceUpperRoute._reason=Reason.UPDATE;
//                 }
//                 replaceRoutes.add(replaceLowerRoute);
//                 replaceRoutes.add(replaceUpperRoute);
//                 _updateTreeNodes.put(originNode,new HashSet<>());
//                 _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,replaceLowerRoute.getIndex(),replaceLowerRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,replaceLowerRoute.getAsPath()));
//                 _originalNodePairs.put(new TreeNode(_nodeName,replaceLowerRoute.getIndex(),replaceLowerRoute.getAsPath().hashCode(),replaceLowerRoute.getAsPathSize().hashCode(),replaceLowerRoute.getAsPathSize(),replaceLowerRoute.getAsPath()),originNode);
//                 _originalNodePairs.put(new TreeNode(_nodeName,replaceUpperRoute.getIndex(),replaceUpperRoute.getAsPath().hashCode(),replaceUpperRoute.getAsPathSize().hashCode(),replaceUpperRoute.getAsPathSize(),replaceUpperRoute.getAsPath()),originNode);
//               }
//               _topologyBDD.deref(ribRoute.getTopologyCondition());
//               _topologyBDD.deref(ribRoute._outBGPTopologyCondition);
//               _topologyBDD.deref(ribRoute._notTopologyCondition);
//               removeIt.remove();
//             }
// //            alreadyWithdraw=true;
// //            break;
//           }else if(ribRoute.getAsPathSize().getLowerbound()<=aspathSize.getLowerbound()&&ribRoute.getAsPathSize().getUpperbound()<=aspathSize.getUpperbound())
//           {
//             _simplifiedChangePrefix.add(routePrefixEc);
//             TreeNode originNode=new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath());
//             SymBgpRoute replaceRoute=cloneBgpRoute(ribRoute);
//             replaceRoute.setAsPathSize(ribRoute.getAsPathSize().getLowerbound(),aspathSize.getLowerbound()-1);
//             SymBounder originBounder=new SymBounder(aspathSize.getLowerbound(),ribRoute.getAsPathSize().getUpperbound());
//             if(ribRoute._reason!=Reason.ADD)
//             {
//               replaceRoute._reason=Reason.UPDATE;
//             }
//             replaceRoutes.add(replaceRoute);
//             _updateTreeNodes.put(originNode,new HashSet<>());
//             _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,replaceRoute.getIndex(),replaceRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,replaceRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,replaceRoute.getIndex(),replaceRoute.getAsPath().hashCode(),replaceRoute.getAsPathSize().hashCode(),replaceRoute.getAsPathSize(),replaceRoute.getAsPath()),originNode);
//             _topologyBDD.deref(ribRoute.getTopologyCondition());
//             _topologyBDD.deref(ribRoute._outBGPTopologyCondition);
//             _topologyBDD.deref(ribRoute._notTopologyCondition);
//             removeIt.remove();
//           }else if(ribRoute.getAsPathSize().getLowerbound()>=aspathSize.getLowerbound()&&ribRoute.getAsPathSize().getUpperbound()>=aspathSize.getUpperbound())
//           {
//             _simplifiedChangePrefix.add(routePrefixEc);
//             TreeNode originNode=new TreeNode(_nodeName,ribRoute.getIndex(),ribRoute.getAsPath().hashCode(),ribRoute.getAsPathSize().hashCode(),ribRoute.getAsPathSize(),ribRoute.getAsPath());
//             SymBgpRoute replaceRoute=cloneBgpRoute(ribRoute);
//             replaceRoute.setAsPathSize(aspathSize.getUpperbound()+1,ribRoute.getAsPathSize().getUpperbound());
//             SymBounder originBounder=new SymBounder(ribRoute.getAsPathSize().getLowerbound(),aspathSize.getUpperbound());
//             if(ribRoute._reason!=Reason.ADD)
//             {
//               replaceRoute._reason=Reason.UPDATE;
//             }
//             replaceRoutes.add(replaceRoute);
//             _updateTreeNodes.put(originNode,new HashSet<>());
//             _updateTreeNodes.get(originNode).add(new TreeNode(_nodeName,replaceRoute.getIndex(),replaceRoute.getAsPath().hashCode(),originBounder.hashCode(),originBounder,replaceRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,replaceRoute.getIndex(),replaceRoute.getAsPath().hashCode(),replaceRoute.getAsPathSize().hashCode(),replaceRoute.getAsPathSize(),replaceRoute.getAsPath()),originNode);
//             _topologyBDD.deref(ribRoute.getTopologyCondition());
//             _topologyBDD.deref(ribRoute._outBGPTopologyCondition);
//             _topologyBDD.deref(ribRoute._notTopologyCondition);
//             removeIt.remove();
//           }
//         }
//       }
// //      if(alreadyWithdraw)
// //      {
// //        break;
// //      }
//       if(routeList.size()==0)
//       {
//         _rib.get(routePrefixEc).remove(i);
//       }
//     }
// //    if(replaceLocation != -1)
// //    {
// //      _rib.get(routePrefixEc).add(replaceLocation,new ArrayList<>());
// //      for(SymBgpRoute replaceRoute:replaceRoutes)
// //      {
// //        _rib.get(routePrefixEc).get(replaceLocation).add(replaceRoute);
// //      }
// //    }
//     if(replaceRoutes.size()!=0){
//       for (SymBgpRoute replaceRoute:replaceRoutes)
//       {
//         AddRoute(replaceRoute,true);
//       }
//     }
//     return asPathHash;
//   }



//   //filter update
// //  public List<SymBgpRoute> GetLastAttributeChangedRoute()
// //  {
// //    List<SymBgpRoute> answer=new ArrayList<>();
// //    answer.addAll(_withDrawRoute);
// //    //new topologyCondition update
// //    for(Integer prefix:_simplifiedChangePrefix)
// //    {
// //      List<List<SymBgpRoute>> routes=_rib.get(prefix);
// //      boolean changed=false;
// //      BDD tc=_topologyfactory.one();
// //      for(int i=0;i<routes.size();i++)
// //      {
// //        List<SymBgpRoute> routeList=routes.get(i);
// //        if(tc.isZero())
// //        {
// //          Iterator<SymBgpRoute> iterator=routeList.iterator();
// //          while(iterator.hasNext())
// //          {
// //            SymBgpRoute bgpRoute=iterator.next();
// //            SymBgpRoute outputRoute=cloneBgpRoute(bgpRoute);
// //            if(bgpRoute._reason==Reason.ADD)
// //            {
// //              iterator.remove();
// //              continue;
// //            }
// //            outputRoute._reason=Reason.WITHDRAW;
// //            outputRoute.convertToLastBgpMessage();
// //            answer.add(outputRoute);
// //            iterator.remove();
// //          }
// //        }else if(!changed)
// //        {
// //          //          System.out.println("step:!chaged");
// //          Iterator<SymBgpRoute> iterator=routeList.iterator();
// //          while(iterator.hasNext())
// //          {
// //            SymBgpRoute symBgpRoute=iterator.next();
// //            if(!(symBgpRoute.getReason()==Reason.NORMAL))
// //            {
// //              changed=true;
// //              SymBgpRoute outputRoute=cloneBgpRoute(symBgpRoute);
// //              BDD outTc=_topologyfactory.one();
// //              //这一部分主要处理的情况是：对于两个来自于同一个外部邻居的路由，如果是某一个路由分割开的两个路由，两个路由的拓扑条件应该不相互影响。
// //              if(outputRoute._external)
// //              {
// //                BDD tempTc=_topologyfactory.one();
// //                for(int j=0;j<i;j++)
// //                {
// //                  List<SymBgpRoute> routeListTemp=routes.get(j);
// //                  for(SymBgpRoute routeTemp:routeListTemp)
// //                  {
// //                    if(routeTemp.getOriginatorIp().equals(outputRoute.getOriginatorIp())&&routeTemp.getNodePath().equals(outputRoute.getNodePath()))
// //                    {
// //                      continue;
// //                    }
// //                    tempTc=tempTc.and(routeTemp.getTopologyCondition().not());
// //                  }
// //                }
// //                outTc=tempTc.and(outputRoute.getTopologyCondition());
// //              }else{
// //                outTc=tc.and(outputRoute.getTopologyCondition());
// //              }
// //              outTc=outTc.and(_topologyFilter);
// //              if(outTc.isZero())
// //              {
// //                outputRoute._reason=Reason.WITHDRAW;
// //                iterator.remove();
// //                if(symBgpRoute._reason==Reason.ADD)
// //                {
// //                  continue;
// //                }
// //              }else{
// //                outputRoute.setTopologyCondition(outTc);
// //                outputRoute._reason=symBgpRoute.getReason()==Reason.ADD?Reason.ADD:Reason.UPDATE;
// //              }
// //              if(symBgpRoute._outISISTopologyCondition!=null&&outTc.equals(symBgpRoute._outISISTopologyCondition))
// //              {
// //                continue;
// //              }
// //              symBgpRoute._outISISTopologyCondition=outTc;
// //              outputRoute.convertToLastBgpMessage();
// //              answer.add(outputRoute);
// //            }else{
// //              symBgpRoute._outISISTopologyCondition= symBgpRoute.getTopologyCondition().and(tc);
// //            }
// //          }
// //          //          System.out.println("step:!chaged-end");
// //        }else{
// //          Iterator<SymBgpRoute> iterator=routeList.iterator();
// //          //          System.out.println("step:chaged");
// //          while(iterator.hasNext())
// //          {
// //            SymBgpRoute symBgpRoute=iterator.next();
// //            SymBgpRoute outputRoute=cloneBgpRoute(symBgpRoute);
// //            BDD outTc=_topologyfactory.one();
// //            //这一部分主要处理的情况是：对于两个来自于同一个外部邻居的路由，如果是某一个路由分割开的两个路由，两个路由的拓扑条件应该不相互影响。
// //            if(outputRoute._external)
// //            {
// //              BDD tempTc=_topologyfactory.one();
// //              for(int j=0;j<i;j++)
// //              {
// //                List<SymBgpRoute> routeListTemp=routes.get(j);
// //                for(SymBgpRoute routeTemp:routeListTemp)
// //                {
// //                  if(routeTemp.getOriginatorIp().equals(outputRoute.getOriginatorIp())&&routeTemp.getNodePath().equals(outputRoute.getNodePath()))
// //                  {
// //                    continue;
// //                  }
// //                  tempTc=tempTc.and(routeTemp.getTopologyCondition().not());
// //                }
// //              }
// //              outTc=tempTc.and(outputRoute.getTopologyCondition());
// //              tempTc.free();
// //            }else{
// //              outTc=tc.and(outputRoute.getTopologyCondition());
// //            }
// //            //            System.out.println("step:filter-k");
// //            outTc=outTc.and(_topologyFilter);
// //            if(outTc.isZero())
// //            {
// //              outputRoute._reason=Reason.WITHDRAW;
// //              if(symBgpRoute._reason==Reason.ADD)
// //              {
// //                iterator.remove();
// //                continue;
// //              }
// //              outputRoute.convertToLastBgpMessage();
// //              answer.add(outputRoute);
// //              iterator.remove();
// //              continue;
// //            }
// //            //            System.out.println("step:filter-k-end");
// //            if(symBgpRoute._outISISTopologyCondition!=null&&outTc.equals(symBgpRoute._outISISTopologyCondition))
// //            {
// //              continue;
// //            }
// //            if(symBgpRoute.getReason()!=Reason.ADD)
// //            {
// //              outputRoute._reason=Reason.UPDATE;
// //            }else{
// //              outputRoute._reason=Reason.ADD;
// //            }
// //            outputRoute.setTopologyCondition(outTc);
// //            symBgpRoute._outISISTopologyCondition=outTc;
// //            outputRoute.convertToLastBgpMessage();
// //            answer.add(outputRoute);
// //          }
// //          //          System.out.println("step:chaged-end");
// //        }
// //        //        System.out.println("step:compute-tc");
// //        BDD tcTemp=_topologyfactory.zero();
// //        for (SymBgpRoute symBgpRoute:routeList) {
// //          //zhe li ka zhu le
// //          //tcTemp.printSet();
// //          tcTemp=tcTemp.or(symBgpRoute.getTopologyCondition());
// //        }
// //        //        System.out.println("step:middle-compute-tc");
// //        tc=tc.and(tcTemp.not());
// //        tcTemp.free();
// //        //        System.out.println("step:compute-tc-end");
// //      }
// //      for(int i=routes.size()-1;i>=0;i--) {
// //        List<SymBgpRoute> routeList = routes.get(i);
// //        if(routeList.size()==0)
// //        {
// //          _rib.get(prefix).remove(i);
// //        }
// //      }
// //      tc.free();
// //    }
// //    Collections.sort(answer,new ReasonComparator());
// //    return answer;
// //  }


//   public void AddRoute(Queue<SymBgpRoute> addQueue,Integer routeNetWork)
//   {
// //    System.out.println("step:Add");
//     _simplifiedChangePrefix.add(routeNetWork);
//     if(!this._rib.containsKey(routeNetWork))
//     {
//       _rib.put(routeNetWork, new ArrayList<>());
//     }
//       //主要就是进行比较,这部分在路由策略之后完成
//       //compare local-preference
//     List<List<SymBgpRoute>> routes=_rib.get(routeNetWork);
//     while(!addQueue.isEmpty())
//     {
//       SymBgpRoute addRoute=addQueue.remove();
//       boolean alreadyInject=false;
//       int location=0;
//       for(;location<routes.size();)
//       {
//         if(routes.get(location).contains(addRoute))
//         {
//           alreadyInject=true;
//           break;
//         }
// //        if(routes.get(location).size()==0)
// //        {
// //          break;
// //        }
//         SymBgpRoute compareRoute=routes.get(location).get(0);
//         int addLocalPreference=addRoute.getLocalPreference().getLowerbound();
//         int compareLocalPreference=compareRoute.getLocalPreference().getLowerbound();
//         if(addLocalPreference==compareLocalPreference)
//         {
//           SymBgpRib.Answer an=processAsPath(location,addRoute,routeNetWork,addQueue);
//           if(an.alreadyInject)
//           {
//             alreadyInject=true;
//             location=an.location;
//             break;
//           }else{
//             alreadyInject=false;
//             location=an.location;
//             continue;
//           }
//         }else if(addLocalPreference<compareLocalPreference)
//         {
//           location++;
//           continue;
//         }else{
//           routes.add(location,new ArrayList<>());
//           if(addRoute._reason==Reason.NORMAL||addRoute._reason==Reason.ADD)
//           {
//             addRoute._reason=Reason.ADD;
//             TreeNode thisNode=new TreeNode(_nodeName,addRoute.getIndex(),addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath());
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
//             _addTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//             _addTreeNodes.get(originAddNode).add(thisNode);
//           }else if (addRoute._reason==Reason.UPDATE)
//           {
//             TreeNode thisNode=new TreeNode(_nodeName,addRoute.getIndex(),addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath());
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
//             _updateTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//             _updateTreeNodes.get(originAddNode).add(thisNode);
//           }
//           routes.get(location).add(addRoute);
//           alreadyInject=true;
//           break;
//         }
//       }
//       if(!alreadyInject)
//       {
//         _rib.get(routeNetWork).add(location,new ArrayList<>());
//         _rib.get(routeNetWork).get(location).add(addRoute);
//         if(addRoute._reason==Reason.NORMAL||addRoute._reason==Reason.ADD)
//         {
//           _rib.get(routeNetWork).get(location).get(0)._reason=Reason.ADD;
//           TreeNode thisNode=new TreeNode(_nodeName,addRoute.getIndex(),addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath());
//           TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //          if(originAddNode==null)
// //          {
// //            System.out.println("break");
// //          }
//           _addTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//           _addTreeNodes.get(originAddNode).add(thisNode);
// //          if(originAddNode.value.equals(thisNode.value))
// //          {
// //            System.out.println("add-break");
// //          }
//         }else if(addRoute._reason==Reason.UPDATE)
//         {
//           _rib.get(routeNetWork).get(location).get(0)._reason=Reason.UPDATE;
//           TreeNode thisNode=new TreeNode(_nodeName,addRoute.getIndex(),addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath());
//           TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //          if(originAddNode==null)
// //          {
// //            System.out.println("break");
// //          }
//           _updateTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//           _updateTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,
//               addRoute.getIndex(), addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath()));
//         }
//       }
//     }
//   }

//   //prefixEcNum you hua
//   //现在的AddRoute，考虑local-preference的时候不用考虑任意值了，因为域内的属性不存在外部的任意路由宣告应先的情况。
//   public Boolean AddRoute(SymBgpRoute route)
//   {
// //    System.out.println("step:Add Route");
//     Integer routeNetWork=route.getPrefixEcNum();
//     _simplifiedChangePrefix.add(routeNetWork);
//     boolean alreadyAdd=true;
//     if(route.getAsPath().getAsPathString().equals("1 2 10070 60 100")&&_nodeName.equals("10080"))
//     {
//       System.out.println("addBreak");
//     }
//     if(!this._rib.containsKey(routeNetWork))
//     {
//       _rib.put(routeNetWork, new ArrayList<>());
//       _rib.get(routeNetWork).add(new ArrayList<>());
//       route._reason=Reason.ADD;
//       _rib.get(routeNetWork).get(0).add(route);

//       TreeNode thisNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.get(thisNode);
//       _addTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//       _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()));
// //      if(originAddNode!=null&&originAddNode.value.equals(_nodeName))
// //      {
// //        System.out.println("add-break");
// //      }
//     }else{
//       //主要就是进行比较,这部分在路由策略之后完成
//       //compare local-preference
//       List<List<SymBgpRoute>> routes=_rib.get(routeNetWork);
//       Queue<SymBgpRoute> addQueue=new LinkedList<SymBgpRoute>();
//       addQueue.add(route);
//       while(!addQueue.isEmpty())
//       {
//         SymBgpRoute addRoute=addQueue.remove();
//         if(addRoute.getAsPath().getAsPathString().equals("20030 300 100 60 10"))
//         {
//           System.out.println("addBreak");
//         }
//         boolean alreadyInject=false;
//         int location=0;
//         for(;location<routes.size();)
//         {
//           if(routes.get(location).contains(addRoute))
//           {
//             alreadyInject=true;
//             alreadyAdd=false;
//             break;
//           }
//           if(routes.get(location).size()==0)
//           {
//             break;
//           }
//           SymBgpRoute compareRoute=routes.get(location).get(0);
//           int addLocalPreference=addRoute.getLocalPreference().getLowerbound();
//           int compareLocalPreference=compareRoute.getLocalPreference().getLowerbound();
//           if(addLocalPreference==compareLocalPreference)
//           {
//             SymBgpRib.Answer an=processAsPath(location,addRoute,routeNetWork,addQueue);
//             if(an.alreadyInject)
//             {
//               alreadyInject=true;
//               location=an.location;
//               break;
//             }else{
//               alreadyInject=false;
//               location=an.location;
//               continue;
//             }
//           }else if(addLocalPreference<compareLocalPreference)
//           {
//             location++;
//             continue;
//           }else{
//             routes.add(location,new ArrayList<>());
//             routes.get(location).add(addRoute);
//             TreeNode thisNode=new TreeNode(_nodeName,addRoute.getIndex(),addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath());
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //            if(originAddNode==null)
// //            {
// //              System.out.println("break");
// //            }
//             if(addRoute._reason==Reason.NORMAL||addRoute._reason==Reason.ADD)
//             {
//               routes.get(location).get(0)._reason=Reason.ADD;
//               _addTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//               _addTreeNodes.get(originAddNode).add(thisNode);
// //              if(originAddNode.value.equals(thisNode.value))
// //              {
// //                System.out.println("add-break");
// //              }
//             }else if (addRoute._reason==Reason.UPDATE)
//             {
//               _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//               _updateTreeNodes.get(originAddNode).add(thisNode);
//             }
//             alreadyInject=true;
//             break;
//           }
//         }
//         if(!alreadyInject)
//         {
//           _rib.get(routeNetWork).add(location,new ArrayList<>());
//           _rib.get(routeNetWork).get(location).add(addRoute);


//           TreeNode thisNode=new TreeNode(_nodeName,addRoute.getIndex(),addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath());
//           TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //          if(originAddNode==null)
// //          {
// //            System.out.println("break");
// //          }
//           if(addRoute._reason==Reason.NORMAL||addRoute._reason==Reason.ADD)
//           {
//             routes.get(location).get(0)._reason=Reason.ADD;
//             _addTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//             _addTreeNodes.get(originAddNode).add(thisNode);
// //            if(originAddNode.value.equals(thisNode.value))
// //            {
// //              System.out.println("add-break");
// //            }
//           }else if (addRoute._reason==Reason.UPDATE)
//           {
//             _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//             _updateTreeNodes.get(originAddNode).add(thisNode);
//           }
//         }
//       }
//     }
//     return alreadyAdd;
//   }


//   public Boolean AddRoute(SymBgpRoute route,Boolean withDraw)
//   {
//     //    System.out.println("step:Add Route");
//     Integer routeNetWork=route.getPrefixEcNum();
//     _simplifiedChangePrefix.add(routeNetWork);
//     boolean alreadyAdd=true;
//     if(!this._rib.containsKey(routeNetWork))
//     {
//       _rib.put(routeNetWork, new ArrayList<>());
//       _rib.get(routeNetWork).add(new ArrayList<>());
//       if(route._reason==Reason.NORMAL)
//       {
//         route._reason=Reason.UPDATE;
//       }
//       _rib.get(routeNetWork).get(0).add(route);

//       TreeNode thisNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//       _updateTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//       _updateTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()));

//     }else{
//       //主要就是进行比较,这部分在路由策略之后完成
//       //compare local-preference
//       List<List<SymBgpRoute>> routes=_rib.get(routeNetWork);
//       Queue<SymBgpRoute> addQueue=new LinkedList<SymBgpRoute>();
//       addQueue.add(route);
//       while(!addQueue.isEmpty())
//       {
//         SymBgpRoute addRoute=addQueue.remove();
//         boolean alreadyInject=false;
//         int location=0;
//         for(;location<routes.size();)
//         {
//           if(routes.get(location).contains(addRoute))
//           {
//             alreadyInject=true;
//             alreadyAdd=false;
//             break;
//           }
//           if(routes.get(location).size()==0)
//           {
//             break;
//           }
//           SymBgpRoute compareRoute=routes.get(location).get(0);
//           int addLocalPreference=addRoute.getLocalPreference().getLowerbound();
//           int compareLocalPreference=compareRoute.getLocalPreference().getLowerbound();
//           if(addLocalPreference==compareLocalPreference)
//           {
//             SymBgpRib.Answer an=processAsPath(location,addRoute,routeNetWork,addQueue,true);
//             if(an.alreadyInject)
//             {
//               alreadyInject=true;
//               location=an.location;
//               break;
//             }else{
//               alreadyInject=false;
//               location=an.location;
//               continue;
//             }
//           }else if(addLocalPreference<compareLocalPreference)
//           {
//             location++;
//             continue;
//           }else{
//             if(addRoute._reason==Reason.NORMAL)
//             {
//               addRoute._reason=Reason.UPDATE;
//             }
//             routes.add(location,new ArrayList<>());
//             routes.get(location).add(addRoute);
//             TreeNode thisNode=new TreeNode(_nodeName,addRoute.getIndex(),addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath());
//             TreeNode originAddNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
// //            if(originAddNode==null)
// //            {
// //              System.out.println("break");
// //            }
//             _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//             _updateTreeNodes.get(originAddNode).add(thisNode);
//             alreadyInject=true;
//             break;
//           }
//         }
//         if(!alreadyInject)
//         {
//           if(addRoute._reason==Reason.NORMAL)
//           {
//             addRoute._reason=Reason.UPDATE;
//           }
//           _rib.get(routeNetWork).add(location,new ArrayList<>());
//           _rib.get(routeNetWork).get(location).add(addRoute);

//           TreeNode thisNode=new TreeNode(_nodeName,addRoute.getIndex(),addRoute.getAsPath().hashCode(),addRoute.getAsPathSize().hashCode(),addRoute.getAsPathSize(),addRoute.getAsPath());
//           TreeNode originAddNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
// //          if(originAddNode==null)
// //          {
// //            System.out.println("break");
// //          }
//           _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//           _updateTreeNodes.get(originAddNode).add(thisNode);
//         }
//       }
//     }
//     return alreadyAdd;
//   }
//   public class Answer{
//     public int location;
//     public boolean alreadyInject;
//     public Queue<SymBgpRoute> queue;
//     public List<updateTreeNode> updateTreeNodes;
//     public Answer()
//     {
//       location=-1;
//       alreadyInject=false;
//       updateTreeNodes=new ArrayList<>();
//     }
//   }

//   public SymBgpRib.Answer processAsPath(int location,SymBgpRoute route,Integer routeNetwork, Queue<SymBgpRoute> addQueue)
//   {
//     SymBgpRib.Answer answer=new SymBgpRib.Answer();
//     int addUpperAsPathSize=route.getAsPathSize().getUpperbound();
//     int addLowerAsPathSize=route.getAsPathSize().getLowerbound();
//     int addConfirmAsPathSize=route.getAsPath().size();

//     int addFinalLower=addLowerAsPathSize+addConfirmAsPathSize;
//     int addFinalUpper=addUpperAsPathSize==Integer.MAX_VALUE?addUpperAsPathSize:addUpperAsPathSize+addConfirmAsPathSize;

//     SymBgpRoute compareRoute=_rib.get(routeNetwork).get(location).get(0);

//     int compareUpperAsPathSize=compareRoute.getAsPathSize().getUpperbound();
//     int compareLowerAsPathSize=compareRoute.getAsPathSize().getLowerbound();
//     int compareConfirmAsPathSize=compareRoute.getAsPath().size();

//     int compareFinalLower=compareLowerAsPathSize+compareConfirmAsPathSize;
//     int compareFinalUpper=compareUpperAsPathSize==Integer.MAX_VALUE?compareUpperAsPathSize:compareUpperAsPathSize+compareConfirmAsPathSize;

//     if(addFinalUpper<compareFinalLower)
//     {
//       _rib.get(routeNetwork).add(location,new ArrayList<>());
//       _rib.get(routeNetwork).get(location).add(route);
//       TreeNode thisNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //      if(originAddNode==null)
// //      {
// //        System.out.println("break1");
// //      }
//       if(route._reason==Reason.NORMAL||route._reason==Reason.ADD)
//       {
//         _rib.get(routeNetwork).get(location).get(0)._reason=Reason.ADD;
//         _addTreeNodes.computeIfAbsent(originAddNode, k -> new HashSet<>());
//         _addTreeNodes.get(originAddNode).add(thisNode);
// //        if(originAddNode.value.equals(thisNode.value))
// //        {
// //          System.out.println("add-break");
// //        }
//       }else if(route._reason==Reason.UPDATE)
//       {
//         _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//         _updateTreeNodes.get(originAddNode).add(thisNode);
//       }
//       answer.alreadyInject=true;
//       answer.queue=addQueue;
//       return answer;
//     }

//     if(addFinalLower>compareFinalUpper)
//     {
//       answer.alreadyInject=false;
//       answer.location=location+1;
//       answer.queue=addQueue;
//       return answer;
//     }

//     if(addFinalLower==compareFinalLower&&addFinalUpper==compareFinalUpper)
//     {
//       return processIsis(location,route,routeNetwork,addQueue);
//     }

//     if(addFinalUpper<=compareFinalUpper&&addFinalLower>=compareFinalLower)
//     {
//       if(addFinalUpper==compareFinalUpper)
//       {
//         List<SymBgpRoute> originalAddRouteLowerList=new ArrayList<>();
//         for(SymBgpRoute originalAddRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalAddRoute.getIndex(),originalAddRoute.getAsPath().hashCode(),originalAddRoute.getAsPathSize().hashCode(),originalAddRoute.getAsPathSize(),originalAddRoute.getAsPath());
//           TreeNode originalTreeNode=null;
//           if(originalAddRoute._reason==Reason.NORMAL)
//           {
//             originalTreeNode=thisNode;
//             SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRoute);
//             if(!_updateTreeNodes.containsKey(originalTreeNode))
//             {
//               _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//               _updateRouteIndex.put(originalTreeNode,originalAddRoute.getIndex());
//             }
//             tempAddRoute.setAsPathSize(tempAddRoute.getAsPathSize().getLowerbound(),addFinalLower-tempAddRoute.getAsPath().size()-1);
//             tempAddRoute._reason=Reason.UPDATE;
//             originalAddRouteLowerList.add(tempAddRoute);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);
//           }else if(originalAddRoute._reason==Reason.UPDATE)
//           {
//             originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//             if(originalTreeNode==null)
//             {
//               originalTreeNode=thisNode;
// //              System.out.println("break");
//             }

//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//             _updateTreeNodes.get(originalTreeNode).remove(thisNode);

//             SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRoute);
//             tempAddRoute.setAsPathSize(tempAddRoute.getAsPathSize().getLowerbound(),addFinalLower-tempAddRoute.getAsPath().size()-1);
//             tempAddRoute._reason=Reason.UPDATE;
//             originalAddRouteLowerList.add(tempAddRoute);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);
//           }else if(originalAddRoute._reason==Reason.ADD)
//           {
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //            if(originAddNode==null)
// //            {
// //              System.out.println("break");
// //            }
//             _addTreeNodes.get(originAddNode).remove(thisNode);
//             SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRoute);
//             tempAddRoute.setAsPathSize(tempAddRoute.getAsPathSize().getLowerbound(),addFinalLower-tempAddRoute.getAsPath().size()-1);
//             tempAddRoute._reason=Reason.ADD;
//             originalAddRouteLowerList.add(tempAddRoute);
//             _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
// //            if(originAddNode.value.equals(_nodeName))
// //            {
// //              System.out.println("add-break");
// //            }
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originAddNode);
//           }
//         }
//         for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//         {
//           if (originalRoute._reason==Reason.NORMAL)
//           {
//             TreeNode originalTreeNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//             originalRoute._reason=Reason.UPDATE;
//             originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),originalRoute.getAsPathSize().getUpperbound());
//             if(!_updateTreeNodes.containsKey(originalTreeNode))
//             {
//               _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//               _updateRouteIndex.put(originalTreeNode,originalRoute.getIndex());
//             }
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           }else if(originalRoute._reason==Reason.UPDATE)
//           {
//             TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//             TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//             if(originalTreeNode==null)
//             {
//               originalTreeNode=thisNode;
// //              System.out.println("break");
//             }

//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());

//             originalRoute._reason=Reason.UPDATE;
//             originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),originalRoute.getAsPathSize().getUpperbound());

//             _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//             _originalNodePairs.remove(thisNode);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           }else if(originalRoute._reason==Reason.ADD)
//           {
//             TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //            if(originAddNode==null)
// //            {
// //              System.out.println("break");
// //            }
//             _addTreeNodes.get(originAddNode).remove(thisNode);
//             originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),originalRoute.getAsPathSize().getUpperbound());
//             _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originAddNode);
//             _originalNodePairs.remove(thisNode);
// //            if(originAddNode.value.equals(_nodeName))
// //            {
// //              System.out.println("add-break");
// //            }
//           }
//         }
//         _rib.get(routeNetwork).add(location,originalAddRouteLowerList);
//         //这里肯定是直接相等的情况，但是直接比ISIS有些困难，所以再返回继续比较localpreference，再回来比较这个As-Path相等，然后再比较ISIS，后续这里可能可以优化。
//         answer=processIsis(location+1,route,routeNetwork,addQueue);
//         return answer;
//       }else if(addFinalLower==compareFinalLower)
//       {
//         List<SymBgpRoute> originalAddRouteUpperList=new ArrayList<>();
//         for(SymBgpRoute originalAddRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalAddRoute.getIndex(),originalAddRoute.getAsPath().hashCode(),originalAddRoute.getAsPathSize().hashCode(),originalAddRoute.getAsPathSize(),originalAddRoute.getAsPath());
//           if(originalAddRoute._reason==Reason.NORMAL)
//           {
//             TreeNode originalTreeNode=thisNode;
//             if(!_updateTreeNodes.containsKey(originalTreeNode))
//             {
//               _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//               _updateRouteIndex.put(originalTreeNode,originalAddRoute.getIndex());
//             }
//             SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRoute);
//             tempAddRoute.setAsPathSize(addFinalUpper-tempAddRoute.getAsPath().size()+1,tempAddRoute.getAsPathSize().getUpperbound());
//             tempAddRoute._reason=Reason.UPDATE;
//             originalAddRouteUpperList.add(tempAddRoute);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));

//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);
//           }else if(originalAddRoute._reason==Reason.UPDATE)
//           {
//             TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//             if(originalTreeNode==null)
//             {
//               originalTreeNode=thisNode;
// //              System.out.println("break");
//             }

//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());

//             SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRoute);
//             tempAddRoute.setAsPathSize(addFinalUpper-tempAddRoute.getAsPath().size()+1,tempAddRoute.getAsPathSize().getUpperbound());
//             tempAddRoute._reason=Reason.UPDATE;
//             originalAddRouteUpperList.add(tempAddRoute);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));

//             if(_addTreeNodes.get(originalTreeNode)!=null)
//             {
//               Set<TreeNode> temp=_addTreeNodes.get(originalTreeNode);
//               System.out.println("temp");
//             }
//             _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),route.getAsPath()),originalTreeNode);
//           }else if(originalAddRoute._reason==Reason.ADD)
//           {
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
//             SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRoute);
//             tempAddRoute.setAsPathSize(addFinalUpper-tempAddRoute.getAsPath().size()+1,tempAddRoute.getAsPathSize().getUpperbound());
//             tempAddRoute._reason=Reason.ADD;
//             originalAddRouteUpperList.add(tempAddRoute);
// //            if(originAddNode==null)
// //            {
// //              System.out.println("break");
// //            }
//             _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
//             _addTreeNodes.get(originAddNode).remove(thisNode);
// //            if(originAddNode.value.equals(_nodeName))
// //            {
// //              System.out.println("add-break");
// //            }
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originAddNode);
//           }
//         }
//         for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//           if(originalRoute._reason==Reason.NORMAL)
//           {
//             TreeNode originalTreeNode=thisNode;
//             if(!_updateTreeNodes.containsKey(originalTreeNode))
//             {
//               _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//               _updateRouteIndex.put(originalTreeNode,originalRoute.getIndex());
//             }
//             originalRoute._reason=Reason.UPDATE;
//             originalRoute.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),addFinalUpper-originalRoute.getAsPath().size());
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           }else if(originalRoute._reason==Reason.UPDATE)
//           {
//             TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//             if(originalTreeNode==null)
//             {
//               originalTreeNode=thisNode;
// //              System.out.println("break");
//             }

//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());

//             originalRoute._reason=Reason.UPDATE;
//             originalRoute.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),addFinalUpper-originalRoute.getAsPath().size());
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//             _originalNodePairs.remove(thisNode);
//           }else if(originalRoute._reason==Reason.ADD)
//           {
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //            if(originAddNode==null)
// //            {
// //              System.out.println("break");
// //            }
//             _addTreeNodes.get(originAddNode).remove(thisNode);
//             originalRoute.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),addFinalUpper-originalRoute.getAsPath().size());
//             _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originAddNode);
//             _originalNodePairs.remove(thisNode);
// //            if(originAddNode.value.equals(_nodeName))
// //            {
// //              System.out.println("add-break");
// //            }
//           }
//         }
//         _rib.get(routeNetwork).add(location+1,originalAddRouteUpperList);
//         //这里肯定是直接相等的情况，但是直接比ISIS有些困难，所以再返回继续比较localpreference，再回来比较这个As-Path相等，然后再比较ISIS，后续这里可能可以优化。
//         answer=processIsis(location,route,routeNetwork,addQueue);
//         return answer;
//       }else{
//         List<SymBgpRoute> originalAddRouteUpperList=new ArrayList<>();
//         List<SymBgpRoute> originalAddRouteLowerList=new ArrayList<>();
//         for(SymBgpRoute originalAddRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalAddRoute.getIndex(),originalAddRoute.getAsPath().hashCode(),originalAddRoute.getAsPathSize().hashCode(),originalAddRoute.getAsPathSize(),originalAddRoute.getAsPath());

//           if(originalAddRoute._reason==Reason.NORMAL)
//           {
//             TreeNode originalTreeNode=thisNode;
//             if(!_updateTreeNodes.containsKey(originalTreeNode))
//             {
//               _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//               _updateRouteIndex.put(originalTreeNode,originalAddRoute.getIndex());
//             }
//             SymBgpRoute tempAddRouteUpper=cloneBgpRoute(originalAddRoute);
//             tempAddRouteUpper.setAsPathSize(addFinalUpper-tempAddRouteUpper.getAsPath().size()+1,tempAddRouteUpper.getAsPathSize().getUpperbound());
//             tempAddRouteUpper._reason=Reason.UPDATE;
//             originalAddRouteUpperList.add(tempAddRouteUpper);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRouteUpper.getIndex(),tempAddRouteUpper.getAsPath().hashCode(),tempAddRouteUpper.getAsPathSize().hashCode(),tempAddRouteUpper.getAsPathSize(),tempAddRouteUpper.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRouteUpper.getIndex(),tempAddRouteUpper.getAsPath().hashCode(),tempAddRouteUpper.getAsPathSize().hashCode(),tempAddRouteUpper.getAsPathSize(),tempAddRouteUpper.getAsPath()),originalTreeNode);

//             SymBgpRoute tempAddRouteLower=cloneBgpRoute(originalAddRoute);
//             tempAddRouteLower.setAsPathSize(tempAddRouteLower.getAsPathSize().getLowerbound(),addFinalLower-tempAddRouteLower.getAsPath().size()-1);
//             tempAddRouteLower._reason=Reason.UPDATE;
//             originalAddRouteLowerList.add(tempAddRouteLower);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRouteLower.getIndex(),tempAddRouteLower.getAsPath().hashCode(),tempAddRouteLower.getAsPathSize().hashCode(),tempAddRouteLower.getAsPathSize(),tempAddRouteLower.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRouteLower.getIndex(),tempAddRouteLower.getAsPath().hashCode(),tempAddRouteLower.getAsPathSize().hashCode(),tempAddRouteLower.getAsPathSize(),tempAddRouteLower.getAsPath()),originalTreeNode);
//           }else if(originalAddRoute._reason==Reason.UPDATE)
//           {
//             TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//             if(originalTreeNode==null)
//             {
//               originalTreeNode=thisNode;
// //              System.out.println("break");
//             }

//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());

//             SymBgpRoute tempAddRouteUpper=cloneBgpRoute(originalAddRoute);
//             tempAddRouteUpper.setAsPathSize(addFinalUpper-tempAddRouteUpper.getAsPath().size()+1,tempAddRouteUpper.getAsPathSize().getUpperbound());
//             tempAddRouteUpper._reason=Reason.UPDATE;
//             originalAddRouteUpperList.add(tempAddRouteUpper);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRouteUpper.getIndex(),tempAddRouteUpper.getAsPath().hashCode(),tempAddRouteUpper.getAsPathSize().hashCode(),tempAddRouteUpper.getAsPathSize(),tempAddRouteUpper.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,
//                 tempAddRouteUpper.getIndex(), tempAddRouteUpper.getAsPath().hashCode(),tempAddRouteUpper.getAsPathSize().hashCode(),tempAddRouteUpper.getAsPathSize(),tempAddRouteUpper.getAsPath()),originalTreeNode);

//             SymBgpRoute tempAddRouteLower=cloneBgpRoute(originalAddRoute);
//             tempAddRouteLower.setAsPathSize(tempAddRouteLower.getAsPathSize().getLowerbound(),addFinalLower-tempAddRouteLower.getAsPath().size()-1);
//             tempAddRouteLower._reason=Reason.UPDATE;
//             originalAddRouteLowerList.add(tempAddRouteLower);
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRouteLower.getIndex(),tempAddRouteLower.getAsPath().hashCode(),tempAddRouteLower.getAsPathSize().hashCode(),tempAddRouteLower.getAsPathSize(),tempAddRouteLower.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRouteLower.getIndex(),tempAddRouteLower.getAsPath().hashCode(),tempAddRouteLower.getAsPathSize().hashCode(),tempAddRouteLower.getAsPathSize(),tempAddRouteLower.getAsPath()),originalTreeNode);

//             _updateTreeNodes.get(originalTreeNode).remove(thisNode);



//           }else if(originalAddRoute._reason==Reason.ADD)
//           {
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //            if(originAddNode==null)
// //            {
// //              System.out.println("break");
// //            }
//             SymBgpRoute tempAddRouteUpper=cloneBgpRoute(originalAddRoute);
//             tempAddRouteUpper.setAsPathSize(addFinalUpper-tempAddRouteUpper.getAsPath().size()+1,tempAddRouteUpper.getAsPathSize().getUpperbound());
//             tempAddRouteUpper._reason=Reason.ADD;
//             originalAddRouteUpperList.add(tempAddRouteUpper);
//             _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,tempAddRouteUpper.getIndex(),tempAddRouteUpper.getAsPath().hashCode(),tempAddRouteUpper.getAsPathSize().hashCode(),tempAddRouteUpper.getAsPathSize(),tempAddRouteUpper.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRouteUpper.getIndex(),tempAddRouteUpper.getAsPath().hashCode(),tempAddRouteUpper.getAsPathSize().hashCode(),tempAddRouteUpper.getAsPathSize(),tempAddRouteUpper.getAsPath()),originAddNode);
// //            if(originAddNode.value.equals(_nodeName))
// //            {
// //              System.out.println("add-break");
// //            }

//             SymBgpRoute tempAddRouteLower=cloneBgpRoute(originalAddRoute);
//             tempAddRouteLower.setAsPathSize(tempAddRouteLower.getAsPathSize().getLowerbound(),addFinalLower-tempAddRouteLower.getAsPath().size()-1);
//             tempAddRouteLower._reason=Reason.ADD;
//             originalAddRouteLowerList.add(tempAddRouteLower);
//             _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,tempAddRouteLower.getIndex(),tempAddRouteLower.getAsPath().hashCode(),tempAddRouteLower.getAsPathSize().hashCode(),tempAddRouteLower.getAsPathSize(),tempAddRouteLower.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,tempAddRouteLower.getIndex(),tempAddRouteLower.getAsPath().hashCode(),tempAddRouteLower.getAsPathSize().hashCode(),tempAddRouteLower.getAsPathSize(),tempAddRouteLower.getAsPath()),originAddNode);

// //            if(originAddNode.value.equals(_nodeName))
// //            {
// //              System.out.println("add-break");
// //            }
//             _addTreeNodes.get(originAddNode).remove(thisNode);
//           }
//         }

//         for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());

//           if(originalRoute._reason==Reason.NORMAL)
//           {
//             TreeNode originalTreeNode=thisNode;
//             if(!_updateTreeNodes.containsKey(originalTreeNode))
//             {
//               _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//               _updateRouteIndex.put(originalTreeNode,originalRoute.getIndex());
//             }
//             originalRoute._reason=Reason.UPDATE;
//             originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),addFinalUpper-originalRoute.getAsPath().size());
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           }else if(originalRoute._reason==Reason.UPDATE)
//           {
//             TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//             if(originalTreeNode==null)
//             {
//               originalTreeNode=thisNode;
// //              System.out.println("break");
//             }

//             originalRoute._reason=Reason.UPDATE;
//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//             _updateTreeNodes.get(originalTreeNode).remove(thisNode);

//             originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),addFinalUpper-originalRoute.getAsPath().size());
//             _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//             _originalNodePairs.remove(thisNode);
//           }else if(originalRoute._reason==Reason.ADD)
//           {
//             originalRoute._reason=Reason.ADD;
//             TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //            if(originAddNode==null)
// //            {
// //              System.out.println("break");
// //            }
//             originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),addFinalUpper-originalRoute.getAsPath().size());
//             _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//             _addTreeNodes.get(originAddNode).remove(thisNode);
// //            if(originAddNode.value.equals(_nodeName))
// //            {
// //              System.out.println("add-break");
// //            }
//             _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),route.getAsPath()),originAddNode);
//             _originalNodePairs.remove(thisNode);
//           }
//         }

//         _rib.get(routeNetwork).add(location+1,originalAddRouteUpperList);
//         _rib.get(routeNetwork).add(location,originalAddRouteLowerList);


//         answer=processIsis(location+1,route,routeNetwork,addQueue);
//         return answer;
//       }
//     }

//     if(addFinalLower<=compareFinalLower&&
//         addFinalUpper>=compareFinalUpper)
//     {
//       if(addFinalUpper==compareFinalUpper)
//       {
//         SymBgpRoute addRouteLower=cloneBgpRoute(route);
//         TreeNode thisAddNode=new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath());
//         TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
// //        if(originAddNode==null)
// //        {
// //          System.out.println("break");
// //        }

//         if(route._reason==Reason.NORMAL||route._reason==Reason.ADD)
//         {
//           addRouteLower.setAsPathSize(addLowerAsPathSize,compareFinalLower-addConfirmAsPathSize-1);
//           route.setAsPathSize(compareFinalLower-addConfirmAsPathSize,addUpperAsPathSize);
//           _rib.get(routeNetwork).add(location,new ArrayList<>());
//           addRouteLower._reason=Reason.ADD;
//           _rib.get(routeNetwork).get(location).add(addRouteLower);


//           _addTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//           _addTreeNodes.get(originAddNode).remove(thisAddNode);
//           _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));

// //          if(originAddNode.value.equals(_nodeName))
// //          {
// //            System.out.println("add-break");
// //          }
//           _originalNodePairs.remove(thisAddNode);
//           _originalNodePairs.put(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),route.getAsPath()),originAddNode);
//           _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);
//         }else if(route._reason==Reason.UPDATE)
//         {
//           addRouteLower.setAsPathSize(addLowerAsPathSize,compareFinalLower-addConfirmAsPathSize-1);
//           route.setAsPathSize(compareFinalLower-addConfirmAsPathSize,addUpperAsPathSize);
//           _rib.get(routeNetwork).add(location,new ArrayList<>());
//           addRouteLower._reason=Reason.UPDATE;
//           _rib.get(routeNetwork).get(location).add(addRouteLower);

//           _updateTreeNodes.get(originAddNode).remove(thisAddNode);
//           _updateTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));

//           _originalNodePairs.remove(thisAddNode);
//           _originalNodePairs.put(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()),originAddNode);
//           _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);
//         }
//         answer=processIsis(location+1,route,routeNetwork,addQueue);
//         return answer;
//       }else if(addFinalLower==compareFinalLower)
//       {
//         SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//         TreeNode thisAddNode=new TreeNode(_nodeName,addRouteUpper.getIndex(),addRouteUpper.getAsPath().hashCode(),addRouteUpper.getAsPathSize().hashCode(),addRouteUpper.getAsPathSize(),addRouteUpper.getAsPath());
//         TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
// //        if(originAddNode==null)
// //        {
// //          System.out.println("break");
// //        }

//         addRouteUpper.setAsPathSize(compareFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);
//         addQueue.add(addRouteUpper);
//         route.setAsPathSize(addLowerAsPathSize,compareFinalUpper-addConfirmAsPathSize);
//         _originalNodePairs.remove(thisAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,addRouteUpper.getIndex(),addRouteUpper.getAsPath().hashCode(),addRouteUpper.getAsPathSize().hashCode(),addRouteUpper.getAsPathSize(),addRouteUpper.getAsPath()),originAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);

//         answer=processIsis(location,route,routeNetwork,addQueue);
//         return answer;
//       }else{
//         TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//         TreeNode originAddNode=_originalNodePairs.get(thisAddNode);

//         SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//         SymBgpRoute addRouteLower=cloneBgpRoute(route);
//         addRouteUpper.setAsPathSize(compareFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);
//         addRouteLower.setAsPathSize(addLowerAsPathSize,compareFinalLower-addConfirmAsPathSize-1);

//         _rib.get(routeNetwork).add(location,new ArrayList<>());
//         _rib.get(routeNetwork).get(location).add(addRouteLower);

//         addQueue.add(addRouteUpper);
//         route.setAsPathSize(compareFinalLower-addConfirmAsPathSize,compareFinalUpper-addConfirmAsPathSize);

//         if(addRouteLower._reason==Reason.NORMAL||addRouteLower._reason==Reason.ADD)
//         {
//           addRouteLower._reason=Reason.ADD;
//           _addTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//           _addTreeNodes.get(originAddNode).remove(thisAddNode);
//           _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));
// //          if(originAddNode.value.equals(_nodeName))
// //          {
// //            System.out.println("add-break");
// //          }
//         }else if(addRouteLower._reason==Reason.UPDATE)
//         {
//           _updateTreeNodes.get(originAddNode).remove(thisAddNode);
//           _updateTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));

//         }

//         _originalNodePairs.remove(thisAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()),originAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName, addRouteUpper.getIndex(), addRouteUpper.getAsPath().hashCode(),addRouteUpper.getAsPathSize().hashCode(),addRouteUpper.getAsPathSize(),addRouteUpper.getAsPath()),originAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);

//         answer=processIsis(location+1,route,routeNetwork,addQueue);
//         return answer;
//       }
//     }

//     if(addFinalLower<compareFinalLower&&
//         addFinalUpper<compareFinalUpper)
//     {
//       SymBgpRoute addRouteLower=cloneBgpRoute(route);
//       List<SymBgpRoute> originalAddRouteUpperList=new ArrayList<>();
//       for(SymBgpRoute originalAddRouteUpper:_rib.get(routeNetwork).get(location))
//       {
//         TreeNode thisNode=new TreeNode(_nodeName,originalAddRouteUpper.getIndex(),originalAddRouteUpper.getAsPath().hashCode(),originalAddRouteUpper.getAsPathSize().hashCode(),originalAddRouteUpper.getAsPathSize(),originalAddRouteUpper.getAsPath());
//         if(originalAddRouteUpper._reason==Reason.NORMAL)
//         {
//           TreeNode originalTreeNode=thisNode;
//           if(!_updateTreeNodes.containsKey(originalTreeNode))
//           {
//             _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//             _updateRouteIndex.put(originalTreeNode,originalAddRouteUpper.getIndex());
//           }
//           SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRouteUpper);
//           tempAddRoute.setAsPathSize(addFinalUpper-tempAddRoute.getAsPath().size()+1,tempAddRoute.getAsPathSize().getUpperbound());
//           tempAddRoute._reason=Reason.UPDATE;
//           originalAddRouteUpperList.add(tempAddRoute);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
//           _originalNodePairs.put(new TreeNode(_nodeName,
//               tempAddRoute.getIndex(), tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);

//         }else if(originalAddRouteUpper._reason==Reason.UPDATE)
//         {
//           TreeNode originalTreeNode=_originalNodePairs.get(thisNode);
//           if(originalTreeNode==null)
//           {
//             originalTreeNode=thisNode;
//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
// //            System.out.println("break");
//           }
//           SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRouteUpper);
//           tempAddRoute.setAsPathSize(addFinalUpper-tempAddRoute.getAsPath().size()+1,tempAddRoute.getAsPathSize().getUpperbound());
//           tempAddRoute._reason=Reason.UPDATE;
//           originalAddRouteUpperList.add(tempAddRoute);
//           _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));

//           _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);
//         }else if(originalAddRouteUpper._reason==Reason.ADD)
//         {
//           TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //          if(originAddNode==null)
// //          {
// //            System.out.println("break");
// //          }
//           SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRouteUpper);
//           tempAddRoute.setAsPathSize(addFinalUpper-tempAddRoute.getAsPath().size()+1,tempAddRoute.getAsPathSize().getUpperbound());
//           tempAddRoute._reason=Reason.ADD;
//           originalAddRouteUpperList.add(tempAddRoute);
//           _addTreeNodes.get(originAddNode).remove(thisNode);
//           _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
// //          if(originAddNode.value.equals(_nodeName))
// //          {
// //            System.out.println("add-break");
// //          }
//           _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originAddNode);
//         }
//       }
//       for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//       {
//         TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//         if(originalRoute._reason==Reason.NORMAL)
//         {
//           TreeNode originalTreeNode=thisNode;
//           if(!_updateTreeNodes.containsKey(originalTreeNode))
//           {
//             _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//             _updateRouteIndex.put(originalTreeNode,originalRoute.getIndex());
//           }
//           originalRoute._reason=Reason.UPDATE;
//           originalRoute.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),addFinalUpper-originalRoute.getAsPath().size());
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));

//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//         }else if(originalRoute._reason==Reason.UPDATE)
//         {
//           TreeNode originalTreeNode=_originalNodePairs.get(thisNode);
//           if(originalTreeNode==null)
//           {
//             originalTreeNode=thisNode;
//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
// //            System.out.println("break");
//           }
//           originalRoute._reason=Reason.UPDATE;
//           originalRoute.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),addFinalUpper-originalRoute.getAsPath().size());
//           _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));

//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           _originalNodePairs.remove(thisNode);
//         }else if(originalRoute._reason==Reason.ADD)
//         {
//           TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //          if(originAddNode==null)
// //          {
// //            System.out.println("break");
// //          }
//           originalRoute._reason=Reason.ADD;
//           originalRoute.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),addFinalUpper-originalRoute.getAsPath().size());
//           _addTreeNodes.get(originAddNode).remove(thisNode);
//           _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
// //          if(originAddNode.value.equals(_nodeName))
// //          {
// //            System.out.println("add-break");
// //          }
//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originAddNode);
//           _originalNodePairs.remove(thisNode);
//         }
//       }

//       TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
// //      if(originAddNode==null)
// //      {
// //        System.out.println("break");
// //      }

//       addRouteLower.setAsPathSize(addLowerAsPathSize,compareFinalLower-addConfirmAsPathSize-1);
//       route.setAsPathSize(compareFinalLower-addConfirmAsPathSize,addUpperAsPathSize);

//       _originalNodePairs.remove(thisAddNode);
//       _originalNodePairs.put(new TreeNode(_nodeName, addRouteLower.getIndex(), addRouteLower.getAsPath().hashCode(), addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()),originAddNode);
//       _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);

//       _rib.get(routeNetwork).add(location+1,originalAddRouteUpperList);

//       _rib.get(routeNetwork).add(location,new ArrayList<>());
//       if(addRouteLower._reason==Reason.NORMAL||addRouteLower._reason==Reason.ADD)
//       {
//         addRouteLower._reason=Reason.ADD;
//         _addTreeNodes.computeIfAbsent(originAddNode, k->new HashSet<>());
//         _addTreeNodes.get(originAddNode).remove(thisAddNode);
//         _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));
// //        if(originAddNode.value.equals(_nodeName))
// //        {
// //          System.out.println("add-break");
// //        }
//       }else if(addRouteLower._reason==Reason.UPDATE)
//       {
//         _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//         _updateTreeNodes.get(originAddNode).remove(thisAddNode);
//         _updateTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));
//       }
//       _rib.get(routeNetwork).get(location).add(addRouteLower);

//       answer=processIsis(location+1,route,routeNetwork,addQueue);
//       return answer;
//     }

//     if(addFinalLower>compareFinalLower&&
//         addFinalUpper>compareFinalUpper)
//     {
//       SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//       List<SymBgpRoute> originalAddRouteLowerList=new ArrayList<>();
//       for(SymBgpRoute originalAddRouteLower:_rib.get(routeNetwork).get(location))
//       {
//         TreeNode thisNode=new TreeNode(_nodeName,originalAddRouteLower.getIndex(),originalAddRouteLower.getAsPath().hashCode(),originalAddRouteLower.getAsPathSize().hashCode(),originalAddRouteLower.getAsPathSize(),originalAddRouteLower.getAsPath());
//         if(originalAddRouteLower._reason==Reason.NORMAL)
//         {
//           TreeNode originalTreeNode=thisNode;
//           if(!_updateTreeNodes.containsKey(originalTreeNode))
//           {
//             _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//             _updateRouteIndex.put(originalTreeNode,originalAddRouteLower.getIndex());
//           }
//           SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRouteLower);
//           tempAddRoute.setAsPathSize(tempAddRoute.getAsPathSize().getLowerbound(), addFinalLower-tempAddRoute.getAsPath().size()-1);
//           tempAddRoute._reason=Reason.UPDATE;
//           originalAddRouteLowerList.add(tempAddRoute);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
//           _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);

//         }else if(originalAddRouteLower._reason==Reason.UPDATE)
//         {
//           TreeNode originalTreeNode=_originalNodePairs.get(thisNode);
//           if(originalTreeNode==null)
//           {
//             originalTreeNode=thisNode;
//             _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
// //            System.out.println("break");
//           }
//           SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRouteLower);
//           tempAddRoute.setAsPathSize(tempAddRoute.getAsPathSize().getLowerbound(), addFinalLower-tempAddRoute.getAsPath().size()-1);
//           tempAddRoute._reason=Reason.UPDATE;
//           originalAddRouteLowerList.add(tempAddRoute);
//           _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));

//           _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);
//         }else if(originalAddRouteLower._reason==Reason.ADD)
//         {
//           TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //          if(originAddNode==null)
// //          {
// //            System.out.println("break");
// //          }
//           SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRouteLower);
//           tempAddRoute.setAsPathSize(tempAddRoute.getAsPathSize().getLowerbound(), addFinalLower-tempAddRoute.getAsPath().size()-1);
//           tempAddRoute._reason=Reason.ADD;
//           originalAddRouteLowerList.add(tempAddRoute);
//           _addTreeNodes.get(originAddNode).remove(thisNode);
//           _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
// //          if(originAddNode.value.equals(_nodeName))
// //          {
// //            System.out.println("add-break");
// //          }
//           _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originAddNode);
//         }
//       }

//       for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//       {
//         TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//         if(originalRoute._reason==Reason.NORMAL)
//         {
//           TreeNode originalTreeNode=thisNode;
//           if(!_updateTreeNodes.containsKey(originalTreeNode))
//           {
//             _updateTreeNodes.put(originalTreeNode,new HashSet<>());
//             _updateRouteIndex.put(originalTreeNode,originalRoute.getIndex());
//           }
//           originalRoute._reason=Reason.UPDATE;
//           originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),originalRoute.getAsPathSize().getUpperbound());
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));

//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//         }else if(originalRoute._reason==Reason.UPDATE)
//         {
//           TreeNode originalTreeNode=_originalNodePairs.get(thisNode);
//           if(originalTreeNode==null)
//           {
//             originalTreeNode=thisNode;
// //            System.out.println("break");
// //            System.out.println("break");
//           }
//           _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//           originalRoute._reason=Reason.UPDATE;
//           originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),originalRoute.getAsPathSize().getUpperbound());
//           _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,
//               originalRoute.getIndex(), originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));

//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           _originalNodePairs.remove(thisNode);
//         }else if(originalRoute._reason==Reason.ADD)
//         {
//           TreeNode originAddNode=_originalNodePairs.get(thisNode);
// //          if(originAddNode==null)
// //          {
// //            System.out.println("break");
// //          }
//           originalRoute._reason=Reason.ADD;
//           originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),originalRoute.getAsPathSize().getUpperbound());
//           _addTreeNodes.get(originAddNode).remove(thisNode);
//           _addTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
// //          if(originAddNode.value.equals(_nodeName))
// //          {
// //            System.out.println("add-break");
// //          }
//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originAddNode);
//           _originalNodePairs.remove(thisNode);
//         }
//       }

//       TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
// //      if(originAddNode==null)
// //      {
// //        System.out.println("break");
// //      }

//       addRouteUpper.setAsPathSize(compareFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);
//       route.setAsPathSize(addLowerAsPathSize,compareFinalUpper-addConfirmAsPathSize);

//       _originalNodePairs.remove(thisAddNode);
//       _originalNodePairs.put(new TreeNode(_nodeName, addRouteUpper.getIndex(), addRouteUpper.getAsPath().hashCode(),addRouteUpper.getAsPathSize().hashCode(),addRouteUpper.getAsPathSize(),addRouteUpper.getAsPath()),originAddNode);
//       _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);

//       _rib.get(routeNetwork).add(location,originalAddRouteLowerList);


//       addQueue.add(addRouteUpper);

//       answer=processIsis(location+1,route,routeNetwork,addQueue);
//       return answer;
//     }
//     return answer;
//   }

//   public SymBgpRib.Answer processAsPath(int location,SymBgpRoute route,Integer routeNetwork, Queue<SymBgpRoute> addQueue,Boolean withDraw)
//   {
//     SymBgpRib.Answer answer=new SymBgpRib.Answer();
//     int addUpperAsPathSize=route.getAsPathSize().getUpperbound();
//     int addLowerAsPathSize=route.getAsPathSize().getLowerbound();
//     int addConfirmAsPathSize=route.getAsPath().size();

//     int addFinalLower=addLowerAsPathSize+addConfirmAsPathSize;
//     int addFinalUpper=addUpperAsPathSize==Integer.MAX_VALUE?addUpperAsPathSize:addUpperAsPathSize+addConfirmAsPathSize;

//     SymBgpRoute compareRoute=_rib.get(routeNetwork).get(location).get(0);

//     int compareUpperAsPathSize=compareRoute.getAsPathSize().getUpperbound();
//     int compareLowerAsPathSize=compareRoute.getAsPathSize().getLowerbound();
//     int compareConfirmAsPathSize=compareRoute.getAsPath().size();

//     int compareFinalLower=compareLowerAsPathSize+compareConfirmAsPathSize;
//     int compareFinalUpper=compareUpperAsPathSize==Integer.MAX_VALUE?compareUpperAsPathSize:compareUpperAsPathSize+compareConfirmAsPathSize;

//     if(addFinalUpper<compareFinalLower)
//     {
//       _rib.get(routeNetwork).add(location,new ArrayList<>());
//       _rib.get(routeNetwork).get(location).add(route);

//       TreeNode thisNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
// //      if(originAddNode==null)
// //      {
// //        System.out.println("break");
// //      }
//       _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//       _updateTreeNodes.get(originAddNode).add(thisNode);

//       answer.alreadyInject=true;
//       answer.queue=addQueue;
//       return answer;
//     }

//     if(addFinalLower>compareFinalUpper)
//     {
//       answer.alreadyInject=false;
//       answer.location=location+1;
//       answer.queue=addQueue;
//       return answer;
//     }

//     if(addFinalLower==compareFinalLower&&addFinalUpper==compareFinalUpper)
//     {
//       return processIsis(location,route,routeNetwork,addQueue,true);
//     }

//     if(addFinalUpper<=compareFinalUpper&&addFinalLower>=compareFinalLower)
//     {
//       if(addFinalUpper==compareFinalUpper)
//       {
//         List<SymBgpRoute> originalAddRouteLowerList=new ArrayList<>();
//         for(SymBgpRoute originalAddRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalAddRoute.getIndex(),originalAddRoute.getAsPath().hashCode(),originalAddRoute.getAsPathSize().hashCode(),originalAddRoute.getAsPathSize(),originalAddRoute.getAsPath());
//           TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//           SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRoute);
//           _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//           tempAddRoute.setAsPathSize(tempAddRoute.getAsPathSize().getLowerbound(),addFinalLower-tempAddRoute.getAsPath().size()-1);
//           if(tempAddRoute._reason==Reason.NORMAL)
//           {
//             tempAddRoute._reason=Reason.UPDATE;
//           }
//           originalAddRouteLowerList.add(tempAddRoute);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));

//           _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);
//         }
//         for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//           TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//           if(originalRoute._reason==Reason.NORMAL)
//           {
//             originalRoute._reason=Reason.UPDATE;
//           }
//           originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),originalRoute.getAsPathSize().getUpperbound());
//           _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           _originalNodePairs.remove(thisNode);
//           _updateTreeNodes.get(originalTreeNode).remove(thisNode);

//         }
//         _rib.get(routeNetwork).add(location,originalAddRouteLowerList);

//         //这里肯定是直接相等的情况，但是直接比ISIS有些困难，所以再返回继续比较localpreference，再回来比较这个As-Path相等，然后再比较ISIS，后续这里可能可以优化。
//         answer=processIsis(location+1,route,routeNetwork,addQueue,true);
//         return answer;
//       }else if(addFinalLower==compareFinalLower)
//       {
//         List<SymBgpRoute> originalAddRouteUpperList=new ArrayList<>();
//         for(SymBgpRoute originalAddRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalAddRoute.getIndex(),originalAddRoute.getAsPath().hashCode(),originalAddRoute.getAsPathSize().hashCode(),originalAddRoute.getAsPathSize(),originalAddRoute.getAsPath());
//           TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//           _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//           SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRoute);
//           tempAddRoute.setAsPathSize(addFinalUpper-tempAddRoute.getAsPath().size()+1,tempAddRoute.getAsPathSize().getUpperbound());
//           if(tempAddRoute._reason==Reason.NORMAL)
//           {
//             tempAddRoute._reason=Reason.UPDATE;
//           }
//           originalAddRouteUpperList.add(tempAddRoute);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));

//           _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);
//         }
//         for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//           TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//           _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//           if(originalRoute._reason==Reason.NORMAL)
//           {
//             originalRoute._reason=Reason.UPDATE;
//           }
//           originalRoute.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),addFinalUpper-originalRoute.getAsPath().size());
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           _originalNodePairs.remove(thisNode);
//           _updateTreeNodes.get(originalTreeNode).remove(thisNode);

//         }
//         _rib.get(routeNetwork).add(location+1,originalAddRouteUpperList);

//         //这里肯定是直接相等的情况，但是直接比ISIS有些困难，所以再返回继续比较localpreference，再回来比较这个As-Path相等，然后再比较ISIS，后续这里可能可以优化。
//         answer=processIsis(location,route,routeNetwork,addQueue,true);
//         return answer;
//       }else{
//         List<SymBgpRoute> originalAddRouteUpperList=new ArrayList<>();
//         List<SymBgpRoute> originalAddRouteLowerList=new ArrayList<>();
//         for(SymBgpRoute originalAddRoute:_rib.get(routeNetwork).get(location)) {
//           TreeNode thisNode = new TreeNode(_nodeName, originalAddRoute.getIndex(), originalAddRoute.getAsPath().hashCode(), originalAddRoute.getAsPathSize().hashCode(), originalAddRoute.getAsPathSize(),originalAddRoute.getAsPath());
//           TreeNode originalTreeNode = _originalNodePairs.getOrDefault(thisNode,thisNode);
//           _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//           SymBgpRoute tempAddRouteUpper = cloneBgpRoute(originalAddRoute);
//           tempAddRouteUpper.setAsPathSize(addFinalUpper - tempAddRouteUpper.getAsPath().size() + 1, tempAddRouteUpper.getAsPathSize().getUpperbound());
//           if(tempAddRouteUpper._reason==Reason.NORMAL)
//           {
//             tempAddRouteUpper._reason = Reason.UPDATE;
//           }
//           originalAddRouteUpperList.add(tempAddRouteUpper);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName, tempAddRouteUpper.getIndex(), tempAddRouteUpper.getAsPath().hashCode(), tempAddRouteUpper.getAsPathSize().hashCode(), tempAddRouteUpper.getAsPathSize(),tempAddRouteUpper.getAsPath()));
//           _originalNodePairs.put(new TreeNode(_nodeName,tempAddRouteUpper.getIndex(),tempAddRouteUpper.getAsPath().hashCode(), tempAddRouteUpper.getAsPathSize().hashCode(), tempAddRouteUpper.getAsPathSize(),tempAddRouteUpper.getAsPath()), originalTreeNode);

//           SymBgpRoute tempAddRouteLower = cloneBgpRoute(originalAddRoute);
//           tempAddRouteLower.setAsPathSize(tempAddRouteLower.getAsPathSize().getLowerbound(), addFinalLower - tempAddRouteLower.getAsPath().size() - 1);
//           if(tempAddRouteLower._reason==Reason.NORMAL)
//           {
//             tempAddRouteLower._reason = Reason.UPDATE;
//           }
//           originalAddRouteLowerList.add(tempAddRouteLower);
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName, tempAddRouteLower.getIndex(), tempAddRouteLower.getAsPath().hashCode(), tempAddRouteLower.getAsPathSize().hashCode(), tempAddRouteLower.getAsPathSize(),tempAddRouteLower.getAsPath()));
//           _originalNodePairs.put(new TreeNode(_nodeName, tempAddRouteLower.getIndex(), tempAddRouteLower.getAsPath().hashCode(), tempAddRouteLower.getAsPathSize().hashCode(), tempAddRouteLower.getAsPathSize(),tempAddRouteLower.getAsPath()), originalTreeNode);

//         }

//         for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//         {
//           TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//           TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//           _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//           if(originalRoute._reason==Reason.NORMAL)
//           {
//             originalRoute._reason=Reason.UPDATE;
//           }
//           originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),addFinalUpper-originalRoute.getAsPath().size());
//           _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//           _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//           _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//           _originalNodePairs.remove(thisNode);

//         }

//         _rib.get(routeNetwork).add(location+1,originalAddRouteUpperList);
//         _rib.get(routeNetwork).add(location,originalAddRouteLowerList);





//         answer=processIsis(location+1,route,routeNetwork,addQueue,true);
//         return answer;
//       }
//     }

//     if(addFinalLower<=compareFinalLower&&
//         addFinalUpper>=compareFinalUpper)
//     {
//       if(addFinalUpper==compareFinalUpper)
//       {
//         if(route._reason==Reason.NORMAL)
//         {
//           route._reason=Reason.UPDATE;
//         }
//         SymBgpRoute addRouteLower=cloneBgpRoute(route);
//         TreeNode thisAddNode=new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath());
//         TreeNode originAddNode=_originalNodePairs.getOrDefault(thisAddNode,thisAddNode);
// //        if(originAddNode==null)
// //        {
// //          System.out.println("break");
// //        }

//         addRouteLower.setAsPathSize(addLowerAsPathSize,compareFinalLower-addConfirmAsPathSize-1);
//         route.setAsPathSize(compareFinalLower-addConfirmAsPathSize,addUpperAsPathSize);
//         _rib.get(routeNetwork).add(location,new ArrayList<>());
//         _rib.get(routeNetwork).get(location).add(addRouteLower);

//         _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//         _updateTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));
//         _originalNodePairs.put(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()),originAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);
//         _originalNodePairs.remove(thisAddNode);
//         _updateTreeNodes.get(originAddNode).remove(thisAddNode);


//         answer=processIsis(location+1,route,routeNetwork,addQueue,true);
//         return answer;
//       }else if(addFinalLower==compareFinalLower)
//       {
//         if(route._reason==Reason.NORMAL)
//         {
//           route._reason=Reason.UPDATE;
//         }
//         SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//         TreeNode thisAddNode=new TreeNode(_nodeName,addRouteUpper.getIndex(),addRouteUpper.getAsPath().hashCode(),addRouteUpper.getAsPathSize().hashCode(),addRouteUpper.getAsPathSize(),addRouteUpper.getAsPath());
//         TreeNode originAddNode=_originalNodePairs.getOrDefault(thisAddNode,thisAddNode);

// //        if(originAddNode==null)
// //        {
// //          System.out.println("break");
// //        }

//         addRouteUpper.setAsPathSize(compareFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);
//         addQueue.add(addRouteUpper);
//         route.setAsPathSize(addLowerAsPathSize,compareFinalUpper-addConfirmAsPathSize);
//         _originalNodePairs.remove(thisAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,addRouteUpper.getIndex(),addRouteUpper.getAsPath().hashCode(),addRouteUpper.getAsPathSize().hashCode(),addRouteUpper.getAsPathSize(),addRouteUpper.getAsPath()),originAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);

//         answer=processIsis(location,route,routeNetwork,addQueue,true);
//         return answer;
//       }else{
//         if(route._reason==Reason.NORMAL)
//         {
//           route._reason=Reason.UPDATE;
//         }
//         TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//         TreeNode originAddNode=_originalNodePairs.getOrDefault(thisAddNode,thisAddNode);

//         SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//         SymBgpRoute addRouteLower=cloneBgpRoute(route);
//         addRouteUpper.setAsPathSize(compareFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);
//         addRouteLower.setAsPathSize(addLowerAsPathSize,compareFinalLower-addConfirmAsPathSize-1);

//         _rib.get(routeNetwork).add(location,new ArrayList<>());
//         _rib.get(routeNetwork).get(location).add(addRouteLower);
//         addQueue.add(addRouteUpper);
//         route.setAsPathSize(compareFinalLower-addConfirmAsPathSize,compareFinalUpper-addConfirmAsPathSize);

//         _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//         _updateTreeNodes.get(originAddNode).add(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));
//         _updateTreeNodes.get(originAddNode).remove(thisAddNode);


//         _originalNodePairs.remove(thisAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,addRouteLower.getIndex(),addRouteLower.getAsPath().hashCode(),addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()),originAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName, addRouteUpper.getIndex(), addRouteUpper.getAsPath().hashCode(),addRouteUpper.getAsPathSize().hashCode(),addRouteUpper.getAsPathSize(),addRouteUpper.getAsPath()),originAddNode);
//         _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);

//         answer=processIsis(location+1,route,routeNetwork,addQueue,true);
//         return answer;
//       }
//     }

//     if(addFinalLower<compareFinalLower&&
//         addFinalUpper<compareFinalUpper)
//     {
//       List<SymBgpRoute> originalAddRouteUpperList=new ArrayList<>();
//       for(SymBgpRoute originalAddRouteUpper:_rib.get(routeNetwork).get(location))
//       {
//         TreeNode thisNode=new TreeNode(_nodeName,originalAddRouteUpper.getIndex(),originalAddRouteUpper.getAsPath().hashCode(),originalAddRouteUpper.getAsPathSize().hashCode(),originalAddRouteUpper.getAsPathSize(),originalAddRouteUpper.getAsPath());
//         TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//         SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRouteUpper);
//         tempAddRoute.setAsPathSize(addFinalUpper-tempAddRoute.getAsPath().size()+1,tempAddRoute.getAsPathSize().getUpperbound());
//         if(tempAddRoute._reason==Reason.NORMAL)
//         {
//           tempAddRoute._reason=Reason.UPDATE;
//         }
//         originalAddRouteUpperList.add(tempAddRoute);
//         _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//         _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));


//         _originalNodePairs.put(new TreeNode(_nodeName, tempAddRoute.getIndex(), tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);
//       }
//       for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//       {
//         TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());

//         TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//         if(originalRoute._reason==Reason.NORMAL)
//         {
//           originalRoute._reason=Reason.UPDATE;
//         }
//         originalRoute.setAsPathSize(originalRoute.getAsPathSize().getLowerbound(),addFinalUpper-originalRoute.getAsPath().size());
//         _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//         _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//         _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//         _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//         _originalNodePairs.remove(thisNode);

//       }

//       if(route._reason==Reason.NORMAL)
//       {
//         route._reason=Reason.UPDATE;
//       }

//       SymBgpRoute addRouteLower=cloneBgpRoute(route);
//       TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.getOrDefault(thisAddNode,thisAddNode);
// //      if(originAddNode==null)
// //      {
// //        System.out.println("break");
// //      }

//       addRouteLower.setAsPathSize(addLowerAsPathSize,compareFinalLower-addConfirmAsPathSize-1);
//       route.setAsPathSize(compareFinalLower-addConfirmAsPathSize,addUpperAsPathSize);

//       _rib.get(routeNetwork).add(location+1,originalAddRouteUpperList);

//       _rib.get(routeNetwork).add(location,new ArrayList<>());
//       _rib.get(routeNetwork).get(location).add(addRouteLower);

//       _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//       _updateTreeNodes.get(originAddNode).add(new TreeNode(_nodeName, addRouteLower.getIndex(), addRouteLower.getAsPath().hashCode(), addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()));
//       _updateTreeNodes.get(originAddNode).remove(thisAddNode);


//       _originalNodePairs.remove(thisAddNode);
//       _originalNodePairs.put(new TreeNode(_nodeName, addRouteLower.getIndex(), addRouteLower.getAsPath().hashCode(), addRouteLower.getAsPathSize().hashCode(),addRouteLower.getAsPathSize(),addRouteLower.getAsPath()),originAddNode);
//       _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);

//       answer=processIsis(location+1,route,routeNetwork,addQueue,true);
//       return answer;
//     }

//     if(addFinalLower>compareFinalLower&&
//         addFinalUpper>compareFinalUpper)
//     {
//       List<SymBgpRoute> originalAddRouteLowerList=new ArrayList<>();
//       for(SymBgpRoute originalAddRouteLower:_rib.get(routeNetwork).get(location))
//       {
//         TreeNode thisNode=new TreeNode(_nodeName,originalAddRouteLower.getIndex(),originalAddRouteLower.getAsPath().hashCode(),originalAddRouteLower.getAsPathSize().hashCode(),originalAddRouteLower.getAsPathSize(),originalAddRouteLower.getAsPath());
//         TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//         SymBgpRoute tempAddRoute=cloneBgpRoute(originalAddRouteLower);
//         tempAddRoute.setAsPathSize(tempAddRoute.getAsPathSize().getLowerbound(), addFinalLower-tempAddRoute.getAsPath().size()-1);
//         if(tempAddRoute._reason==Reason.NORMAL)
//         {
//           tempAddRoute._reason=Reason.UPDATE;
//         }
//         originalAddRouteLowerList.add(tempAddRoute);
//         _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//         _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()));
//         _originalNodePairs.put(new TreeNode(_nodeName,tempAddRoute.getIndex(),tempAddRoute.getAsPath().hashCode(),tempAddRoute.getAsPathSize().hashCode(),tempAddRoute.getAsPathSize(),tempAddRoute.getAsPath()),originalTreeNode);

//       }

//       for(SymBgpRoute originalRoute:_rib.get(routeNetwork).get(location))
//       {
//         TreeNode thisNode=new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath());
//         TreeNode originalTreeNode=_originalNodePairs.getOrDefault(thisNode,thisNode);
//         if(originalRoute._reason==Reason.NORMAL)
//         {
//           originalRoute._reason=Reason.UPDATE;
//         }
//         originalRoute.setAsPathSize(addFinalLower-originalRoute.getAsPath().size(),originalRoute.getAsPathSize().getUpperbound());
//         _updateTreeNodes.computeIfAbsent(originalTreeNode,k->new HashSet<>());
//         _updateTreeNodes.get(originalTreeNode).add(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()));
//         _originalNodePairs.put(new TreeNode(_nodeName,originalRoute.getIndex(),originalRoute.getAsPath().hashCode(),originalRoute.getAsPathSize().hashCode(),originalRoute.getAsPathSize(),originalRoute.getAsPath()),originalTreeNode);
//         _updateTreeNodes.get(originalTreeNode).remove(thisNode);
//         _originalNodePairs.remove(thisNode);

//       }

//       if(route._reason==Reason.NORMAL)
//       {
//         route._reason=Reason.UPDATE;
//       }
//       SymBgpRoute addRouteUpper=cloneBgpRoute(route);
//       TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.getOrDefault(thisAddNode,thisAddNode);
// //      if(originAddNode==null)
// //      {
// //        System.out.println("break");
// //      }

//       addRouteUpper.setAsPathSize(compareFinalUpper-addConfirmAsPathSize+1,addUpperAsPathSize);
//       route.setAsPathSize(addLowerAsPathSize,compareFinalUpper-addConfirmAsPathSize);

//       _originalNodePairs.remove(thisAddNode);
//       _originalNodePairs.put(new TreeNode(_nodeName, addRouteUpper.getIndex(), addRouteUpper.getAsPath().hashCode(),addRouteUpper.getAsPathSize().hashCode(),addRouteUpper.getAsPathSize(),addRouteUpper.getAsPath()),originAddNode);
//       _originalNodePairs.put(new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath()),originAddNode);

//       _rib.get(routeNetwork).add(location,originalAddRouteLowerList);

//       addQueue.add(addRouteUpper);

//       answer=processIsis(location+1,route,routeNetwork,addQueue,true);
//       return answer;
//     }
//     return answer;
//   }

//   public SymBgpRib.Answer processIsis(int location,SymBgpRoute route,Integer routeNetwork,Queue<SymBgpRoute> addQueue,Boolean withDraw)
//   {
//     SymBgpRib.Answer answer=new SymBgpRib.Answer();
//     SymBgpRoute compareRoute=_rib.get(routeNetwork).get(location).get(0);
//     int comparePreference=compareRoute.getOriginatorIp().compareTo(route.getOriginatorIp());
//     if(comparePreference!=0)
//     {
//       if(comparePreference<0)
//       {
//         answer.alreadyInject=false;
//         answer.location=location+1;
//         answer.queue=addQueue;
//         return answer;
//       }else{
//         if(route._reason==Reason.NORMAL)
//         {
//           route._reason=Reason.UPDATE;
//         }
//         TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//         TreeNode originAddNode=_originalNodePairs.getOrDefault(thisAddNode,thisAddNode);

//         _rib.get(routeNetwork).add(location,new ArrayList<>());
//         _rib.get(routeNetwork).get(location).add(route);

//         _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//         _updateTreeNodes.get(originAddNode).add(thisAddNode);

//         answer.alreadyInject=true;
//         answer.location=location;
//         answer.queue=addQueue;
//         return answer;
//       }
//     }

//     //to implement clusterlist

//     comparePreference=compareRoute.getReceivedFromIp().compareTo(route.getReceivedFromIp());
//     if(comparePreference!=0)
//     {
//       if(comparePreference<0)
//       {
//         answer.alreadyInject=false;
//         answer.location=location+1;
//         answer.queue=addQueue;
//         return answer;
//       }else{
//         if(route._reason==Reason.NORMAL)
//         {
//           route._reason=Reason.UPDATE;
//         }
//         TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//         TreeNode originAddNode=_originalNodePairs.getOrDefault(thisAddNode,thisAddNode);

//         _rib.get(routeNetwork).add(location,new ArrayList<>());
//         _rib.get(routeNetwork).get(location).add(route);

//         _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//         _updateTreeNodes.get(originAddNode).add(thisAddNode);

//         answer.alreadyInject=true;
//         answer.location=location;
//         answer.queue=addQueue;
//         return answer;
//       }
//     }


//     if(route._reason==Reason.NORMAL)
//     {
//       route._reason=Reason.UPDATE;
//     }
//     _rib.get(routeNetwork).get(location).add(route);

//     TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//     TreeNode originAddNode=_originalNodePairs.getOrDefault(thisAddNode,thisAddNode);

//     _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//     _updateTreeNodes.get(originAddNode).add(thisAddNode);

//     answer.alreadyInject=true;
//     answer.location=location;
//     answer.queue=addQueue;
//     return answer;
//   }


//   public SymBgpRib.Answer processIsis(int location,SymBgpRoute route,Integer routeNetwork,Queue<SymBgpRoute> addQueue)
//   {
//     SymBgpRib.Answer answer=new SymBgpRib.Answer();
//     SymBgpRoute compareRoute=_rib.get(routeNetwork).get(location).get(0);
//     int comparePreference=compareRoute.getOriginatorIp().compareTo(route.getOriginatorIp());
//     if(comparePreference!=0)
//     {
//       if(comparePreference<0)
//       {
//         answer.alreadyInject=false;
//         answer.location=location+1;
//         answer.queue=addQueue;
//         return answer;
//       }else{
//         if(route._reason==Reason.NORMAL||route._reason==Reason.ADD)
//         {
//           route._reason=Reason.ADD;
//           TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//           TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
//           _addTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//           _addTreeNodes.get(originAddNode).add(thisAddNode);
// //          if(originAddNode.value.equals(thisAddNode.value))
// //          {
// //            System.out.println("add-break");
// //          }
//         }else if(route._reason==Reason.UPDATE)
//         {
//           TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//           TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
//           _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//           _updateTreeNodes.get(originAddNode).add(thisAddNode);
//           if(!originAddNode.value.equals(thisAddNode.value))
//           {
//             System.out.println("update-break");
//           }
//         }
//         _rib.get(routeNetwork).add(location,new ArrayList<>());
//         _rib.get(routeNetwork).get(location).add(route);

//         answer.alreadyInject=true;
//         answer.location=location;
//         answer.queue=addQueue;
//         return answer;
//       }
//     }

//     //to implement clusterlist

//     comparePreference=compareRoute.getReceivedFromIp().compareTo(route.getReceivedFromIp());
//     if(comparePreference!=0)
//     {
//       if(comparePreference<0)
//       {
//         answer.alreadyInject=false;
//         answer.location=location+1;
//         answer.queue=addQueue;
//         return answer;
//       }else{
//         if(route._reason==Reason.NORMAL||route._reason==Reason.ADD)
//         {
//           route._reason=Reason.ADD;
//           TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//           TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
//           _addTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//           _addTreeNodes.get(originAddNode).add(thisAddNode);
//         }else if(route._reason==Reason.UPDATE)
//         {
//           TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//           TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
//           _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//           _updateTreeNodes.get(originAddNode).add(thisAddNode);
//         }
//         _rib.get(routeNetwork).add(location,new ArrayList<>());
//         _rib.get(routeNetwork).get(location).add(route);

//         answer.alreadyInject=true;
//         answer.location=location;
//         answer.queue=addQueue;
//         return answer;
//       }
//     }


//     if(route._reason==Reason.NORMAL||route._reason==Reason.ADD)
//     {
//       route._reason=Reason.ADD;
//       TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
//       _addTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//       _addTreeNodes.get(originAddNode).add(thisAddNode);
//     }else if(route._reason==Reason.UPDATE)
//     {
//       TreeNode thisAddNode=new TreeNode(_nodeName,route.getIndex(),route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize(),route.getAsPath());
//       TreeNode originAddNode=_originalNodePairs.get(thisAddNode);
//       _updateTreeNodes.computeIfAbsent(originAddNode,k->new HashSet<>());
//       _updateTreeNodes.get(originAddNode).add(thisAddNode);
//     }
//     _rib.get(routeNetwork).get(location).add(route);

//     answer.alreadyInject=true;
//     answer.location=location;
//     answer.queue=addQueue;
//     return answer;

// //    if(compareRoute.getIsisMetric()<route.getIsisMetric())
// //    {
// //      answer.alreadyInject=false;
// //      answer.location=location+1;
// //      answer.queue=addQueue;
// //      return answer;
// //    }else if(compareRoute.getIsisMetric()>route.getIsisMetric())
// //    {
// //      if(route._reason==Reason.NORMAL)
// //      {
// //        route._reason=Reason.ADD;
// //        _addTreeNodes.add(new TreeNode(_nodeName,route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize()));
// //      }
// //      _rib.get(routeNetwork).add(location,new ArrayList<>());
// //      _rib.get(routeNetwork).get(location).add(route);
// //
// //      answer.alreadyInject=true;
// //      answer.location=location;
// //      answer.queue=addQueue;
// //    }else{
// //      if(route._reason==Reason.NORMAL)
// //      {
// //        route._reason=Reason.ADD;
// //        _addTreeNodes.add(new TreeNode(_nodeName,route.getAsPath().hashCode(),route.getAsPathSize().hashCode(),route.getAsPathSize()));
// //      }
// //      _rib.get(routeNetwork).get(location).add(route);
// //
// //      answer.alreadyInject=true;
// //      answer.location=location;
// //      answer.queue=addQueue;
// //      return answer;
// //    }
// //    return answer;
//   }

//   public Map<Integer,List<SymBgpRoute>> getSimplifiedRoutes()
//   {
//     Map<Integer,List<SymBgpRoute>> routes=new HashMap<>();
//     for(Integer prefix:_rib.keySet())
//     {
//       List<SymBgpRoute> routeList=new ArrayList<>();
//       for(List<SymBgpRoute> routeTemp:_rib.get(prefix))
//       {
//         routeList.addAll(routeTemp);
//       }
//       routes.put(prefix,routeList);
//     }
//     return routes;
//   }
//   public Integer getSimplifiedRoutesHashCode()
//   {
//     Integer hashcode=1;
//     for(Integer prefix:_rib.keySet())
//     {
//       List<SymBgpRoute> routeList=new ArrayList<>();
//       for(List<SymBgpRoute> routeTemp:_rib.get(prefix))
//       {
//         for(SymBgpRoute route:routeTemp)
//         {
//           hashcode=hashcode*31+route.getAsPath().hashCode();
//         }
//       }
//     }
//     return hashcode;
//   }
//   public SymBgpRoute cloneBgpRoute(SymBgpRoute route)
//   {
//     SymBgpRoute answerRoute=new SymBgpRoute(route._prefixEcNum,route.getTopologyBDD(),route.getPrefixFactory());
//     answerRoute.setAsPath(route.getAsPath());
//     SymBounder bounder= route.getAsPathSize();
//     answerRoute.setAsPathSize(route.getAsPathSize().getLowerbound(),route.getAsPathSize().getUpperbound());
//     answerRoute.setTopologyCondition(route.getTopologyCondition());
//     answerRoute.setLocalPreference(route.getLocalPreference().getLowerbound(),route.getLocalPreference().getUpperbound());
//     answerRoute.setNextHopIp(route.getNextHopIp());
//     answerRoute.setOriginatorIp(route.getOriginatorIp());
//     answerRoute.setOriginType(route.getOriginType());
//     answerRoute.setEgpProtocol(route.getEgpProtocol());
//     answerRoute.setIgpProtocol(route.getIgpProtocol());
//     answerRoute.setSrcProtocol(route.getSrcProtocol());
//     answerRoute.setReceivedFromIp(route.getReceivedFromIp());
//     answerRoute._symCommunities=new SymCommunity(route._symCommunities);
//     answerRoute._reason=route._reason;
//     answerRoute._external=route._external;
//     answerRoute.setSrcNode(route.getSrcNode());
//     answerRoute._relatedNode.addAll(route.getRelatedNode());
//     answerRoute.setPrefixEcNum(route.getPrefixEcNum());
//     answerRoute.setSrcAcl(route.getSrcAcl());
//     answerRoute.setDstAcl(route.getDstAcl());

//     answerRoute._outBGPTopologyCondition=route._outBGPTopologyCondition;
//     answerRoute._outISISTopologyCondition=route._outISISTopologyCondition;

//     answerRoute.setIsisMetric(route.getIsisMetric());
//     answerRoute.setNextNode(route.getNextNode());
//     answerRoute.setNodePath(route.getNodePath());
//     if(route.getIsisLevel()!=null)
//     {
//       if(route.getIsisLevel().includes(IsisLevel.LEVEL_1)&&route.getIsisLevel().includes(IsisLevel.LEVEL_2))
//       {
//         answerRoute.setIsisLevel(IsisLevel.union(IsisLevel.LEVEL_1,IsisLevel.LEVEL_2));
//       }else if(route.getIsisLevel().includes(IsisLevel.LEVEL_2)){
//         answerRoute.setIsisLevel(IsisLevel.LEVEL_2);
//       }
//     }
//     answerRoute._lastLocalPreference=new SymLocalPreference(route._lastLocalPreference.getLowerBound(),route._lastLocalPreference.getUpperBound());
//     answerRoute._lastCommunities=new SymCommunity(route._symCommunities);
//     answerRoute._asNodePath=new ArrayList<>();
//     answerRoute._asNodePath.addAll(route._asNodePath);
// //    answerRoute._lastCommunities.addAll(route.getCommunities());
//     answerRoute.setIndex(route.getIndex());
//     return answerRoute;
//   }

//   public Map<Integer,List<List<SymBgpRoute>>> getRib()
//   {
//     return _rib;
//   }

// //  public void processRibForEnd(ImmutableSortedMap<String, Node> nodes)
// //  {
// //    for(List<List<SymBgpRoute>> routeList:_rib.values())
// //    {
// //      for(List<SymBgpRoute> routes:routeList)
// //      {
// //        for(SymBgpRoute route:routes)
// //        {
// //          if(route.getRelatedNode().size()!=0)
// //          {
// //            AsPath asPath=route.getAsPath();
// //            asPath.toString();
// //          }
// //        }
// //      }
// //    }
// //  }

//   public Set<SymBgpRoute> getWithDrawRoute()
//   {
//     return _withDrawRoute;
//   }

//   public HashMap<TreeNode,Set<TreeNode>> getUpdateTreeNodes()
//   {
//     return _updateTreeNodes;
//   }

//   public HashMap<TreeNode, Set<TreeNode>> getAddTreeNodes()
//   {
//     return _addTreeNodes;
//   }

//   public HashMap<TreeNode, Set<TreeNode>> getReplaceTreeNodes() {
//     return _replaceTreeNodes;
//   }

//   public HashMap<TreeNode, Integer> getUpdateRouteIndex() {
//     return _updateRouteIndex;
//   }

//   public void clearTreeNodes()
//   {
//     _updateTreeNodes.clear();
//     _addTreeNodes.clear();
//     _replaceTreeNodes.clear();
//     _updateRouteIndex.clear();
//     _originalNodePairs.clear();
//   }

//   public void clearDeleteTreeNodes()
//   {
//     _deleteTreeNodes.clear();
//   }

//   public HashMap<Integer, Set<TreeNode>> getDeleteTreeNodes()
//   {
//     return _deleteTreeNodes;
//   }

//   public void clearChangedRoute()
//   {
//     _changedRoute.clear();
//   }

//   public void clearWithDrawRoute()
//   {
//     _withDrawRoute.clear();
//   }

//   public synchronized int andBDD(int tc1,int tc2)
//   {
//     synchronized (_topologyBDD)
//     {
//       return _topologyBDD.and(tc1,tc2);
//     }
//   }
}
