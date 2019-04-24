package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import math.geom2d.Point2D;

public class HeartbeatPacket extends Packet {
  public final Radio sender; // 1 byte (use ID)
  public final Point2D loc; // 3 bytes (use location at transmission)
  
  public HeartbeatPacket(Radio sender, Point2D loc) {
    this(0, sender, loc);
  }
  
  protected HeartbeatPacket(int payloadLen, Radio sender, Point2D loc) {
    super(payloadLen + getExpectedLength());
    this.sender = sender;
    this.loc = loc;
  }


  public static int getExpectedLength() {
    return 1 + 3;
  }
  
}
