package org.batfish.datamodel.routing_policy.expr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import org.batfish.common.Warnings;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.symwork.AECComputeResult;
import org.batfish.symwork.AECEnvironment;

public final class Disjunction extends BooleanExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private List<BooleanExpr> _disjuncts;

  public Disjunction() {
    this(new ArrayList<>());
  }

  public Disjunction(List<BooleanExpr> disjuncts) {
    _disjuncts = disjuncts;
  }

  @Override
  public Set<String> collectSources(
      Set<String> parentSources, Map<String, RoutingPolicy> routingPolicies, Warnings w) {
    ImmutableSet.Builder<String> childSources = ImmutableSet.builder();
    for (BooleanExpr disjunct : _disjuncts) {
      childSources.addAll(disjunct.collectSources(parentSources, routingPolicies, w));
    }
    return childSources.build();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Disjunction)) {
      return false;
    }
    Disjunction other = (Disjunction) obj;
    return Objects.equals(_disjuncts, other._disjuncts);
  }

  @Override
  public Result evaluate(Environment environment) {
    boolean setReturn = true;
    Set<Integer> matchCEC = new HashSet<> ();
    BitSet matchBitSet = new BitSet();
    for (BooleanExpr disjunct : _disjuncts) {
      Result disjunctResult = disjunct.evaluate(environment);
      if (disjunctResult.getMatchCEC().size() == 0 && disjunctResult.getMatchPECSet().isEmpty())
      {
        if (setReturn)
        {
          if (disjunctResult.getExit()) {
            return disjunctResult;
          } else if (disjunctResult.getBooleanValue()) {
            disjunctResult.setReturn(false);
            return disjunctResult;
          }
        }
      } else {
        if (disjunctResult.getExit()) {
          return disjunctResult;
        } else if (disjunctResult.getBooleanValue()) {
          setReturn = false;
          matchCEC.addAll(disjunctResult.getMatchCEC());
          matchBitSet.or(disjunctResult.getMatchPECSet());
        }
      }
    }



    if (!setReturn)
    {
      Result result = new Result();
      result.setReturn(setReturn);
      result.addMatchCEC(matchCEC);
      result.setMatchPECSet(matchBitSet);
      return result;
    }
    Result result = new Result();
    result.setBooleanValue(false);
    return result;
  }

  public List<BooleanExpr> getDisjuncts() {
    return _disjuncts;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_disjuncts == null) ? 0 : _disjuncts.hashCode());
    return result;
  }

  public void setDisjuncts(List<BooleanExpr> disjuncts) {
    _disjuncts = disjuncts;
  }

  @Override
  public BooleanExpr simplify() {
    if (_simplified != null) {
      return _simplified;
    }
    ImmutableList.Builder<BooleanExpr> simpleDisjunctsBuilder = ImmutableList.builder();
    for (BooleanExpr disjunct : _disjuncts) {
      BooleanExpr simpleDisjunct = disjunct.simplify();
      if (simpleDisjunct.equals(BooleanExprs.FALSE)) {
        // Skip false.
        continue;
      }
      simpleDisjunctsBuilder.add(simpleDisjunct);
      if (simpleDisjunct.equals(BooleanExprs.TRUE)) {
        // Short-circuit the disjunction after the last true.
        break;
      }
    }
    List<BooleanExpr> simpleDisjuncts = simpleDisjunctsBuilder.build();
    if (simpleDisjuncts.isEmpty()) {
      _simplified = BooleanExprs.FALSE;
    } else if (simpleDisjuncts.size() == 1) {
      _simplified = simpleDisjuncts.get(0);
    } else {
      Disjunction simple = new Disjunction();
      simple.setDisjuncts(simpleDisjuncts);
      simple.setComment(getComment());
      _simplified = simple;
      simple._simplified = _simplified;
    }
    return _simplified;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "<" + _disjuncts + ">";
  }

  @Override
  public AECComputeResult computeAEC(Integer computeCEC, AECEnvironment aecEnvironment)
  {
    AECComputeResult aecComputeResult = new AECComputeResult();
    aecComputeResult.setRemainCEC(computeCEC);
    Integer disjunctCEC = 0;
    for (BooleanExpr disjunct : _disjuncts) {
      AECComputeResult disjunctResult = disjunct.computeAEC(computeCEC, aecEnvironment);
      disjunctCEC = aecEnvironment.getCommunityBDD().or(disjunctCEC, disjunctResult.getComputeCEC());
    }
    Integer remainCEC = aecEnvironment.getCommunityBDD().and(computeCEC, aecEnvironment.getCommunityBDD().not(disjunctCEC));
    aecEnvironment.getCommunityBDD().ref(disjunctCEC);
    aecEnvironment.getCommunityBDD().ref(remainCEC);
    aecComputeResult.setComputeCEC(disjunctCEC);
    aecComputeResult.setRemainCEC(remainCEC);
    return aecComputeResult;
  }
}
