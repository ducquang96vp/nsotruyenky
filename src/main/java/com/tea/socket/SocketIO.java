package com.tea.socket;

import com.tea.db.jdbc.DbManager;
import com.tea.item.Item;
import com.tea.item.ItemFactory;
import com.tea.model.Char;
import com.tea.network.Message;
import static com.tea.network.NoService.sendMsg;
import com.tea.network.Session;
import org.json.JSONException;
import org.json.JSONObject;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tea.server.Config;
import static com.tea.server.JFrameSendItem.checkNumber;
import com.tea.server.NinjaSchool;
import com.tea.server.Server;
import com.tea.util.Log;
import com.tea.util.NinjaUtils;
import java.io.IOException;

/**
 *
 * @author HIEU HIV
 */
public class SocketIO {

    public static Socket socket;
    public static boolean isInitialized;
    public static boolean connected;

    public static void init() {
        if (isInitialized) {
            return;
        }
        isInitialized = true;
        reconnect(1);
    }

    public static void listen1() {
    }

    public static void connect1() {
        if (connected) {
            return;
        }
        try {
            Config config = Config.getInstance();
            socket = IO.socket(config.getWebsocketHost() + ":" + config.getWebsocketPort());
            socket.connect();
            listen1();
            connected = true;
            Log.info("Connect to socket server successfully!");
        } catch (Exception e) {
            Log.error("Can not connect to socket server", e);
            reconnect(10000);
        }
    }

    public static void reconnect(long time) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(time);
                } catch (Exception ex) {
                }
                connect1();
            }

        })).start();
    }

    public static void on(String event, IAction action) {
        socket.on(event, new Emitter.Listener() {
            public void call(Object... args) {
                JSONObject json = null;
                if (args.length > 0) {
                    try {
                        json = (JSONObject) args[0];
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (json != null) {
                    try {
                        int serverId = json.getInt("server_id");
                        if (serverId == -1 || serverId == Config.getInstance().getServerID()) {
                            action.call(json);
                        }
                    } catch (JSONException e) {
                    }
                }
            }
        });
    }

    public static void on(byte event, IAction action) {
        on(String.valueOf(event), action);
    }
    
    public static void clientConnect(Session session, Message message) throws IOException {
        String type = message.reader().readUTF();
    }

    public static void emit(String event, String data) {
        Object obj = null;
        try {
            obj = new JSONObject(data);
        } catch (JSONException e) {
            obj = data;
        }

        while (true) {
            try {
                JSONObject send = new JSONObject();
                send.put("data", obj);
                //socket.emit(event, send);
                break;
            } catch (JSONException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SocketIO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static void emit(byte event, String data) {
        emit(String.valueOf(event), data);
    }

    public static void disconnect() {
        socket.disconnect();
    }

}
