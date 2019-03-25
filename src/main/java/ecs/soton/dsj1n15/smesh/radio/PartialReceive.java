package ecs.soton.dsj1n15.smesh.radio;

public class PartialReceive {
  public final Transmission transmission;
  
  public final long time;
  
  public final double snr;
  
  public final double rssi;
  
  public PartialReceive(Transmission transmission, long time, double snr, double rssi) {
    this.transmission = transmission;
    this.time = time;
    this.snr = snr;
    this.rssi = rssi;
  }
  
}

