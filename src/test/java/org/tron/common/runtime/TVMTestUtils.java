package org.litetokens.common.runtime;

import static stest.litetokens.wallet.common.client.utils.PublicMethed.jsonStr2Abi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.litetokens.common.crypto.Hash;
import org.litetokens.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.litetokens.common.storage.DepositImpl;
import org.litetokens.core.Wallet;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.db.Manager;
import org.litetokens.core.db.TransactionTrace;
import org.litetokens.core.exception.ContractExeException;
import org.litetokens.core.exception.ContractValidateException;
import org.litetokens.core.exception.ReceiptCheckErrException;
import org.litetokens.core.exception.VMIllegalException;
import org.litetokens.protos.Contract;
import org.litetokens.protos.Contract.CreateSmartContract;
import org.litetokens.protos.Contract.TriggerSmartContract;
import org.litetokens.protos.Protocol.SmartContract;
import org.litetokens.protos.Protocol.Transaction;
import org.litetokens.protos.Protocol.Transaction.Contract.ContractType;
import stest.litetokens.wallet.common.client.Parameter.CommonConstant;
import stest.litetokens.wallet.common.client.WalletClient;
import stest.litetokens.wallet.common.client.utils.AbiUtil;


/**
 * Below functions mock the process to deploy, trigger a contract. Not consider of the transaction
 * process on real chain(such as db revoke...). Just use them to execute runtime actions and vm
 * commands.
 */
@Slf4j
public class TVMTestUtils {

  public static byte[] deployContractWholeProcessReturnContractAddress(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, DepositImpl deposit, BlockCapsule block)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction xlt = generateDeploySmartContractAndGetTransaction(contractName, callerAddress, ABI,
        code, value, feeLimit, consumeUserResourcePercent, libraryAddressPair);
    processTransactionAndReturnRuntime(xlt, deposit, block);
    return Wallet.generateContractAddress(xlt);
  }

  public static Runtime triggerContractWholeProcessReturnContractAddress(byte[] callerAddress,
      byte[] contractAddress, byte[] data, long callValue, long feeLimit, DepositImpl deposit,
      BlockCapsule block)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction xlt = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        data, callValue, feeLimit);
    return processTransactionAndReturnRuntime(xlt, deposit, block);
  }

  /**
   * return generated smart contract Transaction, just before we use it to broadcast and push
   * transaction
   */
  public static Transaction generateDeploySmartContractAndGetTransaction(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair) {

    CreateSmartContract contract = buildCreateSmartContract(contractName, callerAddress, ABI, code,
        value, consumeUserResourcePercent, libraryAddressPair);
    TransactionCapsule xltCapWithoutFeeLimit = new TransactionCapsule(contract,
        ContractType.CreateSmartContract);
    Transaction.Builder transactionBuilder = xltCapWithoutFeeLimit.getInstance().toBuilder();
    Transaction.raw.Builder rawBuilder = xltCapWithoutFeeLimit.getInstance().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transactionBuilder.setRawData(rawBuilder);
    Transaction xlt = transactionBuilder.build();
    return xlt;
  }

  /**
   * use given input Transaction,deposit,block and execute TVM  (for both Deploy and Trigger
   * contracts)
   */

  public static Runtime processTransactionAndReturnRuntime(Transaction xlt,
      DepositImpl deposit, BlockCapsule block)
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    TransactionCapsule xltCap = new TransactionCapsule(xlt);
    TransactionTrace trace = new TransactionTrace(xltCap, deposit.getDbManager());
    Runtime runtime = new Runtime(trace, block, deposit,
        new ProgramInvokeFactoryImpl());

    // init
    trace.init();
    //exec
    trace.exec(runtime);

    trace.finalization(runtime);

    return runtime;
  }


  public static TVMTestResult deployContractAndReturnTVMTestResult(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, Manager dbManager, BlockCapsule blockCap)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction xlt = generateDeploySmartContractAndGetTransaction(contractName, callerAddress, ABI,
        code, value, feeLimit, consumeUserResourcePercent, libraryAddressPair);

    byte[] contractAddress = Wallet.generateContractAddress(xlt);

    return processTransactionAndReturnTVMTestResult(xlt, dbManager, blockCap)
        .setContractAddress(Wallet.generateContractAddress(xlt));
  }

  public static TVMTestResult triggerContractAndReturnTVMTestResult(byte[] callerAddress,
      byte[] contractAddress, byte[] data, long callValue, long feeLimit, Manager dbManager,
      BlockCapsule blockCap)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction xlt = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        data, callValue, feeLimit);
    return processTransactionAndReturnTVMTestResult(xlt, dbManager, blockCap)
        .setContractAddress(contractAddress);
  }


  public static TVMTestResult processTransactionAndReturnTVMTestResult(Transaction xlt,
      Manager dbManager, BlockCapsule blockCap)
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    TransactionCapsule xltCap = new TransactionCapsule(xlt);
    TransactionTrace trace = new TransactionTrace(xltCap, dbManager);
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    Runtime runtime = new Runtime(trace, blockCap, deposit,
        new ProgramInvokeFactoryImpl());

    // init
    trace.init();
    //exec
    trace.exec(runtime);

    trace.finalization(runtime);

    return new TVMTestResult(runtime, trace.getReceipt(), null);
  }


  /**
   * create the Contract Instance for smart contract.
   */
  public static CreateSmartContract buildCreateSmartContract(String contractName,
      byte[] address,
      String ABI, String code, long value, long consumeUserResourcePercent,
      String libraryAddressPair) {
    SmartContract.ABI abi = jsonStr2ABI(ABI);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }

    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(address));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);

    if (value != 0) {
      builder.setCallValue(value);
    }
    byte[] byteCode;
    if (null != libraryAddressPair) {
      byteCode = replaceLibraryAddress(code, libraryAddressPair);
    } else {
      byteCode = Hex.decode(code);
    }

    builder.setBytecode(ByteString.copyFrom(byteCode));
    return CreateSmartContract.newBuilder().setOwnerAddress(ByteString.copyFrom(address)).
        setNewContract(builder.build()).build();
  }


  public static Transaction generateTriggerSmartContractAndGetTransaction(
      byte[] callerAddress, byte[] contractAddress, byte[] data, long callValue, long feeLimit) {

    TriggerSmartContract contract = buildTriggerSmartContract(callerAddress, contractAddress, data,
        callValue);
    TransactionCapsule xltCapWithoutFeeLimit = new TransactionCapsule(contract,
        ContractType.TriggerSmartContract);
    Transaction.Builder transactionBuilder = xltCapWithoutFeeLimit.getInstance().toBuilder();
    Transaction.raw.Builder rawBuilder = xltCapWithoutFeeLimit.getInstance().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transactionBuilder.setRawData(rawBuilder);
    Transaction xlt = transactionBuilder.build();
    return xlt;
  }


  public static TriggerSmartContract buildTriggerSmartContract(byte[] address,
      byte[] contractAddress, byte[] data, long callValue) {
    Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    return builder.build();
  }

  private static byte[] replaceLibraryAddress(String code, String libraryAddressPair) {

    String[] libraryAddressList = libraryAddressPair.split("[,]");

    for (int i = 0; i < libraryAddressList.length; i++) {
      String cur = libraryAddressList[i];

      int lastPosition = cur.lastIndexOf(":");
      if (-1 == lastPosition) {
        throw new RuntimeException("libraryAddress delimit by ':'");
      }
      String libraryName = cur.substring(0, lastPosition);
      String addr = cur.substring(lastPosition + 1);
      String libraryAddressHex;
      try {
        libraryAddressHex = (new String(Hex.encode(WalletClient.decodeFromBase58Check(addr)),
            "US-ASCII")).substring(2);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);  // now ignore
      }
      String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
      String beReplaced = "__" + libraryName + repeated;
      Matcher m = Pattern.compile(beReplaced).matcher(code);
      code = m.replaceAll(libraryAddressHex);
    }

    return Hex.decode(code);
  }

  private static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
    switch (type) {
      case "constructor":
        return SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContract.ABI.Entry.EntryType.Fallback;
      default:
        return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }

  private static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
      String stateMutability) {
    switch (stateMutability) {
      case "pure":
        return SmartContract.ABI.Entry.StateMutabilityType.Pure;
      case "view":
        return SmartContract.ABI.Entry.StateMutabilityType.View;
      case "nonpayable":
        return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
      case "payable":
        return SmartContract.ABI.Entry.StateMutabilityType.Payable;
      default:
        return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
    }
  }

  private static SmartContract.ABI jsonStr2ABI(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null &&
          abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
      boolean constant = abiItem.getAsJsonObject().get("constant") != null &&
          abiItem.getAsJsonObject().get("constant").getAsBoolean();
      String name = abiItem.getAsJsonObject().get("name") != null ?
          abiItem.getAsJsonObject().get("name").getAsString() : null;
      JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null ?
          abiItem.getAsJsonObject().get("inputs").getAsJsonArray() : null;
      JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null ?
          abiItem.getAsJsonObject().get("outputs").getAsJsonArray() : null;
      String type = abiItem.getAsJsonObject().get("type") != null ?
          abiItem.getAsJsonObject().get("type").getAsString() : null;
      boolean payable = abiItem.getAsJsonObject().get("payable") != null &&
          abiItem.getAsJsonObject().get("payable").getAsBoolean();
      String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null ?
          abiItem.getAsJsonObject().get("stateMutability").getAsString() : null;
      if (type == null) {
        logger.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback") && null == inputs) {
        logger.error("No inputs!");
        return null;
      }

      SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (null != inputs) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null ||
              inputItem.getAsJsonObject().get("type") == null) {
            logger.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
              .newBuilder();
          paramBuilder.setIndexed(false);
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null ||
              outputItem.getAsJsonObject().get("type") == null) {
            logger.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
              .newBuilder();
          paramBuilder.setIndexed(false);
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }

      entryBuilder.setType(getEntryType(type));
      entryBuilder.setPayable(payable);
      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }


  public static byte[] parseABI(String selectorStr, String params) {
    if (params == null) {
      params = "";
    }
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(selectorStr.getBytes()), 0, selector, 0, 4);
    byte[] triggerData = Hex.decode(Hex.toHexString(selector) + params);
    return triggerData;
  }

  public static CreateSmartContract createSmartContract(byte[] owner, String contractName,
      String abiString, String code, long value, long consumeUserResourcePercent) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      return null;
    }
    byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setBytecode(ByteString.copyFrom(codeBytes));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    if (value != 0) {
      builder.setCallValue(value);
    }
    CreateSmartContract contractDeployContract = CreateSmartContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(owner)).setNewContract(builder.build()).build();
    return contractDeployContract;
  }

  public static TriggerSmartContract createTriggerContract(byte[] contractAddress, String method,
      String argsStr,
      Boolean isHex, long callValue, byte[] ownerAddress) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    return builder.build();
  }
}
