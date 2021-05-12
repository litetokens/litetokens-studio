package org.litetokens.core.net.peer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.litetokens.common.overlay.server.Channel;
import org.litetokens.common.overlay.server.MessageQueue;
import org.litetokens.core.net.message.LitetokensMessage;

@Component
@Scope("prototype")
public class LitetokensHandler extends SimpleChannelInboundHandler<LitetokensMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue = null;

  public PeerConnectionDelegate peerDel;

  public void setPeerDel(PeerConnectionDelegate peerDel) {
    this.peerDel = peerDel;
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, LitetokensMessage msg) {
    msgQueue.receivedMessage(msg);
    peerDel.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}