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
import com.example.abp.message.GlobalSeqMessage;
import com.example.abp.message.MessageRepository;
import com.example.abp.message.RequestMessage;
import com.example.abp.properties.Properties;

public class SequenceUDP extends Thread {

	InetAddress IPAddress;
	DatagramSocket clientSocket;
	private boolean listen = true;

	public SequenceUDP() {
		try {

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
			for (int peerServer : Properties.peers) {
				DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, IPAddress,
						Properties.requestListenPortMappoing.get(peerServer));
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

	public GlobalSeqMessage receivePacket() {
		GlobalSeqMessage message = null;
		try {
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
			message = (GlobalSeqMessage) in.readObject();
			in.close();
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
			this.clientSocket = new DatagramSocket(Properties.seqUdpListenPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (listen) {
			try {

				GlobalSeqMessage globalSeqMessage = receivePacket();
				if (globalSeqMessage.globalSeqId > MessageRepository.getInstance().lastGlobalSeqNo) {
					if (globalSeqMessage.globalSeqId > MessageRepository.getInstance().lastGlobalSeqNo + 1) {
						for (int i = MessageRepository
								.getInstance().lastGlobalSeqNo+1; i <= globalSeqMessage.globalSeqId; i++) {
							GlobalMessHelper.getInstance().requestRetransmitGlobalSeqMessage(i);
						}
					}
					MessageRepository.getInstance().lastGlobalSeqNo = globalSeqMessage.globalSeqId;
					if (MessageRepository.getInstance().lastGlobalSeqNo
							% Properties.totalServers == Properties.moduloRequired) {
						MessageRepository.getInstance().assignGlobalSeq = true;
					} else {
						MessageRepository.getInstance().assignGlobalSeq = false;
					}
				}
				System.out.println("GlobalSeqMessage received");
				System.out.println("Global Seq Id: " + globalSeqMessage.globalSeqId);
				System.out.println("Message Id: " + globalSeqMessage.messageId);
				System.out.println("Sender Id: " + globalSeqMessage.senderId);
				System.out.println("Message: " + globalSeqMessage.message);

				System.out.println("Delivering Message: " + globalSeqMessage.message);
			} catch (Exception e) {
				e.printStackTrace();
				listen = false;
			}
		}
		closeSocket();
	}
}
