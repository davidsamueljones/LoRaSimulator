package ecs.soton.dsj1n15.smesh.model.lora;

/**
 * A full configuration for a LoRa radio with methods available for determining airtime using LoRa
 * modulation consistent across all LoRa radios.
 * 
 * @author David Jones (dsj1n15)
 */
public class LoRaCfg {
  /*
   * Coding rates
   */
  public static final int CR_4_5 = 5;
  public static final int CR_4_6 = 6;
  public static final int CR_4_7 = 7;
  public static final int CR_4_8 = 8;

  /*
   * Spreading factor limits
   */
  public static final int MIN_SF = 6;
  public static final int MAX_SF = 12;


  /*
   * 125kHz or 250kHz (1% duty cycle) - 14dBm
   */
  public static final double BAND_G1_C0_MHZ = 868.1f;
  public static final double BAND_G1_C1_MHZ = 868.3f;
  public static final double BAND_G1_C2_MHZ = 868.5f;
  public static final double BAND_G_C3_MHZ = 867.1f;
  public static final double BAND_G_C4_MHZ = 867.3f;
  public static final double BAND_G_C5_MHZ = 867.5f;
  public static final double BAND_G_C6_MHZ = 867.7f;
  public static final double BAND_G_C7_MHZ = 867.9f;

  /*
   * 125kHz or 250kHz (10% duty cycle) - 27dBm
   */
  public static final double BAND_G3_MID_MHZ = 869.525f;

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
    double symbolTime = getSymbolTime(cfg); // ms
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
    return (int) Math.ceil((cfg.preambleSymbols + 4.25) * getSymbolTime(cfg));
  }

  /**
   * Sync requires 5 preamble symbols before sync and margin. Determine how long this is in ms.
   * https://www.semtech.com/uploads/documents/an1200.23.pdf
   * 
   * @param cfg LoRa configuration for determining timing
   * @return Preamble time in ms
   */
  public static int requiredPreambleTime(LoRaCfg cfg) {
    return (int) Math.ceil((5 + 4.25) * getSymbolTime(cfg));
  }

  /**
   * The symbol airtime for a given LoRa configuration.
   * 
   * @param cfg LoRa configuration
   * @return Send time for symbol in ms
   */
  public static double getSymbolTime(LoRaCfg cfg) {
    return 1000.0 * Math.pow(2, cfg.sf) / cfg.bw; // ms
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


  /**
   * @return Profile configured for LoRaWAN D0
   */
  public static LoRaCfg getDataRate0() {
    LoRaCfg cfg = getDefault();
    cfg.setSF(12);
    return cfg;
  }

  /**
   * @return Profile configured for LoRaWAN D1
   */
  public static LoRaCfg getDataRate1() {
    LoRaCfg cfg = getDefault();
    cfg.setSF(11);
    return cfg;
  }

  /**
   * @return Profile configured for LoRaWAN D2
   */
  public static LoRaCfg getDataRate2() {
    LoRaCfg cfg = getDefault();
    cfg.setSF(10);
    return cfg;
  }

  /**
   * @return Profile configured for LoRaWAN D3
   */
  public static LoRaCfg getDataRate3() {
    LoRaCfg cfg = getDefault();
    cfg.setSF(9);
    return cfg;
  }

  /**
   * @return Profile configured for LoRaWAN D4
   */
  public static LoRaCfg getDataRate4() {
    LoRaCfg cfg = getDefault();
    cfg.setSF(8);
    return cfg;
  }

  /**
   * @return Profile configured for LoRaWAN D5
   */
  public static LoRaCfg getDataRate5() {
    LoRaCfg cfg = getDefault();
    cfg.setSF(7);
    return cfg;
  }

  /**
   * @return Profile configured for LoRaWAN D6
   */
  public static LoRaCfg getDataRate6() {
    LoRaCfg cfg = getDefault();
    cfg.setSF(7);
    cfg.setBW(250000);
    return cfg;
  }

  /**
   * Default parameters, by default uses G3 ETSI band.
   * 
   * @return Profile with default LoRaWAN configuration
   */
  public static LoRaCfg getDefault() {
    LoRaCfg cfg = new LoRaCfg();
    cfg.setFreq(BAND_G3_MID_MHZ);
    cfg.setPreambleSymbols(8);
    cfg.setSF(MAX_SF);
    cfg.setTxPow(14);
    cfg.setBW(125000);
    cfg.setCrc(true);
    cfg.setExplicitHeader(true);
    cfg.setCR(CR_4_5);
    return cfg;
  }
  
  /**
   * @return Get the LoRa chirp rate
   */
  public double getChirpRate() {
    double bw = this.bw / 1e6;
    return (bw * bw) / Math.pow(2, sf);
  }

}
