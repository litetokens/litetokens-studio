package org.litetokens.core.services.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.litetokens.api.GrpcAPI.BlockList;
import org.litetokens.api.GrpcAPI.EasyTransferResponse;
import org.litetokens.api.GrpcAPI.TransactionExtention;
import org.litetokens.api.GrpcAPI.TransactionList;
import org.litetokens.common.crypto.Hash;
import org.litetokens.common.utils.ByteArray;
import org.litetokens.common.utils.Sha256Hash;
import org.litetokens.core.Wallet;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.services.http.JsonFormat.ParseException;
import org.litetokens.protos.Contract.AccountCreateContract;
import org.litetokens.protos.Contract.AccountUpdateContract;
import org.litetokens.protos.Contract.AssetIssueContract;
import org.litetokens.protos.Contract.CreateSmartContract;
import org.litetokens.protos.Contract.ExchangeCreateContract;
import org.litetokens.protos.Contract.ExchangeInjectContract;
import org.litetokens.protos.Contract.ExchangeTransactionContract;
import org.litetokens.protos.Contract.ExchangeWithdrawContract;
import org.litetokens.protos.Contract.FreezeBalanceContract;
import org.litetokens.protos.Contract.ParticipateAssetIssueContract;
import org.litetokens.protos.Contract.ProposalApproveContract;
import org.litetokens.protos.Contract.ProposalCreateContract;
import org.litetokens.protos.Contract.ProposalDeleteContract;
import org.litetokens.protos.Contract.TransferAssetContract;
import org.litetokens.protos.Contract.TransferContract;
import org.litetokens.protos.Contract.TriggerSmartContract;
import org.litetokens.protos.Contract.UnfreezeAssetContract;
import org.litetokens.protos.Contract.UnfreezeBalanceContract;
import org.litetokens.protos.Contract.UpdateAssetContract;
import org.litetokens.protos.Contract.VoteAssetContract;
import org.litetokens.protos.Contract.VoteWitnessContract;
import org.litetokens.protos.Contract.WithdrawBalanceContract;
import org.litetokens.protos.Contract.WitnessCreateContract;
import org.litetokens.protos.Contract.WitnessUpdateContract;
import org.litetokens.protos.Protocol;
import org.litetokens.protos.Protocol.Block;
import org.litetokens.protos.Protocol.SmartContract;
import org.litetokens.protos.Protocol.Transaction;


@Slf4j
public class Util {

  public static String printErrorMsg(Exception e) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("Error", e.getClass() + " : " + e.getMessage());
    return jsonObject.toJSONString();
  }

  public static String printBlockList(BlockList list) {
    List<Block> blocks = list.getBlockList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list));
    JSONArray jsonArray = new JSONArray();
    blocks.stream().forEach(block -> {
      jsonArray.add(printBlockToJSON(block));
    });
    jsonObject.put("block", jsonArray);

    return jsonObject.toJSONString();
  }

  public static String printBlock(Block block) {
    return printBlockToJSON(block).toJSONString();
  }

  public static JSONObject printBlockToJSON(Block block) {
    BlockCapsule blockCapsule = new BlockCapsule(block);
    String blockID = ByteArray.toHexString(blockCapsule.getBlockId().getBytes());
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(block));
    jsonObject.put("blockID", blockID);
    if (!blockCapsule.getTransactions().isEmpty()) {
      jsonObject.put("transactions", printTransactionListToJSON(blockCapsule.getTransactions()));
    }
    return jsonObject;
  }

  public static String printTransactionList(TransactionList list) {
    List<Transaction> transactions = list.getTransactionList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list));
    JSONArray jsonArray = new JSONArray();
    transactions.stream().forEach(transaction -> {
      jsonArray.add(printTransactionToJSON(transaction));
    });
    jsonObject.put("transaction", jsonArray);

    return jsonObject.toJSONString();
  }

  public static JSONArray printTransactionListToJSON(List<TransactionCapsule> list) {
    JSONArray transactions = new JSONArray();
    list.stream().forEach(transactionCapsule -> {
      transactions.add(printTransactionToJSON(transactionCapsule.getInstance()));
    });
    return transactions;
  }

  public static String printEasyTransferResponse(EasyTransferResponse response) {
    JSONObject jsonResponse = JSONObject.parseObject(JsonFormat.printToString(response));
    jsonResponse.put("transaction", printTransactionToJSON(response.getTransaction()));
    return jsonResponse.toJSONString();
  }

  public static String printTransaction(Transaction transaction) {
    return printTransactionToJSON(transaction).toJSONString();
  }

  private static String printReceipt(Protocol.ResourceReceipt receipt) {
    String result = "";
    result += "EnergyUsage: ";
    result += "\n";
    result += receipt.getEnergyUsage();
    result += "\n";
    result += "EnergyFee(SUN): ";
    result += "\n";
    result += receipt.getEnergyFee();
    result += "\n";
    result += "OriginEnergyUsage: ";
    result += "\n";
    result += receipt.getOriginEnergyUsage();
    result += "\n";
    result += "EnergyUsageTotal: ";
    result += "\n";
    result += receipt.getEnergyUsageTotal();
    result += "\n";
    result += "NetUsage: ";
    result += "\n";
    result += receipt.getNetUsage();
    result += "\n";
    result += "NetFee: ";
    result += "\n";
    result += receipt.getNetFee();
    result += "\n";
    return result;
  }
  
  public static String printLogList(List<Protocol.TransactionInfo.Log> logList) {
    StringBuilder result = new StringBuilder("");
    logList.forEach(log -> {
              result.append("address:\n");
              result.append(ByteArray.toHexString(log.getAddress().toByteArray()));
              result.append("\n");
              result.append("data:\n");
              result.append(ByteArray.toHexString(log.getData().toByteArray()));
              result.append("\n");
              result.append("TopicsList\n");
              StringBuilder topics = new StringBuilder("");

              log.getTopicsList().forEach(bytes -> {
                topics.append(ByteArray.toHexString(bytes.toByteArray()));
                topics.append("\n");
              });
              result.append(topics);
            }
    );

    return result.toString();
  }
  public static String printTransactionInfo(Protocol.TransactionInfo transactionInfo) {
    String result = "";
    result += "txid: ";
    result += "\n";
    result += ByteArray.toHexString(transactionInfo.getId().toByteArray());
    result += "\n";
    result += "fee: ";
    result += "\n";
    result += transactionInfo.getFee();
    result += "\n";
    result += "blockNumber: ";
    result += "\n";
    result += transactionInfo.getBlockNumber();
    result += "\n";
    result += "blockTimeStamp: ";
    result += "\n";
    result += transactionInfo.getBlockTimeStamp();
    result += "\n";
    result += "result: ";
    result += "\n";
    if (transactionInfo.getResult().equals(Protocol.TransactionInfo.code.SUCESS)) {
      result += "SUCCESS";
    } else {
      result += "FAILED";
    }
    result += "\n";
    result += "resMessage: ";
    result += "\n";
    result += ByteArray.toStr(transactionInfo.getResMessage().toByteArray());
    result += "\n";
    result += "contractResult: ";
    result += "\n";
    result += ByteArray.toHexString(transactionInfo.getContractResult(0).toByteArray());
    result += "\n";
    result += "contractAddress: ";
    result += "\n";
    result += Wallet.encode58Check(transactionInfo.getContractAddress().toByteArray());
    result += "\n";
    result += "logList: ";
    result += "\n";
    result += printLogList(transactionInfo.getLogList());
    result += "\n";
    result += "receipt: ";
    result += "\n";
    result += printReceipt(transactionInfo.getReceipt());
    result += "\n";
    return result;
  }

  public static String printTransactionsExt(List<TransactionExtention> transactionList) {
    String result = "\n";
    int i = 0;
    for (TransactionExtention transaction : transactionList) {
      result += "transaction " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printTransactionExtention(transaction);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printTransactionExtention(TransactionExtention transactionExtention) {
    String string = JsonFormat.printToString(transactionExtention);
    JSONObject jsonObject = JSONObject.parseObject(string);
    if (transactionExtention.getResult().getResult()) {
      jsonObject.put("transaction", printTransactionToJSON(transactionExtention.getTransaction()));
    }
    return jsonObject.toJSONString();
  }

  public static byte[] generateContractAddress(Transaction xlt, byte[] ownerAddress) {
    // get tx hash
    byte[] txRawDataHash = Sha256Hash.of(xlt.getRawData().toByteArray()).getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }

  public static JSONObject printTransactionToJSON(Transaction transaction) {
    JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction));
    JSONArray contracts = new JSONArray();
    transaction.getRawData().getContractList().stream().forEach(contract -> {
      try {
        JSONObject contractJson = null;
        Any contractParameter = contract.getParameter();
        switch (contract.getType()) {
          case AccountCreateContract:
            AccountCreateContract accountCreateContract = contractParameter
                .unpack(AccountCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(accountCreateContract));
            break;
          case TransferContract:
            TransferContract transferContract = contractParameter.unpack(TransferContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(transferContract));
            break;
          case TransferAssetContract:
            TransferAssetContract transferAssetContract = contractParameter
                .unpack(TransferAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(transferAssetContract));
            break;
          case VoteAssetContract:
            VoteAssetContract voteAssetContract = contractParameter.unpack(VoteAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(voteAssetContract));
            break;
          case VoteWitnessContract:
            VoteWitnessContract voteWitnessContract = contractParameter
                .unpack(VoteWitnessContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(voteWitnessContract));
            break;
          case WitnessCreateContract:
            WitnessCreateContract witnessCreateContract = contractParameter
                .unpack(WitnessCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessCreateContract));
            break;
          case AssetIssueContract:
            AssetIssueContract assetIssueContract = contractParameter
                .unpack(AssetIssueContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(assetIssueContract));
            break;
          case WitnessUpdateContract:
            WitnessUpdateContract witnessUpdateContract = contractParameter
                .unpack(WitnessUpdateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessUpdateContract));
            break;
          case ParticipateAssetIssueContract:
            ParticipateAssetIssueContract participateAssetIssueContract = contractParameter
                .unpack(ParticipateAssetIssueContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(participateAssetIssueContract));
            break;
          case AccountUpdateContract:
            AccountUpdateContract accountUpdateContract = contractParameter
                .unpack(AccountUpdateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(accountUpdateContract));
            break;
          case FreezeBalanceContract:
            FreezeBalanceContract freezeBalanceContract = contractParameter
                .unpack(FreezeBalanceContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(freezeBalanceContract));
            break;
          case UnfreezeBalanceContract:
            UnfreezeBalanceContract unfreezeBalanceContract = contractParameter
                .unpack(UnfreezeBalanceContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(unfreezeBalanceContract));
            break;
          case UnfreezeAssetContract:
            UnfreezeAssetContract unfreezeAssetContract = contractParameter
                .unpack(UnfreezeAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(unfreezeAssetContract));
            break;
          case WithdrawBalanceContract:
            WithdrawBalanceContract withdrawBalanceContract = contractParameter
                .unpack(WithdrawBalanceContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(withdrawBalanceContract));
            break;
          case UpdateAssetContract:
            UpdateAssetContract updateAssetContract = contractParameter
                .unpack(UpdateAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(updateAssetContract));
            break;
          case CreateSmartContract:
            CreateSmartContract deployContract = contractParameter
                .unpack(CreateSmartContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(deployContract));
            byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
            byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
            jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
            break;
          case TriggerSmartContract:
            TriggerSmartContract triggerSmartContract = contractParameter
                .unpack(TriggerSmartContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(triggerSmartContract));
            break;
          case ProposalCreateContract:
            ProposalCreateContract proposalCreateContract = contractParameter
                .unpack(ProposalCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(proposalCreateContract));
            break;
          case ProposalApproveContract:
            ProposalApproveContract proposalApproveContract = contractParameter
                .unpack(ProposalApproveContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(proposalApproveContract));
            break;
          case ProposalDeleteContract:
            ProposalDeleteContract proposalDeleteContract = contractParameter
                .unpack(ProposalDeleteContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(proposalDeleteContract));
            break;
          case ExchangeCreateContract:
            ExchangeCreateContract exchangeCreateContract = contractParameter
                .unpack(ExchangeCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(exchangeCreateContract));
            break;
          case ExchangeInjectContract:
            ExchangeInjectContract exchangeInjectContract = contractParameter
                .unpack(ExchangeInjectContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(exchangeInjectContract));
            break;
          case ExchangeWithdrawContract:
            ExchangeWithdrawContract exchangeWithdrawContract = contractParameter
                .unpack(ExchangeWithdrawContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(exchangeWithdrawContract));
            break;
          case ExchangeTransactionContract:
            ExchangeTransactionContract exchangeTransactionContract = contractParameter
                .unpack(ExchangeTransactionContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(exchangeTransactionContract));
            break;
          // todo add other contract
          default:
        }
        JSONObject parameter = new JSONObject();
        parameter.put("value", contractJson);
        parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
        JSONObject jsonContract = new JSONObject();
        jsonContract.put("parameter", parameter);
        jsonContract.put("type", contract.getType());
        contracts.add(jsonContract);
      } catch (InvalidProtocolBufferException e) {
        logger.debug("InvalidProtocolBufferException: {}", e.getMessage());
      }
    });

    JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    String txID = ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    jsonTransaction.put("txID", txID);
    return jsonTransaction;
  }

  public static Transaction packTransaction(String strTransaction) {
    JSONObject jsonTransaction = JSONObject.parseObject(strTransaction);
    JSONObject rawData = jsonTransaction.getJSONObject("raw_data");
    JSONArray contracts = new JSONArray();
    JSONArray rawContractArray = rawData.getJSONArray("contract");

    for (int i = 0; i < rawContractArray.size(); i++) {
      try {
        JSONObject contract = rawContractArray.getJSONObject(i);
        JSONObject parameter = contract.getJSONObject("parameter");
        String contractType = contract.getString("type");
        Any any = null;
        switch (contractType) {
          case "AccountCreateContract":
            AccountCreateContract.Builder accountCreateContractBuilder = AccountCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                accountCreateContractBuilder);
            any = Any.pack(accountCreateContractBuilder.build());
            break;
          case "TransferContract":
            TransferContract.Builder transferContractBuilder = TransferContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), transferContractBuilder);
            any = Any.pack(transferContractBuilder.build());
            break;
          case "TransferAssetContract":
            TransferAssetContract.Builder transferAssetContractBuilder = TransferAssetContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                transferAssetContractBuilder);
            any = Any.pack(transferAssetContractBuilder.build());
            break;
          case "VoteAssetContract":
            VoteAssetContract.Builder voteAssetContractBuilder = VoteAssetContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), voteAssetContractBuilder);
            any = Any.pack(voteAssetContractBuilder.build());
            break;
          case "VoteWitnessContract":
            VoteWitnessContract.Builder voteWitnessContractBuilder = VoteWitnessContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), voteWitnessContractBuilder);
            any = Any.pack(voteWitnessContractBuilder.build());
            break;
          case "WitnessCreateContract":
            WitnessCreateContract.Builder witnessCreateContractBuilder = WitnessCreateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                witnessCreateContractBuilder);
            any = Any.pack(witnessCreateContractBuilder.build());
            break;
          case "AssetIssueContract":
            AssetIssueContract.Builder assetIssueContractBuilder = AssetIssueContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), assetIssueContractBuilder);
            any = Any.pack(assetIssueContractBuilder.build());
            break;
          case "WitnessUpdateContract":
            WitnessUpdateContract.Builder witnessUpdateContractBuilder = WitnessUpdateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                witnessUpdateContractBuilder);
            any = Any.pack(witnessUpdateContractBuilder.build());
            break;
          case "ParticipateAssetIssueContract":
            ParticipateAssetIssueContract.Builder participateAssetIssueContractBuilder =
                ParticipateAssetIssueContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                participateAssetIssueContractBuilder);
            any = Any.pack(participateAssetIssueContractBuilder.build());
            break;
          case "AccountUpdateContract":
            AccountUpdateContract.Builder accountUpdateContractBuilder = AccountUpdateContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                accountUpdateContractBuilder);
            any = Any.pack(accountUpdateContractBuilder.build());
            break;
          case "FreezeBalanceContract":
            FreezeBalanceContract.Builder freezeBalanceContractBuilder = FreezeBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                freezeBalanceContractBuilder);
            any = Any.pack(freezeBalanceContractBuilder.build());
            break;
          case "UnfreezeBalanceContract":
            UnfreezeBalanceContract.Builder unfreezeBalanceContractBuilder = UnfreezeBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                unfreezeBalanceContractBuilder);
            any = Any.pack(unfreezeBalanceContractBuilder.build());
            break;
          case "UnfreezeAssetContract":
            UnfreezeAssetContract.Builder unfreezeAssetContractBuilder = UnfreezeAssetContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                unfreezeAssetContractBuilder);
            any = Any.pack(unfreezeAssetContractBuilder.build());
            break;
          case "WithdrawBalanceContract":
            WithdrawBalanceContract.Builder withdrawBalanceContractBuilder = WithdrawBalanceContract
                .newBuilder();
            JsonFormat.merge(parameter.getJSONObject("value").toJSONString(),
                withdrawBalanceContractBuilder);
            any = Any.pack(withdrawBalanceContractBuilder.build());
            break;
          case "UpdateAssetContract":
            UpdateAssetContract.Builder updateAssetContractBuilder = UpdateAssetContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), updateAssetContractBuilder);
            any = Any.pack(updateAssetContractBuilder.build());
            break;
          case "SmartContract":
            SmartContract.Builder smartContractBuilder = SmartContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(), smartContractBuilder);
            any = Any.pack(smartContractBuilder.build());
            break;
          case "TriggerSmartContract":
            TriggerSmartContract.Builder triggerSmartContractBuilder = TriggerSmartContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    triggerSmartContractBuilder);
            any = Any.pack(triggerSmartContractBuilder.build());
            break;
          case "CreateSmartContract":
            CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    createSmartContractBuilder);
            any = Any.pack(createSmartContractBuilder.build());
            break;
          case "ExchangeCreateContract":
            ExchangeCreateContract.Builder exchangeCreateContractBuilder = ExchangeCreateContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    exchangeCreateContractBuilder);
            any = Any.pack(exchangeCreateContractBuilder.build());
            break;
          case "ExchangeInjectContract":
            ExchangeInjectContract.Builder exchangeInjectContractBuilder = ExchangeInjectContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    exchangeInjectContractBuilder);
            any = Any.pack(exchangeInjectContractBuilder.build());
            break;
          case "ExchangeTransactionContract":
            ExchangeTransactionContract.Builder exchangeTransactionContractBuilder =
                ExchangeTransactionContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    exchangeTransactionContractBuilder);
            any = Any.pack(exchangeTransactionContractBuilder.build());
            break;
          case "ExchangeWithdrawContract":
            ExchangeWithdrawContract.Builder exchangeWithdrawContractBuilder =
                ExchangeWithdrawContract.newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    exchangeWithdrawContractBuilder);
            any = Any.pack(exchangeWithdrawContractBuilder.build());
            break;
          case "ProposalCreateContract":
            ProposalCreateContract.Builder ProposalCreateContractBuilder = ProposalCreateContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    ProposalCreateContractBuilder);
            any = Any.pack(ProposalCreateContractBuilder.build());
            break;
          case "ProposalApproveContract":
            ProposalApproveContract.Builder ProposalApproveContractBuilder = ProposalApproveContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    ProposalApproveContractBuilder);
            any = Any.pack(ProposalApproveContractBuilder.build());
            break;
          case "ProposalDeleteContract":
            ProposalDeleteContract.Builder ProposalDeleteContractBuilder = ProposalDeleteContract
                .newBuilder();
            JsonFormat
                .merge(parameter.getJSONObject("value").toJSONString(),
                    ProposalDeleteContractBuilder);
            any = Any.pack(ProposalDeleteContractBuilder.build());
            break;
          // todo add other contract
          default:
        }
        if (any != null) {
          String value = ByteArray.toHexString(any.getValue().toByteArray());
          parameter.put("value", value);
          contract.put("parameter", parameter);
          contracts.add(contract);
        }
      } catch (ParseException e) {
        logger.debug("ParseException: {}", e.getMessage());
      }
    }
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    Transaction.Builder transactionBuilder = Transaction.newBuilder();
    try {
      JsonFormat.merge(jsonTransaction.toJSONString(), transactionBuilder);
      return transactionBuilder.build();
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
      return null;
    }
  }
}
