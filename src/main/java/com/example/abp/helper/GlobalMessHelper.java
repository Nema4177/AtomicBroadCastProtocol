package com.example.abp.helper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;
import com.example.abp.udp.RequestUDP;
import com.example.abp.udp.SequenceUDP;

public class GlobalMessHelper {

	public SequenceUDP sequenceUdp;

	private static GlobalMessHelper globalMessHelper;
	
	private GlobalMessHelper() {
		sequenceUdp = new SequenceUDP();
		sequenceUdp.start();
	}
	
	public static GlobalMessHelper getInstance() {
		if (globalMessHelper == null)
			globalMessHelper = new GlobalMessHelper();
		return globalMessHelper;
	}
	
	public synchronized void updateMessageRepository(GlobalSeqMessage seqMessage,RequestMessage requestMessage,int finishedGlobalSeqNo) {
		
		HashMap<Integer,Integer> senderMap = MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.get(requestMessage.senderId);
		if(senderMap == null){
			senderMap = new HashMap<Integer,Integer>();
		}
		senderMap.put(requestMessage.messageId, finishedGlobalSeqNo);
		MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.put(requestMessage.senderId, senderMap);
		MessageRepository.getInstance().sequenceMessageList.add(seqMessage);
		MessageRepository.getInstance().deliveryQueue.add(seqMessage);
		MessageRepository.getInstance().lastGlobalSeqNo = finishedGlobalSeqNo;
		updateCompleteMessagesReceived(seqMessage,finishedGlobalSeqNo);
		
	}
	
	private synchronized void updateCompleteMessagesReceived(GlobalSeqMessage globalSeqMessage,int lastGlobalSeqNo) {
		ConcurrentHashMap<Integer,Integer> currentCompleteMessages = MessageRepository.getInstance().completeMessagesReceived;
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
	
	public void sendGlobalSeqMessage(RequestMessage requestMessage) {
		int finishedGlobalSeqNo = MessageRepository.getInstance().lastGlobalSeqNo;
		while(!checkIfReceivedAllGlobalSeqMessagesTillNow(finishedGlobalSeqNo));
		int lastWithoutGlobalSeq = RequestMessHelper.getInstance().getLastWithoutSeqNumber(requestMessage.senderId,requestMessage.messageId);
		if( lastWithoutGlobalSeq < requestMessage.messageId) {
			requestMessage = RequestMessHelper.getInstance().getRequestMessage(requestMessage.senderId,lastWithoutGlobalSeq);
			System.out.println("All prev messages of the sender does not have global seq number, hence retrieved older message");
		};
		GlobalSeqMessage seqMessage = new GlobalSeqMessage(++finishedGlobalSeqNo,
				requestMessage.senderId, requestMessage.messageId, requestMessage.messagebytes);
		System.out.println("Glboal Seq Message constructed"+" Global Seq Message Id: " + seqMessage.globalSeqId+" Sender Id: " + seqMessage.senderId+ " Message Id is: " + seqMessage.messageId+" Message is: " + new String(seqMessage.messageBytes));
		byte[] messageBytes = sequenceUdp.convertToBytes(seqMessage);
		sequenceUdp.sendPacketToAll(messageBytes);

		System.out.println("Global Seq Message sent, so delivering the message");
		System.out.println("Delivered message : " + new String(seqMessage.messageBytes));
		
		updateMessageRepository(seqMessage,requestMessage,finishedGlobalSeqNo);
		MessageRepository.getInstance().assignGlobalSeq = false;
	}
	
	public boolean checkIfReceivedAllGlobalSeqMessagesTillNow(int lastGlobalSeqNo) {
		List<GlobalSeqMessage>  seqMessages = MessageRepository.getInstance().sequenceMessageList;
		boolean allPresent = true;
		boolean[] isPresent = new boolean[lastGlobalSeqNo+1];
		for(GlobalSeqMessage seqMessage: seqMessages) {
			int globalSeqId = seqMessage.globalSeqId;
			isPresent[globalSeqId] = true;
		}
		for(int i=1;i<=lastGlobalSeqNo;i++) {
			if(!isPresent[i]) {
				allPresent=false;
				requestRetransmitGlobalSeqMessage(i);
			}
		}
		return allPresent;
	}
	
	public void requestRetransmitGlobalSeqMessage(int globalSeqId) {
		System.out.println("Retransmit request received for glboal seq id: "+globalSeqId);
		int serverNumber = globalSeqId % Properties.totalServers+1;
		GlobalSeqMessage seqMessage = new GlobalSeqMessage(globalSeqId,
				Properties.senderId, 0, Properties.retransmitMessage.getBytes());
		System.out.println("Retransmit message constructed - globalSeqId: "+globalSeqId);
		byte[] messageBytes = sequenceUdp.convertToBytes(seqMessage);
		try {
			int machinePort = Properties.seqListenPortMappoing.get(serverNumber);
			sequenceUdp.sendRetransmitRequest(messageBytes,InetAddress.getByName("localhost"),machinePort);
			Thread.sleep(5000);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean hasGlobalSeqNo(RequestMessage requestMessage) {
		HashMap<Integer,Integer> senderMap = MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.get(requestMessage.senderId);
		if(senderMap.get(requestMessage.messageId) == null) return false;
		return true;
	}

}
