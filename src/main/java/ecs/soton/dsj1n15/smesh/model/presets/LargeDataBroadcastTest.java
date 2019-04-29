package ecs.soton.dsj1n15.smesh.model.presets;

import java.util.Set;
import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.EnvironmentObject;
import ecs.soton.dsj1n15.smesh.model.environment.Forest;
import ecs.soton.dsj1n15.smesh.model.environment.Environment.FreeSpaceModelType;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import math.geom2d.polygon.Rectangle2D;

/**
 * Automatically generated configurable grid of nodes with a couple of forests.
 * 
 * @author David Jones (dsj1n15)
 */
public class LargeDataBroadcastTest extends Preset {
  public final LoRaCfg cfg;
  public final int countX;
  public final int countY;
  public final int spacing;
  public final boolean random;
  public final EnvironmentMode em;

  /**
   * Create a large randomly generated grid of nodes.
   * 
   * @param cfg The default LoRa configuration to use
   * @param countX Number of nodes in vertical
   * @param countY Number of nodes in horizontal
   * @param spacing Spacing between nodes
   * @param random Whether to randomly move nodes so spacing isn't perfect
   * @param em Environment mode
   */
  public LargeDataBroadcastTest(LoRaCfg cfg, int countX, int countY, int spacing, boolean random, EnvironmentMode em) {
    this.cfg = cfg;
    this.countX = countX;
    this.countY = countY;
    this.spacing = spacing;
    this.random = random;
    this.em = em;
    generate();
  }

  @Override
  public void generate() {
    Cloner cloner = new Cloner();
    double z = DEFAULT_NODE_Z;
    // Empty environment
    environment = new Environment(FreeSpaceModelType.EFSPL);
    int nodeID = 1;
    for (int x = 0; x < countX; x++) {
      for (int y = 0; y < countY; y++) {
        LoRaRadio node = generateLoRaRadio(nodeID, cloner.deepClone(cfg));
        node.setX(x * spacing);
        node.setY(y * spacing);
        if (random) {
          double xMod = (spacing * 0.35) * (Utilities.RANDOM.nextDouble() - 0.5);
          node.setX(node.getX() + xMod);
          double yMod = (spacing * 0.35) * (Utilities.RANDOM.nextDouble() - 0.5);
          node.setY(node.getY() + yMod);
        }
        node.setZ(z);
        environment.addNode(node);
        nodeID++;
      }
    }
    // Make some environmental objects
    addTestCaseForests(em, environment);
  }

  /**
   * 
   * @param em
   * @param environment
   */
  public void addTestCaseForests(EnvironmentMode em, Environment environment) {
    Set<EnvironmentObject> objects = environment.getEnvironmentObjects();
    objects.clear();
    int buffer = 100;
    int xDist = spacing * (countX - 1);
    int yDist = spacing * (countY - 1);

    switch (em) {
      case NO_FOREST:
        break;
      case ALL_FOREST:
        objects.add(new Forest(
            new Rectangle2D(-buffer, -buffer, xDist + buffer * 2, yDist + buffer * 2), 0.55));
        break;
      case FOREST_HALF_SIDE:
        objects.add(new Forest(
            new Rectangle2D(-buffer, -buffer, (xDist / 2.0) + buffer, yDist + buffer * 2), 0.55));
        break;
      case FOREST_HALF_MIDDLE:
        objects.add(new Forest(
            new Rectangle2D(xDist / 2.0 - xDist / 4.0, -buffer, xDist / 2.0, yDist + buffer * 2),
            0.55));
        break;
      default:
        break;
    }
  }

  /**
   * Options for placing different environments.
   * 
   * @author David Jones (dsj1n15)
   */
  public enum EnvironmentMode {
    NO_FOREST, ALL_FOREST, FOREST_HALF_SIDE, FOREST_HALF_MIDDLE;
  }

}
