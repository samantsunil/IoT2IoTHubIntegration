/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.iotclouddemoapp;

/**
 *
 * @author Sunil
 */
public class Device {
    
    public Device(){
        
    }
    private String deviceId;
    private String connectionString;
    private int messageSize;
    private int telemInterval;
    private String protocol;

    public Device(String deviceId, String connectionString, int messageSize, int telemInterval, String protocol) {
        this.deviceId = deviceId;
        this.connectionString = connectionString;
        this.messageSize = messageSize;
        this.telemInterval = telemInterval;
        this.protocol = protocol;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }

    public int getTelemInterval() {
        return telemInterval;
    }

    public void setTelemInterval(int telemInterval) {
        this.telemInterval = telemInterval;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    
    
}
