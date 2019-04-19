package ecs.soton.dsj1n15.smesh.model.propogation;

/**
 * ITU-R propagation model for forests.
 * 
 * @author David Jones (dsj1n15)
 */
public class ITURPropagationModel extends PropagationModel {

  protected double freq;

  /**
   * Create a ITU-R model at the given frequency.
   * 
   * @param freq Frequency for ITU-R formula.
   */
  public ITURPropagationModel(double freq) {
    this.freq = freq;
  }

  @Override
  public double getPathLoss(double distance) {
    return 0.2 * Math.pow(freq, 0.3) * Math.pow(distance, 0.6);
  }

}
