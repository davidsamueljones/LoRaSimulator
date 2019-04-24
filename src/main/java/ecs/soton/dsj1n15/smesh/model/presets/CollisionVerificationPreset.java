package ecs.soton.dsj1n15.smesh.model.presets;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.controller.Event;
import ecs.soton.dsj1n15.smesh.controller.MoveEvent;
import ecs.soton.dsj1n15.smesh.controller.ReceiveTestCheckEvent;
import ecs.soton.dsj1n15.smesh.controller.TransmissionEvent;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.Environment.FreeSpaceModelType;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.TestDataPacket;
import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.ReceiveListener;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.MetadataStatus;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;

/**
 * Three nodes where preset events are scheduled such that all collision scenarios are tested.
 * 
 * @author David Jones (dsj1n15)
 */
public class CollisionVerificationPreset extends Preset {

  private final LoRaCfg cfg;
  private boolean passed = false;

  /**
   * Create a new verification preset using DR1.
   */
  public CollisionVerificationPreset() {
    this(LoRaCfg.getDataRate1());
  }

  /**
   * Create a new verification preset using the provided configuration.
   * 
   * @param cfg LoRa configuration to use
   */
  public CollisionVerificationPreset(LoRaCfg cfg) {
    this.cfg = cfg;
    generate();
  }

  @Override
  public void generate() {
    Cloner cloner = new Cloner();

    double z = DEFAULT_NODE_Z;
    // Empty environment
    environment = new Environment(FreeSpaceModelType.EFSPL);

    // Node 1
    LoRaRadio nodeA = generateLoRaRadio(1, cloner.deepClone(cfg));
    nodeA.setX(0);
    nodeA.setY(0);
    nodeA.setZ(z);
    environment.addNode(nodeA);
    // Node 2
    LoRaRadio nodeC = generateLoRaRadio(2, cloner.deepClone(cfg));
    nodeC.setX(1200);
    nodeC.setY(0);
    nodeC.setZ(z);
    environment.addNode(nodeC);
    // Node 3
    LoRaRadio nodeB = generateLoRaRadio(3, cloner.deepClone(cfg));
    nodeB.setX(1800);
    nodeB.setY(0);
    nodeB.setZ(z);
    environment.addNode(nodeB);

    // *** Create test events
    events.clear();
    Packet firstPacket;
    Packet secondPacket;
    int start;
    List<Pair<String, ReceiveTestCheckEvent>> testChecks = new ArrayList<>();
    ReceiveTestCheckEvent rtce;

    // *** Class D
    start = 0;
    // Position nodes for test
    addEvent(start + 0, new MoveEvent(nodeA, -1000, 0, false));
    addEvent(start + 0, new MoveEvent(nodeC, 0, 0, false));
    addEvent(start + 0, new MoveEvent(nodeB, 800, 0, false));
    firstPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeA, firstPacket));
    secondPacket = new TestDataPacket(128);
    int secondPacketStart = cfg.calculatePreambleTime() - (LoRaCfg.requiredPreambleTime(cfg) / 2);
    addEvent(start + secondPacketStart, new TransmissionEvent(nodeB, secondPacket));
    // Test verification
    rtce = new ReceiveTestCheckEvent(nodeC, null, Status.FAIL_COLLISION,
        MetadataStatus.FAIL_PREAMBLE_COLLISION);
    addEvent(start + secondPacketStart + cfg.calculatePacketAirtime(secondPacket.length) + 20,
        rtce);
    testChecks.add(new ImmutablePair<>("Class D", rtce));

    // *** Class A
    start = 10000;
    // Position nodes for test
    addEvent(start + 0, new MoveEvent(nodeA, -1000, 0, false));
    addEvent(start + 0, new MoveEvent(nodeC, 0, 0, false));
    addEvent(start + 0, new MoveEvent(nodeB, 1000, 0, false));
    firstPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeA, firstPacket));
    secondPacket = new TestDataPacket(128);
    addEvent(start + cfg.calculatePreambleTime() + 1, new TransmissionEvent(nodeB, secondPacket));
    // Test verification
    rtce = new ReceiveTestCheckEvent(nodeC, nodeA, Status.SUCCESS, MetadataStatus.SUCCESS);
    addEvent(start + cfg.calculatePacketAirtime(firstPacket.length) + 20, rtce);
    testChecks.add(new ImmutablePair<>("Class A", rtce));

    // *** Class B
    start = 20000;
    // Position nodes for test
    addEvent(start + 0, new MoveEvent(nodeA, -1000, 0, false));
    addEvent(start + 0, new MoveEvent(nodeC, 0, 0, false));
    addEvent(start + 0, new MoveEvent(nodeB, 950, 0, false));
    firstPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeA, firstPacket));
    secondPacket = new TestDataPacket(128);
    addEvent(start + cfg.calculatePreambleTime() + 1, new TransmissionEvent(nodeB, secondPacket));
    // Test verification
    rtce = new ReceiveTestCheckEvent(nodeC, nodeA, Status.SUCCESS, MetadataStatus.SUCCESS);
    addEvent(start + cfg.calculatePacketAirtime(firstPacket.length) + 20, rtce);
    testChecks.add(new ImmutablePair<>("Class B", rtce));

    // *** Class C
    start = 30000;
    // Position nodes for test
    addEvent(start + 0, new MoveEvent(nodeA, -1000, 0, false));
    addEvent(start + 0, new MoveEvent(nodeC, 0, 0, false));
    addEvent(start + 0, new MoveEvent(nodeB, 800, 0, false));
    firstPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeA, firstPacket));
    secondPacket = new TestDataPacket(128);
    addEvent(start + cfg.calculatePreambleTime() + 1, new TransmissionEvent(nodeB, secondPacket));
    // Test verification
    rtce = new ReceiveTestCheckEvent(nodeC, nodeA, Status.FAIL_CRC,
        MetadataStatus.FAIL_PAYLOAD_COLLISION);
    addEvent(start + cfg.calculatePacketAirtime(firstPacket.length) + 20, rtce);
    testChecks.add(new ImmutablePair<>("Class C", rtce));

    // *** Class E
    start = 40000;
    // Position nodes for test
    addEvent(start + 0, new MoveEvent(nodeA, -1000, 0, false));
    addEvent(start + 0, new MoveEvent(nodeC, 0, 0, false));
    addEvent(start + 0, new MoveEvent(nodeB, 1000, 0, false));
    firstPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeA, firstPacket));
    secondPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeB, secondPacket));
    // Test verification
    rtce = new ReceiveTestCheckEvent(nodeC, null, Status.FAIL_COLLISION,
        MetadataStatus.FAIL_PREAMBLE_COLLISION);
    addEvent(start + cfg.calculatePacketAirtime(firstPacket.length) + 20, rtce);
    testChecks.add(new ImmutablePair<>("Class E", rtce));

    // *** Class F
    start = 50000;
    // Position nodes for test
    addEvent(start + 0, new MoveEvent(nodeA, -800, 0, false));
    addEvent(start + 0, new MoveEvent(nodeC, 0, 0, false));
    addEvent(start + 0, new MoveEvent(nodeB, 1000, 0, false));
    firstPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeA, firstPacket));
    secondPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeB, secondPacket));
    // Test verification
    rtce = new ReceiveTestCheckEvent(nodeC, nodeA, Status.SUCCESS, MetadataStatus.SUCCESS);
    addEvent(start + cfg.calculatePacketAirtime(firstPacket.length) + 20, rtce);
    testChecks.add(new ImmutablePair<>("Class F", rtce));


    // *** Extra: (out of range verification)
    start = 60000;
    // Position nodes for test
    addEvent(start + 0, new MoveEvent(nodeA, -1000, 0, false));
    addEvent(start + 0, new MoveEvent(nodeC, 0, 0, false));
    addEvent(start + 0, new MoveEvent(nodeB, 2000, 0, false));
    firstPacket = new TestDataPacket(128);
    addEvent(start + 0, new TransmissionEvent(nodeA, firstPacket));
    secondPacket = new TestDataPacket(128);
    int secondStart = cfg.calculatePreambleTime() + 1;
    addEvent(start + secondStart, new TransmissionEvent(nodeB, secondPacket));
    // Test verification
    rtce = new ReceiveTestCheckEvent(nodeC, nodeA, Status.SUCCESS, MetadataStatus.SUCCESS);
    addEvent(start + secondStart + cfg.calculatePacketAirtime(secondPacket.length) + 20, rtce);
    testChecks.add(new ImmutablePair<>("Extra (OFRT)", rtce));

    // *** Check if tests have passed
    addEvent(100000, new Event() {
      @Override
      public void execute() {
        System.out.println("\n*** TEST RESULTS ***");
        int failed = 0;
        for (Pair<String, ReceiveTestCheckEvent> test : testChecks) {
          System.out.print("Test [" + test.getLeft() + "] : ");
          if (test.getRight().isTestSuccess()) {
            System.out.println("[SUCCESS]");
          } else {
            System.out.println("[FAILURE]");
            failed++;
          }
        }
        System.out.print(String.format("*** %d/%d tests passed! ", (testChecks.size() - failed),
            testChecks.size()));
        if (failed == 0) {
          System.out.print("[SUCCESS]");
        } else {
          System.out.print("[FAILURE]");
        }
        setPassed(failed == 0);
        System.out.println(" ***");
      }
    });
  }

  /**
   * 
   * @return The number of ms the tick should be run for
   */
  public int getTestLength() {
    return 100001;
  }

  /**
   * @param passed Whether the test passed when executed
   */
  private void setPassed(boolean passed) {
    this.passed = passed;
  }

  /**
   * @return Whether the the passed when it was executed, will be false if the test has not been
   *         run.
   */
  public boolean hasPassed() {
    return passed;
  }

}
