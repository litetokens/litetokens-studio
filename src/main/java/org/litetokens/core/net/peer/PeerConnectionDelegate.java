package org.litetokens.core.net.peer;

import org.litetokens.common.overlay.message.Message;
import org.litetokens.common.utils.Sha256Hash;
import org.litetokens.core.net.message.LitetokensMessage;

public abstract class PeerConnectionDelegate {

  public abstract void onMessage(PeerConnection peer, LitetokensMessage msg);

  public abstract Message getMessage(Sha256Hash msgId);

  public abstract void onConnectPeer(PeerConnection peer);

  public abstract void onDisconnectPeer(PeerConnection peer);

}
