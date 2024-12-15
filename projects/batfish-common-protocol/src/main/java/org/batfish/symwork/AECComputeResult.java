package org.batfish.symwork;

import java.util.HashSet;
import java.util.Set;

public class AECComputeResult {
    public Integer _CEC;
    public Integer _remainCEC;
    public Set<Integer> _CECSet;
    public boolean _validAEC;
    public AECComputeResult () { _CEC = 0; _CECSet = new HashSet<>(); _validAEC = false;}


    public void setValidAEC(boolean validAEC)
    {
      _validAEC = validAEC;
    }

    public boolean getValidAEC()
    {
      return _validAEC;
    }

    public Integer getComputeCEC()
    {
      return _CEC;
    }

    public void setComputeCEC(Integer CEC)
    {
       _CEC = CEC;
    }

    public void addComputeCECSet(Set<Integer> CEC)
    {
      _CECSet.addAll(CEC);
    }

    public void setRemainCEC(Integer CEC)
    {
      _remainCEC = CEC;
    }

    public Integer getRemainCEC()
    {
      return this._remainCEC;
    }

    public Set<Integer> getCECSet()
    {
      return _CECSet;
    }
}
