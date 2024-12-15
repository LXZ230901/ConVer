package org.batfish.datamodel.routing_policy.statement;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Set;
import java.util.SortedSet;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.expr.CommunitySetExpr;
import org.batfish.symwork.AttributeEquivalence;
import org.batfish.symwork.CECOpEngine;
import org.batfish.symwork.SymBgpRoute;
import org.batfish.symwork.SymCommunity;

public class AddCommunity extends Statement {
  /** */
  private static final long serialVersionUID = 1L;
  private CommunitySetExpr _expr;

  @JsonCreator
  private AddCommunity() {}

  public AddCommunity(CommunitySetExpr expr) {
    _expr = expr;
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
    AddCommunity other = (AddCommunity) obj;
    if (_expr == null) {
      if (other._expr != null) {
        return false;
      }
    } else if (!_expr.equals(other._expr)) {
      return false;
    }
    return true;
  }

  @Override
  public Result execute(Environment environment) {
    Set<Integer> communities = _expr.getAllMatchSymCommunity(environment);
    Integer originalCEC = environment.getSymBgpOutputRoute().getCommunity();
    for (Integer community : communities)
    {
      originalCEC = CECOpEngine.CECExist(originalCEC, community);
      originalCEC = CECOpEngine.CECAnd(community, originalCEC);
    }
    environment.getSymBgpOutputRoute().setCommunity(originalCEC);
    Result result = new Result();
    return result;
  }

  public CommunitySetExpr getExpr() {
    return _expr;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_expr == null) ? 0 : _expr.hashCode());
    return result;
  }

  public void setExpr(CommunitySetExpr expr) {
    _expr = expr;
  }











//  @Override
//  public AttributeEquivalence computeAEC(Set<Integer> CEC)
//  {
//    AttributeEquivalence AEC = new AttributeEquivalence();
//    return AEC;
//  }
}
