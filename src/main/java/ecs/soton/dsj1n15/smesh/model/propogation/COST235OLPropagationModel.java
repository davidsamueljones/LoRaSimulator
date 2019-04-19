package ecs.soton.dsj1n15.smesh.model.propogation;

/**
 * COST235 propagation model for out of leaf forest transmissions.
 * 
 * @author David Jones (dsj1n15)
 */
public class COST235OLPropagationModel extends PropagationModel {

  protected float freq;

  public COST235OLPropagationModel(float freq) {
    this.freq = freq;
  }

  @Override
  public double getPathLoss(double distance) {
    return 26.6 * Math.pow(freq, -0.2) * Math.pow(distance, 0.5);
  }

}
