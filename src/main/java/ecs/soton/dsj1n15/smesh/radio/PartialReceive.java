package ecs.soton.dsj1n15.smesh.radio;

/**
 * Partial receive for a single time slice for a receiver.
 * 
 * @author David Jones (dsj1n15)
 */
public class PartialReceive {
  /** The transmission being received */
  public final Transmission transmission;
  /** The time the receive occurred */
  public final long time;
  /** The SNR at the time of receive */
  public final double snr;
  /** The RSSI at the time of receive */
  public final double rssi;

  /**
   * Create a new partial receive.
   * 
   * @param transmission Transmission being received
   * @param time Time receive occurred
   * @param snr SNR at the time of receive
   * @param rssi RSSI at the time of receive
   */
  public PartialReceive(Transmission transmission, long time, double snr, double rssi) {
    this.transmission = transmission;
    this.time = time;
    this.snr = snr;
    this.rssi = rssi;
  }

}

