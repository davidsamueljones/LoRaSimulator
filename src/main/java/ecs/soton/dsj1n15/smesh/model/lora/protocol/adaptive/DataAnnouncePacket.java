package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import math.geom2d.Point2D;

/**
 * Packet for announcing a broadcast.<br>
 * Has predictable length.
 * 
 * @author David Jones (dsj1n15)
 */
public class DataAnnouncePacket extends Packet {
  public final Radio sender; // 1 byte (ID)
  public final Radio target; // 1 byte (ID) [null for broadcast]
  public final int blockID; // 1 byte
  public final int packetCount; // 1 byte
  public final int avgPacketLength; // 1 byte
  public final Point2D loc; // 3 bytes (at transmission time)
  public final int dr; // < 1 byte
  public final int channel; // < 1 byte
  public final long startDelay; // 2 bytes

  /**
   * Create a new announcement packet.
   * 
   * @param sender The ID of the sending radio
   * @param target If the broadcast is actually meant to be a unicast, the radio ID
   * @param blockID The identifying ID for the broadcast
   * @param packetCount The number of packets that make up the broadcast
   * @param avgPacketLength Average length of packets that are part of broadcast
   * @param loc Location of sending radio
   * @param dr Datarate broadcast is going to be carried out on
   * @param channel Channel that broadcast is going to be carried out on
   * @param startDelay When the data broadcast is going to actually be carried out (from packet
   *        receive)
   */
  public DataAnnouncePacket(Radio sender, Radio target, int blockID, int packetCount,
      int avgPacketLength, Point2D loc, int dr, int channel, int startDelay) {
    super(getExpectedLength());
    // Identifying information
    this.sender = sender;
    this.target = target;
    this.blockID = blockID;
    // Announcement
    this.packetCount = packetCount;
    this.avgPacketLength = avgPacketLength;
    this.loc = loc;
    this.dr = dr;
    this.channel = channel;
    this.startDelay = startDelay;
  }

  /**
   * @return The length a pure announcement packet should be
   */
  public static int getExpectedLength() {
    return 11;
  }

}
