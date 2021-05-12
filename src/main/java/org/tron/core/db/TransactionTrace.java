package org.litetokens.core.db;

import static org.litetokens.common.runtime.vm.program.InternalTransaction.XltType.XLT_CONTRACT_CALL_TYPE;
import static org.litetokens.common.runtime.vm.program.InternalTransaction.XltType.XLT_CONTRACT_CREATION_TYPE;
import static org.litetokens.common.runtime.vm.program.InternalTransaction.XltType.XLT_PRECOMPILED_TYPE;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.litetokens.common.runtime.Runtime;
import org.litetokens.common.runtime.vm.program.InternalTransaction;
import org.litetokens.common.runtime.vm.program.Program.BadJumpDestinationException;
import org.litetokens.common.runtime.vm.program.Program.IllegalOperationException;
import org.litetokens.common.runtime.vm.program.Program.JVMStackOverFlowException;
import org.litetokens.common.runtime.vm.program.Program.OutOfEnergyException;
import org.litetokens.common.runtime.vm.program.Program.OutOfMemoryException;
import org.litetokens.common.runtime.vm.program.Program.OutOfResourceException;
import org.litetokens.common.runtime.vm.program.Program.PrecompiledContractException;
import org.litetokens.common.runtime.vm.program.Program.StackTooLargeException;
import org.litetokens.common.runtime.vm.program.Program.StackTooSmallException;
import org.litetokens.common.utils.Sha256Hash;
import org.litetokens.core.capsule.AccountCapsule;
import org.litetokens.core.capsule.ContractCapsule;
import org.litetokens.core.capsule.ReceiptCapsule;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.exception.BalanceInsufficientException;
import org.litetokens.core.exception.ContractExeException;
import org.litetokens.core.exception.ContractValidateException;
import org.litetokens.core.exception.ReceiptCheckErrException;
import org.litetokens.core.exception.VMIllegalException;
import org.litetokens.protos.Contract.TriggerSmartContract;
import org.litetokens.protos.Protocol.Transaction;
import org.litetokens.protos.Protocol.Transaction.Contract.ContractType;
import org.litetokens.protos.Protocol.Transaction.Result.contractResult;

@Slf4j(topic = "TransactionTrace")
public class TransactionTrace {

  private TransactionCapsule xlt;

  private ReceiptCapsule receipt;

  private Manager dbManager;

  private EnergyProcessor energyProcessor;

  private InternalTransaction.XltType xltType;

  private long txStartTimeInMs;

  public TransactionCapsule getXlt() {
    return xlt;
  }

  public enum TimeResultType {
    NORMAL,
    LONG_RUNNING,
    OUT_OF_TIME
  }

  @Getter
  @Setter
  private TimeResultType timeResultType = TimeResultType.NORMAL;

  public TransactionTrace(TransactionCapsule xlt, Manager dbManager) {
    this.xlt = xlt;
    Transaction.Contract.ContractType contractType = this.xlt.getInstance().getRawData()
        .getContract(0).getType();
    switch (contractType.getNumber()) {
      case ContractType.TriggerSmartContract_VALUE:
        xltType = XLT_CONTRACT_CALL_TYPE;
        break;
      case ContractType.CreateSmartContract_VALUE:
        xltType = XLT_CONTRACT_CREATION_TYPE;
        break;
      default:
        xltType = XLT_PRECOMPILED_TYPE;
    }

    this.dbManager = dbManager;
    this.receipt = new ReceiptCapsule(Sha256Hash.ZERO_HASH);

    this.energyProcessor = new EnergyProcessor(this.dbManager);
  }

  public boolean needVM() {
    return this.xltType == XLT_CONTRACT_CALL_TYPE || this.xltType == XLT_CONTRACT_CREATION_TYPE;
  }

  //pre transaction check
  public void init() {
    txStartTimeInMs = System.currentTimeMillis();
    // switch (xltType) {
    //   case XLT_PRECOMPILED_TYPE:
    //     break;
    //   case XLT_CONTRACT_CREATION_TYPE:
    //   case XLT_CONTRACT_CALL_TYPE:
    //     // checkForSmartContract();
    //     break;
    //   default:
    //     break;
    // }

  }

  //set bill
  public void setBill(long energyUsage) {
    if (energyUsage < 0) {
      energyUsage = 0L;
    }
    receipt.setEnergyUsageTotal(energyUsage);
  }

  //set net bill
  public void setNetBill(long netUsage, long netFee) {
    receipt.setNetUsage(netUsage);
    receipt.setNetFee(netFee);
  }

  public void exec(Runtime runtime)
      throws ContractExeException, ContractValidateException, VMIllegalException {
    /**  VM execute  **/
    runtime.execute();
    runtime.go();

    if (XLT_PRECOMPILED_TYPE != runtime.getXltType()) {
      if (contractResult.OUT_OF_TIME
          .equals(receipt.getResult())) {
        setTimeResultType(TimeResultType.OUT_OF_TIME);
      } else if (System.currentTimeMillis() - txStartTimeInMs
          > Args.getInstance().getLongRunningTime()) {
        setTimeResultType(TimeResultType.LONG_RUNNING);
      }
    }
  }

  public void finalization(Runtime runtime) throws ContractExeException {
    try {
      pay();
    } catch (BalanceInsufficientException e) {
      throw new ContractExeException(e.getMessage());
    }
    runtime.finalization();
  }

  /**
   * pay actually bill(include ENERGY and storage).
   */
  public void pay() throws BalanceInsufficientException {
    byte[] originAccount;
    byte[] callerAccount;
    long percent = 0;
    switch (xltType) {
      case XLT_CONTRACT_CREATION_TYPE:
        callerAccount = TransactionCapsule.getOwner(xlt.getInstance().getRawData().getContract(0));
        originAccount = callerAccount;
        break;
      case XLT_CONTRACT_CALL_TYPE:
        TriggerSmartContract callContract = ContractCapsule
            .getTriggerContractFromTransaction(xlt.getInstance());
        callerAccount = callContract.getOwnerAddress().toByteArray();

        ContractCapsule contract =
            dbManager.getContractStore().get(callContract.getContractAddress().toByteArray());
        originAccount = contract.getInstance().getOriginAddress().toByteArray();
        percent = Math.max(100 - contract.getConsumeUserResourcePercent(), 0);
        percent = Math.min(percent, 100);
        break;
      default:
        return;
    }

    // originAccount Percent = 30%
    AccountCapsule origin = dbManager.getAccountStore().get(originAccount);
    AccountCapsule caller = dbManager.getAccountStore().get(callerAccount);
    receipt.payEnergyBill(
        dbManager,
        origin,
        caller,
        percent,
        energyProcessor,
        dbManager.getWitnessController().getHeadSlot());
  }

  public boolean checkNeedRetry() {
    if (!needVM()) {
      return false;
    }
    if (!xlt.getContractRet().equals(contractResult.OUT_OF_TIME)
        && receipt.getResult().equals(contractResult.OUT_OF_TIME)) {
      return true;
    }
    return false;
  }

  public void check() throws ReceiptCheckErrException {
    if (!needVM()) {
      return;
    }
    if (Objects.isNull(xlt.getContractRet())) {
      throw new ReceiptCheckErrException("null resultCode");
    }
    if (!xlt.getContractRet().equals(receipt.getResult())) {
      logger.info(
          "this tx resultCode in received block: {}\nthis tx resultCode in self: {}",
          xlt.getContractRet(), receipt.getResult());
      throw new ReceiptCheckErrException("Different resultCode");
    }
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }

  public void setResult(Runtime runtime) {
    if (!needVM()) {
      return;
    }
    RuntimeException exception = runtime.getResult().getException();
    if (Objects.isNull(exception) && StringUtils
        .isEmpty(runtime.getRuntimeError()) && !runtime.getResult().isRevert()) {
      receipt.setResult(contractResult.SUCCESS);
      return;
    }
    if (runtime.getResult().isRevert()) {
      receipt.setResult(contractResult.REVERT);
      return;
    }
    if (exception instanceof IllegalOperationException) {
      receipt.setResult(contractResult.ILLEGAL_OPERATION);
      return;
    }
    if (exception instanceof OutOfEnergyException) {
      receipt.setResult(contractResult.OUT_OF_ENERGY);
      return;
    }
    if (exception instanceof BadJumpDestinationException) {
      receipt.setResult(contractResult.BAD_JUMP_DESTINATION);
      return;
    }
    if (exception instanceof OutOfResourceException) {
      receipt.setResult(contractResult.OUT_OF_TIME);
      return;
    }
    if (exception instanceof OutOfMemoryException) {
      receipt.setResult(contractResult.OUT_OF_MEMORY);
      return;
    }
    if (exception instanceof PrecompiledContractException) {
      receipt.setResult(contractResult.PRECOMPILED_CONTRACT);
      return;
    }
    if (exception instanceof StackTooSmallException) {
      receipt.setResult(contractResult.STACK_TOO_SMALL);
      return;
    }
    if (exception instanceof StackTooLargeException) {
      receipt.setResult(contractResult.STACK_TOO_LARGE);
      return;
    }
    if (exception instanceof JVMStackOverFlowException) {
      receipt.setResult(contractResult.JVM_STACK_OVER_FLOW);
      return;
    }
    receipt.setResult(contractResult.UNKNOWN);
    return;
  }
}
