package com.example.abp.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.example.abp.properties.Properties;

public class MessageRepository {
	
	public List<RequestMessage> requestMessageList = new LinkedList<RequestMessage>();
	public List<GlobalSeqMessage> sequenceMessageList = new LinkedList<GlobalSeqMessage>();
	
	/*below two can be used to determine delivery of messages*/
	
	//global seq messages delivered by the current application
	public List<Integer> deliveredGlobalSeqNos = new ArrayList<Integer>();
	//list of global seq messages and corresponding request messages received by each candidate
	public Map<Integer,ArrayList<Integer>> completeMessagesReceived = new HashMap<Integer,ArrayList<Integer>>();
	
	/*below two can be used to check conditions to send out a sequence message*/
	
	public Map<Integer,HashMap<Integer,Integer>> senderIdReqIdToGlobalSeqNoMap = new HashMap<Integer,HashMap<Integer,Integer>>();
	public Map<Integer,HashMap<Integer,Integer>>  GlobalSeqNoToSenderIdToReqIdMap = new HashMap<Integer,HashMap<Integer,Integer>>();
	
	public int lastGlobalSeqNo = 0;
	public boolean assignGlobalSeq = Properties.assignGlobalSeq;
    private static MessageRepository messageRepository;
    
    public LinkedList<RequestMessage> queuedForAssigningGlboalSeq = new LinkedList<RequestMessage>();
    public HashMap<Integer,Integer> serverToRequestMapping = new HashMap<Integer,Integer>();
	
	private MessageRepository() {
		for(int i=1;i<=Properties.totalServers;i++) {
			this.serverToRequestMapping.put(i,0);
		}
	}
	
	public static MessageRepository getInstance() {
		
		if(messageRepository == null) {
			messageRepository = new MessageRepository();
		}
		return messageRepository;
	}

}
