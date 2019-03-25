package ecs.soton.dsj1n15.smesh.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.EnvironmentObject;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.radio.PartialReceive;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.Transmission;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.line.Line2D;
import math.geom2d.polygon.Rectangle2D;

public class EnvironmentDrawer {
  public static final double USEABLE_AREA = 0.95;
  public static final int MIN_GRID_SIZE = 5;
  public static final int MAX_GRID_SIZE = 200;

  /** The environment to draw */
  private final Environment environment;

  /** The offset in the view to start drawing from */
  private Point2D offset = new Point2D(0, 0);

  /** The fixed size of each grid square where each square is represents gridSize */
  private int gridSize = 20;

  /** How much each square represents (unitless but could be meters/kms) */
  private int gridUnit = 100;

  private boolean showTransmissions = true;
  private boolean showRoutes = false;
  private boolean showRSSIs = true;
  
  private final Map<Radio, Circle2D> nodeShapes = new LinkedHashMap<>();

  private Radio selectedNode = null;

  private Point2D curPos = null;

  /**
   * Instantiates a new environment drawer with an environment.
   *
   * @param tilePuzzle The tile puzzle object to draw
   */
  public EnvironmentDrawer(Environment environment) {
    this.environment = environment;
  }

  public boolean isShowTransmissions() {
    return showTransmissions;
  }

  public void setShowTransmissions(boolean showTransmissions) {
    this.showTransmissions = showTransmissions;
  }

  public boolean isShowRoutes() {
    return showRoutes;
  }

  public void setShowRoutes(boolean showRoutes) {
    this.showRoutes = showRoutes;
  }

  public boolean isShowRSSIs() {
    return showRSSIs;
  }

  public void setShowRSSIs(boolean showRSSIs) {
    this.showRSSIs = showRSSIs;
  }
  
  /**
   * Creates an image of a given size using the given settings and lines.
   *
   * @param size Size of image to create
   * @return Image of the environment
   */
  public BufferedImage getEnvironmentImage(Dimension size) {
    // Create a new image of given size
    BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_3BYTE_BGR);
    // Use BufferedImage's size and graphics object to draw the scaled environment
    drawEnvironment(image);
    return image;
  }

  /**
   * Draws the current environment onto a given buffered image - scaled to image.
   *
   * @param image Image to draw to
   */
  public void drawEnvironment(BufferedImage image) {
    drawEnvironment((Graphics2D) image.getGraphics(),
        new Dimension(image.getWidth(), image.getHeight()));
  }

  /**
   * Draws the current environment onto a given graphics object - scaled to the given dimensions.
   *
   * @param g Graphics object to draw to
   * @param d Dimension to scale tile puzzle to
   */
  public void drawEnvironment(Graphics2D g, Dimension d) {
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
    // Draw the grid background
    drawGrid(g, viewSpace);

    // Draw the environment if it exists
    if (environment != null) {
      drawEnvironment(g, viewSpace);
      drawRoutes(g);
      drawNodes(g, viewSpace);
    }

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
    for (EnvironmentObject object : environment.getEnvironmentObjects()) {
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
    if (curPos == null) {
      g.drawString("X: N/A", 10, 20);
      g.drawString("Y: N/A", 10, 40);
    } else {
      g.drawString(String.format("X: %.2f", curPos.x()), 10, 20);
      g.drawString(String.format("Y: %.2f", curPos.y()), 10, 40);
    }

    // g.drawString(String.format("Time: %s", environment == null ? "N/A" : environment.getTime()),
    // 100, 20);


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
    nodeShapes.clear();
    Rectangle2D viewArea = getCoordinateSpace(viewSpace);
    g.setStroke(new BasicStroke(2));
    int radius = 7;
    for (Radio node : environment.getNodes()) {
      if (viewArea.contains(node.getXY())) {
        Point p = getViewPosition(node.getXY());
        Circle2D s = new Circle2D(p.x, p.y, radius);
        g.setColor(Color.RED);
        g.fill(s.asAwtShape());
        g.setStroke(new BasicStroke(2));
        if (node.equals(selectedNode)) {
          g.setColor(new Color(0, 230, 0));
        } else {
          g.setColor(Color.BLACK);
        }
        g.draw(s.asAwtShape());
        if (showRSSIs && node.getCurrentTransmission() == null) {
          String strRssi = String.format("%d", (int) environment.getRSSI(node));
          FontMetrics fm = g.getFontMetrics();
          int strX = p.x - fm.stringWidth(strRssi) / 2;
          int strY = p.y - fm.getHeight();
          g.setColor(Color.WHITE);
          g.fillRoundRect(strX - 2, strY - fm.getHeight() + 3, fm.stringWidth(strRssi) + 4,
              fm.getHeight(), 5, 5);
          g.setColor(Color.LIGHT_GRAY);
          g.setStroke(new BasicStroke(1));
          g.drawRoundRect(strX - 2, strY - fm.getHeight() + 3, fm.stringWidth(strRssi) + 4,
              fm.getHeight(), 5, 5);
          g.setColor(Color.BLACK);
          g.drawString(strRssi, strX, strY);

        }
        nodeShapes.put(node, s);
      }
    }
  }

  public Map<Radio, Circle2D> getNodeShapes() {
    return nodeShapes;
  }

  private void drawRoute(Graphics2D g, Radio a, Radio b, Color baseColor) {
    Point pa = getViewPosition(a.getXY());
    Point pb = getViewPosition(b.getXY());
    Point mid = new Point(pa.x + (pb.x - pa.x) / 2, pa.y + (pb.y - pa.y) / 2);
    Line2D line = new Line2D(pa.x, pa.y, pb.x, pb.y);

    double snr = environment.getReceiveSNR(a, b);
    snr = Math.min(environment.getReceiveSNR(b, a), snr);
    int opacity = 0;
    if (snr >= b.getRequiredSNR()) {
      opacity = 255;
    } else {
      int leeway = 5;
      double strength = leeway + snr - b.getRequiredSNR();
      opacity = (int) Math.max(0, Math.min(255, strength * 255 / leeway));
    }
    Color color = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), opacity);


    g.setColor(color);
    g.draw(line.asAwtShape());

    String strSNR = String.format("%d", (int) snr);
    FontMetrics fm = g.getFontMetrics();
    int strX = mid.x - fm.stringWidth(strSNR) / 2;
    int strY = mid.y;
    g.setColor(new Color(255, 255, 255, opacity));
    g.fillRoundRect(strX - 2, strY - fm.getHeight() + 3, fm.stringWidth(strSNR) + 4, fm.getHeight(),
        5, 5);
    g.setColor(new Color(192, 192, 192, opacity));
    g.setStroke(new BasicStroke(1));
    g.drawRoundRect(strX - 2, strY - fm.getHeight() + 3, fm.stringWidth(strSNR) + 4, fm.getHeight(),
        5, 5);
    g.setColor(color);
    g.drawString(String.format("%d", (int) snr), strX, strY);

  }

  private static final float[] SOLID_LINE = null;
  private static final float[] LONG_DASH = {5.0f};

  private void drawRoutes(Graphics2D g) {
    Set<Pair<Radio, Radio>> routes = new LinkedHashSet<>();
    // Draw transmissions
    if (showTransmissions) {
      for (Radio radio : environment.getNodes()) {
        PartialReceive receive = radio.getTimeMap().get(environment.getTime());
        if (receive != null) {
          Color color = new Color(0, 0, 204);
          float dash[] = SOLID_LINE;
          Transmission synced = null;
          if (radio instanceof LoRaRadio) {
            synced = ((LoRaRadio) radio).getSyncedSignal();
          }
          if (synced != null) {
            if (synced != receive.transmission) {
              color = new Color(204, 0, 0);
            }
          } else {
            dash = LONG_DASH;
          }
          g.setStroke(
              new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash, 0));
          drawRoute(g, receive.transmission.sender, radio, color);
          routes.add(new ImmutablePair<>(receive.transmission.sender, radio));
        }
      }
    }
    // Draw routes
    if (showRoutes) {
      List<Radio> nodes = new ArrayList<>(environment.getNodes());
      for (int s = 0; s < nodes.size(); s++) {
        for (int t = s + 1; t < nodes.size(); t++) {
          Radio a = nodes.get(s);
          Radio b = nodes.get(t);
          if (!(routes.contains(new ImmutablePair<>(a, b))
              || routes.contains(new ImmutablePair<>(b, a)))) {
            Color color;
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10,
                LONG_DASH, 0));
            if (a.canCommunicate(b)) {
              color = Color.BLACK;
            } else if (a.canInterfere(b)) {
              color = Color.RED;
            } else {
              continue;
            }
            drawRoute(g, nodes.get(s), nodes.get(t), color);
          }
        }
      }
    }
  }

  public void centreView(Rectangle viewSpace) {
    if (environment != null && viewSpace.width > 0 && viewSpace.height > 0) {
      // Find the extremities
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double maxX = Double.MIN_VALUE;
      double maxY = Double.MIN_VALUE;
      for (Radio radio : environment.getNodes()) {
        if (radio.getX() < minX) {
          minX = radio.getX();
        }
        if (radio.getY() < minY) {
          minY = radio.getY();
        }
        if (radio.getX() > maxX) {
          maxX = radio.getX();
        }
        if (radio.getY() > maxY) {
          maxY = radio.getY();
        }
      }

      double width = (maxX - minX);
      double height = (maxY - minY);
      double minWidthScale = viewSpace.width / width;
      double minHeightScale = viewSpace.height / height;
      double minScale = Math.min(minWidthScale, minHeightScale);
      // Allow some boundaries
      minScale *= 0.8;
      int newSize = (int) Math.min(Math.max(MIN_GRID_SIZE, minScale * gridUnit), MAX_GRID_SIZE);
      // Re-centre and scale
      Point2D targetCentre =
          new Point2D(offset.x() + minX + width / 2, offset.y() + minY + height / 2);
      setGridSize(newSize);
      Point2D newCentre =
          getCoordinate(new Point(viewSpace.width / 2, viewSpace.height / 2));
      Point2D newOffset =
          new Point2D(targetCentre.x() - newCentre.x(), targetCentre.y() - newCentre.y());
      setOffset(newOffset);
    }
  }

  public Point2D getCurPos() {
    return curPos;
  }

  public void setCurPos(Point2D curPos) {
    this.curPos = curPos;
  }

  public void setSelectedNode(Radio selectedNode) {
    this.selectedNode = selectedNode;
  }

  public Radio getSelectedNode() {
    return selectedNode;
  }

  public Point2D getOffset() {
    return offset;
  }

  public void setOffset(Point2D offset) {
    this.offset = offset;
  }

  /**
   * @return The size each grid square is in pixels
   */
  public int getGridSize() {
    return gridSize;
  }

  /**
   * @param gridSize The size each grid square should be in pixels
   */
  public void setGridSize(int gridSize) {
    this.gridSize = gridSize;
  }

  /**
   * @return How much each grid square represents
   */
  public int getGridUnit() {
    return gridUnit;
  }

  /**
   * @param gridUnit How much each grid square should represent
   */
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
   * Export an Environment as an image (drawn using a EnvironmentDrawer).
   * 
   * @param puzzle Puzzle to draw
   */
  public static void imgExport(Environment environment) {
    EnvironmentDrawer drawer = new EnvironmentDrawer(environment);
    BufferedImage exportImage = drawer.getEnvironmentImage(new Dimension(2048, 2048));
    String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
    try {
      ImageIO.write(exportImage, "PNG", new File(date + "-Environment.png"));
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
