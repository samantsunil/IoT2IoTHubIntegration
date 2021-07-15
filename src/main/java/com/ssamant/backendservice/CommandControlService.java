/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.backendservice;

import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceMethod;
import com.microsoft.azure.sdk.iot.service.devicetwin.MethodResult;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import com.ssamant.connectioninfo.ConnectionInfo;
import static com.ssamant.iotclouddemoapp.MainForm.lblReadControlMsg;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the methods to pass the payload to control the
 * end-device telemetry operations - depending upon the implemented task in the
 * provided method. The method name provided to send the payload should match
 * the method name implemented in the end device application.
 *
 * @author Sunil
 */
public class CommandControlService {

    private static String iotHubConnString = ConnectionInfo.getIoTHubConnectionString();

    private static final Long RESPONSE_TIMEOUT = TimeUnit.SECONDS.toSeconds(30);
    private static final Long CONNECT_TIMEOUT = TimeUnit.SECONDS.toSeconds(5);

    public static void controlTelemetryInterval(String methodName, String deviceId, Object payload) {

        try {
            System.out.println("Calling direct method...");
            DeviceMethod directMethodInstance = new DeviceMethod(iotHubConnString);

            // Call the direct method.
            //https://<iothuburi>/twins/<device_id>/methods?api-version=2018-06-30  - REST API url
            MethodResult result = directMethodInstance.invoke(deviceId, methodName, RESPONSE_TIMEOUT, CONNECT_TIMEOUT, payload);
            
            if (result == null) {
                throw new IOException("Direct method invoke returns null");
            }

            // Show the acknowledgement from the device.
            System.out.println("Status: " + result.getStatus());
            System.out.println("Response: " + result.getPayload());
            System.out.println("Control command successfully delivered and applied on the selected device!");
            lblReadControlMsg.setText("control command sent to the selected device and device responded.");
        } catch (IOException | IotHubException ex) {
            System.out.println("Unable to connect to IoT Hub or calling direct method on the device. Details: " + ex.getMessage());
        }
    }

}
