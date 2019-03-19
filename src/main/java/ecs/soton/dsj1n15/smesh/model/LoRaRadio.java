package ecs.soton.dsj1n15.smesh.model;

import java.util.Map;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import math.geom2d.Point2D;

public class LoRaRadio implements Radio {
  public static final double MAX_SENSITIVITY = -137;
  
  /** A unique ID within the mesh */
  private final int id;

  /** The mesh that the node belongs to */
  private Mesh mesh;

  /** X Coordinate of node */
  private double x;

  /** Y Coordinate of node */
  private double y;

  /** Z Coordinate of node */
  private double z;

  /** Radio configuration */
  protected final LoRaCfg cfg;

  protected double antennaGain = 3;
  protected double cableLoss = 1.5;
  /** TODO: List of known neighbour information */
  // Map<Node, Integer> discoveredNeighbours;

  /** TODO: Routing table? */



  public LoRaRadio(int id) {
    this(id, new LoRaCfg());
  }

  public LoRaRadio(int id, LoRaCfg cfg) {
    this.id = id;
    this.cfg = cfg;
    cfg.setFreq(LoRaCfg.BAND_868_MHZ); // FIXME
    cfg.setBW(125000);
    cfg.setSF(12);
  }

  public LoRaCfg getLoRaCfg() {
    return cfg;
  }

  public int getID() {
    return id;
  }

  public Mesh getMesh() {
    return mesh;
  }

  public void setMesh(Mesh mesh) {
    this.mesh = mesh;
  }

  public double getX() {
    return x;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getY() {
    return y;
  }

  public void setY(double y) {
    this.y = y;
  }

  public double getZ() {
    return z;
  }

  public void setZ(double z) {
    this.z = z;
  }

  @Override
  public Point2D getXY() {
    return new Point2D(x, y);
  }
  
  @Override
  public double getFrequency() {
    return cfg.getFreq();
  }

  @Override
  public double getAntennaHeight() {
    return getZ();
  }

  public double getSensitivity() {
    return getNoiseFloor() + getRequiredSNR(cfg.getSF());
  }
  
  public static double getRequiredSNR(int sf) {
    return ((sf - LoRaCfg.MIN_SF) * -2.5) - 5;
  }
  

  @Override
  public double getAntennaGain() {
    return antennaGain;
  }

  public void setAntennaGain(double antennaGain) {
    this.antennaGain = antennaGain;
  }

  @Override
  public double getCableLoss() {
    return cableLoss;
  }

  public void setCableLoss(double cableLoss) {
    this.cableLoss = cableLoss;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof LoRaRadio))
      return false;
    LoRaRadio other = (LoRaRadio) obj;
    if (id != other.id)
      return false;
    return true;
  }

  @Override
  public void send(Packet packet) {
    Environment environment = mesh.getEnvironment();
    
    // TODO Auto-generated method stub
    
  }

  @Override
  public Packet recv() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getBandwidth() {
    return cfg.getBW();
  }

  @Override
  public double getNoiseFigure() {
    return 6;
  }

  @Override
  public int getTxPow() { 
    return cfg.getTxPow();
  }

  @Override
  public double getMaxSNR() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * <br>
   * The maximum SNR is limited as signal strength reduced before demodulation.
   */
  @Override
  public double validateSNR(double snr) {
    return Math.min(snr, 10);
  }

}
