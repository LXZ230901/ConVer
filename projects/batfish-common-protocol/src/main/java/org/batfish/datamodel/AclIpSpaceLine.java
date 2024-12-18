package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import javax.annotation.Nonnull;

public class AclIpSpaceLine implements Comparable<AclIpSpaceLine>, Serializable {

  public static class Builder {

    private LineAction _action;

    private IpSpace _ipSpace;

    private Builder() {
      _action = LineAction.ACCEPT;
    }

    public AclIpSpaceLine build() {
      return new AclIpSpaceLine(_ipSpace, _action);
    }

    public Builder setAction(@Nonnull LineAction action) {
      _action = action;
      return this;
    }

    public Builder setIpSpace(@Nonnull IpSpace ipSpace) {
      _ipSpace = ipSpace;
      return this;
    }
  }

  public static final AclIpSpaceLine PERMIT_ALL =
      AclIpSpaceLine.builder().setIpSpace(UniverseIpSpace.INSTANCE).build();

  private static final String PROP_ACTION = "action";

  private static final String PROP_IP_SPACE = "ipSpace";

  /** */
  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  public static AclIpSpaceLine permit(IpSpace ipSpace) {
    return builder().setIpSpace(ipSpace).build();
  }

  public static AclIpSpaceLine reject(IpSpace ipSpace) {
    return builder().setIpSpace(ipSpace).setAction(LineAction.REJECT).build();
  }

  private final LineAction _action;

  private final IpSpace _ipSpace;

  @JsonCreator
  private AclIpSpaceLine(
      @JsonProperty(PROP_IP_SPACE) @Nonnull IpSpace ipSpace,
      @JsonProperty(PROP_ACTION) @Nonnull LineAction action) {
    _ipSpace = ipSpace;
    _action = action;
  }

  @Override
  public int compareTo(AclIpSpaceLine o) {
    return Comparator.comparing(AclIpSpaceLine::getAction)
        .thenComparing(AclIpSpaceLine::getIpSpace)
        .compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof AclIpSpaceLine)) {
      return false;
    }
    AclIpSpaceLine rhs = (AclIpSpaceLine) o;
    return Objects.equals(_action, rhs._action) && Objects.equals(_ipSpace, rhs._ipSpace);
  }

  @JsonProperty(PROP_ACTION)
  public LineAction getAction() {
    return _action;
  }

  @JsonProperty(PROP_IP_SPACE)
  public IpSpace getIpSpace() {
    return _ipSpace;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_action.ordinal(), _ipSpace);
  }

  public Builder toBuilder() {
    return builder().setAction(_action).setIpSpace(_ipSpace);
  }

  @Override
  public String toString() {
    ToStringHelper helper = MoreObjects.toStringHelper(getClass());
    if (_action != LineAction.ACCEPT) {
      helper.add(PROP_ACTION, _action);
    }
    helper.add(PROP_IP_SPACE, _ipSpace);
    return helper.toString();
  }
}
