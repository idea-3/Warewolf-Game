package com.idea.connection;

import java.net.InetAddress;

/**
 * Created by User on 4/29/2016.
 */
public class ClientInfo {
    private int playerId;
    private boolean isAlive;
    private InetAddress address;
    private int port;
    private String username;
    private int voteNum;

    public ClientInfo(int playerId, boolean isAlive, InetAddress address, int port, String username) {
        this.playerId = playerId;
        this.isAlive = isAlive;
        this.address = address;
        this.port = port;
        this.username = username;
        this.voteNum = 0;
    }

    public int getPlayerId() {
        return playerId;
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

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
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

    public void set(int playerId, boolean isAlive, InetAddress address, int port, String username) {
        this.playerId = playerId;
        this.isAlive = isAlive;
        this.address = address;
        this.port = port;
        this.username = username;
    }
}
