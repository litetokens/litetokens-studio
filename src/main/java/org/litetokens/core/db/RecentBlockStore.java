package org.litetokens.core.db;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.litetokens.core.capsule.BytesCapsule;
import org.litetokens.core.exception.ItemNotFoundException;

@Component
public class RecentBlockStore extends LitetokensStoreWithRevoking<BytesCapsule> {

  @Autowired
  private RecentBlockStore(@Value("recent-block") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);

    return new BytesCapsule(value);
  }
}
