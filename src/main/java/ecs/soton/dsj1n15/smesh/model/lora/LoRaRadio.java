  package ecs.soton.dsj1n15.smesh.model.lora;

import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.PartialReceive;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult;
import ecs.soton.dsj1n15.smesh.radio.Transmission;

public class LoRaRadio extends Radio {
  public static final double MAX_SENSITIVITY = -137;
  public static final double DEFAULT_ANTENNA_GAIN = 3;
  public static final double DEFAULT_CABLE_LOSS = 1.5;

  /** Radio configuration */
  private LoRaCfg cfg;

  /** Antenna gain */
  private double antennaGain;

  /** Cable loss */
  private double cableLoss;

  /** Current transmission */
  private Transmission tx = null;

  /** Last time capture stream was checked */
  private long lastTime = Long.MIN_VALUE;

  /** The transmission currently being received */
  private Transmission synced = null;

  /** Whether channel activity detection (CAD) is enabled */
  boolean cadEnabled = false;

  /** Whether channel activity was found */
  boolean cadFound = false;

  /** If enabled, when CAD capture should finish */
  long cadCaptureFinishTime = 0;

  /** If enabled, when CAD capture and processing is finished */
  long cadCompleteTime = 0;

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

  public void setLoRaCfg(LoRaCfg cfg) {
    this.cfg = cfg;
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
    if (cadEnabled) {
      throw new IllegalStateException("Cannot transmit whilst doing CAD");
    }
    long airtime = cfg.calculatePacketAirtime(packet.length);
    tx = new Transmission(this, packet, environment.getTime(), airtime);
    Debugger
        .println(String.format("Time: %8d - Node: %d - Broadcast", environment.getTime(), this.id));
    return tx;
  }

  @Override
  public void recv() {
    if (tx != null) {
      throw new IllegalStateException("Node cannot receive whilst transmitting");
    }
    // Capture data in receive stream
    listen();

    // Whilst in CAD mode cannot attempt to receive packets
    if (cadEnabled) {
      return;
    }
    // Find a signal to synchronise with
    if (synced == null) {
      findSync();
    }
    // Attempt to receive synchronised signal
    if (synced != null) {
      decodePacket();
    }
  }

  public void listen() {
    Long globalTime = environment.getTime();
    if (timeMap.containsKey(environment.getTime())) {
      throw new IllegalStateException("Already listened at this point");
    }
    timeMap.put(globalTime, null);
    // Find the receive to capture in this slot
    PartialReceive receive = null;
    double rssi = environment.getRSSI(this);
    for (Transmission transmission : environment.getTransmissions()) {
      // Ignore transmission messages that have finished
      if (transmission.endTime < lastTime && transmission.endTime < globalTime) {
        continue;
      }
      // Ignore radios we can't listen to, this won't ignore noise of those that can interfere
      if (!canCommunicate(transmission.sender)) {
        continue;
      }
      // Ignore the transmission if it isn't successfully received
      double snr = environment.getReceiveSNR(transmission.sender, this);
      // Mix a bit of random noise in
      snr = snr + Utilities.RANDOM.nextInt(3) - 1;

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
        receive = new PartialReceive(transmission, globalTime, snr, rssi);
      }
    }
    // Record the partial receive if it was successful
    if (receive != null) {
      boolean success = getReceiveSuccess(receive.snr);
      if (success) {
        timeMap.put(globalTime, receive);
      }
    }
  }



  /**
   * Find a preamble in the input stream to synchronise with. Start by finding the first 'important'
   * preamble symbol and then search ahead to make sure the full important preamble is found.
   * 
   * @return Whether a preamble was found and synchronised with
   */
  private boolean findSync() {
    Transmission detected = null;
    boolean gotSync = false;
    long lastPreambleCheck = this.lastTime;
    Iterator<Entry<Long, PartialReceive>> itrTimeMap = timeMap.entrySet().iterator();
    while (itrTimeMap.hasNext()) {
      Entry<Long, PartialReceive> start = itrTimeMap.next();
      if (start.getValue() != null) {
        // Find the time when the needed preamble is available
        LoRaCfg senderCfg = ((LoRaRadio) start.getValue().transmission.sender).getLoRaCfg();
        double preambleFinish =
            start.getValue().transmission.startTime + senderCfg.calculatePreambleTime();
        double preambleReqStart = preambleFinish - LoRaCfg.requiredPreambleTime(senderCfg);
        if (preambleReqStart >= lastPreambleCheck && preambleReqStart <= start.getKey()) {
          detected = start.getValue().transmission;
        } else {
          detected = null;
        }
      }
      lastPreambleCheck = start.getKey();

      // Look ahead to see if full preamble detected
      if (detected != null) {
        Pair<LookaheadResult, Long> lr;
        lr = preambleLookahead(detected, start.getKey());
        // Got the end of the preamble successfully, can synchronise
        if (lr.getLeft() == LookaheadResult.FOUND_END) {
          gotSync = true;
          break;
        } else if (lr.getLeft() != LookaheadResult.NO_END) {
          // Can forget about information up to this point as it will always be a collision
          // Reset the iterator so we can remove from the beginning
          itrTimeMap = timeMap.entrySet().iterator();
          boolean cleared = false;
          while (!cleared && itrTimeMap.hasNext()) {
            Long nextTime = itrTimeMap.next().getKey();
            itrTimeMap.remove();
            if (nextTime == lr.getRight()) {
              cleared = true;
            }
          }
          this.lastTime = lr.getRight();
          lastPreambleCheck = lr.getRight();
          // A radio knows would know if a collision occurs so alert of a failed receive
          if (lr.getLeft() == LookaheadResult.COLLISION) {
            this.lastReceive = ReceiveResult.getCollisionResult(detected, lr.getRight());
            alertReceiveListeners();
          }     
        }
      }
    }
    // Update the sync status
    if (gotSync) {
      synced = detected;
    } else {
      synced = null;
    }
    return gotSync;
  }

  /**
   * From the start time, look ahead to find the end of the preamble, if more than a symbol of the
   * preamble is lost then reply with the point the collision occurred.
   * 
   * @param target The transmission whose preamble was found
   * @param startTime Time when start of preamble was found
   * @return The search result, with a time of collision or end as appropriate
   */
  private Pair<LookaheadResult, Long> preambleLookahead(Transmission target, long startTime) {
    LoRaCfg senderCfg = ((LoRaRadio) target.sender).getLoRaCfg();
    double preambleFinish = target.startTime + senderCfg.calculatePreambleTime();
    long timeNoSignal = 0;
    long timeWrongSignal = 0;
    long lastTime = startTime;
    for (Entry<Long, PartialReceive> next : timeMap.entrySet()) {
      if (next.getKey() < startTime) {
        lastTime = next.getKey();
        continue;
      }
      // Accumulate time where data was not any signal or the wrong signal
      if (next.getValue() == null) {
        timeNoSignal += (next.getKey() - lastTime);
      } else if (next.getValue().transmission != target) {
        timeWrongSignal += (next.getKey() - lastTime);
      }
      // If more than a symbol time is wrong then give up
      if ((timeWrongSignal + timeNoSignal) > LoRaCfg.getSymbolTime(senderCfg)) {
        if (timeWrongSignal >= timeNoSignal) {
          return new ImmutablePair<>(LookaheadResult.COLLISION, next.getKey());
        } else {
          return new ImmutablePair<>(LookaheadResult.TIMEOUT, next.getKey());
        }
      }

      if (target != null) {
        // Found the end and we haven't had a collision, got the full preamble
        if (preambleFinish >= lastTime && preambleFinish <= next.getKey()) {
          return new ImmutablePair<>(LookaheadResult.FOUND_END, next.getKey());
        }
      }
      lastTime = next.getKey();
    }
    return new ImmutablePair<>(LookaheadResult.NO_END, null);
  }

  /**
   * Attempt to receive the payload of a synchronised signal. A successful receive occurs if the
   * number of incorrect partial receives are less than the coding rate can accept. Coding rate
   * performance is assumed to be perfect where CR of 4/5 can correct 20% of the signal etc...
   */
  private void decodePacket() {
    if (synced == null) {
      return;
    }
    LoRaCfg senderCfg = ((LoRaRadio) synced.sender).getLoRaCfg();
    long startTime = synced.startTime + senderCfg.calculatePreambleTime();
    int correct = 0;
    int incorrect = 0;
    double totalSNR = 0;
    double totalRSSI = 0;
    Long clearTo = null;
    for (Entry<Long, PartialReceive> entry : timeMap.entrySet()) {
      long time = entry.getKey();
      if (time < startTime) {
        continue;
      }
      if (time > synced.endTime) {
        int partials = correct + incorrect;
        // Convert final mW values back to dBm
        totalSNR = Utilities.mw2dbm(totalSNR / partials);
        totalRSSI = Utilities.mw2dbm(totalRSSI / partials);
        // Determine if the number of receive errors can be fixed by coding rate
        double maxErrors = 1 - (4 / (double) senderCfg.getCR());
        boolean success = (incorrect / (double) partials) < maxErrors;
        if (success) {
          lastReceive = ReceiveResult.getSuccessResult(synced, time, totalSNR, totalRSSI);
          Debugger.println(this.id + " Successful Receive! " + correct + " | " + incorrect);
        } else {
          lastReceive = ReceiveResult.getCRCFailResult(synced, time, totalSNR, totalRSSI);
          Debugger.println("Failed Receive! " + correct + " | " + incorrect);
        }
        synced = null;
        alertReceiveListeners();
        clearTo = entry.getKey();
        break;
      }
      // Check whether input is part of the expected stream
      if (entry.getValue() == null || entry.getValue().transmission != synced) {
        incorrect++;
      } else {
        correct++;
        totalSNR += Utilities.dbm2mw(entry.getValue().snr);
        totalRSSI += Utilities.dbm2mw(entry.getValue().rssi);
      }
    }
    // Can clear all of receive from input stream if it has been processed
    if (clearTo != null) {
      final long clearCondition = clearTo;
      timeMap.keySet().removeIf(x -> x <= clearCondition);
      this.lastTime = clearTo;
    }
  }

  @Override
  public void tick() {
    // Clear any finished transmissions
    if (tx != null && tx.endTime <= environment.getTime()) {
      tx = null;
      this.lastTime = environment.getTime();
      timeMap.keySet().removeIf(x -> x <= this.lastTime);
    }
    if (cadEnabled) {
      // Check CAD success once complete
      if (cadCompleteTime <= environment.getTime()) {
        doCADSearch();
        stopCAD();
      }
    } else {
      // Use simulation metadata to clear any samples that definitely aren't needed
      if (environment.getTransmissions().isEmpty() && synced == null) {
        timeMap.clear();
        this.lastTime = environment.getTime();
      }
    }

    // Let any external behaviour trigger
    alertTickListeners();
  }

  public boolean getCADStatus() {
    return cadFound;
  }

  /**
   * Determine whether a receive was successful using the probability of a successful receive for
   * the given snr.
   * 
   * @param snr SNR to use as input
   * @return Whether the receive was successful
   */
  public boolean getReceiveSuccess(double snr) {
    return Utilities.RANDOM.nextDouble() <= getReceiveProbability(snr);
  }

  /**
   * Calculate the probability of a receive being successful using a generalised logistic function.
   * 
   * @param snr SNR to use as input
   * @return The probability that the SNR will result in a successful receive
   */
  public double getReceiveProbability(double snr) {
    // https://en.wikipedia.org/wiki/Generalised_logistic_function
    double k = 0.95;
    double a = 0;
    double b = 2;
    double v = 0.3;
    double c = 1;
    double m = getRequiredSNR() - .1;
    double q = 0.1;
    double prob = a + (k - a) / Math.pow(c + q * Math.exp(-b * (snr - m)), 1 / v);
    return prob;
  }


  public void startCAD() {
    // Can't use previous receive input stream
    timeMap.keySet().removeIf(x -> x <= environment.getTime());
    synced = null;
    // Schedule CAD
    cadEnabled = true;
    cadCaptureFinishTime = environment.getTime() + (int) LoRaCfg.getSymbolTime(cfg);
    cadCompleteTime = environment.getTime() + (int) (LoRaCfg.getSymbolTime(cfg) * 1.85);
  }

  public void stopCAD() {
    // Can't use CAD stream for receive
    timeMap.keySet().removeIf(x -> x <= environment.getTime());
    this.cadEnabled = false;
  }

  public void clearCADStatus() {
    this.cadFound = false;
  }

  public boolean isCADMode() {
    return this.cadEnabled;
  }

  public void doCADSearch() {
    boolean gotActivity = false;
    int got = 0;
    int missed = 0;
    // Find a symbol in the input stream
    for (Entry<Long, PartialReceive> entry : timeMap.entrySet()) {
      if (entry.getKey() > cadCaptureFinishTime) {
        // As long as 50% of the signal is preamble accept it, be generous in case of low granularity
//        if ((missed + got) > 0) {
//          gotActivity = got / (missed + got) > 0.5;
//        }
        gotActivity = got > 0;
        break;
      }
      PartialReceive data = entry.getValue();
      if (data == null) {
        missed++;
      } else {
        // Check if the sample is preamble
        LoRaCfg senderCfg = ((LoRaRadio) data.transmission.sender).getLoRaCfg();
        Transmission transmission = data.transmission;
        long preambleEnd = transmission.startTime + senderCfg.calculatePreambleTime();
        if (entry.getKey() >= transmission.startTime && entry.getKey() <= preambleEnd) {
          got++;
        } else {
          missed++;
        }
      }
    }
    this.cadFound = gotActivity;
  }

  @Override
  public Transmission getCurrentTransmission() {
    return tx;
  }

  public Transmission getSyncedSignal() {
    return synced;
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
        double chirpRate = ((LoRaRadio) rx).cfg.getChirpRate();
        if (chirpRate != cfg.getChirpRate()) {
          return false;
        }
      }
      // For other signals just expect the interference
      return true;
    } else {
      return false;
    }
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

  enum LookaheadResult {
    NO_END, FOUND_END, COLLISION, TIMEOUT
  }

}
