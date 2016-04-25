package com.idea.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by angelynz95 on 25-Apr-16.
 */
public class Client {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
//    private JSONObject request;

    /**
     * Konstruktor
     * @param ipAddress IP address server
     * @param port port server
     */
    public Client(String ipAddress, int port) throws IOException {
        clientSocket = new Socket(ipAddress, port);
    }

    /**
     * Run client
     */
    public void run() {
        String serverString;
        while (true) {
            System.out.print("Message: ");
            Scanner s = new Scanner(System.in);
            String message = s.nextLine();

            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println(message);
                out.flush();

                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                serverString = in.readLine();
                System.out.println(serverString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Connecting to " + args[0] + " on port " + args[1]);
        Client client = new Client(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
