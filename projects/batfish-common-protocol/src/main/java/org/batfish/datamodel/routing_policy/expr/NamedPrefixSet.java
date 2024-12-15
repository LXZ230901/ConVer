package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Set;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.symwork.RelatedNodeAnswer;
import org.batfish.symwork.SymPrefixList;

public class NamedPrefixSet extends PrefixSetExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private String _name;

  @JsonCreator
  private NamedPrefixSet() {}

  public NamedPrefixSet(String name) {
    _name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NamedPrefixSet other = (NamedPrefixSet) obj;
    if (_name == null) {
      if (other._name != null) {
        return false;
      }
    } else if (!_name.equals(other._name)) {
      return false;
    }
    return true;
  }

  public String getName() {
    return _name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_name == null) ? 0 : _name.hashCode());
    return result;
  }

  @Override
  public boolean matches(Prefix prefix, Environment environment) {
    //在这里获取routeFliterlist从而进行前缀的判断
    RouteFilterList list = environment.getConfiguration().getRouteFilterLists().get(_name);
    if (list != null) {
      return list.permits(prefix);
    } else {
      environment.setError(true);
      return false;
    }
  }

  @Override
  public boolean matches(Environment environment)
  {
//    RouteFilterList list = environment.getConfiguration().getRouteFilterLists().get(_name);
//    if (list!=null)
//    {
//      //RelatedNodeAnswer answer=list.symPermits(prefix);
//      boolean answer=list.symPermits(environment.getSymBgpOutputRoute().getPrefixEcNum());
//      return answer;
//    }else{
//      environment.setError(true);
//      return false;
//    }
    System.out.println("Named Prefix Set Match Error");
    return false;
  }

  public void setName(String name) {
    _name = name;
  }


  @Override
  public Result symMatches(Environment environment)
  {
    Result result = new Result();
    RouteFilterList list = environment.getConfiguration().getRouteFilterLists().get(_name);
    if (list!=null)
    {
      //RelatedNodeAnswer answer=list.symPermits(prefix);
      Result answer=list.symPermitSets(environment.getSymBgpOutputRoute().getPrefixEcSet());
      return answer;
    }else{
      environment.setError(true);
      return result;
    }

//    Result result = new Result();
//    RouteFilterList list = environment.getConfiguration().getRouteFilterLists().get(_name);
//    if (list!=null)
//    {
//      //RelatedNodeAnswer answer=list.symPermits(prefix);
//      boolean answer=list.symPermits(environment.getSymBgpOutputRoute().getPrefixEcNum());
//      return answer;
//    }else{
//      environment.setError(true);
//      return false;
//    }
//    return result;
  }


}
