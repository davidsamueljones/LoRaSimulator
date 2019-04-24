package ecs.soton.dsj1n15.smesh.model.dutycycle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Duty cycle manager for calculating the allowed airtime over the full duty cycle interval. Allows
 * for sequential transmissions and more advanced behaviour over SingleTransmissionDutyCycleManager.
 * 
 * @author David Jones (dsj1n15)
 */
public class FullPeriodDutyCycleManager extends DutyCycleManager {
  private int dutyCyclePeriod;

  /** List of transmission occurrence times and their duration */
  private List<Pair<Long, Integer>> transmissions = new ArrayList<>();

  /**
   * Create a new transmission duty cycle manager that keeps track of all previous transmissions to
   * determine next transmission times.
   * 
   * @param dutyCycle Duty cycle as a decimal e.g. 0.01 = 1%
   * @param dutyCyclePeriod The length the duty cycle period is calculated over
   */
  public FullPeriodDutyCycleManager(double dutyCycle, int dutyCyclePeriod) {
    super(dutyCycle);
    this.dutyCyclePeriod = dutyCyclePeriod;
  }

  @Override
  public void transmit(long time, int airtime) {
    if (!canTransmit(time, airtime)) {
      throw new IllegalStateException("Cannot transmit within boundaries of duty cycle manager");
    }
    transmissions.add(new ImmutablePair<>(time, airtime));
  }

  @Override
  public boolean canTransmit(long time, int airtime) {
    int allowance = getPeriodTotalAirtime();
    int before = allowance - getPeriodAirtime(time + 1);
    int after = allowance - getPeriodAirtime(time + airtime);
    boolean canTransmit = true;
    canTransmit &= before >= 1;
    canTransmit &= (after - airtime) >= 0;
    return canTransmit;
  }

  @Override
  public long whenCanTransmit(long time, int airtime) {
    if (airtime > getPeriodTotalAirtime()) {
      return Long.MAX_VALUE;
    }
    while (!canTransmit(time, airtime)) {
      time++;
      if (time == 475) {
        System.out.println("");
      }
    }
    return time;
  }

  @Override
  public long getAvailableTransmitTime(long time) {
    int airtime = 0;
    while (canTransmit(time, airtime + 1)) {
      airtime++;
    }
    return airtime;
  }

  /**
   * Determine the amount of airtime that has been used in the past period.
   * 
   * @param time The current time in ms to index from
   * @return The amount of airtime in the period
   */
  public int getPeriodAirtime(long time) {
    int totalAirtime = 0;
    long periodStart = time - dutyCyclePeriod;
    for (Pair<Long, Integer> transmission : transmissions) {
      // If it is completely outside of the duty cycle period ignore it
      if ((transmission.getLeft() + transmission.getRight()) < periodStart) {
        continue;
      }
      totalAirtime += transmission.getRight();
      // If it is partially outside the duty cycle period do not sum the expired part
      if (transmission.getLeft() < periodStart) {
        long expired = periodStart - transmission.getLeft();
        totalAirtime -= expired;
      }
    }
    return totalAirtime;
  }

  /**
   * @return The amount of allowed airtime for the configured period
   */
  public int getPeriodTotalAirtime() {
    return (int) (dutyCyclePeriod * dutyCycle);
  }

  /**
   * Clear any transmissions from the log that will no longer effect duty cycle calculations (those
   * outside the interval).
   * 
   * @param time The current time in ms
   */
  public void clearExpired(long time) {
    Iterator<Pair<Long, Integer>> itrTransmissions = transmissions.iterator();
    long periodStart = time - dutyCyclePeriod;
    while (itrTransmissions.hasNext()) {
      Pair<Long, Integer> next = itrTransmissions.next();
      if ((next.getLeft() + next.getRight()) < periodStart) {
        itrTransmissions.remove();
      }
      if (next.getLeft() >= (time - dutyCyclePeriod)) {
        break;
      }
    }
  }

}
