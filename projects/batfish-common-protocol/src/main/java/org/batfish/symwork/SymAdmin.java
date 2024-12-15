package org.batfish.symwork;

public class SymAdmin {
  private int _lowerBound;
  private int _upperBound;
  public SymAdmin(int lowerBound,int upperBound)
  {
    this._lowerBound=lowerBound;
    this._upperBound=upperBound;
  }
  public void setLp(int lowerBound,int upperBound)
  {
    this._lowerBound=lowerBound;
    this._upperBound=upperBound;
  }
  public int getLowerBound()
  {
    return _lowerBound;
  }
  public int getUpperBound()
  {
    return _upperBound;
  }
}
