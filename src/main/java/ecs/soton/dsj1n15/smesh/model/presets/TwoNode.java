package ecs.soton.dsj1n15.smesh.model.presets;

import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.controller.TransmissionEvent;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.Forest;
import ecs.soton.dsj1n15.smesh.model.environment.Environment.FreeSpaceModelType;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.radio.Packet;
import math.geom2d.polygon.Rectangle2D;


/**
 * Simple preset with two nodes and a forest.
 * 
 * @author David Jones (dsj1n15)
 */
public class TwoNode extends Preset {

  private final double distance;
  private final LoRaCfg cfg;

  public TwoNode(double distance, LoRaCfg cfg) {
    this.distance = distance;
    this.cfg = cfg;
    generate();
  }

  @Override
  public void generate() {
    Cloner cloner = new Cloner();

    double z = DEFAULT_NODE_Z;
    // Empty environment
    environment = new Environment(FreeSpaceModelType.EFSPL);

    Forest forest = new Forest(new Rectangle2D(0, 100, 1000, 500), 1);
    environment.getEnvironmentObjects().add(forest);

    // Node 1
    LoRaRadio node1 = generateLoRaRadio(1, cloner.deepClone(cfg));
    node1.setX(0);
    node1.setY(0);
    node1.setZ(z);
    environment.addNode(node1);
    // Node 2
    LoRaRadio node2 = generateLoRaRadio(2, cloner.deepClone(cfg));
    node2.setX(distance);
    node2.setY(0);
    node2.setZ(z);
    environment.addNode(node2);

    // Create some events
    Packet packet = new Packet(120);
    addEvent(0, new TransmissionEvent(node1, packet));
  }


}
