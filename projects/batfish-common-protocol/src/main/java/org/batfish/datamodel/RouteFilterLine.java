package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

import java.util.BitSet;
import net.sf.javabdd.BDD;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.batfish.symwork.ConvertPrefixToSymPrefix;
import org.batfish.symwork.PrefixRelationshipGenerator;
import org.batfish.symwork.SymPrefixList;
import org.batfish.symwork.SymPrefixOp;

import java.util.ArrayList;
import java.io.Serializable;

@JsonSchemaDescription("A line in an RouteFilterList")
public class RouteFilterLine implements Serializable {

  private static final String PROP_ACTION = "action";

  private static final String PROP_LENGTH_RANGE = "lengthRange";

  private static final String PROP_PREFIX = "prefix";

  private static final long serialVersionUID = 1L;

  private final LineAction _action;

  private final SubRange _lengthRange;

  private final Prefix _prefix;

  private boolean _alreadyConvert;

  private HashSet<Integer> _matchPECNum;

  private BitSet _matchPECSet;

  @JsonCreator
  public RouteFilterLine(
      @JsonProperty(PROP_ACTION) LineAction action,
      @JsonProperty(PROP_PREFIX) Prefix prefix,
      @JsonProperty(PROP_LENGTH_RANGE) SubRange lengthRange) {
    _action = action;
    _prefix = prefix;
    _lengthRange = lengthRange;
    _alreadyConvert = false;
    _matchPECNum = new HashSet<>();
    _matchPECSet = new BitSet();
  }

  public RouteFilterLine(LineAction action, PrefixRange prefixRange) {
    this(action, prefixRange.getPrefix(), prefixRange.getLengthRange());
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RouteFilterLine)) {
      return false;
    }
    RouteFilterLine other = (RouteFilterLine) o;
    if (_action != other._action) {
      return false;
    }
    if (!_lengthRange.equals(other._lengthRange)) {
      return false;
    }
    if (!_prefix.equals(other._prefix)) {
      return false;
    }
    return true;
  }

  @JsonProperty(PROP_ACTION)
  @JsonPropertyDescription(
      "The action the underlying access-list will take when this line matches an IPV4 route.")
  public LineAction getAction() {
    return _action;
  }

  @JsonProperty(PROP_LENGTH_RANGE)
  @JsonPropertyDescription("The range of acceptable prefix-lengths for a route.")
  public SubRange getLengthRange() {
    return _lengthRange;
  }

  @JsonProperty(PROP_PREFIX)
  @JsonPropertyDescription(
      "The bits against which to compare a route's prefix. The length of this prefix is used to "
          + "determine how many leading bits must match.")
  public Prefix getPrefix() {
    return _prefix;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _action.ordinal();
    result = prime * result + _lengthRange.hashCode();
    result = prime * result + _prefix.hashCode();
    return result;
  }








  public String toCompactString() {
    StringBuilder sb = new StringBuilder();
    sb.append(_action + " ");
    sb.append(_prefix + " ");
    sb.append(_lengthRange + " ");
    return sb.toString();
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{ ");
    sb.append("Action=" + _action + " ");
    sb.append("Prefix=" + _prefix + " ");
    sb.append("LengthRange=" + _lengthRange + " ");
    sb.append("}");
    return sb.toString();
  }

  public void toPEC()
  {
    _alreadyConvert = true;
    HashMap<String, List<String>> prefixSpaceConvert = new HashMap<>();
    String length = "[" + _lengthRange.getStart() + "," + _lengthRange.getEnd() + "]";
    List<String> lengthList = new ArrayList<> ();
    lengthList.add(length);
    prefixSpaceConvert.put(_prefix.toString(), lengthList);
    ConvertPrefixToSymPrefix prefixConverter = new ConvertPrefixToSymPrefix(prefixSpaceConvert, PrefixRelationshipGenerator.getPrefixFactory());
    HashMap<BDD, Long> symPrefix = prefixConverter.convertPrefix();

    if (PrefixRelationshipGenerator.getPrefixToPEC().containsKey(_prefix.toString()))
    {
      for (SymPrefixList pec : PrefixRelationshipGenerator.getPrefixToPEC().get(_prefix.toString()))
      {
        if (!SymPrefixOp.and(pec.getPrefixList(), symPrefix).isEmpty())
        {
          _matchPECNum.add(PrefixRelationshipGenerator.getPECToPECNum().get(pec));
        }
      }
    } else {
      System.out.println("Convert ExplicitPrefixSet error - do not have match prefix ip!");
    }
  }

  public HashSet<Integer> getMatchPEC()
  {
    if (!_alreadyConvert)
    {
      HashMap<String, List<String>> prefixSpaceConvert = new HashMap<>();
      String length = "[" + _lengthRange.getStart() + "," + _lengthRange.getEnd() + "]";
      List<String> lengthList = new ArrayList<> ();
      lengthList.add(length);
      prefixSpaceConvert.put(_prefix.toString(), lengthList);
      ConvertPrefixToSymPrefix prefixConverter = new ConvertPrefixToSymPrefix(prefixSpaceConvert, PrefixRelationshipGenerator.getPrefixFactory());
      HashMap<BDD, Long> symPrefix = prefixConverter.convertPrefix();
      
      if (PrefixRelationshipGenerator.getPrefixToPEC().containsKey(_prefix.toString()))
      {
        for (SymPrefixList pec : PrefixRelationshipGenerator.getPrefixToPEC().get(_prefix.toString()))
        {
          if (!SymPrefixOp.and(pec.getPrefixList(), symPrefix).isEmpty())
          {
            _matchPECNum.add(PrefixRelationshipGenerator.getPECToPECNum().get(pec));
          }
        }
      } else {
        System.out.println("Convert ExplicitPrefixSet error - do not have match prefix ip!");
      }
      for (Integer pec : _matchPECNum)
      {
        _matchPECSet.set(pec);
      }
      _alreadyConvert = true;
    }
    return _matchPECNum;
  }

  public BitSet getMatchPECSet()
  {
    return _matchPECSet;
  }
}
