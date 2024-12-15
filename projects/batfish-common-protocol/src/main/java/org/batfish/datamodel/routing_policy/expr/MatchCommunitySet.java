package org.batfish.datamodel.routing_policy.expr;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import jdd.bdd.BDD;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.symwork.AECComputeResult;
import org.batfish.symwork.AECEnvironment;
import org.batfish.symwork.CECEngine;
import org.batfish.symwork.CECOpEngine;
import org.batfish.symwork.SymBgpRoute;
import org.batfish.symwork.SymCommunity;

public class MatchCommunitySet extends BooleanExpr {

  /** */
  private static final long serialVersionUID = 1L;

  private CommunitySetExpr _expr;

  @JsonCreator
  private MatchCommunitySet() {}

  public MatchCommunitySet(CommunitySetExpr expr) {
    _expr = expr;
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
    MatchCommunitySet other = (MatchCommunitySet) obj;
    if (_expr == null) {
      if (other._expr != null) {
        return false;
      }
    } else if (!_expr.equals(other._expr)) {
      return false;
    }
    return true;
  }

  @Override
  public Result evaluate(Environment environment) {
    Result result = new Result();
    boolean match = false;
    Integer routeCommunity = environment.getSymBgpOutputRoute().getCommunity();


    
    Set<Integer> matchCommunity = _expr.getAllMatchSymCommunity(environment);
    Integer matchSymCommunity = 0;
    for (Integer symCommunity : matchCommunity)
    {
      matchSymCommunity = CECOpEngine.CECOr(matchSymCommunity, symCommunity);
    }
    match = CECOpEngine.CECAnd(matchSymCommunity, routeCommunity) != 0;
    
    result.addMatchCEC(matchSymCommunity);
    result.setBooleanValue(match);
    return result;

    // if(inputCommunities._denyAll)
    // {
    //   match = _expr.matchSingleCommunity(environment, inputCommunities._permitCommunities);
    // }else if(inputCommunities._permitAll)
    // {
    //   SortedSet<Long> routeDenyCommunities=inputCommunities._denyCommunities;
    //   SortedSet<Long> routePermitCommunities=new TreeSet<>();
    //   routePermitCommunities.addAll(inputCommunities._permitCommunities);
    //   SortedSet<Long> communities=_expr.allCommunities(environment);
    //   SortedSet<Long> otherCommunities= new TreeSet<>();
    //   otherCommunities.addAll(communities);
    //   otherCommunities.removeAll(routeDenyCommunities);
    //   if(otherCommunities.size()!=0)
    //   {
    //     environment.getSymBgpOutputRoute()._symCommunities._permitCommunities.addAll(otherCommunities);
    //     environment.getSymBgpOutputRoute()._symCommunities._permitAll=false;
    //     environment.getSymBgpOutputRoute()._symCommunities._denyAll=true;
    //     SymBgpRoute.Builder otherRoute=new SymBgpRoute.Builder(environment.getSymBgpOriginalRoute());
    //     otherRoute._symCommunities=new SymCommunity();
    //     otherRoute._symCommunities._permitAll=true;
    //     otherRoute._symCommunities._denyAll=false;
    //     otherRoute._symCommunities._denyCommunities.addAll(communities);
    //     otherRoute._symCommunities._denyCommunities.addAll(routeDenyCommunities);
    //     otherRoute._symCommunities._permitCommunities.addAll(routePermitCommunities);
    //     environment._symBgpOutPutRouteDivide.add(otherRoute.build());
    //     match=true;
    //   }
    // }
//    SortedSet<Long> inputCommunities = environment._symBgpOriginalRoute.getCommunities();
//    boolean arbitrary=environment._symBgpOriginalRoute._arbitraryCommunity;
////    if (environment.getUseOutputAttributes()
////        && environment.getOutputRoute() instanceof BgpRoute.Builder) {
////      BgpRoute.Builder bgpRouteBuilder = (BgpRoute.Builder) environment.getOutputRoute();
////      inputCommunities = bgpRouteBuilder.getCommunities();
////    } else if (environment.getReadFromIntermediateBgpAttributes()) {
////      inputCommunities = environment.getIntermediateBgpAttributes().getCommunities();
////    } else if (environment.getOriginalRoute() instanceof BgpRoute) {
////      BgpRoute bgpRoute = (BgpRoute) environment.getOriginalRoute();
////      inputCommunities = bgpRoute.getCommunities();
////    }
//    if(arbitrary)
//    {
//      SortedSet<Long> communities=_expr.allCommunities(environment);
//      environment.getSymBgpOutputRoute()._communities.addAll(communities);
//      match=true;
//    }else if (inputCommunities != null) {
//      match = _expr.matchSingleCommunity(environment, inputCommunities);
//    }
    // result.setBooleanValue(match);
    // return result;
  }

  public CommunitySetExpr getExpr() {
    return _expr;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_expr == null) ? 0 : _expr.hashCode());
    return result;
  }

  public void setExpr(CommunitySetExpr expr) {
    _expr = expr;
  }

  @Override
  public AECComputeResult computeAEC(Integer computeCEC, AECEnvironment aecEnvironment)
  {
    AECComputeResult aecComputeResult = new AECComputeResult();
    aecComputeResult.setComputeCEC(computeCEC);
    aecComputeResult.setRemainCEC(computeCEC);
    Set<Integer> matchCommunityAtom = _expr.getCommunityAtom(aecEnvironment.getConfiguration());
    Integer matchCommunityBDD = 0;
    BDD communityBDD = aecEnvironment.getCommunityBDD();
    for (Integer communityAtom : matchCommunityAtom)
    {
      matchCommunityBDD = communityBDD.or(matchCommunityBDD, aecEnvironment.getCommunityAtomToBDD().get(communityAtom));
    }
    aecComputeResult.setComputeCEC(aecEnvironment.getCommunityBDD().ref(communityBDD.and(computeCEC, matchCommunityBDD)));
    aecComputeResult.setRemainCEC(aecEnvironment.getCommunityBDD().ref(communityBDD.and(computeCEC, communityBDD.not(matchCommunityBDD))));
    return aecComputeResult;
  }
}
