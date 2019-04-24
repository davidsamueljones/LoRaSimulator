package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.Protocol;
import ecs.soton.dsj1n15.smesh.radio.Radio;


/**
 * Protocol that attempts to switch bands and spreading factors by first sending announcements of
 * the band being switched to.
 * 
 * @author David Jones (dsj1n15)
 */
public class AdaptiveBroadcastProtocol extends Protocol<AdaptiveTickListener> {
  /** The band to use for low data rate communications */
  public final int lowRateDataRate;


  /**
   * Initialise the adapative broadcast protocol for the specified environment.
   * 
   * @param environment The environment for the protocol to control
   * @param lowRateDataRate The band to use for low data rate communications
   */
  public AdaptiveBroadcastProtocol(Environment environment, int lowRateDataRate) {
    super(environment);
    this.lowRateDataRate = 1;
    init();
  }

  @Override
  public void init() {
    for (Radio radio : environment.getNodes()) {
      if (radio instanceof LoRaRadio) {
        LoRaRadio loraRadio = (LoRaRadio) radio;
        AdaptiveTickListener listener = new AdaptiveTickListener(loraRadio, lowRateDataRate);
        loraRadio.addTickListener(listener);
        listeners.put(radio, listener);
      }
    }
  }

}
