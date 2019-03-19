package ecs.soton.dsj1n15.smesh.model.lora;

/**
 * A full configuration for a LoRa radio with methods available for determining airtime using LoRa
 * modulation consistent across all LoRa radios.
 * 
 * @author David Jones (dsj1n15)
 */
public class LoRaCfg {
  public static final int CR_4_5 = 5;
  public static final int CR_4_6 = 6;
  public static final int CR_4_7 = 7;
  public static final int CR_4_8 = 8;

  public static final int MIN_SF = 6;
  public static final int MAX_SF = 12;

  public static final double BAND_868_MHZ = 868f;
  public static final double BAND_869_MHZ = 869.525f;

  private double freq;
  private int sf;
  private int txPow;
  private int bw;
  private int cr;
  private int preambleSymbols;
  private boolean crc;
  private boolean explicitHeader;

  /**
   * @return The current centre frequency in MHz
   */
  public double getFreq() {
    return freq;
  }

  /**
   * @param freq The new centre frequency in MHz
   */
  public void setFreq(double freq) {
    this.freq = freq;
  }

  /**
   * @return The current LoRa spreading factor (chips = 2 ^ sf)
   */
  public int getSF() {
    return sf;
  }

  /**
   * @param sf The new LoRa spreading factor (chips = 2 ^ sf)
   */
  public void setSF(int sf) {
    this.sf = sf;
  }

  /**
   * @return The current transmission power in dBm
   */
  public int getTxPow() {
    return txPow;
  }

  /**
   * @param txPow The new transmission power in dBm
   */
  public void setTxPow(int txPow) {
    this.txPow = txPow;
  }

  /**
   * @param bw The new bandwidth in Hz
   */
  public int getBW() {
    return bw;
  }

  /**
   * @param bw The new bandwidth in Hz
   */
  public void setBW(int bw) {
    this.bw = bw;
  }

  /**
   * @return The current denominator for the coding rate. Actual CR = 4 / cr.
   */
  public int getCR() {
    return cr;
  }

  /**
   * @param cr The new denominator for the coding rate. Actual CR = 4 / cr.
   */
  public void setCR(int cr) {
    this.cr = cr;
  }

  /**
   * @return The current number of user programmable preamble symbols to send (actual -4.25)
   */
  public int getPreambleSymbols() {
    return preambleSymbols;
  }

  /**
   * @param preambleSymbols The new number of user programmable preamble symbols to send (actual
   *        -4.25)
   */
  public void setPreambleSymbols(int preambleSymbols) {
    this.preambleSymbols = preambleSymbols;
  }

  /**
   * @return Whether the CRC is enabled
   */
  public boolean isCRC() {
    return crc;
  }

  /**
   * @param crc Whether the CRC should be enabled
   */
  public void setCrc(boolean crc) {
    this.crc = crc;
  }

  /**
   * @return Whether the explicit header is enabled
   */
  public boolean isExplicitHeader() {
    return explicitHeader;
  }

  /**
   * @param explicitHeader Whether the explicit header should be enabled
   */
  public void setExplicitHeader(boolean explicitHeader) {
    this.explicitHeader = explicitHeader;
  }

  /**
   * Calculate packet airtime for this instance. <br>
   * See {@link #calculatePacketAirtime(LoRaCfg cfg, int packetLen)}.
   * 
   * @param packetLen Length of packet to send
   * @return Total send time for packet in ms
   */
  public int calculatePacketAirtime(int packetLen) {
    return calculatePacketAirtime(this, packetLen);
  }

  /**
   * Calculate preamble airtime for this instance. <br>
   * See {@link #calculatePreambleTime(LoRaCfg cfg)}.
   *
   * @return Total send time for packet in ms
   */
  public int calculatePreambleTime() {
    return calculatePreambleTime(this);
  }

  /**
   * Calculate the airtime for a full packet transmission for a given LoRa configuration and packet
   * length. <br>
   * All equations (modified for our formats) from: <a
   * href=https://www.semtech.com/uploads/documents/LoraDesignGuide_STD.pdf>LoRa Design Guide</a>
   * 
   * @param cfg LoRa configuration
   * @param packetLen Length of packet to send
   * @return Total send time for packet in ms
   */
  public static int calculatePacketAirtime(LoRaCfg cfg, int packetLen) {
    double symbolTime = 1000.0 * Math.pow(2, cfg.sf) / (double) cfg.bw; // ms
    double preambleTime = (cfg.preambleSymbols + 4.25) * symbolTime;
    boolean ldr = isLDRRequired(cfg);
    int pscTop = 8 * packetLen - 4 * cfg.sf + 28 + 16 - 20 * (cfg.explicitHeader ? 1 : 0);
    int pscBot = 4 * (cfg.sf - 2 * (ldr ? 1 : 0));
    int pscLHS = (int) Math.ceil(pscTop / pscBot);
    int payloadSymbolCount = 8 + (Math.max(pscLHS * (cfg.cr), 0));
    double payloadTime = payloadSymbolCount * symbolTime;
    double totalTime = preambleTime + payloadTime;
    return (int) Math.ceil(totalTime);
  }

  /**
   * Calculate the preamble airtime for a given LoRa configuration. See
   * {@link #calculatePacketAirtime(LoRaCfg cfg, int packetLen)} for full packet airtime calculation
   * and equation reference.
   * 
   * @param cfg LoRa configuration
   * @return Send time for preamble in ms
   */
  public static int calculatePreambleTime(LoRaCfg cfg) {
    double symbolTime = 1000.0 * Math.pow(2, cfg.sf) / (double) cfg.bw; // ms
    return (int) Math.ceil((cfg.preambleSymbols + 4.25) * symbolTime);
  }

  /**
   * Check if low data rate is required for this instance.<br>
   * See {@link #sLDRRequired(LoRaCfg cfg)}.
   * 
   * @return Whether LDR is required
   */

  public boolean isLDRRequired() {
    return isLDRRequired(this);
  }

  /**
   * Check if low data rate is required. Value of 16.0 for symbol time required for enabling low
   * data rate is copied from RadioHead library. No source provided but keep the same for
   * consistency.
   * 
   * @param cfg LoRa configuration to check.
   * @return Whether LDR is required
   */
  public static boolean isLDRRequired(LoRaCfg cfg) {
    double symbol_time = 1000.0 * Math.pow(2, cfg.sf) / cfg.bw; // ms
    return symbol_time > 16.0;
  }

}
