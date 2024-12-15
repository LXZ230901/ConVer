package org.batfish.dataplane.bdp;

import org.batfish.datamodel.ConnectedRoute;

public class ConnectedRib extends AbstractRib<ConnectedRoute> {
//直连路由表,和初始的abtract rib相比没有什么需要更改的地方
  /** */
  private static final long serialVersionUID = 1L;

  public ConnectedRib(VirtualRouter owner) {
    super(owner);
  }

  @Override
  public int comparePreference(ConnectedRoute lhs, ConnectedRoute rhs) {
    return 0;
  }
}
