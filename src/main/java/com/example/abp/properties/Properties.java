package com.example.abp.properties;

import java.util.ArrayList;
import java.util.HashMap;

public class Properties {
	
	public static final boolean assignGlobalSeq = true;
	public static final int senderId = 1;
	public static final int requestUdpListenPort = 2000;
	public static final int seqUdpListenPort = 3000;
	public static final String hostIp = "localhost";
	public static final int totalServers = 1;
	public static final int moduloRequired = 0;
	public static final String retransmitMessage = "retransmit message";
	public static final ArrayList<Integer> peers = new ArrayList<Integer>() {{
	}};
	public static final HashMap<Integer,Integer> requestListenPortMappoing = new HashMap<Integer,Integer>(){
		{
			put(1,2000);
			//put(2,2001);
		}
	};
	public static final HashMap<Integer,Integer> seqListenPortMappoing = new HashMap<Integer,Integer>(){
		{
			put(1,3000);
			//put(2,3001);
		}
	};
	public static final int apiNumber = 1;
}
