package org.litetokens.core.net.message;

import java.util.List;
import org.litetokens.common.utils.Sha256Hash;
import org.litetokens.protos.Protocol.Inventory;
import org.litetokens.protos.Protocol.Inventory.InventoryType;

public class TransactionInventoryMessage extends InventoryMessage {

  public TransactionInventoryMessage(byte[] packed) throws Exception {
    super(packed);
  }

  public TransactionInventoryMessage(Inventory inv) {
    super(inv);
  }

  public TransactionInventoryMessage(List<Sha256Hash> hashList) {
    super(hashList, InventoryType.XLT);
  }
}
