package org.batfish.symwork;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ReasonComparator implements Comparator<SymBgpRoute> {
  public static Map<Reason,Integer> _reasonNum;
  static {
    _reasonNum=new HashMap<>();
    _reasonNum.put(Reason.WITHDRAW,1);
    _reasonNum.put(Reason.UPDATE,2);
    _reasonNum.put(Reason.ADD,3);
  }
  @Override
  public int compare(SymBgpRoute p1, SymBgpRoute p2) {
    return _reasonNum.get(p1.getReason())-_reasonNum.get(p2.getReason());
  }
}
