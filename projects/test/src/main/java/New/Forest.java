package New;

import java.util.*;

class TreeNode {
  String value;
  List<TreeNode> children;

  TreeNode(String value) {
    this.value = value;
    this.children = new ArrayList<>();
  }

  // 添加子节点
  public void addChild(TreeNode child) {
    children.add(child);
  }

  // 获取节点的所有后代节点
  public List<TreeNode> getAllDescendants() {
    List<TreeNode> descendants = new ArrayList<>();
    for (TreeNode child : children) {
      descendants.add(child);
      descendants.addAll(child.getAllDescendants());
    }
    return descendants;
  }

  // 递归删除节点及其所有子节点
  public void removeNodeAndChildren() {
    children.clear();
  }
}

public class Forest {
  Map<String, TreeNode> forest;
  Map<String, TreeNode> nodeMap; // 保存节点的引用

  public Forest() {
    this.forest = new HashMap<>();
    this.nodeMap = new HashMap<>();
  }

  // 增加树
  public void addTree(String rootValue) {
    TreeNode root = new TreeNode(rootValue);
    forest.put(rootValue, root);
    nodeMap.put(rootValue, root);
  }

  // 在指定节点下增加子节点
  public void addChild(String parentValue, String childValue) {
    TreeNode parent = nodeMap.get(parentValue);
    if (parent != null) {
      TreeNode child = new TreeNode(childValue);
      parent.addChild(child);
      nodeMap.put(childValue, child);
    } else {
      System.out.println("Parent node not found in the forest.");
    }
  }

  // 获取节点的所有后代节点
  public List<TreeNode> getAllDescendants(String value) {
    TreeNode node = nodeMap.get(value);
    if (node != null) {
      return node.getAllDescendants();
    } else {
      System.out.println("Node not found in the forest.");
      return new ArrayList<>();
    }
  }

  // 删除指定节点及其所有子节点
  public void removeNodeAndChildren(String value) {
    TreeNode nodeToRemove = nodeMap.get(value);
    if (nodeToRemove != null) {
      TreeNode parentNode = getParentNode(value);
      if (parentNode != null) {
        parentNode.children.remove(nodeToRemove);
      }
      nodeToRemove.removeNodeAndChildren();
      nodeMap.remove(value);
    } else {
      System.out.println("Node not found in the forest.");
    }
  }

  // 获取指定节点的父节点
  private TreeNode getParentNode(String value) {
    for (TreeNode node : forest.values()) {
      if (node.children.stream().anyMatch(child -> child.value.equals(value))) {
        return node;
      }
    }
    return null;
  }

  public static void main(String[] args) {
    Forest forest = new Forest();

    forest.addTree("A");
    forest.addChild("A", "B");
    forest.addChild("A", "C");
    forest.addChild("B", "D");
    forest.addChild("B", "E");
    forest.addChild("C", "F");
    forest.addChild("C", "G");
    forest.addChild("C","E");

    forest.removeNodeAndChildren("B");

    List<TreeNode> descendantsOfA = forest.getAllDescendants("A");
    System.out.println("Descendants of A after removing B:");
    for (TreeNode node : descendantsOfA) {
      System.out.println(node.value);
    }
  }
}
