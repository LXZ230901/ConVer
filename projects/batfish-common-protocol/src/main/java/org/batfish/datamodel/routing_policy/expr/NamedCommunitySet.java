package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableSortedSet;

import static org.batfish.common.util.CommonUtil.readFile;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.HashSet;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.symwork.AECComputeResult;

public class NamedCommunitySet extends CommunitySetExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private String _name;

  @JsonCreator
  private NamedCommunitySet() {}

  public NamedCommunitySet(String name) {
    _name = name;
  }

  @Override
  public SortedSet<Long> allCommunities(Environment environment) {
    ImmutableSortedSet.Builder<Long> out = ImmutableSortedSet.naturalOrder();
    CommunityList cl = environment.getConfiguration().getCommunityLists().get(_name);
    for (CommunityListLine line : cl.getLines()) {
      if (line.getAction()== LineAction.REJECT)
      {
        continue;
      }
      Long community = line.toLiteralCommunity();
      out.add(community);
    }
    return out.build();
  }

  public Set<Integer> getAllSymCommunities(Environment environment)
  {
    Set<Integer> symCommunity = new HashSet<> ();
    CommunityList cl = environment.getConfiguration().getCommunityLists().get(_name);
    for (CommunityListLine line : cl.getLines()) {
      if (line.getAction()== LineAction.REJECT)
      {
        continue;
      }
      symCommunity.addAll(line.getSymCommunityAtom());
    }
    return symCommunity;
  }

  @Override
  public SortedSet<Long> communities(Environment environment, Set<Long> communityCandidates) {
    CommunityList cl = environment.getConfiguration().getCommunityLists().get(_name);
    return communityCandidates
        .stream()
        .filter(cl::permits)
        .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
  }

  @Override
  public Set<Integer> getAllMatchSymCommunity(Environment environment)
  {
    CommunityList cl = environment.getConfiguration().getCommunityLists().get(_name);
    return cl.getSymCommunityAtom();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NamedCommunitySet other = (NamedCommunitySet) obj;
    if (_name == null) {
      if (other._name != null) {
        return false;
      }
    } else if (!_name.equals(other._name)) {
      return false;
    }
    return true;
  }

  public String getName() {
    return _name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_name == null) ? 0 : _name.hashCode());
    return result;
  }

  public void setName(String name) {
    _name = name;
  }

  @Override
  public Set<Integer> getCommunityAtom(Configuration configuration)
  {
    CommunityList cl = configuration.getCommunityLists().get(_name);
    Set<Integer> matchCommunityAtom = cl.getCommunityAtom();
    return matchCommunityAtom;
  }
}
