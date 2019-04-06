package ecs.soton.dsj1n15.smesh.radio;

public class ReceiveResult {
  public final Status status;

  public final Transmission transmission;

  public final long time;

  public final double snr;

  public final double rssi;

  private ReceiveResult(Status status, Transmission transmission, long time, double snr,
      double rssi) {
    this.status = status;
    this.transmission = transmission;
    this.time = time;
    this.snr = snr;
    this.rssi = rssi;
  }

  public static ReceiveResult getCollisionResult(Transmission transmission, long time) {
    return new ReceiveResult(Status.COLLISION, transmission, time, 0, 0);
  }
  
  public static ReceiveResult getCRCFailResult(Transmission transmission, long time, double snr,
      double rssi) {
    return new ReceiveResult(Status.CRC_FAIL, transmission, time, snr, rssi);
  }

  public static ReceiveResult getSuccessResult(Transmission transmission, long time, double snr,
      double rssi) {
    return new ReceiveResult(Status.SUCCESS, transmission, time, snr, rssi);
  }

  public enum Status {
    COLLISION, CRC_FAIL, SUCCESS
  }

}
