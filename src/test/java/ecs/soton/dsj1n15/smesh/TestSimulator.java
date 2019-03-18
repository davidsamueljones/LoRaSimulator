package ecs.soton.dsj1n15.smesh;

import java.awt.EventQueue;
import javax.swing.UIManager;
import ecs.soton.dsj1n15.smesh.view.SimulatorFrame;

/**
 * Main class. Used to start the simulator.
 *
 * @author David Jones [dsj1n15]
 */
public class TestSimulator {

  /**
   * The main method. Opens a new simulator frame using the Event Dispatch Thread.
   *
   * @param args Passed arguments [Program uses no arguments]
   */
  public static void main(String[] args) {
    // Get natural GUI appearance
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      System.err.println("Error setting look and feel");
    }
    // Create new simulator on EDT
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          SimulatorFrame frame = new SimulatorFrame();
          frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

}
