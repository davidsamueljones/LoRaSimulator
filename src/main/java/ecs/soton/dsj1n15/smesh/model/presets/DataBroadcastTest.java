package ecs.soton.dsj1n15.smesh.model.presets;

import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.Forest;
import ecs.soton.dsj1n15.smesh.model.environment.Environment.FreeSpaceModelType;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import math.geom2d.polygon.Rectangle2D;

/**
 * Large fixed preset with a grid of 15 nodes and some forests.
 * 
 * @author David Jones (dsj1n15)
 */
public class DataBroadcastTest extends Preset {

  public DataBroadcastTest() {
    generate();
  }

  @Override
  public void generate() {
    final LoRaCfg cfg = LoRaCfg.getDataRate0();
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
    node2.setX(500);
    node2.setY(0);
    node2.setZ(z);
    environment.addNode(node2);
    // Node 3
    LoRaRadio node3 = generateLoRaRadio(3, cloner.deepClone(cfg));
    node3.setX(500);
    node3.setY(500);
    node3.setZ(z);
    environment.addNode(node3);
    // Node 4
    LoRaRadio node4 = generateLoRaRadio(4, cloner.deepClone(cfg));
    node4.setX(0);
    node4.setY(500);
    node4.setZ(z);
    environment.addNode(node4);
    // Node 5
    LoRaRadio node5 = generateLoRaRadio(5, cloner.deepClone(cfg));
    node5.setX(-500);
    node5.setY(500);
    node5.setZ(z);
    environment.addNode(node5);
    // Node 6
    LoRaRadio node6 = generateLoRaRadio(6, cloner.deepClone(cfg));
    node6.setX(-500);
    node6.setY(0);
    node6.setZ(z);
    environment.addNode(node6);
    // Node 7
    LoRaRadio node7 = generateLoRaRadio(7, cloner.deepClone(cfg));
    node7.setX(-500);
    node7.setY(-500);
    node7.setZ(z);
    environment.addNode(node7);
    // Node 8
    LoRaRadio node8 = generateLoRaRadio(8, cloner.deepClone(cfg));
    node8.setX(0);
    node8.setY(-500);
    node8.setZ(z);
    environment.addNode(node8);
    // Node 9
    LoRaRadio node9 = generateLoRaRadio(9, cloner.deepClone(cfg));
    node9.setX(500);
    node9.setY(-500);
    node9.setZ(z);
    environment.addNode(node9);
    // Node 10
    LoRaRadio node10 = generateLoRaRadio(10, cloner.deepClone(cfg));
    node10.setX(-1000);
    node10.setY(-500);
    node10.setZ(z);
    environment.addNode(node10);
    // Node 11
    LoRaRadio node11 = generateLoRaRadio(11, cloner.deepClone(cfg));
    node11.setX(-1000);
    node11.setY(0);
    node11.setZ(z);
    environment.addNode(node11);
    // Node 12
    LoRaRadio node12 = generateLoRaRadio(12, cloner.deepClone(cfg));
    node12.setX(-1000);
    node12.setY(500);
    node12.setZ(z);
    environment.addNode(node12);
    // Node 13
    LoRaRadio node13 = generateLoRaRadio(13, cloner.deepClone(cfg));
    node13.setX(1000);
    node13.setY(-500);
    node13.setZ(z);
    environment.addNode(node13);
    // Node 14
    LoRaRadio node14 = generateLoRaRadio(14, cloner.deepClone(cfg));
    node14.setX(1000);
    node14.setY(0);
    node14.setZ(z);
    environment.addNode(node14);
    // Node 15
    LoRaRadio node15 = generateLoRaRadio(15, cloner.deepClone(cfg));
    node15.setX(1000);
    node15.setY(500);
    node15.setZ(z);
    environment.addNode(node15);

    // Make some environmental objects
    Forest forest1 = new Forest(new Rectangle2D(0, 0, 500, 500), 1);
    environment.getEnvironmentObjects().add(forest1);
    Forest forest2 = new Forest(new Rectangle2D(550, 0, 200, 500), 0.5);
    environment.getEnvironmentObjects().add(forest2);
    Forest forest3 = new Forest(new Rectangle2D(800, 0, 200, 500), 0.25);
    environment.getEnvironmentObjects().add(forest3);
    Forest forest4 = new Forest(new Rectangle2D(0, 600, 1000, 200), 0.1);
    environment.getEnvironmentObjects().add(forest4);
  }

}
