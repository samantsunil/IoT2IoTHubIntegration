# IoT2IoTHubIntegration
The project is developed to demonstrate the IoT device registration, telemetry send at various rates, command and control and data consumption tasks using Azure IoT services, such as IoT Hub DPS, IoT Hub, IoT Device, etc.

# How to run the application (Build from source code):
1. Clone the repo or download the source code zip file.
2. Config the relevant parameters in application.properties file for postgresql server connection info, IoT Hub service related connection params, IoT Hub DPS related params. 
(Before performing step 1, it requires to create an IoT Hub resource, IoT Hub DPS resource configuring both individual enrollment and enrollment group through manage enrollment in Azure portal
and note down the required connection and service end-point strings/values.)
2. Build using maven : mvn clean install, OR build using IDE's maven tool
3. Run executable jar file.
