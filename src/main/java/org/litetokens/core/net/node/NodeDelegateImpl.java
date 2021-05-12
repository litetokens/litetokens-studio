package org.litetokens.core.net.node;

import static org.litetokens.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;
import static org.litetokens.core.config.Parameter.ChainConstant.BLOCK_SIZE;

import com.google.common.primitives.Longs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.litetokens.common.overlay.message.Message;
import org.litetokens.common.utils.Sha256Hash;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.capsule.BlockCapsule.BlockId;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.config.Parameter.NodeConstant;
import org.litetokens.core.db.Manager;
import org.litetokens.core.exception.AccountResourceInsufficientException;
import org.litetokens.core.exception.BadBlockException;
import org.litetokens.core.exception.BadItemException;
import org.litetokens.core.exception.BadNumberBlockException;
import org.litetokens.core.exception.BadTransactionException;
import org.litetokens.core.exception.ContractExeException;
import org.litetokens.core.exception.ContractSizeNotEqualToOneException;
import org.litetokens.core.exception.ContractValidateException;
import org.litetokens.core.exception.DupTransactionException;
import org.litetokens.core.exception.ItemNotFoundException;
import org.litetokens.core.exception.NonCommonBlockException;
import org.litetokens.core.exception.ReceiptCheckErrException;
import org.litetokens.core.exception.StoreException;
import org.litetokens.core.exception.TaposException;
import org.litetokens.core.exception.TooBigTransactionException;
import org.litetokens.core.exception.TooBigTransactionResultException;
import org.litetokens.core.exception.TransactionExpirationException;
import org.litetokens.core.exception.LitetokensException;
import org.litetokens.core.exception.UnLinkedBlockException;
import org.litetokens.core.exception.VMIllegalException;
import org.litetokens.core.exception.ValidateScheduleException;
import org.litetokens.core.exception.ValidateSignatureException;
import org.litetokens.core.net.message.BlockMessage;
import org.litetokens.core.net.message.MessageTypes;
import org.litetokens.core.net.message.TransactionMessage;

@Slf4j
public class NodeDelegateImpl implements NodeDelegate {

  private Manager dbManager;

  public NodeDelegateImpl(Manager dbManager) {
    this.dbManager = dbManager;
  }

  @Override
  public synchronized LinkedList<Sha256Hash> handleBlock(BlockCapsule block, boolean syncMode)
      throws BadBlockException, UnLinkedBlockException, InterruptedException, NonCommonBlockException {

    if (block.getInstance().getSerializedSize() > BLOCK_SIZE + 100) {
      throw new BadBlockException("block size over limit");
    }

    long gap = block.getTimeStamp() - System.currentTimeMillis();
    if (gap >= BLOCK_PRODUCED_INTERVAL) {
      throw new BadBlockException("block time error");
    }
    try {
      dbManager.preValidateTransactionSign(block);
      dbManager.pushBlock(block);
      if (!syncMode) {
        List<TransactionCapsule> xlt = null;
        xlt = block.getTransactions();
        return xlt.stream()
            .map(TransactionCapsule::getTransactionId)
            .collect(Collectors.toCollection(LinkedList::new));
      } else {
        return null;
      }

    } catch (AccountResourceInsufficientException e) {
      throw new BadBlockException("AccountResourceInsufficientException," + e.getMessage());
    } catch (ValidateScheduleException e) {
      throw new BadBlockException("validate schedule exception," + e.getMessage());
    } catch (ValidateSignatureException e) {
      throw new BadBlockException("validate signature exception," + e.getMessage());
    } catch (ContractValidateException e) {
      throw new BadBlockException("ContractValidate exception," + e.getMessage());
    } catch (ContractExeException e) {
      throw new BadBlockException("Contract Execute exception," + e.getMessage());
    } catch (TaposException e) {
      throw new BadBlockException("tapos exception," + e.getMessage());
    } catch (DupTransactionException e) {
      throw new BadBlockException("DupTransaction exception," + e.getMessage());
    } catch (TooBigTransactionException e) {
      throw new BadBlockException("TooBigTransaction exception," + e.getMessage());
    } catch (TooBigTransactionResultException e) {
      throw new BadBlockException("TooBigTransaction exception," + e.getMessage());
    } catch (TransactionExpirationException e) {
      throw new BadBlockException("Expiration exception," + e.getMessage());
    } catch (BadNumberBlockException e) {
      throw new BadBlockException("bad number exception," + e.getMessage());
    } catch (ReceiptCheckErrException e) {
      throw new BadBlockException("TransactionTrace Exception," + e.getMessage());
    } catch (VMIllegalException e) {
      throw new BadBlockException(e.getMessage());
    }

  }


  @Override
  public boolean handleTransaction(TransactionCapsule xlt) throws BadTransactionException {
    if (dbManager.getDynamicPropertiesStore().supportVM()) {
      xlt.resetResult();
    }
    logger.debug("handle transaction");
    if (dbManager.getTransactionIdCache().getIfPresent(xlt.getTransactionId()) != null) {
      logger.warn("This transaction has been processed");
      return false;
    } else {
      dbManager.getTransactionIdCache().put(xlt.getTransactionId(), true);
    }
    try {
      dbManager.pushTransaction(xlt);
    } catch (ContractSizeNotEqualToOneException e) {
      logger.info("Contract validate failed" + e.getMessage());
      throw new BadTransactionException();
    } catch (ContractValidateException e) {
      logger.info("Contract validate failed" + e.getMessage());
      //throw new BadTransactionException();
      return false;
    } catch (ContractExeException e) {
      logger.info("Contract execute failed" + e.getMessage());
      //throw new BadTransactionException();
      return false;
    } catch (ValidateSignatureException e) {
      logger.info("ValidateSignatureException" + e.getMessage());
      throw new BadTransactionException();
    } catch (AccountResourceInsufficientException e) {
      logger.info("AccountResourceInsufficientException" + e.getMessage());
      return false;
    } catch (DupTransactionException e) {
      logger.info("Dup trans" + e.getMessage());
      return false;
    } catch (TaposException e) {
      logger.info("Tapos error" + e.getMessage());
      return false;
    } catch (TooBigTransactionException e) {
      logger.info("Too big transaction" + e.getMessage());
      return false;
    } catch (TransactionExpirationException e) {
      logger.info("Expiration transaction" + e.getMessage());
      return false;
    } catch (ReceiptCheckErrException e) {
      logger.info("ReceiptCheckErrException Exception" + e.getMessage());
      return false;
    } catch (VMIllegalException e) {
      logger.warn(e.getMessage());
      throw new BadTransactionException();
    } catch (TooBigTransactionResultException e) {
      logger.info("Too big transactionresult" + e.getMessage());
      return false;
    }

    return true;
  }

  @Override
  public LinkedList<BlockId> getLostBlockIds(List<BlockId> blockChainSummary)
      throws StoreException {

    if (dbManager.getHeadBlockNum() == 0) {
      return new LinkedList<>();
    }

    BlockId unForkedBlockId;

    if (blockChainSummary.isEmpty() ||
        (blockChainSummary.size() == 1
            && blockChainSummary.get(0).equals(dbManager.getGenesisBlockId()))) {
      unForkedBlockId = dbManager.getGenesisBlockId();
    } else if (blockChainSummary.size() == 1
        && blockChainSummary.get(0).getNum() == 0) {
      return new LinkedList(Arrays.asList(dbManager.getGenesisBlockId()));
    } else {

      Collections.reverse(blockChainSummary);
      unForkedBlockId = blockChainSummary.stream()
          .filter(blockId -> containBlockInMainChain(blockId))
          .findFirst().orElse(null);
      if (unForkedBlockId == null) {
        return new LinkedList<>();
      }
    }

    long unForkedBlockIdNum = unForkedBlockId.getNum();
    long len = Longs
        .min(dbManager.getHeadBlockNum(), unForkedBlockIdNum + NodeConstant.SYNC_FETCH_BATCH_NUM);

    LinkedList<BlockId> blockIds = new LinkedList<>();
    for (long i = unForkedBlockIdNum; i <= len; i++) {
      BlockId id = dbManager.getBlockIdByNum(i);
      blockIds.add(id);
    }
    return blockIds;
  }

  @Override
  public Deque<BlockId> getBlockChainSummary(BlockId beginBlockId, Deque<BlockId> blockIdsToFetch)
      throws LitetokensException {

    Deque<BlockId> retSummary = new LinkedList<>();
    List<BlockId> blockIds = new ArrayList<>(blockIdsToFetch);
    long highBlkNum;
    long highNoForkBlkNum;
    long syncBeginNumber = dbManager.getSyncBeginNumber();
    long lowBlkNum = syncBeginNumber < 0 ? 0 : syncBeginNumber;

    LinkedList<BlockId> forkList = new LinkedList<>();

    if (!beginBlockId.equals(getGenesisBlock().getBlockId())) {
      if (containBlockInMainChain(beginBlockId)) {
        highBlkNum = beginBlockId.getNum();
        if (highBlkNum == 0) {
          throw new LitetokensException(
              "This block don't equal my genesis block hash, but it is in my DB, the block id is :"
                  + beginBlockId.getString());
        }
        highNoForkBlkNum = highBlkNum;
        if (beginBlockId.getNum() < lowBlkNum) {
          lowBlkNum = beginBlockId.getNum();
        }
      } else {
        forkList = dbManager.getBlockChainHashesOnFork(beginBlockId);
        if (forkList.isEmpty()) {
          throw new UnLinkedBlockException(
              "We want to find forkList of this block: " + beginBlockId.getString()
                  + " ,but in KhasoDB we can not find it, It maybe a very old beginBlockId, we are sync once,"
                  + " we switch and pop it after that time. ");
        }
        highNoForkBlkNum = forkList.peekLast().getNum();
        forkList.pollLast();
        Collections.reverse(forkList);
        highBlkNum = highNoForkBlkNum + forkList.size();
        if (highNoForkBlkNum < lowBlkNum) {
          throw new UnLinkedBlockException(
              "It is a too old block that we take it as a forked block long long ago"
                  + "\n lowBlkNum:" + lowBlkNum
                  + "\n highNoForkBlkNum" + highNoForkBlkNum);
        }
      }
    } else {
      highBlkNum = dbManager.getHeadBlockNum();
      highNoForkBlkNum = highBlkNum;

    }

    if (!blockIds.isEmpty() && highBlkNum != blockIds.get(0).getNum() - 1) {
      logger.error("Check ERROR: highBlkNum:" + highBlkNum + ",blockIdToSyncFirstNum is "
          + blockIds.get(0).getNum() + ",blockIdToSyncEnd is " + blockIds.get(blockIds.size() - 1)
          .getNum());
    }

    long realHighBlkNum = highBlkNum + blockIds.size();
    do {
      if (lowBlkNum <= highNoForkBlkNum) {
        retSummary.offer(dbManager.getBlockIdByNum(lowBlkNum));
      } else if (lowBlkNum <= highBlkNum) {
        retSummary.offer(forkList.get((int) (lowBlkNum - highNoForkBlkNum - 1)));
      } else {
        retSummary.offer(blockIds.get((int) (lowBlkNum - highBlkNum - 1)));
      }
      lowBlkNum += (realHighBlkNum - lowBlkNum + 2) / 2;
    } while (lowBlkNum <= realHighBlkNum);

    return retSummary;
  }

  @Override
  public Message getData(Sha256Hash hash, MessageTypes type)
      throws StoreException {
    switch (type) {
      case BLOCK:
        return new BlockMessage(dbManager.getBlockById(hash));
      case XLT:
        TransactionCapsule tx = dbManager.getTransactionStore().get(hash.getBytes());
        if (tx != null) {
          return new TransactionMessage(tx.getData());
        }
        throw new ItemNotFoundException("transaction is not found");
      default:
        throw new BadItemException("message type not block or xlt.");
    }
  }

  @Override
  public void syncToCli(long unSyncNum) {
    logger.info("There are " + unSyncNum + " blocks we need to sync.");
    if (unSyncNum == 0) {
      logger.info("Sync Block Completed !!!");
    }
    dbManager.setSyncMode(unSyncNum == 0);
  }

  @Override
  public long getBlockTime(BlockId id) {
    try {
      return dbManager.getBlockById(id).getTimeStamp();
    } catch (BadItemException e) {
      return dbManager.getGenesisBlock().getTimeStamp();
    } catch (ItemNotFoundException e) {
      return dbManager.getGenesisBlock().getTimeStamp();
    }
  }

  @Override
  public BlockId getHeadBlockId() {
    return dbManager.getHeadBlockId();
  }

  @Override
  public BlockId getSolidBlockId() {
    return dbManager.getSolidBlockId();
  }

  @Override
  public long getHeadBlockTimeStamp() {
    return dbManager.getHeadBlockTimeStamp();
  }

  @Override
  public boolean containBlock(BlockId id) {
    return dbManager.containBlock(id);
  }

  @Override
  public boolean containBlockInMainChain(BlockId id) {
    return dbManager.containBlockInMainChain(id);
  }

  @Override
  public boolean contain(Sha256Hash hash, MessageTypes type) {
    if (type.equals(MessageTypes.BLOCK)) {
      return dbManager.containBlock(hash);
    } else if (type.equals(MessageTypes.XLT)) {
      return dbManager.getTransactionStore().has(hash.getBytes());
    }
    return false;
  }

  @Override
  public BlockCapsule getGenesisBlock() {
    return dbManager.getGenesisBlock();
  }

  @Override
  public boolean canChainRevoke(long num) {
    return num >= dbManager.getSyncBeginNumber();
  }
}
