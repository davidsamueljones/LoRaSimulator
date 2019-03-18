package ecs.soton.dsj1n15.smesh.model.propogation;

public class PlainEarthPropagationModel extends PropagationModel {

  protected double th;
  protected double rh;

  public PlainEarthPropagationModel(double th, double rh) {
    this.th = th;
    this.rh = rh;
  }

  @Override
  public double getPathLoss(double distance) {
    return 40 * Math.log10(distance) - 20 * Math.log10(th) - 20 * Math.log10(rh);
  }

}
