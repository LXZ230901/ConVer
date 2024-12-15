package org.batfish.dataplane.rib;

import static net.sf.javabdd.BDDFactory.diff;
import static org.batfish.datamodel.Prefix.MAX_PREFIX_LENGTH;
import static org.batfish.dataplane.rib.RouteAdvertisement.Reason.REPLACE;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.dataplane.rib.RibDelta.Builder;
import org.batfish.dataplane.rib.RouteAdvertisement.Reason;

/**
 * RibTree is constructed from nodes of this type. A node has a prefix, a set of routes that match
 * the prefix (and it's length) and two children. The children's prefixes must always be more
 * specific (i.e., their prefix length is larger).
 */
class RibTreeNode<R extends AbstractRoute> implements Serializable {

  private static final long serialVersionUID = 1L;

  private RibTreeNode<R> _left;

  private Prefix _prefix;

  private RibTreeNode<R> _right;

  private final Set<R> _routes;

  private final List<R> _allRoutes;

  private AbstractRib<R> _owner;

  RibTreeNode(Prefix prefix, @Nonnull AbstractRib<R> owner) {
    _routes = new HashSet<>();
    _allRoutes = new ArrayList<>();
    _prefix = prefix;
    _owner = owner;
  }

  void collectRoutes(ImmutableCollection.Builder<R> routes) {//Allroutes 路由收集的部分
    if (_left != null) {
      _left.collectRoutes(routes);
    }
    if (_right != null) {
      _right.collectRoutes(routes);
    }
    routes.addAll(_routes);
    //routes.addAll(_allRoutes);
  }

  void collectAllRoutes(ImmutableCollection.Builder<R> routes) {//Allroutes 路由收集的部分
    if (_left != null) {
      _left.collectAllRoutes(routes);
    }
    if (_right != null) {
      _right.collectAllRoutes(routes);
    }
    System.out.println("_Allroutes:"+_allRoutes.toString());
    System.out.println("_routes:"+_routes.toString());
    System.out.println("newallRoutes:"+_allRoutes);
    routes.addAll(_allRoutes);//修改了
    //routes.addAll(_allRoutes);
  }

  void collectEveryRoutes(List<R> routes) {//Allroutes 路由收集的部分
    if (_left != null) {
      _left.collectEveryRoutes(routes);
    }
    if (_right != null) {
      _right.collectEveryRoutes(routes);
    }
    //System.out.println("newallRoutes:"+_allRoutes);
    routes.addAll(_allRoutes);//修改了
    //routes.addAll(_allRoutes);
  }
  @Nullable
  private RibTreeNode<R> findRouteNode(long bits, int prefixLength, int firstUnmatchedBitIndex) {
    // If prefix lengths match, this is the node where such route would be stored.
    if (prefixLength == _prefix.getPrefixLength()) {
      return this;
    }

    boolean currentBit = Ip.getBitAtPosition(bits, firstUnmatchedBitIndex);
    /*
     * If prefixes don't match exactly, look at the current bit. That determines whether we look
     * left or right. As long as the child is not null, recurse.
     *
     * Note that:
     * 1) routes are stored in the nodes where lengths of the node prefix and the route prefix
     *    match exactly; and
     * 2) prefix matches only get more specific (longer) the deeper we go in the tree
     *
     * Therefore, we can fast-forward the firstUnmatchedBitIndex to the prefix length of the
     * child node
     */
    if (currentBit) {
      return (_right != null)
          ? _right.findRouteNode(bits, prefixLength, _right._prefix.getPrefixLength())
          : null;
    } else {
      return (_left != null)
          ? _left.findRouteNode(bits, prefixLength, _left._prefix.getPrefixLength())
          : null;
    }
  }

  /**
   * Check if the route exists in our subtree
   *
   * @param route route in question
   * @param bits route's IP address represented as a BitSet
   * @param prefixLength route's prefix length
   * @return true if the route is in the subtree
   */
  boolean containsRoute(R route, long bits, int prefixLength) {
    RibTreeNode<R> node = findRouteNode(bits, prefixLength, 0);
    return node != null && node._routes.contains(route);
  }

  private Set<R> getLongestPrefixMatch(Ip address) {
    return _routes
        .stream()
        .filter(r -> r.getNetwork().containsIp(address))
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns a set of routes with the longest prefix match for a given IP address
   *
   * @param address IP address
   * @param bits IP address represented as a set of bits
   * @param index the position of the bit up to which the match has already been found
   *     (tail-recursion way of keeping track how deep we are).
   * @return a set of routes
   */
  Set<R> getLongestPrefixMatch(Ip address, long bits, int index) {
    // Get the list of routes stored in our node that contain the IP address
    Set<R> longestPrefixMatches = getLongestPrefixMatch(address);
    // If we reached the max prefix length (e.g., 32 for for IPv4) then return routes
    // from the current node
    if (index == MAX_PREFIX_LENGTH) {
      return longestPrefixMatches;
    }

    // Examine the bit at the given index
    boolean currentBit = Ip.getBitAtPosition(bits, index);
    RibTreeNode<R> child;

    // the current bit is 1, go right recursively
    if (currentBit) {
      child = _right;
    } else {
      child = _left;
    }
    if (child == null) {
      return longestPrefixMatches;
    }

    // Represents any potentially longer route matches (than ones stored at this node)
    Set<R> longerMatches =
        child.getLongestPrefixMatch(address, bits, child._prefix.getPrefixLength());

    // If we found no better matches, return the ones from this node
    if (longerMatches == null || longerMatches.isEmpty()) {
      return longestPrefixMatches;
    } else { // otherwise return longer matches
      return longerMatches;
    }
  }

  private void assignChild(RibTreeNode<R> parent, RibTreeNode<R> child, boolean branchRight) {
    if (branchRight) {
      parent._right = child;
    } else {
      parent._left = child;
    }
  }

  /**
   * Takes care of adding new nodes to the tree and maintaining correct pointers.
   *
   * @param parent node that we are trying to merge a route into
   * @param route the route to merge
   * @param routeBits the bitSet representation of the route's IP address
   * @param prefixLength the route's prefix length
   * @param firstUnmatchedBitIndex the index of the first bit in the route's prefix that we haven't
   *     checked yet
   * @param rightBranch whether we should recurse down the right side of the tree
   * @return True if a route has been inserted into the tree
   */
  @Nullable
  private RibDelta<R> mergeHelper(
      RibTreeNode<R> parent,
      R route,
      long routeBits,
      int prefixLength,
      int firstUnmatchedBitIndex,
      boolean rightBranch) {
    RibTreeNode<R> node;

    // Get our node from one of the tree sides
    if (rightBranch) {
      node = parent._right;
    } else {
      node = parent._left;
    }

    // Node doesn't exist, so create one. By construction, it will be the best match
    // for the given route
    if (node == null) {
      node = new RibTreeNode<>(route.getNetwork(), _owner);
      node._routes.add(route);
      // 更改了
      System.out.println("///////////////////////////////////////");
      node._allRoutes.add(route);
      // don't forget to assign new node object to parent node
      assignChild(parent, node, rightBranch);
      return new RibDelta.Builder<>(_owner).add(route).build();
    }

    // Node exists, get some helper data out of the current node we are examining
    Prefix nodePrefix = node._prefix;
    int nodePrefixLength = nodePrefix.getPrefixLength();
    Ip nodeAddress = nodePrefix.getStartIp();
    long nodeAddressBits = nodeAddress.asLong();
    int nextUnmatchedBit;
    // Set up two "pointers" as we scan through the route's and the node's prefixes
    boolean currentAddressBit = false;
    boolean currentNodeAddressBit;

    /*
     * We know we matched up to firstUnmatchedBitIndex. Continue going forward in the bits
     * to find a longer match.
     * At the end of this loop nextUnmatchedBit will be the first place where the route prefix
     * and this node's prefix diverge.
     * Note that nextUnmatchedBit can be outside of the node's or the route's prefix.
     */
    for (nextUnmatchedBit = firstUnmatchedBitIndex + 1;
        nextUnmatchedBit < nodePrefixLength && nextUnmatchedBit < prefixLength;
        nextUnmatchedBit++) {
      currentAddressBit = Ip.getBitAtPosition(routeBits, nextUnmatchedBit);
      currentNodeAddressBit = Ip.getBitAtPosition(nodeAddressBits, nextUnmatchedBit);
      if (currentNodeAddressBit != currentAddressBit) {
        break;
      }
    }

    /*
     * If the next unmatched bit is the same as node prefix length, we "ran off" the node prefix.
     * Recursively merge the route into this node.
     */
    if (nextUnmatchedBit == nodePrefixLength) {
      return node.mergeRoute(route, routeBits, prefixLength, nextUnmatchedBit);
    }

    /*
     * If we reached the route's prefix length (but not the nodes's) we need to create a new node
     * above the current node that matches the route's prefix and re-attach the current node to
     * the newly created node.
     */
    if (nextUnmatchedBit == prefixLength) {
      currentNodeAddressBit = Ip.getBitAtPosition(nodeAddressBits, nextUnmatchedBit);
      RibTreeNode<R> oldNode = node;
      node = new RibTreeNode<>(route.getNetwork(), _owner);
      node._routes.add(route);
      assignChild(parent, node, rightBranch);
      assignChild(node, oldNode, currentNodeAddressBit);
      return new RibDelta.Builder<>(_owner).add(route).build();
    }

    /*
     * If we are here, there is a bit difference between the node and route prefixes before we
     * reach the end of either prefix. This requires the following:
     * - Compute the max prefix match (up to nextUnmatchedBit)
     * - Create a new node with this new prefix above the current node
     * - Create a new node with the route's full prefix and assign it the parent.
     * - Existing node becomes a sibling of the node with full route prefix
     */
    RibTreeNode<R> oldNode = node;

    // newNetwork has the max prefix match up to nextUnmatchedBit
    Prefix newNetwork = new Prefix(route.getNetwork().getStartIp(), nextUnmatchedBit);
    node = new RibTreeNode<>(newNetwork, _owner); // node is the node we are inserting in the middle
    RibTreeNode<R> child = new RibTreeNode<>(route.getNetwork(), _owner);
    child._routes.add(route);

    child._allRoutes.add(route);//更改了
    assignChild(parent, node, rightBranch);
    // child and old node become siblings, children of the newly inserted node
    assignChild(node, child, currentAddressBit);
    assignChild(node, oldNode, !currentAddressBit);
    return new RibDelta.Builder<>(_owner).add(route).build();
  }

  //更改了
  @Nullable
  private RibDelta<R> mergeHelperStage(
      RibTreeNode<R> parent,
      R route,
      long routeBits,
      int prefixLength,
      int firstUnmatchedBitIndex,
      boolean rightBranch) {
    RibTreeNode<R> node;

    // Get our node from one of the tree sides
    if (rightBranch) {
      node = parent._right;
    } else {
      node = parent._left;
    }

    // Node doesn't exist, so create one. By construction, it will be the best match
    // for the given route
    if (node == null) {
      node = new RibTreeNode<>(route.getNetwork(), _owner);
      node._routes.add(route);
      // 更改了
      System.out.println("///////////////////////////////////////");
      node._allRoutes.add(route);
      // don't forget to assign new node object to parent node
      assignChild(parent, node, rightBranch);
      return new RibDelta.Builder<>(_owner).add(route).build();
    }

    // Node exists, get some helper data out of the current node we are examining
    Prefix nodePrefix = node._prefix;
    int nodePrefixLength = nodePrefix.getPrefixLength();
    Ip nodeAddress = nodePrefix.getStartIp();
    long nodeAddressBits = nodeAddress.asLong();
    int nextUnmatchedBit;
    // Set up two "pointers" as we scan through the route's and the node's prefixes
    boolean currentAddressBit = false;
    boolean currentNodeAddressBit;

    /*
     * We know we matched up to firstUnmatchedBitIndex. Continue going forward in the bits
     * to find a longer match.
     * At the end of this loop nextUnmatchedBit will be the first place where the route prefix
     * and this node's prefix diverge.
     * Note that nextUnmatchedBit can be outside of the node's or the route's prefix.
     */
    for (nextUnmatchedBit = firstUnmatchedBitIndex + 1;
        nextUnmatchedBit < nodePrefixLength && nextUnmatchedBit < prefixLength;
        nextUnmatchedBit++) {
      currentAddressBit = Ip.getBitAtPosition(routeBits, nextUnmatchedBit);
      currentNodeAddressBit = Ip.getBitAtPosition(nodeAddressBits, nextUnmatchedBit);
      if (currentNodeAddressBit != currentAddressBit) {
        break;
      }
    }

    /*
     * If the next unmatched bit is the same as node prefix length, we "ran off" the node prefix.
     * Recursively merge the route into this node.
     */
    if (nextUnmatchedBit == nodePrefixLength) {
      return node.mergeRouteStage(route, routeBits, prefixLength, nextUnmatchedBit,true);
    }

    /*
     * If we reached the route's prefix length (but not the nodes's) we need to create a new node
     * above the current node that matches the route's prefix and re-attach the current node to
     * the newly created node.
     */
    if (nextUnmatchedBit == prefixLength) {
      currentNodeAddressBit = Ip.getBitAtPosition(nodeAddressBits, nextUnmatchedBit);
      RibTreeNode<R> oldNode = node;
      node = new RibTreeNode<>(route.getNetwork(), _owner);
      node._routes.add(route);
      assignChild(parent, node, rightBranch);
      assignChild(node, oldNode, currentNodeAddressBit);
      return new RibDelta.Builder<>(_owner).add(route).build();
    }

    /*
     * If we are here, there is a bit difference between the node and route prefixes before we
     * reach the end of either prefix. This requires the following:
     * - Compute the max prefix match (up to nextUnmatchedBit)
     * - Create a new node with this new prefix above the current node
     * - Create a new node with the route's full prefix and assign it the parent.
     * - Existing node becomes a sibling of the node with full route prefix
     */
    RibTreeNode<R> oldNode = node;

    // newNetwork has the max prefix match up to nextUnmatchedBit
    Prefix newNetwork = new Prefix(route.getNetwork().getStartIp(), nextUnmatchedBit);
    node = new RibTreeNode<>(newNetwork, _owner); // node is the node we are inserting in the middle
    RibTreeNode<R> child = new RibTreeNode<>(route.getNetwork(), _owner);
    child._routes.add(route);

    child._allRoutes.add(route);//更改了
    assignChild(parent, node, rightBranch);
    // child and old node become siblings, children of the newly inserted node
    assignChild(node, child, currentAddressBit);
    assignChild(node, oldNode, !currentAddressBit);
    return new RibDelta.Builder<>(_owner).add(route).build();
  }

//  @Nullable
//  @SuppressWarnings("unchecked")
//  RibDelta<R> mergeRoute(//增加一条新的路由,后边这一部分也是需要改的,这里使用的是树状结构来保存前缀,主要就是比较路由,然后添加路由,过程中使用了delta来保存路由的更改,以便向其他节点发更改的路由
//      R route, long routeBits, int routePrefixLength, int firstUnmatchedBitIndex) {
//    /*
//     * We have reached the node where a route should be inserted, because:
//     * 1) the prefix length of this node matches the prefix length of the route exactly, and
//     * 2) going deeper can only gets us longer matches
//     */
//    if (routePrefixLength == _prefix.getPrefixLength()) {
//
//      // No routes with this prefix, so just add it. No comparison necessary
//      if(route.getProtocol().equals(RoutingProtocol.BGP)&&_allRoutes.isEmpty())
//      {
//        _routes.add(route);
//        _allRoutes.add(route);
//        System.out.println("before_allroutes:"+_allRoutes);
//        System.out.println("add_allroutes:"+_allRoutes);
//        return new RibDelta.Builder<>(_owner).add(route).build();
//      }else if (_routes.isEmpty()&_allRoutes.isEmpty()) {
//        _routes.add(route);
//        _allRoutes.add(route);
//        System.out.println("before_allroutes:"+_allRoutes);
//        System.out.println("add_allroutes:"+_allRoutes);
//        return new RibDelta.Builder<>(_owner).add(route).build();
//      }else if(_routes.isEmpty()){
//        _routes.add(route);
//        System.out.println("before_allroutes:"+_allRoutes);
//        System.out.println("add_allroutes:"+_allRoutes);
//        return new RibDelta.Builder<>(_owner).add(route).build();
//      }
//      System.out.println("1111111111111111111111111111111111");
//      /*
//       * Check if the route we are adding is preferred to the routes we already have.
//       * We only need to compare to one route, because all routes already in this node have the       * same preference level. Hence, the route we are checking will be better than all,
//       * worse than all, or at the same preference level.
//       */
//      //判断在所有路由表中这个路由应该在的位置
//      System.out.println(route.getProtocol());
//      if(route.getProtocol().equals(RoutingProtocol.BGP))
//      {
//        if(!_allRoutes.contains(route))
//        {
//          BgpRoute bgpRoute=(BgpRoute) route;
//          System.out.println("merage_route:all_routes"+_allRoutes.toString()+"; new_route:"+bgpRoute.toString());
//          System.out.println("merage_route:"+"; new_route:"+bgpRoute.getAsPath().toString());
//          bgpRoute.getTopologyCondition().printSet();
//          System.out.println("1rib-allroutes:"+_allRoutes.toString());
//          System.out.println("22222222222222222222222222222");
//
//          System.out.println("newRoute:"+bgpRoute.toString());
//          System.out.println("new_aspath:"+bgpRoute.getAsPath().toString());
//          //bgpRoute.getTopologyCondition().printSet();
//          System.out.println("================Before_RibTreenode_topologyCondition:"+bgpRoute.getTopologyCondition().toString());
//          int place=0;
//          Set<R> withDraw=new HashSet<>();
//          Set<R> addRoute=new HashSet<>();
//          for(int i=0;i<_allRoutes.size();i++)
//          {
//            int preferenceComparison = _owner.comparePreference(route, _allRoutes.get(i));
//            if(preferenceComparison<0)
//            {
//              place++;
//            }
//          }
//          System.out.println("2rib-allroutes:"+_allRoutes.toString());
//          //判断有没有相同的路由,如果有的话则代表应该是路由条件的更新则直接更新了路由拓扑条件就可以了,在这里是直接删除该元素,后续再添加新的路由,方便后续消息的更新
//          if(_allRoutes.size()>0&&place<_allRoutes.size())
//          {
//            System.out.println("3333333333333333555555555555555555555");
//            System.out.println("place:"+place);
//            if(_allRoutes.get(place).getProtocol().equals(RoutingProtocol.BGP))
//            {
//              System.out.println("333333333333333333333333333333333333333");
//              BgpRoute temp = (BgpRoute) _allRoutes.get(place);
//              System.out.println("last_aspath:"+temp.getAsPath().toString());
//              if(bgpRoute.getAsPath().equals(temp.getAsPath()))
//              {
//                //针对于拓扑条件的更新,是先撤销之前的路由再添加新的路由
//                withDraw.add(_allRoutes.get(place));
//                _allRoutes.remove(place);
//              }
//            }
//          }
//          //System.out.println("new_aspath:"+bgpRoute.getAsPath().toString());
//          System.out.println("3rib-allroutes:"+_allRoutes.toString());
//          System.out.println("44444444444444444444444444444444444");
//          //addRoute.add(route);//插入新的路由
//          _allRoutes.add(place,route);//插入这个路由
//
//          System.out.println("4rib-allroutes:"+_allRoutes.toString());
//          //下面就是针对这个路由表当中的路由进行更新,更新路由的拓扑条件,与上比其优先级高的路由拓扑条件的非,比其优先级低的路由要与上其拓扑条件的非,这两部分都需要发送给下一个节点
//          //这里现在是把所哟u的路由都撤销掉了,后面可能还需要优化
//          //这部分BDD固定的后边需要优化
//          System.out.println("5555555555555555555555555555555555");
//
//          BDD beforeBdd=bgpRoute.getTopologyCondition().getFactory().ithVar(0);
//          //计算优先级高的路由的Bdd
//          beforeBdd = beforeBdd.biimpWith(beforeBdd);
//
//          for(int i=0;i<place;i++)
//          {
//            if(_allRoutes.get(i).getProtocol().equals(RoutingProtocol.BGP)) {
//              System.out.println("before_BDD:"+beforeBdd.toString());
//              BgpRoute temp = (BgpRoute) _allRoutes.get(i);
//              System.out.println("route_BDD:"+temp.getTopologyCondition().toString());
//              beforeBdd=beforeBdd.applyWith(temp.getTopologyCondition(), diff);
//              System.out.println("after_BDD:"+beforeBdd.toString());
//            }
//          }
//          System.out.println("666666666666666666666666666666666666666666");
//          System.out.println("place:"+place);
//          System.out.println("allRoutes-size:"+_allRoutes.size());
//          System.out.println(_allRoutes.toString());
//          //更新比其优先级低的路由的拓扑条件
//          for(int i=place+1;i<_allRoutes.size();i++)
//          {
//            System.out.println("lastRoute:"+_allRoutes.get(i).toString());
//            withDraw.add(_allRoutes.get(i));//这一部分没有设置其的拓扑条件,应该对后边的路由更新不影响,因为路由撤回应该不涉及拓扑条件的变化,所以影响不大,后边如果有错就需要关注这一方面
//            if(_allRoutes.get(i).getProtocol().equals(RoutingProtocol.BGP))
//            {
//              BgpRoute temp=(BgpRoute) _allRoutes.get(i);
//              //            System.out.println("lastRouteTopologyCondition:"+temp.getTopologyCondition().toString());
//              //            temp.getTopologyCondition().printSet();
//              //            bgpRoute.getTopologyCondition().printSet();
//              //            BDD not=bgpRoute.getTopologyCondition().not();
//              //            not.printSet();
//              //            BDD pri=temp.getTopologyCondition().not();
//              //            pri=pri.not();
//              //            System.out.println("beafore:pri");
//              //            pri.printSet();
//              //            System.out.println("after:pri");
//              //            BDD l = pri.andWith(not);
//              //            l.printSet();
//              //            BDD a= temp.getTopologyCondition();
//              //            System.out.println("a:");
//              //            a.printSet();
//              //            BDD b= bgpRoute.getTopologyCondition();
//              //            System.out.println("b:");
//              //            b.printSet();
//              //            BDD c= a.applyWith(b,diff);
//              //            System.out.println("c:");
//              //            c.printSet();
//              //            System.out.println("1234567890");
//              bgpRoute.getTopologyCondition().printSet();
//              BDD multiTemp=bgpRoute.getTopologyCondition().not();
//
//              //BDD ans=temp.getTopologyCondition().applyWith(bgpRoute.getTopologyCondition(),diff);
//              BDD ans=temp.getTopologyCondition().and(multiTemp);
//              System.out.println("factory:"+ temp.getTopologyCondition().getFactory().varNum());
//              System.out.println("factory:"+ bgpRoute.getTopologyCondition().getFactory().varNum());
//              bgpRoute.getTopologyCondition().printSet();
//              //temp.getTopologyCondition().applyWith(bgpRoute.getTopologyCondition(),diff).printSet();
//              temp.setTopologyCondition(ans);
//              temp.getTopologyCondition().printSet();
//              if(place!=0)
//                temp.setTopologyCondition(temp.getTopologyCondition().applyWith(beforeBdd,diff));
//              BDD notTemp=bgpRoute.getTopologyCondition().not();
//              notTemp=notTemp.applyWith(notTemp,diff);
//              System.out.println("notTemp:");
//              notTemp.printSet();
//              temp.getTopologyCondition().printSet();
//              if(!temp.getTopologyCondition().equals(notTemp))
//              {
//                System.out.println("减枝");
//                addRoute.add((R) temp);
//              }
//            }else{
//              addRoute.add(_allRoutes.get(i));
//            }
//          }
//          System.out.println("Before_RibTreenode_topologyCondition:"+bgpRoute.getTopologyCondition().toString());
//          if(place!=0)
//          {
//            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
//            BDD multiTemp=bgpRoute.getTopologyCondition().not();
//            multiTemp = multiTemp.not();
//            multiTemp.printSet();
//            beforeBdd.printSet();
//            //bgpRoute.setTopologyCondition(bgpRoute.getTopologyCondition().applyWith(beforeBdd,diff));
//            multiTemp.and(beforeBdd).printSet();
//            bgpRoute.setTopologyCondition(multiTemp.and(beforeBdd));
//            System.out.println("==================================================================");
//          }
//          System.out.println("-------------------------------------------------------------------");
//          System.out.println("after_RibTreenode_topologyCondition:"+bgpRoute.getTopologyCondition().toString());
//          BDD notTemp=bgpRoute.getTopologyCondition().not();
//          notTemp=notTemp.applyWith(notTemp,diff);
//          System.out.println("notTemp:");
//          notTemp.printSet();
//          if(!bgpRoute.getTopologyCondition().equals(notTemp))
//          {
//            System.out.println("减枝");
//            addRoute.add((R) bgpRoute);
//          }
//          //addRoute.add((R)bgpRoute);//增加新的路由
//
//          RibDelta<R> delta = new Builder<> (_owner).remove(withDraw, REPLACE).add(addRoute).build();
//          _routes.clear();
//          _routes.add(route);
//          return delta;
//        }
//
//      }else{
//        R oldRoute = _routes.iterator().next();
//        int place=0;
//        for(int i=0;i<_allRoutes.size();i++)
//        {
//          int preferenceComparison = _owner.comparePreference(route, _allRoutes.get(i));
//          if(preferenceComparison<0)
//          {
//            place++;
//          }
//        }
//        if(!_allRoutes.contains(route))
//        {
//          _allRoutes.add(place,route);
//        }
//        int preferenceComparison = _owner.comparePreference(route, oldRoute);
//
//        if (preferenceComparison < 0) { // less preferable, so route doesn't get added
//          return null;
//        }
//        if (preferenceComparison == 0) { // equal preference, so add for multipath routing
//          if (_routes.contains(route)) {
//            // route is already here, so nothing to do
//            return null;
//          }
//          // Otherwise add the route
//          if (_routes.add(route)) {
//            return new RibDelta.Builder<>(_owner).add(route).build();
//          } else {
//            return null;
//          }
//        }
//        // Last case, preferenceComparison > 0
//        /*
//         * Better than all pre-existing routes for this prefix, so
//         * replace them with this one.
//         */
//        RibDelta<R> delta = new Builder<>(_owner).remove(_routes, REPLACE).add(route).build();
//        _routes.clear();
//        _routes.add(route);
//        return delta;
//      }
//    }
//    /*
//     * The prefix match is not exact, do some extra insertion logic.
//     * Current bit determines which side of the tree to go down (1 = right, 0 = left)
//     */
//    boolean currentBit = Ip.getBitAtPosition(routeBits, firstUnmatchedBitIndex);
//    return mergeHelper(
//        this, route, routeBits, routePrefixLength, firstUnmatchedBitIndex, currentBit);
//  }

  @Nullable
  RibDelta<R> mergeRoute(//增加一条新的路由,后边这一部分也是需要改的,这里使用的是树状结构来保存前缀,主要就是比较路由,然后添加路由,过程中使用了delta来保存路由的更改,以便向其他节点发更改的路由
      R route, long routeBits, int routePrefixLength, int firstUnmatchedBitIndex) {
    /*
     * We have reached the node where a route should be inserted, because:
     * 1) the prefix length of this node matches the prefix length of the route exactly, and
     * 2) going deeper can only gets us longer matches
     */
    if (routePrefixLength == _prefix.getPrefixLength()) {

      // No routes with this prefix, so just add it. No comparison necessary
      if (_routes.isEmpty()) {
        _routes.add(route);
        return new RibDelta.Builder<>(_owner).add(route).build();
      }

      /*
       * Check if the route we are adding is preferred to the routes we already have.
       * We only need to compare to one route, because all routes already in this node have the
       * same preference level. Hence, the route we are checking will be better than all,
       * worse than all, or at the same preference level.
       */
      R oldRoute = _routes.iterator().next();
      int preferenceComparison = _owner.comparePreference(route, oldRoute);
      if (preferenceComparison < 0) { // less preferable, so route doesn't get added
        return null;
      }
      if (preferenceComparison == 0) { // equal preference, so add for multipath routing
        if (_routes.contains(route)) {
          // route is already here, so nothing to do
          return null;
        }
        // Otherwise add the route
        if (_routes.add(route)) {
          return new RibDelta.Builder<>(_owner).add(route).build();
        } else {
          return null;
        }
      }
      // Last case, preferenceComparison > 0
      /*
       * Better than all pre-existing routes for this prefix, so
       * replace them with this one.
       */
      RibDelta<R> delta = new Builder<>(_owner).remove(_routes, REPLACE).add(route).build();
      _routes.clear();
      _routes.add(route);
      return delta;
    }
    /*
     * The prefix match is not exact, do some extra insertion logic.
     * Current bit determines which side of the tree to go down (1 = right, 0 = left)
     */
    boolean currentBit = Ip.getBitAtPosition(routeBits, firstUnmatchedBitIndex);
    return mergeHelper(
        this, route, routeBits, routePrefixLength, firstUnmatchedBitIndex, currentBit);
  }
  @Nullable
  @SuppressWarnings("unchecked")
  RibDelta<R> mergeRouteStage(//增加一条新的路由,后边这一部分也是需要改的,这里使用的是树状结构来保存前缀,主要就是比较路由,然后添加路由,过程中使用了delta来保存路由的更改,以便向其他节点发更改的路由
      R route, long routeBits, int routePrefixLength, int firstUnmatchedBitIndex,boolean stage) {
    /*
     * We have reached the node where a route should be inserted, because:
     * 1) the prefix length of this node matches the prefix length of the route exactly, and
     * 2) going deeper can only gets us longer matches
     */
    System.out.println("Stage:"+stage);
    if (routePrefixLength == _prefix.getPrefixLength()) {
      System.out.println("merage_route:all_routes"+_allRoutes.toString()+"; new_route:"+route.toString());
      System.out.println("come  in");
      // No routes with this prefix, so just add it. No comparison necessary
      if(route.getProtocol().equals(RoutingProtocol.BGP)&&_allRoutes.isEmpty())
      {
        _routes.add(route);
        _allRoutes.add(route);
        System.out.println("before_allroutes:"+_allRoutes);
        System.out.println("add_allroutes:"+_allRoutes);
        return new RibDelta.Builder<>(_owner).add(route).build();
      }else if (_routes.isEmpty()&_allRoutes.isEmpty()) {
        _routes.add(route);
        _allRoutes.add(route);
        System.out.println("before_allroutes:"+_allRoutes);
        System.out.println("add_allroutes:"+_allRoutes);
        return new RibDelta.Builder<>(_owner).add(route).build();
      }else if(_routes.isEmpty()){
        _routes.add(route);
        System.out.println("before_allroutes:"+_allRoutes);
        System.out.println("add_allroutes:"+_allRoutes);
        return new RibDelta.Builder<>(_owner).add(route).build();
      }
      System.out.println("1111111111111111111111111111111111");
      /*
       * Check if the route we are adding is preferred to the routes we already have.
       * We only need to compare to one route, because all routes already in this node have the       * same preference level. Hence, the route we are checking will be better than all,
       * worse than all, or at the same preference level.
       */
      //判断在所有路由表中这个路由应该在的位置
      System.out.println(route.getProtocol());
      if(route.getProtocol().equals(RoutingProtocol.BGP))
      {
        Set<R> withDraw=new HashSet<>();
        Set<R> addRoute=new HashSet<>();
        if(!_allRoutes.contains(route))
        {
          BgpRoute bgpRoute=(BgpRoute) route;
          int place=0;
          for(int i=0;i<_allRoutes.size();i++)
          {
            int preferenceComparison = _owner.comparePreference(route, _allRoutes.get(i));
            if(preferenceComparison<0)
            {
              place++;
            }
          }
          //判断有没有相同的路由,如果有的话则代表应该是路由条件的更新则直接更新了路由拓扑条件就可以了,在这里是直接删除该元素,后续再添加新的路由,方便后续消息的更新
          if(_allRoutes.size()>0&&place<_allRoutes.size())
          {
            System.out.println("3333333333333333555555555555555555555");
            System.out.println("place:"+place);
            if(_allRoutes.get(place).getProtocol().equals(RoutingProtocol.BGP))
            {
              BgpRoute temp = (BgpRoute) _allRoutes.get(place);
              if(bgpRoute.getAsPath().equals(temp.getAsPath()))
              {
                //针对于拓扑条件的更新,是先撤销之前的路由再添加新的路由
                withDraw.add(_allRoutes.get(place));
                _allRoutes.remove(place);
              }
            }
          }
          _allRoutes.add(place,route);//插入这个路由
          addRoute.add(route);
          //下面就是针对这个路由表当中的路由进行更新,更新路由的拓扑条件,与上比其优先级高的路由拓扑条件的非,比其优先级低的路由要与上其拓扑条件的非,这两部分都需要发送给下一个节点
          //这里现在是把所哟u的路由都撤销掉了,后面可能还需要优化
          //这部分BDD固定的后边需要优化
          //更新比其优先级低的路由的拓扑条件
          //addRoute.add((R)bgpRoute);//增加新的路由
        }
        System.out.println("stage_after:"+_allRoutes.toString());
        RibDelta<R> delta = new Builder<> (_owner).remove(withDraw, REPLACE).add(addRoute).build();
        _routes.clear();
        _routes.add(route);
        return delta;
      }else{
        R oldRoute = _routes.iterator().next();
//        int place=0;
//        for(int i=0;i<_allRoutes.size();i++)
//        {
//          int preferenceComparison = _owner.comparePreference(route, _allRoutes.get(i));
//          if(preferenceComparison<0)
//          {
//            place++;
//          }
//        }
//        if(!_allRoutes.contains(route))
//        {
//          _allRoutes.add(place,route);
//        }
        int preferenceComparison = _owner.comparePreference(route, oldRoute);

        if (preferenceComparison < 0) { // less preferable, so route doesn't get added
          return null;
        }
        if (preferenceComparison == 0) { // equal preference, so add for multipath routing
          if (_routes.contains(route)) {
            // route is already here, so nothing to do
            return null;
          }
          // Otherwise add the route
          if (_routes.add(route)) {
            return new RibDelta.Builder<>(_owner).add(route).build();
          } else {
            return null;
          }
        }
        // Last case, preferenceComparison > 0
        /*
         * Better than all pre-existing routes for this prefix, so
         * replace them with this one.
         */
        RibDelta<R> delta = new Builder<>(_owner).remove(_routes, REPLACE).add(route).build();
        _routes.clear();
        _routes.add(route);
        return delta;
      }
    }
    /*
     * The prefix match is not exact, do some extra insertion logic.
     * Current bit determines which side of the tree to go down (1 = right, 0 = left)
     */
    boolean currentBit = Ip.getBitAtPosition(routeBits, firstUnmatchedBitIndex);
    return mergeHelperStage(
        this, route, routeBits, routePrefixLength, firstUnmatchedBitIndex, currentBit);
  }


//  @Nullable
//  RibDelta<R> mergeRoute(//增加一条新的路由,后边这一部分也是需要改的,这里使用的是树状结构来保存前缀,主要就是比较路由,然后添加路由,过程中使用了delta来保存路由的更改,以便向其他节点发更改的路由
//      R route, long routeBits, int routePrefixLength, int firstUnmatchedBitIndex) {
//    /*
//     * We have reached the node where a route should be inserted, because:
//     * 1) the prefix length of this node matches the prefix length of the route exactly, and
//     * 2) going deeper can only gets us longer matches
//     */
//    if (routePrefixLength == _prefix.getPrefixLength()) {
//
//      // No routes with this prefix, so just add it. No comparison necessary
//      if (_routes.isEmpty()) {
//        _routes.add(route);
//        return new RibDelta.Builder<>(_owner).add(route).build();
//      }
//      System.out.println("merageRoute:routes:"+_routes.toString()+";newRoute:"+route.toString());
//      /*
//       * Check if the route we are adding is preferred to the routes we already have.
//       * We only need to compare to one route, because all routes already in this node have the
//       * same preference level. Hence, the route we are checking will be better than all,
//       * worse than all, or at the same preference level.
//       */
//      R oldRoute = _routes.iterator().next();
//      int preferenceComparison = _owner.comparePreference(route, oldRoute);
//      if (preferenceComparison < 0) { // less preferable, so route doesn't get added
//
//        //当优先级小的时候也会更新,只不过拓扑条件变化
//
//        return null;
//      }
//      if (preferenceComparison == 0) { // equal preference, so add for multipath routing
//        if (_routes.contains(route)) {
//          // route is already here, so nothing to do
//          return null;
//        }
//        // Otherwise add the route
//        if (_routes.add(route)) {
//          return new RibDelta.Builder<>(_owner).add(route).build();
//        } else {
//          return null;
//        }
//      }
//      // Last case, preferenceComparison > 0
//      /*
//       * Better than all pre-existing routes for this prefix, so
//       * replace them with this one.
//       */
//      RibDelta<R> delta = new Builder<>(_owner).remove(_routes, REPLACE).add(route).build();
//      _routes.clear();
//      _routes.add(route);
//      return delta;
//    }
//    /*
//     * The prefix match is not exact, do some extra insertion logic.
//     * Current bit determines which side of the tree to go down (1 = right, 0 = left)
//     */
//    boolean currentBit = Ip.getBitAtPosition(routeBits, firstUnmatchedBitIndex);
//    return mergeHelper(
//        this, route, routeBits, routePrefixLength, firstUnmatchedBitIndex, currentBit);
//  }

  @Override
  public String toString() {
    return _prefix.toString();
  }

  @Nullable
  public RibDelta<R> removeRoute(
      R route, long bits, int prefixLength, int firstUnmatchedBitIndex, Reason reason) {
    RibTreeNode<R> node = findRouteNode(bits, prefixLength, firstUnmatchedBitIndex);
    if (node == null) {
      // No effect, return null
      return null;
    }
    Builder<R> b = new Builder<>(_owner);//routes的去除
    if (node._routes.remove(route)&&node._allRoutes.remove(route)) {//这一部分
      b.remove(route, reason);
      if (node._routes.isEmpty() && _owner._backupRoutes != null) {
        SortedSet<? extends R> backups =
            _owner._backupRoutes.getOrDefault(route.getNetwork(), Collections.emptySortedSet());
        if (!backups.isEmpty()) {
          node._routes.add(backups.first());//备份路由这一部分可能需要更改
          //node._allRoutes.add(backups.first());
          b.add(backups.first());
        }
      }
    }
    // Return new delta
    return b.build();
  }

  @Override
  public int hashCode() {
    int hashCode = _routes.hashCode();
    if (_left != null) {
      hashCode += _left.hashCode();
    }
    if (_right != null) {
      hashCode += _right.hashCode();
    }
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    return (this == obj)
        // Given that obj is not null, check route equality recursively
        || (obj instanceof RibTreeNode
            && _routes.equals(((RibTreeNode<?>) obj)._routes)
            && (_left == null
                ? ((RibTreeNode<?>) obj)._left == null
                : _left.equals(((RibTreeNode<?>) obj)._left))
            && (_right == null
                ? ((RibTreeNode<?>) obj)._right == null
                : _right.equals(((RibTreeNode<?>) obj)._right)));
  }

  public RibDelta<R> clearRoutes(Prefix prefix) {
    long bits = prefix.getStartIp().asLong();
    RibTreeNode<R> node = findRouteNode(bits, prefix.getPrefixLength(), 0);
    if (node == null) {
      return null;
    }
    RibDelta<R> delta = new Builder<>(_owner).remove(node._routes, REPLACE).build();
    node._routes.clear();
    return delta;
  }

  public void addMatchingIps(ImmutableMap.Builder<Prefix, IpWildcardSetIpSpace> builder) {
    if (_left != null) {
      _left.addMatchingIps(builder);
    }
    if (_right != null) {
      _right.addMatchingIps(builder);
    }
    if (!_routes.isEmpty()) {
      IpWildcardSetIpSpace.Builder matchingIps = IpWildcardSetIpSpace.builder();
      if (_left != null) {
        _left.excludeRoutableIps(matchingIps);
      }
      if (_right != null) {
        _right.excludeRoutableIps(matchingIps);
      }
      matchingIps.including(new IpWildcard(_prefix));
      builder.put(_prefix, matchingIps.build());
    }
  }

  public void addRoutableIps(IpWildcardSetIpSpace.Builder builder) {
    if (!_routes.isEmpty()) {
      builder.including(new IpWildcard(_prefix));
    } else {
      if (_left != null) {
        _left.addRoutableIps(builder);
      }
      if (_right != null) {
        _right.addRoutableIps(builder);
      }
    }
  }

  public void excludeRoutableIps(IpWildcardSetIpSpace.Builder builder) {
    if (!_routes.isEmpty()) {
      builder.excluding(new IpWildcard(_prefix));
    } else {
      if (_left != null) {
        _left.excludeRoutableIps(builder);
      }
      if (_right != null) {
        _right.excludeRoutableIps(builder);
      }
    }
  }
}
