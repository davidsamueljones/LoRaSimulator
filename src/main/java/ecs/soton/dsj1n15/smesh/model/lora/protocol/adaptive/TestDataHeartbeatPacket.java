package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import ecs.soton.dsj1n15.smesh.model.lora.protocol.TestData;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import math.geom2d.Point2D;

/**
 * A heartbeat packet that can also contain an extra data payload.
 * 
 * @author David Jones (dsj1n15)
 *
 */
public class TestDataHeartbeatPacket extends HeartbeatPacket implements TestData {

  /**
   * Create a heartbeat packet that has some test data.
   * 
   * @param payloadLen Length of the payload on top of the heartbeat packet
   * @param sender ID of the sending radio
   * @param loc Location of sending radio
   */
  public TestDataHeartbeatPacket(int payloadLen, Radio sender, Point2D loc) {
    super(payloadLen, sender, loc);
  }

}
