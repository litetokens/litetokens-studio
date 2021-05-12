/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.litetokens.common.overlay.discover.node.statistics;

import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import org.litetokens.common.overlay.discover.node.Node;
import org.litetokens.core.config.args.Args;
import org.litetokens.protos.Protocol.ReasonCode;

public class NodeStatistics {

  public static final int REPUTATION_PREDEFINED = 100000;
  public static final long TOO_MANY_PEERS_PENALIZE_TIMEOUT = 60 * 1000L;
  private static final long CLEAR_CYCLE_TIME = 60 * 60 * 1000L;
  private final long MIN_DATA_LENGTH = Args.getInstance().getReceiveTcpMinDataLength();

  private boolean isPredefined = false;
  private int persistedReputation = 0;
  @Getter
  private int disconnectTimes = 0;
  @Getter
  private ReasonCode litetokensLastRemoteDisconnectReason = null;
  @Getter
  private ReasonCode litetokensLastLocalDisconnectReason = null;
  private long lastDisconnectedTime = 0;
  private long firstDisconnectedTime = 0;

  public final MessageStatistics messageStatistics = new MessageStatistics();
  public final MessageCount p2pHandShake = new MessageCount();
  public final MessageCount tcpFlow = new MessageCount();

  public final SimpleStatter discoverMessageLatency;
  public final AtomicLong lastPongReplyTime = new AtomicLong(0L); // in milliseconds

  private Reputation reputation;

  public NodeStatistics(Node node) {
    discoverMessageLatency = new SimpleStatter(node.getIdString());
    reputation = new Reputation(this);
  }

  public int getReputation() {
    int score = 0;
    if (!isReputationPenalized()) {
      score += persistedReputation / 5 + reputation.calculate();
    }
    if (isPredefined) {
      score += REPUTATION_PREDEFINED;
    }
    return score;
  }

  public ReasonCode getDisconnectReason() {
    if (litetokensLastLocalDisconnectReason != null) {
      return litetokensLastLocalDisconnectReason;
    }
    if (litetokensLastRemoteDisconnectReason != null) {
      return litetokensLastRemoteDisconnectReason;
    }
    return ReasonCode.UNKNOWN;
  }

  public boolean isReputationPenalized() {

    if (wasDisconnected() && litetokensLastRemoteDisconnectReason == ReasonCode.TOO_MANY_PEERS
        && System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
      return true;
    }

    if (wasDisconnected() && litetokensLastRemoteDisconnectReason == ReasonCode.DUPLICATE_PEER
        && System.currentTimeMillis() - lastDisconnectedTime < TOO_MANY_PEERS_PENALIZE_TIMEOUT) {
      return true;
    }

    if (firstDisconnectedTime > 0
        && (System.currentTimeMillis() - firstDisconnectedTime) > CLEAR_CYCLE_TIME) {
      litetokensLastLocalDisconnectReason = null;
      litetokensLastRemoteDisconnectReason = null;
      disconnectTimes = 0;
      persistedReputation = 0;
      firstDisconnectedTime = 0;
    }

    if (litetokensLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL
        || litetokensLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_PROTOCOL
        || litetokensLastLocalDisconnectReason == ReasonCode.BAD_PROTOCOL
        || litetokensLastRemoteDisconnectReason == ReasonCode.BAD_PROTOCOL
        || litetokensLastLocalDisconnectReason == ReasonCode.BAD_BLOCK
        || litetokensLastRemoteDisconnectReason == ReasonCode.BAD_BLOCK
        || litetokensLastLocalDisconnectReason == ReasonCode.BAD_TX
        || litetokensLastRemoteDisconnectReason == ReasonCode.BAD_TX
        || litetokensLastLocalDisconnectReason == ReasonCode.FORKED
        || litetokensLastRemoteDisconnectReason == ReasonCode.FORKED
        || litetokensLastLocalDisconnectReason == ReasonCode.UNLINKABLE
        || litetokensLastRemoteDisconnectReason == ReasonCode.UNLINKABLE
        || litetokensLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN
        || litetokensLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_CHAIN
        || litetokensLastRemoteDisconnectReason == ReasonCode.SYNC_FAIL
        || litetokensLastLocalDisconnectReason == ReasonCode.SYNC_FAIL
        || litetokensLastRemoteDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION
        || litetokensLastLocalDisconnectReason == ReasonCode.INCOMPATIBLE_VERSION) {
      persistedReputation = 0;
      return true;
    }
    return false;
  }

  public void nodeDisconnectedRemote(ReasonCode reason) {
    lastDisconnectedTime = System.currentTimeMillis();
    litetokensLastRemoteDisconnectReason = reason;
  }

  public void nodeDisconnectedLocal(ReasonCode reason) {
    lastDisconnectedTime = System.currentTimeMillis();
    litetokensLastLocalDisconnectReason = reason;
  }

  public void notifyDisconnect() {
    lastDisconnectedTime = System.currentTimeMillis();
    if (firstDisconnectedTime <= 0) {
      firstDisconnectedTime = lastDisconnectedTime;
    }
    if (litetokensLastLocalDisconnectReason == ReasonCode.RESET) {
      return;
    }
    disconnectTimes++;
    persistedReputation = persistedReputation / 2;
  }

  public boolean wasDisconnected() {
    return lastDisconnectedTime > 0;
  }

  public void setPredefined(boolean isPredefined) {
    this.isPredefined = isPredefined;
  }

  public boolean isPredefined() {
    return isPredefined;
  }

  public void setPersistedReputation(int persistedReputation) {
    this.persistedReputation = persistedReputation;
  }

  @Override
  public String toString() {
    return "NodeStat[reput: " + getReputation() + "(" + persistedReputation + "), discover: "
        + messageStatistics.discoverInPong + "/" + messageStatistics.discoverOutPing + " "
        + messageStatistics.discoverOutPong + "/" + messageStatistics.discoverInPing + " "
        + messageStatistics.discoverInNeighbours + "/" + messageStatistics.discoverOutFindNode
        + " "
        + messageStatistics.discoverOutNeighbours + "/" + messageStatistics.discoverInFindNode
        + " "
        + ((int) discoverMessageLatency.getAvrg()) + "ms"
        + ", p2p: " + p2pHandShake + "/" + messageStatistics.p2pInHello + "/"
        + messageStatistics.p2pOutHello + " "
        + ", litetokens: " + messageStatistics.litetokensInMessage + "/" + messageStatistics.litetokensOutMessage
        + " "
        + (wasDisconnected() ? "X " + disconnectTimes : "")
        + (litetokensLastLocalDisconnectReason != null ? ("<=" + litetokensLastLocalDisconnectReason) : " ")
        + (litetokensLastRemoteDisconnectReason != null ? ("=>" + litetokensLastRemoteDisconnectReason) : " ")
        + ", tcp flow: " + tcpFlow.getTotalCount();
  }

  public class SimpleStatter {

    private final String name;
    private volatile double last;
    private volatile double sum;
    private volatile int count;

    public SimpleStatter(String name) {
      this.name = name;
    }

    public void add(double value) {
      last = value;
      sum += value;
      count++;
    }

    public double getLast() {
      return last;
    }

    public int getCount() {
      return count;
    }

    public double getSum() {
      return sum;
    }

    public double getAvrg() {
      return count == 0 ? 0 : sum / count;
    }

    public String getName() {
      return name;
    }

  }

  public boolean nodeIsHaveDataTransfer() {
    return tcpFlow.getTotalCount() > MIN_DATA_LENGTH;
  }

  public void resetTcpFlow() {
    tcpFlow.reset();
  }

}
