package ecs.soton.dsj1n15.smesh.model.propogation;

/**
 * Empirical free space model from PHY testing. <br>
 * Valid for 0.0m ground level transmission at 868MHz.
 * 
 * @author David Jones (dsj1n15)
 */
public class EmpiricalFreeSpacePropogationModel extends PropagationModel {

  @Override
  public double getPathLoss(double distance) {
    return -167 + 91 * Math.log10(distance + 362);
  }

}
