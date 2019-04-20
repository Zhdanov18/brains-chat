package com.geekbrains.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RepositoryHandler {

    public boolean initRootDirectory(ClientHandler sender) {
        File dir = new File(sender.getNickname());
        if (!dir.exists() && !dir.mkdir()) {
            return false;
        }
        return true;
    }

    public boolean sendFile(ClientHandler sender, String path, byte[] data, int len, boolean append) {
        try (FileOutputStream out = new FileOutputStream(sender.getPath() + System.getProperty("file.separator") + path, append)) {
            out.write(data, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void showFiles(ClientHandler sender) {
        File root = new File(sender.getPath());

        sender.sendMsg("\nFolder " + root.toString() + ":");

        for(File item: root.listFiles()) {
            if (item.isDirectory()) {
                sender.sendMsg(item.getName() + " <DIR>");
            }
            else {
                sender.sendMsg(item.getName());
            }
        }
    }

    public String setPath(ClientHandler sender, String path) {
        File dir = new File(sender.getNickname() + System.getProperty("file.separator") + path);
        if (!dir.exists()) {
            sender.sendMsg("Current folder is " + sender.getPath());
            return null;
        }
        sender.sendMsg("Current folder changed to " + dir.getPath());
        return dir.getPath();
    }

    public boolean delete(ClientHandler sender, String name) {
        //будем удалять только в текущем каталоге
        File file = new File(sender.getPath() + System.getProperty("file.separator") + name);
        if (!file.exists()) {
            return false;
        }
        return file.delete();
    }

    public boolean makeDirectory(ClientHandler sender, String name) {
        //и создаем тоже в текущем
        File file = new File(sender.getPath() + System.getProperty("file.separator") + name);
        if (file.exists() && file.isDirectory()) {
            return false;
        }
        return file.mkdir();
    }
}
