package ecs.soton.dsj1n15.smesh.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.border.BevelBorder;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunnerListener;
import ecs.soton.dsj1n15.smesh.model.environment.Forest;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.presets.NineNodeLine;
import ecs.soton.dsj1n15.smesh.model.presets.TwoNodeNO;
import math.geom2d.polygon.Rectangle2D;

public class SimulatorFrame extends JFrame {
  private static final long serialVersionUID = 8915866815288848109L;

  private final EnvironmentRunner runner;

  private SimulatorViewPanel pnlView;
  private SimulatorControlPanel pnlControls;

  /**
   * Create the frame.
   */
  public SimulatorFrame() {
    // Create the main running thread
    runner = new EnvironmentRunner();

    // Create simulator
    this.setSize(800, 500);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    initialiseGUI();
    initialiseEventHandlers();

    initialiseModel();
    loadPresets();
    pnlControls.loadEnvironment();
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

    /* LHS Simulator View displaying Environment */
    pnlView = new SimulatorViewPanel();
    GridBagConstraints gnc_pnlView = new GridBagConstraints();
    gnc_pnlView.insets = new Insets(5, 5, 5, 2);
    gnc_pnlView.fill = GridBagConstraints.BOTH;
    gnc_pnlView.gridx = 0;
    gnc_pnlView.gridy = 0;
    getContentPane().add(pnlView, gnc_pnlView);

    /* RHS Control panel for Simulator interaction */
    pnlControls = new SimulatorControlPanel(runner, pnlView);
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
    runner.addListener(new EnvironmentRunnerListener() {
      @Override
      public void update() {
        pnlView.repaint();
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Load all presets into selection box.
   */
  private void loadPresets() {
    pnlControls.addPreset("9N NO Line", new NineNodeLine());
    pnlControls.addPreset("2N NO 100m", new TwoNodeNO(100, LoRaCfg.getDataRate0()));
  }



  /**
   * Initialise model.
   */
  private void initialiseModel() {
    //environment = new Environment();
    Forest forest1 = new Forest(new Rectangle2D(0, 0, 500, 500), 1);
    // environment.getEnvironmentObjects().add(forest1);
    Forest forest2 = new Forest(new Rectangle2D(550, 0, 200, 500), 0.5);
    // environment.getEnvironmentObjects().add(forest2);
    Forest forest3 = new Forest(new Rectangle2D(800, 0, 200, 500), 0.25);
    // environment.getEnvironmentObjects().add(forest3);
    Forest forest4 = new Forest(new Rectangle2D(0, 600, 1000, 200), 0.1);
    // environment.getEnvironmentObjects().add(forest4);
  }



}
