package ecs.soton.dsj1n15.smesh.model.lora.protocol;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;
import ecs.soton.dsj1n15.smesh.radio.Transmission;

/**
 * Protocol container for configuring and managing a protocol for a set of nodes.
 * 
 * @author David Jones (dsj1n15)
 */
public abstract class Protocol<T extends ProtocolTickListener> {

  /** Random object to use for all randomness */
  protected final Random r = Utilities.RANDOM;

  /** Listeners used by the protocol */
  protected final Map<Radio, T> listeners = new HashMap<>();

  protected final Environment environment;

  /**
   * Create a new protocol.
   * 
   * @param environment Environment protocol is acting in
   */
  public Protocol(Environment environment) {
    this.environment = environment;
  }

  /**
   * Initialise the protocol.
   */
  public abstract void init();


  /**
   * Print the results of all nodes being managed by the protocol.
   * 
   * @param filterWanted Filter by only those transmissions that were defined as wanted
   */
  public void printNodeResults(boolean filterWanted) {
    printNodeResults(null, filterWanted);
  }

  /**
   * Print the results of all nodes being managed by the protocol.
   * 
   * @param pw Printer writer to use for writing to file
   * @param filterWanted Filter by only those transmissions that were defined as wanted
   */
  public void printNodeResults(PrintWriter pw, boolean filterWanted) {
    Utilities.printAndWrite(pw, "Node Results");
    for (ProtocolTickListener listener : listeners.values()) {
      listener.printReceiveResults(pw, filterWanted);
    }
  }

  /**
   * Print the results of all transmissions that have been sent.
   * 
   * @param filterWanted Filter by only those transmissions that were defined as wanted
   */
  public void printTransmissionResults(boolean filterWanted) {
    printTransmissionResults(null, filterWanted);
  }

  /**
   * Print the results of all transmissions that have been sent.
   * 
   * @param pw Printer writer to use for writing to file
   * @param filterWanted Filter by only those transmissions that were defined as wanted
   */
  public void printTransmissionResults(PrintWriter pw, boolean filterWanted) {
    Utilities.printAndWrite(pw,
        "sender,start,airtime,pl,cf,sf,wantedCount,receivedWanted,"
            + "receivedUnwanted,failedMissed,failedNoPreamble,failedPreambleCollision,"
            + "failedPayloadCollision,failedWeakPayload\n");
    for (ProtocolTickListener transmitter : listeners.values()) {
      for (Transmission transmission : transmitter.sentTransmissions) {
        // Ignore if transmission isn't finished
        if (transmission.endTime > environment.getTime()) {
          continue;
        }
        // Accumulate who has received it
        int wantedCount = 0;
        int receivedWanted = 0;
        int receivedUnwanted = 0;
        int failedMissed = 0;
        int failedNoPreamble = 0;
        int failedPreambleCollision = 0;
        int failedPayloadCollision = 0;
        int failedWeakPayload = 0;
        for (ProtocolTickListener receiver : listeners.values()) {
          if (receiver == transmitter) {
            continue;
          }
          boolean wanted = receiver.wantedTransmissions.get(transmission);
          ReceiveResult receive = receiver.receivedData.get(transmission);
          // Accumulate received unwanted messages
          if (!wanted) {
            if (receive != null && receive.status == Status.SUCCESS) {
              receivedUnwanted++;
            }
            // Ignore unwanted messages if they are being filtered
            if (filterWanted) {
              continue;
            }
          } else {
            // Accumulate count of how many nodes wanted the message
            wantedCount++;
          }

          // Accumulate whether message was received or not
          if (receive == null) {
            failedMissed++;
          } else {
            switch (receive.metadataStatus) {
              case SUCCESS:
                receivedWanted++;
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
        LoRaRadio sender = transmitter.radio;
        LoRaCfg cfg = sender.getLoRaCfg();
        String str = String.format("%d,%d,%d,%d,%f,%d,%d,%d,%d,%d,%d,%d,%d,%d\n", sender.getID(),
            transmission.startTime, transmission.airtime, transmission.packet.length, cfg.getFreq(),
            cfg.getSF(), wantedCount, receivedWanted, receivedUnwanted, failedMissed,
            failedNoPreamble, failedPreambleCollision, failedPayloadCollision, failedWeakPayload);
        Utilities.printAndWrite(pw, str);
      }
    }
  }

}
