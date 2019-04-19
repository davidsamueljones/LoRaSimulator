package ecs.soton.dsj1n15.smesh.radio;

/**
 * Receive result that gets created when a radio has a successful or unsuccessful receive.
 * 
 * @author David Jones (dsj1n15)
 */
public class ReceiveResult {
  /** Status of whether the receive was successful and why */
  public final Status status;
  /** The transmission being received, extract the packet from here */
  public final Transmission transmission;
  /** The time the receive occurred */
  public final long time;
  /** The average SNR over the full receive */
  public final double snr;
  /** The average RSSI over the full receive */
  public final double rssi;

  /**
   * Private constructor for creating a receive result.
   */
  private ReceiveResult(Status status, Transmission transmission, long time, double snr,
      double rssi) {
    this.status = status;
    this.transmission = transmission;
    this.time = time;
    this.snr = snr;
    this.rssi = rssi;
  }

  /**
   * Helper function for creating a successful receive result.
   * 
   * @param transmission Transmission that was received
   * @param time Time that receive occurred
   * @param snr SNR of packet received
   * @param rssi RSSI of packet received
   * @return
   */
  public static ReceiveResult getSuccessResult(Transmission transmission, long time, double snr,
      double rssi) {
    return new ReceiveResult(Status.SUCCESS, transmission, time, snr, rssi);
  }

  /**
   * Helper function for creating a collision result.
   * 
   * @param transmission Transmission that got a collision
   * @param time Time collision occured
   * @return Created result
   */
  public static ReceiveResult getCollisionResult(Transmission transmission, long time) {
    return new ReceiveResult(Status.COLLISION, transmission, time, 0, 0);
  }

  /**
   * Helper function for creating a CRC fail result.
   * 
   * @param transmission Transmission that got a CRC Fail
   * @param time Time CRC fail occurred
   * @param snr SNR of packet that failed
   * @param rssi RSSI of packet that failed
   * @return Created result
   */
  public static ReceiveResult getCRCFailResult(Transmission transmission, long time, double snr,
      double rssi) {
    return new ReceiveResult(Status.CRC_FAIL, transmission, time, snr, rssi);
  }

  /**
   * Possible receive statuses.
   * 
   * @author David Jones (dsj1n15)
   */
  public enum Status {
    SUCCESS, COLLISION, CRC_FAIL
  }

}
