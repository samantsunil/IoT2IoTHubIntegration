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
import static com.ssamant.iotclouddemoapp.MainForm.lblSendStopMsg;
import java.awt.Point;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static ExecutorService executor;
    private static int messageSize = 0;

    private static String devID = "";

    private static class TelemetryDataPoint {

        public String deviceId;
        public String timestamp;
        public int temperature;
        public int humidity;
        public double lat;
        public double lon;
        public String weatherInfo;
        public Boolean isMoving;

        // Serialize object to JSON format.
        public String serialize() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    private static class GeoLocAwareTelemetryDataPoint {

        public String deviceId;
        public double temp;
        public double humidity;
        public String weatherInfo;
        public Boolean isMoving;
        public GeoLocation point;

        public String serialize() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    private static class GeoLocation {

        public double lat;
        public double lon;

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

        private void changeDeviceMobilityStatus(Boolean val) {
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

                case "SetDeviceMobilityONorOFF": {
                    Boolean val;
                    try {
                        int status = METHOD_SUCCESS;
                        switch (payload) {
                            case "ON":
                            case "On":
                            case "on":
                                val = true;
                                break;
                            case "OFF":
                            case "Off":
                            case "off":
                                val = false;
                                break;
                            default:
                                val = false;
                                break;
                        }
                        //val = Boolean.parseBoolean(payload);
                        System.out.println(payload);
                        changeDeviceMobilityStatus(val);
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

        public String deviceId;

        @Override
        public void run() {
            try {
                // Initialize the simulated telemetry data.
                double minTemperature = 10;
                double minHumidity = 20;

                Random rand = new Random();
                DecimalFormat df = new DecimalFormat("##.##");
                DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
                double sensorLocLat = -37.840935f + rand.nextDouble();
                double sensorLocLong = 144.946457f + rand.nextDouble();
                long startTime = System.currentTimeMillis();

                while ((System.currentTimeMillis() - startTime) < duration * 60000) {
                    // Simulate telemetry.
                    double temp, humidity;
                    int val = rand.nextInt(2);
                    if (val == 0) {
                        double currentTemperature = minTemperature + rand.nextDouble() * 50;
                        double currentHumidity = minHumidity + rand.nextDouble() * 60;
                        temp = Math.round(currentTemperature * 100.0) / 100.0;
                        humidity = Math.round(currentHumidity * 100.0) / 100.0;
                    } else {
                        double currentTemperature = minTemperature - rand.nextDouble() * 50;
                        double currentHumidity = minHumidity - rand.nextDouble() * 60;
                        temp = Math.round(currentTemperature * 100.0) / 100.0;
                        humidity = Math.round(currentHumidity * 100.0) / 100.0;
                    }

                    String infoString;
                    String levelValue;
                    if (temp > 35.00f) {
                        if (humidity > 95.00f) {
                            levelValue = "hothumid";
                            infoString = "Hot and humid weather";
                        } else if (humidity < 20.00f) {
                            levelValue = "hotdry";
                            infoString = "Hot and dry weather";
                        } else {
                            levelValue = "Hot";
                            infoString = "Hot weather";
                        }
                    } else if (temp < 10.00f) {
                        if (humidity < 20.00f) {
                            levelValue = "colddry";
                            infoString = "Cold and dry weather";
                        } else {
                            levelValue = "cold";
                            infoString = "Cold weather";
                        }
                    } else {
                        levelValue = "normal";
                        infoString = "Normal weather";
                    }
                     //change geo coordinate when device set as moving       
                    if (turnOn) {
                        if (val == 0) {
                            sensorLocLat = sensorLocLat - rand.nextDouble();
                            sensorLocLong = sensorLocLong + rand.nextDouble();

                        } else {
                            sensorLocLat = sensorLocLat + rand.nextDouble();
                            sensorLocLong = sensorLocLong - rand.nextDouble();
                        }
                    }

                    TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
                    telemetryDataPoint.temperature = (int) temp;
                    telemetryDataPoint.humidity = (int) humidity;
                    telemetryDataPoint.lat = sensorLocLat;
                    telemetryDataPoint.lon = sensorLocLong;
                    telemetryDataPoint.deviceId = devID;
                    telemetryDataPoint.timestamp = Instant.now().toString();
                    telemetryDataPoint.weatherInfo = infoString;
                    telemetryDataPoint.isMoving = turnOn;
                    // Add the telemetry to the message body as JSON.
                    String msgStr = telemetryDataPoint.serialize();
                    Message msg = new Message(msgStr);

                    // Add a custom application property to the message.
                    // An IoT hub can filter on these properties without access to the message body.
                    //msg.setProperty("temperatureAlert", (currentTemperature > 30) ? "true" : "false");
                    msg.setContentEncoding("utf-8");
                    msg.setContentTypeFinal("application/json");
                    msg.setProperty("level", levelValue);
                   // msg.setExpiryTime(D2C_MESSAGE_TIMEOUT);

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
                System.out.println("Finished sending telemetry to cloud...");
                MainForm.txtAreaConsoleOutput.append("Finished sending telemetry.");
                lblSendStopMsg.setText("Finished telemetry sending operation for the defined duration.");
                DBOperations.updateDeviceStatus(deviceId, false);
                
                client.closeNow();
                Thread.sleep(2000);
                executor.shutdownNow();
            } catch (InterruptedException e) {
                //System.out.println("Finished.");
                System.out.println(e.getMessage());
            } catch (IOException ex) {
                System.out.println("Error in disconnecting client - " + ex.getMessage());
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

    private static class GeoLocationMessgeSender implements Runnable {

        public String deviceId;
        int i = 0;

        @Override
        public void run() {
            try {
                // Initialize the simulated telemetry data.
                double minTemperature = 0;
                double minHumidity = 10;

                Random rand = new Random();
                DecimalFormat df = new DecimalFormat("##.##");
                DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
                double sensorLocLat = -37.840935f + rand.nextDouble();
                double sensorLocLong = 144.946457f + rand.nextDouble();
                long startTime = System.currentTimeMillis();

                while ((System.currentTimeMillis() - startTime) < duration * 60000) {
                    // Simulate telemetry.
                    double currentTemperature = minTemperature + rand.nextDouble() * 50;
                    double currentHumidity = minHumidity + rand.nextDouble() * 90;
                    currentTemperature = Math.round(currentTemperature * 100.0) / 100.0;
                    currentHumidity = Math.round(currentHumidity * 100.0) / 100.0;

                    if (turnOn) {
                        if (i % 2 == 0) {
                            sensorLocLat = sensorLocLat + rand.nextInt(3) / 1.0;
                            sensorLocLong = sensorLocLong + rand.nextInt(4) / 1.0;
                        } else {
                            sensorLocLat = sensorLocLat - rand.nextInt(2) / 1.0;
                            sensorLocLong = sensorLocLong - rand.nextInt(3) / 1.0;
                        }
                    }
                    i++;
                    String infoString;
                    String levelValue;
                    if (currentTemperature > 35.00f) {
                        if (currentHumidity > 95.00f) {
                            levelValue = "hothumid";
                            infoString = "Hot and humid weather";
                        } else if (currentHumidity < 20.00f) {
                            levelValue = "hotdry";
                            infoString = "Hot and dry weather";
                        } else {
                            levelValue = "Hot";
                            infoString = "Hot weather";
                        }
                    } else if (currentTemperature < 10.00f) {
                        if (currentHumidity < 20.00f) {
                            levelValue = "colddry";
                            infoString = "Cold and dry weather";
                        } else {
                            levelValue = "cold";
                            infoString = "Cold weather";
                        }
                    } else {
                        levelValue = "normal";
                        infoString = "Normal weather";
                    }

                    GeoLocAwareTelemetryDataPoint telemetryDataPoint = new GeoLocAwareTelemetryDataPoint();
                    telemetryDataPoint.temp = currentTemperature;
                    telemetryDataPoint.humidity = currentHumidity;

                    telemetryDataPoint.deviceId = devID;
                    telemetryDataPoint.weatherInfo = infoString;
                    telemetryDataPoint.isMoving = turnOn;

                    GeoLocation point = new GeoLocation();
                    point.lat = sensorLocLat;
                    point.lon = sensorLocLong;
                    telemetryDataPoint.point = point;

                    // Add the telemetry to the message body as JSON.
                    String msgStr = telemetryDataPoint.serialize();
                    Message msg = new Message(msgStr);

                    // Add a custom application property to the message.
                    // An IoT hub can filter on these properties without access to the message body.
                    //msg.setProperty("temperatureAlert", (currentTemperature > 30) ? "true" : "false");
                    msg.setContentEncoding("utf-8");
                    msg.setContentTypeFinal("application/json");
                    msg.setProperty("level", levelValue);
                    msg.setProperty("moving", turnOn.toString());
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
                System.out.println("Finished sending telemetry to cloud...");
                MainForm.txtAreaConsoleOutput.append("Finished sending telemetry.");
                lblSendStopMsg.setText("Finished telemetry sending operation for the defined duration.");
                DBOperations.updateDeviceStatus(deviceId, false);
                
                client.closeNow();
                executor.shutdown();
            } catch (InterruptedException e) {
                //System.out.println("Finished.");
                System.out.println(e.getMessage());
            } catch (IOException ex) {
                System.out.println("Error in disconnecting client - " + ex.getMessage());
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
            
            // Register to receive direct method calls.
            if (!(msgProtocol == IotHubClientProtocol.HTTPS)) {
                client.subscribeToDeviceMethod(new DirectMethodCallback(), null, new DirectMethodStatusCallback(), null);
            }
            // Create new thread and start sending messages
            MessageSender sender = new MessageSender();
            //GeoLocationMessgeSender sender = new GeoLocationMessgeSender();
            sender.deviceId = device.getDeviceId();
            //FixedSizeMessageSender sender = new FixedSizeMessageSender();
            executor = Executors.newFixedThreadPool(1);
            executor.execute(sender);
        } catch (URISyntaxException | IllegalArgumentException ex) {
            executor.shutdown();
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
                msgProtocol = IotHubClientProtocol.HTTPS;
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
     * currently sending message to cloud. It terminates the currently running
     * thread and stops sending telemetry to cloud.
     *
     * @param deviceId
     */
    public static void stopSendindTelemetry(String deviceId) {
        DBOperations.updateDeviceStatus(deviceId, false);
        if (!executor.isShutdown() && client != null) {
            try {
                client.closeNow();
            } catch (IOException ex) {
                System.out.println("Client connection closed forcefully: " + ex.getMessage());
            }
            executor.shutdown();
        }
    }

    public static void onMainWindowsClosing() {
        if (client != null) {
            try {
                client.closeNow();
            } catch (IOException ex) {

                System.out.println("Main program exit: " + ex.getMessage());
            }
        }
    }

}
