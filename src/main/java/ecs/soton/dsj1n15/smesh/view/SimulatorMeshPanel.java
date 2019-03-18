package ecs.soton.dsj1n15.smesh.view;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JPanel;
import ecs.soton.dsj1n15.smesh.model.Mesh;
import ecs.soton.dsj1n15.smesh.model.LoRaRadio;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;

public class SimulatorMeshPanel extends JPanel
    implements MouseListener, MouseMotionListener, MouseWheelListener {
  private static final long serialVersionUID = 5109577978956951077L;

  /** Mesh being displayed */
  private Mesh mesh;

  /** Drawing object being used to display mesh */
  private MeshDrawer meshDrawer;

  private Point lastPos = null;
  private LoRaRadio nodeAtPress = null;

  /**
   * Create the panel.
   */
  public SimulatorMeshPanel() {
    setMesh(null);
    addMouseListener(this);
    addMouseMotionListener(this);
    addMouseWheelListener(this);
  }

  @Override
  protected void paintComponent(Graphics gr) {
    super.paintComponent(gr);
    Graphics2D g = (Graphics2D) gr;
    Dimension d = this.getSize();
    meshDrawer.drawMesh(g, d);
  }

  /**
   * @return The mesh
   */
  public Mesh getMesh() {
    return mesh;
  }

  /**
   * Sets the mesh object. Generating a new drawer.
   *
   * @param mesh The new mesh puzzle
   */
  public void setMesh(Mesh mesh) {
    this.mesh = mesh;
    meshDrawer = new MeshDrawer(mesh);
    repaint();
  }

  /**
   * @return The mesh drawer
   */
  public MeshDrawer getMeshDrawer() {
    return meshDrawer;
  }

  private void moveNode(LoRaRadio node, MouseEvent e) {
    if (lastPos != null) {
      Point mouse = getPointOnView(e.getPoint());
      Point2D point = meshDrawer.getCoordinate(mouse);
      node.setX(point.x());
      node.setY(point.y());
      repaint();
    }
    lastPos = e.getPoint();
  }

  private void shiftView(MouseEvent e) {
    if (lastPos != null) {
      int difX = (int) (-(e.getX() - lastPos.x) * meshDrawer.getScale());
      int difY = (int) (-(e.getY() - lastPos.y) * meshDrawer.getScale());

      Point2D offset = meshDrawer.getOffset();
      meshDrawer.setOffset(new Point2D(offset.x() + difX, offset.y() + difY));
      repaint();
    }
    lastPos = e.getPoint();
  }

  private LoRaRadio findNode(MouseEvent e) {
    Point2D p = new Point2D(getPointOnView(e.getPoint()));
    Map<LoRaRadio, Circle2D> nodeShapes = meshDrawer.getNodeShapes();
    for (Entry<LoRaRadio, Circle2D> entry : nodeShapes.entrySet()) {
      if (entry.getValue().isInside(p)) {
        return entry.getKey();
      }
    }
    return null;
  }

  private void updateCurPos(MouseEvent e) {
    meshDrawer.setCurPos(getMouseCoordinate(e));
  }

  private void zoomView(Point point, int rotation) {
    int curSize = meshDrawer.getGridSize();
    int modSize = curSize + rotation;
    int newSize = Math.max(MeshDrawer.MIN_GRID_SIZE, Math.min(modSize, MeshDrawer.MAX_GRID_SIZE));
    if (curSize != newSize) {
      Point lastPosition = getPointOnView(point);
      Point2D lastCoordinate = meshDrawer.getCoordinate(lastPosition);
      meshDrawer.setGridSize(newSize);
      Point2D newCoordinate = meshDrawer.getCoordinate(lastPosition);
      Point2D shift = new Point2D(lastCoordinate.x() - newCoordinate.x(),
          lastCoordinate.y() - newCoordinate.y());
      Point2D offset = meshDrawer.getOffset();
      meshDrawer.setOffset(new Point2D(offset.x() + shift.x(), offset.y() + shift.y()));
      repaint();
    }
  }

  private Point2D getMouseCoordinate(MouseEvent e) {
    return meshDrawer.getCoordinate(getPointOnView(e.getPoint()));
  }


  private Point getPointOnView(Point p) {
    Rectangle r = MeshDrawer.getViewSpace(this.getSize());
    return new Point(p.x - r.x, p.y - r.y);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    lastPos = e.getPoint();
    nodeAtPress = findNode(e);
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (nodeAtPress != null && nodeAtPress.equals(meshDrawer.getSelectedNode())) {
      moveNode(nodeAtPress, e);
    } else {
      shiftView(e);
    }
    updateCurPos(e);
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    Rectangle r = MeshDrawer.getViewSpace(this.getSize());
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
    LoRaRadio node = findNode(e);
    meshDrawer.setSelectedNode(node);
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
  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

  }


}
