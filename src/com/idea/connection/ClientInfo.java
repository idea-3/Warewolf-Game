package com.idea.connection;

import java.net.InetAddress;

/**
 * Created by User on 4/29/2016.
 */
public class ClientInfo {
    private boolean isAlive;
    private InetAddress address;
    private int port;
    private String username;
    private int voteNum;
    private String role;

    public ClientInfo(boolean isAlive, InetAddress address, int port, String username, String role) {
        this.isAlive = isAlive;
        this.address = address;
        this.port = port;
        this.username = username;
        this.voteNum = 0;
        this.role = role;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public int getVoteNum() {
        return voteNum;
    }

    public String getRole() {
        return role;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setVoteNum(int voteNum) {
        this.voteNum = voteNum;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void set(boolean isAlive, InetAddress address, int port, String username) {
        this.isAlive = isAlive;
        this.address = address;
        this.port = port;
        this.username = username;
    }
}
