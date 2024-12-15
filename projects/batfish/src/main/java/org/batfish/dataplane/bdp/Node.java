package org.batfish.dataplane.bdp;

import java.util.SortedMap;
import java.util.TreeMap;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.Configuration;

public final class Node extends ComparableStructure<String> {
//具体的路由器节点
  /** */
  private static final long serialVersionUID = 1L;

  final Configuration _c;//这个路由其节点的配置

  SortedMap<String, VirtualRouter> _virtualRouters;//路由器节点的转发实例

  public Node(Configuration c) {
    super(c.getHostname());
    _c = c;
    _virtualRouters = new TreeMap<>();
    for (String vrfName : _c.getVrfs().keySet()) {
      VirtualRouter vr = new VirtualRouter(vrfName, _c);
      vr.initRibs();
      _virtualRouters.put(vrfName, vr);
    }
  }

  public Configuration getConfiguration() {
    return _c;
  }
}
