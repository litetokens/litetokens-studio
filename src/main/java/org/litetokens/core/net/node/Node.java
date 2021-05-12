package org.litetokens.core.net.node;

import org.litetokens.common.overlay.message.Message;
import org.litetokens.common.utils.Quitable;
import org.litetokens.common.utils.Sha256Hash;

public interface Node extends Quitable {

  void setNodeDelegate(NodeDelegate nodeDel);

  void broadcast(Message msg);

  void listen();

  void syncFrom(Sha256Hash myHeadBlockHash);

  void close() throws InterruptedException;
}
