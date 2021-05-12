package org.litetokens.core.exception;

public class VMMemoryOverflowException extends LitetokensException {

  public VMMemoryOverflowException() {
    super("VM memory overflow");
  }

  public VMMemoryOverflowException(String message) {
    super(message);
  }

}
