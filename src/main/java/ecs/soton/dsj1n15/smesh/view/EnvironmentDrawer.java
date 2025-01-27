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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.EnvironmentObject;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.TestData;
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

  private final Color GENERIC_RADIO_COLOR = new Color(200, 200, 200);
  private final Color GRID_COLOR = Color.LIGHT_GRAY;
  private final Color GENERAL_ROUTE_COLOR = Color.BLACK;
  private final Color INTERFERENCE_ROUTE_COLOR = new Color(204, 0, 0);
  private final Color SELECTED_NODE_COLOR = new Color(0, 204, 0);
  private final Color TEST_DATA_COLOR = new Color(0, 0, 204);
  private final Color OVERHEAD_COLOR = new Color(255, 150, 50);

  private static final float[] SOLID_LINE = null;
  private static final float[] SHORT_DASH = {2.0f};
  private static final float[] LONG_DASH = {5.0f};

  static final int INFO_BOX_HEIGHT = 50;

  /** The environment to draw */
  private final Environment environment;

  /** The offset in the view to start drawing from */
  private Point2D offset = new Point2D(0, 0);

  /** The fixed size of each grid square where each square is represents gridSize */
  private int gridSize = 20;

  /** How much each square represents (unitless but could be meters/kms) */
  private int gridUnit = 100;

  // Settings
  private boolean showTransmissions = true;
  private boolean showRoutes = false;
  private boolean showRSSIs = true;
  private boolean enableAntiAlias = false;

  private final Map<Radio, Circle2D> nodeShapes = new LinkedHashMap<>();
  private Radio selectedNode = null;
  private Point2D curPos = null;

  // Caches for intensive calculations
  private final Map<Radio, Double> rssiCache = new HashMap<>();
  private final Map<Pair<Radio, Radio>, Double> snrCache = new HashMap<>();

  // Lists of route information
  private List<Pair<Radio, Radio>> routes = new ArrayList<>();
  private List<Color> routeColors = new ArrayList<>();
  private List<float[]> routeStyles = new ArrayList<>();
  private List<Integer> routeOpacity = new ArrayList<>();
  private List<Double> routeSNRs = new ArrayList<>();

  /**
   * Instantiates a new environment drawer with an environment.
   *
   * @param environment The environment to draw
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

  public boolean isAntiAliasEnabled() {
    return enableAntiAlias;
  }

  public void setAntiAliasEnable(boolean enableAntiAlias) {
    this.enableAntiAlias = enableAntiAlias;
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
    // Only use a cache for a single draw, could be cheaper to keep between draws but doesn't seem
    // to be necessary, drawing is most expensive operation
    clearCaches();

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
    if (enableAntiAlias) {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    // Draw background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, viewSpace.width, viewSpace.height);
    // Draw the grid background
    drawGrid(g, viewSpace);

    // Draw the environment if it exists
    if (environment != null) {
      drawEnvironment(g, viewSpace);
      findRoutes();
      drawRoutes(g);
      drawRouteSNRs(g);
      drawNodes(g, viewSpace);
      if (showRSSIs) {
        drawRSSIs(g, viewSpace);
      }
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

  /**
   * Draw the background grid at the current scale and position.
   * 
   * @param g Graphics object to draw to
   * @param viewSpace The visible area
   */
  private void drawGrid(Graphics2D g, Rectangle viewSpace) {
    Point start = getViewPosition(0, 0);
    start.x %= gridSize;
    start.y %= gridSize;
    g.setColor(GRID_COLOR);
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

  /**
   * Draw all environment objects.
   * 
   * @param g Graphics object to draw to
   * @param viewSpace The visible area
   */
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

  /**
   * Draw the info box at the bottom of the view.
   * 
   * @param g Graphics object to draw to
   * @param viewSpace The visible area
   */
  private void drawInfo(Graphics2D g, Rectangle viewSpace) {
    AffineTransform tempAT = g.getTransform();
    // Shift drawing position to bottom of grid
    AffineTransform newAT = new AffineTransform();
    newAT.setToTranslation(0, viewSpace.height - INFO_BOX_HEIGHT);
    g.transform(newAT);

    Rectangle bg = new Rectangle(0, 0, viewSpace.width, INFO_BOX_HEIGHT);
    g.setColor(Color.WHITE);
    g.fill(bg);
    g.setColor(Color.BLACK);
    g.setStroke(new BasicStroke(3));
    g.draw(bg);

    g.setColor(Color.BLACK);
    g.setFont(new Font("Monospaced", Font.BOLD, 12));
    FontMetrics fm = g.getFontMetrics();
    if (curPos == null) {
      g.drawString("X: N/A", 10, 20);
      g.drawString("Y: N/A", 10, 40);
    } else {
      g.drawString(String.format("X: %.2f", curPos.x()), 10, 20);
      g.drawString(String.format("Y: %.2f", curPos.y()), 10, 40);
    }
    String strScale = String.format("1 Square == %dm  ", getGridUnit());
    g.drawString(strScale, viewSpace.y + viewSpace.width - fm.stringWidth(strScale) - 10, 40);

    g.setTransform(tempAT);
  }

  /**
   * Draw all the radios (nodes) in the environment. Stores the shapes used in nodeShapes.
   * 
   * @param g Graphics object to draw to
   * @param viewSpace The visible area
   */
  private void drawNodes(Graphics2D g, Rectangle viewSpace) {
    nodeShapes.clear();
    Rectangle2D viewArea = getCoordinateSpace(viewSpace);
    g.setStroke(new BasicStroke(2));
    int radius = 10;
    for (Radio node : environment.getNodes()) {
      if (viewArea.contains(node.getXY())) {
        // Determine colour scheme
        Color border = Color.BLACK;
        Color fill = GENERIC_RADIO_COLOR;
        if (node.getCurrentTransmission() != null) {
          fill = new Color(0, 0, 255);
        }
        if (getSelectedNode() == node) {
          border = SELECTED_NODE_COLOR;
        }
        // Draw shape
        Point p = getViewPosition(node.getXY());
        Circle2D s = new Circle2D(p.x, p.y, radius);
        g.setColor(fill);
        g.fill(s.asAwtShape());
        // Label it with its ID
        FontMetrics fm = g.getFontMetrics();
        String strID = String.valueOf(node.getID());
        int strIDX = p.x - fm.stringWidth(strID) / 2;
        int strIDY = p.y + fm.getHeight() / 3;
        g.setColor(getViewableTextColor(fill));
        g.drawString(strID, strIDX, strIDY);
        // Draw a border
        g.setStroke(new BasicStroke(2));
        g.setColor(border);
        g.draw(s.asAwtShape());
        nodeShapes.put(node, s);
      }
    }
  }

  /**
   * @return The current node shapes that are drawn
   */
  public Map<Radio, Circle2D> getNodeShapes() {
    return nodeShapes;
  }

  /**
   * Draw the RSSI values above each node.
   * 
   * @param g Graphics object to draw to
   * @param viewSpace The visible area
   */
  private void drawRSSIs(Graphics2D g, Rectangle viewSpace) {
    for (Radio node : environment.getNodes()) {
      if (node.getCurrentTransmission() == null) {
        // Get RSSI value for node and cache it if it is not already
        double rssi = findNodeRSSI(node);
        // Draw the RSSI string
        Point p = getViewPosition(node.getXY());
        String strRssi = String.format("%d", (int) rssi);
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
    }
  }

  /**
   * Get the RSSI value for a node, first check the cache, if not calculate it and cache it.
   * 
   * @param radio The radio to check for
   * @return The found RSSI value
   */
  private double findNodeRSSI(Radio radio) {
    // Get RSSI value for route and cache it if it is not already
    double rssi;
    if (rssiCache.containsKey(radio)) {
      rssi = rssiCache.get(radio);
    } else {
      rssi = environment.getRSSI(radio);
      rssiCache.put(radio, rssi);
    }
    return rssi;
  }

  /**
   * Find the worst case SNR between two nodes, used to highlight if interference at one end would
   * cause issues for a bi-directional link.
   * 
   * @param a One of the nodes
   * @param b One of the nodes
   * @return The worst case SNR between the nodes
   */
  private double findRouteWorstSNR(Radio a, Radio b) {
    double snrA = findRouteSNR(a, b);
    double snrB = findRouteSNR(b, a);
    return Math.min(snrA, snrB);
  }

  /**
   * Get the SNR value for a route, first check the cache, if not calculate it and cache it.
   * 
   * @param tx The transmitting radio
   * @param rx The receiving radio
   * @return The found SNR value
   */
  private double findRouteSNR(Radio tx, Radio rx) {
    // Get SNR value for route and cache it if it is not already
    double snr;
    Pair<Radio, Radio> route = new ImmutablePair<>(tx, rx);
    if (snrCache.containsKey(route)) {
      snr = snrCache.get(route);
    } else {
      snr = environment.getReceiveSNR(tx, rx);
      snrCache.put(route, snr);
    }
    return snr;
  }

  /**
   * Full wipe of SNR and RSSI values from the cache, do this if the model has changed.
   */
  private void clearCaches() {
    rssiCache.clear();
    snrCache.clear();
  }

  /**
   * Calculate an appropriate opacity for how close a value is to the maximum possible value. When
   * required > value opacity will be 0 (after a bit of fading away leeway).
   * 
   * @param value Value to find opacity for
   * @param required The maximum value before opacity fade occurs
   * @return The calculated opacity
   */
  private int determineOpacity(double value, double required) {
    int opacity = 0;
    if (value >= required) {
      opacity = 255;
    } else {
      double leeway = value * 0.1;
      double strength = (required + leeway) - value;
      opacity = (int) Math.max(0, Math.min(255, strength * 255 / leeway));
    }
    return opacity;
  }

  /**
   * Find all routes and their styles in the environment. Routes can either be from ongoing
   * transmissions or just potential routes.
   */
  private void findRoutes() {
    // Clear current route data
    routes.clear();
    routeColors.clear();
    routeStyles.clear();
    routeOpacity.clear();
    routeSNRs.clear();

    // Find transmission routes
    if (showTransmissions) {
      for (Radio radio : environment.getNodes()) {
        PartialReceive receive = radio.getTimeMap().get(environment.getTime());
        if (receive == null) {
          continue;
        }

        // Default style
        Color color = Color.BLACK;
        float style[] = SOLID_LINE;

        Transmission synced = null;
        long preambleEnd = receive.transmission.startTime;
        if (radio instanceof LoRaRadio) {
          synced = ((LoRaRadio) radio).getSyncedSignal();
          LoRaCfg senderCfg = ((LoRaRadio) receive.transmission.sender).getLoRaCfg();
          preambleEnd += senderCfg.calculatePreambleTime();
          // Assign colour based on it being test data or not
          if (receive.transmission.packet instanceof TestData) {
            color = TEST_DATA_COLOR;
          } else {
            color = OVERHEAD_COLOR;
          }
        }

        if (synced != null) {
          if (synced != receive.transmission) {
            color = INTERFERENCE_ROUTE_COLOR;
            style = LONG_DASH;
          }
        } else {
          if (environment.getTime() >= receive.transmission.startTime
              && environment.getTime() <= preambleEnd) {
            style = LONG_DASH;
          } else {
            color = Color.LIGHT_GRAY;
          }
        }
        // Determine opacity based on snr of a one way route
        double snr = findRouteSNR(radio, receive.transmission.sender);
        int opacity = determineOpacity(snr, radio.getRequiredSNR());

        // Add to routes to draw
        routes.add(new ImmutablePair<>(receive.transmission.sender, radio));
        routeColors.add(color);
        routeStyles.add(style);
        routeOpacity.add(opacity);
        routeSNRs.add(snr);
      }

    }
    // Find possible routes that aren't already created as transmissions
    if (showRoutes) {
      List<Radio> nodes = new ArrayList<>(environment.getNodes());
      for (int s = 0; s < nodes.size(); s++) {
        for (int t = s + 1; t < nodes.size(); t++) {
          Radio a = nodes.get(s);
          Radio b = nodes.get(t);
          if (!(routes.contains(new ImmutablePair<>(a, b))
              || routes.contains(new ImmutablePair<>(b, a)))) {
            Color color = null;
            float[] style = LONG_DASH;
            if (a.canCommunicate(b)) {
              color = GENERAL_ROUTE_COLOR;
            } else if (a.canInterfere(b)) {
              color = Color.RED;
            } else {
              continue;
            }
            // Determine opacity based on SNR of route both ways
            double snr = findRouteWorstSNR(a, b);
            int opacity = determineOpacity(snr, b.getRequiredSNR());
            // Add to routes to draw
            routes.add(new ImmutablePair<>(a, b));
            routeColors.add(color);
            routeStyles.add(style);
            routeOpacity.add(opacity);
            routeSNRs.add(snr);
          }
        }
      }
    }
  }

  /**
   * Draw all routes that have been found.
   * 
   * @param g Graphics object to draw to
   */
  private void drawRoutes(Graphics2D g) {
    // Draw routes in reverse so transmission routes are on top
    for (int i = (routes.size() - 1); i >= 0; i--) {
      Color color = routeColors.get(i);
      color = new Color(color.getRed(), color.getGreen(), color.getBlue(), routeOpacity.get(i));

      float[] style = routeStyles.get(i);
      g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, style, 0));
      g.setColor(color);
      Point pa = getViewPosition(routes.get(i).getLeft().getXY());
      Point pb = getViewPosition(routes.get(i).getRight().getXY());
      Line2D line = new Line2D(pa.x, pa.y, pb.x, pb.y);
      g.draw(line.asAwtShape());
    }
  }

  /**
   * Draw SNR values on top of found routes.
   * 
   * @param g Graphics object to draw to
   */
  private void drawRouteSNRs(Graphics2D g) {
    for (int i = (routes.size() - 1); i >= 0; i--) {
      Color color = routeColors.get(i);
      int opacity = routeOpacity.get(i);
      color = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
      Point pa = getViewPosition(routes.get(i).getLeft().getXY());
      Point pb = getViewPosition(routes.get(i).getRight().getXY());
      Point mid = new Point(pa.x + (pb.x - pa.x) / 2, pa.y + (pb.y - pa.y) / 2);
      String strSNR = String.format("%.1f", routeSNRs.get(i));
      FontMetrics fm = g.getFontMetrics();
      int strX = mid.x - fm.stringWidth(strSNR) / 2;
      int strY = mid.y;
      g.setColor(new Color(255, 255, 255, opacity));
      g.fillRoundRect(strX - 2, strY - fm.getHeight() + 3, fm.stringWidth(strSNR) + 4,
          fm.getHeight(), 5, 5);
      g.setColor(new Color(192, 192, 192, opacity));
      g.setStroke(new BasicStroke(1));
      g.drawRoundRect(strX - 2, strY - fm.getHeight() + 3, fm.stringWidth(strSNR) + 4,
          fm.getHeight(), 5, 5);
      g.setColor(color);
      g.drawString(strSNR, strX, strY);
    }
  }

  /**
   * Centre the coordinate space in the view so that all nodes are visible.
   * 
   * @param viewSpace The physical space to draw in
   */
  public void centreView(Rectangle viewSpace) {
    viewSpace.height = viewSpace.height - INFO_BOX_HEIGHT;
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
      Point2D newCentre = getCoordinate(new Point(viewSpace.width / 2, viewSpace.height / 2));
      Point2D newOffset =
          new Point2D(targetCentre.x() - newCentre.x(), targetCentre.y() - newCentre.y());
      setOffset(newOffset);
    }
  }

  /**
   * @return
   */
  public Point2D getCurPos() {
    return curPos;
  }

  /**
   * @param curPos
   */
  public void setCurPos(Point2D curPos) {
    this.curPos = curPos;
  }

  /**
   * @param selectedNode
   */
  public void setSelectedNode(Radio selectedNode) {
    this.selectedNode = selectedNode;
  }

  /**
   * @return
   */
  public Radio getSelectedNode() {
    return selectedNode;
  }

  /**
   * @return
   */
  public Point2D getOffset() {
    return offset;
  }

  /**
   * @param offset
   */
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
   * @return The scale transform to translate coordinate space to drawing (view) space
   */
  private AffineTransform getScaleTransform() {
    AffineTransform transform = new AffineTransform();
    transform.concatenate(AffineTransform.getTranslateInstance(-offset.x(), -offset.y()));
    transform.preConcatenate(AffineTransform.getScaleInstance(1 / getScale(), 1 / getScale()));
    return transform;
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
    drawer.centreView(getViewSpace(new Dimension(1024, 1024)));
    BufferedImage exportImage = drawer.getEnvironmentImage(new Dimension(1024, 1024));
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


  /**
   * Get a colour that is viewable on a given background colour.
   * 
   * @param bg Background colour
   * @return Colour that should be viewable on background colour
   */
  public static Color getViewableTextColor(Color bg) {
    float[] bgHSB = Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null);
    float bgBrightness = bgHSB[2];
    final float crossOver = (float) 0.8;
    final float fgMinBrightness = 0;
    final float fgMaxBrightness = (float) 1.0;
    float fgBrightness;
    if (bgBrightness < crossOver) {
      fgBrightness = fgMinBrightness;
    } else {
      fgBrightness = fgMaxBrightness;
    }
    return Color.getHSBColor(bgHSB[0], bgHSB[1] / 5, fgBrightness);
  }

}
