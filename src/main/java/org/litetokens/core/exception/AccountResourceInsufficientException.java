package org.litetokens.core.exception;

public class AccountResourceInsufficientException extends LitetokensException {

  public AccountResourceInsufficientException() {
    super("Insufficient bandwidth and balance to create new account");
  }

  public AccountResourceInsufficientException(String message) {
    super(message);
  }
}

