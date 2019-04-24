package ecs.soton.dsj1n15.smesh.controller;

import ecs.soton.dsj1n15.smesh.radio.Radio;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.MetadataStatus;
import ecs.soton.dsj1n15.smesh.radio.ReceiveResult.Status;

/**
 * Event for checking whether a radios last receive was as expected for test verification.
 * 
 * @author David Jones (dsj1n15)
 */
public class ReceiveTestCheckEvent implements Event {
  public final Radio receiver;
  public final Radio expSender;
  public final Status expStatus;
  public final MetadataStatus expMetadataStatus;

  private boolean testSuccess;

  /**
   * Create a new receive check event.
   * 
   * @param receiver Receiver to check
   * @param expSender Sender to expect from, can be null in case this is unpredictable
   * @param expStatus Status to expect
   * @param expMetadataStatus Metadata status to expect
   */
  public ReceiveTestCheckEvent(Radio receiver, Radio expSender, Status expStatus,
      MetadataStatus expMetadataStatus) {
    this.receiver = receiver;
    this.expSender = expSender;
    this.expStatus = expStatus;
    this.expMetadataStatus = expMetadataStatus;
  }

  /**
   * @return Whether the test was successful
   */
  public boolean isTestSuccess() {
    return testSuccess;
  }

  @Override
  public void execute() {
    ReceiveResult res = receiver.getLastReceive();
    if (res == null) {
      System.out.println("[Node C] No receive at expected time!");
    } else {
      System.out
          .print(String.format("[Node C] Sender = %d, Recv = Status = %s, Metadata Status = %s : ",
              res.transmission.sender.getID(), res.status, res.metadataStatus));
      if ((expSender == null || res.transmission.sender == expSender) && res.status == expStatus
          && res.metadataStatus == expMetadataStatus) {
        System.out.println("[SUCCESS]");
        testSuccess = true;
      } else {
        System.out.println("[FAILURE]");
        testSuccess = false;
      }
    }
  }
}
