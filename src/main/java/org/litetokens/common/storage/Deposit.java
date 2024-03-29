package org.litetokens.common.storage;

import org.litetokens.common.runtime.vm.DataWord;
import org.litetokens.common.runtime.vm.program.Storage;
import org.litetokens.core.capsule.AccountCapsule;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.capsule.BytesCapsule;
import org.litetokens.core.capsule.ContractCapsule;
import org.litetokens.core.capsule.ProposalCapsule;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.capsule.VotesCapsule;
import org.litetokens.core.capsule.WitnessCapsule;
import org.litetokens.core.db.Manager;
import org.litetokens.protos.Protocol;

public interface Deposit {

  Manager getDbManager();

  AccountCapsule createAccount(byte[] address, Protocol.AccountType type);

  AccountCapsule createAccount(byte[] address, String accountName, Protocol.AccountType type);

  AccountCapsule getAccount(byte[] address);

  WitnessCapsule getWitness(byte[] address);

  VotesCapsule getVotesCapsule(byte[] address);

  ProposalCapsule getProposalCapsule(byte[] id);

  BytesCapsule getDynamic(byte[] bytesKey);

  void deleteContract(byte[] address);

  void createContract(byte[] address, ContractCapsule contractCapsule);

  ContractCapsule getContract(byte[] address);

  void saveCode(byte[] codeHash, byte[] code);

  byte[] getCode(byte[] codeHash);

  //byte[] getCodeHash(byte[] address);

  void putStorageValue(byte[] address, DataWord key, DataWord value);

  DataWord getStorageValue(byte[] address, DataWord key);

  Storage getStorage(byte[] address);

  long getBalance(byte[] address);

  long addBalance(byte[] address, long value);


  Deposit newDepositChild();

  void setParent(Deposit deposit);

  void flush();

  void commit();

  void putAccount(Key key, Value value);

  void putTransaction(Key key, Value value);

  void putBlock(Key key, Value value);

  void putWitness(Key key, Value value);

  void putCode(Key key, Value value);

  void putContract(Key key, Value value);

  void putStorage(Key key, Storage cache);

  void putVotes(Key key, Value value);

  void putProposal(Key key, Value value);

  void putDynamicProperties(Key key, Value value);

  void putAccountValue(byte[] address, AccountCapsule accountCapsule);

  void putVoteValue(byte[] address, VotesCapsule votesCapsule);

  void putProposalValue(byte[] address, ProposalCapsule proposalCapsule);

  void putDynamicPropertiesWithLatestProposalNum(long num);

  long getLatestProposalNum();

  long getWitnessAllowanceFrozenTime();

  long getMaintenanceTimeInterval();

  long getNextMaintenanceTime();

  TransactionCapsule getTransaction(byte[] xltHash);

  BlockCapsule getBlock(byte[] blockHash);

}
