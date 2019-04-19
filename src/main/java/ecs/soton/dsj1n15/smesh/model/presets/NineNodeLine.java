package ecs.soton.dsj1n15.smesh.model.presets;

import ecs.soton.dsj1n15.smesh.controller.TransmissionEvent;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.Environment.FreeSpaceModelType;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.radio.Packet;

/**
 * Simple preset with nine nodes in a line, no configuration.
 * 
 * @author David Jones (dsj1n15)
 */
public class NineNodeLine extends Preset {

  public NineNodeLine() {
    generate();
  }

  @Override
  public void generate() {
    double z = DEFAULT_NODE_Z;
    // Empty environment
    environment = new Environment(FreeSpaceModelType.EFSPL);

    // Node 1
    LoRaRadio node1 = generateLoRaRadio(1, LoRaCfg.getDataRate0());
    node1.setX(0);
    node1.setY(0);
    node1.setZ(z);
    environment.addNode(node1);
    // Node 2
    LoRaRadio node2 = generateLoRaRadio(2, LoRaCfg.getDataRate0());
    node2.setX(100);
    node2.setY(0);
    node2.setZ(z);
    environment.addNode(node2);
    // Node 3
    LoRaRadio node3 = generateLoRaRadio(3, LoRaCfg.getDataRate0());
    node3.setX(250);
    node3.setY(0);
    node3.setZ(z);
    environment.addNode(node3);
    // Node 4
    LoRaRadio node4 = generateLoRaRadio(4, LoRaCfg.getDataRate0());
    node4.setX(500);
    node4.setY(0);
    node4.setZ(z);
    environment.addNode(node4);
    // Node 5
    LoRaRadio node5 = generateLoRaRadio(5, LoRaCfg.getDataRate0());
    node5.setX(750);
    node5.setY(0);
    node5.setZ(z);
    environment.addNode(node5);
    // Node 6
    LoRaRadio node6 = generateLoRaRadio(6, LoRaCfg.getDataRate0());
    node6.setX(1000);
    node6.setY(0);
    node6.setZ(z);
    environment.addNode(node6);
    // Node 7
    LoRaRadio node7 = generateLoRaRadio(7, LoRaCfg.getDataRate0());
    node7.setX(1250);
    node7.setY(0);
    node7.setZ(z);
    environment.addNode(node7);
    // Node 8
    LoRaRadio node8 = generateLoRaRadio(8, LoRaCfg.getDataRate0());
    node8.setX(1500);
    node8.setY(0);
    node8.setZ(z);
    environment.addNode(node8);
    // Node 9
    LoRaRadio node9 = generateLoRaRadio(9, LoRaCfg.getDataRate0());
    node9.setX(1750);
    node9.setY(0);
    node9.setZ(z);
    environment.addNode(node9);

    // Create some events
    Packet packet = new Packet(8);
    addEvent(50, new TransmissionEvent(node1, packet));
    Packet packet2 = new Packet(120);
    addEvent(50, new TransmissionEvent(node2, packet2));

  }
}
