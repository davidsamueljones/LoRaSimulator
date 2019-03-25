package ecs.soton.dsj1n15.smesh.lib;

public class Utilities {
  
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
