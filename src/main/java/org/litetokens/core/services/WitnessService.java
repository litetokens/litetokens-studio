package org.litetokens.core.services;

import static org.litetokens.core.witness.BlockProductionCondition.NOT_MY_TURN;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.litetokens.common.application.Application;
import org.litetokens.common.application.Service;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.backup.BackupManager;
import org.litetokens.common.backup.BackupManager.BackupStatusEnum;
import org.litetokens.common.backup.BackupServer;
import org.litetokens.common.crypto.ECKey;
import org.litetokens.common.utils.ByteArray;
import org.litetokens.common.utils.StringUtil;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.capsule.WitnessCapsule;
import org.litetokens.core.config.Parameter.ChainConstant;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.exception.AccountResourceInsufficientException;
import org.litetokens.core.exception.ContractExeException;
import org.litetokens.core.exception.ContractValidateException;
import org.litetokens.core.exception.LitetokensException;
import org.litetokens.core.exception.UnLinkedBlockException;
import org.litetokens.core.exception.ValidateScheduleException;
import org.litetokens.core.exception.ValidateSignatureException;
import org.litetokens.core.net.message.BlockMessage;
import org.litetokens.core.witness.BlockProductionCondition;
import org.litetokens.core.witness.WitnessController;

@Slf4j
public class WitnessService implements Service {

  private static final int MIN_PARTICIPATION_RATE = Args.getInstance()
      .getMinParticipationRate(); // MIN_PARTICIPATION_RATE * 1%
  private static final int PRODUCE_TIME_OUT = 500; // ms
  private Application litetokensApp;
  @Getter
  protected Map<ByteString, WitnessCapsule> localWitnessStateMap = Maps
      .newHashMap(); //  <address,WitnessCapsule>
  private Thread generateThread;

  private volatile boolean isRunning = false;
  private Map<ByteString, byte[]> privateKeyMap = Maps.newHashMap();
  private volatile boolean needSyncCheck = Args.getInstance().isNeedSyncCheck();

  private WitnessController controller;

  private LitetokensApplicationContext context;

  private BackupManager backupManager;

  private BackupServer backupServer;

  /**
   * Construction method.
   */
  public WitnessService(Application litetokensApp, LitetokensApplicationContext context) {
    this.litetokensApp = litetokensApp;
    this.context = context;
    backupManager = context.getBean(BackupManager.class);
    backupServer = context.getBean(BackupServer.class);
    generateThread = new Thread(scheduleProductionLoop);
    controller = litetokensApp.getDbManager().getWitnessController();
    new Thread(() -> {
      while (needSyncCheck) {
        try {
          Thread.sleep(100);
        } catch (Exception e) {
        }
      }
      backupServer.initServer();
    }).start();
  }

  /**
   * Cycle thread to generate blocks
   */
  private Runnable scheduleProductionLoop =
      () -> {
        if (localWitnessStateMap == null || localWitnessStateMap.keySet().isEmpty()) {
          logger.error("LocalWitnesses is null");
          return;
        }

        while (isRunning) {
          try {
            if (this.needSyncCheck) {
              Thread.sleep(500L);
            } else {
              DateTime time = DateTime.now();
              long timeToNextSecond = ChainConstant.BLOCK_PRODUCED_INTERVAL
                  - (time.getSecondOfMinute() * 1000 + time.getMillisOfSecond())
                  % ChainConstant.BLOCK_PRODUCED_INTERVAL;
              if (timeToNextSecond < 50L) {
                timeToNextSecond = timeToNextSecond + ChainConstant.BLOCK_PRODUCED_INTERVAL;
              }
              DateTime nextTime = time.plus(timeToNextSecond);
              logger.debug(
                  "ProductionLoop sleep : " + timeToNextSecond + " ms,next time:" + nextTime);
              Thread.sleep(timeToNextSecond);
            }
            this.blockProductionLoop();
          } catch (InterruptedException ex) {
            logger.info("ProductionLoop interrupted");
          } catch (Exception ex) {
            logger.error("unknown exception happened in witness loop", ex);
          } catch (Throwable throwable) {
            logger.error("unknown throwable happened in witness loop", throwable);
          }
        }
      };

  /**
   * Loop to generate blocks
   */
  private void blockProductionLoop() throws InterruptedException {
    BlockProductionCondition result = this.tryProduceBlock();

    if (result == null) {
      logger.warn("Result is null");
      return;
    }

    if (result.ordinal() <= NOT_MY_TURN.ordinal()) {
      logger.debug(result.toString());
    } else {
      logger.info(result.toString());
    }
  }

  /**
   * Generate and broadcast blocks
   */
  private BlockProductionCondition tryProduceBlock() throws InterruptedException {
    logger.info("Try Produce Block");
    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
      return BlockProductionCondition.BACKUP_STATUS_IS_NOT_MASTER;
    }
    long now = DateTime.now().getMillis() + 50L;
    if (this.needSyncCheck) {
      long nexSlotTime = controller.getSlotTime(1);
      if (nexSlotTime > now) { // check sync during first loop
        needSyncCheck = false;
        Thread.sleep(nexSlotTime - now); //Processing Time Drift later
        now = DateTime.now().getMillis();
      } else {
        logger.debug("Not sync ,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
            new DateTime(now),
            new DateTime(this.litetokensApp.getDbManager().getDynamicPropertiesStore()
                .getLatestBlockHeaderTimestamp()),
            this.litetokensApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
            this.litetokensApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderHash());
        return BlockProductionCondition.NOT_SYNCED;
      }
    }

    final int participation = this.controller.calculateParticipationRate();
    if (participation < MIN_PARTICIPATION_RATE) {
      logger.warn(
          "Participation[" + participation + "] <  MIN_PARTICIPATION_RATE[" + MIN_PARTICIPATION_RATE
              + "]");

      if (logger.isDebugEnabled()) {
        this.controller.dumpParticipationLog();
      }

      return BlockProductionCondition.LOW_PARTICIPATION;
    }

    long slot = controller.getSlotAtTime(now);
    logger.debug("Slot:" + slot);

    if (slot == 0) {
      logger.info("Not time yet,now:{},headBlockTime:{},headBlockNumber:{},headBlockId:{}",
          new DateTime(now),
          new DateTime(
              this.litetokensApp.getDbManager().getDynamicPropertiesStore()
                  .getLatestBlockHeaderTimestamp()),
          this.litetokensApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
          this.litetokensApp.getDbManager().getDynamicPropertiesStore().getLatestBlockHeaderHash());
      return BlockProductionCondition.NOT_TIME_YET;
    }

    if (now < controller.getManager().getDynamicPropertiesStore().getLatestBlockHeaderTimestamp()) {
      logger.warn("have a timestamp:{} less than or equal to the previous block:{}",
          new DateTime(now), new DateTime(
              this.litetokensApp.getDbManager().getDynamicPropertiesStore()
                  .getLatestBlockHeaderTimestamp()));
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    }

    if (!controller.activeWitnessesContain(this.getLocalWitnessStateMap().keySet())) {
      logger.info("Unelected. Elected Witnesses: {}",
          StringUtil.getAddressStringList(controller.getActiveWitnesses()));
      return BlockProductionCondition.UNELECTED;
    }

    final ByteString scheduledWitness = controller.getScheduledWitness(slot);

    if (!this.getLocalWitnessStateMap().containsKey(scheduledWitness)) {
      logger.info("It's not my turn, ScheduledWitness[{}],slot[{}],abSlot[{}],",
          ByteArray.toHexString(scheduledWitness.toByteArray()), slot,
          controller.getAbSlotAtTime(now));
      return NOT_MY_TURN;
    }

    long scheduledTime = controller.getSlotTime(slot);

    if (scheduledTime - now > PRODUCE_TIME_OUT) {
      return BlockProductionCondition.LAG;
    }

    if (!privateKeyMap.containsKey(scheduledWitness)) {
      return BlockProductionCondition.NO_PRIVATE_KEY;
    }

    try {

      controller.getManager().lastHeadBlockIsMaintenance();

      controller.setGeneratingBlock(true);
      BlockCapsule block;
      synchronized (litetokensApp.getDbManager()) {
        block = generateBlock(scheduledTime, scheduledWitness,
            controller.lastHeadBlockIsMaintenance());

        if (block == null) {
          logger.warn("exception when generate block");
          return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
        }

        int blockProducedTimeOut = Args.getInstance().getBlockProducedTimeOut();

        if (DateTime.now().getMillis() - now
            > ChainConstant.BLOCK_PRODUCED_INTERVAL * blockProducedTimeOut / 100) {
          logger.warn("Task timeout ( > {}ms)，startTime:{},endTime:{}",
              ChainConstant.BLOCK_PRODUCED_INTERVAL * blockProducedTimeOut / 100,
              new DateTime(now), DateTime.now());
          litetokensApp.getDbManager().eraseBlock();
          return BlockProductionCondition.TIME_OUT;
        }
      }

      logger.info(
          "Produce block successfully, blockNumber:{}, abSlot[{}], blockId:{}, transactionSize:{}, blockTime:{}, parentBlockId:{}",
          block.getNum(), controller.getAbSlotAtTime(now), block.getBlockId(),
          block.getTransactions().size(),
          new DateTime(block.getTimeStamp()),
          block.getParentHash());
      broadcastBlock(block);

      return BlockProductionCondition.PRODUCED;
    } catch (LitetokensException e) {
      logger.error(e.getMessage(), e);
      return BlockProductionCondition.EXCEPTION_PRODUCING_BLOCK;
    } finally {
      controller.setGeneratingBlock(false);
    }
  }

  private void broadcastBlock(BlockCapsule block) {
    try {
      litetokensApp.getP2pNode().broadcast(new BlockMessage(block.getData()));
    } catch (Exception ex) {
      throw new RuntimeException("BroadcastBlock error");
    }
  }

  private BlockCapsule generateBlock(long when, ByteString witnessAddress,
      Boolean lastHeadBlockIsMaintenance)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      UnLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException {
    return litetokensApp.getDbManager().generateBlock(this.localWitnessStateMap.get(witnessAddress), when,
        this.privateKeyMap.get(witnessAddress), lastHeadBlockIsMaintenance);
  }

  /**
   * Initialize the local witnesses
   */
  @Override
  public void init() {
    Args.getInstance().getLocalWitnesses().getPrivateKeys().forEach(key -> {
      byte[] privateKey = ByteArray.fromHexString(key);
      final ECKey ecKey = ECKey.fromPrivate(privateKey);
      byte[] address = ecKey.getAddress();
      WitnessCapsule witnessCapsule = this.litetokensApp.getDbManager().getWitnessStore()
          .get(address);
      // need handle init witness
      if (null == witnessCapsule) {
        logger.warn("WitnessCapsule[" + address + "] is not in witnessStore");
        witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
      }

      this.privateKeyMap.put(witnessCapsule.getAddress(), privateKey);
      this.localWitnessStateMap.put(witnessCapsule.getAddress(), witnessCapsule);
    });

  }

  @Override
  public void init(Args args) {
    //this.privateKey = args.getPrivateKeys();
    init();
  }

  @Override
  public void start() {
    isRunning = true;
    generateThread.start();

  }

  @Override
  public void stop() {
    isRunning = false;
    generateThread.interrupt();
  }
}
