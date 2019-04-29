package ecs.soton.dsj1n15.smesh.model.lora.protocol.adaptive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.lib.Debugger;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.model.dutycycle.FullPeriodDutyCycleManager;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.ProtocolTickListener;
import ecs.soton.dsj1n15.smesh.model.lora.protocol.TestDataPacket;
import ecs.soton.dsj1n15.smesh.radio.Packet;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;
import math.geom2d.Point2D;

/**
 * Main control class for Adaptive Broadcast Protocol.
 * 
 * @author David Jones (dsj1n15)
 */
public class AdaptiveTickListener extends ProtocolTickListener {
  private static final int LAST_SEEN_TIMEOUT_MS = 600000; // 10 mins

  /** The high data rate channels */
  private static final double[] CHANNELS = {865.1, 865.3, 865.5, 865.7, 865.9, 866.1, 866.3, 866.5,
      866.7, 866.9, 867.1, 867.3, 867.5, 867.7, 867.9};

  /** The required SNRs for a respective datarate */
  private static final List<Double> DATARATE_SNRS = new ArrayList<>();
  {
    for (int sf = 7; sf <= 12; sf++) {
      DATARATE_SNRS.add(LoRaRadio.getRequiredSNR(sf) + 2.5);
    }
  }

  /** The order in which to consider the datarates */
  private static final int[] DATARATE_USE_ORDER = {0, 1, 3, 4, 5};

  /**
   * The number of announcement packets to send before broadcast TODO: Initialise on instantiation
   * as opposed to using public variable.
   */
  public static int ANNOUNCEMENT_PACKET_COUNT = 2;

  /** Whether to enable heartbeat packet sending */
  public static boolean HEARTBEAT_ENABLED = true;

  /** Whether to find all the best targets using sim metadata */
  public static boolean TARGET_CHEAT = false;

  /** A duty cycle manager for managing the low data rate band */
  private final FullPeriodDutyCycleManager dcmLowRateBand;
  /** A duty cycle manager for managing the high data rate band */
  private final FullPeriodDutyCycleManager dcmHighRateBand;

  /** Radios and the time they were last seen */
  private Map<Radio, Long> seenRadios = new HashMap<>();
  /** Radios and the SNR at the time they were last seen */
  private Map<Radio, Double> seenRadiosSNRs = new HashMap<>();
  /** Radios and the location at the time they were last seen */
  private Map<Radio, Point2D> seenRadiosLocs = new HashMap<>();

  private boolean usingLowDataRate;
  private int lowRateDataRate;
  private LoRaCfg lowRateBandCfg;
  private final int intervalHeartbeat;
  private long nextHeartbeat;
  private boolean lastHeartbeatComplete = true;

  private long nextBroadcast;
  private long nextBroadcastDelay;
  private DataAnnouncePacket broadcastAnnouncement = null;
  private int announcementsSent = 0;
  private boolean announcementScheduled;
  private boolean announcementSent = true;
  private boolean sendingAnnouncements = false;

  private List<Packet> packets = new ArrayList<>();
  private boolean transmittingPackets = false;
  private long timeoutPacketReceives = 0;


  /**
   * Instantiate a new adaptive tick listener. Use 1% duty cycles for both bands. Use the low rate
   * data rate provided for the management band communications, this is the longest range
   * communications that the protocol will ever use.
   * 
   * @param radio The radio to control
   * @param lowRateDataRate The low rate data rate to use
   */
  public AdaptiveTickListener(LoRaRadio radio, int lowRateDataRate) {
    super(radio);
    // Assign duty cycle managers for the bands
    dcmLowRateBand = new FullPeriodDutyCycleManager(0.01, 1000 * 60 * 60);
    dcmHighRateBand = new FullPeriodDutyCycleManager(0.01, 1000 * 60 * 60);
    // Initialise to low rate band configuration
    this.lowRateDataRate = lowRateDataRate;
    setLowDataRate();
    // Set the heartbeat interval so that half the duty cycle time is taken up with heartbeats
    intervalHeartbeat =
        (int) (lowRateBandCfg.calculatePacketAirtime(HeartbeatPacket.getExpectedLength())
            / dcmLowRateBand.getDutyCycle() * 2);
    // Schedule an initial heartbeat
    scheduleHeartbeat();
    // Schedule the first dump
    nextBroadcast = Utilities.RANDOM.nextInt(600) * 1000;
  }

  @Override
  public void tick() {
    // Simulation metadata gathering
    trackTransmissions();

    // Run generic radio tasks
    checkForSendFinish();
    checkForSync();
    checkForReceive();
    checkCAD();

    // Handle protocol tasks
    if (usingLowDataRate) {
      handleLowDataRateBand();
    } else {
      handleHighDataRateBand();
    }
  }

  /**
   * Switch to the default low rate configuration.
   */
  public void setLowDataRate() {
    usingLowDataRate = true;
    lowRateBandCfg = LoRaCfg.getDatarate(lowRateDataRate);
    lowRateBandCfg.setFreq(868.3);
    lowRateBandCfg.setPreambleSymbols(16);
    radio.setLoRaCfg(lowRateBandCfg);
  }

  /**
   * Switch to high data rate mode using the data announcement packet configuration.
   * 
   * @param dap Data announcement packet to use for configuration
   */
  public void setHighDataRate(DataAnnouncePacket dap) {
    LoRaCfg cfg = LoRaCfg.getDatarate(dap.dr);
    cfg.setFreq(CHANNELS[dap.channel]);
    radio.setLoRaCfg(cfg);
    usingLowDataRate = false;
  }

  /**
   * Handle protocol behaviour whilst in the low-rate band.
   */
  public void handleLowDataRateBand() {
    long curTime = environment.getTime();
    // Send periodic heartbeats
    if (!sendingAnnouncements) {
      if (nextHeartbeat < curTime && HEARTBEAT_ENABLED) {
        sendHeartbeat();
        return;
      }
    }
    // Determine whether a broadcast announcement packet should be sent
    if (!announcementScheduled && lastHeartbeatComplete && nextBroadcast <= curTime
        && announcementsSent < ANNOUNCEMENT_PACKET_COUNT && announcementSent) {
      if (scheduleAnnouncement()) {
        sendingAnnouncements = true;
        announcementScheduled = true;
        announcementSent = false;
      }
    }
    // Handle broadcast announcement
    if (announcementScheduled && broadcastAnnouncement != null) {
      if (attemptSend(broadcastAnnouncement, dcmLowRateBand) == SendStatus.SUCCESS) {
        announcementScheduled = false;
      }
    }
  }


  /**
   * Handle protocol behaviour whilst in the high-rate band.
   */
  public void handleHighDataRateBand() {
    long curTime = environment.getTime();

    // Handle high-rate transmit behaviour
    if (transmittingPackets) {
      if (packets.size() > 0) {
        Packet packet = packets.get(0);
        SendStatus send = attemptSend(packet, dcmHighRateBand);
        if (send == SendStatus.SUCCESS) {
          packets.remove(0);
        }
      } else {
        // Finished transmitting, return to LDR
        if (radio.getCurrentTransmission() == null) {
          setLowDataRate();
          broadcastAnnouncement = null;
          transmittingPackets = false;
          announcementsSent = 0;
        }
      }
    } else {
      // Must be receiving, check if receive timeout has been reached
      if (timeoutPacketReceives < curTime) {
        Debugger.println(String.format("[%8d] - Radio %-2d - Timed Out HDR", environment.getTime(),
            radio.getID()));
        setLowDataRate();
      }
    }
  }

  /**
   * Schedule a broadcast announcement to occur whenever the channel is free, if it is a second
   * announcement in a row use the previous settings with an updated delay field.
   * 
   * @return Whether an announcement was scheduled
   */
  public boolean scheduleAnnouncement() {
    // Check if there is anyone to send to
    if (TARGET_CHEAT) {
      testFindLocalNeighbours();
    }
    Set<Radio> targets = findPotentialTargets();
    if (targets.size() > 0) {
      // Determine how much data to send
      long availableTime = dcmHighRateBand.getPeriodTotalAirtime() / 6;
      long sendingTime = 0;
      while (availableTime > sendingTime) {
        TestDataPacket packet = new TestDataPacket(128);
        int airtime = radio.getLoRaCfg().calculatePacketAirtime(packet.length);
        if (sendingTime + airtime <= availableTime) {
          sendingTime += airtime;
          packets.add(packet);
        } else {
          break;
        }
      }
      // Delay as if the worst datarate were used, aim is not for high rate but to be done asap
      // so no point just increasing channel usage
      nextBroadcastDelay = (int) (Math.ceil(sendingTime / dcmHighRateBand.getDutyCycle()));
      // Create the announcement packet, using the original as a template if this is not the first
      // announcement
      if (broadcastAnnouncement != null) {
        broadcastAnnouncement = makeAnnouncementPacket(broadcastAnnouncement);
      } else {
        broadcastAnnouncement = makeAnnouncementPacket(targets, packets);
      }
      return true;
    } else {
      // Check again in 30s if any targets have been found
      nextBroadcast = environment.getTime() + 30000;
    }
    return false;
  }

  /**
   * Schedule a heartbeat packet some time in the future.
   * 
   * @return When to schedule the heartbeat for
   */
  public double scheduleHeartbeat() {
    nextHeartbeat = environment.getTime();
    nextHeartbeat += (long) (intervalHeartbeat * (1 + Utilities.RANDOM.nextDouble()));
    return nextHeartbeat;
  }

  /**
   * Send a heartbeat packet (with no extra payload) using CAD.
   * 
   * @return The send status from the send attempt
   */
  protected SendStatus sendHeartbeat() {
    HeartbeatPacket packet = new HeartbeatPacket(radio, radio.getXY());
    // Attempt to send heartbeat with CAD
    lastHeartbeatComplete = false;
    SendStatus sendStatus = attemptSendWithCAD(packet, dcmLowRateBand);

    // Handle whether the message was sent
    switch (sendStatus) {
      case RADIO_BUSY:
        // Wait till radio has finished doing what it's doing
        // Could be CAD or a transmission
        break;
      case CAD_NEEDED:
        // CAD will have started, wait till complete
        break;
      case CHANNEL_BUSY:
        // Handle backoff behaviour by just delaying a short period
        nextHeartbeat += r.nextInt(10000);
        break;
      case DUTY_CYCLE_LIMIT:
        // Delay heartbeat till it is allowed
        int airtime = radio.getLoRaCfg().calculatePacketAirtime(packet.length);
        nextHeartbeat = dcmLowRateBand.whenCanTransmit(environment.getTime(), airtime);
        break;
      case SUCCESS:
        // Message sent, schedule the next heartbeat
        nextHeartbeat = (long) (environment.getTime()
            + (intervalHeartbeat * (1 + Utilities.RANDOM.nextDouble())));
        lastHeartbeatComplete = true;
        break;
      default:
        break;
    }
    return sendStatus;
  }

  /**
   * Use received heartbeats to determine which other radios are nearby.
   * 
   * @return Set of radios that should be nearby and ready to receive
   */
  protected Set<Radio> findPotentialTargets() {
    Set<Radio> potentialTargets = new HashSet<>();
    // Find the nodes that may want the packet
    for (Radio target : seenRadios.keySet()) {
      // Predict which radios would want this transmission
      boolean wouldWant = isTransmissionWanted(seenRadiosLocs.get(target), radio.getXY());
      if (!wouldWant) {
        continue;
      }
      // Discard radios that we haven't seen for a long time
      if (seenRadios.get(target) < environment.getTime() - LAST_SEEN_TIMEOUT_MS) {
        continue;
      }
      potentialTargets.add(target);
    }
    return potentialTargets;
  }


  /**
   * Find the local neighbours without heartbeats.<br>
   * This is a test function that utilises simulation metadata.
   */
  private void testFindLocalNeighbours() {
    for (Radio receiver : environment.getNodes()) {
      if (receiver == radio) {
        continue;
      }
      double snr = environment.getReceiveSNR(radio, receiver);
      seenRadios.put(receiver, environment.getTime());
      seenRadiosSNRs.put(receiver, snr);
      seenRadiosLocs.put(receiver, radio.getXY());
    }
  }

  /**
   * Create a broadcast announcement packet using already collected data from heartbeats to make
   * decision on spreading factor and channel.
   * 
   * @param targets The radios to try and fit the broadcast parameters to
   * @param packets The packets that are going to be sent
   * @return The created packet
   */
  protected DataAnnouncePacket makeAnnouncementPacket(Set<Radio> targets, List<Packet> packets) {
    // Find the worst case SNR required to serve all nodes
    double minSNR = Integer.MAX_VALUE;
    for (Radio target : targets) {
      double snr = seenRadiosSNRs.get(target);
      if (snr < minSNR) {
        minSNR = snr;
      }
    }
    // Find the best datarate that can be used, do not use 6 as channels are only 125KHz
    int drBest = lowRateDataRate;
    for (int dr : DATARATE_USE_ORDER) {
      if (dr < drBest) {
        continue;
      }
      if (DATARATE_SNRS.get(dr) < minSNR) {
        drBest = dr;
      } else {
        break;
      }
    }
    // Select a random channel in the high rate band
    int channel = Utilities.RANDOM.nextInt(CHANNELS.length);
    // Just use a random block ID, redundant for testing as retransmissions are not a concern
    int blockID = Utilities.RANDOM.nextInt(255);
    // Determine packet information for receivers
    double totalLength = 0;
    for (Packet packet : packets) {
      totalLength += packet.length;
    }
    int avgPacketLength = (int) Math.ceil(totalLength / packets.size());
    // Set the start delay until all announcements have been sent
    int packetAirtime =
        radio.getLoRaCfg().calculatePacketAirtime(DataAnnouncePacket.getExpectedLength());
    int delay = (ANNOUNCEMENT_PACKET_COUNT - (announcementsSent + 1)) * packetAirtime;
    // Create the packet
    DataAnnouncePacket dap = new DataAnnouncePacket(radio, null, avgPacketLength, packets.size(),
        blockID, radio.getXY(), drBest, channel, delay);
    return dap;
  }

  /**
   * Make a replica of an existing announcement packet, calculate a fresh delay reflecting how many
   * announcement packets have yet to be sent.
   * 
   * @param original Packet to clone
   * @return Replica announcement packet
   */
  private DataAnnouncePacket makeAnnouncementPacket(DataAnnouncePacket original) {
    int packetAirtime =
        radio.getLoRaCfg().calculatePacketAirtime(DataAnnouncePacket.getExpectedLength());
    int delay = (ANNOUNCEMENT_PACKET_COUNT - (announcementsSent + 1)) * packetAirtime;
    DataAnnouncePacket dap = new DataAnnouncePacket(original.sender, original.target,
        original.avgPacketLength, original.packetCount, original.blockID, original.loc, original.dr,
        original.channel, delay);
    return dap;
  }

  /**
   * {@inheritDoc} <br>
   * Along with usual receive behaviour, also track the radios that have been seen by the receiver
   * and the SNRs and locations associated with them (extracted from packet). Additionally, handle
   * data announcement behaviour.
   */
  @Override
  protected boolean checkForReceive() {
    boolean newReceive = super.checkForReceive();
    // Track radios that have been seen
    if (newReceive && lastReceive != null && lastReceive.status == Status.SUCCESS) {
      Radio sender = lastReceive.transmission.sender;
      seenRadios.put(sender, environment.getTime());
      seenRadiosSNRs.put(sender, lastReceive.snr);
      seenRadiosLocs.put(sender, sender.getXY());
      // Handle special behaviour for receiving a data announcement
      Packet packet = lastReceive.transmission.packet;
      if (packet instanceof DataAnnouncePacket) {
        handleDataAnnouncePacket((DataAnnouncePacket) packet);
      }
    }
    return newReceive;
  }


  /**
   * {@inheritDoc} <br>
   * Along with usual send finish behaviour, also handle associated behaviour with data announcement
   * packets.
   */
  @Override
  protected boolean checkForSendFinish() {
    boolean send = super.checkForSendFinish();
    if (send && lastTransmit != null) {
      // Check for end of broadcast announcement send
      if (lastTransmit.packet instanceof DataAnnouncePacket) {
        announcementsSent++;
        announcementSent = true;
        // Switch to high rate once all announcements sent
        if (announcementsSent == ANNOUNCEMENT_PACKET_COUNT) {
          setHighDataRate((DataAnnouncePacket) lastTransmit.packet);
          nextBroadcast = environment.getTime() + nextBroadcastDelay;
          sendingAnnouncements = false;
          transmittingPackets = true;
        }
      }
    }
    return send;
  }


  /**
   * Behaviour for when a receiver gets a data announcement packet. If the announcement is of
   * interest to the receiver it will swap data rates and bands to those specified immediately and
   * start to receive. Will timeout and return to the low rate band regardless of packet receives
   * after the expected transmission period for all packets.
   * 
   * @param dap Data announcement packet from broadcaster
   */
  protected void handleDataAnnouncePacket(DataAnnouncePacket dap) {
    boolean wanted = isTransmissionWanted(radio.getXY(), dap.loc);
    if (!wanted) {
      return;
    }
    setHighDataRate(dap);
    LoRaCfg cfg = radio.getLoRaCfg();
    long airtime = cfg.calculatePacketAirtime(dap.avgPacketLength);
    timeoutPacketReceives =
        environment.getTime() + (airtime * dap.packetCount) + dap.startDelay + 5000;
  }

}
