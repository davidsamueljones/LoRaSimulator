package ecs.soton.dsj1n15.smesh.model;

import java.util.Map;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import math.geom2d.Point2D;

public class LoRaRadio extends Radio {
  public static final double MAX_SENSITIVITY = -137;
  
  /** A unique ID within the mesh */
  private final int id;

  /** The mesh that the node belongs to */
  private Mesh mesh;

  /** Radio configuration */
  protected final LoRaCfg cfg;

  /** Antenna gain */
  protected double antennaGain = 3;
  
  /** Cable loss */
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

 
  
  @Override
  public double getFrequency() {
    return cfg.getFreq();
  }

  @Override
  public double getAntennaHeight() {
    return getZ();
  }

  @Override
  public double getSensitivity() {
    return getNoiseFloor() + getRequiredSNR(cfg.getSF());
  }
  
  /**
   * Get the SNR required for demodulation using a given spreading factor.
   * 
   * @param sf LoRa spreading factor (7-12) 
   * @return The required SNR in dBm
   */
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

  /**
   * {@inheritDoc}
   * <br>
   * The maximum SNR of the LoRa demodulator (RFM95/SX1276/etc...) is limited to 10.
   */
  @Override
  public double validateSNR(double snr) {
    return Math.min(snr, 10);
  }
  
  @Override
  public Transmission send(Packet packet) {
    Environment environment = mesh.getEnvironment();
    Transmission transmission = new LoRaTransmission(this, packet, environment.getTime());

    return transmission;
  }
  
  public static void main(String[] args) {
    Environment environment = new Environment();
    Mesh mesh = new Mesh(1);
    mesh.setEnvironment(environment);
    
    Packet packet = new Packet();
    Radio sender = new LoRaRadio(1);
    //send
    Radio receiver = new LoRaRadio(2);
    sender.send(packet);
    
  }

  @Override
  public Packet recv() {
    // TODO Auto-generated method stub
    return null;
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
  
}
