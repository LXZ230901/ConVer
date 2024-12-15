package org.batfish.symwork;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class SingleConvertPrefixToSymPrefix {
  public static SymPrefixList convert(String Ip, BDDFactory factory)
  {
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
    String addLen="11111111111111111111111111111111";
    StringBuilder prefixLen=new StringBuilder("00000000000000000000000000000000");
    prefixLen.replace(prefixIpLen-1,prefixIpLen,addLen.substring(prefixIpLen-1,prefixIpLen));
    Long prefixActualLen= Long.parseLong(prefixLen.toString(),2);
    SymPrefixList answer=new SymPrefixList(prefixActualIp,prefixActualLen);
    return answer;
  }
}
