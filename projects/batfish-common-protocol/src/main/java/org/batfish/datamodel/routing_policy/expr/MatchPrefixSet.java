package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.symwork.AECComputeResult;
import org.batfish.symwork.AECEnvironment;

public class MatchPrefixSet extends BooleanExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private static final String PROP_PREFIX = "prefix";

  private static final String PROP_PREFIX_SET = "prefixSet";

  private PrefixExpr _prefix;

  private PrefixSetExpr _prefixSet;

  @JsonCreator
  private MatchPrefixSet() {}

  public MatchPrefixSet(PrefixExpr prefix, PrefixSetExpr prefixSet) {
    _prefix = prefix;
    _prefixSet = prefixSet;
  }
  @Override
  public AECComputeResult computeAEC(Integer computeCEC, AECEnvironment aecEnvironment)
  {
    AECComputeResult aecComputeResult = new AECComputeResult();
    aecComputeResult.setComputeCEC(computeCEC);
    aecComputeResult.setRemainCEC(computeCEC);
    return aecComputeResult;
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
    MatchPrefixSet other = (MatchPrefixSet) obj;
    if (_prefix == null) {
      if (other._prefix != null) {
        return false;
      }
    } else if (!_prefix.equals(other._prefix)) {
      return false;
    }
    if (_prefixSet == null) {
      if (other._prefixSet != null) {
        return false;
      }
    } else if (!_prefixSet.equals(other._prefixSet)) {
      return false;
    }
    return true;
  }

  @Override
  public Result evaluate(Environment environment) {//这里是真正匹配的
    boolean match=false;
    if(environment._symbolic)
    {
//      match = _prefixSet.matches(environment);
      return _prefixSet.symMatches(environment);
    } else {
      Prefix prefix = _prefix.evaluate(environment);
      match = _prefixSet.matches(prefix, environment);
    }
    Result result = new Result();
    result.setBooleanValue(match);
    return result;
  }

  public Result symEvaluate(Environment environment) {//这里是真正匹配的
    boolean match = _prefixSet.matches(environment);
    Result result = new Result();
    result.setBooleanValue(match);
    return result;
  }

  @JsonProperty(PROP_PREFIX)
  public PrefixExpr getPrefix() {
    return _prefix;
  }

  @JsonProperty(PROP_PREFIX_SET)
  public PrefixSetExpr getPrefixSet() {
    return _prefixSet;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_prefix == null) ? 0 : _prefix.hashCode());
    result = prime * result + ((_prefixSet == null) ? 0 : _prefixSet.hashCode());
    return result;
  }

  @JsonProperty(PROP_PREFIX)
  public void setPrefix(PrefixExpr prefix) {
    _prefix = prefix;
  }

  @JsonProperty(PROP_PREFIX_SET)
  public void setPrefixSet(PrefixSetExpr prefixSet) {
    _prefixSet = prefixSet;
  }

  @Override
  public String toString() {
    return toStringHelper().add(PROP_PREFIX, _prefix).add(PROP_PREFIX_SET, _prefixSet).toString();
  }
}
