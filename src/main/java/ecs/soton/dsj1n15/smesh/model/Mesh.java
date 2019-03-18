package ecs.soton.dsj1n15.smesh.model;

import java.util.LinkedHashSet;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;

public class Mesh {

  /** A unique ID for the mesh */
  private final int id;

  /** A list of all nodes that belong to the mesh */
  private final Set<LoRaRadio> nodes = new LinkedHashSet<>();


  private Environment environment = null;

  /**
   * Construct a new mesh.
   * 
   * @param id Unique id of the mesh
   */
  public Mesh(int id) {
    this.id = id;
  }

  public int getID() {
    return id;
  }

  public Set<LoRaRadio> getNodes() {
    return nodes;
  }

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public int hashCode() {
    final int prime = 197;
    int result = 1;
    result = prime * result + id;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Mesh))
      return false;
    Mesh other = (Mesh) obj;
    if (id != other.id)
      return false;
    return true;
  }



}
