/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.iotclouddemoapp;

import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;

/**
 *
 * @author Sunil
 */
public class DeviceTelemetryService {

    private static class IotHubEventCallbackImpl implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            System.out.println("Message received! Response status: " + responseStatus);
        }
    }

    public static void sendDeviceTelemetryToCloud(Device device) {
         
        
    }

}
