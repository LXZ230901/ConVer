package org.batfish.symwork;
import java.util.HashMap;
import net.sf.javabdd.BDD;

public class SymPrefixOp {
  public static HashMap<BDD,Long> and(HashMap<BDD,Long> prefix1,HashMap<BDD,Long> prefix2)
  {
    HashMap<BDD,Long> answer=new HashMap<>();//记忆下
    for(BDD pre1:prefix1.keySet())
    {
      for(BDD pre2:prefix2.keySet())
      {
        BDD tmpIp=pre1.and(pre2);
        Long tmpLen=prefix1.get(pre1)&prefix2.get(pre2);
        if(!tmpIp.isZero()&&tmpLen!=0)
        {
          if(answer.containsKey(tmpIp))
          {
            answer.put(tmpIp,answer.get(tmpIp)|tmpLen);
          }else{
            answer.put(tmpIp,tmpLen);
          }
        }
      }
    }
    return answer;
  }

  public static HashMap<BDD,Long> atomNot(BDD Ip,Long length)
  {
    HashMap<BDD,Long> answer=new HashMap<>();
    if(String.format("%32s",Long.toString(length,2)).replace(" ","0").equals("11111111111111111111111111111111"))
    {
      answer.put(Ip.not(),Long.parseLong("11111111111111111111111111111111",2));
    }else{
      answer.put(Ip.not(),Long.parseLong("11111111111111111111111111111111",2));
      Long temp=length^Long.parseLong("11111111111111111111111111111111",2);
      //String notString=Long.toString(temp,2);
      //System.out.println(notString);
      //System.out.println(Long.toString(temp,2).substring(notString.length()-32,notString.length()));
      answer.put(Ip,temp);
    }
    return answer;
  }

  public static HashMap<BDD,Long> not(HashMap<BDD,Long> prefix)
  {
    HashMap<BDD,Long> answer=new HashMap<>();
    for(BDD temp:prefix.keySet())
    {
      answer.put(temp.getFactory().one(),Long.parseLong("11111111111111111111111111111111",2));
      break;
    }
    for(BDD Ip:prefix.keySet())
    {
      answer=SymPrefixOp.and(answer,atomNot(Ip, prefix.get(Ip)));
    }
    return answer;
  }
}
