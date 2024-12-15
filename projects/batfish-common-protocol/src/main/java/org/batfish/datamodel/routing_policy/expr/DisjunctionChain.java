package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableSet;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.BatfishException;
import java.util.HashSet;
import org.batfish.common.Warnings;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.symwork.AECComputeResult;
import org.batfish.symwork.AECEnvironment;

/** Juniper subroutine chain */
public class DisjunctionChain extends BooleanExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private List<BooleanExpr> _subroutines;

  @JsonCreator
  private DisjunctionChain() {}

  public DisjunctionChain(List<BooleanExpr> subroutines) {
    _subroutines = subroutines;
  }

  @Override
  public Set<String> collectSources(
      Set<String> parentSources, Map<String, RoutingPolicy> routingPolicies, Warnings w) {
    ImmutableSet.Builder<String> childSources = ImmutableSet.builder();
    for (BooleanExpr disjunct : _subroutines) {
      childSources.addAll(disjunct.collectSources(parentSources, routingPolicies, w));
    }
    return childSources.build();
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
    DisjunctionChain other = (DisjunctionChain) obj;
    if (_subroutines == null) {
      if (other._subroutines != null) {
        return false;
      }
    } else if (!_subroutines.equals(other._subroutines)) {
      return false;
    }
    return true;
  }

  @Override
  public Result evaluate(Environment environment) {
    boolean setReturn = false;
    Set<Integer> matchCEC = new HashSet<> ();
    Result subroutineResult = new Result();
    subroutineResult.setFallThrough(true);
    BitSet matchBitSet = new BitSet();
    for (BooleanExpr subroutine : _subroutines) {
      environment.setDisjunctOrConjunctChain(true);
      subroutineResult = subroutine.evaluate(environment);
      environment.setDisjunctOrConjunctChain(false);
      if (subroutineResult.getExit()) {
        matchCEC.addAll(subroutineResult.getMatchCEC());
        matchBitSet.or(subroutineResult.getMatchPECSet());
        subroutineResult.addMatchCEC(matchCEC);
        subroutineResult.setMatchPECSet(matchBitSet);
        return subroutineResult;
      } else if (!subroutineResult.getFallThrough() && subroutineResult.getBooleanValue()) {
        setReturn = true;
        matchCEC.addAll(subroutineResult.getMatchCEC());
        matchBitSet.or(subroutineResult.getMatchPECSet());
        subroutineResult.addMatchCEC(matchCEC);
        subroutineResult.setMatchPECSet(matchBitSet);
        subroutineResult.setReturn(true);
        return subroutineResult;
      }
//      if (subroutineResult.getMatchCEC().size() == 0 && subroutineResult.getMatchPECSet().isEmpty())
//      {
//        if (!setReturn)
//        {
//          if (subroutineResult.getExit()) {
//            return subroutineResult;
//          } else if (!subroutineResult.getFallThrough() && subroutineResult.getBooleanValue()) {
//            subroutineResult.setReturn(true);
//            return subroutineResult;
//          }
//        }
//      } else {
//        if (subroutineResult.getExit()) {
//          return subroutineResult;
//        } else if (!subroutineResult.getFallThrough() && subroutineResult.getBooleanValue()) {
//          setReturn = true;
//          matchCEC.addAll(subroutineResult.getMatchCEC());
//          matchBitSet.or(subroutineResult.getMatchPECSet());
//          // subroutineResult.setReturn(true);
//          // return subroutineResult;
//        }
//      }
    }
    if (!subroutineResult.getFallThrough()) {
      return subroutineResult;
    } else {
      String defaultPolicy = environment.getDefaultPolicy();
      if (defaultPolicy != null) {
        CallExpr callDefaultPolicy = new CallExpr(environment.getDefaultPolicy());
        Result defaultPolicyResult = callDefaultPolicy.evaluate(environment);
        return defaultPolicyResult;
      } else {
        throw new BatfishException("Default policy not set");
      }
    }
  }

  public List<BooleanExpr> getSubroutines() {
    return _subroutines;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_subroutines == null) ? 0 : _subroutines.hashCode());
    return result;
  }

  public void setSubroutines(List<BooleanExpr> subroutines) {
    _subroutines = subroutines;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "<" + _subroutines + ">";
  }

  @Override
  public AECComputeResult computeAEC(Integer computeCEC, AECEnvironment aecEnvironment)
  {
    AECComputeResult aecComputeResult = new AECComputeResult();
    aecComputeResult.setComputeCEC(computeCEC);
    aecComputeResult.setRemainCEC(computeCEC);
    return aecComputeResult;
  }
}
