package ecs.soton.dsj1n15.smesh.view;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunnerListener;
import ecs.soton.dsj1n15.smesh.model.presets.Preset;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.JCheckBox;

public class SimulatorControlPanel extends JScrollPane {
  private static final long serialVersionUID = -6615562230893271240L;

  private final Map<String, Preset> presets = new LinkedHashMap<>();

  private JTextField txtTime;

  private final EnvironmentRunner runner;
  private final SimulatorViewPanel simulatorView;

  private JComboBox<String> cboPreset;
  private JComboBox<Integer> cboTimeStep;

  private JButton btnReset;
  private JButton btnPlus1;
  private JButton btnPlus10;
  private JButton btnPlus100;
  private JButton btnRun;
  private JPanel pnlViewSettings;
  private JCheckBox chkShowRoutes;
  private JCheckBox chkShowRssis;

  /**
   * Create the panel.
   */
  public SimulatorControlPanel(EnvironmentRunner runner, SimulatorViewPanel simulatorView) {
    this.runner = runner;
    this.simulatorView = simulatorView;
    initGUI();
    initialiseEventHandlers();

    this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    this.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    this.getVerticalScrollBar().setUnitIncrement(10);

    // Load specifics
    loadEnvironment();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (cboPreset.getItemCount() > 0) {
          cboPreset.setSelectedIndex(0);
        }
      }
    });
  }

  /**
   * Create GUI objects.
   */
  private void initGUI() {
    JPanel pnlControls = new JPanel();
    this.setViewportView(pnlControls);
    GridBagLayout gbl_pnlControls = new GridBagLayout();
    gbl_pnlControls.columnWidths = new int[] {0};
    gbl_pnlControls.rowHeights = new int[] {0, 0, 0, 0};
    gbl_pnlControls.columnWeights = new double[] {1.0};
    gbl_pnlControls.rowWeights = new double[] {0.0, 0.0, 0.0, 1.0};
    pnlControls.setLayout(gbl_pnlControls);

    JPanel pnlPreset = new JPanel();
    pnlPreset.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Presets"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    GridBagConstraints gbc_pnlPreset = new GridBagConstraints();
    gbc_pnlPreset.insets = new Insets(5, 5, 5, 0);
    gbc_pnlPreset.fill = GridBagConstraints.BOTH;
    gbc_pnlPreset.gridx = 0;
    gbc_pnlPreset.gridy = 0;
    pnlControls.add(pnlPreset, gbc_pnlPreset);
    GridBagLayout gbl_pnlPreset = new GridBagLayout();
    gbl_pnlPreset.columnWidths = new int[] {0, 0};
    gbl_pnlPreset.rowHeights = new int[] {0};
    gbl_pnlPreset.columnWeights = new double[] {1.0, 0.0};
    gbl_pnlPreset.rowWeights = new double[] {1.0};
    pnlPreset.setLayout(gbl_pnlPreset);

    cboPreset = new JComboBox<String>();
    GridBagConstraints gbc_cboPreset = new GridBagConstraints();
    gbc_cboPreset.insets = new Insets(0, 0, 0, 5);
    gbc_cboPreset.fill = GridBagConstraints.HORIZONTAL;
    gbc_cboPreset.gridx = 0;
    gbc_cboPreset.gridy = 0;
    pnlPreset.add(cboPreset, gbc_cboPreset);

    btnReset = new JButton("Reset");
    GridBagConstraints gbc_btnReset = new GridBagConstraints();
    gbc_btnReset.insets = new Insets(0, 0, 0, 0);
    gbc_btnReset.gridx = 1;
    gbc_btnReset.gridy = 0;
    pnlPreset.add(btnReset, gbc_btnReset);

    JPanel pnlTime = new JPanel();
    pnlTime.setBorder(
        BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Envionment"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    GridBagConstraints gbc_pnlTime = new GridBagConstraints();
    gbc_pnlTime.insets = new Insets(0, 5, 5, 0);
    gbc_pnlTime.fill = GridBagConstraints.BOTH;
    gbc_pnlTime.gridx = 0;
    gbc_pnlTime.gridy = 1;
    pnlControls.add(pnlTime, gbc_pnlTime);
    GridBagLayout gbl_pnlTime = new GridBagLayout();
    gbl_pnlTime.columnWidths = new int[] {0, 0, 0};
    gbl_pnlTime.rowHeights = new int[] {0, 0, 0};
    gbl_pnlTime.columnWeights = new double[] {0.0, 1.0, 0.0};
    gbl_pnlTime.rowWeights = new double[] {0.0, 0.0, 1.0};
    pnlTime.setLayout(gbl_pnlTime);

    JLabel lblTimeLabel = new JLabel("Time (ms):");
    GridBagConstraints gbc_lblTimeLabel = new GridBagConstraints();
    gbc_lblTimeLabel.anchor = GridBagConstraints.EAST;
    gbc_lblTimeLabel.insets = new Insets(0, 0, 5, 5);
    gbc_lblTimeLabel.gridx = 0;
    gbc_lblTimeLabel.gridy = 0;
    pnlTime.add(lblTimeLabel, gbc_lblTimeLabel);

    txtTime = new JTextField();
    txtTime.setEditable(false);
    GridBagConstraints gbc_txtTime = new GridBagConstraints();
    gbc_txtTime.insets = new Insets(0, 0, 5, 5);
    gbc_txtTime.fill = GridBagConstraints.HORIZONTAL;
    gbc_txtTime.gridx = 1;
    gbc_txtTime.gridy = 0;
    pnlTime.add(txtTime, gbc_txtTime);
    txtTime.setColumns(10);

    JLabel lblTimeStep = new JLabel("Time Step (ms):");
    GridBagConstraints gbc_lblTimeStep = new GridBagConstraints();
    gbc_lblTimeStep.anchor = GridBagConstraints.EAST;
    gbc_lblTimeStep.insets = new Insets(0, 0, 5, 5);
    gbc_lblTimeStep.gridx = 0;
    gbc_lblTimeStep.gridy = 1;
    pnlTime.add(lblTimeStep, gbc_lblTimeStep);

    cboTimeStep = new JComboBox<Integer>();
    for (Integer unit : runner.getTimeUnitOptions()) {
      cboTimeStep.addItem(unit);
    }
    cboTimeStep.setSelectedItem(runner.getTimeUnit());
    GridBagConstraints gbc_cboTimeStep = new GridBagConstraints();
    gbc_cboTimeStep.insets = new Insets(0, 0, 5, 5);
    gbc_cboTimeStep.fill = GridBagConstraints.HORIZONTAL;
    gbc_cboTimeStep.gridx = 1;
    gbc_cboTimeStep.gridy = 1;
    pnlTime.add(cboTimeStep, gbc_cboTimeStep);

    JPanel pnlTimeAddition = new JPanel();
    GridBagConstraints gbc_pnlTimeAddition = new GridBagConstraints();
    gbc_pnlTimeAddition.gridwidth = 3;
    gbc_pnlTimeAddition.fill = GridBagConstraints.BOTH;
    gbc_pnlTimeAddition.gridx = 0;
    gbc_pnlTimeAddition.gridy = 2;
    pnlTime.add(pnlTimeAddition, gbc_pnlTimeAddition);
    GridBagLayout gbl_pnlTimeAddition = new GridBagLayout();
    gbl_pnlTimeAddition.columnWidths = new int[] {0, 0, 0, 0, 0, 0};
    gbl_pnlTimeAddition.rowHeights = new int[] {0, 0};
    gbl_pnlTimeAddition.columnWeights = new double[] {1.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
    gbl_pnlTimeAddition.rowWeights = new double[] {0.0};
    pnlTimeAddition.setLayout(gbl_pnlTimeAddition);

    btnPlus1 = new JButton("+1");
    GridBagConstraints gbc_btnPlus1 = new GridBagConstraints();
    gbc_btnPlus1.insets = new Insets(0, 0, 0, 5);
    gbc_btnPlus1.gridx = 1;
    gbc_btnPlus1.gridy = 0;
    pnlTimeAddition.add(btnPlus1, gbc_btnPlus1);

    btnPlus10 = new JButton("+10");
    GridBagConstraints gbc_btnPlus10 = new GridBagConstraints();
    gbc_btnPlus10.insets = new Insets(0, 0, 0, 5);
    gbc_btnPlus10.gridx = 2;
    gbc_btnPlus10.gridy = 0;
    pnlTimeAddition.add(btnPlus10, gbc_btnPlus10);

    btnPlus100 = new JButton("+100");
    GridBagConstraints gbc_btnPlus100 = new GridBagConstraints();
    gbc_btnPlus100.insets = new Insets(0, 0, 0, 5);
    gbc_btnPlus100.gridx = 3;
    gbc_btnPlus100.gridy = 0;
    pnlTimeAddition.add(btnPlus100, gbc_btnPlus100);

    btnRun = new JButton("Run");
    GridBagConstraints gbc_btnRun = new GridBagConstraints();
    gbc_btnRun.gridx = 4;
    gbc_btnRun.gridy = 0;
    pnlTimeAddition.add(btnRun, gbc_btnRun);

    pnlViewSettings = new JPanel();
    pnlViewSettings.setBorder(
        BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("View Settings"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    GridBagConstraints gbc_pnlViewSettings = new GridBagConstraints();
    gbc_pnlViewSettings.fill = GridBagConstraints.BOTH;
    gbc_pnlViewSettings.gridx = 0;
    gbc_pnlViewSettings.gridy = 2;
    pnlControls.add(pnlViewSettings, gbc_pnlViewSettings);
    GridBagLayout gbl_pnlViewSettings = new GridBagLayout();
    gbl_pnlViewSettings.columnWidths = new int[] {0, 0};
    gbl_pnlViewSettings.rowHeights = new int[] {0, 0, 0};
    gbl_pnlViewSettings.columnWeights = new double[] {0.0, Double.MIN_VALUE};
    gbl_pnlViewSettings.rowWeights = new double[] {0.0, 0.0, Double.MIN_VALUE};
    pnlViewSettings.setLayout(gbl_pnlViewSettings);

    chkShowRoutes = new JCheckBox("Show All Routes");
    chkShowRoutes.setSelected(simulatorView.getEnvironmentDrawer().isShowRoutes());
    GridBagConstraints gbc_chkShowRoutes = new GridBagConstraints();
    gbc_chkShowRoutes.anchor = GridBagConstraints.WEST;
    gbc_chkShowRoutes.insets = new Insets(0, 0, 5, 0);
    gbc_chkShowRoutes.gridx = 0;
    gbc_chkShowRoutes.gridy = 0;
    pnlViewSettings.add(chkShowRoutes, gbc_chkShowRoutes);

    chkShowRssis = new JCheckBox("Show RSSIs");
    chkShowRssis.setSelected(simulatorView.getEnvironmentDrawer().isShowRSSIs());
    GridBagConstraints gbc_chkShowRssis = new GridBagConstraints();
    gbc_chkShowRssis.anchor = GridBagConstraints.WEST;
    gbc_chkShowRssis.gridx = 0;
    gbc_chkShowRssis.gridy = 1;
    pnlViewSettings.add(chkShowRssis, gbc_chkShowRssis);
  }

  /**
   * Attach event handlers to GUI objects.
   */
  private void initialiseEventHandlers() {
    cboPreset.addActionListener(x -> {
      loadPreset((String) cboPreset.getSelectedItem());
    });
    btnReset.addActionListener(x -> {
      loadPreset((String) cboPreset.getSelectedItem());
    });
    cboTimeStep.addActionListener(x -> {
      runner.setTimeUnit((Integer) cboTimeStep.getSelectedItem());
    });
    btnPlus1.addActionListener(x -> {
      runner.addUnitsToRun(1);
    });
    btnPlus10.addActionListener(x -> {
      runner.addUnitsToRun(10);
    });
    btnPlus100.addActionListener(x -> {
      runner.addUnitsToRun(100);
    });
    btnRun.addActionListener(x -> {
      if (runner.isRunning()) {
        btnPlus1.setEnabled(true);
        btnPlus10.setEnabled(true);
        btnPlus100.setEnabled(true);
        btnRun.setText("Start");
        runner.stop();
        loadEnvironment();
      } else {
        btnPlus1.setEnabled(false);
        btnPlus10.setEnabled(false);
        btnPlus100.setEnabled(false);
        btnRun.setText("Stop");
        runner.start();
      }
    });
    runner.addListener(new EnvironmentRunnerListener() {
      @Override
      public void update() {
        loadEnvironment();
      }
    });

    chkShowRoutes.addActionListener(x -> {
      simulatorView.getEnvironmentDrawer().setShowRoutes(chkShowRoutes.isSelected());
      simulatorView.repaint();
    });
    chkShowRssis.addActionListener(x -> {
      simulatorView.getEnvironmentDrawer().setShowRSSIs(chkShowRssis.isSelected());
      simulatorView.repaint();
    });
  }

  /**
   * Add a preset to the preset list.
   * 
   * @param name Name of the preset in the list
   * @param preset Preset
   */
  public void addPreset(String name, Preset preset) {
    if (!presets.containsKey(name)) {
      presets.put(name, preset);
      cboPreset.addItem(name);
    } else {
      throw new IllegalArgumentException("Preset already exists");
    }
  }

  private void loadPreset(String id) {
    Cloner cloner = new Cloner();
    Preset preset = cloner.deepClone(presets.get(id));
    simulatorView.setEnvironment(preset.getEnvironment());
    runner.clearEvents();
    runner.setEnvironment(preset.getEnvironment());
    runner.addEvents(preset.getEvents());
    loadEnvironment();
    simulatorView.getEnvironmentDrawer()
        .centreView(EnvironmentDrawer.getViewSpace(simulatorView.getSize()));
  }
  
  /**
   * Load the model.
   */
  public void loadEnvironment() {
    long time = 0;
    if (runner.getEnvironment() != null) {
      time = runner.getEnvironment().getTime();
    }
    txtTime.setText(String.valueOf(time));
    if (runner.isRunning()) {
      btnRun.setText("Stop");
      btnReset.setEnabled(false);
      cboPreset.setEnabled(false);
      cboTimeStep.setEnabled(false);
    } else {
      btnRun.setText("Start");
      btnReset.setEnabled(true);
      cboPreset.setEnabled(true);
      cboTimeStep.setEnabled(true);
    }
  }

}
