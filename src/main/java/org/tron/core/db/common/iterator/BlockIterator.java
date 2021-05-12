package org.litetokens.core.db.common.iterator;

import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.exception.BadItemException;

import java.util.Iterator;
import java.util.Map.Entry;

public class BlockIterator extends AbstractIterator<BlockCapsule> {

  public BlockIterator(Iterator<Entry<byte[], byte[]>> iterator) {
    super(iterator);
  }

  @Override
  protected BlockCapsule of(byte[] value) {
    try {
      return new BlockCapsule(value);
    } catch (BadItemException e) {
      throw new RuntimeException(e);
    }
  }
}
