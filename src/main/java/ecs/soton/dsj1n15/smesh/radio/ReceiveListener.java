package ecs.soton.dsj1n15.smesh.radio;

/**
 * Listener that gets called whenever a radio gets a receive or a failed receive that would be known
 * about.
 * 
 * @author David Jones (dsj1n15)
 */
public interface ReceiveListener {

  /**
   * Method that gets called on a receive.
   * 
   * @param result The receive result for the last receive.
   */
  public void receive(ReceiveResult result);

}
