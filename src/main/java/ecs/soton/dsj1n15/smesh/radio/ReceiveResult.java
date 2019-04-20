package ecs.soton.dsj1n15.smesh.radio;

/**
 * Receive result that gets created when a radio has a successful or unsuccessful receive.
 * 
 * @author David Jones (dsj1n15)
 */
public class ReceiveResult {
  /** Status of whether the receive was successful and why */
  public final Status status;
  /** Simulator understanding of whether the receive was successful and why */
  public final MetadataStatus metadataStatus;
  /** The transmission being received, extract the packet from here */
  public final Transmission transmission;
  /** The time the receive occurred */
  public final long time;
  /** The average SNR over the full receive */
  public final double snr;
  /** The average RSSI over the full receive */
  public final double rssi;

  /**
   * Create a fully defined receive result.
   * 
   * @param status The status as known by the receiver
   * @param metadataStatus The underlying reason for a failed receive
   * @param transmission The transmission being received
   * @param time The time the receive occurred
   * @param snr The average SNR over the full receive
   * @param rssi The average RSSI over the full receive
   */
  public ReceiveResult(Status status, MetadataStatus metadataStatus, Transmission transmission,
      long time, double snr, double rssi) {
    this.status = status;
    this.metadataStatus = metadataStatus;
    this.transmission = transmission;
    this.time = time;
    this.snr = snr;
    this.rssi = rssi;
  }

  /**
   * Create a receive result without snr and rssi values.
   * 
   * @param status The status as known by the receiver
   * @param metadataStatus The underlying reason for a failed receive
   * @param transmission The transmission being received
   * @param time The time the receive occurred
   */
  public ReceiveResult(Status status, MetadataStatus metadataStatus, Transmission transmission,
      long time) {
    this(status, metadataStatus, transmission, time, 0, 0);
  }


  /**
   * @return Whether the receiver would actually be aware of the receive result.
   */
  public boolean isReceiverAware() {
    return status != Status.UNAWARE_FAIL;
  }

  /**
   * @return Whether the receiver would know the SNR value
   */
  public boolean isReceiverSNRValid() {
    return status == Status.SUCCESS || status == Status.FAIL_CRC;
  }

  /**
   * @return Whether the receiver would know the RSSI value
   */
  public boolean isReceiverRSSIValid() {
    return true;
  }

  /**
   * Enumeration of possible reasons a transmission was or was not received, as known by a receiver.
   * 
   * @author David Jones (dsj1n15)
   */
  public enum Status {
    SUCCESS, // Successful receive
    FAIL_COLLISION, // Two competing packets at preamble stage
    FAIL_CRC, // Some problem with the payload
    UNAWARE_FAIL // Check metadata, receiver unaware of this receive
  }

  /**
   * Enumeration of possible reasons a transmission was or was not received, these are a lower level
   * than would be known by receiver.
   * 
   * @author David Jones (dsj1n15)
   */
  public enum MetadataStatus {
    SUCCESS, // Successful receive
    FAIL_MISSED, // Missed receive, possibly due to being busy
    FAIL_NO_PREAMBLE, // Failed to receive preamble
    FAIL_PREAMBLE_COLLISION, // Two competing packets at preamble stage
    FAIL_PAYLOAD_COLLISION, // Two competing packets at payload stage (bad crc)
    FAIL_PAYLOAD_WEAK, // Weak signal at payload stage (bad crc)
  }

}
