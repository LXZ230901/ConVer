package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixRange;
import org.batfish.datamodel.PrefixSpace;
import org.batfish.datamodel.RouteFilterList;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.symwork.ConvertPrefixToSymPrefix;
import org.batfish.symwork.PrefixRelationshipGenerator;
import org.batfish.symwork.SymPrefixList;
import org.batfish.symwork.SymPrefixOp;


public class ExplicitPrefixSet extends PrefixSetExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private PrefixSpace _prefixSpace;

  public HashMap<BDD,Long> _symPrefixSpace;

  public List<Integer> _matchPECNum;

  public boolean _already;

  public BitSet _matchPECSet;

  @JsonCreator
  private ExplicitPrefixSet() {}
  public ExplicitPrefixSet(PrefixSpace prefixSpace, Configuration configuration) {
    _prefixSpace = prefixSpace;
    _symPrefixSpace=new HashMap<>();
    _already=false;
    _matchPECNum = new ArrayList<>();
    configuration.addExplicitPrefixSet(this);
    if (configuration != null)
    {
      configuration.addExplicitPrefixSet(this);
    }
    _matchPECSet = new BitSet();
  }


  public ExplicitPrefixSet(PrefixSpace prefixSpace) {
    _prefixSpace = prefixSpace;
    _symPrefixSpace=new HashMap<>();
    _already=false;
    _matchPECNum = new ArrayList<>();
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
    ExplicitPrefixSet other = (ExplicitPrefixSet) obj;
    if (_prefixSpace == null) {
      if (other._prefixSpace != null) {
        return false;
      }
    } else if (!_prefixSpace.equals(other._prefixSpace)) {
      return false;
    }
    return true;
  }

  public PrefixSpace getPrefixSpace() {
    return _prefixSpace;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_prefixSpace == null) ? 0 : _prefixSpace.hashCode());
    return result;
  }

  @Override
  public boolean matches(Prefix prefix, Environment environment) {
    boolean value = _prefixSpace.containsPrefix(prefix);
    return value;
  }

  public void toPEC()
  {
    _already = true;
    HashSet<String> relatedPrefix = new HashSet<>();
    HashMap<String,List<String>> prefixSpaceConvert=new HashMap<>();
    Object[] prefixSapce=_prefixSpace.getPrefixRanges().toArray();
    for(int i=0;i<prefixSapce.length;i++)
    {
      PrefixRange temp = (PrefixRange) prefixSapce[i];
      String prefixIp=temp.getPrefix().toString();
      relatedPrefix.add(prefixIp);
      String prefixLength=temp.getLengthRange().toString();
      if(prefixSpaceConvert.containsKey(prefixIp))
      {
        prefixSpaceConvert.get(prefixIp).add(prefixLength);
      }else{
        List<String> lengthTemp=new ArrayList<>();
        lengthTemp.add(prefixLength);
        prefixSpaceConvert.put(prefixIp,lengthTemp);
      }
    }
    ConvertPrefixToSymPrefix prefixConvert=new ConvertPrefixToSymPrefix(prefixSpaceConvert,PrefixRelationshipGenerator.getPrefixFactory());
    this._symPrefixSpace=prefixConvert.convertPrefix();
    SymPrefixList temp = new SymPrefixList(_symPrefixSpace);


    if (relatedPrefix.isEmpty())
    {
      System.out.println("Convert ExplicitPrefixSet error - relatedPrefix empty!");
    }

    for (String prefix : relatedPrefix)
    {
      if (PrefixRelationshipGenerator.getPrefixToPEC().containsKey(prefix))
      {
        for (SymPrefixList pec : PrefixRelationshipGenerator.getPrefixToPEC().get(prefix))
        {
          if (!SymPrefixOp.and(pec.getPrefixList(), _symPrefixSpace).isEmpty())
          {
            _matchPECNum.add(PrefixRelationshipGenerator.getPECToPECNum().get(pec));
          }
        }
      } else {
        System.out.println("Convert ExplicitPrefixSet error - do not have match prefix ip!");
      }
    }



    for (Integer matchPEC : _matchPECNum)
    {
      _matchPECSet.set(matchPEC);
    }
  }

  public boolean matches(Environment environment) {
    boolean accept=false;
//    if(!_already)
//    {
//        HashSet<String> relatedPrefix = new HashSet<>();
//        HashMap<String,List<String>> prefixSpaceConvert=new HashMap<>();
//        Object[] prefixSapce=_prefixSpace.getPrefixRanges().toArray();
//        for(int i=0;i<prefixSapce.length;i++)
//        {
//            PrefixRange temp = (PrefixRange) prefixSapce[i];
//            String prefixIp=temp.getPrefix().toString();
//            relatedPrefix.add(prefixIp);
//            String prefixLength=temp.getLengthRange().toString();
//            if(prefixSpaceConvert.containsKey(prefixIp))
//            {
//              prefixSpaceConvert.get(prefixIp).add(prefixLength);
//            }else{
//              List<String> lengthTemp=new ArrayList<>();
//              lengthTemp.add(prefixLength);
//              prefixSpaceConvert.put(prefixIp,lengthTemp);
//            }
//        }
//        ConvertPrefixToSymPrefix prefixConvert=new ConvertPrefixToSymPrefix(prefixSpaceConvert,environment._symBgpOriginalRoute.getPrefixFactory());
//        this._symPrefixSpace=prefixConvert.convertPrefix();
//        SymPrefixList temp = new SymPrefixList(_symPrefixSpace);
//
//
//        if (relatedPrefix.isEmpty())
//        {
//          System.out.println("Convert ExplicitPrefixSet error - relatedPrefix empty!");
//        }
//
//        for (String prefix : relatedPrefix)
//        {
//            if (PrefixRelationshipGenerator.getPrefixToPEC().containsKey(prefix))
//            {
//              for (SymPrefixList pec : PrefixRelationshipGenerator.getPrefixToPEC().get(prefix))
//              {
//                if (!SymPrefixOp.and(pec.getPrefixList(), _symPrefixSpace).isEmpty())
//                {
//                  _matchPECNum.add(PrefixRelationshipGenerator.getPECToPECNum().get(pec));
//                }
//              }
//            } else {
//              System.out.println("Convert ExplicitPrefixSet error - do not have match prefix ip!");
//            }
//        }
//        _already=true;
//    }

//    if(_matchPECNum.contains(environment.getSymBgpOutputRoute().getPrefixEcNum())) {
//      accept = true;
//    }
    System.out.println("ExplicitPrefixSet match error!");
    return accept;
  }

  @Override
  public Result symMatches(Environment environment)
  {
    Result result = new Result();
    BitSet tempAndBit = (BitSet) _matchPECSet.clone();
    tempAndBit.and(environment.getSymBgpOutputRoute().getPrefixEcSet());
    if (!tempAndBit.isEmpty())
    {
      result.setBooleanValue(true);
      result.setMatchPECSet(tempAndBit);
      return result;
    }
    return result;

    //    Result result = new Result();
    //    RouteFilterList list = environment.getConfiguration().getRouteFilterLists().get(_name);
    //    if (list!=null)
    //    {
    //      //RelatedNodeAnswer answer=list.symPermits(prefix);
    //      boolean answer=list.symPermits(environment.getSymBgpOutputRoute().getPrefixEcNum());
    //      return answer;
    //    }else{
    //      environment.setError(true);
    //      return false;
    //    }
    //    return result;
  }

  public void setPrefixSpace(PrefixSpace prefixSpace) {
    _prefixSpace = prefixSpace;
  }

//  public void convert(BDDFactory prefixFactory)
//  {
//    if(!_already)
//    {
//      HashMap<String,List<String>> prefixSpaceConvert=new HashMap<>();
//      Object[] prefixSapce=_prefixSpace.getPrefixRanges().toArray();
//      for(int i=0;i<prefixSapce.length;i++)
//      {
//        PrefixRange temp = (PrefixRange) prefixSapce[i];
//        String prefixIp=temp.getPrefix().toString();
//        String prefixLength=temp.getLengthRange().toString();
//        if(prefixSpaceConvert.containsKey(prefixIp))
//        {
//          prefixSpaceConvert.get(prefixIp).add(prefixLength);
//        }else{
//          List<String> lengthTemp=new ArrayList<>();
//          lengthTemp.add(prefixLength);
//          prefixSpaceConvert.put(prefixIp,lengthTemp);
//        }
//      }
//      ConvertPrefixToSymPrefix prefixConvert=new ConvertPrefixToSymPrefix(prefixSpaceConvert,prefixFactory);
//      this._symPrefixSpace=prefixConvert.convertPrefix();
//      SymPrefixList temp=new SymPrefixList(_symPrefixSpace);
//      this._prefixEcNum=ConvertPrefixToSymPrefix._prefixEc.get(temp);
//      _already=true;
//    }
//  }
}
