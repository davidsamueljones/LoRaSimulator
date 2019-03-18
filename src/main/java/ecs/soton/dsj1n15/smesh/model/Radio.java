package ecs.soton.dsj1n15.smesh.model;

import math.geom2d.Point2D;

public interface Radio {
  
  /**
   * @return The radio frequency in MHz
   */
  public Point2D getXY();
  
  /**
   * @return The radio frequency in MHz
   */
  public double getFrequency();
  
  /**
   * @return The height from the ground of the antenna in meters
   */
  public double getAntennaHeight();
  
}
