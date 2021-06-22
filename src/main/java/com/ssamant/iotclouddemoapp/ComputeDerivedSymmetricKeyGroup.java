/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.iotclouddemoapp;

import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProviderSymmetricKey;
import com.ssamant.connectioninfo.ConnectionInfo;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sunil
 */
public class ComputeDerivedSymmetricKeyGroup {

    private static final String ENROLLMENT_GROUP_SYMMETRIC_KEY = ConnectionInfo.getSymmetricKeyGroup();

    // private static final String 
    public static byte[] computePerDeviceSymmetricKeyForGroupEnrollment(String deviceId) {

        byte[] derivedSymmetricKey = null;
        try {
            derivedSymmetricKey = SecurityProviderSymmetricKey
                    .ComputeDerivedSymmetricKey(
                            ENROLLMENT_GROUP_SYMMETRIC_KEY.getBytes(StandardCharsets.UTF_8),
                            deviceId);
        } catch (InvalidKeyException | NoSuchAlgorithmException ex) {
            Logger.getLogger(ComputeDerivedSymmetricKeyGroup.class.getName()).log(Level.SEVERE, null, ex);
        }

        return derivedSymmetricKey;

    }

}
