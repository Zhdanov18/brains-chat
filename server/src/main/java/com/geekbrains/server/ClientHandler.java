package com.geekbrains.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler {
    private String nickname;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean initRootDirectory;
    private String path;

    public String getPath() { return path; }

    public String getNickname() {
        return nickname;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        // /auth login1 pass1
                        if (msg.startsWith("/auth ")) {
                            String[] tokens = msg.split("\\s");
                            String nick = server.getAuthService().getNicknameByLoginAndPassword(tokens[1], tokens[2]);
                            if (nick != null && !server.isNickBusy(nick)) {
                                sendMsg("/authok " + nick);
                                nickname = nick;
                                server.subscribe(this);

                                this.path = this.getNickname();
                                this.initRootDirectory = server.getRepositoryHandler().initRootDirectory(this);

                                break;
                            }
                        }
                    }
                    while (true) {
                        String msg = in.readUTF();
                        if(msg.startsWith("/")) {
                            if (msg.equals("/end")) {
                                sendMsg("/end");
                                break;
                            }
                            if(msg.startsWith("/w ")) {
                                String[] tokens = msg.split("\\s", 3);
                                server.privateMsg(this, tokens[1], tokens[2]);
                            }

                            repositoryControl(msg);

                        } else {
                            server.broadcastMsg(nickname + ": " + msg);
                        }
                    }
                } catch (SocketException e) {//добавил catch
                    System.out.println("Соединение закрыто по таймауту");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    ClientHandler.this.disconnect();
                }
            }).start();
            new Thread(() -> { //Добавил thread
                long a = System.currentTimeMillis();
                while (System.currentTimeMillis() - a <= 12000 ) {}
                if(nickname == null){
                    disconnect();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
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

    private void repositoryControl(String msg) {
        if (!initRootDirectory) {
            return;
        }
        if(msg.startsWith("/send ")) {
            String[] tokens = msg.split("\\s", 2);
            String[] pathTokens = tokens[1].split("[\\\\|/]");

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            try (FileInputStream in = new FileInputStream(tokens[1])){
                int fileSize = in.available();
                long offset = 0;
                while (offset < fileSize) {
                    int readBytes = in.read(buffer, 0, bufferSize);
                    offset += readBytes;
                    server.getRepositoryHandler().sendFile(this, pathTokens[pathTokens.length - 1], buffer, readBytes, offset == bufferSize ? false : true);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if(msg.startsWith("/del ")) {
            String[] tokens = msg.split("\\s", 2);
            server.getRepositoryHandler().delete(this, tokens[1]);
        }
        if(msg.startsWith("/dir")) {
            server.getRepositoryHandler().showFiles(this);
        }
        if(msg.startsWith("/cd ")) {
            String[] tokens = msg.split("\\s", 2);
            String result = server.getRepositoryHandler().setPath(this, tokens[1]);
            if (result != null) {
                this.path = result;
            }
        }
        if(msg.startsWith("/md ")) {
            String[] tokens = msg.split("\\s", 2);
            server.getRepositoryHandler().makeDirectory(this, tokens[1]);
        }
    }
}
