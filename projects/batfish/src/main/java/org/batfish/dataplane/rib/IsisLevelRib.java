package org.batfish.dataplane.rib;

import java.util.HashMap;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.IsisRoute;

/** Rib for storing {@link IsisRoute}s */
@ParametersAreNonnullByDefault
public class IsisLevelRib extends AbstractRib<IsisRoute> {

  private static final long serialVersionUID = 1L;

  public IsisLevelRib(boolean withBackups) {
    super(new HashMap<>());
  }

  @Override
  public int comparePreference(IsisRoute lhs, IsisRoute rhs) {
    return IsisRib.routePreferenceComparator.compare(lhs, rhs);
  }
}
