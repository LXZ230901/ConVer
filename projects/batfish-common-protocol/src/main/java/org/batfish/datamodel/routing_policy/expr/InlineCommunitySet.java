package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.Environment;

public class InlineCommunitySet extends CommunitySetExpr {

  private class CachedCommunitiesSupplier implements Serializable, Supplier<SortedSet<Long>> {

    private static final long serialVersionUID = 1L;

    @Override
    public SortedSet<Long> get() {
      return initCommunities();
    }
  }

  private static final long serialVersionUID = 1L;

  private final Supplier<SortedSet<Long>> _cachedCommunities;

  private List<CommunitySetElem> _communities;

  private List<String> _communityString;

  private Configuration _config;


  private Set<Integer> _matchCommunityAtom;

  private Set<Integer> _symCommunityAtom;

  @JsonCreator
  private InlineCommunitySet() {
    _cachedCommunities = Suppliers.memoize(new CachedCommunitiesSupplier());
  }

  public InlineCommunitySet(Collection<Long> communities, Configuration configuration) {
    this();
    _communities = communities.stream().map(CommunitySetElem::new).collect(Collectors.toList());
    configuration.addInlineCommunity(this);
    _communityString = new ArrayList<>();
    _matchCommunityAtom = new HashSet<>();
    _symCommunityAtom = new HashSet<>();
  }

  public InlineCommunitySet(List<CommunitySetElem> communities, Configuration configuration) {
    this();
    _communities = ImmutableList.copyOf(communities);
    configuration.addInlineCommunity(this);
    _communityString = new ArrayList<>();
    _matchCommunityAtom = new HashSet<>();
    _symCommunityAtom = new HashSet<>();
  }

  @Override
  public SortedSet<Long> allCommunities(Environment environment) {
    return _cachedCommunities.get();
  }

  @Override
  public SortedSet<Long> communities(Environment environment, Set<Long> communityCandidates) {
    return ImmutableSortedSet.copyOf(
        Sets.intersection(allCommunities(environment), communityCandidates));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof InlineCommunitySet)) {
      return false;
    }
    InlineCommunitySet other = (InlineCommunitySet) obj;
    return Objects.equals(_communities, other._communities);
  }

  public List<CommunitySetElem> getCommunities() {
    return _communities;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_communities);
  }

  private SortedSet<Long> initCommunities() {
    return _communities
        .stream()
        .map(CommunitySetElem::community)
        .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
  }


  public void setCommunities(List<CommunitySetElem> communities) {
    _communities = communities;
  }


  @Override
  public Set<Integer> getCommunityAtom(Configuration configuration)
  {
    Set<Integer> communityAtom = new HashSet<>();
    List<String> communityString = new ArrayList<>();
    for (CommunitySetElem communitySetElem : _communities)
    {
      String community = communitySetElem.getPrefix().toString() + ":" + communitySetElem.getSuffix().toString();
    }
    return communityAtom;
  }

  @Override
  public Set<Integer> getAllMatchSymCommunity(Environment environment)
  {
    return _symCommunityAtom;
  }

  public void addCommunityString(String communityString)
  {
    _communityString.add(communityString);
  }

  public void toCommunityAtom(HashMap<Automaton, Integer> communityAtomToInteger)
  {
    for (String community : _communityString)
    {
      RegExp communityReg = new RegExp(community);
      Automaton communityAutomaton = communityReg.toAutomaton();
      if (communityAtomToInteger.containsKey(communityAutomaton))
      {
        _matchCommunityAtom.add(communityAtomToInteger.get(communityAutomaton));
        continue;
      }
      for (Automaton communityAtom : communityAtomToInteger.keySet())
      {
        if (!communityAutomaton.intersection(communityAtom).isEmpty())
        {
          _matchCommunityAtom.add(communityAtomToInteger.get(communityAtom));
        }
      }
    }
  }

  public void toSymCommunityAtom(HashMap<Integer, Integer> communityAtomToBDD)
  {
    for (Integer communityAtomInteger : _matchCommunityAtom)
    {
      _symCommunityAtom.add(communityAtomToBDD.get(communityAtomInteger));
    }
  }
}
