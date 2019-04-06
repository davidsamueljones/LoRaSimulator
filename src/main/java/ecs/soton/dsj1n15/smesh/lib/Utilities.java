package ecs.soton.dsj1n15.smesh.lib;

import java.util.Random;

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
  
}
