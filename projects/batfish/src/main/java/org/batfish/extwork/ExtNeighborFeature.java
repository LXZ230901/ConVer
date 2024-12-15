package org.batfish.extwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.batfish.datamodel.BgpNeighbor;
import org.batfish.symwork.ExternalNeighbor;

public class ExtNeighborFeature {
  private HashMap<String, HashSet<String>> _externalExportPolicy;
  private HashMap<String, HashSet<String>> _externalImportPolicy;
  private HashMap<ExternalNeighbor, List<BgpNeighbor>> _extNeighborLink;
  private Set<ExternalNeighbor> _extNeighbor;
  private HashMap<String, HashSet<String>> _internalPolicy;

  public ExtNeighborFeature()
  {
    _externalExportPolicy = new HashMap<>();
    _externalImportPolicy = new HashMap<>();
    _extNeighborLink = new HashMap<>();
    _extNeighbor = new HashSet<>();
    _internalPolicy = new HashMap<>();
  }

  public ExtNeighborFeature(HashMap<String, HashSet<String>> externalExportPolicy,
      HashMap<String, HashSet<String>> externalImportPolicy,
      HashMap<ExternalNeighbor, List<BgpNeighbor>> extNeighborLink,
      Set<ExternalNeighbor> extNeighbor,
      HashMap<String, HashSet<String>> internalPolicy) {
    this._externalExportPolicy = externalExportPolicy;
    this._externalImportPolicy = externalImportPolicy;
    this._extNeighborLink = extNeighborLink;
    this._extNeighbor = extNeighbor;
    this._internalPolicy = internalPolicy;
  }

  // get 和 set 方法
  public HashMap<String, HashSet<String>> getExternalExportPolicy() {
    return _externalExportPolicy;
  }

  public void setExternalExportPolicy(HashMap<String, HashSet<String>> externalExportPolicy) {
    this._externalExportPolicy = externalExportPolicy;
  }

  public HashMap<String, HashSet<String>> getExternalImportPolicy() {
    return _externalImportPolicy;
  }

  public void setExternalImportPolicy(HashMap<String, HashSet<String>> externalImportPolicy) {
    this._externalImportPolicy = externalImportPolicy;
  }

  public HashMap<ExternalNeighbor, List<BgpNeighbor>> getExtNeighborLink() {
    return _extNeighborLink;
  }

  public void setExtNeighborLink(HashMap<ExternalNeighbor, List<BgpNeighbor>> extNeighborLink) {
    this._extNeighborLink = extNeighborLink;
  }

  public Set<ExternalNeighbor> getExtNeighbor()
  {
    return this._extNeighbor;
  }

  public void setExtNeighbor(Set<ExternalNeighbor> extNeighbor)
  {
    this._extNeighbor = extNeighbor;
  }

  public HashMap<String, HashSet<String>> getInternalPolicy()
  {
    return this._internalPolicy;
  }

  public void setInternalPolicy(HashMap<String, HashSet<String>> internalPolicy)
  {
    this._internalPolicy = internalPolicy;
  }

  // equals 方法
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ExtNeighborFeature that = (ExtNeighborFeature) obj;
    return Objects.equals(_externalExportPolicy, that._externalExportPolicy) &&
        Objects.equals(_externalImportPolicy, that._externalImportPolicy) &&
        Objects.equals(_extNeighborLink, that._extNeighborLink) &&
        Objects.equals(_extNeighbor, that._extNeighbor) &&
        Objects.equals(_internalPolicy, that._internalPolicy);
  }

  // hashCode 方法
  @Override
  public int hashCode() {
    return Objects.hash(_externalExportPolicy, _externalImportPolicy, _extNeighborLink, _extNeighbor, _internalPolicy);
  }
}
