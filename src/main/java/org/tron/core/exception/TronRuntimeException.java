package org.litetokens.core.exception;

public class LitetokensRuntimeException extends RuntimeException {

  public LitetokensRuntimeException() {
    super();
  }

  public LitetokensRuntimeException(String message) {
    super(message);
  }

  public LitetokensRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public LitetokensRuntimeException(Throwable cause) {
    super(cause);
  }

  protected LitetokensRuntimeException(String message, Throwable cause,
                             boolean enableSuppression,
                             boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
