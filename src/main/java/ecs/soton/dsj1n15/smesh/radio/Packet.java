package ecs.soton.dsj1n15.smesh.radio;

/**
 * Simple packet class that is of a length. <br>
 * Can be instantiated but has no special receive behaviour.
 * 
 * @author David Jones (dsj1n15)
 */
public class Packet {
  /** Length of the created packet */
  public final int length;

  /**
   * Create a packet of the given length.
   * 
   * @param length Length of the packet to create
   */
  public Packet(int length) {
    this.length = length;
  }

}
