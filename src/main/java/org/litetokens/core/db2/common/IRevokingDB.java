package org.litetokens.core.db2.common;

import java.util.Map;
import java.util.Set;
import org.litetokens.core.exception.ItemNotFoundException;

public interface IRevokingDB extends Iterable<Map.Entry<byte[], byte[]>> {
  void put(byte[] key, byte[] newValue);

  void delete(byte[] key);

  boolean has(byte[] key);

  byte[] get(byte[] key) throws ItemNotFoundException;

  byte[] getUnchecked(byte[] key);

  void close();

  void reset();

  // for blockstore
  Set<byte[]> getlatestValues(long limit);

  // for blockstore
  Set<byte[]> getValuesNext(byte[] key, long limit);

}
