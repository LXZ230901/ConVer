package org.batfish.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import org.batfish.common.BatfishException;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.symwork.ConvertPrefixToSymPrefix;
import org.batfish.symwork.RelatedNodeAnswer;
import org.batfish.symwork.SymPrefixList;

@JsonSchemaDescription("An access-list used to filter IPV4 routes")
public class RouteFilterList extends ComparableStructure<String> {

  private static final String PROP_LINES = "lines";

  private static final long serialVersionUID = 1L;

  private transient Set<Prefix> _deniedCache;

  private List<RouteFilterLine> _lines;

  private transient Set<Prefix> _permittedCache;

  public HashMap<BDD,Long> _symDeniedCache;

  public HashMap<BDD,Long> _symPermittedCache;

  public boolean _alreadyConvert;

  public BDDFactory _prefixFactory;

  public Set<Integer> _symDeniedCachaNum;

  public Set<Integer> _symPermittedCachaNum;

  public Set<Integer> _widePrefix;


  @JsonCreator
  public RouteFilterList(@JsonProperty(PROP_NAME) String name) {
    this(name, Collections.emptyList());
    this._alreadyConvert=false;
    _symPermittedCachaNum=new HashSet<>();
    _symDeniedCachaNum=new HashSet<>();
    _widePrefix=new HashSet<>();
  }

  public RouteFilterList(String name, List<RouteFilterLine> lines) {
    super(name);
    this._lines = lines;
    _symPermittedCachaNum=new HashSet<>();
    _symDeniedCachaNum=new HashSet<>();
    _widePrefix=new HashSet<>();
  }

  public void addLine(RouteFilterLine r) {
    _lines = ImmutableList.<RouteFilterLine>builder().addAll(_lines).add(r).build();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RouteFilterList)) {
      return false;
    }
    RouteFilterList other = (RouteFilterList) o;
    return other._lines.equals(_lines);
  }

  @JsonProperty(PROP_LINES)
  @JsonPropertyDescription("The lines against which to check an IPV4 route")
  public List<RouteFilterLine> getLines() {
    return _lines;
  }


  private boolean newPermits(Prefix prefix) {
    HashMap<String,List<String>> acceptPrefix=new HashMap<>();
    HashMap<String,List<String>> deniedPrefix=new HashMap<>();
    if(_alreadyConvert==false)
    {
      for(RouteFilterLine line:_lines)
      {
        if(line.getAction()==LineAction.ACCEPT)
        {
          String addPrefix=line.getPrefix().toString();
          if(acceptPrefix.containsKey(addPrefix))
          {
            acceptPrefix.get(addPrefix).add(line.getLengthRange().toString());
          }else{
            List<String> len=new ArrayList<>();
            len.add(line.getLengthRange().toString());
            acceptPrefix.put(addPrefix,len);
          }
        }else if(line.getAction()==LineAction.REJECT){
          String addPrefix=line.getPrefix().toString();
          if(acceptPrefix.containsKey(addPrefix))
          {
            deniedPrefix.get(addPrefix).add(line.getLengthRange().toString());
          }else{
            List<String> len=new ArrayList<>();
            len.add(line.getLengthRange().toString());
            deniedPrefix.put(addPrefix,len);
          }
        }
      }

      ConvertPrefixToSymPrefix acceptConvert=new ConvertPrefixToSymPrefix(acceptPrefix);
      ConvertPrefixToSymPrefix deniedConvert=new ConvertPrefixToSymPrefix(deniedPrefix);
      _symPermittedCache=acceptConvert.convertPrefix();
      _symDeniedCache=deniedConvert.convertPrefix();
    }

    boolean accept = false;
    for (RouteFilterLine line : _lines) {
      Prefix linePrefix = line.getPrefix();
      int lineBits = linePrefix.getPrefixLength();
      Prefix truncatedLinePrefix = new Prefix(linePrefix.getStartIp(), lineBits);
      Prefix relevantPortion = new Prefix(prefix.getStartIp(), lineBits);
      if (relevantPortion.equals(truncatedLinePrefix)) {
        int prefixLength = prefix.getPrefixLength();
        SubRange range = line.getLengthRange();
        int min = range.getStart();
        int max = range.getEnd();
        if (prefixLength >= min && prefixLength <= max) {
          accept = line.getAction() == LineAction.ACCEPT;
          break;
        }
      }
    }
    if (accept) {
      _permittedCache.add(prefix);
    } else {
      _deniedCache.add(prefix);
    }
    return accept;
  }

  public boolean permits(Prefix prefix) {
    if (_deniedCache.contains(prefix)) {
      return false;
    } else if (_permittedCache.contains(prefix)) {
      return true;
    }
    return newPermits(prefix);
  }

  public void setBDDFactory(BDDFactory prefixFactory)
  {
    _prefixFactory=prefixFactory;
    return ;
  }

  public boolean symPermits(Integer prefixEcNum)
  {
    boolean accept = false;
    for (RouteFilterLine line : _lines)
    {
      HashSet<Integer> matchPEC = line.getMatchPEC();
      if (matchPEC.contains(prefixEcNum))
      {
        accept = line.getAction() == LineAction.ACCEPT;
        break;
      }
    }
    return accept;
  }

  public Result symPermitSets(BitSet pecSet)
  {
    Result result = new Result();
    for (RouteFilterLine line : _lines)
    {
      HashSet<Integer> matchPEC = line.getMatchPEC();
      BitSet mathcPECset = line.getMatchPECSet();
      BitSet tempPECSet = (BitSet) mathcPECset.clone();
      tempPECSet.and(pecSet);
      if (!tempPECSet.isEmpty())
      {
        result.setBooleanValue(line.getAction() == LineAction.ACCEPT);
        result.setMatchPECSet(tempPECSet);
        return result;
      }
    }
    return result;
  }

  // public RelatedNodeAnswer symPermits(SymPrefixList prefix)
  // {
  //   RelatedNodeAnswer answer=new RelatedNodeAnswer();
  //   answer._accept=false;
  //   answer._related=false;
  //   HashMap<String,List<String>> acceptPrefix=new HashMap<>();
  //   HashMap<String,List<String>> deniedPrefix=new HashMap<>();
  //   if(_alreadyConvert==false)
  //   {
  //     for(RouteFilterLine line:_lines)
  //     {
  //       if(line.getAction()==LineAction.ACCEPT)
  //       {
  //         String addPrefix=line.getPrefix().toString();
  //         if(acceptPrefix.containsKey(addPrefix))
  //         {
  //           acceptPrefix.get(addPrefix).add(line.getLengthRange().toString());
  //         }else{
  //           List<String> len=new ArrayList<>();
  //           len.add(line.getLengthRange().toString());
  //           acceptPrefix.put(addPrefix,len);
  //         }
  //       }else if(line.getAction()==LineAction.REJECT){
  //         String addPrefix=line.getPrefix().toString();
  //         if(acceptPrefix.containsKey(addPrefix))
  //         {
  //           deniedPrefix.get(addPrefix).add(line.getLengthRange().toString());
  //         }else{
  //           List<String> len=new ArrayList<>();
  //           len.add(line.getLengthRange().toString());
  //           deniedPrefix.put(addPrefix,len);
  //         }
  //       }
  //     }

  //     ConvertPrefixToSymPrefix acceptConvert=new ConvertPrefixToSymPrefix(acceptPrefix,_prefixFactory);
  //     ConvertPrefixToSymPrefix deniedConvert=new ConvertPrefixToSymPrefix(deniedPrefix,_prefixFactory);
  //     _symPermittedCache=acceptConvert.convertPrefix();
  //     _symDeniedCache=deniedConvert.convertPrefix();
  //   }
  //   HashMap<BDD,Long> prefixVerified= prefix.getPrefixList();
  //   for(BDD prefixIp:prefixVerified.keySet())
  //   {
  //     //判断前缀IP的BDD自己的交运算
  //     for(BDD prefixRL:_symPermittedCache.keySet())
  //     {
  //       BDD a=prefixRL.and(prefixIp);
  //       if(!prefixRL.and(prefixIp).isZero())
  //       {
  //         if((_symPermittedCache.get(prefixRL)&prefixVerified.get(prefixIp))!=0)
  //         {
  //           answer._accept=true;
  //           return answer;
  //         }
  //       }
  //     }
  //     for(BDD prefixRL:_symDeniedCache.keySet())
  //     {
  //       String lenDeny=Long.toString(_symDeniedCache.get(prefixRL),2);
  //       String lenVerified=Long.toString(prefixVerified.get(prefixIp),2);
  //       Integer maxLenDeny=lenDeny.lastIndexOf("1");
  //       Integer maxLenVerfied=lenVerified.lastIndexOf("1");
  //       if(!prefixRL.and(prefixIp).isZero()&&maxLenVerfied>maxLenDeny)
  //       {
  //         answer._related=true;
  //       }
  //       if(!prefixRL.and(prefixIp).isZero()&&(_symDeniedCache.get(prefixRL)&prefixVerified.get(prefixIp))!=0)
  //       {
  //         answer._accept=false;
  //         return answer;
  //       }
  //     }
  //   }
  //   return answer;
  // }
  /**
   * Returns the set of {@link IpWildcard ips} that match this filter list.
   *
   * @throws BatfishException if any line in this {@link RouteFilterList} does not have an {@link
   *     LineAction#ACCEPT} when matching.
   */
  @JsonIgnore
  public List<IpWildcard> getMatchingIps() {
    return getLines()
        .stream()
        .map(
            rfLine -> {
              if (rfLine.getAction() != LineAction.ACCEPT) {
                throw new BatfishException(
                    "Expected accept action for routerfilterlist from juniper");
              } else {
                return new IpWildcard(rfLine.getPrefix());
              }
            })
        .collect(Collectors.toList());
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _deniedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
    _permittedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  @JsonProperty(PROP_LINES)
  public void setLines(List<RouteFilterLine> lines) {
    _lines = lines;
  }

}
