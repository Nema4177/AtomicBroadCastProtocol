package com.example.abp.controller;

import com.example.abp.background.Auditor;
import com.example.abp.config.StartThread;
import com.example.abp.helper.GlobalMessHelper;
import com.example.abp.helper.RequestMessHelper;
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	static Logger logger = Logger.getLogger(ABPController.class.getName());
	
	RequestMessHelper reqMessHelper = RequestMessHelper.getInstance();
	GlobalMessHelper globalMessHelper = GlobalMessHelper.getInstance();
	
	@Autowired
	public Auditor auditor;
	
	@PostConstruct
	void init() {
		auditor.start();
	}
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
		RequestMessage requestMessage = new RequestMessage(message.getBytes(),getNextId(),Properties.apiNumber);
		logger.info("Send Message constructed: Request Message Id: "+requestMessage.messageId+" Sender Id: "+requestMessage.senderId+" Messages is "+message);
        byte[] messageBytes = reqMessHelper.requestUdp.convertToBytes(requestMessage);
		reqMessHelper.requestUdp.sendPacketToAll(messageBytes);
		Properties.stateChange = true;
		entity.put("status", "success");
		return new ResponseEntity<JSONObject>(entity, HttpStatus.OK);
	}
	
	private int getNextId() {
		return ++requestMessageId;
	}
}
