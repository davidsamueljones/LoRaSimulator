package ecs.soton.dsj1n15.smesh.model.lora;

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
  public void transmit(long time, double airtime) {
    if (!canTransmit(time, airtime)) {
      throw new IllegalStateException("Cannot transmit within boundaries of duty cycle manager");
    }
    this.nextTransmitTime = (long) Math.ceil((time + airtime) + airtime / dutyCycle);
  }

  @Override
  public long nextAvailableTransmitTime() {
    return this.nextTransmitTime;
  }

  @Override
  public long getAvailableTransmitTime() {
    return (long) Math.floor(60 * 60 * 1000 * dutyCycle);
  }

}
