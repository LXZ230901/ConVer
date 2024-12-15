package org.batfish.datamodel.routing_policy;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public class Result {

  private boolean _booleanValue;

  private boolean _exit;

  private boolean _fallThrough;

  private boolean _return;

  private Set<Integer> _matchCEC;

  private BitSet _matchPECSet;

  public Result()
  {
    _matchCEC = new HashSet<>();
    _matchPECSet = new BitSet();
  }

  public Set<Integer> getMatchCEC()
  {
    return _matchCEC;
  }


  public BitSet getMatchPECSet()
  {
    return _matchPECSet;
  }

  public void setMatchPECSet(BitSet pecSet)
  {
    _matchPECSet = (BitSet) pecSet.clone();
  }

  public void addMatchCEC(Integer matchCEC)
  {
    _matchCEC.add(matchCEC);
  }

  public void addMatchCEC(Set<Integer> matchCEC)
  {
    _matchCEC.addAll(matchCEC);
  }
  
  public boolean getBooleanValue() {
    return _booleanValue;
  }

  public boolean getExit() {
    return _exit;
  }

  public boolean getFallThrough() {
    return _fallThrough;
  }

  public boolean getReturn() {
    return _return;
  }

  public void setBooleanValue(boolean booleanValue) {
    _booleanValue = booleanValue;
  }

  public void setExit(boolean exit) {
    _exit = exit;
  }

  public void setFallThrough(boolean fallThrough) {
    _fallThrough = fallThrough;
  }

  public void setReturn(boolean ret) {
    _return = ret;
  }
}
