package com.geekbrains.client;

import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network {
    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out;

    private static Callback callOnMsgReceived;
    private static Callback callOnAuthentificated;
    private static Callback callOnException;
    private static Callback callOnCloseConnection;

    static {
        Callback empty = args -> { };
        callOnMsgReceived = empty;
        callOnAuthentificated = empty;
        callOnException = empty;
        callOnCloseConnection = empty;
    }

    public static void setCallOnMsgReceived(Callback callOnMsgReceived) {
        Network.callOnMsgReceived = callOnMsgReceived;
    }

    public static void setCallOnAuthentificated(Callback callOnAuthentificated) {
        Network.callOnAuthentificated = callOnAuthentificated;
    }

    public static void setCallOnException(Callback callOnException) {
        Network.callOnException = callOnException;
    }

    public static void setCallOnCloseConnection(Callback callOnCloseConnection) {
        Network.callOnCloseConnection = callOnCloseConnection;
    }

    public static void sendAuth(String login, String password) {
        try {
            if (socket == null || socket.isClosed()) {
                connect();
            }
            out.writeUTF("/auth " + login + " " + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/authok ")) {
                            callOnAuthentificated.callback(msg.split("\\s")[1]);
                            break;
                        }
                    }
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.equals("/end")) {
                            break;
                        }
                        callOnMsgReceived.callback(msg);
                    }
                } catch (IOException e) {
                    callOnException.callback("Соединение с сервером разорвано");
                } finally {
                    closeConnection();
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void closeConnection() {
        callOnCloseConnection.callback();
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
