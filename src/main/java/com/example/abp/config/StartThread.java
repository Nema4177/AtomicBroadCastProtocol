package com.example.abp.config;

import com.example.abp.background.Auditor;
import com.example.abp.udp.RequestUDP;
import com.example.abp.udp.SequenceUDP;

public class StartThread {
	
	public Auditor auditor;
	private static StartThread singletonInstance;
	
	private StartThread() {
		auditor = new Auditor();
		auditor.start();
	}
	
	public static StartThread getInstance() {
		if (singletonInstance == null)
			singletonInstance = new StartThread();
		return singletonInstance;
	}
}
