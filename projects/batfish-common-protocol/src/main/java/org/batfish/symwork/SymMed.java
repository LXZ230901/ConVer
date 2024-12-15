package org.batfish.symwork;

public class SymMed {
  private long _lowerBound;
  private long _upperBound;
  public SymMed(long lowerBound,long upperBound)
  {
    this._lowerBound=lowerBound;
    this._upperBound=upperBound;
  }
  public void setLp(long lowerBound,long upperBound)
  {
    this._lowerBound=lowerBound;
    this._upperBound=upperBound;
  }
  public long getLowerBound()
  {
    return _lowerBound;
  }
  public long getUpperBound()
  {
    return _upperBound;
  }
}
