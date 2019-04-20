package com.geekbrains.server;

public interface AuthService {
    String getNicknameByLoginAndPassword(String login, String password);
}
