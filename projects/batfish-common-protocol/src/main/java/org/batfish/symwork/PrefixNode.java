package org.batfish.symwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class PrefixNode {
  public String _prefix;
  public Set<PrefixNode> _subPrefix;
  public PrefixLengthUnit _prefixLengthUnit;
  public PrefixNode(PrefixLengthUnit prefixLengthUnit)
  {
    _prefix = prefixLengthUnit.getPrefix();
    _prefixLengthUnit = prefixLengthUnit;
    _subPrefix = new HashSet<>();
  }
  public void addSubPrefix(PrefixLengthUnit subPrefixUnit)
  {
    boolean allAdd = false;
    for (PrefixNode prefixNode : _subPrefix)
    {
      String nodePrefix = prefixNode.getPrefix();
      if (PrefixRelationshipGenerator.isSubPrefix(subPrefixUnit.getPrefix(), nodePrefix))
      {
        prefixNode.addSubPrefix(subPrefixUnit);
        allAdd = true;
        break;
      }
    }
    if (!allAdd)
    {
      _subPrefix.add(new PrefixNode(subPrefixUnit));
    }
  }

  public String getPrefix()
  {
    return this._prefix;
  }

  public Set<PrefixNode> getSubPrefix()
  {
    return this._subPrefix;
  }

  public PrefixLengthUnit getPrefixLengthUnit()
  {
    return this._prefixLengthUnit;
  }

  public Set<SymPrefixList> computePEC(BDDFactory factory)
  {
    Set<SymPrefixList> PEC = new HashSet<>();
    Set<SymPrefixList> tempPEC = new HashSet<>();
    for (PrefixNode subPrefix : _subPrefix)
    {
      tempPEC.addAll(subPrefix.computePEC(factory));
    }



    String Ip = _prefixLengthUnit.getPrefix();
    Set<SymPrefixList> thisPEC = new HashSet<>();
    if (Ip.equals("0.0.0.0/0"))
    {
      BDD prefix = factory.one();
      long mask = 0xFFFFFFFFL;
      SymPrefixList symPrefixList = new SymPrefixList(prefix,mask);
      symPrefixList.addRelatedPrefix(Ip);
      thisPEC.add(symPrefixList);
      PEC.addAll(thisPEC);
      PEC.addAll(tempPEC);
      return PEC;
    }
    int prefixIpLen=Integer.parseInt(Ip.substring(Ip.indexOf("/")+1,Ip.length()));
    String prefixIp[]=Ip.substring(0,Ip.indexOf("/")).split("\\.");
    String targetIp="";
    for(int i=0;i<prefixIp.length;i++)
    {
      Integer subIp=Integer.parseInt(prefixIp[i]);
      targetIp=targetIp+String.format("%8s",Integer.toBinaryString(subIp)).replace(" ","0");
    }
    BDD prefixActualIp=factory.one();
    for(int i=0;i<prefixIpLen;i++)
    {
      if(targetIp.charAt(i)=='1')
      {
        prefixActualIp = prefixActualIp.and( factory.ithVar(i) );
      }else{
        prefixActualIp = prefixActualIp.and( factory.nithVar(i) );
      }
    }
    List<String> len = new ArrayList<>(_prefixLengthUnit.getLength());
    String addLen="11111111111111111111111111111111";
    SortedSet<Integer> lenDevided=new TreeSet<>();
    SortedSet<Integer> singleLen=new TreeSet<>();
    for(int i=0;i<len.size();i++)
    {
      String tempLen=len.get(i).substring(0,len.get(i).length()-1);
      tempLen=tempLen.substring(1,tempLen.length());
      String tempLenList[]=tempLen.split(",");
      Integer lower=Integer.parseInt(tempLenList[0]);
      Integer upper=Integer.parseInt(tempLenList[1]);
      if(lower!=upper)
      {
        lenDevided.add(lower);
        lenDevided.add(upper);
      }else{
        singleLen.add(upper);
      }
    }
    Object[] lenDevidedArray = lenDevided.toArray();
    Object[] singleLenArray = singleLen.toArray();
    for(int i=0;i<lenDevidedArray.length-1;i++)
    {
      StringBuilder prefixLen=new StringBuilder("00000000000000000000000000000000");
      Integer lower=(Integer) lenDevidedArray[i];
      Integer upper=(Integer) lenDevidedArray[i+1];
      if (lower==0)
      {
        prefixLen.replace(lower,upper,addLen.substring(lower,upper));
      }else{
        prefixLen.replace(lower-1,upper,addLen.substring(lower-1,upper));
      }
      if(singleLenArray.length==0||((Integer) singleLenArray[singleLenArray.length-1]<lower)||((Integer) singleLenArray[0]>upper))
      {

      }else{
        for (int j=0;j<singleLenArray.length;j++)
        {
          if((Integer) singleLenArray[i]>upper)
          {
            continue;
          }else{
            if ((Integer) singleLenArray[i] == 0)
            {
              System.out.println("break");
            }
            prefixLen.replace((Integer) singleLenArray[i]-1,(Integer) singleLenArray[i],"0");
          }
        }
      }
      Long prefixActualLen= Long.parseLong(prefixLen.toString(),2);
      SymPrefixList symPrefixList = new SymPrefixList(prefixActualIp,prefixActualLen);
      symPrefixList.addRelatedPrefix(Ip);
      thisPEC.add(symPrefixList);
    }
    for(int i=0;i<singleLenArray.length;i++)
    {
      StringBuilder prefixLen=new StringBuilder("00000000000000000000000000000000");
      prefixLen.replace((Integer) singleLenArray[i]-1,(Integer) singleLenArray[i],"1");
      Long prefixActualLen=Long.parseLong(prefixLen.toString(),2);
      thisPEC.add(new SymPrefixList(prefixActualIp,prefixActualLen));
    }

    PEC.addAll(tempPEC);
    for (SymPrefixList outPrefixList : thisPEC)
    {
      HashMap<BDD, Long> newPEC = new HashMap<>();
      HashSet<String> relatedPrefix = new HashSet<>();
      newPEC.putAll(outPrefixList.getPrefixList());
      List<SymPrefixList> addPEC=new ArrayList<>();
      for (SymPrefixList inPrefixList : PEC)
      {
        HashMap<BDD, Long> andTemp = SymPrefixOp.and(newPEC, inPrefixList.getPrefixList());
        if (andTemp.size() != 0)
        {
          HashMap<BDD, Long> notAndTemp = SymPrefixOp.not(andTemp);
          HashMap<BDD, Long> remainPEC = SymPrefixOp.and(notAndTemp, inPrefixList.getPrefixList());
          if (remainPEC.size() != 0)
          {
            SymPrefixList symPrefixList = new SymPrefixList(remainPEC);
            symPrefixList.getRelatedPrefix().addAll(inPrefixList.getRelatedPrefix());
            addPEC.add(symPrefixList);
          }
          inPrefixList._prefixList = andTemp;
          newPEC = SymPrefixOp.and(notAndTemp, newPEC);
          relatedPrefix.addAll(inPrefixList.getRelatedPrefix());
          if (newPEC.isEmpty())
          {
            break;
          }
        }
      }
      PEC.addAll(addPEC);
      if (!newPEC.isEmpty())
      {
        SymPrefixList symPrefixList = new SymPrefixList(newPEC);
        symPrefixList.addRelatedPrefix(Ip);
        symPrefixList.getRelatedPrefix().addAll(relatedPrefix);
        PEC.add(symPrefixList);
      }
    }
    return PEC;
  }
}
