package com.example.abp.background;

import java.util.Iterator;
import java.util.List;

import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;

public class Auditor extends Thread{
	
	public void run() {
		try{
			checkForSeqMessage();
			checkForDelivery();
			Thread.sleep(5000);
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
			//TODO - need to add conditions for delivery
			System.out.println("Delivering the message "+new String(seqMessage.messageBytes));
			MessageRepository.getInstance().deliveredGlobalSeqNos.add(seqMessage.globalSeqId);
			it.remove();
		}
	}

}
