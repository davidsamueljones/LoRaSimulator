package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import ecs.soton.dsj1n15.smesh.radio.Packet;
import math.geom2d.Point2D;

public class DataAcceptPacket extends Packet {
  public final int senderID; // 1 byte
  public final int blockID; // 1 byte
  public final int header; // 1 byte
  public final Point2D loc; // 3 bytes
  public final double recvSNR; // 1 byte

  public DataAcceptPacket(int senderID, int blockID, int header, Point2D loc, double recvSNR) {
    super(getExpectedLength());
    this.senderID = senderID;
    this.blockID = blockID;
    this.header = header;
    this.loc = loc;
    this.recvSNR = recvSNR;
  }

  public static int getExpectedLength() {
    return 1 + 1 + 1 + 3 + 1;
  }

}
