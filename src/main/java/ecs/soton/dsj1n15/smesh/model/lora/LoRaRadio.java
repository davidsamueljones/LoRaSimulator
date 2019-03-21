package ecs.soton.dsj1n15.smesh.model.lora;

import ecs.soton.dsj1n15.smesh.model.Packet;
import ecs.soton.dsj1n15.smesh.model.Radio;
import ecs.soton.dsj1n15.smesh.model.Transmission;

public class LoRaRadio extends Radio {
  public static final double MAX_SENSITIVITY = -137;
  public static final double DEFAULT_ANTENNA_GAIN = 3;
  public static final double DEFAULT_CABLE_LOSS = 1.5;

  /** Radio configuration */
  private final LoRaCfg cfg;

  /** Antenna gain */
  private double antennaGain;

  /** Cable loss */
  private double cableLoss;

  /** Current transmission */
  private Transmission transmission = null;

  /**
   * Instantiate a LoRa Radio using the default configuration.
   * 
   * @param id ID of the radio
   */
  public LoRaRadio(int id) {
    this(id, LoRaCfg.getDefault()); // FIXME
  }


  public LoRaRadio(int id, LoRaCfg cfg) {
    super(id);
    this.cfg = cfg;
    this.antennaGain = DEFAULT_ANTENNA_GAIN;
    this.cableLoss = DEFAULT_CABLE_LOSS;
  }

  public LoRaCfg getLoRaCfg() {
    return cfg;
  }

  @Override
  public double getFrequency() {
    return cfg.getFreq();
  }

  @Override
  public double getAntennaHeight() {
    return getZ();
  }

  @Override
  public double getSensitivity() {
    return getNoiseFloor() + getRequiredSNR(cfg.getSF());
  }

  /**
   * Get the SNR required for demodulation using a given spreading factor.
   * 
   * @param sf LoRa spreading factor (7-12)
   * @return The required SNR in dBm
   */
  public static double getRequiredSNR(int sf) {
    return ((sf - LoRaCfg.MIN_SF) * -2.5) - 5;
  }


  @Override
  public double getAntennaGain() {
    return antennaGain;
  }

  public void setAntennaGain(double antennaGain) {
    this.antennaGain = antennaGain;
  }

  @Override
  public double getCableLoss() {
    return cableLoss;
  }

  public void setCableLoss(double cableLoss) {
    this.cableLoss = cableLoss;
  }

  @Override
  public int getBandwidth() {
    return cfg.getBW();
  }

  @Override
  public double getNoiseFigure() {
    return 6;
  }

  @Override
  public int getTxPow() {
    return cfg.getTxPow();
  }

  /**
   * {@inheritDoc} <br>
   * The maximum SNR of the LoRa demodulator (RFM95/SX1276/etc...) is limited to 10.
   */
  @Override
  public double validateSNR(double snr) {
    return Math.min(snr, 10);
  }

  @Override
  public Transmission send(Packet packet) {
    if (transmission != null) {
      throw new IllegalStateException("Node is already transmitting");
    }
    long airtime = cfg.calculatePacketAirtime(packet.length);
    transmission = new Transmission(this, packet, environment.getTime(), airtime);
    return transmission;
  }

  @Override
  public Packet recv() {
    for (Transmission transmission : environment.getTransmissions()) {
      Radio sender = transmission.sender;
      // if ()
      //
      // if (sender.getFrequency() == getFrequency() && sender instanceof ) {
      //
      // }
    }
    return null;
  }

  @Override
  public boolean activityDetection() {
    // Can't detect a transmission that isn't LoRa
    // if (!(transmission instanceof LoRaTransmission)) {
    // return false;
    // }
    // TODO: Attempt to detect preamble
    return false;
  }

  @Override
  public Transmission getCurrentTransmission() {
    return transmission;
  }

  @Override
  public boolean canCommunicate(Radio rx) {
    if (rx instanceof LoRaRadio) {
      boolean receivable = true;
      // Check generic parameters
      receivable &= (getFrequency() == rx.getFrequency());
      receivable &= (getBandwidth() == rx.getBandwidth());
      // Check LoRa specific parameters
      LoRaCfg rxCfg = ((LoRaRadio) rx).getLoRaCfg();
      receivable &= (cfg.getSF() == rxCfg.getSF());
      receivable &= (cfg.getPreambleSymbols() >= rxCfg.getPreambleSymbols());
      receivable &= (cfg.isExplicitHeader() == rxCfg.isExplicitHeader());
      // Some parameters may be different if explicit header is enabled
      if (!cfg.isExplicitHeader() && !rxCfg.isExplicitHeader()) {
        receivable &= (cfg.getCR() == rxCfg.getCR());
        // TODO: Add packet length check
      }
      return receivable;
    }
    return false;
  }

  @Override
  public boolean canInterfere(Radio rx) {
    // Calculate receiver frequency range
    double rxMin = rx.getFrequency() - rx.getBandwidth() / 1e6;
    double rxMax = rx.getFrequency() + rx.getBandwidth() / 1e6;
    // Calculate interferer frequency range
    double txMin = getFrequency() - getBandwidth() / 1e6;
    double txMax = getFrequency() + getBandwidth() / 1e6;
    // If the frequencies cross at all assume interference is possible
    if (txMin <= rxMax && txMax >= rxMin) {
      // Special behaviour with other LoRa signals allows those with a different chirp rate to be
      // ignored (they are orthogonal)
      if (rx instanceof LoRaRadio) {
        double chirpRate = ((LoRaRadio) rx).getChirpRate();
        if (chirpRate != getChirpRate()) {
          return false;
        }
      }
      // For other signals just expect the interference
      return true;
    } else {
      return false;
    }
  }

  public double getChirpRate() {
    double bw = getBandwidth() / 1e6;
    return (bw * bw) / Math.pow(2, cfg.getSF());
  }

  @Override
  public void timePassed() {
    // TODO Auto-generated method stub

  }

  @Override
  public String toString() {
    return String.format("LoRaRadio [id=%d, pos=(%d, %d)]", id, (int) x, (int) y);
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof LoRaRadio))
      return false;
    LoRaRadio other = (LoRaRadio) obj;
    if (id != other.id)
      return false;
    return true;
  }

}
