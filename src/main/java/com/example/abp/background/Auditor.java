package com.example.abp.background;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.example.abp.helper.RequestMessHelper;
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;

@Component
public class Auditor extends Thread{
	
	static Logger logger = Logger.getLogger(Auditor.class.getName());
	int run = 1;
	public void run() {
		try{
			while(true) {
				checkForSeqMessage();
				checkForDelivery();
				Thread.sleep(5000);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
    }
	
	void checkForSeqMessage() {
		
	}
	
	void checkForDelivery() {
		logger.info("Auditor run "+ run);
		run++;
		List<GlobalSeqMessage> deliveryQueue = MessageRepository.getInstance().deliveryQueue;
		Iterator<GlobalSeqMessage> it = deliveryQueue.iterator();
		while(it.hasNext()) {
			GlobalSeqMessage seqMessage =  it.next();
			//TODO - execution of db calls go here
			//TODO - need to add conditions for delivery
			logger.info("Delivering the message "+new String(seqMessage.messageBytes));
			MessageRepository.getInstance().deliveredGlobalSeqNos.add(seqMessage.globalSeqId);
			it.remove();
		}
	}

}
