package org.batfish.extwork;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class ForwardingNode {
  private HashSet<ForwardingPacket> _forwardingPacket;
  private HashSet<ForwardingPacket> _receivePacket;

  private HashMap<BitSet, HashMap<String, Integer>> _fib;
  private String _nodeName;

  private HashMap<ForwardingPacket, HashMap<String, Integer>> _matchForwardingPacket;
  private HashSet<ForwardingPacket> _tempForwardingPacket;
  private HashSet<ForwardingPacket> _loopForwardingPacket;
  public ForwardingNode(String nodeName, HashMap<BitSet, HashMap<String, Integer>> fib)
  {
    _nodeName = nodeName;
    _forwardingPacket = new HashSet<>();
    _receivePacket = new HashSet<>();
    _fib = fib;
    _matchForwardingPacket = new HashMap<>();
    _tempForwardingPacket = new HashSet<>();
    _loopForwardingPacket = new HashSet<>();
  }

  public void addLoopForwardingPacket(ForwardingPacket forwardingPacket)
  {
    _loopForwardingPacket.add(forwardingPacket);
  }

  public HashSet<ForwardingPacket> getLoopForwardingPacket()
  {
    return _loopForwardingPacket;
  }

  public void addTempForwardingPacket(ForwardingPacket forwardingPacket)
  {
    _tempForwardingPacket.add(forwardingPacket);
  }

  public HashSet<ForwardingPacket> getTempForwardingPacket()
  {
    return _tempForwardingPacket;
  }

  public HashSet<ForwardingPacket> getForwardingPacket()
  {
    return _forwardingPacket;
  }

  public HashSet<ForwardingPacket> getReceivePacket()
  {
    return _receivePacket;
  }

  public HashMap<BitSet, HashMap<String, Integer>> getFib()
  {
    return _fib;
  }

  public void addForwardingPacket(ForwardingPacket forwardingPacket)
  {
    _forwardingPacket.add(forwardingPacket);
  }

  public void addReceivePacket(ForwardingPacket receivePacket)
  {
    _receivePacket.add(receivePacket);
  }

  public String getNodeName()
  {
    return _nodeName;
  }

  public void matchForwardingPacket()
  {

//    HashMap<ForwardingPacket, HashMap<String, Integer>> matchPacket = new HashMap<>();
//    for (ForwardingPacket forwardingPacket : _forwardingPacket)
//    {
//      for (BitSet prefix : _fib.keySet())
//      {
//        BitSet andPrefix = (BitSet) prefix.clone();
//        andPrefix.and(forwardingPacket.getPrefix());
//        if (!andPrefix.isEmpty())
//        {
//          ForwardingPacket tempPacket = forwardingPacket.toBuilder().build();
//          tempPacket.setPrefix(andPrefix);
//          matchPacket.put(tempPacket, _fib.get(prefix));
//        }
//      }
//    }
    _matchForwardingPacket.clear();
    // 使用ConcurrentHashMap来支持线程安全的写操作
    ConcurrentHashMap<ForwardingPacket, HashMap<String, Integer>> concurrentMatchPacket = new ConcurrentHashMap<>();
    // 使用同步集合来保证线程安全
    Set<ForwardingPacket> receivePacket = Collections.synchronizedSet(new HashSet<>());

    // 将外层循环改为parallelStream()
    _forwardingPacket.parallelStream().forEach(forwardingPacket -> {
      BitSet matchPrefix = new BitSet();
      for (BitSet prefix : _fib.keySet()) {
        BitSet andPrefix = (BitSet) prefix.clone();
        andPrefix.and(forwardingPacket.getPrefix());
        if (!andPrefix.isEmpty()) {
          matchPrefix.or(prefix);
          ForwardingPacket tempPacket = forwardingPacket.toBuilder().build();
          tempPacket.setPrefix(andPrefix);
          // 这里的put操作是线程安全的
          concurrentMatchPacket.put(tempPacket, _fib.get(prefix));
        }
      }

      BitSet remainPrefix = (BitSet) forwardingPacket.getPrefix().clone(); // 使用clone来避免并发修改
      remainPrefix.andNot(matchPrefix);

      if (!remainPrefix.isEmpty()) {
        ForwardingPacket tempPacket = forwardingPacket.toBuilder().build();
        tempPacket.setPrefix(remainPrefix);
        // 这里的add操作在同步集合中是线程安全的
        receivePacket.add(tempPacket);
      }
    });

    // 将结果收集到原来的matchPacket中
    _matchForwardingPacket.putAll(concurrentMatchPacket);
    _receivePacket.addAll(receivePacket);
    _forwardingPacket.clear();
  }

  public HashMap<ForwardingPacket, HashMap<String, Integer>> getMatchForwardingPacket()
  {
    return  _matchForwardingPacket;
  }
}
