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
   * @return The radio bandwidth in Hz
   */
  public int getBandwidth();

  /**
   * @return The height from the ground of the antenna in meters
   */
  public double getAntennaHeight();


  public void send(Packet packet);


  public Packet recv();

  public double getNoiseFigure();

  public double getSensitivity();



  
  public int getTxPow();

  public double getCableLoss();


  public double getAntennaGain();


  public default double getNoiseFloor() {
    return Radio.getNoiseFloor(this);
  }
  
  public default double getRequiredSNR() {
    return getSensitivity() - getNoiseFloor();
  }

  // https://www.semtech.com/uploads/documents/an1200.22.pdf
  public static double getNoiseFloor(Radio radio) {
    double t = 283; // room temperature
    double k = 1.38 * Math.pow(10, -23); // Boltzmann’s Constant
    return 10 * Math.log10(k * t * radio.getBandwidth() * 1000) + radio.getNoiseFigure();
  }

  public double getMaxSNR();

  public double validateSNR(double snr);

}
