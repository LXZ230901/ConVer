package org.batfish.symwork;

import java.util.BitSet;
import java.util.HashMap;

public class SymBgpFib {
  public HashMap<BitSet, HashMap<String, Integer>> _fib;

  public SymBgpFib()
  {
    _fib = new HashMap<>();
  }

  public HashMap<BitSet, HashMap<String, Integer>> getFib()
  {
    return _fib;
  }
}
