package org.batfish.isis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.datamodel.IsisRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.symwork.Reason;

public class IsisMultiRib {
  HashMap<Prefix, List<IsisRoute>> _rib;
  Set<Prefix> _changedPrefix;
  BDDFactory _edgeBddFactory;
  public IsisMultiRib()
  {
    _rib=new HashMap<>();
    _changedPrefix=new HashSet<>();
  }
  public void initIsisMultiRib()
  {
    _changedPrefix=new HashSet<>();
  }
  public void updateRoute(IsisRoute route)
  {
    if(!_rib.containsKey(route.getNetwork()))
    {
      _rib.put(route.getNetwork(),new ArrayList<>());
      _rib.get(route.getNetwork()).add(route);
    }else
    {
      List<IsisRoute> routeList=_rib.get(route.getNetwork());
      if(routeList.contains(route))
      {
        int pos=routeList.indexOf(route);
        route._reason=Reason.UPDATE;
        routeList.add(pos,route);
        routeList.remove(pos+1);
        return ;
      }
    }
  }
  public void addRoute(IsisRoute route)
  {
    if(!_rib.containsKey(route.getNetwork()))
    {
      _rib.put(route.getNetwork(),new ArrayList<>());
      _rib.get(route.getNetwork()).add(route);
      _changedPrefix.add(route.getNetwork());
    }else{
      List<IsisRoute> routeList=_rib.get(route.getNetwork());
      _changedPrefix.add(route.getNetwork());
      if(routeList.contains(route))
      {
        int pos=routeList.indexOf(route);
        route._reason=Reason.UPDATE;
        routeList.add(pos,route);
        routeList.remove(pos+1);
        return ;
      }
      int position=0;
      for(int i=0;i<routeList.size();i++)
      {
        IsisRoute tempRoute=routeList.get(i);

        if(tempRoute.getAdministrativeCost()<route.getAdministrativeCost())
        {
          position++;
          continue;
        }else if(tempRoute.getAdministrativeCost()==route.getAdministrativeCost())
        {
          if(tempRoute.getMetric()<=route.getMetric())
          {
            position++;
            continue;
          }else
          {
            break;
          }
        }else{
          break;
        }
      }
      route._reason= Reason.ADD;
      routeList.add(position,route);
    }
  }
  public List<IsisRoute.Builder> getChangedRoute()
  {
    List<IsisRoute.Builder> answer=new ArrayList<>();
    for(Prefix prefix:_changedPrefix)
    {
      List<IsisRoute> changedRoutes=_rib.get(prefix);
      boolean firstChanged=false;
      for(int i=0;i<changedRoutes.size();i++)
      {
        if(changedRoutes.get(i)._reason==Reason.NORMAL&&!firstChanged)
        {
          continue;
        }else{
          firstChanged=true;
          BDD tc=_edgeBddFactory.one();
          changedRoutes.get(i)._reason=Reason.NORMAL;
          for(int j=0;j<i;j++)
          {
            if(changedRoutes.get(j).getMetric()==changedRoutes.get(i).getMetric())
            {
              continue;
            }else{
              tc=tc.and(changedRoutes.get(j).getTopologyCondition().not());
            }
          }
          if(tc.isZero())
          {
            continue;
          }
          IsisRoute.Builder addRouteBuilder=changedRoutes.get(i).toBuilder().setTopologyCondition(tc).setReason(Reason.ADD);
          answer.add(addRouteBuilder);
        }
      }
    }
    return answer;
  }
  public List<IsisRoute> getRoutes()
  {
    List<IsisRoute> answer=new ArrayList<>();
    for(Prefix prefix:_rib.keySet())
    {
      answer.addAll(_rib.get(prefix));
    }
    return answer;
  }
  public int getRouteNumber()
  {
    int number=0;
    for (Prefix prefix:_rib.keySet())
    {
      number+=_rib.get(prefix).size();
    }
    return number;
  }
  public void setEdgeBddFactory(BDDFactory factory)
  {
    this._edgeBddFactory=factory;
  }

  public HashMap<Prefix, List<IsisRoute>> getRib()
  {
    return this._rib;
  }
}
