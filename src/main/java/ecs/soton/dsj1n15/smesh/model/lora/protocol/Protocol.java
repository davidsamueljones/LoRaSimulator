package ecs.soton.dsj1n15.smesh.model.lora.protocol;

import java.util.Random;
import ecs.soton.dsj1n15.smesh.lib.Utilities;

/**
 * Protocol container for configuring and managing a protocol for a set of nodes.
 * 
 * @author David Jones (dsj1n15)
 */
public abstract class Protocol {

  /** Random object to use for all randomness */
  protected final Random r = Utilities.RANDOM;

  /**
   * Initialise the protocol.
   */
  public abstract void init();

  /**
   * Print the results of all nodes being managed by the protocol.
   */
  public abstract void printResults();

}
