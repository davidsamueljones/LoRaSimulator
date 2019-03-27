package ecs.soton.dsj1n15.smesh.model.lora;

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
  public abstract void transmit(long time, double airtime);

  /**
   * Determine whether a packet could be transmitted at this time legally.
   *
   * @param time The current time in ms
   * @param airtime The full packet airtime in ms
   * @return Whether the packet can be transmitted legally
   */
  public boolean canTransmit(long time, double airtime) {
    boolean canTransmit = true;
    canTransmit &= time >= nextAvailableTransmitTime();
    canTransmit &= getAvailableTransmitTime() > airtime;
    return canTransmit;
  }

  /**
   * @return The time when a transmission is next allowed
   */
  public abstract long nextAvailableTransmitTime();

  /**
   * @return The amount of available transmission time for a single transmission in ms from the point of
   *         the next available transmission time.
   */
  public abstract long getAvailableTransmitTime();
  
}
