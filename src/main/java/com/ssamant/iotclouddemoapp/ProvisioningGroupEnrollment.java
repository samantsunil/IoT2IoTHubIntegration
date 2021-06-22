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
import com.ssamant.dbservice.DBOperations;
import static com.ssamant.iotclouddemoapp.ProvisioningIndividualEnrollment.SYMMETRIC_KEY;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sunil
 */
public class ProvisioningGroupEnrollment {

    private static final String SCOPE_ID = ConnectionInfo.getScopeId();

    private static final String GLOBAL_ENDPOINT = ConnectionInfo.getGlobalEndPoint();

    private static final ProvisioningDeviceClientTransportProtocol PROVISIONING_DEVICE_CLIENT_TRANSPORT_PROTOCOL = ProvisioningDeviceClientTransportProtocol.HTTPS; //MQTT, AMQPS, MQTTT_WS, AMQPS_WS

    private static final int MAX_TIME_TO_WAIT_FOR_REGISTRATION = 10000; // in milliseconds

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

    /**
     * method to provision individual device under group enrollment DPS service.
     *
     * @param deviceNum
     * @param deviceOwner
     * @throws java.io.IOException
     */
    public static void beginDeviceProvisioningUnderGroupEnrollment(String deviceNum, String deviceOwner) throws IOException {

        String deviceId = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase() + deviceNum;

        String derivedSymmetricKeyPerDevice = new String(ComputeDerivedSymmetricKeyGroup.computePerDeviceSymmetricKeyForGroupEnrollment(deviceId), StandardCharsets.UTF_8);

        System.out.println("Starting...");
        System.out.println("Beginning setup.");
        SecurityProviderSymmetricKey securityClientSymmetricKey;

        DeviceClient deviceClient = null;

        byte[] derivedSymmKey = derivedSymmetricKeyPerDevice.getBytes(StandardCharsets.UTF_8);

        securityClientSymmetricKey = new SecurityProviderSymmetricKey(derivedSymmKey, deviceId);

        ProvisioningDeviceClient provisioningDeviceClient = null;

        try {

            ProvisioningStatus provisioningStatus = new ProvisioningStatus();

            provisioningDeviceClient = ProvisioningDeviceClient.create(GLOBAL_ENDPOINT, SCOPE_ID, PROVISIONING_DEVICE_CLIENT_TRANSPORT_PROTOCOL, securityClientSymmetricKey);

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

                System.out.println("IotHUb Uri : " + provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri());
                System.out.println("Device ID : " + provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getDeviceId());

                System.out.println("Primary key for the device: " + derivedSymmetricKeyPerDevice);

                Device device = new Device();
                device.setDeviceId(provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getDeviceId());
                device.setConnectionString(derivedSymmetricKeyPerDevice);
                device.setIotHubUri(provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri());
                device.setDeviceOwner(deviceOwner);
                DBOperations.newDeviceEntry(device);
                System.out.println("device info updated successfully into the resource database!");

                // block to send data to IoT Hub
                /*
                 // connect to iothub
                String iotHubUri = provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri();
                String deviceId = provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getDeviceId();
                try
                {
                    deviceClient = DeviceClient.createFromSecurityProvider(iotHubUri, deviceId, securityClientSymmetricKey, IotHubClientProtocol.MQTT);
                    deviceClient.open();
                    Message messageToSendFromDeviceToHub =  new Message("Whatever message you would like to send");

                    System.out.println("Sending message from device to IoT Hub...");
                    deviceClient.sendEventAsync(messageToSendFromDeviceToHub, new IotHubEventCallbackImpl(), null);
                }
                catch (IOException e)
                {
                    System.out.println("Device client threw an exception: " + e.getMessage());
                    if (deviceClient != null)
                    {
                        deviceClient.closeNow();
                    }
                }
                
                 */
            }

        } catch (ProvisioningDeviceClientException | InterruptedException ex) {
            Logger.getLogger(ProvisioningGroupEnrollment.class.getName()).log(Level.SEVERE, null, ex);

            if (provisioningDeviceClient != null) {
                provisioningDeviceClient.closeNow();
            }
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
