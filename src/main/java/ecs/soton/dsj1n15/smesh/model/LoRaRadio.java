package ecs.soton.dsj1n15.smesh.model;

import java.util.Map;
import math.geom2d.Point2D;

public class LoRaRadio implements Radio {
  private static final int[] sensitivityBW125 = {-121, -124, -127, -130, -133, -135, -137};
  private static final int[] sensitivityBW250 = {-118, -122, -125, -128, -130, -132, -135};
  private static final int[] sensitivityBW500 = {-118, -122, -125, -128, -130, -132, -135};

  public static final int MAX_SENSITIVITY = sensitivityBW125[LoRaCfg.MAX_SF-LoRaCfg.MIN_SF];
  public static final int MIN_SENSITIVITY = sensitivityBW500[0];
  
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

  public int getRxSensitivity() {
    return getRXSensitivity(this.cfg);
  }
  
  public static int getRXSensitivity(LoRaCfg cfg) {
    int sfIdx = cfg.getSF() - LoRaCfg.MIN_SF;
    switch (cfg.getBW()) {
      case 125000:
        return sensitivityBW125[sfIdx];
      case 250000:
        return sensitivityBW250[sfIdx];
      case 500000:
        return sensitivityBW500[sfIdx];
      default:
        throw new IllegalArgumentException("Invalid bandwidth in configuration");
    }
  }

  public double getAntennaGain() {
    return antennaGain;
  }

  public void setAntennaGain(double antennaGain) {
    this.antennaGain = antennaGain;
  }

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

}
