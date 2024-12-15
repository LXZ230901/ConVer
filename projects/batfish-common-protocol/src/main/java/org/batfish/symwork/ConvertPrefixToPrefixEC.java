package org.batfish.symwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

//public class ConvertPrefixToPrefixEC {
//  private HashMap<String, List<String>> _prefixList;
//  private BDDFactory _factory;
//
//  public ConvertPrefixToPrefixEC(HashMap<String, List<String>> prefixList,
//      BDDFactory prefixFactory) {
//    _prefixList = prefixList;
//    _factory = prefixFactory;
//  }
//
//  public List<SymPrefixList> convertPrefix(BDDFactory factory) {
//    HashSet<SymPrefix> answer = new HashSet<>();
//    List<SymPrefixList> finalAnswer = new ArrayList<>();
//
//    for (String Ip : _prefixList.keySet()) {
//      int prefixIpLen = Integer.parseInt(Ip.substring(Ip.indexOf("/") + 1, Ip.length()));
//      String prefixIp[] = Ip.substring(0, Ip.indexOf("/")).split("\\.");
//      String targetIp = "";
//      for (int i = 0; i < prefixIp.length; i++) {
//        Integer subIp = Integer.parseInt(prefixIp[i]);
//        targetIp = targetIp + String.format("%8s", Integer.toBinaryString(subIp)).replace(" ", "0");
//      }
//
//      BDD prefixActualIp = factory.one();
//      for (int i = 0; i < prefixIpLen; i++) {
//        if (targetIp.charAt(i) == '1') {
//          prefixActualIp = prefixActualIp.and(factory.ithVar(i));
//        } else {
//          prefixActualIp = prefixActualIp.and(factory.nithVar(i));
//        }
//      }
//
//      List<String> len = _prefixList.get(Ip);
//      String addLen = "11111111111111111111111111111111";
//      SortedSet<Integer> lenDevided = new TreeSet<>();
//      SortedSet<Integer> singleLen = new TreeSet<>();
//      for (int i = 0; i < len.size(); i++) {
//        String tempLen = len.get(i).substring(0, len.get(i).length() - 1);
//        tempLen = tempLen.substring(1, tempLen.length());
//        String tempLenList[] = tempLen.split(",");
//        Integer lower = Integer.parseInt(tempLenList[0]);
//        Integer upper = Integer.parseInt(tempLenList[1]);
//        if (lower != upper) {
//          lenDevided.add(lower);
//          lenDevided.add(upper);
//        } else {
//          singleLen.add(upper);
//        }
//      }
//      Object[] lenDevidedArray = lenDevided.toArray();
//      Object[] singleLenArray = singleLen.toArray();
//      for (int i = 0; i < lenDevidedArray.length - 1; i++) {
//        StringBuilder prefixLen = new StringBuilder("00000000000000000000000000000000");
//        Integer lower = (Integer) lenDevidedArray[i];
//        Integer upper = (Integer) lenDevidedArray[i + 1];
//        if (lower == 0) {
//          prefixLen.replace(lower, upper, addLen.substring(lower, upper));
//        } else {
//          prefixLen.replace(lower - 1, upper, addLen.substring(lower - 1, upper));
//        }
//        if (singleLenArray.length == 0 || ((Integer) singleLenArray[singleLenArray.length - 1]
//            < lower) || ((Integer) singleLenArray[0] > upper)) {
//
//        } else {
//          for (int j = 0; j < singleLenArray.length; j++) {
//            if ((Integer) singleLenArray[i] > upper) {
//              continue;
//            } else {
//              prefixLen.replace((Integer) singleLenArray[i] - 1, (Integer) singleLenArray[i], "0");
//            }
//          }
//        }
//        Long prefixActualLen = Long.parseLong(prefixLen.toString(), 2);
//        answer.add(new SymPrefix(factory, prefixActualIp, prefixActualLen));
//      }
//      for (int i = 0; i < singleLenArray.length; i++) {
//        StringBuilder prefixLen = new StringBuilder("00000000000000000000000000000000");
//        prefixLen.replace((Integer) singleLenArray[i] - 1, (Integer) singleLenArray[i], "1");
//        Long prefixActualLen = Long.parseLong(prefixLen.toString(), 2);
//        answer.add(new SymPrefix(factory, prefixActualIp, prefixActualLen));
//      }
//    }
//
//    for (SymPrefix symInAnswer : answer) {
//      HashMap<BDD, Long> temp = new HashMap<>();
//      temp.put(symInAnswer.getIp(), symInAnswer.getLength());
//      List<SymPrefixList> answerTemp = new ArrayList<>();
//      for (int j = 0; j < finalAnswer.size(); j++) {
//        HashMap<BDD, Long> andTemp = SymPrefixOp.and(temp, finalAnswer.get(j).getPrefixList());
//        if (andTemp.size() != 0) {
//          HashMap<BDD, Long> subAdd = SymPrefixOp.and(SymPrefixOp.not(andTemp), finalAnswer.get(j).getPrefixList());
//          if (subAdd.size() != 0) {
//            answerTemp.add(new SymPrefixList(subAdd));
//          }
//          finalAnswer.get(j)._prefixList = andTemp;
//          temp = SymPrefixOp.and(SymPrefixOp.not(andTemp), temp);
//        }
//        if (temp.size() == 0) {
//          break;
//        }
//      }
//      finalAnswer.addAll(answerTemp);
//      if (temp.size() != 0) {
//        finalAnswer.add(new SymPrefixList(temp));
//      }
//    }
//    //    for(int i=0;i<answerTemp.length;i++)
//    //    {
//    //      HashMap<BDD,Long> temp=new HashMap<>();
//    //      SymPrefix symInAnswer=(SymPrefix) answerTemp[i];
//    //      temp.put(symInAnswer.getIp(),symInAnswer.getLength());
//    //      for(int j=0;j<finalAnswer.size();j++)
//    //      {
//    //        temp=SymPrefixOp.and(temp,SymPrefixOp.not(finalAnswer.get(j).getPrefixList()));
//    //      }
//    //      if(temp.size()!=0)
//    //      {
//    //        finalAnswer.add(new SymPrefixList(temp));
//    //      }
//    //    }
//
//    HashMap<BDD, Long> anyValue = new HashMap<>();
//    anyValue.put(factory.one(), Long.parseLong("11111111111111111111111111111111", 2));
//    for (int j = 0; j < finalAnswer.size(); j++) {
//      anyValue = SymPrefixOp.and(anyValue, SymPrefixOp.not(finalAnswer.get(j).getPrefixList()));
//    }
//    if (anyValue.size() != 0) {
//      finalAnswer.add(new SymPrefixList(anyValue));
//    }
//    return finalAnswer;
//  }
//}
public class ConvertPrefixToPrefixEC {
  private HashMap<String, List<String>> _prefixList;
  private BDDFactory _factory;
  public ConvertPrefixToPrefixEC(HashMap<String,List<String>> prefixList,BDDFactory prefixFactory)
  {
    _prefixList=prefixList;
    _factory=prefixFactory;
  }
  public List<SymPrefixList> convertPrefix(BDDFactory factory)
  {
    HashSet<SymPrefix> answer=new HashSet<>();
    List<SymPrefixList> finalAnswer=new ArrayList<>();

    for(String Ip:_prefixList.keySet())
    {
      if (Ip.equals("0.0.0.0/0"))
      {
        BDD prefix = factory.zero();
        answer.add(new SymPrefix(factory,prefix,Long.parseLong("00000000000000000000000000000000")));
        continue;
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

      List<String> len=_prefixList.get(Ip);
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
        answer.add(new SymPrefix(factory,prefixActualIp,prefixActualLen));
      }
      for(int i=0;i<singleLenArray.length;i++)
      {
        StringBuilder prefixLen=new StringBuilder("00000000000000000000000000000000");
        prefixLen.replace((Integer) singleLenArray[i]-1,(Integer) singleLenArray[i],"1");
        Long prefixActualLen=Long.parseLong(prefixLen.toString(),2);
        answer.add(new SymPrefix(factory,prefixActualIp,prefixActualLen));
      }
    }

    for(SymPrefix symInAnswer:answer)
    {
      HashMap<BDD,Long> temp=new HashMap<>();
      temp.put(symInAnswer.getIp(),symInAnswer.getLength());
      List<SymPrefixList> answerTemp=new ArrayList<>();
      for(int j=0;j<finalAnswer.size();j++)
      {
        HashMap<BDD,Long> andTemp=SymPrefixOp.and(temp,finalAnswer.get(j).getPrefixList());
        if(andTemp.size()!=0)
        {
          HashMap<BDD,Long> subAdd=SymPrefixOp.and(SymPrefixOp.not(andTemp),finalAnswer.get(j).getPrefixList());
          if(subAdd.size()!=0)
          {
            answerTemp.add(new SymPrefixList(subAdd));
          }
          finalAnswer.get(j)._prefixList=andTemp;
          temp=SymPrefixOp.and(SymPrefixOp.not(andTemp),temp);
        }
        if(temp.size()==0)
        {
          break;
        }
      }
      finalAnswer.addAll(answerTemp);
      if(temp.size()!=0)
      {
        finalAnswer.add(new SymPrefixList(temp));
      }
    }
    //    for(int i=0;i<answerTemp.length;i++)
    //    {
    //      HashMap<BDD,Long> temp=new HashMap<>();
    //      SymPrefix symInAnswer=(SymPrefix) answerTemp[i];
    //      temp.put(symInAnswer.getIp(),symInAnswer.getLength());
    //      for(int j=0;j<finalAnswer.size();j++)
    //      {
    //        temp=SymPrefixOp.and(temp,SymPrefixOp.not(finalAnswer.get(j).getPrefixList()));
    //      }
    //      if(temp.size()!=0)
    //      {
    //        finalAnswer.add(new SymPrefixList(temp));
    //      }
    //    }

    HashMap<BDD,Long> anyValue=new HashMap<>();
    anyValue.put(factory.one(),Long.parseLong("11111111111111111111111111111111",2));
    for(int j=0;j<finalAnswer.size();j++)
    {
      anyValue=SymPrefixOp.and(anyValue,SymPrefixOp.not(finalAnswer.get(j).getPrefixList()));
    }
    if(anyValue.size()!=0)
    {
      finalAnswer.add(new SymPrefixList(anyValue));
    }
    return finalAnswer;
  }

  
}

