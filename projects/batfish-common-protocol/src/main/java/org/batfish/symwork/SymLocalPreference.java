package org.batfish.symwork;

public class SymLocalPreference {
  private int _lowerBound;
  private int _upperBound;
  public SymLocalPreference(int lowerBound,int upperBound)
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
//  public boolean equals(Object o)
//  {
//    if(o==this)
//    {
//      return true;
//    }else if(!(o instanceof SymLocalPreference))
//    {
//      return false;
//    }
//    SymLocalPreference other=(SymLocalPreference) o;
//    if(_lowerBound!= other._lowerBound||_upperBound!= other._upperBound)
//    {
//      return false;
//    }
//    return true;
//  }
}
