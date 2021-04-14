package com.example.abp.message;

import java.io.Serializable;

public class GlobalSeqMessage implements Serializable{
	public int globalSeqId;
    public int senderId;
    public int messageId;
    public String message;
    
    public GlobalSeqMessage(int globalSeqId, int senderId, int messageId, String message) {
    	this.globalSeqId = globalSeqId;
    	this.senderId = senderId;
    	this.messageId = messageId;
    	this.message = message;
    }
    
}
