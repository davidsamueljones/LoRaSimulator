package ecs.soton.dsj1n15.smesh.controller;

import ecs.soton.dsj1n15.smesh.radio.Radio;

public class MoveEvent implements Event {

  private final Radio radio;
  private final double x;
  private final double y;
  private final boolean shift;

  /**
   * Create a movement event.
   * 
   * @param radio Radio to move
   * @param x X shift or new position
   * @param y Y shift or new position
   * @param shift Whether to shift by x and y coordinates as opposed to move
   */
  public MoveEvent(Radio radio, double x, double y, boolean shift) {
    this.radio = radio;
    this.x = x;
    this.y = y;
    this.shift = shift;
  }

  @Override
  public void execute() {
    double newX;
    double newY;
    if (shift) {
      newX = radio.getX() + x;
      newY = radio.getY() + y;
    } else {
      newX = x;
      newY = y;
    }
    radio.setX(newX);
    radio.setY(newY);
  }

}
