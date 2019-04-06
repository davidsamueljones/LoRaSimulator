package ecs.soton.dsj1n15.smesh.model.lora.protocol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import ecs.soton.dsj1n15.smesh.lib.Utilities;
import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.Transmission;

public abstract class Protocol {
  /** Random object to use for all randomness */
  protected final Random r = Utilities.RANDOM;
  
  /** Map of how many nodes are active at each point in time */
  protected final Map<Long, Integer> activityMap = new LinkedHashMap<>();


  public void dumpActivity() {
    File outputFile = new File("activity_dump.txt");
    try (PrintWriter pw = new PrintWriter(outputFile)) {
      printAndWrite(pw, "time,activity\n");
      for (Entry<Long, Integer> entry : activityMap.entrySet()) {
        printAndWrite(pw, entry.getKey() + "," + entry.getValue() + "\n");
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  protected void printAndWrite(PrintWriter pw, String string) {
    System.out.print(string);
    pw.write(string);
  }

  public abstract void printResults();

}
