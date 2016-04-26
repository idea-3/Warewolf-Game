package com.idea.connection;

import org.json.JSONException;
import org.json.JSONObject;

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
    private Scanner scan;
    private String username;

    /**
     * Konstruktor
     * @param hostName hostname server
     * @param port port server
     */
    public Client(String hostName, int port) throws IOException {
        clientSocket = new Socket(hostName, port);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        scan = new Scanner(System.in);
    }

    /**
     * Menjalankan client
     */
    public void run() throws JSONException, IOException {
        joinGame();
        out.close();
        in.close();
        clientSocket.close();
    }

    /**
     * Mengirim request ke server
     */
    private void sendToServer(JSONObject request) {
        System.out.println("Request to server: " + request);
        out.println(request.toString());
        out.flush();
    }

    /**
     * Menerima response dari server
     */
    private JSONObject receiveFromServer() throws JSONException, IOException {
        JSONObject response;
        String serverString;
        StringBuilder stringBuilder = new StringBuilder();

        serverString = in.readLine();
        stringBuilder.append(serverString);
        response = new JSONObject(stringBuilder.toString());
        System.out.println("Response from server: " + response);

        return response;
    }

    /**
     * Request join game ke server
     */
    private void joinGame() throws JSONException, IOException {
        System.out.print("Username: ");
        String username = scan.nextLine();
        JSONObject request = requestJoinGame(username);
        sendToServer(request);
        receiveFromServer();
    }

    /**
     * Menyusun JSON untuk request join game ke server
     * @param username username client
     */
    private JSONObject requestJoinGame(String username) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("method", "join");
        request.put("username", username);
        request.put("udp_address", clientSocket.getLocalAddress().getHostAddress());
        request.put("udp_port", clientSocket.getLocalPort());

        return request;
    }

    public static void main(String[] args) throws IOException, JSONException {
        Scanner scan = new Scanner(System.in);
        String hostName;
        int port;

        System.out.print("Input server IP host name: ");
        hostName = scan.nextLine();
        System.out.print("Input server port: ");
        port = Integer.parseInt(scan.nextLine());

        System.out.println("Connecting to " + hostName + " on port " + port);
        Client client = new Client(hostName, port);
        client.run();
    }
}
