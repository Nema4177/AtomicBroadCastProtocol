package com.example.abp.message;

import java.io.Serializable;

import org.json.simple.JSONObject;

import com.example.abp.properties.Properties;

public class RequestMessage implements Serializable{
    public int senderId = Properties.senderId;
    public String message;
    public int messageId;
    public JSONObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JSONObject metadata) {
        this.metadata = metadata;
    }

    private JSONObject metadata;
    
    public RequestMessage(String mess,int messageId) {
    	this.message=mess;
    	this.messageId = messageId;
    }
}
