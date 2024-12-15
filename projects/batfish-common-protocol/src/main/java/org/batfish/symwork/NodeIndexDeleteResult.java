package org.batfish.symwork;

public class NodeIndexDeleteResult {
  public String _node;
  public Integer _asPathHash;
  public NodeIndexDeleteResult(String node,Integer asPathHash)
  {
    _node=node;
    _asPathHash=asPathHash;
  }
}
