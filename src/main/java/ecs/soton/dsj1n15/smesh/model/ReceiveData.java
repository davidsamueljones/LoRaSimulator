package ecs.soton.dsj1n15.smesh.model;

public class ReceiveData {
  public final Transmission transmission;
  
  public final long time;
  
  public final double snr;
  
  public final double rssi;
  
  public ReceiveData(Transmission transmission, long time, double snr, double rssi) {
    this.transmission = transmission;
    this.time = time;
    this.snr = snr;
    this.rssi = rssi;
  }
  
}

