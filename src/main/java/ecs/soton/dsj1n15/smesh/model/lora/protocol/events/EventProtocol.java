package ecs.soton.dsj1n15.smesh.model.lora.protocol.events;


import java.util.HashMap;
import java.util.Map;
import ecs.soton.dsj1n15.smesh.model.dutycycle.DutyCycleManager;
import ecs.soton.dsj1n15.smesh.model.dutycycle.SingleTransmissionDutyCycleManager;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.Protocol;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.ProtocolTickListener;
import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;

/**
 * Protocol that does nothing other than track and notify of radio behaviour.
 * 
 * @author David Jones (dsj1n15)
 */
public class EventProtocol extends Protocol {
  private final Environment environment;
  private final Map<Radio, EventTickListener> listeners = new HashMap<>();

  /**
   * Initially the environment with the naive protocol.
   * 
   * @param environment The environment to initialise
   * @param dutyCycle The duty cycle to
   * @param enableCAD Whether channel sensing should be used
   */
  public EventProtocol(Environment environment) {
    this.environment = environment;
    init();
  }

  @Override
  public void init() {
    // Attach protocol listener to each radio to act on each radio tick
    for (Radio radio : environment.getNodes()) {
      if (radio instanceof LoRaRadio) {
        LoRaRadio loraRadio = (LoRaRadio) radio;
        EventTickListener listener = new EventTickListener(loraRadio);
        loraRadio.addTickListener(listener);
        listeners.put(radio, listener);
      }
    }
  }

  @Override
  public void printResults() {
    for (EventTickListener listener : listeners.values()) {
      listener.printResults();
    }
  }

  /**
   * Main control class for Event Protocol.
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
      checkForSendFinish();
      checkForSync();
      checkForReceive();
    }

  }

}
