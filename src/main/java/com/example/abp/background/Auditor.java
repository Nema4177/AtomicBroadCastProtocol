package com.example.abp.background;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.example.abp.helper.RequestMessHelper;
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.properties.Properties;

@Component
public class Auditor extends Thread{
	
	static Logger logger = Logger.getLogger(Auditor.class.getName());
	int run = 1;
	public void run() {
		try{
			while(true) {
				logger.info("Auditor run "+ run);
				run++;
				if(run%5 == 0 || Properties.stateChange) {
					printMetaData();
					Properties.stateChange = false;
				}
				checkForSeqMessage();
				checkForDelivery();
				Thread.sleep(10000);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
    }
	
	void checkForSeqMessage() {
		
	}
	
	void checkForDelivery() {
		List<GlobalSeqMessage> deliveryQueue = MessageRepository.getInstance().deliveryQueue;
		Iterator<GlobalSeqMessage> it = deliveryQueue.iterator();
		while(it.hasNext()) {
			GlobalSeqMessage seqMessage =  it.next();
			//TODO - execution of db calls go here
			if(checkIfPreviousSeqMessDelivery(seqMessage.globalSeqId)) {
				if(doMajorityHavePreviousCompleteMessages(seqMessage.globalSeqId)) {
					logger.info("################Delivering the message "+new String(seqMessage.messageBytes));
					MessageRepository.getInstance().deliveredGlobalSeqNos.add(seqMessage.globalSeqId);
					it.remove();
				}
			}
		}
	}
	
	boolean checkIfPreviousSeqMessDelivery(int currSeqNo) {
		List<Integer> deliveredSeqNos = MessageRepository.getInstance().deliveredGlobalSeqNos;
		for(int i=1; i<currSeqNo; i++) {
			if(!deliveredSeqNos.contains(i)) {
				logger.info("SeqNo: "+i+" is not delivered yet");
				return false;
			}
		}
		logger.info("All previous global seq messages are delivered");
		return true;
	}
	
	boolean doMajorityHavePreviousCompleteMessages(int currSeqNo) {
		logger.info("Checking if majority have messages till "+currSeqNo);
		int total = 0;
		for(int i=1; i<=Properties.totalServers; i++) {
			int index = MessageRepository.getInstance().completeMessagesReceived.get(i);
			if(index >= currSeqNo) {
				total++;
			}
		}
		if(total >= Properties.totalServers/2) {
			logger.info("More than half have complete messages till "+currSeqNo);
			return true;
		}
		logger.info("Less than half have complete messages till "+currSeqNo);
		return false;
	}
	
	void printMetaData() {
		MessageRepository messageRepository = MessageRepository.getInstance();
		System.out.println("Request Message List size is: "+ messageRepository.requestMessageList.size());
		System.out.println("Sequence Message List size is: "+messageRepository.sequenceMessageList.size());
		System.out.println("DeliveryQueue is: "+Arrays.toString(messageRepository.deliveryQueue.toArray()));
		System.out.println("Delivered GlobalSeqNos list is :"+Arrays.toString(messageRepository.deliveredGlobalSeqNos.toArray()));
		System.out.println("lastGlobalSeqNo is "+messageRepository.lastGlobalSeqNo);

		System.out.println("SenderIdReqIdGlobalSeqNo Map is");
		messageRepository.senderIdReqIdToGlobalSeqNoMap.entrySet().forEach(entry -> {
		    System.out.println(entry.getKey() + " " + entry.getValue());
		});
		
		System.out.println("serverToRequestMapping Map is");
		messageRepository.serverToRequestMapping.entrySet().forEach(entry -> {
		    System.out.println(entry.getKey() + " " + entry.getValue());
		});
		
		System.out.println("completeMessagesReceived Map is");
		messageRepository.completeMessagesReceived.entrySet().forEach(entry -> {
		    System.out.println(entry.getKey() + " " + entry.getValue());
		});
	}

}
