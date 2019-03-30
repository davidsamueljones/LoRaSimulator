package ecs.soton.dsj1n15.smesh.model.dutycycle;

public abstract class DutyCycleManager {
  protected final double dutyCycle;

  /**
   * Create a new duty cycle manager.
   * 
   * @param dutyCycle Duty cycle as a decimal e.g. 0.01 = 1%
   */
  public DutyCycleManager(double dutyCycle) {
    this.dutyCycle = dutyCycle;
  }

  /**
   * Update duty cycle manager state for a new transmit for a given length.
   * 
   * @param time The current time in ms
   * @param airtime The full packet airtime in ms
   */
  public abstract void transmit(long time, int airtime);

  /**
   * Determine whether a packet could be transmitted at this time legally.
   *
   * @param time The current time in ms
   * @param airtime The full packet airtime in ms
   * @return Whether the packet can be transmitted legally
   */
  public abstract boolean canTransmit(long time, int airtime);
  
  /**
   * Determine a time when a packet can be transmitted legally.
   *
   * @param time The current time in ms
   * @param airtime The full packet airtime in ms
   * @return Time the packet can be sent
   */
  public abstract long whenCanTransmit(long time, int airtime);

  /**
   * @param time The current time in ms
   * @return The amount of available transmission time for a single transmission in ms from the
   *         point of the next available transmission time.
   */
  public abstract long getAvailableTransmitTime(long time);

}
