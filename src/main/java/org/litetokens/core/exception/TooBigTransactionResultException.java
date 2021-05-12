package org.litetokens.core.exception;

public class TooBigTransactionResultException extends LitetokensException {

    public TooBigTransactionResultException() { super("too big transaction result"); }

    public TooBigTransactionResultException(String message) { super(message); }
}
