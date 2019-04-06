package ecs.soton.dsj1n15.smesh.view;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JPanel;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;

public class SimulatorViewPanel extends JPanel
    implements MouseListener, MouseMotionListener, MouseWheelListener {
  private static final long serialVersionUID = 5109577978956951077L;

  /** Environment being displayed */
  private Environment environment;

  /** Drawing object being used to display environment */
  private EnvironmentDrawer environmentDrawer;

  private Point lastPos = null;
  private Radio nodeAtPress = null;

  private volatile boolean upToDate;

  /**
   * Create the panel.
   */
  public SimulatorViewPanel() {
    setEnvironment(null);
    addMouseListener(this);
    addMouseMotionListener(this);
    addMouseWheelListener(this);
  }

  @Override
  protected void paintComponent(Graphics gr) {
    super.paintComponent(gr);
    Graphics2D g = (Graphics2D) gr;
    Dimension d = this.getSize();
    environmentDrawer.drawEnvironment(g, d);
    upToDate = true;
  }

  /**
   * @return The environment being draw
   */
  public Environment getEnvironment() {
    return environment;
  }

  /**
   * Sets the environment object. Generating a new drawer.
   *
   * @param environment The new environment
   */
  public void setEnvironment(Environment environment) {
    this.environment = environment;
    environmentDrawer = new EnvironmentDrawer(environment);
    repaint();
  }

  /**
   * @return The environment drawer
   */
  public EnvironmentDrawer getEnvironmentDrawer() {
    return environmentDrawer;
  }

  /**
   * @return Whether view is up to date
   */
  public boolean isUpToDate() {
    return upToDate;
  }

  /**
   * Set the flag to indicate the view is out of date.
   */
  public void setUpdateNeeded() {
    this.upToDate = false;
  }
 

  private void moveNode(Radio node, MouseEvent e) {
    if (lastPos != null) {
      Point mouse = getPointOnView(e.getPoint());
      Point2D point = environmentDrawer.getCoordinate(mouse);
      node.setX(point.x());
      node.setY(point.y());
      repaint();
    }
    lastPos = e.getPoint();
  }

  private void shiftView(MouseEvent e) {
    if (lastPos != null) {
      int difX = (int) (-(e.getX() - lastPos.x) * environmentDrawer.getScale());
      int difY = (int) (-(e.getY() - lastPos.y) * environmentDrawer.getScale());

      Point2D offset = environmentDrawer.getOffset();
      environmentDrawer.setOffset(new Point2D(offset.x() + difX, offset.y() + difY));
      repaint();
    }
    lastPos = e.getPoint();
  }

  private Radio findNode(MouseEvent e) {
    Point2D p = new Point2D(getPointOnView(e.getPoint()));
    Map<Radio, Circle2D> nodeShapes = environmentDrawer.getNodeShapes();
    for (Entry<Radio, Circle2D> entry : nodeShapes.entrySet()) {
      if (entry.getValue().isInside(p)) {
        return entry.getKey();
      }
    }
    return null;
  }

  private void updateCurPos(MouseEvent e) {
    environmentDrawer.setCurPos(getMouseCoordinate(e));
  }

  private void zoomView(Point point, int rotation) {
    int curSize = environmentDrawer.getGridSize();
    int modSize = curSize + rotation;
    int newSize = Math.max(EnvironmentDrawer.MIN_GRID_SIZE,
        Math.min(modSize, EnvironmentDrawer.MAX_GRID_SIZE));
    if (curSize != newSize) {
      Point lastPosition = getPointOnView(point);
      Point2D lastCoordinate = environmentDrawer.getCoordinate(lastPosition);
      environmentDrawer.setGridSize(newSize);
      Point2D newCoordinate = environmentDrawer.getCoordinate(lastPosition);
      Point2D shift = new Point2D(lastCoordinate.x() - newCoordinate.x(),
          lastCoordinate.y() - newCoordinate.y());
      Point2D offset = environmentDrawer.getOffset();
      environmentDrawer.setOffset(new Point2D(offset.x() + shift.x(), offset.y() + shift.y()));
      repaint();
    }
  }

  private Point2D getMouseCoordinate(MouseEvent e) {
    return environmentDrawer.getCoordinate(getPointOnView(e.getPoint()));
  }

  private Point getPointOnView(Point p) {
    Rectangle r = EnvironmentDrawer.getViewSpace(this.getSize());
    return new Point(p.x - r.x, p.y - r.y);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    lastPos = e.getPoint();
    nodeAtPress = findNode(e);
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (nodeAtPress != null && nodeAtPress.equals(environmentDrawer.getSelectedNode())) {
      moveNode(nodeAtPress, e);
    } else {
      shiftView(e);
    }
    updateCurPos(e);
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    Rectangle r = EnvironmentDrawer.getViewSpace(this.getSize());
    if (r.contains(e.getX(), e.getY())) {
      this.setCursor(new Cursor(Cursor.HAND_CURSOR));
    } else {
      this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
    updateCurPos(e);
    repaint();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    Radio node = findNode(e);
    environmentDrawer.setSelectedNode(node);
    repaint();
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    nodeAtPress = null;
    lastPos = null;
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    int rotation = e.getWheelRotation();
    updateCurPos(e);
    zoomView(e.getPoint(), rotation);
  }

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}

}
