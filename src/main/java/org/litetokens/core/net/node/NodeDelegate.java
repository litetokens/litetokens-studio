package org.litetokens.core.net.node;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.litetokens.common.overlay.message.Message;
import org.litetokens.common.utils.Sha256Hash;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.capsule.BlockCapsule.BlockId;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.exception.BadBlockException;
import org.litetokens.core.exception.BadTransactionException;
import org.litetokens.core.exception.NonCommonBlockException;
import org.litetokens.core.exception.StoreException;
import org.litetokens.core.exception.LitetokensException;
import org.litetokens.core.exception.UnLinkedBlockException;
import org.litetokens.core.net.message.MessageTypes;

public interface NodeDelegate {

  LinkedList<Sha256Hash> handleBlock(BlockCapsule block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException, NonCommonBlockException;

  boolean handleTransaction(TransactionCapsule xlt) throws BadTransactionException;

  LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary) throws StoreException;

  Deque<BlockId> getBlockChainSummary(BlockId beginBLockId, Deque<BlockId> blockIds)
      throws LitetokensException;

  Message getData(Sha256Hash msgId, MessageTypes type) throws StoreException;

  void syncToCli(long unSyncNum);

  long getBlockTime(BlockId id);

  BlockId getHeadBlockId();

  BlockId getSolidBlockId();

  boolean contain(Sha256Hash hash, MessageTypes type);

  boolean containBlock(BlockId id);

  long getHeadBlockTimeStamp();

  boolean containBlockInMainChain(BlockId id);

  BlockCapsule getGenesisBlock();

  boolean canChainRevoke(long num);
}
