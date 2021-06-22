/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.iotclouddemoapp;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.provisioning.device.*;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceClientException;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProviderSymmetricKey;
import com.ssamant.connectioninfo.ConnectionInfo;
//import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceClientException;
//import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProviderSymmetricKey;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Symmetric Key authenticated individual enrollment
 *
 * @author Sunil
 */
public class ProvisioningIndividualEnrollment {

    static final String SCOPE_ID = ConnectionInfo.getScopeId();
    static final String GLOBAL_ENDPOINT = ConnectionInfo.getGlobalEndPoint();
    static final String SYMMETRIC_KEY = ConnectionInfo.getSymmetricKeyIndividual();
    static final String REGISTRATION_ID = ConnectionInfo.getRegIdIndividual();

    private static final ProvisioningDeviceClientTransportProtocol PROV_DEV_CLIENT_TRANSPORT_PROTOCOL = ProvisioningDeviceClientTransportProtocol.HTTPS; //AMQPS, MQTT, MQTT_WS, AMQP_WS

    private static final int MAX_TIME_TO_WAIT_FOR_REGISTRATION = 10000; //in ms

    static class ProvisioningStatus {

        ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationInfoClient = new ProvisioningDeviceClientRegistrationResult();
        Exception exception;
    }

    static class ProvisioningDeviceClientRegistrationCallbackImpl implements ProvisioningDeviceClientRegistrationCallback {

        @Override
        public void run(ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationResult, Exception exception, Object context) {
            if (context instanceof ProvisioningStatus) {
                ProvisioningStatus status = (ProvisioningStatus) context;
                status.provisioningDeviceClientRegistrationInfoClient = provisioningDeviceClientRegistrationResult;
                status.exception = exception;
            } else {
                System.out.println("Received unknown context");
            }
        }
    }

    private static class IotHubEventCallbackImpl implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            System.out.println("Message received! Response status: " + responseStatus);
        }
    }

    public static void beginIndividualDeviceRegistration() throws IOException {

        System.out.println("Starting...");
        System.out.println("Beginning setup.");
        SecurityProviderSymmetricKey securityClientSymmetricKey;
        //Scanner scanner = new Scanner(System.in);
        DeviceClient deviceClient = null;

        securityClientSymmetricKey = new SecurityProviderSymmetricKey(SYMMETRIC_KEY.getBytes(), REGISTRATION_ID); //connection string for device to connect to IoT Hub

        ProvisioningDeviceClient provisioningDeviceClient = null;

        try {

            ProvisioningStatus provisioningStatus = new ProvisioningStatus();

            provisioningDeviceClient = ProvisioningDeviceClient.create(GLOBAL_ENDPOINT, SCOPE_ID, PROV_DEV_CLIENT_TRANSPORT_PROTOCOL, securityClientSymmetricKey);

            provisioningDeviceClient.registerDevice(new ProvisioningDeviceClientRegistrationCallbackImpl(), provisioningStatus);

            while (provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() != ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED) {

                if (provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ERROR
                        || provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_DISABLED
                        || provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_FAILED) {
                    provisioningStatus.exception.printStackTrace();
                    System.out.println("Registration error, bailing out");
                    break;
                }
                System.out.println("Waiting for Provisioning Service to register");
                Thread.sleep(MAX_TIME_TO_WAIT_FOR_REGISTRATION);
            }

            if (provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED) {

                System.out.println("IoTHUb Uri : " + provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri());
                System.out.println("Device ID : " + provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getDeviceId());
                

                //block to test telemetry send to cloud 
                // connect to iothub
//                String iotHubUri = provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri();
//                String deviceId = provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getDeviceId();
//                try {
//                    
//                    deviceClient = DeviceClient.createFromSecurityProvider(iotHubUri, deviceId, securityClientSymmetricKey, IotHubClientProtocol.MQTT);
//                    deviceClient.open();
//                    Message messageToSendFromDeviceToHub = new Message("this is test data");
//
//                    System.out.println("Sending message from device to IoT Hub...");
//                    deviceClient.sendEventAsync(messageToSendFromDeviceToHub, new IotHubEventCallbackImpl(), null);
//                } 
//                catch (IOException e) {
//                    
//                    System.out.println("Device client threw an exception: " + e.getMessage());
//                    if (deviceClient != null) {
//                        deviceClient.closeNow();
//                    }
//                } catch (URISyntaxException ex) {
//                    Logger.getLogger(ProvisioningIndividualEnrollment.class.getName()).log(Level.SEVERE, null, ex);
//                }
            }

        } catch (ProvisioningDeviceClientException | InterruptedException ex) {

            System.out.println("Provisioning Device Client threw an exception" + ex.getMessage());
            if (provisioningDeviceClient != null) {
                provisioningDeviceClient.closeNow();
            }
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ProvisioningIndividualEnrollment.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (provisioningDeviceClient != null) {
            provisioningDeviceClient.closeNow();
        }

        if (deviceClient != null) {
            deviceClient.closeNow();
        }
        System.out.println("Shutting down...");
    }
}
