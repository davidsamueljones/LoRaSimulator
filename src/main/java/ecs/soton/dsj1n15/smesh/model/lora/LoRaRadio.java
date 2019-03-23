package ecs.soton.dsj1n15.smesh.model.lora;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.model.Packet;
import ecs.soton.dsj1n15.smesh.model.Radio;
import ecs.soton.dsj1n15.smesh.model.ReceiveData;
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
  private Transmission tx = null;

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
    if (tx != null) {
      throw new IllegalStateException("Node is already transmitting");
    }
    long airtime = cfg.calculatePacketAirtime(packet.length);
    tx = new Transmission(this, packet, environment.getTime(), airtime);
    return tx;
  }

  public Transmission synced;

  @Override
  public void listen() {
    Long globalTime = environment.getTime();
    if (timeMap.containsKey(globalTime)) {
      throw new IllegalStateException("Already listened at this point");
    }
    timeMap.put(globalTime, null);

    // Find the receive to capture in this slot
    ReceiveData receive = null;
    double rssi = environment.getRSSI(this);
    for (Transmission transmission : environment.getTransmissions()) {
      // Ignore transmission messages that have finished
      if (transmission.endTime < globalTime) {
        continue;
      }
      // Ignore radios we can't listen to, this won't ignore noise of those that can interfere
      if (!canCommunicate(transmission.sender)) {
        continue;
      }
      // Ignore the transmission if it isn't successfully received
      double snr = environment.getReceiveSNR(transmission.sender, this);
      // Mix a bit of random noise in
      Random r = new Random();
      snr = snr + r.nextInt(5) - 2;

      // Capture if the signal is more likely to be received than the last
      boolean better;
      int syncbonus = 3;
      if (receive == null) {
        better = true;
      } else {
        if (receive.transmission == synced) {
          better = snr > (receive.snr + syncbonus);
        } else if (transmission == synced) {
          better = (snr + syncbonus) > receive.snr;
        } else {
          better = snr > receive.snr;
        }
      }
      if (better) {
        receive = new ReceiveData(transmission, globalTime, snr, rssi);
        // Keep track of transmissions in both time and by transmission for efficiency
        timeMap.put(globalTime, receive);
      }
    }
    // if (receive != null) {
    // System.out.println(String.format("Time: %8d - [%d -> %d] : SNR: %4d RSSI: %4d", receive.time,
    // receive.transmission.sender.getID(), id, (int) receive.snr, (int) receive.rssi));
    // }
    // Find a signal to synchronise with
    if (synced == null) {
      findSync();
    }
    // Attempt to receive synchronised signal
    if (synced != null) {
      recv();
    }
  }

  private void recv() {
    LoRaCfg senderCfg = ((LoRaRadio) synced.sender).getLoRaCfg();
    long startTime = synced.startTime + senderCfg.calculatePreambleTime();
    double correct = 0;
    double incorrect = 0;
    Long clearTo = null;
    for (Entry<Long, ReceiveData> entry : timeMap.entrySet()) {
      if (entry.getKey() < startTime) {
        continue;
      }
      if (entry.getValue() != null) {
        if (entry.getValue().transmission != synced || !getReceiveSuccess(entry.getValue().snr)) {
          incorrect++;
        } else {
          correct++;
        }
      }
      if (entry.getKey() > synced.endTime) {
        double maxErrors = 1 - (4 / (double) senderCfg.getCR());
        boolean success = (incorrect / (correct + incorrect)) < maxErrors;
        if (success) {
          System.out.println("Got a message!");
        } else {
          System.out.println("Failed getting message");
        }
        synced = null;
        clearTo = entry.getKey();
        break;
      }
    }
    if (clearTo != null) {
      final long clearCondition = clearTo;
      timeMap.keySet().removeIf(x -> x <= clearCondition);
    }
  }


  long lastTime = Long.MIN_VALUE;

  private boolean findSync() {
    Transmission detected = null;
    boolean gotSync = false;
    long lastTime = this.lastTime;
    Long clearTo = null;
    for (Entry<Long, ReceiveData> start : timeMap.entrySet()) {
      if (start.getValue() == null) {
        continue;
      }
      // Find the time when the needed preamble is available
      LoRaCfg senderCfg = ((LoRaRadio) start.getValue().transmission.sender).getLoRaCfg();
      double preambleFinish =
          start.getValue().transmission.startTime + senderCfg.calculatePreambleTime();
      double preambleReqStart = preambleFinish - LoRaCfg.requiredPreambleTime(senderCfg);
      if (preambleReqStart >= lastTime && preambleReqStart <= start.getKey()) {
        detected = start.getValue().transmission;
      } else {
        detected = null;
      }
      // Look ahead to see if full preamble detected
      if (detected != null) {
        PreambleLookaheadResult lr =
            preambleLookahead(start.getValue().transmission, start.getKey());
        if (lr == PreambleLookaheadResult.FOUND_END) {
          gotSync = true;
          break;
        }
        if (lr == PreambleLookaheadResult.COLLISION) {
          // If we know a collision is always going to happen we can remove this item
          clearTo = start.getKey();
        }
      }
      lastTime = start.getKey();
    }
    // Clear anything we know isn't a successful preamble
    if (clearTo != null) {
      final long clearCondition = clearTo;
      this.lastTime = clearTo;
      timeMap.keySet().removeIf(x -> x <= clearCondition);
    }

    // Update the sync status
    if (gotSync) {
      synced = detected;
    } else {
      synced = null;
    }
    return gotSync;
  }


  private PreambleLookaheadResult preambleLookahead(Transmission target, long startTime) {
    LoRaCfg senderCfg = ((LoRaRadio) target.sender).getLoRaCfg();
    double preambleFinish = target.startTime + senderCfg.calculatePreambleTime();
    long missedTime = 0;
    long lastTime = startTime;
    for (Entry<Long, ReceiveData> next : timeMap.entrySet()) {
      if (next.getKey() <= startTime) {
        lastTime = next.getKey();
        continue;
      }
      // Another signal detected in important preamble section, this is a collision
      if (next.getValue() == null || next.getValue().transmission != target) {
        missedTime += (next.getKey() - lastTime);
        if (missedTime > (LoRaCfg.getSymbolTime(senderCfg))) {
          return PreambleLookaheadResult.COLLISION;
        }
      }
      if (target != null) {
        // Found the end and we haven't had a collision, got the full preamble
        if (preambleFinish >= lastTime && preambleFinish <= next.getKey()) {
          ;
          return PreambleLookaheadResult.FOUND_END;
        }
      }
      lastTime = next.getKey();
    }
    return PreambleLookaheadResult.NO_END;
  }

  public boolean getReceiveSuccess(double snr) {
    Random r = new Random();
    return r.nextDouble() <= getReceiveProbability(snr);
  }

  public double getReceiveProbability(double snr) {
    // https://en.wikipedia.org/wiki/Generalised_logistic_function
    double k = 1;
    double a = 0;
    double b = 1;
    double v = 0.3;
    double c = 1;
    double m = getRequiredSNR() - .1;
    double q = 0.1;
    double prob = a + (k - a) / Math.pow(c + q * Math.exp(-b * (snr - m)), 1 / v);
    return prob;
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
    return tx;
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
  public void decode() {


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

  enum PreambleLookaheadResult {
    NO_END, FOUND_END, COLLISION,
  }

  public void checkState() {
    if (tx != null && tx.endTime < environment.getTime()) {
      tx = null;
    }
  }
}
