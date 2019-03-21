package ecs.soton.dsj1n15.smesh.model.environment;

import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import ecs.soton.dsj1n15.smesh.model.Radio;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.line.Line2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.SimplePolygon2D;

public abstract class EnvironmentObject {
  
  protected Color fillColor;
  protected Color borderColor;

  public abstract Shape getAwtShape();

  public abstract double getPassThroughDistance(Line2D line);
  
  public abstract List<Point2D> getPassThroughPoints(Line2D line);

  public abstract List<Point2D> getIntersects(Line2D line);
  
  public abstract double getLOSPathLoss(Radio tx, Radio rx);
 
  
  public double getProximityPathLoss(Line2D line) {
    return 0;
  }

  public static double getPassThroughDistance(Polygon2D polygon, Line2D line) {
    List<Point2D> points = getPassThroughPoints(polygon, line);
    if (points.size() == 2) {
      return Point2D.distance(points.get(0), points.get(1));
    } else {
      return 0;
    }
  }

  public static List<Point2D> getPassThroughPoints(Polygon2D polygon, Line2D line) {
    List<Point2D> points = new ArrayList<>(2);
    // Measure from line points if they are in the shape
    if (polygon.contains(line.p1)) {
      points.add(line.p1);
    }
    if (polygon.contains(line.p2)) {
      points.add(line.p2);
    }
    // Find intersects, there should be at most 2 unique points for a regular shape
    if (points.size() < 2) {
      for (Point2D point : getIntersects(polygon, line)) {
        if (!points.contains(point)) {
          points.add(point);
        }
      }
    }
    return points;
  }

  public static List<Point2D> getIntersects(Polygon2D polygon, Line2D line) {
    List<Point2D> intersects = new ArrayList<>(2);
    // Find intersects, there should be at most 2 unique points for a regular shape
    for (LineSegment2D seg : polygon.edges()) {
      Point2D intersect = line.intersection(seg);
      if (intersect != null && !intersects.contains(intersect)) {
        intersects.add(intersect);
      }
    }
    return intersects;
  }
  
  public static Polygon2D getCirclePolygon(Circle2D circle) {
    int res = (int) Math.ceil(circle.length() / 5);
    SimplePolygon2D polygon = new SimplePolygon2D(circle.asPolyline(res));
    return polygon;
  }
  

  
  public Color getFillColor() {
    return fillColor;
  }

  public Color getBorderColor() {
    return borderColor;
  }
  
  


}
