package org.litetokens.common.runtime;

import lombok.extern.slf4j.Slf4j;
import org.litetokens.core.capsule.ReceiptCapsule;

@Slf4j
public class LVMTestResult {

  private Runtime runtime;
  private ReceiptCapsule receipt;
  private byte[] contractAddress;

  public byte[] getContractAddress() {
    return contractAddress;
  }

  public LVMTestResult setContractAddress(byte[] contractAddress) {
    this.contractAddress = contractAddress;
    return this;
  }

  public Runtime getRuntime() {
    return runtime;
  }

  public LVMTestResult setRuntime(Runtime runtime) {
    this.runtime = runtime;
    return this;
  }

  public ReceiptCapsule getReceipt() {
    return receipt;
  }

  public LVMTestResult setReceipt(ReceiptCapsule receipt) {
    this.receipt = receipt;
    return this;
  }

  public LVMTestResult(Runtime runtime, ReceiptCapsule receipt, byte[] contractAddress) {
    this.runtime = runtime;
    this.receipt = receipt;
    this.contractAddress = contractAddress;
  }

}
