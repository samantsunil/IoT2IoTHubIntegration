/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.backendservice;

import com.ssamant.connectioninfo.ConnectionInfo;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.core.amqp.ProxyAuthenticationType;
import com.azure.core.amqp.ProxyOptions;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerAsyncClient;
import com.azure.messaging.eventhubs.models.EventPosition;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 *
 * @author Sunil
 */
public class ReadDeviceToCloudMessages {

    private static final String EVENT_HUB_COMPATIBLE_CONNECTION_STRING = ConnectionInfo.getEventHubCompatibleConnectionString();

    /**
     * Read all the messages ingested to IoT Hub (specific unit) from all the
     * devices connected to the given IoT Hub.
     *
     * @param option
     * @param batchSize
     */
    public static void readDeviceMessagesIngestedToIoTHub(String option, int batchSize) {

        EventHubClientBuilder consumerClientBuilder = new EventHubClientBuilder()
                .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
                .connectionString(EVENT_HUB_COMPATIBLE_CONNECTION_STRING);

        // if proxy setup is required - use below method
        //setupProxy(consumerClientBuilder);
        //If WebSockets need to use - use below method (AMQP over Web Sockets)
        //consumerClientBuilder.transportType(AmqpTransportType.AMQP_WEB_SOCKETS);
        try ( EventHubConsumerAsyncClient eventHubConsumerAsyncClient = consumerClientBuilder.buildAsyncConsumerClient()) {

            if (null == option) {
                System.out.println("Select the right option.");
            } else {
                switch (option) {
                    case "all" ->
                        receiveFromAllPartitions(eventHubConsumerAsyncClient); // received data from all the partitions - default (4)
                    case "single" ->
                        receiveFromSinglePartition(eventHubConsumerAsyncClient); // receive data from a single partition only
                    case "batches" ->
                        receiveFromSinglePartitionInBatches(eventHubConsumerAsyncClient, batchSize); // receives data from a single partition in the given batch size
                    default ->
                        System.out.println("Select the right option.");
                }
            }
            //use below methods to receive from single partition or in batches from a single partition

        }

    }

    /**
     * This method receives messages from all partitions (by default there are 4
     * ) asynchronously from the newly available messages/events in each
     * partition of IoT Hub
     *
     * @param consumerClient
     */
    private static void receiveFromAllPartitions(EventHubConsumerAsyncClient consumerClient) {

        consumerClient.receive(false) //false - allows to read the newly available events instead of reading from start of the partition
                .subscribe(partitionEvent -> {
                    System.out.println();
                    System.out.printf("%nTelemetry received from partition %s:%n%s",
                            partitionEvent.getPartitionContext().getPartitionId(), partitionEvent.getData().getBodyAsString());
                    System.out.printf("%nApplication properties (set by device):%n%s", partitionEvent.getData().getProperties());
                    System.out.printf("%nSystem properties (set by IoT Hub):%n%s",
                            partitionEvent.getData().getSystemProperties());

                }, ex -> {
                    System.out.println("Error receiving events " + ex.getMessage());
                }, () -> {
                    System.out.println("Completed receiving events");
                });
    }

    /**
     * This method queries all available partition in the IoT Hub (event hub
     * compatible end point) and picks a single partition to receive events
     * asynchronously starting from the newly available events in that
     * partition.
     *
     * @param consumerClient
     */
    private static void receiveFromSinglePartition(EventHubConsumerAsyncClient consumerClient) {

        consumerClient.getPartitionIds()
                .take(1)
                .flatMap(partitionId -> {
                    System.out.println("Receiving events from partition id " + partitionId);
                    return consumerClient.receiveFromPartition(partitionId, EventPosition.latest());
                }).subscribe(partitionEvent -> {
            System.out.println();
            System.out.printf("%nTelemetry received from partition %s:%n%s",
                    partitionEvent.getPartitionContext().getPartitionId(), partitionEvent.getData().getBodyAsString());
            System.out.printf("%nApplication properties (set by device):%n%s", partitionEvent.getData().getProperties());
            System.out.printf("%nSystem properties (set by IoT Hub):%n%s",
                    partitionEvent.getData().getSystemProperties());
        }, ex -> {
            System.out.println("Error receiving events " + ex.getMessage());
        }, () -> {
            System.out.println("Completed receiving events");
        });
    }

    /**
     * This method queries all available partitions in the iot hub (event hub)
     * and picks a single partition to receive events asynchronously in batches
     * of given size, starting from the newly available event in that partition.
     *
     * @param consumerClient
     */
    private static void receiveFromSinglePartitionInBatches(EventHubConsumerAsyncClient consumerClient, int batchSize) {

        consumerClient.getPartitionIds()
                .take(1)
                .flatMap(partitionId -> {
                    System.out.println("Receiving events from partition id " + partitionId);
                    return consumerClient
                            .receiveFromPartition(partitionId, EventPosition.latest());
                }).window(batchSize) // batch the events - batch interval 
                .subscribe(partitionEvents -> {
                    partitionEvents.toIterable().forEach(partitionEvent -> {
                        System.out.println();
                        System.out.printf("%nTelemetry received from partition %s:%n%s",
                                partitionEvent.getPartitionContext().getPartitionId(), partitionEvent.getData().getBodyAsString());
                        System.out.printf("%nApplication properties (set by device):%n%s",
                                partitionEvent.getData().getProperties());
                        System.out.printf("%nSystem properties (set by IoT Hub):%n%s",
                                partitionEvent.getData().getSystemProperties());
                    });
                }, ex -> {
                    System.out.println("Error receiving events " + ex.getMessage());
                }, () -> {
                    System.out.println("Completed receiving events");
                });
    }

    /**
     * This method sets up proxy options and updates the
     * {@link EventHubClientBuilder}.
     *
     * @param consumerClientBuilder
     */
    private static void setupProxy(EventHubClientBuilder consumerClientBuilder) {
        int proxyPort = 8000; // replace with right proxy port
        String proxyHost = "{hostname}";
        Proxy proxyAddress = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        String userName = "{username}";
        String password = "{password}";
        ProxyOptions proxyOptions = new ProxyOptions(ProxyAuthenticationType.BASIC, proxyAddress,
                userName, password);

        consumerClientBuilder.proxyOptions(proxyOptions);

        // To use proxy, the transport type has to be Web Sockets.
        consumerClientBuilder.transportType(AmqpTransportType.AMQP_WEB_SOCKETS);
    }

}