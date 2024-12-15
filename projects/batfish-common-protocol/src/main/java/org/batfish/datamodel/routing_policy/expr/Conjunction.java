package org.batfish.datamodel.routing_policy.expr;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.batfish.common.Warnings;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.symwork.AECComputeResult;
import org.batfish.symwork.AECEnvironment;
import org.batfish.symwork.CECOpEngine;

public final class Conjunction extends BooleanExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private static final String PROP_CONJUNCTS = "conjuncts";

  private List<BooleanExpr> _conjuncts;

  public Conjunction() {
    this(new ArrayList<>());
  }

  @JsonCreator
  public Conjunction(@JsonProperty("PROP_CONJUNCTS") List<BooleanExpr> conjuncts) {
    _conjuncts = firstNonNull(conjuncts, Collections.emptyList());
  }

  @Override
  public Set<String> collectSources(
      Set<String> parentSources, Map<String, RoutingPolicy> routingPolicies, Warnings w) {
    ImmutableSet.Builder<String> childSources = ImmutableSet.builder();
    for (BooleanExpr conjunct : _conjuncts) {
      childSources.addAll(conjunct.collectSources(parentSources, routingPolicies, w));
    }
    return childSources.build();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Conjunction)) {
      return false;
    }
    Conjunction other = (Conjunction) obj;
    return Objects.equals(_conjuncts, other._conjuncts);
  }

  @Override
  public Result evaluate(Environment environment) {

    Integer matchCEC = 1;
    HashSet<BitSet> conjunctBitSet = new HashSet<>();
    for (BooleanExpr conjunct : _conjuncts) {

      Result conjunctResult = conjunct.evaluate(environment);
      if (!conjunctResult.getMatchCEC().isEmpty())
      {
        for (Integer cec : conjunctResult.getMatchCEC())
        {
          matchCEC = CECOpEngine.CECAnd(cec, matchCEC);
        }
      }
      if (!conjunctResult.getMatchPECSet().isEmpty())
      {
        conjunctBitSet.add(conjunctResult.getMatchPECSet());
      }
      if (conjunctResult.getExit()) {
        return conjunctResult;
      } else if (!conjunctResult.getBooleanValue()) {
        conjunctResult.setReturn(false);
        if (!conjunctBitSet.isEmpty())
        {
          BitSet andBitSet = new BitSet();
          boolean first = true;
          for (BitSet conjunctBit : conjunctBitSet)
          {
            if (first)
            {
              andBitSet = (BitSet) conjunctBit.clone();
            } else {
              andBitSet.and(conjunctBit);
            }
            first = false;
          }
          andBitSet.and(environment.getSymBgpOutputRoute().getPrefixEcSet());
          if (!andBitSet.isEmpty())
          {
            conjunctResult.setMatchPECSet(andBitSet);
          }
        }
        return conjunctResult;
      }
    }

    Result result = new Result();
    result.setBooleanValue(true);
    if (!conjunctBitSet.isEmpty())
    {
      BitSet andBitSet = new BitSet();
      boolean first = true;
      for (BitSet conjunctBit : conjunctBitSet)
      {
        if (first)
        {
          andBitSet = (BitSet) conjunctBit.clone();
        } else {
          andBitSet.and(conjunctBit);
        }
        first = false;
      }
      result.setMatchPECSet(andBitSet);
    }
    if (matchCEC != 1)
    {
      result.addMatchCEC(matchCEC);
    }
    return result;
  }

  @JsonProperty(PROP_CONJUNCTS)
  public List<BooleanExpr> getConjuncts() {
    return _conjuncts;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_conjuncts == null) ? 0 : _conjuncts.hashCode());
    return result;
  }

  public void setConjuncts(List<BooleanExpr> conjuncts) {
    _conjuncts = conjuncts;
  }

  @Override
  public BooleanExpr simplify() {
    if (_simplified != null) {
      return _simplified;
    }
    ImmutableList.Builder<BooleanExpr> simpleConjunctsBuilder = ImmutableList.builder();
    boolean atLeastOneFalse = false;
    boolean atLeastOneComplex = false;
    for (BooleanExpr conjunct : _conjuncts) {
      BooleanExpr simpleConjunct = conjunct.simplify();
      if (simpleConjunct.equals(BooleanExprs.FALSE)) {
        atLeastOneFalse = true;
        if (!atLeastOneComplex) {
          _simplified = BooleanExprs.FALSE;
          return _simplified;
        } else if (!atLeastOneFalse) {
          simpleConjunctsBuilder.add(simpleConjunct);
        }
      } else if (!simpleConjunct.equals(BooleanExprs.TRUE)) {
        atLeastOneComplex = true;
        simpleConjunctsBuilder.add(simpleConjunct);
      }
    }
    List<BooleanExpr> simpleConjuncts = simpleConjunctsBuilder.build();
    if (simpleConjuncts.isEmpty()) {
      _simplified = BooleanExprs.TRUE;
    } else if (simpleConjuncts.size() == 1) {
      _simplified = simpleConjuncts.get(0);
    } else {
      Conjunction simple = new Conjunction();
      simple.setConjuncts(simpleConjuncts);
      simple.setComment(getComment());
      _simplified = simple;
      simple._simplified = _simplified;
    }
    return _simplified;
  }

  @Override
  public String toString() {
    return toStringHelper().add(PROP_CONJUNCTS, _conjuncts).toString();
  }

  @Override
  public AECComputeResult computeAEC(Integer computeCEC, AECEnvironment aecEnvironment)
  {
    AECComputeResult aecComputeResult = new AECComputeResult();
    aecComputeResult.setRemainCEC(computeCEC);
    Integer conjunctCEC = 1;
    for (BooleanExpr conjunct : _conjuncts) {
      AECComputeResult conjunctResult = conjunct.computeAEC(computeCEC, aecEnvironment);
      conjunctCEC = aecEnvironment.getCommunityBDD().and(conjunctCEC, conjunctResult.getComputeCEC());
    }
    Integer remainCEC = aecEnvironment.getCommunityBDD().and(computeCEC, aecEnvironment.getCommunityBDD().not(conjunctCEC));
    aecEnvironment.getCommunityBDD().ref(conjunctCEC);
    aecEnvironment.getCommunityBDD().ref(remainCEC);
    aecComputeResult.setComputeCEC(conjunctCEC);
    aecComputeResult.setRemainCEC(remainCEC);
    return aecComputeResult;
  }
}
