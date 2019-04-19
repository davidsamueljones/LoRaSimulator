package ecs.soton.dsj1n15.smesh.model.lora;

import ecs.soton.dsj1n15.smesh.lib.Utilities;

/**
 * A LoRa Radio that uses the test model for the RFM95. All LoRa radio behaviour is valid with
 * empirical values for sensitivities, noise figure, and receive probability sigmoids.
 * 
 * @author David Jones (dsj1n15)
 */
public class RFM95W extends LoRaRadio {

  /**
   * Instantiate a RFM95W using the default configuration.
   * 
   * @param id ID of the radio
   */
  public RFM95W(int id) {
    this(id, LoRaCfg.getDefault());
  }


  public RFM95W(int id, LoRaCfg cfg) {
    super(id, cfg);
  }

  @Override
  public double getRequiredSNR() {
    double[] sfs = {-7.5, -10, -12.5, -15, -15.5, -16};
    return sfs[cfg.getSF() - (LoRaCfg.MIN_SF + 1)];
  }

  /**
   * {@inheritDoc} <br>
   * The empirical noise figure of the RFM95W is 15.
   */
  @Override
  public double getNoiseFigure() {
    return 20;
  }

  /**
   * {@inheritDoc} <br>
   * The empirical maximum SNR of the RFM95W is 15.
   */
  @Override
  public double validateSNR(double snr) {
    return Math.min(snr, 15);
  }

  @Override
  public double getReceiveProbability(double snr) {
    // Use emperical sigmoid to determine whether it is receivable
    double[][] params = {//
        {0, 0.9789, -7.3760, 0.5901}, // SF7
        {0, 0.9887, -9.3538, 0.3246}, // SF8
        {0, 0.9887, -12.4933, 0.4004}, // SF9
        {0, 0.9500, -15.7076, 0.4979}, // SF10
        {0, 0.9938, -15.9790, 0.6656}, // SF11
        {0, 0.9922, -16.6661, 0.6838}}; // SF12
    double[] param = params[cfg.getSF() - (LoRaCfg.MIN_SF + 1)];
    param[2] -= 1; // Slight shift due to unexpected simulation bias
    double prob =
        param[0] + (param[1] - param[0]) / (1 + Math.pow(10, ((param[2] - snr) * param[3])));

    // Add some variance based on the SNR
    double snrMaxLimitDist = 2.5;
    double snrDist = Math.abs(snr - getRequiredSNR());
    if (snrDist < snrMaxLimitDist) {
      double mult = (snrMaxLimitDist - snrDist) / snrMaxLimitDist;
      mult = Math.min(prob, Math.min(1 - prob, mult));
      prob += (mult * (Utilities.RANDOM.nextDouble() * 2 - 1));
    }
    return Math.min(Math.max(0, prob), 1);
  }

  @Override
  public String toString() {
    return String.format("RFM95W [id=%d, pos=(%d, %d)]", id, (int) x, (int) y);
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
    if (id != other.getID())
      return false;
    return true;
  }

}
