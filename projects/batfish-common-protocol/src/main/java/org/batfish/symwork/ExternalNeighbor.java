package org.batfish.symwork;

import java.util.Objects;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;

public class ExternalNeighbor {
  public Prefix _prefix;
  public Integer _asNumber;
  // 构造方法
  public ExternalNeighbor(Prefix prefix, Integer asNumber) {
    this._prefix = prefix;
    this._asNumber = asNumber;
  }

  // equals 方法
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true; // 如果是同一个对象，则返回 true
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false; // 如果对象为 null 或不属于同一个类，则返回 false
    }
    ExternalNeighbor that = (ExternalNeighbor) obj;
    return Objects.equals(_prefix, that._prefix) && Objects.equals(_asNumber, that._asNumber);
  }

  // hashCode 方法，建议与 equals 方法配合使用
  @Override
  public int hashCode() {
    return Objects.hash(_prefix, _asNumber);
  }

  public Prefix getPrefix()
  {
    return this._prefix;
  }

  public Integer getAsNumber()
  {
    return this._asNumber;
  }
}
