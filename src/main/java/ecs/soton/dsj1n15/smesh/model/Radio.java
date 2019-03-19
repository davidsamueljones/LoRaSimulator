package ecs.soton.dsj1n15.smesh.model;

import math.geom2d.Point2D;

/**
 * Interface for a general radio. Allows access to parameters and calculations that all radios must
 * make use of, e.g. frequency, bandwidth, SNR...
 * 
 * @author David Jones (dsj1n15)
 */
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


  /**
   * @return The transmission power in dBm
   */
  public int getTxPow();

  /**
   * @return The RX and TX gain of the antenna in dBm
   */
  public double getAntennaGain();

  /**
   * @return The RX and TX loss of cabling in dBm
   */
  public double getCableLoss();

  /**
   * @return The receiver noise figure (NF) in dBm
   */
  public double getNoiseFigure();

  /**
   * @return The sensitivity of the radio in its current configuration.
   */
  public double getSensitivity();

  /**
   * Calculate the SNR required for a successful demodulation.
   * 
   * @return The calculated SNR in dBm
   */
  public default double getRequiredSNR() {
    return getSensitivity() - getNoiseFloor();
  }

  /**
   * A radio will often reduce the power of a very strong signal before demodulation. Apply these
   * limits to the input SNR so the SNR is what the demodulator would see.
   * 
   * @param snr SNR (in dBm) to validate
   * @return SNR as the demodulator would see it
   */
  public double validateSNR(double snr);

  /**
   * Calculate the noise floor for the radio in its current configuration.
   * 
   * @return The calculated noise floor in dBm
   */
  public default double getNoiseFloor() {
    return Radio.getNoiseFloor(this);
  }

  /**
   * Calculate the noise floor for a radio using its current configuration. <br>
   * See: https://www.semtech.com/uploads/documents/an1200.22.pdf
   * 
   * @param radio A configured radio
   * @return The calculated noise floor in dBm
   */
  public static double getNoiseFloor(Radio radio) {
    double t = 290; // room temperature TODO: Should this be temperature of location?
    double k = 1.38 * Math.pow(10, -23); // Boltzmann’s Constant
    return 10 * Math.log10(k * t * radio.getBandwidth() * 1000) + radio.getNoiseFigure();
  }

  /**
   * Send a packet in the current environment. All sends are broadcasts, packet must contain
   * addressing if required.<br>
   * If the radio is already transmitting throw an exception.
   * 
   * @param packet Packet to send
   * @return The transmission if successful
   */
  public Transmission send(Packet packet);


  /**
   * Record transmission information viewable by the current radio. If a full packet receive has
   * occurred since the last check, calculate the probability of its success, return it on success.
   * 
   * @return A full packet if it is available.
   */
  public Packet recv();
  
}
