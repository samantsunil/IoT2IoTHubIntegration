/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.iotclouddemoapp;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.gson.Gson;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.ssamant.dbservice.DBOperations;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Sunil
 */
public class DeviceTelemetryService {

    private static final int METHOD_SUCCESS = 200;
    private static final int METHOD_NOT_DEFINED = 404;
    private static final int INVALID_PARAMETER = 400;
    private static final int D2C_MESSAGE_TIMEOUT = 2000;
    private static int duration = 0;
    private static int interval = 0;
    private static Boolean turnOn = false;
    private static DeviceClient client;
    private static int messageSize = 0;

    private static String devID = "";

    private static class TelemetryDataPoint {

        public String deviceId;
        public double temperature;
        public double humidity;
        public double lat;
        public double longi;
        public String pointInfo;
        public Boolean onOff;

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

    /**
     * callback method - to receive message from the cloud.
     */
    protected static class DirectMethodCallback implements DeviceMethodCallback {

        private void setTelemetryInterval(int val) {
            System.out.println("Direct method # Setting telemetry interval (seconds): " + val * interval);
            interval = interval * val;
        }

        private void turnOffTelemetrySending(Boolean val) {
            System.out.println("Direct method # send command to turn Off the telemetry sending.");
            turnOn = val;
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

                case "TurnOffTelemetrySending": {
                    Boolean val;
                    try {
                        int status = METHOD_SUCCESS;
                        val = Boolean.parseBoolean(payload);
                        System.out.println(payload);
                        turnOffTelemetrySending(val);
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

    /**
     * method runs inside the runnable thread process. It prepared the sample
     * device data and calls async message send method for IoT Hub device
     * client. The method listens for any message send by the IoT Hub through
     * DirectMethod callback. The direct method callback is implemented to
     * listen for any updated value of telemetry send interval and accordingly
     * update the telemetry interval value.
     */
    private static class MessageSender implements Runnable {

        @Override
        public void run() {
            try {
                // Initialize the simulated telemetry data.
                double minTemperature = 20;
                double minHumidity = 60;
                
                Random rand = new Random();
                DecimalFormat df = new DecimalFormat("##.##");
                DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
                double sensorLocLat = -37.840935f + rand.nextInt(100) / 100.0;
                double sensorLocLong = 144.946457f + rand.nextInt(100) / 100.0;
                while (true) {
                    // Simulate telemetry.
                    double currentTemperature = minTemperature + rand.nextDouble() * 15;
                    double currentHumidity = minHumidity + rand.nextDouble() * 20;
                    currentTemperature = Math.round(currentTemperature * 100.0) / 100.0;
                    currentHumidity = Math.round(currentHumidity * 100.0) / 100.0;

                    String infoString;
                    String levelValue;
                    if (rand.nextDouble() > 0.7) {
                        if (rand.nextDouble() > 0.5) {
                            levelValue = "critical";
                            infoString = "message to generate alert.";
                        } else {
                            levelValue = "storage";
                            infoString = "message for storage.";
                        }
                    } else {
                        levelValue = "normal";
                        infoString = "message for real-time insight.";
                    }

                    TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
                    telemetryDataPoint.temperature = currentTemperature;
                    telemetryDataPoint.humidity = currentHumidity;
                    telemetryDataPoint.lat = sensorLocLat;
                    telemetryDataPoint.longi = sensorLocLong;
                    telemetryDataPoint.deviceId = devID;
                    telemetryDataPoint.pointInfo = infoString;
                    telemetryDataPoint.onOff = turnOn;
                    // Add the telemetry to the message body as JSON.
                    String msgStr = telemetryDataPoint.serialize();
                    Message msg = new Message(msgStr);

                    // Add a custom application property to the message.
                    // An IoT hub can filter on these properties without access to the message body.
                    //msg.setProperty("temperatureAlert", (currentTemperature > 30) ? "true" : "false");
                    msg.setProperty("level", levelValue);
                    msg.setExpiryTime(D2C_MESSAGE_TIMEOUT);
                                        
                    //System.out.println("Sending message: " + msgStr);
                    System.out.println(String.format("%s > Message: %s", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(dtf), msgStr));
                    MainForm.txtAreaConsoleOutput.append(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(dtf) + " > Message: " + msgStr + "\n");
                    Object lockobj = new Object();
                    // msg.getBytes();
                    System.out.println("Message size in bytes: " + RamUsageEstimator.sizeOf(msg.getBytes()));
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

    private static class FixedSizeMessageSender implements Runnable {

        @Override
        public void run() {
            try {
                DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
                while (true) {
                    byte[] msgSize = new byte[messageSize];
                    String msgStr = Arrays.toString(msgSize);
                    Message msg = new Message(msgStr);
                    msg.setExpiryTime(D2C_MESSAGE_TIMEOUT);
                    System.out.println(String.format("%s > Message: %s", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(dtf), msgStr));
                    MainForm.txtAreaConsoleOutput.append(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(dtf) + " > Message: " + msgStr + "\n");
                    Object lockobj = new Object();
                    // msg.getBytes();
                    System.out.println("Message Size: " + RamUsageEstimator.sizeOf(msg.getBytes()));
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

    /**
     * method to open the IoT Hub client connection for the selected device
     * (deviceId) and calls the message sender class in executor thread, that
     * implements the statements for message sending.
     *
     * @param device
     * @param msgProtocol
     * @throws IOException
     */
    public static void SendTelemetry(Device device, IotHubClientProtocol msgProtocol) throws IOException {

        try {
            String conStr = "HostName=" + device.getIotHubUri().trim() + ";DeviceId=" + device.getDeviceId().trim() + ";SharedAccessKey=" + device.getConnectionString().trim();

            client = new DeviceClient(conStr, msgProtocol);
            client.open();
            //

            // Register to receive direct method calls.
            client.subscribeToDeviceMethod(new DirectMethodCallback(), null, new DirectMethodStatusCallback(), null);
            // Create new thread and start sending messages
            MessageSender sender = new MessageSender();
            //FixedSizeMessageSender sender = new FixedSizeMessageSender();
            ExecutorService executor = Executors.newFixedThreadPool(1);

            executor.execute(sender);
            // Stop the application.
            System.out.println("Finished sending telemetry");
            //System.in.read();
            long startTime = System.currentTimeMillis();
            while (false || (System.currentTimeMillis() - startTime) < duration * 60000) {
                //keep sending operation active until the send duration completes ( in minutes )....
            }
            executor.shutdownNow();
            client.closeNow();
            stopSendindTelemetry(device.getDeviceId());

            MainForm.txtAreaConsoleOutput.append("Finished sending telemetry.");
        } catch (URISyntaxException | IllegalArgumentException ex) {
            System.out.println("Error occured while conneting to IoT Hub: " + ex.getMessage());
        }

    }

    public static int sendDeviceTelemetryToCloud(Device device, int sendDuration) {
        int retVal = 0;
        try {

            duration = sendDuration;
            devID = device.getDeviceId();
            interval = Integer.parseInt(device.getTelemInterval());
            messageSize = Integer.parseInt(device.getMessageSize().trim());
            IotHubClientProtocol msgProtocol = null;

            if ("AMQP".equals(device.getProtocol())) {
                msgProtocol = IotHubClientProtocol.AMQPS;
            } else if ("MQTT".equals(device.getProtocol())) {
                msgProtocol = IotHubClientProtocol.MQTT;
            } else {
                System.out.println("Invalid transport protocol selection!");
                return 0;
            }
            SendTelemetry(device, msgProtocol);
            retVal = 1;

        } catch (IOException ex) {
            System.out.println("Error while sending telemetry: " + ex.getMessage());

        }
        return retVal;
    }

    /**
     * method to stop the telemetry sending forcefully while the device is
     * currently sending message to cloud. It terminates the IoT Hub client
     * connection for the device.
     *
     * @param deviceId
     */
    public static void stopSendindTelemetry(String deviceId) {
        DBOperations.updateDeviceStatus(deviceId, false);
        try {
            if (client != null) {
                client.closeNow();

            }
        } catch (IOException ex) {

            System.out.println("Telemtry sending from the selected device is stopped: " + ex.getMessage());
        }

    }

}
