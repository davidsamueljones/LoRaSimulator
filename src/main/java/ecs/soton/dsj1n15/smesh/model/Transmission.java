package ecs.soton.dsj1n15.smesh.model;

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


  public Transmission(Radio sender, Packet packet, long startTime, long airtime) {
    this.sender = sender;
    this.packet = packet;
    this.startTime = startTime;
    this.airtime = airtime;
    this.endTime = startTime + airtime;
  }

}
