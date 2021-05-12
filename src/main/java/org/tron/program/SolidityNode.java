package org.litetokens.program;

import ch.qos.logback.classic.Level;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.litetokens.common.application.Application;
import org.litetokens.common.application.ApplicationFactory;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.overlay.client.DatabaseGrpcClient;
import org.litetokens.common.overlay.discover.DiscoverServer;
import org.litetokens.common.overlay.discover.node.NodeManager;
import org.litetokens.common.overlay.server.ChannelManager;
import org.litetokens.core.Constant;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.capsule.TransactionInfoCapsule;
import org.litetokens.core.config.DefaultConfig;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.db.Manager;
import org.litetokens.core.exception.AccountResourceInsufficientException;
import org.litetokens.core.exception.BadBlockException;
import org.litetokens.core.exception.BadItemException;
import org.litetokens.core.exception.ContractExeException;
import org.litetokens.core.exception.ContractValidateException;
import org.litetokens.core.exception.DupTransactionException;
import org.litetokens.core.exception.ReceiptCheckErrException;
import org.litetokens.core.exception.TaposException;
import org.litetokens.core.exception.TooBigTransactionException;
import org.litetokens.core.exception.TooBigTransactionResultException;
import org.litetokens.core.exception.TransactionExpirationException;
import org.litetokens.core.exception.VMIllegalException;
import org.litetokens.core.exception.ValidateScheduleException;
import org.litetokens.core.exception.ValidateSignatureException;
import org.litetokens.core.services.RpcApiService;
import org.litetokens.core.services.http.solidity.SolidityNodeHttpApiService;
import org.litetokens.protos.Protocol.Block;
import org.litetokens.protos.Protocol.DynamicProperties;

@Slf4j
public class SolidityNode {

  private DatabaseGrpcClient databaseGrpcClient;
  private Manager dbManager;

  private ScheduledExecutorService syncExecutor = Executors.newSingleThreadScheduledExecutor();

  public void setDbManager(Manager dbManager) {
    this.dbManager = dbManager;
  }

  private void initGrpcClient(String addr) {
    try {
      databaseGrpcClient = new DatabaseGrpcClient(addr);
    } catch (Exception e) {
      logger.error("Failed to create database grpc client {}", addr);
      System.exit(0);
    }
  }

  private void shutdownGrpcClient() {
    if (databaseGrpcClient != null) {
      databaseGrpcClient.shutdown();
    }
  }

  private void syncLoop(Args args) {
//    while (true) {
//      try {
//        initGrpcClient(args.getTrustNodeAddr());
//        syncSolidityBlock();
//        shutdownGrpcClient();
//      } catch (Exception e) {
//        logger.error("Error in sync solidity block " + e.getMessage(), e);
//      }
//      try {
//        Thread.sleep(5000);
//      } catch (InterruptedException e) {
//        Thread.currentThread().interrupt();
//        e.printStackTrace();
//      }
//    }
  }

  private void syncSolidityBlock() throws BadBlockException {
    DynamicProperties remoteDynamicProperties = databaseGrpcClient.getDynamicProperties();
    long remoteLastSolidityBlockNum = remoteDynamicProperties.getLastSolidityBlockNum();
    while (true) {

//      try {
//        Thread.sleep(10000);
//      } catch (Exception e) {
//
//      }
      long lastSolidityBlockNum = dbManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum();
      logger.info("sync solidity block, lastSolidityBlockNum:{}, remoteLastSolidityBlockNum:{}",
          lastSolidityBlockNum, remoteLastSolidityBlockNum);
      if (lastSolidityBlockNum < remoteLastSolidityBlockNum) {
        Block block = databaseGrpcClient.getBlock(lastSolidityBlockNum + 1);
        try {
          BlockCapsule blockCapsule = new BlockCapsule(block);
          dbManager.pushVerifiedBlock(blockCapsule);
          //dbManager.pushBlock(blockCapsule);
          for (TransactionCapsule xlt : blockCapsule.getTransactions()) {
            TransactionInfoCapsule ret;
            try {
              ret = dbManager.getTransactionHistoryStore().get(xlt.getTransactionId().getBytes());
            } catch (BadItemException ex) {
              logger.warn("", ex);
              continue;
            }
            ret.setBlockNumber(blockCapsule.getNum());
            ret.setBlockTimeStamp(blockCapsule.getTimeStamp());
            dbManager.getTransactionHistoryStore().put(xlt.getTransactionId().getBytes(), ret);
          }
          dbManager.getDynamicPropertiesStore()
              .saveLatestSolidifiedBlockNum(lastSolidityBlockNum + 1);
        } catch (AccountResourceInsufficientException e) {
          throw new BadBlockException("validate AccountResource exception");
        } catch (ValidateScheduleException e) {
          throw new BadBlockException("validate schedule exception");
        } catch (ValidateSignatureException e) {
          throw new BadBlockException("validate signature exception");
        } catch (ContractValidateException e) {
          throw new BadBlockException("ContractValidate exception");
        } catch (ContractExeException e) {
          throw new BadBlockException("Contract Execute exception");
        } catch (TaposException e) {
          throw new BadBlockException("tapos exception");
        } catch (DupTransactionException e) {
          throw new BadBlockException("dup exception");
        } catch (TooBigTransactionException e) {
          throw new BadBlockException("too big exception");
        } catch (TooBigTransactionResultException e) {
          throw new BadBlockException("too big exception result");
        } catch (TransactionExpirationException e) {
          throw new BadBlockException("expiration exception");
//        } catch (BadNumberBlockException e) {
//          throw new BadBlockException("bad number exception");
//        } catch (NonCommonBlockException e) {
//          throw new BadBlockException("non common exception");
        } catch (ReceiptCheckErrException e) {
          throw new BadBlockException("OutOfSlotTime Exception");
        } catch (VMIllegalException e) {
          throw new BadBlockException(e.getMessage());
        }

      } else {
        break;
      }
    }
    logger.info("Sync with trust node completed!!!");
  }

  private void start(Args cfgArgs) {
    syncExecutor.scheduleWithFixedDelay(() -> {
      try {
        initGrpcClient(cfgArgs.getTrustNodeAddr());
        syncSolidityBlock();
        shutdownGrpcClient();
      } catch (Throwable t) {
        logger.error("Error in sync solidity block " + t.getMessage());
      }
    }, 5000, 5000, TimeUnit.MILLISECONDS);
    //new Thread(() -> syncLoop(cfgArgs), logger.getName()).start();
  }

  /**
   * Start the SolidityNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Solidity node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory
        .getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.toLevel(cfgArgs.getLogLevel()));

    if (StringUtils.isEmpty(cfgArgs.getTrustNodeAddr())) {
      logger.error("Trust node not set.");
      return;
    }
    cfgArgs.setSolidityNode(true);

    ApplicationContext context = new LitetokensApplicationContext(DefaultConfig.class);

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    Application appT = ApplicationFactory.create(context);
    FullNode.shutdown(appT);

    //appT.init(cfgArgs);
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    //http
    SolidityNodeHttpApiService httpApiService = context.getBean(SolidityNodeHttpApiService.class);
    appT.addService(httpApiService);

    appT.initServices(cfgArgs);
    appT.startServices();
//    appT.startup();

    //Disable peer discovery for solidity node
    DiscoverServer discoverServer = context.getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = context.getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = context.getBean(NodeManager.class);
    nodeManager.close();

    SolidityNode node = new SolidityNode();
    node.setDbManager(appT.getDbManager());
    node.start(cfgArgs);

    rpcApiService.blockUntilShutdown();
  }
}
