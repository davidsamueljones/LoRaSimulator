package ecs.soton.dsj1n15.smesh.model.lora.protocol;

import java.util.Random;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult;
import ecs.soton.dsj1n15.smesh.radio.TickListener;
import ecs.soton.dsj1n15.smesh.radio.Transmission;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;

public abstract class ProtocolTickListener implements TickListener {
  
  /** Random object to use for all randomness */
  protected final Random r = Utilities.RANDOM;
  
  /** Environment radio is in */
  protected final Environment environment;

  /** Radio providing the ticks */
  protected final LoRaRadio radio;

  protected Transmission currentTransmit = null;
  protected ReceiveResult lastReceive = null;
  protected Transmission syncReceive = null;

  /** Whether a CAD has been started */
  protected boolean startedCAD = false;

  public ProtocolTickListener(LoRaRadio radio) {
    this.radio = radio;
    this.environment = radio.getEnvironment();
  }

  protected boolean isTransmissionWanted(Radio rx, Transmission transmission) {
    double dist = rx.getXY().distance(transmission.sender.getXY());
    boolean dir = r.nextDouble() <= 0.333;
    double a;
    double b;
    if (dir) {
      a = -0.0025;
      b = 1000;
    } else {
      a = -0.02;
      b = 500;
    }
    double prob = 1 / (1 + Math.exp(-a * dist + a * b));
    return r.nextDouble() <= prob;
  }

  protected void checkForSendFinish() {
    // Alert on send finish
    if (currentTransmit != null && radio.getCurrentTransmission() == null) {
      System.out.println(String.format("[%8d] - Radio %-2d - Finished Sending Message!)",
          environment.getTime(), radio.getID()));
      currentTransmit = null;
    }
  }

  protected void checkForSync() {
    if (radio.getSyncedSignal() != null && syncReceive != radio.getSyncedSignal()) {
      System.out.println(
          String.format("[%8d] - Radio %-2d - Got Sync!!!", environment.getTime(), radio.getID()));
      syncReceive = radio.getSyncedSignal();
    }
  }

  protected void checkCAD() {
    if (startedCAD && !radio.isCADMode()) {
      System.out.println(String.format("[%8d] - Radio %-2d - CAD Result [%s]",
          environment.getTime(), radio.getID(), radio.getCADStatus()));
    }
  }

  protected void printReceive(boolean wanted) {
    String status = "";
    if (lastReceive.status == Status.COLLISION) {
      status = "collision on ";
    }
    if (lastReceive.status == Status.CRC_FAIL) {
      status = "CRC failure on ";
    }
    System.out.println(String.format("[%8d] - Radio %-2d - Got %s%s message from Radio %-2d!!!",
        environment.getTime(), radio.getID(), status, wanted ? "wanted" : "unwanted",
        lastReceive.transmission.sender.getID()));
  }
  
  protected Packet makeGenericDataPacket() {
    return new Packet(5 + r.nextInt(200));
  }

}
