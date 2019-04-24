package ecs.soton.dsj1n15.smesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import org.apache.commons.lang3.StringUtils;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive.AdaptiveBroadcastProtocol;
import ecs.soton.dsj1n15.smesh.model.presets.LargeDataBroadcastTest;
import ecs.soton.dsj1n15.smesh.model.presets.LargeDataBroadcastTest.EnvironmentMode;
import ecs.soton.dsj1n15.smesh.model.presets.Preset;
import ecs.soton.dsj1n15.smesh.view.EnvironmentDrawer;

public class AdaptiveProtocolTest {

  /**
   * The main method.
   *
   * @param args Passed arguments [Program uses no arguments]
   */
  public static void main(String[] args) {
    Debugger.setOutputEnabled(false);
    AdaptiveProtocolTest apt = new AdaptiveProtocolTest();
    apt.run(EnvironmentMode.NO_FOREST);
    apt.run(EnvironmentMode.ALL_FOREST);
    apt.run(EnvironmentMode.FOREST_HALF_SIDE);
    apt.run(EnvironmentMode.FOREST_HALF_MIDDLE);
  }

  public void run(EnvironmentMode em) {
    int unit = getExecutionUnit();
    long executionTime = getExecutionTime();
    // Create a runner
    EnvironmentRunner runner = new EnvironmentRunner();
    runner.setTimeUnit(unit);
    // Make the test environment
    Preset preset =
        new LargeDataBroadcastTest(getXCount(), getYCount(), getSeparation(), getRandom(), em);
    Environment environment = preset.getEnvironment();
    runner.setEnvironment(environment);
    // Instantiate the protocol handler
    AdaptiveBroadcastProtocol abp = new AdaptiveBroadcastProtocol(environment, getDataRate());
    // Execute runner
    runner.addUnitsToRun((long) Math.ceil(executionTime / (double) unit));
    while (runner.isRunning()) {
      // Wait until finished
    }

    // Print test environment to file
    EnvironmentDrawer.imgExport(environment);
    // Print results to file
    File outputFile;
    String prefix = StringUtils.lowerCase(em.toString());
    outputFile = makeTestFile(prefix + "_nbp_ts_filtered");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      abp.printTransmissionResults(pw, true);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_nbp_ts");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      abp.printTransmissionResults(pw, false);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_nbp_nr_filtered");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      abp.printNodeResults(pw, true);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_nbp_nr");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      abp.printNodeResults(pw, false);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    runner.getExecutionThread().interrupt();
  }

  public int getExecutionUnit() {
    return 10;
  }

  public long getExecutionTime() {
    return 1000 * 60 * 60 * 1;
  }

  public int getXCount() {
    return 6;
  }

  public int getYCount() {
    return 5;
  }

  public int getSeparation() {
    return 400;
  }

  public boolean getRandom() {
    return true;
  }

  public int getDataRate() {
    return 1;
  }

  private File makeTestFile(String name) {
    return new File(name + ".csv");
  }

}


