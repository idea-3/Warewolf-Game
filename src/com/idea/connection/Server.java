package com.idea.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by angelynz95 on 25-Apr-16.
 */
public class Server extends Thread {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Konstruktor
     * @param port port server
     */
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    /**
     * Run server
     */
    public void run() {
        String clientString;

        while (true) {
            System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
            try {
                clientSocket = serverSocket.accept();
                System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream());

                clientString = in.readLine();
                System.out.println("Received: " + clientString);
                out.println("Server receive your message: " + clientString);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        Thread thread = new Server(port);
        thread.start();
    }
}
