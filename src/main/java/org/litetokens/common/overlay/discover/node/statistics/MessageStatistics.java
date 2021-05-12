package org.litetokens.common.overlay.discover.node.statistics;

import lombok.extern.slf4j.Slf4j;
import org.litetokens.common.net.udp.message.UdpMessageTypeEnum;
import org.litetokens.common.overlay.message.Message;
import org.litetokens.core.net.message.FetchInvDataMessage;
import org.litetokens.core.net.message.InventoryMessage;
import org.litetokens.core.net.message.MessageTypes;
import org.litetokens.core.net.message.TransactionsMessage;

@Slf4j
public class MessageStatistics {

  //udp discovery
  public final MessageCount discoverInPing = new MessageCount();
  public final MessageCount discoverOutPing = new MessageCount();
  public final MessageCount discoverInPong = new MessageCount();
  public final MessageCount discoverOutPong = new MessageCount();
  public final MessageCount discoverInFindNode = new MessageCount();
  public final MessageCount discoverOutFindNode = new MessageCount();
  public final MessageCount discoverInNeighbours = new MessageCount();
  public final MessageCount discoverOutNeighbours = new MessageCount();

  //tcp p2p
  public final MessageCount p2pInHello = new MessageCount();
  public final MessageCount p2pOutHello = new MessageCount();
  public final MessageCount p2pInPing = new MessageCount();
  public final MessageCount p2pOutPing = new MessageCount();
  public final MessageCount p2pInPong = new MessageCount();
  public final MessageCount p2pOutPong = new MessageCount();
  public final MessageCount p2pInDisconnect = new MessageCount();
  public final MessageCount p2pOutDisconnect = new MessageCount();

  //tcp litetokens
  public final MessageCount litetokensInMessage = new MessageCount();
  public final MessageCount litetokensOutMessage = new MessageCount();

  public final MessageCount litetokensInSyncBlockChain = new MessageCount();
  public final MessageCount litetokensOutSyncBlockChain = new MessageCount();
  public final MessageCount litetokensInBlockChainInventory = new MessageCount();
  public final MessageCount litetokensOutBlockChainInventory = new MessageCount();

  public final MessageCount litetokensInXltInventory = new MessageCount();
  public final MessageCount litetokensOutXltInventory = new MessageCount();
  public final MessageCount litetokensInXltInventoryElement = new MessageCount();
  public final MessageCount litetokensOutXltInventoryElement = new MessageCount();

  public final MessageCount litetokensInBlockInventory = new MessageCount();
  public final MessageCount litetokensOutBlockInventory = new MessageCount();
  public final MessageCount litetokensInBlockInventoryElement = new MessageCount();
  public final MessageCount litetokensOutBlockInventoryElement = new MessageCount();

  public final MessageCount litetokensInXltFetchInvData = new MessageCount();
  public final MessageCount litetokensOutXltFetchInvData = new MessageCount();
  public final MessageCount litetokensInXltFetchInvDataElement = new MessageCount();
  public final MessageCount litetokensOutXltFetchInvDataElement = new MessageCount();

  public final MessageCount litetokensInBlockFetchInvData = new MessageCount();
  public final MessageCount litetokensOutBlockFetchInvData = new MessageCount();
  public final MessageCount litetokensInBlockFetchInvDataElement = new MessageCount();
  public final MessageCount litetokensOutBlockFetchInvDataElement = new MessageCount();


  public final MessageCount litetokensInXlt = new MessageCount();
  public final MessageCount litetokensOutXlt = new MessageCount();
  public final MessageCount litetokensInXlts = new MessageCount();
  public final MessageCount litetokensOutXlts = new MessageCount();
  public final MessageCount litetokensInBlock = new MessageCount();
  public final MessageCount litetokensOutBlock = new MessageCount();
  public final MessageCount litetokensOutAdvBlock = new MessageCount();

  public void addUdpInMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, true);
  }

  public void addUdpOutMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, false);
  }

  public void addTcpInMessage(Message msg) {
    addTcpMessage(msg, true);
  }

  public void addTcpOutMessage(Message msg) {
    addTcpMessage(msg, false);
  }

  private void addUdpMessage(UdpMessageTypeEnum type, boolean flag) {
    switch (type) {
      case DISCOVER_PING:
        if (flag) {
          discoverInPing.add();
        } else {
          discoverOutPing.add();
        }
        break;
      case DISCOVER_PONG:
        if (flag) {
          discoverInPong.add();
        } else {
          discoverOutPong.add();
        }
        break;
      case DISCOVER_FIND_NODE:
        if (flag) {
          discoverInFindNode.add();
        } else {
          discoverOutFindNode.add();
        }
        break;
      case DISCOVER_NEIGHBORS:
        if (flag) {
          discoverInNeighbours.add();
        } else {
          discoverOutNeighbours.add();
        }
        break;
      default:
        break;
    }
  }

  private void addTcpMessage(Message msg, boolean flag) {

    if (flag) {
      litetokensInMessage.add();
    } else {
      litetokensOutMessage.add();
    }

    switch (msg.getType()) {
      case P2P_HELLO:
        if (flag) {
          p2pInHello.add();
        } else {
          p2pOutHello.add();
        }
        break;
      case P2P_PING:
        if (flag) {
          p2pInPing.add();
        } else {
          p2pOutPing.add();
        }
        break;
      case P2P_PONG:
        if (flag) {
          p2pInPong.add();
        } else {
          p2pOutPong.add();
        }
        break;
      case P2P_DISCONNECT:
        if (flag) {
          p2pInDisconnect.add();
        } else {
          p2pOutDisconnect.add();
        }
        break;
      case SYNC_BLOCK_CHAIN:
        if (flag) {
          litetokensInSyncBlockChain.add();
        } else {
          litetokensOutSyncBlockChain.add();
        }
        break;
      case BLOCK_CHAIN_INVENTORY:
        if (flag) {
          litetokensInBlockChainInventory.add();
        } else {
          litetokensOutBlockChainInventory.add();
        }
        break;
      case INVENTORY:
        InventoryMessage inventoryMessage = (InventoryMessage) msg;
        int inventorySize = inventoryMessage.getInventory().getIdsCount();
        if (flag) {
          if (inventoryMessage.getInvMessageType() == MessageTypes.XLT) {
            litetokensInXltInventory.add();
            litetokensInXltInventoryElement.add(inventorySize);
          } else {
            litetokensInBlockInventory.add();
            litetokensInBlockInventoryElement.add(inventorySize);
          }
        } else {
          if (inventoryMessage.getInvMessageType() == MessageTypes.XLT) {
            litetokensOutXltInventory.add();
            litetokensOutXltInventoryElement.add(inventorySize);
          } else {
            litetokensOutBlockInventory.add();
            litetokensOutBlockInventoryElement.add(inventorySize);
          }
        }
        break;
      case FETCH_INV_DATA:
        FetchInvDataMessage fetchInvDataMessage = (FetchInvDataMessage) msg;
        int fetchSize = fetchInvDataMessage.getInventory().getIdsCount();
        if (flag) {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.XLT) {
            litetokensInXltFetchInvData.add();
            litetokensInXltFetchInvDataElement.add(fetchSize);
          } else {
            litetokensInBlockFetchInvData.add();
            litetokensInBlockFetchInvDataElement.add(fetchSize);
          }
        } else {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.XLT) {
            litetokensOutXltFetchInvData.add();
            litetokensOutXltFetchInvDataElement.add(fetchSize);
          } else {
            litetokensOutBlockFetchInvData.add();
            litetokensOutBlockFetchInvDataElement.add(fetchSize);
          }
        }
        break;
      case XLTS:
        TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
        if (flag) {
          litetokensInXlts.add();
          litetokensInXlt.add(transactionsMessage.getTransactions().getTransactionsCount());
        } else {
          litetokensOutXlts.add();
          litetokensOutXlt.add(transactionsMessage.getTransactions().getTransactionsCount());
        }
        break;
      case XLT:
        if (flag) {
          litetokensInMessage.add();
        } else {
          litetokensOutMessage.add();
        }
        break;
      case BLOCK:
        if (flag) {
          litetokensInBlock.add();
        }
        litetokensOutBlock.add();
        break;
      default:
        break;
    }
  }

}
