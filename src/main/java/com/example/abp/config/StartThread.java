package com.example.abp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.abp.background.Auditor;

public class StartThread {
	
	public Auditor auditor;
	
	private StartThread() {
		auditor.start();
	}
}
