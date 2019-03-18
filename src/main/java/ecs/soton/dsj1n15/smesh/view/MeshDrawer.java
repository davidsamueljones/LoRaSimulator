package ecs.soton.dsj1n15.smesh.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import ecs.soton.dsj1n15.smesh.model.Mesh;
import ecs.soton.dsj1n15.smesh.model.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.environment.EnvironmentObject;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.conic.Ellipse2D;
import math.geom2d.line.Line2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Rectangle2D;

public class MeshDrawer {
  public static final double USEABLE_AREA = 0.95;
  public static final int MIN_GRID_SIZE = 5;
  public static final int MAX_GRID_SIZE = 200;

  /** The mesh to draw */
  private final Mesh mesh;

  /** The offset in the view to start drawing from */
  private Point2D offset = new Point2D(0, 0);

  /** The fixed size of each grid square where each square is represents gridSize */
  private int gridSize = 20;

  /** How much each square represents (unitless but could be meters/kms) */
  private int gridUnit = 100;

  private Map<LoRaRadio, Circle2D> nodeShapes;

  private LoRaRadio selectedNode = null;

  private Point2D curPos = null;

  public Point2D getCurPos() {
    return curPos;
  }

  public void setCurPos(Point2D curPos) {
    this.curPos = curPos;
  }

  public void setSelectedNode(LoRaRadio selectedNode) {
    this.selectedNode = selectedNode;
  }

  public LoRaRadio getSelectedNode() {
    return selectedNode;
  }

  /**
   * Return a copy, points aren't immmutable.
   * 
   * @return
   */
  public Point2D getOffset() {
    return offset;
  }

  public void setOffset(Point2D offset) {
    this.offset = offset;
  }

  /**
   * Instantiates a new mesh drawer with a mesh.
   *
   * @param tilePuzzle The tile puzzle object to draw
   */
  public MeshDrawer(Mesh mesh) {
    this.mesh = mesh;
  }

  /**
   * Creates an image of a given size using the given settings and lines.
   *
   * @param size Size of image to create
   * @return Image of the mesh
   */
  public BufferedImage getMeshImage(Dimension size) {
    // Create a new image of given size
    BufferedImage meshImage =
        new BufferedImage(size.width, size.height, BufferedImage.TYPE_3BYTE_BGR);
    // Use BufferedImage's size and graphics object to draw the scaled tile puzzle
    drawMesh(meshImage);
    return meshImage;
  }

  /**
   * Draws the current mesh onto a given buffered image - scaled to image.
   *
   * @param image Image to draw to
   */
  public void drawMesh(BufferedImage image) {
    drawMesh((Graphics2D) image.getGraphics(), new Dimension(image.getWidth(), image.getHeight()));
  }


  /**
   * Draws the current mesh onto a given graphics object - scaled to the given dimensions.
   *
   * @param g Graphics object to draw to
   * @param d Dimension to scale tile puzzle to
   */
  public void drawMesh(Graphics2D g, Dimension d) {
    Rectangle viewSpace = getViewSpace(d);

    // Save the current graphics settings
    Shape tempClip = g.getClip();
    AffineTransform tempAT = g.getTransform();
    RenderingHints tempRHs = g.getRenderingHints();

    // Apply a clipping mask
    g.setClip(viewSpace.x - 1, viewSpace.y - 1, viewSpace.width + 2, viewSpace.height + 2);
    // Index from the drawing area
    AffineTransform newAT = new AffineTransform();
    newAT.setToTranslation(viewSpace.getMinX(), viewSpace.getMinY());
    g.transform(newAT);
    // Enable Anti-Aliasing
    // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    // Draw background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, viewSpace.width, viewSpace.height);

    /* Handle drawing behaviour for grid */
    // Draw the grid background
    drawGrid(g, viewSpace);
    drawEnvironment(g, viewSpace);

    // Draw the routes
    final float dash[] = {5.0f};
    g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash, 0));
    List<LoRaRadio> nodes = new ArrayList<>(mesh.getNodes());



    for (int s = 0; s < nodes.size(); s++) {
      for (int t = s + 1; t < nodes.size(); t++) {
        drawRoute(g, nodes.get(s), nodes.get(t));
      }
    }
    // Draw the nodes
    drawNodes(g, viewSpace);

    // Draw the info box
    drawInfo(g, viewSpace);
    // Draw a border
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke(4));
    g.drawRect(0, 0, viewSpace.width, viewSpace.height);


    // Restore original graphic object settings
    g.setTransform(tempAT);
    g.setClip(tempClip);
    g.setRenderingHints(tempRHs);
  }

  private void drawEnvironment(Graphics2D g, Rectangle viewSpace) {
    AffineTransform transform = getScaleTransform();
    for (EnvironmentObject object : mesh.getEnvironment().getEnvironmentObjects()) {
      Shape shape = object.getAwtShape();
      shape = transform.createTransformedShape(shape);
      if (object.getFillColor() != null) {
        g.setColor(object.getFillColor());
        g.fill(shape);
      }
      if (object.getBorderColor() != null) {
        g.setColor(object.getBorderColor());
        g.draw(shape);
      }
    }
  }

  private AffineTransform getScaleTransform() {
    AffineTransform transform = new AffineTransform();
    transform.concatenate(AffineTransform.getTranslateInstance(-offset.x(), -offset.y()));
    transform.preConcatenate(AffineTransform.getScaleInstance(1 / getScale(), 1 / getScale()));
    return transform;
  }

  private void drawInfo(Graphics2D g, Rectangle viewSpace) {
    AffineTransform tempAT = g.getTransform();
    int height = 50;

    AffineTransform newAT = new AffineTransform();
    newAT.setToTranslation(0, viewSpace.height - height);
    g.transform(newAT);

    Rectangle bg = new Rectangle(0, 0, viewSpace.width, height);
    g.setColor(Color.WHITE);
    g.fill(bg);
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke(3));
    g.draw(bg);

    g.setColor(Color.BLACK);
    g.setFont(new Font("Monospaced", Font.BOLD, 12));
    g.drawString(String.format("X: %s", curPos == null ? "N/A" : curPos.x()), 10, 20);
    g.drawString(String.format("Y: %s", curPos == null ? "N/A" : curPos.y()), 10, 40);

    g.setTransform(tempAT);
  }

  private void drawGrid(Graphics2D g, Rectangle viewSpace) {
    Point start = getViewPosition(0, 0);
    start.x %= gridSize;
    start.y %= gridSize;
    g.setColor(Color.LIGHT_GRAY);
    for (int xi = -1; xi <= (viewSpace.width / gridSize + 1); xi++) {
      for (int yi = -1; yi <= (viewSpace.height / gridSize + 1); yi++) {
        int drawX = start.x + gridSize * xi;
        int drawY = start.y + gridSize * yi;
        g.drawRect(drawX, drawY, gridSize, gridSize);
      }
    }
    // Mark (0,0)
    g.setColor(Color.BLACK);
    Point point = getViewPosition(0, 0);
    g.fillOval(point.x - 2, point.y - 2, 4, 4);
  }

  private void drawNodes(Graphics2D g, Rectangle viewSpace) {
    nodeShapes = new LinkedHashMap<>();
    Rectangle2D viewArea = getCoordinateSpace(viewSpace);
    g.setStroke(new BasicStroke(2));
    int radius = 7;
    for (LoRaRadio node : mesh.getNodes()) {
      if (viewArea.contains(node.getX(), node.getY())) {
        Point p = getViewPosition(node.getX(), node.getY());
        Circle2D s = new Circle2D(p.x, p.y, radius);
        g.setColor(Color.RED);
        g.fill(s.asAwtShape());
        if (node.equals(selectedNode)) {
          g.setColor(Color.GREEN);
        } else {
          g.setColor(Color.BLACK);
        }
        g.draw(s.asAwtShape());

        nodeShapes.put(node, s);
      }
    }
  }

  public Map<LoRaRadio, Circle2D> getNodeShapes() {
    return nodeShapes;
  }

  private void drawRoute(Graphics2D g, LoRaRadio a, LoRaRadio b) {
    Point pa = getViewPosition(a.getX(), a.getY());
    Point pb = getViewPosition(b.getX(), b.getY());
    Point mid = new Point(pa.x + (pb.x - pa.x) / 2, pa.y + (pb.y - pa.y) / 2);

    Line2D line = new Line2D(pa.x, pa.y, pb.x, pb.y);
    double receivedPower = mesh.getEnvironment().getReceivedPower(a, b);
    int opacity = 0;
    if (receivedPower > LoRaRadio.MAX_SENSITIVITY) {
      opacity = 255;
    } else {
      int leeway = 5;
      double strength = leeway + receivedPower - LoRaRadio.MAX_SENSITIVITY;
      opacity = (int) Math.max(0, Math.min(255, strength * 255 / leeway));
    }
    
    g.setColor(new Color(0, 0, 0, opacity));
    g.draw(line.asAwtShape());
    g.drawString(String.format("%d", (int) receivedPower), mid.x, mid.y);

  }

  public int getGridSize() {
    return gridSize;
  }

  public void setGridSize(int gridSize) {
    this.gridSize = gridSize;
  }

  public int getGridUnit() {
    return gridUnit;
  }

  public void setGridUnit(int gridUnit) {
    this.gridUnit = gridUnit;
  }


  /**
   * Convert a point in coordinate space into view space.
   * 
   * @param p A point in coordinate space
   * @return The point transformed into view space
   */
  public Point getViewPosition(Point2D p) {
    return getViewPosition(p.x(), p.y());
  }

  /**
   * Convert a point in coordinate space into view space.
   * 
   * @param x X point in coordinate space
   * @param y Y point in coordinate space
   * @return The point transformed into view space
   */
  public Point getViewPosition(double x, double y) {
    int newX = (int) ((x - offset.x()) / getScale());
    int newY = (int) ((y - offset.y()) / getScale());
    return new Point(newX, newY);
  }

  /**
   * Convert a point in view space to coordinate space.
   * 
   * @param p A point in view space
   * @return The point transformed into coordinate space
   */
  public Point2D getCoordinate(Point p) {
    return getCoordinate(p.x, p.y);
  }

  /**
   * Convert a point in view space to coordinate space.
   * 
   * @param x X point in view space
   * @param y Y point in view space
   * @return The pixels transformed into coordinate space (as a point)
   */
  public Point2D getCoordinate(int x, int y) {
    double newX = (x * getScale()) + offset.x();
    double newY = (y * getScale()) + offset.y();
    return new Point2D(newX, newY);
  }

  /**
   * @return The current scaling factor defined as grid unit / grid size
   */
  public float getScale() {
    return gridUnit / (float) gridSize;
  }


  /**
   * Calculate the coordinates that are visible for a drawing area. Have a grid space leeway each
   * side in case drawing starts off the visible screen.
   * 
   * @param viewSpace The visible area
   * @return The area transformed into coordinate space
   */
  private Rectangle2D getCoordinateSpace(Rectangle viewSpace) {
    Point2D min = getCoordinate(-gridUnit, -gridUnit);
    Point2D max = getCoordinate(viewSpace.width + gridSize * 2, viewSpace.height + gridSize * 2);
    return new Rectangle2D(min.x(), min.y(), max.x() - min.x(), max.y() - min.y());
  }

  /**
   * Calculate the area where the grid should be drawn.
   * 
   * @param d The dimension of the object it is being drawn on.
   * @return A rectangle with position and size to act as the view space.
   */
  public static Rectangle getViewSpace(Dimension d) {
    int displaySize = getMaxSquareDisplay(d);
    return new Rectangle((d.width - displaySize) / 2, (d.height - displaySize) / 2, displaySize,
        displaySize);
  }

  /**
   * Export a Mesh as an image (drawn using a MeshDrawer).
   * 
   * @param puzzle Puzzle to draw
   */
  public static void imgExport(Mesh mesh) {
    MeshDrawer drawer = new MeshDrawer(mesh);
    BufferedImage exportImage = drawer.getMeshImage(new Dimension(2048, 2048));
    String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
    try {
      ImageIO.write(exportImage, "PNG", new File(date + "-Mesh.png"));
    } catch (IOException e) {
      System.err.println("Unable to export image");
    }
  }

  /**
   * Gets the maximum display area of a dimension if it were considered square.
   *
   * @param d The dimension
   * @return The size of either dimension (Width == Height)
   */
  public static int getMaxSquareDisplay(Dimension d) {
    return (int) Math.round(Math.min(d.width, d.height) * USEABLE_AREA);
  }

}
