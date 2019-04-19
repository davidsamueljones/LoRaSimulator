package ecs.soton.dsj1n15.smesh;

import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.naive.NaiveBroadcastProtocol;
import ecs.soton.dsj1n15.smesh.model.presets.LargeDataBroadcastTest;
import ecs.soton.dsj1n15.smesh.model.presets.Preset;
import ecs.soton.dsj1n15.smesh.radio.Radio;

public class NaiveProtocolTest {

  /**
   * The main method.
   *
   * @param args Passed arguments [Program uses no arguments]
   */
  public static void main(String[] args) {
    Debugger.setOutputEnabled(false);
    NaiveProtocolTest npt = new NaiveProtocolTest();
    npt.run();
  }

  public NaiveProtocolTest() {

  }

  public void run() {
    int unit = 10;
    int executionTime = 10000;
    // Create a runner with 1ms granularity
    EnvironmentRunner runner = new EnvironmentRunner();
    runner.setTimeUnit(unit);
    // Make the test environment
    Preset preset = new LargeDataBroadcastTest(10, 10, 500, true);
    Environment environment = preset.getEnvironment();
    runner.setEnvironment(environment);
    // Set the configurations to the one we want to test
    for (Radio radio : environment.getNodes()) {
      if (radio instanceof LoRaRadio) {
        LoRaRadio loraRadio = (LoRaRadio) radio;
        loraRadio.setLoRaCfg(LoRaCfg.getDataRate0());
      }
    }
    double dutyCycle = 0.1;
    // Instantiate the protocol handler
    NaiveBroadcastProtocol nbp = new NaiveBroadcastProtocol(environment, dutyCycle, true);
    //runner.addEvents(preset.getEvents());
    // Execute runner
    runner.addUnitsToRun((long) Math.ceil(executionTime / (double) unit));
    while (runner.isRunning()) {
      // Wait until finished
    }
    nbp.dumpActivity();
    nbp.printResults();
    runner.getExecutionThread().interrupt();
  }
}


