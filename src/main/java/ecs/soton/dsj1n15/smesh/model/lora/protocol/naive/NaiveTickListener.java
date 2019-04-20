package ecs.soton.dsj1n15.smesh.model.lora.protocol.naive;

import ecs.soton.dsj1n15.smesh.model.dutycycle.DutyCycleManager;
import ecs.soton.dsj1n15.smesh.model.dutycycle.SingleTransmissionDutyCycleManager;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.ProtocolTickListener;
import ecs.soton.dsj1n15.smesh.radio.Packet;


/**
 * Main control class for Naive Broadcast Protocol. ALOHA protocol that just sends data whenever it
 * is available. Does a bit of basic CSMA to reduce collisions where possible.
 * 
 * @author David Jones (dsj1n15)
 */
class NaiveTickListener extends ProtocolTickListener {

  /** Single band so a single duty cycle manager */
  private final DutyCycleManager dcm;

  /** Whether to enable CAD */
  boolean enableCAD;

  /** The next time to attempt a transmit */
  private long nextTransmit;

  /**
   * Create a tick listener for controlling the protocol behaviour on the radio.
   * 
   * @param radio Radio to control
   * @param dutyCycle Duty cycle to use for single band duty cycle manager
   * @param enableCAD Whether to enable CAD
   */
  public NaiveTickListener(LoRaRadio radio, double dutyCycle, boolean enableCAD) {
    super(radio);
    this.dcm = new SingleTransmissionDutyCycleManager(environment.getTime(), dutyCycle);
    this.enableCAD = enableCAD;
    this.nextTransmit = (long) (radio.getID() * LoRaCfg.getSymbolTime(radio.getLoRaCfg()));
  }

  @Override
  public void tick() {
    // Simulation metadata gathering
    trackTransmissions();

    // Run radio tasks
    checkForSendFinish();
    checkForSync();
    if (enableCAD) {
      checkCAD();
      attemptSendWithCAD();
    } else {
      attemptSend();
    }
    checkForReceive();
  }

  /**
   * Attempt to send a transmission. Delay to some point in the future if currently receiving
   * something.
   */
  protected void attemptSend() {
    // Don't attempt if send not scheduled or currently transmitting
    if (nextTransmit > environment.getTime() && radio.getCurrentTransmission() == null) {
      return;
    }
    // Delay send if currently receiving
    if (radio.getSyncedSignal() != null) {
      nextTransmit =
          environment.getTime() + radio.getLoRaCfg().calculatePacketAirtime(128) + r.nextInt(10000);
      return;
    }
    // Create a message to send
    Packet packet = makeGenericDataPacket();
    int airtime = radio.getLoRaCfg().calculatePacketAirtime(packet.length);
    // Check if duty cycle manager allows packet to be sent, if not update to next possible time
    if (!dcm.canTransmit(environment.getTime(), airtime)) {
      this.nextTransmit = dcm.whenCanTransmit(environment.getTime(), airtime);
      return;
    }

    // Send the message!
    System.out.println(String.format("[%8d] - Radio %-2d - Sending Message...)",
        environment.getTime(), radio.getID()));
    dcm.transmit(environment.getTime(), airtime);
    radio.send(packet);
    trackSend();
  }

  /**
   * Attempt to send a transmission whilst utilising CAD to first do carrier sensing. If now is not
   * a good time to send, delay the transmission to a random point in the future.
   */
  protected void attemptSendWithCAD() {
    // Don't attempt if send not scheduled, completing CAD or currently transmitting
    if (nextTransmit > environment.getTime() || radio.isCADMode()
        || radio.getCurrentTransmission() != null) {
      return;
    }
    // Delay send if CAD comes back positive or currently receiving
    if (startedCAD && radio.getCADStatus() || radio.getSyncedSignal() != null) {
      nextTransmit =
          environment.getTime() + radio.getLoRaCfg().calculatePacketAirtime(128) + r.nextInt(10000);
      // Will need to do CAD again
      radio.clearCADStatus();
      startedCAD = false;
      return;
    }
    // Create a message to send
    Packet packet = makeGenericDataPacket();
    int airtime = radio.getLoRaCfg().calculatePacketAirtime(packet.length);
    // Check if duty cycle manager allows packet to be sent, if not update to next possible time
    if (!dcm.canTransmit(environment.getTime(), airtime)) {
      this.nextTransmit = dcm.whenCanTransmit(environment.getTime(), airtime);
      return;
    }
    // Do CAD if it hasn't been attempted yet
    if (!startedCAD) {
      System.out.println(String.format("[%8d] - Radio %-2d - Starting CAD...",
          environment.getTime(), radio.getID()));
      radio.startCAD();
      startedCAD = true;
    } else {
      // Must have completed CAD stage successfully, we can reset it for next time
      radio.clearCADStatus();
      startedCAD = false;
      // Send the message!
      System.out.println(String.format("[%8d] - Radio %-2d - Sending Message...)",
          environment.getTime(), radio.getID()));
      dcm.transmit(environment.getTime(), airtime);
      radio.send(packet);
      trackSend();
    }
  }
}


