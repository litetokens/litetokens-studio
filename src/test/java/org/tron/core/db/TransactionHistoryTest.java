package org.litetokens.core.db;

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.utils.ByteArray;
import org.litetokens.common.utils.FileUtil;
import org.litetokens.core.Constant;
import org.litetokens.core.capsule.TransactionInfoCapsule;
import org.litetokens.core.config.DefaultConfig;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.exception.BadItemException;

public class TransactionHistoryTest {

  private static String dbPath = "output_TransactionHistoryStore_test";
  private static String dbDirectory = "db_TransactionHistoryStore_test";
  private static String indexDirectory = "index_TransactionHistoryStore_test";
  private static LitetokensApplicationContext context;
  private static TransactionHistoryStore transactionHistoryStore;
  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_CONF
    );
    context = new LitetokensApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    transactionHistoryStore = context.getBean(TransactionHistoryStore.class);
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();

    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);
    transactionHistoryStore.put(transactionId, transactionInfoCapsule);
  }

  @Test
  public void get() throws BadItemException {
    //test get and has Method
    TransactionInfoCapsule resultCapsule = transactionHistoryStore.get(transactionId);
    Assert.assertEquals(1000L, resultCapsule.getFee());
    Assert.assertEquals(100L, resultCapsule.getBlockNumber());
    Assert.assertEquals(200L, resultCapsule.getBlockTimeStamp());
    Assert.assertEquals(ByteArray.toHexString(transactionId),
        ByteArray.toHexString(resultCapsule.getId()));
  }
}