/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.iotclouddemoapp;

import com.google.gson.Gson;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.ssamant.dbservice.DBOperations;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Sunil
 */
public class DeviceTelemetryService {

    private static final int METHOD_SUCCESS = 200;
    private static final int METHOD_NOT_DEFINED = 404;
    private static final int INVALID_PARAMETER = 400;
    private static int duration = 0;
    private static int interval = 0;
    private static DeviceClient client;

    private static String devID = "";

    private static class IotHubEventCallbackImpl implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            System.out.println("Message received! Response status: " + responseStatus);
        }
    }

    private static class TelemetryDataPoint {

        public String deviceId;
        public double temperature;
        public double humidity;
        public double latitude;
        public double longitude;
        public String pointInfo;

        // Serialize object to JSON format.
        public String serialize() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }
    // Print the acknowledgement received from IoT Hub for the method acknowledgement sent.

    protected static class DirectMethodStatusCallback implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode status, Object context) {
            System.out.println("Direct method # IoT Hub responded to device method acknowledgement with status: " + status.name());
        }
    }

    // Print the acknowledgement received from IoT Hub for the telemetry message sent.
    private static class EventCallback implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode status, Object context) {
            System.out.println("IoT Hub responded to message with status: " + status.name());

            if (context != null) {
                synchronized (context) {
                    context.notify();
                }
            }
        }
    }

    protected static class DirectMethodCallback implements DeviceMethodCallback {

        private void setTelemetryInterval(int val) {
            System.out.println("Direct method # Setting telemetry interval (seconds): " + val);
            interval = interval * val;
        }

        @Override
        public DeviceMethodData call(String methodName, Object methodData, Object context) {
            DeviceMethodData deviceMethodData;
            String payload = new String((byte[]) methodData);
            switch (methodName) {
                case "SetTelemetryInterval": {
                    int interval;
                    try {
                        int status = METHOD_SUCCESS;
                        interval = Integer.parseInt(payload);
                        System.out.println(payload);
                        setTelemetryInterval(interval);
                        deviceMethodData = new DeviceMethodData(status, "Executed direct method " + methodName);
                    } catch (NumberFormatException e) {
                        int status = INVALID_PARAMETER;
                        deviceMethodData = new DeviceMethodData(status, "Invalid parameter " + payload);
                    }
                    break;
                }
                default: {
                    int status = METHOD_NOT_DEFINED;
                    deviceMethodData = new DeviceMethodData(status, "Not defined direct method " + methodName);
                }
            }
            return deviceMethodData;
        }
    }

    private static class MessageSender implements Runnable {

        @Override
        public void run() {
            try {
                // Initialize the simulated telemetry.
                double minTemperature = 20;
                double minHumidity = 60;
                double sensorLocLat = -37.840935f;
                double sensorLocLong = 144.946457f;
                Random rand = new Random();
                long startTime = System.currentTimeMillis();
                while (true) {
                    // Simulate telemetry.
                    double currentTemperature = minTemperature + rand.nextDouble() * 15;
                    double currentHumidity = minHumidity + rand.nextDouble() * 20;

                    String infoString;
                    String levelValue;
                    if (rand.nextDouble() > 0.7) {
                        if (rand.nextDouble() > 0.5) {
                            levelValue = "critical";
                            infoString = "This is a critical messgae as alert!";
                        } else {
                            levelValue = "storage";
                            infoString = "This is a storage message.";
                        }
                    } else {
                        levelValue = "normal";
                        infoString = "This is a normal message - processed in real-time.";
                    }

                    TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
                    telemetryDataPoint.temperature = currentTemperature;
                    telemetryDataPoint.humidity = currentHumidity;
                    telemetryDataPoint.latitude = sensorLocLat;
                    telemetryDataPoint.longitude = sensorLocLong;
                    telemetryDataPoint.deviceId = devID;
                    telemetryDataPoint.pointInfo = infoString;
                    // Add the telemetry to the message body as JSON.
                    String msgStr = telemetryDataPoint.serialize();
                    Message msg = new Message(msgStr);

                    // Add a custom application property to the message.
                    // An IoT hub can filter on these properties without access to the message body.
                    //msg.setProperty("temperatureAlert", (currentTemperature > 30) ? "true" : "false");
                    msg.setProperty("level", levelValue);
                    //System.out.println("Sending message: " + msgStr);
                    System.out.println(String.format("%s > Sent message: %s", LocalDateTime.now(), msgStr));
                    Object lockobj = new Object();

                    // Send the message.
                    EventCallback callback = new EventCallback();
                    client.sendEventAsync(msg, callback, lockobj);

                    synchronized (lockobj) {
                        lockobj.wait();
                    }
                    Thread.sleep(interval);
                }
            } catch (InterruptedException e) {
                System.out.println("Finished.");
                System.out.println(e.getMessage());
            }
        }
    }

    public static void SendTelemetry(String conString, IotHubClientProtocol msgProtocol) throws IOException {

        try {
            client = new DeviceClient(conString, msgProtocol);
            client.open();
        } catch (URISyntaxException | IllegalArgumentException ex) {
            System.out.println("Error connecting to IoT Hub: " + ex.getMessage());
        }

        // Register to receive direct method calls.
        client.subscribeToDeviceMethod(new DirectMethodCallback(), null, new DirectMethodStatusCallback(), null);
        // Create new thread and start sending messages 
        MessageSender sender = new MessageSender();
        ExecutorService executor = Executors.newFixedThreadPool(1);

        executor.execute(sender);
        // Stop the application.
        System.out.println("Finished sending telemetry");
        //System.in.read();
        long startTime = System.currentTimeMillis();
        while (false || (System.currentTimeMillis() - startTime) < duration * 60000) {
           //stay until send duration....
        }
        executor.shutdownNow();
        client.closeNow();

    }

    public static void sendDeviceTelemetryToCloud(Device device, int sendDuration) {

        try {
            DBOperations.dbUpdateDeviceInfo(device, true);
            duration = sendDuration;
            devID = device.getDeviceId();
            interval = Integer.parseInt(device.getTelemInterval());
            String conString = device.getConnectionString();
            IotHubClientProtocol msgProtocol = null;
            if ("AMQP".equals(device.getProtocol())) {
                msgProtocol = IotHubClientProtocol.AMQPS;
            } else if ("MQTT".equals(device.getProtocol())) {
                msgProtocol = IotHubClientProtocol.MQTT;
            } else {
                System.out.println("Invalid transport protocol!");
                return;
            }
            SendTelemetry("HostName=iotcloudintegration.azure-devices.net;DeviceId=demo-device-symm-key-001;SharedAccessKey=pkrpdAk4Qvn7sd4bNAxldkXcwMpXo+PBOyWoiWueEe3iMkPw1Feleg8rns4oyIGV1lEnSDACGkwgpQqsf+eUdA==", msgProtocol);
        } catch (IOException ex) {
            System.out.println("Error while sending telemetry: " + ex.getMessage());
        }
    }

    public static void stopSendindTelemetry(String deviceId) {
        DBOperations.updateDeviceStatus(deviceId, false);
    }

}
