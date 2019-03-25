package ecs.soton.dsj1n15.smesh.radio;

public class ReceiveResult {
  public final Status status;

  public final Packet packet;

  public final long time;

  public final double snr;

  public final double rssi;

  private ReceiveResult(Status status, Packet packet, long time, double snr, double rssi) {
    this.status = status;
    this.packet = packet;
    this.time = time;
    this.snr = snr;
    this.rssi = rssi;
  }
 
  public static ReceiveResult getCollisionResult(long time, double snr, double rssi) {
    return new ReceiveResult(Status.COLLISION, null, time, snr, rssi);
  }
  
  public static ReceiveResult getCRCFailResult(long time, double snr, double rssi) {
    return new ReceiveResult(Status.CRC_FAIL, null, time, snr, rssi);
  }
  
  public static ReceiveResult getSuccessResult(Packet packet, long time, double snr, double rssi) {
    return new ReceiveResult(Status.SUCCESS, null, time, snr, rssi);
  }
  
  public enum Status {
    COLLISION, CRC_FAIL, SUCCESS
  }

}
