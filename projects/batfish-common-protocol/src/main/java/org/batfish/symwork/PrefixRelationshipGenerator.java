package org.batfish.symwork;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class PrefixRelationshipGenerator {
  public List<PrefixNode> _prefixTrie;
  public static HashMap<String, HashSet<SymPrefixList>> _prefixToPEC;
  public static HashMap<SymPrefixList, Integer> _PECToPECNum;
  public static BDDFactory _prefixFactory;
  public PrefixRelationshipGenerator()
  {
    _prefixTrie = new ArrayList<>();
  }

  public static void setPrefixToPEC(HashMap<String, HashSet<SymPrefixList>> prefixToPEC)
  {
    _prefixToPEC = prefixToPEC;
  }

  public static void setPECToPECNum(HashMap<SymPrefixList, Integer> PECToPECNum)
  {
    _PECToPECNum = PECToPECNum;
  }

  public static void setPrefixFactory(BDDFactory prefixFactory)
  {
    _prefixFactory = prefixFactory;
  }

  public static BDDFactory getPrefixFactory()
  {
    return _prefixFactory;
  }

  public static HashMap<String, HashSet<SymPrefixList>> getPrefixToPEC()
  {
    return _prefixToPEC;
  }

  public static HashMap<SymPrefixList, Integer> getPECToPECNum()
  {
    return _PECToPECNum;
  }

  public Set<SymPrefixList> computePEC(BDDFactory factory)
  {
    Set<SymPrefixList> PEC = new HashSet<>();
    for (PrefixNode prefixNode : _prefixTrie)
    {
      PEC.addAll(prefixNode.computePEC(factory));
    }
    BDD prefix = factory.one();
    long mask = 0xFFFFFFFFL;
    SymPrefixList allPrefixList = new SymPrefixList(prefix,mask);
    Set<SymPrefixList> thisPEC = new HashSet<>();
    thisPEC.add(allPrefixList);
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
        symPrefixList.getRelatedPrefix().addAll(relatedPrefix);
        PEC.add(symPrefixList);
      }
    }
    return PEC;
  }

  public void addPrefix(HashSet<PrefixLengthUnit> allPrefix)
  {
    for (PrefixLengthUnit prefixLengthUnit : allPrefix)
    {
      boolean allAdd = false;
      for (PrefixNode prefixNode : _prefixTrie)
      {
        String nodePrefix = prefixNode.getPrefix();
        if (isSubPrefix(prefixLengthUnit.getPrefix(), nodePrefix))
        {
          prefixNode.addSubPrefix(prefixLengthUnit);
          allAdd = true;
          break;
        }
      }
      if (!allAdd)
      {
        _prefixTrie.add(new PrefixNode(prefixLengthUnit));
      }
    }
    System.out.println("allReady!");
  }

  // 将 IPv4 地址转换为 32 位二进制字符串
  private static String convertToBinary(String ipAddress) {
    StringBuilder binaryIP = new StringBuilder();
    String[] octets = ipAddress.split("\\.");
    for (String octet : octets) {
      int intVal = Integer.parseInt(octet);
      String binaryOctet = String.format("%8s", Integer.toBinaryString(intVal)).replace(' ', '0');
      binaryIP.append(binaryOctet);
    }
    return binaryIP.toString();
  }

  // 判断 prefixA 是否是 prefixB 的子前缀
  public static boolean isSubPrefix(String prefixA, String prefixB) {
    // 将前缀分为 IP 地址和前缀长度
    String[] partsA = prefixA.split("/");
    String ipA = partsA[0];
    int prefixLengthA = Integer.parseInt(partsA[1]);

    String[] partsB = prefixB.split("/");
    String ipB = partsB[0];
    int prefixLengthB = Integer.parseInt(partsB[1]);

    // prefixA 的长度需要大于等于 prefixB 才有可能是子前缀
    if (prefixLengthA < prefixLengthB) {
      return false;
    }

    // 转换为二进制并比较前缀部分
    String binaryA = convertToBinary(ipA).substring(0, prefixLengthB);
    String binaryB = convertToBinary(ipB).substring(0, prefixLengthB);

    return binaryA.equals(binaryB);
  }
}
