package ecs.soton.dsj1n15.smesh.model.lora.protocol;

import ecs.soton.dsj1n15.smesh.radio.Packet;

/**
 * General packet marked as a test packet. Has no special receive behaviour but indicates generic
 * helpful data.
 * 
 * @author David Jones (dsj1n15)
 */
public class TestDataPacket extends Packet implements TestData {

  /**
   * Create a test packet of the given length.
   * 
   * @param length Length of packet
   */
  public TestDataPacket(int length) {
    super(length);
  }

   
}
