package ecs.soton.dsj1n15.smesh.model.propogation;

public abstract class PropagationModel {

  /**
   * Calculate the path loss using the class model.
   * 
   * @param distance Distance in meters
   * @param freq Frequency in MHz
   * @return The path loss in decibels (dB)
   */
  public abstract double getPathLoss(double distance);

}
