package com.example.abp.config;

import com.example.abp.udp.RequestUDP;
import com.example.abp.udp.SequenceUDP;

public class StartThread {
	
	public SequenceUDP sequenceUdp;
	public RequestUDP requestUdp;
	private static StartThread singletonInstance;
	
	private StartThread() {
		requestUdp = new RequestUDP();
		requestUdp.start();
		sequenceUdp = new SequenceUDP();
		sequenceUdp.start();
	}
	
	public static StartThread getInstance() {
		if (singletonInstance == null)
			singletonInstance = new StartThread();
		return singletonInstance;
	}
}
