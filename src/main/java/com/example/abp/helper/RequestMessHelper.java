package com.example.abp.helper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;
import com.example.abp.udp.RequestUDP;
import com.example.abp.udp.SequenceUDP;

public class RequestMessHelper {
	
	static Logger logger = Logger.getLogger(RequestMessHelper.class.getName());

	public RequestUDP requestUdp;
	
	private static RequestMessHelper requestHelper;
	
	public static RequestMessHelper getInstance() {
		if(requestHelper == null)
			requestHelper = new RequestMessHelper();
		return requestHelper;
	}
	
	private RequestMessHelper() {
		requestUdp = new RequestUDP();
		requestUdp.start();
	}

	public int getLastWithoutSeqNumber(int sender,int currLocalSeqNo) {
		HashMap<Integer,Integer> senderMap = MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.get(sender);
		if(senderMap == null){
			senderMap = new HashMap<Integer,Integer>();
			MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.put(sender, senderMap);
			return 1;
		}
		for(int i=1; i<currLocalSeqNo; i++) {
			if( MessageRepository.getInstance().senderIdReqIdToGlobalSeqNoMap.get(sender).get(i) == null) {
				logger.info("No glboal seq no assigned for sender: "+sender+" messageId: "+i);
				return i;
			}
		}
		return currLocalSeqNo;
	}
	
	public RequestMessage getRequestMessage(int senderId,int localSeqNo) {
		RequestMessage requestMessage = null;
		for(RequestMessage req: MessageRepository.getInstance().requestMessageList) {
			if(req.senderId == senderId && req.messageId == localSeqNo) {
				return req;
			}
		}
		return requestMessage;
	}
	
	public void requestRetransmitReqMessage(int serverId, int requestId) {
		logger.info("Retransmit request received for server id: "+serverId+" requestId: "+requestId);
		RequestMessage seqMessage = new RequestMessage(Properties.retransmitMessage.getBytes(),Properties.senderId,Properties.apiNumber);
		byte[] messageBytes = requestUdp.convertToBytes(seqMessage);
		try {
			int machinePort = Properties.requestListenPortMappoing.get(serverId);
			requestUdp.sendRetransmitRequest(messageBytes,InetAddress.getByName("localhost"),machinePort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
}
