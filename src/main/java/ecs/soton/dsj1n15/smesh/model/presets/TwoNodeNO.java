package ecs.soton.dsj1n15.smesh.model.presets;

import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.radio.Packet;

public class TwoNodeNO extends Preset {

  private final double distance;
  private final LoRaCfg cfg;

  public TwoNodeNO(double distance, LoRaCfg cfg) {
    this.distance = distance;
    this.cfg = cfg;
    generate();
  }

  @Override
  protected void generate() {
    Cloner cloner = new Cloner();

    double z = DEFAULT_NODE_Z;
    // Empty environment
    environment = new Environment();

    // Node 1
    LoRaRadio node1 = new LoRaRadio(1, cloner.deepClone(cfg));
    node1.setX(0);
    node1.setY(0);
    node1.setZ(z);
    environment.addNode(node1);
    // Node 2
    LoRaRadio node2 = new LoRaRadio(2, cloner.deepClone(cfg));
    node2.setX(distance);
    node2.setY(0);
    node2.setZ(z);
    environment.addNode(node2);

    // Create some events
    Packet packet = new Packet(120);
    node1.send(packet);
  }


}
