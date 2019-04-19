package ecs.soton.dsj1n15.smesh.radio;

/**
 * Listener that gets called every time the radios 'tick' method is called.<br>
 * All extra radio behaviour, such as protocols, can be attached using a tick listener.
 * 
 * @author David Jones (dsj1n15)
 */
public interface TickListener {

  /**
   * Called method.
   */
  public void tick();

}
