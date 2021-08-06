/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.devicedeprovision;

import com.ssamant.connectioninfo.ConnectionInfo;

import com.microsoft.azure.sdk.iot.provisioning.service.ProvisioningServiceClient;
import com.microsoft.azure.sdk.iot.provisioning.service.Query;
import com.microsoft.azure.sdk.iot.provisioning.service.configs.*;
import com.microsoft.azure.sdk.iot.provisioning.service.exceptions.ProvisioningServiceClientException;

import java.util.UUID;

/**
 *
 * @author Sunil
 */
public class DeviceDeprovisionService {
    
    private static final String PROVISIONING_CONNECTION_STRING = ConnectionInfo.getSymmetricKeyIndividual();
    private static final String REGISTRATION_ID = ConnectionInfo.getRegIdIndividual();
    private static final ProvisioningStatus PROVISIONING_STATUS = ProvisioningStatus.ENABLED;
    
    public static void removeIoTDevice(){
        
    }
}
