package org.batfish.symwork;

public class SymBounder {
  private int lowerbound;
  private int upperbound;
  public SymBounder(int low,int up)
  {
    this.lowerbound=low;
    this.upperbound=up;
  }

  public int getLowerbound() {
    return lowerbound;
  }

  public int getUpperbound() {
    return upperbound;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + lowerbound;
    result = prime * result + upperbound;
    return result;
  }
}
