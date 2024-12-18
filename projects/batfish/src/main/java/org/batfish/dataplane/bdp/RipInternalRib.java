package org.batfish.dataplane.bdp;

import org.batfish.datamodel.RipInternalRoute;

public class RipInternalRib extends AbstractRib<RipInternalRoute> {

  private static final long serialVersionUID = 1L;

  RipInternalRib(VirtualRouter owner) {
    super(owner);
  }

  @Override
  public int comparePreference(RipInternalRoute lhs, RipInternalRoute rhs) {
    // reversed on purpose, because lower metric is more preferable
    return Long.compare(rhs.getMetric(), lhs.getMetric());
  }
}
