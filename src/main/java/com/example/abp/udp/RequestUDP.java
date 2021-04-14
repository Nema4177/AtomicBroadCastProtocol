package com.example.abp.udp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.example.abp.helper.GlobalMessHelper;
import com.example.abp.helper.RequestMessHelper;
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;

public class RequestUDP extends Thread{

	InetAddress IPAddress;
	DatagramSocket clientSocket ;
	private boolean listen = true;
	GlobalMessHelper globalMessHelper = GlobalMessHelper.getInstance();
	public byte[] convertToBytes(Object object) {
		byte[] serializedMessage = null;
		try {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			ObjectOutput oo = new ObjectOutputStream(bStream);
			oo.writeObject(object);
			oo.close();
			serializedMessage = bStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return serializedMessage;
	}

	public void sendPacketToAll(byte[] messageBytes) {
		try {
			for(int peerServer: Properties.peers) {
				DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, IPAddress, Properties.requestListenPortMappoing.get(peerServer));
				clientSocket.send(sendPacket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendRetransmitRequest(byte[] messageBytes, InetAddress ipAddress, int udpPort) {
		try {
			DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, ipAddress, udpPort);
			clientSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public RequestMessage receivePacket() {
		RequestMessage message=null;
		try {
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
			message = (RequestMessage) in.readObject();
			in.close();
			//returnString = new String(receivePacket.getData());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

	void closeSocket() {
		clientSocket.close();
	}
	
	public void run() {
		try {
			this.IPAddress = InetAddress.getByName(Properties.hostIp);
			this.clientSocket = new DatagramSocket(Properties.requestUdpListenPort);
		}catch(Exception e){
			e.printStackTrace();
		}
        while (listen) {
            try {

            	RequestMessage requestMessage = receivePacket();
                System.out.println("Request Message received");
                System.out.println("Message Id: "+requestMessage.messageId);
                System.out.println("Sender Id: "+requestMessage.senderId);
                System.out.println("Message is: "+requestMessage.message);
                int lastMessageFromServer = MessageRepository.getInstance().serverToRequestMapping.get(requestMessage.senderId);
                if(requestMessage.messageId > lastMessageFromServer+1) {
                	for(int i = requestMessage.messageId+1; i<requestMessage.messageId; i++) {
                    	RequestMessHelper.getInstance().requestRetransmitReqMessage(requestMessage.senderId, i);
                	}
                }
                if(MessageRepository.getInstance().assignGlobalSeq == true) {
                	globalMessHelper.sendGlobalSeqMessage(requestMessage);
                }     
            } catch (Exception e) {
                e.printStackTrace();
                listen = false;
            }
        }
        closeSocket();
    }
}
