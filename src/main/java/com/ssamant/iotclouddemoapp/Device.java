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

    private String deviceId;
    private String connectionString;
    private String messageSize;
    private String telemInterval;
    private String protocol;
    private String iotHubUri;
    private String deviceOwner;


    public Device() {

    }

    public Device(String deviceId, String connectionString, String iotHubUri, String messageSize, String telemInterval, String protocol, String deviceOwner) {

        this.deviceId = deviceId;
        this.connectionString = connectionString;
        this.iotHubUri = iotHubUri;
        this.messageSize = messageSize;
        this.telemInterval = telemInterval;
        this.protocol = protocol;
        this.deviceOwner = deviceOwner;
    }

    public String getDeviceOwner() {
        return deviceOwner;
    }

    public void setDeviceOwner(String deviceOwner) {
        this.deviceOwner = deviceOwner;
    }

    public String getIotHubUri() {
        return iotHubUri;
    }

    public void setIotHubUri(String iotHubUri) {
        this.iotHubUri = iotHubUri;
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

    public String getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(String messageSize) {
        this.messageSize = messageSize;
    }

    public String getTelemInterval() {
        return telemInterval;
    }

    public void setTelemInterval(String telemInterval) {
        this.telemInterval = telemInterval;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void updateTelemInterval(int value) {
        int oldValue = Integer.parseInt(this.telemInterval);
        int newValue = oldValue * value;
        this.telemInterval = String.valueOf(newValue);
    }
    
       // Specify the telemetry to send to your IoT hub.

 
    

}
