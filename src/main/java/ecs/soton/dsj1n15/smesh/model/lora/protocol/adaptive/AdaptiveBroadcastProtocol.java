package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.Protocol;
import ecs.soton.dsj1n15.smesh.radio.Radio;

public class AdaptiveBroadcastProtocol extends Protocol<AdaptiveTickListener> {

  public final int lowRateDataRate;


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
