package ecs.soton.dsj1n15.smesh.model.propogation;

public class ITURPropagationModel extends PropagationModel {

  protected double freq;
  
  public ITURPropagationModel(double freq) {
    this.freq = freq;
  }
  
  @Override
  public double getPathLoss(double distance) {
    return 0.2*Math.pow(freq, 0.3)*Math.pow(distance, 0.6);
  }

}
