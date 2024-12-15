package org.batfish.symwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

;

//将给定形式的前缀<prefix:LenRange>转换成符号前缀形式
public class ConvertPrefixToSymPrefix {
  private HashMap<String, List<String>> _prefixList;
  private BDDFactory factory;
  public static Map<SymPrefixList,Integer> _prefixEc;
  public ConvertPrefixToSymPrefix(HashMap<String,List<String>> prefixList)
  {
    _prefixList=prefixList;
  }
  public ConvertPrefixToSymPrefix(HashMap<String,List<String>> prefixList,BDDFactory prefixFactory)
  {
    _prefixList=prefixList;
    factory=prefixFactory;
  }
  public HashMap<BDD, Long> convertPrefix()
  {
    HashMap<BDD,Long> answer=new HashMap<>();
    List<SymPrefixList> finalAnswer=new ArrayList<>();

    for(String Ip:_prefixList.keySet())
    {
      if (Ip.equals("0.0.0.0/0"))
      {
        BDD prefix = factory.one();
        long mask = 0xFFFFFFFFL;
        answer.put(prefix, mask);
        continue;
      }
      System.out.println(Ip.substring(Ip.indexOf("/")+1,Ip.length()));
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

      List<String> len=_prefixList.get(Ip);
      String addLen="11111111111111111111111111111111";
      SortedSet<Integer> lenDevided=new TreeSet<>();
      SortedSet<Integer> singleLen=new TreeSet<>();
      StringBuilder prefixLen=new StringBuilder("00000000000000000000000000000000");
      for(int i=0;i<len.size();i++)
      {
        String tempLen=len.get(i).substring(0,len.get(i).length()-1);
        tempLen=tempLen.substring(1,tempLen.length());
        String tempLenList[]=tempLen.split(",");
        Integer lower=Integer.parseInt(tempLenList[0]);
        Integer upper=Integer.parseInt(tempLenList[1]);
        prefixLen.replace(lower-1,upper,addLen.substring(lower-1,upper));
      }
      Long prefixActualLen= Long.parseLong(prefixLen.toString(),2);
      if(answer.containsKey(prefixActualLen))
      {
        answer.put(prefixActualIp,answer.get(prefixActualIp)|prefixActualLen);
      }else{
        answer.put(prefixActualIp,prefixActualLen);
      }
    }
    return answer;
  }
}
