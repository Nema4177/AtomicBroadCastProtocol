package com.example.abp.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

import com.example.abp.properties.Properties;

public class RequestMessage implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int senderId = Properties.senderId;
    //public String message;
    public byte[] messagebytes;
    public int messageId;
    public int apiNumber;
    public ConcurrentHashMap<Integer,Integer> completeMessagesReceived;
    
    public RequestMessage(byte[] messageBytes,int messageId,int api) {
    	this.messagebytes=messageBytes;
    	this.messageId = messageId;
    	this.apiNumber = api;
    	this.completeMessagesReceived = MessageRepository.getInstance().completeMessagesReceived;
    }
}
