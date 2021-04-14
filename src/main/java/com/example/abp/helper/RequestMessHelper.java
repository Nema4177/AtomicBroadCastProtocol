package com.example.abp.helper;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;
import com.example.abp.udp.RequestUDP;
import com.example.abp.udp.SequenceUDP;

public class RequestMessHelper {

	public RequestUDP requestUdp;
	
	private static RequestMessHelper requestHelper;
	
	public static RequestMessHelper getInstance() {
		if(requestHelper == null)
			requestHelper = new RequestMessHelper();
		return requestHelper;
	}

	public boolean globalSeqNumbersForAllSenderMessagesLessThan(int sender,int currLocalSeqNo) {
		for(int i=1; i<currLocalSeqNo; i++) {
			if( MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.get(sender).get(i) == null) {
				System.out.println("No glboal seq no assigned for sender: "+sender+" messageId: "+i);
				return false;
			}
		}
		return true;
	}
	
	public void requestRetransmitReqMessage(int serverId, int requestId) {
		System.out.println("Retransmit request received for server id: "+serverId+" requestId: "+requestId);
		RequestMessage seqMessage = new RequestMessage(Properties.retransmitMessage,Properties.senderId);
		byte[] messageBytes = requestUdp.convertToBytes(seqMessage);
		try {
			int machinePort = Properties.requestListenPortMappoing.get(serverId);
			requestUdp.sendRetransmitRequest(messageBytes,InetAddress.getByName("localhost"),machinePort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
}
