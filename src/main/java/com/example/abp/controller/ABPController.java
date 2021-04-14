package com.example.abp.controller;

import com.example.abp.helper.GlobalMessHelper;
import com.example.abp.helper.RequestMessHelper;
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;

import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ABPController {
	
	RequestMessHelper reqMessHelper = RequestMessHelper.getInstance();
	GlobalMessHelper globalMessHelper = GlobalMessHelper.getInstance();
	int requestMessageId=0;
		
	@GetMapping(path = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String test() {
		return "success";
	}
	
	@PostMapping(path = "/publish", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JSONObject> createAccount(@RequestBody JSONObject input) {
		JSONObject entity = new JSONObject();
		String message = (String) input.get("message");
		RequestMessage requestMessage = new RequestMessage(message,getNextId());
		System.out.println("Send Message constructed");
        System.out.println("Message Id: "+requestMessage.messageId);
        System.out.println("Sender Id: "+requestMessage.senderId);
        System.out.println("Message is: "+requestMessage.message);		
        byte[] messageBytes = reqMessHelper.requestUdp.convertToBytes(requestMessage);
		reqMessHelper.requestUdp.sendPacketToAll(messageBytes);
		 if(MessageRepository.getInstance().assignGlobalSeq == true) {
         	globalMessHelper.sendGlobalSeqMessage(requestMessage);
         }
		entity.put("status", "success");
		return new ResponseEntity<JSONObject>(entity, HttpStatus.OK);
	}
	
	private int getNextId() {
		return requestMessageId++;
	}
}
