package ecs.soton.dsj1n15.smesh.model.environment;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.model.Radio;
import ecs.soton.dsj1n15.smesh.model.Transmission;
import ecs.soton.dsj1n15.smesh.model.propogation.PlainEarthPropagationModel;
import ecs.soton.dsj1n15.smesh.model.propogation.PropagationModel;
import math.geom2d.line.Line2D;

public class Environment {

  /** A list of objects in the environment */
  private final Set<EnvironmentObject> objects = new LinkedHashSet<>();

  /** A list of all nodes in the environment */
  private final Set<Radio> nodes = new LinkedHashSet<>();

  /** The current time in the environment */
  private long time;

  public static double mw2dbm(double mw) {
    return 10 * Math.log10(mw);
  }

  public static double dbm2mw(double dbm) {
    return Math.pow(10, dbm / 10);
  }

  public Set<EnvironmentObject> getEnvironmentObjects() {
    return objects;
  }

  // public static void main(String[] args) {
  //
  //// Environment environment = new Environment();
  //// double z = 0.25;
  //// LoRaRadio sender = new LoRaRadio(1, LoRaCfg.getDefault());
  //// sender.setZ(z);
  //// environment.addNode(sender);
  //// LoRaRadio receiver = new LoRaRadio(2, LoRaCfg.getDefault());
  //// environment.addNode(receiver);
  //// LoRaRadio interferer = new LoRaRadio(3, LoRaCfg.getDefault());
  //// environment.addNode(interferer);
  ////
  ////
  //// System.out.println(getReceivePower(se d));
  //// Packet packet = new Packet(10);
  ////
  ////
  ////
  ////
  //// environment.setTime(500);
  //// Transmission tx = sender.send(packet);
  //// for (int i=0; i < 20; i++) {
  //// System.out.println(environment.getTransmissions().size());
  //// environment.clearFinishedTransmissions();
  //// long curTime = environment.getTime();
  //// environment.setTime(curTime + 100);
  //// }
  // }

  public double getReceiveSNR(Radio tx, Radio rx) {
    double strength = getReceivePower(tx, rx);
    double noise = getNoise(rx, tx.getCurrentTransmission());
    double snr = rx.validateSNR(strength - noise);
    return snr;
  }

  public double getRSSI(Radio rx) {
    return getNoise(rx, null);
  }

  public double getNoise(Radio rx, Transmission target) {
    // Calculate base noise level
    double noise = dbm2mw(rx.getNoiseFloor());
    // Sum the effect of any interfering transmissions on the target transmission
    for (Transmission interferer : getTransmissions()) {
      // Can't transmit and receive at the same time
      if (interferer.sender == rx) {
        continue;
      }
      // Not noise if its the signal we want or it doesn't interfere
      if (target == interferer || !interferer.sender.canInterfere(rx)) {
        continue;
      }
      double rp = dbm2mw(getReceivePower(interferer.sender, rx));
      noise += rp;
    }
    return mw2dbm(noise);
  }

  public double getReceivePower(Radio tx, Radio rx) {
    double txPow = tx.getTxPow() + tx.getAntennaGain() - tx.getCableLoss();
    double rxGain = rx.getAntennaGain() - rx.getCableLoss();
    return txPow - getAveragedPathLoss(tx, rx) + rxGain;
  }

  public double getAveragedPathLoss(Radio tx, Radio rx) {
    double a = getPathLoss(tx, rx);
    double b = getPathLoss(rx, tx);
    return Math.min(a, b);
  }

  public double getPathLoss(Radio tx, Radio rx) {
    // Calculate the line of sight between transmitter and receive
    Line2D los = new Line2D(tx.getXY(), rx.getXY());
    // Get the loss in free space
    PropagationModel freeSpaceModel = // new FreeSpacePropagationModel(rx.getFrequency());
        new PlainEarthPropagationModel(rx.getAntennaHeight(), tx.getAntennaHeight());
    double loss = freeSpaceModel.getPathLoss(los.length());

    // Add propagation effects of environmental objects
    for (EnvironmentObject object : objects) {
      loss += object.getLOSPathLoss(tx, rx);
      loss += object.getProximityPathLoss(los);
    }

    return loss;
  }


  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public Set<Radio> getNodes() {
    return nodes;
  }

  public Set<Transmission> getTransmissions() {
    HashSet<Transmission> transmissions = new LinkedHashSet<>();
    for (Radio radio : nodes) {
      if (radio.getCurrentTransmission() != null) {
        transmissions.add(radio.getCurrentTransmission());
      }
    }
    return transmissions;
  }

  public void addNode(Radio radio) {
    Environment current = radio.getEnvironment();
    if (!this.equals(current)) {
      if (current != null) {
        current.removeNode(radio);
      }
      radio.setEnvironment(this);
    }
    if (!nodes.contains(radio)) {
      nodes.add(radio);
    }
  }

  public void removeNode(Radio radio) {
    radio.setEnvironment(null);
    nodes.remove(radio);
  }

}
