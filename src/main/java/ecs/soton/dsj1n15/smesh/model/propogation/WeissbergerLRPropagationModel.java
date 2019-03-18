package ecs.soton.dsj1n15.smesh.model.propogation;

/**
 * VALID UP TO 400m
 * 
 * @author davidjones
 *
 */
public class WeissbergerLRPropagationModel extends PropagationModel {

  protected double freq;

  public WeissbergerLRPropagationModel(double freq) {
    this.freq = freq;
  }

  @Override
  public double getPathLoss(double distance) {
    if (distance < 14) {
      return 0.45 * Math.pow(freq, 0.284) * distance;
    } else {
      return 1.33 * Math.pow(freq, 0.284) * Math.pow(distance, 0.588);
    }
  }

}
