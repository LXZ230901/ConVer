package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdd.bdd.BDD;
import org.batfish.common.util.CommonUtil;

@JsonSchemaDescription("A line in a CommunityList")
public class CommunityListLine implements Serializable {

  private static final String PROP_ACTION = "action";

  private static final String PROP_REGEX = "regex";

  private static final long serialVersionUID = 1L;

  private final LineAction _action;

  private final String _regex;

  private Automaton _automaton;

  private Set<Integer> _matchCommunityAtom;

  private Set<Integer> _symCommunityAtom;

  private HashMap<Integer, Integer> _communityAtomToBDD;

  private BDD _communityAtomBDD;

  @JsonCreator
  public CommunityListLine(
      @JsonProperty(PROP_ACTION) LineAction action, @JsonProperty(PROP_REGEX) String regex) {
    _action = action;
    _regex = regex;
    String automatonRegex = _regex;
    automatonRegex = automatonRegex.replace("^", "");
    automatonRegex = automatonRegex.replace("$", "");
    RegExp regExp = new RegExp(automatonRegex);
    _automaton = regExp.toAutomaton();
    _matchCommunityAtom = new HashSet<>();
    _symCommunityAtom = new HashSet<>();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof CommunityListLine)) {
      return false;
    }
    CommunityListLine other = (CommunityListLine) o;
    if (_action != other._action) {
      return false;
    }
    if (!_regex.equals(other._regex)) {
      return false;
    }
    return true;
  }

  @JsonProperty(PROP_ACTION)
  @JsonPropertyDescription(
      "The action the underlying access-list will take when this line matches a route.")
  public LineAction getAction() {
    return _action;
  }

  public Set<Long> getExactMatchingCommunities(Set<Long> allCommunities) {
    Pattern p = Pattern.compile(_regex);
    Set<Long> matchingCommunitites = new LinkedHashSet<>();
    for (long candidateCommunity : allCommunities) {
      String candidateCommunityStr = CommonUtil.longToCommunity(candidateCommunity);
      Matcher matcher = p.matcher(candidateCommunityStr);
      if (matcher.matches()) {
        matchingCommunitites.add(candidateCommunity);
      }
    }
    return matchingCommunitites;
  }

  public Set<Long> getMatchingCommunities(Set<Long> allCommunities, boolean invertMatch) {
    Pattern p = Pattern.compile(_regex);
    Set<Long> matchingCommunitites = new LinkedHashSet<>();
    for (long candidateCommunity : allCommunities) {
      String candidateCommunityStr = CommonUtil.longToCommunity(candidateCommunity);
      Matcher matcher = p.matcher(candidateCommunityStr);
      if (matcher.find() ^ invertMatch) {
        matchingCommunitites.add(candidateCommunity);
      }
    }
    return matchingCommunitites;
  }

  @JsonProperty(PROP_REGEX)
  @JsonPropertyDescription("The regex against which a route's communities will be compared")
  public String getRegex() {
    return _regex;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _action.ordinal();
    result = prime * result + _regex.hashCode();
    return result;
  }


  public Long toLiteralCommunity() {
    return CommonUtil.communityStringToLong(_regex.substring(1,_regex.length()-1));
    // TODO Auto-generated method stub
  }










  public Set<Integer> getSymCommunityAtom()
  {
    return _symCommunityAtom;
  }

  public void toSymCommunityAtom(HashMap<Integer, Integer> communityAtomToBDD)
  {
    for (Integer communityAtomInteger : _matchCommunityAtom)
    {
      _symCommunityAtom.add(communityAtomToBDD.get(communityAtomInteger));
    }
  }

  public void toCommunityAtom(HashMap<Automaton, Integer> communityAtomToInteger)
  {
     if (communityAtomToInteger.containsKey(_automaton))
     {
       _matchCommunityAtom.add(communityAtomToInteger.get(_automaton));
       return;
     }
     for (Automaton communityAtom : communityAtomToInteger.keySet())
     {
        if (!_automaton.intersection(communityAtom).isEmpty())
        {
          _matchCommunityAtom.add(communityAtomToInteger.get(communityAtom));
        }
     }
  }







  // Getter for _communityAtomToBDD
  public HashMap<Integer, Integer> getCommunityAtomToBDD() {
    return _communityAtomToBDD;
  }

  // Setter for _communityAtomToBDD
  public void setCommunityAtomToBDD(HashMap<Integer, Integer> communityAtomToBDD) {
    this._communityAtomToBDD = communityAtomToBDD;
  }

  // Getter for _communityAtomBDD
  public BDD getCommunityAtomBDD() {
    return _communityAtomBDD;
  }

  // Setter for _communityAtomBDD
  public void setCommunityAtomBDD(BDD communityAtomBDD) {
    this._communityAtomBDD = communityAtomBDD;
  }

  public Set<Integer> getMatchCommunityAtom()
  {
    return _matchCommunityAtom;
  }
}
