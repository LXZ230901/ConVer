package org.batfish.datamodel.routing_policy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.batfish.common.Warnings;
import org.batfish.common.util.ComparableStructure;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AbstractRouteBuilder;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkFactory;
import org.batfish.datamodel.NetworkFactory.NetworkFactoryBuilder;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.statement.If;
import org.batfish.datamodel.routing_policy.statement.SetCommunity;
import org.batfish.datamodel.routing_policy.statement.SetLocalPreference;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.symwork.AECComputeResult;
import org.batfish.symwork.AECEnvironment;
import org.batfish.symwork.SymBgpRoute;
import org.batfish.symwork.SymPolicyAnswer;

@JsonSchemaDescription(
    "A procedural routing policy used to transform and accept/reject IPV4/IPV6 routes")
//路由策略用来保存已有的路由策略,其中包含路由策略的主机,statement要执行的内容等,通过process函数根据传入的路由信息生成一个结构体用于后续的执行判断
//然后通过call函数来执行这个结构体,对每一个statement进行判断
public class RoutingPolicy extends ComparableStructure<String> {

  public static class Builder extends NetworkFactoryBuilder<RoutingPolicy> {

    private String _name;

    private Configuration _owner;

    private List<Statement> _statements;

    public Builder(NetworkFactory networkFactory) {
      super(networkFactory, RoutingPolicy.class);
      _statements = Collections.emptyList();
    }

    @Override
    public RoutingPolicy build() {
      String name = _name != null ? _name : generateName();
      RoutingPolicy routingPolicy = new RoutingPolicy(name, _owner);
      if (_owner != null) {
        _owner.getRoutingPolicies().put(name, routingPolicy);
      }
      routingPolicy.setStatements(_statements);
      return routingPolicy;
    }

    public Builder setName(String name) {
      _name = name;
      return this;
    }

    public Builder setOwner(Configuration owner) {
      _owner = owner;
      return this;
    }

    public Builder setStatements(List<Statement> statements) {
      _statements = statements;
      return this;
    }
  }
  public static boolean isGenerated(String s) {
    return s.startsWith("~");
  }

  /** */
  private static final long serialVersionUID = 1L;

  private static final String PROP_STATEMENTS = "statements";

  private Configuration _owner;

  private transient Set<String> _sources;

  private List<Statement> _statements;

  private boolean _validPolicy;
  @JsonCreator
  private RoutingPolicy(@JsonProperty(PROP_NAME) String name) {
    super(name);
    _statements = new ArrayList<>();
  }
  public RoutingPolicy(String name, Configuration owner) {
    this(name);
    _owner = owner;
    _validPolicy = false;
  }
  public Result call(Environment environment) {
    Set<Integer> matchCEC = new HashSet<>();
    BitSet matchPECSet = new BitSet();
    for (Statement statement : _statements) {
      Result result = statement.execute(environment);
      if (!result.getMatchPECSet().isEmpty())
      {
        matchPECSet.or(result.getMatchPECSet());
      }
      if (!result.getMatchCEC().isEmpty())
      {
        matchCEC.addAll(result.getMatchCEC());
      }
      if (result.getExit()) {
        result.setMatchPECSet(matchPECSet);
        result.addMatchCEC(matchCEC);
        return result;
      }
      if (result.getReturn()) {
        result.setMatchPECSet(matchPECSet);
        result.addMatchCEC(matchCEC);
        result.setReturn(false);
        return result;
      }
    }
    Result result = new Result();
    result.setMatchPECSet(matchPECSet);
    result.addMatchCEC(matchCEC);
    result.setFallThrough(true);
    result.setBooleanValue(environment.getDefaultAction());
    return result;
  }

  public Set<String> computeSources(
      Set<String> parentSources, Map<String, RoutingPolicy> routingPolicies, Warnings w) {
    if (_sources == null) {
      Set<String> newParentSources = Sets.union(parentSources, ImmutableSet.of(_key));
      ImmutableSet.Builder<String> childSources = ImmutableSet.<String>builder();
      childSources.add(_key);
      for (Statement statement : _statements) {
        childSources.addAll(statement.collectSources(newParentSources, routingPolicies, w));
      }
      _sources = childSources.build();
    }
    return _sources;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RoutingPolicy)) {
      return false;
    }
    RoutingPolicy other = (RoutingPolicy) o;
    return _statements.equals(other._statements);
  }

  @JsonIgnore
  public Configuration getOwner() {
    return _owner;
  }

  @JsonIgnore
  public Set<String> getSources() {
    return _sources;
  }

  @JsonProperty(PROP_STATEMENTS)
  @JsonPropertyDescription("The list of routing-policy statements to execute")
  public List<Statement> getStatements() {
    return _statements;
  }

  public boolean process(
      AbstractRoute inputRoute,
      AbstractRouteBuilder<?, ?> outputRoute,
      @Nullable Ip peerAddress,
      String vrf,
      Direction direction) {
    return process(inputRoute, outputRoute, peerAddress, null, vrf, direction);
  }

  public SymPolicyAnswer symProcess(
      SymBgpRoute inputRoute,
      SymBgpRoute.Builder outputRoute,
      @Nullable Ip peerAddress,
      @Nullable Prefix peerPrefix,
      String vrf,
      Direction direction) {
    try {
      Environment environment = Environment.builder(_owner)
          .setVrf(vrf)
          .setSymBgpOriginalRoute(inputRoute)
          .setSymBgpOutputRouteBuilder(outputRoute)
          .setPeerAddress(peerAddress)
          .setDirection(direction)
          .setPeerPrefix(peerPrefix)
          .build();
      environment._symbolic = true;
      Result result = call(environment);
      SymPolicyAnswer answer = new SymPolicyAnswer();
      answer._accept = result.getBooleanValue();
      if (environment._symBgpOutPutRouteDivide.size() != 0) {
        answer._divideRotue = environment._symBgpOutPutRouteDivide.get(0);
      } else {
        answer._divideRotue = null;
      }
      return answer;
    }catch (Exception e)
    {
      e.printStackTrace();
      System.out.println("policy:"+e);
    }
    return new SymPolicyAnswer();
  }

  public boolean process(
      AbstractRoute inputRoute,
      AbstractRouteBuilder<?, ?> outputRoute,
      @Nullable Ip peerAddress,
      @Nullable Prefix peerPrefix,
      String vrf,
      Direction direction) {
    Environment environment =
        Environment.builder(_owner)
            .setVrf(vrf)
            .setOriginalRoute(inputRoute)
            .setOutputRoute(outputRoute)
            .setPeerAddress(peerAddress)
            .setDirection(direction)
            .setPeerPrefix(peerPrefix)
            .build();
    Result result = call(environment);
    return result.getBooleanValue();
  }

  @JsonProperty(PROP_STATEMENTS)
  public void setStatements(List<Statement> statements) {
    _statements = statements;
  }

  public RoutingPolicy simplify() {
    ImmutableList.Builder<Statement> simpleStatements = ImmutableList.builder();
    for (Statement statement : _statements) {
      simpleStatements.addAll(statement.simplify());
    }
    RoutingPolicy simple = new RoutingPolicy(_key, _owner);
    simple.setStatements(simpleStatements.build());
    return simple;
  }

  public void setValidPolicy(boolean validPolicy)
  {
    _validPolicy = validPolicy;
  }
















  public boolean getValidPolicy()
  {
    return _validPolicy;
  }

//  public Result computeEquivalence(Set<Integer> CEC) {
//    for (Statement statement : _statements) {
//      Result result = statement.execute(environment);
//      if (result.getExit()) {
//        return result;
//      }
//      if (result.getReturn()) {
//        result.setReturn(false);
//        return result;
//      }
//    }
//    Result result = new Result();
//    result.setFallThrough(true);
//    result.setBooleanValue(environment.getDefaultAction());
//    return result;
//  }










  public AECComputeResult computeAEC(Integer CEC, AECEnvironment aecEnvironment)
  {
    Set<Integer> computeCEC = new HashSet<>();
    AECComputeResult AEC = new AECComputeResult();
    boolean validPolicy = false;
    int validNum = 0;
    boolean lastValid = true;
    for (Statement statement : _statements)
    {
      if (validNum != 0 && statement instanceof SetLocalPreference)
      {
        lastValid = false;
      }
      if (statement instanceof SetCommunity)
      {
        break;
      }
      if (statement instanceof If)
      {
        AECComputeResult aecComputeResult = ((If) statement).computeAEC(CEC, aecEnvironment);
        if (aecComputeResult.getCECSet().size() > 1)
        {
          validNum ++;
        } else if (aecComputeResult.getCECSet().size() == 1)
        {
          for (Integer cec : aecComputeResult.getCECSet())
          {
            if (!Objects.equals(cec, aecComputeResult.getRemainCEC()))
            {
              validNum ++;
            }
          }
        }
        if (aecComputeResult.getValidAEC())
        {
          validPolicy = true;
        }
        computeCEC.addAll(aecComputeResult.getCECSet());
        CEC = aecComputeResult.getRemainCEC();
      }
    }
    computeCEC.add(CEC);
    AEC.addComputeCECSet(computeCEC);
    if (validPolicy || validNum > 1 || (validNum == 1 && !lastValid))
    {
      AEC.setValidAEC(true);
    }
    return AEC;
  }
}
