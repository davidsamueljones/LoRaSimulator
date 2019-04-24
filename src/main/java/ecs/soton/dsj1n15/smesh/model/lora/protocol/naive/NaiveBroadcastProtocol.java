package ecs.soton.dsj1n15.smesh.model.lora.protocol.naive;

import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.Protocol;
import ecs.soton.dsj1n15.smesh.radio.Radio;

/**
 * ALOHA protocol that just sends data whenever it is available. Does a bit of basic CSMA to reduce
 * collisions where possible.
 * 
 * @author David Jones (dsj1n15)
 */
public class NaiveBroadcastProtocol extends Protocol<NaiveTickListener> {
  private final double dutyCycle;
  private final boolean enableCAD;

  /**
   * Initially the environment with the naive protocol.
   * 
   * @param environment The environment to initialise
   * @param dutyCycle The duty cycle to
   * @param enableCAD Whether channel sensing should be used
   */
  public NaiveBroadcastProtocol(Environment environment, double dutyCycle, boolean enableCAD) {
    super(environment);
    this.dutyCycle = dutyCycle;
    this.enableCAD = enableCAD;
    init();
  }

  @Override
  public void init() {
    // Attach protocol listener to each radio to act on each radio tick
    for (Radio radio : environment.getNodes()) {
      if (radio instanceof LoRaRadio) {
        LoRaRadio loraRadio = (LoRaRadio) radio;
        NaiveTickListener listener = new NaiveTickListener(loraRadio, dutyCycle, enableCAD);
        loraRadio.addTickListener(listener);
        listeners.put(radio, listener);
        // Extend preamble when CAD is used to improve reliability
        if (enableCAD) {
          loraRadio.getLoRaCfg().setPreambleSymbols(32);
        }
      }
    }
  }

}
