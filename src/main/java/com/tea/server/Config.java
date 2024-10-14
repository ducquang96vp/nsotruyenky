package com.tea.server;

import com.tea.util.Log;
import com.tea.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import lombok.Getter;

/**
 *
 * @author Administrator
 */
@Getter
public class Config {

    private static final Config instance = new Config();
    private boolean vxmm;
    private boolean dametrung;
    private boolean doiluong;
    private boolean map;
    private boolean evenclose;
    private boolean historySQL;

    public static Config getInstance() {
        return instance;
    }

    // Version
    private int dataVersion = 26;
    private int itemVersion = 26;
    private int mapVersion = 26;
    private int skillVersion = 26;
    private boolean isTestVersion = false;

    // Server
    private int serverID;
    private int port;

    // MySql
    private String dbHost;
    private int dbPort;
    private String dbPortkey1;
    private String dbUser;
    private String dbPassword;
    private String dbHostkey1;
    private String setuserkey1;
    private String setpasskey1;
    private String dbNamekey1;
    private String dbName;
    private String dbDriver;
    private int dbMinConnections;
    private int dbMaxConnections;
    private int dbConnectionTimeout;
    private int dbLeakDetectionThreshold;
    private int dbIdleTimeout;

    // MongoDB
    private String mongodbHost;
    private int mongodbPort;
    private String mongodbName;
    private String mongodbUser;
    private String mongodbPassword;

    // Socket.IO
    private String websocketHost;
    private int websocketPort;

    // Game
    private double maxPercentAdd;
    private int sale;
    private boolean showLog;
    private boolean shinwa;
    private int shinwaFee;
    private double expconf;
    private int levelconf;
    private int CSconf;
    private int auctionMax;
    private int shinwaMax;
    private boolean arena;
    private int ipAddressLimit;
    private int maxQuantity;
    private String serverDir;
    private String notification;
    private int messageSizeMax;
    private String event;
    
    private int eventYear;
    private int eventMonth;
    private int eventDay;
    private int eventHour;
    private int eventMinute;
    private int eventSecond;

    //


    public boolean load() {
        try {
            FileInputStream input = new FileInputStream(new File("config.properties"));
            Properties props = new Properties();
            props.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            props.forEach((t, u) -> {
                Log.info(String.format("Config - %s: %s", t, u));
            });
            showLog = Boolean.parseBoolean(props.getProperty("server.log.display"));
            serverID = Integer.parseInt(props.getProperty("server.id"));
            port = Integer.parseInt(props.getProperty("server.port"));
            dbDriver = props.getProperty("db.driver");
            dbHost = props.getProperty("db.host");
            dbPort = Integer.parseInt(props.getProperty("db.port"));
            //dbUser = props.getProperty("db.user");  //DuyDebug
            dbUser ="root";
            dbPassword ="";
            //dbPassword = props.getProperty("db.password");
            dbName = props.getProperty("db.dbname");
            dbHostkey1 = props.getProperty("db.dbHostkey1");
            setuserkey1 = props.getProperty("db.setuserkey1");
            setpasskey1 = props.getProperty("db.setpasskey1");
            dbPortkey1 = props.getProperty("db.portkey1");
            dbNamekey1 = props.getProperty("db.dbnamekey1");
            dbMaxConnections = Integer.parseInt(props.getProperty("db.maxconnections"));
            dbMinConnections = Integer.parseInt(props.getProperty("db.minconnections"));
            dbConnectionTimeout = Integer.parseInt(props.getProperty("db.connectionTimeout"));
            dbLeakDetectionThreshold = Integer.parseInt(props.getProperty("db.leakDetectionThreshold"));
            dbIdleTimeout = Integer.parseInt(props.getProperty("db.idleTimeout"));
            mongodbHost = props.getProperty("mongodb.host");
            mongodbPort = Integer.parseInt(props.getProperty("mongodb.port"));
            mongodbUser = props.getProperty("mongodb.user");
            mongodbPassword = props.getProperty("mongodb.password");
            mongodbName = props.getProperty("mongodb.dbname");
            websocketHost = props.getProperty("websocket.host");
            websocketPort = Integer.parseInt(props.getProperty("websocket.port"));
            maxPercentAdd = Integer.parseInt(props.getProperty("game.upgrade.percent.add"));
            sale = Integer.parseInt(props.getProperty("game.store.discount"));
            shinwa = Boolean.parseBoolean(props.getProperty("game.shinwa.active"));
            shinwaFee = Integer.parseInt(props.getProperty("game.shinwa.fee"));
            auctionMax = Integer.parseInt(props.getProperty("game.shinwa.max"));
            shinwaMax = Integer.parseInt(props.getProperty("game.shinwa.player.max"));
            arena = Boolean.parseBoolean(props.getProperty("game.arena.active"));
            ipAddressLimit = Integer.parseInt(props.getProperty("game.login.limit"));
            maxQuantity = Integer.parseInt(props.getProperty("game.quantity.display.max"));
            expconf = Double.parseDouble(props.getProperty("game.expconf"));
            levelconf = Integer.parseInt(props.getProperty("game.levelconf"));
            CSconf = Integer.parseInt(props.getProperty("game.CSconf"));
            if (props.containsKey("server.resources.dir")) {
                serverDir = props.getProperty("server.resources.dir");
            } else {
                serverDir = System.getProperty("user.dir");
            }
            if (props.containsKey("server.notification")) {
                notification = props.getProperty("server.notification");
            }
            if (props.containsKey("game.data.version")) {
                dataVersion = Integer.parseInt(props.getProperty("game.data.version"));
            }
            if (props.containsKey("game.item.version")) {
                itemVersion = Integer.parseInt(props.getProperty("game.item.version"));
            }
            if (props.containsKey("game.map.version")) {
                mapVersion = Integer.parseInt(props.getProperty("game.map.version"));
            }
            if (props.containsKey("game.skill.version")) {
                skillVersion = Integer.parseInt(props.getProperty("game.skill.version"));
            }
            if (props.containsKey("client.data.size.max")) {
                messageSizeMax = Integer.parseInt(props.getProperty("client.data.size.max"));
            } else {
                messageSizeMax = 2024;
            }
            if (props.containsKey("game.event")) {
                event = props.getProperty("game.event");
            }

            if (props.containsKey("game.isTest")) {
                isTestVersion = Boolean.parseBoolean(props.getProperty("game.isTest"));
            }
             if (props.containsKey("evenclose")) {
                evenclose = Boolean.parseBoolean(props.getProperty("evenclose"));
            }else{
                evenclose = false;
            }

            if (props.containsKey("open.vxmm")) {
                vxmm = Boolean.parseBoolean(props.getProperty("open.vxmm"));
            }else{
                vxmm = false;
            }
            if (props.containsKey("open.dametrung")) {
                dametrung = Boolean.parseBoolean(props.getProperty("open.dametrung"));
            }else{
                dametrung = false;
            }
            if (props.containsKey("open.doiluong")) {
                doiluong = Boolean.parseBoolean(props.getProperty("open.doiluong"));
            }else{
                doiluong = false;
            }
            
            if (props.containsKey("open.map")) {
                map = Boolean.parseBoolean(props.getProperty("open.map"));
            }else{
                map = false;
            }
            if (props.containsKey("open.historySQL")) {
                historySQL = Boolean.parseBoolean(props.getProperty("open.historySQL"));
            }else{
                historySQL = false;
            }
            
            if (props.containsKey("event.year")) {
                eventYear = Integer.parseInt(props.getProperty("event.year"));
            }
            if (props.containsKey("event.month")) {
                eventMonth = Integer.parseInt(props.getProperty("event.month"));
            }
            if (props.containsKey("event.day")) {
                eventDay = Integer.parseInt(props.getProperty("event.day"));
            }
            if (props.containsKey("event.hour")) {
                eventHour = Integer.parseInt(props.getProperty("event.hour"));
            }
            if (props.containsKey("event.minute")) {
                eventMinute = Integer.parseInt(props.getProperty("event.minute"));
            }
            if (props.containsKey("event.second")) {
                eventSecond = Integer.parseInt(props.getProperty("event.second"));
            }
            
        } catch (IOException | NumberFormatException ex) {
            Log.error("load config err: " + ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    public boolean isTestVersion(){
        return this.isTestVersion;
    }
    public String getJdbcUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
    }
    public String getJdbcUrlkey() {
        String decodedDbHost = new String(Base64.getDecoder().decode(dbHostkey1));
    String decodedDbPort = new String(Base64.getDecoder().decode(dbPortkey1));
    String decodedDbName = new String(Base64.getDecoder().decode(dbNamekey1));
       return  "jdbc:mysql://" + decodedDbHost + ":" + decodedDbPort + "/" + decodedDbName;
   
    }
    public boolean isOpenVxmm() {
        return this.vxmm;
    }
    public boolean isOpendametrung() {
        return this.dametrung;
    }
    public boolean isOpendoiluong() {
        return this.doiluong;
    }
    public boolean isOpenEvent() {
        return this.evenclose;
    }
    public boolean isOpenmap() {
        return this.map;
    }
    public boolean isOpenhistorySQL() {
        return this.historySQL;
    }
    
     public int getEventYear() {
        return eventYear;
    }

    public int getEventMonth() {
        return eventMonth;
    }

    public int getEventDay() {
        return eventDay;
    }

    public int getEventHour() {
        return eventHour;
    }

    public int getEventMinute() {
        return eventMinute;
    }

    public int getEventSecond() {
        return eventSecond;
    }
     public double getexpconf() {
        return expconf;
    }
     
     public int getlevelconf() {
        return levelconf;
    }
     public int getCSconf() {
        return CSconf;
    }
    public String getMongodbUrl() {
        if (!StringUtils.isNullOrEmpty(mongodbUser) && !StringUtils.isNullOrEmpty(mongodbPassword)) {
            return String.format("mongodb://%s:%s@%s:%d/%s", mongodbUser, mongodbPassword, mongodbHost, mongodbPort, mongodbName);
        }
        return String.format("mongodb://%s:%d", mongodbHost, mongodbPort);
    }

}
