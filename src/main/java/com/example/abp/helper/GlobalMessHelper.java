package com.example.abp.helper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.example.abp.controller.ABPController;
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;
import com.example.abp.udp.RequestUDP;
import com.example.abp.udp.SequenceUDP;

public class GlobalMessHelper {
	
	static Logger logger = Logger.getLogger(GlobalMessHelper.class.getName());

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
	
	public void sendGlobalSeqMessage(RequestMessage requestMessage) {
		int previousGlobalSeqNos = MessageRepository.getInstance().lastGlobalSeqNo;
		while(!checkIfReceivedAllGlobalSeqMessagesTillNow(previousGlobalSeqNos));
		int lastWithoutGlobalSeq = RequestMessHelper.getInstance().getLastWithoutSeqNumber(requestMessage.senderId,requestMessage.messageId);
		if( lastWithoutGlobalSeq < requestMessage.messageId) {
			requestMessage = RequestMessHelper.getInstance().getRequestMessage(requestMessage.senderId,lastWithoutGlobalSeq);
			if(requestMessage == null) {
				logger.info("RequestMessage returned is null");
				return;
			}
			logger.info("All prev messages of the sender does not have global seq number, hence retrieved older message");
		};
		
		Properties.stateChange = true;

		GlobalSeqMessage seqMessage = new GlobalSeqMessage(++previousGlobalSeqNos,
				requestMessage.senderId, requestMessage.messageId, requestMessage.messagebytes,requestMessage.apiNumber);
		logger.info("Glboal Seq Message constructed"+" Global Seq Message Id: " + seqMessage.globalSeqId+" Sender Id: " + seqMessage.senderId+ " Message Id is: " + seqMessage.messageId+" Message is: " + new String(seqMessage.messageBytes));
		byte[] messageBytes = sequenceUdp.convertToBytes(seqMessage);
		sequenceUdp.sendPacketToAll(messageBytes);
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
		logger.info("Retransmit request received for glboal seq id: "+globalSeqId);
		int serverNumber = globalSeqId % Properties.totalServers+1;
		GlobalSeqMessage seqMessage = new GlobalSeqMessage(globalSeqId,
				Properties.senderId, 0, Properties.retransmitMessage.getBytes(),Properties.apiNumber);
		logger.info("Retransmit message constructed - globalSeqId: "+globalSeqId);
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
