package ecs.soton.dsj1n15.smesh.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Collection;
import javax.swing.JFrame;
import javax.swing.border.BevelBorder;
import ecs.soton.dsj1n15.smesh.model.Mesh;
import ecs.soton.dsj1n15.smesh.model.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.environment.Forest;
import math.geom2d.AffineTransform2D;
import math.geom2d.Box2D;
import math.geom2d.GeometricObject2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearContourArray2D;
import math.geom2d.circulinear.CirculinearDomain2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.polygon.LinearRing2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Rectangle2D;
import math.geom2d.transform.CircleInversion2D;

public class SimulatorFrame extends JFrame {
  private static final long serialVersionUID = 8915866815288848109L;

  private Environment environment = null;
  
  private SimulatorViewPanel pnlView;
  private SimulatorControlPanel pnlControls;
  
  /**
   * Create the frame.
   */
  public SimulatorFrame() {
    this.setSize(800, 500);
    initialiseGUI();
    initialiseEventHandlers();
    initialiseModel();
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    
    pnlView.setEnvironment(environment);
  }

  /**
   * Create GUI objects.
   */
  private void initialiseGUI() {
    GridBagLayout gridBagLayout = new GridBagLayout();
    gridBagLayout.columnWidths = new int[] {0, 300};
    gridBagLayout.rowHeights = new int[] {0};
    gridBagLayout.columnWeights = new double[] {1.0, 0.0};
    gridBagLayout.rowWeights = new double[] {1.0};
    getContentPane().setLayout(gridBagLayout);

    /* LHS TilePuzzlePanel displaying TilePuzzle */
    pnlView = new SimulatorViewPanel();
    GridBagConstraints gbc_pnlTilePuzzle = new GridBagConstraints();
    gbc_pnlTilePuzzle.insets = new Insets(5, 5, 5, 2);
    gbc_pnlTilePuzzle.fill = GridBagConstraints.BOTH;
    gbc_pnlTilePuzzle.gridx = 0;
    gbc_pnlTilePuzzle.gridy = 0;
    getContentPane().add(pnlView, gbc_pnlTilePuzzle);
    
    /* RHS Control panel for TilePuzzle interaction */
    pnlControls = new SimulatorControlPanel();
    pnlControls.setBackground(Color.GRAY);
    pnlControls.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
    GridBagConstraints gbc_pnlControls = new GridBagConstraints();
    gbc_pnlControls.insets = new Insets(5, 2, 5, 5);
    gbc_pnlControls.fill = GridBagConstraints.BOTH;
    gbc_pnlControls.gridx = 1;
    gbc_pnlControls.gridy = 0;
    getContentPane().add(pnlControls, gbc_pnlControls);
  }

  /**
   * Attach event handlers to GUI objects.
   */
  private void initialiseEventHandlers() {

  }
    
  /**
   * Initialise model.
   */
  private void initialiseModel() {
    environment = new Environment();
    Forest forest1 = new Forest(new Rectangle2D(0, 0, 500, 500), 1);
    environment.getEnvironmentObjects().add(forest1);
    //environment.getEnvironmentObjects().addAll(forest1.generateTrees());
    Forest forest2 = new Forest(new Rectangle2D(550, 0, 200, 500), 0.5);
    environment.getEnvironmentObjects().add(forest2);
    Forest forest3 = new Forest(new Rectangle2D(800, 0, 200, 500), 0.25);
    environment.getEnvironmentObjects().add(forest3);
    Forest forest4 = new Forest(new Rectangle2D(0, 600, 1000, 200), 0.1);
    environment.getEnvironmentObjects().add(forest4);
    
    //mesh = new Mesh(1);
    double z = 0.25;
    LoRaRadio node1 = new LoRaRadio(1);
    node1.setX(0);
    node1.setY(50);
    node1.setZ(z);
    environment.addNode(node1);
    
    LoRaRadio node2 = new LoRaRadio(2);
    node2.setX(50);
    node2.setY(0);
    node2.setZ(z);
    environment.addNode(node2);
    
    LoRaRadio node3 = new LoRaRadio(3);
    node3.setX(200);
    node3.setY(0);
    node3.setZ(z);
    environment.addNode(node3);
    
    LoRaRadio node4 = new LoRaRadio(4);
    node4.setX(310);
    node4.setY(200);
    node4.setZ(z);
    environment.addNode(node4);
    
    LoRaRadio node5 = new LoRaRadio(5);
    node5.setX(500);
    node5.setY(500);
    node5.setZ(z);
    environment.addNode(node5);
  }
  
}
