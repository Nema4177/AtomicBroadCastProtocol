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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.example.abp.helper.GlobalMessHelper;
import com.example.abp.helper.RequestMessHelper;
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;

public class RequestUDP extends Thread{
	
	static Logger logger = Logger.getLogger(RequestUDP.class.getName());

	InetAddress IPAddress;
	DatagramSocket clientSocket ;
	private boolean listen = true;
	GlobalMessHelper globalMessHelper = GlobalMessHelper.getInstance();
	
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
			for(int i=1; i<=Properties.totalServers; i++) {
				DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, IPAddress, Properties.requestListenPortMappoing.get(i));
				clientSocket.send(sendPacket);
			}
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
	
	public void retransmitMessage(byte[] messageBytes, InetAddress ipAddress, int udpPort) {
		try {
			DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, ipAddress, udpPort);
			clientSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public RequestMessage receivePacket() {
		RequestMessage message=null;
		try {
			byte[] receiveData = new byte[2000];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
			message = (RequestMessage) in.readObject();
			in.close();
			//returnString = new String(receivePacket.getData());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

	void closeSocket() {
		clientSocket.close();
	}
	
	private synchronized void updateCompleteMessagesReceived(RequestMessage requestMessage,int lastGlobalSeqNo) {
		ConcurrentHashMap<Integer,Integer> receivedCompleteMessages = requestMessage.completeMessagesReceived;
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
	
	private int getLastMessage(int senderId) {
		List<Integer> reqMessageIdList = MessageRepository.getInstance().serverToRequestMapping.get(senderId);
		int lastMessage=0;
		for(int reqMessageId: reqMessageIdList) {
			if(reqMessageId > lastMessage) {
				lastMessage = reqMessageId;
			}
		}
		return lastMessage;
	}
	
	private void updateSenderReqList(RequestMessage requestMessage) {
		ArrayList<Integer> reqMessageIdList = MessageRepository.getInstance().serverToRequestMapping.get(requestMessage.senderId);
		logger.info("Updating reqSenderId: "+requestMessage.senderId+" reqMessageId: "+requestMessage.messageId);
		reqMessageIdList.add(requestMessage.messageId);
		MessageRepository.getInstance().serverToRequestMapping.put(requestMessage.senderId, reqMessageIdList);
	}
	
	public void run() {
		try {
			this.IPAddress = InetAddress.getByName(Properties.hostIp);
			this.clientSocket = new DatagramSocket(Properties.requestUdpListenPort);
		}catch(Exception e){
			e.printStackTrace();
		}
        while (listen) {
            try {

            	RequestMessage requestMessage = receivePacket();
				Properties.stateChange = true;
            	String messageString = new String(requestMessage.messagebytes);
				if(messageString.equals(Properties.retransmitMessage)) {
					triggerRetransmit(requestMessage);
					continue;
				}
				logger.info("Request Message received, Message Id: "+requestMessage.messageId+" Sender Id: "+requestMessage.senderId+" Message is: "+new String(requestMessage.messagebytes));
                
                int lastMessageFromServer = getLastMessage(requestMessage.senderId);
                updateSenderReqList(requestMessage);
                updateCompleteMessagesReceived(requestMessage,MessageRepository.getInstance().lastGlobalSeqNo);
                if(requestMessage.messageId > lastMessageFromServer+1) {
                	for(int i = requestMessage.messageId+1; i<requestMessage.messageId; i++) {
                    	RequestMessHelper.getInstance().requestRetransmitReqMessage(requestMessage.senderId, i);
                	}
                }
                MessageRepository.getInstance().requestMessageList.add(requestMessage);
                if(MessageRepository.getInstance().assignGlobalSeq == true) {
                	if(!globalMessHelper.hasGlobalSeqNo(requestMessage)) {
                    	globalMessHelper.sendGlobalSeqMessage(requestMessage);
                	}
                }     
            } catch (Exception e) {
                e.printStackTrace();
                listen = false;
            }
        }
        closeSocket();
    }
	
	void triggerRetransmit(RequestMessage requestMessage) {
		try {
			int senderId = requestMessage.senderId;
			int messageId = requestMessage.messageId;
			List<RequestMessage> reqMessages = MessageRepository.getInstance().requestMessageList;
			for(RequestMessage reqMessage: reqMessages) {
				if(reqMessage.senderId == senderId && reqMessage.messageId == messageId) {
					byte[] seqBytes = convertToBytes(reqMessage);
					retransmitMessage(seqBytes,InetAddress.getByName(Properties.hostIp),Properties.requestListenPortMappoing.get(senderId));
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
