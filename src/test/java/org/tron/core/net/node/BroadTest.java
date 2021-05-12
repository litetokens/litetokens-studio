package org.litetokens.core.net.node;

import com.google.common.cache.Cache;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.junit.*;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.application.Application;
import org.litetokens.common.application.ApplicationFactory;
import org.litetokens.common.overlay.client.PeerClient;
import org.litetokens.common.overlay.discover.node.Node;
import org.litetokens.common.overlay.message.Message;
import org.litetokens.common.overlay.server.Channel;
import org.litetokens.common.overlay.server.ChannelManager;
import org.litetokens.common.overlay.server.MessageQueue;
import org.litetokens.common.overlay.server.SyncPool;
import org.litetokens.common.utils.FileUtil;
import org.litetokens.common.utils.ReflectUtils;
import org.litetokens.common.utils.Sha256Hash;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.config.DefaultConfig;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.db.ByteArrayWrapper;
import org.litetokens.core.db.Manager;
import org.litetokens.core.net.message.BlockMessage;
import org.litetokens.core.net.message.MessageTypes;
import org.litetokens.core.net.message.TransactionMessage;
import org.litetokens.core.net.node.NodeImpl.PriorItem;
import org.litetokens.core.net.peer.PeerConnection;
import org.litetokens.core.services.RpcApiService;
import org.litetokens.core.services.WitnessService;
import org.litetokens.protos.Protocol.Block;
import org.litetokens.protos.Protocol.Inventory.InventoryType;
import org.litetokens.protos.Protocol.Transaction;

@Slf4j
public class BroadTest {

  private static LitetokensApplicationContext context;
  private static NodeImpl node;
  RpcApiService rpcApiService;
  private static PeerClient peerClient;
  ChannelManager channelManager;
  SyncPool pool;
  private static Application appT;
  private static final String dbPath = "output-nodeImplTest-broad";
  private static final String dbDirectory = "db_Broad_test";
  private static final String indexDirectory = "index_Broad_test";

  private class Condition {

    private Sha256Hash blockId;
    private Sha256Hash transactionId;

    public Condition(Sha256Hash blockId, Sha256Hash transactionId) {
      this.blockId = blockId;
      this.transactionId = transactionId;
    }

    public Sha256Hash getBlockId() {
      return blockId;
    }

    public Sha256Hash getTransactionId() {
      return transactionId;
    }

  }

  private Sha256Hash testBlockBroad() {
    Block block = Block.getDefaultInstance();
    BlockMessage blockMessage = new BlockMessage(new BlockCapsule(block));
    node.broadcast(blockMessage);
    ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = ReflectUtils
        .getFieldValue(node, "advObjToSpread");
    Assert.assertEquals(advObjToSpread.get(blockMessage.getMessageId()), InventoryType.BLOCK);
    return blockMessage.getMessageId();
  }

  private Sha256Hash testTransactionBroad() {
    Transaction transaction = Transaction.getDefaultInstance();
    TransactionMessage transactionMessage = new TransactionMessage(transaction);
    node.broadcast(transactionMessage);
    ConcurrentHashMap<Sha256Hash, InventoryType> advObjToSpread = ReflectUtils
        .getFieldValue(node, "advObjToSpread");
    Assert.assertEquals(advObjToSpread.get(transactionMessage.getMessageId()), InventoryType.XLT);
    return transactionMessage.getMessageId();
  }

  private Condition testConsumerAdvObjToSpread() {
    Sha256Hash blockId = testBlockBroad();
    Sha256Hash transactionId = testTransactionBroad();
    //remove the tx and block
    removeTheTxAndBlock(blockId, transactionId);

    ReflectUtils.invokeMethod(node, "consumerAdvObjToSpread");
    Collection<PeerConnection> activePeers = ReflectUtils.invokeMethod(node, "getActivePeer");

    boolean result = true;
    for (PeerConnection peerConnection : activePeers) {
      if (!peerConnection.getAdvObjWeSpread().containsKey(blockId)) {
        result &= false;
      }
      if (!peerConnection.getAdvObjWeSpread().containsKey(transactionId)) {
        result &= false;
      }
    }
    for (PeerConnection peerConnection : activePeers) {
      peerConnection.getAdvObjWeSpread().clear();
    }
    Assert.assertTrue(result);
    return new Condition(blockId, transactionId);
  }

  private void removeTheTxAndBlock(Sha256Hash blockId, Sha256Hash transactionId) {
    Cache<Sha256Hash, TransactionMessage> xltCache = ReflectUtils.getFieldValue(node, "XltCache");
    Cache<Sha256Hash, BlockMessage> blockCache = ReflectUtils.getFieldValue(node, "BlockCache");
    xltCache.invalidate(transactionId);
    blockCache.invalidate(blockId);
  }

  @Test
  public void testConsumerAdvObjToFetch() throws InterruptedException {
    Condition condition = testConsumerAdvObjToSpread();
    Thread.sleep(1000);
    //
    Map<Sha256Hash, PriorItem> advObjToFetch = ReflectUtils
        .getFieldValue(node, "advObjToFetch");
    logger.info("advObjToFetch:{}", advObjToFetch);
    logger.info("advObjToFetchSize:{}", advObjToFetch.size());
    //Assert.assertEquals(advObjToFetch.get(condition.getBlockId()), InventoryType.BLOCK);
    //Assert.assertEquals(advObjToFetch.get(condition.getTransactionId()), InventoryType.XLT);
    //To avoid writing the database, manually stop the sending of messages.
    Collection<PeerConnection> activePeers = ReflectUtils.invokeMethod(node, "getActivePeer");
    for (PeerConnection peerConnection : activePeers) {
      MessageQueue messageQueue = ReflectUtils.getFieldValue(peerConnection, "msgQueue");
      ReflectUtils.setFieldValue(messageQueue, "sendMsgFlag", false);
    }
    //
    ReflectUtils.invokeMethod(node, "consumerAdvObjToFetch");
    Thread.sleep(1000);
    boolean result = true;
    int count = 0;
    for (PeerConnection peerConnection : activePeers) {
      if (peerConnection.getAdvObjWeRequested()
          .containsKey(new Item(condition.getTransactionId(), InventoryType.XLT))) {
        ++count;
      }
      if (peerConnection.getAdvObjWeRequested()
          .containsKey(new Item(condition.getBlockId(), InventoryType.BLOCK))) {
        ++count;
      }
      MessageQueue messageQueue = ReflectUtils.getFieldValue(peerConnection, "msgQueue");
      BlockingQueue<Message> msgQueue = ReflectUtils.getFieldValue(messageQueue, "msgQueue");
      for (Message message : msgQueue) {
        if (message.getType() == MessageTypes.BLOCK) {
          Assert.assertEquals(message.getMessageId(), condition.getBlockId());
        }
        if (message.getType() == MessageTypes.XLT) {
          Assert.assertEquals(message.getMessageId(), condition.getTransactionId());
        }
      }
    }
    Assert.assertTrue(count >= 2);
  }

  private static boolean go = false;

  @Before
  public void init() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        logger.info("Full node running.");
        Args.setParam(
            new String[]{
                "--output-directory", dbPath,
                "--storage-db-directory", dbDirectory,
                "--storage-index-directory", indexDirectory
            },
            "config.conf"
        );
        Args cfgArgs = Args.getInstance();
        cfgArgs.setNodeListenPort(17889);
        cfgArgs.setNodeDiscoveryEnable(false);
        cfgArgs.getSeedNode().getIpList().clear();
        cfgArgs.setNeedSyncCheck(false);
        cfgArgs.setNodeExternalIp("127.0.0.1");

        context = new LitetokensApplicationContext(DefaultConfig.class);

        if (cfgArgs.isHelp()) {
          logger.info("Here is the help message.");
          return;
        }
        appT = ApplicationFactory.create(context);
        rpcApiService = context.getBean(RpcApiService.class);
        appT.addService(rpcApiService);
        if (cfgArgs.isWitness()) {
          appT.addService(new WitnessService(appT, context));
        }
//        appT.initServices(cfgArgs);
//        appT.startServices();
//        appT.startup();
        node = context.getBean(NodeImpl.class);
        peerClient = context.getBean(PeerClient.class);
        channelManager = context.getBean(ChannelManager.class);
        pool = context.getBean(SyncPool.class);
        Manager dbManager = context.getBean(Manager.class);
        NodeDelegate nodeDelegate = new NodeDelegateImpl(dbManager);
        node.setNodeDelegate(nodeDelegate);
        pool.init(node);
        prepare();
        rpcApiService.blockUntilShutdown();
      }
    }).start();
    int tryTimes = 1;
    while (tryTimes <= 30 && (node == null || peerClient == null
        || channelManager == null || pool == null || !go)) {
      try {
        logger.info("node:{},peerClient:{},channelManager:{},pool:{},{}", node, peerClient,
            channelManager, pool, go);
        Thread.sleep(1000 * tryTimes);
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        ++tryTimes;
      }
    }
  }

  private void prepare() {
    try {
      ExecutorService advertiseLoopThread = ReflectUtils.getFieldValue(node, "broadPool");
      advertiseLoopThread.shutdownNow();

      ReflectUtils.setFieldValue(node, "isAdvertiseActive", false);
      ReflectUtils.setFieldValue(node, "isFetchActive", false);

      Node node = new Node(
          "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17889");
      new Thread(new Runnable() {
        @Override
        public void run() {
          peerClient.connect(node.getHost(), node.getPort(), node.getHexId());
        }
      }).start();
      Thread.sleep(2000);
      Map<ByteArrayWrapper, Channel> activePeers = ReflectUtils
          .getFieldValue(channelManager, "activePeers");
      int tryTimes = 0;
      while (MapUtils.isEmpty(activePeers) && ++tryTimes < 10) {
        Thread.sleep(1000);
      }
      go = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    Collection<PeerConnection> peerConnections = ReflectUtils.invokeMethod(node, "getActivePeer");
    for (PeerConnection peer : peerConnections) {
      peer.close();
    }
    context.destroy();
    peerClient.close();
    appT.shutdownServices();
    appT.shutdown();
    FileUtil.deleteDir(new File(dbPath));
  }

}
