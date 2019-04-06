package ecs.soton.dsj1n15.smesh.radio;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
  /** A unique ID */
  protected final int id;

  protected Environment environment;

  protected double x;
  protected double y;
  protected double z;

  /** A mapping of all transmissions seen to observation data */
  protected Map<Long, PartialReceive> timeMap = new LinkedHashMap<>();

  /** Set of receive listeners that get triggered after every receive attempt */
  protected Set<ReceiveListener> receiveListeners = new LinkedHashSet<>();

  /** Set of tick listeners that get triggered after every tick attempt */
  protected Set<TickListener> tickListeners = new LinkedHashSet<>();

  protected ReceiveResult lastReceive = null;

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
    double floor = 10 * Math.log10(k * t * radio.getBandwidth() * 1000) + radio.getNoiseFigure();
    return floor;
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
   * Listen for traffic, recording transmission information viewable by the current radio.<br>
   * Attempt to sync and decode if required transmission data is available.
   */
  public abstract void recv();

  /**
   * @return The current time map
   */
  public Map<Long, PartialReceive> getTimeMap() {
    return timeMap;
  }

  /**
   * @return The last received message
   */
  public ReceiveResult getLastReceive() {
    return this.lastReceive;
  }

  /**
   * Add a listener that will get triggered on every unsuccessful or successful receive. If the
   * radio would not know of an unsuccessful receive for any reason, e.g. the preamble was never
   * detected then the listener will not get called.
   * 
   * @param receiveListener Receive listener to add
   */
  public void addReceiveListener(ReceiveListener receiveListener) {
    receiveListeners.add(receiveListener);
  }

  /**
   * Remove a receive listener.
   * 
   * @param receiveListener Receive listener to remove
   */
  public void removeReceiveListener(ReceiveListener receiveListener) {
    receiveListeners.remove(receiveListener);
  }

  /**
   * Alert receive listeners with last receive.
   */
  protected void alertReceiveListeners() {
    for (ReceiveListener listener : receiveListeners) {
      listener.receive(lastReceive);
    }
  }

  /**
   * Called after any time change. Do {@link #listen()} and {@link #send(Packet packet)} calls
   * before this so that this can be used as a cleanup function.
   */
  public abstract void tick();

  /**
   * Add a listener that will get triggered on every tick.
   * 
   * @param tickListener Tick listener to add
   */
  public void addTickListener(TickListener tickListener) {
    tickListeners.add(tickListener);
  }

  /**
   * Remove a tick listener.
   * 
   * @param tickListener Tick listener to remove
   */
  public void removeTickListener(TickListener tickListener) {
    tickListeners.remove(tickListener);
  }

  /**
   * Alert tick listeners.
   */
  protected void alertTickListeners() {
    for (TickListener tickListener : tickListeners) {
      tickListener.tick();
    }
  }

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

}
