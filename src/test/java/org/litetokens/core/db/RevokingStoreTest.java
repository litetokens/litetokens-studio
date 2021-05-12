package org.litetokens.core.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.litetokens.common.application.Application;
import org.litetokens.common.application.ApplicationFactory;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.utils.SessionOptional;
import org.litetokens.common.utils.FileUtil;
import org.litetokens.core.Constant;
import org.litetokens.core.capsule.ProtoCapsule;
import org.litetokens.core.config.DefaultConfig;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.db.AbstractRevokingStore.Dialog;
import org.litetokens.core.db2.core.ISession;
import org.litetokens.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class RevokingStoreTest {

  private AbstractRevokingStore revokingDatabase;
  private  LitetokensApplicationContext context;
  private Application appT;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"},
        Constant.TEST_CONF);
    context = new LitetokensApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = new TestRevokingLitetokensDatabase();
    revokingDatabase.enable();
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
  }

  @Test
  public synchronized void testUndo() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingLitetokensStore litetokensDatabase = new TestRevokingLitetokensStore(
        "testrevokinglitetokensstore-testUndo", revokingDatabase);

    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    for (int i = 0; i < 10; i++) {
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("undo" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        litetokensDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertEquals(revokingDatabase.getStack().size(), 2);
        tmpSession.merge();
        Assert.assertEquals(revokingDatabase.getStack().size(), 1);
      }
    }

    Assert.assertEquals(revokingDatabase.getStack().size(), 1);

    dialog.reset();

    Assert.assertTrue(revokingDatabase.getStack().isEmpty());
    Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);

    dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    revokingDatabase.disable();
    TestProtoCapsule testProtoCapsule = new TestProtoCapsule("del".getBytes());
    litetokensDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
    revokingDatabase.enable();

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      litetokensDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del2".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      litetokensDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del22".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      litetokensDatabase.put(testProtoCapsule.getData(), new TestProtoCapsule("del222".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      litetokensDatabase.delete(testProtoCapsule.getData());
      tmpSession.merge();
    }

    dialog.reset();

    logger.info("**********testProtoCapsule:" + String.valueOf(litetokensDatabase.getUnchecked(testProtoCapsule.getData())));
    Assert.assertArrayEquals("del".getBytes(), litetokensDatabase.getUnchecked(testProtoCapsule.getData()).getData());
    Assert.assertEquals(testProtoCapsule, litetokensDatabase.getUnchecked(testProtoCapsule.getData()));

    litetokensDatabase.close();
  }

  @Test
  public synchronized void testPop() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingLitetokensStore litetokensDatabase = new TestRevokingLitetokensStore(
        "testrevokinglitetokensstore-testPop", revokingDatabase);

    for (int i = 1; i < 11; i++) {
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("pop" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        litetokensDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 1);
        tmpSession.commit();
        Assert.assertEquals(revokingDatabase.getStack().size(), i);
        Assert.assertEquals(revokingDatabase.getActiveDialog(), 0);
      }
    }

    for (int i = 1; i < 11; i++) {
      revokingDatabase.pop();
      Assert.assertEquals(10 - i, revokingDatabase.getStack().size());
    }

    litetokensDatabase.close();

    Assert.assertEquals(revokingDatabase.getStack().size(), 0);
  }

  @Test
  public void shutdown() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingLitetokensStore litetokensDatabase = new TestRevokingLitetokensStore(
        "testrevokinglitetokensstore-shutdown", revokingDatabase);

    List<TestProtoCapsule> capsules = new ArrayList<>();
    for (int i = 1; i < 11; i++) {
      revokingDatabase.buildSession();
      TestProtoCapsule testProtoCapsule = new TestProtoCapsule(("test" + i).getBytes());
      capsules.add(testProtoCapsule);
      litetokensDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
      Assert.assertEquals(revokingDatabase.getActiveDialog(), i);
      Assert.assertEquals(revokingDatabase.getStack().size(), i);
    }

    for (TestProtoCapsule capsule : capsules) {
      logger.info(new String(capsule.getData()));
      Assert.assertEquals(capsule, litetokensDatabase.getUnchecked(capsule.getData()));
    }

    revokingDatabase.shutdown();

    for (TestProtoCapsule capsule : capsules) {
      logger.info(litetokensDatabase.getUnchecked(capsule.getData()).toString());
      Assert.assertEquals(null, litetokensDatabase.getUnchecked(capsule.getData()).getData());
    }

    Assert.assertEquals(revokingDatabase.getStack().size(), 0);
    litetokensDatabase.close();

  }

  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class TestProtoCapsule implements ProtoCapsule<Object> {

    private byte[] value;

    @Override
    public byte[] getData() {
      return value;
    }

    @Override
    public Object getInstance() {
      return value;
    }

    @Override
    public String toString() {
      return "TestProtoCapsule{"
          + "value=" + Arrays.toString(value)
          + ", string=" + (value == null ? "" : new String(value))
          + '}';
    }
  }

  private static class TestRevokingLitetokensStore extends LitetokensStoreWithRevoking<TestProtoCapsule> {

    protected TestRevokingLitetokensStore(String dbName, RevokingDatabase revokingDatabase) {
      super(dbName, revokingDatabase);
    }
  }

  private static class TestRevokingLitetokensDatabase extends AbstractRevokingStore {

  }
}
