package org.batfish.symwork;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.BgpAdvertisement.BgpAdvertisementType;

public enum Reason {
  ADD,
  UPDATE,
  WITHDRAW,
  DIVIDE,
  NORMAL;
  private static final Map<String, Reason> _map = buildMap();

  private static Map<String, Reason> buildMap() {
    ImmutableMap.Builder<String, Reason> map = ImmutableMap.builder();
    for (Reason reason : Reason.values()) {
      String name = reason.toString().toLowerCase();
      map.put(name, reason);
    }
    return map.build();
  }

  @JsonCreator
  public static Reason fromName(String name) {
    String lName = name.toLowerCase();
    Reason reason = _map.get(lName);
    if (reason == null) {
      throw new BatfishException("Invalid name: \"" + name + "\"");
    }
    return reason;
  }

  @JsonValue
  public String getName() {
    return name().toLowerCase();
  }
}
