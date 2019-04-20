package ecs.soton.dsj1n15.smesh.model.lora.protocol.events;

import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.ProtocolTickListener;

/**
 * Main control class for Event Protocol, only handles tracking of transmissions and receives for
 * user alerts and statistics.
 * 
 * @author David Jones (dsj1n15)
 */
class EventTickListener extends ProtocolTickListener {

  /**
   * Create a tick listener for controlling the protocol behaviour on the radio.
   * 
   * @param radio Radio to control
   */
  public EventTickListener(LoRaRadio radio) {
    super(radio);
  }

  @Override
  public void tick() {
    // Simulation metadata gathering
    trackTransmissions();

    // Run radio tasks
    trackSend();
    checkForSendFinish();
    checkForSync();
    checkForReceive();
  }

}
