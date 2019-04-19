package ecs.soton.dsj1n15.smesh.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.border.BevelBorder;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;

/**
 * Main frame for the managing the GUI overlay.
 * 
 * @author David Jones (dsj1n15)
 */
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
    this.setSize(950, 600);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    initialiseGUI();
    pnlControls.loadEnvironment();
  }

  /**
   * Create GUI objects.
   */
  private void initialiseGUI() {
    GridBagLayout gridBagLayout = new GridBagLayout();
    gridBagLayout.columnWidths = new int[] {0, 0};
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

}
