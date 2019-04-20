package ecs.soton.dsj1n15.smesh.model.presets;

import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.controller.TransmissionEvent;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.Environment.FreeSpaceModelType;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.radio.Packet;

/**
 * Three nodes where events are scheduled such that Node A sends first and gets sync with Node B,
 * then Node C interferes with more powerful transmission. Node B should get a CRC fail.
 * 
 * @author David Jones (dsj1n15)
 */
public class PayloadCollisionPreset extends Preset {

  private final LoRaCfg cfg = LoRaCfg.getDataRate1();

  public PayloadCollisionPreset() {
    generate();
  }

  @Override
  public void generate() {
    Cloner cloner = new Cloner();

    double z = DEFAULT_NODE_Z;
    // Empty environment
    environment = new Environment(FreeSpaceModelType.EFSPL);

    // Node 1
    LoRaRadio node1 = generateLoRaRadio(1, cloner.deepClone(cfg));
    node1.setX(0);
    node1.setY(0);
    node1.setZ(z);
    environment.addNode(node1);
    // Node 2
    LoRaRadio node2 = generateLoRaRadio(2, cloner.deepClone(cfg));
    node2.setX(1200);
    node2.setY(0);
    node2.setZ(z);
    environment.addNode(node2);
    // Node 3
    LoRaRadio node3 = generateLoRaRadio(3, cloner.deepClone(cfg));
    node3.setX(1800);
    node3.setY(0);
    node3.setZ(z);
    environment.addNode(node3);


    // Create some events
    events.clear();
    Packet packet;
    packet = new Packet(128);
    addEvent(0, new TransmissionEvent(node1, packet));
    packet = new Packet(128);
    addEvent(250, new TransmissionEvent(node3, packet));
  }

}
