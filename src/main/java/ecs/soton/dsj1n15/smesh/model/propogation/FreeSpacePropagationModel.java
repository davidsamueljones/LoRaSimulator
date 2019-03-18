 package ecs.soton.dsj1n15.smesh.model.propogation;

public class FreeSpacePropagationModel extends PropagationModel {
  
  protected double freq;
  
  public FreeSpacePropagationModel(double freq) {
    this.freq = freq;
  }
  
  @Override
  public double getPathLoss(double distance) {
    return 20*Math.log10(distance) + 20*Math.log10(freq) - 27.55;
  }

}
