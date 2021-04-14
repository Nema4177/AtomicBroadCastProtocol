package com.example.abp.udp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.example.abp.helper.GlobalMessHelper;
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;

public class SequenceUDP extends Thread {

	InetAddress IPAddress;
	DatagramSocket clientSocket;
	private boolean listen = true;

	public SequenceUDP() {
		try {

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public byte[] convertToBytes(Object object) {
		byte[] serializedMessage = null;
		try {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			ObjectOutput oo = new ObjectOutputStream(bStream);
			oo.writeObject(object);
			oo.close();
			serializedMessage = bStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return serializedMessage;
	}

	public void sendPacketToAll(byte[] messageBytes) {
		try {
			for (int peerServer : Properties.peers) {
				DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, IPAddress,
						Properties.seqListenPortMappoing.get(peerServer));
				clientSocket.send(sendPacket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void retransmitMessage(byte[] messageBytes, InetAddress ipAddress, int udpPort) {
		try {
			DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, ipAddress, udpPort);
			clientSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendRetransmitRequest(byte[] messageBytes, InetAddress ipAddress, int udpPort) {
		try {
			DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, ipAddress, udpPort);
			clientSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public GlobalSeqMessage receivePacket() {
		GlobalSeqMessage message = null;
		try {
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
			message = (GlobalSeqMessage) in.readObject();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

	void closeSocket() {
		clientSocket.close();
	}
	
	private synchronized void updateMetaData(GlobalSeqMessage globalSeqMessage,int lastGlobalSeqNo) {
		MessageRepository.getInstance().sequenceMessageList.add(globalSeqMessage);
		
		HashMap<Integer,Integer> senderMap = MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.get(globalSeqMessage.senderId);
		senderMap.put(globalSeqMessage.messageId, globalSeqMessage.globalSeqId);
		MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.put(globalSeqMessage.senderId, senderMap);
		
		MessageRepository.getInstance().deliveryQueue.add(globalSeqMessage);
		updateCompleteMessagesReceived(globalSeqMessage,lastGlobalSeqNo);
	}
	
	private synchronized void updateCompleteMessagesReceived(GlobalSeqMessage globalSeqMessage,int lastGlobalSeqNo) {
		ConcurrentHashMap<Integer,Integer> receivedCompleteMessages = globalSeqMessage.completeMessagesReceived;
		ConcurrentHashMap<Integer,Integer> currentCompleteMessages = MessageRepository.getInstance().completeMessagesReceived;
		for(int peerId: Properties.peers) {
			if(receivedCompleteMessages.get(peerId) > currentCompleteMessages.get(peerId)) {
				currentCompleteMessages.put(peerId, receivedCompleteMessages.get(peerId));
			}
		}
		int lastCompleteMessage = currentCompleteMessages.get(Properties.senderId);
		List<GlobalSeqMessage> sequenceMessages = MessageRepository.getInstance().sequenceMessageList;
		List<RequestMessage> requestMessages = MessageRepository.getInstance().requestMessageList;
		for(int i=lastCompleteMessage+1; i<= lastGlobalSeqNo; i++) {
			GlobalSeqMessage gSeqMessage = getSeqMessagePresent(sequenceMessages,i);
			if(gSeqMessage != null) {
				if(isReqMessagePresent(requestMessages,gSeqMessage.messageId,gSeqMessage.senderId)) {
					lastCompleteMessage = i;
				}else break;
			}else break;
		}
		currentCompleteMessages.put(Properties.senderId, lastCompleteMessage);
		MessageRepository.getInstance().completeMessagesReceived = currentCompleteMessages;
	}
	
	private GlobalSeqMessage getSeqMessagePresent(List<GlobalSeqMessage> list,int requiredIndex) {
		for(GlobalSeqMessage element: list) {
			if(element.globalSeqId == requiredIndex) {
				return element;
			}
		}
		return null;
	}
	
	private boolean isReqMessagePresent(List<RequestMessage> list,int requiredMessageId,int requiredSenderId) {
		for(RequestMessage element: list) {
			if(element.messageId == requiredMessageId && element.senderId == requiredSenderId) {
				return true;
			}
		}
		return false;
	}
	
	public void run() {
		try {
			this.IPAddress = InetAddress.getByName(Properties.hostIp);
			this.clientSocket = new DatagramSocket(Properties.seqUdpListenPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (listen) {
			try {

				GlobalSeqMessage globalSeqMessage = receivePacket();
				String messageString = new String(globalSeqMessage.messageBytes);
				if(messageString.equals(Properties.retransmitMessage)) {
					triggerRetransmit(globalSeqMessage);
					continue;
				}
				if (globalSeqMessage.globalSeqId > MessageRepository.getInstance().lastGlobalSeqNo) {
					if (globalSeqMessage.globalSeqId > MessageRepository.getInstance().lastGlobalSeqNo + 1) {
						for (int i = MessageRepository
								.getInstance().lastGlobalSeqNo+1; i <= globalSeqMessage.globalSeqId; i++) {
							GlobalMessHelper.getInstance().requestRetransmitGlobalSeqMessage(i);
						}
					}
					MessageRepository.getInstance().lastGlobalSeqNo = globalSeqMessage.globalSeqId;
					if (MessageRepository.getInstance().lastGlobalSeqNo
							% Properties.totalServers == Properties.moduloRequired) {
						MessageRepository.getInstance().assignGlobalSeq = true;
					} else {
						MessageRepository.getInstance().assignGlobalSeq = false;
					}
				}
				updateMetaData(globalSeqMessage,MessageRepository.getInstance().lastGlobalSeqNo);
				System.out.println("GlobalSeqMessage received"+" Global Seq Id: " + globalSeqMessage.globalSeqId+" Message Id: " + globalSeqMessage.messageId+"Sender Id: " + globalSeqMessage.senderId+" Message: " + new String(globalSeqMessage.messageBytes));
				System.out.println("Delivering Message: " + new String(globalSeqMessage.messageBytes));
				//TODO-add DB call
			} catch (Exception e) {
				e.printStackTrace();
				listen = false;
			}
		}
		closeSocket();
	}
	
	void triggerRetransmit(GlobalSeqMessage seqMessage) {
		try {
			int globalSeqId = seqMessage.globalSeqId;
			int senderId = seqMessage.senderId;
			List<GlobalSeqMessage> seqMessages = MessageRepository.getInstance().sequenceMessageList;
			for(GlobalSeqMessage gSeqMessage: seqMessages) {
				if(gSeqMessage.globalSeqId == globalSeqId) {
					byte[] seqBytes = convertToBytes(gSeqMessage);
					retransmitMessage(seqBytes,InetAddress.getByName(Properties.hostIp),Properties.seqListenPortMappoing.get(senderId));
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
