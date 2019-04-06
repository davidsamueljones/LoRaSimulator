package ecs.soton.dsj1n15.smesh.view;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import com.rits.cloning.Cloner;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunner;
import ecs.soton.dsj1n15.smesh.controller.EnvironmentRunnerListener;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.Protocol;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive.AdaptiveBroadcastProtocol;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.naive.NaiveBroadcastProtocol;
import ecs.soton.dsj1n15.smesh.model.presets.DataBroadcastTest;
import ecs.soton.dsj1n15.smesh.model.presets.LargeDataBroadcastTest;
import ecs.soton.dsj1n15.smesh.model.presets.NineNodeLine;
import ecs.soton.dsj1n15.smesh.model.presets.Preset;
import ecs.soton.dsj1n15.smesh.model.presets.TwoNodeNO;
import java.awt.GridBagLayout;
import java.awt.Dimension;
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

  private static String NAIVE_PROTOCOL_1P_NAME = "Naive Broadcast (1%)";
  private static String NAIVE_PROTOCOL_10P_NAME = "Naive Broadcast (10%)";
  private static String NAIVE_PROTOCOL_10P_NO_CAD_NAME = "Naive Broadcast (10% NC)";
  private static String ADAPTIVE_PROTOCOL_NAME = "Adaptive Broadcast";
  private static String EVENTS_ONLY = "Preset Events";

  private final Map<String, Preset> presets = new LinkedHashMap<>();

  private JTextField txtTime;

  private final EnvironmentRunner runner;
  private final SimulatorViewPanel pnlView;
  private ViewUpdater viewUpdater = new ViewUpdater();

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
  private JPanel pnlProtocol;
  private JCheckBox chkAntiAlias;
  private JLabel lblProtocol;
  private JComboBox<String> cboProtocol;
  private JPanel pnlProtocolExport;
  private JButton btnDumpActivity;
  private JButton btnDumpReceiveStats;
  private JCheckBox chkUpdateView;

  /**
   * Create the panel.
   */
  public SimulatorControlPanel(EnvironmentRunner runner, SimulatorViewPanel pnlView) {
    this.runner = runner;
    this.pnlView = pnlView;
    initGUI();
    initialiseEventHandlers();

    this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    this.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    this.getVerticalScrollBar().setUnitIncrement(10);


    // Load specifics
    loadPresets();
    loadProtocols();
    loadEnvironment();

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        setMinimumSize(new Dimension(getPreferredSize().width, 0));
      }
    });

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (cboPreset.getItemCount() > 0) {
          cboPreset.setSelectedIndex(0);
          cboProtocol.setSelectedIndex(0);
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
    gbl_pnlControls.rowHeights = new int[] {0, 0, 0, 0, 0};
    gbl_pnlControls.columnWeights = new double[] {1.0};
    gbl_pnlControls.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 1.0};
    pnlControls.setLayout(gbl_pnlControls);

    JPanel pnlPreset = new JPanel();
    pnlPreset.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Presets"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    GridBagConstraints gbc_pnlPreset = new GridBagConstraints();
    gbc_pnlPreset.insets = new Insets(5, 5, 5, 5);
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

    JPanel pnlEnvironment = new JPanel();
    pnlEnvironment.setBorder(
        BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Environment"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    GridBagConstraints gbc_pnlEnvironment = new GridBagConstraints();
    gbc_pnlEnvironment.insets = new Insets(0, 5, 5, 5);
    gbc_pnlEnvironment.fill = GridBagConstraints.BOTH;
    gbc_pnlEnvironment.gridx = 0;
    gbc_pnlEnvironment.gridy = 1;
    pnlControls.add(pnlEnvironment, gbc_pnlEnvironment);
    GridBagLayout gbl_pnlEnvironment = new GridBagLayout();
    gbl_pnlEnvironment.columnWidths = new int[] {0, 0, 0};
    gbl_pnlEnvironment.rowHeights = new int[] {0, 0, 0};
    gbl_pnlEnvironment.columnWeights = new double[] {0.0, 1.0, 0.0};
    gbl_pnlEnvironment.rowWeights = new double[] {0.0, 0.0, 1.0};
    pnlEnvironment.setLayout(gbl_pnlEnvironment);

    JLabel lblTimeLabel = new JLabel("Time (ms):");
    GridBagConstraints gbc_lblTimeLabel = new GridBagConstraints();
    gbc_lblTimeLabel.anchor = GridBagConstraints.EAST;
    gbc_lblTimeLabel.insets = new Insets(0, 0, 5, 5);
    gbc_lblTimeLabel.gridx = 0;
    gbc_lblTimeLabel.gridy = 0;
    pnlEnvironment.add(lblTimeLabel, gbc_lblTimeLabel);

    txtTime = new JTextField();
    txtTime.setEditable(false);
    GridBagConstraints gbc_txtTime = new GridBagConstraints();
    gbc_txtTime.insets = new Insets(0, 0, 5, 5);
    gbc_txtTime.fill = GridBagConstraints.HORIZONTAL;
    gbc_txtTime.gridx = 1;
    gbc_txtTime.gridy = 0;
    pnlEnvironment.add(txtTime, gbc_txtTime);
    txtTime.setColumns(10);

    JLabel lblTimeStep = new JLabel("Time Step (ms):");
    GridBagConstraints gbc_lblTimeStep = new GridBagConstraints();
    gbc_lblTimeStep.anchor = GridBagConstraints.EAST;
    gbc_lblTimeStep.insets = new Insets(0, 0, 5, 5);
    gbc_lblTimeStep.gridx = 0;
    gbc_lblTimeStep.gridy = 1;
    pnlEnvironment.add(lblTimeStep, gbc_lblTimeStep);

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
    pnlEnvironment.add(cboTimeStep, gbc_cboTimeStep);

    JPanel pnlTimeAddition = new JPanel();
    GridBagConstraints gbc_pnlTimeAddition = new GridBagConstraints();
    gbc_pnlTimeAddition.gridwidth = 3;
    gbc_pnlTimeAddition.fill = GridBagConstraints.BOTH;
    gbc_pnlTimeAddition.gridx = 0;
    gbc_pnlTimeAddition.gridy = 2;
    pnlEnvironment.add(pnlTimeAddition, gbc_pnlTimeAddition);
    GridBagLayout gbl_pnlTimeAddition = new GridBagLayout();
    gbl_pnlTimeAddition.columnWidths = new int[] {0, 0, 0, 0, 0, 0};
    gbl_pnlTimeAddition.rowHeights = new int[] {0};
    gbl_pnlTimeAddition.columnWeights = new double[] {1.0, 0.0, 0.0, 0.0, 0.0, 1.0};
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

    pnlProtocol = new JPanel();
    pnlProtocol.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Protocol"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    GridBagConstraints gbc_pnlProtocol = new GridBagConstraints();
    gbc_pnlProtocol.insets = new Insets(0, 5, 5, 5);
    gbc_pnlProtocol.fill = GridBagConstraints.BOTH;
    gbc_pnlProtocol.gridx = 0;
    gbc_pnlProtocol.gridy = 2;
    pnlControls.add(pnlProtocol, gbc_pnlProtocol);
    GridBagLayout gbl_pnlProtocol = new GridBagLayout();
    gbl_pnlProtocol.columnWidths = new int[] {0, 0, 0};
    gbl_pnlProtocol.rowHeights = new int[] {0, 0, 0};
    gbl_pnlProtocol.columnWeights = new double[] {0.0, 1.0, Double.MIN_VALUE};
    gbl_pnlProtocol.rowWeights = new double[] {0.0, 1.0, Double.MIN_VALUE};
    pnlProtocol.setLayout(gbl_pnlProtocol);

    lblProtocol = new JLabel("Protocol:");
    GridBagConstraints gbc_lblProtocol = new GridBagConstraints();
    gbc_lblProtocol.insets = new Insets(0, 0, 5, 5);
    gbc_lblProtocol.anchor = GridBagConstraints.EAST;
    gbc_lblProtocol.gridx = 0;
    gbc_lblProtocol.gridy = 0;
    pnlProtocol.add(lblProtocol, gbc_lblProtocol);

    cboProtocol = new JComboBox<>();
    GridBagConstraints gbc_cboProtocol = new GridBagConstraints();
    gbc_cboProtocol.insets = new Insets(0, 0, 5, 0);
    gbc_cboProtocol.fill = GridBagConstraints.HORIZONTAL;
    gbc_cboProtocol.gridx = 1;
    gbc_cboProtocol.gridy = 0;
    pnlProtocol.add(cboProtocol, gbc_cboProtocol);

    pnlProtocolExport = new JPanel();
    pnlProtocolExport.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Export"), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    GridBagConstraints gbc_pnlProtocolExport = new GridBagConstraints();
    gbc_pnlProtocolExport.gridwidth = 2;
    gbc_pnlProtocolExport.insets = new Insets(0, 0, 0, 0);
    gbc_pnlProtocolExport.fill = GridBagConstraints.BOTH;
    gbc_pnlProtocolExport.gridx = 0;
    gbc_pnlProtocolExport.gridy = 1;
    pnlProtocol.add(pnlProtocolExport, gbc_pnlProtocolExport);
    GridBagLayout gbl_pnlProtocolExport = new GridBagLayout();
    gbl_pnlProtocolExport.columnWidths = new int[] {0, 0, 0, 0, 0};
    gbl_pnlProtocolExport.rowHeights = new int[] {0};
    gbl_pnlProtocolExport.columnWeights = new double[] {1.0, 0.0, 0.0, 1.0};
    gbl_pnlProtocolExport.rowWeights = new double[] {0.0};
    pnlProtocolExport.setLayout(gbl_pnlProtocolExport);

    btnDumpActivity = new JButton("Activity");
    GridBagConstraints gbc_btnDumpActivity = new GridBagConstraints();
    gbc_btnDumpActivity.insets = new Insets(0, 0, 0, 5);
    gbc_btnDumpActivity.gridx = 1;
    gbc_btnDumpActivity.gridy = 0;
    pnlProtocolExport.add(btnDumpActivity, gbc_btnDumpActivity);

    btnDumpReceiveStats = new JButton("Receive Stats");
    GridBagConstraints gbc_btnDumpReceiveStats = new GridBagConstraints();
    gbc_btnDumpReceiveStats.insets = new Insets(0, 0, 0, 0);
    gbc_btnDumpReceiveStats.gridx = 2;
    gbc_btnDumpReceiveStats.gridy = 0;
    pnlProtocolExport.add(btnDumpReceiveStats, gbc_btnDumpReceiveStats);

    pnlViewSettings = new JPanel();
    pnlViewSettings.setBorder(
        BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("View Settings"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    GridBagConstraints gbc_pnlViewSettings = new GridBagConstraints();
    gbc_pnlViewSettings.insets = new Insets(0, 5, 5, 5);
    gbc_pnlViewSettings.fill = GridBagConstraints.BOTH;
    gbc_pnlViewSettings.gridx = 0;
    gbc_pnlViewSettings.gridy = 3;
    pnlControls.add(pnlViewSettings, gbc_pnlViewSettings);
    GridBagLayout gbl_pnlViewSettings = new GridBagLayout();
    gbl_pnlViewSettings.columnWidths = new int[] {0, 0};
    gbl_pnlViewSettings.rowHeights = new int[] {0, 0, 0, 0, 0};
    gbl_pnlViewSettings.columnWeights = new double[] {0.0, Double.MIN_VALUE};
    gbl_pnlViewSettings.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
    pnlViewSettings.setLayout(gbl_pnlViewSettings);

    chkShowRoutes = new JCheckBox("Show All Routes");
    chkShowRoutes.setSelected(pnlView.getEnvironmentDrawer().isShowRoutes());
    GridBagConstraints gbc_chkShowRoutes = new GridBagConstraints();
    gbc_chkShowRoutes.anchor = GridBagConstraints.WEST;
    gbc_chkShowRoutes.insets = new Insets(0, 0, 5, 0);
    gbc_chkShowRoutes.gridx = 0;
    gbc_chkShowRoutes.gridy = 0;
    pnlViewSettings.add(chkShowRoutes, gbc_chkShowRoutes);

    chkShowRssis = new JCheckBox("Show RSSIs");
    chkShowRssis.setSelected(pnlView.getEnvironmentDrawer().isShowRSSIs());
    GridBagConstraints gbc_chkShowRssis = new GridBagConstraints();
    gbc_chkShowRssis.insets = new Insets(0, 0, 5, 0);
    gbc_chkShowRssis.anchor = GridBagConstraints.WEST;
    gbc_chkShowRssis.gridx = 0;
    gbc_chkShowRssis.gridy = 1;
    pnlViewSettings.add(chkShowRssis, gbc_chkShowRssis);

    chkAntiAlias = new JCheckBox("Anti-Alias");
    chkAntiAlias.setSelected(pnlView.getEnvironmentDrawer().isAntiAliasEnabled());
    GridBagConstraints gbc_chkAntiAlias = new GridBagConstraints();
    gbc_chkAntiAlias.insets = new Insets(0, 0, 5, 0);
    gbc_chkAntiAlias.anchor = GridBagConstraints.WEST;
    gbc_chkAntiAlias.gridx = 0;
    gbc_chkAntiAlias.gridy = 2;
    pnlViewSettings.add(chkAntiAlias, gbc_chkAntiAlias);

    chkUpdateView = new JCheckBox("Auto View Update");
    chkUpdateView.setSelected(viewUpdater.isEnabled());
    GridBagConstraints gbc_chkUpdateView = new GridBagConstraints();
    gbc_chkUpdateView.anchor = GridBagConstraints.WEST;
    gbc_chkUpdateView.gridx = 0;
    gbc_chkUpdateView.gridy = 3;
    pnlViewSettings.add(chkUpdateView, gbc_chkUpdateView);
  }

  /**
   * Attach event handlers to GUI objects.
   */
  private void initialiseEventHandlers() {
    cboPreset.addActionListener(x -> {
      loadPreset((String) cboPreset.getSelectedItem(), (String) cboProtocol.getSelectedItem());
    });
    btnReset.addActionListener(x -> {
      loadPreset((String) cboPreset.getSelectedItem(), (String) cboProtocol.getSelectedItem());
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
        runner.stop();
        while (runner.isRunning()) {
          // Wait until stopped
        }
        loadEnvironment();
      } else {
        loadEnvironment();
        runner.start();
      }
    });

    btnDumpActivity.addActionListener(x -> {
      if (protocol != null) {
        protocol.dumpActivity();
      }
    });
    btnDumpReceiveStats.addActionListener(x -> {
      if (protocol != null) {
        protocol.printResults();
      }
    });

    cboProtocol.addActionListener(x -> {
      loadPreset((String) cboPreset.getSelectedItem(), (String) cboProtocol.getSelectedItem());
    });
    chkShowRoutes.addActionListener(x -> {
      pnlView.getEnvironmentDrawer().setShowRoutes(chkShowRoutes.isSelected());
      pnlView.repaint();
    });
    chkShowRssis.addActionListener(x -> {
      pnlView.getEnvironmentDrawer().setShowRSSIs(chkShowRssis.isSelected());
      pnlView.repaint();
    });
    chkAntiAlias.addActionListener(x -> {
      pnlView.getEnvironmentDrawer().setAntiAliasEnable(chkAntiAlias.isSelected());
      pnlView.repaint();
    });
    chkUpdateView.addActionListener(x -> {
      viewUpdater.setEnabled(chkUpdateView.isSelected());
      pnlView.repaint();
    });

    runner.addListener(new EnvironmentRunnerListener() {
      @Override
      public void update() {
        loadEnvironment();
      }
    });
    runner.addListener(viewUpdater);
  }

  /**
   * Load all presets into selection box.
   */
  private void loadPresets() {
    addPreset("9N NO Line", new NineNodeLine());
    addPreset("2N NO 100m", new TwoNodeNO(100, LoRaCfg.getDataRate0()));
    addPreset("Broadcast Test", new DataBroadcastTest());
    addPreset("Large Broadcast Test", new LargeDataBroadcastTest(6, 5, 100, true));
  }

  private void loadProtocols() {
    cboProtocol.addItem(NAIVE_PROTOCOL_1P_NAME);
    cboProtocol.addItem(NAIVE_PROTOCOL_10P_NAME);
    cboProtocol.addItem(NAIVE_PROTOCOL_10P_NO_CAD_NAME);
    cboProtocol.addItem(ADAPTIVE_PROTOCOL_NAME);
    cboProtocol.addItem(EVENTS_ONLY);
  }

  private Protocol protocol = null;

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

  private void loadPreset(String id, String strProtocol) {
    if (strProtocol == null || id == null) {
      return;
    }

    Cloner cloner = new Cloner();
    Preset preset = cloner.deepClone(presets.get(id));
    // Regenerate the preset in case it has any random effects
    preset.generate();

    pnlView.setEnvironment(preset.getEnvironment());
    runner.clearEvents();
    runner.setEnvironment(preset.getEnvironment());

    if (strProtocol.equals(EVENTS_ONLY)) {
      runner.addEvents(preset.getEvents());
      protocol = null;
    } else if (strProtocol.equals(NAIVE_PROTOCOL_1P_NAME)) {
      protocol = new NaiveBroadcastProtocol(preset.getEnvironment(), 0.01, true);
    } else if (strProtocol.equals(NAIVE_PROTOCOL_10P_NAME)) {
      protocol = new NaiveBroadcastProtocol(preset.getEnvironment(), 0.1, true);
    } else if (strProtocol.equals(NAIVE_PROTOCOL_10P_NO_CAD_NAME)) {
      protocol = new NaiveBroadcastProtocol(preset.getEnvironment(), 0.1, false);
    } else if (strProtocol.equals(ADAPTIVE_PROTOCOL_NAME)) {
      protocol = new AdaptiveBroadcastProtocol(preset.getEnvironment());
    }
    loadEnvironment();
    EnvironmentDrawer drawer = pnlView.getEnvironmentDrawer();
    drawer.centreView(EnvironmentDrawer.getViewSpace(pnlView.getSize()));
    drawer.setShowRoutes(chkShowRoutes.isSelected());
    drawer.setShowRSSIs(chkShowRssis.isSelected());
    drawer.setAntiAliasEnable(chkAntiAlias.isSelected());
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
      btnPlus1.setEnabled(false);
      btnPlus10.setEnabled(false);
      btnPlus100.setEnabled(false);
      btnDumpActivity.setEnabled(false);
      btnDumpReceiveStats.setEnabled(false);
      cboProtocol.setEnabled(false);
    } else {
      btnRun.setText("Start");
      btnReset.setEnabled(true);
      cboPreset.setEnabled(true);
      cboTimeStep.setEnabled(true);
      btnPlus1.setEnabled(true);
      btnPlus10.setEnabled(true);
      btnPlus100.setEnabled(true);
      btnDumpActivity.setEnabled(true);
      btnDumpReceiveStats.setEnabled(true);
      cboProtocol.setEnabled(true);
    }
  }

  class ViewUpdater implements EnvironmentRunnerListener {
    private boolean enabled = true;
    private boolean waitForUpdate = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    @Override
    public void update() {
      if (!enabled) {
        return;
      }
      pnlView.setUpdateNeeded();
      pnlView.repaint();
      // Wait until repaint has occurred on other thread
      while (waitForUpdate && !pnlView.isUpToDate()) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

}
