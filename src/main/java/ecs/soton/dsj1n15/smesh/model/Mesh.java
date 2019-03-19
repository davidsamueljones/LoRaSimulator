package ecs.soton.dsj1n15.smesh.model;

import java.util.LinkedHashSet;
import java.util.Set;
import ecs.soton.dsj1n15.smesh.model.environment.Environment;

public class Mesh {

  /** A unique ID for the mesh */
  private final int id;


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
