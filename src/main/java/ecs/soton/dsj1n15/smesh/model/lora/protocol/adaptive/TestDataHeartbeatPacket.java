package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import ecs.soton.dsj1n15.smesh.model.lora.protocol.TestData;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import math.geom2d.Point2D;

public class TestDataHeartbeatPacket extends HeartbeatPacket implements TestData {

  public TestDataHeartbeatPacket(int payloadLen, Radio sender, Point2D loc) {
    super(payloadLen, sender, loc);
  }

}
