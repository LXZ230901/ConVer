package org.batfish.symwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;


import net.sf.javabdd.BDD;

public class SymPrefixList {
  public HashMap<BDD, Long> _prefixList;
  public HashSet<String> _relatedPrefix;

  public SymPrefixList() {
      _prefixList = new HashMap<>();
      _relatedPrefix = new HashSet<>();
  }
  public SymPrefixList(HashMap<BDD,Long> prefixList)
  {
    _prefixList=prefixList;
    _relatedPrefix = new HashSet<>();
  }
  public SymPrefixList(BDD prefix,Long prefixMask)
  {
    _prefixList=new HashMap<>();
    _prefixList.put(prefix,prefixMask);
    _relatedPrefix = new HashSet<>();
  }
//  public SymPrefixList(String prefix)
//  {
//    Integer prefixIpLen=Integer.parseInt(prefix.substring(prefix.indexOf("/")+1,prefix.length()));
//    HashMap<String,List<String>> prefixTemp=new HashMap<>();
//    prefixTemp.put(prefix,new ArrayList<>());
//    prefixTemp.get(prefix).add("["+prefixIpLen.toString()+","+prefixIpLen.toString()+"]");
//    ConvertPrefixToPrefixEC convertPrefixToSymPrefix=new ConvertPrefixToPrefixEC(prefixTemp);
//    List<SymPrefixList> an=convertPrefixToSymPrefix.convertPrefix();
//    System.out.println("--------------------PrefixECConvert:------------------");
//    System.out.println("prefix:"+prefix);
//    for(int i=0;i<an.size();i++)
//    {
//      an.get(i)._prefixList
//    }
//    this._prefixList=convertPrefixToSymPrefix.convertPrefix().get(0).getPrefixList();
//  }
  public HashMap<BDD,Long> getPrefixList()
  {
    return _prefixList;
  }


  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SymPrefixList)) {
      return false;
    }
    SymPrefixList other = (SymPrefixList) o;
    return Objects.equals(_prefixList, other._prefixList);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(_prefixList);
  }

//  public boolean equals(Object o)
//  {
//    boolean answer=true;
//    return answer;
//  }
//  public final int hashCode()
//  {
//    return 1;
//  }

  public HashSet<String> getRelatedPrefix()
  {
    return _relatedPrefix;
  }

  public void addRelatedPrefix(String prefix)
  {
    _relatedPrefix.add(prefix);
  }
}
