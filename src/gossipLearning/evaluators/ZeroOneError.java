package gossipLearning.evaluators;

/**
 * This class can compute the zero-one error.
 * 
 * @author István Hegedűs
 */
public class ZeroOneError extends ValueBasedEvaluator {
  private static final long serialVersionUID = 3078153893635373513L;

  public ZeroOneError() {
    super();
  }
  
  public ZeroOneError(ZeroOneError a) {
    super(a);
  }
  
  @Override
  public Object clone() {
    return new ZeroOneError(this);
  }

  @Override
  public double getValue(double expected, double predicted) {
    return expected == predicted ? 0.0 : 1.0;
  }

  @Override
  public double postProcess(double meanValue) {
    return meanValue;
  }

}