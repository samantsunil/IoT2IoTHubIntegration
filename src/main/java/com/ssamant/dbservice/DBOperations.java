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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 *
 * @author Sunil
 */
public class DBOperations {

    /**
     * function to update the telemetry parameters of the registered device.
     *
     * @param device
     * @param status
     */
    public static void dbUpdateDeviceInfo(Device device, Boolean status) {

        try {
            if (ConnectionInfo.con == null) {
                ConnectionInfo.con = getDbConnection();
            }
            String Updatequery = "UPDATE deviceinfo SET msgsize = ?, teleminterval = ?, protocol = ?, active = ? WHERE deviceid = ?";
            try (PreparedStatement pstmt = ConnectionInfo.con.prepareStatement(Updatequery)) {
                pstmt.setString(1, device.getMessageSize());
                pstmt.setString(2, device.getTelemInterval());
                pstmt.setString(3, device.getProtocol());
                pstmt.setBoolean(4, status);
                pstmt.setString(5, device.getDeviceId());
                pstmt.executeUpdate();
                pstmt.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * function to insert the device info for newly created IoT device into
     * database
     *
     * @param device
     */
    public static void newDeviceEntry(Device device) {

        String insertSQL = "INSERT INTO deviceinfo (deviceid, connectionstring, iothuburi, deviceowner, active) "
                + "VALUES (?, ?, ?, ?, ?)";
        if (ConnectionInfo.con == null) {
            ConnectionInfo.con = getDbConnection();
        }
        try (PreparedStatement pstmt = ConnectionInfo.con.prepareStatement(insertSQL)) {

            pstmt.setString(1, device.getDeviceId());
            pstmt.setString(2, device.getConnectionString());
            pstmt.setString(3, device.getIotHubUri());
            pstmt.setString(4, device.getDeviceOwner());
            pstmt.setBoolean(5, false); //active status is false while registering the device to IoT Hub
            pstmt.execute();
            pstmt.close();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

    }

    /**
     * function to update the device telemetry send status - active or idle.
     *
     * @param deviceId
     * @param status
     */
    public static void updateDeviceStatus(String deviceId, Boolean status) {
        try {
            if (ConnectionInfo.con == null) {
                ConnectionInfo.con = getDbConnection();
            }
            String Updatequery = "UPDATE deviceinfo SET active = ? WHERE deviceid = ?";
            try (PreparedStatement pstmt = ConnectionInfo.con.prepareStatement(Updatequery)) {
                pstmt.setBoolean(1, status);
                pstmt.setString(2, deviceId);

                pstmt.executeUpdate();
                pstmt.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * function to return all the device Ids of already registered devices.
     *
     * @return
     */
    public static ArrayList getAllDeviceIds() {
        if (ConnectionInfo.con == null) {
            ConnectionInfo.con = getDbConnection();
        }
        ArrayList<String> deviceList = new ArrayList<>();
        String qry = "SELECT deviceid from deviceinfo";
        try {
            Statement stm;
            stm = ConnectionInfo.con.createStatement();

            ResultSet rs = stm.executeQuery(qry);

            while (rs.next()) {
                String deviceId = rs.getString("deviceid");
                deviceList.add(deviceId);
            }
            rs.close();

        } catch (SQLException ex) {

        }
        return deviceList;
    }

    public static ArrayList getAllDirectMethods() {
        if (ConnectionInfo.con == null) {
            ConnectionInfo.con = getDbConnection();
        }
        ArrayList<String> methods = new ArrayList<>();

        String qry = "SELECT DISTINCT methodname FROM devicecallbackmethod";
        try {
            Statement stm;
            stm = ConnectionInfo.con.createStatement();

            try (ResultSet rs = stm.executeQuery(qry)) {
                while (rs.next()) {
                    String methodName = rs.getString("methodname");
                    methods.add(methodName);
                }
            }

        } catch (SQLException ex) {

        }
        return methods;
    }

    /**
     * method to obtain device info, such as device id, deviceowner and active
     * status.
     *
     * @return
     */
    public static ResultSet getAllDevices() {
        if (ConnectionInfo.con == null) {
            ConnectionInfo.con = getDbConnection();
        }
        ResultSet rs = null;
        String qry = "SELECT deviceid, deviceowner, active FROM deviceinfo";
        try {
            Statement stm;
            stm = ConnectionInfo.con.createStatement();

            rs = stm.executeQuery(qry);

        } catch (SQLException ex) {

        }
        return rs;
    }

    public static String[] getDeviceConnInfo(String deviceId) {

        if (ConnectionInfo.con == null) {
            ConnectionInfo.con = getDbConnection();
        }

        String[] devConnInfo = new String[2];
        ResultSet rs = null;
        String qry = "SELECT connectionstring, iothuburi FROM deviceinfo WHERE deviceId = ? AND active = ?";
        try (PreparedStatement pstmt = ConnectionInfo.con.prepareStatement(qry)) {
            pstmt.setString(1, deviceId);
            pstmt.setBoolean(2, false);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                String constr = rs.getString("connectionstring");
                String huburi = rs.getString("iothuburi");
                devConnInfo[0] = constr;
                devConnInfo[1] = huburi;
            }
            rs.close();

        } catch (SQLException ex) {

        }
        return devConnInfo;
    }

    public static Boolean isDeviceActive(String deviceId) {
        if (ConnectionInfo.con == null) {
            ConnectionInfo.con = getDbConnection();
        }
        Boolean isActive = false;
        ResultSet rs = null;
        String qry = "SELECT active FROM deviceinfo WHERE deviceId = ?";
        try (PreparedStatement pstmt = ConnectionInfo.con.prepareStatement(qry)) {
            pstmt.setString(1, deviceId);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                isActive = rs.getBoolean("active");
            }
            rs.close();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return isActive;
    }

    public static void deRegisterDevice(String deviceId) {

        try {
            if (ConnectionInfo.con == null) {
                ConnectionInfo.con = getDbConnection();
            }
            Statement st = ConnectionInfo.con.createStatement();
            st.addBatch("DELETE FROM deviceinfo WHERE deviceid = '" + deviceId + "';");
            st.addBatch("DELETE FROM devicecallbackmethod WHERE deviceid = '" + deviceId + "';");

            st.executeBatch();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
//        try {
//            if (ConnectionInfo.con == null) {
//                ConnectionInfo.con = getDbConnection();
//            }
//            String Updatequery = "DELETE FROM deviceinfo WHERE deviceid = ?";
//            try (PreparedStatement pstmt = ConnectionInfo.con.prepareStatement(Updatequery)) {
//                pstmt.setString(1, deviceId);
//
//                pstmt.executeUpdate();
//                pstmt.close();
//            }
//        } catch (SQLException ex) {
//            System.out.println(ex.getMessage());
//        }
    }

}
