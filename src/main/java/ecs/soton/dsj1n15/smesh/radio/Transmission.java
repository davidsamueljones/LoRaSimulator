package ecs.soton.dsj1n15.smesh.radio;

/**
 * A transmission of a packet, has no addressing and is always global to the environment it is
 * placed in. Use the sender configuration to determine if the transmission can be received. from.
 * 
 * @author David Jones (dsj1n15)
 */
public class Transmission {

  /** The radio that sent the transmission, use its configuration */
  public final Radio sender;

  /** The packet that is contained in the transmission */
  public final Packet packet;

  /** When the packet transmission started */
  public final long startTime;

  /** When the packet finishes */
  public final long airtime;

  /** When the packet finishes */
  public final long endTime;

  /**
   * Create a new transmission.
   * 
   * @param sender The sending radio
   * @param packet The packet being sent
   * @param startTime The initial time of being sent.
   * @param airtime The full airtime of the packet.
   */
  public Transmission(Radio sender, Packet packet, long startTime, long airtime) {
    this.sender = sender;
    this.packet = packet;
    this.startTime = startTime;
    this.airtime = airtime;
    this.endTime = startTime + airtime;
  }

}
