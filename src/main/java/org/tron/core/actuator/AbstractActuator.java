package org.litetokens.core.actuator;

import com.google.protobuf.Any;
import org.litetokens.common.storage.Deposit;
import org.litetokens.core.capsule.TransactionResultCapsule;
import org.litetokens.core.db.Manager;
import org.litetokens.core.exception.ContractExeException;

public abstract class AbstractActuator implements Actuator {

  protected Any contract;
  protected Manager dbManager;

  public Deposit getDeposit() {
    return deposit;
  }

  public void setDeposit(Deposit deposit) {
    this.deposit = deposit;
  }

  protected Deposit deposit;

  AbstractActuator(Any contract, Manager dbManager) {
    this.contract = contract;
    this.dbManager = dbManager;
  }
}
