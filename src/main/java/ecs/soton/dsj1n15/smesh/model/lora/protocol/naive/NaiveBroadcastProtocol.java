package ecs.soton.dsj1n15.smesh.model.lora.protocol.naive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.model.dutycycle.DutyCycleManager;
import ecs.soton.dsj1n15.smesh.model.dutycycle.SingleTransmissionDutyCycleManager;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.Protocol;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.ProtocolTickListener;
import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult;
import ecs.soton.dsj1n15.smesh.radio.Transmission;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;

public class NaiveBroadcastProtocol extends Protocol {
  private final Environment environment;
  private final double dutyCycle;
  private final boolean enableCAD;

  private final List<NaiveTickListener> listeners = new ArrayList<>();

  public NaiveBroadcastProtocol(Environment environment, double dutyCycle, boolean enableCAD) {
    this.environment = environment;
    this.dutyCycle = dutyCycle;
    this.enableCAD = enableCAD;
    init();
  }

  /**
   * Attach a listener to each radio to act on each radio tick.
   */
  public void init() {
    for (Radio radio : environment.getNodes()) {
      if (radio instanceof LoRaRadio) {
        LoRaRadio loraRadio = (LoRaRadio) radio;
        NaiveTickListener listener = new NaiveTickListener(loraRadio);
        loraRadio.addTickListener(listener);
        listeners.add(listener);
        if (enableCAD) {
          loraRadio.getLoRaCfg().setPreambleSymbols(32);
        }
      }
    }
  }

  @Override
  public void printResults() {
    for (NaiveTickListener listener : listeners) {
      listener.printResults();
    }
  }

  /**
   * Main control class for Naive Broadcast Protocol.
   * 
   * @author David Jones (dsj1n15)
   */
  class NaiveTickListener extends ProtocolTickListener {
    /** Single band so a single duty cycle manager */
    private final DutyCycleManager dcm;

    /**
     * All transmissions that have been in the environment over all time with a mapping to whether
     * the data they contain would be wanted. Anything that is not wanted is either irrelevant or
     * overhead.
     */
    private final Map<Transmission, Boolean> seenTransmissions = new HashMap<>();

    /** The data that has actually been received by the radio */
    private final Set<ReceiveResult> receivedData = new HashSet<>();

    /** The next time to attempt a transmit */
    protected long nextTransmit;

    Map<Radio, Boolean> targets = new HashMap<>();
    
    /**
     * Create a tick listener for controlling the protocol behaviour on the radio.
     * 
     * @param radio Radio to control
     */
    public NaiveTickListener(LoRaRadio radio) {
      super(radio);
      this.dcm = new SingleTransmissionDutyCycleManager(environment.getTime(), dutyCycle);
      this.nextTransmit = r.nextInt(2000);
    }

    @Override
    public void tick() {
      // Simulation metadata gathering
      trackActivity();
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

    protected void trackActivity() {
      // Track if the node is active for activity plot
      long curTime = environment.getTime();
      activityMap.putIfAbsent(curTime, 0);
      if (radio.getCurrentTransmission() != null) {
        activityMap.put(curTime, activityMap.get(curTime) + 1);
      }
    }

    private void trackTransmissions() {
      // Track all transmissions and decide which ones are 'wanted' (simulator metadata for testing
      // purposes)
      for (Transmission transmission : environment.getTransmissions()) {
        if (transmission.sender != this.radio)
          if (!seenTransmissions.containsKey(transmission)) {
            boolean wanted = isTransmissionWanted(radio, transmission);
            seenTransmissions.put(transmission, wanted);
          }
      }
    }

    public void printResults() {
      int seenWanted = 0;
      for (Entry<Transmission, Boolean> seen : seenTransmissions.entrySet()) {
        if (seen.getValue() == true) {
          seenWanted++;
        }
      }
      int gotWanted = 0;
      for (ReceiveResult result : receivedData) {
        if (result.status == Status.SUCCESS && seenTransmissions.get(result.transmission)) {
          gotWanted++;
        }
      }
      System.out.println(
          String.format("[%8d] - Radio %d -  Got (of wanted): %d/%d - %f", environment.getTime(),
              radio.getID(), gotWanted, seenWanted, gotWanted / (double) seenWanted));
    }


    protected void attemptSend() {
      // Don't attempt if send not scheduled or currently transmitting
      if (nextTransmit > environment.getTime() && radio.getCurrentTransmission() == null) {
        return;
      }
      // Delay send if currently receiving
      if (radio.getSyncedSignal() != null) {
        nextTransmit = environment.getTime() + radio.getLoRaCfg().calculatePacketAirtime(120)
            + r.nextInt(10000);
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
    
    protected void trackSend() {
      currentTransmit = radio.getCurrentTransmission();
      // Determine who this message is actually trying to reach using metadata
      for (Radio target : environment.getNodes()) {
        if (target == this.radio) {
          continue;
        }
        boolean wanted = isTransmissionWanted(target, currentTransmit);
        targets.put(target, wanted);
      }
    }

    protected void attemptSendWithCAD() {
      // Don't attempt if send not scheduled, completing CAD or currently transmitting
      if (nextTransmit > environment.getTime() || radio.isCADMode()
          || radio.getCurrentTransmission() != null) {
        return;
      }
      // Delay send if CAD comes back positive or currently receiving
      if (startedCAD && radio.getCADStatus() || radio.getSyncedSignal() != null) {
        nextTransmit = environment.getTime() + radio.getLoRaCfg().calculatePacketAirtime(120)
            + r.nextInt(10000);
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

    protected void checkForReceive() {
      if (lastReceive != radio.getLastReceive()) {
        lastReceive = radio.getLastReceive();
        receivedData.add(lastReceive);
        boolean wanted = seenTransmissions.get(lastReceive.transmission);
        printReceive(wanted);
      }
    }



  }

}
