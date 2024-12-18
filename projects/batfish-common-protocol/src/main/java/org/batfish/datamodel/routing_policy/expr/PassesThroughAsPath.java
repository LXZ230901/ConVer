package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.symwork.AECComputeResult;
import org.batfish.symwork.AECEnvironment;

public class PassesThroughAsPath extends BooleanExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private boolean _exact;

  private List<SubRangeExpr> _range;

  @JsonCreator
  private PassesThroughAsPath() {}

  public PassesThroughAsPath(List<SubRangeExpr> range, boolean exact) {
    _range = range;
    _exact = exact;
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
    PassesThroughAsPath other = (PassesThroughAsPath) obj;
    if (_exact != other._exact) {
      return false;
    }
    if (_range == null) {
      if (other._range != null) {
        return false;
      }
    } else if (!_range.equals(other._range)) {
      return false;
    }
    return true;
  }

  @Override
  public Result evaluate(Environment environment) {
    throw new UnsupportedOperationException("no implementation for generated method");
    // TODO Auto-generated method stub
  }

  public boolean getExact() {
    return _exact;
  }

  public List<SubRangeExpr> getRange() {
    return _range;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (_exact ? 1231 : 1237);
    result = prime * result + ((_range == null) ? 0 : _range.hashCode());
    return result;
  }
  @Override
  public AECComputeResult computeAEC(Integer computeCEC, AECEnvironment aecEnvironment)
  {
    AECComputeResult aecComputeResult = new AECComputeResult();
    aecComputeResult.setComputeCEC(computeCEC);
    aecComputeResult.setRemainCEC(computeCEC);
    return aecComputeResult;
  }
  public void setExact(boolean exact) {
    _exact = exact;
  }

  public void setRange(List<SubRangeExpr> range) {
    _range = range;
  }
}
