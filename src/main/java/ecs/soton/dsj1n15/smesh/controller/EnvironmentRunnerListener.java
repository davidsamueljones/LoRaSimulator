package ecs.soton.dsj1n15.smesh.controller;

/**
 * Listener that gets called every environment runner tick.
 * 
 * @author David Jones (dsj1n15)
 */
public interface EnvironmentRunnerListener {

  /**
   * Method called on every tick for custom run behaviour.
   */
  public void update();
  
}
