package ecs.soton.dsj1n15.smesh.lib;

/**
 * Very simple static wrapper for sending print statements that can be toggled off globally.
 * 
 * @author David Jones (dsj1n15)
 */
public class Debugger {
  /** Whether output is enabled for any debug reports */
  private static boolean outputEnabled = true;

  /**
   * This class should not be instantiated.
   */
  private Debugger() {}

  /**
   * @param outputEnabled Whether debug output should be enabled globally
   */
  public static void setOutputEnabled(boolean outputEnabled) {
    Debugger.outputEnabled = outputEnabled;
  }

  /**
   * @return Whether global debug output is enabled
   */
  public static boolean isOutputEnabled() {
    return Debugger.outputEnabled;
  }

  /**
   * Print with no linefeed if output is enabled.<br>
   * See {@link System.out#print()}.
   * 
   * @param obj Object to print
   */
  public static void print(Object obj) {
    if (Debugger.isOutputEnabled()) {
      System.out.print(obj.toString());
    }
  }

  /**
   * Print with linefeed if output is enabled.<br>
   * See {@link System.out#println()}.
   * 
   * @param obj Object to print
   */
  public static void println(Object obj) {
    if (Debugger.isOutputEnabled()) {
      System.out.println(obj.toString());
    }
  }

}
