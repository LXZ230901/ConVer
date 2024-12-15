package org.batfish.symwork;

import java.util.HashMap;
import jdd.bdd.BDD;
import org.batfish.datamodel.Configuration;

public class AECEnvironment {



  public Configuration _config;
  public HashMap<Integer, Integer> _communityAtomToBDD;
  public BDD _communityBDD;

  public AECEnvironment(Configuration configuration, HashMap<Integer, Integer> communityAtomToBDD, BDD communityBDD)
  {
    _config = configuration;
    _communityAtomToBDD = communityAtomToBDD;
    _communityBDD = communityBDD;
  }

  public Configuration getConfiguration()
  {
    return _config;
  }

  public HashMap<Integer, Integer> getCommunityAtomToBDD()
  {
    return _communityAtomToBDD;
  }

  public BDD getCommunityBDD()
  {
    return _communityBDD;
  }
}
