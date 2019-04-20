package ecs.soton.dsj1n15.smesh.model.lora.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
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
   * All transmissions that have been in the environment over all time with a mapping to whether the
   * data they contain would be wanted. Anything that is not wanted is either irrelevant or
   * overhead.
   */
  protected final Map<Transmission, Boolean> wantedTransmissions = new HashMap<>();

  /**
   * All transmissions that have been in the environment over all time with a mapping to their
   * corresponding receive result if it exists. If it does not exist then it can be assumed that the
   * receiver was busy whilst the transmission was ongoing.
   */
  private final Map<Transmission, ReceiveResult> receivedData = new HashMap<>();

  /* Previous state variables for checking change between ticks */
  protected Transmission currentTransmit = null;
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
   * @param transmission The transmission to check for
   * @return Whether the transmission is wanted
   */
  protected boolean isTransmissionWanted(Point2D rxPos, Transmission transmission) {
    double dist = rxPos.distance(transmission.sender.getXY());
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
    return isTransmissionWanted(rx.getXY(), transmission);
  }

  protected void trackTransmissions() {
    // Track all transmissions and decide which ones are 'wanted' (simulator metadata for testing
    // purposes)
    for (Transmission transmission : environment.getTransmissions()) {
      if (transmission.sender != this.radio)
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
  }

  /**
   * Prints whether the last transmission has finished sending.
   * 
   * @return Whether the last transmission has finished sending
   */
  protected boolean checkForSendFinish() {
    // Alert on send finish
    if (currentTransmit != null && radio.getCurrentTransmission() == null) {
      System.out.println(String.format("[%8d] - Radio %-2d - Finished Sending Message!)",
          environment.getTime(), radio.getID()));
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
      System.out.println(
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
      System.out.println(String.format("[%8d] - Radio %-2d - CAD Result [%s]",
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
    System.out.println(String.format("[%8d] - Radio %-2d - Got %s%s message from Radio %-2d!!!",
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
  protected Packet makeGenericDataPacket() {
    return new Packet(5 + r.nextInt(250));
  }

  public void printResults() {
    int seenWanted = 0;
    int gotWanted = 0;
    int failedMissed = 0;
    int failedNoPreamble = 0;
    int failedPreambleCollision = 0;
    int failedPayloadCollision = 0;
    int failedWeakPayload = 0;

    for (Entry<Transmission, Boolean> seen : wantedTransmissions.entrySet()) {
      // Ignore if transmission isn't finished
      if(seen.getKey().endTime > environment.getTime()) {
        continue;
      }
      // Ignore if not wanted
      if (!seen.getValue()) {
        //continue;
      }
      // Determine if the wanted transmission was received and if not, why not
      seenWanted++;
      ReceiveResult result = receivedData.get(seen.getKey());
      if (result == null) {
        failedMissed++;
      } else {
        switch (result.metadataStatus) {
          case SUCCESS:
            gotWanted++;
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
    System.out.println(String.format("Radio %2d -  %3d/%-3d - %3.0f%% - [%3d, %3d, %3d, %3d, %3d]",
        radio.getID(), gotWanted, seenWanted, gotWanted / (double) seenWanted * 100, failedMissed,
        failedNoPreamble, failedPreambleCollision, failedPayloadCollision, failedWeakPayload));
  }

  public void printReceivedData() {
    // System.out.println("rx,tx,wanted,success,snr,rssi");
    // System.out.println(
    // String.format("%d - Got (of wanted): %d/%d - %f", environment.getTime(),
    // radio.getID(), gotWanted, seenWanted, gotWanted / (double) seenWanted));
  }



}
