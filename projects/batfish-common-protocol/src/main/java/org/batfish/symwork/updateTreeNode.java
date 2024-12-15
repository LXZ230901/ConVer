package org.batfish.symwork;

import java.util.ArrayList;
import java.util.List;

public class updateTreeNode {
  public TreeNode _originNode;
  public List<TreeNode> _newNode;
  public updateTreeNode(TreeNode originNode){
    _originNode=originNode;
    _newNode=new ArrayList<>();
  }
}
