package org.litetokens.core.net.node;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.junit.*;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.application.Application;
import org.litetokens.common.application.ApplicationFactory;
import org.litetokens.common.crypto.ECKey;
import org.litetokens.common.overlay.client.PeerClient;
import org.litetokens.common.overlay.discover.node.Node;
import org.litetokens.common.overlay.server.Channel;
import org.litetokens.common.overlay.server.ChannelManager;
import org.litetokens.common.overlay.server.SyncPool;
import org.litetokens.common.utils.ByteArray;
import org.litetokens.common.utils.FileUtil;
import org.litetokens.common.utils.ReflectUtils;
import org.litetokens.core.Constant;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.config.DefaultConfig;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.db.ByteArrayWrapper;
import org.litetokens.core.db.Manager;
import org.litetokens.core.net.peer.PeerConnection;
import org.litetokens.core.services.RpcApiService;
import org.litetokens.core.services.WitnessService;
import org.litetokens.protos.Protocol.Block;
import org.litetokens.protos.Protocol.BlockHeader;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;


@Slf4j
public class FinishProcessSyncBlockTest {

    private static LitetokensApplicationContext context;
    private static NodeImpl node;
    RpcApiService rpcApiService;
    private static PeerClient peerClient;
    ChannelManager channelManager;
    SyncPool pool;
    private static Application appT;
    Manager dbManager;

    private static final String dbPath = "output-FinishProcessSyncBlockTest";
    private static final String dbDirectory = "db_FinishProcessSyncBlock_test";
    private static final String indexDirectory = "index_FinishProcessSyncBlock_test";

    @Test
    public void testFinishProcessSyncBlock() throws Exception {
        Collection<PeerConnection> activePeers = ReflectUtils.invokeMethod(node, "getActivePeer");
        PeerConnection peer = (PeerConnection) activePeers.toArray()[1];
        BlockCapsule headBlockCapsule = dbManager.getHead();
        BlockCapsule blockCapsule = generateOneBlockCapsule(headBlockCapsule);
        Class[] cls = {BlockCapsule.class};
        Object[] params = {blockCapsule};
        peer.getBlockInProc().add(blockCapsule.getBlockId());
        ReflectUtils.invokeMethod(node, "finishProcessSyncBlock", cls, params);
        Assert.assertEquals(peer.getBlockInProc().isEmpty(), true);
    }

    // generate ong block by parent block
    private BlockCapsule generateOneBlockCapsule(BlockCapsule parentCapsule) {
        ByteString witnessAddress = ByteString.copyFrom(
                ECKey.fromPrivate(
                        ByteArray.fromHexString(
                                Args.getInstance().getLocalWitnesses().getPrivateKey()))
                        .getAddress());
        BlockHeader.raw raw = BlockHeader.raw.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setParentHash(parentCapsule.getBlockId().getByteString())
                .setNumber(parentCapsule.getNum() + 1)
                .setWitnessAddress(witnessAddress)
                .setWitnessId(1).build();
        BlockHeader blockHeader = BlockHeader.newBuilder()
                .setRawData(raw)
                .build();

        Block block = Block.newBuilder().setBlockHeader(blockHeader).build();

        BlockCapsule blockCapsule = new BlockCapsule(block);
        blockCapsule.setMerkleRoot();
        blockCapsule.sign(
                ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));
        blockCapsule.setMerkleRoot();
        blockCapsule.sign(
                ByteArray.fromHexString(Args.getInstance().getLocalWitnesses().getPrivateKey()));

        return blockCapsule;
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
                        Constant.TEST_CONF
                );
                Args cfgArgs = Args.getInstance();
                cfgArgs.setNodeListenPort(17895);
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
                dbManager = context.getBean(Manager.class);
                NodeDelegate nodeDelegate = new NodeDelegateImpl(dbManager);
                node.setNodeDelegate(nodeDelegate);
                pool.init(node);
                prepare();
                rpcApiService.blockUntilShutdown();
            }
        }).start();
        int tryTimes = 0;
        while (tryTimes < 10 && (node == null || peerClient == null
                || channelManager == null || pool == null || !go)) {
            try {
                logger.info("node:{},peerClient:{},channelManager:{},pool:{},{}", node, peerClient,
                        channelManager, pool, go);
                Thread.sleep(1000);
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
                    "enode://e437a4836b77ad9d9ffe73ee782ef2614e6d8370fcf62191a6e488276e23717147073a7ce0b444d485fff5a0c34c4577251a7a990cf80d8542e21b95aa8c5e6c@127.0.0.1:17895");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    peerClient.connect(node.getHost(), node.getPort(), node.getHexId());
                }
            }).start();
            Thread.sleep(1000);
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
        peerClient.close();
        appT.shutdownServices();
        appT.shutdown();
        context.destroy();
        FileUtil.deleteDir(new File(dbPath));
    }
}
