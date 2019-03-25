package ecs.soton.dsj1n15.smesh.model.environment;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.propogation.PlainEarthPropagationModel;
import ecs.soton.dsj1n15.smesh.model.propogation.PropagationModel;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.Transmission;
import math.geom2d.line.Line2D;

public class Environment {

  /** A list of objects in the environment */
  private final Set<EnvironmentObject> objects = new LinkedHashSet<>();

  /** A list of all nodes in the environment */
  private final Set<Radio> nodes = new LinkedHashSet<>();

  /** The current time in the environment */
  private long time;

  /**
   * @return Reference to object
   */
  public Set<EnvironmentObject> getEnvironmentObjects() {
    return objects;
  }

  /**
   * Calculate the signal to noise ratio for a signal sent from the given transmitter to the given
   * receiver. A transmission must not necessarily be already occurring, the same value will be
   * returned either way.
   * 
   * @param tx Transmitter
   * @param rx Receiver
   * @return The SNR in dBm kept within the range of the radio possibilities
   */
  public double getReceiveSNR(Radio tx, Radio rx) {
    double strength = getReceivePower(tx, rx);
    double noise = getNoise(rx, tx.getCurrentTransmission());
    double snr = rx.validateSNR(strength - noise);
    return snr;
  }

  /**
   * Sum up the power in the listening channel; this is the sum of the noise floor, general
   * receiever noise and all signals. See {@link #getNoise(Radio rx, Transmission target)}.
   * 
   * @param rx The receiver
   * @return The RSSI in dBm
   */
  public double getRSSI(Radio rx) {
    return getNoise(rx, null);
  }

  /**
   * Sum up all noise that the receiver will see. This is the thermal noise floor, the general
   * receiver noise and noise from any signal that crosses into the listening channel. If the signal
   * is considered to not interfere for other reasons such as orthogonality it is also ignored. If a
   * target transmission is provided (not null), it will not be considered in any noise calculation.
   * 
   * @param rx The receiver
   * @param target Signal to not count as noise
   * @return The amount of noise in the channel in dBm
   */
  public double getNoise(Radio rx, Transmission target) {
    // Calculate base noise level
    double noise = Utilities.dbm2mw(rx.getNoiseFloor());
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
      double rp = Utilities.dbm2mw(getReceivePower(interferer.sender, rx));
      noise += rp;
    }
    return Utilities.mw2dbm(noise);
  }

  /**
   * Calculate how much power from a transmission will reach the receiver taking into account
   * transmission power, gains and path loss.
   * 
   * @param tx Transmitter
   * @param rx Receiver
   * @return The signal power in dBm as seen by the receiver
   */
  public double getReceivePower(Radio tx, Radio rx) {
    double txPow = tx.getTxPow() + tx.getAntennaGain() - tx.getCableLoss();
    double rxGain = rx.getAntennaGain() - rx.getCableLoss();
    return txPow - getAveragedPathLoss(tx, rx) + rxGain;
  }

  /**
   * Calculate the path loss after passing through objects in the environment. Averages two calls of
   * {@link #getPathLoss(Radio tx, Radio rx)} with the input order reversed. This creates a value
   * that is the average of path loss from transmitter to receiver and from receiver to transmitter.
   * so that the value will be the same in both directions. This is not necessarily realistic but
   * avoids unrealistic path differences.
   * 
   * @param tx A radio
   * @param rx A radio
   * @return The path loss in dbm in either direction
   */
  public double getAveragedPathLoss(Radio tx, Radio rx) {
    double a = getPathLoss(tx, rx);
    double b = getPathLoss(rx, tx);
    return Math.min(a, b);
  }

  /**
   * Calculate the path loss after passing through objects in the environment. This is the sum of
   * loss in free space and the sum of all propagation effects of said objects.
   * 
   * @param tx Transmitter
   * @param rx Receiver
   * @return The path loss in dBm from transmitter to receiver
   */
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
    }
    return loss;
  }

  /**
   * @return All nodes in the environment
   */
  public Set<Radio> getNodes() {
    return nodes;
  }

  /**
   * Add a new node to the environment, setting the nodes environment object automatically and
   * removing it from its old environment if it exists.
   * 
   * @param radio Radio to add as node
   */
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

  /**
   * Get the node with the corresponding ID. 
   * 
   * @param id ID of the node
   * @return Corresponding node
   */
  public Radio getNode(Integer id) {
    for (Radio node : nodes) {
      if (node.getID() == id) {
        return node;
      }
    }
    return null;
  }
  
  /**
   * Remove node from the environment, unsetting radios environment object automatically.
   * 
   * @param radio Radio to remove
   */
  public void removeNode(Radio radio) {
    radio.setEnvironment(null);
    nodes.remove(radio);
  }

  /**
   * Check every node in the environment to see if they are transmitting, collating any found
   * transmissions.
   * 
   * @return All transmissions in the environment
   */
  public Set<Transmission> getTransmissions() {
    HashSet<Transmission> transmissions = new LinkedHashSet<>();
    for (Radio radio : nodes) {
      if (radio.getCurrentTransmission() != null) {
        transmissions.add(radio.getCurrentTransmission());
      }
    }
    return transmissions;
  }

  /**
   * @return The current time (ms) in the environment
   */
  public long getTime() {
    return time;
  }

  /**
   * @param time The new time (ms) for the environment
   */
  public void setTime(long time) {
    this.time = time;
  }

  /**
   * @param time Value of time (ms) to add to the current time
   */
  public void addTime(long time) {
    this.time += time;
  }

}
