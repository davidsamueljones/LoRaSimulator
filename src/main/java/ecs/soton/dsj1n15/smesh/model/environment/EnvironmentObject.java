package ecs.soton.dsj1n15.smesh.model.environment;

import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.line.Line2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.SimplePolygon2D;

/**
 * Generic object that can be placed in an environment. Has a shape for LOS collision calculations,
 * an extra propagation model, and an AWT 'look' for displaying it.
 * 
 * @author David Jones (dsj1n15)
 */
public abstract class EnvironmentObject {

  protected Color fillColor = null;
  protected Color borderColor = null;

  /**
   * @return The graphics shape that represents the object
   */
  public abstract Shape getAwtShape();


  /**
   * Calculate the line of sight (LOS) path loss (PL) caused by a transmission passing from
   * transmitter to receiver. This should not include free space loss. If object begins at distance
   * ds and finishes at distance de LOS path loss should be calculated as LOSPL(de) - LOSPL(ds).
   * 
   * @param tx The transmitter
   * @param rx The receiver
   * @return The path loss in dbm
   */
  public abstract double getLOSPathLoss(Radio tx, Radio rx);

  /**
   * @return The colour to fill the shape
   */
  public Color getFillColor() {
    return fillColor;
  }

  /**
   * @return The colour to draw the shape border
   */
  public Color getBorderColor() {
    return borderColor;
  }

  /**
   * Find the length of the line passing through the object.
   * 
   * @param line The line passing through the object
   * @return The length of the part of the line passing through the object
   */
  public abstract double getPassThroughDistance(Line2D line);

  /**
   * Find the length of the line passing through the given polygon.
   * 
   * @param polygon The polygon being passed through
   * @param line The line passing through the polygon
   * @return The length of the part of the line passing through the polygon
   */
  public static double getPassThroughDistance(Polygon2D polygon, Line2D line) {
    List<Point2D> points = getPassThroughPoints(polygon, line);
    if (points.size() == 2) {
      return Point2D.distance(points.get(0), points.get(1));
    } else {
      return 0;
    }
  }

  /**
   * Find the points of a line that either intersect or are inside the object (if there are any).
   * Will either return no points or 2 points.
   * 
   * @param line Line passing through the object
   * @return A list of pass through points
   */
  public abstract List<Point2D> getPassThroughPoints(Line2D line);

  /**
   * Find the points of a line that either intersect or are inside a polygon (if there are any).
   * Will either return no points or 2 points.
   * 
   * @param polygon The polygon being passed through
   * @param line The line passing through the polygon
   * @return A list of pass through points
   */
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

  /**
   * Get the points where the line intersects with a shape (if there are any). Can return 0, 1 or 2
   * points.
   * 
   * @param line Line passing through the object
   * @return The intersects with the object
   */
  public abstract List<Point2D> getIntersects(Line2D line);

  /**
   * Get the points where a line intersects with a polygon (if there are any). Can return 0, 1 or 2
   * points.
   * 
   * @param polygon The polygon being passed through
   * @param line The line passing through the polygon
   * @return A list of intersects if they exist
   */
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

  /**
   * Convert a circle into a polygon so that processes such as discrete intersect finding can occur.
   * 
   * @param circle Circle to convert to a polygon
   * @return Polygon representing the circle
   */
  public static Polygon2D getCirclePolygon(Circle2D circle) {
    int res = (int) Math.ceil(circle.length() / 5);
    SimplePolygon2D polygon = new SimplePolygon2D(circle.asPolyline(res));
    return polygon;
  }

}
