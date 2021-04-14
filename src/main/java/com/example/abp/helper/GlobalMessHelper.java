package com.example.abp.helper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;
import com.example.abp.udp.RequestUDP;
import com.example.abp.udp.SequenceUDP;

public class GlobalMessHelper {

	public SequenceUDP sequenceUdp;

	private static GlobalMessHelper globalMessHelper;

	public static GlobalMessHelper getInstance() {
		if (globalMessHelper == null)
			globalMessHelper = new GlobalMessHelper();
		return globalMessHelper;
	}

	public void sendGlobalSeqMessage(RequestMessage requestMessage) {
		int finishedGlobalSeqNo = MessageRepository.getInstance().lastGlobalSeqNo;
		while(!checkIfReceivedAllGlobalSeqMessagesTillNow(finishedGlobalSeqNo));
		if(!RequestMessHelper.getInstance().globalSeqNumbersForAllSenderMessagesLessThan(requestMessage.senderId,requestMessage.messageId)) {
			System.out.println("All prev messages of the sender does not have global seq number, hence not sending the message");
		};
		GlobalSeqMessage seqMessage = new GlobalSeqMessage(finishedGlobalSeqNo++,
				requestMessage.senderId, requestMessage.messageId, requestMessage.message);
		System.out.println("Glboal Seq Message constructed");
		System.out.println("Global Seq Message Id: " + seqMessage.globalSeqId);
		System.out.println("Sender Id: " + seqMessage.senderId);
		System.out.println("Message Id is: " + seqMessage.messageId);
		System.out.println("Message is: " + seqMessage.message);
		byte[] messageBytes = sequenceUdp.convertToBytes(seqMessage);
		sequenceUdp.sendPacketToAll(messageBytes);
		System.out.println("Global Seq Message sent, so delivering the message");
		System.out.println("Delivered message : " + seqMessage.message);
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
		int serverNumber = globalSeqId % Properties.totalServers;
		GlobalSeqMessage seqMessage = new GlobalSeqMessage(globalSeqId,
				Properties.senderId, 0, Properties.retransmitMessage);
		System.out.println("Retransmit message constructed - globalSeqId: "+globalSeqId);
		byte[] messageBytes = sequenceUdp.convertToBytes(seqMessage);
		try {
			int machinePort = Properties.seqListenPortMappoing.get(serverNumber);
			sequenceUdp.sendRetransmitRequest(messageBytes,InetAddress.getByName("localhost"),machinePort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

}
