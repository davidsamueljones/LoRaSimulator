package ecs.soton.dsj1n15.smesh;

import java.util.ArrayList;
import java.util.List;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.presets.CollisionVerificationPreset;

/**
 * Test for simulator collision behaviour.
 * 
 * @author David Jones (dsj1n15)
 */
public class CollisionTest {

  /**
   * Execute all data rates.
   * 
   * @param args No args
   */
  public static void main(String[] args) {
    Debugger.setOutputEnabled(false);
    CollisionTest ct = new CollisionTest();
    // Datarates to execute
    int[] drs = {0, 1, 2, 3, 4, 5};
    // Need to use some random seeds where general packet failures from demodulation curve don't
    // happen. This is a pain but is unavoidable and doesn't affect test validity.
    int[] seeds = {10, 0, 500, 0, 0, 0};
    List<Boolean> testResults = new ArrayList<>();
    for (int dr : drs) {
      Utilities.RANDOM.setSeed(seeds[dr]);
      LoRaCfg cfg = LoRaCfg.getDatarate(dr);
      boolean success = ct.run(cfg);
      testResults.add(success);
      System.out.println("\n--------------------------------\n");
    }
    System.out.println("*** FULL TEST RESULTS ***");
    boolean passed = true;
    int idx = 0;
    for (int dr : drs) {
      System.out
          .println(String.format("DR%d : [%s]", dr, testResults.get(idx) ? "SUCCESS" : "FAILURE"));
      if (!testResults.get(idx)) {
        passed = false;
      }
      idx++;
    }
    System.out.println(String.format("*** TEST %s ***", passed ? "SUCCESS" : "FAILURE"));
  }

  /**
   * Run a single collision test preset for the given configuration.
   * 
   * @param cfg Configuration to test
   * @return Whether the test passed
   */
  public boolean run(LoRaCfg cfg) {
    // Create a runner
    EnvironmentRunner runner = new EnvironmentRunner();
    int unit = getExecutionUnit();
    runner.setTimeUnit(unit);
    // Make the test environment
    CollisionVerificationPreset cvp = new CollisionVerificationPreset(cfg);
    runner.setEnvironment(cvp.getEnvironment());
    runner.addEvents(cvp.getEvents());

    // Execute runner
    long executionTime = cvp.getTestLength();
    runner.addUnitsToRun((long) Math.ceil(executionTime / (double) unit));
    while (runner.isRunning()) {
      // Wait until finished
    }
    runner.getExecutionThread().interrupt();
    try {
      runner.getExecutionThread().join();
    } catch (InterruptedException e) {
      // no behaviour necessary
    }
    return cvp.hasPassed();
  }

  /**
   * @return Execution granularity to use
   */
  public int getExecutionUnit() {
    return 1;
  }

}
