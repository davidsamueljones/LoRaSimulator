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
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.MetadataStatus;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;
import ecs.soton.dsj1n15.smesh.radio.Transmission;

/**
 * A theoretical LoRa Radio based on the SX1272/76 datasheets. Has support for channel activity
 * detection, interference modelling, receive success probability, preamble synchronisation.
 * 
 * @author David Jones (dsj1n15)
 */
public class LoRaRadio extends Radio {
  public static final double MAX_SENSITIVITY = -137;
  public static final double DEFAULT_ANTENNA_GAIN = 0;
  public static final double DEFAULT_CABLE_LOSS = 0;

  /** Radio configuration */
  protected LoRaCfg cfg;

  /** Antenna gain */
  protected double antennaGain;

  /** Cable loss */
  protected double cableLoss;

  /** Current transmission */
  protected Transmission tx = null;

  /** Last time capture stream was checked */
  protected long lastTime = Long.MIN_VALUE;

  /** The transmission currently being received */
  protected Transmission synced = null;

  /** Whether channel activity detection (CAD) is enabled */
  protected boolean cadEnabled = false;

  /** Whether channel activity was found */
  protected boolean cadFound = false;

  /** If enabled, when CAD capture should finish */
  protected long cadCaptureFinishTime = 0;

  /** If enabled, when CAD capture and processing is finished */
  protected long cadCompleteTime = 0;

  /**
   * Instantiate a LoRa Radio using the default configuration.
   * 
   * @param id ID of the radio
   */
  public LoRaRadio(int id) {
    this(id, LoRaCfg.getDefault());
  }


  /**
   * Instantiate a LoRa Radio.
   * 
   * @param id ID of the radio
   * @param cfg LoRa configuration to initialise with
   */
  public LoRaRadio(int id, LoRaCfg cfg) {
    super(id);
    this.cfg = cfg;
    this.antennaGain = DEFAULT_ANTENNA_GAIN;
    this.cableLoss = DEFAULT_CABLE_LOSS;
  }

  /**
   * @param cfg New LoRa configuration to use
   */
  public void setLoRaCfg(LoRaCfg cfg) {
    this.cfg = cfg;
  }

  /**
   * @return Current LoRa configuration
   */
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
   * The maximum SNR of the LoRa demodulator (SX1276/etc...) is limited to 10.
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

  /**
   * Listen for the current time slice to all transmissions in the environment. Record the strongest
   * transmission that can be heard, (i.e. on the correct SF, etc...). Adds a slight bit of bias to
   * signals the radio is synchronised with, but does not consider them as more 'powerful'. Always
   * record something if it exists, even if it is weak, leave it to receive behaviour to determine
   * whether the signal was strong enough.
   */
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
      // Determine SNR of signal
      double snr = environment.getReceiveSNR(transmission.sender, this);
      // Mix a bit of random noise in
      // snr = snr + Utilities.RANDOM.nextInt(3) - 1;
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
    // Record any partial receives no matter the strength
    if (receive != null) {
      timeMap.put(globalTime, receive);
    }
  }

  /**
   * Find a preamble in the input stream to synchronise with. Start by finding the first 'important'
   * preamble symbol and then search ahead to see if the preamble sync is successful. If successful
   * will set the class sync variable to the transmission, otherwise it will be set to null.
   * 
   * @return Whether a preamble was found and synchronised with
   */
  private boolean findSync() {
    Transmission detected = null;
    boolean gotSync = false;
    long lastPreambleCheck = this.lastTime;
    Iterator<Entry<Long, PartialReceive>> itrTimeMap = timeMap.entrySet().iterator();
    while (itrTimeMap.hasNext()) {
      detected = null;
      // Determine if the preamble starts in this time slot
      Entry<Long, PartialReceive> first = itrTimeMap.next();
      if (first.getValue() != null) {
        LoRaCfg senderCfg = ((LoRaRadio) first.getValue().transmission.sender).getLoRaCfg();
        double preambleFinish =
            first.getValue().transmission.startTime + senderCfg.calculatePreambleTime();
        double preambleReqStart = preambleFinish - LoRaCfg.requiredPreambleTime(senderCfg);
        if (preambleReqStart >= lastPreambleCheck && preambleReqStart <= first.getKey()) {
          detected = first.getValue().transmission;
        }
      }
      lastPreambleCheck = first.getKey();
      if (detected == null) {
        // No preamble keep searching
        continue;
      }
      // Look ahead to see if full preamble detected
      Pair<PreambleResult, Long> pr;
      pr = syncLookahead(detected, first.getKey());
      if (pr.getLeft() != PreambleResult.NOT_COMPLETE) {
        this.lastTime = pr.getRight();
        lastPreambleCheck = pr.getRight();
      }
      // Got the end of the preamble successfully, can synchronise
      if (pr.getLeft() == PreambleResult.SUCCESS) {
        gotSync = true;
        break;
      }
      // On a failure can clear up some of the old receive data
      if (pr.getLeft() == PreambleResult.FAIL) {
        // Can forget about information up to this point as it is of no use
        // Reset the iterator so we can remove from the beginning
        itrTimeMap = timeMap.entrySet().iterator();
        boolean cleared = false;
        while (!cleared && itrTimeMap.hasNext()) {
          Long nextTime = itrTimeMap.next().getKey();
          itrTimeMap.remove();
          if (nextTime == pr.getRight()) {
            cleared = true;
          }
        }
      }
    }
    // Update the sync status
    synced = gotSync ? detected : null;
    return gotSync;
  }

  /**
   * From the start time, look ahead to find the rest of the preamble in the partial receive buffer.
   * If enough is found use typical radio receive behaviour to determine whether it was strong
   * enough to be found successfully. If there is conflicting receive data trigger a collision,
   * otherwise fail as not found.
   * 
   * @param target The transmission whose preamble was found
   * @param startTime Time when start of preamble was found
   * @return The search result, with a time of collision or end as appropriate
   */
  private Pair<PreambleResult, Long> syncLookahead(Transmission target, long startTime) {
    LoRaCfg senderCfg = ((LoRaRadio) target.sender).getLoRaCfg();
    double preambleFinish = target.startTime + senderCfg.calculatePreambleTime();
    int noSignalCount = 0;
    int signalCount = 0;
    double signalSNR = 0;
    int wrongSignalCount = 0;
    double wrongSignalSNR = 0;

    // Search ahead in the stream
    for (Entry<Long, PartialReceive> next : timeMap.entrySet()) {
      if (next.getKey() < startTime) {
        continue;
      }
      if (next.getValue() == null) {
        noSignalCount++;
      } else if (next.getValue().transmission == target) {
        signalCount++;
        signalSNR += Utilities.dbm2mw(next.getValue().snr);
      } else {
        wrongSignalCount++;
        wrongSignalSNR += Utilities.dbm2mw(next.getValue().snr);
      }

      // Passed end of preamble, check if preamble was found
      if (next.getKey() >= preambleFinish) {
        int totalParts = (signalCount + wrongSignalCount + noSignalCount);
        double signalPercentage = signalCount / (double) totalParts;
        if (signalPercentage > 0.8) {
          double signalAvgSNR = Utilities.mw2dbm(signalSNR / signalCount);
          // System.out.println(signalAvgSNR);
          if (getReceiveSuccess(signalAvgSNR)) {
            return new ImmutablePair<>(PreambleResult.SUCCESS, next.getKey());
          }
        } else if (wrongSignalCount >= noSignalCount) {
          // Check for preamble collision
          double wrongSignalAvgSNR = Utilities.mw2dbm(wrongSignalSNR / wrongSignalCount);
          if (getReceiveSuccess(wrongSignalAvgSNR)) {
            this.lastReceive = new ReceiveResult(Status.FAIL_COLLISION,
                MetadataStatus.FAIL_PREAMBLE_COLLISION, target, next.getKey());
            alertReceiveListeners();
          }
        }
        // Failed to get preamble but no strong opposing signal so not a collision, just a missed
        // preamble, record it as a failed receive for simulation analysis
        this.lastReceive = new ReceiveResult(Status.UNAWARE_FAIL, MetadataStatus.FAIL_NO_PREAMBLE,
            target, next.getKey());
        alertReceiveListeners();
        return new ImmutablePair<>(PreambleResult.FAIL, next.getKey());
      }
    }
    // Reached end of search but not found end of preamble
    return new ImmutablePair<>(PreambleResult.NOT_COMPLETE, null);
  }

  /**
   * Attempt to receive the payload of a synchronised signal. A successful receive occurs if the
   * number of incorrect partial receives are less than the coding rate can accept and the
   * getReceiveSuccess check passes. Coding rate performance is assumed to be perfect where CR of
   * 4/5 can correct 20% of the signal etc... Will clear the input stream if the end of the
   * synchronised transmission is detected.
   */
  private void decodePacket() {
    if (synced == null) {
      return;
    }
    LoRaCfg senderCfg = ((LoRaRadio) synced.sender).getLoRaCfg();
    long startTime = synced.startTime + senderCfg.calculatePreambleTime();
    int signalCount = 0;
    double signalSNR = 0;
    double signalRSSI = 0;
    int noSignalCount = 0;
    int wrongSignalCount = 0;
    double wrongSignalSNR = 0;

    Long clearTo = null;
    for (Entry<Long, PartialReceive> entry : timeMap.entrySet()) {
      long time = entry.getKey();
      if (time < startTime) {
        continue;
      }
      // Accumulate time slices
      if (entry.getValue() == null) {
        noSignalCount++;
      } else if (entry.getValue().transmission == synced) {
        signalCount++;
        signalSNR += Utilities.dbm2mw(entry.getValue().snr);
        signalRSSI += Utilities.dbm2mw(entry.getValue().rssi);
      } else {
        wrongSignalCount++;
        wrongSignalSNR += Utilities.dbm2mw(entry.getValue().snr);
      }

      if (time > synced.endTime) {
        ReceiveResult receive = null;
        int totalParts = (signalCount + wrongSignalCount + noSignalCount);
        double signalPercentage = signalCount / (double) totalParts;
        double signalAvgSNR = Utilities.mw2dbm(signalSNR / signalCount);
        double signalAvgRSSI = Utilities.mw2dbm(signalRSSI / signalCount);
        // Determine if the number of receive errors can be fixed by coding rate
        if (signalPercentage > (1 - (4 / (double) senderCfg.getCR()))) {
          if (getReceiveSuccess(signalAvgSNR)) {
            receive = new ReceiveResult(Status.SUCCESS, MetadataStatus.SUCCESS, synced, time,
                signalAvgSNR, signalAvgRSSI);
          }
        } else if (wrongSignalCount >= noSignalCount) {
          // Check for collision
          double wrongSignalAvgSNR = Utilities.mw2dbm(wrongSignalSNR / wrongSignalCount);
          if (getReceiveSuccess(wrongSignalAvgSNR)) {
            receive = new ReceiveResult(Status.FAIL_CRC, MetadataStatus.FAIL_PAYLOAD_COLLISION,
                synced, startTime, signalAvgSNR, signalAvgRSSI);
          }
        }
        // Failed to get signal but no strong opposing signal so not a collision
        if (receive == null) {
          receive = new ReceiveResult(Status.FAIL_CRC, MetadataStatus.FAIL_PAYLOAD_WEAK, synced,
              startTime, signalAvgSNR, signalAvgRSSI);
        }
        synced = null;
        this.lastReceive = receive;
        alertReceiveListeners();
        clearTo = entry.getKey();
        break;
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
   * For the theoretical LoRa Radio this sticks to theoretical values.
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

  /**
   * Start a channel activity detection (CAD) process. Will disable receive and transmit behaviour
   * until complete. Cannot use the current input stream.
   */
  public void startCAD() {
    // Can't use previous receive input stream
    timeMap.keySet().removeIf(x -> x <= environment.getTime());
    synced = null;
    // Schedule CAD
    cadEnabled = true;
    cadCaptureFinishTime = environment.getTime() + (int) LoRaCfg.getSymbolTime(cfg);
    cadCompleteTime = environment.getTime() + (int) (LoRaCfg.getSymbolTime(cfg) * 1.85);
  }

  /**
   * Clear up after a channel activity detection (CAD) finishes.
   */
  protected void stopCAD() {
    // Can't use CAD stream for receive
    timeMap.keySet().removeIf(x -> x <= environment.getTime());
    this.cadEnabled = false;
  }

  /**
   * @return Whether the last CAD was true
   */
  public boolean getCADStatus() {
    return cadFound;
  }

  /**
   * Set whether the last CAD was successful to false
   */
  public void clearCADStatus() {
    this.cadFound = false;
  }

  /**
   * @return Whether the radio is currently in CAD mode.
   */
  public boolean isCADMode() {
    return this.cadEnabled;
  }

  /**
   * Use standard receive behaviour to look for any amount of preamble that is strong enough in the
   * current receive stream.
   */
  public void doCADSearch() {
    boolean gotActivity = false;
    int got = 0;
    int not = 0;
    double totalSNR = 0;

    // Find a symbol in the input stream
    for (Entry<Long, PartialReceive> entry : timeMap.entrySet()) {
      if (entry.getKey() > cadCaptureFinishTime) {
        double avgSNR = Utilities.mw2dbm(totalSNR / got);
        gotActivity = got >= not && getReceiveSuccess(avgSNR);
        break;
      }
      PartialReceive data = entry.getValue();
      if (data != null) {
        // Check if the sample is preamble
        LoRaCfg senderCfg = ((LoRaRadio) data.transmission.sender).getLoRaCfg();
        Transmission transmission = data.transmission;
        long preambleEnd = transmission.startTime + senderCfg.calculatePreambleTime();
        if (entry.getKey() >= transmission.startTime && entry.getKey() <= preambleEnd) {
          got++;
          totalSNR += Utilities.dbm2mw(entry.getValue().snr);
        } else {
          not++;
        }
      } else {
        not++;
      }
    }
    this.cadFound = gotActivity;
  }

  @Override
  public Transmission getCurrentTransmission() {
    return tx;
  }

  /**
   * @return The transmission that the receiver is synchronised with due to preamble detection.
   */
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

  /**
   * Possible preamble search results.
   * 
   * @author David Jones (dsj1n15)
   */
  enum PreambleResult {
    NOT_COMPLETE, SUCCESS, FAIL
  }

}
