package ecs.soton.dsj1n15.smesh.model.environment;

import java.awt.Shape;
import java.util.List;
import ecs.soton.dsj1n15.smesh.model.Radio;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.line.Line2D;

public class Tree extends EnvironmentObject {

  private Circle2D shape;

  public Tree(Point2D pos) {

    // Environments at Tree Trunk Level

    shape = new Circle2D(pos, 0.5);
    fillColor = null;
    borderColor = null;
  }

  @Override
  public Shape getAwtShape() {
    return shape.asAwtShape();
  }

  @Override
  public double getPassThroughDistance(Line2D line) {
    return EnvironmentObject.getPassThroughDistance(EnvironmentObject.getCirclePolygon(shape),
        line);
  }

  @Override
  public double getLOSPathLoss(Radio a, Radio b) {
    return 0;
  }

  @Override
  public double getProximityPathLoss(Line2D line) {
    // FROM: An Empirical Propagation Model for Forest
    final double proximityPathLoss = 10;
    final double proximityRequirement = 1;
    double loss = 0;
    if (line.p1.distance(shape.center()) < proximityRequirement) {
      loss += proximityPathLoss;
    }
    if (line.p2.distance(shape.center()) < proximityRequirement) {
      loss += proximityPathLoss;
    }
    return loss;
  }


  @Override
  public List<Point2D> getIntersects(Line2D line) {
    return getIntersects(EnvironmentObject.getCirclePolygon(shape), line);
  }

  @Override
  public List<Point2D> getPassThroughPoints(Line2D line) {
    return getIntersects(EnvironmentObject.getCirclePolygon(shape), line);
  }

}
