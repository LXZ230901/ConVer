package org.batfish.isis;

import javax.annotation.Nonnull;
import org.batfish.datamodel.IsisRoute;
import org.batfish.symwork.Reason;

public class IsisMultiRouteAdvertisement {
  private IsisRoute _route;
  private Reason _reason;
  public IsisMultiRouteAdvertisement(@Nonnull IsisRoute route, boolean withdraw, Reason reason) {
    _route = route;
    _reason = reason;
  }

  public IsisMultiRouteAdvertisement(@Nonnull IsisRoute route) {
    _route = route;
    _reason = Reason.ADD;
  }

  public IsisRoute getRoute() {
    return _route;
  }

  public Reason getReason() {
    return _reason;
  }
}
