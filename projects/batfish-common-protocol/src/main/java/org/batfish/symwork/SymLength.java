package org.batfish.symwork;

import com.google.common.primitives.UnsignedInteger;

public class SymLength {
  public UnsignedInteger _len;
  public boolean _any;
  public SymLength(UnsignedInteger length,boolean any)
  {
    _len=length;
    _any=any;
  }
}
