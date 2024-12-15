package org.batfish.symwork;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.batfish.datamodel.IsisRouteOlder;

public class IsisRouteAdvertisement {
  private IsisRouteOlder _route;
  private boolean _withdraw;
  private Reason _reason;

  public enum Reason {
    ADD,
    REPLACE,
    WITHDRAW
  }

  /**
   * Create a new route advertisement, optionally allowing this to be a withdrawal advertisement
   *
   * @param route route that is advertised
   * @param withdraw whether the route is being withdrawn
   */
  public IsisRouteAdvertisement(@Nonnull IsisRouteOlder route, boolean withdraw, Reason reason) {
    _route = route;
    _withdraw = withdraw;
    _reason = reason;
  }

  /**
   * Create a new route advertisement
   *
   * @param route route that is advertised
   */
  public IsisRouteAdvertisement(@Nonnull IsisRouteOlder route) {
    _route = route;
    _withdraw = false;
    _reason = Reason.ADD;
  }

  /** Get the underlying route that's being advertised (or withdrawn) */
  public IsisRouteOlder getRoute() {
    return _route;
  }

  public Reason getReason() {
    return _reason;
  }

  /**
   * Check if this route is being withdrawn
   *
   * @return true if the route is being withdrawn
   */
  public boolean isWithdrawn() {
    return _withdraw;
  }

  @Override
  public int hashCode() {
    return _route.hashCode() + Boolean.hashCode(_withdraw) + Objects.hash(_reason.ordinal());
  }

  @Override
  public boolean equals(Object obj) {
    return (this == obj)
        || (obj instanceof IsisRouteAdvertisement
        && _withdraw == ((IsisRouteAdvertisement) obj)._withdraw
        && _reason == ((IsisRouteAdvertisement) obj)._reason
        && _route.equals(((IsisRouteAdvertisement) obj)._route));
  }

  @Override
  public String toString() {
    return _route + ", withdraw=" + _withdraw + ", reason=" + _reason;
  }
}
