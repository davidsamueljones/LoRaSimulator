package ecs.soton.dsj1n15.smesh.model.presets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ecs.soton.dsj1n15.smesh.controller.Event;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;

public abstract class Preset {
  public static final double DEFAULT_NODE_Z = 0.225;
  
  protected Environment environment;
  protected Map<Long, List<Event>> events = new LinkedHashMap<>();
  
  protected abstract void generate();
  
  public Environment getEnvironment() {
    return environment;
  }
  
  public Map<Long, List<Event>> getEvents() {
    return events;
  }
  
  public void addEvent(long time, Event event) {
    if (!events.containsKey(time)) {
      events.put(time, new ArrayList<>());
    }
    events.get(time).add(event);
  }
  
}
