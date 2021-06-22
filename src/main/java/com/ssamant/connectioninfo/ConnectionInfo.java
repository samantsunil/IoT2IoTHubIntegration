/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ssamant.connectioninfo;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sunil
 */
public class ConnectionInfo {

    public static Connection con = null;

    public static String getScopeId() {
        Properties prop = readPropertyFile();
        String scopeId = prop.getProperty("com.ssamant.connectionInfo.dps.idScope");
        return scopeId;
    }

    public static String getGlobalEndPoint() {
        Properties prop = readPropertyFile();
        String endPoint = prop.getProperty("com.ssamant.connectionInfo.dps.globalEndpoint");
        return endPoint;
    }

    public static String getSymmetricKeyIndividual() {
        Properties prop = readPropertyFile();
        String symmkeyIndividual = prop.getProperty("com.ssamant.connectionInfo.dps.individualEnrollment.primaryKey");
        return symmkeyIndividual;
    }

    public static String getRegIdIndividual() {
        Properties prop = readPropertyFile();
        String regIdIndividual = prop.getProperty("com.ssamant.connectionInfo.dps.individualEnrollment.regId");
        return regIdIndividual;
    }

    public static String getRegIdGroup() {
        Properties prop = readPropertyFile();
        String regidGroup = prop.getProperty("com.ssamant.connectionInfo.dps.groupEnrollment.groupregId");
        return regidGroup;
    }

    public static String getSymmetricKeyGroup() {
        Properties prop = readPropertyFile();
        String symmkeyGroup = prop.getProperty("com.ssamant.connectionInfo.dps.groupEnrollment.primaryKey");
        return symmkeyGroup;
    }

    public static Connection getDbConnection() throws SQLException {
        String driver = "org.postgresql.Driver";
        Properties prop = readPropertyFile();
        String postgresHost = prop.getProperty("com.ssamant.connectioninfo.postgres.hostUrl");
        String postgresDb = prop.getProperty("com.ssamant.connectioninfo.postgres.database");
        String username = prop.getProperty("com.ssamant.connectioninfo.postgres.user");
        String password = prop.getProperty("com.ssamant.connectioninfo.postgres.password");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ConnectionInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
        //con = DriverManager.getConnection("jdbc:mysql://136.186.108.219:3306/dpp_resources", "root", "dpp2020*");
        con = DriverManager.getConnection(postgresHost + "/" + postgresDb, username, password);
        if (con != null) {
            System.out.println("Database connection successful.");
        } else {
            System.out.println("Failed to connect to database.");
        }
        return con;

    }

    public static Properties readPropertyFile() {
        Properties prop = new Properties();
        if (prop.isEmpty()) {
            InputStream input = ConnectionInfo.class.getClassLoader().getResourceAsStream("application.properties");
            //FileInputStream input=null;
            //String path= "./poc-config.properties";	
            try {
                //input = new FileInputStream(path);	
                prop.load(input);
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ex) {
                        Logger.getLogger(ConnectionInfo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        return prop;
    }

}
