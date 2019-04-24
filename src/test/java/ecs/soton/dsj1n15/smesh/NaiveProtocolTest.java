package ecs.soton.dsj1n15.smesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import org.apache.commons.lang3.StringUtils;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.naive.NaiveBroadcastProtocol;
import ecs.soton.dsj1n15.smesh.model.presets.LargeDataBroadcastTest;
import ecs.soton.dsj1n15.smesh.model.presets.LargeDataBroadcastTest.EnvironmentMode;
import ecs.soton.dsj1n15.smesh.model.presets.Preset;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.view.EnvironmentDrawer;

public class NaiveProtocolTest {

  /**
   * The main method.<br>
   * Run the naive protocol for all environment modes on the configured large broadcast preset.
   *
   * @param args Passed arguments [Program uses no arguments]
   */
  public static void main(String[] args) {
    Debugger.setOutputEnabled(false);
    NaiveProtocolTest npt = new NaiveProtocolTest();
    npt.run(EnvironmentMode.NO_FOREST);
    npt.run(EnvironmentMode.ALL_FOREST);
    npt.run(EnvironmentMode.FOREST_HALF_SIDE);
    npt.run(EnvironmentMode.FOREST_HALF_MIDDLE);
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
    Preset preset =
        new LargeDataBroadcastTest(getXCount(), getYCount(), getSeparation(), getRandom(), em);
    Environment environment = preset.getEnvironment();
    runner.setEnvironment(environment);
    // Set the configurations to the one we want to test
    for (Radio radio : environment.getNodes()) {
      if (radio instanceof LoRaRadio) {
        LoRaRadio loraRadio = (LoRaRadio) radio;
        loraRadio.setLoRaCfg(LoRaCfg.getDataRate1());
      }
    }
    double dutyCycle = getDutyCycle();
    // Instantiate the protocol handler
    NaiveBroadcastProtocol nbp = new NaiveBroadcastProtocol(environment, dutyCycle, true);
    // runner.addEvents(preset.getEvents());
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
      nbp.printTransmissionResults(pw, true);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_nbp_ts");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      nbp.printTransmissionResults(pw, false);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_nbp_nr_filtered");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      nbp.printNodeResults(pw, true);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    outputFile = makeTestFile(prefix + "_nbp_nr");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      nbp.printNodeResults(pw, false);
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
   * @return The duty cycle to test with
   */
  public double getDutyCycle() {
    return 0.01;
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


