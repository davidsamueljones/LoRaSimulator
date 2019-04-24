package ecs.soton.dsj1n15.smesh.model.lora.protocol;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.dutycycle.DutyCycleManager;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult;
import ecs.soton.dsj1n15.smesh.radio.TickListener;
import ecs.soton.dsj1n15.smesh.radio.Transmission;
import math.geom2d.Point2D;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;

/**
 * Generic tick listener for defining extra protocol behaviour for a radio.
 * 
 * @author David Jones (dsj1n15)
 */
public abstract class ProtocolTickListener implements TickListener {

  /** Random object to use for all randomness */
  protected final Random r = Utilities.RANDOM;

  /** Environment radio is in */
  protected final Environment environment;

  /** Radio providing the ticks */
  protected final LoRaRadio radio;

  /**
   * All transmissions that have been sent by the node.
   */
  public final Set<Transmission> sentTransmissions = new LinkedHashSet<>();

  /**
   * All transmissions that have been in the environment over all time with a mapping to whether the
   * data they contain would be wanted. Anything that is not wanted is either irrelevant or
   * overhead.
   */
  public final Map<Transmission, Boolean> wantedTransmissions = new HashMap<>();

  /**
   * All transmissions that have been in the environment over all time with a mapping to their
   * corresponding receive result if it exists. If it does not exist then it can be assumed that the
   * receiver was busy whilst the transmission was ongoing.
   */
  public final Map<Transmission, ReceiveResult> receivedData = new HashMap<>();

  /* Previous state variables for checking change between ticks */
  protected Transmission currentTransmit = null;
  protected Transmission lastTransmit = null;
  protected ReceiveResult lastReceive = null;
  protected Transmission syncReceive = null;
  protected boolean startedCAD = false;

  /**
   * Create a new protocol tick listener.
   * 
   * @param radio Radio being controlled
   */
  public ProtocolTickListener(LoRaRadio radio) {
    this.radio = radio;
    this.environment = radio.getEnvironment();
  }

  /**
   * Use the protocol wanting definition (whether a receiver is close enough) to determine whether a
   * transmission would be wanted at the given position. Use of position over radio object allows
   * for a transmitter to have a local idea of whether a transmission would be wanted.
   * 
   * @param rxPos The location of the receiving radio
   * @param txPos The location of the transmitting radio
   * @return Whether the transmission is wanted
   */
  protected boolean isTransmissionWanted(Point2D rxPos, Point2D txPos) {
    double dist = rxPos.distance(txPos);
    double a = 1;
    double b = 0;
    double d = 0.05;
    double c = 500;
    double prob = a + (b - a) / (1 + Math.pow(10, d * (c - dist)));
    return r.nextDouble() <= prob;
  }

  /**
   * Use {@link #isTransmissionWanted(Point2D rxPos, Transmission transmission)} to determine
   * whether the receiving radio would 'want' the transmission using the test's definition of
   * wanting.
   * 
   * @param rx The radio that is receiving the transmission
   * @param transmission The transmission to check for
   * @return Whether the transmission is wanted
   */
  protected boolean isTransmissionWanted(Radio rx, Transmission transmission) {
    return isTransmissionWanted(rx.getXY(), transmission.sender.getXY());
  }

  /**
   * Track all transmissions in the environment.
   */
  protected void trackTransmissions() {
    // Track all transmissions and decide which ones are 'wanted' (simulator metadata for testing
    // purposes)
    for (Transmission transmission : environment.getTransmissions()) {
      if (transmission.sender == this.radio) {
        continue;
      }
      if (!wantedTransmissions.containsKey(transmission)) {
        boolean wanted = isTransmissionWanted(radio, transmission);
        wantedTransmissions.put(transmission, wanted);
      }
    }
  }

  /**
   * Let the protocol track that the radio has sent something.
   */
  protected void trackSend() {
    currentTransmit = radio.getCurrentTransmission();
    if (currentTransmit != null) {
      sentTransmissions.add(currentTransmit);
    }
  }

  /**
   * Prints whether the last transmission has finished sending and records the last transmission.
   * 
   * @return Whether the last transmission has finished sending
   */
  protected boolean checkForSendFinish() {
    // Alert on send finish
    if (currentTransmit != null && radio.getCurrentTransmission() == null) {
      Debugger.println(String.format("[%8d] - Radio %-2d - Finished Sending Message!)",
          environment.getTime(), radio.getID()));
      lastTransmit = currentTransmit;
      currentTransmit = null;
      return true;
    }
    return false;
  }

  /**
   * Prints whether a new signal has been synchronised with.
   * 
   * @return Whether a new signal has been synchronised with
   */
  protected boolean checkForSync() {
    if (radio.getSyncedSignal() != null && syncReceive != radio.getSyncedSignal()) {
      Debugger.println(
          String.format("[%8d] - Radio %-2d - Got Sync!!!", environment.getTime(), radio.getID()));
      syncReceive = radio.getSyncedSignal();
      return true;
    }
    return false;
  }

  /**
   * Prints whether CAD has finished, and if it has come back true.
   * 
   * @return Whether CAD is true
   */
  protected boolean checkCAD() {
    if (startedCAD && !radio.isCADMode()) {
      Debugger.println(String.format("[%8d] - Radio %-2d - CAD Result [%s]",
          environment.getTime(), radio.getID(), radio.getCADStatus()));
      return radio.getCADStatus();
    }
    return false;
  }

  /**
   * Prints whether the last receive was successful.
   */
  protected boolean checkForReceive() {
    if (lastReceive != radio.getLastReceive()) {
      lastReceive = radio.getLastReceive();
      receivedData.put(lastReceive.transmission, lastReceive);
      if (lastReceive.isReceiverAware()) {
        boolean wanted = wantedTransmissions.get(lastReceive.transmission);
        printReceive(wanted);
      }
      return true;
    }
    return false;
  }

  /**
   * Prints the status of the last receive.
   * 
   * @param wanted Whether or not the message is of interest to the receiver.
   */
  protected void printReceive(boolean wanted) {
    String status = "";
    if (lastReceive.status == Status.FAIL_COLLISION) {
      status = "collision on ";
    }
    if (lastReceive.status == Status.FAIL_CRC) {
      status = "CRC failure on ";
    }
    Debugger.println(String.format("[%8d] - Radio %-2d - Got %s%s message from Radio %-2d!!!",
        environment.getTime(), radio.getID(), status, wanted ? "wanted" : "unwanted",
        lastReceive.transmission.sender.getID()));
  }

  /**
   * Make a generic data packet with some data in it that is potentially of interest to someone.<br>
   * Must be long enough to include information such as sender and position of sender so minimum is
   * 5 bytes.
   * 
   * @return A test packet between 5 and 255 bytes
   */
  protected Packet makeTestDataPacket() {
    return new TestDataPacket(5 + r.nextInt(250));
  }

  /**
   * Attempt to send a transmission without carrier sensing (ongoing receive checking does occur).
   * If a transmission is not successful then an appropriate status code is returned for failure
   * handling.
   * 
   * @param packet Packet to transmit
   * @param dcm Duty cycle manager that must be obeyed
   * @return Status code corresponding to send success
   */
  protected SendStatus attemptSend(Packet packet, DutyCycleManager dcm) {
    // Don't attempt if send not scheduled or currently transmitting
    if (radio.getCurrentTransmission() != null) {
      return SendStatus.RADIO_BUSY;
    }
    // Delay send if currently receiving
    if (radio.getSyncedSignal() != null) {
      return SendStatus.CHANNEL_BUSY;
    }
    // Check if duty cycle manager allows packet to be sent
    int airtime = radio.getLoRaCfg().calculatePacketAirtime(packet.length);
    if (!dcm.canTransmit(environment.getTime(), airtime)) {
      return SendStatus.DUTY_CYCLE_LIMIT;
    }
    // Send the message!
    Debugger.println(String.format("[%8d] - Radio %-2d - Sending Message...)",
        environment.getTime(), radio.getID()));
    dcm.transmit(environment.getTime(), airtime);
    radio.send(packet);
    trackSend();
    return SendStatus.SUCCESS;
  }


  /**
   * Attempt to send a transmission whilst utilising CAD to first do carrier sensing. If a
   * transmission is not successful then an appropriate status code is returned for failure
   * handling.
   * 
   * @param packet Packet to transmit
   * @param dcm Duty cycle manager that must be obeyed
   * @return Status code corresponding to send success
   */
  protected SendStatus attemptSendWithCAD(Packet packet, DutyCycleManager dcm) {
    // Don't attempt if send not scheduled, completing CAD or currently transmitting
    if (radio.isCADMode() || radio.getCurrentTransmission() != null) {
      return SendStatus.RADIO_BUSY;
    }
    // Don't send if CAD comes back positive or currently receiving
    if (startedCAD && radio.getCADStatus() || radio.getSyncedSignal() != null) {
      // Will need to do CAD again
      radio.clearCADStatus();
      startedCAD = false;
      return SendStatus.CHANNEL_BUSY;
    }
    // Check if duty cycle manager allows packet to be sent
    int airtime = radio.getLoRaCfg().calculatePacketAirtime(packet.length);
    if (!dcm.canTransmit(environment.getTime(), airtime)) {
      return SendStatus.DUTY_CYCLE_LIMIT;
    }
    // Do CAD if it hasn't been attempted yet
    if (!startedCAD) {
      Debugger.println(String.format("[%8d] - Radio %-2d - Starting CAD...",
          environment.getTime(), radio.getID()));
      radio.startCAD();
      startedCAD = true;
      return SendStatus.CAD_NEEDED;
    }
    // Must have completed CAD stage successfully, we can reset it for next time
    radio.clearCADStatus();
    startedCAD = false;
    // Send the message!
    Debugger.println(String.format("[%8d] - Radio %-2d - Sending Message...)",
        environment.getTime(), radio.getID()));
    dcm.transmit(environment.getTime(), airtime);
    radio.send(packet);
    trackSend();
    return SendStatus.SUCCESS;
  }

  /**
   * Enumeration of status's for send attempts.
   * 
   * @author David Jones (dsj1n15)
   */
  public enum SendStatus {
    SUCCESS, RADIO_BUSY, CHANNEL_BUSY, DUTY_CYCLE_LIMIT, CAD_NEEDED;
  }

  /**
   * Print the nodes receive results for all transmissions received. Only include those that are
   * test data packets as they won't be able to be filtered out later.
   * 
   * @param pw Print writer to use
   * @param filterWanted Whether to filter to only those wanted
   */
  public void printReceiveResults(PrintWriter pw, boolean filterWanted) {
    int wantedCount = 0;
    int receivedWanted = 0;
    int receivedUnwanted = 0;
    int failedMissed = 0;
    int failedNoPreamble = 0;
    int failedPreambleCollision = 0;
    int failedPayloadCollision = 0;
    int failedWeakPayload = 0;

    for (Entry<Transmission, Boolean> seen : wantedTransmissions.entrySet()) {
      Transmission transmission = seen.getKey();
      boolean wanted = seen.getValue();
      ReceiveResult receive = receivedData.get(seen.getKey());

      // Ignore if transmission isn't finished
      if (transmission.endTime > environment.getTime()) {
        continue;
      }
      // Ignore if transmission is not data packet
      if (!(transmission.packet instanceof TestData)) {
        continue;
      }
      // Ignore if not wanted (and filtering)
      if (!wanted) {
        if (receive != null && receive.status == Status.SUCCESS) {
          receivedUnwanted++;
        }
        // Ignore unwanted messages if they are being filtered
        if (filterWanted) {
          continue;
        }
      } else {
        // Accumulate count of wanted messages were received
        wantedCount++;
      }
      // Accumulate whether message was received or not
      if (receive == null) {
        failedMissed++;
      } else {
        switch (receive.metadataStatus) {
          case SUCCESS:
            if (wanted) {
              receivedWanted++;
            }
            break;
          case FAIL_NO_PREAMBLE:
            failedNoPreamble++;
            break;
          case FAIL_PAYLOAD_COLLISION:
            failedPayloadCollision++;
            break;
          case FAIL_PAYLOAD_WEAK:
            failedWeakPayload++;
            break;
          case FAIL_PREAMBLE_COLLISION:
            failedPreambleCollision++;
            break;
          default:
            break;
        }
      }
    }
    String str = String.format("%d,%d,%d,%d,%d,%d,%d,%d,%d\n", radio.getID(), wantedCount,
        receivedWanted, receivedUnwanted, failedMissed, failedNoPreamble, failedPreambleCollision,
        failedPayloadCollision, failedWeakPayload);
    Utilities.printAndWrite(pw, str);
  }

}
