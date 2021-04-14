package com.example.abp.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.abp.properties.Properties;

public class MessageRepository {
	
	public List<RequestMessage> requestMessageList = Collections.synchronizedList(new ArrayList<RequestMessage>());
	public List<GlobalSeqMessage> sequenceMessageList = Collections.synchronizedList(new ArrayList<GlobalSeqMessage>());
	//global seq messages delivered by the current application
	public List<Integer> deliveredGlobalSeqNos = Collections.synchronizedList(new ArrayList<Integer>());
    public List<GlobalSeqMessage> deliveryQueue = Collections.synchronizedList(new LinkedList<GlobalSeqMessage>());
	
	//list of global seq messages and corresponding request messages received by each candidate
	public ConcurrentHashMap<Integer,Integer> completeMessagesReceived = new ConcurrentHashMap<Integer,Integer>();	
	public ConcurrentHashMap<Integer,HashMap<Integer,Integer>> senderIdReqIdToGlobalSeqNoMap = new ConcurrentHashMap<Integer,HashMap<Integer,Integer>>();
    public ConcurrentHashMap<Integer,ArrayList<Integer>> serverToRequestMapping = new ConcurrentHashMap<Integer,ArrayList<Integer>>();

	public int lastGlobalSeqNo = 0;
	public boolean assignGlobalSeq = Properties.assignGlobalSeq;
    private static MessageRepository messageRepository;
	
	private MessageRepository() {
		for(int i=1;i<=Properties.totalServers;i++) {
			this.serverToRequestMapping.put(i,new ArrayList<>());
		}
		for(int i=1;i<=Properties.totalServers;i++) {
			this.completeMessagesReceived.put(i,0);
		}
		for(int i=1;i<=Properties.totalServers;i++) {
			this.senderIdReqIdToGlobalSeqNoMap.put(i,new HashMap<Integer,Integer>());
		}
	}
	
	public static MessageRepository getInstance() {
		
		if(messageRepository == null) {
			messageRepository = new MessageRepository();
		}
		return messageRepository;
	}

}
