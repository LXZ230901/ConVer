package org.batfish.symwork;









public class ForwardingNexthop {
  private String _nexthopNode;
  private Integer _topologyCondition;

  public ForwardingNexthop(String nexthopNode, Integer topologyCondition)
  {
    _nexthopNode = nexthopNode;
    _topologyCondition = topologyCondition;
  }

  public void setNexthopNode(String nexthopNode)
  {
    _nexthopNode = nexthopNode;
  }

  public void setMatchTopologyCondition(Integer topologyCondition)
  {
    _topologyCondition = topologyCondition;
  }

  public String getNexthopNode()
  {
    return _nexthopNode;
  }

  public Integer getTopologyCondition()
  {
    return _topologyCondition;
  }
}
