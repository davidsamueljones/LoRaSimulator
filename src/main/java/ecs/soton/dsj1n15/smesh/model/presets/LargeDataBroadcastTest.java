package ecs.soton.dsj1n15.smesh.model.presets;

import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
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
  public final int countX;
  public final int countY;
  public final int spacing;
  public final boolean random;

  /**
   * Create a large randomly generated grid of nodes.
   * 
   * @param countX Number of nodes in vertical
   * @param countY Number of nodes in horizontal 
   * @param spacing Spacing between nodes
   * @param random Whether to randomly move nodes so spacing isn't perfect
   */
  public LargeDataBroadcastTest(int countX, int countY, int spacing, boolean random) {
    this.countX = countX;
    this.countY = countY;
    this.spacing = spacing;
    this.random = random;
    generate();
  }

  @Override
  public void generate() {
    final LoRaCfg cfg = LoRaCfg.getDataRate1();
    Cloner cloner = new Cloner();
    double z = DEFAULT_NODE_Z;
    // Empty environment
    environment = new Environment(FreeSpaceModelType.EFSPL);

    for (int x = 0; x < countX; x++) {
      for (int y = 0; y < countY; y++) {
        LoRaRadio node = generateLoRaRadio(x * countX + y, cloner.deepClone(cfg));
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
      }
    }
    // Make some environmental objects
    Forest forest1 = new Forest(new Rectangle2D(-1250, -400, 700, 800), 0.4);
    environment.getEnvironmentObjects().add(forest1);
    Forest forest2 = new Forest(new Rectangle2D(-100, 400, 1500, 300), 0.75);
    environment.getEnvironmentObjects().add(forest2);
  }

}
