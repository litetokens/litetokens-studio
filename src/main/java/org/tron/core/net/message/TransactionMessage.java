package org.litetokens.core.net.message;

import org.litetokens.common.utils.Sha256Hash;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.exception.BadItemException;
import org.litetokens.protos.Protocol.Transaction;

public class TransactionMessage extends LitetokensMessage {

  private TransactionCapsule transactionCapsule;

  public TransactionMessage(byte[] data) throws BadItemException {
    this.transactionCapsule = new TransactionCapsule(data);
    this.data = data;
    this.type = MessageTypes.XLT.asByte();
  }

  public TransactionMessage(Transaction xlt) {
    this.transactionCapsule = new TransactionCapsule(xlt);
    this.type = MessageTypes.XLT.asByte();
    this.data = xlt.toByteArray();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString())
        .append("messageId: ").append(super.getMessageId()).toString();
  }

  @Override
  public Sha256Hash getMessageId() {
    return this.transactionCapsule.getTransactionId();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public TransactionCapsule getTransactionCapsule() {
    return this.transactionCapsule;
  }
}
