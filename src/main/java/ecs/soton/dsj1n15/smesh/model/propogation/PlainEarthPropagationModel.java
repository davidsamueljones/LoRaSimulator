package ecs.soton.dsj1n15.smesh.model.propogation;

/**
 * Plain earth propagation model for surface ray modelling.
 * 
 * @author David Jones (dsj1n15)
 */
public class PlainEarthPropagationModel extends PropagationModel {

  protected double th;
  protected double rh;

  /**
   * Create a PE model for a transmitter and receiver at the given heights.
   * 
   * @param th Transmitter height
   * @param rh Receiver height
   */
  public PlainEarthPropagationModel(double th, double rh) {
    this.th = th;
    this.rh = rh;
  }

  @Override
  public double getPathLoss(double distance) {
    return 40 * Math.log10(distance) - 20 * Math.log10(th) - 20 * Math.log10(rh);
  }

}
