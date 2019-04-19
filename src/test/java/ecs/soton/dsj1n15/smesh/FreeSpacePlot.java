package ecs.soton.dsj1n15.smesh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.presets.Preset;
import ecs.soton.dsj1n15.smesh.model.presets.TwoNode;
import ecs.soton.dsj1n15.smesh.radio.ReceiveListener;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;

public class FreeSpacePlot {
  private Map<Double, Integer> exps = null;
  private Map<Double, Integer> recvs = null;
  private Map<Double, Double> snrs = null;
  private Map<Double, Double> rssis = null;

  /**
   * The main method.
   *
   * @param args Passed arguments [Program uses no arguments]
   */
  public static void main(String[] args) {
    Debugger.setOutputEnabled(false);
    List<LoRaCfg> cfgs = new ArrayList<>();
    cfgs.add(LoRaCfg.getDataRate5());
    cfgs.add(LoRaCfg.getDataRate4());
    cfgs.add(LoRaCfg.getDataRate3());
    cfgs.add(LoRaCfg.getDataRate2());
    cfgs.add(LoRaCfg.getDataRate1());
    cfgs.add(LoRaCfg.getDataRate0());

    FreeSpacePlot plot = new FreeSpacePlot();
    System.out.println("group,distance,height,density,weather,n,sf,pl,cr,pp,snr,rssi,ps");
    for (LoRaCfg cfg : cfgs) {
      plot.generatePoints(cfg);
      for (Double distance : plot.recvs.keySet()) {
        int exp = plot.exps.get(distance);
        int recv = plot.recvs.get(distance);
        double snr = plot.snrs.get(distance);
        double rssi = plot.rssis.get(distance);
        System.out.print("free_space_test,");
        System.out.printf("%f,", distance);
        System.out.printf("%d,", 0);
        System.out.printf("free_space,");
        System.out.printf("clear,");
        System.out.printf("%d,", exp);
        System.out.printf("%d,", cfg.getSF());
        System.out.printf("%d,", 120);
        System.out.printf("%d,", cfg.getCR());
        System.out.printf("%.3f,", recv / (double) exp);
        System.out.printf("%.1f,", snr);
        System.out.printf("%.1f,", rssi);
        System.out.printf("%.1f", snr + rssi);
        System.out.printf("\n");
      }
    }

  }

  public void generatePoints(LoRaCfg cfg) {
    exps = new LinkedHashMap<>();
    recvs = new LinkedHashMap<>();
    snrs = new LinkedHashMap<>();
    rssis = new LinkedHashMap<>();

    double distance = 30;
    int step = getStep();
    // Create a runner with 5ms granularity
    EnvironmentRunner runner = new EnvironmentRunner();
    runner.setTimeUnit(10);
    // Sweep through the distances, with a given number of attempts at each
    final int exp = getAttempts();
    final int maxFails = 5;
    int noRecvCount = 0;
    while (noRecvCount < maxFails) {
      int recv = 0;
      double avgSNR = 0;
      double avgRSSI = 0;
      for (int n = 0; n < exp; n++) {
        Preset preset = new TwoNode(distance, cfg);
        Environment environment = preset.getEnvironment();
        runner.clearEvents();
        runner.addEvents(preset.getEvents());
        TestListener listener = new TestListener();
        environment.getNode(2).addReceiveListener(listener);
        runner.setEnvironment(environment);
        for (int t = 0; t < 20000; t++) {
          runner.addUnitsToRun(1);
          while (runner.isRunning()) {
            // Wait for run to execute
          }
          if (listener.gotPacket) {
            recv++;
            avgSNR += Utilities.dbm2mw(listener.result.snr);
            avgRSSI += Utilities.dbm2mw(listener.result.rssi);
            break;
          }
        }
      }
      exps.put(distance, exp);
      recvs.put(distance, recv);
      snrs.put(distance, Utilities.mw2dbm(avgSNR / recv));
      rssis.put(distance, Utilities.mw2dbm(avgRSSI / recv));
      if (recv == 0) {
        noRecvCount++;
      }
      distance += step;
    }
    // Stop the test
    runner.getExecutionThread().interrupt();
  }

  public int getAttempts() {
    return 75;
  }

  public int getStep() {
    return 10;
  }


  class TestListener implements ReceiveListener {

    boolean gotPacket = false;
    ReceiveResult result = null;

    @Override
    public void receive(ReceiveResult result) {
      if (result.status == Status.SUCCESS) {
        gotPacket = true;
        this.result = result;
      }
    }

  }
}
