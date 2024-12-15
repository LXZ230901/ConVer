package org.batfish.symwork;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.CommunityList;
import org.batfish.datamodel.CommunityListLine;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.expr.CommunitySetElem;
import org.batfish.datamodel.routing_policy.expr.InlineCommunitySet;
import org.batfish.datamodel.routing_policy.expr.LiteralCommunitySetElemHalf;
import org.batfish.datamodel.routing_policy.expr.VarCommunitySetElemHalf;






public class CECEngine {
    public CECEngine() {}
    public static Set<Automaton> computeCommunityAtom(Map<String, Configuration> nodeSet)
    {
        Set<Automaton> communityAtom = new HashSet<>();
        Set<String> communityRegex = new HashSet<>();
        for (Configuration node : nodeSet.values())
        {
            for (CommunityList communityList : node.getCommunityLists().values())
            {
                for (CommunityListLine communityListLine : communityList.getLines())
                {
                    String communityString = communityListLine.getRegex();
                    communityString = communityString.replace("^", "");
                    communityString = communityString.replace("$", "");
                    communityRegex.add(communityString);
                }
            }
            for (InlineCommunitySet inlineCommunitySet : node.getInlineCommunity())
            {
                for (CommunitySetElem communitySetElem : inlineCommunitySet.getCommunities())
                {
                    if (communitySetElem.getPrefix() instanceof VarCommunitySetElemHalf)
                    {
                        String community = ((VarCommunitySetElemHalf) communitySetElem.getPrefix()).getVar() + ":" + ((VarCommunitySetElemHalf) communitySetElem.getSuffix()).getVar();
                        communityRegex.add(community);
                        inlineCommunitySet.addCommunityString(community);
                    } else if (communitySetElem.getPrefix() instanceof LiteralCommunitySetElemHalf)
                    {
                        String community = ((LiteralCommunitySetElemHalf) communitySetElem.getPrefix()).getValue() + ":" + ((LiteralCommunitySetElemHalf) communitySetElem.getSuffix()).getValue();
                        communityRegex.add(community);
                        inlineCommunitySet.addCommunityString(community);
                    } else {
                        System.out.println("extract inlinecommunity error do not process");
                    }
                }
            }
        }
        for (String regexString : communityRegex)
        {
            RegExp regExp = new RegExp(regexString);
            Automaton automaton = regExp.toAutomaton();
            Set<Automaton> addAutomatonSet = new HashSet<>();
            Set<Automaton> removeAutomatonSet = new HashSet<>();
            if (communityAtom.contains(automaton))
            {
                continue;
            }
            for (Automaton automatonTemp : communityAtom)
            {
                Automaton automatonIntersection = automaton.intersection(automatonTemp);
                if (automaton.intersection(automatonTemp).isEmpty())
                {
                    continue;
                } else {
                    Automaton remainAutomatonNot = automatonTemp.intersection(automaton.complement());
                    automaton = automaton.intersection(automatonTemp.complement());
                    removeAutomatonSet.add(automatonTemp);
                    addAutomatonSet.add(automatonIntersection);
                    if (!remainAutomatonNot.isEmpty())
                    {
                        addAutomatonSet.add(remainAutomatonNot);
                    }
                    if (automaton.isEmpty())
                    {
                        break;
                    }
                }
            }
            communityAtom.removeAll(removeAutomatonSet);
            communityAtom.addAll(addAutomatonSet);
            if (!automaton.isEmpty())
            {
                communityAtom.add(automaton);
            }
        }
        return communityAtom;
    }
}
