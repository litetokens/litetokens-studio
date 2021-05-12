package org.litetokens.core.net.message;

import java.util.List;

import org.litetokens.core.exception.P2pException;
import org.litetokens.protos.Protocol;
import org.litetokens.protos.Protocol.Transaction;

public class TransactionsMessage extends LitetokensMessage {

  private Protocol.Transactions transactions;

  public TransactionsMessage(List<Transaction> xlts) {
    Protocol.Transactions.Builder builder = Protocol.Transactions.newBuilder();
    xlts.forEach(xlt -> builder.addTransactions(xlt));
    this.transactions = builder.build();
    this.type = MessageTypes.XLTS.asByte();
    this.data = this.transactions.toByteArray();
  }

  public TransactionsMessage(byte[] data) throws Exception {
    this.type = MessageTypes.XLTS.asByte();
    this.data = data;
    this.transactions = Protocol.Transactions.parseFrom(data);
  }

  public Protocol.Transactions getTransactions() {
    return transactions;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append("xlt size: ")
        .append(this.transactions.getTransactionsList().size()).toString();
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

}
