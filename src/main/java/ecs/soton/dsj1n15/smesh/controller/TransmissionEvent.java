package ecs.soton.dsj1n15.smesh.controller;

import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;

public class TransmissionEvent implements Event {

  private final Radio radio;
  private final Packet packet;
  
  /**
   * Create a transmission event.
   * 
   * @param radio Radio to send transmission
   * @param packet Packet to send
   */
  public TransmissionEvent(Radio radio, Packet packet) {
    this.radio = radio;
    this.packet = packet;
  }
  
  @Override
  public void execute() {
    radio.send(packet);
  }

}
