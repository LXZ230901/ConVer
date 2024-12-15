package New;

public class GeneratorException  extends RuntimeException{
  /**
   * Runtime exception thrown during ETG generation.
   * @author Aaron Gember-Jacobson (agember@cs.wisc.edu)
   */
    private static final long serialVersionUID = 1L;

    public GeneratorException(String msg) {
      super(msg);
    }

    public GeneratorException(String msg, Exception ex) {
      super(msg, ex);
    }
}
