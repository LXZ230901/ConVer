package org.batfish.symwork;

import java.util.HashSet;


import java.util.Objects;

public class PrefixLengthUnit {
  private String _prefix;
  private HashSet<String> _length;

  // 构造函数，初始化 _prefix 和 _length
  public PrefixLengthUnit(String prefix, HashSet<String> length) {
    this._prefix = prefix;
    this._length = new HashSet<>(length);
  }

  // 无参构造函数
  public PrefixLengthUnit() {
    this._prefix = "";
    this._length = new HashSet<>();
  }

  // Getter 和 Setter 方法
  public String getPrefix() {
    return _prefix;
  }

  public void setPrefix(String prefix) {
    this._prefix = prefix;
  }

  public HashSet<String> getLength() {
    return new HashSet<>(_length);
  }

  public void setLength(HashSet<String> length) {
    this._length = new HashSet<>(length);
  }

  // 重写 equals 方法
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    PrefixLengthUnit that = (PrefixLengthUnit) obj;
    return Objects.equals(_prefix, that._prefix) && Objects.equals(_length, that._length);
  }

  // 重写 hashCode 方法
  @Override
  public int hashCode() {
    return Objects.hash(_prefix, _length);
  }

  // toString 方法（可选）
  @Override
  public String toString() {
    return "PrefixLengthUnit{" +
        "_prefix='" + _prefix + '\'' +
        ", _length=" + _length +
        '}';
  }
}
