package ecs.soton.dsj1n15.smesh.model.environment;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.model.Radio;
import ecs.soton.dsj1n15.smesh.model.Transmission;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.propogation.COST235OLPropagationModel;
import ecs.soton.dsj1n15.smesh.model.propogation.FreeSpacePropagationModel;
import ecs.soton.dsj1n15.smesh.model.propogation.PlainEarthPropagationModel;
import ecs.soton.dsj1n15.smesh.model.propogation.PropagationModel;
import math.geom2d.Point2D;
import math.geom2d.Shape2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.curve.AbstractContinuousCurve2D;
import math.geom2d.line.Line2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.line.StraightLine2D;
import math.geom2d.polygon.LinearRing2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Rectangle2D;
import math.geom2d.polygon.SimplePolygon2D;

public class Environment {

  /** A list of objects in the environment */
  private final Set<EnvironmentObject> objects = new LinkedHashSet<>();

  /** A list of all nodes in the environment */
  private final Set<Radio> nodes = new LinkedHashSet<>();

  private long time;

  private final Set<Transmission> transmissions = new LinkedHashSet<>();

  /** Temperature of the environment in degrees Celsius */
  // private double temperature = 11;

  public Set<EnvironmentObject> getEnvironmentObjects() {
    return objects;
  }

  public double getReceiveSNR(Radio tx, Radio rx) {
    double strength = getReceivePower(tx, rx);
    double noise = rx.getNoiseFloor();
    double snr = rx.validateSNR(strength - noise);
    return snr;
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
    PropagationModel freeSpaceModel =
        new PlainEarthPropagationModel(rx.getAntennaHeight(), tx.getAntennaHeight());
    double loss = freeSpaceModel.getPathLoss(los.length());

    // Add propagation effects of environmental objects
    for (EnvironmentObject object : objects) {
      loss += object.getLOSPathLoss(tx, rx);
      loss += object.getProximityPathLoss(los);
    }

    return loss;
  }

  public static void main(String[] args) {
    Radio radio = new LoRaRadio(1);
    System.out.println(radio.getNoiseFloor());
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
