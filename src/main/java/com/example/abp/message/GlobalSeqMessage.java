package com.example.abp.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalSeqMessage implements Serializable{
	
	public int globalSeqId;
    public int senderId;
    public int messageId;
    public byte[] messageBytes;
    public int apiNum;
    public ConcurrentHashMap<Integer,Integer> completeMessagesReceived;
    
    public GlobalSeqMessage(int globalSeqId, int senderId, int messageId, byte[] messageBytes,int apiNum) {
    	this.globalSeqId = globalSeqId;
    	this.senderId = senderId;
    	this.messageId = messageId;
    	this.messageBytes = messageBytes;
    	this.completeMessagesReceived = MessageRepository.getInstance().completeMessagesReceived;
    	this.apiNum = apiNum;
    }
    
}
