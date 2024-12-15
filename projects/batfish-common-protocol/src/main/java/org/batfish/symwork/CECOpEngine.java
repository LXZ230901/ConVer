package org.batfish.symwork;



public class CECOpEngine {
    public static jdd.bdd.BDD _CECBdd;
    public static Integer CECAnd(Integer c1, Integer c2)
    {
        return _CECBdd.and(c1, c2);
    }
    public static Integer CECNot(Integer c1)
    {
        return _CECBdd.not(c1);
    }
    public static void setCECBdd(jdd.bdd.BDD CECBdd)
    {
        _CECBdd = CECBdd;
    }
    public static Integer CECOr(Integer c1, Integer c2)
    {
        return _CECBdd.or(c1, c2);
    }


    public static Integer CECExist(Integer c1, Integer c2)
    {
        return _CECBdd.exists(c1, c2);
    }
}
