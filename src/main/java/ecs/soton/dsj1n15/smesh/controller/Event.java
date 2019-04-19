package ecs.soton.dsj1n15.smesh.controller;

/**
 * Event that can be executed by a EnvironmentRunner.
 * 
 * @author David Jones (dsj1n15)
 */
public interface Event {

  /**
   * Method that gets executed by runner.
   */
  public void execute();
  
}
