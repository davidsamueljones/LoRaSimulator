package ecs.soton.dsj1n15.smesh.model;

public class ReceiveData {
  public final double time;
  
  public final double snr;
  
  public final double rssi;
  
  public ReceiveData(double time, double snr, double rssi) {
    this.time = time;
    this.snr = snr;
    this.rssi = rssi;
  }
  
}

