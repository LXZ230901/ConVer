package org.batfish.symwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import sun.util.resources.ro.CalendarData_ro;

public class RouteForest {
  public Map<TreeNode, List<TreeNode>> _sonNodes;
  public Map<TreeNode, List<TreeNode>> _faterNodes;

  public Map<TreeNode,Set<TreeNode>> _tempNodes;

  public Map<TreeNode,TreeNode> _targetTempNodes;

  public Map<TreeNode, Boolean> _virtualNodes;
  public RouteForest() {
    this._sonNodes=new HashMap<>();
    this._faterNodes=new HashMap<>();
    this._virtualNodes=new HashMap<>();
    this._tempNodes=new HashMap<>();
    this._targetTempNodes=new HashMap<>();
  }


  public void addNode(TreeNode sonNode,TreeNode fatherNode)
  {
    _sonNodes.put(sonNode,new ArrayList<>());
    _faterNodes.put(sonNode,new ArrayList<>());
    _faterNodes.get(sonNode).add(fatherNode);
    _sonNodes.get(fatherNode).add(sonNode);
    _virtualNodes.put(sonNode,false);
  }

  public void addNodes(Set<TreeNode> sonNodes,TreeNode fatherNode)
  {
    for(TreeNode sonNode:sonNodes)
    {
//      if(sonNode.equals(fatherNode))
//      {
//        System.out.println("son-father-node-equal-break");
//      }
//      if(_sonNodes.containsKey(sonNode))
//      {
//        System.out.println("contains-break");
//      }
//      if(sonNode.value.equals(fatherNode.value))
//      {
//        System.out.println("error-relationship-break");
//      }
      _sonNodes.put(sonNode,new ArrayList<>());
      _faterNodes.put(sonNode,new ArrayList<>());
      _faterNodes.get(sonNode).add(fatherNode);
      if(_sonNodes.get(fatherNode).contains(sonNode))
      {
        System.out.println("break");
      }
      _sonNodes.get(fatherNode).add(sonNode);
//      if(_faterNodes.get(sonNode).size()==0)
//      {
//        System.out.println("break");
//      }
    }
//    for(TreeNode node:_faterNodes.keySet())
//    {
//      if(_faterNodes.get(node).size()==0&&!(node.pathSize.getLowerbound()==0&&node.pathSize.getUpperbound()==0)&&!(node.pathSize.getLowerbound()==0&&node.pathSize.getUpperbound()==Integer.MAX_VALUE))
//      {
//        System.out.println("size-0-break");
//      }
//    }
//    System.out.println("break");
  }

//  public void updateNodes(TreeNode fatherNode,Set<TreeNode> updateNodes)
//  {
//    for(TreeNode faterNode:updateNodes.keySet())
//    {
//      for(TreeNode sonNode:updateNodes.get(faterNode)){
//        _sonNodes.put(sonNode,new ArrayList<>());
//        _faterNodes.put(sonNode,new ArrayList<>());
//        _sonNodes.get(sonNode).add(faterNode);
//        _faterNodes.get(sonNode).addAll(_faterNodes.get(faterNode));
//      }
//      for(TreeNode node:_faterNodes.get(faterNode))
//      {
//        _sonNodes.get(node).addAll(updateNodes.get(faterNode));
//      }
//      _faterNodes.get(faterNode).clear();
//      _faterNodes.get(faterNode).addAll(updateNodes.get(faterNode));
//    }
//  }

  public void updateNodes(TreeNode fatherNode,Set<TreeNode> updateNodes)
  {
//    for(TreeNode node:_faterNodes.keySet())
//    {
//      if(_faterNodes.get(node).size()==0&&!(node.pathSize.getLowerbound()==0&&node.pathSize.getUpperbound()==Integer.MAX_VALUE))
//      {
//        System.out.println("size-0-break");
//      }
//    }
//    if(!_sonNodes.containsKey(fatherNode))
//    {
//      System.out.println("update-dont-have-upnode-break");
//      return;
//    }
    TreeNode targetFatherNode=fatherNode;
    if(_targetTempNodes.containsKey(fatherNode)&&_sonNodes.containsKey(fatherNode))
    {
      targetFatherNode=_targetTempNodes.get(fatherNode);
//      if(!_tempNodes.containsKey(targetFatherNode))
//      {
//        System.out.println("break");
//      }
      _tempNodes.get(targetFatherNode).remove(fatherNode);
      _tempNodes.get(targetFatherNode).addAll(updateNodes);
    }else if(!_targetTempNodes.containsKey(fatherNode)&&!_tempNodes.containsKey(targetFatherNode))
    {
      _tempNodes.put(targetFatherNode,new HashSet<>());
      _tempNodes.get(targetFatherNode).addAll(updateNodes);
      for(TreeNode node:updateNodes)
      {
        if(!targetFatherNode.value.equals(node.value))
        {
          System.out.println("update-break");
        }
      }
    }
    for(TreeNode sonNode:updateNodes){
//      if(sonNode.equals(fatherNode))
//      {
//        System.out.println("update-break");
//      }
      _sonNodes.put(sonNode,new ArrayList<>());
      _faterNodes.put(sonNode,new ArrayList<>());
      if(!_sonNodes.containsKey(fatherNode))
      {
        System.out.println("break");
        return ;
      }
      _sonNodes.get(sonNode).addAll(_sonNodes.get(fatherNode));
      _faterNodes.get(sonNode).addAll(_faterNodes.get(fatherNode));
      _targetTempNodes.put(sonNode,targetFatherNode);
    }
//    for(TreeNode node:_faterNodes.keySet())
//    {
//      if(_faterNodes.get(node).size()==0&&!(node.pathSize.getLowerbound()==0&&node.pathSize.getUpperbound()==Integer.MAX_VALUE))
//      {
//        System.out.println("size-0-break");
//      }
//    }
    for(TreeNode node:_faterNodes.get(fatherNode))
    {
//      if(!_sonNodes.containsKey(node))
//      {
//        System.out.println("break");
//      }
//      if(updateNodes.contains(node))
//      {
//        System.out.println("update-contains-break");
//      }
      _sonNodes.get(node).addAll(updateNodes);
      _sonNodes.get(node).remove(fatherNode);
    }
    for(TreeNode node:_sonNodes.get(fatherNode))
    {
//      if(updateNodes.contains(node))
//      {
//        System.out.println("break-contains-break");
//      }
      _faterNodes.get(node).addAll(updateNodes);
      _faterNodes.get(node).remove(fatherNode);
//      for(TreeNode nodet:_faterNodes.keySet())
//      {
//        if(_faterNodes.get(nodet).size()==0&&nodet.pathSize.getUpperbound()!=Integer.MAX_VALUE)
//        {
//          System.out.println("size-0-break");
//        }
//      }
    }
//    for(TreeNode node:_faterNodes.keySet())
//    {
//      if(_faterNodes.get(node).contains(fatherNode))
//      {
//        System.out.println("break");
//      }
//    }
//    for(TreeNode node:_sonNodes.keySet())
//    {
//      if(_sonNodes.get(node).contains(fatherNode))
//      {
//        System.out.println("break");
//      }
//    }
//    for(TreeNode node:_faterNodes.keySet())
//    {
//      boolean no=false;
//      List<TreeNode> nodeList=_faterNodes.get(node);
//      for(TreeNode node1:_faterNodes.get(node))
//      {
//        if(!(node1.pathSize.getLowerbound()>node.pathSize.getUpperbound()||node1.pathSize.getUpperbound()<node.pathSize.getLowerbound()))
//        {
//          no=true;
//          break;
//        }
//      }
//      if(!no&&node.pathSize.getUpperbound()!=Integer.MAX_VALUE)
//      {
//        System.out.println("break");
//      }
//    }
    _faterNodes.remove(fatherNode);
    _sonNodes.remove(fatherNode);
    _virtualNodes.remove(fatherNode);
  }

//  public void updateNodes(TreeNode fatherNode,Set<TreeNode> sonNodes)
//  {
//    for(TreeNode sonNode:sonNodes){
//      _sonNodes.put(sonNode,new ArrayList<>());
//      _faterNodes.put(sonNode,new ArrayList<>());
//      _virtualNodes.put(sonNode,false);
//      _sonNodes.get(sonNode).add(fatherNode);
//      _faterNodes.get(sonNode).addAll(_faterNodes.get(fatherNode));
//    }
//    for(TreeNode node:_faterNodes.get(fatherNode))
//    {
//      _sonNodes.get(node).addAll(sonNodes);
//      _sonNodes.get(node).remove(fatherNode);
//    }
//    _faterNodes.remove(fatherNode);
//    _faterNodes.put(fatherNode,new ArrayList<>());
//    _faterNodes.get(fatherNode).addAll(sonNodes);
//    _virtualNodes.replace(fatherNode,true);
//  }

  public void addNode(TreeNode node)
  {
    _sonNodes.put(node,new ArrayList<>());
    _faterNodes.put(node,new ArrayList<>());
    _virtualNodes.put(node,false);
  }

  public List<TreeNode> getAllChilds(TreeNode rootNode)
  {
    List<TreeNode> nodes=new ArrayList<>();
//    if(!_sonNodes.containsKey(rootNode))
//    {
//      System.out.println("getchildBreak");
//      //return nodes;
//    }
    for(TreeNode node:_sonNodes.get(rootNode))
    {
      nodes.addAll(getChilds(node));
    }
    return nodes;
  }

  public List<TreeNode> getAllChilds(TreeNode rootNode,boolean external)
  {
    List<TreeNode> nodes=new ArrayList<>();
    Queue<TreeNode> subNodeQueue=new LinkedList<>();
    subNodeQueue.add(rootNode);
    while (!subNodeQueue.isEmpty())
    {
      TreeNode node=subNodeQueue.poll();
      for(TreeNode sonNode:_sonNodes.get(node))
      {
        if(!(sonNode.pathSize.getUpperbound()<rootNode.pathSize.getLowerbound()||sonNode.pathSize.getLowerbound()>rootNode.pathSize.getUpperbound()))
        {
          subNodeQueue.add(sonNode);
          nodes.add(sonNode);
        }
      }
    }
//    if(!_sonNodes.containsKey(rootNode))
//    {
//      System.out.println("getchildBreak");
//      //return nodes;
//    }
//    for(TreeNode node:_sonNodes.get(rootNode))
//    {
//      nodes.addAll(getChilds(node,rootNode.pathSize));
//    }
    return nodes;
  }

  public List<TreeNode> getChilds(TreeNode node)
  {
    List<TreeNode> nodes=new ArrayList<>();
    nodes.add(node);
    if(!_sonNodes.containsKey(node))
    {
      System.out.println("break");
      return nodes;
    }
    for(TreeNode child:_sonNodes.get(node))
    {
      nodes.addAll(getChilds(child));
    }
    return nodes;
  }

  public List<TreeNode> getChilds(TreeNode node,SymBounder rootPathSize)
  {
    List<TreeNode> nodes=new ArrayList<>();
    if(node.pathSize.getUpperbound()<=rootPathSize.getUpperbound()&&node.pathSize.getLowerbound()>=rootPathSize.getLowerbound())
    {
      nodes.add(node);
      for(TreeNode child:_sonNodes.get(node))
      {
        nodes.addAll(getChilds(child,rootPathSize));
      }
    }
    return nodes;
  }

  public ContainsAnswer contains(TreeNode node, SymBounder aspathSize)
  {
    ContainsAnswer answer=new ContainsAnswer();
    if(!_sonNodes.containsKey(node)) {
      if(_tempNodes.containsKey(node))
      {
        for (TreeNode sonNode : _tempNodes.get(node)) {
          if (_sonNodes.containsKey(sonNode)) {
            answer._newAsPathSizeBounder.add(new SymBounder(sonNode.pathSize.getLowerbound(), sonNode.pathSize.getUpperbound()));
          }
        }
      }
      if (answer._newAsPathSizeBounder.size() == 0) {
        answer._contains = false;
        return answer;
      } else {
        answer._contains = true;
        return answer;
      }
    }
    answer._contains=true;
    return answer;
  }

  public static class ContainsAnswer{
    public Boolean _contains;
    public List<SymBounder> _newAsPathSizeBounder;
    public ContainsAnswer()
    {
      _contains=false;
      _newAsPathSizeBounder=new ArrayList<>();
    }
  }
//  public void deleteNode(TreeNode rootNode)
//  {
//    for(TreeNode node:_sonNodes.get(rootNode))
//    {
//      deleteSonNode(node);
//    }
//    _sonNodes.remove(rootNode);
//    for(TreeNode node:_faterNodes.get(rootNode))
//    {
//      if(_sonNodes.get(node)!=null)
//      {
//        _sonNodes.get(node).remove(rootNode);
//      }
//    }
//    _faterNodes.remove(rootNode);
//  }
//
//  public void deleteSonNode(TreeNode rootNode)
//  {
//    for(TreeNode node:_sonNodes.get(rootNode))
//    {
//      deleteSonNode(node);
//    }
//    _sonNodes.remove(rootNode);
//    _faterNodes.remove(rootNode);
//  }

  public void deleteNode(TreeNode rootNode) {
//    if(rootNode.value.equals("60")&&rootNode.pathHash==1219960704)
//    {
//      System.out.println("break");
//    }
//    if(!_sonNodes.containsKey(rootNode))
//    {
//      System.out.println("break");
//      //return;
//    }
//    deleteSubtree(rootNode);
//
//    List<TreeNode> copyFatherNode=new ArrayList<>();
//    copyFatherNode.addAll(_faterNodes.get(rootNode));
//    if(rootNode.asPath.getAsPathString().equals("1 2 10070 60 100")&&rootNode.pathHash==39291484&&rootNode.pathSizeHash==961)
//    {
//      System.out.println("break");
//    }
//    // Update parent-child relationships
//    for (TreeNode parent : _faterNodes.get(rootNode)) {
//      _sonNodes.get(parent).remove(rootNode);
//    }
//
////    for(TreeNode node:_faterNodes.keySet())
////    {
////      if(_faterNodes.get(node).contains(rootNode))
////      {
////        System.out.println("break");
////      }
////    }
//    _sonNodes.remove(rootNode);
//    _faterNodes.remove(rootNode);
//    _virtualNodes.remove(rootNode);
//    for(TreeNode node:_sonNodes.keySet())
//    {
//      for (TreeNode sonNode : _sonNodes.get(node)) {
//        if (!_sonNodes.containsKey(sonNode)) {
//          System.out.println("break");
//        }
//      }
//    }
//    for (TreeNode parent : copyFatherNode) {
//      //      if(_sonNodes.get(parent)==null)
//      //      {
//      //        System.out.println("break");
//      //        return;
//      //      }
//      List<TreeNode> beforeNodes = new ArrayList<>();
//      beforeNodes.addAll(_sonNodes.get(parent));
//      _sonNodes.get(parent).remove(rootNode);
//      List<TreeNode> afterNodes = new ArrayList<>();
//      afterNodes.addAll(_sonNodes.get(parent));
//    }

    Queue<TreeNode> deleteQueue=new LinkedList<>();
    for(TreeNode child:_sonNodes.get(rootNode))
    {
      deleteQueue.add(child);
    }
    deleteSubtree(deleteQueue);


    for (TreeNode sonNode : _sonNodes.get(rootNode)) {
      _faterNodes.get(sonNode).remove(rootNode);
    }
    for (TreeNode parent : _faterNodes.get(rootNode)) {
      _sonNodes.get(parent).remove(rootNode);
    }


    _sonNodes.remove(rootNode);
    _faterNodes.remove(rootNode);
    _virtualNodes.remove(rootNode);
  }

  public void deleteNode(TreeNode rootNode,boolean external) {
//    if(!_sonNodes.containsKey(rootNode))
//    {
//      System.out.println("delete-subtree-break");
//      return;
//    }
    Queue<TreeNode> deleteQueue=new LinkedList<>();
    Queue<TreeNode> deleteQueueTemp=new LinkedList<>();
    Map<TreeNode, List<TreeNode>> _sonNodesTemp=new HashMap<>();
    _sonNodesTemp.put(rootNode,new ArrayList<>(_sonNodes.get(rootNode)));
    List<TreeNode> nodeTemp=new ArrayList<>();
    nodeTemp.addAll(_sonNodes.get(rootNode));
//    if(rootNode.pathSize.getUpperbound()==Integer.MAX_VALUE)
//    {
//      System.out.println("break");
//    }
//    for(TreeNode node:_faterNodes.keySet())
//    {
//      if(_faterNodes.get(node).size()==0&&!(node.pathSize.getLowerbound()==0&&node.pathSize.getUpperbound()==Integer.MAX_VALUE))
//      {
//        System.out.println("size-0-break");
//      }
//    }
    for(TreeNode child:_sonNodes.get(rootNode))
    {
//      nodeTemp.add(child);
      if(child.pathSize.getUpperbound()<=rootNode.pathSize.getUpperbound()&&child.pathSize.getLowerbound()>=rootNode.pathSize.getLowerbound())
      {
//        if(!_sonNodes.containsKey(child))
//        {
//          System.out.println("break-no-node-exist");
//        }
        deleteQueue.add(child);
        deleteQueueTemp.add(child);
      }
    }
//    deleteQueueTemp.addAll(deleteQueue);
//    if(!deleteQueue.isEmpty())
//    {
//      System.out.println("break");
//    }
//    System.out.println("delete-begin");
    deleteSubtree(deleteQueue,rootNode.pathSize);

//    for(TreeNode node:_faterNodes.keySet())
//    {
//      if(_faterNodes.get(node).size()==0&&!(node.pathSize.getLowerbound()==0&&node.pathSize.getUpperbound()==Integer.MAX_VALUE))
//      {
//        System.out.println("size-0-break");
//      }
//    }

    if(_targetTempNodes.containsKey(rootNode))
    {
      _tempNodes.get(_targetTempNodes.get(rootNode)).remove(rootNode);
    }
    _tempNodes.remove(rootNode);
    // Update parent-child relationships
//    if(!_sonNodes.containsKey(rootNode))
//    {
//      System.out.println("break");
//      return;
//    }
    for (TreeNode sonNode : _sonNodes.get(rootNode)) {
      _faterNodes.get(sonNode).remove(rootNode);
    }
    for (TreeNode parent : _faterNodes.get(rootNode)) {
//      if(!_sonNodes.containsKey(parent))
//      {
//        System.out.println("delete-node-break");
//      }
      _sonNodes.get(parent).remove(rootNode);
//      for(TreeNode node:_faterNodes.keySet())
//      {
//        if(_faterNodes.get(node).size()==0&&node.pathSize.getUpperbound()!=Integer.MAX_VALUE)
//        {
//          System.out.println("size-0-break");
//        }
//      }
    }
//    for(TreeNode node:_faterNodes.keySet())
//    {
//      if(_faterNodes.get(node).contains(rootNode))
//      {
//        System.out.println("break");
//      }
//    }
//    for(TreeNode node:_sonNodes.keySet())
//    {
//      if(_sonNodes.get(node).contains(rootNode))
//      {
//        System.out.println("break");
//      }
//    }

//    for(TreeNode node:_faterNodes.keySet())
//    {
//      boolean no=false;
//      List<TreeNode> nodeList=_faterNodes.get(node);
//      for(TreeNode node1:_faterNodes.get(node))
//      {
//        if(!(node1.pathSize.getLowerbound()>node.pathSize.getUpperbound()||node1.pathSize.getUpperbound()<node.pathSize.getLowerbound()))
//        {
//          no=true;
//          break;
//        }
//      }
//      if(!no&&node.pathSize.getUpperbound()!=Integer.MAX_VALUE)
//      {
//        System.out.println("break");
//      }
//    }


    _sonNodes.remove(rootNode);
    _faterNodes.remove(rootNode);
    _virtualNodes.remove(rootNode);
  }

  private void deleteSubtree(Queue<TreeNode> sonNodeQueue) {
//    if(node.value.equals("60")&&node.pathHash==1219960704)
//    {
//      System.out.println("break");
//    }
//    for (TreeNode child : children) {
//      deleteSubtree(child);
//      _sonNodes.remove(child);
//      _faterNodes.remove(child);
//      _virtualNodes.remove(child);
//    }

    while(!sonNodeQueue.isEmpty()) {
      TreeNode thisNode = sonNodeQueue.poll();
      for (TreeNode child : _sonNodes.get(thisNode)) {
        sonNodeQueue.add(child);
      }
      for (TreeNode fatherNode : _faterNodes.get(thisNode)) {
        _sonNodes.get(fatherNode).remove(thisNode);
      }
      for (TreeNode sonNode : _sonNodes.get(thisNode)) {
        _faterNodes.get(sonNode).remove(thisNode);
      }
      _sonNodes.remove(thisNode);
      _faterNodes.remove(thisNode);
    }
  }

  private void deleteSubtree(Queue<TreeNode> sonNodeQueue,SymBounder rootPathsize) {
    while(!sonNodeQueue.isEmpty()) {
//      System.out.println("in-delete");
      TreeNode thisNode = sonNodeQueue.poll();
//      if(!_sonNodes.containsKey(thisNode))
//      {
//        System.out.println("break");
//        continue;
//      }
      for (TreeNode child : _sonNodes.get(thisNode)) {
        if (child.pathSize.getLowerbound() >= rootPathsize.getLowerbound()
            && child.pathSize.getUpperbound() <= rootPathsize.getUpperbound()) {
//          if(!_sonNodes.containsKey(child))
//          {
//            System.out.println("break");
//          }
          if(!sonNodeQueue.contains(child))
          {
            sonNodeQueue.add(child);
          }
        }
      }
      for (TreeNode fatherNode : _faterNodes.get(thisNode)) {
        _sonNodes.get(fatherNode).remove(thisNode);
      }
      for (TreeNode sonNode : _sonNodes.get(thisNode)) {
        _faterNodes.get(sonNode).remove(thisNode);
//        if(sonNode.pathSize.getLowerbound()>=thisNode.pathSize.getLowerbound()&&sonNode.pathSize.getUpperbound()<=thisNode.pathSize.getUpperbound()&&!sonNodeQueue.contains(sonNode))
//        {
//          System.out.println("break");
//        }
//        for(TreeNode node:_faterNodes.keySet())
//        {
//          if(_faterNodes.get(node).size()==0&&node.pathSize.getUpperbound()!=Integer.MAX_VALUE&&!sonNodeQueue.contains(node)&&!node.equals(thisNode))
//          {
//            System.out.println("size-0-break");
//          }
//        }
      }
      if (_targetTempNodes.containsKey(thisNode)) {
        _tempNodes.get(_targetTempNodes.get(thisNode)).remove(thisNode);
        _targetTempNodes.remove(thisNode);
      }
//      for(TreeNode node:_faterNodes.keySet())
//      {
//        if(_faterNodes.get(node).size()==0&&!(node.pathSize.getLowerbound()>=rootPathsize.getLowerbound()&&node.pathSize.getUpperbound()<=rootPathsize.getUpperbound())&&node.pathSize.getUpperbound()!=Integer.MAX_VALUE)
//        {
//          System.out.println("size-0-break");
//        }
//      }
//      for(TreeNode node:_faterNodes.keySet())
//      {
//        if(_faterNodes.get(node).contains(thisNode))
//        {
//          System.out.println("break");
//        }
//      }
//      for(TreeNode node:_sonNodes.keySet())
//      {
//        if(_sonNodes.get(node).contains(thisNode))
//        {
//          System.out.println("break");
//        }
//      }
      _sonNodes.remove(thisNode);
      _faterNodes.remove(thisNode);
    }

//    for(TreeNode node:_faterNodes.keySet())
//    {
//      if(_faterNodes.get(node).size()==0)
//      {
//        System.out.println("size-0-break");
//      }
//    }
//      for(TreeNode child:_sonNodes.get(thisNode))
//      {
//       if(child.pathSize.getLowerbound()>=rootPathsize.getLowerbound()&&child.pathSize.getUpperbound()<=rootPathsize.getUpperbound())
//       {
//         sonNodeQueue.add(child);
//         for(TreeNode fatherNode:_faterNodes.get(child))
//         {
//           if(fatherNode.equals(thisNode))
//           {
//             continue;
//           }
//           _sonNodes.get(fatherNode).remove(child);
//         }
//         _sonNodes.remove(child);
//         _faterNodes.remove(child);
//       }
//      }
//    }
//    List<TreeNode> deleteChildNode=new ArrayList<>();
//    if (!_sonNodes.containsKey(node))
//    {
//      System.out.println("break");
//      return;
//    }
//    for(TreeNode child:_sonNodes.get(node))
//    {
//      if(child.value.equals("10160")&&child.pathHash==597928931&&child.pathSizeHash==961)
//      {
//        System.out.println("break");
//      }
//      if(child.pathSize.getLowerbound()>=rootPathsize.getLowerbound()&&child.pathSize.getUpperbound()<=rootPathsize.getUpperbound())
//      {
//        if(_targetTempNodes.containsKey(child))
//        {
//          _tempNodes.get(_targetTempNodes.get(child)).remove(child);
//          _targetTempNodes.remove(child);
//        }
//        deleteSubtree(child,rootPathsize);
//        for(TreeNode fatherNode:_faterNodes.get(child))
//        {
//          if(fatherNode.equals(node))
//          {
//            deleteChildNode.add(child);
//            continue;
//          }
//          _sonNodes.get(fatherNode).remove(child);
//        }
//        _sonNodes.remove(child);
//        _faterNodes.remove(child);
//      }
////      deleteSubtree(child,rootPathsize);
////      if(rootPathsize.getLowerbound()<=child.pathSize.getLowerbound()&&rootPathsize.getUpperbound()>=child.pathSize.getUpperbound())
////      {
////        for(TreeNode childFatherNode:_faterNodes.get(child))
////        {
////          if(childFatherNode.equals(node))
////          {
////            deleteChildNode.add(child);
////            continue;
////          }
////          _sonNodes.get(childFatherNode).remove(child);
////        }
////        for(TreeNode childeSonNode:_sonNodes.get(child))
////        {
////          _faterNodes.get(childeSonNode).remove(child);
////          if(_faterNodes.get(childeSonNode).size()==0)
////          {
////            deleteVirtualNode(childeSonNode);
////            _faterNodes.remove(childeSonNode);
////            _sonNodes.remove(childeSonNode);
////            _virtualNodes.remove(childeSonNode);
////          }
////        }
////        _sonNodes.remove(child);
////        _faterNodes.remove(child);
////        _virtualNodes.remove(child);
////      }
//    }
  }


  public List<TreeNode> getParentNode(TreeNode rootNode)
  {
    return _faterNodes.get(rootNode);
  }

  public boolean containsNode(TreeNode rootNode)
  {
    if(_sonNodes.get(rootNode)==null||_faterNodes.get(rootNode)==null)
    {
      return false;
    }else{
      return true;
    }
  }

  // 在指定节点下增加子节点

//  public static void main(String[] args) {
//    RouteForest forest = new RouteForest();
//
//    forest.addTree("A");
//    forest.addChild("A", "B");
//    forest.addChild("A", "C");
//    forest.addChild("B", "D");
//    forest.addChild("B", "E");
//    forest.addChild("C", "F");
//    forest.addChild("C", "G");
//    forest.addChild("C","E");
//
//    forest.removeNodeAndChildren("B");
//
//    List<TreeNode> descendantsOfA = forest.getAllDescendants("A");
//    System.out.println("Descendants of A after removing B:");
//    for (TreeNode node : descendantsOfA) {
//      System.out.println(node.value);
//    }
//  }

  public void clearTempNodes()
  {
    _tempNodes.clear();
    _targetTempNodes.clear();
  }
}
