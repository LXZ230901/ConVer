package org.batfish.symwork;

public class SymAspathSize {
  private int _lowerBound;
  private int _upperBound;
  public SymAspathSize(int lowerBound,int upperBound)
  {
    if(lowerBound>upperBound)
    {
      System.out.println("as-path-size-error");
    }
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
  public void add(int size)
  {
    this._lowerBound=this._lowerBound+size;
    this._upperBound=this._upperBound+size;
  }
//  public boolean equals(Object o)
//  {
//    if(o==this)
//    {
//      return true;
//    }else if(!(o instanceof SymAspathSize))
//    {
//      return false;
//    }
//    SymAspathSize other=(SymAspathSize) o;
//    if(_lowerBound!=other._lowerBound||_upperBound!= other._upperBound)
//    {
//      return false;
//    }
//    return true;
//  }

}
