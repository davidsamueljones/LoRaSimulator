package ecs.soton.dsj1n15.smesh.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;
import ecs.soton.dsj1n15.smesh.radio.Radio;

/**
 * Environment runner for executing simulation on a separate thread with incremental tick behaviour.
 * 
 * @author David Jones (dsj1n15)
 */
public class EnvironmentRunner {
  /** The currently loaded environment */
  private Environment environment = null;
  /** Thread simulation is being executed on */
  private final Thread runner;

  /** Whether the environment is running */
  private volatile boolean running = false;
  /** The number of units left to run if not running */
  private volatile long unitsToRun = 0;
  /** The amount of time each unit represents */
  private volatile int timeUnit = 5;

  /** List of listeners */
  private List<EnvironmentRunnerListener> listeners = new ArrayList<>();

  /** List of events to execute */
  private Map<Long, List<Event>> eventMap = new LinkedHashMap<>();

  /**
   * Start a new execution thread running the environment.
   */
  public EnvironmentRunner() {
    runner = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!Thread.interrupted()) {
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          if (environment != null) {
            while (running || unitsToRun > 0) {
              // Run any initialisation events one time
              if (environment.getTime() == 0) {
                runEvents();
              }
              environment.addTime(timeUnit);
              if (unitsToRun > 0) {
                unitsToRun--;
              }
              // Do simulation behaviour
              runEvents();
              // Handle radio behaviour
              for (Radio radio : environment.getNodes()) {
                if (radio.getCurrentTransmission() == null) {
                  radio.recv();
                }
                radio.tick();
              }
              // Let listeners know an update has occurred
              for (EnvironmentRunnerListener listener : listeners) {
                listener.update();
              }
            }
          }
        }
      }
    });
    runner.start();
  }

  /**
   * @return The execution thread running the environment
   */
  public Thread getExecutionThread() {
    return runner;
  }

  /**
   * @return The environment that is being run
   */
  public Environment getEnvironment() {
    return environment;
  }

  /**
   * @param environment The new environment to run
   */
  public void setEnvironment(Environment environment) {
    if (!running && unitsToRun == 0) {
      this.environment = environment;
    } else {
      throw new IllegalStateException("Cannot change environment whilst running");
    }
  }

  /**
   * Run all events
   */
  private void runEvents() {
    for (int i = (timeUnit); i > 0; i--) {
      long time = environment.getTime() - i;
      if (time < 0) {
        continue;
      }
      List<Event> events = eventMap.get(time);
      if (events != null) {
        for (Event event : events) {
          event.execute();
        }
      }
    }
  }

  /**
   * Add new events, appending existing events.
   * 
   * @param eventMap Map of new events
   */
  public void addEvents(Map<Long, List<Event>> eventMap) {
    for (Entry<Long, List<Event>> entry : eventMap.entrySet()) {
      if (this.eventMap.containsKey(entry.getKey())) {
        this.eventMap.get(entry.getKey()).addAll(entry.getValue());
      } else {
        this.eventMap.put(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Clear existing events
   */
  public void clearEvents() {
    eventMap.clear();
  }

  /**
   * @return Whether the simulation is running
   */
  public boolean isRunning() {
    return running || unitsToRun > 0;
  }

  /**
   * Start the simulation in run mode.
   */
  public void start() {
    this.running = true;
    this.unitsToRun = 0;
  }

  /**
   * Stop the simulation.
   */
  public void stop() {
    this.running = false;
    this.unitsToRun = 0;
  }

  /**
   * @return The number of units still to run
   */
  public long getUnitsToRun() {
    return unitsToRun;
  }

  /**
   * @param unitsToRun The new number of units to run
   */
  public void setUnitsToRun(int unitsToRun) {
    this.unitsToRun = unitsToRun;
  }

  /**
   * @param unitsToRun Add units to run
   */
  public void addUnitsToRun(long unitsToRun) {
    this.unitsToRun += unitsToRun;
  }

  /**
   * @return The current time unit
   */
  public int getTimeUnit() {
    return timeUnit;
  }

  /**
   * @param timeUnit The new time unit to use
   */
  public void setTimeUnit(int timeUnit) {
    this.timeUnit = timeUnit;
  }

  /**
   * @return A list of valid time units
   */
  public List<Integer> getTimeUnitOptions() {
    List<Integer> timeUnits = new ArrayList<>();
    timeUnits.add(1);
    timeUnits.add(5);
    timeUnits.add(10);
    timeUnits.add(50);
    timeUnits.add(100);
    return timeUnits;
  }

  /**
   * @param listener Listener to add
   */
  public void addListener(EnvironmentRunnerListener listener) {
    listeners.add(listener);
  }

  /**
   * @param listener Listener to remove
   */
  public void removeListener(EnvironmentRunnerListener listener) {
    listeners.remove(listener);
  }

}
