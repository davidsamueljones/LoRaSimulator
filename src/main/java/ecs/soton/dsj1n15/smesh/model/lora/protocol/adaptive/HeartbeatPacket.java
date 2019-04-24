package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import math.geom2d.Point2D;

/**
 * A packet that purely contains heartbeat information for a node, in this implementation this it is
 * purely a location but could be expanded.
 * 
 * @author David Jones (dsj1n15);
 *
 */
public class HeartbeatPacket extends Packet {
  public final Radio sender; // 1 byte (use ID)
  public final Point2D loc; // 3 bytes (use location at transmission)

  /**
   * Create a heartbeat with no payload.
   * 
   * @param sender
   * @param loc
   */
  public HeartbeatPacket(Radio sender, Point2D loc) {
    this(0, sender, loc);
  }

  /**
   * Create a heartbeat that has a payload.
   * 
   * @param payloadLen Length of payload
   * @param sender ID of sending radio
   * @param loc Location of sender
   */
  protected HeartbeatPacket(int payloadLen, Radio sender, Point2D loc) {
    super(payloadLen + getExpectedLength());
    this.sender = sender;
    this.loc = loc;
  }

  /**
   * @return The length a pure heartbeat packet should be
   */
  public static int getExpectedLength() {
    return 1 + 3;
  }

}
