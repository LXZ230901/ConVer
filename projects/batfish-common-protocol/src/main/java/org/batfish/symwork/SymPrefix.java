package org.batfish.symwork;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class SymPrefix {
  private BDDFactory _factory;
  private BDD _Ip;
  private Long _length;

  public SymPrefix(BDDFactory factory,BDD Ip,Long length)
  {
    this._factory=factory;
    this._Ip=Ip;
    this._length=length;
  }

//  public HashMap<BDD,Long> not()
//  {
//    HashMap<BDD,Long> answer=new HashMap<>();
//    if(_length==0xffffffff)
//    {
//      UnsignedInteger len;
//      len=UnsignedInteger.valueOf("11111111111111111111111111111111");
//      answer.put(_Ip.not(),0xffffffff);
//    }else{
//      answer.put(_Ip.not(),0xffffffff);
//      answer.put(_Ip,~_length);
//    }
//    return answer;
//  }
//
//  public SymPrefix and(SymPrefix Prefix)
//  {
//    if(!_Ip.and(Prefix._Ip).isZero()&&(_length & Prefix._length)!=0x00000000)
//    {
//      return new SymPrefix( _factory , _Ip.and(Prefix._Ip) , _length & Prefix._length );
//    }else{
//      return null;
//    }
//  }

  public BDD getIp()
  {
    return this._Ip;
  }

  public Long getLength()
  {
    return this._length;
  }

  public String toString()
  {
    return this._Ip.toString()+";"+this._length;
  }
}
