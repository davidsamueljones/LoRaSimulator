package ecs.soton.dsj1n15.smesh.model.presets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ecs.soton.dsj1n15.smesh.controller.Event;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaCfg;
import ecs.soton.dsj1n15.smesh.model.lora.LoRaRadio;
import ecs.soton.dsj1n15.smesh.model.lora.RFM95W;
import ecs.soton.dsj1n15.smesh.radio.Radio;

/**
 * Generic preset for radio testing. Can hold both an envioronment and a set of scheduled events
 * that can be executed by an environment runner. By changing the static radio type, any presets
 * created will use that radio type.
 * 
 * @author David Jones (dsj1n15)
 */
public abstract class Preset {
  public static final double DEFAULT_NODE_Z = 0.225;
  public static RadioType radioType = RadioType.RFM95W;

  protected Environment environment = null;
  protected Map<Long, List<Event>> events = new LinkedHashMap<>();

  /**
   * Generate the preset, make a fresh version if called again.
   */
  public abstract void generate();

  /**
   * @return The generated environment.
   */
  public Environment getEnvironment() {
    return environment;
  }

  /**
   * @return Mapping of generated preset events and their scheduled times.
   */
  public Map<Long, List<Event>> getEvents() {
    return events;
  }

  /**
   * Add a new event to the corresponding time. Create a new list of events at that time if this is
   * the first event.
   * 
   * @param time Time for event to occur
   * @param event Event to add
   */
  public void addEvent(long time, Event event) {
    if (!events.containsKey(time)) {
      events.put(time, new ArrayList<>());
    }
    events.get(time).add(event);
  }

  @Deprecated
  protected Radio generateRadio(int id) {
    throw new IllegalStateException("Current radio type is not a generic radio");
  }

  /**
   * Method for generating a preset configured LoRa radio. Will throw an exception if the current
   * preset radio type selection is not LoRa.
   * 
   * @param id ID of radio
   * @param cfg LoRa configuration to preset with
   * @return Generated radio
   */
  protected LoRaRadio generateLoRaRadio(int id, LoRaCfg cfg) {
    switch (radioType) {
      case LoRaRadio:
        return new LoRaRadio(id, cfg);
      case RFM95W:
        return new RFM95W(id, cfg);
      default:
        throw new IllegalStateException("Current radio type is not a LoRa radio");
    }
  }

  public enum RadioType {
    LoRaRadio, RFM95W
  }

}
