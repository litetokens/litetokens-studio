package org.litetokens.core.exception;

public class LitetokensException extends Exception {

  public LitetokensException() {
    super();
  }

  public LitetokensException(String message) {
    super(message);
  }

  public LitetokensException(String message, Throwable cause) {
    super(message, cause);
  }

}
