package ecs.soton.dsj1n15.smesh.model.propogation;

/**
 * General free space model.
 * 
 * @author David Jones (dsj1n15)
 */
public class FreeSpacePropagationModel extends PropagationModel {

  protected double freq;

  /**
   * Create a free space model at the given frequency.
   * 
   * @param freq Frequency for free space formula.
   */
  public FreeSpacePropagationModel(double freq) {
    this.freq = freq;
  }

  @Override
  public double getPathLoss(double distance) {
    return 20 * Math.log10(distance) + 20 * Math.log10(freq) - 27.55;
  }

}
