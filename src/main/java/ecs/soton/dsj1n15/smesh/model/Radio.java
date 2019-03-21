package ecs.soton.dsj1n15.smesh.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import math.geom2d.Point2D;

/**
 * Abstract class for a general radio. Radio Provides access to radio parameters and calculations
 * that all radios must make use of, e.g. frequency, bandwidth, SNR...
 * 
 * @author David Jones (dsj1n15)
 */
public abstract class Radio {
  /** A mapping of all transmissions seen to observation data */
  protected Map<Transmission, Set<ReceiveData>> receives = new LinkedHashMap<>();

  /** A unique ID */
  protected final int id;
  
  protected Environment environment;

  protected double x;
  protected double y;
  protected double z;

  /** The last time the radio state was checked */
  protected long lastTime;

  /**
   * Instantiate a Radio.
   * 
   * @param id ID of the radio
   */
  public Radio(int id) {
    this.id = id;
  }
  
  /**
   * @return The Radio ID
   */
  public int getID() {
    return id;
  }

  /**
   * @return The environment the radio is located in
   */
  public Environment getEnvironment() {
    return environment;
  }

  /**
   * @param environment The environment to place the radio in
   */
  public void setEnvironment(Environment environment) {
    this.environment = environment;
    this.lastTime = Long.MIN_VALUE;
  }

  /**
   * @return The current x coordinate of the radio
   */
  public double getX() {
    return x;
  }

  /**
   * @param x The new x coordinate of the radio
   */
  public void setX(double x) {
    this.x = x;
  }

  /**
   * @return The current y coordinate of the radio
   */
  public double getY() {
    return y;
  }

  /**
   * @param y The new y coordinate of the radio
   */
  public void setY(double y) {
    this.y = y;
  }

  /**
   * @return The current z coordinate of the radio
   */
  public double getZ() {
    return z;
  }

  /**
   * @param z The new z coordinate of the radio
   */
  public void setZ(double z) {
    this.z = z;
  }

  /**
   * @return The XY coordinate of the radio
   */
  public Point2D getXY() {
    return new Point2D(x, y);
  }

  /**
   * @return The radio frequency in MHz
   */
  public abstract double getFrequency();

  /**
   * @return The radio bandwidth in Hz
   */
  public abstract int getBandwidth();

  /**
   * @return The height from the ground of the antenna in meters
   */
  public abstract double getAntennaHeight();

  /**
   * @return The transmission power in dBm
   */
  public abstract int getTxPow();

  /**
   * @return The RX and TX gain of the antenna in dBm
   */
  public abstract double getAntennaGain();

  /**
   * @return The RX and TX loss of cabling in dBm
   */
  public abstract double getCableLoss();

  /**
   * @return The receiver noise figure (NF) in dBm
   */
  public abstract double getNoiseFigure();

  /**
   * @return The sensitivity of the radio in its current configuration.
   */
  public abstract double getSensitivity();

  /**
   * Calculate the SNR required for a successful demodulation.
   * 
   * @return The calculated SNR in dBm
   */
  public double getRequiredSNR() {
    return getSensitivity() - getNoiseFloor();
  }

  /**
   * A radio will often reduce the power of a very strong signal before demodulation. Apply these
   * limits to the input SNR so the SNR is what the demodulator would see.
   * 
   * @param snr SNR (in dBm) to validate
   * @return SNR as the demodulator would see it
   */
  public abstract double validateSNR(double snr);

  /**
   * Calculate the noise floor for the radio in its current configuration.
   * 
   * @return The calculated noise floor in dBm
   */
  public double getNoiseFloor() {
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
    double t = 290; // Room temperature in Kelvin
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
  public abstract Transmission send(Packet packet);

  /**
   * Record transmission information viewable by the current radio. If a full packet receive has
   * occurred since the last check, calculate the probability of its success, return it on success.
   * 
   * @return A full packet if it is available.
   */
  public abstract Packet recv();

  /**
   * Use the recorded transmission information to check if there are any ongoing transmissions that
   * would affect a transmission from this radio.
   * 
   * @return Whether there is any ongoing activity
   */
  public abstract boolean activityDetection();

  /**
   * If the radio is currently transmitting return the transmission object. This object may still be
   * returned even if the transmission has finished but has not been tidied up.
   * 
   * @return Current transmission if there is one, otherwise null
   */
  public abstract Transmission getCurrentTransmission();

  /**
   * Checks whether this radio can communicate with another radio using the current parameters of
   * each. This should not check any environmental parameters.
   * 
   * @param rx Receiver to check communication with
   * @return Whether the two radios can communicate
   */
  public abstract boolean canCommunicate(Radio rx);

  /**
   * Checks whether this radio can interfere with another radio - i.e. will it cause noise on the
   * receiver end. This should not check any environmental parameters. This will still return true
   * if the devices can communicate as well as interfere.
   * 
   * @param rx Receiver to check interference with
   * @return Whether the two radios interfere
   */
  public abstract boolean canInterfere(Radio b);

  /**
   * Use the time from the environment to carry out any behaviour since last time passed event.
   */
  public abstract void timePassed();

}
