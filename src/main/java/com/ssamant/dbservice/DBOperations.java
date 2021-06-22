/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.dbservice;

import com.ssamant.connectioninfo.ConnectionInfo;
import static com.ssamant.connectioninfo.ConnectionInfo.getDbConnection;
import com.ssamant.iotclouddemoapp.Device;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sunil
 */
public class DBOperations {

    public static void dbUpdateDeviceInfo(Device device) {

        try {
            if (ConnectionInfo.con == null) {
                ConnectionInfo.con = getDbConnection();
            }
            String Updatequery = "UPDATE device SET msgsize = ?, teleminterval = ?, protocol = ? WHERE deviceid = ?";
            try ( PreparedStatement pstmt = ConnectionInfo.con.prepareStatement(Updatequery)) {
                pstmt.setString(1, device.getMessageSize());
                pstmt.setString(2, device.getTelemInterval());
                pstmt.setString(3, device.getProtocol());
                pstmt.setString(4, device.getDeviceId());
                pstmt.executeUpdate();
                pstmt.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void newDeviceEntry(Device device) {

        String insertSQL = "INSERT INTO deviceinfo (deviceid, connectionstring, iothuburi, deviceowner) "
                + "VALUES (?, ?, ?)";
        if (ConnectionInfo.con == null) {
            ConnectionInfo.con = getDbConnection();
        }
        try ( PreparedStatement pstmt = ConnectionInfo.con.prepareStatement(insertSQL)) {

            pstmt.setString(1, device.getDeviceId());
            pstmt.setString(2, device.getConnectionString());
            pstmt.setString(3, device.getIotHubUri());
            pstmt.execute();
            pstmt.close();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

    }
    
    public static void getDeviceConnectionInfo(String deviceId) {
        
    }

}
