package ecs.soton.dsj1n15.smesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import org.apache.commons.lang3.StringUtils;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive.AdaptiveBroadcastProtocol;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive.AdaptiveTickListener;
import ecs.soton.dsj1n15.smesh.model.presets.LargeDataBroadcastTest;
import ecs.soton.dsj1n15.smesh.model.presets.LargeDataBroadcastTest.EnvironmentMode;
import ecs.soton.dsj1n15.smesh.model.presets.Preset;
import ecs.soton.dsj1n15.smesh.view.EnvironmentDrawer;

public class AdaptiveProtocolTest {

  /**
   * The main method.<br>
   * Run the adaptive protocol for all environment modes on the configured large broadcast preset.
   *
   * @param args Passed arguments [Program uses no arguments]
   */
  public static void main(String[] args) {
    Debugger.setOutputEnabled(true);
    AdaptiveProtocolTest apt = new AdaptiveProtocolTest();
    apt.run(EnvironmentMode.NO_FOREST);
    apt.run(EnvironmentMode.ALL_FOREST);
    apt.run(EnvironmentMode.FOREST_HALF_SIDE);
    apt.run(EnvironmentMode.FOREST_HALF_MIDDLE);
  }

  /**
   * Run the test for a single environmental configuration (the forest objects to use).
   * 
   * @param em Environment configuration
   */
  public void run(EnvironmentMode em) {
    int unit = getExecutionUnit();
    long executionTime = getExecutionTime();
    // Create a runner
    EnvironmentRunner runner = new EnvironmentRunner();
    runner.setTimeUnit(unit);
    // Make the test environment
    Preset preset = new LargeDataBroadcastTest(null, getXCount(), getYCount(), getSeparation(),
        getRandom(), em);
    Environment environment = preset.getEnvironment();
    runner.setEnvironment(environment);
    // Instantiate the protocol handler, this will handle setting of the correct data rate
    AdaptiveTickListener.ANNOUNCEMENT_PACKET_COUNT = 1;
    AdaptiveTickListener.HEARTBEAT_ENABLED = true;
    AdaptiveTickListener.TARGET_CHEAT = false;
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
    outputFile = makeTestFile(prefix + "_apt_ts_filtered");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      abp.printTransmissionResults(pw, true);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_apt_ts");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      abp.printTransmissionResults(pw, false);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_apt_nr_filtered");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      abp.printNodeResults(pw, true);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_apt_nr");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      abp.printNodeResults(pw, false);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    runner.getExecutionThread().interrupt();
  }

  /**
   * @return The granularity of the test
   */
  public int getExecutionUnit() {
    return 10;
  }

  /**
   * @return How long to execute the test for (6 hours)
   */
  public long getExecutionTime() {
    return 1000 * 60 * 60 * 6;
  }

  /**
   * @return The number of radios in per horizontal
   */
  public int getXCount() {
    return 6;
  }

  /**
   * @return The number of radios per vertical
   */
  public int getYCount() {
    return 5;
  }

  /**
   * @return The distance between radios
   */
  public int getSeparation() {
    return 400;
  }

  /**
   * @return Whether radios should be placed randomly
   */
  public boolean getRandom() {
    return true;
  }

  /**
   * @return The data rate to use for the LDR band
   */
  public int getDataRate() {
    return 1;
  }

  /**
   * Create a test output file object (csv).
   * 
   * @param name Name of the file to create
   * @return A file object
   */
  private File makeTestFile(String name) {
    return new File(name + ".csv");
  }

}


