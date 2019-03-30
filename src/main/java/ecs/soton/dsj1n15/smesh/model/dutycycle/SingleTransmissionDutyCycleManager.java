package ecs.soton.dsj1n15.smesh.model.dutycycle;

public class SingleTransmissionDutyCycleManager extends DutyCycleManager {
  private long nextTransmitTime = 0;

  /**
   * Create a new single transmission duty cycle manager.
   * 
   * @param dutyCycle Duty cycle as a decimal e.g. 0.01 = 1%
   */
  public SingleTransmissionDutyCycleManager(double dutyCycle) {
    this(0, dutyCycle);
  }

  /**
   * Create a new single transmission duty cycle manager.
   * 
   * @param initTime Initialisation time
   * @param dutyCycle Duty cycle as a decimal e.g. 0.01 = 1%
   */
  public SingleTransmissionDutyCycleManager(long initTime, double dutyCycle) {
    super(dutyCycle);
    this.nextTransmitTime = initTime;
  }

  @Override
  public void transmit(long time, int airtime) {
    if (!canTransmit(time, airtime)) {
      throw new IllegalStateException("Cannot transmit within boundaries of duty cycle manager");
    }
    this.nextTransmitTime = (long) Math.ceil(time + airtime / dutyCycle);
  }

  @Override
  public boolean canTransmit(long time, int airtime) {
    return time >= whenCanTransmit(time, airtime);
  }
  
  @Override
  public long whenCanTransmit(long time, int airtime) {
    if (getAvailableTransmitTime(time) > airtime) {
      return this.nextTransmitTime;
    } else {
      return 0;
    }
  }

  @Override
  public long getAvailableTransmitTime(long time) {
    return (long) Math.floor(60 * 60 * 1000 * dutyCycle);
  }

}
