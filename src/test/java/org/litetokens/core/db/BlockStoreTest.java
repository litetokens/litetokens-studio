package org.litetokens.core.db;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.utils.FileUtil;
import org.litetokens.core.Constant;
import org.litetokens.core.config.DefaultConfig;
import org.litetokens.core.config.args.Args;

@Slf4j
public class BlockStoreTest {

  private static final String dbPath = "output-blockStore-test";
  BlockStore blockStore;
  private static LitetokensApplicationContext context;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
    context = new LitetokensApplicationContext(DefaultConfig.class);
  }

  @Before
  public void init() {
    blockStore = context.getBean(BlockStore.class);
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testCreateBlockStore() {
  }
}
