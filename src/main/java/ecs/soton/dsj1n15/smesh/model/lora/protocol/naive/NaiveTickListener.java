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
public class NaiveTickListener extends ProtocolTickListener {

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

    // Run generic radio tasks
    checkForSendFinish();
    checkForSync();
    checkForReceive();
    checkCAD();
    
    // Handle protocol tasks
    if (nextTransmit < environment.getTime()) {
      sendTestData();
    }
  }

  /**
   * Handle sending of a random amount of test data either with or without CAD. Will backoff if
   * either the channel is busy or not enough duty cycle limit is available.
   */
  protected void sendTestData() {
    Packet packet = makeTestDataPacket();
    // Attempt to send either with or without CAD
    SendStatus sendStatus;
    if (enableCAD) {
      sendStatus = attemptSendWithCAD(packet, dcm);
    } else {
      sendStatus = attemptSend(packet, dcm);
    }
    // Handle whether the message was sent
    switch (sendStatus) {
      case RADIO_BUSY:
        // Wait till radio has finished doing what it's doing
        // Could be CAD or a transmission
        break;
      case CAD_NEEDED:
        // CAD will have started, wait till complete
        break;
      case CHANNEL_BUSY:
        // Handle backoff behaviour, by just delaying the average packet length and some random
        // amount, could be more complex but should be good enough
        nextTransmit = environment.getTime() + radio.getLoRaCfg().calculatePacketAirtime(128)
            + r.nextInt(10000);
        break;
      case DUTY_CYCLE_LIMIT:
        // Delay transmission till it is allowed
        int airtime = radio.getLoRaCfg().calculatePacketAirtime(packet.length);
        this.nextTransmit = dcm.whenCanTransmit(environment.getTime(), airtime);
        break;
      case SUCCESS:
        // Message sent, nothing to worry about
        break;
      default:
        break;
    }
  }

}


