package ecs.soton.dsj1n15.smesh.model;

public class LoRaCfg {
  public static final int MIN_SF = 6;
  public static final int MAX_SF = 12;
  
  public static final double BAND_868_MHZ = 868f;
  public static final double BAND_869_MHZ = 869.525f;
  
  /** The centre frequency in MHz */
  private double freq;

  /** The spreading factor */
  private int sf;

  /** The transmit power in dB */
  private int txPow;

  /** The centre frequency in Hz */
  private int bw;

  /** Coding rate 4 / n */
  private int cr;

  /** Number of preamble symbols */
  private int preambleSymbols;

  /** Whether CRC is enabled */
  private boolean crc;

  /** Whether the explicit header is enabled */
  private boolean explicitHeader;

  public double getFreq() {
    return freq;
  }

  public void setFreq(double freq) {
    this.freq = freq;
  }

  public int getSF() {
    return sf;
  }

  public void setSF(int sf) {
    this.sf = sf;
  }

  public int getTxPow() {
    return txPow;
  }

  public void setTxPow(int txPow) {
    this.txPow = txPow;
  }

  public int getBW() {
    return bw;
  }

  public void setBW(int bw) {
    this.bw = bw;
  }

  public int getCR() {
    return cr;
  }

  public void setCR(int cr) {
    this.cr = cr;
  }

  public int getPreambleSymbols() {
    return preambleSymbols;
  }

  public void setPreambleSymbols(int preambleSymbols) {
    this.preambleSymbols = preambleSymbols;
  }

  public boolean isCRC() {
    return crc;
  }

  public void setCrc(boolean crc) {
    this.crc = crc;
  }

  public boolean isExplicitHeader() {
    return explicitHeader;
  }

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
   * Calculate the airtime for a given LoRa configuration and packet length. All equations used from
   * (modified slightly for our formats): <br>
   * https://www.semtech.com/uploads/documents/LoraDesignGuide_STD.pdf
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
