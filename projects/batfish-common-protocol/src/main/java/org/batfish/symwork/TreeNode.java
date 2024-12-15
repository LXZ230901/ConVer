package org.batfish.symwork;

import java.util.ArrayList;
import java.util.List;
import jdk.internal.org.objectweb.asm.Handle;
import org.batfish.datamodel.AsPath;
import sun.text.resources.in.FormatData_in;

public class TreeNode {

  public boolean external;
  public Integer prefixEc;
  public Integer index;
  public String value;
  public Integer pathHash;

  public Integer pathSizeHash;

  public boolean withdrawChanged;

  public AsPath asPath;

  public SymBounder pathSize;

  public TreeNode(String value,Integer index,Integer pathHash,Integer pathSizeHash,SymBounder pathSize,AsPath asPath) {
    this.value = value;
    this.pathHash=pathHash;
    this.pathSizeHash=pathSizeHash;
    this.withdrawChanged=false;
    this.pathSize=pathSize;
    this.index=index;
    this.external=false;
    this.asPath=asPath;
  }

  public void setWithdrawChanged(boolean withdrawChanged)
  {
    this.withdrawChanged=withdrawChanged;
  }

  public boolean getWithdrawChanged()
  {
    return this.withdrawChanged;
  }

  @Override
  public boolean equals(Object o)
  {
    if(!(o instanceof TreeNode))
    {
      return false;
    }
    TreeNode node=(TreeNode) o;
    if(!node.index.equals(index))
    {
      return false;
    }
    if(!node.value.equals(value))
    {
      return false;
    }
    if(!node.pathSizeHash.equals(pathSizeHash))
    {
      return false;
    }
    return node.pathHash.equals(pathHash);
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + index.hashCode();
    result = prime * result + value.hashCode();
    result = prime * result + pathHash.hashCode();
    result = prime * result + pathSizeHash.hashCode();
    return result;
  }

  public Integer getIndex()
  {
    return index;
  }

}

