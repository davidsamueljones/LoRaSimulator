package ecs.soton.dsj1n15.smesh.model.environment;

import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import ecs.soton.dsj1n15.smesh.model.Radio;
import ecs.soton.dsj1n15.smesh.model.propogation.ITURPropagationModel;
import ecs.soton.dsj1n15.smesh.model.propogation.PropagationModel;
import math.geom2d.Box2D;
import math.geom2d.Point2D;
import math.geom2d.line.Line2D;
import math.geom2d.polygon.Polygon2D;

public class Forest extends EnvironmentObject {

  private Polygon2D shape;
  private double density;

  /**
   * Create a forest.
   * 
   * @param shape The shape of the forest
   * @param density The density of the forest (empirical multiplier for LOS path loss)
   */
  public Forest(Polygon2D shape, double density) {
    this.shape = shape;
    this.density = density;
    fillColor = Color.getHSBColor(85.0f / 360.0f, 1.0f, (float) (0.6f + 0.39f * (1 - density)));
    borderColor = Color.BLACK;
  }

  @Override
  public Shape getAwtShape() {
    return shape.boundary().asAwtShape();
  }

  @Override
  public double getPassThroughDistance(Line2D line) {
    return getPassThroughDistance(shape, line);
  }

  @Override
  public List<Point2D> getPassThroughPoints(Line2D line) {
    return getPassThroughPoints(shape, line);
  }

  @Override
  public List<Point2D> getIntersects(Line2D line) {
    return getIntersects(shape, line);
  }

  @Override
  public double getLOSPathLoss(Radio tx, Radio rx) {
    if (tx.getFrequency() != rx.getFrequency()) {
      throw new IllegalArgumentException("Communicating radios are not on the same frequency");
    }

    double loss = 0;
    // Use the ITU-R model with an empirical multiplier
    PropagationModel model = new ITURPropagationModel(tx.getFrequency());
    // Calculate the line of sight between transmitter and receive
    Line2D los = new Line2D(tx.getXY(), rx.getXY());
    double objectDistance = getPassThroughDistance(los);
    if (objectDistance > 0) {
      List<Point2D> points = getPassThroughPoints(los);
      double objectStart = Double.MAX_VALUE;
      double objectEnd = Double.MIN_VALUE;
      for (Point2D point : points) {
        double temp = Point2D.distance(tx.getXY(), point);
        if (temp < objectStart) {
          objectStart = temp;
        }
        if (temp > objectEnd) {
          objectEnd = temp;
        }
      }
      loss += (model.getPathLoss(objectEnd));
      if (objectStart != 0 && objectStart != objectEnd) {
        loss -= model.getPathLoss(objectStart);
      }

    }

    return loss * density;
  }

}
