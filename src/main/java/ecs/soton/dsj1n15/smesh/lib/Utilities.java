package ecs.soton.dsj1n15.smesh.lib;

import java.io.PrintWriter;
import java.util.Random;

/**
 * Collection of extraneous utilities for whole project.
 * 
 * @author David Jones (dsj1n15)
 */
public class Utilities {
  public static Random RANDOM = new Random(0);
  
  /**
   * @param mw Value in mW
   * @return Value in dBm
   */
  public static double mw2dbm(double mw) {
    return 10 * Math.log10(mw);
  }

  /**
   * @param dbm Value in dBm
   * @return Value in mW
   */
  public static double dbm2mw(double dbm) {
    return Math.pow(10, dbm / 10);
  }
  
  /**
   * Helper function for writing to an output source and the console.
   * 
   * @param pw Extra print writer to use for output
   * @param string String to print
   */
  public static void printAndWrite(PrintWriter pw, String string) {
    System.out.print(string);
    pw.write(string);
  }
  
}
