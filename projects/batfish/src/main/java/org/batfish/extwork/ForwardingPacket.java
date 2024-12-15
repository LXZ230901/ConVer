package org.batfish.extwork;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

public class ForwardingPacket {
  private BitSet _prefix;
  private List<String> _nodePath;
  private String _dstNode;
  private Integer _topologyCondition;

  public ForwardingPacket(BitSet prefix, List<String> nodePath, Integer topologyCondition)
  {
    _prefix = (BitSet) prefix.clone();
    _nodePath = new ArrayList<>();
    _dstNode = "";
    _nodePath.addAll(nodePath);
    _topologyCondition = topologyCondition;
  }

  public BitSet getPrefix()
  {
    return _prefix;
  }

  public List<String> getNodePath()
  {
    return _nodePath;
  }

  public Integer getTopologyCondition()
  {
    return _topologyCondition;
  }

  public Builder toBuilder()
  {
    return new Builder(this);
  }

  public void setPrefix(BitSet prefix)
  {
    _prefix = (BitSet) prefix.clone();
  }




  public void setTopologyCondition(Integer topologyCondition)
  {
    _topologyCondition = topologyCondition;
  }

  public static class Builder
  {
    private BitSet _prefix;
    private List<String> _nodePath;
    private String _dstNode;
    private Integer _topologyCondition;

    public Builder(ForwardingPacket forwardingPacket)
    {
      _prefix = (BitSet) forwardingPacket.getPrefix().clone();
      _nodePath = new ArrayList<>();
      _nodePath.addAll(forwardingPacket.getNodePath());
      _topologyCondition = forwardingPacket.getTopologyCondition();
    }

    public ForwardingPacket build()
    {
      return new ForwardingPacket(_prefix, _nodePath, _topologyCondition);
    }
  }
}
