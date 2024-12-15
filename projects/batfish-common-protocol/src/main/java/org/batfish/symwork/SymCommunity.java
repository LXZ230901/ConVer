package org.batfish.symwork;

import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

public class SymCommunity {
  public SortedSet<Long> _denyCommunities;
  public SortedSet<Long> _permitCommunities;

  public boolean _denyAll;

  public boolean _permitAll;

  public SymCommunity()
  {
    _denyAll=false;
    _permitAll=true;
    _denyCommunities=new TreeSet<>();
    _permitCommunities=new TreeSet<>();
  }
  public SymCommunity(boolean no)
  {
    _denyAll=true;
    _permitAll=false;
    _denyCommunities=new TreeSet<>();
    _permitCommunities=new TreeSet<>();
  }
  public SymCommunity(SortedSet<Long> communities)
  {
    _denyAll=false;
    _permitAll=false;
    _denyCommunities=new TreeSet<>();
    _permitCommunities=new TreeSet<>();
    _permitCommunities.addAll(communities);
  }

  public SymCommunity(SymCommunity community)
  {
    _denyAll= community._denyAll;
    _permitAll= community._permitAll;
    _denyCommunities=new TreeSet<>();
    _permitCommunities=new TreeSet<>();
    _denyCommunities.addAll(community._denyCommunities);
    _permitCommunities.addAll(community._permitCommunities);
  }
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof SymCommunity)) {
      return false;
    }
    SymCommunity other = (SymCommunity) o;
    if(_denyAll!= other._denyAll)
    {
      return false;
    }
    if(_permitAll!= other._permitAll)
    {
      return false;
    }
    if(!_denyCommunities.equals(other._denyCommunities))
    {
      return false;
    }
    if(!_permitCommunities.equals(other._permitCommunities))
    {
      return false;
    }
    return true;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (_denyAll?1:0);
    result = prime * result + (_permitAll?1:0);
    result = prime * result + _denyCommunities.hashCode();
    result = prime * result + _permitCommunities.hashCode();
    return result;
  }
}
